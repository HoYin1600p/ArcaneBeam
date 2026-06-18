package dev.hoyin1600p.arcanebeam.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class StormArrowProjectileRenderer extends RenderType {
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

    private StormArrowProjectileRenderer(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static void renderLocal(PoseStack poseStack, MultiBufferSource buffer, Entity projectile, float partialTick, String shaderCompatibility) {
        boolean shaderSafe = ArcaneBeamConfig.ShaderCompatibility.fromId(shaderCompatibility) == ArcaneBeamConfig.ShaderCompatibility.ON;
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
        poseStack.mulPose(Vector3f.YP.rotationDegrees(yaw));
        poseStack.mulPose(Vector3f.XP.rotationDegrees(pitch));
        poseStack.mulPose(Vector3f.XP.rotationDegrees(roll));
        renderDesignator(poseStack, buffer, shaderSafe);
        poseStack.popPose();
    }

    private static void renderDesignator(PoseStack poseStack, MultiBufferSource buffer, boolean shaderCompatibility) {
        VertexConsumer body = buffer.getBuffer(shaderCompatibility ? SHADER_BODY : BODY);
        VertexConsumer glow = buffer.getBuffer(shaderCompatibility ? SHADER_GLOW : GLOW);

        // Cosmetic target designator: about 15 cm across with six small emissive face lights.
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
        return RenderType.create("arcanebeam_storm_arrow_projectile_body", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 512, false, false, state);
    }

    private static RenderType createGlowRenderType() {
        CompositeState state = CompositeState.builder()
                .setShaderState(POSITION_COLOR_TEX_SHADER)
                .setTextureState(new TextureStateShard(WHITE_TEXTURE, false, false))
                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .createCompositeState(false);
        return RenderType.create("arcanebeam_storm_arrow_projectile_glow", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 512, false, true, state);
    }
}
