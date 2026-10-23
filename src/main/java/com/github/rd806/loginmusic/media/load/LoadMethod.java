package com.github.rd806.loginmusic.media.load;

import com.github.rd806.loginmusic.LoginMusic;
import com.github.rd806.loginmusic.gui.LoadScreen;
import com.github.rd806.loginmusic.media.audio.PreparedAudio;
import com.github.rd806.loginmusic.media.audio.SimpleMusicPlayer;
import com.github.rd806.loginmusic.gui.MusicInfo;
import com.github.rd806.loginmusic.media.music.MusicCache;
import com.github.rd806.loginmusic.media.music.MusicEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import javax.sound.sampled.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 资源加载方法，新创建两个线程 {@code AUDIO_LOADER} 和 {@code LYRIC_LOADER}
 */
@OnlyIn(Dist.CLIENT)
public class LoadMethod {

    private static final Minecraft mc = Minecraft.getInstance();
    private static LoadScreen loadScreen;

    private static volatile PreparedAudio audio;
    private static volatile String lyric;

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
    // 加载资源
    private static CompletableFuture<Void> audioFuture;
    private static CompletableFuture<Void> lyricFuture;

    /* ----- 音乐下载方法 ----- */
    // 开始下载
    public static void startDownload(MusicEntry music, LoadScreen screen) {
        loadScreen = screen;
        // 创建两个 CompletableFuture 来跟踪下载任务
        audioFuture = CompletableFuture.runAsync(() -> audio = downloadMusic(music, (downloaded, total, progress) -> {
            Component status = Component.translatable("gui.loginmusic.load.progress.music",
                    String.format("%.1f", downloaded / 1024.0 / 1024.0),
                    String.format("%.1f", total / 1024.0 / 1024.0));
            mc.execute(() -> screen.updateAudioProgress(progress, status));
        }), AUDIO_LOADER);  // 使用 AUDIO_LOADER 作为执行器

        lyricFuture = CompletableFuture.runAsync(() -> lyric = downloadLyrics(music, (downloaded, total, progress) -> {
            Component status = Component.translatable("gui.loginmusic.load.progress.lyric",
                    String.format("%.1f", downloaded / 1024.0 / 1024.0),
                    String.format("%.1f", total / 1024.0 / 1024.0));
            mc.execute(() -> screen.updateLyricProgress(progress, status));
        }), LYRIC_LOADER);

        // 等待两个任务都完成
        CompletableFuture.allOf(audioFuture, lyricFuture)
                .thenAccept(ignored -> {
                    // 确保在 Minecraft 主线程中执行
                    mc.execute(() -> {
                        // 检查是否都完成了
                        if (audio != null) {
                            SimpleMusicPlayer.playMusic(music, audio, lyric);
                        } else {
                            LoginMusic.LOGGER.error("Could not load music!");
                            MusicInfo.stopMusicInfo();
                        }
                    });
                })
                .exceptionally(throwable -> {
                    // 错误处理
                    LoginMusic.LOGGER.error("Failed to download music or lyrics", throwable);
                    return null;
                });
    }

    // 进度回调
    @FunctionalInterface
    private interface DownloadCallback {
        void onProgress(long downloadedBytes, long totalBytes, float progress);
    }

