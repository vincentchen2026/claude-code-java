package com.claudecode.ui.dialog;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;

import java.io.PrintWriter;

/**
 * PowerShell permission dialog.
 * Task 60.4: PowerShell permission
 */
public class PowerShellPermissionDialog {

    private final PrintWriter writer;

    public PowerShellPermissionDialog(PrintWriter writer) {
        this.writer = writer;
    }

    public void renderPowerShellPermission(String command) {
        writer.println();
        writer.println(Ansi.styled("┌─ PowerShell Permission ────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Tool: " + Ansi.colored("PowerShell", AnsiColor.YELLOW));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Shell: " + Ansi.colored(detectShell(), AnsiColor.WHITE));
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Command:");
        for (String line : wrapCommand(command, 55)) {
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + "   " + Ansi.styled(line, AnsiStyle.DIM));
        }
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [a]lways  [d]eny  [o]nce  [c]ancel");
        writer.print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.print(" > ");
        writer.flush();
    }

    private String detectShell() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return "powershell.exe";
        } else {
            return "pwsh";
        }
    }

    private String[] wrapCommand(String command, int width) {
        if (command == null) return new String[]{""};
        if (command.length() <= width) return new String[]{command};

        String[] words = command.split(" ");
        StringBuilder line = new StringBuilder();
        java.util.List<String> lines = new java.util.ArrayList<>();

        for (String word : words) {
            if (line.length() + word.length() + 1 > width) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                if (line.length() > 0) line.append(" ");
                line.append(word);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines.toArray(new String[0]);
    }
}