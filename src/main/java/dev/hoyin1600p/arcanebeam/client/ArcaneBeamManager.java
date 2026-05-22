package dev.hoyin1600p.arcanebeam.client;

import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ArcaneBeam.MOD_ID, value = Dist.CLIENT)
public final class ArcaneBeamManager {
    private static final ResourceLocation ARCANE = new ResourceLocation("the_vault", "arcane");
    private static final ResourceLocation ARCANE_RAIL = new ResourceLocation("the_vault", "arcane_rail");
    private static final Map<UUID, ActiveBeam> activeBeams = new LinkedHashMap<>();
    private static long lastArcaneSeenGameTime = Long.MIN_VALUE;
    private static long lastLocalArcaneSeenGameTime = Long.MIN_VALUE;
    private static long lastLocalRailSeenGameTime = Long.MIN_VALUE;

    private ArcaneBeamManager() {
    }

    public static boolean captureParticle(ParticleOptions particle, double x, double y, double z) {
        ResourceLocation key = Registry.PARTICLE_TYPE.getKey(particle.getType());
        BeamKind kind;
        if (ARCANE.equals(key)) {
            kind = BeamKind.ARCANE;
        } else if (ARCANE_RAIL.equals(key)) {
            kind = BeamKind.RAIL;
        } else {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            AbstractClientPlayer caster = findCaster(minecraft.level, new Vec3(x, y, z), kind);
            if (caster != null) {
                long gameTime = minecraft.level.getGameTime();
                ActiveBeam existing = activeBeams.get(caster.getUUID());
                long firstSeen = existing != null && existing.kind == kind ? existing.firstSeenGameTime : gameTime;
                activeBeams.put(caster.getUUID(), new ActiveBeam(caster.getUUID(), kind, firstSeen, gameTime));
                if (kind == BeamKind.ARCANE) {
                    lastArcaneSeenGameTime = gameTime;
                }
                if (minecraft.player != null && caster.getUUID().equals(minecraft.player.getUUID())) {
                    if (kind == BeamKind.ARCANE) {
                        lastLocalArcaneSeenGameTime = gameTime;
                    } else {
                        lastLocalRailSeenGameTime = gameTime;
                    }
                }
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.getInstance().level == null) {
            activeBeams.clear();
        }
    }

    @SubscribeEvent
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

    private static AbstractClientPlayer findCaster(ClientLevel level, Vec3 particlePosition, BeamKind kind) {
        AbstractClientPlayer bestPlayer = null;
        double bestDistance = Double.MAX_VALUE;
        double maxRange = kind.settings().maxRange;

        for (AbstractClientPlayer player : level.players()) {
            Vec3 start = beamStart(player, 1.0F, kind.settings());
            Vec3 look = player.getLookAngle().normalize();
            Vec3 toParticle = particlePosition.subtract(start);
            double alongRay = toParticle.dot(look);
            if (alongRay < -0.5D || alongRay > maxRange + 1.0D) {
                continue;
            }

            Vec3 closestPoint = start.add(look.scale(alongRay));
            double distance = closestPoint.distanceToSqr(particlePosition);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestPlayer = player;
            }
        }

        return bestDistance <= 4.0D ? bestPlayer : Minecraft.getInstance().player;
    }

    private static void removeExpiredBeams(long gameTime) {
        Iterator<ActiveBeam> iterator = activeBeams.values().iterator();
        while (iterator.hasNext()) {
            ActiveBeam beam = iterator.next();
            if (gameTime - beam.lastSeenGameTime > beam.settings().lifetimeTicks) {
                iterator.remove();
            }
        }
    }

    public static BeamTrace traceBeam(ActiveBeam beam, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return null;
        }

        Entity entity = level.getPlayerByUUID(beam.casterId);
        if (!(entity instanceof LivingEntity caster)) {
            return null;
        }

        Vec3 aimStart = caster.getEyePosition(partialTick);
        Vec3 start = beamStart(caster, partialTick, beam.settings());
        Vec3 look = caster.getLookAngle().normalize();
        double maxRange = beam.settings().maxRange;
        Vec3 rangeEnd = aimStart.add(look.scale(maxRange));

