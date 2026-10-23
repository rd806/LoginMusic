package com.github.rd806.loginmusic;

import com.mojang.logging.LogUtils;
import com.github.rd806.loginmusic.config.ClientConfig;
import com.github.rd806.loginmusic.config.ServerConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(LoginMusic.MODID)
public class LoginMusic {

    public static final String MODID = "loginmusic";
    public static final Logger LOGGER = LogUtils.getLogger();
    // 配置文件目录
    public static final Path DATA_PATH = Paths.get("data/login_music");
    // 音乐缓存目录
    public static final Path MUSICS_DIR = DATA_PATH.resolve("MusicsCache");
    // 歌词缓存目录
    public static final Path LYRICS_DIR = DATA_PATH.resolve("LyricsCache");
    // 模组加载上下文
    public static FMLJavaModLoadingContext fmlContext;

    public LoginMusic(FMLJavaModLoadingContext context) {
        fmlContext = context;
        // 生成配置文件
        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.init());
        context.registerConfig(ModConfig.Type.COMMON, ServerConfig.init());
    }

    // 去除文件扩展名
    public static String removeExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return filename;
        }
        int dotIndex = filename.lastIndexOf('.');
        // 注意：如果点位于第一个字符（隐藏文件）或不存在，则返回原文件名
        if (dotIndex <= 0) {
            return filename;
        }
        return filename.substring(0, dotIndex);
    }
}
