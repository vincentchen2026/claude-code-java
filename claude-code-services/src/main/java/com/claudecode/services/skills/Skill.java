package com.claudecode.services.skills;

import java.nio.file.Path;
import java.util.List;

/**
 * Represents a loaded skill with its metadata and content.
 */
public record Skill(
    String name,
    String description,
    List<String> allowedTools,
    List<String> paths,
    String content,
    Path sourceFile,
    SkillSource source
) {

    public enum SkillSource {
        MANAGED,
        USER,
        PROJECT,
        BUNDLED,
        MCP
    }

    /**
     * Returns true if this skill is conditional (has path patterns).
     */
    public boolean isConditional() {
        return paths != null && !paths.isEmpty();
    }

    /**
     * Returns the slash command name for this skill (e.g., "/skill-name").
     */
    public String slashCommand() {
        return "/" + name.toLowerCase().replaceAll("\\s+", "-");
    }
}
