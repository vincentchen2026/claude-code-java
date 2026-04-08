package com.claudecode.tools;

import java.util.Optional;

public class NoOpSubAgentFactory implements SubAgentFactory {

    @Override
    public SubAgentResult runSubAgent(SubAgentRequest request) {
        return SubAgentResult.of("Sub-agent not configured: " + request.prompt());
    }
}