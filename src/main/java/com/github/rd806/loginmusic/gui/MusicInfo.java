package com.github.rd806.loginmusic.gui;

import com.github.rd806.loginmusic.LoginMusic;
import com.github.rd806.loginmusic.media.music.MusicEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@Mod(value = LoginMusic.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = LoginMusic.MODID, value = Dist.CLIENT)
public class MusicInfo {

    private enum Status {
        PREPARING,
        PLAYING,
        STOPPED
    }

    private static String musicName;
    private static Status status = Status.STOPPED;
    private static float progress;

    private static final Minecraft mc = Minecraft.getInstance();

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        // 绘制文字
        var guiGraphics = event.getGuiGraphics();

        switch (status) {
            case PREPARING -> renderPreparingInfo(guiGraphics);
            case PLAYING -> renderPlayingInfo(guiGraphics);
            case STOPPED -> {}
        }
    }

    private static void renderPreparingInfo(GuiGraphics guiGraphics) {
        Component text = Component.translatable("gui.loginmusic.info.prepare",
                musicName, String.format("%.1f", progress * 100));
        guiGraphics.drawString(mc.font, text, 20, 20, 0xffffff, false);
    }

    private static void renderPlayingInfo(GuiGraphics guiGraphics) {
        Component text = Component.translatable("gui.loginmusic.info.play", musicName);
        guiGraphics.drawString(mc.font, text, 20, 20, 0xffffff, false);
    }

    public static void prepareMusicInfo(MusicEntry musicEntry) {
        musicName = LoginMusic.removeExtension(musicEntry.getMusicName());
        status = Status.PREPARING;
    }

    public static void setProgress(float prepareProgress) { progress = prepareProgress; }

    public static void playMusicInfo() {
        progress = 0;
        status = Status.PLAYING;
    }

    public static void stopMusicInfo() { status = Status.STOPPED; }
}
