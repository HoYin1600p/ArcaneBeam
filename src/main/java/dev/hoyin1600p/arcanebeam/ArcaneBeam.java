package dev.hoyin1600p.arcanebeam;

import dev.hoyin1600p.arcanebeam.client.ArcaneBeamClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ArcaneBeam.MOD_ID)
public class ArcaneBeam {
    public static final String MOD_ID = "arcanebeam";

    public ArcaneBeam() {
        ArcaneBeamSounds.SOUND_EVENTS.register(FMLJavaModLoadingContext.get().getModEventBus());
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ArcaneBeamClient::init);
        MinecraftForge.EVENT_BUS.register(this);
    }
}
