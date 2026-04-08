package com.claudecode.services.skills;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;

/**
 * Matches conditional skills based on file path patterns.
 * Skills with paths patterns are only active when the current file matches.
 */
public class ConditionalSkillMatcher {

    /**
     * Check if a skill applies to the given file path.
     * Non-conditional skills always apply.
     * Conditional skills apply only if the file matches any of their path patterns.
     *
     * @param skill    the skill to check
     * @param filePath the current file path
     * @return true if the skill applies
     */
    public boolean matches(Skill skill, Path filePath) {
        if (!skill.isConditional()) {
            return true;
        }

        if (filePath == null) {
            return false;
        }

        String fileStr = filePath.toString().replace('\\', '/');

        for (String pattern : skill.paths()) {
            if (matchesPattern(pattern, fileStr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Filter a list of skills to only those matching the given file path.
     */
    public List<Skill> filterMatching(List<Skill> skills, Path filePath) {
        return skills.stream()
                .filter(s -> matches(s, filePath))
                .toList();
    }

    /**
     * Match a glob pattern against a file path string.
     */
    static boolean matchesPattern(String pattern, String filePath) {
        try {
            PathMatcher matcher = FileSystems.getDefault()
                    .getPathMatcher("glob:" + pattern);
            return matcher.matches(Path.of(filePath));
        } catch (Exception e) {
            // Fallback to simple contains check
            return filePath.contains(pattern.replace("*", ""));
        }
    }
}
