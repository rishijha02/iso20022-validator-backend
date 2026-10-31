package com.isovalidator.iso.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Discovers message schemas bundled under schemas/{profile}/{family}. */
@Service
public class Pacs008SchemaCatalog {
    private static final Pattern VERSION_FILE = Pattern.compile("(pacs|camt|pain)\\.(\\d{3})\\.(\\d{3}\\.\\d{2})\\.xsd");
    private static final Pattern FAMILY = Pattern.compile("pacs|camt|pain");
    private static final Pattern MESSAGE_TYPE = Pattern.compile("(pacs|camt|pain)\\.\\d{3}");
    private final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public List<String> messageTypes(String family, String geography) {
        String normalizedFamily = normalizeFamily(family);
        String profile = normalizeProfile(geography);
        String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "/schemas/" + profile + "/" + normalizedFamily + "/" + normalizedFamily + ".*.xsd";
        try {
            return Arrays.stream(resolver.getResources(pattern)).map(Resource::getFilename).filter(Objects::nonNull)
                    .map(VERSION_FILE::matcher).filter(Matcher::matches)
                    .map(matcher -> normalizedFamily + "." + matcher.group(2)).distinct().sorted().toList();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to list " + normalizedFamily + " schemas for " + profile, e);
        }
    }

    public List<String> versions(String geography) { return versions("pacs.008", geography); }

    public List<String> versions(String messageType, String geography) {
        String type = normalizeMessageType(messageType);
        String family = type.substring(0, type.indexOf('.'));
        String profile = normalizeProfile(geography);
        String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "/schemas/" + profile + "/" + family + "/" + type + ".*.xsd";
        try {
            return Arrays.stream(resolver.getResources(pattern)).map(Resource::getFilename).filter(Objects::nonNull)
                    .map(VERSION_FILE::matcher).filter(Matcher::matches)
                    .filter(matcher -> type.equals(matcher.group(1) + "." + matcher.group(2)))
                    .map(matcher -> matcher.group(3)).distinct().sorted(Comparator.reverseOrder()).toList();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to list " + type + " schemas for " + profile, e);
        }
    }

    public String resolveVersion(String geography, String requestedVersion) { return resolveVersion("pacs.008", geography, requestedVersion); }

    public String resolveVersion(String messageType, String geography, String requestedVersion) {
        String type = normalizeMessageType(messageType);
        List<String> available = versions(type, geography);
        if (available.isEmpty()) throw new IllegalArgumentException("No " + type + " XSD is bundled for " + geography);
        if (requestedVersion == null || requestedVersion.isBlank()) return available.get(0);
        String requested = requestedVersion.trim();
        if (!available.contains(requested)) throw new IllegalArgumentException(type + " version " + requested + " is not bundled for " + geography + ". Available versions: " + String.join(", ", available));
        return requested;
    }

    public String schemaResourcePath(String geography, String version) { return schemaResourcePath("pacs.008", geography, version); }

    public String schemaResourcePath(String messageType, String geography, String version) {
        String type = normalizeMessageType(messageType);
        String family = type.substring(0, type.indexOf('.'));
        return "/schemas/" + normalizeProfile(geography) + "/" + family + "/" + type + "." + version + ".xsd";
    }

    private String normalizeMessageType(String messageType) {
        String type = messageType == null ? "pacs.008" : messageType.trim().toLowerCase(Locale.ROOT);
        if (!MESSAGE_TYPE.matcher(type).matches()) throw new IllegalArgumentException("Unsupported ISO 20022 message type: " + messageType);
        return type;
    }

    private String normalizeFamily(String family) {
        String type = family == null ? "" : family.trim().toLowerCase(Locale.ROOT);
        if (!FAMILY.matcher(type).matches()) throw new IllegalArgumentException("Unsupported ISO 20022 message family: " + family);
        return type;
    }

    private String normalizeProfile(String geography) {
        String profile = geography == null ? "ISO20022" : geography.trim().toUpperCase(Locale.ROOT);
        return switch (profile) {
            case "ISO", "ISO20022" -> "ISO20022";
            case "CBPR_PLUS" -> "CBPR_PLUS";
            case "SEPA" -> "SEPA";
            default -> throw new IllegalArgumentException("Unsupported profile: " + geography);
        };
    }
}
