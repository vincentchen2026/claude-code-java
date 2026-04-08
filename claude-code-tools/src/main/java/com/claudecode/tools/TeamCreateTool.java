package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TeamCreateTool — creates a team/multi-agent group.
 * Task 50.6
 */
public class TeamCreateTool extends Tool<JsonNode, String> {

    private static final Map<String, TeamInfo> TEAMS = new ConcurrentHashMap<>();
    private static int TEAM_COUNTER = 0;

    @Override
    public String name() { return "TeamCreate"; }

    @Override
    public String description() {
        return "Create a team of agents for collaborative task execution.";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = createObjectSchema();
        ObjectNode props = (ObjectNode) schema.get("properties");

        ObjectNode nameSchema = mapper().createObjectNode();
        nameSchema.put("type", "string");
        nameSchema.put("description", "Name of the team to create");
        props.set("team_name", nameSchema);

        ObjectNode membersSchema = mapper().createObjectNode();
        membersSchema.put("type", "integer");
        membersSchema.put("minimum", 1);
        membersSchema.put("maximum", 10);
        membersSchema.put("description", "Number of team members (default: 2)");
        props.set("member_count", membersSchema);

        ObjectNode modelSchema = mapper().createObjectNode();
        modelSchema.put("type", "string");
        modelSchema.put("description", "Model to use for team agents (optional)");
        props.set("model", modelSchema);

        schema.set("required", mapper().createArrayNode().add("team_name"));
        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String teamName = input.has("team_name") ? input.get("team_name").asText() : null;
        int memberCount = input.has("member_count") ? input.get("member_count").asInt(2) : 2;
        String model = input.has("model") ? input.get("model").asText() : "claude-sonnet-4-20250514";

        if (teamName == null || teamName.isBlank()) {
            return "Error: team_name is required.";
        }

        if (memberCount < 1 || memberCount > 10) {
            return "Error: member_count must be between 1 and 10.";
        }

        String teamId = "team_" + (++TEAM_COUNTER);
        List<TeamMember> members = new ArrayList<>();

        for (int i = 0; i < memberCount; i++) {
            members.add(new TeamMember(
                "agent_" + (i + 1),
                "Member " + (i + 1),
                "idle",
                model
            ));
        }

        TeamInfo team = new TeamInfo(
            teamId,
            teamName,
            members,
            Instant.now().toString(),
            "active",
            context.sessionId()
        );

        TEAMS.put(teamId, team);

        StringBuilder sb = new StringBuilder();
        sb.append("Team Created Successfully\n");
        sb.append("========================\n\n");
        sb.append("Team ID: ").append(teamId).append("\n");
        sb.append("Team Name: ").append(teamName).append("\n");
        sb.append("Members: ").append(memberCount).append("\n");
        sb.append("Model: ").append(model).append("\n");
        sb.append("Status: active\n\n");
        sb.append("Team members:\n");
        for (TeamMember member : members) {
            sb.append("  - ").append(member.agentId())
              .append(" (").append(member.role()).append(") [").append(member.status()).append("]\n");
        }
        sb.append("\nUse /team ").append(teamId).append(" to interact with this team.\n");

        return sb.toString();
    }

    /**
     * Get team info by ID.
     */
    public static Optional<TeamInfo> getTeam(String teamId) {
        return Optional.ofNullable(TEAMS.get(teamId));
    }

    /**
     * Get all teams.
     */
    public static Map<String, TeamInfo> getAllTeams() {
        return Map.copyOf(TEAMS);
    }

    /**
     * Remove a team.
     */
    public static TeamInfo removeTeam(String teamId) {
        return TEAMS.remove(teamId);
    }

    public record TeamInfo(
        String teamId,
        String teamName,
        List<TeamMember> members,
        String createdAt,
        String status,
        String sessionId
    ) {}

    public record TeamMember(
        String agentId,
        String role,
        String status,
        String model
    ) {}
}
