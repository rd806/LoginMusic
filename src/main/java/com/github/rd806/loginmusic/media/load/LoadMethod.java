package com.github.rd806.loginmusic.media.load;

import com.github.rd806.loginmusic.LoginMusic;
import com.github.rd806.loginmusic.gui.LoadScreen;
import com.github.rd806.loginmusic.gui.MusicInfo;
import com.github.rd806.loginmusic.media.audio.SimpleMusicPlayer;
import com.github.rd806.loginmusic.media.audio.StreamingAudioStream;
import com.github.rd806.loginmusic.media.audio.TeeInputStream;
import com.github.rd806.loginmusic.media.music.MusicCache;
import com.github.rd806.loginmusic.media.music.MusicEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 音频以“边下边播”的方式加载：后台建立音频流并预缓冲一小段后即可开始播放，
 * 不需要等待整个文件下载和解码完成。歌词独立下载，晚到时会按已播放时间接入。
 */
@OnlyIn(Dist.CLIENT)
public class LoadMethod {

    private static final Minecraft mc = Minecraft.getInstance();
    // 预缓冲秒数，越小开播越快，但网络抖动时更容易卡顿
    private static final int PREBUFFER_SECONDS = 3;
    private static final int READ_BUFFER_SIZE = 64 * 1024;

    private static final Object LOAD_LOCK = new Object();
    private static final AtomicInteger LOAD_ID = new AtomicInteger();

    private static volatile String lyric;
    private static volatile StreamingAudioStream pendingStream;
    private static CompletableFuture<Void> audioFuture;
    private static CompletableFuture<Void> lyricFuture;

