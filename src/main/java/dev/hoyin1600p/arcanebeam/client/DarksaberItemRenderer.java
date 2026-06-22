package dev.hoyin1600p.arcanebeam.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class DarksaberItemRenderer extends RenderType {
    private static final ResourceLocation MODEL = new ResourceLocation(ArcaneBeam.MOD_ID, "models/darksaber/darksaber2.1.bbmodel");
    private static final ResourceLocation BLADE_MESH = new ResourceLocation(ArcaneBeam.MOD_ID, "models/darksaber/darksaber_blade_mesh.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(ArcaneBeam.MOD_ID, "textures/item/darksaber/darksaber2.png");
    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation(ArcaneBeam.MOD_ID, "textures/entity/white.png");
    private static final ResourceLocation VAULT_SWORD = new ResourceLocation("the_vault", "sword");
    private static final String GEAR_MODEL_TAG = "the_vault:gear_model";
    private static final RenderType HILT = RenderType.entityCutoutNoCull(TEXTURE);
    private static final RenderType BLADE = createBladeRenderType();
    private static final RenderType BLADE_TRIANGLES = createBladeTriangleRenderType();
    private static final RenderType GLOW = createGlowRenderType();
    private static final int TEXTURE_SIZE = 64;
    private static final float BLADE_X_MIN = -0.31F;
    private static final float BLADE_X_MAX = 0.56F;
    private static final float BLADE_X_CENTER = (BLADE_X_MIN + BLADE_X_MAX) * 0.5F;
    private static final float BLADE_THICKNESS = 0.08F; // 5 mm: 0.005 blocks * 16 model units.
    private static final float BLADE_X_BACK = BLADE_X_CENTER - BLADE_THICKNESS * 0.5F;
    private static final float BLADE_X_FRONT = BLADE_X_CENTER + BLADE_THICKNESS * 0.5F;
    private static final float BLADE_SURFACE_OFFSET = 0.006F;
    private static final float BLADE_BASE_Z = 7.15F;
    private static final float BLADE_TIP_Z = -22.85F;
    private static final float EFFECT_START_T = 0.075F;
    private static DarksaberModel cachedModel;

    private DarksaberItemRenderer(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static boolean renderItem(ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ArcaneBeamConfig.DarksaberSettings settings = ArcaneBeamConfig.INSTANCE.darksaber;
        if (settings == null || !settings.enabled || !matches(stack, settings)) {
            return false;
        }
        DarksaberModel model = model();
        if (model == null) {
            return false;
        }
        renderModel(poseStack, buffer, packedLight, packedOverlay, settings, Minecraft.getInstance().getFrameTime(), 1.0F);
        return true;
    }

    public static void renderScreenPreview(PoseStack poseStack, MultiBufferSource buffer, int layoutWidth, int drawerY, float partialTick) {
        DarksaberModel model = model();
        if (model == null) {
            return;
        }
        ArcaneBeamConfig.DarksaberSettings settings = ArcaneBeamConfig.INSTANCE.darksaber;
        int x = layoutWidth / 2 + Math.round(settings.previewOffsetX);
        float pixelScale = Math.min(1500.0F, Math.max(420.0F, layoutWidth * 0.45F)) * settings.previewZoom;
        int y = Math.max(150, drawerY - Math.round(pixelScale * 0.24F)) + Math.round(settings.previewOffsetY);
        poseStack.pushPose();
        poseStack.translate(x, y, 180.0D);
        poseStack.scale(pixelScale, -pixelScale, pixelScale);
        poseStack.mulPose(Vector3f.YP.rotationDegrees(settings.previewYaw));
        poseStack.mulPose(Vector3f.XP.rotationDegrees(settings.previewPitch));
        poseStack.mulPose(Vector3f.ZP.rotationDegrees(settings.previewRoll));
        renderModel(poseStack, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, settings, partialTick, 1.0F);
        poseStack.popPose();
    }

    private static boolean matches(ItemStack stack, ArcaneBeamConfig.DarksaberSettings settings) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (!isVaultSword(stack)) {
            return false;
        }
        String configured = ArcaneBeamConfig.normalizeSwordTransmog(settings.transmog);
        String dynamicModel = dynamicModelId(stack);
        if (matchesConfiguredModel(dynamicModel, configured)) {
            return true;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tagContainsModel(tag, configured);
    }

    private static boolean isVaultSword(ItemStack stack) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (VAULT_SWORD.equals(itemId)) {
            return true;
        }
        String className = stack.getItem().getClass().getName();
        return className.equals("iskallia.vault.item.gear.VaultSwordItem") || className.endsWith(".VaultSwordItem");
    }

    private static String dynamicModelId(ItemStack stack) {
        try {
            Method method = stack.getItem().getClass().getMethod("getDynamicModelId", ItemStack.class);
            Object result = method.invoke(stack.getItem(), stack);
            if (result instanceof Optional<?> optional && optional.isPresent()) {
                return optional.get().toString();
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Vault keeps the current transmog in its gear cache; fall back to NBT for older or unusual stacks.
        }
        return null;
    }

    private static boolean tagContainsModel(CompoundTag tag, String configured) {
        if (tag.contains(GEAR_MODEL_TAG) && matchesConfiguredModel(tag.getString(GEAR_MODEL_TAG), configured)) {
            return true;
        }
        for (String key : tag.getAllKeys()) {
            Tag child = tag.get(key);
            if (child == null) {
                continue;
            }
            if (child.getId() == 8 && matchesConfiguredModel(child.getAsString(), configured)) {
                return true;
            }
            if (child instanceof CompoundTag compound && tagContainsModel(compound, configured)) {
                return true;
            }
            if (child instanceof ListTag list && listContainsModel(list, configured)) {
                return true;
            }
        }
        return false;
    }

    private static boolean listContainsModel(ListTag list, String configured) {
        for (Tag child : list) {
            if (child.getId() == 8 && matchesConfiguredModel(child.getAsString(), configured)) {
                return true;
            }
            if (child instanceof CompoundTag compound && tagContainsModel(compound, configured)) {
                return true;
            }
            if (child instanceof ListTag nested && listContainsModel(nested, configured)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesConfiguredModel(String candidate, String configured) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        String normalizedCandidate = ArcaneBeamConfig.normalizeSwordTransmog(candidate);
        if (configured.equals(normalizedCandidate)) {
            return true;
        }
        String rawCandidate = candidate.trim().toLowerCase(Locale.ROOT).replace('\\', '/');
        String shortName = shortTransmogName(configured);
        return rawCandidate.equals(shortName) || rawCandidate.endsWith("/" + shortName) || rawCandidate.endsWith(":" + shortName);
    }

    private static String shortTransmogName(String modelId) {
        int slash = modelId.lastIndexOf('/');
        return slash >= 0 ? modelId.substring(slash + 1) : modelId;
    }

    private static void renderModel(PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, ArcaneBeamConfig.DarksaberSettings settings, float partialTick, float previewScale) {
        DarksaberModel model = model();
        if (model == null) {
            return;
        }
        float shimmer = 1.0F - settings.shimmerAmount + settings.shimmerAmount * (0.5F + 0.5F * (float) Math.sin((Minecraft.getInstance().level == null ? 0.0F : Minecraft.getInstance().level.getGameTime() + partialTick) / settings.shimmerSpeed * Math.PI * 2.0D));

        poseStack.pushPose();
        poseStack.translate(0.5D + settings.offsetX / 16.0D, 0.5D + settings.offsetY / 16.0D, 0.5D + settings.offsetZ / 16.0D);
        poseStack.mulPose(Vector3f.XP.rotationDegrees(settings.rotationX));
        poseStack.mulPose(Vector3f.YP.rotationDegrees(settings.rotationY));
        poseStack.mulPose(Vector3f.ZP.rotationDegrees(settings.rotationZ));
        float scale = settings.scale * previewScale;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-model.centerX / 16.0D, -model.centerY / 16.0D, -model.centerZ / 16.0D);

        VertexConsumer hilt = buffer.getBuffer(HILT);
        for (Element element : model.elements) {
            if (element.group == Group.HILT) {
                renderElement(poseStack, hilt, element, 0xFFFFFF, 1.0F, LightTexture.FULL_BRIGHT, packedOverlay, true);
            }
        }
        renderCustomBlade(poseStack, buffer, model.bladeMesh, settings, partialTick, shimmer);
        poseStack.popPose();
    }

    private static void renderCustomBlade(PoseStack poseStack, MultiBufferSource buffer, BladeMesh bladeMesh, ArcaneBeamConfig.DarksaberSettings settings, float partialTick, float shimmer) {
        if (bladeMesh != null && !bladeMesh.triangles.isEmpty()) {
            renderImportedBlade(poseStack, buffer, bladeMesh, settings, partialTick, shimmer);
            return;
        }
        renderProceduralBlade(poseStack, buffer, settings, partialTick, shimmer);
    }

    private static void renderImportedBlade(PoseStack poseStack, MultiBufferSource buffer, BladeMesh bladeMesh, ArcaneBeamConfig.DarksaberSettings settings, float partialTick, float shimmer) {
        VertexConsumer mesh = buffer.getBuffer(BLADE_TRIANGLES);
        for (Triangle triangle : bladeMesh.triangles) {
            addMeshVertex(poseStack, mesh, bladeMesh.vertices.get(triangle.a), settings.coreColor, settings.coreOpacity);
            addMeshVertex(poseStack, mesh, bladeMesh.vertices.get(triangle.b), settings.coreColor, settings.coreOpacity);
            addMeshVertex(poseStack, mesh, bladeMesh.vertices.get(triangle.c), settings.coreColor, settings.coreOpacity);
        }
        renderBladeEffects(poseStack, buffer, bladeMesh::slice, settings, partialTick, shimmer);
    }

    private static void renderProceduralBlade(PoseStack poseStack, MultiBufferSource buffer, ArcaneBeamConfig.DarksaberSettings settings, float partialTick, float shimmer) {
        VertexConsumer blade = buffer.getBuffer(BLADE);
        int segments = 18;
        for (int i = 0; i < segments; i++) {
            float t0 = i / (float) segments;
            float t1 = (i + 1) / (float) segments;
            BladeSlice a = bladeSlice(t0);
            BladeSlice b = bladeSlice(t1);
            renderBladeFace(poseStack, blade, BLADE_X_FRONT, a, b, settings.coreColor, settings.coreOpacity, settings.coreOpacity);
            renderBladeFace(poseStack, blade, BLADE_X_BACK, b, a, settings.coreColor, settings.coreOpacity, settings.coreOpacity);
            renderBladeEdge(poseStack, blade, a.leadingY, a.z, b.leadingY, b.z, settings.edgeColor, settings.edgeOpacity * 0.78F);
            renderBladeEdge(poseStack, blade, b.trailingY, b.z, a.trailingY, a.z, settings.edgeColor, settings.edgeOpacity * 0.34F);
            if (i == 0) {
                renderBladeCap(poseStack, blade, a, settings.edgeColor, settings.edgeOpacity * 0.46F);
            } else if (i == segments - 1) {
                renderBladeCap(poseStack, blade, b, settings.edgeColor, settings.edgeOpacity * 0.62F);
            }
        }
        renderBladeEffects(poseStack, buffer, DarksaberItemRenderer::bladeSlice, settings, partialTick, shimmer);
    }

    private static void renderBladeEffects(PoseStack poseStack, MultiBufferSource buffer, BladeProfile profile, ArcaneBeamConfig.DarksaberSettings settings, float partialTick, float shimmer) {
        VertexConsumer blade = buffer.getBuffer(BLADE);
        VertexConsumer glow = buffer.getBuffer(GLOW);
        int segments = 18;
        for (int i = 0; i < segments; i++) {
            float t0 = EFFECT_START_T + (1.0F - EFFECT_START_T) * i / (float) segments;
            float t1 = EFFECT_START_T + (1.0F - EFFECT_START_T) * (i + 1) / (float) segments;
            BladeSlice a = profile.slice(t0);
            BladeSlice b = profile.slice(t1);

            float leadingAlpha = settings.edgeOpacity * (0.72F + 0.28F * shimmer);
            renderLeadingGlowFace(poseStack, blade, BLADE_X_FRONT + BLADE_SURFACE_OFFSET, a, b, settings.edgeColor, leadingAlpha, 0.04F, 0.38F);
            renderLeadingGlowFace(poseStack, blade, BLADE_X_BACK - BLADE_SURFACE_OFFSET, b, a, settings.edgeColor, leadingAlpha, 0.04F, 0.38F);
            renderLeadingGlowFace(poseStack, glow, BLADE_X_FRONT + BLADE_SURFACE_OFFSET * 2.0F, a, b, settings.glowColor, settings.glowOpacity * shimmer, -0.12F, 0.58F);
            renderLeadingGlowFace(poseStack, glow, BLADE_X_BACK - BLADE_SURFACE_OFFSET * 2.0F, b, a, settings.glowColor, settings.glowOpacity * shimmer, -0.12F, 0.58F);

            float trailingAlpha = settings.edgeOpacity * 0.22F * (0.65F + 0.35F * shimmer);
            renderTrailingGlowFace(poseStack, blade, BLADE_X_FRONT + BLADE_SURFACE_OFFSET, a, b, settings.edgeColor, trailingAlpha);
            renderTrailingGlowFace(poseStack, blade, BLADE_X_BACK - BLADE_SURFACE_OFFSET, b, a, settings.edgeColor, trailingAlpha);
        }
        renderBladeArcs(poseStack, glow, profile, settings, partialTick, shimmer);
    }

    private static void renderBladeFace(PoseStack poseStack, VertexConsumer builder, float x, BladeSlice a, BladeSlice b, int color, float alphaA, float alphaB) {
        addBladeVertex(poseStack, builder, x, a.trailingY, a.z, color, alphaA);
        addBladeVertex(poseStack, builder, x, b.trailingY, b.z, color, alphaB);
        addBladeVertex(poseStack, builder, x, b.leadingY, b.z, color, alphaB);
        addBladeVertex(poseStack, builder, x, a.leadingY, a.z, color, alphaA);
    }

    private static void renderBladeEdge(PoseStack poseStack, VertexConsumer builder, float y0, float z0, float y1, float z1, int color, float alpha) {
        addBladeVertex(poseStack, builder, BLADE_X_BACK, y0, z0, color, alpha);
        addBladeVertex(poseStack, builder, BLADE_X_BACK, y1, z1, color, alpha);
        addBladeVertex(poseStack, builder, BLADE_X_FRONT, y1, z1, color, alpha);
        addBladeVertex(poseStack, builder, BLADE_X_FRONT, y0, z0, color, alpha);
    }

    private static void renderBladeCap(PoseStack poseStack, VertexConsumer builder, BladeSlice slice, int color, float alpha) {
        addBladeVertex(poseStack, builder, BLADE_X_BACK, slice.trailingY, slice.z, color, alpha);
        addBladeVertex(poseStack, builder, BLADE_X_FRONT, slice.trailingY, slice.z, color, alpha);
        addBladeVertex(poseStack, builder, BLADE_X_FRONT, slice.leadingY, slice.z, color, alpha);
        addBladeVertex(poseStack, builder, BLADE_X_BACK, slice.leadingY, slice.z, color, alpha);
    }

    private static void renderLeadingGlowFace(PoseStack poseStack, VertexConsumer builder, float x, BladeSlice a, BladeSlice b, int color, float alpha, float outerOffset, float innerFactor) {
        addBladeVertex(poseStack, builder, x, a.leadingY + outerOffset, a.z, color, alpha);
        addBladeVertex(poseStack, builder, x, b.leadingY + outerOffset, b.z, color, alpha);
        addBladeVertex(poseStack, builder, x, b.blendY(innerFactor), b.z, color, 0.0F);
        addBladeVertex(poseStack, builder, x, a.blendY(innerFactor), a.z, color, 0.0F);
    }

    private static void renderTrailingGlowFace(PoseStack poseStack, VertexConsumer builder, float x, BladeSlice a, BladeSlice b, int color, float alpha) {
        addBladeVertex(poseStack, builder, x, a.trailingY, a.z, color, alpha);
        addBladeVertex(poseStack, builder, x, b.trailingY, b.z, color, alpha);
        addBladeVertex(poseStack, builder, x, b.blendY(0.14F), b.z, color, 0.0F);
        addBladeVertex(poseStack, builder, x, a.blendY(0.14F), a.z, color, 0.0F);
    }

    private static void renderBladeArcs(PoseStack poseStack, VertexConsumer builder, BladeProfile profile, ArcaneBeamConfig.DarksaberSettings settings, float partialTick, float shimmer) {
        Minecraft minecraft = Minecraft.getInstance();
        float time = (minecraft.level == null ? 0.0F : minecraft.level.getGameTime()) + partialTick;
        float period = Math.max(10.0F, settings.shimmerSpeed * 0.7F);
        int arcs = 12;
        for (int i = 0; i < arcs; i++) {
            float track = time / period + i * 0.173F;
            int cycle = (int) Math.floor(track);
            float phase = track - cycle;
            float motion = smoothstep(phase);
            float fade = smoothstep(phase / 0.22F) * smoothstep((1.0F - phase) / 0.28F);
            float start = hash(cycle * 37 + i * 19);
            float end = hash((cycle + 1) * 37 + i * 19);
            float t = EFFECT_START_T + 0.88F * lerp(start, end, motion);
            float inwardStart = hash(cycle * 53 + i * 23);
            float inwardEnd = hash((cycle + 1) * 53 + i * 23);
            float inward = 0.18F + 0.34F * lerp(inwardStart, inwardEnd, motion);
            float alpha = settings.edgeOpacity * fade * (0.62F + 0.38F * shimmer);
            int seed = cycle * 97 + i * 11;
            drawArcStroke(poseStack, builder, profile, t, inward, alpha, settings.edgeColor, seed, BLADE_X_FRONT + BLADE_SURFACE_OFFSET * 2.0F);
            drawArcStroke(poseStack, builder, profile, t, inward, alpha * 0.82F, settings.edgeColor, seed, BLADE_X_BACK - BLADE_SURFACE_OFFSET * 2.0F);
        }
    }

    private static void drawArcStroke(PoseStack poseStack, VertexConsumer builder, BladeProfile profile, float startT, float inward, float alpha, int color, int seed, float x) {
        float lastT = startT;
        float lastInward = 0.02F;
        for (int i = 1; i <= 3; i++) {
            float nextT = clamp01(startT + (hash(seed + i * 17) - 0.5F) * 0.075F + i * 0.012F);
            float nextInward = inward * (i / 3.0F);
            BladePoint a = pointInsideBlade(profile, lastT, lastInward);
            BladePoint b = pointInsideBlade(profile, nextT, nextInward);
            drawBladeLine(poseStack, builder, x, a, b, color, alpha * (1.0F - i * 0.14F), 0.055F);
            lastT = nextT;
            lastInward = nextInward;
        }
    }

    private static void drawBladeLine(PoseStack poseStack, VertexConsumer builder, float x, BladePoint a, BladePoint b, int color, float alpha, float width) {
        float dy = b.y - a.y;
        float dz = b.z - a.z;
        float length = (float) Math.sqrt(dy * dy + dz * dz);
        if (length <= 0.0001F) {
            return;
        }
        float offY = -dz / length * width;
        float offZ = dy / length * width;
        addBladeVertex(poseStack, builder, x, a.y + offY, a.z + offZ, color, alpha);
        addBladeVertex(poseStack, builder, x, b.y + offY, b.z + offZ, color, alpha * 0.65F);
        addBladeVertex(poseStack, builder, x, b.y - offY, b.z - offZ, color, 0.0F);
        addBladeVertex(poseStack, builder, x, a.y - offY, a.z - offZ, color, 0.0F);
    }

    private static BladePoint pointInsideBlade(BladeProfile profile, float t, float inward) {
        BladeSlice slice = profile.slice(clamp01(t));
        return new BladePoint(slice.leadingY + (slice.trailingY - slice.leadingY) * clamp01(inward), slice.z);
    }

    private static BladeSlice bladeSlice(float t) {
        float z = BLADE_BASE_Z + (BLADE_TIP_Z - BLADE_BASE_Z) * t;
        float tipY = 4.78F;

        float trailingBaseY = 3.96F;
        float leadingBaseY = 6.28F;

        float leadingTaper = smoothstep((t - 0.18F) / 0.82F);
        float trailingTaper = smoothstep((t - 0.72F) / 0.28F);
        float leadingY = leadingBaseY + (tipY - leadingBaseY) * leadingTaper;
        float trailingY = trailingBaseY + (tipY - trailingBaseY) * trailingTaper;

        float finalTip = smoothstep((t - 0.93F) / 0.07F);
        leadingY = leadingY + (tipY - leadingY) * finalTip;
        trailingY = trailingY + (tipY - trailingY) * finalTip;
        return new BladeSlice(trailingY, leadingY, z);
    }

    private static void addBladeVertex(PoseStack poseStack, VertexConsumer builder, float x, float y, float z, int color, float alpha) {
        Matrix4f matrix = poseStack.last().pose();
        builder.vertex(matrix, x / 16.0F, y / 16.0F, z / 16.0F)
                .color(red(color), green(color), blue(color), Math.max(0.0F, Math.min(1.0F, alpha)))
                .endVertex();
    }

    private static void addMeshVertex(PoseStack poseStack, VertexConsumer builder, MeshVertex vertex, int color, float alpha) {
        addBladeVertex(poseStack, builder, vertex.x, vertex.y, vertex.z, color, alpha);
    }

    private static float smoothstep(float value) {
        float t = clamp01(value);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static float hash(int seed) {
        double value = Math.sin(seed * 12.9898D) * 43758.5453D;
        return (float) (value - Math.floor(value));
    }

    private static void renderElement(PoseStack poseStack, VertexConsumer builder, Element element, int color, float alpha, int packedLight, int packedOverlay, boolean textured) {
        Vec3[] vertices = element.vertices();
        addFace(poseStack, builder, vertices, 0, 1, 2, 3, element.face("north"), color, alpha, packedLight, packedOverlay, textured);
        addFace(poseStack, builder, vertices, 5, 4, 7, 6, element.face("south"), color, alpha, packedLight, packedOverlay, textured);
        addFace(poseStack, builder, vertices, 4, 0, 3, 7, element.face("west"), color, alpha, packedLight, packedOverlay, textured);
        addFace(poseStack, builder, vertices, 1, 5, 6, 2, element.face("east"), color, alpha, packedLight, packedOverlay, textured);
        addFace(poseStack, builder, vertices, 3, 2, 6, 7, element.face("up"), color, alpha, packedLight, packedOverlay, textured);
        addFace(poseStack, builder, vertices, 4, 5, 1, 0, element.face("down"), color, alpha, packedLight, packedOverlay, textured);
    }

    private static void addFace(PoseStack poseStack, VertexConsumer builder, Vec3[] vertices, int a, int b, int c, int d, FaceUv uv, int color, float alpha, int packedLight, int packedOverlay, boolean textured) {
        float u1 = textured && uv != null ? uv.u1 / TEXTURE_SIZE : 0.0F;
        float v1 = textured && uv != null ? uv.v1 / TEXTURE_SIZE : 0.0F;
        float u2 = textured && uv != null ? uv.u2 / TEXTURE_SIZE : 1.0F;
        float v2 = textured && uv != null ? uv.v2 / TEXTURE_SIZE : 1.0F;
        addVertex(poseStack, builder, vertices[a], color, alpha, u1, v1, packedLight, packedOverlay);
        addVertex(poseStack, builder, vertices[b], color, alpha, u2, v1, packedLight, packedOverlay);
        addVertex(poseStack, builder, vertices[c], color, alpha, u2, v2, packedLight, packedOverlay);
        addVertex(poseStack, builder, vertices[d], color, alpha, u1, v2, packedLight, packedOverlay);
    }

    private static void addVertex(PoseStack poseStack, VertexConsumer builder, Vec3 point, int color, float alpha, float u, float v, int packedLight, int packedOverlay) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        builder.vertex(matrix, (float) point.x / 16.0F, (float) point.y / 16.0F, (float) point.z / 16.0F)
                .color(red(color), green(color), blue(color), alpha)
                .uv(u, v)
                .overlayCoords(packedOverlay)
                .uv2(packedLight)
                .normal(normal, 0.0F, 1.0F, 0.0F)
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

    private static DarksaberModel model() {
        if (cachedModel != null) {
            return cachedModel;
        }
        try {
            try (Reader reader = new InputStreamReader(Minecraft.getInstance().getResourceManager().getResource(MODEL).getInputStream(), StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                List<Element> elements = new ArrayList<>();
                JsonArray jsonElements = root.getAsJsonArray("elements");
                for (JsonElement jsonElement : jsonElements) {
                    JsonObject object = jsonElement.getAsJsonObject();
                    String name = object.has("name") ? object.get("name").getAsString().toLowerCase(Locale.ROOT) : "";
                    Group group = switch (name) {
                        case "blade" -> Group.BLADE;
                        case "white" -> Group.WHITE;
                        default -> Group.HILT;
                    };
                    elements.add(Element.fromJson(object, group));
                }
                cachedModel = DarksaberModel.from(elements, loadBladeMesh());
            }
        } catch (Exception ignored) {
            cachedModel = DarksaberModel.EMPTY;
        }
        return cachedModel == DarksaberModel.EMPTY ? null : cachedModel;
    }

    private static BladeMesh loadBladeMesh() {
        try (Reader reader = new InputStreamReader(Minecraft.getInstance().getResourceManager().getResource(BLADE_MESH).getInputStream(), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            List<MeshVertex> vertices = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray("vertices")) {
                JsonArray array = element.getAsJsonArray();
                vertices.add(new MeshVertex(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()));
            }
            List<Triangle> triangles = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray("triangles")) {
                JsonArray array = element.getAsJsonArray();
                triangles.add(new Triangle(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt()));
            }
            return BladeMesh.from(vertices, triangles);
        } catch (Exception ignored) {
            return BladeMesh.EMPTY;
        }
    }

    private static RenderType createBladeRenderType() {
        CompositeState state = CompositeState.builder()
                .setShaderState(POSITION_COLOR_SHADER)
                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false);
        return RenderType.create("arcanebeam_darksaber_blade", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 512, false, true, state);
    }

    private static RenderType createBladeTriangleRenderType() {
        CompositeState state = CompositeState.builder()
                .setShaderState(POSITION_COLOR_SHADER)
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .createCompositeState(false);
        return RenderType.create("arcanebeam_darksaber_blade_mesh", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES, 512, false, true, state);
    }

    private static RenderType createGlowRenderType() {
        CompositeState state = CompositeState.builder()
                .setShaderState(POSITION_COLOR_SHADER)
                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false);
        return RenderType.create("arcanebeam_darksaber_glow", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 512, false, true, state);
    }

    private enum Group {
        HILT,
        BLADE,
        WHITE
    }

    private record FaceUv(float u1, float v1, float u2, float v2) {
        private static FaceUv fromJson(JsonObject faces, String key) {
            if (faces == null || !faces.has(key)) {
                return null;
            }
            JsonArray uv = faces.getAsJsonObject(key).getAsJsonArray("uv");
            return new FaceUv(uv.get(0).getAsFloat(), uv.get(1).getAsFloat(), uv.get(2).getAsFloat(), uv.get(3).getAsFloat());
        }
    }

    private record Element(Group group, Vec3 from, Vec3 to, Vec3 rotation, Vec3 origin, FaceUv north, FaceUv east, FaceUv south, FaceUv west, FaceUv up, FaceUv down) {
        private static Element fromJson(JsonObject object, Group group) {
            JsonObject faces = object.has("faces") ? object.getAsJsonObject("faces") : null;
            return new Element(group,
                    vec(object.getAsJsonArray("from")),
                    vec(object.getAsJsonArray("to")),
                    object.has("rotation") ? vec(object.getAsJsonArray("rotation")) : Vec3.ZERO,
                    object.has("origin") ? vec(object.getAsJsonArray("origin")) : Vec3.ZERO,
                    FaceUv.fromJson(faces, "north"),
                    FaceUv.fromJson(faces, "east"),
                    FaceUv.fromJson(faces, "south"),
                    FaceUv.fromJson(faces, "west"),
                    FaceUv.fromJson(faces, "up"),
                    FaceUv.fromJson(faces, "down"));
        }

        private FaceUv face(String key) {
            return switch (key) {
                case "north" -> north;
                case "east" -> east;
                case "south" -> south;
                case "west" -> west;
                case "up" -> up;
                case "down" -> down;
                default -> null;
            };
        }

        private Vec3[] vertices() {
            double minX = Math.min(from.x, to.x);
            double minY = Math.min(from.y, to.y);
            double minZ = Math.min(from.z, to.z);
            double maxX = Math.max(from.x, to.x);
            double maxY = Math.max(from.y, to.y);
            double maxZ = Math.max(from.z, to.z);
            return new Vec3[]{
                    rotate(new Vec3(minX, minY, minZ)),
                    rotate(new Vec3(maxX, minY, minZ)),
                    rotate(new Vec3(maxX, maxY, minZ)),
                    rotate(new Vec3(minX, maxY, minZ)),
                    rotate(new Vec3(minX, minY, maxZ)),
                    rotate(new Vec3(maxX, minY, maxZ)),
                    rotate(new Vec3(maxX, maxY, maxZ)),
                    rotate(new Vec3(minX, maxY, maxZ))
            };
        }

        private Vec3 rotate(Vec3 point) {
            Vec3 relative = point.subtract(origin);
            relative = rotateX(relative, rotation.x);
            relative = rotateY(relative, rotation.y);
            relative = rotateZ(relative, rotation.z);
            return origin.add(relative);
        }

        private static Vec3 vec(JsonArray array) {
            return new Vec3(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
        }

        private static Vec3 rotateX(Vec3 point, double degrees) {
            if (degrees == 0.0D) {
                return point;
            }
            double radians = Math.toRadians(degrees);
            double cos = Math.cos(radians);
            double sin = Math.sin(radians);
            return new Vec3(point.x, point.y * cos - point.z * sin, point.y * sin + point.z * cos);
        }

        private static Vec3 rotateY(Vec3 point, double degrees) {
            if (degrees == 0.0D) {
                return point;
            }
            double radians = Math.toRadians(degrees);
            double cos = Math.cos(radians);
            double sin = Math.sin(radians);
            return new Vec3(point.x * cos + point.z * sin, point.y, -point.x * sin + point.z * cos);
        }

        private static Vec3 rotateZ(Vec3 point, double degrees) {
            if (degrees == 0.0D) {
                return point;
            }
            double radians = Math.toRadians(degrees);
            double cos = Math.cos(radians);
            double sin = Math.sin(radians);
            return new Vec3(point.x * cos - point.y * sin, point.x * sin + point.y * cos, point.z);
        }
    }

    private record BladeSlice(float trailingY, float leadingY, float z) {
        private float blendY(float leadingFactor) {
            return leadingY + (trailingY - leadingY) * leadingFactor;
        }
    }

    private record BladePoint(float y, float z) {
    }

    private interface BladeProfile {
        BladeSlice slice(float t);
    }

    private record MeshVertex(float x, float y, float z) {
    }

    private record Triangle(int a, int b, int c) {
    }

    private record BladeMesh(List<MeshVertex> vertices, List<Triangle> triangles, BladeSlice[] profile) {
        private static final BladeMesh EMPTY = new BladeMesh(List.of(), List.of(), new BladeSlice[0]);

        private static BladeMesh from(List<MeshVertex> vertices, List<Triangle> triangles) {
            if (vertices.isEmpty() || triangles.isEmpty()) {
                return EMPTY;
            }
            return new BladeMesh(vertices, triangles, buildProfile(vertices, triangles));
        }

        private BladeSlice slice(float t) {
            if (profile.length == 0) {
                return bladeSlice(t);
            }
            float clamped = clamp01(t);
            float scaled = clamped * (profile.length - 1);
            int lower = Math.min(profile.length - 1, (int) Math.floor(scaled));
            int upper = Math.min(profile.length - 1, lower + 1);
            float blend = scaled - lower;
            BladeSlice a = profile[lower];
            BladeSlice b = profile[upper];
            return new BladeSlice(
                    lerp(a.trailingY, b.trailingY, blend),
                    lerp(a.leadingY, b.leadingY, blend),
                    lerp(a.z, b.z, blend));
        }

        private static BladeSlice[] buildProfile(List<MeshVertex> vertices, List<Triangle> triangles) {
            float minZ = Float.MAX_VALUE;
            float maxZ = -Float.MAX_VALUE;
            for (MeshVertex vertex : vertices) {
                minZ = Math.min(minZ, vertex.z);
                maxZ = Math.max(maxZ, vertex.z);
            }
            int samples = 33;
            BladeSlice[] slices = new BladeSlice[samples];
            for (int i = 0; i < samples; i++) {
                float t = i / (float) (samples - 1);
                float z = maxZ + (minZ - maxZ) * t;
                float trailing = Float.MAX_VALUE;
                float leading = -Float.MAX_VALUE;
                for (Triangle triangle : triangles) {
                    MeshVertex a = vertices.get(triangle.a);
                    MeshVertex b = vertices.get(triangle.b);
                    MeshVertex c = vertices.get(triangle.c);
                    float y = intersectionY(a, b, z);
                    if (!Float.isNaN(y)) {
                        trailing = Math.min(trailing, y);
                        leading = Math.max(leading, y);
                    }
                    y = intersectionY(b, c, z);
                    if (!Float.isNaN(y)) {
                        trailing = Math.min(trailing, y);
                        leading = Math.max(leading, y);
                    }
                    y = intersectionY(c, a, z);
                    if (!Float.isNaN(y)) {
                        trailing = Math.min(trailing, y);
                        leading = Math.max(leading, y);
                    }
                }
                if (trailing == Float.MAX_VALUE || leading == -Float.MAX_VALUE) {
                    BladeSlice fallback = bladeSlice(t);
                    trailing = fallback.trailingY;
                    leading = fallback.leadingY;
                }
                slices[i] = new BladeSlice(trailing, leading, z);
            }
            return slices;
        }

        private static float intersectionY(MeshVertex a, MeshVertex b, float z) {
            float min = Math.min(a.z, b.z);
            float max = Math.max(a.z, b.z);
            if (z < min || z > max || Math.abs(a.z - b.z) < 0.00001F) {
                return Float.NaN;
            }
            float t = (z - a.z) / (b.z - a.z);
            return a.y + (b.y - a.y) * t;
        }
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private record DarksaberModel(List<Element> elements, BladeMesh bladeMesh, double centerX, double centerY, double centerZ) {
        private static final DarksaberModel EMPTY = new DarksaberModel(List.of(), BladeMesh.EMPTY, 0.0D, 0.0D, 0.0D);

        private static DarksaberModel from(List<Element> elements, BladeMesh bladeMesh) {
            if (elements.isEmpty()) {
                return EMPTY;
            }
            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double maxY = -Double.MAX_VALUE;
            double maxZ = -Double.MAX_VALUE;
            for (Element element : elements) {
                for (Vec3 vertex : element.vertices()) {
                    minX = Math.min(minX, vertex.x);
                    minY = Math.min(minY, vertex.y);
                    minZ = Math.min(minZ, vertex.z);
                    maxX = Math.max(maxX, vertex.x);
                    maxY = Math.max(maxY, vertex.y);
                    maxZ = Math.max(maxZ, vertex.z);
                }
            }
            return new DarksaberModel(elements, bladeMesh, (minX + maxX) * 0.5D, (minY + maxY) * 0.5D, (minZ + maxZ) * 0.5D);
        }
    }
}
