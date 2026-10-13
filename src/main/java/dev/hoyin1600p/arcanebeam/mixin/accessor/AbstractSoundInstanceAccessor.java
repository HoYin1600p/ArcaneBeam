package dev.hoyin1600p.arcanebeam.mixin.accessor;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractSoundInstance.class)
public interface AbstractSoundInstanceAccessor {
    @Accessor("pitch")
    float arcanebeam$getRawPitch();

    @Accessor("volume")
    float arcanebeam$getRawVolume();
}
