package com.claudecode.services.compact;

import com.claudecode.core.message.Message;

import java.util.List;

/**
 * Interface for summarizing conversation messages during compaction.
 * Implementations call an LLM to generate a summary of the conversation.
 */
public interface CompactSummarizer {

    /**
     * Summarize the given messages using the provided compact prompt.
     *
     * @param messages     the messages to summarize
     * @param compactPrompt the prompt instructing the LLM how to summarize
     * @return the summary text, or a string starting with "prompt is too long" if the prompt exceeds limits
     */
    String summarize(List<Message> messages, String compactPrompt);
}
