package com.claudecode.ui.renderer;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;

import java.util.function.Consumer;

public class SelectionIndicator {

    private final int totalLines;
    private volatile int selectedIndex;
    private volatile boolean verboseMode;
    private final String[] items;

    public SelectionIndicator(String[] items) {
        this.items = items;
        this.totalLines = items.length;
        this.selectedIndex = 0;
        this.verboseMode = false;
    }

    public void select(int index) {
        if (index >= 0 && index < totalLines) {
            this.selectedIndex = index;
        }
    }

    public void selectNext() {
        if (selectedIndex < totalLines - 1) {
            selectedIndex++;
        }
    }

    public void selectPrevious() {
        if (selectedIndex > 0) {
            selectedIndex--;
        }
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public String getSelectedItem() {
        return items[selectedIndex];
    }

    public void toggleVerbose() {
        this.verboseMode = !verboseMode;
    }

    public boolean isVerbose() {
        return verboseMode;
    }

    public void render(StringBuilder sb) {
        for (int i = 0; i < totalLines; i++) {
            renderLine(sb, i);
        }
    }

    private void renderLine(StringBuilder sb, int index) {
        boolean isSelected = (index == selectedIndex);
        String line = items[index];

        if (isSelected) {
            sb.append(Ansi.styled(" > ", AnsiColor.CYAN, AnsiStyle.BOLD));
            sb.append(Ansi.styled(line, AnsiColor.WHITE, AnsiStyle.BOLD));
            if (verboseMode) {
                sb.append(Ansi.styled(" [V]", AnsiColor.GRAY));
            }
        } else {
            sb.append(Ansi.colored("   ", AnsiColor.GRAY));
            sb.append(Ansi.colored(line, AnsiColor.GRAY));
        }
        sb.append("\n");
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        render(sb);
        return sb.toString();
    }

    public static class Builder {
        private String[] items = new String[0];
        private int defaultIndex = 0;

        public Builder items(String[] items) {
            this.items = items;
            return this;
        }

        public Builder defaultIndex(int index) {
            this.defaultIndex = index;
            return this;
        }

        public SelectionIndicator build() {
            SelectionIndicator indicator = new SelectionIndicator(items);
            if (defaultIndex > 0 && defaultIndex < items.length) {
                indicator.select(defaultIndex);
            }
            return indicator;
        }
    }
}