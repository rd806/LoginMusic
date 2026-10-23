package com.github.rd806.loginmusic.setup;

import com.github.rd806.loginmusic.LoginMusic;
import com.github.rd806.loginmusic.config.MusicConfig;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;


@Mod(value = LoginMusic.MODID)
@EventBusSubscriber(modid = LoginMusic.MODID)
public class CommonSetup {
    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        // 加载音乐配置
        MusicConfig.loadFromConfig();
        LoginMusic.LOGGER.info("[LoginMusic] If you have any issues with LoginMusic, please report it at https://github.com/rd806/LoginMusic!");
    }
}
