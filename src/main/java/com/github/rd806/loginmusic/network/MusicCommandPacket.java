package com.github.rd806.loginmusic.network;

import com.github.rd806.loginmusic.LoginMusic;
import com.github.rd806.loginmusic.command.CommandType;
import com.github.rd806.loginmusic.media.audio.SimpleMusicPlayer;
import com.github.rd806.loginmusic.media.music.MusicCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class MusicCommandPacket implements CustomPacketPayload {

    // 定义包类型
    public static final Type<MusicCommandPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LoginMusic.MODID, "command"));

    private final CommandType type;

    // 构造器：播放音乐
    public MusicCommandPacket(CommandType type) {
        this.type = type;
    }

    // 自定义编解码器（因为需要处理两种不同的数据类型）
    public static final StreamCodec<FriendlyByteBuf, MusicCommandPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, MusicCommandPacket packet) {
            buf.writeEnum(packet.type);
        }

        @Override
        public @NotNull MusicCommandPacket decode(FriendlyByteBuf buf) {
            // 读取音乐ID
            CommandType type = buf.readEnum(CommandType.class);
            return new MusicCommandPacket(type);
        }
    };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }

    // 处理数据包
    public static void handle(final MusicCommandPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            // 只在客户端执行
            if (Dist.CLIENT.isClient()) {
                switch(packet.type) {
                    case CACHE_LIST -> MusicCache.showCache();
                    case CACHE_CLEAR -> MusicCache.clearCache();
                    case STOP_MUSIC -> SimpleMusicPlayer.stopCurrentMusic();
                }
            }
        });
    }
}