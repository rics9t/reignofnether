package com.solegendary.reignofnether.sounds;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class FadeableMusicInstance extends AbstractTickableSoundInstance {
    private boolean fadingOut = false;

    public FadeableMusicInstance(SoundEvent soundEvent) {
        super(soundEvent, SoundSource.MUSIC, RandomSource.create());
        this.volume = 1.0f;
        this.looping = true;
        this.delay = 0;
        this.x = 0;
        this.y = 0;
        this.z = 0;

        // THESE TWO LINES FIX THE IN-GAME MUSIC:
        // By default, AbstractTickableSoundInstance makes 3D positional sounds.
        // This tells Minecraft to play the music globally (2D) without distance fading.
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        if (fadingOut) {
            volume -= 0.01f;
            if (volume <= 0f) {
                volume = 0f;
                this.stop();
            }
        }
    }

    public void startFadeOut() {
        fadingOut = true;
    }
}
