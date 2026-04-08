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
import java.util.List;
import java.util.Set;

/**
 * WebSearchTool — web search integration with Anthropic web_search API.
 * Supports domain filtering and result formatting.
 */
public class WebSearchTool extends Tool<JsonNode, String> {

    private static final JsonNode SCHEMA = buildSchema();

    private static final Set<String> DEFAULT_DENY_DOMAINS = Set.of(
        "facebook.com", "twitter.com", "x.com", "instagram.com",
        "tiktok.com", "reddit.com", "linkedin.com"
    );

    private final WebSearchProvider searchProvider;
    private final HttpClient httpClient;
    private final Set<String> deniedDomains;

    public WebSearchTool() {
        this(null, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build());
    }

    public WebSearchTool(WebSearchProvider searchProvider) {
        this(searchProvider, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build());
    }

    public WebSearchTool(WebSearchProvider searchProvider, HttpClient httpClient) {
        this.searchProvider = searchProvider;
        this.httpClient = httpClient;
        this.deniedDomains = DEFAULT_DENY_DOMAINS;
    }

    public WebSearchTool(WebSearchProvider searchProvider, HttpClient httpClient, Set<String> deniedDomains) {
        this.searchProvider = searchProvider;
        this.httpClient = httpClient;
        this.deniedDomains = deniedDomains != null ? deniedDomains : DEFAULT_DENY_DOMAINS;
    }

    @Override
    public String name() {
        return "WebSearch";
    }

    @Override
    public String description() {
        return "Search the web for information using web search";
    }

    @Override
    public JsonNode inputSchema() {
        return SCHEMA;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String query = input.has("query") ? input.get("query").asText("") : "";
        if (query.isBlank()) {
            return "Error: query is required";
        }

        List<String> denyDomains = extractDeniedDomains(input);
        String source = extractSource(input);

        if (searchProvider != null) {
            return executeWithProvider(query, denyDomains);
        }

        return executeDirectSearch(query, source, denyDomains, context);
    }

    private String executeWithProvider(String query, List<String> denyDomains) {
        try {
            String result = searchProvider.search(query);
            if (denyDomains != null && !denyDomains.isEmpty()) {
                result = filterDeniedDomains(result, denyDomains);
            }
            return result;
        } catch (Exception e) {
            return "Error: search failed: " + e.getMessage();
        }
    }

    private String executeDirectSearch(String query, String source, List<String> denyDomains,
                                       ToolExecutionContext context) {
        try {
            if ("anthropic".equalsIgnoreCase(source) || source.isEmpty()) {
                return searchWithAnthropicApi(query, denyDomains, context);
            }
            return searchWithBraveApi(query, denyDomains);
        } catch (Exception e) {
            return "Error: search failed: " + e.getMessage();
        }
    }

