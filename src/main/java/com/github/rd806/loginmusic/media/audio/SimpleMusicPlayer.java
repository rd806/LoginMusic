package com.github.rd806.loginmusic.media.audio;

import com.github.rd806.loginmusic.LoginMusic;
import com.github.rd806.loginmusic.config.ClientConfig;
import com.github.rd806.loginmusic.gui.MusicInfo;
import com.github.rd806.loginmusic.media.load.LoadMethod;
import com.github.rd806.loginmusic.media.lyric.LyricEntry;
import com.github.rd806.loginmusic.media.lyric.LyricParser;
import com.github.rd806.loginmusic.media.lyric.LyricPlayer;
import com.github.rd806.loginmusic.media.music.MusicEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 使用 Minecraft 内置音频系统（SoundEngine / OpenAL）播放入场音乐。
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SimpleMusicPlayer {

    private static final Minecraft mc = Minecraft.getInstance();
    private static MusicEntry musicEntry;
    private static String musicName;

    private static LoginMusicSound currentSound;
    private static boolean musicPlaying = false;
    private static boolean lyricPlaying = false;
    // 声音是否真正开始播放（音乐音量为 0 时 SoundEngine 会跳过播放）
    private static boolean soundStarted = false;
    // 声音长时间未开始播放时的计数
    private static int inactiveTicks = 0;

    // 播放音乐
    public static void playMusic(MusicEntry music, PreparedAudio audio, String lyric) {
        musicEntry = music;
        musicName = LoginMusic.removeExtension(music.getMusicName());
        if (audio == null) {
            LoginMusic.LOGGER.warn("Music audio not found!");
            return;
        }
        startMusic(audio, lyric);
        if (musicPlaying) {
            MusicInfo.playMusicInfo();
        }
    }

    // 使用 Minecraft 音频引擎播放
    private static void startMusic(PreparedAudio preparedAudio, String lyric) {
        try {
            currentSound = new LoginMusicSound(preparedAudio);
            soundStarted = false;
            inactiveTicks = 0;
            musicPlaying = true;
            // 先停止当前正在播放的原版背景音乐，之后由 PlaySoundEvent 阻止新的背景音乐
            if (ClientConfig.MUTE_VANILLA_MUSIC.get()) {
                mc.getSoundManager().stop(null, SoundSource.MUSIC);
            }
            mc.getSoundManager().play(currentSound);
            // 播放歌词
            startLyric(musicEntry, lyric, System.currentTimeMillis());
        } catch (Exception e) {
            LoginMusic.LOGGER.error("Fail to play music with the Minecraft sound engine.", e);
            stopMusic();
        }
    }

    // 播放歌词
    private static void startLyric(MusicEntry entry, String lyric, long startTimeMillis) {
        if (ClientConfig.ALLOW_LYRICS.get()) {
            if (lyric != null && !lyric.isEmpty() && !lyricPlaying) {
                List<LyricEntry> currentLyrics = LyricParser.parseLRC(lyric);
                lyricPlaying = true;
                // 计算展示歌词与播放开始的间隔时间，即已播放的时间
                if (startTimeMillis > 0) {
                    long elapsedTime = System.currentTimeMillis() - startTimeMillis;
                    LyricPlayer.startLyricDisplay(currentLyrics, elapsedTime);
                }
            } else {
                LoginMusic.LOGGER.warn("No lyrics found, {}", entry.getMusicName());
            }
        } else {
            LoginMusic.LOGGER.info("Lyrics are not available!");
        }
    }

    // 检测音乐是否播放结束
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !musicPlaying) {
            return;
        }
        if (currentSound != null && mc.getSoundManager().isActive(currentSound)) {
            // 声音已被音频引擎接受
            soundStarted = true;
            inactiveTicks = 0;
            return;
        }
        if (!soundStarted) {
            // 例如音乐音量被调为 0，SoundEngine 不会创建声道
            if (++inactiveTicks > 40) {
                LoginMusic.LOGGER.info("Login music was not started by the sound engine, stop it.");
                stopMusic();
            }
            return;
        }
        // 声道已停止，说明音乐自然播放结束
        finishMusic();
    }

    // 播放结束后停止音乐
    private static void finishMusic() {
        stopMusic();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.translatable("message.loginmusic.play.ended", musicName), true);
        }
    }

    // 阻止原版背景音乐与入场音乐同时播放
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlaySound(PlaySoundEvent event) {
        if (!musicPlaying || currentSound == null || !ClientConfig.MUTE_VANILLA_MUSIC.get()) {
            return;
        }
        SoundInstance sound = event.getSound();
        if (sound != null && sound != currentSound && sound.getSource() == SoundSource.MUSIC) {
            event.setSound(null);
        }
    }

    // 停止播放
    private static void stopMusic() {
        if (currentSound != null) {
            mc.getSoundManager().stop(currentSound);
            currentSound = null;
        }
        // 停止歌词
        LyricPlayer.stopLyricDisplay();
        MusicInfo.stopMusicInfo();
        lyricPlaying = false;
        musicPlaying = false;
        soundStarted = false;
        inactiveTicks = 0;
        LoadMethod.stopDownloading();
    }

    // 立即停止播放
    public static void stopCurrentMusic() {
        if (!musicPlaying && currentSound == null) { return; }
        stopMusic();
    }

    // 获取播放状态
    public static boolean isPlaying() { return musicPlaying; }
}