        BlockHitResult blockHit = level.clip(new ClipContext(aimStart, rangeEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? rangeEnd : blockHit.getLocation();

        if (start.distanceToSqr(end) < 0.01D) {
            return null;
        }
        return new BeamTrace(beam.kind, start, end, beam.alphaMultiplier(level.getGameTime(), partialTick));
    }

    public static BeamTrace tracePreviewBeam(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Screen screen = minecraft.screen;
        if (level == null || !(screen instanceof ArcaneBeamConfigScreen configScreen) || minecraft.player == null) {
            return null;
        }

        LivingEntity caster = minecraft.player;
        BeamKind kind = configScreen.previewKind();
        ArcaneBeamConfig.BeamSettings settings = kind.settings();
        Vec3 aimStart = caster.getEyePosition(partialTick);
        Vec3 start = beamStart(caster, partialTick, settings);
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 rangeEnd = aimStart.add(look.scale(settings.maxRange));
        BlockHitResult blockHit = level.clip(new ClipContext(aimStart, rangeEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? rangeEnd : blockHit.getLocation();

        if (start.distanceToSqr(end) < 0.01D) {
            return null;
        }
        return new BeamTrace(kind, start, end, 1.0F);
    }

    public static ActiveBeam getLocalActiveBeam(BeamKind kind) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return null;
        }
        ActiveBeam beam = activeBeams.get(minecraft.player.getUUID());
        return beam != null && beam.kind == kind ? beam : null;
    }

    public static boolean shouldSuppressAbilityCooldownSound() {
        return isWithinRecentWindow(lastArcaneSeenGameTime, 5L);
    }

    public static boolean shouldSuppressArcaneCastSound() {
        return getLocalActiveBeam(BeamKind.ARCANE) != null || isWithinRecentWindow(lastLocalArcaneSeenGameTime, 5L);
    }

    public static boolean shouldSuppressRailCastSound() {
        return getLocalActiveBeam(BeamKind.RAIL) != null || isWithinRecentWindow(lastLocalRailSeenGameTime, 5L);
    }

    private static boolean isWithinRecentWindow(long lastSeenGameTime, long windowTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }
        long gameTime = minecraft.level.getGameTime();
        return gameTime - lastSeenGameTime >= 0L && gameTime - lastSeenGameTime <= windowTicks;
    }

    private static Vec3 beamStart(LivingEntity caster, float partialTick, ArcaneBeamConfig.BeamSettings settings) {
        Vec3 eye = caster.getEyePosition(partialTick);
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(look);
        if (right.lengthSqr() < 1.0E-4D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }

        ArcaneBeamConfig.StartHand startHand = ArcaneBeamConfig.StartHand.fromId(settings.startHand);
        HumanoidArm arm = startHand == ArcaneBeamConfig.StartHand.MAIN_HAND ? caster.getMainArm() : caster.getMainArm().getOpposite();
        double side = arm == HumanoidArm.RIGHT ? -1.0D : 1.0D;
        return eye.add(right.scale(settings.startOffsetX * side)).add(0.0D, settings.startOffsetY, 0.0D).add(look.scale(settings.startOffsetZ));
    }

    public enum BeamKind {
        ARCANE,
        RAIL;

        public ArcaneBeamConfig.BeamSettings settings() {
            return this == RAIL ? ArcaneBeamConfig.INSTANCE.rail : ArcaneBeamConfig.INSTANCE.arcane;
        }
    }

    public record ActiveBeam(UUID casterId, BeamKind kind, long firstSeenGameTime, long lastSeenGameTime) {
        public ArcaneBeamConfig.BeamSettings settings() {
            return kind.settings();
        }

        public float alphaMultiplier(long gameTime, float partialTick) {
            if (kind != BeamKind.RAIL) {
                return 1.0F;
            }

            float lifetime = Math.max(1.0F, settings().lifetimeTicks);
            float age = Math.max(0.0F, gameTime - firstSeenGameTime + partialTick);
            float fadeIn = Math.min(1.0F, age / 1.0F);
            float fadeOutStart = lifetime * 0.5F;
            float fadeOut = age <= fadeOutStart ? 1.0F : Math.max(0.0F, (lifetime - age) / (lifetime - fadeOutStart));
            return Math.min(1.0F, fadeIn * fadeOut);
        }
    }

    public record BeamTrace(BeamKind kind, Vec3 start, Vec3 end, float alphaMultiplier) {
        public ArcaneBeamConfig.BeamSettings settings() {
            return kind.settings();
        }
    }
}
