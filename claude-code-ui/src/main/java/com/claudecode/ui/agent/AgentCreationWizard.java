package com.claudecode.ui.agent;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import org.jline.terminal.Terminal;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;

import java.util.ArrayList;
import java.util.List;

public class AgentCreationWizard {

    private final Terminal terminal;
    private final LineReader reader;
    private final List<WizardStep> steps;
    private int currentStep;

    public AgentCreationWizard(Terminal terminal) {
        this.terminal = terminal;
        this.reader = LineReaderBuilder.builder().terminal(terminal).build();
        this.steps = new ArrayList<>();
        this.currentStep = 0;
        initializeSteps();
    }

    private void initializeSteps() {
        steps.add(new WizardStep("name", "Enter agent name:"));
        steps.add(new WizardStep("model", "Select model (1-" + 4 + "):"));
        steps.add(new WizardStep("description", "Enter agent description:"));
        steps.add(new WizardStep("color", "Select color (1-" + 6 + "):"));
        steps.add(new WizardStep("tools", "Select tools (a=all, c=custom, n=none):"));
        steps.add(new WizardStep("confirm", "Confirm creation (y/n):"));
    }

    public WizardResult run() {
        terminal.writer().println();
        terminal.writer().println(Ansi.styled("┌─ Create New Agent ───────────────────────────────────────", AnsiColor.CYAN));
        terminal.writer().println(Ansi.styled("│", AnsiColor.CYAN) + " Follow the prompts to create a new agent");
        terminal.writer().println(Ansi.styled("├─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        terminal.writer().println(Ansi.styled("│", AnsiColor.CYAN) + " Press [q] to quit at any time");
        terminal.writer().println(Ansi.styled("└─────────────────────────────────────────────────────────", AnsiColor.CYAN));
        terminal.writer().println();

        String agentName = null;
        String model = null;
        String description = null;
        String color = null;
        String tools = null;

        for (int i = 0; i < steps.size(); i++) {
            currentStep = i;
            WizardStep step = steps.get(i);

            terminal.writer().print(Ansi.styled("Step " + (i + 1) + "/" + steps.size() + ": ", AnsiColor.YELLOW));
            terminal.writer().print(Ansi.colored(step.prompt(), AnsiColor.WHITE));
            terminal.writer().print(" > ");
            terminal.writer().flush();

            try {
                String line = reader.readLine();
                if (line == null || line.equals("q")) {
                    return WizardResult.cancelled();
                }

                switch (step.id()) {
                    case "name" -> agentName = line.trim();
                    case "model" -> model = parseModel(line.trim());
                    case "description" -> description = line.trim();
                    case "color" -> color = parseColor(line.trim());
                    case "tools" -> tools = parseTools(line.trim());
                    case "confirm" -> {
                        if (!line.trim().equalsIgnoreCase("y")) {
                            return WizardResult.cancelled();
                        }
                    }
                }
            } catch (Exception e) {
                return WizardResult.error(e.getMessage());
            }
        }

        return WizardResult.success(new CreatedAgent(agentName, model, description, color, tools));
    }

    private String parseModel(String input) {
        try {
            int choice = Integer.parseInt(input) - 1;
            String[] models = {"claude-sonnet-4-20250514", "claude-opus-4-20250514", 
                             "claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022"};
            if (choice >= 0 && choice < models.length) {
                return models[choice];
            }
        } catch (NumberFormatException e) {
            // not a number, use as custom
        }
        return input.isEmpty() ? "claude-sonnet-4-20250514" : input;
    }

    private String parseColor(String input) {
        try {
            int choice = Integer.parseInt(input) - 1;
            String[] colors = {"CYAN", "GREEN", "YELLOW", "MAGENTA", "BLUE", "WHITE"};
            if (choice >= 0 && choice < colors.length) {
                return colors[choice];
            }
        } catch (NumberFormatException e) {
            // not a number
        }
        return "CYAN";
    }

    private String parseTools(String input) {
        String lower = input.toLowerCase();
        if (lower.equals("a")) return "all";
        if (lower.equals("c")) return "custom";
        if (lower.equals("n")) return "none";
        return "all";
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public int getTotalSteps() {
        return steps.size();
    }

    public record WizardStep(String id, String prompt) {}

    public record CreatedAgent(
        String name,
        String model,
        String description,
        String color,
        String tools
    ) {}

    public record WizardResult(
        boolean isSuccess,
        boolean isCancelled,
        String error,
        CreatedAgent agent
    ) {
        public static WizardResult success(CreatedAgent agent) {
            return new WizardResult(true, false, null, agent);
        }

        public static WizardResult cancelled() {
            return new WizardResult(false, true, null, null);
        }

        public static WizardResult error(String error) {
            return new WizardResult(false, false, error, null);
        }
    }
}