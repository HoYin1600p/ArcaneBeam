package dev.hoyin1600p.arcanebeam.client;

import dev.hoyin1600p.arcanebeam.ArcaneBeam;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Startup diagnostic: reports which Vault members this mod's mixins and reflective reads expect but cannot find,
 * so a Vault update that renames something is named in the log instead of a feature quietly doing nothing.
 *
 * <p>Members are matched by name and full descriptor (a bridge method with the same name but an erased signature
 * is not accepted), fields also by static-ness, and the call sites that INVOKE injectors and redirects target are
 * checked in the bytecode of the Vault method that contains them. The check only reads class files and
 * reflection data; it never changes behaviour, and it is wrapped so it can never break startup.
 */
public final class VaultMixinTargetCheck {
    private static final String RENDER_TAIL = "FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";
    private static final String PARTICLE_ENGINE = "net/minecraft/client/particle/ParticleEngine";
    private static final String LEVEL = "net/minecraft/world/level/Level";
    private static final String PARTICLE_ARGS = "(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)";
    private static final String CREATE_PARTICLE_DESC = PARTICLE_ARGS + "Lnet/minecraft/client/particle/Particle;";
    private static final String ADD_PARTICLE_DESC = PARTICLE_ARGS + "V";
    private static final String VAULT_MAP_DATA = "iskallia/vault/client/map/VaultMapData";

