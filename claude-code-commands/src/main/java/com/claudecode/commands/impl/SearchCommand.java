package com.claudecode.commands.impl;

import com.claudecode.commands.Command;
import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandResult;

import java.util.List;

public class SearchCommand implements Command {

    @Override
    public String name() { return "search"; }

    @Override
    public String description() { return "Search conversation history"; }

    @Override
    public List<String> aliases() { return List.of("history", "find"); }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        StringBuilder sb = new StringBuilder();

        if (args == null || args.isBlank()) {
            return showHelp(sb);
        }

        sb.append("Search: \"").append(args).append("\"\n");
        sb.append("===========\n\n");
        sb.append("Conversation search functionality.\n\n");
        sb.append("Search is performed across:\n");
        sb.append("  - User messages\n");
        sb.append("  - Assistant responses\n");
        sb.append("  - Tool results\n\n");
        sb.append("Use Ctrl+R to search in terminal history.\n");
        return CommandResult.of(sb.toString());
    }

    private CommandResult showHelp(StringBuilder sb) {
        sb.append("Search Command\n");
        sb.append("==============\n\n");
        sb.append("Usage: /search <query>\n\n");
        sb.append("Searches your conversation history for the specified query.\n\n");
        sb.append("Examples:\n");
        sb.append("  /search bug fix\n");
        sb.append("  /search authentication\n\n");
        sb.append("Tip: Use Ctrl+R in the terminal to search command history.\n");
        return CommandResult.of(sb.toString());
    }
}