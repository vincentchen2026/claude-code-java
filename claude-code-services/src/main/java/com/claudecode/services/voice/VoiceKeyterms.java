package com.claudecode.services.voice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * VoiceKeyterms — keyword detection stub.
 * Since this requires audio hardware, this is a stub with proper interface documentation.
 * In a real implementation, this would detect keywords in an audio stream.
 */
public class VoiceKeyterms {

    private static final Logger LOG = LoggerFactory.getLogger(VoiceKeyterms.class);

    private final List<String> registeredKeywords = new ArrayList<>();

    /**
     * Register keywords to listen for.
     * In a real implementation, these would be loaded into a keyword
     * detection model for real-time audio matching.
     */
    public void registerKeywords(List<String> keywords) {
        registeredKeywords.clear();
        registeredKeywords.addAll(keywords);
        LOG.info("Registered {} keywords for detection", keywords.size());
    }

    /**
     * Check if a keyword was detected in a transcription.
     * Performs simple string matching against registered keywords.
     *
     * @param transcription the text to search for keywords
     * @return the first matched keyword, or null if none found
     */
    public String detectKeyword(String transcription) {
        if (transcription == null || transcription.isBlank()) {
            return null;
        }

        String lower = transcription.toLowerCase();
        for (String keyword : registeredKeywords) {
            if (lower.contains(keyword.toLowerCase())) {
                LOG.debug("Detected keyword '{}' in transcription", keyword);
                return keyword;
            }
        }
        return null;
    }

    /** Get all registered keywords. */
    public List<String> getRegisteredKeywords() {
        return List.copyOf(registeredKeywords);
    }
}
