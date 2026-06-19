package dev.hoyin1600p.arcanebeam.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
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

import java.util.Collection;

public class StormArrowVisualRenderer extends RenderType {
    private static final int CIRCLE_SEGMENTS = 96;
    private static final int FLASH_SEGMENTS = 28;
    private static final int IMPACT_SPARK_COUNT = 48;
    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation(ArcaneBeam.MOD_ID, "textures/entity/white.png");
    private static final RenderType NORMAL = createUnlitRenderType("storm_arrow_normal");
    private static final RenderType SHADER_SAFE = createLitRenderType("storm_arrow_shader");

    private StormArrowVisualRenderer(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static void render(PoseStack poseStack, Vec3 cameraPosition, float partialTick, Collection<? extends CircleVisual> activeStorms, Collection<? extends StrikeVisual> activeStrikes) {
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        for (CircleVisual storm : activeStorms) {
            renderStormCircle(poseStack, buffer, storm);
        }
        long gameTime = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
        for (StrikeVisual strike : activeStrikes) {
            renderBlasterStrike(poseStack, buffer, strike, gameTime, partialTick);
        }

        poseStack.popPose();
        buffer.endBatch(NORMAL);
        buffer.endBatch(SHADER_SAFE);
    }

    private static void renderStormCircle(PoseStack poseStack, MultiBufferSource buffer, CircleVisual storm) {
        StormArrowVisualManager.StormArrowRenderSettings settings = storm.settings();
        if (!settings.showTargetingCircle() || settings.circleAlpha() <= 0.001F) {
            return;
        }

        boolean shaderCompatibility = shaderCompatibility(settings);
        VertexConsumer builder = buffer.getBuffer(shaderCompatibility ? SHADER_SAFE : NORMAL);
        Vec3 center = storm.groundCenter();
        float radius = Math.max(0.01F, storm.radius());
        float innerRadius = Math.max(0.0F, radius - settings.circleThickness() * 0.5F);
        float outerRadius = radius + settings.circleThickness() * 0.5F;
        float[] rgb = rgb(settings.circleColor());

        poseStack.pushPose();
        poseStack.translate(center.x, center.y, center.z);
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            int next = (i + 1) % CIRCLE_SEGMENTS;
            float angle1 = (float) (Math.PI * 2.0D * i / CIRCLE_SEGMENTS);
            float angle2 = (float) (Math.PI * 2.0D * next / CIRCLE_SEGMENTS);
            addCircleVertex(pose, normal, builder, rgb, settings.circleAlpha(), outerRadius, angle1, 0.0F, 0.0F, shaderCompatibility, settings.fullbright());
            addCircleVertex(pose, normal, builder, rgb, settings.circleAlpha(), innerRadius, angle1, 0.0F, 1.0F, shaderCompatibility, settings.fullbright());
            addCircleVertex(pose, normal, builder, rgb, settings.circleAlpha(), innerRadius, angle2, 1.0F, 1.0F, shaderCompatibility, settings.fullbright());
            addCircleVertex(pose, normal, builder, rgb, settings.circleAlpha(), outerRadius, angle2, 1.0F, 0.0F, shaderCompatibility, settings.fullbright());
        }

        // Idona's Barrage style crosshair lines make the active AOE easier to read without particle spam.
        renderMarkerLine(poseStack, builder, rgb, settings.circleAlpha(), -radius, -radius, radius, radius, settings.circleThickness(), shaderCompatibility, settings.fullbright());
        renderMarkerLine(poseStack, builder, rgb, settings.circleAlpha(), -radius, radius, radius, -radius, settings.circleThickness(), shaderCompatibility, settings.fullbright());
        poseStack.popPose();
    }

