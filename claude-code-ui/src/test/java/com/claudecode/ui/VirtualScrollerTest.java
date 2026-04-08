package com.claudecode.ui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VirtualScroller — viewport management for large message lists.
 */
class VirtualScrollerTest {

    @Test
    void constructor_rejectsNonPositiveViewportSize() {
        assertThrows(IllegalArgumentException.class, () -> new VirtualScroller<>(0));
        assertThrows(IllegalArgumentException.class, () -> new VirtualScroller<>(-1));
    }

    @Test
    void emptyItems_returnsEmptyVisible() {
        VirtualScroller<String> scroller = new VirtualScroller<>(5);
        assertTrue(scroller.getVisibleItems().isEmpty());
        assertEquals(0, scroller.getTotalItems());
    }

    @Test
    void setItems_scrollsToBottom() {
        VirtualScroller<String> scroller = new VirtualScroller<>(3);
        scroller.setItems(List.of("a", "b", "c", "d", "e"));

        // Viewport of 3, 5 items → top should be at index 2
        assertEquals(2, scroller.getTopIndex());
        assertEquals(List.of("c", "d", "e"), scroller.getVisibleItems());
    }

    @Test
    void setItems_fewerThanViewport() {
        VirtualScroller<String> scroller = new VirtualScroller<>(10);
        scroller.setItems(List.of("a", "b", "c"));

        assertEquals(0, scroller.getTopIndex());
        assertEquals(List.of("a", "b", "c"), scroller.getVisibleItems());
    }

    @Test
    void setItems_exactlyViewportSize() {
        VirtualScroller<String> scroller = new VirtualScroller<>(3);
        scroller.setItems(List.of("a", "b", "c"));

        assertEquals(0, scroller.getTopIndex());
        assertEquals(List.of("a", "b", "c"), scroller.getVisibleItems());
    }

    @Test
    void scrollUp_movesViewportUp() {
        VirtualScroller<String> scroller = new VirtualScroller<>(3);
        scroller.setItems(List.of("a", "b", "c", "d", "e"));

        // Initially at bottom: [c, d, e]
        scroller.scrollUp(1);
        assertEquals(1, scroller.getTopIndex());
        assertEquals(List.of("b", "c", "d"), scroller.getVisibleItems());
    }

    @Test
    void scrollUp_clampsAtTop() {
        VirtualScroller<String> scroller = new VirtualScroller<>(3);
        scroller.setItems(List.of("a", "b", "c", "d", "e"));

        scroller.scrollUp(100);
        assertEquals(0, scroller.getTopIndex());
        assertEquals(List.of("a", "b", "c"), scroller.getVisibleItems());
    }

    @Test
    void scrollDown_movesViewportDown() {
        VirtualScroller<String> scroller = new VirtualScroller<>(3);
        scroller.setItems(List.of("a", "b", "c", "d", "e"));

        scroller.scrollToTop();
        assertEquals(List.of("a", "b", "c"), scroller.getVisibleItems());

        scroller.scrollDown(1);
        assertEquals(1, scroller.getTopIndex());
        assertEquals(List.of("b", "c", "d"), scroller.getVisibleItems());
    }

    @Test
    void scrollDown_clampsAtBottom() {
        VirtualScroller<String> scroller = new VirtualScroller<>(3);
        scroller.setItems(List.of("a", "b", "c", "d", "e"));

        scroller.scrollToTop();
        scroller.scrollDown(100);
        assertEquals(2, scroller.getTopIndex());
        assertEquals(List.of("c", "d", "e"), scroller.getVisibleItems());
    }

    @Test
    void scrollToTop_movesToBeginning() {
        VirtualScroller<String> scroller = new VirtualScroller<>(3);
        scroller.setItems(List.of("a", "b", "c", "d", "e"));

        scroller.scrollToTop();
        assertEquals(0, scroller.getTopIndex());
        assertEquals(List.of("a", "b", "c"), scroller.getVisibleItems());
    }

    @Test
    void scrollToBottom_movesToEnd() {
        VirtualScroller<String> scroller = new VirtualScroller<>(3);
        scroller.setItems(List.of("a", "b", "c", "d", "e"));

        scroller.scrollToTop();
        scroller.scrollToBottom();
        assertEquals(2, scroller.getTopIndex());
        assertEquals(List.of("c", "d", "e"), scroller.getVisibleItems());
    }

    @Test
    void canScrollUp_returnsTrueWhenNotAtTop() {
        VirtualScroller<String> scroller = new VirtualScroller<>(3);
        scroller.setItems(List.of("a", "b", "c", "d", "e"));

        assertTrue(scroller.canScrollUp()); // at bottom
        scroller.scrollToTop();
        assertFalse(scroller.canScrollUp()); // at top
    }

    @Test
    void canScrollDown_returnsTrueWhenNotAtBottom() {
        VirtualScroller<String> scroller = new VirtualScroller<>(3);
        scroller.setItems(List.of("a", "b", "c", "d", "e"));

        assertFalse(scroller.canScrollDown()); // at bottom
        scroller.scrollToTop();
        assertTrue(scroller.canScrollDown()); // at top
    }

    @Test
    void isAtBottom_returnsTrueWhenAtBottom() {
        VirtualScroller<String> scroller = new VirtualScroller<>(3);
        scroller.setItems(List.of("a", "b", "c", "d", "e"));

        assertTrue(scroller.isAtBottom());
        scroller.scrollUp(1);
        assertFalse(scroller.isAtBottom());
    }

    @Test
    void getViewportSize_returnsConfiguredSize() {
        VirtualScroller<String> scroller = new VirtualScroller<>(7);
        assertEquals(7, scroller.getViewportSize());
    }

    @Test
    void setItems_withNull_treatsAsEmpty() {
        VirtualScroller<String> scroller = new VirtualScroller<>(5);
        scroller.setItems(null);
        assertTrue(scroller.getVisibleItems().isEmpty());
        assertEquals(0, scroller.getTotalItems());
    }

    @Test
    void largeList_viewportCorrect() {
        VirtualScroller<Integer> scroller = new VirtualScroller<>(10);
        List<Integer> items = IntStream.range(0, 1000).boxed().toList();
        scroller.setItems(items);

        assertEquals(990, scroller.getTopIndex());
        List<Integer> visible = scroller.getVisibleItems();
        assertEquals(10, visible.size());
        assertEquals(990, visible.get(0));
        assertEquals(999, visible.get(9));
    }

    @Test
    void visibleItems_areUnmodifiable() {
        VirtualScroller<String> scroller = new VirtualScroller<>(3);
        scroller.setItems(List.of("a", "b", "c"));

        List<String> visible = scroller.getVisibleItems();
        assertThrows(UnsupportedOperationException.class, () -> visible.add("d"));
    }
}
