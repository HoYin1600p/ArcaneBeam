package dev.hoyin1600p.arcanebeam.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import iskallia.vault.skill.ability.effect.ChainLightningAbility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

public class LightningStrikeChargeRenderer extends RenderType {
    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation(ArcaneBeam.MOD_ID, "textures/entity/white.png");
    private static final RenderType BODY = createBodyRenderType();
    private static final RenderType GLOW = createGlowRenderType();
    private static final RenderType SHADER_BODY = RenderType.entityCutoutNoCull(WHITE_TEXTURE);
    private static final RenderType SHADER_GLOW = RenderType.entityTranslucent(WHITE_TEXTURE);
    private static final int FULLBRIGHT = 15728880;
    private static final int SIDES = 16;
    private static final int SPHERE_LATITUDES = 8;
    private static final int SPHERE_LONGITUDES = 16;
    private static final float DESIGNATOR_RADIUS = 0.075F;
    private static final float LIGHT_RADIUS = 0.015F;

    private LightningStrikeChargeRenderer(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static void render(PoseStack poseStack, Vec3 cameraPosition, float partialTick, Collection<ChainLightningAbility.ChainLightningProjectile> projectiles) {
        if (projectiles.isEmpty()) {
            return;
        }

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        boolean shaderCompatibility = shaderCompatibilityEnabled();
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        for (ChainLightningAbility.ChainLightningProjectile projectile : projectiles) {
            renderProjectile(poseStack, buffer, projectile, partialTick, shaderCompatibility);
        }

        poseStack.popPose();
        buffer.endBatch(BODY);
        buffer.endBatch(GLOW);
        buffer.endBatch(SHADER_BODY);
        buffer.endBatch(SHADER_GLOW);
    }

    private static boolean shaderCompatibilityEnabled() {
        ArcaneBeamConfig.ShaderCompatibility compatibility = ArcaneBeamConfig.ShaderCompatibility.fromId(ArcaneBeamConfig.INSTANCE.lightningStrike.shaderCompatibility);
        return compatibility == ArcaneBeamConfig.ShaderCompatibility.ON;
    }

    private static void renderProjectile(PoseStack poseStack, MultiBufferSource buffer, ChainLightningAbility.ChainLightningProjectile projectile, float partialTick, boolean shaderCompatibility) {
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
        poseStack.mulPose(Vector3f.XP.rotationDegrees(roll));
        renderChargeBody(poseStack, buffer, shaderCompatibility);
        poseStack.popPose();
    }

    private static void renderChargeBody(PoseStack poseStack, MultiBufferSource buffer, boolean shaderCompatibility) {
        VertexConsumer body = buffer.getBuffer(shaderCompatibility ? SHADER_BODY : BODY);
        VertexConsumer glow = buffer.getBuffer(shaderCompatibility ? SHADER_GLOW : GLOW);

        // Cosmetic-only target designator: about 15 cm across with six small emissive face lights.
        renderSphere(poseStack, body, DESIGNATOR_RADIUS, 0x5B6268, shaderCompatibility);
        renderEquatorBands(poseStack, body, shaderCompatibility);
        renderLightCap(poseStack, glow, 1.0F, 0.0F, 0.0F, 0xFFD447, shaderCompatibility);
        renderLightCap(poseStack, glow, -1.0F, 0.0F, 0.0F, 0xF13B2F, shaderCompatibility);
        renderLightCap(poseStack, glow, 0.0F, 1.0F, 0.0F, 0xFFD447, shaderCompatibility);
        renderLightCap(poseStack, glow, 0.0F, -1.0F, 0.0F, 0xF13B2F, shaderCompatibility);
        renderLightCap(poseStack, glow, 0.0F, 0.0F, 1.0F, 0xFFD447, shaderCompatibility);
        renderLightCap(poseStack, glow, 0.0F, 0.0F, -1.0F, 0xF13B2F, shaderCompatibility);
    }

    private static void renderSphere(PoseStack poseStack, VertexConsumer builder, float radius, int color, boolean shaderCompatibility) {
        PoseStack.Pose pose = poseStack.last();
        float red = red(color);
        float green = green(color);
        float blue = blue(color);
        for (int lat = 0; lat < SPHERE_LATITUDES; lat++) {
            float v1 = lat / (float) SPHERE_LATITUDES;
            float v2 = (lat + 1) / (float) SPHERE_LATITUDES;
            float pitch1 = (float) (-Math.PI / 2.0D + Math.PI * v1);
            float pitch2 = (float) (-Math.PI / 2.0D + Math.PI * v2);
            for (int lon = 0; lon < SPHERE_LONGITUDES; lon++) {
                float u1 = lon / (float) SPHERE_LONGITUDES;
                float u2 = (lon + 1) / (float) SPHERE_LONGITUDES;
                Vec3 p1 = spherePoint(radius, pitch1, (float) (Math.PI * 2.0D * u1));
                Vec3 p2 = spherePoint(radius, pitch2, (float) (Math.PI * 2.0D * u1));
                Vec3 p3 = spherePoint(radius, pitch2, (float) (Math.PI * 2.0D * u2));
                Vec3 p4 = spherePoint(radius, pitch1, (float) (Math.PI * 2.0D * u2));
                float shade = 0.55F + 0.45F * Math.max(0.0F, (float) p1.normalize().dot(new Vec3(0.35D, 0.75D, 0.55D).normalize()));
                addVertex(pose, builder, red * shade, green * shade, blue * shade, 1.0F, (float) p1.x, (float) p1.y, (float) p1.z, u1, v1, shaderCompatibility);
                addVertex(pose, builder, red * shade, green * shade, blue * shade, 1.0F, (float) p2.x, (float) p2.y, (float) p2.z, u1, v2, shaderCompatibility);
                addVertex(pose, builder, red * shade, green * shade, blue * shade, 1.0F, (float) p3.x, (float) p3.y, (float) p3.z, u2, v2, shaderCompatibility);
                addVertex(pose, builder, red * shade, green * shade, blue * shade, 1.0F, (float) p4.x, (float) p4.y, (float) p4.z, u2, v1, shaderCompatibility);
            }
        }
    }

    private static Vec3 spherePoint(float radius, float pitch, float yaw) {
        double cosPitch = Math.cos(pitch);
        return new Vec3(
                Math.cos(yaw) * cosPitch * radius,
                Math.sin(pitch) * radius,
                Math.sin(yaw) * cosPitch * radius
        );
    }

    private static void renderEquatorBands(PoseStack poseStack, VertexConsumer builder, boolean shaderCompatibility) {
        renderBand(poseStack, builder, Vector3f.YP, 0x2A2E33, shaderCompatibility);
        renderBand(poseStack, builder, Vector3f.XP, 0x343A40, shaderCompatibility);
    }

    private static void renderBand(PoseStack poseStack, VertexConsumer builder, Vector3f axis, int color, boolean shaderCompatibility) {
        poseStack.pushPose();
        if (axis == Vector3f.XP) {
            poseStack.mulPose(Vector3f.ZP.rotationDegrees(90.0F));
        }
        PoseStack.Pose pose = poseStack.last();
        float red = red(color);
        float green = green(color);
        float blue = blue(color);
        for (int i = 0; i < SIDES; i++) {
            float a1 = (float) (Math.PI * 2.0D * i / SIDES);
            float a2 = (float) (Math.PI * 2.0D * (i + 1) / SIDES);
            float y1 = -0.006F;
            float y2 = 0.006F;
            float r = DESIGNATOR_RADIUS + 0.002F;
            addVertex(pose, builder, red, green, blue, 1.0F, (float) Math.cos(a1) * r, y1, (float) Math.sin(a1) * r, 0.0F, 0.0F, shaderCompatibility);
            addVertex(pose, builder, red, green, blue, 1.0F, (float) Math.cos(a1) * r, y2, (float) Math.sin(a1) * r, 0.0F, 1.0F, shaderCompatibility);
            addVertex(pose, builder, red, green, blue, 1.0F, (float) Math.cos(a2) * r, y2, (float) Math.sin(a2) * r, 1.0F, 1.0F, shaderCompatibility);
            addVertex(pose, builder, red, green, blue, 1.0F, (float) Math.cos(a2) * r, y1, (float) Math.sin(a2) * r, 1.0F, 0.0F, shaderCompatibility);
        }
        poseStack.popPose();
    }

    private static void renderLightCap(PoseStack poseStack, VertexConsumer builder, float normalX, float normalY, float normalZ, int color, boolean shaderCompatibility) {
        PoseStack.Pose pose = poseStack.last();
        Vec3 normal = new Vec3(normalX, normalY, normalZ).normalize();
        Vec3 tangent = Math.abs(normal.y) > 0.8D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D).cross(normal).normalize();
        Vec3 bitangent = normal.cross(tangent).normalize();
        Vec3 center = normal.scale(DESIGNATOR_RADIUS + 0.004F);
        float red = red(color);
        float green = green(color);
        float blue = blue(color);
        for (int i = 0; i < SIDES; i++) {
            float a1 = (float) (Math.PI * 2.0D * i / SIDES);
            float a2 = (float) (Math.PI * 2.0D * (i + 1) / SIDES);
            Vec3 p1 = center.add(tangent.scale(Math.cos(a1) * LIGHT_RADIUS)).add(bitangent.scale(Math.sin(a1) * LIGHT_RADIUS));
            Vec3 p2 = center.add(tangent.scale(Math.cos(a2) * LIGHT_RADIUS)).add(bitangent.scale(Math.sin(a2) * LIGHT_RADIUS));
            addVertex(pose, builder, red, green, blue, 0.98F, (float) center.x, (float) center.y, (float) center.z, 0.5F, 0.5F, shaderCompatibility);
            addVertex(pose, builder, red, green, blue, 0.98F, (float) p1.x, (float) p1.y, (float) p1.z, 0.0F, 0.0F, shaderCompatibility);
            addVertex(pose, builder, red, green, blue, 0.98F, (float) p2.x, (float) p2.y, (float) p2.z, 1.0F, 0.0F, shaderCompatibility);
            addVertex(pose, builder, red, green, blue, 0.98F, (float) center.x, (float) center.y, (float) center.z, 0.5F, 0.5F, shaderCompatibility);
        }
    }

