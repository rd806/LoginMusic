package com.github.rd806.loginmusic.compat.cloth;

import com.github.rd806.loginmusic.event.SelectionKey;
import com.github.rd806.loginmusic.config.ClientConfig;
import com.github.rd806.loginmusic.config.ServerConfig;
import com.github.rd806.loginmusic.gui.LyricLayer;
import com.github.rd806.loginmusic.media.music.MusicConfig;
import com.github.rd806.loginmusic.media.music.MusicEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.SubCategoryListEntry;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ClothConfigGUI {

    public static ConfigBuilder buildScreen() {

        ConfigBuilder builder = ConfigBuilder.create().setTitle(Component.translatable("cloth.loginmusic.title"));
        builder.setGlobalized(true);
        builder.setGlobalizedExpanded(false);
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        // 音乐条目
        ConfigCategory musicEntries = builder.getOrCreateCategory(Component.translatable("cloth.loginmusic.entries"));
        buildMusicEntries(entryBuilder, musicEntries);
        // 音乐播放设置
        ConfigCategory clientSettings = builder.getOrCreateCategory(Component.translatable("cloth.loginmusic.client"));
        buildClientSettings(entryBuilder, clientSettings);
        // 服务端设置
        ConfigCategory serverSettings = builder.getOrCreateCategory(Component.translatable("cloth.loginmusic.server"));
        buildServerSettings(entryBuilder, serverSettings);
        // 保存回调
        builder.setSavingRunnable(() -> {
            // 保存并触发 MusicConfig 重载
            MusicConfig.saveToFile();
            LyricLayer.setLyricLayer(ClientConfig.LYRIC_POS.get(), ClientConfig.LYRIC_COLOR.get());
        });

        return builder;
    }

    // 音乐条目配置
    private static void buildMusicEntries(ConfigEntryBuilder entryBuilder, ConfigCategory musicEntries) {
        // 使用临时列表来存储 GUI 中的顺序
        List<MusicEntry> currentList = MusicConfig.getMusicList();
        // 为每个音乐条目创建编辑界面
        for (int index = 0; index < currentList.size(); index++) {
            MusicEntry entry = currentList.get(index);
            SubCategoryListEntry subEntry = createMusicSubCategory(entryBuilder, entry, index);
            musicEntries.addEntry(subEntry);
        }
        // 添加新音乐的按钮
        musicEntries.addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("cloth.loginmusic.addEnable"), false)
                        .setTooltip(Component.translatable("cloth.loginmusic.addEnable.tooltip"))
                        .setSaveConsumer(shouldAdd -> {
                            if (shouldAdd) {
                                MusicEntry newEntry = new MusicEntry();
                                newEntry.setId("NewMusic" + (MusicConfig.getMusicList().size() + 1));
                                newEntry.setMusicName("");
                                newEntry.setMusicPath("");
                                newEntry.setLyricName("");
                                newEntry.setLyricPath("");
                                currentList.add(newEntry);
                                MusicConfig.setMusicList(currentList);
                            }
                        })
                        .build());
    }

    private static SubCategoryListEntry createMusicSubCategory(ConfigEntryBuilder entryBuilder, MusicEntry entry, int index) {
        // 创建子分类构建器
        var subCategoryBuilder = entryBuilder.startSubCategory(Component.literal(" " + entry.getId()));
        // 添加 ID 字段
        subCategoryBuilder.add(
                entryBuilder.startStrField(Component.literal("ID"), entry.getId())
                        .setDefaultValue("" + index)
                        .setTooltip(Component.translatable("cloth.loginmusic.id.tooltip"))
                        .setSaveConsumer(entry::setId)
                        .build());
        // 添加音乐文件字段
        subCategoryBuilder.add(
                entryBuilder.startStrField(Component.translatable("cloth.loginmusic.music"), entry.getMusicName())
                        .setDefaultValue("")
                        .setTooltip(Component.translatable("cloth.loginmusic.music.tooltip"))
                        .setSaveConsumer(entry::setMusicName)
                        .build());
        // 添加音乐 URL 字段
        subCategoryBuilder.add(
                entryBuilder.startStrField(Component.translatable("cloth.loginmusic.musicUrl"), entry.getMusicPath())
                        .setDefaultValue("")
                        .setTooltip(Component.translatable("cloth.loginmusic.musicUrl.tooltip"))
                        .setSaveConsumer(entry::setMusicPath)
                        .build());
        // 本地歌词文件字段
        subCategoryBuilder.add(
                entryBuilder.startStrField(Component.translatable("cloth.loginmusic.lyric"), entry.getLyricName())
                        .setDefaultValue("")
                        .setTooltip(Component.translatable("cloth.loginmusic.lyric.tooltip"))
                        .setSaveConsumer(entry::setLyricName)
                        .build());
        // 歌词 URL 字段
        subCategoryBuilder.add(
                entryBuilder.startStrField(Component.translatable("cloth.loginmusic.lyricUrl"), entry.getLyricPath())
                        .setDefaultValue("")
                        .setTooltip(Component.translatable("cloth.loginmusic.lyricUrl.tooltip"))
                        .setSaveConsumer(entry::setLyricPath)
                        .build()
        );
        // 删除按钮
        subCategoryBuilder.add(
                entryBuilder.startBooleanToggle(Component.translatable("cloth.loginmusic.delete"), false)
                        .setSaveConsumer(shouldDelete -> {
                            if (shouldDelete) { MusicConfig.getMusicList().remove(index); }
                        })
                        .build()
        );

        return subCategoryBuilder.build();
    }

    // 客户端配置
    private static void buildClientSettings(ConfigEntryBuilder entryBuilder, ConfigCategory clientSettings) {
        // 显示加载界面
        clientSettings.addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("config.loginmusic.loading.show"), ClientConfig.SHOW_LOADING.get())
                        .setDefaultValue(true)
                        .setTooltip(Component.translatable("config.loginmusic.loading.show.tooltip"))
                        .setSaveConsumer(showLoading -> ClientConfig.SHOW_LOADING.set(showLoading))
                        .build());
        // 音乐播放范围
        clientSettings.addEntry(
                entryBuilder.startIntSlider(Component.translatable("config.loginmusic.music.play_range"), ClientConfig.MUSIC_PLAY_RANGE.get(), 0, 100)
                        .setDefaultValue(3)
                        .setTooltip(Component.translatable("config.loginmusic.music.play_range.tooltip"))
                        .setSaveConsumer(range -> ClientConfig.MUSIC_PLAY_RANGE.set(range))
                        .build());
        // 是否允许播放其他玩家的音乐
        clientSettings.addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("config.loginmusic.music.allow_others"), ClientConfig.ALLOW_OTHERS_MUSIC.get())
                        .setDefaultValue(false)
                        .setTooltip(Component.translatable("config.loginmusic.music.allow_others.tooltip"))
                        .setSaveConsumer(allowOthersMusic -> ClientConfig.ALLOW_OTHERS_MUSIC.set(allowOthersMusic))
                        .build());
        // 播放入场音乐时是否静音原版背景音乐
        clientSettings.addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("config.loginmusic.music.mute_vanilla"), ClientConfig.MUTE_VANILLA_MUSIC.get())
                        .setDefaultValue(true)
                        .setTooltip(Component.translatable("config.loginmusic.music.mute_vanilla.tooltip"))
                        .setSaveConsumer(muteVanilla -> ClientConfig.MUTE_VANILLA_MUSIC.set(muteVanilla))
                        .build());
        // 歌曲缓存大小
        clientSettings.addEntry(
                entryBuilder.startIntSlider(Component.translatable("config.loginmusic.music.cache_size"), ClientConfig.CACHE_SIZE.get(), 0, 10)
                        .setDefaultValue(5)
                        .setTooltip(Component.translatable("config.loginmusic.music.cache_size.tooltip"))
                        .setSaveConsumer(cacheSize -> ClientConfig.CACHE_SIZE.set(cacheSize))
                        .build());
        // 是否展示歌词
        clientSettings.addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("config.loginmusic.lyric.allow"), ClientConfig.ALLOW_LYRICS.get())
                        .setDefaultValue(true)
                        .setTooltip(Component.translatable("config.loginmusic.lyric.allow.tooltip"))
                        .setSaveConsumer(allowLyrics -> ClientConfig.ALLOW_LYRICS.set(allowLyrics))
                        .build());
        // 歌词位置设置
        clientSettings.addEntry(
                entryBuilder.startEnumSelector(Component.translatable("config.loginmusic.lyric.pos"), ClientConfig.Position.class, ClientConfig.LYRIC_POS.get())
                        .setDefaultValue(ClientConfig.Position.DOWN)
                        .setTooltip(Component.translatable("config.loginmusic.lyric.pos.tooltip"))
                        .setSaveConsumer(lyricPos -> ClientConfig.LYRIC_POS.set(lyricPos))
                        .build());
        // 歌词颜色设置
        clientSettings.addEntry(
                entryBuilder.startColorField(Component.translatable("config.loginmusic.lyric.color"), ClientConfig.LYRIC_COLOR.get())
                        .setDefaultValue(0xFFFFFF)
                        .setTooltip(Component.translatable("config.loginmusic.lyric.color.tooltip"))
                        .setSaveConsumer(lyricColor -> ClientConfig.LYRIC_COLOR.set(lyricColor))
                        .build());
    }

    private static void buildServerSettings(ConfigEntryBuilder entryBuilder, ConfigCategory serverSettings) {
        serverSettings.addEntry(
                entryBuilder.startEnumSelector(Component.translatable("config.loginmusic.music.id"), SelectionKey.class, ServerConfig.MUSIC_ID_TYPE.get())
                        .setDefaultValue(SelectionKey.NAME)
                        .setTooltip(Component.translatable("config.loginmusic.music.id.tooltip"))
                        .setSaveConsumer(selectionKey -> ServerConfig.MUSIC_ID_TYPE.set(selectionKey))
                        .build());
    }
}
