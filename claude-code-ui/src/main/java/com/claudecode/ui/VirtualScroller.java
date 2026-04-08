package com.claudecode.ui;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Virtual scroller for large message lists.
 * Task 66 enhancements:
 * - 66.1: Dynamic height measurement (terminal-width based line estimation)
 * - 66.2: Sticky prompt header
 * - 66.3: Full-text search with incremental highlighting (n/N navigation)
 * - 66.4: Two-phase jump (scrollToIndex → precise)
 * - 66.5: Overscan management / incremental key diff / search index warming
 * - 66.6: Cursor navigation (shift+↑/↓)
 */
public class VirtualScroller<T> {

    private final int viewportSize;
    private int topIndex;
    private List<T> items;
    private final int terminalWidth;

    // Task 66.5: Overscan (render extra items above/below viewport for smooth scrolling)
    private final int overscan;

    // Task 66.1: Dynamic height cache (item index → estimated line count)
    private final Map<Integer, Integer> heightCache = new ConcurrentHashMap<>();

    // Task 66.2: Sticky prompt header
    private volatile boolean stickyHeaderEnabled = true;
    private volatile T stickyHeaderItem;

    // Task 66.3: Full-text search
    private volatile String searchQuery;
    private volatile List<Integer> searchResults;
    private volatile int searchResultIndex = -1;
    private final Map<Integer, String> textIndex = new ConcurrentHashMap<>();

    // Task 66.6: Cursor navigation
    private volatile int cursorIndex = -1;

    // Task 66.5: Incremental key array for diff-based updates
    private volatile List<Object> lastKeyArray = List.of();

    /**
     * Create a virtual scroller with the given viewport size.
     *
     * @param viewportSize maximum number of items visible at once
     */
    public VirtualScroller(int viewportSize) {
        this(viewportSize, 80, 0);
    }

    /**
     * Create a virtual scroller with custom settings.
     *
     * @param viewportSize  maximum number of items visible at once
     * @param terminalWidth terminal width in characters (for height estimation)
     * @param overscan      number of extra items to render above/below viewport
     */
    public VirtualScroller(int viewportSize, int terminalWidth, int overscan) {
        if (viewportSize <= 0) {
            throw new IllegalArgumentException("viewportSize must be positive");
        }
        this.viewportSize = viewportSize;
        this.terminalWidth = terminalWidth;
        this.overscan = overscan;
        this.topIndex = 0;
        this.items = List.of();
    }

    /**
     * Set the full list of items.
     * Automatically scrolls to the bottom (most recent items visible).
     * Task 66.5: Also warms the search index.
     */
    public void setItems(List<T> items) {
        this.items = items != null ? items : List.of();
        scrollToBottom();

        // Task 66.5: Warm search index
        warmSearchIndex();
    }

    /**
     * Task 66.5: Incremental update — only process changed items.
     * Uses key-based diff to minimize work.
     */
    public void updateItems(List<T> items, java.util.function.Function<T, Object> keyFn) {
        List<Object> newKeys = items.stream().map(keyFn).toList();

        // Check if items actually changed
        if (newKeys.equals(lastKeyArray)) {
            return; // No change, skip update
        }

        this.items = items != null ? items : List.of();
        this.lastKeyArray = newKeys;

        // Task 66.1: Invalidate height cache for changed items
        heightCache.clear();

        // Task 66.5: Warm search index incrementally
        warmSearchIndex();

        scrollToBottom();
    }

