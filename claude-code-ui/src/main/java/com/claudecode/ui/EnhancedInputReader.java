package com.claudecode.ui;

import org.jline.terminal.Terminal;
import org.jline.reader.*;
import org.jline.utils.InfoCmp;

import java.util.ArrayList;
import java.util.List;

public class EnhancedInputReader {

    private final Terminal terminal;
    private final LineReader reader;
    private final List<String> history;
    private String modeIndicator;
    private String footer;
    private boolean notificationPending;
    private String lastNotification;

    public EnhancedInputReader(Terminal terminal) {
        this.terminal = terminal;
        this.reader = LineReaderBuilder.builder()
            .terminal(terminal)
            .build();
        this.history = new ArrayList<>();
        this.modeIndicator = "";
        this.footer = "";
        this.notificationPending = false;
    }

    public String readLine(String prompt) {
        String fullPrompt = buildPrompt(prompt);
        try {
            String line = reader.readLine(fullPrompt);
            if (line != null && !line.isEmpty()) {
                history.add(line);
            }
            return line;
        } catch (UserInterruptException e) {
            return null;
        }
    }

    public String readLine() {
        return readLine("> ");
    }

    public void addToHistory(String line) {
        if (line != null && !line.isEmpty()) {
            history.add(line);
        }
    }

    public List<String> getHistory() {
        return new ArrayList<>(history);
    }

    public void searchHistory(String prefix) {
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).startsWith(prefix)) {
                return;
            }
        }
    }

    public void setModeIndicator(String mode) {
        this.modeIndicator = mode;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    public void showNotification(String message) {
        this.lastNotification = message;
        this.notificationPending = true;
        terminal.puts(InfoCmp.Capability.bell);
        terminal.flush();
    }

    public void clearNotification() {
        this.notificationPending = false;
        this.lastNotification = null;
    }

    public boolean hasNotification() {
        return notificationPending;
    }

    public String getLastNotification() {
        return lastNotification;
    }

    private String buildPrompt(String prompt) {
        StringBuilder sb = new StringBuilder();
        sb.append(Ansi.colored(prompt, AnsiColor.CYAN));

        if (modeIndicator != null && !modeIndicator.isEmpty()) {
            sb.append(Ansi.styled("[" + modeIndicator + "] ", AnsiStyle.DIM));
        }

        if (notificationPending) {
            sb.append(Ansi.colored("(!) ", AnsiColor.YELLOW));
        }

        return sb.toString();
    }

    public void renderStatusBar() {
        if (footer == null || footer.isEmpty()) {
            return;
        }

        terminal.writer().println();
        terminal.writer().print(Ansi.styled("┌", AnsiColor.GRAY));
        terminal.writer().print(Ansi.styled("─".repeat(Math.max(0, terminal.getWidth() - 2)), AnsiColor.GRAY));
        terminal.writer().println(Ansi.styled("┐", AnsiColor.GRAY));

        String displayFooter = footer.length() > terminal.getWidth() - 4 
            ? footer.substring(0, terminal.getWidth() - 4) 
            : footer;
        terminal.writer().println(Ansi.styled("│", AnsiColor.GRAY) + " " + Ansi.colored(displayFooter, AnsiColor.GRAY));

        terminal.writer().print(Ansi.styled("└", AnsiColor.GRAY));
        terminal.writer().print(Ansi.styled("─".repeat(Math.max(0, terminal.getWidth() - 2)), AnsiColor.GRAY));
        terminal.writer().println(Ansi.styled("┘", AnsiColor.GRAY));
        terminal.flush();
    }

    public void clearLine() {
        terminal.puts(InfoCmp.Capability.delete_line);
        terminal.flush();
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public LineReader getReader() {
        return reader;
    }
}