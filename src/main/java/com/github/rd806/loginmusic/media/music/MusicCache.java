package com.github.rd806.loginmusic.media.music;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class MusicCache {

    // 缓存（音频缓存保存原始压缩字节，边下边播完成后写入）
    public static final MusicLRUCache<String, byte[]> MUSIC_CACHE = new MusicLRUCache<>();
    public static final MusicLRUCache<String, String> LYRIC_CACHE = new MusicLRUCache<>();

    // 放入
    public static void putMusic(String key, byte[] value) {
        synchronized (MUSIC_CACHE) {
            MUSIC_CACHE.put(key, value);
        }
    }

    public static void putLyric(String key, String value) {
        synchronized (LYRIC_CACHE) {
            LYRIC_CACHE.put(key, value);
        }
    }

    // 取出
    public static byte[] getMusic(String key) {
        synchronized (MUSIC_CACHE) {
            return MUSIC_CACHE.get(key);
        }
    }

    public static String getLyric(String key) {
        synchronized (LYRIC_CACHE) {
            return LYRIC_CACHE.get(key);
        }
    }

    // 查看缓存条目
    public static void showCache() {
        Set<String> musicKeys;
        Set<String> lyricKeys;
        synchronized (MUSIC_CACHE) {
            musicKeys = Set.copyOf(MUSIC_CACHE.keySet());
        }
        synchronized (LYRIC_CACHE) {
            lyricKeys = Set.copyOf(LYRIC_CACHE.keySet());
        }

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
        synchronized (MUSIC_CACHE) {
            MUSIC_CACHE.clear();
        }
        synchronized (LYRIC_CACHE) {
            LYRIC_CACHE.clear();
        }

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(
                    Component.translatable("message.loginmusic.command.cache.clear").withStyle(ChatFormatting.GREEN),
                    false);
        }
    }
}
