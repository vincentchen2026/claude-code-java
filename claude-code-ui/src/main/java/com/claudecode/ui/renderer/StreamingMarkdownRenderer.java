package com.claudecode.ui.renderer;

import com.claudecode.ui.MarkdownRenderer;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.List;

/**
 * Streaming markdown renderer for incremental rendering.
 * Task 59.2: Streaming Markdown incremental parsing
 *
 * Features:
 * - Stable prefix detection (reuse parsed structure)
 * - Unstable suffix highlighting
 * - Partial re-render optimization
 */
public class StreamingMarkdownRenderer {

    private final MarkdownRenderer fullRenderer;
    private final Cache<String, String> renderCache;

    private String lastStablePrefix = "";
    private String lastContent = "";

    public StreamingMarkdownRenderer(MarkdownRenderer fullRenderer) {
        this.fullRenderer = fullRenderer;
        this.renderCache = Caffeine.newBuilder().maximumSize(100).build();
    }

    public StreamingMarkdownRenderer() {
        this.fullRenderer = new MarkdownRenderer();
        this.renderCache = Caffeine.newBuilder().maximumSize(100).build();
    }

    /**
     * Render new content incrementally, highlighting only the changed portion.
     *
     * @param newContent the new content to render
     * @return rendered string with incremental highlighting
     */
    public RenderedOutput renderIncremental(String newContent) {
        if (newContent == null) {
            newContent = "";
        }

        if (newContent.equals(lastContent)) {
            return new RenderedOutput(fullRenderer.render(newContent), "", false);
        }

        String stablePrefix = findStablePrefix(lastContent, newContent);
        String unstableSuffix = newContent.substring(stablePrefix.length());

        String stableRendered = renderCache.getIfPresent(stablePrefix);
        if (stableRendered == null) {
            stableRendered = fullRenderer.render(stablePrefix);
            renderCache.put(stablePrefix, stableRendered);
        }

        String unstableRendered = fullRenderer.render(unstableSuffix);

        lastStablePrefix = stablePrefix;
        lastContent = newContent;

        return new RenderedOutput(stableRendered, unstableRendered, true);
    }

    /**
     * Find the common prefix between old and new content.
     */
    private String findStablePrefix(String oldContent, String newContent) {
        int minLength = Math.min(oldContent.length(), newContent.length());
        int commonLength = 0;

        for (int i = 0; i < minLength; i++) {
            if (oldContent.charAt(i) == newContent.charAt(i)) {
                commonLength++;
            } else {
                break;
            }
        }

        return newContent.substring(0, commonLength);
    }

    /**
     * Reset the renderer state.
     */
    public void reset() {
        lastStablePrefix = "";
        lastContent = "";
        renderCache.invalidateAll();
    }

    /**
     * Get the last rendered content.
     */
    public String getLastContent() {
        return lastContent;
    }

    public record RenderedOutput(String stablePrefix, String unstableSuffix, boolean hasChange) {}
}