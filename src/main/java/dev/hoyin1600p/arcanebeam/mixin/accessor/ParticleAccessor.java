package dev.hoyin1600p.arcanebeam.mixin.accessor;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
    @Accessor("roll")
    float arcanebeam$getRoll();

    @Accessor("roll")
    void arcanebeam$setRoll(float roll);

    @Accessor("oRoll")
    void arcanebeam$setORoll(float roll);

    @Accessor("x")
    double arcanebeam$getX();

    @Accessor("y")
    double arcanebeam$getY();

    @Accessor("z")
    double arcanebeam$getZ();
}
