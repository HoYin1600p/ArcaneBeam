package dev.hoyin1600p.arcanebeam.beam;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import dev.hoyin1600p.arcanebeam.config.model.BeamSettings;
import dev.hoyin1600p.arcanebeam.config.model.ShaderCompatibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

import static dev.hoyin1600p.arcanebeam.util.ArcaneBeamIds.WHITE_TEXTURE_PATH;
import static dev.hoyin1600p.arcanebeam.util.ArcaneBeamIds.RENDER_TYPE_PREFIX;

public class ArcaneBeamRenderer extends RenderType {
    private static final long ANIMATION_EPOCH_NANOS = System.nanoTime();
    private static final int CYLINDER_SIDES = 8;
    private static final ResourceLocation LOOT_BEAM_TEXTURE = new ResourceLocation(ArcaneBeam.MOD_ID, "textures/entity/loot_beam.png");
    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation(ArcaneBeam.MOD_ID, WHITE_TEXTURE_PATH);
    private static final RenderType DEFAULT_BEAM = createUnlitBeamRenderType("default", LOOT_BEAM_TEXTURE);
    private static final RenderType SHADER_DEFAULT_BEAM = createLitBeamRenderType("shader_default", LOOT_BEAM_TEXTURE);
    private static final RenderType SHADER_SOLID_BEAM = createLitBeamRenderType("shader_solid", WHITE_TEXTURE);
    private static final RenderType SOLID_BEAM = createCoreRenderType("solid", WHITE_TEXTURE);

