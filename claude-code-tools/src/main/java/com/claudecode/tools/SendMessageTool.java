package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.claudecode.session.SessionManager;
import com.claudecode.session.SessionStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SendMessageTool — send a message to another agent/task or session.
 * Input: {recipient, message, session_id (optional)}.
 * Supports cross-session messaging via SessionManager/SessionStorage.
 */
public class SendMessageTool extends Tool<JsonNode, String> {

    private static final Logger LOG = LoggerFactory.getLogger(SendMessageTool.class);
    private static final JsonNode SCHEMA = buildSchema();
    private static final String MESSAGES_DIR_NAME = "messages";
    private static final String MESSAGE_FILE_NAME = "inbox.jsonl";

    private final CrossSessionMessenger messenger;

    public SendMessageTool() {
        this(null);
    }

    public SendMessageTool(CrossSessionMessenger messenger) {
        this.messenger = messenger;
    }

    @Override
    public String name() { return "SendMessage"; }

    @Override
    public String description() {
        return "Send a message to another agent, task, or session";
    }

    @Override
    public JsonNode inputSchema() { return SCHEMA; }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String recipient = input.has("recipient") ? input.get("recipient").asText("") : "";
        String message = input.has("message") ? input.get("message").asText("") : "";
        String sessionId = input.has("session_id") && !input.get("session_id").isNull()
            ? input.get("session_id").asText() : null;

        if (recipient.isBlank()) {
            return "Error: recipient is required";
        }
        if (message.isBlank()) {
            return "Error: message is required";
        }

        if (sessionId != null) {
            return sendToSession(sessionId, recipient, message, context);
        }

        if (recipient.startsWith("task:")) {
            return sendToTask(recipient.substring(5), message);
        }

        if (recipient.startsWith("session:")) {
            return sendToSession(recipient.substring(8), "direct", message, context);
        }

