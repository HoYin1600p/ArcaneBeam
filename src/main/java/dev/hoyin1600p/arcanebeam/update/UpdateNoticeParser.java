package dev.hoyin1600p.arcanebeam.update;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.versioning.ComparableVersion;

public final class UpdateNoticeParser {
    private static final String CRITICAL_PREFIX = "[CRITICAL]";
    private static final String NORMAL_PREFIX = "[NORMAL]";
    private static final int MAX_MESSAGE_LENGTH = 96;
    private static final Logger LOGGER = LogManager.getLogger(
            UpdateNoticeParser.class
    );

    private UpdateNoticeParser() {
    }

    public static Optional<UpdateNotice> parse(
            String modId,
            String currentVersion,
            String targetVersion,
            Map<String, String> changes,
            String displayName,
            String downloadUrl
    ) {
        if (currentVersion != null && !isKnownVersion(currentVersion)) {
            LOGGER.debug(
                    "Skipping update notice for {}: current version '{}' is"
                            + " unknown or an unexpanded placeholder",
                    modId,
                    currentVersion
            );
            return Optional.empty();
        }
        if (currentVersion == null
                || targetVersion == null
                || new ComparableVersion(currentVersion).compareTo(
                new ComparableVersion(targetVersion)
        ) >= 0) {
            return Optional.empty();
        }

        ParsedMessage parsedMessage = parseMessage(
                findTargetMessage(targetVersion, changes)
        );
        if (parsedMessage.severity() != UpdateNotice.Severity.CRITICAL) {
            // A player who skipped a critical release still needs to hear about it, even
            // when the newest release is a normal one: show the newest skipped critical one.
            ParsedMessage skipped = newestSkippedCritical(currentVersion, targetVersion, changes);
            if (skipped != null) {
                parsedMessage = skipped;
            }
        }
        return Optional.of(new UpdateNotice(
                modId,
                displayName,
                targetVersion,
                parsedMessage.severity(),
                parsedMessage.message(),
                downloadUrl
        ));
    }

    static ParsedMessage parseMessage(String rawMessage) {
        String message = normalizeMessage(rawMessage);
        UpdateNotice.Severity severity = UpdateNotice.Severity.NORMAL;

        if (startsWithIgnoreCase(message, CRITICAL_PREFIX)) {
            severity = UpdateNotice.Severity.CRITICAL;
            message = message.substring(CRITICAL_PREFIX.length()).trim();
        } else if (startsWithIgnoreCase(message, NORMAL_PREFIX)) {
            message = message.substring(NORMAL_PREFIX.length()).trim();
        }

        if (message.codePointCount(0, message.length()) > MAX_MESSAGE_LENGTH) {
            int end = message.offsetByCodePoints(0, MAX_MESSAGE_LENGTH - 1);
            message = message.substring(0, end).trim() + "…";
        }
        return new ParsedMessage(severity, message);
    }

    /**
     * The newest critical message for a version after {@code current} and before
     * {@code target}, or null when every skipped release was a normal one.
     */
    static ParsedMessage newestSkippedCritical(
            String current,
            String target,
            Map<String, String> changes
    ) {
        if (changes == null || changes.isEmpty()) {
            return null;
        }
        ComparableVersion comparableCurrent = new ComparableVersion(current);
        ComparableVersion comparableTarget = new ComparableVersion(target);
        ComparableVersion newest = null;
        ParsedMessage found = null;
        for (Map.Entry<String, String> entry : changes.entrySet()) {
            ComparableVersion version = new ComparableVersion(entry.getKey());
            if (version.compareTo(comparableCurrent) <= 0
                    || version.compareTo(comparableTarget) >= 0
                    || (newest != null && version.compareTo(newest) <= 0)) {
                continue;
            }
            ParsedMessage parsed = parseMessage(entry.getValue());
            if (parsed.severity() == UpdateNotice.Severity.CRITICAL) {
                newest = version;
                found = parsed;
            }
        }
        return found;
    }

    private static String findTargetMessage(
            String target,
            Map<String, String> changes
    ) {
        if (changes == null || changes.isEmpty()) {
            return "";
        }

        ComparableVersion comparableTarget = new ComparableVersion(target);
        for (Map.Entry<String, String> entry : changes.entrySet()) {
            if (new ComparableVersion(entry.getKey()).compareTo(
                    comparableTarget
            ) == 0) {
                return entry.getValue();
            }
        }
        return "";
    }

    private static String normalizeMessage(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        // Collapse whitespace first, then drop section-sign formatting codes
        // (with their code character) and any remaining control characters.
        return value.replaceAll("\\s+", " ")
                .replaceAll("§.?", "")
                .replaceAll("\\p{Cc}", "")
                .trim();
    }

    static boolean isKnownVersion(String version) {
        String trimmed = version.trim();
        return !trimmed.isEmpty()
                && !trimmed.contains("${")
                && !trimmed.toUpperCase(Locale.ROOT).contains("NONE")
                && Character.isDigit(trimmed.charAt(0));
    }

    private static boolean startsWithIgnoreCase(String value, String prefix) {
        return value.length() >= prefix.length()
                && value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    record ParsedMessage(UpdateNotice.Severity severity, String message) {
    }
}