    // Vault names some methods with SRG ids (tick, onHit) and others with readable names (render), and the
    // vanilla methods it calls appear under their SRG names in a production jar, so each name may have an alias.
    private static final List<Target> TARGETS = List.of(
            method("ability activity tracking", "iskallia.vault.network.message.AbilityActivityMessage",
                    "(Liskallia/vault/network/message/AbilityActivityMessage;Ljava/util/function/Supplier;)V", "handle"),
            method("known ability tracking", "iskallia.vault.network.message.AbilityKnownOnesMessage",
                    "(Liskallia/vault/network/message/AbilityKnownOnesMessage;Ljava/util/function/Supplier;)V", "handle"),
            method("ability key tracking", "iskallia.vault.network.message.ServerboundAbilityKeyMessage",
                    "(Liskallia/vault/network/message/ServerboundAbilityKeyMessage$Opcode;)V", "send"),
            method("quickselect tracking", "iskallia.vault.network.message.AbilityQuickselectMessage",
                    "(Ljava/lang/String;I)V", "<init>"),
            field("quickselect tracking", "iskallia.vault.network.message.AbilityQuickselectMessage", "PRESSED", "I", true),
            method("arcane particle replacement", "iskallia.vault.client.particles.ArcaneParticle$Factory",
                    "(Lnet/minecraft/core/particles/SimpleParticleType;Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDD)"
                            + "Lnet/minecraft/client/particle/Particle;", "createParticle"),
            field("arcane particle replacement", "iskallia.vault.client.particles.ArcaneParticle$Factory", "isRail", "Z", false),
            method("blast wave particle replacement", "iskallia.vault.network.message.ClientboundBlastWaveParticleMessage",
                    "(Lnet/minecraft/world/phys/Vec3;F[I)V", "spawnParticles"),
            method("chain lightning visuals", "iskallia.vault.skill.ability.effect.ChainLightningAbility$ChainLightningProjectile",
                    "(Lnet/minecraft/world/phys/HitResult;)V", "onHit", "m_6532_"),
            method("chain lightning visuals", "iskallia.vault.skill.ability.effect.ChainLightningAbility$LightningArrowRenderer",
                    "(Liskallia/vault/skill/ability/effect/ChainLightningAbility$ChainLightningProjectile;" + RENDER_TAIL, "render"),
            method("smite visuals", "iskallia.vault.skill.ability.effect.spi.AbstractSmiteAbility$SmiteBoltRenderer",
                    "(Liskallia/vault/skill/ability/effect/spi/AbstractSmiteAbility$SmiteBolt;" + RENDER_TAIL, "render"),
            field("smite visuals", "iskallia.vault.skill.ability.effect.spi.AbstractSmiteAbility$SmiteBolt", "COLOR",
                    "Lnet/minecraft/network/syncher/EntityDataAccessor;", true),
            method("storm arrow visuals", "iskallia.vault.entity.renderer.StormArrowRenderer",
                    "(Liskallia/vault/entity/entity/VaultStormArrow;" + RENDER_TAIL, "render"),
            method("storm arrow visuals", "iskallia.vault.entity.entity.VaultStormArrow", "()V", List.of("tick", "m_8119_"),
                    List.of(call(PARTICLE_ENGINE, CREATE_PARTICLE_DESC, 2, "createParticle", "m_107370_"))),
            method("storm visuals", "iskallia.vault.entity.entity.VaultStormEntity", "()V", List.of("tick", "m_8119_"),
                    List.of(call(PARTICLE_ENGINE, CREATE_PARTICLE_DESC, 1, "createParticle", "m_107370_"),
                            call(LEVEL, ADD_PARTICLE_DESC, 2, "addParticle", "m_7106_"))),
            type("storm visuals", "iskallia.vault.entity.entity.VaultStormEntity$SmiteBolt"),
            method("storm visuals", "iskallia.vault.entity.entity.VaultStormEntity$SmiteBoltRenderer",
                    "(Liskallia/vault/entity/entity/VaultStormEntity$SmiteBolt;" + RENDER_TAIL, "render"),
            field("storm visuals", "iskallia.vault.entity.entity.VaultStormEntity", "RADIUS",
                    "Lnet/minecraft/network/syncher/EntityDataAccessor;", true),
            method("dungeon globe map opacity", "iskallia.vault.client.render.hud.module.vault.VaultMapOverlayModule",
                    "(Liskallia/vault/client/render/hud/module/context/ModuleRenderContext;Liskallia/vault/core/vault/Vault;"
                            + "Liskallia/vault/core/vault/player/Listener;)V", List.of("renderVault"),
                    List.of(call(VAULT_MAP_DATA, "()Ljava/util/List;", 1, "getRooms"),
                            call(VAULT_MAP_DATA + "$Room", "()Lnet/minecraft/resources/ResourceLocation;", 1, "getRoomId"),
                            call(VAULT_MAP_DATA, "()Ljava/util/List;", 1, "getTunnels"))),
            method("dungeon globe map opacity (minimap)", "iskallia.vault.client.render.hud.module.vault.VaultMinimapModule",
                    "(Lnet/minecraft/core/Direction;Lnet/minecraft/client/player/LocalPlayer;Liskallia/vault/core/vault/Vault;"
                            + "Liskallia/vault/client/map/VaultMapData;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
                    List.of("lambda$renderVault$2"),
                    List.of(call(VAULT_MAP_DATA, "()Ljava/util/List;", 1, "getRooms"),
                            call(VAULT_MAP_DATA + "$Room", "()Lnet/minecraft/resources/ResourceLocation;", 1, "getRoomId"),
                            call(VAULT_MAP_DATA, "()Ljava/util/List;", 1, "getTunnels"))),
            method("dungeon globe map opacity (map screen)", "iskallia.vault.client.gui.screen.map.IngameVaultMapElement",
                    "(Liskallia/vault/client/gui/framework/render/spi/IElementRenderer;Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V",
                    List.of("render"),
                    List.of(call(VAULT_MAP_DATA, "()Ljava/util/List;", 1, "getRooms"),
                            call(VAULT_MAP_DATA + "$Room", "()Lnet/minecraft/resources/ResourceLocation;", 1, "getRoomId"),
                            call(VAULT_MAP_DATA, "()Ljava/util/List;", 1, "getTunnels"))),
            method("mana stealer visuals", "iskallia.vault.entity.entity.ManaStealerEntity", "()V", "spawnClientParticles"),
            method("mana stealer visuals", "iskallia.vault.entity.entity.ManaStealerEntity", "()F", "getRadius"),
            method("mana stealer visuals", "iskallia.vault.entity.renderer.ManaStealerRenderer",
                    "(Liskallia/vault/entity/entity/ManaStealerEntity;" + RENDER_TAIL, "render"),
            method("vault altar beam", "iskallia.vault.block.entity.VaultAltarTileEntity", "()I", "getInfusionTimer")
    );