        LOG.info("Message sent to '{}': {}", recipient, message);
        return "Message sent to '" + recipient + "': " + message;
    }

    private String sendToSession(String sessionId, String recipient, String message,
                                ToolExecutionContext context) {
        if (messenger == null) {
            return "Error: cross-session messaging not configured. Provide a CrossSessionMessenger implementation.";
        }

        try {
            String messageId = messenger.sendMessageToSession(sessionId, recipient, message);
            return "Message sent to session '" + sessionId + "' (recipient: " + recipient + ")\n" +
                   "Message ID: " + messageId;
        } catch (Exception e) {
            LOG.error("Failed to send message to session {}: {}", sessionId, e.getMessage());
            return "Error: failed to send message to session: " + e.getMessage();
        }
    }

    private String sendToTask(String taskId, String message) {
        Path taskMessageFile = Path.of(System.getProperty("user.home"), ".claude", "tasks",
            taskId + ".messages");

        try {
            Files.createDirectories(taskMessageFile.getParent());
            String messageId = UUID.randomUUID().toString();
            String entry = messageId + "|" + Instant.now().toString() + "|" + message + "\n";
            Files.writeString(taskMessageFile, entry,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);

            return "Message sent to task '" + taskId + "'\nMessage ID: " + messageId;
        } catch (Exception e) {
            return "Error: failed to send message to task: " + e.getMessage();
        }
    }

    private static JsonNode buildSchema() {
        ObjectNode schema = mapper().createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");

        ObjectNode recipientProp = properties.putObject("recipient");
        recipientProp.put("type", "string");
        recipientProp.put("description", "The recipient (agent name, 'task:<id>', or 'session:<id>')");

        ObjectNode messageProp = properties.putObject("message");
        messageProp.put("type", "string");
        messageProp.put("description", "The message content to send");

        ObjectNode sessionIdProp = properties.putObject("session_id");
        sessionIdProp.put("type", "string");
        sessionIdProp.put("description", "Target session ID for cross-session messaging");

        ArrayNode required = schema.putArray("required");
        required.add("recipient");
        required.add("message");
        return schema;
    }

    public interface CrossSessionMessenger {
        String sendMessageToSession(String sessionId, String recipient, String message);
        List<CrossSessionMessage> receiveMessages(String sessionId);
        List<CrossSessionMessage> receiveMessages(String sessionId, String recipient);
    }

    public record CrossSessionMessage(
        @JsonProperty("message_id") String messageId,
        @JsonProperty("from_session") String fromSession,
        @JsonProperty("recipient") String recipient,
        @JsonProperty("content") String content,
        @JsonProperty("timestamp") Instant timestamp,
        @JsonProperty("read") boolean read
    ) {}

    public static class SessionBasedCrossSessionMessenger implements CrossSessionMessenger {
        private static final Logger LOG = LoggerFactory.getLogger(SessionBasedCrossSessionMessenger.class);

        private final SessionManager sessionManager;
        private final SessionStorage sessionStorage;
        private final ObjectMapper objectMapper;
        private final ConcurrentMap<String, Path> messageInboxes;
        private final String currentSessionId;

        public SessionBasedCrossSessionMessenger(SessionManager sessionManager, String currentSessionId) {
            this.sessionManager = sessionManager;
            this.sessionStorage = new SessionStorage();
            this.objectMapper = new ObjectMapper();
            this.objectMapper.findAndRegisterModules();
            this.messageInboxes = new ConcurrentHashMap<>();
            this.currentSessionId = currentSessionId;
        }

        public SessionBasedCrossSessionMessenger(String currentSessionId) {
            this(new SessionManager(), currentSessionId);
        }

        @Override
        public String sendMessageToSession(String sessionId, String recipient, String content) {
            if (sessionId == null || sessionId.isBlank()) {
                throw new IllegalArgumentException("Session ID is required");
            }
            if (content == null || content.isBlank()) {
                throw new IllegalArgumentException("Message content is required");
            }

            CrossSessionMessage message = new CrossSessionMessage(
                UUID.randomUUID().toString(),
                currentSessionId,
                recipient != null ? recipient : "direct",
                content,
                Instant.now(),
                false
            );

            Path inboxPath = getMessageInbox(sessionId);
            try {
                Files.createDirectories(inboxPath.getParent());
                String json = objectMapper.writeValueAsString(message);
                Files.writeString(inboxPath, json + "\n",
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
                LOG.info("Message {} sent to session {} (recipient: {})", message.messageId(), sessionId, recipient);
                return message.messageId();
            } catch (IOException e) {
                LOG.error("Failed to send message to session {}: {}", sessionId, e.getMessage());
                throw new RuntimeException("Failed to send message: " + e.getMessage(), e);
            }
        }

        @Override
        public List<CrossSessionMessage> receiveMessages(String sessionId) {
            return receiveMessages(sessionId, null);
        }

        @Override
        public List<CrossSessionMessage> receiveMessages(String sessionId, String recipient) {
            Path inboxPath = getMessageInbox(sessionId);
            if (!Files.exists(inboxPath)) {
                return List.of();
            }

            List<CrossSessionMessage> messages = new ArrayList<>();
            try {
                List<String> lines = Files.readAllLines(inboxPath);
                for (String line : lines) {
                    if (line.isBlank()) continue;
                    try {
                        CrossSessionMessage msg = objectMapper.readValue(line, CrossSessionMessage.class);
                        if (recipient == null || recipient.equals(msg.recipient()) || "direct".equals(msg.recipient())) {
                            messages.add(msg);
                        }
                    } catch (Exception e) {
                        LOG.warn("Skipping malformed message line: {}", line);
                    }
                }
            } catch (IOException e) {
                LOG.error("Failed to read messages for session {}: {}", sessionId, e.getMessage());
            }
            return messages;
        }

        public void markAsRead(String sessionId, String messageId) {
            Path inboxPath = getMessageInbox(sessionId);
            if (!Files.exists(inboxPath)) {
                return;
            }

            try {
                List<String> lines = Files.readAllLines(inboxPath);
                List<String> updatedLines = new ArrayList<>();
                for (String line : lines) {
                    if (line.isBlank()) continue;
                    try {
                        CrossSessionMessage msg = objectMapper.readValue(line, CrossSessionMessage.class);
                        if (msg.messageId().equals(messageId)) {
                            CrossSessionMessage readMsg = new CrossSessionMessage(
                                msg.messageId(), msg.fromSession(), msg.recipient(),
                                msg.content(), msg.timestamp(), true
                            );
                            updatedLines.add(objectMapper.writeValueAsString(readMsg));
                        } else {
                            updatedLines.add(line);
                        }
                    } catch (Exception e) {
                        updatedLines.add(line);
                    }
                }
                Files.writeString(inboxPath, String.join("\n", updatedLines) + "\n");
            } catch (Exception e) {
                LOG.error("Failed to mark message {} as read: {}", messageId, e.getMessage());
            }
        }

        public void clearMessages(String sessionId) {
            Path inboxPath = getMessageInbox(sessionId);
            if (Files.exists(inboxPath)) {
                try {
                    Files.delete(inboxPath);
                } catch (IOException e) {
                    LOG.error("Failed to delete messages for session {}: {}", sessionId, e.getMessage());
                }
            }
            messageInboxes.remove(sessionId);
        }

        public int getUnreadCount(String sessionId) {
            return (int) receiveMessages(sessionId).stream()
                .filter(m -> !m.read())
                .count();
        }

        private Path getMessageInbox(String sessionId) {
            return messageInboxes.computeIfAbsent(sessionId, id ->
                sessionManager.getSessionDir(id).resolve(MESSAGES_DIR_NAME).resolve(MESSAGE_FILE_NAME)
            );
        }
    }
}
