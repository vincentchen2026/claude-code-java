package com.claudecode.services.skills;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SkillLoaderTest {

    @TempDir
    Path tempDir;

    private SkillLoader loader;

    @BeforeEach
    void setUp() {
        loader = new SkillLoader();
    }

    @Test
    void loadFromDirectoryWithSkillFiles() throws IOException {
        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);

        Files.writeString(skillsDir.resolve("coding.md"), """
                ---
                name: coding-standards
                description: Coding standards for the project
                ---
                Follow these coding standards...
                """);

        Files.writeString(skillsDir.resolve("review.md"), """
                ---
                name: code-review
                description: Code review guidelines
                ---
                When reviewing code...
                """);

        List<Skill> skills = loader.loadFromDirectory(skillsDir, Skill.SkillSource.PROJECT);

        assertEquals(2, skills.size());
    }

    @Test
    void loadFromNonExistentDirectory() {
        List<Skill> skills = loader.loadFromDirectory(
                tempDir.resolve("nonexistent"), Skill.SkillSource.PROJECT);
        assertTrue(skills.isEmpty());
    }

    @Test
    void loadAllFromMultipleSources() throws IOException {
        Path managedDir = tempDir.resolve("managed");
        Path userDir = tempDir.resolve("user");
        Files.createDirectories(managedDir);
        Files.createDirectories(userDir);

        Files.writeString(managedDir.resolve("base.md"), """
                ---
                name: base-skill
                description: Base skill
                ---
                Base content.
                """);

        Files.writeString(userDir.resolve("custom.md"), """
                ---
                name: custom-skill
                description: Custom skill
                ---
                Custom content.
                """);

        loader.addSource(Skill.SkillSource.MANAGED, managedDir);
        loader.addSource(Skill.SkillSource.USER, userDir);

        List<Skill> skills = loader.loadAll();
        assertEquals(2, skills.size());
    }

    @Test
    void skillWithoutNameUseFilename() throws IOException {
        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);

        Files.writeString(skillsDir.resolve("unnamed.md"), """
                ---
                description: No name field
                ---
                Content without name.
                """);

        List<Skill> skills = loader.loadFromDirectory(skillsDir, Skill.SkillSource.PROJECT);
        assertEquals(1, skills.size());
        assertEquals("unnamed", skills.get(0).name());
    }

    @Test
    void laterSourceOverridesEarlier() throws IOException {
        Path dir1 = tempDir.resolve("dir1");
        Path dir2 = tempDir.resolve("dir2");
        Files.createDirectories(dir1);
        Files.createDirectories(dir2);

        Files.writeString(dir1.resolve("skill.md"), """
                ---
                name: shared-skill
                description: From dir1
                ---
                Content 1.
                """);

        Files.writeString(dir2.resolve("skill.md"), """
                ---
                name: shared-skill
                description: From dir2
                ---
                Content 2.
                """);

        loader.addSource(Skill.SkillSource.MANAGED, dir1);
        loader.addSource(Skill.SkillSource.USER, dir2);

        List<Skill> skills = loader.loadAll();
        assertEquals(1, skills.size());
        assertEquals("From dir2", skills.get(0).description());
    }
}
