package com.github.rd806.loginmusic.gui;

import com.github.rd806.loginmusic.LoginMusic;
import com.github.rd806.loginmusic.media.music.MusicEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;


public class LoadScreen extends Screen {

    public enum Status {
        DOWNLOAD,
        COMPLETED,
        ERROR
    }

    private static final int barWidth = 200;
    private static final int barHeight = 20;
    private static int barX;
    private static int barY;

    private final MusicEntry music;
    private final String musicName;
    private final Runnable onComplete;

    // 进度信息
    private volatile Status audioStatus;
    private volatile Status lyricStatus;
    private volatile Component audioStatusMessage = Component.translatable("gui.loginmusic.load.start");
    private volatile Component lyricStatusMessage = Component.translatable("gui.loginmusic.load.start");
    private volatile float audioProgress;
    private volatile float lyricProgress;

    private volatile String audioErrorMessage = "";
    private volatile String lyricErrorMessage = "";

    public LoadScreen(MusicEntry music, Runnable onComplete) {
        super(Component.translatable("gui.loginmusic.load.title"));
        this.music = music;
        this.musicName = LoginMusic.removeExtension(music.getMusicName());
        this.audioStatus = Status.DOWNLOAD;
        this.lyricStatus = Status.DOWNLOAD;
        this.onComplete = onComplete;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 渲染背景
        // renderBackground函数有变化
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        barX = centerX - barWidth / 2;
        barY = centerY + 20;
        // 标题
        graphics.drawCenteredString(this.font, "[LoginMusic]", centerX, centerY - 60, 0x00AAFF);
        // 音乐名称，放大两倍
        graphics.pose().pushPose();
        graphics.pose().scale(2, 2, 2);
        graphics.drawCenteredString(
                this.font, musicName,
                centerX / 2, (centerY - 30) / 2 , 0xFFFFFF);
        graphics.pose().popPose();
        // 底部提示
        graphics.drawCenteredString(
                this.font, Component.translatable("gui.loginmusic.load.tooltip"),
                centerX, this.height - 30,
                0x808080);
        // 设置渲染类型
        switch (audioStatus) {
            case COMPLETED -> renderAudioCompleted(graphics, centerX);
            case ERROR -> renderAudioError(graphics, centerX);
            case DOWNLOAD -> renderAudioDownload(graphics, centerX);
        }
        switch (lyricStatus) {
            case COMPLETED -> renderLyricCompleted(graphics, centerX);
            case ERROR -> renderLyricError(graphics, centerX);
            case DOWNLOAD -> renderLyricDownload(graphics, centerX);
        }
        // 自动关闭
        if (audioStatus.equals(Status.COMPLETED) && lyricStatus.equals(Status.COMPLETED)) {
            onComplete.run();
        }
    }

    // 正在下载
    private void renderAudioDownload(GuiGraphics graphics, int centerX) {
        // 背景
        graphics.fill(
                barX, barY,
                barX + barWidth, barY + barHeight, 0xFF333333);
        // 进度条
        int audioWidth = (int) (barWidth * audioProgress);
        graphics.fill(
                barX, barY,
                barX + audioWidth, barY + barHeight,
                0xFF00AA00);
        // 状态文字
        graphics.drawCenteredString(this.font, audioStatusMessage, centerX, barY + 5, 0xFFFFFF);
    }
    private void renderLyricDownload(GuiGraphics graphics, int centerX) {
        // 背景
        graphics.fill(
                barX, barY + barHeight + 5,
                barX + barWidth, barY +  2* barHeight + 5, 0xFF333333);
        // 进度条
        int lyricWidth = (int) (barWidth * lyricProgress);
        graphics.fill(
                barX, barY + barHeight + 5,
                barX + lyricWidth, barY + 2 * barHeight + 5,
                0xFF00AA00);
        // 状态文字
        graphics.drawCenteredString(this.font, lyricStatusMessage, centerX, barY + barHeight + 10, 0xFFFFFF);
    }

    // 下载完成
    private void renderAudioCompleted(GuiGraphics graphics, int centerX) {
        graphics.drawCenteredString(
                this.font,
                Component.translatable("gui.loginmusic.load.complete", musicName),
                centerX, barY + 5, 0x00FF00);
    }
    private void renderLyricCompleted(GuiGraphics graphics, int centerX) {
        graphics.drawCenteredString(
                this.font,
                Component.translatable("gui.loginmusic.load.complete", music.getLyricName()),
                centerX, barY + barHeight + 10, 0x00FF00);
    }

    // 下载失败
    private void renderAudioError(GuiGraphics graphics, int centerX) {
        graphics.drawCenteredString(
                this.font,
                Component.literal(I18n.get("gui.loginmusic.load.fail") + audioErrorMessage),
                centerX, barY + 5, 0xFF0000);
    }
    private void renderLyricError(GuiGraphics graphics, int centerX) {
        graphics.drawCenteredString(
                this.font,
                Component.literal(I18n.get("gui.loginmusic.load.fail") + lyricErrorMessage),
                centerX, barY + barHeight + 10, 0xFF0000);
    }

    // 线程安全的更新
    // 计算progress，保证在[0.0f, 1.0f]，max和min别写反了
    public synchronized void updateAudioProgress(float progress, Component status) {
        this.audioProgress = Math.clamp(progress, 0.0f, 1.0f);
        this.audioStatusMessage = status;
    }
    public synchronized void updateLyricProgress(float progress, Component status) {
        this.lyricProgress = Math.clamp(progress, 0.0f, 1.0f);
        this.lyricStatusMessage = status;
    }

    public void setAudioCompleted() { this.audioStatus = Status.COMPLETED; }

    public void setLyricCompleted() { this.lyricStatus = Status.COMPLETED; }

    public void setAudioError(String message) {
        this.audioStatus = Status.ERROR;
        this.audioErrorMessage = message;
    }

    public void setLyricError(String message) {
        this.lyricStatus = Status.ERROR;
        this.lyricErrorMessage = message;
    }

    // 打开此界面时游戏不暂停
    @Override
    public boolean isPauseScreen() { return false; }

    // 是否允许ESC关闭
    @Override
    public boolean shouldCloseOnEsc() { return true; }
}
