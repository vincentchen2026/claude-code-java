package com.claudecode.services.skills;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Loads skills from multiple sources: managed, user, project, bundled, and MCP paths.
 * Each source is a directory containing .md skill files with YAML frontmatter.
 */
public class SkillLoader {

    private static final Logger LOG = LoggerFactory.getLogger(SkillLoader.class);
    private static final String SKILL_EXTENSION = ".md";

    private final FrontmatterParser frontmatterParser;
    private final Map<Skill.SkillSource, Path> sourcePaths;

    public SkillLoader() {
        this(new FrontmatterParser(), new LinkedHashMap<>());
    }

    public SkillLoader(FrontmatterParser frontmatterParser, Map<Skill.SkillSource, Path> sourcePaths) {
        this.frontmatterParser = frontmatterParser;
        this.sourcePaths = new LinkedHashMap<>(sourcePaths);
    }

    /**
     * Configure a source path for skill loading.
     */
    public void addSource(Skill.SkillSource source, Path directory) {
        sourcePaths.put(source, directory);
    }

    /**
     * Load all skills from all configured sources.
     * Skills from later sources override earlier ones with the same name.
     *
     * @return list of loaded skills
     */
    public List<Skill> loadAll() {
        Map<String, Skill> skillsByName = new LinkedHashMap<>();

        for (var entry : sourcePaths.entrySet()) {
            List<Skill> skills = loadFromDirectory(entry.getValue(), entry.getKey());
            for (Skill skill : skills) {
                skillsByName.put(skill.name(), skill);
            }
        }

        return List.copyOf(skillsByName.values());
    }

    /**
     * Load skills from a single directory.
     *
     * @param directory the directory to scan
     * @param source    the source type
     * @return list of skills found
     */
    public List<Skill> loadFromDirectory(Path directory, Skill.SkillSource source) {
        if (directory == null || !Files.isDirectory(directory)) {
            return List.of();
        }

        List<Skill> skills = new ArrayList<>();
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(p -> p.toString().endsWith(SKILL_EXTENSION))
                 .filter(Files::isRegularFile)
                 .forEach(file -> {
                     try {
                         Skill skill = loadSkillFile(file, source);
                         if (skill != null) {
                             skills.add(skill);
                         }
                     } catch (IOException e) {
                         LOG.warn("Failed to load skill file: {}", file, e);
                     }
                 });
        } catch (IOException e) {
            LOG.warn("Failed to list skill directory: {}", directory, e);
        }

        return skills;
    }

    /**
     * Load a single skill file.
     */
    Skill loadSkillFile(Path file, Skill.SkillSource source) throws IOException {
        String content = Files.readString(file);
        FrontmatterParser.ParseResult parsed = frontmatterParser.parse(content);

        String name = parsed.name();
        if (name == null || name.isBlank()) {
            // Use filename without extension as name
            String fileName = file.getFileName().toString();
            name = fileName.substring(0, fileName.length() - SKILL_EXTENSION.length());
        }

        return new Skill(
                name,
                parsed.description(),
                parsed.allowedTools(),
                parsed.paths(),
                parsed.body(),
                file,
                source
        );
    }
}
