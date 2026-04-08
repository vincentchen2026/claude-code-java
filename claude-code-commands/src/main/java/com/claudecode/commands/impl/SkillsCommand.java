package com.claudecode.commands.impl;

import com.claudecode.commands.Command;
import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class SkillsCommand implements Command {

    private static final String SKILLS_DIR = ".claude/skills";

    @Override
    public String name() { return "skills"; }

    @Override
    public String description() { return "Manage custom skills"; }

    @Override
    public List<String> aliases() { return List.of("skill"); }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        StringBuilder sb = new StringBuilder();
        Path skillsDir = Path.of(context.workingDirectory(), SKILLS_DIR);

        String action = args != null && !args.isBlank() ? args.trim().toLowerCase() : "list";

        return switch (action) {
            case "list", "ls" -> listSkills(sb, skillsDir);
            case "add", "create" -> {
                if (args != null && args.contains(" ")) {
                    String skillName = args.substring(args.indexOf(' ') + 1).trim();
                    yield createSkill(sb, skillsDir, skillName);
                }
                yield CommandResult.of("Usage: /skills add <skill-name>\nExample: /skills add my-custom-skill");
            }
            case "remove", "delete", "rm" -> {
                if (args != null && args.contains(" ")) {
                    String skillName = args.substring(args.indexOf(' ') + 1).trim();
                    yield removeSkill(sb, skillsDir, skillName);
                }
                yield CommandResult.of("Usage: /skills remove <skill-name>");
            }
            case "show", "view" -> {
                if (args != null && args.contains(" ")) {
                    String skillName = args.substring(args.indexOf(' ') + 1).trim();
                    yield showSkill(sb, skillsDir, skillName);
                }
                yield CommandResult.of("Usage: /skills show <skill-name>");
            }
            case "enable" -> {
                if (args != null && args.contains(" ")) {
                    String skillName = args.substring(args.indexOf(' ') + 1).trim();
                    yield enableSkill(sb, skillsDir, skillName);
                }
                yield CommandResult.of("Usage: /skills enable <skill-name>");
            }
            case "disable" -> {
                if (args != null && args.contains(" ")) {
                    String skillName = args.substring(args.indexOf(' ') + 1).trim();
                    yield disableSkill(sb, skillsDir, skillName);
                }
                yield CommandResult.of("Usage: /skills disable <skill-name>");
            }
            default -> showHelp(sb);
        };
    }

    private CommandResult showHelp(StringBuilder sb) {
        sb.append("Custom Skills Management\n");
        sb.append("========================\n\n");
        sb.append("Skills are stored in: ").append(SKILLS_DIR).append("/\n\n");
        sb.append("Commands:\n");
        sb.append("  /skills list              - List all skills\n");
        sb.append("  /skills add <name>       - Create a new skill\n");
        sb.append("  /skills show <name>      - Show skill details\n");
        sb.append("  /skills remove <name>    - Delete a skill\n");
        sb.append("  /skills enable <name>    - Enable a skill\n");
        sb.append("  /skills disable <name>   - Disable a skill\n\n");
        sb.append("Skills are markdown files with instructions for Claude Code.");
        return CommandResult.of(sb.toString());
    }

    private CommandResult listSkills(StringBuilder sb, Path skillsDir) {
        sb.append("Custom Skills\n");
        sb.append("=============\n\n");

        if (!skillsDir.toFile().exists()) {
            sb.append("No skills directory found.\n");
            sb.append("Skills are stored in: ").append(skillsDir).append("\n");
            sb.append("\nCreate your first skill with: /skills add <name>");
            return CommandResult.of(sb.toString());
        }

        try {
            var skillFiles = skillsDir.toFile().listFiles((dir, name) -> 
                name.endsWith(".md") && !name.startsWith("."));

            if (skillFiles == null || skillFiles.length == 0) {
                sb.append("No skills found.\n");
                sb.append("\nCreate your first skill with: /skills add <name>");
                return CommandResult.of(sb.toString());
            }

            sb.append("Found ").append(skillFiles.length).append(" skill(s):\n\n");

            for (var file : skillFiles) {
                String name = file.getName().replace(".md", "");
                boolean enabled = !file.getName().startsWith(".");
                String status = enabled ? "[enabled]" : "[disabled]";
                String preview = getFilePreview(file);
                sb.append(status).append(" ").append(name).append("\n");
                sb.append("       ").append(preview).append("\n\n");
            }

            sb.append("Use /skills show <name> for details.");

        } catch (Exception e) {
            return CommandResult.of("Error listing skills: " + e.getMessage());
        }

        return CommandResult.of(sb.toString());
    }

    private CommandResult createSkill(StringBuilder sb, Path skillsDir, String name) {
        if (name == null || name.isBlank()) {
            return CommandResult.of("Skill name cannot be empty.");
        }

        String fileName = sanitizeFileName(name);
        Path skillFile = skillsDir.resolve(fileName + ".md");

        if (skillFile.toFile().exists()) {
            return CommandResult.of("Skill '" + name + "' already exists.");
        }

        try {
            if (!skillsDir.toFile().exists()) {
                Files.createDirectories(skillsDir);
            }

            String content = "# Skill: " + name + "\n\n" +
                "Describe this skill here...\n\n" +
                "## Instructions\n\n" +
                "- What this skill does\n" +
                "- When to use it\n" +
                "- Examples\n\n" +
                "## Notes\n\n" +
                "Additional notes and context...\n";

            Files.writeString(skillFile, content);
            sb.append("Created skill: ").append(name).append("\n");
            sb.append("File: ").append(skillFile);
        } catch (IOException e) {
            return CommandResult.of("Failed to create skill: " + e.getMessage());
        }

        return CommandResult.of(sb.toString());
    }

    private CommandResult removeSkill(StringBuilder sb, Path skillsDir, String name) {
        if (name == null || name.isBlank()) {
            return CommandResult.of("Skill name cannot be empty.");
        }

        String fileName = sanitizeFileName(name);
        Path skillFile = skillsDir.resolve(fileName + ".md");

        if (!skillFile.toFile().exists()) {
            return CommandResult.of("Skill '" + name + "' not found.");
        }

        try {
            Files.delete(skillFile);
            sb.append("Removed skill: ").append(name);
        } catch (IOException e) {
            return CommandResult.of("Failed to remove skill: " + e.getMessage());
        }

        return CommandResult.of(sb.toString());
    }

    private CommandResult showSkill(StringBuilder sb, Path skillsDir, String name) {
        if (name == null || name.isBlank()) {
            return CommandResult.of("Skill name cannot be empty.");
        }

        String fileName = sanitizeFileName(name);
        Path skillFile = skillsDir.resolve(fileName + ".md");

        if (!skillFile.toFile().exists()) {
            return CommandResult.of("Skill '" + name + "' not found.");
        }

        try {
            String content = Files.readString(skillFile);
            sb.append("Skill: ").append(name).append("\n");
            sb.append("File: ").append(skillFile).append("\n\n");
            sb.append(content);
        } catch (IOException e) {
            return CommandResult.of("Failed to read skill: " + e.getMessage());
        }

        return CommandResult.of(sb.toString());
    }

    private CommandResult enableSkill(StringBuilder sb, Path skillsDir, String name) {
        if (name == null || name.isBlank()) {
            return CommandResult.of("Skill name cannot be empty.");
        }

        String fileName = sanitizeFileName(name);
        Path disabledFile = skillsDir.resolve("." + fileName + ".md");
        Path enabledFile = skillsDir.resolve(fileName + ".md");

        if (enabledFile.toFile().exists()) {
            return CommandResult.of("Skill '" + name + "' is already enabled.");
        }

        if (!disabledFile.toFile().exists()) {
            return CommandResult.of("Skill '" + name + "' not found (in disabled form).");
        }

        try {
            Files.move(disabledFile, enabledFile);
            sb.append("Enabled skill: ").append(name);
        } catch (IOException e) {
            return CommandResult.of("Failed to enable skill: " + e.getMessage());
        }

        return CommandResult.of(sb.toString());
    }

    private CommandResult disableSkill(StringBuilder sb, Path skillsDir, String name) {
        if (name == null || name.isBlank()) {
            return CommandResult.of("Skill name cannot be empty.");
        }

        String fileName = sanitizeFileName(name);
        Path enabledFile = skillsDir.resolve(fileName + ".md");
        Path disabledFile = skillsDir.resolve("." + fileName + ".md");

        if (!enabledFile.toFile().exists()) {
            return CommandResult.of("Skill '" + name + "' not found or already disabled.");
        }

        try {
            Files.move(enabledFile, disabledFile);
            sb.append("Disabled skill: ").append(name);
        } catch (IOException e) {
            return CommandResult.of("Failed to disable skill: " + e.getMessage());
        }

        return CommandResult.of(sb.toString());
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9-_]", "-").toLowerCase();
    }

    private String getFilePreview(java.io.File file) {
        try {
            String content = Files.readString(file.toPath());
            String[] lines = content.split("\n");
            String firstLine = lines.length > 0 ? lines[0].replace("# Skill:", "").trim() : "";
            if (firstLine.length() > 60) {
                firstLine = firstLine.substring(0, 60) + "...";
            }
            return firstLine;
        } catch (Exception e) {
            return "(unable to read)";
        }
    }
}
