package com.claudecode.ui;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.commonmark.Extension;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.ext.gfm.tables.TableCell;

import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * Renders Markdown text to ANSI-styled terminal output using commonmark-java.
 * Task 68 enhancements:
 * - 68.1: Token caching (Caffeine LRU cache 500 entries)
 * - 68.2: Incremental Markdown parsing (stable prefix reuse)
 * - 68.3: Table rendering (Ansi table with column alignment)
 * - 68.4: Ordered list numbering
 * - 68.5: Plain text fast path
 * - 68.6: XML tag stripping
 * - 68.7: Dim color support
 */
public class MarkdownRenderer {

    private final Parser parser;

    // Task 68.1: Token cache (LRU, 500 entries)
    private final Cache<Long, String> renderCache;

    // Task 68.5: Plain text detection pattern (no markdown syntax)
    private static final Pattern MARKDOWN_SYNTAX_PATTERN = Pattern.compile(
        "[#*`_\\[\\]!>|~]|^\\s*[-*+]\\s|^\\s*\\d+\\.");

    // Task 68.6: XML tag stripping pattern (for <think> etc.)
    private static final Pattern XML_TAG_PATTERN = Pattern.compile("<[^/>][^>]*>.*?</[^>]+>", Pattern.DOTALL);
    private static final Pattern SELF_CLOSING_XML_PATTERN = Pattern.compile("<[^>]+/>");

    public MarkdownRenderer() {
        this(500);
    }

    public MarkdownRenderer(int cacheSize) {
        // Task 68.3: Enable GFM tables extension
        List<Extension> extensions = List.of(TablesExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
        // Task 68.1: Caffeine LRU cache
        this.renderCache = Caffeine.newBuilder()
            .maximumSize(cacheSize)
            .build();
    }

    /**
     * Render markdown text to an ANSI-styled string for terminal display.
     * Uses caching for repeated content and plain text fast path.
     */
    public String render(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }

        // Task 68.6: Strip XML tags (prompt injection artifacts)
        String cleaned = stripXmlTags(markdown);

        // Task 68.5: Plain text fast path
        if (!MARKDOWN_SYNTAX_PATTERN.matcher(cleaned).find()) {
            return cleaned;
        }

        // Task 68.1: Cache lookup
        long hash = computeHash(cleaned);
        String cached = renderCache.getIfPresent(hash);
        if (cached != null) {
            return cached;
        }

        String result = renderMarkdown(cleaned);
        renderCache.put(hash, result);
        return result;
    }

    /**
     * Task 68.2: Incremental render — only re-render the unstable suffix.
     * Returns the full rendered string by combining cached stable prefix with new suffix.
     */
    public String renderIncremental(String stablePrefix, String unstableSuffix) {
        String renderedPrefix = render(stablePrefix);
        String renderedSuffix = render(unstableSuffix);
        return renderedPrefix + renderedSuffix;
    }

    /**
     * Task 68.6: Strip XML tags from markdown content.
     */
    private String stripXmlTags(String markdown) {
        String result = XML_TAG_PATTERN.matcher(markdown).replaceAll("");
        return SELF_CLOSING_XML_PATTERN.matcher(result).replaceAll("");
    }

    /**
     * Compute a fast hash for cache key.
     */
    private long computeHash(String text) {
        CRC32 crc = new CRC32();
        crc.update(text.getBytes());
        return crc.getValue();
    }

    /**
     * Core markdown rendering logic.
     */
    private String renderMarkdown(String markdown) {
        Node document = parser.parse(markdown);
        StringBuilder sb = new StringBuilder();
        document.accept(new TerminalMarkdownVisitor(sb));
        return sb.toString();
    }

    /**
     * Visitor that converts commonmark AST nodes to ANSI-styled text.
     */
    static class TerminalMarkdownVisitor extends AbstractVisitor {

        private final StringBuilder sb;
        private int listDepth = 0;
        private int orderedCounter = 0;
        private boolean inCodeBlock = false;
        private String codeBlockLanguage = null;

        TerminalMarkdownVisitor(StringBuilder sb) {
            this.sb = sb;
        }

