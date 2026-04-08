package com.claudecode.services.skills;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Discovers skills by walking up from a file path to the working directory,
 * finding .claude/skills/ directories along the way.
 */
public class SkillDiscovery {

    private static final Logger LOG = LoggerFactory.getLogger(SkillDiscovery.class);
    private static final String SKILLS_DIR = ".claude/skills";

    private final SkillLoader skillLoader;
    private final Path workingDirectory;

    public SkillDiscovery(SkillLoader skillLoader, Path workingDirectory) {
        this.skillLoader = skillLoader;
        this.workingDirectory = workingDirectory.toAbsolutePath().normalize();
    }

    /**
     * Discover skills relevant to a given file path by walking up the directory tree
     * from the file's parent to the working directory, collecting skills from
     * .claude/skills/ directories found along the way.
     *
     * @param filePath the file path to discover skills for
     * @return list of discovered skills (closest directories first)
     */
    public List<Skill> discoverForFile(Path filePath) {
        Path absFile = filePath.toAbsolutePath().normalize();
        Path current = absFile.getParent();

        if (current == null) {
            return List.of();
        }

        List<Skill> allSkills = new ArrayList<>();

        while (current != null && current.startsWith(workingDirectory)) {
            Path skillsDir = current.resolve(SKILLS_DIR);
            if (Files.isDirectory(skillsDir)) {
                List<Skill> skills = skillLoader.loadFromDirectory(
                        skillsDir, Skill.SkillSource.PROJECT);
                allSkills.addAll(skills);
            }
            current = current.getParent();
        }

        // Also check the working directory itself
        Path cwdSkillsDir = workingDirectory.resolve(SKILLS_DIR);
        if (Files.isDirectory(cwdSkillsDir) && !allSkills.stream()
                .anyMatch(s -> s.sourceFile() != null
                        && s.sourceFile().startsWith(cwdSkillsDir))) {
            allSkills.addAll(skillLoader.loadFromDirectory(
                    cwdSkillsDir, Skill.SkillSource.PROJECT));
        }

        return allSkills;
    }

    /**
     * Discover all skills directories from working directory.
     *
     * @return list of paths to .claude/skills/ directories
     */
    public List<Path> findSkillDirectories() {
        List<Path> dirs = new ArrayList<>();
        Path skillsDir = workingDirectory.resolve(SKILLS_DIR);
        if (Files.isDirectory(skillsDir)) {
            dirs.add(skillsDir);
        }
        return dirs;
    }
}
