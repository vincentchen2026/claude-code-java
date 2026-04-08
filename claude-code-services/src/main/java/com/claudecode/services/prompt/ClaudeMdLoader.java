package com.claudecode.services.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads and merges CLAUDE.md instruction files from multiple locations.
 * <p>
 * Search order (later entries take precedence when merging):
 * <ol>
 *   <li>~/.claude/CLAUDE.md (user-level)</li>
 *   <li>Parent directories walking up from working directory</li>
 *   <li>Project root CLAUDE.md (highest precedence)</li>
 * </ol>
 * <p>
 * When merging, project-level instructions appear last (highest precedence)
 * and user-level instructions appear first (lowest precedence).
 */
public class ClaudeMdLoader {

    private static final Logger log = LoggerFactory.getLogger(ClaudeMdLoader.class);
    private static final String CLAUDE_MD = "CLAUDE.md";

    /**
     * Loads and merges CLAUDE.md content from the given paths.
     * Files that don't exist or can't be read are silently skipped.
     * Content is concatenated with section separators, project-level last.
     *
     * @param paths ordered list of CLAUDE.md file paths (user-level first, project-level last)
     * @return merged instruction text, or empty string if no files found
     */
    public String loadAndMerge(List<Path> paths) {
        if (paths == null || paths.isEmpty()) {
            return "";
        }

        List<String> sections = new ArrayList<>();
        for (Path path : paths) {
            String content = loadFile(path);
            if (content != null && !content.isBlank()) {
                sections.add(content.strip());
            }
        }

        if (sections.isEmpty()) {
            return "";
        }

        return String.join("\n\n---\n\n", sections);
    }

    /**
     * Discovers CLAUDE.md files by searching standard locations.
     * Returns paths in merge order: user-level first, project-level last.
     *
     * @param workingDirectory the project working directory
     * @return list of discovered CLAUDE.md paths (may include non-existent paths)
     */
    public List<Path> discoverClaudeMdPaths(Path workingDirectory) {
        List<Path> paths = new ArrayList<>();

        // 1. User-level: ~/.claude/CLAUDE.md
        Path userHome = Path.of(System.getProperty("user.home"));
        Path userClaudeMd = userHome.resolve(".claude").resolve(CLAUDE_MD);
        if (Files.isRegularFile(userClaudeMd)) {
            paths.add(userClaudeMd);
        }

        // 2. Walk parent directories from working directory up to root
        //    Collect in reverse order (root first), then project-level is last
        List<Path> parentPaths = new ArrayList<>();
        if (workingDirectory != null) {
            Path current = workingDirectory.toAbsolutePath().normalize();
            while (current != null) {
                Path candidate = current.resolve(CLAUDE_MD);
                if (Files.isRegularFile(candidate)) {
                    parentPaths.add(candidate);
                }
                current = current.getParent();
            }
        }

        // Reverse so that root-level comes first, project-level comes last
        for (int i = parentPaths.size() - 1; i >= 0; i--) {
            Path p = parentPaths.get(i);
            // Avoid duplicates with user-level path
            if (!paths.contains(p)) {
                paths.add(p);
            }
        }

        return paths;
    }

    /**
     * Loads a single file, returning null if it doesn't exist or can't be read.
     */
    String loadFile(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return null;
        }
        try {
            return Files.readString(path);
        } catch (IOException e) {
            log.warn("Failed to read CLAUDE.md at {}: {}", path, e.getMessage());
            return null;
        }
    }
}
