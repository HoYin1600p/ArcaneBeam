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

public class LightningStrikeShockwaveRenderer extends RenderType {
    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation(ArcaneBeam.MOD_ID, "textures/entity/white.png");
    private static final RenderType NORMAL_RING = createNormalRenderType();
    private static final RenderType SHADER_RING = createShaderSafeRenderType();

    private LightningStrikeShockwaveRenderer(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static void render(PoseStack poseStack, Vec3 cameraPosition, float partialTick, Collection<LightningStrikeShockwaveManager.ActiveShockwave> shockwaves) {
        if (shockwaves.isEmpty()) {
            return;
        }

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        for (LightningStrikeShockwaveManager.ActiveShockwave shockwave : shockwaves) {
            renderShockwave(poseStack, buffer, shockwave, partialTick);
        }

        poseStack.popPose();
        buffer.endBatch(NORMAL_RING);
        buffer.endBatch(SHADER_RING);
    }

    private static void renderShockwave(PoseStack poseStack, MultiBufferSource buffer, LightningStrikeShockwaveManager.ActiveShockwave shockwave, float partialTick) {
        LightningStrikeShockwaveManager.ShockwaveRenderSettings settings = shockwave.settings();
        renderRingPass(poseStack, buffer, shockwave.position(), shockwave.age(partialTick), settings, 1.0F, 0);
        for (int i = 0; i < settings.secondaryRippleCount(); i++) {
            float age = shockwave.age(partialTick) - settings.secondaryRippleDelayTicks() * (i + 1);
            if (age >= 0.0F) {
                renderRingPass(poseStack, buffer, shockwave.position(), age, settings, settings.secondaryRippleSize(), i + 1);
            }
        }
    }

    private static void renderRingPass(PoseStack poseStack, MultiBufferSource buffer, Vec3 position, float age, LightningStrikeShockwaveManager.ShockwaveRenderSettings settings, float radiusScale, int rippleIndex) {
        float progress = Mth.clamp(age / Math.max(1.0F, settings.lifetimeTicks()), 0.0F, 1.0F);
        if (progress >= 1.0F) {
            return;
        }

        // Radius is cosmetic only. It intentionally ignores Vault Hunters damage radius, level, talents, and modifiers.
        float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
        float radius = Mth.lerp(eased, settings.startRadius(), settings.endRadius() * radiusScale);

        // Thickness is independent of radius so the ring stays readable as it expands.
        float halfThickness = Math.max(0.01F, settings.ringThickness() * 0.5F);
        float innerRadius = Math.max(0.0F, radius - halfThickness);
        float outerRadius = radius + halfThickness;

        // Alpha fades smoothly toward zero over the configured lifetime.
        float fade = 1.0F - progress;
        float alpha = settings.alpha() * fade * fade;
        if (rippleIndex > 0) {
            alpha *= 0.55F;
        }
        if (alpha <= 0.002F) {
            return;
        }

        int sides = Math.max(8, settings.ringSideCount());
        boolean shaderCompatibility = ArcaneBeamConfig.ShaderCompatibility.fromId(settings.shaderCompatibility()) == ArcaneBeamConfig.ShaderCompatibility.ON;
        VertexConsumer builder = buffer.getBuffer(shaderCompatibility ? SHADER_RING : NORMAL_RING);

        poseStack.pushPose();
        poseStack.translate(position.x, position.y + 0.05D, position.z);
        if (shaderCompatibility) {
            renderLitAnnulus(poseStack, builder, settings.ringColor(), alpha, innerRadius, outerRadius, sides, settings.fullbright());
        } else {
            renderAnnulus(poseStack, builder, settings.ringColor(), alpha, innerRadius, outerRadius, sides);
        }

        float flashProgress = Mth.clamp(age / Math.max(1.0F, settings.lifetimeTicks() * 0.35F), 0.0F, 1.0F);
        if (flashProgress < 1.0F && rippleIndex == 0) {
            float flashAlpha = settings.alpha() * (1.0F - flashProgress);
            float flashRadius = settings.startRadius() * Mth.lerp(flashProgress, 0.25F, 0.75F);
            if (shaderCompatibility) {
                renderLitDisc(poseStack, builder, settings.centerFlashColor(), flashAlpha, flashRadius, sides, settings.fullbright());
            } else {
                renderDisc(poseStack, builder, settings.centerFlashColor(), flashAlpha, flashRadius, sides);
            }
        }
        poseStack.popPose();
    }

    private static void renderAnnulus(PoseStack stack, VertexConsumer builder, int color, float alpha, float innerRadius, float outerRadius, int sides) {
        Matrix4f pose = stack.last().pose();
        float red = red(color);
        float green = green(color);
        float blue = blue(color);
        for (int i = 0; i < sides; i++) {
            int next = (i + 1) % sides;
            float angle1 = (float) (Math.PI * 2.0D * i / sides);
            float angle2 = (float) (Math.PI * 2.0D * next / sides);
            addVertex(pose, builder, red, green, blue, alpha, outerRadius, angle1, 0.0F, 0.0F);
            addVertex(pose, builder, red, green, blue, alpha, innerRadius, angle1, 0.0F, 1.0F);
            addVertex(pose, builder, red, green, blue, alpha, innerRadius, angle2, 1.0F, 1.0F);
            addVertex(pose, builder, red, green, blue, alpha, outerRadius, angle2, 1.0F, 0.0F);
        }
    }

    private static void renderLitAnnulus(PoseStack stack, VertexConsumer builder, int color, float alpha, float innerRadius, float outerRadius, int sides, boolean fullbright) {
        PoseStack.Pose pose = stack.last();
        float red = red(color);
        float green = green(color);
        float blue = blue(color);
        int packedLight = fullbright ? 15728880 : 0;
        for (int i = 0; i < sides; i++) {
            int next = (i + 1) % sides;
            float angle1 = (float) (Math.PI * 2.0D * i / sides);
            float angle2 = (float) (Math.PI * 2.0D * next / sides);
            addLitVertex(pose.pose(), pose.normal(), builder, red, green, blue, alpha, outerRadius, angle1, 0.0F, 0.0F, packedLight);
            addLitVertex(pose.pose(), pose.normal(), builder, red, green, blue, alpha, innerRadius, angle1, 0.0F, 1.0F, packedLight);
            addLitVertex(pose.pose(), pose.normal(), builder, red, green, blue, alpha, innerRadius, angle2, 1.0F, 1.0F, packedLight);
            addLitVertex(pose.pose(), pose.normal(), builder, red, green, blue, alpha, outerRadius, angle2, 1.0F, 0.0F, packedLight);
        }
    }

    private static void renderDisc(PoseStack stack, VertexConsumer builder, int color, float alpha, float radius, int sides) {
        Matrix4f pose = stack.last().pose();
        float red = red(color);
        float green = green(color);
        float blue = blue(color);
        for (int i = 0; i < sides; i++) {
            int next = (i + 1) % sides;
            float angle1 = (float) (Math.PI * 2.0D * i / sides);
            float angle2 = (float) (Math.PI * 2.0D * next / sides);
            addRawVertex(pose, builder, red, green, blue, alpha, 0.0F, 0.0F, 0.0F, 0.5F, 0.5F);
            addVertex(pose, builder, red, green, blue, 0.0F, radius, angle2, 1.0F, 0.0F);
            addVertex(pose, builder, red, green, blue, 0.0F, radius, angle1, 0.0F, 0.0F);
            addRawVertex(pose, builder, red, green, blue, alpha, 0.0F, 0.0F, 0.0F, 0.5F, 0.5F);
        }
    }

    private static void renderLitDisc(PoseStack stack, VertexConsumer builder, int color, float alpha, float radius, int sides, boolean fullbright) {
        PoseStack.Pose pose = stack.last();
        float red = red(color);
        float green = green(color);
        float blue = blue(color);
        int packedLight = fullbright ? 15728880 : 0;
        for (int i = 0; i < sides; i++) {
            int next = (i + 1) % sides;
            float angle1 = (float) (Math.PI * 2.0D * i / sides);
            float angle2 = (float) (Math.PI * 2.0D * next / sides);
            addRawLitVertex(pose.pose(), pose.normal(), builder, red, green, blue, alpha, 0.0F, 0.0F, 0.0F, 0.5F, 0.5F, packedLight);
            addLitVertex(pose.pose(), pose.normal(), builder, red, green, blue, 0.0F, radius, angle2, 1.0F, 0.0F, packedLight);
            addLitVertex(pose.pose(), pose.normal(), builder, red, green, blue, 0.0F, radius, angle1, 0.0F, 0.0F, packedLight);
            addRawLitVertex(pose.pose(), pose.normal(), builder, red, green, blue, alpha, 0.0F, 0.0F, 0.0F, 0.5F, 0.5F, packedLight);
        }
    }

    private static void addVertex(Matrix4f pose, VertexConsumer builder, float red, float green, float blue, float alpha, float radius, float angle, float u, float v) {
        addRawVertex(pose, builder, red, green, blue, alpha, (float) Math.cos(angle) * radius, 0.0F, (float) Math.sin(angle) * radius, u, v);
    }

    private static void addRawVertex(Matrix4f pose, VertexConsumer builder, float red, float green, float blue, float alpha, float x, float y, float z, float u, float v) {
        builder.vertex(pose, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .endVertex();
    }

    private static void addLitVertex(Matrix4f pose, Matrix3f normalMatrix, VertexConsumer builder, float red, float green, float blue, float alpha, float radius, float angle, float u, float v, int packedLight) {
        addRawLitVertex(pose, normalMatrix, builder, red, green, blue, alpha, (float) Math.cos(angle) * radius, 0.0F, (float) Math.sin(angle) * radius, u, v, packedLight);
    }

    private static void addRawLitVertex(Matrix4f pose, Matrix3f normalMatrix, VertexConsumer builder, float red, float green, float blue, float alpha, float x, float y, float z, float u, float v, int packedLight) {
        builder.vertex(pose, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalMatrix, Vector3f.YP.x(), Vector3f.YP.y(), Vector3f.YP.z())
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

    private static RenderType createNormalRenderType() {
        CompositeState state = CompositeState.builder()
                .setShaderState(POSITION_COLOR_TEX_SHADER)
                .setTextureState(new TextureStateShard(WHITE_TEXTURE, false, false))
                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .createCompositeState(false);
        return RenderType.create("arcanebeam_lightning_shockwave", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 256, false, true, state);
    }

    private static RenderType createShaderSafeRenderType() {
        CompositeState state = CompositeState.builder()
                .setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
                .setTextureState(new TextureStateShard(WHITE_TEXTURE, false, false))
                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .createCompositeState(false);
        return RenderType.create("arcanebeam_lightning_shockwave_shader", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 256, false, true, state);
    }
}
