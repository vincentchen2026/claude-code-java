package com.claudecode.services.skills;

import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.permissions.PermissionDecision;
import com.claudecode.permissions.ToolPermissionContext;
import com.claudecode.tools.Tool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Tool that discovers and lists available skills.
 * Extends Tool&lt;JsonNode, String&gt;.
 */
public class DiscoverSkillsTool extends Tool<JsonNode, String> {

    private final SkillLoader skillLoader;

    public DiscoverSkillsTool(SkillLoader skillLoader) {
        this.skillLoader = skillLoader;
    }

    @Override
    public String name() {
        return "DiscoverSkills";
    }

    @Override
    public String description() {
        return "List all available skills with their names and descriptions";
    }

    @Override
    public JsonNode inputSchema() {
        return createObjectSchema();
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        List<Skill> skills = skillLoader.loadAll();

        if (skills.isEmpty()) {
            return "No skills available.";
        }

        return skills.stream()
                .map(s -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("- ").append(s.name());
                    if (s.description() != null) {
                        sb.append(": ").append(s.description());
                    }
                    if (s.isConditional()) {
                        sb.append(" [conditional: ").append(String.join(", ", s.paths())).append("]");
                    }
                    sb.append(" (").append(s.slashCommand()).append(")");
                    return sb.toString();
                })
                .collect(Collectors.joining("\n"));
    }

    @Override
    public PermissionDecision checkPermissions(JsonNode input, ToolPermissionContext permCtx) {
        return PermissionDecision.ALLOW;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }
}
