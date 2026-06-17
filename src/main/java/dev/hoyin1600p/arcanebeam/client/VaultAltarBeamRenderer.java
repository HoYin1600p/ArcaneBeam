package dev.hoyin1600p.arcanebeam.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

public class VaultAltarBeamRenderer extends RenderType {
    private static final int CYLINDER_SIDES = 8;
    private static final int SPARK_COUNT = 7;
    private static final double ALTAR_TOP_OFFSET = 17.25D / 16.0D;
    private static final double CORNER_SOURCE_GAP = 3.0D;
    private static final ResourceLocation LOOT_BEAM_TEXTURE = new ResourceLocation(ArcaneBeam.MOD_ID, "textures/entity/loot_beam.png");
    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation(ArcaneBeam.MOD_ID, "textures/entity/white.png");
    private static final RenderType DEFAULT_BEAM = createUnlitRenderType("vault_altar_default", LOOT_BEAM_TEXTURE);
    private static final RenderType SOLID_BEAM = createUnlitRenderType("vault_altar_solid", WHITE_TEXTURE);
    private static final RenderType SHADER_DEFAULT_BEAM = createLitRenderType("vault_altar_shader_default", LOOT_BEAM_TEXTURE);
    private static final RenderType SHADER_SOLID_BEAM = createLitRenderType("vault_altar_shader_solid", WHITE_TEXTURE);