    private static void renderEmitterPanelsX(PoseStack poseStack, VertexConsumer builder, float x1, float x2, int color, float alpha, boolean shaderCompatibility) {
        PoseStack.Pose pose = poseStack.last();
        float red = red(color);
        float green = green(color);
        float blue = blue(color);
        float r1 = chargeRadiusAtX(x1) + 0.004F;
        float r2 = chargeRadiusAtX(x2) + 0.004F;
        for (int i = 0; i < 8; i += 2) {
            float a1 = (float) (Math.PI * 2.0D * (i + 0.18D) / 8.0D);
            float a2 = (float) (Math.PI * 2.0D * (i + 0.82D) / 8.0D);
            addCylinderQuadX(pose, builder, red, green, blue, alpha, r1, r2, x1, x2, a1, a2, shaderCompatibility);
        }
    }

    private static float chargeRadiusAtX(float x) {
        if (x < -0.095F) {
            float progress = Mth.clamp((x + 0.175F) / 0.080F, 0.0F, 1.0F);
            return Mth.lerp(progress, 0.045F, 0.100F);
        }
        if (x > 0.095F) {
            float progress = Mth.clamp((x - 0.095F) / 0.080F, 0.0F, 1.0F);
            return Mth.lerp(progress, 0.100F, 0.052F);
        }
        return 0.100F;
    }

