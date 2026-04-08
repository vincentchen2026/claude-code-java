package com.claudecode.services.dream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;

public class DreamLock {

    private static final Logger log = LoggerFactory.getLogger(DreamLock.class);
    private static final String LOCK_FILE = ".dream.lock";

    private final Path lockPath;
    private FileChannel channel;
    private FileLock lock;

    public DreamLock(Path workDir) {
        this.lockPath = workDir.resolve(LOCK_FILE);
    }

    public boolean acquire() {
        try {
            Files.createDirectories(lockPath.getParent());
            channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
            lock = channel.tryLock();
            
            if (lock != null) {
                log.debug("Acquired dream lock: {}", lockPath);
                return true;
            } else {
                log.debug("Failed to acquire dream lock - already held: {}", lockPath);
                closeChannel();
                return false;
            }
        } catch (IOException e) {
            log.error("Error acquiring dream lock", e);
            closeChannel();
            return false;
        }
    }

    public void release() {
        try {
            if (lock != null && lock.isValid()) {
                lock.release();
                log.debug("Released dream lock: {}", lockPath);
            }
            closeChannel();
        } catch (IOException e) {
            log.error("Error releasing dream lock", e);
        }
    }

    private void closeChannel() {
        try {
            if (channel != null) {
                channel.close();
                channel = null;
            }
        } catch (IOException e) {
            log.error("Error closing channel", e);
        }
    }

    public boolean isHeld() {
        return lock != null && lock.isValid();
    }

    public Path getLockPath() {
        return lockPath;
    }

    public void forceRelease() {
        try {
            if (Files.exists(lockPath)) {
                Files.delete(lockPath);
                log.info("Force released dream lock: {}", lockPath);
            }
            closeChannel();
        } catch (IOException e) {
            log.error("Error force releasing dream lock", e);
        }
    }
}