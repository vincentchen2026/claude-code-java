package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Optional;

/**
 * TeamDeleteTool — deletes a team/multi-agent group.
 * Task 50.7
 */
public class TeamDeleteTool extends Tool<JsonNode, String> {

    @Override
    public String name() { return "TeamDelete"; }

    @Override
    public String description() {
        return "Delete a team of agents and clean up resources.";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = createObjectSchema();
        ObjectNode props = (ObjectNode) schema.get("properties");

        ObjectNode teamIdSchema = mapper().createObjectNode();
        teamIdSchema.put("type", "string");
        teamIdSchema.put("description", "Team ID to delete");
        props.set("team_id", teamIdSchema);

        ObjectNode forceSchema = mapper().createObjectNode();
        forceSchema.put("type", "boolean");
        forceSchema.put("description", "Force delete even if team has active members");
        props.set("force", forceSchema);

        schema.set("required", mapper().createArrayNode().add("team_id"));
        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String teamId = input.has("team_id") ? input.get("team_id").asText() : null;
        boolean force = input.has("force") && input.get("force").asBoolean(false);

        if (teamId == null || teamId.isBlank()) {
            return "Error: team_id is required.";
        }

        Optional<TeamCreateTool.TeamInfo> teamOpt = TeamCreateTool.getTeam(teamId);
        if (teamOpt.isEmpty()) {
            return "Error: team '" + teamId + "' not found.";
        }

        TeamCreateTool.TeamInfo team = teamOpt.get();

        boolean hasActiveMembers = team.members().stream()
            .anyMatch(m -> "active".equals(m.status()) || "busy".equals(m.status()));

        if (hasActiveMembers && !force) {
            return "Error: Team has active members. Use force=true to delete anyway.\n" +
                   "Use /team " + teamId + " stop to stop active members first.";
        }

        TeamCreateTool.TeamInfo removed = TeamCreateTool.removeTeam(teamId);

        StringBuilder sb = new StringBuilder();
        sb.append("Team Deleted Successfully\n");
        sb.append("========================\n\n");
        sb.append("Team ID: ").append(teamId).append("\n");
        sb.append("Team Name: ").append(team.teamName()).append("\n");
        sb.append("Members released: ").append(team.members().size()).append("\n\n");
        sb.append("All agent instances have been terminated and resources cleaned up.\n");

        return sb.toString();
    }

    @Override
    public boolean isReadOnly() { return false; }
}