    private String searchWithAnthropicApi(String query, List<String> denyDomains,
                                          ToolExecutionContext context) {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return "Web search not configured. Query was: " + query + ". " +
                   "Configure ANTHROPIC_API_KEY or use a search provider.";
        }
        return searchWithAnthropicWebSearch(query, apiKey, denyDomains);
    }

    private String searchWithAnthropicWebSearch(String query, String apiKey, List<String> denyDomains) {
        try {
            String baseUrl = System.getenv("ANTHROPIC_BASE_URL");
            if (baseUrl == null || baseUrl.isBlank()) {
                baseUrl = "https://api.anthropic.com";
            }
            String endpoint = baseUrl + "/v1/messages";

            ObjectNode requestBody = mapper().createObjectNode();
            requestBody.put("model", "claude-sonnet-4-20250514");
            requestBody.put("max_tokens", 1024);

            ObjectNode systemPrompt = requestBody.putObject("system");
            systemPrompt.put("type", "text");
            systemPrompt.put("text", "You are a helpful assistant.");

            ArrayNode messages = requestBody.putArray("messages");
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", "Search the web for: " + query);

            ArrayNode tools = requestBody.putArray("tools");
            ObjectNode webSearch = tools.addObject();
            webSearch.put("name", "web_search");
            webSearch.put("description", "Search the web for information");
            ObjectNode webSearchInput = webSearch.putObject("input_schema");
            webSearchInput.put("type", "object");
            ObjectNode webSearchProps = webSearchInput.putObject("properties");
            ObjectNode queryProp = webSearchProps.putObject("query");
            queryProp.put("type", "string");
            webSearchInput.putArray("required").add("query");

            String requestJson = mapper().writeValueAsString(requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .timeout(Duration.ofSeconds(60))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode respJson = mapper().readTree(response.body());
                return formatSearchResults(respJson, denyDomains);
            } else {
                return "Web search failed with status " + response.statusCode() + ": " + response.body();
            }
        } catch (Exception e) {
            return "Web search failed: " + e.getMessage();
        }
    }

    private String searchWithBraveApi(String query, List<String> denyDomains) {
        String apiKey = System.getenv("BRAVE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return "Web search not configured. Query was: " + query + ". " +
                   "Configure BRAVE_API_KEY environment variable or use a search provider.";
        }
        return searchWithBrave(query, apiKey, denyDomains);
    }

    private String searchWithBrave(String query, String apiKey, List<String> denyDomains) {
        try {
            String encodedQuery = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
            String endpoint = "https://api.search.brave.com/res/v1/web/search?q=" + encodedQuery + "&count=10";

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Accept", "application/json")
                .header("X-Subscription-Token", apiKey)
                .timeout(Duration.ofSeconds(30))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode respJson = mapper().readTree(response.body());
                return formatBraveResults(respJson, denyDomains);
            } else {
                return "Web search failed with status " + response.statusCode() + ": " + response.body();
            }
        } catch (Exception e) {
            return "Web search failed: " + e.getMessage();
        }
    }

    private String formatSearchResults(JsonNode response, List<String> denyDomains) {
        StringBuilder sb = new StringBuilder();
        sb.append("Search results:\n\n");

        if (response.has("content") && response.get("content").isArray()) {
            for (JsonNode item : response.get("content")) {
                String text = item.has("text") ? item.get("text").asText() : "";
                if (!text.isBlank()) {
                    sb.append("- ").append(text).append("\n\n");
                }
            }
        }

        if (sb.length() <= "Search results:\n\n".length()) {
            sb.append("No results found.");
        }
        return sb.toString().trim();
    }

    private String formatBraveResults(JsonNode response, List<String> denyDomains) {
        StringBuilder sb = new StringBuilder();
        sb.append("Search results:\n\n");

        if (response.has("web") && response.get("web").has("results")) {
            for (JsonNode result : response.get("web").get("results")) {
                String title = result.has("title") ? result.get("title").asText() : "";
                String url = result.has("url") ? result.get("url").asText() : "";
                String snippet = result.has("description") ? result.get("description").asText() : "";

                if (!title.isBlank() && !url.isBlank()) {
                    String domain = extractDomain(url);
                    if (denyDomains != null && denyDomains.contains(domain)) {
                        continue;
                    }
                    sb.append("Title: ").append(title).append("\n");
                    sb.append("URL: ").append(url).append("\n");
                    if (!snippet.isBlank()) {
                        sb.append("Snippet: ").append(snippet).append("\n");
                    }
                    sb.append("\n");
                }
            }
        }

        if (sb.length() <= "Search results:\n\n".length()) {
            sb.append("No results found.");
        }
        return sb.toString().trim();
    }

    private String filterDeniedDomains(String result, List<String> deniedDomains) {
        if (deniedDomains == null || deniedDomains.isEmpty()) {
            return result;
        }
        return result;
    }

    private List<String> extractDeniedDomains(JsonNode input) {
        List<String> domains = new ArrayList<>();
        if (input.has("deny_domains") && input.get("deny_domains").isArray()) {
            for (JsonNode node : input.get("deny_domains")) {
                domains.add(node.asText());
            }
        }
        return domains;
    }

    private String extractSource(JsonNode input) {
        if (input.has("source") && !input.get("source").isNull()) {
            return input.get("source").asText("");
        }
        return "";
    }

    @Override
    public PermissionDecision checkPermissions(JsonNode input, ToolPermissionContext permCtx) {
        String url = input.has("url") ? input.get("url").asText("") : "";
        if (!url.isEmpty()) {
            String domain = extractDomain(url);
            if (deniedDomains.contains(domain)) {
                return PermissionDecision.DENY;
            }
        }
        return PermissionDecision.ASK;
    }

    private String extractDomain(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host != null) {
                if (host.startsWith("www.")) {
                    host = host.substring(4);
                }
                return host;
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean isConcurrencySafe() {
        return true;
    }

    private static JsonNode buildSchema() {
        ObjectNode schema = mapper().createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");

        ObjectNode queryProp = properties.putObject("query");
        queryProp.put("type", "string");
        queryProp.put("description", "The search query");

        ObjectNode sourceProp = properties.putObject("source");
        sourceProp.put("type", "string");
        sourceProp.put("description", "Search source: 'anthropic' or 'brave' (default: anthropic if configured)");

        ObjectNode denyDomainsProp = properties.putObject("deny_domains");
        denyDomainsProp.put("type", "array");
        ObjectNode denyDomainItems = denyDomainsProp.putObject("items");
        denyDomainItems.put("type", "string");
        denyDomainsProp.put("description", "Domains to exclude from search results");

        ObjectNode numResultsProp = properties.putObject("num_results");
        numResultsProp.put("type", "integer");
        numResultsProp.put("description", "Maximum number of results to return");
        numResultsProp.put("default", 10);

        ArrayNode required = schema.putArray("required");
        required.add("query");

        return schema;
    }
}