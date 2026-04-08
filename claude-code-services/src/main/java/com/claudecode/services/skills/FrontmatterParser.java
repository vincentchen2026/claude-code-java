package com.claudecode.services.skills;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses YAML frontmatter from markdown skill files.
 * Extracts metadata between --- markers: name, description, allowedTools, paths.
 */
public class FrontmatterParser {

    private static final Pattern FRONTMATTER_PATTERN =
            Pattern.compile("\\A---\\s*\\n(.*?)\\n---\\s*\\n?(.*)", Pattern.DOTALL);

    /**
     * Parse a markdown file content into frontmatter metadata and body content.
     *
     * @param content the full file content
     * @return parsed result with metadata map and body
     */
    public ParseResult parse(String content) {
        if (content == null || content.isBlank()) {
            return new ParseResult(Map.of(), "");
        }

        Matcher matcher = FRONTMATTER_PATTERN.matcher(content);
        if (!matcher.matches()) {
            return new ParseResult(Map.of(), content.trim());
        }

        String yamlSection = matcher.group(1);
        String body = matcher.group(2).trim();

        Map<String, Object> metadata = parseYamlSimple(yamlSection);
        return new ParseResult(metadata, body);
    }

    /**
     * Simple YAML-like parser for frontmatter key-value pairs.
     * Supports: string values, list values (- item syntax).
     */
    Map<String, Object> parseYamlSimple(String yaml) {
        Map<String, Object> result = new LinkedHashMap<>();
        String currentKey = null;
        List<String> currentList = null;

        for (String line : yaml.split("\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            // List item
            if (trimmed.startsWith("- ") && currentKey != null) {
                if (currentList == null) {
                    currentList = new ArrayList<>();
                }
                currentList.add(trimmed.substring(2).trim());
                continue;
            }

            // Flush previous list
            if (currentKey != null && currentList != null) {
                result.put(currentKey, List.copyOf(currentList));
                currentList = null;
                currentKey = null;
            }

            // Key-value pair
            int colonIdx = trimmed.indexOf(':');
            if (colonIdx > 0) {
                String key = trimmed.substring(0, colonIdx).trim();
                String value = trimmed.substring(colonIdx + 1).trim();

                if (value.isEmpty()) {
                    // Next lines might be a list
                    currentKey = key;
                } else {
                    result.put(key, value);
                    currentKey = key;
                }
            }
        }

        // Flush trailing list
        if (currentKey != null && currentList != null) {
            result.put(currentKey, List.copyOf(currentList));
        }

        return result;
    }

    /**
     * Extract a string value from metadata.
     */
    public static String getString(Map<String, Object> metadata, String key) {
        Object val = metadata.get(key);
        return val instanceof String s ? s : null;
    }

    /**
     * Extract a list of strings from metadata.
     */
    @SuppressWarnings("unchecked")
    public static List<String> getStringList(Map<String, Object> metadata, String key) {
        Object val = metadata.get(key);
        if (val instanceof List<?> list) {
            return (List<String>) list;
        }
        if (val instanceof String s) {
            return List.of(s);
        }
        return List.of();
    }

    /**
     * Result of parsing a markdown file with frontmatter.
     */
    public record ParseResult(Map<String, Object> metadata, String body) {

        public String name() {
            return FrontmatterParser.getString(metadata, "name");
        }

        public String description() {
            return FrontmatterParser.getString(metadata, "description");
        }

        public List<String> allowedTools() {
            return FrontmatterParser.getStringList(metadata, "allowedTools");
        }

        public List<String> paths() {
            return FrontmatterParser.getStringList(metadata, "paths");
        }
    }
}
