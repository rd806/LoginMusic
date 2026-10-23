package com.github.rd806.loginmusic.network;

import com.github.rd806.loginmusic.LoginMusic;
import com.github.rd806.loginmusic.event.SelectionKey;
import com.github.rd806.loginmusic.command.CommandType;
import com.github.rd806.loginmusic.media.music.MusicEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 网络包类
 */
public class NetworkConfig {

    public static final ResourceLocation CHANNEL_ID =
            ResourceLocation.fromNamespaceAndPath(LoginMusic.MODID, "main");

    // 注册网络音乐数据包
    public static void register(final RegisterPayloadHandlersEvent event) {
        // 获取注册器
        final PayloadRegistrar registrar = event.registrar(CHANNEL_ID.getNamespace())
                .versioned("2.0.0")
                .optional();

        registrar.playToClient(MusicEntryPacket.TYPE, MusicEntryPacket.STREAM_CODEC, MusicEntryPacket::handle);
        registrar.playToClient(MusicCommandPacket.TYPE, MusicCommandPacket.STREAM_CODEC, MusicCommandPacket::handle);

        LoginMusic.LOGGER.info("Network config registered!");
    }

    // 发送音乐给特定玩家
    public static void  sendMusicToPlayer(ServerPlayer player, BlockPos pos, MusicEntry music, SelectionKey key) {
        // 发送数据包到客户端
        LoginMusic.LOGGER.info("Send music {} to {}", music.getMusicName(), player.getName().getString());
        PacketDistributor.sendToPlayer(player, new MusicEntryPacket(music, pos, key));
    }

    // 查看本地缓存
    public static void sendClientCommand(ServerPlayer player, CommandType type) {
        PacketDistributor.sendToPlayer(player, new MusicCommandPacket(type));
    }
}