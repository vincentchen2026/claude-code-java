package com.claudecode.bridge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BridgeComponentsTest {

    // --- BridgePointer tests ---

    @Test
    void pointerStartsAtZero(@TempDir Path tempDir) {
        var pointer = new BridgePointer(tempDir);
        assertEquals(0, pointer.getOffset());
    }

    @Test
    void pointerAdvancesAndPersists(@TempDir Path tempDir) {
        var pointer = new BridgePointer(tempDir);
        pointer.advance(100);
        assertEquals(100, pointer.getOffset());

        // Create new instance to verify persistence
        var pointer2 = new BridgePointer(tempDir);
        assertEquals(100, pointer2.getOffset());
    }

    @Test
    void pointerCannotGoBackwards(@TempDir Path tempDir) {
        var pointer = new BridgePointer(tempDir);
        pointer.advance(100);
        assertThrows(IllegalArgumentException.class, () -> pointer.advance(50));
    }

    @Test
    void pointerReset(@TempDir Path tempDir) {
        var pointer = new BridgePointer(tempDir);
        pointer.advance(100);
        pointer.reset();
        assertEquals(0, pointer.getOffset());
    }

    // --- CapacityWake tests ---

    @Test
    void capacityWakeSignalAndClear() {
        var wake = new CapacityWake();
        assertFalse(wake.isSignaled());

        wake.signal();
        assertTrue(wake.isSignaled());

        assertTrue(wake.checkAndClear());
        assertFalse(wake.isSignaled());
    }

    // --- PeerSessionDedup tests ---

    @Test
    void sessionDedupClaimAndRelease() {
        var dedup = new PeerSessionDedup();
        assertTrue(dedup.tryClaimSession("session_1"));
        assertFalse(dedup.tryClaimSession("session_1")); // duplicate
        assertTrue(dedup.isSessionActive("session_1"));
        assertEquals(1, dedup.activeCount());

        assertTrue(dedup.releaseSession("session_1"));
        assertFalse(dedup.isSessionActive("session_1"));
    }

    // --- WebhookSanitizer tests ---

    @Test
    void sanitizerRemovesScriptTags() {
        String input = "Hello <script>alert('xss')</script> World";
        assertEquals("Hello  World", WebhookSanitizer.sanitize(input));
    }

    @Test
    void sanitizerRemovesHtmlTags() {
        String input = "Hello <b>bold</b> World";
        assertEquals("Hello bold World", WebhookSanitizer.sanitize(input));
    }

    @Test
    void sanitizerHandlesNull() {
        assertEquals("", WebhookSanitizer.sanitize(null));
    }

    @Test
    void sanitizerValidation() {
        assertTrue(WebhookSanitizer.isValid("valid payload"));
        assertFalse(WebhookSanitizer.isValid(null));
        assertFalse(WebhookSanitizer.isValid(""));
    }

    // --- WorkSecretDecoder tests ---

    @Test
    void decodeValidBase64() {
        String encoded = WorkSecretDecoder.encode("my-secret");
        var decoded = WorkSecretDecoder.decode(encoded);
        assertTrue(decoded.isPresent());
        assertEquals("my-secret", decoded.get());
    }

    @Test
    void decodeInvalidBase64() {
        assertTrue(WorkSecretDecoder.decode("not-valid-base64!!!").isEmpty());
    }

    @Test
    void decodeNullOrBlank() {
        assertTrue(WorkSecretDecoder.decode(null).isEmpty());
        assertTrue(WorkSecretDecoder.decode("").isEmpty());
    }

    // --- SessionIdCompat tests ---

    @Test
    void detectSessionFormat() {
        assertEquals(SessionIdCompat.SessionFormat.SESSION,
            SessionIdCompat.detectFormat("session_abc123"));
        assertEquals(SessionIdCompat.SessionFormat.CSE,
            SessionIdCompat.detectFormat("cse_xyz789"));
        assertEquals(SessionIdCompat.SessionFormat.UNKNOWN,
            SessionIdCompat.detectFormat("random-id"));
    }

    @Test
    void sessionIdValidation() {
        assertTrue(SessionIdCompat.isValid("session_abc"));
        assertTrue(SessionIdCompat.isValid("cse_abc"));
        assertFalse(SessionIdCompat.isValid("invalid"));
        assertFalse(SessionIdCompat.isValid(null));
    }

    @Test
    void sessionIdNormalization() {
        assertEquals("session_abc", SessionIdCompat.normalize("session_abc"));
        assertEquals("session_xyz", SessionIdCompat.normalize("cse_xyz"));
    }

    // --- MessageFlushGate tests ---

    @Test
    void flushGateOpenByDefault() {
        var gate = new MessageFlushGate();
        assertTrue(gate.isOpen());
    }

    @Test
    void flushGateBlocksWhenClosed() {
        var gate = new MessageFlushGate();
        gate.close();
        assertFalse(gate.tryProcess("msg-1"));
    }

    @Test
    void flushGateDeduplicates() {
        var gate = new MessageFlushGate();
        assertTrue(gate.tryProcess("msg-1"));
        assertFalse(gate.tryProcess("msg-1")); // duplicate
        assertTrue(gate.isDuplicate("msg-1"));
        assertEquals(1, gate.processedCount());
    }

    // --- JwtTokenRefresher tests ---

    @Test
    void tokenRefresherGetsInitialToken() {
        var refresher = new JwtTokenRefresher(() -> "test-token");
        refresher.start();
        assertEquals("test-token", refresher.getCurrentToken());
        refresher.close();
    }

    // --- DirectConnectManager tests ---

    @Test
    void directConnectManagerCrud() {
        var manager = new DirectConnectManager();
        assertEquals(0, manager.connectionCount());

        manager.registerConnection("conn-1", "ws://localhost:9090");
        assertEquals(1, manager.connectionCount());
        assertTrue(manager.getConnection("conn-1").isPresent());

        assertTrue(manager.removeConnection("conn-1"));
        assertEquals(0, manager.connectionCount());
        assertFalse(manager.removeConnection("conn-1"));
    }
}