        @Override
        public void visit(Heading heading) {
            String prefix = "#".repeat(heading.getLevel()) + " ";
            sb.append(Ansi.styled(prefix, AnsiColor.CYAN, AnsiStyle.BOLD));
            StringBuilder headingText = new StringBuilder();
            collectText(heading, headingText);
            sb.append(Ansi.styled(headingText.toString(), AnsiStyle.BOLD));
            sb.append("\n");
        }

        @Override
        public void visit(Paragraph paragraph) {
            if (listDepth > 0) {
                visitChildren(paragraph);
            } else {
                visitChildren(paragraph);
                sb.append("\n");
            }
        }

        @Override
        public void visit(Text text) {
            sb.append(text.getLiteral());
        }

        @Override
        public void visit(Emphasis emphasis) {
            StringBuilder content = new StringBuilder();
            collectText(emphasis, content);
            sb.append(Ansi.styled(content.toString(), AnsiStyle.ITALIC));
        }

        @Override
        public void visit(StrongEmphasis strongEmphasis) {
            StringBuilder content = new StringBuilder();
            collectText(strongEmphasis, content);
            sb.append(Ansi.styled(content.toString(), AnsiStyle.BOLD));
        }

        @Override
        public void visit(Code code) {
            sb.append(Ansi.colored(code.getLiteral(), AnsiColor.YELLOW));
        }

        @Override
        public void visit(FencedCodeBlock fencedCodeBlock) {
            String info = fencedCodeBlock.getInfo();
            String lang = (info != null && !info.isEmpty()) ? info.split("\\s+")[0] : null;
            String literal = fencedCodeBlock.getLiteral();
            if (literal == null) literal = "";

            if (lang != null && !lang.isEmpty()) {
                sb.append(Ansi.colored("  [" + lang + "]", AnsiColor.GRAY));
                sb.append("\n");
            }

            String highlighted = SyntaxHighlighter.highlight(literal, lang);
            for (String line : highlighted.split("\n", -1)) {
                sb.append(Ansi.colored("  │ ", AnsiColor.GRAY));
                sb.append(line);
                sb.append("\n");
            }
        }

        @Override
        public void visit(IndentedCodeBlock indentedCodeBlock) {
            String literal = indentedCodeBlock.getLiteral();
            if (literal == null) literal = "";
            for (String line : literal.split("\n", -1)) {
                sb.append(Ansi.colored("  │ ", AnsiColor.GRAY));
                sb.append(line);
                sb.append("\n");
            }
        }

        @Override
        public void visit(BulletList bulletList) {
            listDepth++;
            visitChildren(bulletList);
            listDepth--;
            if (listDepth == 0) {
                sb.append("\n");
            }
        }

        @Override
        public void visit(OrderedList orderedList) {
            listDepth++;
            orderedCounter = orderedList.getStartNumber();
            visitChildren(orderedList);
            listDepth--;
            if (listDepth == 0) {
                sb.append("\n");
            }
        }

        @Override
        public void visit(ListItem listItem) {
            String indent = "  ".repeat(Math.max(0, listDepth - 1));
            sb.append(indent);

            // Task 68.4: Ordered list numbering
            if (orderedCounter > 0) {
                sb.append(Ansi.colored(orderedCounter + ". ", AnsiColor.CYAN));
                orderedCounter++;
            } else {
                sb.append(Ansi.colored("• ", AnsiColor.CYAN));
            }
            visitChildren(listItem);
            sb.append("\n");
        }

        @Override
        public void visit(Link link) {
            StringBuilder linkText = new StringBuilder();
            collectText(link, linkText);
            String text = linkText.toString();
            String dest = link.getDestination();
            sb.append(Ansi.styled(text, AnsiStyle.UNDERLINE));
            if (dest != null && !dest.isEmpty() && !dest.equals(text)) {
                sb.append(Ansi.colored(" (" + dest + ")", AnsiColor.GRAY));
            }
        }

        @Override
        public void visit(BlockQuote blockQuote) {
            StringBuilder content = new StringBuilder();
            collectText(blockQuote, content);
            for (String line : content.toString().split("\n", -1)) {
                sb.append(Ansi.colored("  ▎ ", AnsiColor.GRAY));
                sb.append(Ansi.styled(line, AnsiStyle.ITALIC));
                sb.append("\n");
            }
        }

