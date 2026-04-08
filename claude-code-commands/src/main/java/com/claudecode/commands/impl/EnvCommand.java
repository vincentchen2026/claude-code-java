package com.claudecode.commands.impl;

import com.claudecode.commands.Command;
import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandResult;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class EnvCommand implements Command {

    @Override
    public String name() { return "env"; }

    @Override
    public String description() { return "Show environment variables"; }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        StringBuilder sb = new StringBuilder();
        sb.append("Environment Variables\n");
        sb.append("====================\n\n");

        if (args == null || args.isBlank()) {
            return listAllEnvVars(sb);
        }

        args = args.trim().toLowerCase();

        if (args.equals("list") || args.equals("ls")) {
            return listAllEnvVars(sb);
        } else if (args.equals("path")) {
            return showPath(sb);
        } else if (args.startsWith("get ") || args.startsWith("show ")) {
            String varName = args.substring(args.indexOf(' ') + 1).trim();
            return getEnvVar(sb, varName);
        } else if (args.startsWith("set ")) {
            return CommandResult.of("Setting environment variables is not supported in this context.");
        } else if (args.equals("home")) {
            return getEnvVar(sb, "HOME");
        } else if (args.equals("user")) {
            return getEnvVar(sb, "USER");
        } else if (args.equals("shell")) {
            return getEnvVar(sb, "SHELL");
        } else if (args.equals("pwd") || args.equals("cwd")) {
            return CommandResult.of("CWD: " + context.workingDirectory());
        } else if (args.equals("java")) {
            return showJavaEnv(sb);
        } else {
            return listAllEnvVars(sb);
        }
    }

    private CommandResult listAllEnvVars(StringBuilder sb) {
        Map<String, String> sortedEnv = new TreeMap<>(System.getenv());
        
        sb.append("Total environment variables: ").append(sortedEnv.size()).append("\n\n");

        String[] importantVars = {
            "PATH", "HOME", "USER", "SHELL", "PWD",
            "JAVA_HOME", "MAVEN_HOME", "GRADLE_HOME",
            "ANTHROPIC_API_KEY", "OPENAI_API_KEY",
            "CLAUDE_API_KEY", "CLAUDE_BASE_URL",
            "LANG", "LC_ALL", "TERM", "TERM_PROGRAM"
        };

        sb.append("Important variables:\n");
        for (String var : importantVars) {
            String value = System.getenv(var);
            if (value != null) {
                if (var.contains("KEY") && !value.isEmpty()) {
                    sb.append("  ").append(var).append("=").append(maskSensitive(var, value)).append("\n");
                } else {
                    sb.append("  ").append(var).append("=").append(value).append("\n");
                }
            }
        }

        sb.append("\nAll variables (sorted):\n");
        for (Map.Entry<String, String> entry : sortedEnv.entrySet()) {
            String value = entry.getValue();
            if (entry.getKey().contains("KEY") || entry.getKey().contains("SECRET") || entry.getKey().contains("PASSWORD")) {
                value = maskSensitive(entry.getKey(), value);
            } else if (value.length() > 100) {
                value = value.substring(0, 100) + "...";
            }
            sb.append("  ").append(entry.getKey()).append("=").append(value).append("\n");
        }

        return CommandResult.of(sb.toString());
    }

    private CommandResult showPath(StringBuilder sb) {
        String path = System.getenv("PATH");
        if (path == null || path.isBlank()) {
            return CommandResult.of("PATH is not set.");
        }

        sb.append("PATH directories:\n\n");
        String[] paths = path.split(":");
        for (int i = 0; i < paths.length; i++) {
            sb.append(String.format("  %2d. %s\n", i + 1, paths[i]));
        }

        return CommandResult.of(sb.toString());
    }

    private CommandResult getEnvVar(StringBuilder sb, String varName) {
        String value = System.getenv(varName);
        if (value == null) {
            return CommandResult.of(varName + " is not set.");
        }

        if (varName.contains("KEY") || varName.contains("SECRET") || varName.contains("PASSWORD")) {
            value = maskSensitive(varName, value);
        }

        sb.append(varName).append("=").append(value);
        return CommandResult.of(sb.toString());
    }

    private CommandResult showJavaEnv(StringBuilder sb) {
        sb.append("Java Environment:\n\n");
        sb.append("  java.version: ").append(System.getProperty("java.version")).append("\n");
        sb.append("  java.vendor: ").append(System.getProperty("java.vendor")).append("\n");
        sb.append("  java.home: ").append(System.getProperty("java.home")).append("\n");
        sb.append("  java.runtime.name: ").append(System.getProperty("java.runtime.name")).append("\n");
        sb.append("  java.runtime.version: ").append(System.getProperty("java.runtime.version")).append("\n");
        sb.append("  java.vm.name: ").append(System.getProperty("java.vm.name")).append("\n");
        sb.append("  java.vm.version: ").append(System.getProperty("java.vm.version")).append("\n");
        sb.append("  os.name: ").append(System.getProperty("os.name")).append("\n");
        sb.append("  os.version: ").append(System.getProperty("os.version")).append("\n");
        sb.append("  os.arch: ").append(System.getProperty("os.arch")).append("\n");
        sb.append("  file.separator: ").append(System.getProperty("file.separator")).append("\n");
        sb.append("  path.separator: ").append(System.getProperty("path.separator")).append("\n");
        sb.append("  user.dir: ").append(System.getProperty("user.dir")).append("\n");
        sb.append("  user.name: ").append(System.getProperty("user.name")).append("\n");
        sb.append("  user.home: ").append(System.getProperty("user.home")).append("\n");

        return CommandResult.of(sb.toString());
    }

    private String maskSensitive(String varName, String value) {
        if (value == null || value.isEmpty()) {
            return "(not set)";
        }
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