    private static void renderMarkerLine(PoseStack stack, VertexConsumer builder, float[] rgb, float alpha, float x1, float z1, float x2, float z2, float thickness, boolean shaderCompatibility, boolean fullbright) {
        float dx = x2 - x1;
        float dz = z2 - z1;
        float length = Mth.sqrt(dx * dx + dz * dz);
        if (length <= 0.001F) {
            return;
        }

        float half = thickness * 0.35F;
        float ox = -dz / length * half;
        float oz = dx / length * half;
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        addRawVertex(pose, normal, builder, rgb, alpha, x1 + ox, 0.006F, z1 + oz, 0.0F, 0.0F, shaderCompatibility, fullbright, Vector3f.YP);
        addRawVertex(pose, normal, builder, rgb, alpha, x1 - ox, 0.006F, z1 - oz, 0.0F, 1.0F, shaderCompatibility, fullbright, Vector3f.YP);
        addRawVertex(pose, normal, builder, rgb, alpha, x2 - ox, 0.006F, z2 - oz, 1.0F, 1.0F, shaderCompatibility, fullbright, Vector3f.YP);
        addRawVertex(pose, normal, builder, rgb, alpha, x2 + ox, 0.006F, z2 + oz, 1.0F, 0.0F, shaderCompatibility, fullbright, Vector3f.YP);
    }

    private static void renderBlasterStrike(PoseStack poseStack, MultiBufferSource buffer, StrikeVisual strike, long gameTime, float partialTick) {
        StormArrowVisualManager.StormArrowRenderSettings settings = strike.settings();
        float progress = strike.progress(gameTime, partialTick);
        if (progress >= 1.0F || settings.blasterAlpha() <= 0.001F) {
            return;
        }

        boolean shaderCompatibility = shaderCompatibility(settings);
        VertexConsumer builder = buffer.getBuffer(shaderCompatibility ? SHADER_SAFE : NORMAL);
        float alpha = settings.blasterAlpha();
        Vec3 impact = strike.impact().add(0.0D, 0.08D, 0.0D);
        poseStack.pushPose();
        poseStack.translate(impact.x, impact.y, impact.z);

        // The Vault strike is instant. This keeps gameplay timing intact and only animates one visual bolt from orbit to impact.
        float originY = settings.originHeight();
        float segmentLength = Math.min(settings.segmentLength(), originY + settings.segmentLength());
        float headY = Mth.lerp(progress, originY, 0.0F);
        float bottom = Math.max(0.0F, headY);
        float top = headY + segmentLength;
        if (top > bottom) {
            renderBlasterSegment(poseStack, builder, settings, bottom, top, alpha, shaderCompatibility);
        }
        if (settings.impactFlashEnabled() && progress >= 0.72F) {
            float flashAlpha = settings.blasterAlpha() * Mth.clamp((progress - 0.72F) / 0.18F, 0.0F, 1.0F) * Mth.clamp((1.0F - progress) / 0.10F, 0.0F, 1.0F);
            renderImpactFlash(poseStack, builder, settings, flashAlpha, shaderCompatibility);
            renderImpactSparks(poseStack, builder, settings, flashAlpha, progress, impact, shaderCompatibility);
        }
        poseStack.popPose();
    }

    private static void renderBlasterSegment(PoseStack stack, VertexConsumer builder, StormArrowVisualManager.StormArrowRenderSettings settings, float bottom, float top, float alpha, boolean shaderCompatibility) {
        float glowWidth = settings.blasterWidth();
        float coreWidth = glowWidth * 0.38F;
        renderVerticalRibbons(stack, builder, rgb(settings.blasterColor()), alpha * 0.75F, glowWidth, bottom, top, shaderCompatibility, settings.fullbright());
        renderVerticalRibbons(stack, builder, rgb(settings.coreColor()), alpha, coreWidth, bottom, top, shaderCompatibility, settings.fullbright());
    }

    private static void renderVerticalRibbons(PoseStack stack, VertexConsumer builder, float[] rgb, float alpha, float width, float bottom, float top, boolean shaderCompatibility, boolean fullbright) {
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        float half = width * 0.5F;
        addRawVertex(pose, normal, builder, rgb, alpha, -half, bottom, 0.0F, 0.0F, 1.0F, shaderCompatibility, fullbright, Vector3f.ZP);
        addRawVertex(pose, normal, builder, rgb, alpha, half, bottom, 0.0F, 1.0F, 1.0F, shaderCompatibility, fullbright, Vector3f.ZP);
        addRawVertex(pose, normal, builder, rgb, alpha, half, top, 0.0F, 1.0F, 0.0F, shaderCompatibility, fullbright, Vector3f.ZP);
        addRawVertex(pose, normal, builder, rgb, alpha, -half, top, 0.0F, 0.0F, 0.0F, shaderCompatibility, fullbright, Vector3f.ZP);

        addRawVertex(pose, normal, builder, rgb, alpha, 0.0F, bottom, -half, 0.0F, 1.0F, shaderCompatibility, fullbright, Vector3f.XP);
        addRawVertex(pose, normal, builder, rgb, alpha, 0.0F, bottom, half, 1.0F, 1.0F, shaderCompatibility, fullbright, Vector3f.XP);
        addRawVertex(pose, normal, builder, rgb, alpha, 0.0F, top, half, 1.0F, 0.0F, shaderCompatibility, fullbright, Vector3f.XP);
        addRawVertex(pose, normal, builder, rgb, alpha, 0.0F, top, -half, 0.0F, 0.0F, shaderCompatibility, fullbright, Vector3f.XP);
    }

