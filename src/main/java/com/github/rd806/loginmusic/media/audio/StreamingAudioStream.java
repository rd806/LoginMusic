package com.github.rd806.loginmusic.media.audio;

import net.minecraft.client.sounds.AudioStream;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

/**
 * 边下边播的音频流：后台线程从网络/文件读取并解码为 PCM，
 * 预缓冲一小段后就可以交给 Minecraft 的 SoundEngine 播放。
 * <p>
 * {@link #read(int)} 只从内存队列取数据，不会阻塞 SoundEngine 的线程；
 * 网络读取由独立的 pump 线程完成。
 */
@OnlyIn(Dist.CLIENT)
public class StreamingAudioStream implements AudioStream {

    // 队列最多缓存约 12 秒音频，避免网络抖动导致播放中断
    private static final int MAX_QUEUED_CHUNKS = 12;
    // 预缓冲最长的等待时间
    private static final long PREBUFFER_TIMEOUT_MS = 20_000L;

    private final AudioInputStream stream;
    private final AudioFormat format;
    private final int frameSize;
    private final int chunkSize;
    private final int prebufferBytes;
    private final Consumer<Float> progressCallback;
    private final Consumer<Throwable> errorCallback;
    private final Object lock = new Object();
    private final Deque<ByteBuffer> queue = new ArrayDeque<>();
    private final Thread pumpThread;

    private int bufferedBytes;
    private boolean progressComplete;
    private volatile boolean finished;
    private volatile boolean closed;
    private volatile Throwable error;

    public StreamingAudioStream(InputStream input, int prebufferSeconds,
                                Consumer<Float> progressCallback,
                                Consumer<Throwable> errorCallback) throws IOException, UnsupportedAudioFileException {
        this.progressCallback = progressCallback;
        this.errorCallback = errorCallback;

        AudioInputStream source;
        try {
            source = AudioSystem.getAudioInputStream(input);
        } catch (Throwable throwable) {
            closeQuietly(input);
            throw throwable;
        }

        AudioInputStream converted;
        try {
            AudioFormat sourceFormat = source.getFormat();
            AudioFormat targetFormat = getTargetFormat(sourceFormat);
            converted = sourceFormat.matches(targetFormat)
                    ? source
                    : AudioSystem.getAudioInputStream(targetFormat, source);
        } catch (Throwable throwable) {
            closeQuietly(source);
            closeQuietly(input);
            throw throwable;
        }

        this.stream = converted;
        this.format = converted.getFormat();
        this.frameSize = Math.max(1, this.format.getFrameSize());

        int oneSecond = (int) (this.format.getSampleRate() * this.frameSize);
        this.chunkSize = Math.max(this.frameSize, oneSecond - oneSecond % this.frameSize);
        this.prebufferBytes = Math.max(this.chunkSize, Math.max(1, prebufferSeconds) * this.chunkSize);

        this.pumpThread = new Thread(this::pump, "LoginMusic-AudioStream");
        this.pumpThread.setDaemon(true);
        this.pumpThread.start();

        try {
            awaitPrebuffer();
        } catch (Throwable throwable) {
            close();
            throw throwable;
        }
    }

