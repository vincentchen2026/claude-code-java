package com.claudecode.services.voice;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VoiceIntegrationTest {

    @Test
    void voiceStreamSTTLifecycle() {
        var stt = new VoiceStreamSTT();
        assertFalse(stt.isListening());
        stt.startListening();
        assertTrue(stt.isListening());

        // Should return stub message while listening
        String transcription = stt.getTranscription();
        assertNotNull(transcription);
        assertFalse(transcription.isBlank());

        stt.stopListening();
        assertFalse(stt.isListening());
    }

    @Test
    void voiceStreamSTTNotListeningMessage() {
        var stt = new VoiceStreamSTT();
        String result = stt.getTranscription();
        assertTrue(result.contains("Not listening"));
    }

    @Test
    void voiceKeytermsDetectsRegisteredKeywords() {
        var keyterms = new VoiceKeyterms();
        assertTrue(keyterms.getRegisteredKeywords().isEmpty());

        keyterms.registerKeywords(List.of("stop", "cancel", "help"));
        assertEquals(3, keyterms.getRegisteredKeywords().size());

        // Should detect keyword in transcription
        assertEquals("stop", keyterms.detectKeyword("please stop now"));
        assertEquals("help", keyterms.detectKeyword("I need help"));

        // Should not detect unregistered keywords
        assertNull(keyterms.detectKeyword("hello world"));

        // Null/blank returns null
        assertNull(keyterms.detectKeyword(null));
        assertNull(keyterms.detectKeyword(""));
    }
}
