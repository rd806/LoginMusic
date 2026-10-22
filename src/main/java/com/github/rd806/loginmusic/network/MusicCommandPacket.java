package com.github.rd806.loginmusic.network;

import com.github.rd806.loginmusic.command.CommandType;
import com.github.rd806.loginmusic.media.audio.SimpleMusicPlayer;
import com.github.rd806.loginmusic.media.music.MusicCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MusicCommandPacket {

    private final CommandType type;

    public MusicCommandPacket(CommandType type) {
        this.type = type;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(type);
    }

    public static MusicCommandPacket decode(FriendlyByteBuf buf) {
        return new MusicCommandPacket(buf.readEnum(CommandType.class));
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            switch(type) {
                case CACHE_LIST -> DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> MusicCache::showCache);
                case CACHE_CLEAR -> DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> MusicCache::clearCache);
                case STOP_MUSIC -> DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> SimpleMusicPlayer::stopCurrentMusic);
            }
        });
        context.setPacketHandled(true);
    }
}
