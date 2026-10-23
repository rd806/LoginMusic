package com.github.rd806.loginmusic.setup;

import com.github.rd806.loginmusic.LoginMusic;
import com.github.rd806.loginmusic.config.ClientConfig;
import com.github.rd806.loginmusic.compat.cloth.ClothConfigGUI;
import com.github.rd806.loginmusic.gui.LyricLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.io.IOException;
import java.nio.file.Files;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = LoginMusic.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = LoginMusic.MODID, value = Dist.CLIENT)
public class ClientSetup {

    public ClientSetup(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // 注册 Cloth Config API
        container.registerExtensionPoint(IConfigScreenFactory.class, (container1, parent) ->
                ClothConfigGUI.buildScreen().setParentScreen(parent).build());
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // 创建缓存目录
        try {
            Files.createDirectories(LoginMusic.MUSICS_DIR);
            Files.createDirectories(LoginMusic.LYRICS_DIR);
        } catch (IOException e) {
            LoginMusic.LOGGER.warn("Failed to create cache directory!", e);
        }
        LyricLayer.setLyricLayer(ClientConfig.LYRIC_POS.get(), ClientConfig.LYRIC_COLOR.get());
        LoginMusic.LOGGER.info("Start LoginMusic on client!");
    }
}