        @Override
        public void visit(ThematicBreak thematicBreak) {
            sb.append(Ansi.colored("─".repeat(40), AnsiColor.GRAY));
            sb.append("\n");
        }

        @Override
        public void visit(SoftLineBreak softLineBreak) {
            sb.append(" ");
        }

        @Override
        public void visit(HardLineBreak hardLineBreak) {
            sb.append("\n");
        }

        @Override
        public void visit(HtmlInline htmlInline) {
            sb.append(htmlInline.getLiteral().replaceAll("<[^>]+>", ""));
        }

        @Override
        public void visit(HtmlBlock htmlBlock) {
            sb.append(htmlBlock.getLiteral().replaceAll("<[^>]+>", ""));
        }

        @Override
        public void visit(CustomBlock customBlock) {
            // Task 68.3: Handle GFM table blocks
            if (customBlock instanceof TableBlock tableBlock) {
                visit(tableBlock);
                return;
            }
            visitChildren(customBlock);
        }

        // Task 68.3: Table rendering
        public void visit(TableBlock tableBlock) {
            // Collect all rows
            java.util.List<java.util.List<String>> rows = new java.util.ArrayList<>();
            Node tableChild = tableBlock.getFirstChild();
            while (tableChild != null) {
                if (tableChild instanceof TableHead || tableChild instanceof TableBody) {
                    Node cell = tableChild.getFirstChild();
                    while (cell != null) {
                        if (cell instanceof TableCell tableCell) {
                            java.util.List<String> rowData = new java.util.ArrayList<>();
                            Node content = tableCell.getFirstChild();
                            while (content != null) {
                                if (content instanceof Text text) {
                                    rowData.add(text.getLiteral());
                                }
                                content = content.getNext();
                            }
                            rows.add(rowData);
                        }
                        cell = cell.getNext();
                    }
                }
                tableChild = tableChild.getNext();
            }

            if (rows.isEmpty()) return;

            // Calculate column widths
            int maxCols = rows.stream().mapToInt(java.util.List::size).max().orElse(0);
            int[] colWidths = new int[maxCols];
            for (java.util.List<String> r : rows) {
                for (int i = 0; i < Math.min(r.size(), maxCols); i++) {
                    colWidths[i] = Math.max(colWidths[i], r.get(i).length());
                }
            }
            // Cap column widths to terminal width
            int maxWidth = 80;
            for (int i = 0; i < colWidths.length; i++) {
                colWidths[i] = Math.min(colWidths[i], maxWidth / maxCols);
            }

            // Render table
            sb.append("\n");
            for (int r = 0; r < rows.size(); r++) {
                java.util.List<String> rowData = rows.get(r);
                sb.append("  ");
                for (int c = 0; c < maxCols; c++) {
                    String cellText = c < rowData.size() ? rowData.get(c) : "";
                    if (cellText.length() > colWidths[c]) {
                        cellText = cellText.substring(0, colWidths[c] - 1) + "…";
                    }
                    String padded = String.format("%-" + colWidths[c] + "s", cellText);
                    if (r == 0) {
                        sb.append(Ansi.styled(padded, AnsiStyle.BOLD));
                    } else {
                        sb.append(padded);
                    }
                    if (c < maxCols - 1) {
                        sb.append(Ansi.colored(" │ ", AnsiColor.GRAY));
                    }
                }
                sb.append("\n");
                // Separator after header
                if (r == 0) {
                    sb.append("  ");
                    for (int c = 0; c < maxCols; c++) {
                        sb.append(Ansi.colored("─".repeat(colWidths[c]), AnsiColor.GRAY));
                        if (c < maxCols - 1) {
                            sb.append(Ansi.colored("─┼─", AnsiColor.GRAY));
                        }
                    }
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }

        /**
         * Collect plain text from a node and its children.
         */
        private void collectText(Node node, StringBuilder target) {
            Node child = node.getFirstChild();
            while (child != null) {
                if (child instanceof Text text) {
                    target.append(text.getLiteral());
                } else if (child instanceof Code code) {
                    target.append(code.getLiteral());
                } else if (child instanceof SoftLineBreak) {
                    target.append(" ");
                } else if (child instanceof HardLineBreak) {
                    target.append("\n");
                } else {
                    collectText(child, target);
                }
                child = child.getNext();
            }
        }
    }
}
