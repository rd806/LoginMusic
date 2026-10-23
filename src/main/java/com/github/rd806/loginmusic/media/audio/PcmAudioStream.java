package com.github.rd806.loginmusic.media.audio;

import net.minecraft.client.sounds.AudioStream;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;

/**
 * 把已经解码好的 PCM 数据包装为 Minecraft 音频引擎能够读取的流。
 * 这样音乐就可以交给 SoundEngine（OpenAL）播放，而不是使用系统播放器。
 */
@OnlyIn(Dist.CLIENT)
public class PcmAudioStream implements AudioStream {

    private final byte[] data;
    private final AudioFormat format;
    private final int frameSize;
    private int position;

    public PcmAudioStream(byte[] data, AudioFormat format) {
        this.data = data;
        this.format = format;
        // OpenAL 要求按完整帧读取
        this.frameSize = Math.max(1, format.getFrameSize());
    }

    @Override
    public @NotNull AudioFormat getFormat() {
        return format;
    }

    @Override
    public ByteBuffer read(int size) {
        if (position >= data.length || size <= 0) {
            // 返回 null 表示流已结束，Channel 不会再向 OpenAL 提交新的缓冲区
            return null;
        }
        int length = Math.min(size, data.length - position);
        // 只读取完整帧，避免 OpenAL 解析出错
        int aligned = length - (length % frameSize);
        if (aligned <= 0) {
            return null;
        }
        ByteBuffer buffer = BufferUtils.createByteBuffer(aligned);
        buffer.put(data, position, aligned);
        position += aligned;
        buffer.flip();
        return buffer;
    }

    @Override
    public void close() {
        // PCM 数据存放在内存中，无需释放资源
    }
}
