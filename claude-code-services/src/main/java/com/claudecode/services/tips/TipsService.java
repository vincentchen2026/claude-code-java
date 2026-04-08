package com.claudecode.services.tips;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TipsService — maintains a registry of tips with shown/unshown state.
 * Stores tips in a Map, tracks which have been shown,
 * getNextTip() returns the first unshown tip.
 */
public class TipsService {

    private static final Logger LOG = LoggerFactory.getLogger(TipsService.class);

    private final Map<String, String> tips = new LinkedHashMap<>();
    private final Set<String> shownTips = new LinkedHashSet<>();

    /** Register a new tip. */
    public void registerTip(String tipId, String content) {
        tips.put(tipId, content);
        LOG.debug("Registered tip: {}", tipId);
    }

    /** Get the next tip to show (first unshown tip). */
    public String getNextTip() {
        for (Map.Entry<String, String> entry : tips.entrySet()) {
            if (!shownTips.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /** Get all available tips. */
    public List<String> getAllTips() {
        return new ArrayList<>(tips.values());
    }

    /** Mark a tip as shown. */
    public void markShown(String tipId) {
        shownTips.add(tipId);
        LOG.debug("Marked tip as shown: {}", tipId);
    }

    /** Check if a tip has been shown. */
    public boolean isShown(String tipId) {
        return shownTips.contains(tipId);
    }
}
