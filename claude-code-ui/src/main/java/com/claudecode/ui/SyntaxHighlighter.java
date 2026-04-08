package com.claudecode.ui;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple keyword-based syntax highlighter for common languages.
 * Highlights keywords, strings, comments, and numbers with ANSI colors.
 */
public final class SyntaxHighlighter {

    private SyntaxHighlighter() {}

    private static final Map<String, Set<String>> KEYWORDS = new HashMap<>();
    private static final Set<String> KNOWN_LANGUAGES = new HashSet<>();

    static {
        Set<String> javaKeywords = Set.of(
                "abstract", "assert", "boolean", "break", "byte", "case", "catch",
                "char", "class", "const", "continue", "default", "do", "double",
                "else", "enum", "extends", "final", "finally", "float", "for",
                "if", "implements", "import", "instanceof", "int", "interface",
                "long", "native", "new", "package", "private", "protected",
                "public", "return", "short", "static", "strictfp", "super",
                "switch", "synchronized", "this", "throw", "throws", "transient",
                "try", "var", "void", "volatile", "while", "yield", "record",
                "sealed", "permits", "null", "true", "false"
        );
        KEYWORDS.put("java", javaKeywords);

        Set<String> pythonKeywords = Set.of(
                "and", "as", "assert", "async", "await", "break", "class",
                "continue", "def", "del", "elif", "else", "except", "finally",
                "for", "from", "global", "if", "import", "in", "is", "lambda",
                "nonlocal", "not", "or", "pass", "raise", "return", "try",
                "while", "with", "yield", "None", "True", "False"
        );
        KEYWORDS.put("python", pythonKeywords);
        KEYWORDS.put("py", pythonKeywords);

        Set<String> jsKeywords = Set.of(
                "async", "await", "break", "case", "catch", "class", "const",
                "continue", "debugger", "default", "delete", "do", "else",
                "export", "extends", "finally", "for", "function", "if",
                "import", "in", "instanceof", "let", "new", "of", "return",
                "static", "super", "switch", "this", "throw", "try", "typeof",
                "var", "void", "while", "with", "yield", "null", "undefined",
                "true", "false"
        );
        KEYWORDS.put("javascript", jsKeywords);
        KEYWORDS.put("js", jsKeywords);
        KEYWORDS.put("typescript", jsKeywords);
        KEYWORDS.put("ts", jsKeywords);

        Set<String> bashKeywords = Set.of(
                "if", "then", "else", "elif", "fi", "for", "while", "do",
                "done", "case", "esac", "in", "function", "return", "exit",
                "local", "export", "readonly", "declare", "typeset", "unset",
                "shift", "source", "true", "false"
        );
        KEYWORDS.put("bash", bashKeywords);
        KEYWORDS.put("sh", bashKeywords);
        KEYWORDS.put("shell", bashKeywords);

        KNOWN_LANGUAGES.addAll(KEYWORDS.keySet());
    }

    // Patterns for token types
    private static final Pattern SINGLE_LINE_COMMENT = Pattern.compile("(//.*|#.*)$", Pattern.MULTILINE);
    private static final Pattern MULTI_LINE_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern STRING_DOUBLE = Pattern.compile("\"(?:[^\"\\\\]|\\\\.)*\"");
    private static final Pattern STRING_SINGLE = Pattern.compile("'(?:[^'\\\\]|\\\\.)*'");
    private static final Pattern STRING_BACKTICK = Pattern.compile("`(?:[^`\\\\]|\\\\.)*`");
    private static final Pattern NUMBER = Pattern.compile("\\b\\d+(\\.\\d+)?\\b");
    private static final Pattern WORD = Pattern.compile("\\b[a-zA-Z_]\\w*\\b");

    /**
     * Highlight source code with ANSI colors.
     *
     * @param code     the source code
     * @param language the language identifier (e.g., "java", "python", "js")
     * @return ANSI-colored string
     */
    public static String highlight(String code, String language) {
        if (code == null || code.isEmpty()) {
            return "";
        }
        if (language == null || !KNOWN_LANGUAGES.contains(language.toLowerCase())) {
            return code;
        }

        String lang = language.toLowerCase();
        Set<String> keywords = KEYWORDS.getOrDefault(lang, Set.of());

        // Build a list of colored regions (start, end, color)
        List<ColorRegion> regions = new ArrayList<>();

        // Comments (highest priority — added first, checked for overlap)
        addRegions(regions, MULTI_LINE_COMMENT, code, AnsiColor.GRAY);
        if (lang.equals("python") || lang.equals("py") || lang.equals("bash")
                || lang.equals("sh") || lang.equals("shell")) {
            addRegions(regions, Pattern.compile("#.*$", Pattern.MULTILINE), code, AnsiColor.GRAY);
        } else {
            addRegions(regions, SINGLE_LINE_COMMENT, code, AnsiColor.GRAY);
        }

        // Strings
        addRegions(regions, STRING_DOUBLE, code, AnsiColor.GREEN);
        addRegions(regions, STRING_SINGLE, code, AnsiColor.GREEN);
        if (lang.equals("javascript") || lang.equals("js") || lang.equals("typescript") || lang.equals("ts")) {
            addRegions(regions, STRING_BACKTICK, code, AnsiColor.GREEN);
        }

        // Numbers
        addRegions(regions, NUMBER, code, AnsiColor.YELLOW);

        // Sort by start position
        regions.sort(Comparator.comparingInt(r -> r.start));

        // Remove overlapping regions (first one wins)
        List<ColorRegion> filtered = new ArrayList<>();
        int lastEnd = 0;
        for (ColorRegion region : regions) {
            if (region.start >= lastEnd) {
                filtered.add(region);
                lastEnd = region.end;
            }
        }

        // Build output, coloring keywords in uncolored segments
        StringBuilder result = new StringBuilder();
        int pos = 0;
        for (ColorRegion region : filtered) {
            if (pos < region.start) {
                result.append(highlightKeywords(code.substring(pos, region.start), keywords));
            }
            result.append(Ansi.colored(code.substring(region.start, region.end), region.color));
            pos = region.end;
        }
        if (pos < code.length()) {
            result.append(highlightKeywords(code.substring(pos), keywords));
        }

        return result.toString();
    }

    /**
     * Returns true if the given language is supported for highlighting.
     */
    public static boolean isLanguageSupported(String language) {
        return language != null && KNOWN_LANGUAGES.contains(language.toLowerCase());
    }

    private static void addRegions(List<ColorRegion> regions, Pattern pattern, String code, AnsiColor color) {
        Matcher matcher = pattern.matcher(code);
        while (matcher.find()) {
            regions.add(new ColorRegion(matcher.start(), matcher.end(), color));
        }
    }

    private static String highlightKeywords(String text, Set<String> keywords) {
        if (keywords.isEmpty()) return text;
        Matcher matcher = WORD.matcher(text);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            sb.append(text, lastEnd, matcher.start());
            String word = matcher.group();
            if (keywords.contains(word)) {
                sb.append(Ansi.colored(word, AnsiColor.BLUE));
            } else {
                sb.append(word);
            }
            lastEnd = matcher.end();
        }
        sb.append(text, lastEnd, text.length());
        return sb.toString();
    }

    private record ColorRegion(int start, int end, AnsiColor color) {}
}
