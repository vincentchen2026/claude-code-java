package com.claudecode.ui.vim;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JLine3 KeyMap integration for Vim mode.
 * Binds Vim key sequences to the VimStateMachine.
 * 
 * This is a lightweight adapter — actual JLine KeyMap binding
 * happens at the InputReader level using this as a helper.
 */
public class VimKeyMapBinder {

    private static final Logger LOG = LoggerFactory.getLogger(VimKeyMapBinder.class);

    private final VimStateMachine stateMachine;
    private boolean enabled;

    public VimKeyMapBinder(VimStateMachine stateMachine) {
        this.stateMachine = stateMachine;
        this.enabled = false;
    }

    /**
     * Enable or disable Vim mode.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            stateMachine.reset();
        }
        LOG.debug("Vim mode {}", enabled ? "enabled" : "disabled");
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Process a key through the Vim state machine if enabled.
     * Returns null if Vim mode is disabled (key should be handled normally).
     */
    public VimAction handleKey(char key) {
        if (!enabled) {
            return null;
        }
        return stateMachine.processKey(key);
    }

    public VimStateMachine getStateMachine() {
        return stateMachine;
    }
}
