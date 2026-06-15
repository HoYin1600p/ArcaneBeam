package dev.hoyin1600p.arcanebeam.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import iskallia.vault.skill.ability.effect.ChainLightningAbility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

public class LightningStrikeChargeRenderer extends RenderType {
    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation(ArcaneBeam.MOD_ID, "textures/entity/white.png");
    private static final RenderType BODY = createBodyRenderType();
    private static final RenderType GLOW = createGlowRenderType();
    private static final int SIDES = 16;

    private LightningStrikeChargeRenderer(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static void render(PoseStack poseStack, Vec3 cameraPosition, float partialTick, Collection<ChainLightningAbility.ChainLightningProjectile> projectiles) {
        if (projectiles.isEmpty()) {
            return;
        }

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        for (ChainLightningAbility.ChainLightningProjectile projectile : projectiles) {
            renderProjectile(poseStack, buffer, projectile, partialTick);
        }

        poseStack.popPose();
        buffer.endBatch(BODY);
        buffer.endBatch(GLOW);
    }

    private static void renderProjectile(PoseStack poseStack, MultiBufferSource buffer, ChainLightningAbility.ChainLightningProjectile projectile, float partialTick) {
        double x = Mth.lerp(partialTick, projectile.xOld, projectile.getX());
        double y = Mth.lerp(partialTick, projectile.yOld, projectile.getY());
        double z = Mth.lerp(partialTick, projectile.zOld, projectile.getZ());
        Vec3 motion = projectile.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-5D) {
            motion = new Vec3(projectile.getX() - projectile.xOld, projectile.getY() - projectile.yOld, projectile.getZ() - projectile.zOld);
        }
        if (motion.lengthSqr() < 1.0E-5D) {
            motion = new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 direction = motion.normalize();

        float yaw = (float) Math.toDegrees(Math.atan2(direction.x, direction.z));
        float pitch = (float) Math.toDegrees(-Math.asin(direction.y));
        float roll = (projectile.tickCount + partialTick) * 18.0F;
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Vector3f.YP.rotationDegrees(yaw));
        poseStack.mulPose(Vector3f.XP.rotationDegrees(pitch));
        poseStack.mulPose(Vector3f.ZP.rotationDegrees(roll));
        renderChargeBody(poseStack, buffer);
        poseStack.popPose();
    }

    private static void renderChargeBody(PoseStack poseStack, MultiBufferSource buffer) {
        VertexConsumer body = buffer.getBuffer(BODY);
        VertexConsumer glow = buffer.getBuffer(GLOW);

        // Dimensions are cosmetic only: roughly 0.2 blocks wide by 0.35 blocks long.
        // The body axis is intentionally perpendicular to flight direction, matching the seismic charge broadside orientation.
        renderFrustumX(poseStack, body, -0.175F, -0.095F, 0.045F, 0.100F, 0x2A2D31, 1.0F);
        renderFrustumX(poseStack, body, -0.095F, 0.095F, 0.100F, 0.100F, 0x3A3E43, 1.0F);
        renderFrustumX(poseStack, body, 0.095F, 0.175F, 0.100F, 0.052F, 0x25282D, 1.0F);

        renderFrustumX(poseStack, body, -0.115F, -0.095F, 0.112F, 0.112F, 0xA7A9A7, 1.0F);
        renderFrustumX(poseStack, body, 0.095F, 0.115F, 0.112F, 0.112F, 0xB8B7B0, 1.0F);
        renderFrustumX(poseStack, body, -0.020F, 0.020F, 0.106F, 0.106F, 0x8A7B65, 1.0F);

        renderFrustum(poseStack, body, -0.120F, -0.080F, 0.055F, 0.040F, 0x111216, 1.0F);
        renderFrustum(poseStack, glow, -0.140F, -0.115F, 0.032F, 0.018F, 0x79E9FF, 0.85F);
        renderEnginePlume(poseStack, glow);

        renderEmitterPanelsX(poseStack, glow, -0.155F, -0.105F, 0xFFE36A, 0.95F);
        renderEmitterPanelsX(poseStack, glow, 0.105F, 0.155F, 0xFFE36A, 0.95F);
        renderPanelLinesX(poseStack, body);
    }

    private static void renderEmitterPanelsX(PoseStack poseStack, VertexConsumer builder, float x1, float x2, int color, float alpha) {
        Matrix4f pose = poseStack.last().pose();
        float red = red(color);
        float green = green(color);
        float blue = blue(color);
        for (int i = 0; i < 8; i += 2) {
            float a1 = (float) (Math.PI * 2.0D * (i + 0.18D) / 8.0D);
            float a2 = (float) (Math.PI * 2.0D * (i + 0.82D) / 8.0D);
            addCylinderQuadX(pose, builder, red, green, blue, alpha, 0.107F, 0.107F, x1, x2, a1, a2);
        }
    }

    private static void renderPanelLinesX(PoseStack poseStack, VertexConsumer builder) {
        Matrix4f pose = poseStack.last().pose();
        for (int i = 1; i < SIDES; i += 4) {
            float a1 = (float) (Math.PI * 2.0D * i / SIDES - 0.012D);
            float a2 = (float) (Math.PI * 2.0D * i / SIDES + 0.012D);
            addCylinderQuadX(pose, builder, 0.07F, 0.08F, 0.09F, 1.0F, 0.108F, 0.108F, -0.080F, 0.080F, a1, a2);
        }
    }

