package com.claudecode.services;

import com.claudecode.services.notify.OsNotifier;
import com.claudecode.services.suggestion.PromptSuggestionService;
import com.claudecode.services.summary.ToolUseSummaryGenerator;
import com.claudecode.services.system.SleepPreventer;
import com.claudecode.services.tips.TipsService;
import com.claudecode.services.vcr.VcrService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OtherServicesTest {

    @TempDir
    Path tempDir;

    @Test
    void promptSuggestionServiceReturnsSuggestionsForKeywords() {
        var svc = new PromptSuggestionService();

        // Should return suggestions for known keywords
        List<String> testSuggestions = svc.getSuggestions("I need to write a test");
        assertFalse(testSuggestions.isEmpty());

        List<String> bugSuggestions = svc.getSuggestions("there's a bug");
        assertFalse(bugSuggestions.isEmpty());

        // Should return empty for unknown context
        List<String> empty = svc.getSuggestions("xyz123");
        assertTrue(empty.isEmpty());

        // Null/blank returns empty
        assertTrue(svc.getSuggestions("").isEmpty());
        assertTrue(svc.getSuggestions(null).isEmpty());
    }

    @Test
    void promptSuggestionServiceCustomProvider() {
        var svc = new PromptSuggestionService();
        svc.registerProvider("deploy", List.of("Deploy to staging", "Deploy to production"));

        List<String> suggestions = svc.getSuggestions("I want to deploy");
        assertTrue(suggestions.stream().anyMatch(s -> s.contains("Deploy")));
    }

    @Test
    void toolUseSummaryGeneratorTracksInvocations() {
        var gen = new ToolUseSummaryGenerator();

        assertEquals(0, gen.getInvocationCount());
        assertEquals("No tool invocations recorded.", gen.generateSummary());

        gen.recordToolUse("BashTool", 100);
        gen.recordToolUse("FileReadTool", 50);

        assertEquals(2, gen.getInvocationCount());

        String summary = gen.generateSummary();
        assertTrue(summary.contains("BashTool"));
        assertTrue(summary.contains("FileReadTool"));
        assertTrue(summary.contains("Total invocations: 2"));
    }

    @Test
    void toolUseSummaryGeneratorClear() {
        var gen = new ToolUseSummaryGenerator();
        gen.recordToolUse("Test", 10);
        assertEquals(1, gen.getInvocationCount());

        gen.clear();
        assertEquals(0, gen.getInvocationCount());
    }

    @Test
    void osNotifierIsSupported() {
        var notifier = new OsNotifier();
        String os = System.getProperty("os.name", "").toLowerCase();
        boolean expected = os.contains("mac") || os.contains("linux");
        assertEquals(expected, notifier.isSupported());

        // Should not throw
        notifier.notify("Test", "Message");
    }

    @Test
    void sleepPreventerLifecycle() {
        var sp = new SleepPreventer();
        assertFalse(sp.isActive());
        sp.preventSleep();
        assertTrue(sp.isActive());
        sp.allowSleep();
        assertFalse(sp.isActive());
    }

    @Test
    void tipsServiceRegistersAndReturnsNextTip() {
        var tips = new TipsService();

        // No tips registered yet
        assertNull(tips.getNextTip());
        assertTrue(tips.getAllTips().isEmpty());

        // Register tips
        tips.registerTip("tip1", "Use /help for commands");
        tips.registerTip("tip2", "Try compact mode for long sessions");

        assertEquals(2, tips.getAllTips().size());

        // Next tip should be the first unshown
        assertEquals("Use /help for commands", tips.getNextTip());

        // Mark first as shown
        tips.markShown("tip1");
        assertTrue(tips.isShown("tip1"));

        // Next tip should now be the second
        assertEquals("Try compact mode for long sessions", tips.getNextTip());

        // Mark second as shown
        tips.markShown("tip2");

        // No more unshown tips
        assertNull(tips.getNextTip());
    }

    @Test
    void vcrServiceRecordAndReplay() {
        var vcr = new VcrService(tempDir);
        assertFalse(vcr.isRecording());

        vcr.startRecording("test-session");
        assertTrue(vcr.isRecording());
        assertEquals("test-session", vcr.getCurrentSessionId());

        // Record some messages
        vcr.record("message 1");
        vcr.record("message 2");

        vcr.stopRecording();
        assertFalse(vcr.isRecording());

        // Replay
        String content = vcr.replay("test-session");
        assertTrue(content.contains("message 1"));
        assertTrue(content.contains("message 2"));

        // Replay messages as list
        List<String> messages = vcr.replayMessages("test-session");
        assertEquals(2, messages.size());
    }

    @Test
    void vcrServiceReplayNonexistentSession() {
        var vcr = new VcrService(tempDir);
        String result = vcr.replay("nonexistent");
        assertTrue(result.contains("No recording found"));
    }
}
