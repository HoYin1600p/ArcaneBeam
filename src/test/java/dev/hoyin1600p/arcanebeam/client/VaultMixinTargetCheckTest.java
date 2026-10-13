package dev.hoyin1600p.arcanebeam.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaultMixinTargetCheckTest {
    private static final String FAKE = FakeVault.class.getName();
    private static final String RENDER_TYPED = "(Ljava/lang/String;F)V";

    /** A stand-in for a Vault class: a typed method plus the bridge a generic superclass would add. */
    @SuppressWarnings("unused")
    static class FakeVault {
        static final int PRESSED = 1;
        private boolean isRail;

        public void render(String typed, float partial) {
        }

        public void render(Object erased, float partial) {
        }

        public static void onlyErased(Object value) {
        }

        FakeVault(String name, int action) {
        }

        void tick() {
            String.valueOf(1);
            String.valueOf(2);
            String.valueOf(3);
        }
    }

    private static VaultMixinTargetCheck.Target method(String name, String descriptor) {
        return new VaultMixinTargetCheck.Target("demo", FAKE, VaultMixinTargetCheck.Kind.METHOD, List.of(name), descriptor, false, List.of());
    }

    private static List<VaultMixinTargetCheck.Finding> check(VaultMixinTargetCheck.Target... targets) {
        return VaultMixinTargetCheck.findMissing(List.of(targets), VaultMixinTargetCheckTest::lookup, VaultMixinTargetCheckTest::bytes);
    }

    @Test
    void presentTypedMembersProduceNoFindings() {
        assertTrue(check(
                method("render", RENDER_TYPED),
                method("<init>", "(Ljava/lang/String;I)V"),
                new VaultMixinTargetCheck.Target("demo", FAKE, VaultMixinTargetCheck.Kind.FIELD, List.of("isRail"), "Z", false, List.of()),
                new VaultMixinTargetCheck.Target("demo", FAKE, VaultMixinTargetCheck.Kind.FIELD, List.of("PRESSED"), "I", true, List.of()),
                new VaultMixinTargetCheck.Target("demo", FAKE, VaultMixinTargetCheck.Kind.TYPE, List.of(""), "", false, List.of())
        ).isEmpty());
    }

    @Test
    void aBridgeWithAnErasedSignatureIsNotProofThatTheTypedMethodExists() {
        // Only render(Object,float) exists for this descriptor, as with a renamed typed render in Vault.
        List<VaultMixinTargetCheck.Finding> findings = check(method("onlyErased", "(Ljava/lang/String;)V"));
        assertEquals(1, findings.size());
        assertTrue(findings.get(0).detail().contains("onlyErased"));
    }

    @Test
    void aChangedParameterTypeIsReported() {
        assertEquals(1, check(method("render", "(Ljava/lang/Integer;F)V")).size());
        assertEquals(1, check(method("render", "(Ljava/lang/String;D)V")).size());
        assertEquals(1, check(method("<init>", "(Ljava/lang/String;J)V")).size());
    }

    @Test
    void aFieldWithTheWrongTypeOrStaticnessIsReported() {
        assertEquals(1, check(new VaultMixinTargetCheck.Target("demo", FAKE, VaultMixinTargetCheck.Kind.FIELD,
                List.of("isRail"), "I", false, List.of())).size());
        assertEquals(1, check(new VaultMixinTargetCheck.Target("demo", FAKE, VaultMixinTargetCheck.Kind.FIELD,
                List.of("isRail"), "Z", true, List.of())).size());
    }

    @Test
    void aliasesAreAcceptedForTheSameDescriptor() {
        assertTrue(check(method("m_1234_", RENDER_TYPED)).size() == 1);
        assertTrue(check(new VaultMixinTargetCheck.Target("demo", FAKE, VaultMixinTargetCheck.Kind.METHOD,
                List.of("m_1234_", "render"), RENDER_TYPED, false, List.of())).isEmpty());
    }

    @Test
    void missingClassIsReportedAndLookupFailuresNeverThrow() {
        List<VaultMixinTargetCheck.Finding> findings = VaultMixinTargetCheck.findMissing(List.of(
                new VaultMixinTargetCheck.Target("a", "no.such.Type", VaultMixinTargetCheck.Kind.TYPE, List.of(""), "", false, List.of()),
                method("render", RENDER_TYPED)), name -> {
            if (name.equals("no.such.Type")) {
                return null;
            }
            throw new NoClassDefFoundError(name);
        }, name -> null);
        assertEquals(2, findings.size());
        assertTrue(findings.get(0).detail().contains("not found"));
        assertTrue(findings.get(1).detail().contains("could not be inspected"));
    }

    @Test
    void callSitesAreCountedInsideTheTargetMethodBody() {
        VaultMixinTargetCheck.Call valueOf = new VaultMixinTargetCheck.Call("java/lang/String", List.of("valueOf"), "(I)Ljava/lang/String;", 3);
        VaultMixinTargetCheck.Target ok = new VaultMixinTargetCheck.Target("demo", FAKE, VaultMixinTargetCheck.Kind.METHOD,
                List.of("tick"), "()V", false, List.of(valueOf));
        assertTrue(check(ok).isEmpty());

        VaultMixinTargetCheck.Call wrongCount = new VaultMixinTargetCheck.Call("java/lang/String", List.of("valueOf"), "(I)Ljava/lang/String;", 2);
        VaultMixinTargetCheck.Call wrongOwner = new VaultMixinTargetCheck.Call("java/lang/Integer", List.of("valueOf"), "(I)Ljava/lang/String;", 0);
        VaultMixinTargetCheck.Call renamed = new VaultMixinTargetCheck.Call("java/lang/String", List.of("copyValueOf"), "(I)Ljava/lang/String;", 0);
        List<VaultMixinTargetCheck.Finding> findings = check(new VaultMixinTargetCheck.Target("demo", FAKE,
                VaultMixinTargetCheck.Kind.METHOD, List.of("tick"), "()V", false, List.of(wrongCount, wrongOwner, renamed)));
        assertEquals(3, findings.size());
        assertTrue(findings.get(0).detail().contains("found 3x, expected 2x"));
    }

    @Test
    void missingClassBytesSkipOnlyTheCallSiteCheck() {
        VaultMixinTargetCheck.Call valueOf = new VaultMixinTargetCheck.Call("java/lang/String", List.of("valueOf"), "(I)Ljava/lang/String;", 99);
        assertTrue(VaultMixinTargetCheck.findMissing(List.of(new VaultMixinTargetCheck.Target("demo", FAKE,
                VaultMixinTargetCheck.Kind.METHOD, List.of("tick"), "()V", false, List.of(valueOf))),
                VaultMixinTargetCheckTest::lookup, name -> null).isEmpty());
    }

    @Test
    void summaryNamesEachAffectedFeatureOnce() {
        String summary = VaultMixinTargetCheck.summarize(List.of(
                new VaultMixinTargetCheck.Finding("smite visuals", "field SmiteBolt#COLOR not found"),
                new VaultMixinTargetCheck.Finding("storm visuals", "field VaultStormEntity#RADIUS not found"),
                new VaultMixinTargetCheck.Finding("smite visuals", "method SmiteBoltRenderer#render not found")));
        assertEquals(1, summary.lines().count(), "one WARN line");
        assertTrue(summary.indexOf("smite visuals") == summary.lastIndexOf("smite visuals"));
        assertTrue(summary.contains("storm visuals"));
    }

    private static Class<?> lookup(String name) {
        try {
            return Class.forName(name, false, VaultMixinTargetCheckTest.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static byte[] bytes(String className) {
        try (InputStream stream = VaultMixinTargetCheckTest.class.getClassLoader()
                .getResourceAsStream(className.replace('.', '/') + ".class")) {
            return stream == null ? null : stream.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }
}
