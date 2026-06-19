package dev.hoyin1600p.arcanebeam.mixin;

import iskallia.vault.skill.ability.effect.spi.AbstractSmiteAbility;
import net.minecraft.network.syncher.EntityDataAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractSmiteAbility.SmiteBolt.class, remap = false)
public interface SmiteBoltAccessor {
    @Accessor("COLOR")
    static EntityDataAccessor<Integer> arcanebeam$getColorAccessor() {
        throw new AssertionError();
    }
}
