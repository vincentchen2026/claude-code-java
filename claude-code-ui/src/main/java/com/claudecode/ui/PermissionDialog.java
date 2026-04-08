package com.claudecode.ui;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Permission dialog system for terminal-based permission requests.
 * Task 60: Permission dialogs
 *
 * Uses JLine3-style interactive prompts with keyboard selection:
 * [a]lways / [d]eny / [o]nce / [c]ancel
 */
public class PermissionDialog {

    private final PrintWriter writer;
    private final String promptPrefix;
    private BufferedReader reader;

    // Permission decision enum
    public enum Decision {
        ALWAYS_ALLOW,
        ALLOW_ONCE,
        DENY,
        CANCEL
    }

    // Permission context record
    public record PermissionContext(
        String toolName,
        String toolUseId,
        Map<String, Object> toolInput,
        String commandPreview,
        String securityLevel,
        String ruleSource
    ) {}

    public PermissionDialog(PrintWriter writer) {
        this(writer, "Permission");
    }

    public PermissionDialog(PrintWriter writer, String promptPrefix) {
        this.writer = writer;
        this.promptPrefix = promptPrefix;
    }

    /**
     * Set the reader for input. Must be called before show() if not using JLine.
     */
    public void setReader(BufferedReader reader) {
        this.reader = reader;
    }

    /**
     * Show a permission prompt and wait for user input.
     * Returns the user's decision.
     */
    public Decision show(PermissionContext ctx) {
        renderPermissionPrompt(ctx);
        return waitForDecision();
    }

    /**
     * Render the permission prompt to the terminal.
     */
    private void renderPermissionPrompt(PermissionContext ctx) {
        writer.println();
        writer.println(Ansi.styled("┌─ " + promptPrefix + " ─────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Tool: " + Ansi.colored(ctx.toolName(), AnsiColor.YELLOW));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " ID:   " + Ansi.styled(ctx.toolUseId(), AnsiStyle.DIM));

        if (ctx.commandPreview() != null && !ctx.commandPreview().isEmpty()) {
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Cmd:  " + Ansi.colored(truncate(ctx.commandPreview(), 60), AnsiColor.WHITE));
        }

        if (ctx.securityLevel() != null && !ctx.securityLevel().isEmpty()) {
            AnsiColor levelColor = switch (ctx.securityLevel().toLowerCase()) {
                case "safe" -> AnsiColor.GREEN;
                case "moderate" -> AnsiColor.YELLOW;
                case "dangerous" -> AnsiColor.RED;
                default -> AnsiColor.GRAY;
            };
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Risk: " + Ansi.colored(ctx.securityLevel(), levelColor));
        }

        if (ctx.ruleSource() != null && !ctx.ruleSource().isEmpty()) {
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Rule: " + Ansi.styled(ctx.ruleSource(), AnsiStyle.DIM));
        }

        if (ctx.toolInput() != null && !ctx.toolInput().isEmpty()) {
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Input:");
            for (Map.Entry<String, Object> entry : ctx.toolInput().entrySet()) {
                String value = String.valueOf(entry.getValue());
                writer.println(Ansi.styled("│", AnsiColor.CYAN) + "   " +
                    Ansi.styled(entry.getKey(), AnsiStyle.BOLD) + ": " +
                    Ansi.styled(truncate(value, 40), AnsiStyle.DIM));
            }
        }

        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [a]lways  [d]eny  [o]nce  [c]ancel");
        writer.print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.print(" > ");
        writer.flush();
    }

    /**
     * Wait for user decision from stdin.
     * Reads a single character and maps to Decision.
     */
    private Decision waitForDecision() {
        try {
            if (reader != null) {
                String line = reader.readLine();
                if (line != null && !line.isEmpty()) {
                    char c = Character.toLowerCase(line.charAt(0));
                    switch (c) {
                        case 'a': return Decision.ALWAYS_ALLOW;
                        case 'd': return Decision.DENY;
                        case 'o': return Decision.ALLOW_ONCE;
                        case 'c':
                        default: return Decision.CANCEL;
                    }
                }
            }
        } catch (Exception e) {
            writer.println("Error reading input: " + e.getMessage());
        }
        return Decision.CANCEL;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 3) + "...";
    }

    /**
     * Task 60.2: Bash permission prompt with command preview.
     */
    public Decision showBashPermission(String command, String toolUseId) {
        String securityLevel = classifyBashCommand(command);
        return show(new PermissionContext(
            "Bash", toolUseId, Map.of("command", command),
            command, securityLevel, "default"));
    }

    /**
     * Task 60.3: File permission prompt with path tree.
     */
    public Decision showFilePermission(String action, String filePath, String toolUseId) {
        return show(new PermissionContext(
            action, toolUseId, Map.of("file_path", filePath),
            filePath, "moderate", "default"));
    }

    /**
     * Task 60.8: Render permission rule explanation.
     */
    public void renderRuleExplanation(String ruleSource, String behavior) {
        writer.println();
        writer.println(Ansi.styled("ℹ Permission Rule:", AnsiColor.CYAN));
        writer.println(Ansi.styled("  Source: " + ruleSource, AnsiStyle.DIM));
        writer.println(Ansi.styled("  Behavior: " + behavior, AnsiStyle.DIM));
        writer.println();
        writer.flush();
    }

    /**
     * Task 60.10: Render worker badge / pending permissions indicator.
     */
    public void renderPendingPermissions(List<String> pendingTools) {
        if (pendingTools.isEmpty()) return;

        writer.print(Ansi.colored("⏳ ", AnsiColor.YELLOW));
        writer.print(Ansi.styled("Pending: ", AnsiStyle.DIM));
        writer.println(String.join(", ", pendingTools));
        writer.flush();
    }

    /**
     * Classify a bash command's security level.
     */
    private String classifyBashCommand(String command) {
        String cmd = command.trim().toLowerCase();

        // Dangerous commands
        if (cmd.contains("rm -rf") || cmd.contains("sudo") || cmd.contains("chmod 777")
                || cmd.contains("dd ") || cmd.contains("mkfs") || cmd.contains("> /dev/")) {
            return "dangerous";
        }

        // Moderate commands (write operations)
        if (cmd.startsWith("write") || cmd.startsWith("edit") || cmd.startsWith("mv ")
                || cmd.startsWith("cp ") || cmd.startsWith("mkdir ") || cmd.startsWith("touch ")) {
            return "moderate";
        }

        // Safe commands (read-only)
        if (cmd.startsWith("ls") || cmd.startsWith("cat ") || cmd.startsWith("grep ")
                || cmd.startsWith("find ") || cmd.startsWith("head ") || cmd.startsWith("tail ")
                || cmd.startsWith("wc ") || cmd.startsWith("pwd") || cmd.startsWith("echo ")) {
            return "safe";
        }

        return "moderate";
    }
}
