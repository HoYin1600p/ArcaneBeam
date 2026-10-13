package dev.hoyin1600p.arcanebeam.beam;

import dev.hoyin1600p.arcanebeam.config.model.BeamSettings;
import iskallia.vault.network.message.AbilityActivityMessage;
import iskallia.vault.network.message.AbilityKnownOnesMessage;
import iskallia.vault.network.message.ServerboundAbilityKeyMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ArcaneBeamManager {
    private static final Map<UUID, ActiveBeam> activeBeams = new LinkedHashMap<>();

    private ArcaneBeamManager() {
    }

    public static boolean handleArcaneParticle(boolean rail, double x, double y, double z,
                                        double xSpeed, double ySpeed, double zSpeed) {
        return ArcaneParticleRouter.handleArcaneParticle(rail, x, y, z, xSpeed, ySpeed, zSpeed);
    }

    static boolean usesResourcePackParticles(BeamKind kind, BeamSettings settings) {
        return kind == BeamKind.ARCANE && settings != null && settings.enabled && settings.resourcePackParticles;
    }

    /**
     * Resource pack mode keeps Vault's own Arcane particles (so a pack retexture shows) but re-aims them.
     * Returns null when the particle should be left exactly where Vault put it.
     */
    public static Vec3 resourcePackParticlePosition(boolean rail, double x, double y, double z) {
        return ArcaneParticleRouter.resourcePackParticlePosition(rail, x, y, z);
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalAbilityState.ensureClientContext(minecraft);
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (minecraft.level == null) {
            clearAllState();
            return;
        }

        clearDisabledReplacementState();

        if (minecraft.player != null) {
            LocalAbilityState.tick(minecraft, minecraft.level.getGameTime());
        }
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        long gameTime = level.getGameTime();
        removeExpiredBeams(gameTime);
        if (!activeBeams.isEmpty()) {
            ArcaneBeamRenderer.render(event.getPoseStack(), event.getCamera().getPosition(), event.getPartialTick(), activeBeams.values());
        }

        BeamTrace previewTrace = tracePreviewBeam(event.getPartialTick());
        if (previewTrace != null) {
            ArcaneBeamRenderer.renderPreview(event.getPoseStack(), event.getCamera().getPosition(), previewTrace);
        }
    }

    private static void removeExpiredBeams(long gameTime) {
        Iterator<ActiveBeam> iterator = activeBeams.values().iterator();
        while (iterator.hasNext()) {
            ActiveBeam beam = iterator.next();
            if (gameTime - beam.lastSeenGameTime() > beam.expireAfterTicks()) {
                BeamTracer.forget(beam.casterId());
                iterator.remove();
            }
        }
    }

    public static void syncLocalAbilityState(AbilityKnownOnesMessage message) {
        LocalAbilityState.syncLocalAbilityState(message);
    }

    public static void observeLocalAbilityKey(ServerboundAbilityKeyMessage.Opcode opcode) {
        LocalAbilityState.observeLocalAbilityKey(opcode);
    }

    /** A quickselect press casts the chosen specialization directly; only that one's window is armed. */
    public static void observeLocalQuickselect(String abilityId) {
        LocalAbilityState.observeLocalQuickselect(abilityId);
    }

    public static void syncLocalAbilityActivity(AbilityActivityMessage message) {
        LocalAbilityState.syncLocalAbilityActivity(message);
    }

    public static boolean handleArcaneCastSound(SoundInstance sound) {
        return LocalAbilityState.handleArcaneCastSound(sound);
    }

    public static boolean handleRailCastSound(SoundInstance sound) {
        return LocalAbilityState.handleRailCastSound(sound);
    }

    static void putLocalBeam(BeamKind kind, long firstSeen, long gameTime) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        // Every local beam goes through here, including the activation packet that arrives between
        // ticks, so resource pack mode is enforced here rather than only in the tick cleanup.
        if (usesResourcePackParticles(kind, kind.settings())) {
            removeLocalBeam(kind);
            return;
        }
        UUID casterId = minecraft.player.getUUID();
        activeBeams.put(casterId, new ActiveBeam(casterId, kind, kind.settings(), firstSeen, gameTime));
    }

    static void removeLocalBeam(BeamKind kind) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        UUID casterId = minecraft.player.getUUID();
        ActiveBeam existing = activeBeams.get(casterId);
        if (existing != null && existing.kind() == kind) {
            activeBeams.remove(casterId);
            BeamTracer.forget(casterId);
        }
    }

    static void refreshBeam(UUID casterId, BeamKind kind, BeamSettings settings, long gameTime) {
        ActiveBeam existing = activeBeams.get(casterId);
        long firstSeen = existing != null && existing.kind() == kind ? existing.firstSeenGameTime() : gameTime;
        activeBeams.put(casterId, new ActiveBeam(casterId, kind, settings, firstSeen, gameTime));
    }

    static ActiveBeam activeBeam(UUID casterId) {
        return activeBeams.get(casterId);
    }

    public static BeamTrace traceBeam(ActiveBeam beam, float partialTick) {
        return BeamTracer.traceBeam(beam, partialTick);
    }

    public static BeamTrace tracePreviewBeam(float partialTick) {
        return BeamTracer.tracePreviewBeam(partialTick);
    }

    public static ActiveBeam getLocalActiveBeam(BeamKind kind) {
        if (!replacementEnabled(kind)) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return null;
        }
        ActiveBeam beam = activeBeams.get(minecraft.player.getUUID());
        return beam != null && beam.kind() == kind ? beam : null;
    }

    public static boolean shouldSuppressAbilityCooldownSound() {
        return LocalAbilityState.shouldSuppressAbilityCooldownSound();
    }

    static boolean replacementEnabled(BeamKind kind) {
        return kind.settings().enabled;
    }

    private static void clearDisabledReplacementState() {
        for (BeamKind kind : BeamKind.values()) {
            if (!replacementEnabled(kind)) {
                clearReplacementState(kind);
            }
        }
    }

    private static void clearReplacementState(BeamKind kind) {
        removeLocalBeam(kind);
        LocalAbilityState.resetReplacement(kind);
    }

    private static void clearAllState() {
        clearAllState(false);
    }

    static void clearAllState(boolean keepSelections) {
        LocalAbilityState.reset(keepSelections);
        ArcaneParticleRouter.clear();
        activeBeams.clear();
        BeamTracer.clear();
    }
}
