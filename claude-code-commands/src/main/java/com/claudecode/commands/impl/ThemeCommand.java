package com.claudecode.commands.impl;

import com.claudecode.commands.Command;
import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandResult;

import java.util.List;

public class ThemeCommand implements Command {

    private static final String[] THEMES = {
        "default", "monokai", "solarized-dark", "solarized-light",
        "dracula", "nord", "gruvbox", "one-dark"
    };

    @Override
    public String name() { return "theme"; }

    @Override
    public String description() { return "Switch UI theme"; }

    @Override
    public List<String> aliases() { return List.of("color", "colors"); }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        StringBuilder sb = new StringBuilder();
        String action = args != null && !args.isBlank() ? args.trim().toLowerCase() : "list";

        return switch (action) {
            case "list", "ls" -> listThemes(sb);
            case "set" -> showSetHelp(sb);
            default -> showHelp(sb);
        };
    }

    private CommandResult showHelp(StringBuilder sb) {
        sb.append("Terminal Theme\n");
        sb.append("=============\n\n");
        sb.append("Commands:\n");
        sb.append("  /theme list   - List available themes\n\n");
        sb.append("Themes are configured in settings.json:\n");
        sb.append("{\n");
        sb.append("  \"theme\": \"monokai\"\n");
        sb.append("}\n");
        return CommandResult.of(sb.toString());
    }

    private CommandResult listThemes(StringBuilder sb) {
        sb.append("Available Themes\n");
        sb.append("================\n\n");
        for (String theme : THEMES) {
            sb.append("  ").append(theme).append("\n");
        }
        sb.append("\nSet a theme in settings.json, then restart Claude Code.\n");
        return CommandResult.of(sb.toString());
    }

    private CommandResult showSetHelp(StringBuilder sb) {
        sb.append("Theme Selection\n");
        sb.append("===============\n\n");
        sb.append("Themes are set via the theme property in settings.json:\n\n");
        sb.append("{\n");
        sb.append("  \"theme\": \"monokai\"\n");
        sb.append("}\n\n");
        sb.append("Available themes:\n");
        for (String theme : THEMES) {
            sb.append("  - ").append(theme).append("\n");
        }
        return CommandResult.of(sb.toString());
    }
}