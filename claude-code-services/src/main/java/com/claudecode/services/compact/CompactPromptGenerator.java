package com.claudecode.services.compact;

import com.claudecode.core.message.Message;

import java.util.List;

public class CompactPromptGenerator {

    private static final String DEFAULT_TEMPLATE = """
        The conversation has been compacted due to length.
        
        Previous summary:
        %s
        
        Please continue the conversation from this point.
        """;

    public String generateResumePrompt(List<Message> summaryMessages) {
        String summary = extractSummaryText(summaryMessages);
        return String.format(DEFAULT_TEMPLATE, summary);
    }

    public String generateCustomPrompt(List<Message> summaryMessages, String context) {
        String summary = extractSummaryText(summaryMessages);
        return """
            %s
            
            Previous summary:
            %s
            
            Please continue the conversation from this point.
            """.formatted(context, summary);
    }

    public String generateCondensedPrompt(List<Message> summaryMessages, int maxTokens) {
        String fullSummary = extractSummaryText(summaryMessages);
        if (fullSummary.length() <= maxTokens * 4) {
            return generateResumePrompt(summaryMessages);
        }
        
        String truncated = fullSummary.substring(0, maxTokens * 4) + "...";
        return String.format(DEFAULT_TEMPLATE, truncated);
    }

    private String extractSummaryText(List<Message> summaryMessages) {
        if (summaryMessages == null || summaryMessages.isEmpty()) {
            return "(No summary available)";
        }

        StringBuilder sb = new StringBuilder();
        for (Message msg : summaryMessages) {
            sb.append(msg.type());
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    public PromptTemplate getDefaultTemplate() {
        return new PromptTemplate("default", DEFAULT_TEMPLATE, List.of("summary"));
    }

    public record PromptTemplate(
        String name,
        String template,
        List<String> requiredFields
    ) {}
}