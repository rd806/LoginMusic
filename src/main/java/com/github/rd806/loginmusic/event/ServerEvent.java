package com.github.rd806.loginmusic.event;

import com.github.rd806.loginmusic.LoginMusic;
import com.github.rd806.loginmusic.config.ServerConfig;
import com.github.rd806.loginmusic.media.music.MusicConfig;
import com.github.rd806.loginmusic.media.music.MusicEntry;
import com.github.rd806.loginmusic.network.NetworkConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = LoginMusic.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEvent {
    // 玩家登录事件
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // 根据选择的键获取音乐
            SelectionKey key = ServerConfig.MUSIC_ID_TYPE.get();
            MusicEntry music = chooseMusic(serverPlayer, key);
            // 判空
            if (music == null) {
                LoginMusic.LOGGER.error("Can't find correct music!");
                return;
            }
            playMusic(music, serverPlayer, key);
        } else {
            LoginMusic.LOGGER.error("Not a player {} !", event.getEntity().getName());
        }
    }

    public static void playMusic(MusicEntry music, ServerPlayer player, SelectionKey key) {
        MinecraftServer server = player.getServer();
        // 将音乐发送给全体玩家
        if (server != null) {
            PlayerList playerList = server.getPlayerList();
            for (ServerPlayer target : playerList.getPlayers()) {
                BlockPos pos = target.blockPosition();
                LoginMusic.LOGGER.info("Send music {} to {}", music.getMusicName(), player.getName().getString());
                NetworkConfig.sendMusicToPlayer(target, pos, music, key);
            }
        }
    }

    // 选择音乐
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
