package com.claudecode.services.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * OsNotifier — cross-platform desktop notifications.
 * macOS: `osascript -e 'display notification'`.
 * Linux: `notify-send`.
 */
public class OsNotifier {

    private static final Logger LOG = LoggerFactory.getLogger(OsNotifier.class);

    /** Send a desktop notification. */
    public void notify(String title, String message) {
        String os = System.getProperty("os.name", "").toLowerCase();

        try {
            if (os.contains("mac")) {
                String script = "display notification \""
                        + escapeAppleScript(message)
                        + "\" with title \""
                        + escapeAppleScript(title) + "\"";
                new ProcessBuilder("osascript", "-e", script)
                        .start()
                        .waitFor(5, TimeUnit.SECONDS);
                LOG.debug("Sent macOS notification: {}", title);
            } else if (os.contains("linux")) {
                new ProcessBuilder("notify-send", title, message)
                        .start()
                        .waitFor(5, TimeUnit.SECONDS);
                LOG.debug("Sent Linux notification: {}", title);
            } else {
                LOG.debug("Desktop notifications not supported on {}", os);
            }
        } catch (Exception e) {
            LOG.warn("Failed to send notification: {}", e.getMessage());
        }
    }

    /** Check if notifications are supported on this platform. */
    public boolean isSupported() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("mac") || os.contains("linux");
    }

    private static String escapeAppleScript(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
