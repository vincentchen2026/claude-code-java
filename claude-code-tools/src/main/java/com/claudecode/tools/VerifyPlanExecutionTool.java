package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashSet;
import java.util.Set;

/**
 * VerifyPlanExecutionTool — verifies plan execution against actual results.
 * Task 55.2
 *
 * Performs keyword-based verification to check plan completion.
 * Full LLM-based verification is available through the verification service.
 */
public class VerifyPlanExecutionTool extends Tool<JsonNode, String> {

    @Override
    public String name() { return "VerifyPlanExecution"; }

    @Override
    public String description() {
        return "Verify that a plan was executed correctly by comparing the plan with actual results.";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = createObjectSchema();
        ObjectNode props = (ObjectNode) schema.get("properties");

        ObjectNode planSchema = mapper().createObjectNode();
        planSchema.put("type", "string");
        planSchema.put("description", "The original plan text to verify against");
        props.set("plan", planSchema);

        ObjectNode resultSchema = mapper().createObjectNode();
        resultSchema.put("type", "string");
        resultSchema.put("description", "The actual result or description of what was done");
        props.set("actual_result", resultSchema);

        schema.set("required", mapper().createArrayNode().add("plan").add("actual_result"));
        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String plan = input.has("plan") ? input.get("plan").asText() : null;
        String actualResult = input.has("actual_result") ? input.get("actual_result").asText() : null;

        if (plan == null || plan.isBlank()) {
            return "Error: plan is required.";
        }
        if (actualResult == null || actualResult.isBlank()) {
            return "Error: actual_result is required.";
        }

        Set<String> planKeywords = extractKeywords(plan);
        Set<String> resultKeywords = extractKeywords(actualResult);

        int matchedKeywords = 0;
        for (String keyword : planKeywords) {
            if (resultKeywords.contains(keyword)) {
                matchedKeywords++;
            }
        }

        double coverage = planKeywords.isEmpty() ? 0.0 : (double) matchedKeywords / planKeywords.size() * 100;

        StringBuilder sb = new StringBuilder();
        sb.append("Plan Execution Verification\n");
        sb.append("============================\n\n");

        sb.append(String.format("Plan keywords: %d\n", planKeywords.size()));
        sb.append(String.format("Matched keywords: %d\n", matchedKeywords));
        sb.append(String.format("Coverage: %.1f%%\n\n", coverage));

        if (coverage >= 80) {
            sb.append("Status: ✓ COMPLETE\n");
            sb.append("The plan appears to have been executed successfully.\n");
        } else if (coverage >= 50) {
            sb.append("Status: ⚠ PARTIAL\n");
            sb.append("Some aspects of the plan may not have been completed.\n");
        } else {
            sb.append("Status: ✗ INCOMPLETE\n");
            sb.append("The plan execution appears incomplete.\n");
        }

        if (coverage < 100) {
            Set<String> missing = new HashSet<>(planKeywords);
            missing.removeAll(resultKeywords);
            if (!missing.isEmpty() && missing.size() <= 5) {
                sb.append("\nMissing keywords: ").append(String.join(", ", missing)).append("\n");
            }
        }

        return sb.toString();
    }

    private Set<String> extractKeywords(String text) {
        Set<String> keywords = new HashSet<>();
        if (text == null) return keywords;

        String[] words = text.toLowerCase().split("[\\s\\p{Punct}]+");
        Set<String> stopWords = Set.of(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "is", "was", "are", "were", "be", "been",
            "being", "have", "has", "had", "do", "does", "did", "will", "would",
            "could", "should", "may", "might", "must", "shall", "can", "this",
            "that", "these", "those", "i", "you", "he", "she", "it", "we", "they"
        );

        for (String word : words) {
            if (word.length() > 3 && !stopWords.contains(word)) {
                keywords.add(word);
            }
        }
        return keywords;
    }

    @Override
    public boolean isReadOnly() { return true; }

    @Override
    public boolean isConcurrencySafe() { return true; }
}
