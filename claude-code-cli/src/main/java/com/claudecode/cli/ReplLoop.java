package com.claudecode.cli;

import com.claudecode.commands.Command;
import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandFactory;
import com.claudecode.commands.CommandRegistry;
import com.claudecode.commands.CommandResult;
import com.claudecode.core.engine.CostCalculator;
import com.claudecode.core.engine.QueryEngine;
import com.claudecode.core.message.Message;
import com.claudecode.core.message.Usage;
import org.jline.reader.*;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jline.reader.History;
import org.jline.reader.impl.history.DefaultHistory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Supplier;

/**
 * REPL with JLine3 input (Tab completion for slash commands, history, ❯ prompt)
 * and a welcome banner matching the original Claude Code UI.
 */
public class ReplLoop {

    private static final Logger log = LoggerFactory.getLogger(ReplLoop.class);
    private static final String EXIT_COMMAND = "/exit";

    // ANSI codes
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";
    private static final String ORANGE = "\u001B[38;5;209m";
    private static final String GRAY = "\u001B[90m";
    private static final String CYAN = "\u001B[36m";

    private final QueryEngine engine;
    private final PrintWriter out;
    private final String model;
    private final String cwd;
    private final String originalCwd;
    private final List<String> slashCommands;
    private volatile boolean running = true;
    private final CommandRegistry commandRegistry;

    // Cumulative token usage tracking
    private long totalInputTokens = 0;
    private long totalOutputTokens = 0;
    private double totalCost = 0.0;

    // For testing — allows injecting a BufferedReader instead of JLine
    private final BufferedReader testReader;

    // JLine components for terminal-aware redraw
    private Terminal terminal;
    private LineReader lineReader;

    public ReplLoop(QueryEngine engine, PrintWriter out) {
        this(engine, out, null, "claude-sonnet-4-20250514", System.getProperty("user.dir"), defaultCommands());
    }

    public ReplLoop(QueryEngine engine, PrintWriter out, BufferedReader reader) {
        this(engine, out, reader, "claude-sonnet-4-20250514", System.getProperty("user.dir"), defaultCommands());
    }

    public ReplLoop(QueryEngine engine, PrintWriter out, BufferedReader reader,
                    String model, String cwd) {
        this(engine, out, reader, model, cwd, defaultCommands());
    }

    public ReplLoop(QueryEngine engine, PrintWriter out, BufferedReader reader,
                    String model, String cwd, List<String> slashCommands) {
        this.engine = engine;
        this.out = out;
        this.testReader = reader;
        this.model = model != null ? model : "unknown";
        this.originalCwd = cwd != null ? cwd : System.getProperty("user.dir");
        this.cwd = shortenPath(this.originalCwd);
        this.slashCommands = slashCommands;
        this.commandRegistry = CommandFactory.createDefault();
    }

    public int run() {
        Thread mainThread = Thread.currentThread();
        Thread shutdownHook = new Thread(() -> {
            running = false;
            engine.interrupt();
            mainThread.interrupt();
        });

        try {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        } catch (IllegalStateException e) { }

        printWelcomeBanner();
        printStatusLine();

        // Use JLine3 if no test reader injected
        if (testReader != null) {
            return runWithBufferedReader();
        }
        return runWithJLine();
    }