    private static @NotNull AudioFormat getTargetFormat(AudioFormat sourceFormat) throws UnsupportedAudioFileException {
        float sampleRate = sourceFormat.getSampleRate();
        int channels = sourceFormat.getChannels();
        if (sampleRate <= 0 || channels <= 0) {
            throw new UnsupportedAudioFileException("Unknown audio format: " + sourceFormat);
        }
        // Minecraft 的 OpenAL 只支持单声道/立体声的 16 位 PCM
        int targetChannels = Math.min(2, channels);
        return new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate,
                16,
                targetChannels,
                targetChannels * 2,
                sampleRate,
                false
        );
    }

    // 等待预缓冲完成，短音频在读到文件末尾时也会提前返回
    private void awaitPrebuffer() throws IOException {
        long deadline = System.currentTimeMillis() + PREBUFFER_TIMEOUT_MS;
        synchronized (lock) {
            while (!closed && !finished && error == null && bufferedBytes < prebufferBytes) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    throw new IOException("Timed out while buffering audio");
                }
                try {
                    lock.wait(Math.min(remaining, 200L));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while buffering audio", exception);
                }
            }
        }
        if (error != null) {
            throw new IOException("Failed to buffer audio", error);
        }
        if (closed) {
            throw new IOException("Audio stream was closed");
        }
        if (finished && bufferedBytes <= 0) {
            throw new IOException("Audio stream is empty");
        }
    }

    // 后台读取并解码
    private void pump() {
        byte[] readBuffer = new byte[chunkSize];
        try {
            while (!closed) {
                int written = 0;
                while (!closed && written < chunkSize) {
                    // 按完整帧读取，避免 OpenAL 解析到半个采样
                    int toRead = chunkSize - written;
                    toRead -= toRead % frameSize;
                    if (toRead <= 0) {
                        break;
                    }
                    int read = stream.read(readBuffer, written, toRead);
                    if (read == -1) {
                        finished = true;
                        break;
                    }
                    if (read == 0) {
                        Thread.yield();
                        continue;
                    }
                    written += read;
                }
                // 丢弃不完整的帧
                int aligned = written - (written % frameSize);
                if (aligned > 0) {
                    ByteBuffer buffer = BufferUtils.createByteBuffer(aligned);
                    buffer.put(readBuffer, 0, aligned);
                    buffer.flip();
                    enqueue(buffer);
                }
                if (finished) {
                    break;
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (Throwable throwable) {
            if (!closed) {
                error = throwable;
                if (errorCallback != null) {
                    try {
                        errorCallback.accept(throwable);
                    } catch (Throwable ignored) {
                    }
                }
            }
        } finally {
            finished = true;
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }

    private void enqueue(ByteBuffer buffer) throws InterruptedException {
        synchronized (lock) {
            while (!closed && queue.size() >= MAX_QUEUED_CHUNKS) {
                lock.wait(200L);
            }
            if (closed) {
                return;
            }
            queue.add(buffer);
            bufferedBytes += buffer.remaining();
            if (progressCallback != null && !progressComplete) {
                float progress = Math.min(1.0F, (float) bufferedBytes / prebufferBytes);
                if (progress >= 1.0F) {
                    progressComplete = true;
                }
                try {
                    progressCallback.accept(progress);
                } catch (Throwable ignored) {
                }
            }
            lock.notifyAll();
        }
    }

    @Override
    public @NotNull AudioFormat getFormat() {
        return format;
    }

    @Override
    public @NotNull ByteBuffer read(int size) {
        synchronized (lock) {
            if (closed || size <= 0 || bufferedBytes <= 0) {
                return null;
            }
            // 有多少返回多少：即使不足一个完整缓冲区，也可以交给 OpenAL 播放
            int toRead = Math.min(size, bufferedBytes);
            toRead -= toRead % frameSize;
            if (toRead <= 0) {
                return null;
            }
            ByteBuffer output = BufferUtils.createByteBuffer(toRead);
            while (output.hasRemaining() && !queue.isEmpty()) {
                ByteBuffer head = queue.peek();
                int count = Math.min(head.remaining(), output.remaining());
                ByteBuffer slice = head.duplicate();
                slice.limit(slice.position() + count);
                output.put(slice);
                head.position(head.position() + count);
                if (!head.hasRemaining()) {
                    queue.poll();
                }
            }
            int read = output.position();
            if (read <= 0) {
                return null;
            }
            bufferedBytes -= read;
            // 唤醒可能因为队列已满而等待的 pump 线程
            lock.notifyAll();
            output.flip();
            return output;
        }
    }

    public boolean isFinished() {
        return finished && bufferedBytes <= 0;
    }

    public Throwable getError() {
        return error;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        synchronized (lock) {
            queue.clear();
            bufferedBytes = 0;
            lock.notifyAll();
        }
        closeQuietly(stream);
        pumpThread.interrupt();
    }

    private static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ignored) {
            // 关闭音频
        }
    }
}
