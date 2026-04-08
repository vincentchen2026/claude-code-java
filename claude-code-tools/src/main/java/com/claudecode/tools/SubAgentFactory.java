package com.claudecode.tools;

/**
 * Factory interface for creating and running sub-agent instances.
 * Implementations create a new QueryEngine with restricted tool set
 * and independent message history.
 */
public interface SubAgentFactory {

    /**
     * Runs a sub-agent with the given request parameters.
     *
     * @param request the sub-agent configuration and prompt
     * @return the result of the sub-agent execution
     */
    SubAgentResult runSubAgent(SubAgentRequest request);
}