    private int runWithJLine() {
        History history = null;
        try {
            terminal = TerminalBuilder.builder()
                    .system(true)
                    .jansi(true)
                    .build();

            // Setup history file: ~/.claude/history
            Path historyDir = Path.of(System.getProperty("user.home"), ".claude");
            Path historyFile = historyDir.resolve("history");
            try {
                Files.createDirectories(historyDir);
                history = new DefaultHistory();
                // Load existing history from file if it exists
                if (Files.exists(historyFile)) {
                    history.read(historyFile, false);
                }
            } catch (IOException e) {
                log.warn("Failed to setup history, using in-memory history", e);
                history = new DefaultHistory();
            }

            // Build completer: when user types /, show all slash commands
            Completer completer = new SlashCommandCompleter(slashCommands);

            lineReader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .history(history)
                    .completer(completer)
                    .option(LineReader.Option.AUTO_LIST, true)
                    .option(LineReader.Option.LIST_PACKED, true)
                    .option(LineReader.Option.AUTO_MENU, true)
                    .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
                    .option(LineReader.Option.COMPLETE_IN_WORD, true)
                    .build();

            // Set auto-complete to show up to 50 items
            lineReader.setVariable(LineReader.LIST_MAX, 50);

            String prompt = ORANGE + "❯ " + RESET;

            while (running) {
                String line;
                try {
                    line = lineReader.readLine(prompt);
                } catch (UserInterruptException e) {
                    continue;
                } catch (EndOfFileException e) {
                    out.println();
                    break;
                }

                if (line == null) break;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                if (!handleInput(trimmed)) break;
            }

            // Save history to disk before closing
            if (history instanceof DefaultHistory dh) {
                try {
                    dh.append(historyFile, false);
                } catch (IOException e) {
                    log.warn("Failed to save history", e);
                }
            }
            terminal.close();
        } catch (IOException e) {
            log.warn("JLine terminal failed, falling back to basic input", e);
            return runWithBufferedReader();
        }
        cleanupShutdownHook();
        return 0;
    }

    private int runWithBufferedReader() {
        try {
            BufferedReader reader = testReader != null ? testReader
                    : new BufferedReader(new InputStreamReader(System.in));

            while (running) {
                out.print(ORANGE + "❯ " + RESET);
                out.flush();

                String line;
                try {
                    line = reader.readLine();
                } catch (IOException e) {
                    break;
                }

                if (line == null) {
                    out.println();
                    break;
                }

                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                if (!handleInput(trimmed)) break;
            }
        } finally {
            cleanupShutdownHook();
        }
        return 0;
    }

    private void cleanupShutdownHook() {
        // no-op — hooks are cleaned up in the calling method
    }

    public void stop() {
        running = false;
    }

    /**
     * Handles a single input line. Returns false if the REPL should exit.
     */
    private boolean handleInput(String input) {
        // Check for ? (alias for /help)
        if ("?".equals(input)) {
            printHelp();
            return true;
        }

        // Check for slash commands
        if (input.startsWith("/")) {
            return handleSlashCommand(input);
        }

        // Regular prompt — send to AI
        try {
            // Print separator line before the response
            out.println(DIM + "────────────────────" + RESET);
            ClaudeCodeCli.processPrompt(engine, input, out);
            // Update cumulative token counters from engine
            Usage usage = engine.getTotalUsage();
            if (usage != null) {
                totalInputTokens = usage.inputTokens();
                totalOutputTokens = usage.outputTokens();
                totalCost = CostCalculator.forModel(model).calculateCost(usage);
            }
        } catch (Exception e) {
            if (!running) {
                out.println("\n(interrupted)");
                return true;
            }
            out.println("Error: " + e.getMessage());
            log.debug("Error processing prompt", e);
        }
        out.println();
        printStatusLine();

        // Redraw prompt via JLine to sync cursor state after direct terminal writes
        if (lineReader != null) {
            try {
                // Print a newline and force JLine to redraw the prompt at column 0
                lineReader.printAbove("");
                lineReader.getTerminal().flush();
            } catch (Exception ignored) {}
        }
        return true;
    }

    /**
     * Handles a slash command. Returns false if the REPL should exit.
     */
    private boolean handleSlashCommand(String input) {
        String cmd = input.substring(1).trim().toLowerCase();
        String args = "";
        int spaceIdx = cmd.indexOf(' ');
        if (spaceIdx > 0) {
            args = cmd.substring(spaceIdx + 1).trim();
            cmd = cmd.substring(0, spaceIdx);
        }

        // Try CommandRegistry first for all commands
        CommandContext ctx = createCommandContext(engine.getMessages());
        CommandResult result = commandRegistry.dispatch("/" + cmd + (args.isEmpty() ? "" : " " + args), ctx);
        if (result != null && !result.output().contains("Unknown command")) {
            out.println(result.output());
            return !result.shouldExit();
        }

        switch (cmd) {
            case "exit", "quit" -> {
                out.println("Goodbye!");
                return false;
            }
            case "vim" -> out.println(DIM + "Vim mode toggled. (Use ESC for normal mode, i for insert)" + RESET);
            default -> out.println(DIM + "Unknown command: /" + cmd + ". Type /help or ? for available commands." + RESET);
        }
        return true;
    }

