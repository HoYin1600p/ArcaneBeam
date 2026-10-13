package dev.hoyin1600p.arcanebeam.beam;

import dev.hoyin1600p.arcanebeam.beam.profile.PlayerBeamAssignments;
import dev.hoyin1600p.arcanebeam.beam.profile.PlayerBeamProfileManager;
import dev.hoyin1600p.arcanebeam.config.model.BeamSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Decides which player owns a Vault Arcane particle and either swallows it, re-aims it or turns it into a beam.
 *
 * <p>Arcane Prism emits the same two particle types as Base and Rail, so a particle is never claimed on geometry
 * alone: the local player's particles need a Base or Rail selection (the synced Arcane specialization), and a
 * remote player's need an Arcane or Rail cast sound heard at that player shortly before (see
 * {@link RemoteCastEvidence}); both are bypassed for Prism, whose particles stay Vault's own.
 */
final class ArcaneParticleRouter {
    private ArcaneParticleRouter() {
    }

    private static final double MAX_PARTICLE_RAY_DISTANCE_SQR = 0.36D;
    private static final double OWNERSHIP_AMBIGUITY_MARGIN = 0.04D;
    private static final double REMOTE_ACQUIRE_DISTANCE = 3.0D;
    private static final long REMOTE_CLAIM_GRACE_TICKS = 8L;
    /** A particle farther than this from the beam axis is scatter noise, not part of the beam. */
    static final double REAIM_MAX_LATERAL = 1.5D;
    private static final Map<ClaimKey, Long> remoteClaims = new HashMap<>();
    private static final List<PlayerRay> rayCache = new ArrayList<>();
    private static ClientLevel rayCacheLevel;
    private static long rayCacheGameTime = Long.MIN_VALUE;

    private record ClaimKey(UUID player, BeamKind kind) {
    }

    /** Forgets remote claims; called whenever the tracked world or player changes. */
    static void clear() {
        remoteClaims.clear();
        rayCache.clear();
        rayCacheLevel = null;
        rayCacheGameTime = Long.MIN_VALUE;
    }

    /**
     * Whether a remote player's particle may be claimed: a beam or claim already running for this player and kind
     * continues, a new one needs a cast sound of that kind heard at the player.
     */
    private static boolean mayClaimRemote(BeamKind kind, AbstractClientPlayer owner, long gameTime, boolean continuing) {
        ClaimKey key = new ClaimKey(owner.getUUID(), kind);
        Long last = remoteClaims.get(key);
        boolean recent = last != null && gameTime >= last && gameTime - last <= REMOTE_CLAIM_GRACE_TICKS;
        if (continuing || recent || LocalAbilityState.hasCastEvidence(kind, owner.position(), gameTime)) {
            remoteClaims.put(key, gameTime);
            return true;
        }
        return false;
    }

