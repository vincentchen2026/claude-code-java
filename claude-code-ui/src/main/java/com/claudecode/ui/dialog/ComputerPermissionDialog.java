package com.claudecode.ui.dialog;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;

import java.io.PrintWriter;

public class ComputerPermissionDialog {

    private final PrintWriter writer;

    public ComputerPermissionDialog(PrintWriter writer) {
        this.writer = writer;
    }

    public void renderComputerUse(String action, String target) {
        writer.println();
        writer.println(Ansi.styled("┌─ Computer Use ─────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Action: " + Ansi.colored(action, AnsiColor.YELLOW));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Target: " + Ansi.colored(target, AnsiColor.WHITE));
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [a]llow  [d]eny  [l]imit  [c]ancel");
        writer.print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.print(" > ");
        writer.flush();
    }

    public void renderMonitor(String sessionId, String pid) {
        writer.println();
        writer.println(Ansi.styled("┌─ Monitor Permission ───────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Type: " + Ansi.colored("Process Monitor", AnsiColor.YELLOW));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " PID: " + Ansi.colored(pid, AnsiColor.WHITE));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Session: " + Ansi.colored(sessionId, AnsiColor.GRAY));
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [a]llow  [d]eny  [c]ancel");
        writer.print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.print(" > ");
        writer.flush();
    }

    public void renderReviewArtifact(String artifactId, String artifactType) {
        writer.println();
        writer.println(Ansi.styled("┌─ Review Artifact ───────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " ID: " + Ansi.colored(artifactId, AnsiColor.WHITE));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Type: " + Ansi.colored(artifactType, AnsiColor.YELLOW));
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [v]iew  [d]ismiss  [c]ancel");
        writer.print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.print(" > ");
        writer.flush();
    }

    public void renderSandbox(String sandboxId, String sandboxType) {
        writer.println();
        writer.println(Ansi.styled("┌─ Sandbox Permission ───────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Sandbox: " + Ansi.colored(sandboxId, AnsiColor.WHITE));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Type: " + Ansi.colored(sandboxType, AnsiColor.YELLOW));
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [c]reate  [u]se  [d]eny  [c]ancel");
        writer.print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.print(" > ");
        writer.flush();
    }

    public void renderSandboxUse(String sandboxId, String action) {
        writer.println();
        writer.println(Ansi.styled("┌─ Sandbox Action ────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Sandbox: " + Ansi.colored(sandboxId, AnsiColor.WHITE));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Action: " + Ansi.colored(action, AnsiColor.YELLOW));
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [e]xecute  [r]ead  [w]rite  [d]eny  [c]ancel");
        writer.print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.print(" > ");
        writer.flush();
    }
}