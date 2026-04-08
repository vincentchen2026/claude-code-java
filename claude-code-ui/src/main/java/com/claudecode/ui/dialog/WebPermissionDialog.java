package com.claudecode.ui.dialog;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;

import java.io.PrintWriter;

/**
 * Web permission dialog for WebFetch and AskUserQuestion.
 * Task 60.5: WebFetch/AskUserQuestion permission
 */
public class WebPermissionDialog {

    private final PrintWriter writer;

    public WebPermissionDialog(PrintWriter writer) {
        this.writer = writer;
    }

    public void renderWebFetchPermission(String url, String method) {
        String hostname = extractHostname(url);

        writer.println();
        writer.println(Ansi.styled("┌─ Web Permission ───────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Tool: " + Ansi.colored("WebFetch", AnsiColor.YELLOW));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Host: " + Ansi.colored(hostname, AnsiColor.WHITE));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " URL:  " + Ansi.styled(truncateUrl(url, 50), AnsiStyle.DIM));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Method: " + Ansi.colored(method, AnsiColor.YELLOW));
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));

        AnsiColor riskColor = isRiskyHostname(hostname) ? AnsiColor.YELLOW : AnsiColor.GREEN;
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Risk: " + Ansi.colored(isRiskyHostname(hostname) ? "external" : "standard", riskColor));
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [a]lways  [d]eny  [o]nce  [c]ancel");
        writer.print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.print(" > ");
        writer.flush();
    }

    public void renderAskUserQuestion(String question, int optionCount) {
        writer.println();
        writer.println(Ansi.styled("┌─ Question ─────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " " + truncate(question, 60));
        if (optionCount > 0) {
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Options: " + Ansi.colored(optionCount + " choices", AnsiColor.YELLOW));
        }
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [a]nswer  [c]ancel");
        writer.print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.print(" > ");
        writer.flush();
    }

    private String extractHostname(String url) {
        try {
            if (url.startsWith("http://")) {
                url = url.substring(7);
            } else if (url.startsWith("https://")) {
                url = url.substring(8);
            }
            int slashIndex = url.indexOf('/');
            if (slashIndex > 0) {
                return url.substring(0, slashIndex);
            }
            return url;
        } catch (Exception e) {
            return url;
        }
    }

    private String truncateUrl(String url, int maxLength) {
        if (url == null) return "";
        if (url.length() <= maxLength) return url;
        return url.substring(0, maxLength - 3) + "...";
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    private boolean isRiskyHostname(String hostname) {
        if (hostname == null) return false;
        String lower = hostname.toLowerCase();
        return lower.contains("facebook.com") || lower.contains("twitter.com") ||
               lower.contains("tiktok.com") || lower.contains("reddit.com") ||
               lower.contains("linkedin.com");
    }
}