    private VaultMixinTargetCheck() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener((FMLClientSetupEvent event) -> run());
    }

    private static void run() {
        try {
            ClassLoader loader = VaultMixinTargetCheck.class.getClassLoader();
            Function<String, Class<?>> classLookup = name -> {
                try {
                    return Class.forName(name, false, loader);
                } catch (Throwable throwable) {
                    ClassLoader context = Thread.currentThread().getContextClassLoader();
                    if (context == null || context == loader) {
                        return null;
                    }
                    try {
                        return Class.forName(name, false, context);
                    } catch (Throwable ignored) {
                        return null;
                    }
                }
            };
            Function<String, byte[]> classBytes = name -> readClassBytes(loader, name);
            List<Finding> findings = findMissing(TARGETS, classLookup, classBytes);
            if (findings.isEmpty()) {
                ArcaneBeam.LOGGER.debug("All Vault mixin targets were found.");
            } else {
                ArcaneBeam.LOGGER.warn("{}", summarize(findings));
            }
        } catch (Throwable throwable) {
            try {
                ArcaneBeam.LOGGER.debug("Vault mixin target check could not run", throwable);
            } catch (Throwable ignored) {
                // Diagnostics must never break startup.
            }
        }
    }

    private static byte[] readClassBytes(ClassLoader loader, String className) {
        try (InputStream stream = loader.getResourceAsStream(className.replace('.', '/') + ".class")) {
            return stream == null ? null : stream.readAllBytes();
        } catch (IOException | RuntimeException failure) {
            return null;
        }
    }

    /** One line naming every affected feature with what is missing for it. */
    static String summarize(List<Finding> findings) {
        Map<String, List<String>> byFeature = new LinkedHashMap<>();
        for (Finding finding : findings) {
            byFeature.computeIfAbsent(finding.feature(), key -> new ArrayList<>()).add(finding.detail());
        }
        StringBuilder text = new StringBuilder("The installed Vault Hunters differs from the version Arcane Beam was built against. ")
                .append("These features may be inactive: ");
        boolean first = true;
        for (Map.Entry<String, List<String>> entry : byFeature.entrySet()) {
            if (!first) {
                text.append("; ");
            }
            first = false;
            text.append(entry.getKey()).append(" (").append(String.join(", ", entry.getValue())).append(')');
        }
        return text.toString();
    }

    /**
     * Compares every target with the Vault classes. {@code classBytes} supplies class files for the call-site checks
     * and may return null, which skips only those checks.
     */
    static List<Finding> findMissing(List<Target> targets, Function<String, Class<?>> classLookup, Function<String, byte[]> classBytes) {
        List<Finding> missing = new ArrayList<>();
        Map<String, byte[]> bytesCache = new LinkedHashMap<>();
        for (Target target : targets) {
            String label = shortName(target.className()) + (target.kind() == Kind.TYPE ? "" : "#" + target.names().get(0));
            try {
                Class<?> type = classLookup.apply(target.className());
                if (type == null) {
                    missing.add(new Finding(target.feature(), "class " + shortName(target.className()) + " not found"));
                    continue;
                }
                switch (target.kind()) {
                    case TYPE -> {
                    }
                    case FIELD -> {
                        if (!hasField(type, target)) {
                            missing.add(new Finding(target.feature(), "field " + label + " " + target.descriptor()
                                    + (target.staticMember() ? " (static)" : "") + " not found"));
                        }
                    }
                    case METHOD -> {
                        if (!hasMethod(type, target)) {
                            missing.add(new Finding(target.feature(), "method " + label + target.descriptor() + " not found"));
                        } else if (!target.calls().isEmpty()) {
                            byte[] bytes = bytesCache.computeIfAbsent(target.className(), classBytes);
                            if (bytes != null) {
                                checkCalls(target, label, bytes, missing);
                            }
                        }
                    }
                }
            } catch (Throwable throwable) {
                missing.add(new Finding(target.feature(), label + " could not be inspected (" + throwable.getClass().getSimpleName() + ")"));
            }
        }
        return missing;
    }

    private static void checkCalls(Target target, String label, byte[] classBytes, List<Finding> missing) {
        Map<Call, Integer> counts = countCalls(classBytes, target);
        for (Call call : target.calls()) {
            int found = counts.getOrDefault(call, 0);
            boolean ok = call.expected() > 0 ? found == call.expected() : found > 0;
            if (!ok) {
                missing.add(new Finding(target.feature(), "call " + shortName(call.owner().replace('/', '.')) + "#" + call.names().get(0)
                        + call.descriptor() + " inside " + label + " found " + found + "x"
                        + (call.expected() > 0 ? ", expected " + call.expected() + "x" : "")));
            }
        }
    }

    /** Counts, for each wanted call, the matching invoke instructions inside the target method's body. */
    static Map<Call, Integer> countCalls(byte[] classBytes, Target target) {
        Map<Call, Integer> counts = new LinkedHashMap<>();
        new ClassReader(classBytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if (!target.names().contains(name) || !target.descriptor().equals(descriptor)) {
                    return null;
                }
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String callName, String callDescriptor, boolean isInterface) {
                        for (Call call : target.calls()) {
                            if (call.owner().equals(owner) && call.names().contains(callName) && call.descriptor().equals(callDescriptor)) {
                                counts.merge(call, 1, Integer::sum);
                            }
                        }
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return counts;
    }

    private static boolean hasField(Class<?> type, Target target) {
        for (Field field : type.getDeclaredFields()) {
            if (target.names().contains(field.getName())
                    && field.getType().descriptorString().equals(target.descriptor())
                    && Modifier.isStatic(field.getModifiers()) == target.staticMember()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasMethod(Class<?> type, Target target) {
        if (target.names().contains("<init>")) {
            for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                if (descriptor(void.class, constructor.getParameterTypes()).equals(target.descriptor())) {
                    return true;
                }
            }
            return false;
        }
        for (Method method : type.getDeclaredMethods()) {
            // A compiler-generated bridge has the same name but an erased signature, so it never matches a typed descriptor.
            if (target.names().contains(method.getName()) && descriptor(method.getReturnType(), method.getParameterTypes())
                    .equals(target.descriptor())) {
                return true;
            }
        }
        return false;
    }

    static String descriptor(Class<?> returnType, Class<?>[] parameterTypes) {
        return MethodType.methodType(returnType, parameterTypes).toMethodDescriptorString();
    }

    private static String shortName(String className) {
        int dot = className.lastIndexOf('.');
        return dot < 0 ? className : className.substring(dot + 1);
    }

    private static Target method(String feature, String className, String descriptor, String... names) {
        return new Target(feature, className, Kind.METHOD, List.of(names), descriptor, false, List.of());
    }

    private static Target method(String feature, String className, String descriptor, List<String> names, List<Call> calls) {
        return new Target(feature, className, Kind.METHOD, names, descriptor, false, calls);
    }

    private static Target field(String feature, String className, String name, String descriptor, boolean isStatic) {
        return new Target(feature, className, Kind.FIELD, List.of(name), descriptor, isStatic, List.of());
    }

    private static Target type(String feature, String className) {
        return new Target(feature, className, Kind.TYPE, List.of(""), "", false, List.of());
    }

    private static Call call(String owner, String descriptor, int expected, String... names) {
        return new Call(owner, List.of(names), descriptor, expected);
    }

    enum Kind {
        TYPE, FIELD, METHOD
    }

    /**
     * A Vault member the mod depends on. {@code names} are accepted aliases for the same member; {@code descriptor}
     * is a JVM descriptor; {@code calls} are invocations that must occur inside a METHOD target.
     */
    record Target(String feature, String className, Kind kind, List<String> names, String descriptor,
                  boolean staticMember, List<Call> calls) {
    }

    /** An invoke instruction an injector or redirect aims at; {@code expected} 0 means "at least once". */
    record Call(String owner, List<String> names, String descriptor, int expected) {
    }

    /** One missing or changed member and the feature that needs it. */
    record Finding(String feature, String detail) {
    }
}
