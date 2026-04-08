package com.claudecode.ui.renderer;

import java.util.regex.Pattern;

/**
 * Task 68.5: Plain text detector for markdown fast path.
 * Detects whether text contains markdown syntax, enabling skipping
 * full markdown parsing for plain text content.
 */
public class PlainTextDetector {

    private static final Pattern MARKDOWN_SYNTAX_PATTERN = Pattern.compile(
        "[#*`_\\[\\]!>|~]|^\\s*[-*+]\\s|^\\s*\\d+\\.\\s"
    );

    private static final Pattern COMPLEX_MARKDOWN_PATTERN = Pattern.compile(
        "(^#{1,6}\\s)|" +           // Headings
        "(\\*\\*[^*]+\\*\\*)|" +    // Bold
        "(\\*[^*]+\\*)|" +          // Italic
        "(`[^`]+`)|" +               // Inline code
        "(\\[[^\\]]+\\]\\([^)]+\\))|" + // Links
        "(!\\[.*?\\]\\(.*?\\))|" +  // Images
        "(^\\s*[-*+]\\s)|" +        // Bullet lists
        "(^\\s*\\d+\\.\\s)|" +      // Ordered lists
        "(^\\s*>\\s)|" +            // Blockquotes
        "(^\\s*[-*_]{3,}\\s*$)|" +  // Horizontal rules
        "(```)"                      // Code blocks
    );

    /**
     * Check if the given text contains any markdown syntax.
     *
     * @param text the text to check
     * @return true if markdown syntax is detected, false if plain text
     */
    public boolean containsMarkdown(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return MARKDOWN_SYNTAX_PATTERN.matcher(text).find();
    }

    /**
     * Check if the given text contains complex markdown syntax
     * that requires full parsing.
     *
     * @param text the text to check
     * @return true if complex markdown is detected
     */
    public boolean containsComplexMarkdown(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return COMPLEX_MARKDOWN_PATTERN.matcher(text).find();
    }

    /**
     * Determine if text can be rendered as-is without markdown processing.
     * This is a fast path that avoids full markdown parsing.
     *
     * @param text the text to check
     * @return true if text is plain text without markdown syntax
     */
    public boolean isPlainText(String text) {
        return !containsMarkdown(text);
    }

    /**
     * Get a summary of what markdown features are present in the text.
     *
     * @param text the text to analyze
     * @return summary string describing detected markdown features
     */
    public String detectFeatures(String text) {
        if (text == null || text.isEmpty()) {
            return "empty";
        }

        StringBuilder features = new StringBuilder();
        if (Pattern.compile("^#{1,6}\\s", Pattern.MULTILINE).matcher(text).find()) {
            features.append("headings ");
        }
        if (Pattern.compile("\\*\\*[^*]+\\*\\*").matcher(text).find()) {
            features.append("bold ");
        }
        if (Pattern.compile("(?<!\\*)\\*[^*]+\\*(?!\\*)").matcher(text).find()) {
            features.append("italic ");
        }
        if (Pattern.compile("`[^`]+`").matcher(text).find()) {
            features.append("code ");
        }
        if (Pattern.compile("\\[[^\\]]+\\]\\([^)]+\\)").matcher(text).find()) {
            features.append("links ");
        }
        if (Pattern.compile("^\\s*[-*+]\\s", Pattern.MULTILINE).matcher(text).find()) {
            features.append("lists ");
        }
        if (Pattern.compile("^\\s*>\\s", Pattern.MULTILINE).matcher(text).find()) {
            features.append("blockquote ");
        }
        if (features.isEmpty()) {
            return "plain";
        }
        return features.toString().trim();
    }
}