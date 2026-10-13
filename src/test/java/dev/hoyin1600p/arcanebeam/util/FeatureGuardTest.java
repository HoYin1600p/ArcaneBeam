package dev.hoyin1600p.arcanebeam.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatureGuardTest {
    @Test
    void failDisablesTheFeatureUntilReset() {
        FeatureGuard guard = new FeatureGuard("demo");
        assertFalse(guard.isDisabled());
        guard.fail(new IllegalStateException("boom"));
        assertTrue(guard.isDisabled());
        guard.fail(new IllegalStateException("again"));
        assertTrue(guard.isDisabled());
        guard.reset();
        assertFalse(guard.isDisabled());
    }

    @Test
    void wrappedHandlerRunsUntilItThrowsAndIsThenSkipped() {
        FeatureGuard guard = new FeatureGuard("demo");
        AtomicInteger calls = new AtomicInteger();
        Consumer<String> wrapped = guard.wrap(event -> {
            calls.incrementAndGet();
            if (event.equals("bad")) {
                throw new IllegalArgumentException("bad event");
            }
        });
        wrapped.accept("ok");
        wrapped.accept("ok");
        assertEquals(2, calls.get());
        wrapped.accept("bad");
        assertTrue(guard.isDisabled());
        wrapped.accept("ok");
        assertEquals(3, calls.get(), "a disabled feature must not run its handler again");
    }

    @Test
    void linkageErrorsFromARenamedVaultMemberAreCaughtToo() {
        FeatureGuard guard = new FeatureGuard("demo");
        guard.wrap((String event) -> {
            throw new NoSuchMethodError("renamed");
        }).accept("x");
        assertTrue(guard.isDisabled());
    }

    @Test
    void guardsAreIndependent() {
        FeatureGuard first = new FeatureGuard("a");
        FeatureGuard second = new FeatureGuard("b");
        first.fail(new RuntimeException());
        assertFalse(second.isDisabled());
    }
}