    private ArcaneBeamRenderer(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling,
            boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static void render(PoseStack poseStack, Vec3 cameraPosition, float partialTick, Collection<ActiveBeam> activeBeams) {
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();

        poseStack.pushPose();

        for (ActiveBeam activeBeam : activeBeams) {
            BeamTrace trace = ArcaneBeamManager.traceBeam(activeBeam, partialTick);
            if (trace != null) {
                renderTrace(poseStack, buffer, trace, cameraPosition);
            }
        }

        poseStack.popPose();
        buffer.endBatch(DEFAULT_BEAM);
        buffer.endBatch(SHADER_DEFAULT_BEAM);
        buffer.endBatch(SHADER_SOLID_BEAM);
        buffer.endBatch(SOLID_BEAM);
    }

    public static void renderPreview(PoseStack poseStack, Vec3 cameraPosition, BeamTrace trace) {
        if (trace == null) {
            return;
        }

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        poseStack.pushPose();
        renderTrace(poseStack, buffer, trace, cameraPosition);
        poseStack.popPose();
        buffer.endBatch(DEFAULT_BEAM);
        buffer.endBatch(SHADER_DEFAULT_BEAM);
        buffer.endBatch(SHADER_SOLID_BEAM);
        buffer.endBatch(SOLID_BEAM);
    }

    /** The pose stack is not pre-translated: world offsets are subtracted from the camera in double, then narrowed once. */
    private static void renderTrace(PoseStack poseStack, MultiBufferSource buffer, BeamTrace trace, Vec3 cameraPosition) {
        BeamSettings settings = trace.settings();
        double animationSeconds = animationSeconds();
        int color = getAnimatedColor(settings.colors, settings.color, settings.colorShiftTicks, animationSeconds);
        int glowColor = getAnimatedColor(settings.glowColors, settings.glowColor, settings.colorShiftTicks, animationSeconds);
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
        boolean shaderCompatibility = shaderCompatibilityEnabled(settings);

        if (alpha <= 0.001F || beamRadius <= 0.0005F || height <= 0.0005F) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(trace.start().x - cameraPosition.x, trace.start().y - cameraPosition.y, trace.start().z - cameraPosition.z);
        rotateYAxisToDirection(poseStack, trace.direction());

        // The shared buffer source re-begins one BufferBuilder for every non-fixed type, so each
        // layer's consumer must be fetched and fully written before the next type is requested.
        VertexConsumer main = buffer.getBuffer(shaderCompatibility ? SHADER_DEFAULT_BEAM : DEFAULT_BEAM);

        float effectiveGlowRadius = shaderCompatibility ? Math.max(beamRadius, glowRadius * 0.75F) : glowRadius;
        float effectiveGlowAlpha = shaderCompatibility ? glowAlpha * 0.65F : glowAlpha;
        if (effectiveGlowAlpha > 0.001F) {
            poseStack.pushPose();
            applyGlowRotation(poseStack, settings, animationSeconds);
            if (shaderCompatibility) {
                renderLitTube(poseStack, main, glowRed, glowGreen, glowBlue, effectiveGlowAlpha, height, effectiveGlowRadius, false);
            } else {
                renderTube(poseStack, main, glowRed, glowGreen, glowBlue, effectiveGlowAlpha, height, effectiveGlowRadius, false);
            }
            poseStack.popPose();
        }

        if (shaderCompatibility) {
            renderLitTube(poseStack, main, red, green, blue, alpha, height, beamRadius, false);
        } else {
            renderTube(poseStack, main, red, green, blue, alpha, height, beamRadius, false);
        }

        VertexConsumer solid = buffer.getBuffer(shaderCompatibility ? SHADER_SOLID_BEAM : SOLID_BEAM);
        if (shaderCompatibility) {
            renderLitTube(poseStack, solid, red, green, blue, alpha, height, beamRadius * 0.35F, false);
        } else {
            renderTube(poseStack, solid, red, green, blue, alpha, height, beamRadius * 0.35F, false);
        }

        poseStack.popPose();
    }

    private static void applyGlowRotation(PoseStack poseStack, BeamSettings settings, double animationSeconds) {
        if (settings.glowRotationRpm <= 0.0F) {
            return;
        }
        double degreesPerSecond = settings.glowRotationRpm * 6.0D;
        float angle = (float) ((animationSeconds * degreesPerSecond) % 360.0D);
        poseStack.mulPose(Vector3f.YP.rotationDegrees(angle));
    }

    private static int getAnimatedColor(int[] colors, int fallbackColor, float colorShiftTicks, double animationSeconds) {
        if (colors == null || colors.length == 0) {
            return fallbackColor;
        }
        int length = colors.length;
        double shiftSeconds = Math.max(2.0D, colorShiftTicks) / 20.0D;
        double cyclePosition = animationSeconds / shiftSeconds;
        long colorIndex = (long) Math.floor(cyclePosition);
        int index = (int) Math.floorMod(colorIndex, length);
        int nextIndex = (index + 1) % length;
        float progress = (float) (cyclePosition - Math.floor(cyclePosition));
        return lerpColor(colors[index], colors[nextIndex], progress);
    }

    private static double animationSeconds() {
        return (System.nanoTime() - ANIMATION_EPOCH_NANOS) / 1_000_000_000.0D;
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

    private static boolean shaderCompatibilityEnabled(BeamSettings settings) {
        ShaderCompatibility compatibility = ShaderCompatibility.fromId(settings.shaderCompatibility);
        return compatibility == ShaderCompatibility.ON;
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

    private static void renderTube(PoseStack stack, VertexConsumer builder, float red, float green, float blue, float alpha, float height,
            float radius, boolean gradient) {
        Matrix4f matrixPose = stack.last().pose();
        for (int i = 0; i < CYLINDER_SIDES; i++) {
            int next = (i + 1) % CYLINDER_SIDES;
            float angle1 = (float) (Math.PI * 2.0D * i / CYLINDER_SIDES);
            float angle2 = (float) (Math.PI * 2.0D * next / CYLINDER_SIDES);
            float x1 = (float) Math.sin(angle1) * radius;
            float z1 = (float) Math.cos(angle1) * radius;
            float x2 = (float) Math.sin(angle2) * radius;
            float z2 = (float) Math.cos(angle2) * radius;
            float u1 = i / (float) CYLINDER_SIDES;
            float u2 = next / (float) CYLINDER_SIDES;
            renderQuad(matrixPose, builder, red, green, blue, alpha, height, x1, z1, x2, z2, u1, u2, gradient);
        }
    }

    private static void renderLitTube(PoseStack stack, VertexConsumer builder, float red, float green, float blue, float alpha, float height,
            float radius, boolean gradient) {
        PoseStack.Pose pose = stack.last();
        Matrix4f matrixPose = pose.pose();
        for (int i = 0; i < CYLINDER_SIDES; i++) {
            int next = (i + 1) % CYLINDER_SIDES;
            float angle1 = (float) (Math.PI * 2.0D * i / CYLINDER_SIDES);
            float angle2 = (float) (Math.PI * 2.0D * next / CYLINDER_SIDES);
            float x1 = (float) Math.sin(angle1) * radius;
            float z1 = (float) Math.cos(angle1) * radius;
            float x2 = (float) Math.sin(angle2) * radius;
            float z2 = (float) Math.cos(angle2) * radius;
            float u1 = i / (float) CYLINDER_SIDES;
            float u2 = next / (float) CYLINDER_SIDES;
            renderLitQuad(matrixPose, pose.normal(), builder, red, green, blue, alpha, height, x1, z1, x2, z2, u1, u2, gradient);
        }
    }

    private static void renderQuad(Matrix4f pose, VertexConsumer builder, float red, float green, float blue, float alpha, float y, float x1,
            float z1, float x2, float z2, float u1, float u2, boolean gradient) {
        addVertex(pose, builder, red, green, blue, gradient ? 0.0F : alpha, y, x1, z1, u1, BeamTextureRow.TIP_V);
        addVertex(pose, builder, red, green, blue, alpha, 0.0F, x1, z1, u1, BeamTextureRow.ORIGIN_V);
        addVertex(pose, builder, red, green, blue, alpha, 0.0F, x2, z2, u2, BeamTextureRow.ORIGIN_V);
        addVertex(pose, builder, red, green, blue, gradient ? 0.0F : alpha, y, x2, z2, u2, BeamTextureRow.TIP_V);
    }

    private static void renderLitQuad(Matrix4f pose, Matrix3f normalMatrix, VertexConsumer builder, float red, float green, float blue, float alpha,
            float y, float x1, float z1, float x2, float z2, float u1, float u2, boolean gradient) {
        Vector3f faceNormal = faceNormal(x1, z1, x2, z2);
        addLitVertex(pose, normalMatrix, builder, red, green, blue, gradient ? 0.0F : alpha, y, x1, z1, u1, BeamTextureRow.TIP_V, faceNormal);
        addLitVertex(pose, normalMatrix, builder, red, green, blue, alpha, 0.0F, x1, z1, u1, BeamTextureRow.ORIGIN_V, faceNormal);
        addLitVertex(pose, normalMatrix, builder, red, green, blue, alpha, 0.0F, x2, z2, u2, BeamTextureRow.ORIGIN_V, faceNormal);
        addLitVertex(pose, normalMatrix, builder, red, green, blue, gradient ? 0.0F : alpha, y, x2, z2, u2, BeamTextureRow.TIP_V, faceNormal);
    }

    private static void addVertex(Matrix4f pose, VertexConsumer builder, float red, float green, float blue, float alpha, float y, float x, float z,
            float u, float v) {
        builder.vertex(pose, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .endVertex();
    }

    private static void addLitVertex(Matrix4f pose, Matrix3f normalMatrix, VertexConsumer builder, float red, float green, float blue, float alpha,
            float y, float x, float z, float u, float v, Vector3f faceNormal) {
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
        return RenderType.create(RENDER_TYPE_PREFIX + type, DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 256, false, true, state);
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
        return RenderType.create(RENDER_TYPE_PREFIX + type, DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 256, false, true, state);
    }

    private static RenderType createCoreRenderType(String type, ResourceLocation texture) {
        CompositeState state = CompositeState.builder()
                .setShaderState(POSITION_COLOR_TEX_SHADER)
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .createCompositeState(false);
        return RenderType.create(RENDER_TYPE_PREFIX + type, DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 256, false, true, state);
    }
}
