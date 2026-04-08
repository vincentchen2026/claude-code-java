package com.claudecode.ui;

/**
 * Terminal protocol utilities for iTerm2, Kitty, and Ghostty.
 * Implements escape sequences for images, notifications, hyperlinks, and progress bars.
 */
public final class TerminalProtocols {

    private TerminalProtocols() {}

    // CSI (Control Sequence Introducer) prefix
    private static final String CSI = "\u001B[";
    private static final String OSC = "\u001B]";
    private static final String ST = "\u001B\\";  // String Terminator

    // Bell
    public static final String BELL = "\u0007";

    // Cursor control
    public static final String SAVE_CURSOR = CSI + "s";
    public static final String RESTORE_CURSOR = CSI + "u";
    public static final String CLEAR_LINE = CSI + "K";
    public static final String CLEAR_SCREEN = CSI + "2J";
    public static final String MOVE_CURSOR = CSI + "%d;%dH";

    /**
     * Move cursor to specific position (1-indexed).
     */
    public static String moveCursor(int row, int col) {
        return String.format(MOVE_CURSOR, row, col);
    }

    // ==================== OSC 8: Hyperlinks ====================

    /**
     * Create a hyperlink with OSC 8 escape sequence.
     * Supported by: iTerm2, Kitty, Ghostty, Windows Terminal, VTE-based terminals.
     *
     * @param url the hyperlink URL
     * @param text the visible text
     * @return the escape sequence with text
     */
    public static String hyperlink(String url, String text) {
        return OSC + "8;;" + url + ST + text + OSC + "8;;" + ST;
    }

    /**
     * Create a hyperlink with OSC 8 escape sequence and custom ID.
     *
     * @param url the hyperlink URL
     * @param id the link ID
     * @param text the visible text
     * @return the escape sequence with text
     */
    public static String hyperlinkWithId(String url, String id, String text) {
        return OSC + "8;id=" + id + ";" + url + ST + text + OSC + "8;;" + ST;
    }

    // ==================== iTerm2 Image Protocol (OSC 1337) ====================

    /**
     * Display an image from file path.
     * Requires iTerm2 3.3+ or compatible terminal.
     *
     * @param path absolute path to the image file
     * @param height height as fraction of terminal height (0.0-1.0) or pixels with 'p' suffix
     * @param width width as fraction of terminal width (0.0-1.0) or pixels with 'p' suffix
     * @param preserveAspectRatio whether to preserve aspect ratio
     * @return the escape sequence
     */
    public static String itermImage(String path, String height, String width, boolean preserveAspectRatio) {
        int inline = preserveAspectRatio ? 1 : 0;
        return String.format("%s1337;File=path=%s:height=%s:width=%s:preserveAspectRatio=%d%s",
                OSC, escapeParam(path), height, width, inline, ST);
    }

    /**
     * Display an image from file path with size in lines.
     */
    public static String itermImage(String path, int heightLines) {
        return String.format("%s1337;File=path=%s:height=%d%s",
                OSC, escapeParam(path), heightLines, ST);
    }

    /**
     * Display base64-encoded image data.
     */
    public static String itermImageBase64(String base64Data, String mimeType, String width) {
        return String.format("%s1337;Inline=%s:%s:width=%s%s",
                OSC, mimeType, base64Data, width, ST);
    }

    /**
     * Clear all images from the current line.
     */
    public static String itermClearImage() {
        return OSC + "1337;ClearSelf=s" + ST;
    }

    // ==================== Kitty Graphics Protocol ====================

    /**
     * Display image using Kitty graphics protocol.
     * Supported by: Kitty, some VTE terminals.
     *
     * @param path absolute path to the image file
     * @param x x position in cells (0-based)
     * @param y y position in cells (0-based)
     * @param cellWidth cell width (0 = full terminal width)
     * @param cellHeight cell height (0 = auto)
     */
    public static String kittyImage(String path, int x, int y, int cellWidth, int cellHeight) {
        StringBuilder sb = new StringBuilder();
        sb.append(OSC).append("1337;File=file='").append(escapeParam(path)).append("'");
        if (x > 0 || y > 0) {
            sb.append(",x=").append(x);
            sb.append(",y=").append(y);
        }
        if (cellWidth > 0) sb.append(",cell_width=").append(cellWidth);
        if (cellHeight > 0) sb.append(",cell_height=").append(cellHeight);
        sb.append(ST);
        return sb.toString();
    }

    /**
     * Upload image to Kitty and get transmission key.
     * First transmit the image, then use the key to display it.
     */
    public static String kittyUploadImage(String path) {
        // This requires binary transmission, typically handled separately
        return "";
    }

    // ==================== OSC 9;4: Progress Bar ====================

    /**
     * Show progress bar in terminal title bar or use terminal's progress bar.
     * Supported by: iTerm2, Windows Terminal, some others.
     *
     * @param percentage progress percentage (0-100)
     * @param title progress title
     * @return the escape sequence
     */
    public static String progressBar(int percentage, String title) {
        return String.format("%s9;4;%d;%s%s", OSC, percentage, escapeParam(title), ST);
    }

