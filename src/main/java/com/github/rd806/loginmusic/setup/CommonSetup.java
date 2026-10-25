package com.github.rd806.loginmusic.setup;

import com.github.rd806.loginmusic.LoginMusic;
import com.github.rd806.loginmusic.config.MusicConfig;
import com.github.rd806.loginmusic.network.NetworkConfig;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = LoginMusic.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonSetup {
    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        // 注册网络
        NetworkConfig.register();
        // 加载音乐配置
        MusicConfig.loadFromConfig();
        LoginMusic.LOGGER.info("[LoginMusic] If you have any issues with LoginMusic, please report it at https://github.com/rd806/LoginMusic!");
    }
}
