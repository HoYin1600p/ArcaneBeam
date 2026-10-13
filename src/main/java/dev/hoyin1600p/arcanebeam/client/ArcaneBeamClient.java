package dev.hoyin1600p.arcanebeam.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import dev.hoyin1600p.arcanebeam.beam.ArcaneBeamManager;
import dev.hoyin1600p.arcanebeam.beam.BeamKind;
import dev.hoyin1600p.arcanebeam.beam.profile.PlayerBeamAssignments;
import dev.hoyin1600p.arcanebeam.beam.profile.PlayerBeamProfileManager;
import dev.hoyin1600p.arcanebeam.config.ArcaneBeamConfig;
import dev.hoyin1600p.arcanebeam.gui.ArcaneBeamConfigScreen;
import dev.hoyin1600p.arcanebeam.labyrinth.LabyrinthKeys;
import dev.hoyin1600p.arcanebeam.map.DungeonGlobeMapRooms;
import dev.hoyin1600p.arcanebeam.sound.ArcaneBeamSoundController;
import dev.hoyin1600p.arcanebeam.update.UpdateNoticeService;
import dev.hoyin1600p.arcanebeam.util.Features;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.ConfigGuiHandler;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

import java.lang.ref.WeakReference;

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

    private static WeakReference<ClientLevel> trackedLevel = new WeakReference<>(null);
    private static boolean trackingLevel;

    private ArcaneBeamClient() {
    }

    public static void init() {
        ArcaneBeamConfig.load();
        UpdateNoticeService.initialize(
                ArcaneBeam.MOD_ID,
                "Arcane Beam",
                UPDATE_MANIFEST_URL,
                CURSEFORGE_URL,
                ArcaneBeamConfig.updateChecksEnabled(),
                ArcaneBeamConfig.updateNoticeFilter()
        );
        PlayerBeamAssignments.load();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ArcaneBeamClient::registerReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(ArcaneBeamClientCommands::register);
        ClientRegistry.registerKeyBinding(CONFIG_KEY);
        ClientRegistry.registerKeyBinding(LabyrinthKeys.SHOW_LINES);
        ArcaneBeamConfig.installShutdownFlush();
        GuardedEvents.register();
        // The Mods list "Config" button opens the same screen as the key, and closing it returns to the list.
        ModLoadingContext.get().registerExtensionPoint(ConfigGuiHandler.ConfigGuiFactory.class,
                () -> new ConfigGuiHandler.ConfigGuiFactory((minecraft, parent) -> new ArcaneBeamConfigScreen(parent)));
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
        // Each step is isolated so a failure in one feature cannot stop the others or crash the tick.
        if (!Features.CLIENT.isDisabled()) {
            try {
                watchLevelIdentity(minecraft);
            } catch (RuntimeException | LinkageError failure) {
                Features.CLIENT.fail(failure);
            }
        }
        if (!Features.SOUNDS.isDisabled()) {
            try {
                ArcaneBeamSoundController.tick(minecraft,
                        () -> ArcaneBeamManager.getLocalActiveBeam(BeamKind.ARCANE),
                        () -> ArcaneBeamManager.getLocalActiveBeam(BeamKind.RAIL));
            } catch (RuntimeException | LinkageError failure) {
                Features.SOUNDS.fail(failure);
            }
        }
        if (!Features.GLOBE_MAP.isDisabled()) {
            try {
                DungeonGlobeMapRooms.tick(minecraft);
            } catch (RuntimeException | LinkageError failure) {
                Features.GLOBE_MAP.fail(failure);
            }
        }
        try {
            ArcaneBeamConfig.tickPendingSave();
        } catch (RuntimeException failure) {
            ArcaneBeam.LOGGER.warn("Arcane Beam could not start a pending config save", failure);
        }
        try {
            while (CONFIG_KEY.consumeClick()) {
                if (minecraft.screen == null) {
                    minecraft.setScreen(new ArcaneBeamConfigScreen());
                }
            }
        } catch (RuntimeException failure) {
            ArcaneBeam.LOGGER.warn("Arcane Beam could not open its config screen", failure);
        }
    }

    /**
     * Dimension and server changes swap the ClientLevel instance without necessarily passing through a
     * null level on a tick we observe, so per-level effect state is cleared whenever the instance changes.
     */
    private static void watchLevelIdentity(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == trackedLevel.get() && (level != null || !trackingLevel)) {
            return;
        }
        // The weak reference can clear once the old level is dropped, so a separate flag records
        // that a level was seen; only the very first level of the session skips the clear.
        boolean hadLevel = trackingLevel;
        trackedLevel = new WeakReference<>(level);
        trackingLevel = level != null;
        if (!hadLevel) {
            return;
        }
        ClientEffectResets.runAll();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderGameOverlayPre(RenderGameOverlayEvent.Pre event) {
        if (Features.GUI.isDisabled()) {
            return;
        }
        try {
            if (event.getType() == RenderGameOverlayEvent.ElementType.ALL && ArcaneBeamConfigScreen.shouldSuppressGameHud()) {
                event.setCanceled(true);
            }
        } catch (RuntimeException | LinkageError failure) {
            Features.GUI.fail(failure);
        }
    }
}