    private VaultAltarBeamRenderer(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static void render(PoseStack poseStack, Vec3 cameraPosition, float partialTick, Collection<VaultAltarBeamManager.ActiveAltarBeam> activeBeams) {
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        for (VaultAltarBeamManager.ActiveAltarBeam activeBeam : activeBeams) {
            renderAltar(poseStack, buffer, activeBeam, partialTick);
        }
        poseStack.popPose();

        buffer.endBatch(DEFAULT_BEAM);
        buffer.endBatch(SOLID_BEAM);
        buffer.endBatch(SHADER_DEFAULT_BEAM);
        buffer.endBatch(SHADER_SOLID_BEAM);
    }

    private static void renderAltar(PoseStack poseStack, MultiBufferSource buffer, VaultAltarBeamManager.ActiveAltarBeam activeBeam, float partialTick) {
        VaultAltarBeamManager.VaultAltarRenderSettings settings = activeBeam.settings();
        float age = activeBeam.age(partialTick);
        if (age < 0.0F) {
            return;
        }

        boolean shaderCompatibility = ArcaneBeamConfig.ShaderCompatibility.fromId(settings.shaderCompatibility()) == ArcaneBeamConfig.ShaderCompatibility.ON;
        VertexConsumer main = buffer.getBuffer(shaderCompatibility ? SHADER_DEFAULT_BEAM : DEFAULT_BEAM);
        VertexConsumer solid = buffer.getBuffer(shaderCompatibility ? SHADER_SOLID_BEAM : SOLID_BEAM);
        BlockPos pos = activeBeam.pos();
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        double topY = y + ALTAR_TOP_OFFSET;
        double sourceY = topY + CORNER_SOURCE_GAP;

        int cornerColor = animatedColor(settings.cornerColors(), age);
        float[] cornerRgb = rgb(cornerColor);
        float cornerAlpha = settings.cornerOpacity();
        float cornerRadius = settings.cornerRadius();
        float convergeProgress = convergenceProgress(age, settings);

        Vec3 center = new Vec3(x + 0.5D, topY, z + 0.5D);
        renderCornerBeamWithSparks(poseStack, main, solid, new Vec3(x, sourceY, z), new Vec3(x, topY, z), center, convergeProgress, cornerRgb, cornerAlpha, cornerRadius, age, 0, shaderCompatibility);
        renderCornerBeamWithSparks(poseStack, main, solid, new Vec3(x + 1.0D, sourceY, z), new Vec3(x + 1.0D, topY, z), center, convergeProgress, cornerRgb, cornerAlpha, cornerRadius, age, 1, shaderCompatibility);
        renderCornerBeamWithSparks(poseStack, main, solid, new Vec3(x, sourceY, z + 1.0D), new Vec3(x, topY, z + 1.0D), center, convergeProgress, cornerRgb, cornerAlpha, cornerRadius, age, 2, shaderCompatibility);
        renderCornerBeamWithSparks(poseStack, main, solid, new Vec3(x + 1.0D, sourceY, z + 1.0D), new Vec3(x + 1.0D, topY, z + 1.0D), center, convergeProgress, cornerRgb, cornerAlpha, cornerRadius, age, 3, shaderCompatibility);

        float centerGrowth = centerGrowthProgress(age, settings);
        if (centerGrowth > 0.0F) {
            renderCenterBeam(poseStack, main, solid, center, settings, age, centerGrowth, shaderCompatibility);
        }
    }

    private static void renderCornerBeamWithSparks(PoseStack poseStack, VertexConsumer main, VertexConsumer solid, Vec3 start, Vec3 verticalEnd, Vec3 centerEnd, float convergeProgress, float[] rgb, float alpha, float radius, float age, int cornerIndex, boolean shaderCompatibility) {
        Vec3 contact = verticalEnd.lerp(centerEnd, convergeProgress);
        renderCornerBeam(poseStack, main, solid, start, verticalEnd, centerEnd, convergeProgress, rgb, alpha, radius, shaderCompatibility);
        renderSparkPlume(poseStack, solid, contact, cornerIndex, age, rgb, alpha, shaderCompatibility);
    }

    private static void renderCornerBeam(PoseStack poseStack, VertexConsumer main, VertexConsumer solid, Vec3 start, Vec3 verticalEnd, Vec3 centerEnd, float convergeProgress, float[] rgb, float alpha, float radius, boolean shaderCompatibility) {
        Vec3 end = verticalEnd.lerp(centerEnd, convergeProgress);
        Vec3 direction = end.subtract(start);
        float height = (float) direction.length();
        if (height <= 0.001F || alpha <= 0.001F || radius <= 0.0005F) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(start.x, start.y, start.z);
        rotateYAxisToDirection(poseStack, direction.normalize());
        renderTube(poseStack, main, rgb[0], rgb[1], rgb[2], alpha, 0.0F, height, radius, radius, shaderCompatibility);
        renderTube(poseStack, solid, rgb[0], rgb[1], rgb[2], alpha, 0.0F, height, radius * 0.35F, radius * 0.35F, shaderCompatibility);
        poseStack.popPose();
    }

    private static void renderSparkPlume(PoseStack poseStack, VertexConsumer builder, Vec3 contact, int cornerIndex, float age, float[] rgb, float alpha, boolean shaderCompatibility) {
        if (alpha <= 0.001F) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(contact.x, contact.y + 0.006D, contact.z);
        for (int i = 0; i < SPARK_COUNT; i++) {
            float seed = cornerIndex * 13.37F + i * 7.91F;
            float flicker = 0.45F + 0.55F * hash01(seed + Mth.floor(age * 1.7F) * 3.11F);
            float sparkAlpha = alpha * 0.72F * flicker;
            float angle = hash01(seed + 1.0F) * ((float) Math.PI * 2.0F) + age * (0.09F + hash01(seed + 2.0F) * 0.06F);
            float height = 0.055F + hash01(seed + 3.0F) * 0.095F;
            float lean = 0.015F + hash01(seed + 4.0F) * 0.045F;
            float baseHalfWidth = 0.006F + hash01(seed + 5.0F) * 0.004F;
            float tipHalfWidth = baseHalfWidth * 0.25F;
            float tipX = Mth.cos(angle) * lean;
            float tipZ = Mth.sin(angle) * lean;
            float sideX = Mth.cos(angle + (float) Math.PI * 0.5F);
            float sideZ = Mth.sin(angle + (float) Math.PI * 0.5F);

            renderSparkQuad(poseStack, builder, rgb, sparkAlpha, tipX, tipZ, height, sideX, sideZ, baseHalfWidth, tipHalfWidth, shaderCompatibility);
            renderSparkQuad(poseStack, builder, rgb, sparkAlpha * 0.55F, tipX * 0.72F, tipZ * 0.72F, height * 0.82F, -tipZ, tipX, baseHalfWidth * 0.65F, tipHalfWidth * 0.65F, shaderCompatibility);
        }
        poseStack.popPose();
    }

    private static void renderSparkQuad(PoseStack stack, VertexConsumer builder, float[] rgb, float alpha, float tipX, float tipZ, float height, float sideX, float sideZ, float baseHalfWidth, float tipHalfWidth, boolean shaderCompatibility) {
        Matrix4f matrixPose = stack.last().pose();
        float baseAlpha = Mth.clamp(alpha, 0.0F, 1.0F);
        if (shaderCompatibility) {
            Vector3f normal = new Vector3f(0.0F, 1.0F, 0.0F);
            addLitPositionVertex(matrixPose, stack.last().normal(), builder, rgb[0], rgb[1], rgb[2], baseAlpha, -sideX * baseHalfWidth, 0.0F, -sideZ * baseHalfWidth, 0.0F, 1.0F, normal);
            addLitPositionVertex(matrixPose, stack.last().normal(), builder, rgb[0], rgb[1], rgb[2], baseAlpha, sideX * baseHalfWidth, 0.0F, sideZ * baseHalfWidth, 1.0F, 1.0F, normal);
            addLitPositionVertex(matrixPose, stack.last().normal(), builder, rgb[0], rgb[1], rgb[2], 0.0F, tipX + sideX * tipHalfWidth, height, tipZ + sideZ * tipHalfWidth, 1.0F, 0.0F, normal);
            addLitPositionVertex(matrixPose, stack.last().normal(), builder, rgb[0], rgb[1], rgb[2], 0.0F, tipX - sideX * tipHalfWidth, height, tipZ - sideZ * tipHalfWidth, 0.0F, 0.0F, normal);
            return;
        }

        addPositionVertex(matrixPose, builder, rgb[0], rgb[1], rgb[2], baseAlpha, -sideX * baseHalfWidth, 0.0F, -sideZ * baseHalfWidth, 0.0F, 1.0F);
        addPositionVertex(matrixPose, builder, rgb[0], rgb[1], rgb[2], baseAlpha, sideX * baseHalfWidth, 0.0F, sideZ * baseHalfWidth, 1.0F, 1.0F);
        addPositionVertex(matrixPose, builder, rgb[0], rgb[1], rgb[2], 0.0F, tipX + sideX * tipHalfWidth, height, tipZ + sideZ * tipHalfWidth, 1.0F, 0.0F);
        addPositionVertex(matrixPose, builder, rgb[0], rgb[1], rgb[2], 0.0F, tipX - sideX * tipHalfWidth, height, tipZ - sideZ * tipHalfWidth, 0.0F, 0.0F);
    }

    private static void renderCenterBeam(PoseStack poseStack, VertexConsumer main, VertexConsumer solid, Vec3 center, VaultAltarBeamManager.VaultAltarRenderSettings settings, float age, float growProgress, boolean shaderCompatibility) {
        int glowColor = animatedColor(settings.centerGlowColors(), age);
        float[] glowRgb = rgb(glowColor);
        if (settings.centerGlowOpacity() > 0.001F) {
            poseStack.pushPose();
            poseStack.translate(center.x, center.y, center.z);
            applyGlowRotation(poseStack, settings, age);
            renderTaperedFadeTube(poseStack, main, glowRgb, settings.centerGlowOpacity(), settings.centerGlowHeight() * growProgress, settings.centerGlowFadeHeight() * growProgress, settings.centerGlowBottomRadius(), settings.centerGlowTopRadius(), shaderCompatibility);
            poseStack.popPose();
        }

        int centerColor = animatedColor(settings.centerColors(), age);
        float[] centerRgb = rgb(centerColor);
        if (settings.centerOpacity() > 0.001F) {
            poseStack.pushPose();
            poseStack.translate(center.x, center.y, center.z);
            renderTaperedFadeTube(poseStack, main, centerRgb, settings.centerOpacity(), settings.centerHeight() * growProgress, settings.centerFadeHeight() * growProgress, settings.centerBottomRadius(), settings.centerTopRadius(), shaderCompatibility);
            renderTaperedFadeTube(poseStack, solid, centerRgb, settings.centerOpacity(), settings.centerHeight() * growProgress, settings.centerFadeHeight() * growProgress, settings.centerBottomRadius() * 0.35F, settings.centerTopRadius() * 0.35F, shaderCompatibility);
            poseStack.popPose();
        }
    }

    private static void renderTaperedFadeTube(PoseStack poseStack, VertexConsumer builder, float[] rgb, float bottomAlpha, float height, float fadeHeight, float bottomRadius, float topRadius, boolean shaderCompatibility) {
        float clampedHeight = Mth.clamp(height, 0.0F, 3.0F);
        float clampedFadeHeight = Mth.clamp(fadeHeight, 0.001F, 3.0F);
        float visibleHeight = Math.min(clampedHeight, clampedFadeHeight);
        float radiusProgress = clampedHeight <= 0.001F ? 1.0F : visibleHeight / clampedHeight;
        float visibleTopRadius = Mth.lerp(radiusProgress, bottomRadius, topRadius);
        renderTube(poseStack, builder, rgb[0], rgb[1], rgb[2], bottomAlpha, 0.0F, visibleHeight, bottomRadius, visibleTopRadius, shaderCompatibility);
    }

    private static void renderTube(PoseStack stack, VertexConsumer builder, float red, float green, float blue, float bottomAlpha, float topAlpha, float height, float bottomRadius, float topRadius, boolean shaderCompatibility) {
        if (shaderCompatibility) {
            renderLitTube(stack, builder, red, green, blue, bottomAlpha, topAlpha, height, bottomRadius, topRadius);
        } else {
            renderUnlitTube(stack, builder, red, green, blue, bottomAlpha, topAlpha, height, bottomRadius, topRadius);
        }
    }

    private static void renderUnlitTube(PoseStack stack, VertexConsumer builder, float red, float green, float blue, float bottomAlpha, float topAlpha, float height, float bottomRadius, float topRadius) {
        Matrix4f matrixPose = stack.last().pose();
        for (int i = 0; i < CYLINDER_SIDES; i++) {
            int next = (i + 1) % CYLINDER_SIDES;
            float angle1 = (float) (Math.PI * 2.0D * i / CYLINDER_SIDES);
            float angle2 = (float) (Math.PI * 2.0D * next / CYLINDER_SIDES);
            float bottomX1 = (float) Math.sin(angle1) * bottomRadius;
            float bottomZ1 = (float) Math.cos(angle1) * bottomRadius;
            float bottomX2 = (float) Math.sin(angle2) * bottomRadius;
            float bottomZ2 = (float) Math.cos(angle2) * bottomRadius;
            float topX1 = (float) Math.sin(angle1) * topRadius;
            float topZ1 = (float) Math.cos(angle1) * topRadius;
            float topX2 = (float) Math.sin(angle2) * topRadius;
            float topZ2 = (float) Math.cos(angle2) * topRadius;
            float u1 = i / (float) CYLINDER_SIDES;
            float u2 = next / (float) CYLINDER_SIDES;
            addVertex(matrixPose, builder, red, green, blue, topAlpha, height, topX1, topZ1, u1, 0.0F);
            addVertex(matrixPose, builder, red, green, blue, bottomAlpha, 0.0F, bottomX1, bottomZ1, u1, 1.0F);
            addVertex(matrixPose, builder, red, green, blue, bottomAlpha, 0.0F, bottomX2, bottomZ2, u2, 1.0F);
            addVertex(matrixPose, builder, red, green, blue, topAlpha, height, topX2, topZ2, u2, 0.0F);
        }
    }

    private static void renderLitTube(PoseStack stack, VertexConsumer builder, float red, float green, float blue, float bottomAlpha, float topAlpha, float height, float bottomRadius, float topRadius) {
        PoseStack.Pose pose = stack.last();
        Matrix4f matrixPose = pose.pose();
        for (int i = 0; i < CYLINDER_SIDES; i++) {
            int next = (i + 1) % CYLINDER_SIDES;
            float angle1 = (float) (Math.PI * 2.0D * i / CYLINDER_SIDES);
            float angle2 = (float) (Math.PI * 2.0D * next / CYLINDER_SIDES);
            float bottomX1 = (float) Math.sin(angle1) * bottomRadius;
            float bottomZ1 = (float) Math.cos(angle1) * bottomRadius;
            float bottomX2 = (float) Math.sin(angle2) * bottomRadius;
            float bottomZ2 = (float) Math.cos(angle2) * bottomRadius;
            float topX1 = (float) Math.sin(angle1) * topRadius;
            float topZ1 = (float) Math.cos(angle1) * topRadius;
            float topX2 = (float) Math.sin(angle2) * topRadius;
            float topZ2 = (float) Math.cos(angle2) * topRadius;
            float u1 = i / (float) CYLINDER_SIDES;
            float u2 = next / (float) CYLINDER_SIDES;
            Vector3f faceNormal = faceNormal(bottomX1, bottomZ1, bottomX2, bottomZ2);
            addLitVertex(matrixPose, pose.normal(), builder, red, green, blue, topAlpha, height, topX1, topZ1, u1, 0.0F, faceNormal);
            addLitVertex(matrixPose, pose.normal(), builder, red, green, blue, bottomAlpha, 0.0F, bottomX1, bottomZ1, u1, 1.0F, faceNormal);
            addLitVertex(matrixPose, pose.normal(), builder, red, green, blue, bottomAlpha, 0.0F, bottomX2, bottomZ2, u2, 1.0F, faceNormal);
            addLitVertex(matrixPose, pose.normal(), builder, red, green, blue, topAlpha, height, topX2, topZ2, u2, 0.0F, faceNormal);
        }
    }

    private static float convergenceProgress(float age, VaultAltarBeamManager.VaultAltarRenderSettings settings) {
        if (age <= settings.cornerVerticalTicks()) {
            return 0.0F;
        }
        return Mth.clamp((age - settings.cornerVerticalTicks()) / Math.max(1.0F, settings.cornerConvergeTicks()), 0.0F, 1.0F);
    }

    private static float centerGrowthProgress(float age, VaultAltarBeamManager.VaultAltarRenderSettings settings) {
        float startAge = settings.cornerVerticalTicks() + settings.cornerConvergeTicks();
        if (age <= startAge) {
            return 0.0F;
        }
        return Mth.clamp((age - startAge) / Math.max(1.0F, settings.centerGrowTicks()), 0.0F, 1.0F);
    }

    private static int animatedColor(int[] colors, float age) {
        if (colors == null || colors.length == 0) {
            return 0xFFFFFF;
        }
        if (colors.length == 1) {
            return colors[0];
        }
        float wave = (Mth.sin(age * 0.18F) + 1.0F) * 0.5F;
        return lerpColor(colors[0], colors[1], wave);
    }

    private static float hash01(float value) {
        return Mth.frac(Mth.sin(value * 12.9898F) * 43758.547F);
    }

    private static void applyGlowRotation(PoseStack poseStack, VaultAltarBeamManager.VaultAltarRenderSettings settings, float age) {
        if (settings.centerGlowRotationRpm() <= 0.0F) {
            return;
        }
        float degreesPerTick = settings.centerGlowRotationRpm() * 0.3F;
        poseStack.mulPose(Vector3f.YP.rotationDegrees(age * degreesPerTick));
    }

    private static void rotateYAxisToDirection(PoseStack poseStack, Vec3 direction) {
        Vector3f from = Vector3f.YP.copy();
        Vector3f to = new Vector3f(direction);
        if (!to.normalize()) {
            return;
        }

        float dot = from.dot(to);
        if (dot > 0.9999F) {
            return;
        }
        if (dot < -0.9999F) {
            poseStack.mulPose(Vector3f.XP.rotationDegrees(180.0F));
            return;
        }

        Vector3f axis = from.copy();
        axis.cross(to);
        axis.normalize();
        float angle = (float) Math.acos(dot);
        poseStack.mulPose(new Quaternion(axis, angle, false));
    }

    private static void addVertex(Matrix4f pose, VertexConsumer builder, float red, float green, float blue, float alpha, float y, float x, float z, float u, float v) {
        builder.vertex(pose, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .endVertex();
    }

    private static void addPositionVertex(Matrix4f pose, VertexConsumer builder, float red, float green, float blue, float alpha, float x, float y, float z, float u, float v) {
        builder.vertex(pose, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .endVertex();
    }

    private static void addLitVertex(Matrix4f pose, com.mojang.math.Matrix3f normalMatrix, VertexConsumer builder, float red, float green, float blue, float alpha, float y, float x, float z, float u, float v, Vector3f faceNormal) {
        builder.vertex(pose, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(normalMatrix, faceNormal.x(), faceNormal.y(), faceNormal.z())
                .endVertex();
    }

    private static void addLitPositionVertex(Matrix4f pose, com.mojang.math.Matrix3f normalMatrix, VertexConsumer builder, float red, float green, float blue, float alpha, float x, float y, float z, float u, float v, Vector3f faceNormal) {
        builder.vertex(pose, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(normalMatrix, faceNormal.x(), faceNormal.y(), faceNormal.z())
                .endVertex();
    }

    private static Vector3f faceNormal(float x1, float z1, float x2, float z2) {
        Vector3f edge = new Vector3f(x2 - x1, 0.0F, z2 - z1);
        Vector3f up = Vector3f.YP.copy();
        edge.cross(up);
        if (!edge.normalize()) {
            return new Vector3f(0.0F, 0.0F, 1.0F);
        }
        return edge;
    }

    private static float[] rgb(int color) {
        return new float[]{
                ((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F
        };
    }

    private static int lerpColor(int first, int second, float progress) {
        int r1 = (first >> 16) & 0xFF;
        int g1 = (first >> 8) & 0xFF;
        int b1 = first & 0xFF;
        int r2 = (second >> 16) & 0xFF;
        int g2 = (second >> 8) & 0xFF;
        int b2 = second & 0xFF;
        int r = (int) (r1 + (r2 - r1) * progress);
        int g = (int) (g1 + (g2 - g1) * progress);
        int b = (int) (b1 + (b2 - b1) * progress);
        return (r << 16) | (g << 8) | b;
    }

    private static RenderType createUnlitRenderType(String type, ResourceLocation texture) {
        CompositeState state = CompositeState.builder()
                .setShaderState(POSITION_COLOR_TEX_SHADER)
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .createCompositeState(false);
        return RenderType.create("arcane_beam_" + type, DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 256, false, true, state);
    }

    private static RenderType createLitRenderType(String type, ResourceLocation texture) {
        CompositeState state = CompositeState.builder()
                .setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .createCompositeState(false);
        return RenderType.create("arcane_beam_" + type, DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 256, false, true, state);
    }
}
