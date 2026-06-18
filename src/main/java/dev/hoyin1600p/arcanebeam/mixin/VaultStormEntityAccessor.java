package dev.hoyin1600p.arcanebeam.mixin;

import iskallia.vault.entity.entity.VaultStormEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = VaultStormEntity.class, remap = false)
public interface VaultStormEntityAccessor {
    @Accessor("RADIUS")
    static EntityDataAccessor<Float> arcanebeam$getRadiusAccessor() {
        throw new AssertionError();
    }
}
