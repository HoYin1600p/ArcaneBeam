package dev.hoyin1600p.arcanebeam.update;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Parses the update.json committed in the repository with the mod's own parser. */
class RepositoryManifestTest {
    private static final String DOWNLOAD_URL =
            "https://www.curseforge.com/minecraft/mc-mods/arcanebeam";

    private static UpdateNotice parseFrom(String installedVersion) throws IOException {
        String json = Files.readString(Path.of("update.json"), StandardCharsets.UTF_8);
        return UpdateManifestParser.parse(
                json,
                "arcanebeam",
                "Arcane Beam",
                installedVersion,
                "1.18.2",
                DOWNLOAD_URL
        ).orElseThrow();
    }

    @Test
    void releaseManifestAdvertisesCriticalZeroTwoZero() throws IOException {
        for (String installed : new String[] {"0.1.11", "0.1.14"}) {
            UpdateNotice notice = parseFrom(installed);
            assertEquals("0.2.0", notice.targetVersion());
            assertEquals(UpdateNotice.Severity.CRITICAL, notice.severity());
            assertEquals(
                    "Major visual update, labyrinth route, new settings screen and startup fixes",
                    notice.message()
            );
        }
    }
}
