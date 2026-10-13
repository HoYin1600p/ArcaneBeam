package dev.hoyin1600p.arcanebeam.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UpdateNoticeParserTest {
    private static final String DOWNLOAD_URL =
            "https://www.curseforge.com/minecraft/mc-mods/arcanebeam";

    @Test
    void parsesCriticalTargetMessage() {
        String target = "1.0.12";
        Map<String, String> changes = new LinkedHashMap<>();
        changes.put(
                target,
                "[CRITICAL] Critical Bug Fix"
        );
        UpdateNotice notice = UpdateNoticeParser.parse(
                "arcanebeam",
                "1.0.11",
                target,
                changes,
                "Arcane Beam",
                DOWNLOAD_URL
        ).orElseThrow();

        assertEquals(UpdateNotice.Severity.CRITICAL, notice.severity());
        assertEquals("Critical Bug Fix", notice.message());
        assertEquals("1.0.12", notice.targetVersion());
    }

    @Test
    void treatsPlainChangelogAsNormalMessage() {
        UpdateNotice notice = UpdateNoticeParser.parse(
                "arcanebeam",
                "1.0.11",
                "1.0.12",
                Map.of("1.0.12", "Performance Improvement"),
                "Arcane Beam",
                DOWNLOAD_URL
        ).orElseThrow();

        assertEquals(UpdateNotice.Severity.NORMAL, notice.severity());
        assertEquals("Performance Improvement", notice.message());
    }

    @Test
    void ignoresUpToDateResult() {
        assertTrue(UpdateNoticeParser.parse(
                "arcanebeam",
                "1.0.12",
                "1.0.12",
                Map.of(),
                "Arcane Beam",
                DOWNLOAD_URL
        ).isEmpty());
    }

    @Test
    void stableReleaseSupersedesAPreRelease() {
        UpdateNotice notice = UpdateNoticeParser.parse(
                "arcanebeam",
                "1.0.12-beta.1",
                "1.0.12",
                Map.of("1.0.12", "Stable Release"),
                "Arcane Beam",
                DOWNLOAD_URL
        ).orElseThrow();

        assertEquals("1.0.12", notice.targetVersion());
        assertEquals("Stable Release", notice.message());
    }

    @Test
    void stripsSectionSignFormattingAndControlCharacters() {
        UpdateNotice notice = UpdateNoticeParser.parse(
                "arcanebeam",
                "1.0.11",
                "1.0.12",
                Map.of("1.0.12", "§cRed §lBold text"),
                "Arcane Beam",
                DOWNLOAD_URL
        ).orElseThrow();

        assertEquals("Red Bold text", notice.message());
    }

    @Test
    void truncatesWithoutSplittingASurrogatePair() {
        String emoji = new String(Character.toChars(0x1F600));
        String raw = "a".repeat(94) + emoji + "tail";

        String message = UpdateNoticeParser.parseMessage(raw).message();

        assertEquals("a".repeat(94) + emoji + "…", message);
        assertTrue(!Character.isHighSurrogate(
                message.charAt(message.length() - 2)
        ));
    }

    @Test
    void skipsPlaceholderAndUnknownCurrentVersions() {
        for (String current : new String[] {
                "${file.jarVersion}", "NONE", "0.0NONE", "dev", " "
        }) {
            assertTrue(UpdateNoticeParser.parse(
                    "arcanebeam",
                    current,
                    "1.0.12",
                    Map.of(),
                    "Arcane Beam",
                    DOWNLOAD_URL
            ).isEmpty(), current);
        }
    }

    @Test
    void skippedCriticalReleaseKeepsTheNoticeCritical() {
        Map<String, String> changes = new LinkedHashMap<>();
        changes.put("1.1.1", "[CRITICAL] Fixes invisible blocks");
        changes.put("1.1.2", "[CRITICAL] Fixes vanishing world parts");
        changes.put("1.1.3", "Adds an in-game settings screen");
        UpdateNotice notice = UpdateNoticeParser.parse(
                "arcanebeam",
                "1.1.0",
                "1.1.3",
                changes,
                "Arcane Beam",
                DOWNLOAD_URL
        ).orElseThrow();

        assertEquals(UpdateNotice.Severity.CRITICAL, notice.severity());
        assertEquals("Fixes vanishing world parts", notice.message());
        assertEquals("1.1.3", notice.targetVersion());
    }

    @Test
    void criticalReleasesAlreadyInstalledDoNotEscalate() {
        Map<String, String> changes = new LinkedHashMap<>();
        changes.put("1.1.2", "[CRITICAL] Fixes vanishing world parts");
        changes.put("1.1.3", "Adds an in-game settings screen");
        UpdateNotice notice = UpdateNoticeParser.parse(
                "arcanebeam",
                "1.1.2",
                "1.1.3",
                changes,
                "Arcane Beam",
                DOWNLOAD_URL
        ).orElseThrow();

        assertEquals(UpdateNotice.Severity.NORMAL, notice.severity());
        assertEquals("Adds an in-game settings screen", notice.message());
    }
}
