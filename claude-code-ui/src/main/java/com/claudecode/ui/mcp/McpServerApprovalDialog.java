package com.claudecode.ui.mcp;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;
import org.jline.terminal.Terminal;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;

import java.util.List;

public class McpServerApprovalDialog {

    private final Terminal terminal;
    private final LineReader reader;

    public McpServerApprovalDialog(Terminal terminal) {
        this.terminal = terminal;
        this.reader = LineReaderBuilder.builder().terminal(terminal).build();
    }

    public ApprovalResult requestApproval(String serverName, String serverVersion, List<String> permissions) {
        terminal.writer().println();
        terminal.writer().println(Ansi.styled("┌─ MCP Server Approval ────────────────────────────────────", AnsiColor.CYAN));
        terminal.writer().println(Ansi.styled("│", AnsiColor.CYAN) + " Server: " + Ansi.styled(serverName, AnsiColor.WHITE, AnsiStyle.BOLD));
        terminal.writer().println(Ansi.styled("│", AnsiColor.CYAN) + " Version: " + Ansi.colored(serverVersion, AnsiColor.GRAY));
        terminal.writer().println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        
        if (permissions != null && !permissions.isEmpty()) {
            terminal.writer().println(Ansi.styled("│", AnsiColor.CYAN) + " Requested permissions:");
            for (String permission : permissions) {
                terminal.writer().println(Ansi.styled("│", AnsiColor.CYAN) + "   - " + Ansi.colored(permission, AnsiColor.YELLOW));
            }
        } else {
            terminal.writer().println(Ansi.styled("│", AnsiColor.CYAN) + " " + Ansi.colored("(no specific permissions requested)", AnsiColor.GRAY));
        }
        
        terminal.writer().println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        terminal.writer().println(Ansi.styled("│", AnsiColor.CYAN) + " [a]pprove  [r]eject  [l]imit  [c]ancel");
        terminal.writer().print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        terminal.writer().print(" > ");
        terminal.writer().flush();

        try {
            String line = reader.readLine().trim().toLowerCase();
            return switch (line) {
                case "a" -> new ApprovalResult(true, ApprovalType.APPROVED, null);
                case "r" -> new ApprovalResult(true, ApprovalType.REJECTED, null);
                case "l" -> new ApprovalResult(true, ApprovalType.LIMITED, null);
                case "c", "q" -> new ApprovalResult(false, ApprovalType.CANCELLED, null);
                default -> requestApproval(serverName, serverVersion, permissions);
            };
        } catch (Exception e) {
            return new ApprovalResult(false, ApprovalType.ERROR, e.getMessage());
        }
    }

    public ApprovalResult requestApprovalWithLimit(String serverName, List<String> allowedPermissions) {
        terminal.writer().println();
        terminal.writer().println(Ansi.styled("┌─ Limited Approval ───────────────────────────────────────", AnsiColor.CYAN));
        terminal.writer().println(Ansi.styled("│", AnsiColor.CYAN) + " Server: " + Ansi.colored(serverName, AnsiColor.WHITE));
        terminal.writer().println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        terminal.writer().println(Ansi.styled("│", AnsiColor.CYAN) + " Allowed permissions:");
        for (String permission : allowedPermissions) {
            terminal.writer().println(Ansi.styled("│", AnsiColor.CYAN) + "   + " + Ansi.colored(permission, AnsiColor.GREEN));
        }
        terminal.writer().println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        terminal.writer().println(Ansi.styled("│", AnsiColor.CYAN) + " [c]onfirm  [e]dit  [c]ancel");
        terminal.writer().print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        terminal.writer().print(" > ");
        terminal.writer().flush();

        try {
            String line = reader.readLine().trim().toLowerCase();
            return switch (line) {
                case "c" -> new ApprovalResult(true, ApprovalType.LIMITED, allowedPermissions);
                case "e" -> requestApproval(serverName, null, allowedPermissions);
                default -> new ApprovalResult(false, ApprovalType.CANCELLED, null);
            };
        } catch (Exception e) {
            return new ApprovalResult(false, ApprovalType.ERROR, e.getMessage());
        }
    }

    public record ApprovalResult(
        boolean responded,
        ApprovalType type,
        Object detail
    ) {}

    public enum ApprovalType {
        APPROVED,
        REJECTED,
        LIMITED,
        CANCELLED,
        ERROR
    }
}