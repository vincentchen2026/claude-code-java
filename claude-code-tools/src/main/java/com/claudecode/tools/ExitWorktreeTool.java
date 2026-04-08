package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * ExitWorktreeTool — exit and clean up git worktree.
 * Input: {branch}. Runs git worktree remove.
 */
public class ExitWorktreeTool extends Tool<JsonNode, String> {

    private static final JsonNode SCHEMA = buildSchema();

    @Override
    public String name() { return "ExitWorktree"; }

    @Override
    public String description() { return "Exit git worktree mode"; }

    @Override
    public JsonNode inputSchema() { return SCHEMA; }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String branch = input.has("branch")
            ? input.get("branch").asText("") : "";

        if (branch.isBlank()) {
            return "Error: branch is required";
        }

        Path workDir = Path.of(context.workingDirectory());
        String worktreePath = workDir.resolve(".worktrees")
            .resolve(branch).toString();

        try {
            ProcessBuilder pb = new ProcessBuilder(
                "git", "worktree", "remove", worktreePath);
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            context.abortController().onAbort(() -> {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            });

            StringBuilder output = new StringBuilder();
            try (var reader = new BufferedReader(
                    new InputStreamReader(
                        process.getInputStream(),
                        StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }

            boolean completed = process.waitFor(
                30, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                return "Error: git worktree remove timed out";
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return "Error: git worktree remove failed: "
                    + output.toString().trim();
            }

            return "Exited and removed worktree for branch '"
                + branch + "'";
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return "Error: " + e.getMessage();
        }
    }

    private static JsonNode buildSchema() {
        ObjectNode schema = mapper().createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");

        ObjectNode branchProp = props.putObject("branch");
        branchProp.put("type", "string");
        branchProp.put("description",
            "The branch name of the worktree to remove");

        ArrayNode required = schema.putArray("required");
        required.add("branch");
        return schema;
    }
}