    static boolean handleArcaneParticle(boolean rail, double x, double y, double z,
                                        double xSpeed, double ySpeed, double zSpeed) {
        BeamKind kind = rail ? BeamKind.RAIL : BeamKind.ARCANE;
        Minecraft minecraft = Minecraft.getInstance();
        LocalAbilityState.ensureClientContext(minecraft);
        if (minecraft.level == null || minecraft.player == null) {
            return false;
        }

        Vec3 particlePosition = new Vec3(x, y, z);
        ParticleOwner owner = findParticleOwner(minecraft.level, particlePosition, kind);
        if (owner != null && owner.player().getUUID().equals(minecraft.player.getUUID())
                && LocalAbilityState.isLocalPrismSelected()) {
            return false;
        }
        if (kind == BeamKind.ARCANE && !ArcaneBeamManager.usesResourcePackParticles(kind, kind.settings()) && LocalAbilityState.shouldSuppressArcaneParticle(
                minecraft.player.getUUID(),
                owner == null ? null : owner.player().getUUID(),
                minecraft.level.getGameTime(), particlePosition)) {
            return true;
        }
        if (owner == null) {
            return false;
        }

        if (owner.player().getUUID().equals(minecraft.player.getUUID())) {
            if (kind == BeamKind.ARCANE) {
                return false;
            }
            long gameTime = minecraft.level.getGameTime();
            // Arming already required a Rail selection (ability key) or the Arcane quickselect key.
            if (ArcaneBeamManager.replacementEnabled(kind) && owner.alongRay() <= REMOTE_ACQUIRE_DISTANCE
                    && LocalAbilityState.isRailParticleArmedOrLatched(gameTime)) {
                LocalAbilityState.acquireRailFromParticle(gameTime);
            }
            return ArcaneBeamManager.getLocalActiveBeam(kind) != null;
        }

        ResourceLocation profileId = PlayerBeamAssignments.profileFor(owner.player());
        PlayerBeamProfileManager.ParticleSettings particleSettings =
                PlayerBeamProfileManager.particleSettings(profileId, kind);
        long now = minecraft.level.getGameTime();
        ActiveBeam existing = ArcaneBeamManager.activeBeam(owner.player().getUUID());
        boolean continuing = existing != null && existing.kind() == kind;
        if (particleSettings != null) {
            if (!mayClaimRemote(kind, owner.player(), now, false)) {
                return false;
            }
            RemotePlayerParticleManager.addParticle(x, y, z, xSpeed, ySpeed, zSpeed, particleSettings);
            return true;
        }

        BeamSettings settings = PlayerBeamProfileManager.settings(profileId, kind);
        if (settings == null || !settings.enabled || ArcaneBeamManager.usesResourcePackParticles(kind, settings)) {
            return false;
        }

        if (!continuing && owner.alongRay() > REMOTE_ACQUIRE_DISTANCE) {
            return false;
        }
        if (!mayClaimRemote(kind, owner.player(), now, continuing)) {
            return false;
        }

        ArcaneBeamManager.refreshBeam(owner.player().getUUID(), kind, settings, minecraft.level.getGameTime());
        return true;
    }

