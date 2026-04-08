package com.claudecode.ui.mcp;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;

import java.io.PrintWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class McpCapabilityPanel {

    private final PrintWriter writer;
    private final String serverName;
    private final List<Capability> capabilities;
    private final ConcurrentLinkedQueue<ElicitationRequest> elicitations;
    private final ConcurrentLinkedQueue<Warning> warnings;
    private ConnectionState connectionState;
    private Instant lastConnected;

    public McpCapabilityPanel(PrintWriter writer, String serverName) {
        this.writer = writer;
        this.serverName = serverName;
        this.capabilities = new ArrayList<>();
        this.elicitations = new ConcurrentLinkedQueue<>();
        this.warnings = new ConcurrentLinkedQueue<>();
        this.connectionState = ConnectionState.DISCONNECTED;
        this.lastConnected = null;
    }

    public void render() {
        writer.println();
        writer.println(Ansi.styled("┌─ MCP Capabilities: " + serverName + " ──────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " " + renderConnectionState());
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));

        if (!capabilities.isEmpty()) {
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " " + Ansi.styled("Capabilities:", AnsiColor.WHITE));
            for (Capability cap : capabilities) {
                String icon = getCapabilityIcon(cap.type());
                writer.println(Ansi.styled("│", AnsiColor.CYAN) + "   " + icon + " " +
                    Ansi.colored(cap.name(), AnsiColor.GRAY) + " - " +
                    Ansi.styled(cap.description(), AnsiStyle.DIM));
            }
        }

        if (!elicitations.isEmpty()) {
            writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " " + Ansi.colored("⚠ Elicitation Requests:", AnsiColor.YELLOW));
            for (ElicitationRequest req : elicitations) {
                writer.println(Ansi.styled("│", AnsiColor.CYAN) + "   " +
                    Ansi.colored(req.message(), AnsiColor.WHITE));
                writer.println(Ansi.styled("│", AnsiColor.CYAN) + "     " +
                    Ansi.styled("[1] Allow  [2] Deny  [q] Dismiss", AnsiColor.DIM));
            }
        }

        if (!warnings.isEmpty()) {
            writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " " + Ansi.colored("⚠ Parsing Warnings:", AnsiColor.YELLOW));
            int count = 0;
            for (Warning w : warnings) {
                if (count++ >= 3) {
                    writer.println(Ansi.styled("│", AnsiColor.CYAN) + "   " +
                        Ansi.colored("... and " + (warnings.size() - 3) + " more", AnsiColor.DIM));
                    break;
                }
                writer.println(Ansi.styled("│", AnsiColor.CYAN) + "   " +
                    Ansi.colored("[" + w.timestamp() + "]", AnsiColor.DIM) + " " +
                    Ansi.colored(w.message(), AnsiColor.YELLOW));
            }
        }

        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        if (connectionState == ConnectionState.DISCONNECTED) {
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [c]onnect  [r]etry  [q]uit");
        } else if (connectionState == ConnectionState.CONNECTING) {
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " " + Ansi.colored("Connecting...", AnsiColor.YELLOW));
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [c]ancel");
        } else {
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [d]isconnect  [r]efresh  [q]uit");
        }
        writer.print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.print(" > ");
        writer.flush();
    }

    private String renderConnectionState() {
        return switch (connectionState) {
            case CONNECTED -> Ansi.colored("● Connected", AnsiColor.GREEN);
            case CONNECTING -> Ansi.colored("○ Connecting...", AnsiColor.YELLOW);
            case RECONNECTING -> Ansi.colored("◌ Reconnecting...", AnsiColor.YELLOW);
            case DISCONNECTED -> Ansi.colored("✕ Disconnected", AnsiColor.RED);
        };
    }

    private String getCapabilityIcon(CapabilityType type) {
        return switch (type) {
            case TOOLS -> Ansi.colored("🔧", AnsiColor.CYAN);
            case RESOURCES -> Ansi.colored("📄", AnsiColor.BLUE);
            case PROMPTS -> Ansi.colored("💬", AnsiColor.MAGENTA);
            case LOGGING -> Ansi.colored("📝", AnsiColor.GRAY);
        };
    }

    public void addCapability(Capability capability) {
        capabilities.add(capability);
    }

    public void addElicitation(ElicitationRequest request) {
        elicitations.offer(request);
    }

    public ElicitationRequest pollElicitation() {
        return elicitations.poll();
    }

    public void addWarning(String message) {
        warnings.offer(new Warning(Instant.now().toString(), message));
        if (warnings.size() > 10) {
            warnings.poll();
        }
    }

    public void setConnectionState(ConnectionState state) {
        this.connectionState = state;
        if (state == ConnectionState.CONNECTED) {
            this.lastConnected = Instant.now();
        }
    }

    public void clearWarnings() {
        warnings.clear();
    }

    public enum ConnectionState {
        CONNECTED, CONNECTING, RECONNECTING, DISCONNECTED
    }

    public enum CapabilityType {
        TOOLS, RESOURCES, PROMPTS, LOGGING
    }

    public record Capability(
        CapabilityType type,
        String name,
        String description
    ) {}

    public record ElicitationRequest(
        String id,
        String message,
        List<ElicitationOption> options
    ) {
        public record ElicitationOption(
            String label,
            String value
        ) {}
    }

    public record Warning(
        String timestamp,
        String message
    ) {}
}