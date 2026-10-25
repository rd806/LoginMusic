package com.github.rd806.loginmusic.media.audio;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

/**
 * 一边读取网络音频，一边把原始字节缓存下来。
 * 完整读完（到达流末尾）后回调一次，用于下次播放时直接使用内存缓存。
 */
@OnlyIn(Dist.CLIENT)
public class TeeInputStream extends FilterInputStream {

    private static final int MAX_INITIAL_SIZE = 8 * 1024 * 1024;

    private final ByteArrayOutputStream captured;
    private final Consumer<byte[]> onComplete;
    private final long expectedLength;
    private boolean completed;

    public TeeInputStream(InputStream input, long totalBytes, Consumer<byte[]> onComplete) {
        super(input);
        int initialSize = totalBytes > 0
                ? (int) Math.min(totalBytes, MAX_INITIAL_SIZE)
                : 8192;
        this.captured = new ByteArrayOutputStream(Math.max(initialSize, 8192));
        this.onComplete = onComplete;
        this.expectedLength = totalBytes;
    }

    @Override
    public int read() throws IOException {
        int value = super.read();
        if (value == -1) {
            complete();
        } else {
            captured.write(value);
        }
        return value;
    }

    @Override
    public int read(byte @NotNull [] buffer, int offset, int length) throws IOException {
        int read = super.read(buffer, offset, length);
        if (read == -1) {
            complete();
        } else if (read > 0) {
            captured.write(buffer, offset, read);
        }
        return read;
    }

    // skip 也必须经过 read，否则跳过的字节不会进入缓存
    @Override
    public long skip(long count) throws IOException {
        long remaining = count;
        byte[] buffer = new byte[8192];
        while (remaining > 0) {
            int read = read(buffer, 0, (int) Math.min(buffer.length, remaining));
            if (read == -1) {
                break;
            }
            remaining -= read;
        }
        return count - remaining;
    }

    private void complete() {
        if (!completed) {
            completed = true;
            // 已知总大小时，只有完整读取才写入缓存，避免缓存截断的音频
            if (expectedLength > 0 && captured.size() != expectedLength) {
                return;
            }
            try {
                onComplete.accept(captured.toByteArray());
            } catch (Throwable ignored) {
                // 缓存失败不应影响音频播放
            }
        }
    }
}
