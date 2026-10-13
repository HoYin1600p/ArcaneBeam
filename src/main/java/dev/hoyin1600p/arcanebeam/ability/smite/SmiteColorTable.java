package dev.hoyin1600p.arcanebeam.ability.smite;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The bolt colour each Smite variant uses, as the server configured it and synced it to this client with the
 * ability tree. Packs recolour Smite, so the colour is data to be looked up, never a constant to compare against.
 */
public final class SmiteColorTable {
    private final Map<SmiteVariant, Set<Integer>> colors = new EnumMap<>(SmiteVariant.class);

    public void add(SmiteVariant variant, int color) {
        colors.computeIfAbsent(variant, key -> new HashSet<>()).add(color);
    }

    public boolean isEmpty() {
        return colors.isEmpty();
    }

    /** Every variant that is configured with this bolt colour (more than one when a pack shares a colour). */
    public Set<SmiteVariant> variantsWith(int color) {
        Set<SmiteVariant> found = EnumSet.noneOf(SmiteVariant.class);
        for (Map.Entry<SmiteVariant, Set<Integer>> entry : colors.entrySet()) {
            if (entry.getValue().contains(color)) {
                found.add(entry.getKey());
            }
        }
        return found;
    }

    public void clear() {
        colors.clear();
    }
}