    private void printHelp() {
        out.println();
        out.println(BOLD + "Available commands:" + RESET);
        out.println();

        CommandContext ctx = createCommandContext();
        List<Command> available = commandRegistry.getAvailable(ctx);

        for (Command cmd : available) {
            String name = "/" + cmd.name();
            String aliases = cmd.aliases().isEmpty() ? "" : " (" + String.join(", ", cmd.aliases()) + ")";
            String desc = cmd.description();
            out.printf("  " + ORANGE + "%-18s" + RESET + " %s%s%n", name + aliases, desc, "");
        }
        out.println();
    }

    /**
     * Prints a status line showing model, permission mode, cumulative tokens, and help hint.
     */
    private void printStatusLine() {
        long cumulative = totalInputTokens + totalOutputTokens;
        out.println(DIM + "  " + model + " · default · "
            + ClaudeCodeCli.formatTokens(cumulative) + " tokens · /help for commands" + RESET);
        out.println();
    }

    private CommandContext createCommandContext() {
        return createCommandContext(List.of());
    }

    private CommandContext createCommandContext(List<Message> messages) {
        return new CommandContext(
            model,
            () -> messages,
            () -> {},
            m -> {},
            () -> Usage.EMPTY,
            u -> 0.0,
            originalCwd,
            false
        );
    }

    private void printWelcomeBanner() {
        String version = "0.1.0";
        int boxWidth = 64;
        String topLabel = " Claude Code Java v" + version + " ";
        int topPadLen = boxWidth - 2 - topLabel.length() - 1;

        out.println(ORANGE + "╭─" + DIM + topLabel + RESET + ORANGE + "─".repeat(Math.max(0, topPadLen)) + "╮" + RESET);
        printBoxLine("", boxWidth);
        printBoxLine("   " + BOLD + "Welcome back!" + RESET + "              " + BOLD + CYAN + "Tips for getting started" + RESET, boxWidth);
        printBoxLine("                                Run /init to create a", boxWidth);
        printBoxLine("   " + ORANGE + "   ███████   " + RESET + "              CLAUDE.md file with", boxWidth);
        printBoxLine("   " + ORANGE + "  ██▄███▄██  " + RESET + "              instructions for Claude", boxWidth);
        printBoxLine("   " + ORANGE + "   ███████   " + RESET, boxWidth);
        printBoxLine("                                " + BOLD + CYAN + "Recent activity" + RESET, boxWidth);
        printBoxLine("                                No recent activity", boxWidth);
        printBoxLine("", boxWidth);
        printBoxLine("   " + DIM + model + " · API Usage Billing" + RESET, boxWidth);
        printBoxLine("       " + DIM + cwd + RESET, boxWidth);
        printBoxLine("", boxWidth);
        out.println(ORANGE + "╰" + "─".repeat(boxWidth - 2) + "╯" + RESET);
        out.println();
    }

    private void printBoxLine(String content, int boxWidth) {
        String visible = content.replaceAll("\u001B\\[[;\\d]*m", "");
        int pad = boxWidth - 2 - visible.length();
        out.println(ORANGE + "│" + RESET + content + " ".repeat(Math.max(0, pad)) + ORANGE + "│" + RESET);
    }

    private static String shortenPath(String path) {
        String home = System.getProperty("user.home");
        if (path.startsWith(home)) {
            return "~" + path.substring(home.length());
        }
        return path;
    }

    private static List<String> defaultCommands() {
        return List.of(
            "help", "exit", "quit", "clear", "compact", "config", "model", "cost",
            "commit", "diff", "review", "resume", "share", "export",
            "memory", "doctor", "permissions", "status", "vim", "init",
            "add-dir", "agents", "branch", "btw", "copy", "env",
            "feedback", "files", "hooks", "keybindings", "login", "logout",
            "mcp", "onboarding", "plan", "plugin", "rename", "rewind",
            "session", "skills", "stats", "summary", "tag", "tasks",
            "theme", "upgrade", "update", "usage", "version", "context",
            "effort", "fast", "bridge", "voice", "brief", "buddy", "chrome",
            "dream", "fork", "peers", "workflows", "release-notes", "good-claude",
            "search", "team-create", "team-delete", "snip"
        );
    }

