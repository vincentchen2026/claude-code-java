package com.claudecode.api;

import com.claudecode.core.message.ContentBlock;
import com.claudecode.core.message.TextBlock;
import com.claudecode.core.message.ThinkingBlock;
import com.claudecode.core.message.ToolUseBlock;
import com.claudecode.core.message.Usage;
import com.claudecode.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Anthropic API client using java.net.http.HttpClient with SSE parsing.
 * <p>
 * This adapter uses the standard Java HTTP client to communicate with the
 * Anthropic Messages API. It parses SSE streams into our unified StreamEvent
 * interface. When the official Anthropic Java SDK becomes available, this can
 * be swapped out via the LlmClient interface.
 */
public class AnthropicSdkClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicSdkClient.class);
    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com";
    private static final String API_VERSION = "2023-06-01";
    private static final String MESSAGES_PATH = "/v1/messages";

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public AnthropicSdkClient(ApiConfig.AnthropicConfig config) {
        this.apiKey = config.apiKey();
        this.model = config.model();
        this.baseUrl = config.baseUrl().orElse(DEFAULT_BASE_URL);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.mapper = JsonUtils.getMapper();
    }

    @Override
    public Iterator<StreamEvent> createMessageStream(CreateMessageRequest request) {
        try {
            String requestBody = mapper.writeValueAsString(request);
            HttpRequest httpRequest = buildRequest(requestBody);

            HttpResponse<InputStream> response = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                String errorBody = new String(response.body().readAllBytes());
                throw new ApiException(
                        "API request failed: " + errorBody,
                        response.statusCode());
            }

            SseParser sseParser = new SseParser(response.body());
            return new StreamEventIterator(sseParser, mapper);
        } catch (ApiException e) {
            throw e;
        } catch (IOException e) {
            throw new ApiException("Failed to send request: " + e.getMessage(), 0, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("Request interrupted", 0, e);
        }
    }

    @Override
    public ApiMessage createMessage(CreateMessageRequest request) {
        // For non-streaming, build a request with stream=false
        CreateMessageRequest nonStreamRequest = CreateMessageRequest.builder()
                .model(request.model())
                .maxTokens(request.maxTokens())
                .systemPrompt(request.systemPrompt())
                .messages(request.messages())
                .tools(request.tools())
                .metadata(request.metadata())
                .stopSequences(request.stopSequences())
                .stream(false)
                .temperature(request.temperature())
                .topP(request.topP())
                .topK(request.topK())
                .build();

        try {
            String requestBody = mapper.writeValueAsString(nonStreamRequest);
            HttpRequest httpRequest = buildRequest(requestBody);

            HttpResponse<String> response = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new ApiException(
                        "API request failed: " + response.body(),
                        response.statusCode());
            }

            return mapper.readValue(response.body(), ApiMessage.class);
        } catch (ApiException e) {
            throw e;
        } catch (IOException e) {
            throw new ApiException("Failed to send request: " + e.getMessage(), 0, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("Request interrupted", 0, e);
        }
    }

    @Override
    public String getModel() {
        return model;
    }

    private HttpRequest buildRequest(String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + MESSAGES_PATH))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", API_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofMinutes(10))
                .build();
    }

    /**
     * Adapts SSE events to our StreamEvent interface.
     */
    static class StreamEventIterator implements Iterator<StreamEvent> {

        private final SseParser sseParser;
        private final ObjectMapper mapper;
        private StreamEvent nextEvent = null;
        private boolean done = false;

        StreamEventIterator(SseParser sseParser, ObjectMapper mapper) {
            this.sseParser = sseParser;
            this.mapper = mapper;
        }

        @Override
        public boolean hasNext() {
            if (nextEvent != null) return true;
            if (done) return false;

            while (sseParser.hasNext()) {
                SseParser.SseEvent sseEvent = sseParser.next();
                StreamEvent parsed = parseEvent(sseEvent);
                if (parsed != null) {
                    nextEvent = parsed;
                    if (parsed instanceof StreamEvent.MessageStop) {
                        done = true;
                    }
                    return true;
                }
            }
            done = true;
            return false;
        }

        @Override
        public StreamEvent next() {
            if (!hasNext()) throw new NoSuchElementException();
            StreamEvent event = nextEvent;
            nextEvent = null;
            return event;
        }

        private StreamEvent parseEvent(SseParser.SseEvent sseEvent) {
            try {
                return switch (sseEvent.event()) {
                    case "message_start" -> parseMessageStart(sseEvent.data());
                    case "content_block_start" -> parseContentBlockStart(sseEvent.data());
                    case "content_block_delta" -> parseContentBlockDelta(sseEvent.data());
                    case "content_block_stop" -> parseContentBlockStop(sseEvent.data());
                    case "message_delta" -> parseMessageDelta(sseEvent.data());
                    case "message_stop" -> new StreamEvent.MessageStop();
                    case "ping" -> new StreamEvent.Ping();
                    case "error" -> new StreamEvent.Error(
                            new ApiException(sseEvent.data(), 0));
                    default -> null; // Ignore unknown events
                };
            } catch (Exception e) {
                log.warn("Failed to parse SSE event: {} - {}", sseEvent.event(), e.getMessage());
                return new StreamEvent.Error(new ApiException(
                        "Failed to parse event: " + e.getMessage(), 0, e));
            }
        }

        private StreamEvent.MessageStart parseMessageStart(String data) throws IOException {
            JsonNode root = mapper.readTree(data);
            JsonNode messageNode = root.get("message");
            ApiMessage message = mapper.treeToValue(messageNode, ApiMessage.class);
            return new StreamEvent.MessageStart(message);
        }

        private StreamEvent.ContentBlockStart parseContentBlockStart(String data) throws IOException {
            JsonNode root = mapper.readTree(data);
            int index = root.get("index").asInt();
            JsonNode blockNode = root.get("content_block");
            ContentBlock block = parseContentBlock(blockNode);
            return new StreamEvent.ContentBlockStart(index, block);
        }

        private StreamEvent.ContentBlockDelta parseContentBlockDelta(String data) throws IOException {
            JsonNode root = mapper.readTree(data);
            int index = root.get("index").asInt();
            JsonNode deltaNode = root.get("delta");
            Delta delta = parseDelta(deltaNode);
            return new StreamEvent.ContentBlockDelta(index, delta);
        }

        private StreamEvent.ContentBlockStop parseContentBlockStop(String data) throws IOException {
            JsonNode root = mapper.readTree(data);
            int index = root.get("index").asInt();
            return new StreamEvent.ContentBlockStop(index);
        }

        private StreamEvent.MessageDelta parseMessageDelta(String data) throws IOException {
            JsonNode root = mapper.readTree(data);
            JsonNode deltaNode = root.get("delta");
            JsonNode usageNode = root.get("usage");

            MessageDeltaData delta = mapper.treeToValue(deltaNode, MessageDeltaData.class);
            Usage usage = usageNode != null
                    ? mapper.treeToValue(usageNode, Usage.class)
                    : Usage.EMPTY;

            return new StreamEvent.MessageDelta(delta, usage);
        }

        private ContentBlock parseContentBlock(JsonNode node) throws IOException {
            String type = node.has("type") ? node.get("type").asText() : "text";
            return switch (type) {
                case "text" -> new TextBlock(node.has("text") ? node.get("text").asText() : "");
                case "tool_use" -> new ToolUseBlock(
                        node.get("id").asText(),
                        node.get("name").asText(),
                        node.get("input"));
                case "thinking" -> new ThinkingBlock(
                        node.has("thinking") ? node.get("thinking").asText() : "");
                default -> new TextBlock("");
            };
        }

        private Delta parseDelta(JsonNode node) {
            String type = node.has("type") ? node.get("type").asText() : "text_delta";
            return switch (type) {
                case "text_delta" -> new Delta.TextDelta(
                        node.has("text") ? node.get("text").asText() : "");
                case "input_json_delta" -> new Delta.InputJsonDelta(
                        node.has("partial_json") ? node.get("partial_json").asText() : "");
                case "thinking_delta" -> new Delta.ThinkingDelta(
                        node.has("thinking") ? node.get("thinking").asText() : "");
                default -> new Delta.TextDelta("");
            };
        }
    }
}
