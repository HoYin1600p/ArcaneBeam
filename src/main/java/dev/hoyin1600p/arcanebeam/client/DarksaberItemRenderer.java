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
    private static final ResourceLocation TEXTURE = new ResourceLocation(ArcaneBeam.MOD_ID, "textures/item/darksaber/darksaber2.png");
    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation(ArcaneBeam.MOD_ID, "textures/entity/white.png");
    private static final ResourceLocation VAULT_SWORD = new ResourceLocation("the_vault", "sword");
    private static final String GEAR_MODEL_TAG = "the_vault:gear_model";
    private static final RenderType HILT = RenderType.entityCutoutNoCull(TEXTURE);
    private static final RenderType BLADE = RenderType.entityTranslucent(WHITE_TEXTURE);
    private static final RenderType GLOW = createGlowRenderType();
    private static final int TEXTURE_SIZE = 64;
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
        VertexConsumer blade = buffer.getBuffer(BLADE);
        VertexConsumer glow = buffer.getBuffer(GLOW);
        for (Element element : model.elements) {
            if (element.group == Group.HILT) {
                renderElement(poseStack, hilt, element, 0xFFFFFF, 1.0F, packedLight, packedOverlay, true);
            } else if (element.group == Group.BLADE) {
                renderElement(poseStack, blade, element, settings.coreColor, settings.coreOpacity, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, false);
            } else if (element.group == Group.WHITE) {
                renderElement(poseStack, blade, element, settings.edgeColor, settings.edgeOpacity * shimmer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, false);
                poseStack.pushPose();
                poseStack.scale(1.015F, 1.015F, 1.015F);
                renderElement(poseStack, glow, element, settings.glowColor, settings.glowOpacity * shimmer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, false);
                poseStack.popPose();
            }
        }
        poseStack.popPose();
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
                cachedModel = DarksaberModel.from(elements);
            }
        } catch (Exception ignored) {
            cachedModel = DarksaberModel.EMPTY;
        }
        return cachedModel == DarksaberModel.EMPTY ? null : cachedModel;
    }

    private static RenderType createGlowRenderType() {
        CompositeState state = CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                .setTextureState(new TextureStateShard(WHITE_TEXTURE, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .createCompositeState(false);
        return RenderType.create("arcanebeam_darksaber_glow", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 512, false, true, state);
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

    private record DarksaberModel(List<Element> elements, double centerX, double centerY, double centerZ) {
        private static final DarksaberModel EMPTY = new DarksaberModel(List.of(), 0.0D, 0.0D, 0.0D);

        private static DarksaberModel from(List<Element> elements) {
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
            return new DarksaberModel(elements, (minX + maxX) * 0.5D, (minY + maxY) * 0.5D, (minZ + maxZ) * 0.5D);
        }
    }
}
