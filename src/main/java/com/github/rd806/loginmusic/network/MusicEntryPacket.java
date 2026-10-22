package com.github.rd806.loginmusic.network;

import com.github.rd806.loginmusic.event.SelectionKey;
import com.github.rd806.loginmusic.event.ClientEvent;
import com.github.rd806.loginmusic.media.music.MusicEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MusicEntryPacket {

    private final MusicEntry music;
    private final BlockPos pos;
    private final SelectionKey key;

    public MusicEntryPacket(MusicEntry music, BlockPos pos, SelectionKey key) {
        this.music = music;
        this.pos = pos;
        this.key = key;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(music.getId());
        buf.writeUtf(music.getMusicName());
        buf.writeUtf(music.getMusicPath());
        buf.writeUtf(music.getLyricName());
        buf.writeUtf(music.getLyricPath());
        // 位置信息
        buf.writeLong(pos.asLong());
        // 关键字
        buf.writeEnum(key);
    }

    public static MusicEntryPacket decode(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        String musicName = buf.readUtf();
        String musicPath = buf.readUtf();
        String lyricName = buf.readUtf();
        String lyricPath = buf.readUtf();
        // 位置信息
        BlockPos pos = buf.readBlockPos();
        SelectionKey type = buf.readEnum(SelectionKey.class);
        MusicEntry music = new MusicEntry(id, musicName, musicPath, lyricName, lyricPath);
        return new MusicEntryPacket(music, pos, type);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.safeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientEvent.playLoginMusic(music, pos, key)));
        context.setPacketHandled(true);
    }

}
