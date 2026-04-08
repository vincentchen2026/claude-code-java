package com.claudecode.cli;

import com.claudecode.api.CreateMessageRequest;
import com.claudecode.api.Delta;
import com.claudecode.api.LlmClient;
import com.claudecode.api.StreamEvent;
import com.claudecode.core.engine.StreamingClient;
import com.claudecode.core.message.ContentBlock;
import com.claudecode.core.message.TextBlock;
import com.claudecode.core.message.ThinkingBlock;
import com.claudecode.core.message.Usage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Adapter that bridges the api module's {@link LlmClient} to the core module's
 * {@link StreamingClient} interface.
 * <p>
 * This is necessary because the core module defines StreamingClient as its
 * abstraction (to avoid circular dependencies), while the api module implements
 * LlmClient with its own request/response types.
 */
public class LlmClientAdapter implements StreamingClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClientAdapter.class);

    private final LlmClient llmClient;

    public LlmClientAdapter(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public Iterator<StreamingEvent> createStream(StreamRequest request) {
        // Convert core's StreamRequest to api's CreateMessageRequest
        List<CreateMessageRequest.RequestMessage> apiMessages = new ArrayList<>();
        for (StreamRequest.RequestMessage msg : request.messages()) {
            apiMessages.add(new CreateMessageRequest.RequestMessage(
                msg.role(),
                msg.content()
            ));
        }

        CreateMessageRequest.Builder requestBuilder = CreateMessageRequest.builder()
            .model(request.model())
            .maxTokens(request.maxTokens())
            .systemPrompt(request.systemPrompt())
            .messages(apiMessages)
            .stream(request.stream())
            .thinking(CreateMessageRequest.ThinkingConfig.enabled(10000));

        // Pass tool definitions if available
        if (request.tools() != null && !request.tools().isEmpty()) {
            List<CreateMessageRequest.ToolDefinition> apiTools = request.tools().stream()
                .map(td -> new CreateMessageRequest.ToolDefinition(
                    td.name(),
                    td.description(),
                    td.inputSchema() instanceof com.fasterxml.jackson.databind.JsonNode jn ? jn : null
                ))
                .toList();
            requestBuilder.tools(apiTools);
        }

        CreateMessageRequest apiRequest = requestBuilder.build();

        // Get the api StreamEvent iterator and adapt to core StreamingEvent
        Iterator<StreamEvent> apiEvents = llmClient.createMessageStream(apiRequest);
        return new StreamingEventAdapter(apiEvents);
    }

    @Override
    public String getModel() {
        return llmClient.getModel();
    }

    /**
     * Adapts api module's StreamEvent iterator to core module's StreamingEvent iterator.
     */
    static class StreamingEventAdapter implements Iterator<StreamingEvent> {

        private final Iterator<StreamEvent> apiEvents;
        private StreamingEvent nextEvent = null;
        private boolean done = false;

        StreamingEventAdapter(Iterator<StreamEvent> apiEvents) {
            this.apiEvents = apiEvents;
        }

        @Override
        public boolean hasNext() {
            if (nextEvent != null) return true;
            if (done) return false;

            while (apiEvents.hasNext()) {
                StreamEvent apiEvent = apiEvents.next();
                StreamingEvent converted = convert(apiEvent);
                if (converted != null) {
                    nextEvent = converted;
                    if (converted instanceof StreamingEvent.MessageStopEvent) {
                        done = true;
                    }
                    return true;
                }
            }
            done = true;
            return false;
        }

        @Override
        public StreamingEvent next() {
            if (!hasNext()) throw new NoSuchElementException();
            StreamingEvent event = nextEvent;
            nextEvent = null;
            return event;
        }

        private StreamingEvent convert(StreamEvent apiEvent) {
            return switch (apiEvent) {
                case StreamEvent.MessageStart ms -> {
                    var msg = ms.message();
                    List<ContentBlock> content = msg != null && msg.content() != null
                        ? msg.content() : List.of();
                    Usage usage = msg != null && msg.usage() != null
                        ? msg.usage() : Usage.EMPTY;
                    String messageId = msg != null ? msg.id() : null;
                    String model = msg != null ? msg.model() : null;
                    yield new StreamingEvent.MessageStartEvent(
                        messageId, model, content, usage
                    );
                }
                case StreamEvent.ContentBlockStart cbs -> {
                    // Forward content_block_start so the engine can detect tool_use blocks
                    ContentBlock block = cbs.contentBlock();
                    String type = "text";
                    String id = null;
                    String name = null;
                    if (block instanceof com.claudecode.core.message.ToolUseBlock tub) {
                        type = "tool_use";
                        id = tub.id();
                        name = tub.name();
                    } else if (block instanceof com.claudecode.core.message.ThinkingBlock) {
                        type = "thinking";
                    } else if (block instanceof TextBlock) {
                        type = "text";
                    }
                    yield new StreamingEvent.ContentBlockStartEvent(
                        cbs.index(), type, id, name
                    );
                }
                case StreamEvent.ContentBlockDelta cbd -> {
                    String text = "";
                    String deltaType = "text_delta";
                    if (cbd.delta() instanceof Delta.TextDelta td) {
                        text = td.text();
                        deltaType = "text_delta";
                    } else if (cbd.delta() instanceof Delta.ThinkingDelta tkd) {
                        text = tkd.thinking();
                        deltaType = "thinking_delta";
                    } else if (cbd.delta() instanceof Delta.InputJsonDelta ijd) {
                        text = ijd.partialJson();
                        deltaType = "input_json_delta";
                    }
                    yield new StreamingEvent.ContentBlockDeltaEvent(
                        cbd.index(), deltaType, text
                    );
                }
                case StreamEvent.ContentBlockStop cbs ->
                    new StreamingEvent.ContentBlockStopEvent(cbs.index());
                case StreamEvent.MessageDelta md -> {
                    String stopReason = md.delta() != null ? md.delta().stopReason() : null;
                    Usage usage = md.usage() != null ? md.usage() : Usage.EMPTY;
                    yield new StreamingEvent.MessageDeltaEvent(stopReason, usage);
                }
                case StreamEvent.MessageStop ignored ->
                    new StreamingEvent.MessageStopEvent();
                case StreamEvent.Error err ->
                    new StreamingEvent.ErrorEvent(err.exception());
                case StreamEvent.Ping ignored -> null;
            };
        }
    }
}
