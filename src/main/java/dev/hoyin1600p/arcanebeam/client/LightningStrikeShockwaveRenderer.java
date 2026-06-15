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
    private static final int DISC_BANDS = 28;
    private static final int SPHERE_LATITUDES = 8;
    private static final int SPHERE_LONGITUDES = 16;

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
        float fade = 1.0F - progress;
        float rippleAlphaScale = rippleIndex == 0 ? 1.0F : 0.55F;
        float finalEdgeFade = Mth.clamp(fade / 0.18F, 0.0F, 1.0F);
        float leadingAlpha = settings.alpha() * rippleAlphaScale * finalEdgeFade;
        if (leadingAlpha <= 0.002F) {
            return;
        }

        int sides = Math.max(8, settings.ringSideCount());
        boolean shaderCompatibility = ArcaneBeamConfig.ShaderCompatibility.fromId(settings.shaderCompatibility()) == ArcaneBeamConfig.ShaderCompatibility.ON;
        VertexConsumer builder = buffer.getBuffer(shaderCompatibility ? SHADER_RING : NORMAL_RING);

        poseStack.pushPose();
        poseStack.translate(position.x, position.y + 0.05D, position.z);
        renderEnergyDisc(poseStack, builder, settings.ringColor(), leadingAlpha, settings.ringInteriorOpacity(), settings.ringThickness(), fade, radius, sides, shaderCompatibility, settings.fullbright());
        renderSpots(poseStack, builder, settings, leadingAlpha, fade, radius, sides, shaderCompatibility);

        if (rippleIndex == 0) {
            renderCenterGeometry(poseStack, builder, settings, progress, shaderCompatibility);
        }
        poseStack.popPose();
    }

    private static void renderEnergyDisc(PoseStack stack, VertexConsumer builder, int color, float leadingAlpha, float interiorOpacity, float edgeThickness, float fade, float radius, int sides, boolean shaderCompatibility, boolean fullbright) {
        float edgeWidth = Mth.clamp(edgeThickness / Math.max(0.01F, radius), 0.02F, 0.45F);
        for (int band = 0; band < DISC_BANDS; band++) {
            float innerFactor = band / (float) DISC_BANDS;
            float outerFactor = (band + 1) / (float) DISC_BANDS;
            float innerRadius = radius * innerFactor;
            float outerRadius = radius * outerFactor;
            float innerAlpha = discAlpha(leadingAlpha, interiorOpacity, edgeWidth, fade, innerFactor);
            float outerAlpha = discAlpha(leadingAlpha, interiorOpacity, edgeWidth, fade, outerFactor);
            if (innerAlpha <= 0.001F && outerAlpha <= 0.001F) {
                continue;
            }
            if (shaderCompatibility) {
                renderLitGradientAnnulus(stack, builder, color, innerAlpha, outerAlpha, innerRadius, outerRadius, sides, fullbright);
            } else {
                renderGradientAnnulus(stack, builder, color, innerAlpha, outerAlpha, innerRadius, outerRadius, sides);
            }
        }
    }

    private static float discAlpha(float leadingAlpha, float interiorOpacity, float edgeWidth, float fade, float radiusFactor) {
        float edgeStart = Math.max(0.0F, 1.0F - edgeWidth);
        float edgeWeight = smoothStep(edgeStart, 1.0F, radiusFactor);
        float interiorFade = (float) Math.pow(fade, 1.35D);
        float interiorAlpha = leadingAlpha * interiorOpacity * interiorFade * (0.20F + 0.80F * radiusFactor);
        return Mth.lerp(edgeWeight, interiorAlpha, leadingAlpha);
    }

    private static float smoothStep(float edge0, float edge1, float value) {
        float t = Mth.clamp((value - edge0) / Math.max(0.0001F, edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static void renderSpots(PoseStack stack, VertexConsumer builder, LightningStrikeShockwaveManager.ShockwaveRenderSettings settings, float leadingAlpha, float fade, float radius, int sides, boolean shaderCompatibility) {
        int count = Math.max(0, settings.spotCount());
        if (count == 0 || settings.spotOpacity() <= 0.0F || settings.spotSize() <= 0.0F) {
            return;
        }
        float lifetimeFade = (float) Math.pow(fade, 0.45D);
        for (int i = 0; i < count; i++) {
            float angle = (float) (Math.PI * 2.0D * hash01(i * 19 + 7));
            float distribution = hash01(i * 37 + 13);
            float radialFactor = distribution < 0.72F
                    ? Mth.lerp((float) Math.pow(hash01(i * 53 + 3), 0.35D), 0.58F, 0.99F)
                    : Mth.lerp(hash01(i * 71 + 11), 0.16F, 0.58F);
            float spotRadiusFromCenter = radius * radialFactor;
            float x = (float) Math.cos(angle) * spotRadiusFromCenter;
            float z = (float) Math.sin(angle) * spotRadiusFromCenter;
            float sizeScale = Mth.clamp(spotRadiusFromCenter / 10.0F, 0.25F, 1.0F);
            float spotRadius = settings.spotSize() * sizeScale * Mth.lerp(hash01(i * 97 + 5), 0.65F, 1.25F);
            float alpha = leadingAlpha * settings.spotOpacity() * lifetimeFade * Mth.lerp(radialFactor, 0.45F, 1.0F);
            if (shaderCompatibility) {
                renderLitSpot(stack, builder, settings.spotColor(), alpha, x, 0.012F, z, spotRadius, sides, settings.fullbright());
            } else {
                renderSpot(stack, builder, settings.spotColor(), alpha, x, 0.012F, z, spotRadius, sides);
            }
        }
    }

    private static void renderCenterGeometry(PoseStack stack, VertexConsumer builder, LightningStrikeShockwaveManager.ShockwaveRenderSettings settings, float progress, boolean shaderCompatibility) {
        float fade = 1.0F - progress;
        float centerFade = (float) Math.pow(fade, 1.15D);
        float sphereAlpha = settings.sphereOpacity() * centerFade;
        if (sphereAlpha > 0.002F && settings.sphereRadius() > 0.0F) {
            if (shaderCompatibility) {
                renderLitSphere(stack, builder, settings.sphereColor(), sphereAlpha, settings.sphereRadius(), settings.fullbright());
            } else {
                renderSphere(stack, builder, settings.sphereColor(), sphereAlpha, settings.sphereRadius());
            }
        }

        float coneAlpha = settings.coneOpacity() * centerFade;
        if (coneAlpha > 0.002F && settings.coneHeight() > 0.0F && settings.coneRadius() > 0.0F) {
            float tipOffset = Math.max(0.02F, settings.sphereRadius() * 0.72F);
            if (shaderCompatibility) {
                renderLitVerticalCone(stack, builder, settings.coneColor(), coneAlpha, tipOffset, tipOffset + settings.coneHeight(), settings.coneRadius(), settings.fullbright());
                renderLitVerticalCone(stack, builder, settings.coneColor(), coneAlpha, -tipOffset, -tipOffset - settings.coneHeight(), settings.coneRadius(), settings.fullbright());
            } else {
                renderVerticalCone(stack, builder, settings.coneColor(), coneAlpha, tipOffset, tipOffset + settings.coneHeight(), settings.coneRadius());
                renderVerticalCone(stack, builder, settings.coneColor(), coneAlpha, -tipOffset, -tipOffset - settings.coneHeight(), settings.coneRadius());
            }
        }
    }

    private static void renderGradientAnnulus(PoseStack stack, VertexConsumer builder, int color, float innerAlpha, float outerAlpha, float innerRadius, float outerRadius, int sides) {
        Matrix4f pose = stack.last().pose();
        float red = red(color);
        float green = green(color);
        float blue = blue(color);
        for (int i = 0; i < sides; i++) {
            int next = (i + 1) % sides;
            float angle1 = (float) (Math.PI * 2.0D * i / sides);
            float angle2 = (float) (Math.PI * 2.0D * next / sides);
            addVertex(pose, builder, red, green, blue, outerAlpha, outerRadius, angle1, 0.0F, 0.0F);
            addVertex(pose, builder, red, green, blue, innerAlpha, innerRadius, angle1, 0.0F, 1.0F);
            addVertex(pose, builder, red, green, blue, innerAlpha, innerRadius, angle2, 1.0F, 1.0F);
            addVertex(pose, builder, red, green, blue, outerAlpha, outerRadius, angle2, 1.0F, 0.0F);
        }
    }

    private static void renderLitGradientAnnulus(PoseStack stack, VertexConsumer builder, int color, float innerAlpha, float outerAlpha, float innerRadius, float outerRadius, int sides, boolean fullbright) {
        PoseStack.Pose pose = stack.last();
        float red = red(color);
        float green = green(color);
        float blue = blue(color);
        int packedLight = fullbright ? 15728880 : 0;
        for (int i = 0; i < sides; i++) {
            int next = (i + 1) % sides;
            float angle1 = (float) (Math.PI * 2.0D * i / sides);
            float angle2 = (float) (Math.PI * 2.0D * next / sides);
            addLitVertex(pose.pose(), pose.normal(), builder, red, green, blue, outerAlpha, outerRadius, angle1, 0.0F, 0.0F, packedLight);
            addLitVertex(pose.pose(), pose.normal(), builder, red, green, blue, innerAlpha, innerRadius, angle1, 0.0F, 1.0F, packedLight);
            addLitVertex(pose.pose(), pose.normal(), builder, red, green, blue, innerAlpha, innerRadius, angle2, 1.0F, 1.0F, packedLight);
            addLitVertex(pose.pose(), pose.normal(), builder, red, green, blue, outerAlpha, outerRadius, angle2, 1.0F, 0.0F, packedLight);
        }
    }

    private static void renderSpot(PoseStack stack, VertexConsumer builder, int color, float alpha, float x, float y, float z, float radius, int sides) {
        Matrix4f pose = stack.last().pose();
        float red = red(color);
        float green = green(color);
        float blue = blue(color);
        for (int i = 0; i < sides; i++) {
            int next = (i + 1) % sides;
            float angle1 = (float) (Math.PI * 2.0D * i / sides);
            float angle2 = (float) (Math.PI * 2.0D * next / sides);
            addRawVertex(pose, builder, red, green, blue, alpha, x, y, z, 0.5F, 0.5F);
            addRawVertex(pose, builder, red, green, blue, 0.0F, x + (float) Math.cos(angle2) * radius, y, z + (float) Math.sin(angle2) * radius, 1.0F, 0.0F);
            addRawVertex(pose, builder, red, green, blue, 0.0F, x + (float) Math.cos(angle1) * radius, y, z + (float) Math.sin(angle1) * radius, 0.0F, 0.0F);
            addRawVertex(pose, builder, red, green, blue, alpha, x, y, z, 0.5F, 0.5F);
        }
    }

    private static void renderLitSpot(PoseStack stack, VertexConsumer builder, int color, float alpha, float x, float y, float z, float radius, int sides, boolean fullbright) {
        PoseStack.Pose pose = stack.last();
        float red = red(color);
        float green = green(color);
        float blue = blue(color);
        int packedLight = fullbright ? 15728880 : 0;
        for (int i = 0; i < sides; i++) {
            int next = (i + 1) % sides;
            float angle1 = (float) (Math.PI * 2.0D * i / sides);
            float angle2 = (float) (Math.PI * 2.0D * next / sides);
            addRawLitVertex(pose.pose(), pose.normal(), builder, red, green, blue, alpha, x, y, z, 0.5F, 0.5F, packedLight);
            addRawLitVertex(pose.pose(), pose.normal(), builder, red, green, blue, 0.0F, x + (float) Math.cos(angle2) * radius, y, z + (float) Math.sin(angle2) * radius, 1.0F, 0.0F, packedLight);
            addRawLitVertex(pose.pose(), pose.normal(), builder, red, green, blue, 0.0F, x + (float) Math.cos(angle1) * radius, y, z + (float) Math.sin(angle1) * radius, 0.0F, 0.0F, packedLight);
            addRawLitVertex(pose.pose(), pose.normal(), builder, red, green, blue, alpha, x, y, z, 0.5F, 0.5F, packedLight);
        }
    }

    private static void renderSphere(PoseStack stack, VertexConsumer builder, int color, float alpha, float radius) {
        Matrix4f pose = stack.last().pose();
        float red = red(color);
        float green = green(color);
        float blue = blue(color);
        for (int lat = 0; lat < SPHERE_LATITUDES; lat++) {
            float v1 = lat / (float) SPHERE_LATITUDES;
            float v2 = (lat + 1) / (float) SPHERE_LATITUDES;
            float phi1 = (float) (-Math.PI * 0.5D + Math.PI * v1);
            float phi2 = (float) (-Math.PI * 0.5D + Math.PI * v2);
            for (int lon = 0; lon < SPHERE_LONGITUDES; lon++) {
                float u1 = lon / (float) SPHERE_LONGITUDES;
                float u2 = (lon + 1) / (float) SPHERE_LONGITUDES;
                float theta1 = (float) (Math.PI * 2.0D * u1);
                float theta2 = (float) (Math.PI * 2.0D * u2);
                addSphereVertex(pose, builder, red, green, blue, alpha, radius, phi1, theta1, u1, v1);
                addSphereVertex(pose, builder, red, green, blue, alpha, radius, phi2, theta1, u1, v2);
                addSphereVertex(pose, builder, red, green, blue, alpha, radius, phi2, theta2, u2, v2);
                addSphereVertex(pose, builder, red, green, blue, alpha, radius, phi1, theta2, u2, v1);
            }
        }
    }

    private static void renderLitSphere(PoseStack stack, VertexConsumer builder, int color, float alpha, float radius, boolean fullbright) {
        PoseStack.Pose pose = stack.last();
        float red = red(color);
        float green = green(color);
        float blue = blue(color);
        int packedLight = fullbright ? 15728880 : 0;
        for (int lat = 0; lat < SPHERE_LATITUDES; lat++) {
            float v1 = lat / (float) SPHERE_LATITUDES;
            float v2 = (lat + 1) / (float) SPHERE_LATITUDES;
            float phi1 = (float) (-Math.PI * 0.5D + Math.PI * v1);
            float phi2 = (float) (-Math.PI * 0.5D + Math.PI * v2);
            for (int lon = 0; lon < SPHERE_LONGITUDES; lon++) {
                float u1 = lon / (float) SPHERE_LONGITUDES;
                float u2 = (lon + 1) / (float) SPHERE_LONGITUDES;
                float theta1 = (float) (Math.PI * 2.0D * u1);
                float theta2 = (float) (Math.PI * 2.0D * u2);
                addLitSphereVertex(pose.pose(), pose.normal(), builder, red, green, blue, alpha, radius, phi1, theta1, u1, v1, packedLight);
                addLitSphereVertex(pose.pose(), pose.normal(), builder, red, green, blue, alpha, radius, phi2, theta1, u1, v2, packedLight);
                addLitSphereVertex(pose.pose(), pose.normal(), builder, red, green, blue, alpha, radius, phi2, theta2, u2, v2, packedLight);
                addLitSphereVertex(pose.pose(), pose.normal(), builder, red, green, blue, alpha, radius, phi1, theta2, u2, v1, packedLight);
            }
        }
    }

    private static void renderVerticalCone(PoseStack stack, VertexConsumer builder, int color, float alpha, float tipY, float wideY, float wideRadius) {
        Matrix4f pose = stack.last().pose();
        float red = red(color);
        float green = green(color);
        float blue = blue(color);
        for (int i = 0; i < SPHERE_LONGITUDES; i++) {
            int next = (i + 1) % SPHERE_LONGITUDES;
            float angle1 = (float) (Math.PI * 2.0D * i / SPHERE_LONGITUDES);
            float angle2 = (float) (Math.PI * 2.0D * next / SPHERE_LONGITUDES);
            addRawVertex(pose, builder, red, green, blue, alpha, 0.0F, tipY, 0.0F, 0.5F, 0.0F);
            addRawVertex(pose, builder, red, green, blue, 0.0F, (float) Math.cos(angle1) * wideRadius, wideY, (float) Math.sin(angle1) * wideRadius, 0.0F, 1.0F);
            addRawVertex(pose, builder, red, green, blue, 0.0F, (float) Math.cos(angle2) * wideRadius, wideY, (float) Math.sin(angle2) * wideRadius, 1.0F, 1.0F);
            addRawVertex(pose, builder, red, green, blue, alpha, 0.0F, tipY, 0.0F, 0.5F, 0.0F);
        }
    }

    private static void renderLitVerticalCone(PoseStack stack, VertexConsumer builder, int color, float alpha, float tipY, float wideY, float wideRadius, boolean fullbright) {
        PoseStack.Pose pose = stack.last();
        float red = red(color);
        float green = green(color);
        float blue = blue(color);
        int packedLight = fullbright ? 15728880 : 0;
        for (int i = 0; i < SPHERE_LONGITUDES; i++) {
            int next = (i + 1) % SPHERE_LONGITUDES;
            float angle1 = (float) (Math.PI * 2.0D * i / SPHERE_LONGITUDES);
            float angle2 = (float) (Math.PI * 2.0D * next / SPHERE_LONGITUDES);
            addRawLitVertex(pose.pose(), pose.normal(), builder, red, green, blue, alpha, 0.0F, tipY, 0.0F, 0.5F, 0.0F, packedLight);
            addRawLitVertex(pose.pose(), pose.normal(), builder, red, green, blue, 0.0F, (float) Math.cos(angle1) * wideRadius, wideY, (float) Math.sin(angle1) * wideRadius, 0.0F, 1.0F, packedLight);
            addRawLitVertex(pose.pose(), pose.normal(), builder, red, green, blue, 0.0F, (float) Math.cos(angle2) * wideRadius, wideY, (float) Math.sin(angle2) * wideRadius, 1.0F, 1.0F, packedLight);
            addRawLitVertex(pose.pose(), pose.normal(), builder, red, green, blue, alpha, 0.0F, tipY, 0.0F, 0.5F, 0.0F, packedLight);
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

    private static void addSphereVertex(Matrix4f pose, VertexConsumer builder, float red, float green, float blue, float alpha, float radius, float phi, float theta, float u, float v) {
        float cosPhi = (float) Math.cos(phi);
        addRawVertex(pose, builder, red, green, blue, alpha,
                (float) Math.cos(theta) * cosPhi * radius,
                (float) Math.sin(phi) * radius,
                (float) Math.sin(theta) * cosPhi * radius,
                u, v);
    }

    private static void addLitSphereVertex(Matrix4f pose, Matrix3f normalMatrix, VertexConsumer builder, float red, float green, float blue, float alpha, float radius, float phi, float theta, float u, float v, int packedLight) {
        float cosPhi = (float) Math.cos(phi);
        addRawLitVertex(pose, normalMatrix, builder, red, green, blue, alpha,
                (float) Math.cos(theta) * cosPhi * radius,
                (float) Math.sin(phi) * radius,
                (float) Math.sin(theta) * cosPhi * radius,
                u, v, packedLight);
    }

    private static float hash01(int seed) {
        int value = seed;
        value ^= value << 13;
        value ^= value >>> 17;
        value ^= value << 5;
        return (value & 0x00FFFFFF) / (float) 0x01000000;
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
        return RenderType.create("arcanebeam_lightning_shockwave", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 1024, false, true, state);
    }

    private static RenderType createShaderSafeRenderType() {
        CompositeState state = CompositeState.builder()
                .setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
                .setTextureState(new TextureStateShard(WHITE_TEXTURE, false, false))
                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .createCompositeState(false);
        return RenderType.create("arcanebeam_lightning_shockwave_shader", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 1024, false, true, state);
    }
}