    /**
     * Custom completer that only activates when input starts with '/'.
     * Shows all matching slash commands as a dropdown list.
     */
    static class SlashCommandCompleter implements Completer {
        private final List<String> commands;

        SlashCommandCompleter(List<String> commands) {
            this.commands = commands;
        }

        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            String buffer = line.line();

            // Only complete if the line starts with /
            if (!buffer.startsWith("/")) return;

            // Get the word being completed (after the /)
            String word = line.word();
            String searchWord = word.startsWith("/") ? word.substring(1) : word;

            for (String cmd : commands) {
                // Match commands that start with the search word (case-insensitive)
                if (searchWord.isEmpty() || cmd.toLowerCase().startsWith(searchWord.toLowerCase())) {
                    String display = "/" + cmd;
                    String desc = getDescription(cmd);
                    candidates.add(new Candidate(display, display, null, desc, null, null, true));
                }
            }
        }

        private String getDescription(String cmd) {
            return switch (cmd) {
                case "help" -> "Show available commands";
                case "exit" -> "Exit the REPL";
                case "quit" -> "Exit the REPL";
                case "clear" -> "Clear conversation history";
                case "compact" -> "Compact conversation context";
                case "config" -> "Show configuration";
                case "model" -> "Show or change model";
                case "cost" -> "Show session cost";
                case "commit" -> "Create a git commit";
                case "diff" -> "Show git diff summary";
                case "review" -> "Code review";
                case "resume" -> "Resume a previous session";
                case "share" -> "Share session";
                case "export" -> "Export conversation";
                case "memory" -> "Manage memory";
                case "doctor" -> "Run diagnostics";
                case "permissions" -> "Manage permissions";
                case "status" -> "Show session status";
                case "vim" -> "Toggle vim mode";
                case "init" -> "Initialize CLAUDE.md";
                case "add-dir" -> "Add a working directory";
                case "agents" -> "Manage agent configurations";
                case "branch" -> "Show or switch git branch";
                case "btw" -> "Quick side note";
                case "color" -> "Set prompt color";
                case "copy" -> "Copy last response";
                case "env" -> "Show environment variables";
                case "feedback" -> "Send feedback";
                case "files" -> "List tracked files";
                case "hooks" -> "Manage lifecycle hooks";
                case "keybindings" -> "Show keybindings";
                case "login" -> "Authenticate";
                case "logout" -> "Clear credentials";
                case "mcp" -> "Manage MCP servers";
                case "onboarding" -> "New user guide";
                case "plan" -> "Enter plan mode";
                case "plugin" -> "Manage plugins";
                case "rename" -> "Rename session";
                case "rewind" -> "Rewind conversation";
                case "session" -> "Show session info";
                case "skills" -> "Manage skills";
                case "stats" -> "Session statistics";
                case "summary" -> "Session summary";
                case "tag" -> "Tag session";
                case "tasks" -> "Background tasks";
                case "theme" -> "Switch theme";
                case "upgrade", "update" -> "Check for updates";
                case "usage" -> "Token usage info";
                case "version" -> "Show version";
                case "context" -> "Context window info";
                case "effort" -> "Set effort level";
                case "fast" -> "Toggle fast mode";
                case "bridge" -> "Bridge mode";
                case "voice" -> "Voice mode";
                case "brief" -> "Toggle brief mode";
                case "buddy" -> "Companion mode";
                case "chrome" -> "Chrome integration";
                case "dream" -> "Dream consolidation";
                case "fork" -> "Fork sub-agent";
                case "peers" -> "Peer sessions";
                case "workflows" -> "Manage workflows";
                case "release-notes" -> "Release notes";
                case "good-claude" -> "🎉";
                case "search" -> "Search conversation history";
                case "team-create" -> "Create a team";
                case "team-delete" -> "Delete a team";
                case "snip" -> "Snip conversation history";
                default -> "";
            };
        }
    }
}
