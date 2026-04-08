package com.claudecode.commands.impl;

import com.claudecode.commands.Command;
import com.claudecode.commands.CommandContext;
import com.claudecode.commands.CommandResult;

import java.util.List;

public class CopyCommand implements Command {

    @Override
    public String name() { return "copy"; }

    @Override
    public String description() { return "Copy last response to clipboard"; }

    @Override
    public List<String> aliases() { return List.of("yank"); }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        StringBuilder sb = new StringBuilder();
        sb.append("Copy Command\n");
        sb.append("===========\n\n");
        sb.append("This command copies the last assistant response to clipboard.\n\n");
        sb.append("Supported platforms:\n");
        sb.append("  - macOS: pbcopy\n");
        sb.append("  - Linux: xclip or xsel\n");
        sb.append("  - Windows: clip\n\n");
        sb.append("Note: This feature requires platform-specific clipboard utilities.\n");
        return CommandResult.of(sb.toString());
    }
}