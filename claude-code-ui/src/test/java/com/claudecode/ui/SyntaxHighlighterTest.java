package com.claudecode.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for syntax highlighting.
 */
class SyntaxHighlighterTest {

    @Test
    void highlightNullReturnsEmpty() {
        assertEquals("", SyntaxHighlighter.highlight(null, "java"));
    }

    @Test
    void highlightEmptyReturnsEmpty() {
        assertEquals("", SyntaxHighlighter.highlight("", "java"));
    }

    @Test
    void highlightUnknownLanguageReturnsPlainText() {
        String code = "some code here";
        assertEquals(code, SyntaxHighlighter.highlight(code, "unknown"));
    }

    @Test
    void highlightNullLanguageReturnsPlainText() {
        String code = "some code here";
        assertEquals(code, SyntaxHighlighter.highlight(code, null));
    }

    @Test
    void highlightJavaKeywords() {
        String result = SyntaxHighlighter.highlight("public class Foo", "java");
        assertTrue(result.contains("public"), "Should contain keyword");
        assertTrue(result.contains("class"), "Should contain keyword");
        assertTrue(result.contains("Foo"), "Should contain identifier");
    }

    @Test
    void highlightJavaString() {
        String result = SyntaxHighlighter.highlight("String s = \"hello\";", "java");
        assertTrue(result.contains("hello"), "Should contain string content");
    }

    @Test
    void highlightPythonKeywords() {
        String result = SyntaxHighlighter.highlight("def foo():\n    return True", "python");
        assertTrue(result.contains("def"));
        assertTrue(result.contains("return"));
        assertTrue(result.contains("True"));
    }

    @Test
    void highlightJavaScriptKeywords() {
        String result = SyntaxHighlighter.highlight("const x = function() {}", "js");
        assertTrue(result.contains("const"));
        assertTrue(result.contains("function"));
    }

    @Test
    void highlightBashKeywords() {
        String result = SyntaxHighlighter.highlight("if [ -f file ]; then\n  echo ok\nfi", "bash");
        assertTrue(result.contains("if"));
        assertTrue(result.contains("then"));
        assertTrue(result.contains("fi"));
    }

    @Test
    void isLanguageSupportedForKnownLanguages() {
        assertTrue(SyntaxHighlighter.isLanguageSupported("java"));
        assertTrue(SyntaxHighlighter.isLanguageSupported("python"));
        assertTrue(SyntaxHighlighter.isLanguageSupported("js"));
        assertTrue(SyntaxHighlighter.isLanguageSupported("typescript"));
        assertTrue(SyntaxHighlighter.isLanguageSupported("bash"));
    }

    @Test
    void isLanguageSupportedForUnknownLanguages() {
        assertFalse(SyntaxHighlighter.isLanguageSupported("cobol"));
        assertFalse(SyntaxHighlighter.isLanguageSupported(null));
    }

    @Test
    void highlightPreservesLineStructure() {
        String code = "line1\nline2\nline3";
        String result = SyntaxHighlighter.highlight(code, "java");
        // Should still have 3 lines
        String[] lines = result.split("\n", -1);
        assertEquals(3, lines.length);
    }
}
