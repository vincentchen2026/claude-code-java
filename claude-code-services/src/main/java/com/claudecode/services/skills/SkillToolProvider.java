package com.claudecode.services.skills;

import com.claudecode.tools.Tool;
import com.claudecode.tools.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Discovers and registers skill-related tools into the tool registry.
 * Initializes SkillLoader with configured source paths.
 */
public class SkillToolProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SkillToolProvider.class);

    private final SkillLoader skillLoader;
    private final ShellVariableInjector variableInjector;
    private boolean initialized = false;

    public SkillToolProvider() {
        this.skillLoader = new SkillLoader();
        this.variableInjector = new ShellVariableInjector();
    }

    public SkillToolProvider(SkillLoader skillLoader, ShellVariableInjector variableInjector) {
        this.skillLoader = skillLoader;
        this.variableInjector = variableInjector;
    }

    /**
     * Initializes skill sources and registers tools.
     * Should be called once during startup.
     *
     * @param projectDir the workspace root directory
     * @param registry  the tool registry to register tools into
     */
    public void initialize(Path projectDir, ToolRegistry registry) {
        if (initialized) {
            LOG.warn("SkillToolProvider already initialized");
            return;
        }

        // Configure source paths
        Path homeDir = Path.of(System.getProperty("user.home"));

        // Managed skills (bundled with application)
        Path managedDir = homeDir.resolve(".claude").resolve("skills").resolve("managed");
        skillLoader.addSource(Skill.SkillSource.MANAGED, managedDir);

        // User skills
        Path userDir = homeDir.resolve(".claude").resolve("skills").resolve("user");
        skillLoader.addSource(Skill.SkillSource.USER, userDir);

        // Project skills
        if (projectDir != null) {
            Path projectSkillsDir = projectDir.resolve(".claude").resolve("skills");
            skillLoader.addSource(Skill.SkillSource.PROJECT, projectSkillsDir);
        }

        // Bundled skills (classpath resources)
        Path bundledDir = resolveBundledSkillsDir();
        if (bundledDir != null) {
            skillLoader.addSource(Skill.SkillSource.BUNDLED, bundledDir);
        }

        // Set shell variables
        variableInjector.setSkillDir(userDir);

        // Register tools
        registry.register(new SkillTool(skillLoader, variableInjector));
        registry.register(new DiscoverSkillsTool(skillLoader));

        // Pre-load skills to verify configuration
        try {
            var skills = skillLoader.loadAll();
            LOG.info("Skill tools initialized: {} skills loaded", skills.size());
        } catch (Exception e) {
            LOG.warn("Failed to pre-load skills during initialization", e);
        }

        initialized = true;
    }

    /**
     * Returns the skill loader for direct access if needed.
     */
    public SkillLoader getSkillLoader() {
        return skillLoader;
    }

    /**
     * Returns the variable injector for direct access if needed.
     */
    public ShellVariableInjector getVariableInjector() {
        return variableInjector;
    }

    /**
     * Returns the directory for bundled skills, if it exists.
     */
    private Path resolveBundledSkillsDir() {
        // Try to resolve from classpath
        try {
            Path bundledDir = Path.of(System.getProperty("user.dir"), "skills");
            if (Files.isDirectory(bundledDir)) {
                return bundledDir;
            }
        } catch (Exception e) {
            LOG.debug("Could not resolve bundled skills directory", e);
        }
        return null;
    }

}
