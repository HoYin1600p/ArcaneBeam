package dev.hoyin1600p.arcanebeam.labyrinth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Where the overlay is active, and where the player is placed on the cell graph. */
class ActivationAndPlacementTest {
    private static LabyrinthRooms.Room room(LabyrinthVariant v, int ox, int oz) {
        return new LabyrinthRooms.Room(v, Math.floorDiv(ox, 100), Math.floorDiv(oz, 100), ox, LabyrinthVariant.ROOM_Y_OFFSET, oz);
    }

    private static boolean active(LabyrinthRooms.Room r, double x, double y, double z) {
        return r.containsColumn(x, z) && r.containsFeet(y);
    }

    @Test
    void activeOnlyBetweenTheFirstFloorAndTheTopOfTheExit() {
        for (LabyrinthVariant v : LabyrinthVariant.values()) {
            for (int[] origin : new int[][]{{200, 300}, {-100, -300}, {0, 0}}) {
                LabyrinthRooms.Room r = room(v, origin[0], origin[1]);
                int cx = origin[0] + 20;
                int cz = origin[1] + 20;
                int y0 = LabyrinthVariant.ROOM_Y_OFFSET;
                assertFalse(active(r, cx, y0 + 0.0, cz), v + ": below the first floor");
                assertFalse(active(r, cx, y0 + 0.99, cz), v + ": still inside the floor block");
                assertTrue(active(r, cx, y0 + 1.0, cz), v + ": standing on the first floor");
                assertTrue(active(r, cx, y0 + v.topFeetY + 0.5, cz), v + ": at the top of the exit");
                assertFalse(active(r, cx, y0 + v.topFeetY + 1.0, cz), v + ": just above the band, in the room above");
                assertFalse(active(r, cx, y0 + 60, cz), v + ": on the surface or higher");
            }
        }
    }

    @Test
    void notActiveInAnAdjacentRoomOrJustOutsideTheBox() {
        LabyrinthRooms.Room r = room(LabyrinthVariant.GILDED, 200, 300);
        double y = LabyrinthVariant.ROOM_Y_OFFSET + 1.0;
        assertTrue(active(r, 200.0, y, 300.0), "the first block of the box");
        assertTrue(active(r, 246.99, y, 346.99), "the last block of the box");
        assertFalse(active(r, 199.99, y, 320), "one block west of the box");
        assertFalse(active(r, 247.0, y, 320), "one block east of the box");
        assertFalse(active(r, 220, y, 299.99), "one block north of the box");
        assertFalse(active(r, 220, y, 347.0), "one block south of the box");
        // the neighbouring room's own box starts at the next grid cell
        LabyrinthRooms.Room next = room(LabyrinthVariant.GILDED, 300, 300);
        assertFalse(active(r, 320, y, 320));
        assertTrue(active(next, 320, y, 320));
    }

    @Test
    void theEntranceTopCellOfEveryVariantIsInsideTheBand() {
        for (LabyrinthVariant v : LabyrinthVariant.values()) {
            if (v == LabyrinthVariant.WOLDS_LABYRINTH) {
                continue;
            }
            for (int[] cell : v.entranceCells()) {
                LabyrinthRooms.Room r = room(v, 0, 0);
                assertTrue(r.containsFeet(r.originY() + cell[1]), v + ": the exit cell is inside the band");
            }
        }
    }

    // ---- placing the player ------------------------------------------------------------------------------------------

    private static final int OX = 100;
    private static final int OY = 9;
    private static final int OZ = -50;

    private static int cellFor(WalkGraph g, double x, double y, double z) {
        return PlayerCell.find(g, OX + x, OY + y, OZ + z, OX, OY, OZ);
    }

    private static TestVoxels flat() {
        TestVoxels v = new TestVoxels(RoomTransform.ROOM, 12);
        v.fill(0, 0, 0, 46, 0, 46, BlockCode.fullBlock());
        v.maxFeet = 10;
        return v;
    }

