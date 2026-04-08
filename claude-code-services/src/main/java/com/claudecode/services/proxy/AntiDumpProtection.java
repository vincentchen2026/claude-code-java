package com.claudecode.services.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface for platform-specific anti-dump protection (prctl/ptrace).
 * Stub implementation — actual JNA calls would be added when JNA is available.
 */
public interface AntiDumpProtection {

    /**
     * Attempts to set the process as non-dumpable.
     * Returns true if the operation succeeded or was not applicable.
     */
    boolean setNonDumpable();

    /**
     * Returns the current platform name.
     */
    String platform();

    /**
     * Creates the appropriate implementation for the current platform.
     */
    static AntiDumpProtection create() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("linux")) {
            return new LinuxAntiDump();
        } else if (os.contains("mac")) {
            return new MacOsAntiDump();
        }
        return new NoOpAntiDump();
    }

    /** Linux: would call prctl(PR_SET_DUMPABLE=4, 0) via JNA */
    class LinuxAntiDump implements AntiDumpProtection {
        private static final Logger log = LoggerFactory.getLogger(LinuxAntiDump.class);

        @Override
        public boolean setNonDumpable() {
            log.debug("Linux anti-dump: prctl(PR_SET_DUMPABLE, 0) stub — JNA not linked");
            return true; // Stub: succeed silently
        }

        @Override
        public String platform() { return "linux"; }
    }

    /** macOS: would call ptrace(PT_DENY_ATTACH=31, 0, 0, 0) via JNA */
    class MacOsAntiDump implements AntiDumpProtection {
        private static final Logger log = LoggerFactory.getLogger(MacOsAntiDump.class);

        @Override
        public boolean setNonDumpable() {
            log.debug("macOS anti-dump: ptrace(PT_DENY_ATTACH) stub — JNA not linked");
            return true; // Stub: succeed silently
        }

        @Override
        public String platform() { return "macos"; }
    }

    /** No-op for unsupported platforms */
    class NoOpAntiDump implements AntiDumpProtection {
        @Override
        public boolean setNonDumpable() { return true; }

        @Override
        public String platform() { return "unsupported"; }
    }
}
