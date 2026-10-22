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

    public static ForgeConfigSpec init() {
        MUSIC_ID_TYPE = BUILDER
                .comment("Keywords for music selection (name/uuid)")
                .translation("config.loginmusic.music.id")
                .defineEnum("type", SelectionKey.NAME);
        return BUILDER.build();
    }
}
