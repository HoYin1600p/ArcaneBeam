package dev.hoyin1600p.arcanebeam;

import dev.hoyin1600p.arcanebeam.ability.manastealer.ManaStealerVisualModule;
import dev.hoyin1600p.arcanebeam.client.ArcaneBeamClient;
import dev.hoyin1600p.arcanebeam.client.VaultMixinTargetCheck;
import dev.hoyin1600p.arcanebeam.sound.ArcaneBeamSoundController;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.eventbus.api.IEventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ArcaneBeam.MOD_ID)
public class ArcaneBeam {
    public static final String MOD_ID = "arcanebeam";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public ArcaneBeam() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ManaStealerVisualModule.register(modEventBus);
            ArcaneBeamSoundController.registerReloadHook(modEventBus);
            VaultMixinTargetCheck.register(modEventBus);
        }
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ArcaneBeamClient::init);
    }
}
