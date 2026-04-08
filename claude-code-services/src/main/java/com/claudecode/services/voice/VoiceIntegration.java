package com.claudecode.services.voice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class VoiceIntegration {

    private static final Logger LOG = LoggerFactory.getLogger(VoiceIntegration.class);

    private final VoiceStreamSTT stt;
    private final VoiceKeyterms keyterms;
    private final ExecutorService executor;
    private final List<Consumer<VoiceEvent>> eventListeners;
    private final AtomicBoolean active;

    public VoiceIntegration() {
        this.stt = new VoiceStreamSTT();
        this.keyterms = new VoiceKeyterms();
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.eventListeners = new CopyOnWriteArrayList<>();
        this.active = new AtomicBoolean(false);
    }

    public VoiceIntegration(VoiceStreamSTT stt, VoiceKeyterms keyterms) {
        this.stt = stt;
        this.keyterms = keyterms;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.eventListeners = new CopyOnWriteArrayList<>();
        this.active = new AtomicBoolean(false);
    }

    public void registerKeywords(List<String> keywords) {
        keyterms.registerKeywords(keywords);
    }

    public void startListening() {
        if (active.compareAndSet(false, true)) {
            stt.startListening();
            LOG.info("VoiceIntegration: started");
        }
    }

    public void stopListening() {
        if (active.compareAndSet(true, false)) {
            stt.stopListening();
            LOG.info("VoiceIntegration: stopped");
        }
    }

    public boolean isListening() {
        return stt.isListening();
    }

    public String processTranscription() {
        if (!active.get()) {
            return null;
        }
        String transcription = stt.getTranscription();
        if (transcription != null && !transcription.isBlank()) {
            String keyword = keyterms.detectKeyword(transcription);
            VoiceEvent event = new VoiceEvent(transcription, keyword, System.currentTimeMillis());
            notifyListeners(event);
            if (keyword != null) {
                LOG.info("VoiceIntegration: keyword '{}' detected in transcription", keyword);
            }
        }
        return transcription;
    }

    public void startContinuousListening(long intervalMs) {
        startListening();
        executor.submit(() -> {
            while (active.get()) {
                try {
                    String transcription = processTranscription();
                    if (transcription != null) {
                        LOG.debug("Continuous transcription: {}", transcription);
                    }
                    Thread.sleep(intervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    public void addListener(Consumer<VoiceEvent> listener) {
        if (listener != null) {
            eventListeners.add(listener);
        }
    }

    public void removeListener(Consumer<VoiceEvent> listener) {
        eventListeners.remove(listener);
    }

    private void notifyListeners(VoiceEvent event) {
        for (Consumer<VoiceEvent> listener : eventListeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                LOG.warn("VoiceIntegration: listener threw exception", e);
            }
        }
    }

    public void shutdown() {
        stopListening();
        executor.shutdown();
        eventListeners.clear();
    }

    public VoiceStreamSTT getStt() {
        return stt;
    }

    public VoiceKeyterms getKeyterms() {
        return keyterms;
    }

    public record VoiceEvent(
        String transcription,
        String detectedKeyword,
        long timestamp
    ) {}
}
