package dev.hoyin1600p.arcanebeam.beam;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Remembers where and when Vault's Arcane and Rail cast sounds were heard, whoever cast them. A remote player's
 * particles are only claimed as an Arcane or Rail beam when such a sound was heard at that player's position
 * shortly before, because Arcane Prism emits the same particle types but a different sound, and nothing else
 * distinguishes a remote Prism cast from a Base or Rail cast on the client.
 */
final class RemoteCastEvidence {
    private static final int MAX_RECORDS = 32;
    private static final double MAX_HORIZONTAL_DISTANCE_SQR = 6.25D;
    private static final double MAX_VERTICAL_DISTANCE = 3.0D;
    static final long ARCANE_EVIDENCE_TICKS = 30L;
    static final long RAIL_EVIDENCE_TICKS = 12L;

    private final Deque<Record> records = new ArrayDeque<>();

    void record(BeamKind kind, Vec3 soundPosition, long gameTime) {
        expire(gameTime);
        records.addLast(new Record(kind, soundPosition, gameTime));
        while (records.size() > MAX_RECORDS) {
            records.removeFirst();
        }
    }

    /** True when a cast sound of this kind was heard recently at (or very near) the given player position. */
    boolean hasEvidence(BeamKind kind, Vec3 playerPosition, long gameTime) {
        expire(gameTime);
        long window = kind == BeamKind.RAIL ? RAIL_EVIDENCE_TICKS : ARCANE_EVIDENCE_TICKS;
        for (Record record : records) {
            if (record.kind != kind || gameTime - record.gameTime > window) {
                continue;
            }
            double dx = record.position.x - playerPosition.x;
            double dz = record.position.z - playerPosition.z;
            if (dx * dx + dz * dz <= MAX_HORIZONTAL_DISTANCE_SQR
                    && Math.abs(record.position.y - playerPosition.y) <= MAX_VERTICAL_DISTANCE) {
                return true;
            }
        }
        return false;
    }

    void clear() {
        records.clear();
    }

    private void expire(long gameTime) {
        if (!records.isEmpty() && gameTime < records.peekLast().gameTime) {
            records.clear();
        }
        while (!records.isEmpty() && gameTime - records.peekFirst().gameTime > ARCANE_EVIDENCE_TICKS) {
            records.removeFirst();
        }
    }

    private record Record(BeamKind kind, Vec3 position, long gameTime) {
    }
}
