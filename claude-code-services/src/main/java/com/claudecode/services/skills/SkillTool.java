package com.claudecode.services.skills;

import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.permissions.PermissionDecision;
import com.claudecode.permissions.ToolPermissionContext;
import com.claudecode.tools.Tool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tool that returns the content of a specific skill by name.
 * Extends Tool&lt;JsonNode, String&gt;.
 */
public class SkillTool extends Tool<JsonNode, String> {

    private final SkillLoader skillLoader;
    private final ShellVariableInjector variableInjector;

    public SkillTool(SkillLoader skillLoader, ShellVariableInjector variableInjector) {
        this.skillLoader = skillLoader;
        this.variableInjector = variableInjector;
    }

    @Override
    public String name() {
        return "Skill";
    }

    @Override
    public String description() {
        return "Retrieve the content of a named skill";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = createObjectSchema();
        ObjectNode props = (ObjectNode) schema.get("properties");
        ObjectNode nameProp = mapper().createObjectNode();
        nameProp.put("type", "string");
        nameProp.put("description", "The name of the skill to retrieve");
        props.set("name", nameProp);
        schema.set("required", mapper().createArrayNode().add("name"));
        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String skillName = input.has("name") ? input.get("name").asText() : null;
        if (skillName == null || skillName.isBlank()) {
            return "Error: skill name is required";
        }

        for (Skill skill : skillLoader.loadAll()) {
            if (skillName.equals(skill.name())) {
                String content = skill.content();
                if (variableInjector != null) {
                    content = variableInjector.inject(content);
                }
                return content;
            }
        }

        return "Skill not found: " + skillName;
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
