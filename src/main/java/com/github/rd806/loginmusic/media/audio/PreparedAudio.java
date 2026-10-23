package com.github.rd806.loginmusic.media.audio;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.sound.sampled.AudioFormat;

/**
 * 已经转换为 16 位 PCM 的音频数据，可直接交给 Minecraft 音频引擎播放。
 */
@OnlyIn(Dist.CLIENT)
public record PreparedAudio(byte[] data, AudioFormat format) {}
