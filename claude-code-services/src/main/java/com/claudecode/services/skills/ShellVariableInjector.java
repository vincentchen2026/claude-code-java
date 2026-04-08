package com.claudecode.services.skills;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Injects shell variables into skill content.
 * Supported variables: ${CLAUDE_SKILL_DIR}, ${CLAUDE_SESSION_ID}.
 */
public class ShellVariableInjector {

    private final Map<String, String> variables;

    public ShellVariableInjector() {
        this.variables = new LinkedHashMap<>();
    }

    /**
     * Set the skill directory variable.
     */
    public void setSkillDir(Path skillDir) {
        variables.put("CLAUDE_SKILL_DIR", skillDir.toAbsolutePath().toString());
    }

    /**
     * Set the session ID variable.
     */
    public void setSessionId(String sessionId) {
        variables.put("CLAUDE_SESSION_ID", sessionId != null ? sessionId : "");
    }

    /**
     * Add a custom variable.
     */
    public void setVariable(String name, String value) {
        variables.put(name, value != null ? value : "");
    }

    /**
     * Inject all configured variables into the given content.
     * Replaces ${VAR_NAME} patterns with their values.
     *
     * @param content the content to process
     * @return content with variables replaced
     */
    public String inject(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        String result = content;
        for (var entry : variables.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    /**
     * Returns an unmodifiable view of the current variables.
     */
    public Map<String, String> getVariables() {
        return Map.copyOf(variables);
    }
}
