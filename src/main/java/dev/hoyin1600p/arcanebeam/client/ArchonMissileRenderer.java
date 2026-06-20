package dev.hoyin1600p.arcanebeam.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
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

public class ArchonMissileRenderer extends RenderType {
    private static final int SIDES = 8;
    private static final int FLASH_SEGMENTS = 24;
    private static final int FULLBRIGHT = 15728880;
    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation(ArcaneBeam.MOD_ID, "textures/entity/white.png");
    private static final RenderType BODY = createUnlitRenderType("archon_missile_body", DefaultVertexFormat.POSITION_COLOR_TEX);
    private static final RenderType GLOW = createUnlitRenderType("archon_missile_glow", DefaultVertexFormat.POSITION_COLOR_TEX);
    private static final RenderType SHADER_BODY = createLitRenderType("archon_missile_shader_body");
    private static final RenderType SHADER_GLOW = createLitRenderType("archon_missile_shader_glow");

    private ArchonMissileRenderer(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static void render(PoseStack poseStack, Vec3 cameraPosition, float partialTick, Collection<? extends MissileVisual> activeMissiles) {
        if (activeMissiles.isEmpty()) {
            return;
        }

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        long gameTime = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        for (MissileVisual missile : activeMissiles) {
            renderMissile(poseStack, buffer, missile, gameTime, partialTick);
        }

        poseStack.popPose();
        buffer.endBatch(BODY);
        buffer.endBatch(GLOW);
        buffer.endBatch(SHADER_BODY);
        buffer.endBatch(SHADER_GLOW);
    }

    private static void renderMissile(PoseStack poseStack, MultiBufferSource buffer, MissileVisual missile, long gameTime, float partialTick) {
        StormArrowVisualManager.StormArrowRenderSettings settings = missile.settings();
        float progress = missile.progress(gameTime, partialTick);
        if (progress >= 1.0F) {
            return;
        }

        boolean shaderCompatibility = ArcaneBeamConfig.ShaderCompatibility.fromId(settings.shaderCompatibility()) == ArcaneBeamConfig.ShaderCompatibility.ON;
        VertexConsumer bodyBuilder = buffer.getBuffer(shaderCompatibility ? SHADER_BODY : BODY);
        VertexConsumer glowBuilder = buffer.getBuffer(shaderCompatibility ? SHADER_GLOW : GLOW);
        Vec3 impact = missile.impact().add(0.0D, 0.42D, 0.0D);
        Vec3 origin = missile.origin();
        Vec3 current = missilePosition(origin, impact, progress);
        Vec3 previous = missilePosition(origin, impact, Math.max(0.0F, progress - 0.035F));
        Vec3 direction = current.subtract(previous);
        if (direction.lengthSqr() < 1.0E-5D) {
            direction = impact.subtract(origin);
        }

        poseStack.pushPose();
        poseStack.translate(current.x, current.y, current.z);
        alignLocalNegativeYToDirection(poseStack, direction);
        renderBody(poseStack, bodyBuilder, shaderCompatibility, settings.fullbright());
        renderPlume(poseStack, glowBuilder, settings, shaderCompatibility);
        poseStack.popPose();

        if (settings.impactFlashEnabled() && progress >= 0.72F) {
            float flashAlpha = settings.blasterAlpha()
                    * Mth.clamp((progress - 0.72F) / 0.18F, 0.0F, 1.0F)
                    * Mth.clamp((1.0F - progress) / 0.10F, 0.0F, 1.0F);
            poseStack.pushPose();
            poseStack.translate(impact.x, impact.y, impact.z);
            renderImpactFlash(poseStack, glowBuilder, settings, flashAlpha, shaderCompatibility);
            poseStack.popPose();
        }
    }

    private static Vec3 missilePosition(Vec3 origin, Vec3 impact, float progress) {
        float t = Mth.clamp(progress, 0.0F, 1.0F);
        Vec3 control = missileControlPoint(origin, impact);
        double inv = 1.0D - t;
        return origin.scale(inv * inv)
                .add(control.scale(2.0D * inv * t))
                .add(impact.scale(t * t));
    }

    private static Vec3 missileControlPoint(Vec3 origin, Vec3 impact) {
        float seed = (float) (impact.x * 17.37D + impact.y * 5.91D + impact.z * 23.43D);
        Vec3 delta = impact.subtract(origin);
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
        double horizontalDistance = Math.max(0.1D, horizontal.length());
        Vec3 perpendicular = horizontal.lengthSqr() < 1.0E-5D
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : new Vec3(-horizontal.z, 0.0D, horizontal.x).normalize();
        double side = hash01(seed) < 0.5F ? -1.0D : 1.0D;
        double sweep = Mth.clamp((float) (horizontalDistance * 0.45D), 0.35F, 2.5F);
        double arch = Mth.clamp((float) (horizontalDistance * 0.30D), 0.50F, 3.0F);
        Vec3 midpoint = origin.add(impact).scale(0.5D);
        return new Vec3(
                midpoint.x + perpendicular.x * side * sweep,
                Math.max(origin.y, impact.y) + arch,
                midpoint.z + perpendicular.z * side * sweep
        );
    }

    private static void alignLocalNegativeYToDirection(PoseStack poseStack, Vec3 direction) {
        if (direction.lengthSqr() < 1.0E-5D) {
            return;
        }
        Vec3 target = direction.normalize();
        Vec3 localForward = new Vec3(0.0D, -1.0D, 0.0D);
        double dot = Mth.clamp((float) localForward.dot(target), -1.0F, 1.0F);
        Vec3 axis = localForward.cross(target);
        if (axis.lengthSqr() < 1.0E-5D) {
            if (dot < 0.0D) {
                poseStack.mulPose(Vector3f.XP.rotationDegrees(180.0F));
            }
            return;
        }
        Vec3 normalizedAxis = axis.normalize();
        poseStack.mulPose(new Quaternion(new Vector3f((float) normalizedAxis.x, (float) normalizedAxis.y, (float) normalizedAxis.z), (float) Math.acos(dot), false));
    }

    private static void renderBody(PoseStack stack, VertexConsumer builder, boolean shaderCompatibility, boolean fullbright) {
        float radius = 0.055F;
        float halfLength = 0.16F;
        renderCylinder(stack, builder, radius, -halfLength, halfLength, 0x4A5056, shaderCompatibility, fullbright);
        renderCone(stack, builder, radius * 0.95F, -halfLength - 0.12F, -halfLength, 0x747C84, shaderCompatibility, fullbright);
        renderCone(stack, builder, radius * 0.90F, halfLength, halfLength + 0.08F, 0x343A40, shaderCompatibility, fullbright);
        renderCylinder(stack, builder, radius * 1.08F, -0.02F, 0.02F, 0x22262A, shaderCompatibility, fullbright);
    }

    private static void renderCylinder(PoseStack stack, VertexConsumer builder, float radius, float minY, float maxY, int color, boolean shaderCompatibility, boolean fullbright) {
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        float[] rgb = rgb(color);
        for (int i = 0; i < SIDES; i++) {
            int next = (i + 1) % SIDES;
            float a1 = (float) (Math.PI * 2.0D * i / SIDES);
            float a2 = (float) (Math.PI * 2.0D * next / SIDES);
            addRawVertex(pose, normal, builder, rgb, 1.0F, Mth.cos(a1) * radius, minY, Mth.sin(a1) * radius, 0.0F, 1.0F, shaderCompatibility, fullbright, Vector3f.YP);
            addRawVertex(pose, normal, builder, rgb, 1.0F, Mth.cos(a2) * radius, minY, Mth.sin(a2) * radius, 1.0F, 1.0F, shaderCompatibility, fullbright, Vector3f.YP);
            addRawVertex(pose, normal, builder, rgb, 1.0F, Mth.cos(a2) * radius, maxY, Mth.sin(a2) * radius, 1.0F, 0.0F, shaderCompatibility, fullbright, Vector3f.YP);
            addRawVertex(pose, normal, builder, rgb, 1.0F, Mth.cos(a1) * radius, maxY, Mth.sin(a1) * radius, 0.0F, 0.0F, shaderCompatibility, fullbright, Vector3f.YP);
        }
    }

    private static void renderCone(PoseStack stack, VertexConsumer builder, float radius, float tipY, float baseY, int color, boolean shaderCompatibility, boolean fullbright) {
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        float[] rgb = rgb(color);
        for (int i = 0; i < SIDES; i++) {
            int next = (i + 1) % SIDES;
            float a1 = (float) (Math.PI * 2.0D * i / SIDES);
            float a2 = (float) (Math.PI * 2.0D * next / SIDES);
            addRawVertex(pose, normal, builder, rgb, 1.0F, 0.0F, tipY, 0.0F, 0.5F, 0.0F, shaderCompatibility, fullbright, Vector3f.YP);
            addRawVertex(pose, normal, builder, rgb, 1.0F, Mth.cos(a1) * radius, baseY, Mth.sin(a1) * radius, 0.0F, 1.0F, shaderCompatibility, fullbright, Vector3f.YP);
            addRawVertex(pose, normal, builder, rgb, 1.0F, Mth.cos(a2) * radius, baseY, Mth.sin(a2) * radius, 1.0F, 1.0F, shaderCompatibility, fullbright, Vector3f.YP);
            addRawVertex(pose, normal, builder, rgb, 1.0F, 0.0F, tipY, 0.0F, 0.5F, 0.0F, shaderCompatibility, fullbright, Vector3f.YP);
        }
    }

    private static void renderPlume(PoseStack stack, VertexConsumer builder, StormArrowVisualManager.StormArrowRenderSettings settings, boolean shaderCompatibility) {
        float alpha = settings.blasterAlpha();
        if (alpha <= 0.001F) {
            return;
        }

        float plumeLength = Math.max(0.12F, Math.min(settings.segmentLength(), 3.0F));
        float glowWidth = Math.max(0.015F, settings.blasterWidth());
        float coreWidth = glowWidth * 0.35F;
        renderVerticalPlume(stack, builder, rgb(settings.blasterColor()), alpha * 0.75F, glowWidth, 0.12F, 0.12F + plumeLength, shaderCompatibility, settings.fullbright());
        renderVerticalPlume(stack, builder, rgb(settings.coreColor()), alpha, coreWidth, 0.12F, 0.12F + plumeLength * 0.55F, shaderCompatibility, settings.fullbright());
    }

    private static void renderVerticalPlume(PoseStack stack, VertexConsumer builder, float[] rgb, float alpha, float width, float bottom, float top, boolean shaderCompatibility, boolean fullbright) {
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        float half = width * 0.5F;
        addRawVertex(pose, normal, builder, rgb, alpha, -half, bottom, 0.0F, 0.0F, 1.0F, shaderCompatibility, fullbright, Vector3f.ZP);
        addRawVertex(pose, normal, builder, rgb, alpha, half, bottom, 0.0F, 1.0F, 1.0F, shaderCompatibility, fullbright, Vector3f.ZP);
        addRawVertex(pose, normal, builder, rgb, 0.0F, half * 0.35F, top, 0.0F, 1.0F, 0.0F, shaderCompatibility, fullbright, Vector3f.ZP);
        addRawVertex(pose, normal, builder, rgb, 0.0F, -half * 0.35F, top, 0.0F, 0.0F, 0.0F, shaderCompatibility, fullbright, Vector3f.ZP);

        addRawVertex(pose, normal, builder, rgb, alpha, 0.0F, bottom, -half, 0.0F, 1.0F, shaderCompatibility, fullbright, Vector3f.XP);
        addRawVertex(pose, normal, builder, rgb, alpha, 0.0F, bottom, half, 1.0F, 1.0F, shaderCompatibility, fullbright, Vector3f.XP);
        addRawVertex(pose, normal, builder, rgb, 0.0F, 0.0F, top, half * 0.35F, 1.0F, 0.0F, shaderCompatibility, fullbright, Vector3f.XP);
        addRawVertex(pose, normal, builder, rgb, 0.0F, 0.0F, top, -half * 0.35F, 0.0F, 0.0F, shaderCompatibility, fullbright, Vector3f.XP);
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
            addRawVertex(pose, normal, builder, rgb, 0.0F, Mth.cos(angle2) * radius, 0.012F, Mth.sin(angle2) * radius, 1.0F, 0.0F, shaderCompatibility, settings.fullbright(), Vector3f.YP);
            addRawVertex(pose, normal, builder, rgb, 0.0F, Mth.cos(angle1) * radius, 0.012F, Mth.sin(angle1) * radius, 0.0F, 0.0F, shaderCompatibility, settings.fullbright(), Vector3f.YP);
            addRawVertex(pose, normal, builder, rgb, alpha * 0.8F, 0.0F, 0.012F, 0.0F, 0.5F, 0.5F, shaderCompatibility, settings.fullbright(), Vector3f.YP);
        }
    }

