package com.claudecode.ui.renderer;

import java.util.regex.Pattern;

/**
 * Task 68.6: Prompt XML tag stripper.
 * Removes XML tags like <think>, <think>, and other prompt-related
 * XML tags from markdown content to prevent prompt injection.
 */
public class PromptXmlStripper {

    private static final Pattern XML_TAG_PATTERN = Pattern.compile(
        "<[^/>][^>]*>.*?</[^>]+>",
        Pattern.DOTALL
    );

    private static final Pattern SELF_CLOSING_XML_PATTERN = Pattern.compile("<[^>]+/>");

    private static final Pattern PROMPT_XML_TAGS = Pattern.compile(
        "<think>|</think>|<think>|</think>|<output>|</output>",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern HTML_COMMENTS_PATTERN = Pattern.compile(
        "<!--.*?-->",
        Pattern.DOTALL
    );

    /**
     * Strip all XML tags from the given content.
     *
     * @param content the content containing XML tags
     * @return content with all XML tags removed
     */
    public String stripAll(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        String result = XML_TAG_PATTERN.matcher(content).replaceAll("");
        result = SELF_CLOSING_XML_PATTERN.matcher(result).replaceAll("");
        return result;
    }

    /**
     * Strip only prompt-related XML tags (<think>, <think>, etc.).
     *
     * @param content the content containing prompt XML tags
     * @return content with prompt XML tags removed
     */
    public String stripPromptTags(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        return PROMPT_XML_TAGS.matcher(content).replaceAll("");
    }

    /**
     * Strip HTML comments from content.
     *
     * @param content the content containing HTML comments
     * @return content with HTML comments removed
     */
    public String stripHtmlComments(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        return HTML_COMMENTS_PATTERN.matcher(content).replaceAll("");
    }

    /**
     * Strip all prompt injection artifacts: XML tags and HTML comments.
     *
     * @param content the content to clean
     * @return cleaned content
     */
    public String stripInjectionArtifacts(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        String result = stripAll(content);
        result = stripHtmlComments(result);
        return result;
    }

    /**
     * Check if content contains any prompt XML tags.
     *
     * @param content the content to check
     * @return true if prompt XML tags are present
     */
    public boolean containsPromptTags(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        return PROMPT_XML_TAGS.matcher(content).find();
    }

    /**
     * Check if content contains any XML-like tags.
     *
     * @param content the content to check
     * @return true if XML-like tags are present
     */
    public boolean containsXmlTags(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        return XML_TAG_PATTERN.matcher(content).find() ||
               SELF_CLOSING_XML_PATTERN.matcher(content).find();
    }

    /**
     * Count the number of prompt XML tag pairs in content.
     *
     * @param content the content to analyze
     * @return count of prompt tag pairs
     */
    public int countPromptTagPairs(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        java.util.regex.Matcher matcher = PROMPT_XML_TAGS.matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count / 2;
    }
}