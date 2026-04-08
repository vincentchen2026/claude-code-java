package com.claudecode.services.skills;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class RemoteSkillLoader {

    private static final Logger log = LoggerFactory.getLogger(RemoteSkillLoader.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Path cacheDir;

    public RemoteSkillLoader(Path cacheDir) {
        this.cacheDir = cacheDir;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public CompletableFuture<Skill> loadFromUrl(String skillId, URI url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Loading skill {} from {}", skillId, url);

                HttpRequest request = HttpRequest.newBuilder(url)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new SkillLoadException("Failed to load skill: HTTP " + response.statusCode());
                }

                SkillMetadata metadata = objectMapper.readValue(response.body(), SkillMetadata.class);
                
                return new Skill(
                    skillId,
                    metadata.name(),
                    metadata.description(),
                    response.body(),
                    SkillSource.REMOTE
                );

            } catch (Exception e) {
                log.error("Failed to load skill {} from {}", skillId, url, e);
                throw new SkillLoadException("Failed to load skill: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<Skill> loadFromRegistry(String skillId, String registryUrl) {
        try {
            URI skillUri = URI.create(registryUrl + "/skills/" + skillId);
            return loadFromUrl(skillId, skillUri);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new SkillLoadException("Invalid registry URL", e));
        }
    }

    public void cacheSkill(Skill skill) throws IOException {
        Path cachePath = cacheDir.resolve(skill.id() + ".json");
        Files.createDirectories(cacheDir);
        Files.writeString(cachePath, skill.content());
        log.debug("Cached skill {} to {}", skill.id(), cachePath);
    }

    public Skill loadFromCache(String skillId) throws IOException {
        Path cachePath = cacheDir.resolve(skillId + ".json");
        if (!Files.exists(cachePath)) {
            return null;
        }
        String content = Files.readString(cachePath);
        return new Skill(skillId, skillId, null, content, SkillSource.CACHED);
    }

    public boolean isCached(String skillId) {
        return Files.exists(cacheDir.resolve(skillId + ".json"));
    }

    public void clearCache() throws IOException {
        if (Files.exists(cacheDir)) {
            try (var stream = Files.list(cacheDir)) {
                stream.filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            log.warn("Failed to delete cache file {}", p);
                        }
                    });
            }
        }
    }

    public enum SkillSource {
        REMOTE, CACHED, BUNDLED
    }

    public record Skill(
        String id,
        String name,
        String description,
        String content,
        SkillSource source
    ) {}

    public record SkillMetadata(
        String name,
        String description,
        String version,
        String[] tags
    ) {}

    public static class SkillLoadException extends RuntimeException {
        public SkillLoadException(String message) {
            super(message);
        }

        public SkillLoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}