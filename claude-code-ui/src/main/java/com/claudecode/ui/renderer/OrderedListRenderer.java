package com.claudecode.ui.renderer;

import org.commonmark.node.ListItem;
import org.commonmark.node.OrderedList;

import java.util.ArrayList;
import java.util.List;

/**
 * Task 68.4: Ordered list renderer with nested numbering support.
 * Renders ordered lists as 1. 2. 3. with nested sub-items as 1.1, 1.2, etc.
 */
public class OrderedListRenderer {

    private final List<Integer> counters;
    private final int baseIndent;

    public OrderedListRenderer() {
        this.counters = new ArrayList<>();
        this.baseIndent = 2;
    }

    /**
     * Render an ordered list with nested numbering.
     *
     * @param orderedList the ordered list node from commonmark
     * @return rendered string with proper numbering
     */
    public String render(OrderedList orderedList) {
        counters.clear();
        counters.add(orderedList.getStartNumber());

        StringBuilder sb = new StringBuilder();
        renderListItem(orderedList.getFirstChild(), sb, 0);
        return sb.toString();
    }

    /**
     * Render a list item and its children with nested numbering.
     */
    private void renderListItem(org.commonmark.node.Node node, StringBuilder sb, int depth) {
        while (node != null) {
            if (node instanceof ListItem listItem) {
                renderItemContent(listItem, sb, depth);
            }
            node = node.getNext();
        }
    }

    private void renderItemContent(ListItem listItem, StringBuilder sb, int depth) {
        ensureCounterSize(depth);
        int counter = counters.get(depth);

        String indent = "  ".repeat(Math.max(0, depth));
        String prefix = getNestedPrefix(depth) + counter + ". ";
        sb.append(indent).append(prefix);

        counters.set(depth, counter + 1);

        org.commonmark.node.Node child = listItem.getFirstChild();
        while (child != null) {
            if (child instanceof OrderedList nestedList) {
                sb.append("\n");
                renderListItem(nestedList.getFirstChild(), sb, depth + 1);
            } else {
                sb.append(getNodeText(child));
            }
            child = child.getNext();
        }
        sb.append("\n");
    }

    private String getNestedPrefix(int depth) {
        if (depth == 0) return "";
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            prefix.append(counters.get(i)).append(".");
        }
        return prefix.toString();
    }

    private void ensureCounterSize(int depth) {
        while (counters.size() <= depth) {
            counters.add(1);
        }
    }

    private String getNodeText(org.commonmark.node.Node node) {
        if (node instanceof org.commonmark.node.Text text) {
            return text.getLiteral();
        } else if (node instanceof org.commonmark.node.SoftLineBreak) {
            return " ";
        } else if (node instanceof org.commonmark.node.HardLineBreak) {
            return "\n";
        }
        return "";
    }

    /**
     * Reset the counter state for fresh rendering.
     */
    public void reset() {
        counters.clear();
    }
}