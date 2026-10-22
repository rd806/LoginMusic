package com.github.rd806.loginmusic.network;

import com.github.rd806.loginmusic.LoginMusic;
import com.github.rd806.loginmusic.event.SelectionKey;
import com.github.rd806.loginmusic.command.CommandType;
import com.github.rd806.loginmusic.media.music.MusicEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

// 网络包类
public class NetworkConfig {

    private static final String PROTOCOL_VERSION = "2.0.0";
    private static SimpleChannel CHANNEL;
    private static int packetID = 1;

    // 注册网络音乐数据包
    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                ResourceLocation.tryBuild(LoginMusic.MODID, "main"),
                () -> PROTOCOL_VERSION,
                clientVersion -> true,
                PROTOCOL_VERSION::equals
        );

        CHANNEL.registerMessage(
                packetID++,
                MusicEntryPacket.class, MusicEntryPacket::encode, MusicEntryPacket::decode, MusicEntryPacket::handle);

        CHANNEL.registerMessage(
                packetID++,
                MusicCommandPacket.class, MusicCommandPacket::encode, MusicCommandPacket::decode, MusicCommandPacket::handle);

        LoginMusic.LOGGER.info("Network config registered!");
    }

    // 发送音乐给特定玩家
    public static void sendMusicToPlayer(ServerPlayer player, BlockPos pos, MusicEntry music, SelectionKey key) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MusicEntryPacket(music, pos, key));
    }

    // 查看本地缓存
    public static void sendClientCommand(ServerPlayer player, CommandType type) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MusicCommandPacket(type));
    }
}