    /**
     * Get the items currently visible in the viewport.
     * Task 66.5: Includes overscan items for smooth scrolling.
     */
    public List<T> getVisibleItems() {
        if (items.isEmpty()) {
            return List.of();
        }

        int effectiveViewport = viewportSize + (overscan * 2);
        int fromIndex = Math.max(0, Math.min(topIndex - overscan, items.size()));
        int toIndex = Math.min(fromIndex + effectiveViewport, items.size());

        if (fromIndex >= toIndex) {
            return List.of();
        }

        List<T> visible = Collections.unmodifiableList(items.subList(fromIndex, toIndex));

        // Task 66.2: Prepend sticky header if enabled
        if (stickyHeaderEnabled && stickyHeaderItem != null && !visible.contains(stickyHeaderItem)) {
            List<T> result = new ArrayList<>(visible.size() + 1);
            result.add(stickyHeaderItem);
            result.addAll(visible);
            return Collections.unmodifiableList(result);
        }

        return visible;
    }

    /**
     * Scroll up by the given number of items.
     */
    public void scrollUp(int count) {
        topIndex = Math.max(0, topIndex - count);
    }

    /**
     * Scroll down by the given number of items.
     */
    public void scrollDown(int count) {
        int maxTop = Math.max(0, items.size() - viewportSize);
        topIndex = Math.min(maxTop, topIndex + count);
    }

    /**
     * Scroll to the top of the list.
     */
    public void scrollToTop() {
        topIndex = 0;
    }

    /**
     * Scroll to the bottom of the list (most recent items visible).
     */
    public void scrollToBottom() {
        topIndex = Math.max(0, items.size() - viewportSize);
    }

    /**
     * Task 66.4: Two-phase jump — coarse scrollToIndex followed by precise adjustment.
     * Phase 1: Jump to approximate position
     * Phase 2: Fine-tune based on dynamic height estimation
     */
    public void scrollToIndex(int index) {
        if (items.isEmpty() || index < 0 || index >= items.size()) return;

        // Phase 1: Coarse jump
        topIndex = Math.max(0, index - viewportSize / 2);

        // Phase 2: Precise adjustment based on estimated heights
        int estimatedLines = 0;
        for (int i = Math.max(0, topIndex); i < Math.min(topIndex + viewportSize, items.size()); i++) {
            estimatedLines += getEstimatedHeight(i);
            if (estimatedLines > viewportSize) {
                // Adjust topIndex to fit within viewport
                topIndex = Math.max(0, topIndex + (estimatedLines - viewportSize));
                break;
            }
        }
    }

    /**
     * Task 66.3: Start a text search across all items.
     *
     * @param query the search query (regex pattern)
     */
    public void startSearch(String query) {
        this.searchQuery = query;
        this.searchResults = new ArrayList<>();
        this.searchResultIndex = -1;

        if (query == null || query.isEmpty()) {
            return;
        }

        try {
            Pattern pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
            for (int i = 0; i < items.size(); i++) {
                String text = textIndex.getOrDefault(i, "");
                if (pattern.matcher(text).find()) {
                    searchResults.add(i);
                }
            }
            if (!searchResults.isEmpty()) {
                searchResultIndex = 0;
                scrollToIndex(searchResults.get(0));
            }
        } catch (Exception e) {
            // Invalid regex — ignore
        }
    }

    /**
     * Task 66.3: Navigate to next search result (n key).
     */
    public void searchNext() {
        if (searchResults == null || searchResults.isEmpty()) return;
        searchResultIndex = (searchResultIndex + 1) % searchResults.size();
        scrollToIndex(searchResults.get(searchResultIndex));
    }

    /**
     * Task 66.3: Navigate to previous search result (N key).
     */
    public void searchPrevious() {
        if (searchResults == null || searchResults.isEmpty()) return;
        searchResultIndex = (searchResultIndex - 1 + searchResults.size()) % searchResults.size();
        scrollToIndex(searchResults.get(searchResultIndex));
    }

    /**
     * Task 66.3: Clear the current search.
     */
    public void clearSearch() {
        this.searchQuery = null;
        this.searchResults = null;
        this.searchResultIndex = -1;
    }

