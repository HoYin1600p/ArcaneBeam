package dev.hoyin1600p.arcanebeam.labyrinth;

import iskallia.vault.core.util.RegionPos;
import iskallia.vault.core.vault.ClientVaults;
import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.vault.WorldManager;
import iskallia.vault.core.vault.stat.DetailedRoomEntry;
import iskallia.vault.core.vault.stat.DetailedRoomStat;
import iskallia.vault.core.vault.stat.StatCollector;
import iskallia.vault.core.vault.stat.StatsCollector;
import iskallia.vault.core.world.generator.GridGenerator;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Finds out which room of the Vault grid the player stands in, and whether it holds a labyrinth. The room's template
 * id comes from the player's own discovered-room stat, which Vault syncs to the client. Client thread only.
 */
final class LabyrinthRooms {
    /** How often an unresolved room (no entry yet, or not a labyrinth room) is looked up again, in ticks. */
    private static final int RELOOKUP_TICKS = 20;

    /** A labyrinth room: its variant and the world position of its box origin (template 0, 0, 0). */
    record Room(LabyrinthVariant variant, int regionX, int regionZ, int originX, int originY, int originZ) {
        boolean sameRoom(Room other) {
            return other != null && variant == other.variant && regionX == other.regionX && regionZ == other.regionZ;
        }

        boolean containsColumn(double x, double z) {
            return x >= originX && x < originX + RoomTransform.ROOM && z >= originZ && z < originZ + RoomTransform.ROOM;
        }

        /** True when the feet block is in the labyrinth band: from the first floor up to the top of the exit. */
        boolean containsFeet(double y) {
            int feet = (int) Math.floor(y + 1.0E-4) - originY;
            return feet >= 1 && feet <= variant.topFeetY;
        }
    }

    private java.util.UUID lastVaultId;
    private int lastRegionX = Integer.MIN_VALUE;
    private int lastRegionZ = Integer.MIN_VALUE;
    private Room lastRoom;
    private int lastLookupTick = Integer.MIN_VALUE;

    void reset() {
        lastVaultId = null;
        lastRegionX = Integer.MIN_VALUE;
        lastRegionZ = Integer.MIN_VALUE;
        lastRoom = null;
        lastLookupTick = Integer.MIN_VALUE;
    }

    /** The labyrinth room the player's column belongs to, or null (not a vault, other room, not yet known). */
    Room find(LocalPlayer player, int tick) {
        Vault vault = ClientVaults.getActive().orElse(null);
        if (vault == null) {
            reset();
            return null;
        }
        if (!(vault.getOptional(Vault.WORLD).map(world -> world.get(WorldManager.GENERATOR)).orElse(null) instanceof GridGenerator generator)) {
            return null;
        }
        Integer cellX = generator.get(GridGenerator.CELL_X);
        Integer cellZ = generator.get(GridGenerator.CELL_Z);
        if (cellX == null || cellZ == null || cellX <= 0 || cellZ <= 0) {
            return null;
        }
        // The vault stays "active" on the client for a moment after the player has left its dimension: only trust it
        // while the player's level is the vault's own, and never reuse a room cached for another vault.
        ResourceLocation vaultWorld = vault.getOptional(Vault.WORLD).map(world -> world.get(WorldManager.KEY)).orElse(null);
        if (vaultWorld == null || !vaultWorld.equals(player.level.dimension().location())) {
            reset();
            return null;
        }
        java.util.UUID vaultId = vault.get(Vault.ID);
        if (!java.util.Objects.equals(vaultId, lastVaultId)) {
            reset();
            lastVaultId = vaultId;
        }
        RegionPos region = RegionPos.ofBlockPos(player.blockPosition(), cellX, cellZ);
        boolean sameRegion = region.getX() == lastRegionX && region.getZ() == lastRegionZ;
        if (sameRegion && (lastRoom != null || tick - lastLookupTick < RELOOKUP_TICKS)) {
            return lastRoom;
        }
        lastRegionX = region.getX();
        lastRegionZ = region.getZ();
        lastLookupTick = tick;
        lastRoom = null;
        ResourceLocation id = roomId(vault, player, region);
        LabyrinthVariant variant = id == null ? null : LabyrinthVariant.fromTemplateId(id.toString());
        if (variant != null) {
            BlockPos origin = region.toBlockPos();
            lastRoom = new Room(variant, region.getX(), region.getZ(), origin.getX(), LabyrinthVariant.ROOM_Y_OFFSET, origin.getZ());
        }
        return lastRoom;
    }

    private static ResourceLocation roomId(Vault vault, LocalPlayer player, RegionPos region) {
        StatsCollector stats = vault.getOptional(Vault.STATS).orElse(null);
        if (stats == null || stats.getMap() == null) {
            return null;
        }
        StatCollector collector = stats.get(player.getUUID());
        DetailedRoomStat rooms = collector == null ? null : collector.getDetailedRoomsDiscovered();
        if (rooms == null) {
            return null;
        }
        DetailedRoomEntry entry = rooms.get(new BlockPos(region.getX(), 0, region.getZ()));
        return entry == null ? null : entry.get(DetailedRoomEntry.ID);
    }
}
