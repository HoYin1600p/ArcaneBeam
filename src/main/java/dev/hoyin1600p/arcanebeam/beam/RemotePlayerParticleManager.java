package dev.hoyin1600p.arcanebeam.beam;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import dev.hoyin1600p.arcanebeam.beam.profile.PlayerBeamProfileManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class RemotePlayerParticleManager {
    private static final int FULLBRIGHT = 15728880;
    private static final int MAX_PARTICLES = 4096;
    private static final Deque<RemoteParticle> PARTICLES = new ArrayDeque<>();
    private static final Map<ResourceLocation, RenderType> RENDER_TYPES = new HashMap<>();
    private static final Map<RenderType, List<RemoteParticle>> BUCKETS = new LinkedHashMap<>();
    private static final BlockPos.MutableBlockPos LIGHT_POS = new BlockPos.MutableBlockPos();
    private static final Random RANDOM = new Random();

    private RemotePlayerParticleManager() {
    }

    public static void addParticle(double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
                                   PlayerBeamProfileManager.ParticleSettings settings) {
        if (PARTICLES.size() >= MAX_PARTICLES) {
            PARTICLES.removeFirst();
        }
        PARTICLES.addLast(new RemoteParticle(x, y, z, xSpeed, ySpeed, zSpeed, settings));
    }

    /** Drops all per-level state; called when the client level instance changes. */
    public static void clear() {
        PARTICLES.clear();
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (Minecraft.getInstance().level == null) {
            PARTICLES.clear();
            return;
        }

        Iterator<RemoteParticle> iterator = PARTICLES.iterator();
        while (iterator.hasNext()) {
            RemoteParticle particle = iterator.next();
            if (!particle.tick()) {
                iterator.remove();
            }
        }
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || PARTICLES.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPosition = event.getCamera().getPosition();
        float partialTick = event.getPartialTick();
        // A profile may list several textures, chosen per particle. Drawing in queue order would switch render
        // type constantly and make the buffer source flush a tiny batch each time, so particles are bucketed by
        // type first and each type is drawn and flushed once per frame.
        for (List<RemoteParticle> bucket : BUCKETS.values()) {
            bucket.clear();
        }
        for (RemoteParticle particle : PARTICLES) {
            RenderType renderType = RENDER_TYPES.computeIfAbsent(particle.texture, RenderType::entityTranslucent);
            BUCKETS.computeIfAbsent(renderType, type -> new ArrayList<>()).add(particle);
        }
        for (Map.Entry<RenderType, List<RemoteParticle>> entry : BUCKETS.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            VertexConsumer builder = buffer.getBuffer(entry.getKey());
            for (RemoteParticle particle : entry.getValue()) {
                renderParticle(poseStack, builder, event, cameraPosition, particle, partialTick);
            }
            buffer.endBatch(entry.getKey());
        }
    }

    private static void renderParticle(PoseStack poseStack, VertexConsumer builder, RenderLevelStageEvent event,
                                       Vec3 cameraPosition, RemoteParticle particle, float partialTick) {
        float progress = Mth.clamp((particle.age + partialTick) / particle.settings.lifetimeTicks(), 0.0F, 1.0F);
        float halfSize = particle.settings.size() * (1.0F - progress * progress * 0.5F);
        float alpha = particle.settings.alpha() * (1.0F - progress * progress);
        double worldX = Mth.lerp(partialTick, particle.previousX, particle.x);
        double worldY = Mth.lerp(partialTick, particle.previousY, particle.y);
        double worldZ = Mth.lerp(partialTick, particle.previousZ, particle.z);
        double x = worldX - cameraPosition.x;
        double y = worldY - cameraPosition.y;
        double z = worldZ - cameraPosition.z;

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(event.getCamera().rotation());
        PoseStack.Pose pose = poseStack.last();
        // Light 0 is the darkest lightmap cell, so a particle that is not fullbright samples the world light
        // where it actually is, like a vanilla particle does.
        int light = particle.settings.fullbright() ? FULLBRIGHT : worldLight(worldX, worldY, worldZ);

        addVertex(pose, builder, -halfSize, -halfSize, 0.0F, 1.0F, alpha, light);
        addVertex(pose, builder, halfSize, -halfSize, 1.0F, 1.0F, alpha, light);
        addVertex(pose, builder, halfSize, halfSize, 1.0F, 0.0F, alpha, light);
        addVertex(pose, builder, -halfSize, halfSize, 0.0F, 0.0F, alpha, light);
        poseStack.popPose();
    }

    private static int worldLight(double x, double y, double z) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return FULLBRIGHT;
        }
        LIGHT_POS.set(x, y, z);
        return LevelRenderer.getLightColor(level, LIGHT_POS);
    }

    private static void addVertex(PoseStack.Pose pose, VertexConsumer builder, float x, float y,
                                  float u, float v, float alpha, int packedLight) {
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        builder.vertex(matrix, x, y, 0.0F)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal, Vector3f.ZP.x(), Vector3f.ZP.y(), Vector3f.ZP.z())
                .endVertex();
    }

    private static final class RemoteParticle {
        private final PlayerBeamProfileManager.ParticleSettings settings;
        private final ResourceLocation texture;
        private double previousX;
        private double previousY;
        private double previousZ;
        private double x;
        private double y;
        private double z;
        private double xSpeed;
        private double ySpeed;
        private double zSpeed;
        private int age;

        private RemoteParticle(double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
                               PlayerBeamProfileManager.ParticleSettings settings) {
            this.settings = settings;
            this.texture = settings.chooseTexture(RANDOM);
            this.previousX = this.x = x;
            this.previousY = this.y = y;
            this.previousZ = this.z = z;
            this.xSpeed = xSpeed;
            this.ySpeed = ySpeed;
            this.zSpeed = zSpeed;
        }

        private boolean tick() {
            previousX = x;
            previousY = y;
            previousZ = z;
            x += xSpeed;
            y += ySpeed;
            z += zSpeed;
            xSpeed *= 0.96D;
            ySpeed *= 0.96D;
            zSpeed *= 0.96D;
            age++;
            return age < settings.lifetimeTicks();
        }
    }
}