    private static void renderPanelLinesX(PoseStack poseStack, VertexConsumer builder, boolean shaderCompatibility) {
        PoseStack.Pose pose = poseStack.last();
        for (int i = 1; i < SIDES; i += 4) {
            float a1 = (float) (Math.PI * 2.0D * i / SIDES - 0.012D);
            float a2 = (float) (Math.PI * 2.0D * i / SIDES + 0.012D);
            addCylinderQuadX(pose, builder, 0.07F, 0.08F, 0.09F, 1.0F, 0.108F, 0.108F, -0.080F, 0.080F, a1, a2, shaderCompatibility);
        }
    }

    private static void renderEnginePlume(PoseStack poseStack, VertexConsumer builder, boolean shaderCompatibility) {
        PoseStack.Pose pose = poseStack.last();
        for (int i = 0; i < SIDES; i++) {
            int next = (i + 1) % SIDES;
            float a1 = (float) (Math.PI * 2.0D * i / SIDES);
            float a2 = (float) (Math.PI * 2.0D * next / SIDES);
            addCylinderQuad(pose, builder, 0.35F, 0.88F, 1.0F, 0.40F, 0.060F, 0.000F, -0.175F, -0.355F, a1, a2, shaderCompatibility);
        }
    }

    private static void renderFrustum(PoseStack poseStack, VertexConsumer builder, float z1, float z2, float r1, float r2, int color, float alpha, boolean shaderCompatibility) {
        PoseStack.Pose pose = poseStack.last();
        float red = red(color);
        float green = green(color);
        float blue = blue(color);
        for (int i = 0; i < SIDES; i++) {
            int next = (i + 1) % SIDES;
            float a1 = (float) (Math.PI * 2.0D * i / SIDES);
            float a2 = (float) (Math.PI * 2.0D * next / SIDES);
            float shade = 0.72F + 0.28F * Math.max(0.0F, (float) Math.cos(a1 - 0.65F));
            addCylinderQuad(pose, builder, red * shade, green * shade, blue * shade, alpha, r1, r2, z1, z2, a1, a2, shaderCompatibility);
        }
    }

