package com.claudecode.services.compact;

import com.claudecode.core.message.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PartialCompactTest {

    @Test
    void partialCompactFromDirection() {
        CompactSummarizer summarizer = (msgs, prompt) -> "Summary of compacted messages";
        CompactService service = new CompactService(
                TokenEstimator.getInstance(), summarizer, true);

        List<Message> messages = createTestMessages(6);

        PartialCompactResult result = service.partialCompactConversation(
                messages, 3, "from", null);

        assertNotNull(result);
        assertEquals("from", result.direction());
        assertEquals(3, result.pivotIndex());
        // Kept messages are the first 3
        assertEquals(3, result.keptMessages().size());
        assertTrue(result.hasCompaction());
    }

    @Test
    void partialCompactUpToDirection() {
        CompactSummarizer summarizer = (msgs, prompt) -> "Summary of compacted messages";
        CompactService service = new CompactService(
                TokenEstimator.getInstance(), summarizer, true);

        List<Message> messages = createTestMessages(6);

        PartialCompactResult result = service.partialCompactConversation(
                messages, 3, "up_to", null);

        assertNotNull(result);
        assertEquals("up_to", result.direction());
        // Kept messages are from index 3 onwards
        assertEquals(3, result.keptMessages().size());
        assertTrue(result.hasCompaction());
    }

    @Test
    void partialCompactInvalidDirection() {
        CompactService service = new CompactService(
                TokenEstimator.getInstance(), null, true);

        List<Message> messages = createTestMessages(4);

        assertThrows(CompactException.class, () ->
                service.partialCompactConversation(messages, 2, "invalid", null));
    }

    @Test
    void partialCompactEmptyMessages() {
        CompactService service = new CompactService(
                TokenEstimator.getInstance(), null, true);

        assertThrows(CompactException.class, () ->
                service.partialCompactConversation(List.of(), 0, "from", null));
    }

    @Test
    void partialCompactOutOfBoundsPivot() {
        CompactService service = new CompactService(
                TokenEstimator.getInstance(), null, true);

        List<Message> messages = createTestMessages(3);

        assertThrows(CompactException.class, () ->
                service.partialCompactConversation(messages, 10, "from", null));
    }

    @Test
    void filterKeptMessagesRemovesProgressAndBoundary() {
        CompactService service = new CompactService();

        List<Message> messages = new ArrayList<>();
        messages.add(new UserMessage("u1", MessageContent.ofText("Hello")));
        messages.add(new SystemMessage("s1", "progress", "info", "Working..."));
        messages.add(new SystemMessage("s2", "compact_boundary", "info", "boundary"));
        messages.add(new UserMessage("u2", MessageContent.ofText("World")));

        List<Message> filtered = service.filterKeptMessages(messages);

        assertEquals(2, filtered.size());
        assertTrue(filtered.stream().allMatch(m -> m instanceof UserMessage));
    }

    @Test
    void filterKeptMessagesRemovesCompactSummary() {
        CompactService service = new CompactService();

        List<Message> messages = new ArrayList<>();
        messages.add(new UserMessage("u1", MessageContent.ofText("Hello")));
        messages.add(new UserMessage("u2", MessageContent.ofText("Summary"),
                false, true, null, MessageOrigin.COMPACT_SUMMARY, null, null));
        messages.add(new UserMessage("u3", MessageContent.ofText("World")));

        List<Message> filtered = service.filterKeptMessages(messages);

        assertEquals(2, filtered.size());
    }

    @Test
    void isFilterableMessageDetectsProgressSubtype() {
        SystemMessage progress = new SystemMessage("s1", "progress", "info", "Working...");
        assertTrue(CompactService.isFilterableMessage(progress));
    }

    @Test
    void isFilterableMessageDetectsCompactBoundary() {
        SystemMessage boundary = new SystemMessage("s1", "compact_boundary", "info", "boundary");
        assertTrue(CompactService.isFilterableMessage(boundary));
    }

    @Test
    void isFilterableMessageAllowsNormalMessages() {
        UserMessage normal = new UserMessage("u1", MessageContent.ofText("Hello"));
        assertFalse(CompactService.isFilterableMessage(normal));
    }

    private List<Message> createTestMessages(int count) {
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            messages.add(new UserMessage("u" + i, MessageContent.ofText("Message " + i)));
        }
        return messages;
    }
}
