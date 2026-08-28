package dev.hoyin1600p.arcanebeam.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import dev.hoyin1600p.arcanebeam.client.update.UpdateNoticeService;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = ArcaneBeam.MOD_ID, value = Dist.CLIENT)
public final class ArcaneBeamClient {
    private static final String UPDATE_MANIFEST_URL =
            "https://raw.githubusercontent.com/HoYin1600p/ArcaneBeam/main/update.json";
    private static final String CURSEFORGE_URL =
            "https://www.curseforge.com/minecraft/mc-mods/arcanebeam";
    private static final KeyMapping CONFIG_KEY = new KeyMapping(
            "key.arcanebeam.config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.arcanebeam"
    );

    private ArcaneBeamClient() {
    }

    public static void init() {
        ArcaneBeamConfig.load();
        UpdateNoticeService.initialize(
                ArcaneBeam.MOD_ID,
                "Arcane Beam",
                UPDATE_MANIFEST_URL,
                CURSEFORGE_URL,
                ArcaneBeamConfig.updateChecksEnabled()
        );
        PlayerBeamAssignments.load();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ArcaneBeamClient::registerReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(ArcaneBeamClientCommands::register);
        ClientRegistry.registerKeyBinding(CONFIG_KEY);
    }

    private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new PlayerBeamProfileManager());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ArcaneBeamSoundController.tick(minecraft);
        while (CONFIG_KEY.consumeClick()) {
            if (minecraft.screen == null) {
                minecraft.setScreen(new ArcaneBeamConfigScreen());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderGameOverlayPre(RenderGameOverlayEvent.Pre event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL && ArcaneBeamConfigScreen.shouldSuppressGameHud()) {
            event.setCanceled(true);
        }
    }
}