    private static void renderFrustumX(PoseStack poseStack, VertexConsumer builder, float x1, float x2, float r1, float r2, int color, float alpha, boolean shaderCompatibility) {
        PoseStack.Pose pose = poseStack.last();
        float red = red(color);
        float green = green(color);
        float blue = blue(color);
        for (int i = 0; i < SIDES; i++) {
            int next = (i + 1) % SIDES;
            float a1 = (float) (Math.PI * 2.0D * i / SIDES);
            float a2 = (float) (Math.PI * 2.0D * next / SIDES);
            float shade = 0.72F + 0.28F * Math.max(0.0F, (float) Math.cos(a1 - 0.65F));
            addCylinderQuadX(pose, builder, red * shade, green * shade, blue * shade, alpha, r1, r2, x1, x2, a1, a2, shaderCompatibility);
        }
    }

    private static void addCylinderQuad(PoseStack.Pose pose, VertexConsumer builder, float red, float green, float blue, float alpha, float r1, float r2, float z1, float z2, float a1, float a2, boolean shaderCompatibility) {
        addVertex(pose, builder, red, green, blue, alpha, (float) Math.cos(a1) * r1, (float) Math.sin(a1) * r1, z1, 0.0F, 0.0F, shaderCompatibility);
        addVertex(pose, builder, red, green, blue, alpha, (float) Math.cos(a1) * r2, (float) Math.sin(a1) * r2, z2, 0.0F, 1.0F, shaderCompatibility);
        addVertex(pose, builder, red, green, blue, alpha, (float) Math.cos(a2) * r2, (float) Math.sin(a2) * r2, z2, 1.0F, 1.0F, shaderCompatibility);
        addVertex(pose, builder, red, green, blue, alpha, (float) Math.cos(a2) * r1, (float) Math.sin(a2) * r1, z1, 1.0F, 0.0F, shaderCompatibility);
    }

    private static void addCylinderQuadX(PoseStack.Pose pose, VertexConsumer builder, float red, float green, float blue, float alpha, float r1, float r2, float x1, float x2, float a1, float a2, boolean shaderCompatibility) {
        addVertex(pose, builder, red, green, blue, alpha, x1, (float) Math.cos(a1) * r1, (float) Math.sin(a1) * r1, 0.0F, 0.0F, shaderCompatibility);
        addVertex(pose, builder, red, green, blue, alpha, x2, (float) Math.cos(a1) * r2, (float) Math.sin(a1) * r2, 0.0F, 1.0F, shaderCompatibility);
        addVertex(pose, builder, red, green, blue, alpha, x2, (float) Math.cos(a2) * r2, (float) Math.sin(a2) * r2, 1.0F, 1.0F, shaderCompatibility);
        addVertex(pose, builder, red, green, blue, alpha, x1, (float) Math.cos(a2) * r1, (float) Math.sin(a2) * r1, 1.0F, 0.0F, shaderCompatibility);
    }

    private static void addVertex(PoseStack.Pose pose, VertexConsumer builder, float red, float green, float blue, float alpha, float x, float y, float z, float u, float v, boolean shaderCompatibility) {
        Matrix4f matrix = pose.pose();
        if (shaderCompatibility) {
            Matrix3f normal = pose.normal();
            builder.vertex(matrix, x, y, z)
                    .color(red, green, blue, alpha)
                    .uv(u, v)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(FULLBRIGHT)
                    .normal(normal, Vector3f.YP.x(), Vector3f.YP.y(), Vector3f.YP.z())
                    .endVertex();
            return;
        }

        builder.vertex(matrix, x, y, z)
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