    /**
     * Resource pack mode keeps Vault's own Arcane particles (so a pack retexture shows) but re-aims them:
     * a particle at fraction f along the server's eye ray to the hit is moved to fraction f along the
     * line from the configured beam start to that same hit, keeping its sideways scatter.
     * Returns null when the particle should be left exactly where Vault put it.
     */
    static Vec3 resourcePackParticlePosition(boolean rail, double x, double y, double z) {
        if (rail) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalAbilityState.ensureClientContext(minecraft);
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return null;
        }
        Vec3 particlePosition = new Vec3(x, y, z);
        ParticleOwner owner = findParticleOwner(level, particlePosition, BeamKind.ARCANE);
        LivingEntity caster;
        BeamSettings settings;
        if (owner == null || owner.player().getUUID().equals(minecraft.player.getUUID())) {
            boolean local = owner != null || LocalAbilityState.shouldSuppressArcaneParticle(
                    minecraft.player.getUUID(), null, level.getGameTime(), particlePosition);
            if (!local || LocalAbilityState.isLocalPrismSelected()) {
                return null;
            }
            caster = minecraft.player;
            settings = BeamKind.ARCANE.settings();
        } else {
            if (!mayClaimRemote(BeamKind.ARCANE, owner.player(), level.getGameTime(), false)) {
                return null;
            }
            caster = owner.player();
            settings = PlayerBeamProfileManager.settings(PlayerBeamAssignments.profileFor(owner.player()), BeamKind.ARCANE);
        }
        if (!ArcaneBeamManager.usesResourcePackParticles(BeamKind.ARCANE, settings)) {
            return null;
        }
        return reaimParticle(level, caster, settings, particlePosition);
    }

    private static Vec3 reaimParticle(ClientLevel level, LivingEntity caster, BeamSettings settings, Vec3 particle) {
        Vec3 serverStart = BeamTracer.serverBeamStart(caster, 1.0F);
        Vec3 look = BeamTracer.resolveLookVector(caster.getUUID(), caster, 1.0F);
        Vec3 newStart = BeamTracer.rawBeamStart(caster, 1.0F, settings);
        // One trace per caster per tick: a cast spawns many particles in the same packet batch, and the
        // per-caster cache answers all of them after the first.
        Vec3 hitEnd = BeamTracer.cachedCrosshairHit(level, caster, caster.getEyePosition(1.0F), look, settings.maxRange);
        ResourcePackParticleStyle.prepare(settings, hitEnd == null ? look : hitEnd.subtract(newStart));
        return reaim(serverStart, look, newStart, hitEnd, particle);
    }

    /**
     * Moves a particle from the server's beam (eye ray to the hit) onto the local beam (hand origin to the same
     * hit). The position along the beam is clamped to the local segment, so a client hit that stops short of the
     * server's (glass panes, plants, unloaded chunks) can never push a particle past the local hit point or in
     * front of the origin, and scatter beyond {@link #REAIM_MAX_LATERAL} is dropped rather than carried along.
     */
    static Vec3 reaim(Vec3 serverStart, Vec3 look, Vec3 newStart, Vec3 hitEnd, Vec3 particle) {
        double along = particle.subtract(serverStart).dot(look);
        Vec3 sideways = particle.subtract(serverStart.add(look.scale(along)));
        double serverLength = hitEnd == null ? 0.0D : serverStart.distanceTo(hitEnd);
        if (serverLength < 0.5D) {
            return particle.add(newStart.subtract(serverStart));
        }
        double fraction = Math.max(0.0D, Math.min(1.0D, along / serverLength));
        if (sideways.lengthSqr() > REAIM_MAX_LATERAL * REAIM_MAX_LATERAL) {
            sideways = Vec3.ZERO;
        }
        return newStart.add(hitEnd.subtract(newStart).scale(fraction)).add(sideways);
    }

    /**
     * Every player's server-side beam ray, computed once per game tick: a cast spawns many particles in one packet
     * batch and each used to rebuild every player's ray (several vectors and a view-vector call per player).
     */
    private static List<PlayerRay> playerRays(ClientLevel level) {
        long gameTime = level.getGameTime();
        if (rayCacheLevel != level || rayCacheGameTime != gameTime) {
            rayCache.clear();
            for (AbstractClientPlayer player : level.players()) {
                rayCache.add(new PlayerRay(player, BeamTracer.serverBeamStart(player, 1.0F),
                        BeamTracer.safeLookVector(player.getUUID(), player.getViewVector(1.0F), player.getLookAngle())));
            }
            rayCacheLevel = level;
            rayCacheGameTime = gameTime;
        }
        return rayCache;
    }

    private record PlayerRay(AbstractClientPlayer player, Vec3 start, Vec3 look) {
    }

    private static ParticleOwner findParticleOwner(ClientLevel level, Vec3 particlePosition, BeamKind kind) {
        ParticleOwner best = null;
        double bestDistance = Double.MAX_VALUE;
        double secondBestDistance = Double.MAX_VALUE;
        double maxRange = kind == BeamKind.RAIL ? 160.0D : 64.0D;

        for (PlayerRay ray : playerRays(level)) {
            AbstractClientPlayer player = ray.player();
            Vec3 start = ray.start();
            Vec3 look = ray.look();
            Vec3 toParticle = particlePosition.subtract(start);
            double alongRay = toParticle.dot(look);
            if (alongRay < -0.5D || alongRay > maxRange + 1.0D) {
                continue;
            }

            Vec3 closestPoint = start.add(look.scale(alongRay));
            double distance = closestPoint.distanceToSqr(particlePosition);
            if (distance < bestDistance) {
                secondBestDistance = bestDistance;
                bestDistance = distance;
                best = new ParticleOwner(player, alongRay);
            } else if (distance < secondBestDistance) {
                secondBestDistance = distance;
            }
        }

        if (best == null || bestDistance > MAX_PARTICLE_RAY_DISTANCE_SQR) {
            return null;
        }
        if (secondBestDistance < Double.MAX_VALUE && secondBestDistance - bestDistance < OWNERSHIP_AMBIGUITY_MARGIN) {
            return null;
        }
        return best;
    }
}
