package com.claudecode.services.skills;

import com.claudecode.core.engine.AbortController;
import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SkillToolTest {

    @TempDir
    Path tempDir;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private SkillLoader loader;
    private ShellVariableInjector injector;
    private ToolExecutionContext context;

    @BeforeEach
    void setUp() {
        loader = new SkillLoader();
        injector = new ShellVariableInjector();
        context = ToolExecutionContext.of(new AbortController(), "test-session");
    }

    @Test
    void callReturnsSkillContent() throws IOException {
        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("test.md"), """
                ---
                name: test-skill
                ---
                This is the skill content.
                """);

        loader.addSource(Skill.SkillSource.PROJECT, skillsDir);
        SkillTool tool = new SkillTool(loader, injector);

        ObjectNode input = MAPPER.createObjectNode();
        input.put("name", "test-skill");

        String result = tool.call(input, context);
        assertEquals("This is the skill content.", result);
    }

    @Test
    void callReturnsNotFoundForMissingSkill() {
        SkillTool tool = new SkillTool(loader, injector);

        ObjectNode input = MAPPER.createObjectNode();
        input.put("name", "nonexistent");

        String result = tool.call(input, context);
        assertTrue(result.contains("not found"));
    }

    @Test
    void callReturnsErrorForMissingName() {
        SkillTool tool = new SkillTool(loader, injector);

        ObjectNode input = MAPPER.createObjectNode();
        String result = tool.call(input, context);
        assertTrue(result.contains("Error"));
    }

    @Test
    void toolMetadata() {
        SkillTool tool = new SkillTool(loader, injector);
        assertEquals("Skill", tool.name());
        assertNotNull(tool.description());
        assertNotNull(tool.inputSchema());
        assertTrue(tool.isReadOnly());
    }
}
