package com.claudecode.ui;

import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Text input handler wrapping JLine3 LineReader.
 * Provides command history, tab completion for slash commands, and multi-line input.
 */
public class InputReader {

    private final LineReader lineReader;
    private final Terminal terminal;

    /**
     * Create an InputReader with the given terminal and slash commands for completion.
     *
     * @param terminal      the JLine terminal
     * @param historyFile   path to the history file (null to disable persistence)
     * @param slashCommands list of slash command names (without /) for tab completion
     */
    public InputReader(Terminal terminal, Path historyFile, Collection<String> slashCommands) {
        this.terminal = terminal;

        // Build completer for slash commands
        Completer completer = buildCompleter(slashCommands);

        // Configure parser for multi-line support
        DefaultParser parser = new DefaultParser();
        parser.setEofOnUnclosedBracket(DefaultParser.Bracket.CURLY,
                DefaultParser.Bracket.ROUND, DefaultParser.Bracket.SQUARE);

        LineReaderBuilder builder = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(completer)
                .parser(parser)
                .history(new DefaultHistory());

        if (historyFile != null) {
            builder.variable(LineReader.HISTORY_FILE, historyFile);
        }

        this.lineReader = builder.build();

        // Configure line reader options
        lineReader.setOpt(LineReader.Option.AUTO_FRESH_LINE);
    }

    /**
     * Read a single line of input with the given prompt.
     *
     * @param prompt the prompt string
     * @return the input line, or null if EOF
     */
    public String readLine(String prompt) {
        try {
            return lineReader.readLine(prompt);
        } catch (UserInterruptException e) {
            return null;
        } catch (EndOfFileException e) {
            return null;
        }
    }

    /**
     * Read multi-line input. Continues reading until an empty line is entered.
     *
     * @param prompt          the initial prompt
     * @param continuationPrompt the prompt for continuation lines
     * @return the complete multi-line input, or null if EOF
     */
    public String readMultiLine(String prompt, String continuationPrompt) {
        StringBuilder sb = new StringBuilder();
        String firstLine = readLine(prompt);
        if (firstLine == null) return null;
        sb.append(firstLine);

        while (true) {
            String line = readLine(continuationPrompt);
            if (line == null || line.isEmpty()) {
                break;
            }
            sb.append("\n").append(line);
        }
        return sb.toString();
    }

    /**
     * Get the underlying JLine LineReader.
     */
    public LineReader getLineReader() {
        return lineReader;
    }

    /**
     * Parse a command string into command name and arguments.
     *
     * @param input the raw input (e.g., "/help compact")
     * @return parsed command, or null if not a slash command
     */
    public static ParsedCommand parseCommand(String input) {
        if (input == null || !input.startsWith("/")) {
            return null;
        }
        String trimmed = input.trim();
        int spaceIdx = trimmed.indexOf(' ');
        if (spaceIdx < 0) {
            return new ParsedCommand(trimmed.substring(1), "");
        }
        String name = trimmed.substring(1, spaceIdx);
        String args = trimmed.substring(spaceIdx + 1).trim();
        return new ParsedCommand(name, args);
    }

    /**
     * A parsed slash command.
     */
    public record ParsedCommand(String name, String arguments) {}

    private Completer buildCompleter(Collection<String> slashCommands) {
        if (slashCommands == null || slashCommands.isEmpty()) {
            return new StringsCompleter();
        }
        List<String> completions = slashCommands.stream()
                .map(cmd -> "/" + cmd)
                .toList();
        return new AggregateCompleter(new StringsCompleter(completions));
    }
}
