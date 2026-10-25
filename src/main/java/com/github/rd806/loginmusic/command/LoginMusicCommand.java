package com.github.rd806.loginmusic.command;

import com.github.rd806.loginmusic.LoginMusic;
import com.github.rd806.loginmusic.event.SelectionKey;
import com.github.rd806.loginmusic.config.ServerConfig;
import com.github.rd806.loginmusic.event.ServerEvent;
import com.github.rd806.loginmusic.network.NetworkConfig;
import com.mojang.brigadier.Command;
import com.github.rd806.loginmusic.config.MusicConfig;
import com.github.rd806.loginmusic.media.music.MusicEntry;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;

public class LoginMusicCommand {

    private static final String ROOT = "loginmusic";
    private static final String PLAY = "play";
    private static final String STOP = "stop";
    private static final String LIST = "list";
    private static final String CACHE = "cache";
    private static final String SHOW = "show";
    private static final String CLEAR = "clear";
    private static final String RELOAD = "reload";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(ROOT)
                .requires(source -> source.hasPermission(2));
        LiteralArgumentBuilder<CommandSourceStack> play = Commands.literal(PLAY);
        LiteralArgumentBuilder<CommandSourceStack> stop = Commands.literal(STOP);
        LiteralArgumentBuilder<CommandSourceStack> list = Commands.literal(LIST);
        LiteralArgumentBuilder<CommandSourceStack> cache = Commands.literal(CACHE);
        LiteralArgumentBuilder<CommandSourceStack> show = Commands.literal(SHOW);
        LiteralArgumentBuilder<CommandSourceStack> clear = Commands.literal(CLEAR);
        LiteralArgumentBuilder<CommandSourceStack> reload = Commands.literal(RELOAD);

        root.then(play.executes(LoginMusicCommand::playMusic));
        root.then(stop.executes(LoginMusicCommand::stopMusic));
        root.then(list.executes(LoginMusicCommand::showList));
        root.then(cache.then(show.executes(LoginMusicCommand::showCache)));
        root.then(cache.then(clear.executes(LoginMusicCommand::clearCache)));
        root.then(reload.executes(LoginMusicCommand::reloadConfig));
        return root;
    }

    // 播放音乐
    private static int playMusic(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer serverPlayer = context.getSource().getPlayer();
            if (serverPlayer != null) {
                SelectionKey key = ServerConfig.MUSIC_ID_TYPE.get();
                MusicEntry music = ServerEvent.chooseMusic(serverPlayer, key);
                ServerEvent.playMusic(music, serverPlayer, key);
            }
        } catch (Exception e) {
            LoginMusic.LOGGER.error("Fail to send music", e);
        }
        return Command.SINGLE_SUCCESS;
    }

    // 停止音乐
    private static int stopMusic(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer serverPlayer = context.getSource().getPlayer();
            if (serverPlayer != null) {
                NetworkConfig.sendClientCommand(serverPlayer, CommandType.STOP_MUSIC);
            }
        } catch (Exception e) {
            LoginMusic.LOGGER.error("Fail to stop music", e);
        }
        return Command.SINGLE_SUCCESS;
    }

    // 展示列表
    private static int showList(CommandContext<CommandSourceStack> context) {
        try {
            // 显示音乐主键
            SelectionKey key = ServerConfig.MUSIC_ID_TYPE.get();
            context.getSource().sendSuccess(
                    () -> Component.translatable("message.loginmusic.command.list.key", key),
                    false
            );
            // 默认音乐
            MusicEntry defaultMusic = MusicConfig.getDefaultMusic();
            context.getSource().sendSuccess(
                    () -> Component.literal("§a▍ §7Default: §r" + defaultMusic.getMusicName()),
                    false);
            // 音乐列表
            Map<String, MusicEntry> tempMap = MusicConfig.getMusicEntryMap();
            if (tempMap.isEmpty()) {
                context.getSource().sendFailure(Component.translatable("message.loginmusic.command.list.empty"));
            } else {
                // 显示音乐配置信息
                context.getSource().sendSuccess(
                        () -> Component.translatable("message.loginmusic.command.list.success", String.valueOf(tempMap.size())),
                        false);
                for (Map.Entry<String, MusicEntry> entry : tempMap.entrySet()) {
                    String musicName = entry.getValue().getMusicName();
                    context.getSource().sendSuccess(
                            () -> Component.literal("§a▍ §7" + entry.getKey() + ": §r" + musicName),
                            false);
                }
            }
        } catch (Exception e) {
            LoginMusic.LOGGER.error(e.getMessage());
        }
        return Command.SINGLE_SUCCESS;
    }

    // 显示缓存
    private static int showCache(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer serverPlayer = context.getSource().getPlayer();
            if (serverPlayer != null) {
                NetworkConfig.sendClientCommand(serverPlayer, CommandType.CACHE_LIST);
            }
        } catch (Exception e) {
            LoginMusic.LOGGER.error(e.getMessage());
        }
        return Command.SINGLE_SUCCESS;
    }

    // 清理缓存
    private static int clearCache(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer serverPlayer = context.getSource().getPlayer();
            if (serverPlayer != null) {
                NetworkConfig.sendClientCommand(serverPlayer, CommandType.CACHE_CLEAR);
            }
        } catch (Exception e) {
            LoginMusic.LOGGER.error(e.getMessage());
        }
        return Command.SINGLE_SUCCESS;
    }

    // 重新加载
    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        try {
            MusicConfig.loadFromConfig();
            context.getSource().sendSuccess(
                    () -> Component.translatable("message.loginmusic.command.reload.success"),
                    true);
        } catch (Exception e) {
            LoginMusic.LOGGER.error("Fail to reload LoginMusic config: {}", e.getMessage());
        }
        return Command.SINGLE_SUCCESS;
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(get());
    }
}