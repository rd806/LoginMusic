package com.github.rd806.loginmusic.media.audio;

import com.github.rd806.loginmusic.LoginMusic;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 交给 Minecraft 内置音频系统播放的音乐实例。
 * <p>
 * 声音资源本身并不存在于资源包中，{@link #resolve(SoundManager)} 和
 * {@link #getStream(SoundBufferLibrary, Sound, boolean)} 分别绕过资源包查找与
 * Ogg 解码，直接把边下边播的 PCM 流喂给 SoundEngine。
 */
@OnlyIn(Dist.CLIENT)
public class LoginMusicSound extends AbstractSoundInstance {

    public static final ResourceLocation SOUND_LOCATION = ResourceLocation.fromNamespaceAndPath(LoginMusic.MODID, "login_music");

    // stream = true，SoundEngine 才会调用 getStream 而不是从资源包加载
    private static final Sound SOUND = new Sound(
            SOUND_LOCATION.toString(),
            ConstantFloat.of(1.0F),
            ConstantFloat.of(1.0F),
            1,
            Sound.Type.FILE,
            true,
            false,
            16
    );
    private static final WeighedSoundEvents SOUND_EVENTS = createSoundEvents();

    private final StreamingAudioStream stream;
    private final AtomicBoolean streamRequested = new AtomicBoolean();

    public LoginMusicSound(StreamingAudioStream stream) {
        super(SOUND_LOCATION, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
        this.stream = stream;
        // 提前设置，避免在 resolve 之前被其他模组读取音量时出现空指针
        this.sound = SOUND;
        this.volume = 1.0F;
        this.pitch = 1.0F;
        // 背景音乐不参与 3D 定位
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.looping = false;
    }

    private static WeighedSoundEvents createSoundEvents() {
        WeighedSoundEvents events = new WeighedSoundEvents(SOUND_LOCATION, null);
        events.addSound(SOUND);
        return events;
    }

    @Override
    public @NotNull WeighedSoundEvents resolve(@NotNull SoundManager manager) {
        return SOUND_EVENTS;
    }

    @Override
    public @NotNull CompletableFuture<AudioStream> getStream(@NotNull SoundBufferLibrary soundBuffers, @NotNull Sound sound, boolean looping) {
        if (streamRequested.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(stream);
        }
        return CompletableFuture.failedFuture(new IllegalStateException("Audio stream has already been requested"));
    }

    // 停止播放时释放边下边播线程
    public void releaseStream() {
        stream.close();
    }

    // 流在播放过程中发生的错误
    public Throwable getStreamError() {
        return stream.getError();
    }
}
