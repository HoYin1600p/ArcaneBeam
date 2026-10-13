package dev.hoyin1600p.arcanebeam.beam;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/** Retains firing poses because vanilla particle packets do not identify their caster. */
final class LocalArcaneParticleTracker {
    private static final long HISTORY_TICKS = 20L;
    private static final int MAX_POSES = 64;
    private static final double MAX_RANGE = 65.0D;
    private static final double RADIUS_SQUARED = 0.36D;
    private final Deque<FiringPose> poses = new ArrayDeque<>();

    void record(long gameTime, Vec3 start, Vec3 direction) {
        expire(gameTime);
        if (!finite(start) || !finite(direction) || direction.lengthSqr() < 1.0E-6D) {
            return;
        }
        Vec3 normalized = direction.normalize();
        FiringPose latest = poses.peekLast();
        if (latest != null && latest.gameTime == gameTime
                && latest.start.equals(start) && latest.direction.equals(normalized)) {
            return;
        }
        poses.addLast(new FiringPose(gameTime, start, normalized));
        while (poses.size() > MAX_POSES) {
            poses.removeFirst();
        }
    }

    boolean shouldSuppress(boolean replacementWindow, UUID localPlayerId, UUID inferredOwnerId,
                           long gameTime, Vec3 particlePosition) {
        expire(gameTime);
        if (!replacementWindow) {
            return false;
        }
        // A confirmed local owner always suppresses. Unresolved packets and remote guesses must
        // lie on our recent firing path, otherwise nearby players' particles would vanish.
        return inferredOwnerId != null && inferredOwnerId.equals(localPlayerId)
                || matchesRecentPath(particlePosition);
    }

    void clear() {
        poses.clear();
    }

    /**
     * Whether local Arcane particles are being replaced right now: the cast is confirmed (active, beam drawn or
     * its short release tail) or the Arcane key was just pressed. Vault sends a cast's first tick of particles
     * before the activation message that confirms the cast, so the key-press window has to count as well or
     * every cast starts with a burst of Vault's own particles.
     */
    static boolean suppressionWindowOpen(boolean confirmedWindow, long gameTime, long armedUntilGameTime) {
        return confirmedWindow || gameTime <= armedUntilGameTime;
    }

    private boolean matchesRecentPath(Vec3 position) {
        for (FiringPose pose : poses) {
            Vec3 offset = position.subtract(pose.start);
            double along = offset.dot(pose.direction);
            if (along >= -0.5D && along <= MAX_RANGE
                    && pose.start.add(pose.direction.scale(along)).distanceToSqr(position) <= RADIUS_SQUARED) {
                return true;
            }
        }
        return false;
    }

    private void expire(long gameTime) {
        // A clock reset must not carry associations into a new session.
        if (!poses.isEmpty() && gameTime < poses.peekLast().gameTime) {
            clear();
        }
        while (!poses.isEmpty() && gameTime - poses.peekFirst().gameTime > HISTORY_TICKS) {
            poses.removeFirst();
        }
    }

    private static boolean finite(Vec3 vector) {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    private record FiringPose(long gameTime, Vec3 start, Vec3 direction) {
    }
}
