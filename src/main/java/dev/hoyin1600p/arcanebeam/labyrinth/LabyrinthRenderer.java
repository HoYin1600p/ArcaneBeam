package dev.hoyin1600p.arcanebeam.labyrinth;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import dev.hoyin1600p.arcanebeam.config.model.LabyrinthSettings;
import dev.hoyin1600p.arcanebeam.config.model.ShaderCompatibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import static dev.hoyin1600p.arcanebeam.util.ArcaneBeamIds.RENDER_TYPE_PREFIX;
import static dev.hoyin1600p.arcanebeam.util.ArcaneBeamIds.WHITE_TEXTURE_PATH;
import static dev.hoyin1600p.arcanebeam.util.ColorChannels.blue;
import static dev.hoyin1600p.arcanebeam.util.ColorChannels.green;
import static dev.hoyin1600p.arcanebeam.util.ColorChannels.red;

/**
 * Draws the route to the labyrinth exit as flat bars (procedural quads, like the Storm Arrow targeting circle). Two
 * render types: an unlit one for a full-bright route, and a lit one (block format, translucent shader) for a route that
 * follows the world light and for shader packs. Positions are subtracted from the camera in double and narrowed once.
 * Nothing is allocated per frame: the per-frame work is loops over the piece arrays.
 */
final class LabyrinthRenderer extends RenderType {
    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation(ArcaneBeam.MOD_ID, WHITE_TEXTURE_PATH);
    private static final RenderType UNLIT = createUnlit();
    private static final RenderType LIT = createLit();
    private static final RenderType BEAM = createBeam();

    /** Spacing of the flow chevrons along the route, in blocks. */
    static final float CHEVRON_SPACING = 2.0F;
    /** How fast the chevrons travel, in blocks per second. */
    static final float CHEVRON_SPEED = 3.0F;
    /** The route is lifted a hair above its elevation so the chevrons sit on it. */
    private static final float ROUTE_LIFT = 0.02F;
    /** Scratch values reused every frame so nothing is allocated. */
    private static final float[] SIDE = new float[2];
    private static final float[] CHEVRON_RGB = new float[3];
    private static final float[] ROUTE_RGB = new float[3];

