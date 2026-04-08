package com.claudecode.ui.agent;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;

import java.util.List;

/**
 * Agent progress line for coordinator agent status display.
 * Task 61.5: Agent progress line / Coordinator agent status
 */
public class AgentProgressLine {

    public void render(String coordinatorName, List<WorkerStatus> workers) {
        StringBuilder sb = new StringBuilder();
        sb.append(Ansi.colored("┌─ Coordinator: ", AnsiColor.CYAN));
        sb.append(Ansi.colored(coordinatorName, AnsiColor.WHITE));
        sb.append(Ansi.styled(" [", AnsiStyle.DIM));
        sb.append(Ansi.colored(workers.size() + " workers", AnsiColor.YELLOW));
        sb.append(Ansi.styled("] ┐", AnsiStyle.DIM));
        sb.append("\n");

        for (WorkerStatus worker : workers) {
            sb.append(Ansi.colored("│ ", AnsiColor.CYAN));
            sb.append(renderWorkerStatus(worker));
            sb.append("\n");
        }

        sb.append(Ansi.colored("└────────────────────────────────────────────", AnsiColor.CYAN));
        System.out.print(sb.toString());
        System.out.flush();
    }

    private String renderWorkerStatus(WorkerStatus worker) {
        StringBuilder sb = new StringBuilder();
        sb.append(Ansi.colored("⠋", AnsiColor.CYAN));
        sb.append(" ");
        sb.append(Ansi.colored(worker.name(), AnsiColor.WHITE));
        sb.append(": ");

        if (worker.progress() >= 0) {
            sb.append(renderProgressBar(worker.progress()));
            sb.append(" ");
        }

        if (worker.status() != null && !worker.status().isEmpty()) {
            sb.append(Ansi.styled(worker.status(), AnsiStyle.DIM));
        }

        return sb.toString();
    }

    private String renderProgressBar(int percent) {
        int width = 10;
        int filled = (int) ((percent / 100.0) * width);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < width; i++) {
            if (i < filled) {
                sb.append(Ansi.colored("█", AnsiColor.GREEN));
            } else {
                sb.append(Ansi.colored("░", AnsiColor.GRAY));
            }
        }
        sb.append("]");
        sb.append(" ").append(Ansi.colored(percent + "%", AnsiColor.CYAN));
        return sb.toString();
    }

    public void clear() {
        System.out.print("\r\u001B[K");
        System.out.flush();
    }

    public record WorkerStatus(
        String name,
        int progress,
        String status
    ) {}
}