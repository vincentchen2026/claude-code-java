package com.claudecode.ui.agent;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;

/**
 * Agent status bar shown at the bottom of the terminal.
 * Task 61.2: Agent navigation bottom bar
 */
public class AgentStatusBar {

    public void render(String currentAgent, String status) {
        StringBuilder sb = new StringBuilder();
        sb.append("\r");
        sb.append(Ansi.colored("👤 ", AnsiColor.CYAN));
        sb.append(Ansi.colored(currentAgent != null ? currentAgent : "main", AnsiColor.WHITE));

        if (status != null && !status.isEmpty()) {
            sb.append(" ");
            sb.append(Ansi.colored("(" + status + ")", AnsiColor.GRAY));
        }

        System.out.print(sb);
        System.out.flush();
    }

    public void clear() {
        System.out.print("\r\u001B[K");
        System.out.flush();
    }
}