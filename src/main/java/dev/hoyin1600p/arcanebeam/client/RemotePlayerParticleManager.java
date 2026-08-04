package dev.hoyin1600p.arcanebeam.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = ArcaneBeam.MOD_ID, value = Dist.CLIENT)
public final class RemotePlayerParticleManager {
    private static final int FULLBRIGHT = 15728880;
    private static final int MAX_PARTICLES = 4096;
    private static final List<RemoteParticle> PARTICLES = new ArrayList<>();
    private static final Map<ResourceLocation, RenderType> RENDER_TYPES = new HashMap<>();

    private RemotePlayerParticleManager() {
    }

    public static void addParticle(double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
                                   PlayerBeamProfileManager.ParticleSettings settings) {
        if (PARTICLES.size() >= MAX_PARTICLES) {
            PARTICLES.remove(0);
        }
        PARTICLES.add(new RemoteParticle(x, y, z, xSpeed, ySpeed, zSpeed, settings));
    }

    @SubscribeEvent
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

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || PARTICLES.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPosition = event.getCamera().getPosition();
        float partialTick = event.getPartialTick();
        Map<ResourceLocation, RenderType> usedTypes = new HashMap<>();

        for (RemoteParticle particle : PARTICLES) {
            ResourceLocation texture = particle.settings.texture();
            RenderType renderType = RENDER_TYPES.computeIfAbsent(texture, RenderType::entityTranslucent);
            usedTypes.put(texture, renderType);
            renderParticle(poseStack, buffer.getBuffer(renderType), event, cameraPosition, particle, partialTick);
        }
        for (RenderType renderType : usedTypes.values()) {
            buffer.endBatch(renderType);
        }
    }

    private static void renderParticle(PoseStack poseStack, VertexConsumer builder, RenderLevelStageEvent event,
                                       Vec3 cameraPosition, RemoteParticle particle, float partialTick) {
        float progress = Mth.clamp((particle.age + partialTick) / particle.settings.lifetimeTicks(), 0.0F, 1.0F);
        float halfSize = particle.settings.size() * (1.0F - progress * progress * 0.5F);
        float alpha = particle.settings.alpha() * (1.0F - progress * progress);
        double x = Mth.lerp(partialTick, particle.previousX, particle.x) - cameraPosition.x;
        double y = Mth.lerp(partialTick, particle.previousY, particle.y) - cameraPosition.y;
        double z = Mth.lerp(partialTick, particle.previousZ, particle.z) - cameraPosition.z;

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(event.getCamera().rotation());
        PoseStack.Pose pose = poseStack.last();
        int light = particle.settings.fullbright() ? FULLBRIGHT : 0;

        addVertex(pose, builder, -halfSize, -halfSize, 0.0F, 1.0F, alpha, light);
        addVertex(pose, builder, halfSize, -halfSize, 1.0F, 1.0F, alpha, light);
        addVertex(pose, builder, halfSize, halfSize, 1.0F, 0.0F, alpha, light);
        addVertex(pose, builder, -halfSize, halfSize, 0.0F, 0.0F, alpha, light);
        poseStack.popPose();
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
