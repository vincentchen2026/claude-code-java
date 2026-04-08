package com.claudecode.ui;

import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Enhanced input reader with history search.
 */
public class EnhancedInputReaderV2 {

    private final Terminal terminal;
    private final LineReader reader;
    private final List<String> history = new ArrayList<>();

    private volatile InputMode mode = InputMode.NORMAL;

    public enum InputMode { NORMAL, MULTI_LINE, HISTORY_SEARCH }

    public EnhancedInputReaderV2(Terminal terminal, Path historyFile) {
        this.terminal = terminal;

        LineReaderBuilder builder = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(new DefaultParser());

        this.reader = builder.build();
        reader.setOpt(LineReader.Option.AUTO_FRESH_LINE);
        reader.setOpt(LineReader.Option.INSERT_TAB);
    }

    public String readLine(String prompt) {
        try {
            mode = InputMode.NORMAL;
            String line = reader.readLine(prompt);
            if (line != null && !line.isEmpty()) {
                history.add(line);
            }
            return line;
        } catch (UserInterruptException | EndOfFileException e) {
            return null;
        }
    }

    public String readMultiLine(String prompt, String continuation) {
        mode = InputMode.MULTI_LINE;
        StringBuilder sb = new StringBuilder();
        String cont = continuation != null ? continuation : ". ";
        String line = readLine(prompt);
        if (line == null) return null;
        sb.append(line);
        while (true) {
            line = reader.readLine(cont);
            if (line == null || line.isEmpty()) break;
            sb.append("\n").append(line);
        }
        String result = sb.toString();
        if (!result.isEmpty()) history.add(result);
        mode = InputMode.NORMAL;
        return result;
    }

    public List<String> searchHistory(String query) {
        if (query == null || query.isEmpty()) {
            return new ArrayList<>(history);
        }
        String lower = query.toLowerCase();
        return history.stream()
                .filter(h -> h.toLowerCase().contains(lower))
                .collect(Collectors.toList());
    }

    public void addToHistory(String line) {
        if (line != null && !line.isEmpty() && !history.contains(line)) {
            history.add(line);
        }
    }

    public List<String> getHistory() {
        return new ArrayList<>(history);
    }

    public InputMode getMode() {
        return mode;
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public PrintWriter writer() {
        return terminal.writer();
    }

    public void clearLine() {
        terminal.puts(InfoCmp.Capability.delete_line);
        writer().flush();
    }
}
