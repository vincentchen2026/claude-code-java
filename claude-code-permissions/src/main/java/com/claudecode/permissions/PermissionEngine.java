package com.claudecode.permissions;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Permission engine that evaluates tool permission requests against rules and mode.
 * <p>
 * Evaluation order:
 * <ol>
 *   <li>Check deny rules first (highest priority)</li>
 *   <li>Check allow rules</li>
 *   <li>Fall back to mode-based decision</li>
 * </ol>
 */
public class PermissionEngine {

    /** Set of tool names considered "write" tools for PLAN mode. */
    private static final List<String> WRITE_TOOLS = List.of(
        "Bash", "FileWrite", "FileEdit", "NotebookEdit"
    );

    /**
     * Evaluates whether a tool invocation should be allowed, denied, or requires user input.
     *
     * @param toolName the name of the tool being invoked
     * @param input    the tool input (may be used for pattern matching)
     * @param context  the current permission context (mode, rules, etc.)
     * @return the permission decision
     */
    public PermissionDecision evaluate(String toolName, JsonNode input, ToolPermissionContext context) {
        List<PermissionRule> rules = context.rules();

        // 1. Check deny rules first (highest priority)
        Optional<PermissionRule> denyMatch = findMatchingRule(toolName, input, rules, PermissionBehavior.DENY);
        if (denyMatch.isPresent()) {
            return PermissionDecision.DENY;
        }

        // 2. Check allow rules
        Optional<PermissionRule> allowMatch = findMatchingRule(toolName, input, rules, PermissionBehavior.ALLOW);
        if (allowMatch.isPresent()) {
            return PermissionDecision.ALLOW;
        }

        // 3. Check ask rules
        Optional<PermissionRule> askMatch = findMatchingRule(toolName, input, rules, PermissionBehavior.ASK);
        if (askMatch.isPresent()) {
            return PermissionDecision.ASK;
        }

        // 4. Fall back to mode-based decision
        return evaluateByMode(toolName, context.mode());
    }

    /**
     * Finds the first rule matching the given tool name, input, and behavior.
     */
    private Optional<PermissionRule> findMatchingRule(String toolName, JsonNode input,
                                                      List<PermissionRule> rules,
                                                      PermissionBehavior behavior) {
        return rules.stream()
            .filter(rule -> rule.behavior() == behavior)
            .filter(rule -> matchesToolName(rule.toolName(), toolName))
            .filter(rule -> matchesPattern(rule, input))
            .findFirst();
    }

    /**
     * Checks if a rule's tool name matches the given tool name.
     * Supports exact match and wildcard "*".
     */
    private boolean matchesToolName(String ruleToolName, String toolName) {
        if ("*".equals(ruleToolName)) {
            return true;
        }
        return ruleToolName.equalsIgnoreCase(toolName);
    }

    /**
     * Checks if a rule's optional pattern matches the tool input.
     * If no pattern is specified, the rule matches any input.
     */
    private boolean matchesPattern(PermissionRule rule, JsonNode input) {
        if (rule.pattern().isEmpty()) {
            return true;
        }
        String pattern = rule.pattern().get();
        // Convert glob pattern to regex for matching against input content
        String inputText = extractInputText(input);
        if (inputText == null) {
            return false;
        }
        String regex = globToRegex(pattern);
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(inputText).matches();
    }

    /**
     * Extracts a text representation from the tool input for pattern matching.
     * Looks for common fields like "command", "file_path", "path".
     */
    private String extractInputText(JsonNode input) {
        if (input == null) {
            return null;
        }
        // Try common input field names
        for (String field : List.of("command", "file_path", "path", "content")) {
            if (input.has(field) && input.get(field).isTextual()) {
                return input.get(field).asText();
            }
        }
        return input.toString();
    }

    /**
     * Converts a simple glob pattern to a regex.
     * Supports * (any chars) and ? (single char).
     */
    static String globToRegex(String glob) {
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*' -> regex.append(".*");
                case '?' -> regex.append(".");
                case '.' -> regex.append("\\.");
                case '\\' -> regex.append("\\\\");
                case '(' -> regex.append("\\(");
                case ')' -> regex.append("\\)");
                case '[' -> regex.append("\\[");
                case ']' -> regex.append("\\]");
                case '{' -> regex.append("\\{");
                case '}' -> regex.append("\\}");
                case '^' -> regex.append("\\^");
                case '$' -> regex.append("\\$");
                case '|' -> regex.append("\\|");
                case '+' -> regex.append("\\+");
                default -> regex.append(c);
            }
        }
        return regex.toString();
    }

    /**
     * Mode-based fallback decision when no rules match.
     */
    private PermissionDecision evaluateByMode(String toolName, PermissionMode mode) {
        return switch (mode) {
            case BYPASS_PERMISSIONS, AUTO, DONT_ASK -> PermissionDecision.ALLOW;
            case PLAN -> isWriteTool(toolName) ? PermissionDecision.DENY : PermissionDecision.ALLOW;
            case ACCEPT_EDITS -> isWriteTool(toolName) ? PermissionDecision.ALLOW : PermissionDecision.ASK;
            case DEFAULT -> PermissionDecision.ASK;
        };
    }

    /**
     * Checks if a tool is considered a "write" tool.
     */
    private boolean isWriteTool(String toolName) {
        return WRITE_TOOLS.stream().anyMatch(w -> w.equalsIgnoreCase(toolName));
    }
}
