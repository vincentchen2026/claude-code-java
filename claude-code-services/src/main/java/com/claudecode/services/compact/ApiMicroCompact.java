package com.claudecode.services.compact;

import com.claudecode.core.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * ApiMicroCompact — API-level micro-compaction for reducing payload size (P2 stub).
 */
public class ApiMicroCompact {

    private static final Logger LOG = LoggerFactory.getLogger(ApiMicroCompact.class);

    /** Apply micro-compaction to reduce API payload size. */
    public List<Message> microCompact(List<Message> messages) {
        LOG.debug("ApiMicroCompact: Not yet implemented");
        return messages;
    }
}
