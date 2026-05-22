package dev.hoyin1600p.arcanebeam;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ArcaneBeamSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ArcaneBeam.MOD_ID);

    public static final RegistryObject<SoundEvent> ARCANE_1 = register("arcane_1");
    public static final RegistryObject<SoundEvent> ARCANE_2_STARTUP = register("arcane_2_startup");
    public static final RegistryObject<SoundEvent> ARCANE_2_LOOP = register("arcane_2_loop");
    public static final RegistryObject<SoundEvent> RAIL_1 = register("rail_1");
    public static final RegistryObject<SoundEvent> RAIL_2 = register("rail_2");

    private ArcaneBeamSounds() {
    }

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> new SoundEvent(new ResourceLocation(ArcaneBeam.MOD_ID, name)));
    }
}
