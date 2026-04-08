package com.claudecode.services.dream;

import java.util.List;

public class ConsolidationPromptGenerator {

    private static final String DEFAULT_CONSOLIDATION_TEMPLATE = """
        You are consolidating a long-running conversation session.
        
        ## Conversation History Summary
        %s
        
        ## Recent Context
        The last few messages were about:
        %s
        
        ## Task
        Please provide a brief summary of the current state of the conversation,
        key decisions made, and what remains to be done.
        """;

    public String generateConsolidationPrompt(List<String> summaries, String recentContext) {
        String historySummary = joinSummaries(summaries);
        return String.format(DEFAULT_CONSOLIDATION_TEMPLATE, historySummary, recentContext);
    }

    public String generateMinimalPrompt(String recentContext) {
        return """
            Consolidate the conversation state.
            
            Recent context:
            %s
            
            Provide a brief summary of what was accomplished and what remains.
            """.formatted(recentContext);
    }

    public String generateCustomPrompt(List<String> summaries, String recentContext, String customInstructions) {
        String historySummary = joinSummaries(summaries);
        return """
            %s
            
            ## History Summary
            %s
            
            ## Recent Context
            %s
            
            ## Custom Instructions
            %s
            """.formatted(
                getHeader(),
                historySummary,
                recentContext,
                customInstructions
            );
    }

    private String getHeader() {
        return """
            Consolidation Prompt
            ===================
            """;
    }

    private String joinSummaries(List<String> summaries) {
        if (summaries == null || summaries.isEmpty()) {
            return "(No prior summaries available)";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < summaries.size(); i++) {
            sb.append(i + 1).append(". ").append(summaries.get(i)).append("\n");
        }
        return sb.toString();
    }

    public PromptTemplate getDefaultTemplate() {
        return new PromptTemplate("default", DEFAULT_CONSOLIDATION_TEMPLATE, List.of("summaries", "recentContext"));
    }

    public record PromptTemplate(
        String name,
        String template,
        List<String> requiredFields
    ) {}
}