    private static void addRawVertex(Matrix4f pose, Matrix3f normal, VertexConsumer builder, float[] rgb, float alpha, float x, float y, float z, float u, float v, boolean shaderCompatibility, boolean fullbright, Vector3f faceNormal) {
        if (shaderCompatibility) {
            builder.vertex(pose, x, y, z)
                    .color(rgb[0], rgb[1], rgb[2], alpha)
                    .uv(u, v)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(fullbright ? FULLBRIGHT : 0)
                    .normal(normal, faceNormal.x(), faceNormal.y(), faceNormal.z())
                    .endVertex();
            return;
        }
        builder.vertex(pose, x, y, z)
                .color(rgb[0], rgb[1], rgb[2], alpha)
                .uv(u, v)
                .endVertex();
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

    private static RenderType createUnlitRenderType(String type, VertexFormat format) {
        CompositeState state = CompositeState.builder()
                .setShaderState(POSITION_COLOR_TEX_SHADER)
                .setTextureState(new TextureStateShard(WHITE_TEXTURE, false, false))
                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .createCompositeState(false);
        return RenderType.create("arcane_beam_" + type, format, VertexFormat.Mode.QUADS, 512, false, true, state);
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

    public interface MissileVisual {
        StormArrowVisualManager.StormArrowRenderSettings settings();

        Vec3 origin();

        Vec3 impact();

        float progress(long gameTime, float partialTick);
    }
}
