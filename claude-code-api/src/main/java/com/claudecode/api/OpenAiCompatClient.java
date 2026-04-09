package com.claudecode.api;

import com.claudecode.core.message.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * OpenAI-compatible API client.
 * Implements LlmClient for any OpenAI API-compatible backend
 * (Azure OpenAI, local models via Ollama, LM Studio, etc.).
 */
public class OpenAiCompatClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatClient.class);

    private final ApiConfig.OpenAiConfig config;
    private final ObjectMapper mapper;
    private final String apiUrl;
    private final HttpExecutor httpExecutor;

    public OpenAiCompatClient(ApiConfig.OpenAiConfig config) {
        this(config, new RealHttpExecutor());
    }

    public OpenAiCompatClient(ApiConfig.OpenAiConfig config, HttpExecutor httpExecutor) {
        this.config = config;
        this.mapper = new ObjectMapper();
        this.httpExecutor = httpExecutor;
        this.apiUrl = (config.baseUrl() != null ? config.baseUrl() : "https://api.openai.com/v1")
                + "/chat/completions";
    }

    @Override
    public Iterator<StreamEvent> createMessageStream(CreateMessageRequest request) {
        log.info("OpenAI-compat streaming request to {} model {}", apiUrl, config.model());

        try {
            HttpURLConnection conn = httpExecutor.createConnection(apiUrl);
            conn.setRequestProperty("Authorization", "Bearer " + config.apiKey());
            conn.setRequestProperty("Accept", "application/json");
            if (config.baseUrl() != null && config.baseUrl().contains("azure")) {
                conn.setRequestProperty("api-key", config.apiKey());
            }
            String requestBody = buildChatRequest(request);
            httpExecutor.sendRequest(conn, requestBody);

            int responseCode = httpExecutor.getResponseCode(conn);
            if (responseCode != 200) {
                String errorBody = httpExecutor.readErrorBody(conn);
                throw new ApiException("OpenAI API error: " + responseCode + " - " + errorBody, responseCode);
            }

            return parseStreamingResponse(conn);

        } catch (ApiException e) {
            return new ErrorIterator(e);
        } catch (Exception e) {
            log.error("OpenAI streaming request failed", e);
            return new ErrorIterator(new ApiException("Request failed: " + e.getMessage(), 0, e));
        }
    }

    @Override
    public ApiMessage createMessage(CreateMessageRequest request) {
        log.info("OpenAI-compat request to {} model {}", apiUrl, config.model());

        try {
            HttpURLConnection conn = httpExecutor.createConnection(apiUrl);
            conn.setRequestProperty("Authorization", "Bearer " + config.apiKey());
            conn.setRequestProperty("Accept", "application/json");
            if (config.baseUrl() != null && config.baseUrl().contains("azure")) {
                conn.setRequestProperty("api-key", config.apiKey());
            }
            String requestBody = buildChatRequest(request);
            httpExecutor.sendRequest(conn, requestBody);

            int responseCode = httpExecutor.getResponseCode(conn);
            if (responseCode != 200) {
                String errorBody = httpExecutor.readErrorBody(conn);
                throw new ApiException("OpenAI API error: " + responseCode + " - " + errorBody, responseCode);
            }

            String responseBody = httpExecutor.readResponseBody(conn);
            return parseNonStreamingResponse(responseBody);

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenAI request failed", e);
            throw new ApiException("Request failed: " + e.getMessage(), 0, e);
        }
    }

    @Override
    public String getModel() {
        return config.model();
    }

    public String getBaseUrl() {
        return config.baseUrl();
    }

    private String buildChatRequest(CreateMessageRequest request) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", request.model() != null ? request.model() : config.model());
        root.put("max_tokens", request.maxTokens());
        root.put("stream", true);

        if (request.temperature() != null) {
            root.put("temperature", request.temperature());
        }
        if (request.topP() != null) {
            root.put("top_p", request.topP());
        }

        ArrayNode messages = mapper.createArrayNode();

        if (request.systemPrompt() != null && !request.systemPrompt().isEmpty()) {
            ObjectNode systemMsg = mapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content", request.systemPrompt());
            messages.add(systemMsg);
        }

        if (request.messages() != null) {
            for (CreateMessageRequest.RequestMessage msg : request.messages()) {
                messages.add(convertRequestMessage(msg));
            }
        }

        root.set("messages", messages);

        if (request.tools() != null && !request.tools().isEmpty()) {
            ArrayNode tools = mapper.createArrayNode();
            for (CreateMessageRequest.ToolDefinition tool : request.tools()) {
                ObjectNode toolNode = mapper.createObjectNode();
                toolNode.put("type", "function");

                ObjectNode function = mapper.createObjectNode();
                function.put("name", tool.name());
                if (tool.description() != null) {
                    function.put("description", tool.description());
                }
                if (tool.inputSchema() != null) {
                    function.set("parameters", tool.inputSchema());
                }
                toolNode.set("function", function);
                tools.add(toolNode);
            }
            root.set("tools", tools);
        }

        return mapper.writeValueAsString(root);
    }

    private ObjectNode convertRequestMessage(CreateMessageRequest.RequestMessage msg) {
        ObjectNode node = mapper.createObjectNode();
        node.put("role", msg.role());

        if (msg.content() instanceof String strContent) {
            node.put("content", strContent);
        } else if (msg.content() instanceof List<?> listContent) {
            ArrayNode contentArray = mapper.createArrayNode();
            for (Object item : listContent) {
                if (item instanceof Map<?, ?> mapItem) {
                    ObjectNode toolCallNode = mapper.createObjectNode();
                    toolCallNode.put("type", "tool_call");
                    ObjectNode toolCall = mapper.createObjectNode();
                    toolCall.put("id", String.valueOf(mapItem.get("id")));
                    toolCall.put("type", "function");
                    ObjectNode function = mapper.createObjectNode();
                    function.put("name", String.valueOf(mapItem.get("name")));
                    function.put("arguments", mapItem.get("input") != null ? mapItem.get("input").toString() : "{}");
                    toolCall.set("function", function);
                    toolCallNode.set("tool_call", toolCall);
                    contentArray.add(toolCallNode);
                } else if (item instanceof String strItem) {
                    ObjectNode textNode = mapper.createObjectNode();
                    textNode.put("type", "text");
                    textNode.put("text", strItem);
                    contentArray.add(textNode);
                }
            }
            node.set("content", contentArray);
        }

        return node;
    }

    private Iterator<StreamEvent> parseStreamingResponse(HttpURLConnection conn) {
        List<StreamEvent> events = new CopyOnWriteArrayList<>();
        StringBuilder textAccumulator = new StringBuilder();
        List<ToolUseBlock> toolCalls = new CopyOnWriteArrayList<>();
        List<String> toolCallArgs = new CopyOnWriteArrayList<>();
        List<String> toolCallNames = new CopyOnWriteArrayList<>();
        int currentToolIndex = -1;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            String messageId = "chatcmpl_" + UUID.randomUUID().toString().substring(0, 8);

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6);
                    if (data.equals("[DONE]")) {
                        break;
                    }

                    try {
                        JsonNode chunk = mapper.readTree(data);
                        processChunk(chunk, events, textAccumulator, toolCalls,
                                toolCallArgs, toolCallNames);

                    } catch (Exception e) {
                        log.warn("Failed to parse SSE chunk: {}", data, e);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error reading streaming response", e);
            events.add(new StreamEvent.Error(new ApiException("Stream read error: " + e.getMessage(), 0, e)));
        }

        if (!toolCalls.isEmpty()) {
            for (int i = 0; i < toolCalls.size(); i++) {
                events.add(new StreamEvent.ContentBlockStart(i, toolCalls.get(i)));
                events.add(new StreamEvent.ContentBlockStop(i));
            }
        }

        if (!textAccumulator.isEmpty() && events.stream().noneMatch(e -> e instanceof StreamEvent.ContentBlockStart)) {
            events.add(new StreamEvent.ContentBlockStart(0, new TextBlock(textAccumulator.toString())));
        }

        events.add(new StreamEvent.MessageDelta(
                new MessageDeltaData("stop", null), Usage.EMPTY));
        events.add(new StreamEvent.MessageStop());

        return events.iterator();
    }

    private void processChunk(JsonNode chunk, List<StreamEvent> events,
            StringBuilder textAccumulator, List<ToolUseBlock> toolCalls,
            List<String> toolCallArgs, List<String> toolCallNames) {

        JsonNode choices = chunk.get("choices");
        if (choices == null || choices.isNull()) return;

        for (JsonNode choice : choices) {
            JsonNode delta = choice.get("delta");
            if (delta == null) continue;

            if (delta.has("content")) {
                String content = delta.get("content").asText();
                if (content != null && !content.isEmpty()) {
                    if (events.stream().noneMatch(e -> e instanceof StreamEvent.ContentBlockStart)) {
                        events.add(new StreamEvent.ContentBlockStart(0, new TextBlock("")));
                    }
                    events.add(new StreamEvent.ContentBlockDelta(0, new Delta.TextDelta(content)));
                    textAccumulator.append(content);
                }
            }

            if (delta.has("tool_calls")) {
                JsonNode toolCallsDelta = delta.get("tool_calls");
                for (JsonNode tc : toolCallsDelta) {
                    int index = tc.has("index") ? tc.get("index").asInt() : toolCalls.size();

                    while (toolCalls.size() <= index) {
                        toolCalls.add(null);
                        toolCallArgs.add("");
                        toolCallNames.add(null);
                    }

                    if (tc.has("function")) {
                        if (tc.get("function").has("name")) {
                            String name = tc.get("function").get("name").asText();
                            toolCallNames.set(index, name);
                            String id = "toolu_" + UUID.randomUUID().toString().substring(0, 8);
                            toolCalls.set(index, new ToolUseBlock(id, name, mapper.createObjectNode()));
                        }

                        if (tc.get("function").has("arguments")) {
                            String args = tc.get("function").get("arguments").asText();
                            String currentArgs = toolCallArgs.get(index);
                            toolCallArgs.set(index, currentArgs + args);

                            String name = toolCallNames.get(index);
                            if (name != null) {
                                try {
                                    JsonNode argsNode = mapper.readTree(toolCallArgs.get(index));
                                    String id = toolCalls.get(index) != null ? toolCalls.get(index).id() :
                                            "toolu_" + UUID.randomUUID().toString().substring(0, 8);
                                    toolCalls.set(index, new ToolUseBlock(id, name, argsNode));
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                }
            }

            JsonNode finishReason = choice.get("finish_reason");
            if (finishReason != null && !finishReason.isNull()) {
                String reason = finishReason.asText();
                String stopReason = "stop".equals(reason) ? "end_turn" : reason;
                events.add(new StreamEvent.MessageDelta(
                        new MessageDeltaData(stopReason, null), Usage.EMPTY));
            }
        }
    }

    private ApiMessage parseNonStreamingResponse(String responseBody) throws Exception {
        JsonNode root = mapper.readTree(responseBody);

        String id = root.has("id") ? root.get("id").asText() : "chatcmpl_" + UUID.randomUUID();
        String model = root.has("model") ? root.get("model").asText() : config.model();
        JsonNode choices = root.get("choices");
        String stopReason = "stop";
        List<ContentBlock> contentBlocks = new ArrayList<>();

        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode choice = choices.get(0);
            JsonNode message = choice.get("message");
            if (message != null) {
                if (message.has("content")) {
                    String content = message.get("content").asText();
                    if (content != null && !content.isEmpty()) {
                        contentBlocks.add(new TextBlock(content));
                    }
                }

                if (message.has("tool_calls")) {
                    for (JsonNode tc : message.get("tool_calls")) {
                        String name = tc.get("function").get("name").asText();
                        String args = tc.get("function").get("arguments").asText();
                        String toolId = tc.has("id") ? tc.get("id").asText() :
                                "toolu_" + UUID.randomUUID().toString().substring(0, 8);

                        JsonNode argsNode;
                        try {
                            argsNode = mapper.readTree(args);
                        } catch (Exception e) {
                            argsNode = mapper.createObjectNode();
                        }
                        contentBlocks.add(new ToolUseBlock(toolId, name, argsNode));
                    }
                }

                JsonNode finish = choice.get("finish_reason");
                if (finish != null) {
                    stopReason = finish.asText();
                    if ("length".equals(stopReason)) {
                        stopReason = "max_tokens";
                    }
                }
            }
        }

        JsonNode usage = root.get("usage");
        Usage tokenUsage = Usage.EMPTY;
        if (usage != null) {
            long inputTokens = usage.has("prompt_tokens") ? usage.get("prompt_tokens").asLong() : 0;
            long outputTokens = usage.has("completion_tokens") ? usage.get("completion_tokens").asLong() : 0;
            tokenUsage = new Usage(inputTokens, outputTokens, 0, 0);
        }

        return new ApiMessage(id, "message", "assistant", contentBlocks, model,
                stopReason, null, tokenUsage);
    }

    private static class ErrorIterator implements Iterator<StreamEvent> {
        private final ApiException error;
        private boolean returned = false;

        ErrorIterator(ApiException error) {
            this.error = error;
        }

        @Override
        public boolean hasNext() {
            return !returned;
        }

        @Override
        public StreamEvent next() {
            if (returned) throw new NoSuchElementException();
            returned = true;
            return new StreamEvent.Error(error);
        }
    }
}