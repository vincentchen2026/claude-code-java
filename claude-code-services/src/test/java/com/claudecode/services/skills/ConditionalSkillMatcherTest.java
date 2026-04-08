package com.claudecode.services.skills;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConditionalSkillMatcherTest {

    private final ConditionalSkillMatcher matcher = new ConditionalSkillMatcher();

    @Test
    void nonConditionalSkillAlwaysMatches() {
        Skill skill = new Skill("test", "desc", List.of(), List.of(),
                "content", null, Skill.SkillSource.PROJECT);

        assertTrue(matcher.matches(skill, Path.of("any/file.java")));
        assertTrue(matcher.matches(skill, null));
    }

    @Test
    void conditionalSkillMatchesPattern() {
        Skill skill = new Skill("java-skill", "desc", List.of(),
                List.of("**.java"), "content", null, Skill.SkillSource.PROJECT);

        assertTrue(matcher.matches(skill, Path.of("src/Main.java")));
    }

    @Test
    void conditionalSkillDoesNotMatchNullPath() {
        Skill skill = new Skill("java-skill", "desc", List.of(),
                List.of("*.java"), "content", null, Skill.SkillSource.PROJECT);

        assertFalse(matcher.matches(skill, null));
    }

    @Test
    void filterMatchingSkills() {
        Skill unconditional = new Skill("all", "desc", List.of(), List.of(),
                "content", null, Skill.SkillSource.PROJECT);
        Skill javaOnly = new Skill("java", "desc", List.of(),
                List.of("**.java"), "content", null, Skill.SkillSource.PROJECT);

        List<Skill> filtered = matcher.filterMatching(
                List.of(unconditional, javaOnly), Path.of("test.ts"));

        // Unconditional always matches; java-only won't match .ts
        assertEquals(1, filtered.size());
        assertEquals("all", filtered.get(0).name());
    }
}
