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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

public class ArcaneBeamRenderer extends RenderType {
    private static final ResourceLocation LOOT_BEAM_TEXTURE = new ResourceLocation(ArcaneBeam.MOD_ID, "textures/entity/loot_beam.png");
    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation(ArcaneBeam.MOD_ID, "textures/entity/white.png");
    private static final RenderType DEFAULT_BEAM = createUnlitBeamRenderType("default", LOOT_BEAM_TEXTURE);
    private static final RenderType SHADER_DEFAULT_BEAM = createLitBeamRenderType("shader_default", LOOT_BEAM_TEXTURE);
    private static final RenderType SHADER_SOLID_BEAM = createLitBeamRenderType("shader_solid", WHITE_TEXTURE);
    private static final RenderType SOLID_BEAM = createCoreRenderType("solid", WHITE_TEXTURE);

    private ArcaneBeamRenderer(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static void render(PoseStack poseStack, Vec3 cameraPosition, float partialTick, Collection<ArcaneBeamManager.ActiveBeam> activeBeams) {
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        for (ArcaneBeamManager.ActiveBeam activeBeam : activeBeams) {
            ArcaneBeamManager.BeamTrace trace = ArcaneBeamManager.traceBeam(activeBeam, partialTick);
            if (trace != null) {
                renderTrace(poseStack, buffer, trace);
            }
        }

        poseStack.popPose();
        buffer.endBatch(DEFAULT_BEAM);
        buffer.endBatch(SHADER_DEFAULT_BEAM);
        buffer.endBatch(SHADER_SOLID_BEAM);
        buffer.endBatch(SOLID_BEAM);
    }

    public static void renderPreview(PoseStack poseStack, Vec3 cameraPosition, ArcaneBeamManager.BeamTrace trace) {
        if (trace == null) {
            return;
        }

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        renderTrace(poseStack, buffer, trace);
        poseStack.popPose();
        buffer.endBatch(DEFAULT_BEAM);
        buffer.endBatch(SHADER_DEFAULT_BEAM);
        buffer.endBatch(SHADER_SOLID_BEAM);
        buffer.endBatch(SOLID_BEAM);
    }

    private static void renderTrace(PoseStack poseStack, MultiBufferSource buffer, ArcaneBeamManager.BeamTrace trace) {
        ArcaneBeamConfig.BeamSettings settings = trace.settings();
        int color = getAnimatedColor(settings.colors, settings.color, settings.colorShiftTicks);
        int glowColor = getAnimatedColor(settings.glowColors, settings.glowColor, settings.colorShiftTicks);
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float glowRed = ((glowColor >> 16) & 0xFF) / 255.0F;
        float glowGreen = ((glowColor >> 8) & 0xFF) / 255.0F;
        float glowBlue = (glowColor & 0xFF) / 255.0F;
        float alpha = settings.opacity * trace.alphaMultiplier();
        float glowAlpha = alpha * settings.glowOpacity;
        float beamRadius = settings.intensity * trace.beamRadiusMultiplier();
        float glowRadius = Math.max(beamRadius, settings.glowRadius * trace.glowRadiusMultiplier());
        float height = (float) trace.start().distanceTo(trace.end());
        boolean shaderCompatibility = shaderCompatibilityEnabled();

        if (alpha <= 0.001F || beamRadius <= 0.0005F) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(trace.start().x, trace.start().y, trace.start().z);
        rotateYAxisToDirection(poseStack, trace.end().subtract(trace.start()).normalize());

        VertexConsumer main = shaderCompatibility ? buffer.getBuffer(SHADER_DEFAULT_BEAM) : buffer.getBuffer(DEFAULT_BEAM);
        VertexConsumer solid = shaderCompatibility ? buffer.getBuffer(SHADER_SOLID_BEAM) : buffer.getBuffer(SOLID_BEAM);

        float effectiveGlowRadius = shaderCompatibility ? Math.max(beamRadius, glowRadius * 0.75F) : glowRadius;
        float effectiveGlowAlpha = shaderCompatibility ? glowAlpha * 0.65F : glowAlpha;
        if (effectiveGlowAlpha > 0.001F) {
            VertexConsumer shaderGlow = main;
            poseStack.pushPose();
            applyGlowRotation(poseStack, settings, partialTicks());
            if (shaderCompatibility) {
                renderLitPart(poseStack, shaderGlow, glowRed, glowGreen, glowBlue, effectiveGlowAlpha, height, -effectiveGlowRadius, -effectiveGlowRadius, effectiveGlowRadius, -effectiveGlowRadius, -beamRadius, effectiveGlowRadius, effectiveGlowRadius, effectiveGlowRadius, false);
            } else {
                renderPart(poseStack, shaderGlow, glowRed, glowGreen, glowBlue, effectiveGlowAlpha, height, -effectiveGlowRadius, -effectiveGlowRadius, effectiveGlowRadius, -effectiveGlowRadius, -beamRadius, effectiveGlowRadius, effectiveGlowRadius, effectiveGlowRadius, false);
            }
            poseStack.popPose();
        }

        if (shaderCompatibility) {
            renderLitPart(poseStack, main, red, green, blue, alpha, height, 0.0F, beamRadius, beamRadius, 0.0F, -beamRadius, 0.0F, 0.0F, -beamRadius, false);
            renderLitPart(poseStack, solid, red, green, blue, alpha, height, 0.0F, beamRadius * 0.35F, beamRadius * 0.35F, 0.0F, -beamRadius * 0.35F, 0.0F, 0.0F, -beamRadius * 0.35F, false);
        } else {
            renderPart(poseStack, main, red, green, blue, alpha, height, 0.0F, beamRadius, beamRadius, 0.0F, -beamRadius, 0.0F, 0.0F, -beamRadius, false);
            renderPart(poseStack, solid, red, green, blue, alpha, height, 0.0F, beamRadius * 0.35F, beamRadius * 0.35F, 0.0F, -beamRadius * 0.35F, 0.0F, 0.0F, -beamRadius * 0.35F, false);
        }

        poseStack.popPose();
    }

    private static void applyGlowRotation(PoseStack poseStack, ArcaneBeamConfig.BeamSettings settings, float partialTick) {
        if (settings.glowRotationRpm <= 0.0F) {
            return;
        }
        long gameTime = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
        float degreesPerTick = settings.glowRotationRpm * 0.3F;
        float angle = (gameTime + partialTick) * degreesPerTick;
        poseStack.mulPose(Vector3f.YP.rotationDegrees(angle));
    }

    private static int getAnimatedColor(int[] colors, int fallbackColor, float colorShiftTicks) {
        if (colors == null || colors.length == 0) {
            return fallbackColor;
        }
        long gameTime = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
        int length = colors.length;
        float clampedShiftTicks = Math.max(2.0F, colorShiftTicks);
        int index = (int) Math.floor(gameTime / clampedShiftTicks) % length;
        int nextIndex = (index + 1) % length;
        float progress = (gameTime % clampedShiftTicks) / clampedShiftTicks;
        return lerpColor(colors[index], colors[nextIndex], progress);
    }

    private static float partialTicks() {
        return Minecraft.getInstance().getFrameTime();
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

    private static boolean shaderCompatibilityEnabled() {
        ArcaneBeamConfig.ShaderCompatibility compatibility = ArcaneBeamConfig.ShaderCompatibility.fromId(ArcaneBeamConfig.INSTANCE.shaderCompatibility);
        return compatibility == ArcaneBeamConfig.ShaderCompatibility.ON;
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

    private static void renderPart(PoseStack stack, VertexConsumer builder, float red, float green, float blue, float alpha, float height, float radius1, float radius2, float radius3, float radius4, float radius5, float radius6, float radius7, float radius8, boolean gradient) {
        Matrix4f matrixPose = stack.last().pose();
        renderQuad(matrixPose, builder, red, green, blue, alpha, height, radius1, radius2, radius3, radius4, gradient);
        renderQuad(matrixPose, builder, red, green, blue, alpha, height, radius7, radius8, radius5, radius6, gradient);
        renderQuad(matrixPose, builder, red, green, blue, alpha, height, radius3, radius4, radius7, radius8, gradient);
        renderQuad(matrixPose, builder, red, green, blue, alpha, height, radius5, radius6, radius1, radius2, gradient);
    }

    private static void renderLitPart(PoseStack stack, VertexConsumer builder, float red, float green, float blue, float alpha, float height, float radius1, float radius2, float radius3, float radius4, float radius5, float radius6, float radius7, float radius8, boolean gradient) {
        PoseStack.Pose pose = stack.last();
        Matrix4f matrixPose = pose.pose();
        renderLitQuad(matrixPose, pose.normal(), builder, red, green, blue, alpha, height, radius1, radius2, radius3, radius4, gradient);
        renderLitQuad(matrixPose, pose.normal(), builder, red, green, blue, alpha, height, radius7, radius8, radius5, radius6, gradient);
        renderLitQuad(matrixPose, pose.normal(), builder, red, green, blue, alpha, height, radius3, radius4, radius7, radius8, gradient);
        renderLitQuad(matrixPose, pose.normal(), builder, red, green, blue, alpha, height, radius5, radius6, radius1, radius2, gradient);
    }

    private static void renderQuad(Matrix4f pose, VertexConsumer builder, float red, float green, float blue, float alpha, float y, float x1, float z1, float x2, float z2, boolean gradient) {
        addVertex(pose, builder, red, green, blue, gradient ? 0.0F : alpha, y, x1, z1, 1.0F, 0.0F);
        addVertex(pose, builder, red, green, blue, alpha, 0.0F, x1, z1, 1.0F, 1.0F);
        addVertex(pose, builder, red, green, blue, alpha, 0.0F, x2, z2, 0.0F, 1.0F);
        addVertex(pose, builder, red, green, blue, gradient ? 0.0F : alpha, y, x2, z2, 0.0F, 0.0F);
    }

    private static void renderLitQuad(Matrix4f pose, com.mojang.math.Matrix3f normalMatrix, VertexConsumer builder, float red, float green, float blue, float alpha, float y, float x1, float z1, float x2, float z2, boolean gradient) {
        Vector3f faceNormal = faceNormal(x1, z1, x2, z2);
        addLitVertex(pose, normalMatrix, builder, red, green, blue, gradient ? 0.0F : alpha, y, x1, z1, 1.0F, 0.0F, faceNormal);
        addLitVertex(pose, normalMatrix, builder, red, green, blue, alpha, 0.0F, x1, z1, 1.0F, 1.0F, faceNormal);
        addLitVertex(pose, normalMatrix, builder, red, green, blue, alpha, 0.0F, x2, z2, 0.0F, 1.0F, faceNormal);
        addLitVertex(pose, normalMatrix, builder, red, green, blue, gradient ? 0.0F : alpha, y, x2, z2, 0.0F, 0.0F, faceNormal);
    }

    private static void addVertex(Matrix4f pose, VertexConsumer builder, float red, float green, float blue, float alpha, float y, float x, float z, float u, float v) {
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

    private static RenderType createUnlitBeamRenderType(String type, ResourceLocation texture) {
        CompositeState state = CompositeState.builder()
                .setShaderState(POSITION_COLOR_TEX_SHADER)
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .createCompositeState(false);
        return RenderType.create("arcane_beam_" + type, DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 256, false, true, state);
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

    private static RenderType createLitBeamRenderType(String type, ResourceLocation texture) {
        CompositeState state = CompositeState.builder()
                .setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .createCompositeState(false);
        return RenderType.create("arcane_beam_" + type, DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 256, false, true, state);
    }

    private static RenderType createCoreRenderType(String type, ResourceLocation texture) {
        CompositeState state = CompositeState.builder()
                .setShaderState(POSITION_COLOR_TEX_SHADER)
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .createCompositeState(false);
        return RenderType.create("arcane_beam_" + type, DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 256, false, true, state);
    }
}