    /**
     * Show indeterminate progress (spinner).
     */
    public static String progressIndeterminate(String title) {
        return String.format("%s9;4;%s%s", OSC, escapeParam(title), ST);
    }

    /**
     * Clear progress bar.
     */
    public static String clearProgress() {
        return OSC + "9;4;" + ST;
    }

    // ==================== OSC 9;2: Terminal Notification ====================

    /**
     * Show a notification in the terminal.
     * Supported by: iTerm2, Windows Terminal.
     *
     * @param title notification title
     * @param body notification body (may be ignored by some terminals)
     * @return the escape sequence
     */
    public static String notification(String title, String body) {
        return String.format("%s9;2;%s;%s%s", OSC, escapeParam(title), escapeParam(body), ST);
    }

    // ==================== Terminal Title ====================

    /**
     * Set terminal title.
     *
     * @param title new terminal title
     * @return the escape sequence
     */
    public static String setTitle(String title) {
        return OSC + "0;" + escapeParam(title) + ST;
    }

    /**
     * Set terminal title and icon name.
     *
     * @param title terminal title
     * @param iconName icon name
     * @return the escape sequence
     */
    public static String setTitleAndIcon(String title, String iconName) {
        return OSC + "0;" + escapeParam(title) + "\u0007" + OSC + "1;" + escapeParam(iconName) + ST;
    }

    // ==================== iTerm2 Specific ====================

    /**
     * Set terminal profile/colors.
     *
     * @param profileName iTerm2 profile name
     * @return the escape sequence
     */
    public static String itermSetProfile(String profileName) {
        return OSC + "1337;SetProfile=" + escapeParam(profileName) + ST;
    }

    /**
     * Run a command in a new tab.
     *
     * @param command the command to run
     * @return the escape sequence
     */
    public static String itermNewTab(String command) {
        return OSC + "1337;NewTerminal=" + escapeParam(command) + ST;
    }

    /**
     * Open URL with default browser.
     *
     * @param url the URL to open
     * @return the escape sequence
     */
    public static String itermOpenUrl(String url) {
        return OSC + "8;;" + url + ST;  // Same as hyperlink
    }

    // ==================== Color Palette ====================

    /**
     * Set ANSI 256 color.
     *
     * @param isBackground true for background, false for foreground
     * @param colorIndex 0-255 for 256-color palette
     * @return the escape sequence
     */
    public static String ansiColor(boolean isBackground, int colorIndex) {
        String prefix = isBackground ? "48" : "38";
        return String.format(CSI + "%s;5;%dm", prefix, colorIndex);
    }

    /**
     * Set Truecolor (24-bit) color.
     *
     * @param isBackground true for background, false for foreground
     * @param r red component (0-255)
     * @param g green component (0-255)
     * @param b blue component (0-255)
     * @return the escape sequence
     */
    public static String trueColor(boolean isBackground, int r, int g, int b) {
        String prefix = isBackground ? "48" : "38";
        return String.format(CSI + "%s;2;%d;%d;%dm", prefix, r, g, b);
    }

    // ==================== Utility Methods ====================

    /**
     * Escape special characters in OSC parameters.
     */
    private static String escapeParam(String param) {
        if (param == null) return "";
        return param
                .replace("\\", "\\\\")
                .replace(":", "\\072")
                .replace(";", "\\073")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    /**
     * Check if terminal supports OSC 8 hyperlinks.
     * Most modern terminals support this.
     */
    public static boolean supportsHyperlinks() {
        // Can be detected via terminal query, but defaults to true for modern terminals
        String term = System.getenv("TERM");
        if (term == null) return true;
        // Dumb terminals don't support hyperlinks
        return !term.contains("dumb");
    }

    /**
     * Check if terminal appears to be iTerm2.
     */
    public static boolean isITerm2() {
        return "xterm-256color".equals(System.getenv("TERM_PROGRAM"))
                || System.getenv("ITERM_PROFILE") != null;
    }

    /**
     * Check if terminal appears to be Kitty.
     */
    public static boolean isKitty() {
        String term = System.getenv("TERM");
        return term != null && term.toLowerCase().contains("kitty");
    }

    /**
     * Check if terminal supports image protocols.
     */
    public static boolean supportsImages() {
        // iTerm2, Kitty, and some VTE terminals support images
        return isITerm2() || isKitty() || hasVteVersion();
    }

    private static boolean hasVteVersion() {
        // VTE version can be queried, simplified check
        return System.getenv("VTE_VERSION") != null;
    }

    /**
     * Detect terminal capabilities.
     */
    public static TerminalCapabilities detectCapabilities() {
        return new TerminalCapabilities(
                supportsHyperlinks(),
                supportsImages(),
                isITerm2(),
                isKitty(),
                System.getenv("VTE_VERSION") != null
        );
    }

    /**
     * Terminal capabilities record.
     */
    public record TerminalCapabilities(
            boolean supportsHyperlinks,
            boolean supportsImages,
            boolean isITerm2,
            boolean isKitty,
            boolean isVte
    ) {}
}