    private static void renderImpactFlash(PoseStack stack, VertexConsumer builder, StormArrowVisualManager.StormArrowRenderSettings settings, float alpha, boolean shaderCompatibility) {
        float radius = settings.impactFlashSize();
        float[] rgb = rgb(settings.impactFlashColor());
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        for (int i = 0; i < FLASH_SEGMENTS; i++) {
            int next = (i + 1) % FLASH_SEGMENTS;
            float angle1 = (float) (Math.PI * 2.0D * i / FLASH_SEGMENTS);
            float angle2 = (float) (Math.PI * 2.0D * next / FLASH_SEGMENTS);
            addRawVertex(pose, normal, builder, rgb, alpha * 0.8F, 0.0F, 0.012F, 0.0F, 0.5F, 0.5F, shaderCompatibility, settings.fullbright(), Vector3f.YP);
            addCircleVertex(pose, normal, builder, rgb, 0.0F, radius, angle2, 1.0F, 0.0F, shaderCompatibility, settings.fullbright());
            addCircleVertex(pose, normal, builder, rgb, 0.0F, radius, angle1, 0.0F, 0.0F, shaderCompatibility, settings.fullbright());
            addRawVertex(pose, normal, builder, rgb, alpha * 0.8F, 0.0F, 0.012F, 0.0F, 0.5F, 0.5F, shaderCompatibility, settings.fullbright(), Vector3f.YP);
        }
    }

    private static void renderImpactSparks(PoseStack stack, VertexConsumer builder, StormArrowVisualManager.StormArrowRenderSettings settings, float alpha, float progress, Vec3 impact, boolean shaderCompatibility) {
        if (alpha <= 0.001F) {
            return;
        }

        // These are render-only tapered quads, not particles. They appear only at the target impact point.
        float[] sparkRgb = rgb(settings.impactFlashColor());
        float[] coreRgb = rgb(settings.coreColor());
        float seedBase = (float) (impact.x * 17.37D + impact.y * 5.91D + impact.z * 23.43D);
        float impactAge = progress * Math.max(1.0F, settings.lifetimeTicks());
        float plumeScale = 4.0F;

        for (int i = 0; i < IMPACT_SPARK_COUNT; i++) {
            float seed = seedBase + i * 7.91F;
            float flicker = 0.55F + 0.45F * hash01(seed + Mth.floor(impactAge * 2.3F) * 3.11F);
            float sparkAlpha = alpha * 1.35F * flicker;
            float angle = hash01(seed + 1.0F) * ((float) Math.PI * 2.0F) + impactAge * (0.12F + hash01(seed + 2.0F) * 0.08F);
            float height = (0.10F + hash01(seed + 3.0F) * 0.12F) * plumeScale;
            float lean = (0.025F + hash01(seed + 4.0F) * 0.075F) * plumeScale;
            float baseHalfWidth = (0.010F + hash01(seed + 5.0F) * 0.009F) * plumeScale;
            float tipHalfWidth = baseHalfWidth * 0.25F;
            float tipX = Mth.cos(angle) * lean;
            float tipZ = Mth.sin(angle) * lean;
            float sideX = Mth.cos(angle + (float) Math.PI * 0.5F);
            float sideZ = Mth.sin(angle + (float) Math.PI * 0.5F);
            float[] rgb = i % 3 == 0 ? coreRgb : sparkRgb;

            renderSparkQuad(stack, builder, rgb, sparkAlpha, tipX, tipZ, height, sideX, sideZ, baseHalfWidth, tipHalfWidth, shaderCompatibility, settings.fullbright());
            renderSparkQuad(stack, builder, rgb, sparkAlpha * 0.55F, tipX * 0.72F, tipZ * 0.72F, height * 0.82F, -tipZ, tipX, baseHalfWidth * 0.65F, tipHalfWidth * 0.65F, shaderCompatibility, settings.fullbright());
        }
    }

