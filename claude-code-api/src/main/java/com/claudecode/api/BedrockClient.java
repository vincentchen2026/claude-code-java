package com.claudecode.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

/**
 * AWS Bedrock API client stub.
 * Implements LlmClient for AWS Bedrock runtime.
 */
public class BedrockClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(BedrockClient.class);

    private final ApiConfig.BedrockConfig config;

    public BedrockClient(ApiConfig.BedrockConfig config) {
        this.config = config;
    }

    @Override
    public Iterator<StreamEvent> createMessageStream(CreateMessageRequest request) {
        log.info("Bedrock streaming request to region {} model {}", config.region(), config.model());
        // Stub: would use AWS SDK BedrockRuntime
        return createStubStream(request);
    }

    @Override
    public ApiMessage createMessage(CreateMessageRequest request) {
        log.info("Bedrock request to region {} model {}", config.region(), config.model());
        return ApiMessage.stub(config.model(), "Bedrock response stub");
    }

    @Override
    public String getModel() {
        return config.model();
    }

    /**
     * Returns the configured AWS region.
     */
    public String getRegion() {
        return config.region();
    }

    private Iterator<StreamEvent> createStubStream(CreateMessageRequest request) {
        ApiMessage stubMessage = ApiMessage.stub(config.model(), "Bedrock streaming stub");
        List<StreamEvent> events = List.of(
            new StreamEvent.MessageStart(stubMessage),
            new StreamEvent.ContentBlockStart(0, new com.claudecode.core.message.TextBlock("Bedrock streaming stub")),
            new StreamEvent.ContentBlockStop(0),
            new StreamEvent.MessageStop()
        );
        return events.iterator();
    }
}
