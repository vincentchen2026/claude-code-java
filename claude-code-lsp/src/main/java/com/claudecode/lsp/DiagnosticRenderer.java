package com.claudecode.lsp;

import com.claudecode.services.lsp.Diagnostic;
import com.claudecode.services.lsp.Diagnostic.Severity;
import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Renders LSP diagnostics in a user-friendly format.
 */
public class DiagnosticRenderer {

    private static final String SEPARATOR = "─".repeat(60);
    private static final String DOUBLE_SEPARATOR = "═".repeat(60);

    /**
     * Render diagnostics for a single file.
     */
    public static String renderFileDiagnostics(Path filePath, List<Diagnostic> diagnostics) {
        if (diagnostics == null || diagnostics.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        String filename = filePath.getFileName().toString();
        
        // Group by severity
        Map<Severity, List<Diagnostic>> bySeverity = diagnostics.stream()
            .collect(Collectors.groupingBy(Diagnostic::severity));

        // Count
        int errors = bySeverity.getOrDefault(Severity.ERROR, List.of()).size();
        int warnings = bySeverity.getOrDefault(Severity.WARNING, List.of()).size();
        int infos = bySeverity.getOrDefault(Severity.INFORMATION, List.of()).size();

        // Header
        sb.append("\n");
        sb.append(SEPARATOR).append("\n");
        sb.append(Ansi.styled(filename, AnsiStyle.BOLD)).append("\n");

        // Summary
        if (errors > 0) {
            sb.append(Ansi.colored("  ✖ " + errors + " error" + (errors > 1 ? "s" : ""), AnsiColor.RED));
        }
        if (warnings > 0) {
            if (errors > 0) sb.append("  ");
            sb.append(Ansi.colored("⚠ " + warnings + " warning" + (warnings > 1 ? "s" : ""), AnsiColor.YELLOW));
        }
        if (infos > 0) {
            if (errors > 0 || warnings > 0) sb.append("  ");
            sb.append(Ansi.colored("ℹ " + infos + " info", AnsiColor.BLUE));
        }
        sb.append("\n");
        sb.append(SEPARATOR).append("\n");

        // List each diagnostic
        for (Diagnostic diag : diagnostics) {
            sb.append(renderDiagnostic(diag));
        }

        return sb.toString();
    }

    /**
     * Render a single diagnostic.
     */
    public static String renderDiagnostic(Diagnostic diag) {
        StringBuilder sb = new StringBuilder();
        
        // Location
        sb.append(Ansi.colored(String.format("  %d:%d", diag.startLine() + 1, diag.startCharacter() + 1), AnsiColor.CYAN));
        
        // Severity indicator
        switch (diag.severity()) {
            case ERROR -> {
                sb.append("  ");
                sb.append(Ansi.styled("error", AnsiColor.RED, AnsiStyle.BOLD));
            }
            case WARNING -> {
                sb.append("  ");
                sb.append(Ansi.colored("warning", AnsiColor.YELLOW));
            }
            case INFORMATION -> {
                sb.append("  ");
                sb.append(Ansi.colored("info", AnsiColor.BLUE));
            }
            case HINT -> {
                sb.append("  ");
                sb.append(Ansi.colored("hint", AnsiColor.MAGENTA));
            }
        }

        // Source and code
        if (diag.source() != null) {
            sb.append("  [").append(diag.source()).append("]");
        }
        if (diag.code() != null) {
            sb.append(" (").append(diag.code()).append(")");
        }

        sb.append("\n");
        
        // Message
        sb.append("    ").append(diag.message()).append("\n");

        return sb.toString();
    }

    /**
     * Render all diagnostics for a workspace (condensed format).
     */
    public static String renderWorkspaceDiagnostics(Map<String, List<Diagnostic>> allDiagnostics) {
        if (allDiagnostics.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append(DOUBLE_SEPARATOR).append("\n");
        sb.append(Ansi.styled("Java Problems", AnsiStyle.BOLD)).append("\n");
        sb.append(DOUBLE_SEPARATOR).append("\n");

        int totalErrors = 0;
        int totalWarnings = 0;

        for (Map.Entry<String, List<Diagnostic>> entry : allDiagnostics.entrySet()) {
            String uri = entry.getKey();
            List<Diagnostic> diags = entry.getValue();
            
            if (diags.isEmpty()) continue;

            // Extract filename from URI
            String filename = Path.of(uri).getFileName().toString();
            
            int errors = (int) diags.stream().filter(d -> d.severity() == Severity.ERROR).count();
            int warnings = (int) diags.stream().filter(d -> d.severity() == Severity.WARNING).count();
            totalErrors += errors;
            totalWarnings += warnings;

            sb.append("\n");
            sb.append(Ansi.styled(filename, AnsiColor.WHITE, AnsiStyle.BOLD));
            
            if (errors > 0) {
                sb.append("  ").append(Ansi.colored(errors + " error" + (errors > 1 ? "s" : ""), AnsiColor.RED));
            }
            if (warnings > 0) {
                sb.append("  ").append(Ansi.colored(warnings + " warning" + (warnings > 1 ? "s" : ""), AnsiColor.YELLOW));
            }
            sb.append("\n");

            // Show first few diagnostics
            int shown = 0;
            for (Diagnostic diag : diags) {
                if (shown >= 5) {
                    sb.append("    ... and ").append(diags.size() - 5).append(" more\n");
                    break;
                }
                sb.append(String.format("    %d:%d: %s\n", 
                    diag.startLine() + 1, diag.startCharacter() + 1, 
                    truncate(diag.message(), 60)));
                shown++;
            }
        }

        sb.append("\n");
        sb.append(SEPARATOR);
        
        // Total summary
        sb.append("\n");
        if (totalErrors > 0) {
            sb.append(Ansi.colored("Total: " + totalErrors + " error" + (totalErrors > 1 ? "s" : ""), AnsiColor.RED));
        }
        if (totalWarnings > 0) {
            if (totalErrors > 0) sb.append("  ");
            sb.append(Ansi.colored(totalWarnings + " warning" + (totalWarnings > 1 ? "s" : ""), AnsiColor.YELLOW));
        }
        sb.append("\n");

        return sb.toString();
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 3) + "...";
    }
}
