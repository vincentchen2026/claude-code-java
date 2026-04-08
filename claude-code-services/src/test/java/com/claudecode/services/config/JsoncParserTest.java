package com.claudecode.services.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsoncParserTest {

    @Test
    void parsesPlainJson() {
        String json = """
            {"key": "value", "num": 42}""";
        assertEquals(json, JsoncParser.parse(json));
    }

    @Test
    void stripsSingleLineComments() {
        String jsonc = """
            {
              // this is a comment
              "key": "value"
            }""";
        String result = JsoncParser.parse(jsonc);
        assertFalse(result.contains("//"));
        assertTrue(result.contains("\"key\": \"value\""));
    }

    @Test
    void stripsMultiLineComments() {
        String jsonc = """
            {
              /* multi
                 line */
              "key": "value"
            }""";
        String result = JsoncParser.parse(jsonc);
        assertFalse(result.contains("/*"));
        assertFalse(result.contains("*/"));
        assertTrue(result.contains("\"key\": \"value\""));
    }

    @Test
    void preservesCommentsInsideStrings() {
        String jsonc = """
            {"url": "https://example.com // not a comment"}""";
        String result = JsoncParser.parse(jsonc);
        assertTrue(result.contains("// not a comment"));
    }

    @Test
    void preservesMultiLineCommentInsideStrings() {
        String jsonc = """
            {"note": "/* not a comment */"}""";
        String result = JsoncParser.parse(jsonc);
        assertTrue(result.contains("/* not a comment */"));
    }

    @Test
    void removesTrailingCommaBeforeBrace() {
        String jsonc = """
            {"a": 1, "b": 2,}""";
        String result = JsoncParser.parse(jsonc);
        assertTrue(result.contains("\"b\": 2}"));
    }

    @Test
    void removesTrailingCommaBeforeBracket() {
        String jsonc = """
            {"arr": [1, 2, 3,]}""";
        String result = JsoncParser.parse(jsonc);
        assertTrue(result.contains("[1, 2, 3]"));
    }

    @Test
    void handlesNullInput() {
        assertEquals("", JsoncParser.parse(null));
    }

    @Test
    void handlesBlankInput() {
        assertEquals("", JsoncParser.parse("   "));
    }

    @Test
    void handlesEscapedQuotesInStrings() {
        String jsonc = """
            {"msg": "say \\"hello\\""}""";
        String result = JsoncParser.parse(jsonc);
        assertTrue(result.contains("say \\\"hello\\\""));
    }

    @Test
    void combinedCommentsAndTrailingCommas() {
        String jsonc = """
            {
              // API settings
              "model": "sonnet", /* default */
              "maxTokens": 4096,
            }""";
        String result = JsoncParser.parse(jsonc);
        assertFalse(result.contains("//"));
        assertFalse(result.contains("/*"));
        // Should be valid JSON (no trailing comma)
        assertDoesNotThrow(() -> {
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(result);
        });
    }
}
