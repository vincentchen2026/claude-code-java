package com.claudecode.services.skills;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SkillDiscoveryTest {

    @TempDir
    Path tempDir;

    private SkillLoader loader;

    @BeforeEach
    void setUp() {
        loader = new SkillLoader();
    }

    @Test
    void discoverSkillsFromParentDirectories() throws IOException {
        // Create .claude/skills/ at project root
        Path skillsDir = tempDir.resolve(".claude/skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("root-skill.md"), """
                ---
                name: root-skill
                description: Root level skill
                ---
                Root skill content.
                """);

        // Create a nested file
        Path nestedDir = tempDir.resolve("src/main/java");
        Files.createDirectories(nestedDir);
        Path nestedFile = nestedDir.resolve("App.java");
        Files.writeString(nestedFile, "class App {}");

        SkillDiscovery discovery = new SkillDiscovery(loader, tempDir);
        List<Skill> skills = discovery.discoverForFile(nestedFile);

        assertFalse(skills.isEmpty());
        assertEquals("root-skill", skills.get(0).name());
    }

    @Test
    void findSkillDirectories() throws IOException {
        Path skillsDir = tempDir.resolve(".claude/skills");
        Files.createDirectories(skillsDir);

        SkillDiscovery discovery = new SkillDiscovery(loader, tempDir);
        List<Path> dirs = discovery.findSkillDirectories();

        assertEquals(1, dirs.size());
    }

    @Test
    void noSkillDirectories() {
        SkillDiscovery discovery = new SkillDiscovery(loader, tempDir);
        List<Path> dirs = discovery.findSkillDirectories();
        assertTrue(dirs.isEmpty());
    }
}
