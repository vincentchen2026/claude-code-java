package com.claudecode.ui.renderer;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;

public class VerboseToggle {

    private volatile boolean verbose;

    public VerboseToggle() {
        this(false);
    }

    public VerboseToggle(boolean initialVerbose) {
        this.verbose = initialVerbose;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void toggle() {
        this.verbose = !verbose;
    }

    public void enable() {
        this.verbose = true;
    }

    public void disable() {
        this.verbose = false;
    }

    public String renderStatus() {
        String indicator = verbose ? "[V] verbose" : "[v] verbose";
        return verbose ? Ansi.colored(indicator, AnsiColor.GREEN) : Ansi.colored(indicator, AnsiColor.GRAY);
    }

    public void renderStatus(StringBuilder sb) {
        sb.append(" ");
        sb.append(renderStatus());
    }

    public static String formatVerboseKey() {
        return Ansi.colored("[v]", AnsiColor.GRAY);
    }

    public static String formatExpanded(String detail) {
        return detail;
    }
}