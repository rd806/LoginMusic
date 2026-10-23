package com.github.rd806.loginmusic;

import com.github.rd806.loginmusic.config.ClientConfig;
import com.github.rd806.loginmusic.config.ServerConfig;
import com.github.rd806.loginmusic.network.NetworkConfig;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(LoginMusic.MODID)
public class LoginMusic {

    // MODID
    public static final String MODID = "loginmusic";
    // 日志文件
    public static final Logger LOGGER = LogUtils.getLogger();
    // 配置文件目录
    public static final Path DATA_PATH = Paths.get("data/login_music");
    // 音乐缓存目录
    public static final Path MUSICS_DIR = DATA_PATH.resolve("MusicsCache");
    // 歌词缓存目录
    public static final Path LYRICS_DIR = DATA_PATH.resolve("LyricsCache");


    public LoginMusic(IEventBus modEventBus, ModContainer modContainer) {
        // 注册网络
        modEventBus.addListener(NetworkConfig::register);
        // 生成配置文件
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.init());
        modContainer.registerConfig(ModConfig.Type.COMMON, ServerConfig.init());
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