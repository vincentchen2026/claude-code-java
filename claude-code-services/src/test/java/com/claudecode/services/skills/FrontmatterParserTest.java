package com.claudecode.services.skills;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FrontmatterParserTest {

    private FrontmatterParser parser;

    @BeforeEach
    void setUp() {
        parser = new FrontmatterParser();
    }

    @Test
    void parseWithFrontmatter() {
        String content = """
                ---
                name: test-skill
                description: A test skill
                ---
                This is the body content.
                """;

        FrontmatterParser.ParseResult result = parser.parse(content);

        assertEquals("test-skill", result.name());
        assertEquals("A test skill", result.description());
        assertEquals("This is the body content.", result.body());
    }

    @Test
    void parseWithListValues() {
        String content = """
                ---
                name: conditional-skill
                allowedTools:
                - Read
                - Write
                - Bash
                paths:
                - *.java
                - src/**/*.ts
                ---
                Body here.
                """;

        FrontmatterParser.ParseResult result = parser.parse(content);

        assertEquals("conditional-skill", result.name());
        assertEquals(3, result.allowedTools().size());
        assertTrue(result.allowedTools().contains("Read"));
        assertTrue(result.allowedTools().contains("Write"));
        assertEquals(2, result.paths().size());
    }

    @Test
    void parseWithoutFrontmatter() {
        String content = "Just plain content without frontmatter.";

        FrontmatterParser.ParseResult result = parser.parse(content);

        assertTrue(result.metadata().isEmpty());
        assertEquals("Just plain content without frontmatter.", result.body());
    }

    @Test
    void parseEmptyContent() {
        FrontmatterParser.ParseResult result = parser.parse("");
        assertTrue(result.metadata().isEmpty());
        assertEquals("", result.body());
    }

    @Test
    void parseNullContent() {
        FrontmatterParser.ParseResult result = parser.parse(null);
        assertTrue(result.metadata().isEmpty());
        assertEquals("", result.body());
    }

    @Test
    void parseWithComments() {
        String content = """
                ---
                name: skill-with-comments
                # This is a comment
                description: Has comments
                ---
                Body.
                """;

        FrontmatterParser.ParseResult result = parser.parse(content);
        assertEquals("skill-with-comments", result.name());
        assertEquals("Has comments", result.description());
    }
}
