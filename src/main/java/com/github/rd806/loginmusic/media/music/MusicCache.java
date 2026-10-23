package com.github.rd806.loginmusic.media.music;

import com.github.rd806.loginmusic.media.audio.PreparedAudio;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class MusicCache {

    // 缓存
    public static final MusicLRUCache<String, PreparedAudio> MUSIC_CACHE = new MusicLRUCache<>();
    public static final MusicLRUCache<String, String> LYRIC_CACHE = new MusicLRUCache<>();

    // 放入
    public static void putMusic(String key, PreparedAudio value) { MUSIC_CACHE.put(key, value); }
    public static void putLyric(String key, String value) { LYRIC_CACHE.put(key, value); }

    // 取出
    public static PreparedAudio getMusic(String key) { return MUSIC_CACHE.get(key); }
    public static String getLyric(String key) { return LYRIC_CACHE.get(key); }

    // 查看缓存条目
    public static void showCache() {
        Set<String> musicKeys = MUSIC_CACHE.keySet();
        Set<String> lyricKeys = LYRIC_CACHE.keySet();
        Player player = Minecraft.getInstance().player;
        if (player == null) { return; }
        // 显示缓存的音乐
        if (musicKeys.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.loginmusic.command.cache.music.empty").withStyle(ChatFormatting.GRAY),
                    false);
        } else {
            player.displayClientMessage(
                    Component.translatable("message.loginmusic.command.cache.music.info").withStyle(ChatFormatting.GREEN),
                    false);
            for (String key : musicKeys) {
                player.displayClientMessage(Component.literal("§a▍ §7" + key), false);
            }
        }
        // 显示缓存的歌词
        if (lyricKeys.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.loginmusic.command.cache.lyric.empty").withStyle(ChatFormatting.GRAY),
                    false);
        } else {
            player.displayClientMessage(
                    Component.translatable("message.loginmusic.command.cache.lyric.info").withStyle(ChatFormatting.GREEN),
                    false);
            for (String key : lyricKeys) {
                player.displayClientMessage(Component.literal("§a▍ §7" + key), false);
            }
        }
    }

    // 清除缓存
    public static void clearCache() {
        MUSIC_CACHE.clear();
        LYRIC_CACHE.clear();

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(
                    Component.translatable("message.loginmusic.command.cache.clear").withStyle(ChatFormatting.GREEN),
                    false);
        }
    }
}