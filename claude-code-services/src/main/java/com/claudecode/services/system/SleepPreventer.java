package com.claudecode.services.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SleepPreventer — prevents system sleep during long operations.
 * macOS: uses `caffeinate` subprocess.
 * Linux: uses `systemd-inhibit`.
 */
public class SleepPreventer {

    private static final Logger LOG = LoggerFactory.getLogger(SleepPreventer.class);

    private boolean active = false;
    private Process process;

    /** Prevent system from sleeping. */
    public void preventSleep() {
        if (active) return;

        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("mac")) {
                // caffeinate -i prevents idle sleep
                process = new ProcessBuilder("caffeinate", "-i")
                        .redirectErrorStream(true)
                        .start();
                active = true;
                LOG.info("Sleep prevention started (caffeinate)");
            } else if (os.contains("linux")) {
                // systemd-inhibit blocks sleep while running
                process = new ProcessBuilder(
                        "systemd-inhibit", "--what=idle",
                        "--who=claude-code", "--why=Long operation in progress",
                        "sleep", "infinity")
                        .redirectErrorStream(true)
                        .start();
                active = true;
                LOG.info("Sleep prevention started (systemd-inhibit)");
            } else {
                LOG.debug("Sleep prevention not supported on {}", os);
                active = true; // Mark active even if unsupported
            }
        } catch (Exception e) {
            LOG.warn("Failed to prevent sleep: {}", e.getMessage());
            active = true; // Mark active to track state
        }
    }

    /** Allow system to sleep again. */
    public void allowSleep() {
        if (!active) return;

        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            LOG.info("Sleep prevention stopped");
        }
        process = null;
        active = false;
    }

    /** Check if sleep prevention is active. */
    public boolean isActive() {
        return active;
    }
}
