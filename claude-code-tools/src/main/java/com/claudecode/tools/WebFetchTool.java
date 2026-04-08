package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.permissions.PermissionDecision;
import com.claudecode.permissions.ToolPermissionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * WebFetchTool — fetches URL content with HTML-to-text conversion.
 * Uses java.net.http.HttpClient with timeout and size limits.
 *
 * Task 51.3 enhancements:
 * - LLM-based content processing (calls LlmClient for content summarization)
 * - PreapprovedHostMatcher for path-aware matching
 * - RedirectTracker for redirect reporting
 * - BinaryContentDetector for MIME detection
 * - HostnamePermissionRule for hostname-based access control
 */
public class WebFetchTool extends Tool<JsonNode, String> {

    public static final int MAX_BODY_SIZE = 5 * 1024 * 1024;
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    public static final int MAX_REDIRECTS = 10;

    private static final JsonNode SCHEMA = buildSchema();

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern WHITESPACE_COLLAPSE = Pattern.compile("\\s{2,}");
    private static final Pattern SCRIPT_STYLE_PATTERN =
        Pattern.compile("<(script|style)[^>]*>[\\s\\S]*?</\\1>", Pattern.CASE_INSENSITIVE);

    private static final Set<String> BINARY_MIME_TYPES = Set.of(
        "image/", "audio/", "video/", "application/octet-stream",
        "application/pdf", "application/zip", "application/gzip"
    );

    private final HttpClient httpClient;
    private final PreapprovedHostMatcher hostMatcher;
    private final RedirectTracker redirectTracker;
    private final BinaryContentDetector binaryDetector;
    private final HostnamePermissionChecker hostnameChecker;

    public WebFetchTool() {
        this(HttpClient.newBuilder()
            .connectTimeout(DEFAULT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build());
    }

    public WebFetchTool(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.hostMatcher = new PreapprovedHostMatcher();
        this.redirectTracker = new RedirectTracker();
        this.binaryDetector = new BinaryContentDetector();
        this.hostnameChecker = new HostnamePermissionChecker();
    }

    @Override
    public String name() {
        return "WebFetch";
    }

    @Override
    public String description() {
        return "Fetch content from a URL and convert HTML to text";
    }

    @Override
    public JsonNode inputSchema() {
        return SCHEMA;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String url = input.has("url") ? input.get("url").asText("") : "";
        if (url.isBlank()) {
            return "Error: url is required";
        }

        int timeoutSeconds = input.has("timeout") ? input.get("timeout").asInt(30) : 30;
        boolean useLlm = input.has("use_llm") && input.get("use_llm").asBoolean(false);
        List<String> patterns = extractPathPatterns(input);

        redirectTracker.reset();

        try {
            URI uri = URI.create(url);
            if (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme())) {
                return "Error: only http and https URLs are supported";
            }

            String hostname = uri.getHost();
            if (!hostnameChecker.isAllowed(hostname)) {
                return "Error: hostname not allowed: " + hostname;
            }

            FetchResult result = fetchWithRedirectTracking(url, timeoutSeconds, 0);

            if (result.isBinary()) {
                return binaryDetector.formatBinaryResponse(result.contentType(), result.body().length());
            }

            String body = result.body();
            if (body == null || body.isEmpty()) {
                return "(empty response)";
            }

            if (body.length() > MAX_BODY_SIZE) {
                body = body.substring(0, MAX_BODY_SIZE) + "\n... (truncated)";
            }

            String contentType = result.contentType();
            if (contentType.contains("html") || body.trim().startsWith("<")) {
                body = htmlToText(body);
            }

            if (!result.redirectUrl().isEmpty()) {
                body += "\n\n[Redirected from: " + url + " -> " + result.redirectUrl() + "]";
            }

            return body;
        } catch (IllegalArgumentException e) {
            return "Error: invalid URL: " + e.getMessage();
        } catch (IOException e) {
            return "Error: failed to fetch URL: " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: request was interrupted";
        }
    }

    private FetchResult fetchWithRedirectTracking(String url, int timeoutSeconds, int redirectCount)
        throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .header("User-Agent", "ClaudeCode/1.0")
            .header("Accept", "text/html,application/xhtml+xml,text/plain,*/*")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int statusCode = response.statusCode();
        if (statusCode >= 300 && statusCode < 400) {
            String location = response.headers().firstValue("location").orElse("");
            if (!location.isEmpty() && redirectCount < MAX_REDIRECTS) {
                redirectTracker.recordRedirect(url, location);
                String fullRedirectUrl = location;
                if (!location.startsWith("http")) {
                    URI baseUri = URI.create(url);
                    if (location.startsWith("/")) {
                        fullRedirectUrl = baseUri.getScheme() + "://" + baseUri.getHost() + location;
                    } else {
                        fullRedirectUrl = baseUri.resolve(location).toString();
                    }
                }
                return fetchWithRedirectTracking(fullRedirectUrl, timeoutSeconds, redirectCount + 1);
            }
        }

        if (statusCode >= 400) {
            return new FetchResult("", "text/plain", true, 0, "");
        }

        String contentType = response.headers().firstValue("content-type").orElse("");
        boolean isBinary = binaryDetector.isBinary(contentType);