    @Test
    void onTheFloorOnAHalfSlabOnAStairAndOnACarpet() {
        TestVoxels v = flat();
        v.set(5, 1, 5, BlockCode.solid(0, 8));
        v.set(8, 1, 5, BlockCode.stair());
        v.set(11, 1, 5, BlockCode.solid(0, 1));
        WalkGraph g = v.scan();
        assertEquals(g.cellAt(3, 0, 5), cellFor(g, 3.5, 1.0, 5.5));
        assertEquals(g.cellAt(5, 1, 5), cellFor(g, 5.5, 1.5, 5.5), "standing on a bottom slab");
        assertEquals(g.cellAt(8, 1, 5), cellFor(g, 8.5, 1.5, 5.5), "on the lower tread of a stair");
        assertEquals(g.cellAt(8, 1, 5), cellFor(g, 8.5, 2.0, 5.5), "on the upper tread of a stair");
        assertEquals(g.cellAt(11, 1, 5), cellFor(g, 11.5, 1.0625, 5.5), "on a carpet");
    }

    @Test
    void midLadderAndMidJumpAreOnTheNearestCell() {
        TestVoxels v = flat();
        v.fill(20, 1, 20, 20, 9, 20, BlockCode.ladder());
        v.fill(19, 1, 20, 19, 9, 20, BlockCode.fullBlock());
        WalkGraph g = v.scan();
        int low = g.cellAt(20, 3, 20);
        int high = g.cellAt(20, 4, 20);
        assertTrue(low >= 0 && high >= 0, "a cell on every rung");
        assertEquals(low, cellFor(g, 20.5, 4.2, 20.5), "feet at 4.2: the rung below is nearest");
        assertEquals(high, cellFor(g, 20.5, 4.8, 20.5), "feet at 4.8: the rung above is nearest");
        // a jump: one block above the floor is still the floor's cell
        assertEquals(g.cellAt(3, 0, 5), cellFor(g, 3.5, 2.0, 5.5));
        // 1.4 blocks up: out of the 1.25 block tolerance, no cell: the previous route stays
        assertEquals(-1, cellFor(g, 3.5, 2.4, 5.5));
    }

    @Test
    void inShallowAndDeepWaterAndInACobweb() {
        TestVoxels v = flat();
        // a 1 deep channel: water in the feet block over the floor
        v.set(10, 1, 10, BlockCode.water());
        // a 4 deep pool: water from y 1 to 4
        v.fill(15, 1, 15, 15, 4, 15, BlockCode.water());
        // a cobweb has no collision: it is air to the scan
        v.set(25, 1, 25, BlockCode.AIR);
        WalkGraph g = v.scan();
        assertEquals(g.cellAt(10, 0, 10), cellFor(g, 10.5, 1.3, 10.5), "swimming at the top of a shallow channel");
        assertEquals(g.cellAt(15, 0, 15), cellFor(g, 15.5, 1.2, 15.5), "at the bottom of a deep pool");
        assertEquals(-1, cellFor(g, 15.5, 4.6, 15.5), "at the surface of a deep pool: no cell, the route stays as it was");
        assertEquals(g.cellAt(25, 0, 25), cellFor(g, 25.5, 1.0, 25.5), "inside a cobweb");
    }

    @Test
    void onAClosedBottomTrapdoorOverAHole() {
        TestVoxels v = flat();
        // a hole one block deep (floor removed), covered by a closed bottom trapdoor at the floor level: 3/16 floor
        v.set(30, 0, 30, BlockCode.solid(0, 3));
        WalkGraph g = v.scan();
        int cell = g.cellAt(30, 0, 30);
        assertTrue(cell >= 0, "the trapdoor is a place to stand");
        assertEquals(cell, cellFor(g, 30.5, 0.1875, 30.5));
    }

    @Test
    void betweenTwoCellsTheBlockUnderTheFeetDecides() {
        TestVoxels v = flat();
        WalkGraph g = v.scan();
        assertEquals(g.cellAt(6, 0, 6), cellFor(g, 6.999, 1.0, 6.5));
        assertEquals(g.cellAt(7, 0, 6), cellFor(g, 7.0, 1.0, 6.5));
        assertEquals(g.cellAt(6, 0, 6), cellFor(g, 6.0, 1.0, 6.999));
        assertEquals(g.cellAt(6, 0, 7), cellFor(g, 6.0, 1.0, 7.0));
        // negative world coordinates floor correctly
        assertEquals(g.cellAt(9, 0, 4), PlayerCell.find(g, -50.5, OY + 1.0, -105.5, -60, OY, -110));
    }
}