    private static void renderEnginePlume(PoseStack poseStack, VertexConsumer builder) {
        Matrix4f pose = poseStack.last().pose();
        for (int i = 0; i < SIDES; i++) {
            int next = (i + 1) % SIDES;
            float a1 = (float) (Math.PI * 2.0D * i / SIDES);
            float a2 = (float) (Math.PI * 2.0D * next / SIDES);
            addCylinderQuad(pose, builder, 0.35F, 0.88F, 1.0F, 0.40F, 0.060F, 0.000F, -0.175F, -0.355F, a1, a2);
        }
    }

    private static void renderFrustum(PoseStack poseStack, VertexConsumer builder, float z1, float z2, float r1, float r2, int color, float alpha) {
        Matrix4f pose = poseStack.last().pose();
        float red = red(color);
        float green = green(color);
        float blue = blue(color);
        for (int i = 0; i < SIDES; i++) {
            int next = (i + 1) % SIDES;
            float a1 = (float) (Math.PI * 2.0D * i / SIDES);
            float a2 = (float) (Math.PI * 2.0D * next / SIDES);
            float shade = 0.72F + 0.28F * Math.max(0.0F, (float) Math.cos(a1 - 0.65F));
            addCylinderQuad(pose, builder, red * shade, green * shade, blue * shade, alpha, r1, r2, z1, z2, a1, a2);
        }
    }

    private static void renderFrustumX(PoseStack poseStack, VertexConsumer builder, float x1, float x2, float r1, float r2, int color, float alpha) {
        Matrix4f pose = poseStack.last().pose();
        float red = red(color);
        float green = green(color);
        float blue = blue(color);
        for (int i = 0; i < SIDES; i++) {
            int next = (i + 1) % SIDES;
            float a1 = (float) (Math.PI * 2.0D * i / SIDES);
            float a2 = (float) (Math.PI * 2.0D * next / SIDES);
            float shade = 0.72F + 0.28F * Math.max(0.0F, (float) Math.cos(a1 - 0.65F));
            addCylinderQuadX(pose, builder, red * shade, green * shade, blue * shade, alpha, r1, r2, x1, x2, a1, a2);
        }
    }

    private static void addCylinderQuad(Matrix4f pose, VertexConsumer builder, float red, float green, float blue, float alpha, float r1, float r2, float z1, float z2, float a1, float a2) {
        addVertex(pose, builder, red, green, blue, alpha, (float) Math.cos(a1) * r1, (float) Math.sin(a1) * r1, z1, 0.0F, 0.0F);
        addVertex(pose, builder, red, green, blue, alpha, (float) Math.cos(a1) * r2, (float) Math.sin(a1) * r2, z2, 0.0F, 1.0F);
        addVertex(pose, builder, red, green, blue, alpha, (float) Math.cos(a2) * r2, (float) Math.sin(a2) * r2, z2, 1.0F, 1.0F);
        addVertex(pose, builder, red, green, blue, alpha, (float) Math.cos(a2) * r1, (float) Math.sin(a2) * r1, z1, 1.0F, 0.0F);
    }

    private static void addCylinderQuadX(Matrix4f pose, VertexConsumer builder, float red, float green, float blue, float alpha, float r1, float r2, float x1, float x2, float a1, float a2) {
        addVertex(pose, builder, red, green, blue, alpha, x1, (float) Math.cos(a1) * r1, (float) Math.sin(a1) * r1, 0.0F, 0.0F);
        addVertex(pose, builder, red, green, blue, alpha, x2, (float) Math.cos(a1) * r2, (float) Math.sin(a1) * r2, 0.0F, 1.0F);
        addVertex(pose, builder, red, green, blue, alpha, x2, (float) Math.cos(a2) * r2, (float) Math.sin(a2) * r2, 1.0F, 1.0F);
        addVertex(pose, builder, red, green, blue, alpha, x1, (float) Math.cos(a2) * r1, (float) Math.sin(a2) * r1, 1.0F, 0.0F);
    }

    private static void addVertex(Matrix4f pose, VertexConsumer builder, float red, float green, float blue, float alpha, float x, float y, float z, float u, float v) {
        builder.vertex(pose, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .endVertex();
    }

    private static float red(int color) {
        return ((color >> 16) & 0xFF) / 255.0F;
    }

    private static float green(int color) {
        return ((color >> 8) & 0xFF) / 255.0F;
    }

    private static float blue(int color) {
        return (color & 0xFF) / 255.0F;
    }

    private static RenderType createBodyRenderType() {
        CompositeState state = CompositeState.builder()
                .setShaderState(POSITION_COLOR_TEX_SHADER)
                .setTextureState(new TextureStateShard(WHITE_TEXTURE, false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .createCompositeState(false);
        return RenderType.create("arcanebeam_lightning_charge_body", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 512, false, false, state);
    }

    private static RenderType createGlowRenderType() {
        CompositeState state = CompositeState.builder()
                .setShaderState(POSITION_COLOR_TEX_SHADER)
                .setTextureState(new TextureStateShard(WHITE_TEXTURE, false, false))
                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .createCompositeState(false);
        return RenderType.create("arcanebeam_lightning_charge_glow", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 512, false, true, state);
    }
}
