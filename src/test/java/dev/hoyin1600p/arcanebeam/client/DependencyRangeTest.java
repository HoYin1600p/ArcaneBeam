package dev.hoyin1600p.arcanebeam.client;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A dependency range that rejects the installed Vault or Forge stops the game at the loading screen, so pin the
 * builds the mod is played with (FML compares versions with Maven's rules, as here).
 */
class DependencyRangeTest {
    private static String modsToml() throws IOException {
        try (InputStream stream = DependencyRangeTest.class.getResourceAsStream("/META-INF/mods.toml")) {
            assertNotNull(stream);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static VersionRange rangeFor(String modId) throws Exception {
        Matcher block = Pattern.compile("modId\\s*=\\s*\"" + Pattern.quote(modId) + "\"(.*?)(?=\\[\\[|\\z)", Pattern.DOTALL)
                .matcher(modsToml());
        assertTrue(block.find(), "dependency block for " + modId);
        Matcher range = Pattern.compile("versionRange\\s*=\\s*\"([^\"]+)\"").matcher(block.group(1));
        assertTrue(range.find(), "versionRange for " + modId);
        return VersionRange.createFromVersionSpec(range.group(1));
    }

    @Test
    void everyVaultBuildInUseSatisfiesTheRange() throws Exception {
        VersionRange vault = rangeFor("the_vault");
        for (String version : new String[] {
                "1.18.2-3.21.6.6884",            // Wolds 0.34.1
                "1.18.2-20.0.3-remastered.6886",  // reference jar
                "1.18.2-20.0.3-remastered.6932",
                "1.18.2-20.0.3-remastered.6937",  // vaultcrafters pack
        }) {
            assertTrue(vault.containsVersion(new DefaultArtifactVersion(version)), version + " must satisfy " + vault);
        }
        assertFalse(vault.containsVersion(new DefaultArtifactVersion("1.18.2-3.20.0")), "older Wolds builds are excluded");
    }

    @Test
    void theForgeInUseSatisfiesTheRange() throws Exception {
        VersionRange forge = rangeFor("forge");
        for (String version : new String[] {"40.1.67", "40.1.93", "40.2.0", "40.2.10", "40.2.21", "40.3.11"}) {
            assertTrue(forge.containsVersion(new DefaultArtifactVersion(version)), version + " must satisfy " + forge);
        }
    }
}