    private static void renderSparkQuad(PoseStack stack, VertexConsumer builder, float[] rgb, float alpha, float tipX, float tipZ, float height, float sideX, float sideZ, float baseHalfWidth, float tipHalfWidth, boolean shaderCompatibility, boolean fullbright) {
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        float baseAlpha = Mth.clamp(alpha, 0.0F, 1.0F);
        addRawVertex(pose, normal, builder, rgb, baseAlpha, -sideX * baseHalfWidth, 0.012F, -sideZ * baseHalfWidth, 0.0F, 1.0F, shaderCompatibility, fullbright, Vector3f.YP);
        addRawVertex(pose, normal, builder, rgb, baseAlpha, sideX * baseHalfWidth, 0.012F, sideZ * baseHalfWidth, 1.0F, 1.0F, shaderCompatibility, fullbright, Vector3f.YP);
        addRawVertex(pose, normal, builder, rgb, 0.0F, tipX + sideX * tipHalfWidth, height, tipZ + sideZ * tipHalfWidth, 1.0F, 0.0F, shaderCompatibility, fullbright, Vector3f.YP);
        addRawVertex(pose, normal, builder, rgb, 0.0F, tipX - sideX * tipHalfWidth, height, tipZ - sideZ * tipHalfWidth, 0.0F, 0.0F, shaderCompatibility, fullbright, Vector3f.YP);
    }

    private static void addCircleVertex(Matrix4f pose, Matrix3f normal, VertexConsumer builder, float[] rgb, float alpha, float radius, float angle, float u, float v, boolean shaderCompatibility, boolean fullbright) {
        addRawVertex(pose, normal, builder, rgb, alpha, (float) Math.cos(angle) * radius, 0.0F, (float) Math.sin(angle) * radius, u, v, shaderCompatibility, fullbright, Vector3f.YP);
    }

    private static void addRawVertex(Matrix4f pose, Matrix3f normal, VertexConsumer builder, float[] rgb, float alpha, float x, float y, float z, float u, float v, boolean shaderCompatibility, boolean fullbright, Vector3f faceNormal) {
        if (shaderCompatibility) {
            builder.vertex(pose, x, y, z)
                    .color(rgb[0], rgb[1], rgb[2], alpha)
                    .uv(u, v)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(fullbright ? 15728880 : 0)
                    .normal(normal, faceNormal.x(), faceNormal.y(), faceNormal.z())
                    .endVertex();
            return;
        }
        builder.vertex(pose, x, y, z)
                .color(rgb[0], rgb[1], rgb[2], alpha)
                .uv(u, v)
                .endVertex();
    }

    private static boolean shaderCompatibility(StormArrowVisualManager.StormArrowRenderSettings settings) {
        return ArcaneBeamConfig.ShaderCompatibility.fromId(settings.shaderCompatibility()) == ArcaneBeamConfig.ShaderCompatibility.ON;
    }

    private static float[] rgb(int color) {
        return new float[]{
                ((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F
        };
    }

    private static float hash01(float value) {
        return Mth.frac(Mth.sin(value * 12.9898F) * 43758.547F);
    }

    private static RenderType createUnlitRenderType(String type) {
        CompositeState state = CompositeState.builder()
                .setShaderState(POSITION_COLOR_TEX_SHADER)
                .setTextureState(new TextureStateShard(WHITE_TEXTURE, false, false))
                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .createCompositeState(false);
        return RenderType.create("arcane_beam_" + type, DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 512, false, true, state);
    }

    private static RenderType createLitRenderType(String type) {
        CompositeState state = CompositeState.builder()
                .setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
                .setTextureState(new TextureStateShard(WHITE_TEXTURE, false, false))
                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .createCompositeState(false);
        return RenderType.create("arcane_beam_" + type, DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 512, false, true, state);
    }

    public interface CircleVisual {
        StormArrowVisualManager.StormArrowRenderSettings settings();

        Vec3 groundCenter();

        float radius();
    }

    public interface StrikeVisual {
        StormArrowVisualManager.StormArrowRenderSettings settings();

        Vec3 impact();

        float progress(long gameTime, float partialTick);
    }
}
