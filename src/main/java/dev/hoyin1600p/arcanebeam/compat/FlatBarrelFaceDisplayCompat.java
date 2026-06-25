package dev.hoyin1600p.arcanebeam.compat;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public final class FlatBarrelFaceDisplayCompat {
    private static final float MIN_WIDE_AXIS = 0.45F;
    private static final float MAX_THIN_AXIS = 0.08F;
    private static final ResourceLocation SPORE_BLOSSOM = new ResourceLocation("minecraft", "spore_blossom");
    private static final ResourceLocation SPORE_BLOSSOM_FLOWER = new ResourceLocation("minecraft", "block/spore_blossom");
    private static final ResourceLocation SPORE_BLOSSOM_BASE = new ResourceLocation("minecraft", "block/spore_blossom_base");
    private static final Set<ResourceLocation> EXISTING_BLOCK_TEXTURES = ConcurrentHashMap.newKeySet();
    private static final Set<ResourceLocation> MISSING_BLOCK_TEXTURES = ConcurrentHashMap.newKeySet();

    private FlatBarrelFaceDisplayCompat() {
    }

    public static boolean shouldRenderAsFlatSprite(ItemStack stack, BakedModel model) {
        if (!(stack.getItem() instanceof BlockItem) || CompressiumDisplayCompat.isCompressiumItem(stack)) {
            return false;
        }

        List<BakedQuad> quads = collectQuads(model);
        if (quads.isEmpty()) {
            return false;
        }

        Bounds bounds = Bounds.from(quads);
        if (bounds.isVisiblyFlat()) {
            return true;
        }

        EnumSet<Direction> directions = EnumSet.noneOf(Direction.class);
        for (BakedQuad quad : quads) {
            directions.add(quad.getDirection());
        }

        return directions.size() <= 2 && directions.stream().anyMatch(direction -> directions.contains(direction.getOpposite()));
    }

    public static double flatSpriteDisplayItemOffset() {
        return -CompressiumDisplayCompat.BARREL_FACE_OUTSET;
    }

    public static TextureAtlasSprite sprite(ItemStack stack, BakedModel model) {
        ResourceLocation texture = blockTexture(stack);
        if (texture != null) {
            return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        }
        return model.getParticleIcon();
    }

    public static List<SpriteLayer> spriteLayers(ItemStack stack, BakedModel model) {
        if (isSporeBlossom(stack)) {
            return List.of(
                    new SpriteLayer(textureSprite(SPORE_BLOSSOM_FLOWER), 1.0F, 0.0F, -0.08F),
                    new SpriteLayer(textureSprite(SPORE_BLOSSOM_BASE), 0.55F, 0.0F, 0.28F)
            );
        }
        return Collections.singletonList(new SpriteLayer(sprite(stack, model), 1.0F, 0.0F, 0.0F));
    }

    public static boolean isSporeBlossom(ItemStack stack) {
        ResourceLocation itemKey = Registry.ITEM.getKey(stack.getItem());
        return SPORE_BLOSSOM.equals(itemKey);
    }

    private static TextureAtlasSprite textureSprite(ResourceLocation texture) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
    }

    private static ResourceLocation blockTexture(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return null;
        }

        ResourceLocation blockKey = Registry.BLOCK.getKey(blockItem.getBlock());
        if (blockKey == null) {
            return null;
        }

        ResourceLocation texture = new ResourceLocation(blockKey.getNamespace(), "block/" + blockKey.getPath());
        if (EXISTING_BLOCK_TEXTURES.contains(texture)) {
            return texture;
        }
        if (MISSING_BLOCK_TEXTURES.contains(texture)) {
            return null;
        }

        ResourceLocation textureFile = new ResourceLocation(texture.getNamespace(), "textures/" + texture.getPath() + ".png");
        if (Minecraft.getInstance().getResourceManager().hasResource(textureFile)) {
            EXISTING_BLOCK_TEXTURES.add(texture);
            return texture;
        }

        MISSING_BLOCK_TEXTURES.add(texture);
        return null;
    }

    public record SpriteLayer(TextureAtlasSprite sprite, float scale, float xOffset, float yOffset) {
    }

    private static List<BakedQuad> collectQuads(BakedModel model) {
        Random random = new Random(42L);
        List<BakedQuad> quads = new ArrayList<>(model.getQuads(null, null, random));
        for (Direction direction : Direction.values()) {
            quads.addAll(model.getQuads(null, direction, random));
        }
        return quads;
    }

    private record Bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        static Bounds from(List<BakedQuad> quads) {
            float minX = Float.POSITIVE_INFINITY;
            float minY = Float.POSITIVE_INFINITY;
            float minZ = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY;
            float maxY = Float.NEGATIVE_INFINITY;
            float maxZ = Float.NEGATIVE_INFINITY;

            for (BakedQuad quad : quads) {
                int[] vertices = quad.getVertices();
                for (int vertex = 0; vertex < 4; vertex++) {
                    int offset = vertex * 8;
                    float x = Float.intBitsToFloat(vertices[offset]);
                    float y = Float.intBitsToFloat(vertices[offset + 1]);
                    float z = Float.intBitsToFloat(vertices[offset + 2]);
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    minZ = Math.min(minZ, z);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                    maxZ = Math.max(maxZ, z);
                }
            }

            return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
        }

        boolean isVisiblyFlat() {
            int wideAxes = 0;
            int thinAxes = 0;
            for (float size : new float[] {maxX - minX, maxY - minY, maxZ - minZ}) {
                if (size >= MIN_WIDE_AXIS) {
                    wideAxes++;
                } else if (size <= MAX_THIN_AXIS) {
                    thinAxes++;
                }
            }
            return wideAxes >= 2 && thinAxes >= 1;
        }
    }
}
