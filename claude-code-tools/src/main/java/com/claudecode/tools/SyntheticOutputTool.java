package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SyntheticOutputTool — return structured JSON output with optional schema validation.
 * Input: any JSON with optional schema validation. Used for structured output mode.
 *
 * Task 55.5 enhancements:
 * - JSON Schema validation
 * - Validation error reporting
 * - Output formatting options
 */
public class SyntheticOutputTool extends Tool<JsonNode, String> {

    private final SchemaValidator schemaValidator;
    private final boolean strictValidation;

    public SyntheticOutputTool() {
        this(null, false);
    }

    public SyntheticOutputTool(SchemaValidator schemaValidator) {
        this(schemaValidator, false);
    }

    public SyntheticOutputTool(SchemaValidator schemaValidator, boolean strictValidation) {
        this.schemaValidator = schemaValidator;
        this.strictValidation = strictValidation;
    }

    @Override
    public String name() { return "SyntheticOutput"; }

    @Override
    public String description() { return "Generate synthetic output for tool results with optional schema validation"; }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = createObjectSchema();
        ObjectNode props = (ObjectNode) schema.get("properties");

        ObjectNode schemaProp = mapper().createObjectNode();
        schemaProp.put("type", "string");
        schemaProp.put("description", "JSON Schema to validate output against");
        props.set("schema", schemaProp);

        ObjectNode validateProp = mapper().createObjectNode();
        validateProp.put("type", "boolean");
        validateProp.put("description", "Whether to validate against schema");
        validateProp.put("default", false);
        props.set("validate", validateProp);

        ObjectNode formatProp = mapper().createObjectNode();
        formatProp.put("type", "string");
        formatProp.put("description", "Output format: 'json' (default), 'pretty', 'compact'");
        props.set("format", formatProp);

        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        if (input == null || input.isEmpty()) {
            return "{}";
        }

        boolean validate = input.has("validate") && input.get("validate").asBoolean(false);
        String schemaStr = input.has("schema") ? input.get("schema").asText("") : null;
        String format = input.has("format") ? input.get("format").asText("json") : "json";

        JsonNode data = extractData(input);

        if (validate && schemaStr != null && !schemaStr.isEmpty()) {
            ValidationResult result = validateOutput(data, schemaStr);
            if (!result.isValid()) {
                if (strictValidation) {
                    return "Error: validation failed - " + result.errors().stream()
                        .reduce((a, b) -> a + "; " + b)
                        .orElse("unknown error");
                }
                return formatOutput(data, format) + "\n[Validation warnings: " +
                       result.errors().stream()
                           .reduce((a, b) -> a + "; " + b)
                           .orElse("") + "]";
            }
        }

        return formatOutput(data, format);
    }

    private JsonNode extractData(JsonNode input) {
        if (input.has("data")) {
            return input.get("data");
        }
        if (input.has("output")) {
            return input.get("output");
        }
        return input;
    }

    private ValidationResult validateOutput(JsonNode data, String schemaStr) {
        if (schemaValidator == null) {
            return new ValidationResult(true, List.of());
        }
        return schemaValidator.validate(data, schemaStr);
    }

    private String formatOutput(JsonNode data, String format) {
        return switch (format.toLowerCase()) {
            case "pretty" -> formatPretty(data);
            case "compact" -> formatCompact(data);
            default -> data.toString();
        };
    }

    private String formatPretty(JsonNode data) {
        try {
            return mapper().writerWithDefaultPrettyPrinter().writeValueAsString(data);
        } catch (Exception e) {
            return data.toString();
        }
    }

    private String formatCompact(JsonNode data) {
        return data.toString();
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean isConcurrencySafe() {
        return true;
    }

    public record ValidationResult(boolean isValid, List<String> errors) {}

    @FunctionalInterface
    public interface SchemaValidator {
        ValidationResult validate(JsonNode data, String schema);
    }
}
