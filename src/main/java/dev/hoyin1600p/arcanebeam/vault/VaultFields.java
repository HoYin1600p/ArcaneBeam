package dev.hoyin1600p.arcanebeam.vault;

import iskallia.vault.client.particles.ArcaneParticle;
import iskallia.vault.entity.entity.VaultStormEntity;
import iskallia.vault.skill.ability.effect.spi.AbstractSmiteAbility;
import net.minecraft.network.syncher.EntityDataAccessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * Reads private Vault fields by reflection instead of through mixin accessors or {@code @Shadow}. A mixin
 * accessor or shadow whose field was renamed makes Mixin fail the whole target class at load; a reflective
 * lookup only fails when it is used, so a Vault rename costs the one visual that needs the field.
 * Every lookup runs once and is cached, including a failed one, which is logged a single time.
 */
public final class VaultFields {
    private static final Logger LOGGER = LogManager.getLogger("arcanebeam");

    private static final Lookup<EntityDataAccessor<Integer>> SMITE_BOLT_COLOR =
            new Lookup<>(AbstractSmiteAbility.SmiteBolt.class, "COLOR", "Smite bolt colour");
    private static final Lookup<EntityDataAccessor<Float>> STORM_RADIUS =
            new Lookup<>(VaultStormEntity.class, "RADIUS", "Storm radius");
    private static final Lookup<Boolean> ARCANE_FACTORY_IS_RAIL =
            new Lookup<>(ArcaneParticle.Factory.class, "isRail", "Arcane particle rail flag");

    private VaultFields() {
    }

    /** The synced-data key of a Smite bolt's colour; throws {@link IllegalStateException} when Vault renamed it. */
    public static EntityDataAccessor<Integer> smiteBoltColorAccessor() {
        return SMITE_BOLT_COLOR.staticValue();
    }

    /** The synced-data key of a Vault storm's radius; throws {@link IllegalStateException} when Vault renamed it. */
    public static EntityDataAccessor<Float> stormRadiusAccessor() {
        return STORM_RADIUS.staticValue();
    }

    /**
     * Whether an Arcane particle factory produces Rail particles: 1 for Rail, 0 for Arcane, -1 when the flag
     * cannot be read, in which case the caller must leave the particle to Vault.
     */
    public static int arcaneFactoryIsRail(Object factory) {
        Boolean rail = ARCANE_FACTORY_IS_RAIL.instanceValue(factory);
        return rail == null ? -1 : rail ? 1 : 0;
    }

    /** One cached reflective field. The first failure is logged once; the field is then treated as absent. */
    static final class Lookup<T> {
        private final Class<?> owner;
        private final String name;
        private final String description;
        private volatile Field field;
        private volatile boolean resolved;

        Lookup(Class<?> owner, String name, String description) {
            this.owner = owner;
            this.name = name;
            this.description = description;
        }

        private Field field() {
            if (!resolved) {
                synchronized (this) {
                    if (!resolved) {
                        try {
                            Field candidate = owner.getDeclaredField(name);
                            candidate.setAccessible(true);
                            field = candidate;
                        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                            LOGGER.warn("Vault field {}#{} was not found ({}); the visual that needs it is inactive",
                                    owner.getName(), name, description, failure);
                        }
                        resolved = true;
                    }
                }
            }
            return field;
        }

        @SuppressWarnings("unchecked")
        T staticValue() {
            Field resolvedField = field();
            if (resolvedField == null) {
                throw new IllegalStateException(description + " is unavailable");
            }
            try {
                return (T) resolvedField.get(null);
            } catch (IllegalAccessException | RuntimeException failure) {
                throw new IllegalStateException(description + " could not be read", failure);
            }
        }

        @SuppressWarnings("unchecked")
        T instanceValue(Object instance) {
            Field resolvedField = field();
            if (resolvedField == null) {
                return null;
            }
            try {
                return (T) resolvedField.get(instance);
            } catch (IllegalAccessException | RuntimeException failure) {
                return null;
            }
        }
    }
}
