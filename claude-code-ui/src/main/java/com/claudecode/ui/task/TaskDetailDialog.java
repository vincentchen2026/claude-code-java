package com.claudecode.ui.task;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.AnsiStyle;

import java.io.PrintWriter;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

public class TaskDetailDialog {

    private final PrintWriter writer;
    private final TaskInfo taskInfo;

    public TaskDetailDialog(PrintWriter writer, TaskInfo taskInfo) {
        this.writer = writer;
        this.taskInfo = taskInfo;
    }

    public void render() {
        writer.println();
        renderHeader();
        renderBody();
        renderFooter();
    }

    private void renderHeader() {
        String title = switch (taskInfo.type()) {
            case WORKFLOW -> "Workflow Task";
            case DREAM -> "Dream Task";
            case ASYNC_AGENT -> "Async Agent Task";
            case TEAMMATE -> "Teammate Task";
            case REMOTE_SESSION -> "Remote Session";
            case MONITOR_MCP -> "Monitor MCP";
            case SHELL -> "Shell Task";
        };

        writer.println(Ansi.styled("┌─ " + title + " ───────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " ID: " + Ansi.colored(taskInfo.id(), AnsiColor.WHITE));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Status: " + renderStatus(taskInfo.status()));
        if (taskInfo.parentId() != null) {
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Parent: " + Ansi.colored(taskInfo.parentId(), AnsiColor.GRAY));
        }
    }

    private void renderBody() {
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));

        switch (taskInfo.type()) {
            case WORKFLOW -> renderWorkflowBody();
            case DREAM -> renderDreamBody();
            case ASYNC_AGENT -> renderAsyncAgentBody();
            case TEAMMATE -> renderTeammateBody();
            case REMOTE_SESSION -> renderRemoteSessionBody();
            case MONITOR_MCP -> renderMonitorMcpBody();
            case SHELL -> renderShellBody();
        }
    }

    private void renderWorkflowBody() {
        WorkflowInfo workflow = (WorkflowInfo) taskInfo.extra();
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " " + Ansi.styled("Steps:", AnsiColor.WHITE));
        for (int i = 0; i < workflow.steps().size(); i++) {
            WorkflowInfo.WorkflowStep step = workflow.steps().get(i);
            String statusIcon = step.completed() ? Ansi.colored("✓", AnsiColor.GREEN)
                                                  : Ansi.colored("○", AnsiColor.GRAY);
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + "   " + statusIcon + " " +
                Ansi.colored("Step " + (i + 1) + ": " + step.name(), step.completed() ? AnsiColor.GRAY : AnsiColor.WHITE));
        }
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Progress: " +
            Ansi.colored(workflow.currentStep() + "/" + workflow.steps().size(), AnsiColor.CYAN));
    }

    private void renderDreamBody() {
        DreamInfo dream = (DreamInfo) taskInfo.extra();
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " " + Ansi.styled("Dream:", AnsiColor.WHITE));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + "   " + Ansi.styled(dream.prompt(), AnsiStyle.DIM));
        if (dream.result() != null) {
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Result: " + Ansi.colored(dream.result(), AnsiColor.GREEN));
        }
    }

    private void renderAsyncAgentBody() {
        AsyncAgentInfo agent = (AsyncAgentInfo) taskInfo.extra();
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Model: " + Ansi.colored(agent.model(), AnsiColor.MAGENTA));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Messages: " + Ansi.colored(String.valueOf(agent.messageCount()), AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Last Activity: " +
            Ansi.colored(agent.lastActivity(), AnsiColor.GRAY));
    }

    private void renderTeammateBody() {
        TeammateInfo teammate = (TeammateInfo) taskInfo.extra();
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Agent: " + Ansi.colored(teammate.agentName(), AnsiColor.MAGENTA));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Role: " + Ansi.colored(teammate.role(), AnsiColor.GRAY));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Messages: " + Ansi.colored(String.valueOf(teammate.messageCount()), AnsiColor.CYAN));
    }

    private void renderRemoteSessionBody() {
        RemoteSessionInfo session = (RemoteSessionInfo) taskInfo.extra();
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Remote: " + Ansi.colored(session.remoteUrl(), AnsiColor.MAGENTA));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Session: " + Ansi.colored(session.sessionId(), AnsiColor.GRAY));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Connected: " +
            Ansi.colored(String.valueOf(session.connected()), session.connected() ? AnsiColor.GREEN : AnsiColor.RED));
    }

    private void renderMonitorMcpBody() {
        MonitorMcpInfo monitor = (MonitorMcpInfo) taskInfo.extra();
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Server: " + Ansi.colored(monitor.serverName(), AnsiColor.MAGENTA));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Tools: " + Ansi.colored(String.valueOf(monitor.toolCount()), AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Last Check: " + Ansi.colored(monitor.lastCheck(), AnsiColor.GRAY));
    }

    private void renderShellBody() {
        ShellInfo shell = (ShellInfo) taskInfo.extra();
        if (shell.command() != null) {
            writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Command: " + Ansi.colored(shell.command(), AnsiColor.GRAY));
        }
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " Exit Code: " +
            (shell.exitCode() != null ? Ansi.colored(String.valueOf(shell.exitCode()), AnsiColor.GREEN) : Ansi.colored("running", AnsiColor.YELLOW)));
    }

    private void renderFooter() {
        writer.println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.println(Ansi.styled("│", AnsiColor.CYAN) + " [v]iew output  [w]atch  [c]lose");
        writer.print(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        writer.print(" > ");
        writer.flush();
    }

    private String renderStatus(String status) {
        AnsiColor color = switch (status.toLowerCase()) {
            case "running", "active" -> AnsiColor.GREEN;
            case "pending", "queued" -> AnsiColor.YELLOW;
            case "completed", "done" -> AnsiColor.CYAN;
            case "failed", "error" -> AnsiColor.RED;
            case "cancelled" -> AnsiColor.MAGENTA;
            default -> AnsiColor.GRAY;
        };
        return Ansi.colored(status, color);
    }

    public enum TaskType {
        WORKFLOW, DREAM, ASYNC_AGENT, TEAMMATE, REMOTE_SESSION, MONITOR_MCP, SHELL
    }

    public record TaskInfo(
        String id,
        TaskType type,
        String status,
        String parentId,
        Object extra
    ) {}

    public record WorkflowInfo(
        List<WorkflowStep> steps,
        int currentStep
    ) {
        public record WorkflowStep(
            String name,
            boolean completed
        ) {}
    }

    public record DreamInfo(
        String prompt,
        String result
    ) {}

    public record AsyncAgentInfo(
        String model,
        int messageCount,
        String lastActivity
    ) {}

    public record TeammateInfo(
        String agentName,
        String role,
        int messageCount
    ) {}

    public record RemoteSessionInfo(
        String remoteUrl,
        String sessionId,
        boolean connected
    ) {}

    public record MonitorMcpInfo(
        String serverName,
        int toolCount,
        String lastCheck
    ) {}

    public record ShellInfo(
        String command,
        Integer exitCode
    ) {}
}