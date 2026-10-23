package com.github.rd806.loginmusic.config;

import com.github.rd806.loginmusic.event.SelectionKey;
import net.neoforged.neoforge.common.ModConfigSpec;


public class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // 音乐选择的关键字
    public static ModConfigSpec.EnumValue<SelectionKey> MUSIC_ID_TYPE;

    public static ModConfigSpec init() {
        BUILDER.push("Selection");
        MUSIC_ID_TYPE = BUILDER
                .comment("Keywords for music selection (name/uuid)")
                .translation("config.loginmusic.music.id")
                .defineEnum("type", SelectionKey.NAME);
        BUILDER.pop();

        return BUILDER.build();
    }
}
