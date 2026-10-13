package dev.hoyin1600p.arcanebeam.hook;

import dev.hoyin1600p.arcanebeam.map.DungeonGlobeMapRooms;
import dev.hoyin1600p.arcanebeam.map.DungeonGlobeMapRooms.Surface;
import iskallia.vault.client.map.VaultMapData;
import iskallia.vault.core.vault.Vault;

/**
 * Entry points the Vault map mixins (overlay, minimap, full map screen) call while a map is drawn.
 * None of them may throw into Vault's renderer: any failure disables the fade for the session and restores
 * the render state, and {@link #endFrame()} always restores.
 */
public final class MapHooks {
    private MapHooks() {
    }

    public static void beginFrame(Surface surface, Vault vault) {
        try {
            DungeonGlobeMapRooms.beginFrame(surface, vault);
        } catch (RuntimeException | LinkageError error) {
            DungeonGlobeMapRooms.fail("beginFrame", error);
        }
    }

    public static void beginRooms() {
        try {
            DungeonGlobeMapRooms.beginRooms();
        } catch (RuntimeException | LinkageError error) {
            DungeonGlobeMapRooms.fail("beginRooms", error);
        }
    }

    public static void applyRoom(VaultMapData.Room room) {
        try {
            DungeonGlobeMapRooms.applyRoom(room);
        } catch (RuntimeException | LinkageError error) {
            DungeonGlobeMapRooms.fail("applyRoom", error);
        }
    }

    public static void endRooms() {
        try {
            DungeonGlobeMapRooms.endRooms();
        } catch (RuntimeException | LinkageError error) {
            DungeonGlobeMapRooms.fail("endRooms", error);
        }
    }

    public static void endFrame() {
        try {
            DungeonGlobeMapRooms.endFrame();
        } catch (RuntimeException | LinkageError error) {
            DungeonGlobeMapRooms.fail("endFrame", error);
        }
    }
}
