package com.claudecode.ui;

import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Transcript screen for full-screen conversation viewing.
 * Simplified version for Phase 6.1.
 */
public class TranscriptScreen {

    private final Terminal terminal;
    private final PrintWriter writer;
    private final VirtualScroller<String> scroller;
    private final List<String> lines = new ArrayList<>();

    private final AtomicBoolean active = new AtomicBoolean(false);
    private volatile String searchQuery = null;
    private int searchMatchIndex = -1;
    private final List<Integer> searchMatches = new ArrayList<>();
    private int terminalWidth = 80;
    private int terminalHeight = 24;

    public TranscriptScreen(Terminal terminal) {
        this.terminal = terminal;
        this.writer = terminal.writer();
        this.scroller = new VirtualScroller<>(20);
    }

    public void enter() {
        if (!active.compareAndSet(false, true)) return;
        terminal.puts(InfoCmp.Capability.enter_ca_mode);
        terminal.puts(InfoCmp.Capability.clear_screen);
        terminal.puts(InfoCmp.Capability.cursor_invisible);
        updateSize();
        render();
    }

    public void exit() {
        if (!active.compareAndSet(true, false)) return;
        terminal.puts(InfoCmp.Capability.exit_ca_mode);
        terminal.puts(InfoCmp.Capability.cursor_visible);
        terminal.puts(InfoCmp.Capability.clear_screen);
        writer.flush();
    }

    public boolean toggle() {
        if (active.get()) exit();
        else enter();
        return active.get();
    }

    public boolean isActive() {
        return active.get();
    }

    public void addMessage(String message) {
        lines.add(message);
        scroller.setItems(lines);
        if (active.get()) render();
    }

    public void clear() {
        lines.clear();
        scroller.setItems(lines);
        if (active.get()) render();
    }

    public boolean handleKey(char key) {
        if (!active.get()) return false;
        switch (key) {
            case 'q', 27: exit(); return true;
            case 'j': scrollDown(1); return true;
            case 'k': scrollUp(1); return true;
            case 'g': scrollToTop(); return true;
            case 'G': scrollToBottom(); return true;
            case '/': startSearch(); return true;
            case 'n': searchNext(); return true;
            case 'N': searchPrevious(); return true;
            default: return false;
        }
    }

    public void startSearch() {
        searchQuery = "";
        searchMatchIndex = -1;
        render();
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query;
        this.searchMatches.clear();
        if (query != null && !query.isEmpty()) {
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).toLowerCase().contains(query.toLowerCase())) {
                    searchMatches.add(i);
                }
            }
            if (!searchMatches.isEmpty()) {
                searchMatchIndex = 0;
                scroller.scrollToIndex(searchMatches.get(0));
            }
        }
        if (active.get()) render();
    }

    public void searchNext() {
        if (searchMatches.isEmpty()) return;
        searchMatchIndex = (searchMatchIndex + 1) % searchMatches.size();
        scroller.scrollToIndex(searchMatches.get(searchMatchIndex));
    }

    public void searchPrevious() {
        if (searchMatches.isEmpty()) return;
        searchMatchIndex = (searchMatchIndex - 1 + searchMatches.size()) % searchMatches.size();
        scroller.scrollToIndex(searchMatches.get(searchMatchIndex));
    }

    public void scrollDown(int lines) { scroller.scrollDown(lines); if (active.get()) render(); }
    public void scrollUp(int lines) { scroller.scrollUp(lines); if (active.get()) render(); }
    public void scrollToTop() { scroller.scrollToTop(); if (active.get()) render(); }
    public void scrollToBottom() { scroller.scrollToBottom(); if (active.get()) render(); }

    private void updateSize() {
        var size = terminal.getSize();
        terminalWidth = size.getColumns();
        terminalHeight = size.getRows();
    }

    private void render() {
        if (!active.get()) return;
        updateSize();
        terminal.puts(InfoCmp.Capability.clear_screen);

        writer.println();
        writer.println(Ansi.colored("┌─ Transcript ────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.colored("│", AnsiColor.CYAN) + " q: quit  j/k: scroll  /: search");
        writer.println(Ansi.colored("├─────────────────────────────────────────────────────┤", AnsiColor.CYAN));

        List<String> visible = scroller.getVisibleItems();
        for (int i = 0; i < visible.size(); i++) {
            String line = visible.get(i);
            int idx = scroller.getTopIndex() + i;
            boolean isMatch = searchMatches.contains(idx);
            boolean isCurrent = isMatch && searchMatches.indexOf(idx) == searchMatchIndex;

            String prefix = String.format("%4d ", idx + 1);
            if (isCurrent && searchQuery != null) {
                int pos = line.toLowerCase().indexOf(searchQuery.toLowerCase());
                if (pos >= 0) {
                    line = line.substring(0, pos) +
                           Ansi.styled(searchQuery, AnsiStyle.BOLD) +
                           line.substring(pos + searchQuery.length());
                }
            }
            writer.println(prefix + (isMatch ? Ansi.styled(line, AnsiStyle.BOLD) : line));
        }

        int from = scroller.getTopIndex() + 1;
        int to = Math.min(from + visible.size() - 1, lines.size());
        String footerLeft = searchQuery != null ? " /" + searchQuery + " [" + (searchMatchIndex + 1) + "/" + searchMatches.size() + "]" : "transcript";
        writer.println(Ansi.colored("├─────────────────────────────────────────────────────┤", AnsiColor.CYAN));
        writer.println(Ansi.colored("│", AnsiColor.CYAN) + " " + footerLeft +
                " ".repeat(Math.max(0, terminalWidth - footerLeft.length() - (to + "").length() - 6)) +
                from + "-" + to + "/" + lines.size() + " " + Ansi.colored("│", AnsiColor.CYAN));
        writer.println(Ansi.colored("└─────────────────────────────────────────────────────┘", AnsiColor.CYAN));
        writer.flush();
    }
}
