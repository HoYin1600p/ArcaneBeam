package dev.hoyin1600p.arcanebeam.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Forge's one-argument {@code addListener(Consumer)} finds the event type from the lambda's generic signature. A
 * handler wrapped by {@link FeatureGuard#wrap} has an erased type, so Forge sees {@code Object} and the game fails
 * to load. Every wrapped listener must use the overload that names the event class.
 */
class GuardedListenerRegistrationTest {
    private static final Pattern ADD_LISTENER = Pattern.compile("addListener\\((.*?)\\);", Pattern.DOTALL);

    @Test
    void wrappedListenersNameTheirEventClass() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : (Iterable<Path>) files.filter(path -> path.toString().endsWith(".java"))::iterator) {
                Matcher matcher = ADD_LISTENER.matcher(Files.readString(file));
                while (matcher.find()) {
                    String arguments = matcher.group(1);
                    if (arguments.contains(".wrap(") && !arguments.contains(".class") && !arguments.contains(", type,")) {
                        offenders.add(file.getFileName() + ": addListener(" + arguments.trim() + ")");
                    }
                }
            }
        }
        assertTrue(offenders.isEmpty(), "Wrapped listeners without an event class: " + offenders);
    }
}