    private LabyrinthRenderer(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling,
            boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    /** Result of the per-frame selection: which route pieces are drawn. */
    private static int[] routeDraw = new int[0];

    static void render(PoseStack stack, Vec3 camera, Frustum frustum, float partialTick, long gameTime, LabyrinthSession session,
            LabyrinthSettings cfg, double playerX, double playerZ, boolean compat) {
        if (!session.hasRoute) {
            return;
        }
        PieceLayer route = session.route;
        double maxSq = (double) cfg.maxDistance * cfg.maxDistance;
        int routeCount = select(route, frustum, camera, maxSq);
        if (routeCount == 0 && route.pieces.count == 0) {
            return;
        }
        // Shader packs get the beacon-beam program like every other ArcaneBeam effect: the translucent program is
        // swapped for the pack's water pass, which writes extra buffers. The beam program is always full bright.
        RenderType type = compat ? BEAM : !cfg.fullbright ? LIT : UNLIT;

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        stack.pushPose();
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        float[] rgb = ROUTE_RGB;
        rgb[0] = red(cfg.routeColor);
        rgb[1] = green(cfg.routeColor);
        rgb[2] = blue(cfg.routeColor);

        // The feature guard turns an exception into "feature disabled", but the level pose stack and the shared buffer source
        // outlive this call: leave neither pushed nor mid-batch, or vanilla rendering fails later outside the guard.
        try {
            VertexConsumer vc = buffer.getBuffer(type);
            drawRoute(vc, pose, normal, type != UNLIT,session, route, routeCount, camera, rgb, cfg.opacity, cfg.fullbright,
                    cfg.width * 0.5F, cfg.thickness * 0.5F, cfg.elevation, cfg.chevrons, gameTime, partialTick, playerX, playerZ, maxSq);
            buffer.endBatch(type);
        } catch (RuntimeException | LinkageError failure) {
            FailedDraw.closeBatches(buffer::endBatch, type, type);
            throw failure;
        } finally {
            stack.popPose();
        }
    }

    /** Fills the draw list with the pieces that are known visible, in range and inside the view frustum. */
    private static int select(PieceLayer layer, Frustum frustum, Vec3 camera, double maxSq) {
        LinePieces p = layer.pieces;
        if (routeDraw.length < p.count) {
            routeDraw = new int[p.count];
        }
        int[] list = routeDraw;
        int n = 0;
        for (int i = 0; i < p.count; i++) {
            if (!layer.culler.visible(i)) {
                continue;
            }
            double dx = p.midX(i) - camera.x;
            double dy = p.midY(i) - camera.y;
            double dz = p.midZ(i) - camera.z;
            if (dx * dx + dy * dy + dz * dz > maxSq) {
                continue;
            }
            if (layer.bounds(i) != null && !frustum.isVisible(layer.bounds(i))) {
                continue;
            }
            list[n++] = i;
        }
        return n;
    }

    private static void drawRoute(VertexConsumer vc, Matrix4f pose, Matrix3f normal, boolean block, LabyrinthSession session, PieceLayer route,
            int count, Vec3 camera, float[] rgb, float alpha, boolean full, float hw, float ht, float elevation, boolean chevrons,
            long gameTime, float partialTick, double playerX, double playerZ, double maxSq) {
        LinePieces p = route.pieces;
        int[] draw = routeDraw;
        float lift = elevation + ROUTE_LIFT;
        for (int k = 0; k < count; k++) {
            int i = draw[k];
            int light = full ? PieceLayer.FULL_LIGHT : route.light[i];
            bar(vc, pose, normal, block, rgb, alpha, light,
                    (float) (p.startX(i) - camera.x), (float) (p.startY(i) + lift - camera.y), (float) (p.startZ(i) - camera.z),
                    (float) (p.endX(i) - camera.x), (float) (p.endY(i) + lift - camera.y), (float) (p.endZ(i) - camera.z),
                    hw, ht);
        }
        if (p.count == 0) {
            return;
        }
        // The route starts at the player: a segment from the player's feet to the first route vertex.
        double hx = p.startX(0);
        double hz = p.startZ(0);
        double along = Math.sqrt((hx - playerX) * (hx - playerX) + (hz - playerZ) * (hz - playerZ));
        if (along > 0.25 && along < 12.0) {
            bar(vc, pose, normal, block, rgb, alpha, full ? PieceLayer.FULL_LIGHT : route.light[0],
                    (float) (playerX - camera.x), (float) (session.headSurfaceY + lift - camera.y), (float) (playerZ - camera.z),
                    (float) (hx - camera.x), (float) (p.startY(0) + lift - camera.y), (float) (hz - camera.z),
                    hw, ht);
        }
        if (chevrons) {
            drawChevrons(vc, pose, normal, block, route, camera, rgb, alpha, full, hw, ht, lift, gameTime, partialTick, maxSq);
        }
    }

    /** Small V shapes that travel along the route towards the exit. */
    private static void drawChevrons(VertexConsumer vc, Matrix4f pose, Matrix3f normal, boolean block, PieceLayer route, Vec3 camera,
            float[] rgb, float alpha, boolean full, float hw, float ht, float lift, long gameTime, float partialTick, double maxSq) {
        LinePieces p = route.pieces;
        float total = p.startLength(p.count - 1) + (float) p.length(p.count - 1);
        float phase = ((gameTime + partialTick) * 0.05F * CHEVRON_SPEED) % CHEVRON_SPACING;
        float[] tint = CHEVRON_RGB;
        tint[0] = 0.55F + rgb[0] * 0.45F;
        tint[1] = 0.55F + rgb[1] * 0.45F;
        tint[2] = 0.55F + rgb[2] * 0.45F;
        int piece = 0;
        for (float s = phase; s < total; s += CHEVRON_SPACING) {
            while (piece + 1 < p.count && p.startLength(piece + 1) <= s) {
                piece++;
            }
            double len = p.length(piece);
            if (len < 1.0E-4 || !route.culler.visible(piece)) {
                continue;
            }
            float t = (float) ((s - p.startLength(piece)) / len);
            double dx = p.endX(piece) - p.startX(piece);
            double dy = p.endY(piece) - p.startY(piece);
            double dz = p.endZ(piece) - p.startZ(piece);
            float tx = (float) (dx / len);
            float ty = (float) (dy / len);
            float tz = (float) (dz / len);
            float px = (float) (p.startX(piece) + dx * t - camera.x);
            float py = (float) (p.startY(piece) + dy * t + lift + 0.012F - camera.y);
            float pz = (float) (p.startZ(piece) + dz * t - camera.z);
            if ((double) px * px + (double) py * py + (double) pz * pz > maxSq) {
                continue;
            }
            float[] side = sideOf(tx, tz, px, pz);
            float tip = 0.16F;
            float back = 0.12F;
            float spread = 0.24F;
            int lightValue = full ? PieceLayer.FULL_LIGHT : route.light[piece];
            float kx = px + tx * tip;
            float ky = py + ty * tip;
            float kz = pz + tz * tip;
            float lx = px - tx * back + side[0] * spread;
            float ly = py - ty * back;
            float lz = pz - tz * back + side[1] * spread;
            float rx = px - tx * back - side[0] * spread;
            float rz = pz - tz * back - side[1] * spread;
            bar(vc, pose, normal, block, tint, alpha, lightValue, kx, ky, kz, lx, ly, lz, hw * 0.9F, ht);
            bar(vc, pose, normal, block, tint, alpha, lightValue, kx, ky, kz, rx, ly, rz, hw * 0.9F, ht);
        }
    }

    /** The horizontal direction across a line travelling along (tx, tz); toward the camera sides for vertical lines. */
    private static float[] sideOf(float tx, float tz, float relX, float relZ) {
        float len = Mth.sqrt(tx * tx + tz * tz);
        if (len > 1.0E-3F) {
            SIDE[0] = -tz / len;
            SIDE[1] = tx / len;
            return SIDE;
        }
        float rl = Mth.sqrt(relX * relX + relZ * relZ);
        SIDE[0] = rl > 1.0E-4F ? -relZ / rl : 1.0F;
        SIDE[1] = rl > 1.0E-4F ? relX / rl : 0.0F;
        return SIDE;
    }

    /**
     * One bar from A to B (camera-relative): a box of horizontal width 2 * hw across the line and vertical thickness
     * 2 * ht. Both faces and both sides are drawn since the render types do not cull.
     */
    private static void bar(VertexConsumer vc, Matrix4f pose, Matrix3f normal, boolean block, float[] rgb, float alpha, int light,
            float ax, float ay, float az, float bx, float by, float bz, float hw, float ht) {
        float dx = bx - ax;
        float dz = bz - az;
        float len = Mth.sqrt(dx * dx + dz * dz);
        float sx;
        float sz;
        if (len > 1.0E-3F) {
            sx = -dz / len * hw;
            sz = dx / len * hw;
        } else {
            // A vertical piece: widen it across the view direction so it stays visible.
            float cx = (ax + bx) * 0.5F;
            float cz = (az + bz) * 0.5F;
            float cl = Mth.sqrt(cx * cx + cz * cz);
            sx = cl > 1.0E-4F ? -cz / cl * hw : hw;
            sz = cl > 1.0E-4F ? cx / cl * hw : 0.0F;
        }
        float r = rgb[0];
        float g = rgb[1];
        float b = rgb[2];
        // top
        quad(vc, pose, normal, block, r, g, b, alpha, light, 0.0F, 1.0F, 0.0F,
                ax - sx, ay + ht, az - sz, ax + sx, ay + ht, az + sz, bx + sx, by + ht, bz + sz, bx - sx, by + ht, bz - sz);
        // bottom
        quad(vc, pose, normal, block, r, g, b, alpha, light, 0.0F, -1.0F, 0.0F,
                ax - sx, ay - ht, az - sz, bx - sx, by - ht, bz - sz, bx + sx, by - ht, bz + sz, ax + sx, ay - ht, az + sz);
        // sides
        float nx = sx == 0.0F && sz == 0.0F ? 1.0F : sx / hw;
        float nz = sx == 0.0F && sz == 0.0F ? 0.0F : sz / hw;
        quad(vc, pose, normal, block, r, g, b, alpha, light, nx, 0.0F, nz,
                ax + sx, ay - ht, az + sz, bx + sx, by - ht, bz + sz, bx + sx, by + ht, bz + sz, ax + sx, ay + ht, az + sz);
        quad(vc, pose, normal, block, r, g, b, alpha, light, -nx, 0.0F, -nz,
                ax - sx, ay - ht, az - sz, ax - sx, ay + ht, az - sz, bx - sx, by + ht, bz - sz, bx - sx, by - ht, bz - sz);
    }

    private static void quad(VertexConsumer vc, Matrix4f pose, Matrix3f normal, boolean block, float r, float g, float b, float a, int light,
            float nx, float ny, float nz, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3,
            float x4, float y4, float z4) {
        vertex(vc, pose, normal, block, r, g, b, a, light, nx, ny, nz, x1, y1, z1, 0.0F, 0.0F);
        vertex(vc, pose, normal, block, r, g, b, a, light, nx, ny, nz, x2, y2, z2, 0.0F, 1.0F);
        vertex(vc, pose, normal, block, r, g, b, a, light, nx, ny, nz, x3, y3, z3, 1.0F, 1.0F);
        vertex(vc, pose, normal, block, r, g, b, a, light, nx, ny, nz, x4, y4, z4, 1.0F, 0.0F);
    }

    private static void vertex(VertexConsumer vc, Matrix4f pose, Matrix3f normal, boolean block, float r, float g, float b, float a, int light,
            float nx, float ny, float nz, float x, float y, float z, float u, float v) {
        if (block) {
            vc.vertex(pose, x, y, z).color(r, g, b, a).uv(u, v).uv2(light).normal(normal, nx, ny, nz).endVertex();
        } else {
            vc.vertex(pose, x, y, z).color(r, g, b, a).uv(u, v).endVertex();
        }
    }

    static boolean compat() {
        return ShaderCompatibility.fromId(dev.hoyin1600p.arcanebeam.config.ArcaneBeamConfig.INSTANCE.shaderCompatibility)
                == ShaderCompatibility.ON;
    }

    private static RenderType createUnlit() {
        CompositeState state = CompositeState.builder()
                .setShaderState(POSITION_COLOR_TEX_SHADER)
                .setTextureState(new TextureStateShard(WHITE_TEXTURE, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false);
        return RenderType.create(RENDER_TYPE_PREFIX + "labyrinth_unlit", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 1024,
                false, true, state);
    }

    private static RenderType createLit() {
        CompositeState state = CompositeState.builder()
                .setShaderState(RENDERTYPE_TRANSLUCENT_SHADER)
                .setTextureState(new TextureStateShard(WHITE_TEXTURE, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false);
        return RenderType.create(RENDER_TYPE_PREFIX + "labyrinth_lit", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 1024, false, true, state);
    }

    private static RenderType createBeam() {
        CompositeState state = CompositeState.builder()
                .setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
                .setTextureState(new TextureStateShard(WHITE_TEXTURE, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false);
        return RenderType.create(RENDER_TYPE_PREFIX + "labyrinth_beam", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 1024, false, true, state);
    }
}
