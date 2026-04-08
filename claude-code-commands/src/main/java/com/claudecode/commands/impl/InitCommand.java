package com.claudecode.commands.impl;

import com.claudecode.commands.Command;
import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class InitCommand implements Command {

    @Override
    public String name() { return "init"; }

    @Override
    public String description() { return "Initialize CLAUDE.md project configuration"; }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        StringBuilder sb = new StringBuilder();
        Path claudeMdPath = Path.of(context.workingDirectory(), "CLAUDE.md");

        if (args != null && args.contains("--force")) {
            return createClaueMd(sb, context, claudeMdPath, true);
        }

        if (Files.exists(claudeMdPath)) {
            sb.append("CLAUDE.md already exists at " + claudeMdPath + ".\n");
            sb.append("Use /init --force to overwrite.");
            return CommandResult.of(sb.toString());
        }

        return createClaueMd(sb, context, claudeMdPath, false);
    }

    private CommandResult createClaueMd(StringBuilder sb, CommandContext context, Path claudeMdPath, boolean force) {
        try {
            if (Files.exists(claudeMdPath) && !force) {
                sb.append("CLAUDE.md already exists.\n");
                sb.append("Use /init --force to overwrite.");
                return CommandResult.of(sb.toString());
            }

            StringBuilder content = new StringBuilder();
            content.append("# Project Name\n\n");
            content.append("Describe your project here.\n\n");
            content.append("## Overview\n\n");
            content.append("What does this project do? Who is it for?\n\n");
            content.append("## Coding Standards\n\n");
            content.append("- Language/Framework: \n");
            content.append("- Code style: \n");
            content.append("- Testing requirements: \n\n");
            content.append("## Project Structure\n\n");
            content.append("Describe the main directories and their purposes.\n\n");
            content.append("## Commands\n\n");
            content.append("Common development commands:\n");
            content.append("- Build: \n");
            content.append("- Test: \n");
            content.append("- Run: \n\n");
            content.append("## Notes\n\n");
            content.append("Additional notes for Claude Code.\n");

            Path readmePath = Path.of(context.workingDirectory(), "README.md");
            if (Files.exists(readmePath)) {
                try {
                    String readmeContent = Files.readString(readmePath);
                    String[] lines = readmeContent.split("\n");
                    if (lines.length > 0) {
                        String title = lines[0].replaceAll("^#+\\s*", "").trim();
                        if (!title.isEmpty()) {
                            content = new StringBuilder(content.toString().replace("Project Name", title));
                        }
                    }
                } catch (Exception e) {
                    // Use default content
                }
            }

            Path pomPath = Path.of(context.workingDirectory(), "pom.xml");
            Path buildGradle = Path.of(context.workingDirectory(), "build.gradle");
            Path packageJson = Path.of(context.workingDirectory(), "package.json");
            Path cargoToml = Path.of(context.workingDirectory(), "Cargo.toml");
            Path goMod = Path.of(context.workingDirectory(), "go.mod");

            String contentStr = content.toString();
            if (Files.exists(pomPath)) {
                contentStr = contentStr.replace("Language/Framework:", "Language/Framework: Java (Maven)");
                contentStr = contentStr.replace("Build:", "Build: mvn compile");
                contentStr = contentStr.replace("Test:", "Test: mvn test");
            } else if (Files.exists(buildGradle)) {
                contentStr = contentStr.replace("Language/Framework:", "Language/Framework: Java (Gradle)");
                contentStr = contentStr.replace("Build:", "Build: gradle build");
                contentStr = contentStr.replace("Test:", "Test: gradle test");
            } else if (Files.exists(packageJson)) {
                contentStr = contentStr.replace("Language/Framework:", "Language/Framework: JavaScript/TypeScript");
                contentStr = contentStr.replace("Build:", "Build: npm run build");
                contentStr = contentStr.replace("Test:", "Test: npm test");
            } else if (Files.exists(cargoToml)) {
                contentStr = contentStr.replace("Language/Framework:", "Language/Framework: Rust (Cargo)");
                contentStr = contentStr.replace("Build:", "Build: cargo build");
                contentStr = contentStr.replace("Test:", "Test: cargo test");
            } else if (Files.exists(goMod)) {
                contentStr = contentStr.replace("Language/Framework:", "Language/Framework: Go");
                contentStr = contentStr.replace("Build:", "Build: go build");
                contentStr = contentStr.replace("Test:", "Test: go test");
            }

            Files.writeString(claudeMdPath, contentStr);

            sb.append("Created CLAUDE.md in project root.\n");
            sb.append("File: ").append(claudeMdPath).append("\n\n");
            sb.append("Edit this file to add project-specific instructions for Claude Code.");

        } catch (IOException e) {
            return CommandResult.of("Error creating CLAUDE.md: " + e.getMessage());
        }

        return CommandResult.of(sb.toString());
    }
}
