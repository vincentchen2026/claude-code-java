package com.claudecode.ui.agent;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import org.jline.terminal.Terminal;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;

import java.util.Arrays;
import java.util.List;

public class AgentConfigMenu {

    private final Terminal terminal;
    private final LineReader reader;
    private final List<String> availableModels;
    private final List<String> availableColors;

    public AgentConfigMenu(Terminal terminal) {
        this.terminal = terminal;
        this.reader = LineReaderBuilder.builder().terminal(terminal).build();
        this.availableModels = Arrays.asList(
            "claude-sonnet-4-20250514",
            "claude-opus-4-20250514",
            "claude-3-5-sonnet-20241022",
            "claude-3-5-haiku-20241022"
        );
        this.availableColors = Arrays.asList(
            "CYAN", "GREEN", "YELLOW", "MAGENTA", "BLUE", "WHITE"
        );
    }

    public AgentConfigMenu(Terminal terminal, List<String> models, List<String> colors) {
        this.terminal = terminal;
        this.reader = LineReaderBuilder.builder().terminal(terminal).build();
        this.availableModels = models;
        this.availableColors = colors;
    }

    public AgentConfig selectModel() {
        terminal.writer().println();
        terminal.writer().println(Ansi.styled("┌─ Select Model ─────────────────────────────────────────", AnsiColor.CYAN));
        for (int i = 0; i < availableModels.size(); i++) {
            String model = availableModels.get(i);
            terminal.writer().println(Ansi.styled("│", AnsiColor.CYAN) + " [" + (i + 1) + "] " + Ansi.colored(model, AnsiColor.WHITE));
        }
        terminal.writer().println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        terminal.writer().print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        terminal.writer().print(" > ");
        terminal.writer().flush();

        try {
            String line = reader.readLine();
            int choice = Integer.parseInt(line.trim()) - 1;
            if (choice >= 0 && choice < availableModels.size()) {
                return new AgentConfig(availableModels.get(choice), null, null);
            }
        } catch (Exception e) {
            // fall through
        }
        return new AgentConfig(availableModels.get(0), null, null);
    }

    public AgentConfig selectColor() {
        terminal.writer().println();
        terminal.writer().println(Ansi.styled("┌─ Select Color ─────────────────────────────────────────", AnsiColor.CYAN));
        for (int i = 0; i < availableColors.size(); i++) {
            String color = availableColors.get(i);
            AnsiColor ansiColor = AnsiColor.valueOf(color);
            terminal.writer().println(Ansi.styled("│", AnsiColor.CYAN) + " [" + (i + 1) + "] " + Ansi.styled(color, ansiColor));
        }
        terminal.writer().println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        terminal.writer().print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        terminal.writer().print(" > ");
        terminal.writer().flush();

        try {
            String line = reader.readLine();
            int choice = Integer.parseInt(line.trim()) - 1;
            if (choice >= 0 && choice < availableColors.size()) {
                return new AgentConfig(null, availableColors.get(choice), null);
            }
        } catch (Exception e) {
            // fall through
        }
        return new AgentConfig(null, availableColors.get(0), null);
    }

    public AgentConfig selectTools() {
        terminal.writer().println();
        terminal.writer().println(Ansi.styled("┌─ Select Tools ──────────────────────────────────────────", AnsiColor.CYAN));
        terminal.writer().println(Ansi.styled("│", AnsiColor.CYAN) + " [a]ll tools  [c]ustom  [n]one");
        terminal.writer().println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        terminal.writer().print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        terminal.writer().print(" > ");
        terminal.writer().flush();

        try {
            String line = reader.readLine().trim().toLowerCase();
            return switch (line) {
                case "a" -> new AgentConfig(null, null, "all");
                case "c" -> new AgentConfig(null, null, "custom");
                case "n" -> new AgentConfig(null, null, "none");
                default -> new AgentConfig(null, null, "all");
            };
        } catch (Exception e) {
            return new AgentConfig(null, null, "all");
        }
    }

    public AgentConfig selectAll() {
        AgentConfig config = new AgentConfig(null, null, null);

        terminal.writer().println();
        terminal.writer().println(Ansi.styled("┌─ Agent Configuration ────────────────────────────────────", AnsiColor.CYAN));
        terminal.writer().println(Ansi.styled("│", AnsiColor.CYAN) + " Configure your agent settings");
        terminal.writer().println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));

        String model = selectModel().model();
        String color = selectColor().color();
        String tools = selectTools().tools();

        return new AgentConfig(model, color, tools);
    }

    public record AgentConfig(
        String model,
        String color,
        String tools
    ) {}
}