package com.github.rd806.loginmusic.media.layer;

import com.github.rd806.loginmusic.config.ClientConfig.Position;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 显示歌词的专用区域
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class LyricLayer {

    private static final Minecraft mc = Minecraft.getInstance();
    private static String currentLyricText;
    private static int textColor;
    private static int yOffset;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (currentLyricText == null || mc.player == null) return;

        var guiGraphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        // 计算文本位置（居中）
        int x = screenWidth / 2;
        int y = screenHeight / 2 + (yOffset * screenHeight) / 4;
        // 绘制文本
        guiGraphics.drawCenteredString(mc.font, currentLyricText, x, y, textColor);
    }

    // 展示歌词
    public static void showLyric(String lyric) { currentLyricText = lyric; }

    // 设置歌词样式
    public static void setLyricLayer(Position pos, int hexColor) {
        textColor = hexColor;
        switch (pos) {
            case UP -> yOffset = -1;
            case MIDDLE -> yOffset = 0;
            case DOWN -> yOffset = 1;
        }
    }
}
