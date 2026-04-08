package com.claudecode.services.voice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VoiceStreamSTT — streaming speech-to-text integration (stub).
 * Since this requires audio hardware, this is a stub with proper interface documentation.
 * Implementations would integrate with platform-specific audio APIs.
 */
public class VoiceStreamSTT {

    private static final Logger LOG = LoggerFactory.getLogger(VoiceStreamSTT.class);

    private boolean listening = false;

    /**
     * Start listening for voice input.
     * In a real implementation, this would open an audio stream
     * and begin streaming to a speech-to-text service.
     */
    public void startListening() {
        LOG.info("VoiceStreamSTT: started listening (stub - no audio hardware)");
        listening = true;
    }

    /**
     * Stop listening.
     * In a real implementation, this would close the audio stream.
     */
    public void stopListening() {
        LOG.info("VoiceStreamSTT: stopped listening");
        listening = false;
    }

    /** Check if currently listening. */
    public boolean isListening() {
        return listening;
    }

    /**
     * Get the latest transcription result.
     * In a real implementation, this would return the latest
     * transcribed text from the audio stream.
     *
     * @return transcription text, or a stub message if no audio hardware
     */
    public String getTranscription() {
        if (!listening) {
            return "Not listening. Call startListening() first.";
        }
        return "Voice STT: stub - no audio hardware available";
    }
}
