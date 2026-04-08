package com.claudecode.ui.dialog;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * File permission dialog with path tree display.
 * Task 60.3: File permission prompt with path tree
 */
public class FilePermissionDialog {

    private final PrintWriter writer;

    public FilePermissionDialog(PrintWriter writer) {
        this.writer = writer;
    }

    public void renderFileTree(String action, List<String> paths) {
        writer.println();
        writer.println(Ansi.styled("┌─ File Permission ─────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Action: " + Ansi.colored(action, AnsiColor.YELLOW));
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));

        if (paths != null && !paths.isEmpty()) {
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Files:");
            for (String path : paths) {
                writer.println(Ansi.styled("│", AnsiColor.CYAN) + "   " + Ansi.colored("📄", AnsiColor.GREEN) + " " + Ansi.styled(path, AnsiStyle.DIM));
            }
        }

        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [a]lways  [d]eny  [o]nce  [c]ancel");
        writer.print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.print(" > ");
        writer.flush();
    }

    public void renderFileDiff(String filePath, String diff) {
        writer.println();
        writer.println(Ansi.styled("┌─ File Diff ────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " File: " + Ansi.colored(filePath, AnsiColor.YELLOW));
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));

        if (diff != null && !diff.isEmpty()) {
            for (String line : diff.split("\n", -1)) {
                if (line.startsWith("+")) {
                    writer.println(Ansi.styled("│", AnsiColor.GREEN) + " " + Ansi.colored(line, AnsiColor.GREEN));
                } else if (line.startsWith("-")) {
                    writer.println(Ansi.styled("│", AnsiColor.RED) + " " + Ansi.colored(line, AnsiColor.RED));
                } else {
                    writer.println(Ansi.styled("│", AnsiColor.CYAN) + " " + Ansi.styled(line, AnsiStyle.DIM));
                }
            }
        }

        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [a]ccept  [c]ancel");
        writer.print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.print(" > ");
        writer.flush();
    }
}