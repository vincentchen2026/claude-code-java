package com.claudecode.ui.renderer;

import com.claudecode.ui.Ansi;
import com.claudecode.ui.AnsiColor;
import com.claudecode.ui.TerminalProtocols;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Renders images in the terminal using iTerm2, Kitty, or ASCII fallback.
 */
public class ImageMessageRenderer {

    private static final Pattern IMAGE_PATTERN = Pattern.compile(
            "\\.(png|jpg|jpeg|gif|webp|bmp|ico|tiff?)$", Pattern.CASE_INSENSITIVE);

    private final boolean supportsImages;
    private final boolean isITerm2;
    private final boolean isKitty;
    private final int maxWidth;
    private final int maxHeight;

    public ImageMessageRenderer() {
        var caps = TerminalProtocols.detectCapabilities();
        this.supportsImages = caps.supportsImages();
        this.isITerm2 = caps.isITerm2();
        this.isKitty = caps.isKitty();
        this.maxWidth = 80;
        this.maxHeight = 24;
    }

    public String render(Path path) {
        if (path == null || !Files.exists(path)) {
            return Ansi.colored("[Image not found]", AnsiColor.RED);
        }
        String ext = getExtension(path);
        if (ext == null) return renderPlaceholder(path, "Unknown");

        if (supportsImages) {
            return renderWithProtocol(path);
        }
        return renderAsciiFallback(path, ext);
    }

    private String renderWithProtocol(Path path) {
        if (isITerm2) {
            return TerminalProtocols.itermImage(path.toAbsolutePath().toString(), "0.5", "0.8", true);
        } else if (isKitty) {
            return TerminalProtocols.kittyImage(path.toAbsolutePath().toString(), 0, 0, 0, 0);
        }
        return renderPlaceholder(path, getExtension(path));
    }

    private String renderAsciiFallback(Path path, String ext) {
        String filename = path.getFileName().toString();
        StringBuilder sb = new StringBuilder();
        sb.append(Ansi.colored("┌─ [", AnsiColor.GRAY));
        sb.append(Ansi.colored(filename, AnsiColor.CYAN));
        sb.append(Ansi.colored("] ", AnsiColor.GRAY));
        sb.append(Ansi.colored("[" + ext + "]", AnsiColor.YELLOW));
        int padding = Math.max(0, maxWidth - filename.length() - ext.length() - 12);
        sb.append("─".repeat(padding));
        sb.append(Ansi.colored("┐", AnsiColor.GRAY));
        return sb.toString();
    }

    private String renderPlaceholder(Path path, String type) {
        return Ansi.colored("[Image: " + path.getFileName() + "]", AnsiColor.GRAY);
    }

    public String renderClipboardImage(byte[] pngData) {
        if (pngData == null || pngData.length == 0) {
            return Ansi.colored("[No image data]", AnsiColor.YELLOW);
        }
        if (isITerm2 && pngData.length > 8 &&
            pngData[0] == (byte) 0x89 && pngData[1] == 'P' &&
            pngData[2] == 'N' && pngData[3] == 'G') {
            String base64 = Base64.getEncoder().encodeToString(pngData);
            return TerminalProtocols.itermImageBase64(base64, "image/png", "50");
        }
        return Ansi.colored("[Clipboard Image: " + pngData.length + " bytes]", AnsiColor.GRAY);
    }

    public static boolean isImage(Path path) {
        if (path == null) return false;
        return IMAGE_PATTERN.matcher(path.getFileName().toString()).find();
    }

    private static String getExtension(Path path) {
        if (path == null) return null;
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? null : name.substring(dot + 1).toLowerCase();
    }
}
