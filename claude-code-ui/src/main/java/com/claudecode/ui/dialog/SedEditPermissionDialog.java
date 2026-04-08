package com.claudecode.ui.dialog;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;

import java.io.PrintWriter;
import java.util.List;

/**
 * SedEdit permission dialog for sed/pattern-based edits.
 * Task 60.4: SedEdit permission
 */
public class SedEditPermissionDialog {

    private final PrintWriter writer;

    public SedEditPermissionDialog(PrintWriter writer) {
        this.writer = writer;
    }

    public void renderSedEditPermission(String filePath, String pattern, String replacement, int occurrences) {
        writer.println();
        writer.println(Ansi.styled("┌─ SedEdit Permission ────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " File: " + Ansi.colored(filePath, AnsiColor.YELLOW));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Pattern: " + Ansi.colored("\"" + pattern + "\"", AnsiColor.WHITE));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Replace: " + Ansi.colored("\"" + replacement + "\"", AnsiColor.WHITE));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Matches: " + Ansi.colored(occurrences + " occurrences", occurrences > 1 ? AnsiColor.YELLOW : AnsiColor.GREEN));
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [a]lways  [d]eny  [o]nce  [c]ancel");
        writer.print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.print(" > ");
        writer.flush();
    }

    public void renderNotebookEditPermission(String notebookPath, int cellIndex, String cellType) {
        writer.println();
        writer.println(Ansi.styled("┌─ NotebookEdit Permission ───────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Notebook: " + Ansi.colored(notebookPath, AnsiColor.YELLOW));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Cell: " + Ansi.colored("#" + cellIndex, AnsiColor.WHITE));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Type: " + Ansi.colored(cellType, AnsiColor.CYAN));
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [a]lways  [d]eny  [o]nce  [c]ancel");
        writer.print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.print(" > ");
        writer.flush();
    }
}