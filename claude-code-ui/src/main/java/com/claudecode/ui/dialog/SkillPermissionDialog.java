package com.claudecode.ui.dialog;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;

import java.io.PrintWriter;

/**
 * Skill permission dialog.
 * Task 60.7: Skill permission
 */
public class SkillPermissionDialog {

    private final PrintWriter writer;

    public SkillPermissionDialog(PrintWriter writer) {
        this.writer = writer;
    }

    public void renderSkillPermission(String skillName, String description, String source) {
        writer.println();
        writer.println(Ansi.styled("┌─ Skill Permission ───────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Skill: " + Ansi.colored(skillName, AnsiColor.YELLOW));
        if (description != null && !description.isEmpty()) {
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Desc:  " + Ansi.styled(truncate(description, 50), AnsiStyle.DIM));
        }
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Source: " + Ansi.colored(source, AnsiColor.WHITE));
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [a]lways  [d]eny  [o]nce  [c]ancel");
        writer.print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.print(" > ");
        writer.flush();
    }

    public void renderPlanModePermission(String action, String planSummary) {
        writer.println();
        writer.println(Ansi.styled("┌─ Plan Mode ─────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Action: " + Ansi.colored(action, AnsiColor.YELLOW));
        if (planSummary != null && !planSummary.isEmpty()) {
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Plan: " + Ansi.styled(truncate(planSummary, 50), AnsiStyle.DIM));
        }
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [y]es  [n]o");
        writer.print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.print(" > ");
        writer.flush();
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}