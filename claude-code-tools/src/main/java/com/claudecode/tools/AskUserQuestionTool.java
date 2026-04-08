package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * AskUserQuestionTool — ask the user questions and wait for response(s).
 * Supports single and multi-question formats with options.
 *
 * Task 54.4 enhancements:
 * - MultiQuestion support (1-4 questions)
 * - Option lists (2-4 items per question)
 * - Multi-select mode
 * - Preview content
 * - Annotations
 */
public class AskUserQuestionTool extends Tool<JsonNode, String> {

    private static final JsonNode SCHEMA = buildSchema();
    private static final int MAX_QUESTIONS = 4;
    private static final int MIN_OPTIONS = 2;
    private static final int MAX_OPTIONS = 4;

    private final BufferedReader reader;
    private final PrintStream out;
    private final UserInputHandler inputHandler;

    public AskUserQuestionTool() {
        this(
            new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)),
            System.out,
            new TerminalUserInputHandler()
        );
    }

    public AskUserQuestionTool(BufferedReader reader, PrintStream out) {
        this(reader, out, new TerminalUserInputHandler());
    }

    public AskUserQuestionTool(BufferedReader reader, PrintStream out, UserInputHandler inputHandler) {
        this.reader = reader;
        this.out = out;
        this.inputHandler = inputHandler;
    }

    @Override
    public String name() { return "AskUserQuestion"; }

    @Override
    public String description() { return "Ask the user one or more questions and wait for response(s)"; }

    @Override
    public JsonNode inputSchema() { return SCHEMA; }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        if (input.has("questions") && input.get("questions").isArray()) {
            return handleMultiQuestion(input, context);
        }

        String question = input.has("question") ? input.get("question").asText("") : "";
        if (question.isBlank()) {
            return "Error: question is required";
        }

        List<Option> options = extractOptions(input, "options");

        if (!options.isEmpty()) {
            return handleOptionsQuestion(question, options, input, context);
        }

        return askSimpleQuestion(question);
    }

    private String handleMultiQuestion(JsonNode input, ToolExecutionContext context) {
        List<JsonNode> questionsNodes = new ArrayList<>();
        input.get("questions").elements().forEachRemaining(questionsNodes::add);

        if (questionsNodes.isEmpty() || questionsNodes.size() > MAX_QUESTIONS) {
            return "Error: questions array must have between 1 and " + MAX_QUESTIONS + " items";
        }

        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < questionsNodes.size(); i++) {
            JsonNode qNode = questionsNodes.get(i);
            String questionText = qNode.has("question") ? qNode.get("question").asText("") : "";
            List<Option> options = extractOptions(qNode, "options");
            boolean multiSelect = qNode.has("multi_select") && qNode.get("multi_select").asBoolean(false);

            if (questionText.isBlank()) {
                return "Error: question " + (i + 1) + " is blank";
            }

            if (!options.isEmpty() && (options.size() < MIN_OPTIONS || options.size() > MAX_OPTIONS)) {
                return "Error: question " + (i + 1) + " must have between " + MIN_OPTIONS +
                       " and " + MAX_OPTIONS + " options";
            }

            questions.add(new Question(questionText, options, multiSelect));
        }

        return askMultipleQuestions(questions);
    }

    private String handleOptionsQuestion(String question, List<Option> options,
                                        JsonNode input, ToolExecutionContext context) {
        boolean multiSelect = input.has("multi_select") && input.get("multi_select").asBoolean(false);
        String preview = extractPreview(input);

        out.println("\n" + question);
        if (!preview.isEmpty()) {
            out.println("[Preview: " + preview + "]");
        }
        out.println();

        for (int i = 0; i < options.size(); i++) {
            Option opt = options.get(i);
            String prefix = multiSelect ? "[" + (char)('a' + i) + "] " : "(" + (i + 1) + ") ";
            out.println(prefix + opt.label());
            if (opt.description() != null && !opt.description().isEmpty()) {
                out.println("    " + opt.description());
            }
        }
        out.println();
        out.flush();

        return readAnswer(multiSelect, options.size());
    }

    private String askSimpleQuestion(String question) {
        out.println(question);
        out.flush();

        return readAnswer(false, 0);
    }

    private String askMultipleQuestions(List<Question> questions) {
        StringBuilder results = new StringBuilder();

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            out.println("\n[" + (i + 1) + "/" + questions.size() + "] " + q.text());

            if (!q.options().isEmpty()) {
                for (int j = 0; j < q.options().size(); j++) {
                    Option opt = q.options().get(j);
                    String prefix = q.multiSelect() ? "[" + (char)('a' + j) + "] " : "(" + (j + 1) + ") ";
                    out.println(prefix + opt.label());
                }
            }
            out.println();
            out.flush();

            String answer = readAnswer(q.multiSelect(), q.options().size());
            results.append("Q").append(i + 1).append(": ").append(answer);
            if (i < questions.size() - 1) {
                results.append("\n");
            }
        }

        return results.toString();
    }

    private String readAnswer(boolean multiSelect, int optionCount) {
        try {
            String line = reader.readLine();
            if (line == null) {
                return "Error: no input received (EOF)";
            }
            line = line.trim();

            if (optionCount > 0) {
                return parseAnswer(line, multiSelect, optionCount);
            }

            return line;
        } catch (IOException e) {
            return "Error: failed to read user input: " + e.getMessage();
        }
    }

    private String parseAnswer(String line, boolean multiSelect, int optionCount) {
        if (multiSelect) {
            return parseMultiSelect(line, optionCount);
        }
        return parseSingleSelect(line, optionCount);
    }

    private String parseSingleSelect(String line, int optionCount) {
        try {
            int choice = Integer.parseInt(line);
            if (choice < 1 || choice > optionCount) {
                return "Error: please enter a number between 1 and " + optionCount;
            }
            return String.valueOf(choice);
        } catch (NumberFormatException e) {
            return "Error: please enter a valid number";
        }
    }

    private String parseMultiSelect(String line, int optionCount) {
        Set<Character> validChars = Set.of();
        for (int i = 0; i < optionCount; i++) {
            validChars.add((char)('a' + i));
        }

        List<Integer> selected = new ArrayList<>();
        for (char c : line.toLowerCase().toCharArray()) {
            if (validChars.contains(c)) {
                selected.add(c - 'a' + 1);
            }
        }

        if (selected.isEmpty()) {
            return "Error: please enter at least one valid option (a-" + (char)('a' + optionCount - 1) + ")";
        }

        selected.sort(Integer::compareTo);
        return selected.stream()
            .map(String::valueOf)
            .reduce((a, b) -> a + ", " + b)
            .orElse("");
    }

    private List<Option> extractOptions(JsonNode input, String fieldName) {
        List<Option> options = new ArrayList<>();
        if (input.has(fieldName) && input.get(fieldName).isArray()) {
            for (JsonNode optNode : input.get(fieldName)) {
                String label = optNode.has("label") ? optNode.get("label").asText("") : "";
                String description = optNode.has("description") ? optNode.get("description").asText("") : null;
                if (!label.isBlank()) {
                    options.add(new Option(label, description));
                }
            }
        }
        return options;
    }

    private String extractPreview(JsonNode input) {
        if (input.has("preview") && !input.get("preview").isNull()) {
            return input.get("preview").asText("");
        }
        return "";
    }

    private static JsonNode buildSchema() {
        ObjectNode schema = mapper().createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");

        ObjectNode questionProp = properties.putObject("question");
        questionProp.put("type", "string");
        questionProp.put("description", "The question to ask the user");

        ObjectNode questionsProp = properties.putObject("questions");
        questionsProp.put("type", "array");
        questionsProp.put("description", "Multiple questions (1-4)");

        ObjectNode optionsProp = properties.putObject("options");
        optionsProp.put("type", "array");
        optionsProp.put("description", "Answer options (2-4 items)");

        ObjectNode multiSelectProp = properties.putObject("multi_select");
        multiSelectProp.put("type", "boolean");
        multiSelectProp.put("description", "Allow multiple selections");
        multiSelectProp.put("default", false);

        ObjectNode previewProp = properties.putObject("preview");
        previewProp.put("type", "string");
        previewProp.put("description", "Preview content to display with question");

        ObjectNode annotationProp = properties.putObject("annotation");
        annotationProp.put("type", "string");
        annotationProp.put("description", "Additional context or hint for the question");

        ArrayNode required = schema.putArray("required");
        required.add("question");

        return schema;
    }

    private record Question(String text, List<Option> options, boolean multiSelect) {}
    private record Option(String label, String description) {}

    public interface UserInputHandler {
        String handleInput(String prompt, List<Option> options, boolean multiSelect);
    }

    public static class TerminalUserInputHandler implements UserInputHandler {
        @Override
        public String handleInput(String prompt, List<Option> options, boolean multiSelect) {
            return prompt;
        }
    }
}
