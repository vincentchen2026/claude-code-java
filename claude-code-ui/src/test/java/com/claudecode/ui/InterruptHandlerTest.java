package com.claudecode.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for InterruptHandler — Ctrl+C behavior in different REPL states.
 */
class InterruptHandlerTest {

    private InterruptHandler handler;

    @BeforeEach
    void setUp() {
        handler = new InterruptHandler();
    }

    @Test
    void initialState_isInput() {
        assertEquals(InterruptHandler.ReplState.INPUT, handler.getState());
        assertFalse(handler.isExitRequested());
    }

    @Test
    void handleInterrupt_duringInput_clearsLine() {
        handler.setState(InterruptHandler.ReplState.INPUT);
        InterruptHandler.InterruptAction action = handler.handleInterrupt();
        assertEquals(InterruptHandler.InterruptAction.CLEAR_LINE, action);
        assertFalse(handler.isExitRequested());
    }

    @Test
    void handleInterrupt_duringApiCall_cancelsApi() {
        AtomicBoolean interrupted = new AtomicBoolean(false);
        handler.setApiInterruptAction(() -> interrupted.set(true));
        handler.setState(InterruptHandler.ReplState.API_CALL);

        InterruptHandler.InterruptAction action = handler.handleInterrupt();
        assertEquals(InterruptHandler.InterruptAction.CANCEL_API, action);
        assertTrue(interrupted.get());
    }

    @Test
    void handleInterrupt_duringApiCall_withoutAction_stillReturnsCancelApi() {
        handler.setState(InterruptHandler.ReplState.API_CALL);
        InterruptHandler.InterruptAction action = handler.handleInterrupt();
        assertEquals(InterruptHandler.InterruptAction.CANCEL_API, action);
    }

    @Test
    void doubleCtrlC_requestsExit() {
        handler.setState(InterruptHandler.ReplState.INPUT);

        // First Ctrl+C
        handler.handleInterrupt();
        assertFalse(handler.isExitRequested());

        // Second Ctrl+C immediately (within threshold)
        InterruptHandler.InterruptAction action = handler.handleInterrupt();
        assertEquals(InterruptHandler.InterruptAction.EXIT, action);
        assertTrue(handler.isExitRequested());
    }

    @Test
    void doubleCtrlC_duringApiCall_requestsExit() {
        handler.setState(InterruptHandler.ReplState.API_CALL);

        // First Ctrl+C cancels API
        handler.handleInterrupt();

        // Second Ctrl+C immediately exits
        InterruptHandler.InterruptAction action = handler.handleInterrupt();
        assertEquals(InterruptHandler.InterruptAction.EXIT, action);
        assertTrue(handler.isExitRequested());
    }

    @Test
    void resetExitRequest_clearsState() {
        // Trigger double Ctrl+C
        handler.handleInterrupt();
        handler.handleInterrupt();
        assertTrue(handler.isExitRequested());

        handler.resetExitRequest();
        assertFalse(handler.isExitRequested());
    }

    @Test
    void setState_changesState() {
        handler.setState(InterruptHandler.ReplState.API_CALL);
        assertEquals(InterruptHandler.ReplState.API_CALL, handler.getState());

        handler.setState(InterruptHandler.ReplState.INPUT);
        assertEquals(InterruptHandler.ReplState.INPUT, handler.getState());
    }

    @Test
    void singleCtrlC_afterDelay_doesNotExit() throws InterruptedException {
        handler.setState(InterruptHandler.ReplState.INPUT);

        // First Ctrl+C
        handler.handleInterrupt();
        assertFalse(handler.isExitRequested());

        // Wait longer than the threshold (1 second)
        Thread.sleep(1100);

        // Second Ctrl+C after delay — should NOT exit
        InterruptHandler.InterruptAction action = handler.handleInterrupt();
        assertEquals(InterruptHandler.InterruptAction.CLEAR_LINE, action);
        assertFalse(handler.isExitRequested());
    }
}
