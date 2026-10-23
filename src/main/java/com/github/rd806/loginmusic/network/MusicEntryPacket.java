package com.github.rd806.loginmusic.network;

import com.github.rd806.loginmusic.LoginMusic;
import com.github.rd806.loginmusic.event.SelectionKey;
import com.github.rd806.loginmusic.event.ClientEvent;
import com.github.rd806.loginmusic.media.music.MusicEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class MusicEntryPacket implements CustomPacketPayload {

    // 定义包类型
    public static final CustomPacketPayload.Type<MusicEntryPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(LoginMusic.MODID, "music"));

    // 音乐播放信息
    private final MusicEntry music;
    private final BlockPos pos;
    private final SelectionKey key;


    // 构造器：播放音乐
    public MusicEntryPacket(MusicEntry music, BlockPos pos, SelectionKey key) {
        this.music = music;
        this.pos = pos;
        this.key = key;
    }

    // 自定义编解码器（因为需要处理两种不同的数据类型）
    public static final StreamCodec<FriendlyByteBuf, MusicEntryPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, MusicEntryPacket packet) {
            // 写入音乐
            buf.writeUtf(packet.music.getId());
            buf.writeUtf(packet.music.getMusicName());
            buf.writeUtf(packet.music.getMusicPath());
            buf.writeUtf(packet.music.getLyricName());
            buf.writeUtf(packet.music.getLyricPath());
            // 写入位置
            buf.writeBlockPos(packet.pos);
            buf.writeEnum(packet.key);
        }

        @Override
        public @NotNull MusicEntryPacket decode(FriendlyByteBuf buf) {
            // 读取音乐ID
            String musicId = buf.readUtf();
            String musicName = buf.readUtf();
            String musicPath = buf.readUtf();
            String lyricName = buf.readUtf();
            String lyricPath = buf.readUtf();
            BlockPos pos = buf.readBlockPos();
            SelectionKey key = buf.readEnum(SelectionKey.class);
            MusicEntry music = new MusicEntry(musicId, musicName, musicPath, lyricName, lyricPath);
            return new MusicEntryPacket(music, pos, key);
        }
    };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }

    // 处理数据包
    public static void handle(final MusicEntryPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            // 播放音乐
            LoginMusic.LOGGER.info("Prepare music: {}", packet.music.getMusicName());
            // 只在客户端执行
            if (Dist.CLIENT.isClient()) {
                ClientEvent.playLoginMusic(packet.music, packet.pos, packet.key);
            }
        });
    }
}