    private static URLConnection openConnection(String path) {
        try {
            URI uri = new URI(path);
            URL url = uri.toURL();
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "LoginMusic");
            return connection;
        } catch (Exception ex) {
            LoginMusic.LOGGER.error("Failed to open URL", ex);
        }
        return null;
    }

    // 音乐下载方法
    private static PreparedAudio downloadMusic(MusicEntry music, DownloadCallback callback) {
        String name = music.getMusicName();
        String path = music.getMusicPath();
        try {
            PreparedAudio audio = MusicCache.getMusic(path);

            if (audio != null) {
                mc.execute(loadScreen::setAudioCompleted);
                return audio;
            }

            // 检查缓存，命中直接返回
            Path cacheFile = LoginMusic.MUSICS_DIR.resolve(name);
            if (isDownloaded(cacheFile, callback)) {
                LoginMusic.LOGGER.info("The file has been downloaded!");
                // 读取文件所有字节到 byte[]
                byte[] data = Files.readAllBytes(cacheFile);
                // 检查文件
                if (!TypeDetector.isAudioMagic(data, data.length)) {
                    LoginMusic.LOGGER.warn("Not an audio file!");
                    mc.execute(() -> loadScreen.setAudioError("Not an audio file!"));
                    return null;
                }
                // 包装为 ByteArrayInputStream
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                audio = prepareAudio(bais);
                MusicCache.putMusic(path, audio);
                mc.execute(loadScreen::setAudioCompleted);
                return audio;
            }

            // 从网络加载
            LoginMusic.LOGGER.info("Start downloading music from {}", path);
            URLConnection connection = openConnection(path);
            if (connection == null) {
                LoginMusic.LOGGER.error("Audio network connection error!");
                mc.execute(() -> loadScreen.setAudioError("Downloading error!"));
                return null;
            }
            // 获取文件大小
            long totalBytes = connection.getContentLengthLong();
            LoginMusic.LOGGER.info("Music size: {} bytes", totalBytes);

            InputStream inputStream = connection.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            long downloadedBytes = 0;

            // 加载文件
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
                downloadedBytes += bytesRead;
                // 回调进度
                if (callback != null) {
                    float progress = totalBytes > 0 ? downloadedBytes / (float) totalBytes : 0;
                    callback.onProgress(downloadedBytes, totalBytes, progress);
                    MusicInfo.setProgress(progress);
                }
            }
            // 转换为输入流
            byte[] data = baos.toByteArray();
            // 检查文件
            if (!TypeDetector.isAudioMagic(data, data.length)) {
                LoginMusic.LOGGER.warn("Not an audio file!");
                mc.execute(() -> loadScreen.setAudioError("Not an audio file!"));
                return null;
            }
            // 移除下载到本地功能
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            LoginMusic.LOGGER.info("Downloading music completed, total {} bytes", downloadedBytes);
            audio = prepareAudio(bais);
            MusicCache.putMusic(path, audio);
            mc.execute(loadScreen::setAudioCompleted);

            return audio;
        } catch (Exception e) {
            LoginMusic.LOGGER.warn("Downloading music error!", e);
            mc.execute(() -> loadScreen.setAudioError("Downloading error!"));
            return null;
        }
    }

    // 歌词下载方法
    private static String downloadLyrics(MusicEntry music, DownloadCallback callback) {
        String name = music.getLyricName();
        String path = music.getLyricPath();
        try {
            String lyric = MusicCache.getLyric(path);

            if (lyric != null) {
                mc.execute(loadScreen::setLyricCompleted);
                return lyric;
            }

            // 检查缓存，命中直接返回
            Path cacheFile = LoginMusic.LYRICS_DIR.resolve(name);
            if (isDownloaded(cacheFile, callback)) {
                try {
                    LoginMusic.LOGGER.info("Lyric file loaded");
                    lyric = Files.readString(cacheFile);
                    MusicCache.putLyric(path, lyric);
                    mc.execute(loadScreen::setLyricCompleted);
                    return lyric;
                } catch (Exception e) {
                    LoginMusic.LOGGER.error("Error while loading Lyric from {}", path, e);
                    mc.execute(() -> loadScreen.setLyricError("Downloading error!"));
                    return null;
                }
            }

            // 从网络加载
            LoginMusic.LOGGER.info("Start downloading lyrics from {}", path);
            URLConnection connection = openConnection(path);
            if (connection == null) {
                LoginMusic.LOGGER.error("Lyric network connection error!");
                mc.execute(() -> loadScreen.setLyricError("Downloading error!"));
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
            mc.execute(loadScreen::setLyricCompleted);
            return lyric;
        } catch (Exception e) {
            LoginMusic.LOGGER.warn("Downloading lyric error!", e);
            mc.execute(() -> loadScreen.setLyricError("Downloading error!"));
            return null;
        }
    }

    // 检查是否有本地缓存
    private static boolean isDownloaded(Path cacheFile, DownloadCallback callback) {
        try {
            if (Files.exists(cacheFile) && callback != null) {
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

    // 停止加载线程
    public static void stopDownloading() {
        if (audioFuture != null) { audioFuture.cancel(true); }
        if (lyricFuture != null) { lyricFuture.cancel(true); }
        LoginMusic.LOGGER.info("Stop downloading!");
    }

    // 准备外部音乐
    private static PreparedAudio prepareAudio(ByteArrayInputStream inputStream) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputStream);
            // 转换格式
            AudioFormat sourceFormat = audioInputStream.getFormat();
            AudioFormat targetFormat = getTargetFormat(sourceFormat);
            if (!sourceFormat.matches(targetFormat)) {
                audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
            }
            // 预加载音频数据到字节数组，供 Minecraft 音频引擎流式读取
            byte[] audioData = audioInputStream.readAllBytes();
            LoginMusic.LOGGER.info("Audio data prepared!");
            return new PreparedAudio(audioData, targetFormat);
        } catch (UnsupportedAudioFileException e) {
            LoginMusic.LOGGER.error("Unsupported type: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            LoginMusic.LOGGER.error("Fail to play music");
            return null;
        }
    }

    private static @NotNull AudioFormat getTargetFormat(AudioFormat sourceFormat) throws UnsupportedAudioFileException {
        float sampleRate = sourceFormat.getSampleRate();
        if (sampleRate <= 0) {
            throw new UnsupportedAudioFileException("Unknown sample rate");
        }
        // Minecraft 的 OpenAL 只支持单声道/立体声的 16 位 PCM
        int channels = Math.min(2, sourceFormat.getChannels());
        return new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate,
                16,
                channels,
                channels * 2,
                sampleRate,
                false
        );
    }
}
