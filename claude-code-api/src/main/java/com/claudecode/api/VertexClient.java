package com.claudecode.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

/**
 * Google Vertex AI API client stub.
 * Implements LlmClient for Google Cloud Vertex AI.
 */
public class VertexClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(VertexClient.class);

    private final ApiConfig.VertexConfig config;

    public VertexClient(ApiConfig.VertexConfig config) {
        this.config = config;
    }

    @Override
    public Iterator<StreamEvent> createMessageStream(CreateMessageRequest request) {
        log.info("Vertex AI streaming request to project {} location {} model {}",
            config.projectId(), config.location(), config.model());
        // Stub: would use Google Cloud Vertex AI SDK
        return createStubStream(request);
    }

    @Override
    public ApiMessage createMessage(CreateMessageRequest request) {
        log.info("Vertex AI request to project {} location {} model {}",
            config.projectId(), config.location(), config.model());
        return ApiMessage.stub(config.model(), "Vertex AI response stub");
    }

    @Override
    public String getModel() {
        return config.model();
    }

    /**
     * Returns the configured project ID.
     */
    public String getProjectId() {
        return config.projectId();
    }

    /**
     * Returns the configured location.
     */
    public String getLocation() {
        return config.location();
    }

    private Iterator<StreamEvent> createStubStream(CreateMessageRequest request) {
        ApiMessage stubMessage = ApiMessage.stub(config.model(), "Vertex AI streaming stub");
        List<StreamEvent> events = List.of(
            new StreamEvent.MessageStart(stubMessage),
            new StreamEvent.ContentBlockStart(0, new com.claudecode.core.message.TextBlock("Vertex AI streaming stub")),
            new StreamEvent.ContentBlockStop(0),
            new StreamEvent.MessageStop()
        );
        return events.iterator();
    }
}
