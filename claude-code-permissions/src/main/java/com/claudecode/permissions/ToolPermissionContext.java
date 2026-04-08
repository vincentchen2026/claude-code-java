package com.claudecode.permissions;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Immutable context for permission checking, containing the current mode,
 * active rules, working directory, and additional directories.
 * <p>
 * All mutation methods return a new instance (immutable update pattern).
 */
public record ToolPermissionContext(
    Path workingDirectory,
    PermissionMode mode,
    List<PermissionRule> rules,
    List<Path> additionalDirs
) {

    /**
     * Compact constructor ensuring defensive copies of mutable collections.
     */
    public ToolPermissionContext {
        rules = List.copyOf(rules);
        additionalDirs = List.copyOf(additionalDirs);
    }

    /**
     * Creates a minimal context with just a working directory and default mode.
     */
    public static ToolPermissionContext of(Path workingDirectory) {
        return new ToolPermissionContext(workingDirectory, PermissionMode.DEFAULT, List.of(), List.of());
    }

    /**
     * Returns a new builder pre-populated with this context's values.
     */
    public Builder toBuilder() {
        return new Builder()
            .workingDirectory(workingDirectory)
            .mode(mode)
            .rules(rules)
            .additionalDirs(additionalDirs);
    }

    /**
     * Returns a new context with the given rules added.
     */
    public ToolPermissionContext addRules(List<PermissionRule> newRules) {
        List<PermissionRule> merged = new ArrayList<>(this.rules);
        merged.addAll(newRules);
        return new ToolPermissionContext(workingDirectory, mode, merged, additionalDirs);
    }

    /**
     * Returns a new context with all rules replaced.
     */
    public ToolPermissionContext replaceRules(List<PermissionRule> newRules) {
        return new ToolPermissionContext(workingDirectory, mode, newRules, additionalDirs);
    }

    /**
     * Returns a new context with rules matching the predicate removed.
     */
    public ToolPermissionContext removeRules(Predicate<PermissionRule> filter) {
        List<PermissionRule> remaining = this.rules.stream()
            .filter(filter.negate())
            .toList();
        return new ToolPermissionContext(workingDirectory, mode, remaining, additionalDirs);
    }

    /**
     * Returns a new context with the mode changed.
     */
    public ToolPermissionContext setMode(PermissionMode newMode) {
        return new ToolPermissionContext(workingDirectory, newMode, rules, additionalDirs);
    }

    /**
     * Returns a new context with additional directories added.
     */
    public ToolPermissionContext addDirectories(List<Path> dirs) {
        List<Path> merged = new ArrayList<>(this.additionalDirs);
        merged.addAll(dirs);
        return new ToolPermissionContext(workingDirectory, mode, rules, merged);
    }

    /**
     * Returns a new context with the specified directories removed.
     */
    public ToolPermissionContext removeDirectories(List<Path> dirs) {
        List<Path> remaining = this.additionalDirs.stream()
            .filter(d -> !dirs.contains(d))
            .toList();
        return new ToolPermissionContext(workingDirectory, mode, rules, remaining);
    }

    /**
     * Builder for constructing ToolPermissionContext instances.
     */
    public static class Builder {
        private Path workingDirectory = Path.of(".");
        private PermissionMode mode = PermissionMode.DEFAULT;
        private List<PermissionRule> rules = new ArrayList<>();
        private List<Path> additionalDirs = new ArrayList<>();

        public Builder workingDirectory(Path workingDirectory) {
            this.workingDirectory = workingDirectory;
            return this;
        }

        public Builder mode(PermissionMode mode) {
            this.mode = mode;
            return this;
        }

        public Builder rules(List<PermissionRule> rules) {
            this.rules = new ArrayList<>(rules);
            return this;
        }

        public Builder addRule(PermissionRule rule) {
            this.rules.add(rule);
            return this;
        }

        public Builder additionalDirs(List<Path> additionalDirs) {
            this.additionalDirs = new ArrayList<>(additionalDirs);
            return this;
        }

        public Builder addDir(Path dir) {
            this.additionalDirs.add(dir);
            return this;
        }

        public ToolPermissionContext build() {
            return new ToolPermissionContext(workingDirectory, mode, rules, additionalDirs);
        }
    }

    /**
     * Creates a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }
}
