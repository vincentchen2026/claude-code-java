package com.claudecode.core.state;

import java.util.Map;

/**
 * Placeholder record for attribution state.
 * Tracks attribution information for content generated during the session.
 *
 * @param attributions map of attribution entries keyed by identifier
 */
public record AttributionState(
    Map<String, Object> attributions
) {
    /** Empty attribution state. */
    public static final AttributionState EMPTY =
        new AttributionState(Map.of());
}