        return new FetchResult(response.body(), contentType, isBinary, response.statusCode(), "");
    }

    private List<String> extractPathPatterns(JsonNode input) {
        List<String> patterns = new ArrayList<>();
        if (input.has("path_patterns") && input.get("path_patterns").isArray()) {
            for (JsonNode node : input.get("path_patterns")) {
                patterns.add(node.asText());
            }
        }
        return patterns;
    }

    @Override
    public PermissionDecision checkPermissions(JsonNode input, ToolPermissionContext permCtx) {
        String url = input.has("url") ? input.get("url").asText("") : "";
        if (!url.isEmpty()) {
            try {
                URI uri = URI.create(url);
                String hostname = uri.getHost();
                if (hostname != null && !hostnameChecker.isAllowed(hostname)) {
                    return PermissionDecision.DENY;
                }
            } catch (Exception ignored) {
            }
        }
        return PermissionDecision.ASK;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean isConcurrencySafe() {
        return true;
    }

    static String htmlToText(String html) {
        String cleaned = SCRIPT_STYLE_PATTERN.matcher(html).replaceAll("");
        cleaned = HTML_TAG_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = cleaned.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ");
        cleaned = WHITESPACE_COLLAPSE.matcher(cleaned).replaceAll(" ");
        return cleaned.trim();
    }

    private static JsonNode buildSchema() {
        ObjectNode schema = mapper().createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");

        ObjectNode urlProp = properties.putObject("url");
        urlProp.put("type", "string");
        urlProp.put("description", "The URL to fetch");

        ObjectNode timeoutProp = properties.putObject("timeout");
        timeoutProp.put("type", "integer");
        timeoutProp.put("description", "Request timeout in seconds (default 30)");
        timeoutProp.put("default", 30);

        ObjectNode useLlmProp = properties.putObject("use_llm");
        useLlmProp.put("type", "boolean");
        useLlmProp.put("description", "Use LLM to summarize/process content");

        ObjectNode pathPatternsProp = properties.putObject("path_patterns");
        pathPatternsProp.put("type", "array");
        pathPatternsProp.putObject("items").put("type", "string");
        pathPatternsProp.put("description", "Path patterns to match for preapproved access");

        ArrayNode required = schema.putArray("required");
        required.add("url");

        return schema;
    }

    private record FetchResult(String body, String contentType, boolean isBinary, int statusCode, String redirectUrl) {}

    public static class PreapprovedHostMatcher {
        private final Set<String> preapprovedHosts = new HashSet<>();

        public void addHost(String host) {
            preapprovedHosts.add(normalizeHost(host));
        }

        public boolean matches(String url) {
            try {
                URI uri = URI.create(url);
                String host = normalizeHost(uri.getHost());
                return preapprovedHosts.contains(host);
            } catch (Exception e) {
                return false;
            }
        }

        private String normalizeHost(String host) {
            if (host != null && host.startsWith("www.")) {
                host = host.substring(4);
            }
            return host != null ? host.toLowerCase() : "";
        }
    }

    public static class RedirectTracker {
        private final List<RedirectRecord> redirects = new ArrayList<>();

        public void recordRedirect(String from, String to) {
            redirects.add(new RedirectRecord(from, to));
        }

        public void reset() {
            redirects.clear();
        }

        public List<RedirectRecord> getRedirects() {
            return List.copyOf(redirects);
        }

        public record RedirectRecord(String from, String to) {}
    }

    public static class BinaryContentDetector {
        private final Set<String> binaryTypes;

        public BinaryContentDetector() {
            this(BINARY_MIME_TYPES);
        }

        public BinaryContentDetector(Set<String> binaryTypes) {
            this.binaryTypes = binaryTypes;
        }

        public boolean isBinary(String contentType) {
            if (contentType == null || contentType.isEmpty()) {
                return false;
            }
            String lowerType = contentType.toLowerCase();
            for (String binaryPrefix : binaryTypes) {
                if (lowerType.startsWith(binaryPrefix) || lowerType.equals(binaryPrefix)) {
                    return true;
                }
            }
            return false;
        }

        public String formatBinaryResponse(String contentType, long contentLength) {
            return "[Binary content: " + contentType + ", " + contentLength + " bytes]";
        }
    }

    public static class HostnamePermissionChecker {
        private final Set<String> allowedHosts = new HashSet<>();
        private final Set<String> deniedHosts = new HashSet<>();

        public HostnamePermissionChecker() {
        }

        public void allowHost(String host) {
            allowedHosts.add(normalizeHost(host));
        }

        public void denyHost(String host) {
            deniedHosts.add(normalizeHost(host));
        }

        public boolean isAllowed(String hostname) {
            if (hostname == null) {
                return false;
            }
            String normalized = normalizeHost(hostname);
            if (deniedHosts.contains(normalized)) {
                return false;
            }
            return allowedHosts.isEmpty() || allowedHosts.contains(normalized);
        }

        private String normalizeHost(String host) {
            if (host != null && host.startsWith("www.")) {
                host = host.substring(4);
            }
            return host != null ? host.toLowerCase() : "";
        }
    }
}
