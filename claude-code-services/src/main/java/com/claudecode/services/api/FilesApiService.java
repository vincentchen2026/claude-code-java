package com.claudecode.services.api;

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

public class FilesApiService {

    private static final Logger log = LoggerFactory.getLogger(FilesApiService.class);

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;

    public FilesApiService(String baseUrl, String apiKey) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.baseUrl = baseUrl != null ? baseUrl : "https://api.anthropic.com";
        this.apiKey = apiKey;
    }

    public FilesApiService(String baseUrl, String apiKey, HttpClient httpClient) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl != null ? baseUrl : "https://api.anthropic.com";
        this.apiKey = apiKey;
    }

    public UploadResult uploadFile(Path filePath, String purpose) throws IOException {
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + filePath);
        }

        String filename = filePath.getFileName().toString();
        byte[] fileContent;
        try {
            fileContent = Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new IOException("Failed to read file: " + filePath, e);
        }

        String boundary = "Boundary-" + System.currentTimeMillis();
        String mimeType = guessMimeType(filename);

        String body = buildMultipartBody(boundary, filename, mimeType, fileContent, purpose);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/files"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(120))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                return new UploadResult(true, response.body(), null, response.statusCode());
            } else {
                return new UploadResult(false, null, response.body(), response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Upload interrupted", e);
        }
    }

    public CompletableFuture<DownloadResult> downloadFile(String fileId, Path destPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/files/" + fileId + "/content"))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .timeout(Duration.ofSeconds(60))
                    .build();

                HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

                if (response.statusCode() == 200) {
                    Files.write(destPath, response.body());
                    return new DownloadResult(true, destPath, null, response.statusCode());
                } else {
                    return new DownloadResult(false, null, "HTTP " + response.statusCode(), response.statusCode());
                }
            } catch (Exception e) {
                return new DownloadResult(false, null, e.getMessage(), -1);
            }
        });
    }

    public DeleteResult deleteFile(String fileId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/files/" + fileId))
                .header("Authorization", "Bearer " + apiKey)
                .DELETE()
                .timeout(Duration.ofSeconds(30))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 204) {
                return new DeleteResult(true, null, response.statusCode());
            } else {
                return new DeleteResult(false, response.body(), response.statusCode());
            }
        } catch (Exception e) {
            return new DeleteResult(false, e.getMessage(), -1);
        }
    }

    private String buildMultipartBody(String boundary, String filename, String mimeType, byte[] content, String purpose) {
        StringBuilder sb = new StringBuilder();
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(filename).append("\"\r\n");
        sb.append("Content-Type: ").append(mimeType).append("\r\n\r\n");
        sb.append(new String(content));
        sb.append("\r\n");
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"purpose\"\r\n\r\n");
        sb.append(purpose).append("\r\n");
        sb.append("--").append(boundary).append("--\r\n");
        return sb.toString();
    }

    private String guessMimeType(String filename) {
        if (filename.endsWith(".txt")) return "text/plain";
        if (filename.endsWith(".json")) return "application/json";
        if (filename.endsWith(".pdf")) return "application/pdf";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".zip")) return "application/zip";
        return "application/octet-stream";
    }

    public record UploadResult(boolean success, String responseBody, String errorMessage, int statusCode) {}
    public record DownloadResult(boolean success, Path filePath, String errorMessage, int statusCode) {}
    public record DeleteResult(boolean success, String errorMessage, int statusCode) {}
}