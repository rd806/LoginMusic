package com.github.rd806.loginmusic.media.music;

import com.github.rd806.loginmusic.config.ClientConfig;

import java.util.LinkedHashMap;
import java.util.Map;

public class MusicLRUCache<K, V> extends LinkedHashMap<K, V> {

    private static int MAX_CAPACITY = 5;

    public MusicLRUCache() {
        super(16, 0.75f, true);
        MAX_CAPACITY = ClientConfig.CACHE_SIZE.get();
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > MAX_CAPACITY;
    }
}
