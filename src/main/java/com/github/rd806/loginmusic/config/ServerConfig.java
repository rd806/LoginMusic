package com.github.rd806.loginmusic.config;

import com.github.rd806.loginmusic.LoginMusic;
import com.github.rd806.loginmusic.event.SelectionKey;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LoginMusic.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ServerConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // 音乐选择的关键字
    public static ForgeConfigSpec.EnumValue<SelectionKey> MUSIC_ID_TYPE;
    // 是否广播音乐
    public static ForgeConfigSpec.BooleanValue MUSIC_BROADCAST;

    public static ForgeConfigSpec init() {
        MUSIC_ID_TYPE = BUILDER
                .comment("Keywords for music selection (name/uuid)")
                .translation("config.loginmusic.music.id")
                .defineEnum("type", SelectionKey.NAME);
        MUSIC_BROADCAST = BUILDER
                .comment("Broadcast login music to every player")
                .translation("config.loginmusic.music.broadcast")
                .define("broadcast", false);
        return BUILDER.build();
    }
}
