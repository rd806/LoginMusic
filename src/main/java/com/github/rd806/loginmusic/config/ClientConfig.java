package com.github.rd806.loginmusic.config;

import com.github.rd806.loginmusic.LoginMusic;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;

// 通用配置文件
@Mod.EventBusSubscriber(modid = LoginMusic.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public enum Position {
        UP,
        MIDDLE,
        DOWN,
    }

    // 是否展示加载界面
    public static ForgeConfigSpec.BooleanValue SHOW_LOADING;
    // 允许音乐播放的范围
    public static ForgeConfigSpec.ConfigValue<Integer> MUSIC_PLAY_RANGE;
    // 是否允许展示歌词
    public static ForgeConfigSpec.BooleanValue ALLOW_LYRICS;
    // 歌词文本位置
    public static ForgeConfigSpec.EnumValue<Position> LYRIC_POS;
    // 歌词颜色
    public static ForgeConfigSpec.ConfigValue<Integer> LYRIC_COLOR;
    // 是否播放来自其他玩家的音频
    public static ForgeConfigSpec.BooleanValue ALLOW_OTHERS_MUSIC;
    // 播放入场音乐时是否静音原版背景音乐
    public static ForgeConfigSpec.BooleanValue MUTE_VANILLA_MUSIC;
    // 缓存容量
    public static ForgeConfigSpec.ConfigValue<Integer> CACHE_SIZE;

    public static ForgeConfigSpec init() {
        BUILDER.push("Loading").translation("config.loginmusic.loading.title");
        SHOW_LOADING = BUILDER
                .comment("Whether to show loading screen when preparing music")
                .translation("config.loginmusic.loading.show")
                .define("show_loading", true);
        BUILDER.pop();

        BUILDER.push("Music").translation("config.loginmusic.music.title");
        MUSIC_PLAY_RANGE = BUILDER
                .comment("Range of music play (a non negative integer)")
                .translation("config.loginmusic.music.play_range")
                .defineInRange("range", 3, 0, 100);
        ALLOW_OTHERS_MUSIC = BUILDER
                .comment("Whether to play musics from other players")
                .translation("config.loginmusic.music.allow_others")
                .define("AllowOthersMusic", false);
        MUTE_VANILLA_MUSIC = BUILDER
                .comment("Whether to mute Minecraft's background music while login music is playing")
                .translation("config.loginmusic.music.mute_vanilla")
                .define("MuteVanillaMusic", true);
        CACHE_SIZE = BUILDER
                .comment("The maximum number of cache entries")
                .translation("config.loginmusic.music.cache_size")
                .defineInRange("CacheSize", 5, 0, 10);
        BUILDER.pop();

        BUILDER.push("Lyric").translation("config.loginmusic.lyric.title");
        ALLOW_LYRICS = BUILDER
                .comment("Whether to show lyrics while playing music")
                .translation("config.loginmusic.lyric.allow")
                .define("ShowLyrics", true);
        LYRIC_POS = BUILDER
                .comment("Defines the position of the lyrics")
                .translation("config.loginmusic.lyric.pos")
                .defineEnum("LyricsPosition", Position.DOWN);
        LYRIC_COLOR = BUILDER
                .comment("Defines the color of the lyrics you want to use")
                .translation("config.loginmusic.lyric.color")
                .define("LyricsColor", 0xFFFFFF);
        BUILDER.pop();
        return BUILDER.build();
    }
}
