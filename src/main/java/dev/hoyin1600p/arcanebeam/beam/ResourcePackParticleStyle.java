package dev.hoyin1600p.arcanebeam.beam;

import com.mojang.math.Vector3f;
import dev.hoyin1600p.arcanebeam.mixin.accessor.ParticleAccessor;
import dev.hoyin1600p.arcanebeam.config.model.BeamSettings;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Size and facing for Vault Arcane particles shown in resource pack mode. Facing is a roll around
 * the view axis, recomputed every tick from the camera, so the sprite's top follows the beam's
 * on-screen direction wherever the beam origin sits. All calls happen on the client thread.
 */
public final class ResourcePackParticleStyle {
    public static final float MIN_SCALE = 0.1F;
    static final float MAX_SCALE = 8.0F;
    private static final double PROJECTION_STEP = 0.25D;
    private static final double MIN_DEPTH = 0.05D;
    private static final Map<Particle, Facing> FACINGS = new WeakHashMap<>();
    private static Pending pending;

    private ResourcePackParticleStyle() {
    }

    /** Remembers how the particle about to be created for this beam should look. */
    static void prepare(BeamSettings settings, Vec3 beamDirection) {
        pending = new Pending(clampScale(settings.particleScale), settings.orientParticles,
                (float) Math.toRadians(settings.particleRotationDegrees), beamDirection);
    }

    public static void apply(Particle particle) {
        Pending style = pending;
        pending = null;
        if (particle == null || style == null) {
            return;
        }
        if (style.scale() != 1.0F) {
            particle.scale(style.scale());
        }
        if (style.orient() && style.direction() != null && style.direction().lengthSqr() > 1.0E-6D) {
            FACINGS.put(particle, new Facing(style.direction().normalize(), style.rotationRadians()));
            updateRoll(particle, true);
        }
    }

    /** Called after each particle tick; keeps the previous roll so the renderer interpolates smoothly. */
    public static void tick(Particle particle) {
        if (!FACINGS.isEmpty()) {
            updateRoll(particle, false);
        }
    }

    private static void updateRoll(Particle particle, boolean first) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (camera == null || !camera.isInitialized()) {
            return;
        }
        ParticleAccessor accessor = (ParticleAccessor) particle;
        Float roll = rollFor(particle, camera, accessor.arcanebeam$getX(), accessor.arcanebeam$getY(), accessor.arcanebeam$getZ());
        if (roll == null) {
            return;
        }
        accessor.arcanebeam$setORoll(first ? roll : accessor.arcanebeam$getRoll());
        accessor.arcanebeam$setRoll(roll);
    }

    /**
     * The roll to render this particle with, or null to leave Vault's roll alone. The beam's on-screen
     * angle depends on perspective, so the particle's own position and a point a little further along
     * the beam are both projected, and the sprite is turned toward the second.
     */
    public static Float rollFor(Particle particle, Camera camera, double x, double y, double z) {
        if (FACINGS.isEmpty()) {
            return null;
        }
        Facing facing = FACINGS.get(particle);
        if (facing == null) {
            return null;
        }
        Vec3 cameraPosition = camera.getPosition();
        Vec3 forward = toVec3(camera.getLookVector());
        Vec3 up = toVec3(camera.getUpVector());
        Vec3 right = toVec3(camera.getLeftVector()).scale(-1.0D);
        Vec3 near = new Vec3(x, y, z).subtract(cameraPosition);
        Vec3 far = near.add(facing.direction().scale(PROJECTION_STEP));
        double nearDepth = near.dot(forward);
        double farDepth = far.dot(forward);
        if (nearDepth < MIN_DEPTH || farDepth < MIN_DEPTH) {
            // Too close to the camera plane for a stable projection: fall back to the direction alone.
            return roll(facing.direction().dot(right), facing.direction().dot(up), facing.offsetRadians());
        }
        double screenRight = far.dot(right) / farDepth - near.dot(right) / nearDepth;
        double screenUp = far.dot(up) / farDepth - near.dot(up) / nearDepth;
        return roll(screenRight, screenUp, facing.offsetRadians());
    }

    private static Vec3 toVec3(Vector3f vector) {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }

    /**
     * Roll that turns a sprite drawn pointing up so it points toward (right, up) on screen.
     * A positive particle roll turns the sprite clockwise on screen (checked in game), so pointing
     * right is a positive roll and the user's clockwise offset adds to it.
     */
    static float roll(double screenRight, double screenUp, float offsetRadians) {
        if (screenRight * screenRight + screenUp * screenUp < 1.0E-8D) {
            return offsetRadians;
        }
        return (float) Math.atan2(screenRight, screenUp) + offsetRadians;
    }

    static float clampScale(float scale) {
        if (!Float.isFinite(scale)) {
            return 1.0F;
        }
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
    }

    static boolean hasPending() {
        return pending != null;
    }

    /** Forgets a prepared style that no particle consumed. */
    public static void clearPending() {
        pending = null;
    }

    /** Drops every remembered facing and any unused prepared style; called when the client level changes. */
    public static void clear() {
        FACINGS.clear();
        pending = null;
    }

    private record Pending(float scale, boolean orient, float rotationRadians, Vec3 direction) {
    }

    private record Facing(Vec3 direction, float offsetRadians) {
    }
}
