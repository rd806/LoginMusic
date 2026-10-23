package com.github.rd806.loginmusic.event;

import com.github.rd806.loginmusic.LoginMusic;
import com.github.rd806.loginmusic.config.ServerConfig;
import com.github.rd806.loginmusic.config.MusicConfig;
import com.github.rd806.loginmusic.media.music.MusicEntry;
import com.github.rd806.loginmusic.network.NetworkConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Random;

/**
 * 玩家登录事件，播放音乐
 */
@EventBusSubscriber(modid = LoginMusic.MODID)
public class ServerEvent {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        // 服务端
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // 根据玩家名称获取音乐
            SelectionKey key = ServerConfig.MUSIC_ID_TYPE.get();
            MusicEntry entry = chooseMusic(serverPlayer, key);
            // 判空
            if (entry == null) {
                LoginMusic.LOGGER.error("Can't find correct music!");
                return;
            }
            playMusic(entry, serverPlayer, key);
        }
    }

    public static void playMusic(MusicEntry entry, ServerPlayer player, SelectionKey key) {
        MinecraftServer server = player.getServer();
        // 将音乐发送给全体玩家
        if (server != null) {
            PlayerList playerList = server.getPlayerList();
            for (ServerPlayer target : playerList.getPlayers()) {
                BlockPos pos = target.blockPosition();
                NetworkConfig.sendMusicToPlayer(target, pos, entry, key);
            }
        }
    }

    public static MusicEntry chooseMusic(ServerPlayer player, SelectionKey type) {
        MusicEntry result = null;
        switch (type) {
            case NAME -> result = chooseMusicByName(player);
            case UUID -> result = chooseMusicByUuid(player);
            case RANDOM -> result = chooseRandomMusic();
            case DEFAULT -> result = MusicConfig.getDefaultMusic();
        }
        if (result == null) {
            result = MusicConfig.getDefaultMusic();
        }
        return result;
    }

    // 通过name选择音乐
    private static MusicEntry chooseMusicByName(ServerPlayer player) {
        String playerName = player.getName().getString();
        return MusicConfig.getMusic(playerName);
    }

    // 通过uuid选择音乐
    private static MusicEntry chooseMusicByUuid(ServerPlayer player) {
        String playerUUIDString = player.getStringUUID();
        return MusicConfig.getMusic(playerUUIDString);
    }

    // 随机分配音乐
    private static MusicEntry chooseRandomMusic() {
        Random random = new Random();
        int index = random.nextInt(MusicConfig.getMusicList().size());
        return MusicConfig.getMusicList().get(index);
    }
}