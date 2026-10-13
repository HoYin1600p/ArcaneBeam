package dev.hoyin1600p.arcanebeam.ability.manastealer;

import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public final class ManaStealerVisualConfig {
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue ENABLED;
    private static final ForgeConfigSpec.BooleanValue REPLACE_GROUND_SIGIL;
    private static final ForgeConfigSpec.IntValue TRAP_HALO_COLOR;
    private static final ForgeConfigSpec.IntValue TRAP_CORE_COLOR;
    private static final ForgeConfigSpec.IntValue BEAM_HALO_COLOR;
    private static final ForgeConfigSpec.IntValue BEAM_CORE_COLOR;
    private static final ForgeConfigSpec.IntValue ALL_POPULATION;
    private static final ForgeConfigSpec.IntValue DECREASED_POPULATION;
    private static final ForgeConfigSpec.IntValue MINIMAL_POPULATION;
    private static final ForgeConfigSpec.IntValue MAX_SPAWNS_PER_TICK;
    private static final ForgeConfigSpec.DoubleValue MINIMUM_SPEED;
    private static final ForgeConfigSpec.DoubleValue MAXIMUM_SPEED;
    private static final ForgeConfigSpec.DoubleValue OUTER_DIAMETER;
    private static final ForgeConfigSpec.DoubleValue INNER_RATIO;
    private static final ForgeConfigSpec.BooleanValue DRAIN_STREAM_ENABLED;
    private static final ForgeConfigSpec.DoubleValue DRAIN_STREAM_DENSITY;
    private static final ForgeConfigSpec.IntValue DRAIN_STREAM_MINIMUM_ORBS;
    private static final ForgeConfigSpec.IntValue DRAIN_STREAM_MAXIMUM_ORBS;
    private static final ForgeConfigSpec.DoubleValue DRAIN_STREAM_SPEED;
    private static final ForgeConfigSpec.DoubleValue DRAIN_STREAM_ORB_DIAMETER;
    private static final ForgeConfigSpec.DoubleValue DRAIN_STREAM_SPREAD;

    private static volatile boolean enabled = true;
    private static volatile boolean replaceGroundSigil = true;
    private static volatile int trapHaloColor = 0x64C7FF;
    private static volatile int trapCoreColor = 0x0A2B80;
    private static volatile int beamHaloColor = 0x64C7FF;
    private static volatile int beamCoreColor = 0x0A2B80;
    private static volatile int allPopulation = 80;
    private static volatile int decreasedPopulation = 52;
    private static volatile int minimalPopulation = 20;
    private static volatile int maxSpawnsPerTick = 8;
    private static volatile double minimumSpeed = 0.1625D;
    private static volatile double maximumSpeed = 0.26D;
    private static volatile float outerDiameter = 0.3F;
    private static volatile float innerRatio = 0.46F;
    private static volatile boolean drainStreamEnabled = true;
    private static volatile double drainStreamDensity = 6.5D;
    private static volatile int drainStreamMinimumOrbs = 6;
    private static volatile int drainStreamMaximumOrbs = 48;
    private static volatile double drainStreamSpeed = 0.32D;
    private static volatile float drainStreamOrbDiameter = 0.22F;
    private static volatile float drainStreamSpread = 0.18F;

    private static final Object SAVE_LOCK = new Object();
    private static final long SAVE_DEBOUNCE_NANOS = 400_000_000L;
    private static final long OWN_WRITE_ECHO_NANOS = 2_000_000_000L;
    private static boolean dirty;
    private static long lastEditNanos;
    private static long lastOwnWriteNanos = Long.MIN_VALUE / 2;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment(
                "Mana Stealer trap visual replacement.",
                "All values apply on the client and leave Vault gameplay unchanged."
        ).push("mana_stealer_visuals");
        ENABLED = builder
                .comment("Replace the Mana Stealer dust with bounded inward-moving composite orbs.")
                .define("enabled", true);
        REPLACE_GROUND_SIGIL = builder
                .comment("Hide Vault's legacy flat line sigil while the orb replacement is active.")
                .define("replace_ground_sigil", true);
        TRAP_HALO_COLOR = builder
                .comment("RGB color of the outer halo on trap particles. Decimal form of #64C7FF by default.")
                .defineInRange("trap_halo_color", 0x64C7FF, 0, 0xFFFFFF);
        TRAP_CORE_COLOR = builder
                .comment("RGB color of the inner core on trap particles. Decimal form of #0A2B80 by default.")
                .defineInRange("trap_core_color", 0x0A2B80, 0, 0xFFFFFF);
        BEAM_HALO_COLOR = builder
                .comment("RGB color of the outer halo on player-to-trap beam particles.")
                .defineInRange("beam_halo_color", 0x64C7FF, 0, 0xFFFFFF);
        BEAM_CORE_COLOR = builder
                .comment("RGB color of the inner core on player-to-trap beam particles.")
                .defineInRange("beam_core_color", 0x0A2B80, 0, 0xFFFFFF);
        ALL_POPULATION = builder
                .comment("Live composite-orb target for Minecraft's All particle setting.")
                .defineInRange("all_population", 80, 0, 512);
        DECREASED_POPULATION = builder
                .comment("Live composite-orb target for Minecraft's Decreased particle setting.")
                .defineInRange("decreased_population", 52, 0, 512);
        MINIMAL_POPULATION = builder
                .comment("Live composite-orb target for Minecraft's Minimal particle setting.")
                .defineInRange("minimal_population", 20, 0, 512);
        MAX_SPAWNS_PER_TICK = builder
                .comment("Maximum deficit filled each tick; limits the initial visual ramp.")
                .defineInRange("max_spawns_per_tick", 8, 1, 64);
        MINIMUM_SPEED = builder
                .comment("Minimum inward speed in blocks per tick; 0.1625 equals 3.25 blocks/second.")
                .defineInRange("minimum_speed", 0.1625D, 0.01D, 2.0D);
        MAXIMUM_SPEED = builder
                .comment("Maximum inward speed in blocks per tick; 0.26 equals 5.2 blocks/second.")
                .defineInRange("maximum_speed", 0.26D, 0.01D, 2.0D);
        OUTER_DIAMETER = builder
                .comment(
                        "Reference outer-orb diameter in blocks. Orbs spawn at 250 percent of this value",
                        "and shrink to 12.5 percent of it at the center."
                )
                .defineInRange("outer_diameter", 0.3D, 0.02D, 2.0D);
        INNER_RATIO = builder
                .comment("Inner navy ball diameter as a fraction of the pale outer ball.")
                .defineInRange("inner_diameter_ratio", 0.46D, 0.1D, 0.95D);
        DRAIN_STREAM_ENABLED = builder
                .comment("Show a blue composite-orb stream from each affected player to the active trap.")
                .define("drain_stream_enabled", true);
        DRAIN_STREAM_DENSITY = builder
                .comment("All-particles stream density in visible composite orbs per block.")
                .defineInRange("drain_stream_density", 6.5D, 0.25D, 32.0D);
        DRAIN_STREAM_MINIMUM_ORBS = builder
                .comment("Minimum visible stream orbs while a player is inside the drain radius.")
                .defineInRange("drain_stream_minimum_orbs", 6, 1, 128);
        DRAIN_STREAM_MAXIMUM_ORBS = builder
                .comment("Maximum visible stream orbs per player and trap on All particles.")
                .defineInRange("drain_stream_maximum_orbs", 48, 1, 256);
        DRAIN_STREAM_SPEED = builder
                .comment("Player-to-trap stream speed in blocks per tick; 0.32 equals 6.4 blocks/second.")
                .defineInRange("drain_stream_speed", 0.32D, 0.01D, 2.0D);
        DRAIN_STREAM_ORB_DIAMETER = builder
                .comment("Reference diameter in blocks for each composite orb in the drain stream.")
                .defineInRange("drain_stream_orb_diameter", 0.22D, 0.02D, 2.0D);
        DRAIN_STREAM_SPREAD = builder
                .comment("Maximum radial spread in blocks around the player-to-trap center line.")
                .defineInRange("drain_stream_spread", 0.18D, 0.0D, 2.0D);
        builder.pop();
        SPEC = builder.build();
    }

    private ManaStealerVisualConfig() {
    }

    public static boolean active() {
        return enabled && ManaStealerOwnership.owns() && ManaStealerOwnership.mixinsAvailable();
    }

    public static boolean configuredEnabled() {
        return enabled;
    }

    public static boolean replaceGroundSigil() {
        return replaceGroundSigil;
    }

    public static int trapHaloColor() {
        return trapHaloColor;
    }

    public static int trapCoreColor() {
        return trapCoreColor;
    }

    public static int beamHaloColor() {
        return beamHaloColor;
    }

    public static int beamCoreColor() {
        return beamCoreColor;
    }

    public static int allPopulation() {
        return allPopulation;
    }

    public static int trapParticleCount() {
        return allPopulation;
    }

    public static int decreasedPopulation() {
        return decreasedPopulation;
    }

    public static int minimalPopulation() {
        return minimalPopulation;
    }

    public static int maxSpawnsPerTick() {
        return maxSpawnsPerTick;
    }

    public static double minimumSpeed() {
        return minimumSpeed;
    }

    public static double maximumSpeed() {
        return maximumSpeed;
    }

    public static float outerDiameter() {
        return outerDiameter;
    }

    public static float innerRatio() {
        return innerRatio;
    }

    public static boolean drainStreamActive() {
        return active() && drainStreamEnabled;
    }

    public static boolean drainStreamEnabled() {
        return drainStreamEnabled;
    }

    public static double drainStreamDensity() {
        return drainStreamDensity;
    }

    public static int drainStreamMinimumOrbs() {
        return drainStreamMinimumOrbs;
    }

    public static int drainStreamMaximumOrbs() {
        return drainStreamMaximumOrbs;
    }

    public static double drainStreamSpeed() {
        return drainStreamSpeed;
    }

    public static float drainStreamOrbDiameter() {
        return drainStreamOrbDiameter;
    }

    public static float drainStreamSpread() {
        return drainStreamSpread;
    }

    public static int beamParticleCount() {
        return ManaStealerDrainStreamPolicy.visibleOrbCount(
                6.0D,
                drainStreamDensity,
                drainStreamMinimumOrbs,
                drainStreamMaximumOrbs,
                ManaStealerDrainStreamPolicy.ALL
        );
    }

    public static void setEnabled(boolean value) {
        if (enabled == value) {
            return;
        }
        enabled = value;
        markDirty();
    }

    public static void setReplaceGroundSigil(boolean value) {
        if (replaceGroundSigil == value) {
            return;
        }
        replaceGroundSigil = value;
        markDirty();
    }

    public static void setTrapHaloColor(int value) {
        int bounded = boundColor(value);
        if (trapHaloColor != bounded) {
            trapHaloColor = bounded;
            markDirty();
        }
    }

    public static void setTrapCoreColor(int value) {
        int bounded = boundColor(value);
        if (trapCoreColor != bounded) {
            trapCoreColor = bounded;
            markDirty();
        }
    }

    public static void setBeamHaloColor(int value) {
        int bounded = boundColor(value);
        if (beamHaloColor != bounded) {
            beamHaloColor = bounded;
            markDirty();
        }
    }

    public static void setBeamCoreColor(int value) {
        int bounded = boundColor(value);
        if (beamCoreColor != bounded) {
            beamCoreColor = bounded;
            markDirty();
        }
    }

    public static void setTrapParticleCount(int value) {
        int bounded = Math.max(0, Math.min(512, value));
        int decreased = (int) Math.round(bounded * 0.65D);
        int minimal = (int) Math.round(bounded * 0.25D);
        if (allPopulation == bounded && decreasedPopulation == decreased && minimalPopulation == minimal) {
            return;
        }
        allPopulation = bounded;
        decreasedPopulation = decreased;
        minimalPopulation = minimal;
        markDirty();
    }

    public static void setBeamParticleCount(int value) {
        int bounded = Math.max(1, Math.min(256, value));
        int minimum = Math.max(1, Math.min(bounded, (int) Math.round(bounded * (6.0D / 45.0D))));
        minimum = Math.max(minimum, bounded - 192);
        double density = Math.max(0.25D, Math.min(32.0D, (bounded - minimum) / 6.0D));
        if (drainStreamDensity == density
                && drainStreamMinimumOrbs == minimum
                && drainStreamMaximumOrbs == bounded) {
            return;
        }
        drainStreamDensity = density;
        drainStreamMinimumOrbs = minimum;
        drainStreamMaximumOrbs = bounded;
        markDirty();
    }

    public static void setDrainStreamEnabled(boolean value) {
        if (drainStreamEnabled == value) {
            return;
        }
        drainStreamEnabled = value;
        markDirty();
    }

    private static int boundColor(int value) {
        return Math.max(0, Math.min(0xFFFFFF, value));
    }

    private static void markDirty() {
        synchronized (SAVE_LOCK) {
            dirty = true;
            lastEditNanos = System.nanoTime();
        }
    }

    /** Writes pending edits once the user has stopped changing values for a short moment. */
    public static void tickPendingSave() {
        boolean due;
        synchronized (SAVE_LOCK) {
            due = dirty && System.nanoTime() - lastEditNanos >= SAVE_DEBOUNCE_NANOS;
        }
        if (due) {
            flushPendingSave();
        }
    }

    /** Writes pending edits now; safe to call when nothing is pending. */
    public static void flushPendingSave() {
        synchronized (SAVE_LOCK) {
            if (!dirty) {
                return;
            }
            dirty = false;
            try {
                ENABLED.set(enabled);
                REPLACE_GROUND_SIGIL.set(replaceGroundSigil);
                TRAP_HALO_COLOR.set(trapHaloColor);
                TRAP_CORE_COLOR.set(trapCoreColor);
                BEAM_HALO_COLOR.set(beamHaloColor);
                BEAM_CORE_COLOR.set(beamCoreColor);
                ALL_POPULATION.set(allPopulation);
                DECREASED_POPULATION.set(decreasedPopulation);
                MINIMAL_POPULATION.set(minimalPopulation);
                DRAIN_STREAM_ENABLED.set(drainStreamEnabled);
                DRAIN_STREAM_DENSITY.set(drainStreamDensity);
                DRAIN_STREAM_MINIMUM_ORBS.set(drainStreamMinimumOrbs);
                DRAIN_STREAM_MAXIMUM_ORBS.set(drainStreamMaximumOrbs);
                ENABLED.save();
                lastOwnWriteNanos = System.nanoTime();
            } catch (RuntimeException failure) {
                ArcaneBeam.LOGGER.warn("Could not save Mana Stealer visual settings", failure);
            }
        }
    }

    public static void onLoading(ModConfigEvent.Loading event) {
        bake(event.getConfig());
    }

    public static void onReloading(ModConfigEvent.Reloading event) {
        bake(event.getConfig());
    }

    private static void bake(ModConfig config) {
        if (config.getSpec() != SPEC) {
            return;
        }
        synchronized (SAVE_LOCK) {
            // In-memory values are newer than the file while an edit is pending, and the file
            // watcher echoes our own write back shortly after it; baking either would snap values back.
            if (dirty || System.nanoTime() - lastOwnWriteNanos < OWN_WRITE_ECHO_NANOS) {
                return;
            }
            bakeValues();
        }
    }

    private static void bakeValues() {
        enabled = ENABLED.get();
        replaceGroundSigil = REPLACE_GROUND_SIGIL.get();
        trapHaloColor = TRAP_HALO_COLOR.get();
        trapCoreColor = TRAP_CORE_COLOR.get();
        beamHaloColor = BEAM_HALO_COLOR.get();
        beamCoreColor = BEAM_CORE_COLOR.get();
        allPopulation = ALL_POPULATION.get();
        decreasedPopulation = DECREASED_POPULATION.get();
        minimalPopulation = MINIMAL_POPULATION.get();
        maxSpawnsPerTick = MAX_SPAWNS_PER_TICK.get();
        minimumSpeed = Math.min(MINIMUM_SPEED.get(), MAXIMUM_SPEED.get());
        maximumSpeed = Math.max(MINIMUM_SPEED.get(), MAXIMUM_SPEED.get());
        outerDiameter = OUTER_DIAMETER.get().floatValue();
        innerRatio = INNER_RATIO.get().floatValue();
        drainStreamEnabled = DRAIN_STREAM_ENABLED.get();
        drainStreamDensity = DRAIN_STREAM_DENSITY.get();
        drainStreamMinimumOrbs = Math.min(DRAIN_STREAM_MINIMUM_ORBS.get(), DRAIN_STREAM_MAXIMUM_ORBS.get());
        drainStreamMaximumOrbs = Math.max(DRAIN_STREAM_MINIMUM_ORBS.get(), DRAIN_STREAM_MAXIMUM_ORBS.get());
        drainStreamSpeed = DRAIN_STREAM_SPEED.get();
        drainStreamOrbDiameter = DRAIN_STREAM_ORB_DIAMETER.get().floatValue();
        drainStreamSpread = DRAIN_STREAM_SPREAD.get().floatValue();
    }
}
