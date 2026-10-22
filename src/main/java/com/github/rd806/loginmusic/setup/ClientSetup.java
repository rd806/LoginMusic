package com.github.rd806.loginmusic.setup;

import com.github.rd806.loginmusic.LoginMusic;
import com.github.rd806.loginmusic.config.ClientConfig;
import com.github.rd806.loginmusic.compat.cloth.ClothConfigGUI;
import com.github.rd806.loginmusic.media.layer.LyricLayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.io.IOException;
import java.nio.file.Files;

@Mod.EventBusSubscriber(modid = LoginMusic.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 创建缓存目录
        try {
            Files.createDirectories(LoginMusic.MUSICS_DIR);
            Files.createDirectories(LoginMusic.LYRICS_DIR);
        } catch (IOException e) {
            LoginMusic.LOGGER.error("Failed to create cache directory!", e);
        }
        // 注册配置界面
        // 用 Cloth Config API 构建的屏幕
        if (ModList.get().isLoaded("cloth_config")) {
            LoginMusic.fmlContext.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) ->
                            ClothConfigGUI.buildScreen().setParentScreen(parent).build()));
        }
        LyricLayer.setLyricLayer(ClientConfig.LYRIC_POS.get(), ClientConfig.LYRIC_COLOR.get());
    }
}
