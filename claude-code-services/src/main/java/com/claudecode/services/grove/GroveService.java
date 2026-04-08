package com.claudecode.services.grove;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GroveService {

    private static final Logger log = LoggerFactory.getLogger(GroveService.class);

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;

    public GroveService(String baseUrl, String apiKey) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.baseUrl = baseUrl != null ? baseUrl : "https://api.anthropic.com/v1";
        this.apiKey = apiKey;
    }

    public GroveService(String baseUrl, String apiKey, HttpClient httpClient) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl != null ? baseUrl : "https://api.anthropic.com/v1";
        this.apiKey = apiKey;
    }

    public RepositoryInfo getRepositoryInfo(String repoId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/repositories/" + repoId))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return new RepositoryInfo(repoId, response.body(), true);
            } else {
                return new RepositoryInfo(repoId, response.body(), false);
            }
        } catch (Exception e) {
            log.error("Failed to get repository info for {}", repoId, e);
            return new RepositoryInfo(repoId, e.getMessage(), false);
        }
    }

    public BranchList listBranches(String repoId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/repositories/" + repoId + "/branches"))
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return new BranchList(repoId, List.of(), true, null);
            } else {
                return new BranchList(repoId, List.of(), false, response.body());
            }
        } catch (Exception e) {
            log.error("Failed to list branches for {}", repoId, e);
            return new BranchList(repoId, List.of(), false, e.getMessage());
        }
    }

    public CommitInfo getCommit(String repoId, String commitSha) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/repositories/" + repoId + "/commits/" + commitSha))
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return new CommitInfo(commitSha, response.body(), true);
            } else {
                return new CommitInfo(commitSha, response.body(), false);
            }
        } catch (Exception e) {
            log.error("Failed to get commit {} in {}", commitSha, repoId, e);
            return new CommitInfo(commitSha, e.getMessage(), false);
        }
    }

    public DiffResult getDiff(String repoId, String baseRef, String headRef) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/repositories/" + repoId + "/diff?base=" + baseRef + "&head=" + headRef))
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .timeout(Duration.ofSeconds(60))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return new DiffResult(baseRef, headRef, response.body(), true);
            } else {
                return new DiffResult(baseRef, headRef, response.body(), false);
            }
        } catch (Exception e) {
            log.error("Failed to get diff between {} and {} in {}", baseRef, headRef, repoId, e);
            return new DiffResult(baseRef, headRef, e.getMessage(), false);
        }
    }

    public PathDiffsResult getPathDiffs(String repoId, String commitSha, List<String> paths) {
        try {
            String pathsParam = String.join(",", paths);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/repositories/" + repoId + "/commits/" + commitSha + "/diff?paths=" + pathsParam))
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .timeout(Duration.ofSeconds(60))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return new PathDiffsResult(paths, response.body(), true);
            } else {
                return new PathDiffsResult(paths, response.body(), false);
            }
        } catch (Exception e) {
            log.error("Failed to get path diffs for {} in {}", paths, repoId, e);
            return new PathDiffsResult(paths, e.getMessage(), false);
        }
    }

    public CompletableFuture<RepositoryInfo> getRepositoryInfoAsync(String repoId) {
        return CompletableFuture.supplyAsync(() -> getRepositoryInfo(repoId));
    }

    public record RepositoryInfo(String repoId, String response, boolean success) {}
    public record BranchList(String repoId, List<String> branches, boolean success, String errorMessage) {}
    public record CommitInfo(String commitSha, String response, boolean success) {}
    public record DiffResult(String baseRef, String headRef, String diff, boolean success) {}
    public record PathDiffsResult(List<String> paths, String diffs, boolean success) {}
}