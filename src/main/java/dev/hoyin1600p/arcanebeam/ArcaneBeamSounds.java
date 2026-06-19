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
    public static final RegistryObject<SoundEvent> LIGHTNING_SEISMIC_CHARGE_CAST = register("lightning_seismic_charge_cast");
    public static final RegistryObject<SoundEvent> LIGHTNING_SEISMIC_CHARGE_IMPACT = register("lightning_seismic_charge_impact");
    public static final RegistryObject<SoundEvent> LIGHTNING_RESOURCEPACK_1_CAST = register("lightning_resourcepack_1_cast");
    public static final RegistryObject<SoundEvent> LIGHTNING_RESOURCEPACK_1_IMPACT = register("lightning_resourcepack_1_impact");
    public static final RegistryObject<SoundEvent> LIGHTNING_RESOURCEPACK_2_CAST = register("lightning_resourcepack_2_cast");
    public static final RegistryObject<SoundEvent> LIGHTNING_RESOURCEPACK_2_IMPACT = register("lightning_resourcepack_2_impact");
    public static final RegistryObject<SoundEvent> VAULT_ALTAR_BEAM = register("vault_altar_beam");
    public static final RegistryObject<SoundEvent> VAULT_ALTAR_RESOURCEPACK_1 = register("vault_altar_resourcepack_1");
    public static final RegistryObject<SoundEvent> VAULT_ALTAR_RESOURCEPACK_2 = register("vault_altar_resourcepack_2");
    public static final RegistryObject<SoundEvent> STORM_ARROW_1 = register("storm_arrow_1");
    public static final RegistryObject<SoundEvent> STORM_ARROW_PROJECTILE = register("storm_arrow_projectile");
    public static final RegistryObject<SoundEvent> STORM_ARROW_RESOURCEPACK_1 = register("storm_arrow_resourcepack_1");
    public static final RegistryObject<SoundEvent> STORM_ARROW_RESOURCEPACK_2 = register("storm_arrow_resourcepack_2");
    public static final RegistryObject<SoundEvent> STORM_ARROW_PROJECTILE_RESOURCEPACK_1 = register("storm_arrow_projectile_resourcepack_1");
    public static final RegistryObject<SoundEvent> STORM_ARROW_PROJECTILE_RESOURCEPACK_2 = register("storm_arrow_projectile_resourcepack_2");
    public static final RegistryObject<SoundEvent> SMITE_1 = register("smite_1");
    public static final RegistryObject<SoundEvent> SMITE_ACTIVATION = register("smite_activation");
    public static final RegistryObject<SoundEvent> SMITE_RESOURCEPACK_1 = register("smite_resourcepack_1");
    public static final RegistryObject<SoundEvent> SMITE_RESOURCEPACK_2 = register("smite_resourcepack_2");
    public static final RegistryObject<SoundEvent> SMITE_ACTIVATION_RESOURCEPACK_1 = register("smite_activation_resourcepack_1");
    public static final RegistryObject<SoundEvent> SMITE_ACTIVATION_RESOURCEPACK_2 = register("smite_activation_resourcepack_2");

    private ArcaneBeamSounds() {
    }

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> new SoundEvent(new ResourceLocation(ArcaneBeam.MOD_ID, name)));
    }
}