    // 音频加载线程
    public static final ExecutorService AUDIO_LOADER = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "LoginMusic AudioLoader");
        t.setDaemon(true);
        return t;
    });
    // 歌词加载线程
    public static final ExecutorService LYRIC_LOADER = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "LoginMusic LyricLoader");
        t.setDaemon(true);
        return t;
    });

    // 进度回调
    @FunctionalInterface
    private interface DownloadCallback {
        void onProgress(long downloadedBytes, long totalBytes, float progress);
    }

    private static URLConnection openConnection(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        try {
            URI uri = new URI(path);
            URLConnection connection = uri.toURL().openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "LoginMusic");
            return connection;
        } catch (Exception ex) {
            LoginMusic.LOGGER.error("Failed to open URL", ex);
        }
        return null;
    }

    /**
     * 开始加载音乐（边下边播）
     */
    public static void startStreaming(MusicEntry music, LoadScreen screen) {
        lyric = null;
        pendingStream = null;
        int loadId = LOAD_ID.incrementAndGet();

        // 歌词：独立下载，不阻塞音乐开始播放
        lyricFuture = CompletableFuture.runAsync(() -> {
            String text = openLyrics(music, screen, (downloaded, total, progress) -> {
                Component status = Component.translatable("gui.loginmusic.load.progress.lyric",
                        String.format("%.1f", downloaded / 1024.0 / 1024.0),
                        String.format("%.1f", total / 1024.0 / 1024.0));
                mc.execute(() -> screen.updateLyricProgress(progress, status));
            });
            if (text == null || LOAD_ID.get() != loadId) {
                return;
            }
            lyric = text;
            mc.execute(() -> {
                if (LOAD_ID.get() != loadId) {
                    return;
                }
                screen.setLyricCompleted();
                // 音乐可能已经开始播放，此时按已播放时长接入歌词
                SimpleMusicPlayer.addLyrics(text);
            });
        }, LYRIC_LOADER);

        // 音频：建立流、预缓冲，完成后立即开始播放
        audioFuture = CompletableFuture.runAsync(() -> {
            StreamingAudioStream stream = null;
            try {
                InputStream input = openAudioInput(music);
                if (input == null) {
                    mc.execute(() -> {
                        if (LOAD_ID.get() != loadId) {
                            return;
                        }
                        screen.setAudioError("Audio not found!");
                        SimpleMusicPlayer.onAudioError(new IOException("Audio input is not available"));
                    });
                    return;
                }
                stream = new StreamingAudioStream(input, PREBUFFER_SECONDS,
                        progress -> {
                            Component status = Component.translatable("gui.loginmusic.load.buffering",
                                    String.format("%.0f%%", progress * 100));
                            mc.execute(() -> {
                                if (LOAD_ID.get() != loadId) {
                                    return;
                                }
                                screen.updateAudioProgress(progress, status);
                                MusicInfo.setProgress(progress);
                            });
                        },
                        error -> mc.execute(() -> {
                            if (LOAD_ID.get() == loadId) {
                                SimpleMusicPlayer.onAudioError(error);
                            }
                        })
                );

                StreamingAudioStream ready = stream;
                synchronized (LOAD_LOCK) {
                    if (LOAD_ID.get() != loadId) {
                        ready.close();
                        return;
                    }
                    pendingStream = ready;
                }

                mc.execute(() -> {
                    synchronized (LOAD_LOCK) {
                        if (LOAD_ID.get() != loadId) {
                            ready.close();
                            return;
                        }
                        pendingStream = null;
                    }
                    screen.setAudioCompleted();
                    // 极端情况下可能已有另一首音乐开始播放，避免两首同时播放
                    if (SimpleMusicPlayer.isPlaying()) {
                        ready.close();
                        return;
                    }
                    SimpleMusicPlayer.playMusic(music, ready, lyric);
                });
            } catch (Throwable throwable) {
                if (stream != null) {
                    stream.close();
                }
                LoginMusic.LOGGER.error("Failed to open the audio stream", throwable);
                mc.execute(() -> {
                    if (LOAD_ID.get() != loadId) {
                        return;
                    }
                    screen.setAudioError("Downloading error!");
                    SimpleMusicPlayer.onAudioError(throwable);
                });
            }
        }, AUDIO_LOADER);
    }

    /**
     * 打开音频输入：
     * <br>
     * 内存缓存 > 本地缓存文件 > 网络 URL
     */
    private static InputStream openAudioInput(MusicEntry music) throws IOException {
        String name = music.getMusicName();
        String path = music.getMusicPath();
        // 先读取缓存
        byte[] cached = MusicCache.getMusic(path);
        if (cached != null) {
            LoginMusic.LOGGER.info("Use cached audio for {}", path);
            return new ByteArrayInputStream(cached);
        }
        // 本地缓存
        if (name != null && !name.isEmpty()) {
            Path local = LoginMusic.MUSICS_DIR.resolve(name);
            if (Files.isRegularFile(local)) {
                LoginMusic.LOGGER.info("Streaming local audio {}", local);
                return new BufferedInputStream(Files.newInputStream(local), READ_BUFFER_SIZE);
            }
        }
        // 网络 URL
        URLConnection connection = openConnection(path);
        if (connection == null) {
            return null;
        }
        long totalBytes = connection.getContentLengthLong();
        InputStream raw = connection.getInputStream();
        if ("file".equals(connection.getURL().getProtocol())) {
            return new BufferedInputStream(raw, READ_BUFFER_SIZE);
        }
        // 网络音频：边读边缓存原始字节，整首播放完后可供下次直接使用
        TeeInputStream tee = new TeeInputStream(raw, totalBytes, bytes -> MusicCache.putMusic(path, bytes));
        return new BufferedInputStream(tee, READ_BUFFER_SIZE);
    }

    // 歌词下载
    private static String openLyrics(MusicEntry music, LoadScreen screen, DownloadCallback callback) {
        String name = music.getLyricName();
        String path = music.getLyricPath();
        if (name == null || name.isEmpty()) {
            return null;
        }
        try {
            String lyric = MusicCache.getLyric(path);
            if (lyric != null) {
                mc.execute(screen::setLyricCompleted);
                return lyric;
            }

            // 检查缓存，命中直接返回
            Path cacheFile = LoginMusic.LYRICS_DIR.resolve(name);
            if (isDownloaded(cacheFile, callback)) {
                try {
                    LoginMusic.LOGGER.info("Lyric file loaded");
                    lyric = Files.readString(cacheFile);
                    MusicCache.putLyric(path, lyric);
                    mc.execute(screen::setLyricCompleted);
                    return lyric;
                } catch (Exception e) {
                    LoginMusic.LOGGER.error("Error while loading Lyric from {}", path, e);
                    mc.execute(() -> screen.setLyricError("Downloading error!"));
                    return null;
                }
            }
            // 从网络加载
            LoginMusic.LOGGER.info("Start downloading lyrics from {}", path);
            URLConnection connection = openConnection(path);
            if (connection == null) {
                LoginMusic.LOGGER.error("Lyric network connection error!");
                mc.execute(() -> screen.setLyricError("Downloading error!"));
                return null;
            }

            // 获取文件大小
            long totalBytes = connection.getContentLengthLong();
            LoginMusic.LOGGER.info("Lyrics size: {} bytes", totalBytes);
            // 下载文件
            InputStream inputStream = connection.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            long downloadedBytes = 0;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
                downloadedBytes += bytesRead;
                // 回调进度
                if (callback != null) {
                    float progress = totalBytes > 0 ? downloadedBytes / (float) totalBytes : 0;
                    callback.onProgress(downloadedBytes, totalBytes, progress);
                }
            }
            byte[] data = baos.toByteArray();
            // 检测编码
            String charset = TypeDetector.detectCharset(data);
            // 转换为字符串
            lyric = new String(data, charset);
            MusicCache.putLyric(path, lyric);
            LoginMusic.LOGGER.info("Downloading lyric completed, total {} bytes", downloadedBytes);
            mc.execute(screen::setLyricCompleted);
            return lyric;
        } catch (Exception e) {
            LoginMusic.LOGGER.warn("Downloading lyric error!", e);
            mc.execute(() -> screen.setLyricError("Downloading error!"));
            return null;
        }
    }

    // 检查是否有本地缓存
    private static boolean isDownloaded(Path cacheFile, DownloadCallback callback) {
        try {
            if (Files.isRegularFile(cacheFile) && callback != null) {
                long size = Files.size(cacheFile);
                callback.onProgress(size, size, 1.0f);
                return true;
            }
            return false;
        } catch (Exception e) {
            LoginMusic.LOGGER.warn("Failed to load from local file!", e);
            return false;
        }
    }

    // 停止加载线程并关闭尚未开始播放的音频流
    public static void stopDownloading() {
        LOAD_ID.incrementAndGet();
        if (audioFuture != null) { audioFuture.cancel(true); }
        if (lyricFuture != null) { lyricFuture.cancel(true); }
        synchronized (LOAD_LOCK) {
            if (pendingStream != null) {
                pendingStream.close();
                pendingStream = null;
            }
        }
        LoginMusic.LOGGER.info("Stop loading!");
    }
}