    /**
     * Task 66.3: Get current search info.
     */
    public SearchInfo getSearchInfo() {
        if (searchResults == null || searchResults.isEmpty()) {
            return new SearchInfo(searchQuery, 0, 0, -1);
        }
        return new SearchInfo(searchQuery, searchResults.size(), searchResultIndex + 1,
            searchResults.get(searchResultIndex));
    }

    /**
     * Task 66.3: Check if an item index matches the current search result.
     */
    public boolean isSearchMatch(int index) {
        return searchResults != null && searchResults.contains(index);
    }

    /**
     * Task 66.3: Check if an item is the current search highlight.
     */
    public boolean isCurrentSearchHighlight(int index) {
        if (searchResults == null || searchResultIndex < 0) return false;
        return index == searchResults.get(searchResultIndex);
    }

    /**
     * Task 66.6: Set cursor position.
     */
    public void setCursorIndex(int index) {
        if (index >= 0 && index < items.size()) {
            cursorIndex = index;
        }
    }

    /**
     * Task 66.6: Move cursor up (shift+↑).
     */
    public void cursorUp() {
        if (cursorIndex > 0) {
            cursorIndex--;
            // Auto-scroll if cursor goes out of viewport
            if (cursorIndex < topIndex) {
                scrollUp(1);
            }
        }
    }

    /**
     * Task 66.6: Move cursor down (shift+↓).
     */
    public void cursorDown() {
        if (cursorIndex < items.size() - 1) {
            cursorIndex++;
            // Auto-scroll if cursor goes out of viewport
            if (cursorIndex >= topIndex + viewportSize) {
                scrollDown(1);
            }
        }
    }

    /**
     * Task 66.6: Get current cursor index.
     */
    public int getCursorIndex() {
        return cursorIndex;
    }

    /**
     * Task 66.2: Set the sticky header item.
     */
    public void setStickyHeader(T header) {
        this.stickyHeaderItem = header;
    }

    /**
     * Task 66.2: Enable/disable sticky header.
     */
    public void setStickyHeaderEnabled(boolean enabled) {
        this.stickyHeaderEnabled = enabled;
    }

    /**
     * Task 66.1: Get the estimated line count for an item.
     */
    public int getEstimatedHeight(int index) {
        return heightCache.computeIfAbsent(index, i -> {
            if (i < 0 || i >= items.size()) return 1;
            String text = items.get(i).toString();
            return estimateLineCount(text);
        });
    }

    /**
     * Task 66.1: Estimate how many terminal lines a text will occupy.
     */
    private int estimateLineCount(String text) {
        if (text == null || text.isEmpty()) return 1;
        long lines = text.lines().count();
        // Each line may wrap based on terminal width
        long wrappedLines = 0;
        for (String line : text.split("\n", -1)) {
            wrappedLines += Math.max(1, (line.length() + terminalWidth - 1) / terminalWidth);
        }
        return (int) Math.max(1, wrappedLines);
    }

    /**
     * Task 66.5: Warm the search text index.
     */
    private void warmSearchIndex() {
        for (int i = 0; i < items.size(); i++) {
            textIndex.putIfAbsent(i, items.get(i).toString());
        }
    }

    /**
     * Get the current top index.
     */
    public int getTopIndex() {
        return topIndex;
    }

    /**
     * Get the total number of items.
     */
    public int getTotalItems() {
        return items.size();
    }

    /**
     * Get the viewport size.
     */
    public int getViewportSize() {
        return viewportSize;
    }

    /**
     * Returns true if there are items above the viewport.
     */
    public boolean canScrollUp() {
        return topIndex > 0;
    }

    /**
     * Returns true if there are items below the viewport.
     */
    public boolean canScrollDown() {
        return topIndex + viewportSize < items.size();
    }

    /**
     * Returns true if the viewport is at the bottom.
     */
    public boolean isAtBottom() {
        return !canScrollDown();
    }

    /**
     * Task 66.3: Search info record.
     */
    public record SearchInfo(String query, int totalMatches, int currentMatch, int currentIndex) {}
}
