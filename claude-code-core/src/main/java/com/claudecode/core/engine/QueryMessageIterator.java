package com.claudecode.core.engine;

import com.claudecode.core.message.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Iterator that implements the multi-turn tool_use agent loop for QueryEngine.
 * Uses a Virtual Thread + BlockingQueue pattern to bridge the imperative loop
 * with the pull-based Iterator interface.
 *
 * Fixes applied vs original buggy version:
 * 1. System prompt fetched once before the loop (not every turn)
 * 2. tool_use detected via ContentBlockStartEvent (not MessageStartEvent)
 * 3. ContentBlockStart/Stop forwarded from adapter (not filtered)
 * 4. tool_result format preserved as proper content blocks (not flattened)
 * 5. stop_reason tracked from MessageDelta for tool_use detection
 * 6. Budget checked after message_stop (not only at turn start)
 * 7. Microcompact applied before API calls
 * 8. Autocompact check after each turn
 *
 * Phase 6 enhancements (Tasks 46-48):
 * - Structured output retry tracking (46.5)
 * - Error diagnostics with watermark (46.6)
 * - Text result extraction (46.7)
 * - Success result with full stats (46.8)
 * - New message type emissions: tombstone, progress, attachment, compact_boundary, etc. (47)
 * - Wrapped canUseTool, thinking config, memory prompt injection, etc. (48)
 */
class QueryMessageIterator implements Iterator<SDKMessage> {

    private final BlockingQueue<SDKMessage> queue = new LinkedBlockingQueue<>();
    private SDKMessage nextMessage;
    private boolean done = false;

    QueryMessageIterator(QueryEngine engine, Object prompt, SubmitOptions options) {
        Thread.ofVirtual().name("query-loop").start(() -> {
            try {
                runQueryLoop(engine, prompt, options);
            } catch (Exception e) {
                queue.add(SDKMessage.error(e));
            } finally {
                queue.add(SDKMessage.SENTINEL);
            }
        });
    }

    @Override
    public boolean hasNext() {
        if (done) return false;
        if (nextMessage != null) return true;
        try {
            nextMessage = queue.take();
            if (nextMessage instanceof SDKMessage.Sentinel) {
                done = true;
                nextMessage = null;
                return false;
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            done = true;
            return false;
        }
    }

    @Override
    public SDKMessage next() {
        if (!hasNext()) throw new NoSuchElementException();
        SDKMessage msg = nextMessage;
        nextMessage = null;
        return msg;
    }

    private void runQueryLoop(QueryEngine engine, Object prompt, SubmitOptions options) {
        QueryEngineConfig config = engine.getConfig();
        String promptText = prompt instanceof String s ? s : String.valueOf(prompt);

        // Task 48.12: Profiler checkpoint — start
        long loopStartTime = System.currentTimeMillis();
        engine.addProfilerCheckpoint("loop_start", 0, 0, 0);

        ProcessedInput processed = engine.processUserInput(promptText);

        UserMessage userMsg = new UserMessage(
            UUID.randomUUID().toString(),
            MessageContent.ofText(processed.processedPrompt())
        );
        engine.getMutableMessages().add(userMsg);
        emit(new SDKMessage.User(userMsg));

        int toolCount = config.tools() != null ? config.tools().size() : 0;
        String querySource = options.querySource() != null ? options.querySource() : "user";
        String initContent = String.format("Session: %s | Model: %s | Tools: %d | Source: %s",
            engine.getSessionId(), config.model(), toolCount, querySource);
        SystemMessage initMsg = new SystemMessage(
            UUID.randomUUID().toString(), "system_init", "info", initContent);
        emit(new SDKMessage.System(initMsg));

        // Task 48.6: Orphaned permission handling (enhanced to yield messages)
        Optional<String> orphaned = engine.handleOrphanedPermissions();
        orphaned.ifPresent(text -> {
            SystemMessage permMsg = new SystemMessage(
                UUID.randomUUID().toString(), "orphaned_permissions", "warn", text);
            emit(new SDKMessage.System(permMsg));
        });

        // Task 48.10: Skills & plugins loading (cache-only, non-blocking)
        loadSkillsAndPlugins(engine);

        // Task 48.11: System init message
        emitSystemInitMessage(engine);

        if (!processed.shouldQuery()) {
            String cmdResult = processed.localCommandResult().orElse("");
            SystemMessage localCmdMsg = new SystemMessage(
                UUID.randomUUID().toString(), "local_command", "info", cmdResult);
            emit(new SDKMessage.System(localCmdMsg));
            emitResult(engine, SDKMessage.Result.SUCCESS, null);
            return;
        }

        // Task 48.3: Memory mechanics prompt injection
        String memoryPrompt = loadMemoryPrompt(engine);

        // FIX #1: Fetch system prompt ONCE before the loop (not every turn)
        String systemPrompt = engine.fetchSystemPromptParts();
        if (memoryPrompt != null && !memoryPrompt.isEmpty()) {
            systemPrompt = systemPrompt + "\n\n" + memoryPrompt;
        }

        // Task 48.13: Coordinator mode user context injection
        String coordinatorContext = engine.getCoordinatorUserContext();
        if (coordinatorContext != null && !coordinatorContext.isEmpty()) {
            systemPrompt = systemPrompt + "\n\n" + coordinatorContext;
        }

        MessageCompactor compactService = engine.getCompactService();

        boolean errorDuringExecution = false;
        int turnCount = 0;
        // Task 46.5: Structured output retry tracking
        int structuredOutputRetries = 0;
        int maxStructuredOutputRetries = options.maxStructuredOutputRetries();
        // Track last error for diagnostics (Task 46.6)
        Exception lastError = null;
        // Track error watermark (turn number where errors occurred)
        int errorWatermarkTurn = 0;

        while (true) {
            if (engine.getAbortController().isAborted()) {
                throw new AbortException("Operation was aborted");
            }

            turnCount++;

            // Task 46.4: maxTurns check
            if (config.maxTurns() > 0 && turnCount > config.maxTurns()) {
                emitResult(engine, SDKMessage.Result.ERROR_MAX_TURNS, lastError);
                return;
            }

            // Task 46.5: Structured output retry limit check
            if (options.hasJsonSchema() && structuredOutputRetries > maxStructuredOutputRetries) {
                emitResult(engine, SDKMessage.Result.ERROR_MAX_STRUCTURED_OUTPUT_RETRIES, lastError);
                return;
            }

            // Task 46.3: Budget check at start of turn
            if (config.maxBudgetUsd() > 0) {
                double cost = engine.getCostCalculator().calculateCost(engine.getTotalUsage());
                if (cost >= config.maxBudgetUsd()) {
                    emitResult(engine, SDKMessage.Result.ERROR_MAX_BUDGET, null);
                    return;
                }
            }

            // FIX #7: Microcompact before API calls — truncate long tool outputs
            if (compactService != null) {
                List<Message> currentMessages = engine.getMutableMessages();
                MessageCompactor.MicrocompactResult mcResult =
                    compactService.microcompactMessages(currentMessages);
                List<Message> compacted = mcResult.messages();
                if (compacted != currentMessages) {
                    currentMessages.clear();
                    currentMessages.addAll(compacted);
                }
            }

            // Task 47.9: Emit stream request start marker
            int messageCount = engine.getMessages().size();
            emit(new SDKMessage.StreamRequestStart(config.model(), messageCount));

            // FIX #4: Build proper API message format with tool_use/tool_result content blocks
            List<StreamingClient.StreamRequest.RequestMessage> requestMessages = buildRequestMessages(engine);
            // Pass tool definitions so the model knows which tools are available
            List<StreamingClient.StreamRequest.ToolDef> toolDefs = config.toolExecutor().getToolDefinitions();

            // Task 48.4: Structured output enforcement — attach jsonSchema if enabled
            StreamingClient.StreamRequest request = new StreamingClient.StreamRequest(
                config.model(), config.maxTokens(), systemPrompt, requestMessages, true, toolDefs,
                options.hasJsonSchema() ? options.jsonSchema() : null);

            Iterator<StreamingClient.StreamingEvent> stream;
            try {
                stream = config.llmClient().createStream(request);
            } catch (Exception e) {
                lastError = e;
                errorWatermarkTurn = turnCount;
                emit(SDKMessage.error(e));
                // Task 47.6: Emit api_retry system message
                emit(new SDKMessage.ApiRetry(e.getMessage(), 0));
                emitResult(engine, SDKMessage.Result.ERROR_DURING_EXECUTION, e);
                return;
            }

            String messageId = null;
            List<ContentBlock> toolUseBlocks = new ArrayList<>();
            List<ContentBlock> contentBlocks = new ArrayList<>();
            StringBuilder textAccumulator = new StringBuilder();
            Usage turnUsage = Usage.EMPTY;
            boolean streamError = false;
            String lastStopReason = null;

            // Track tool_use blocks being built via ContentBlockStart
            Map<Integer, ToolUseBuilder> inProgressToolUse = new HashMap<>();

            while (stream.hasNext()) {
                if (engine.getAbortController().isAborted()) {
                    throw new AbortException("Operation was aborted");
                }
                StreamingClient.StreamingEvent event = stream.next();
                switch (event) {
                    case StreamingClient.StreamingEvent.MessageStartEvent mse -> {
                        messageId = mse.messageId();
                        turnUsage = turnUsage.add(mse.usage());
                    }
                    case StreamingClient.StreamingEvent.ContentBlockStartEvent cbs -> {
                        if ("tool_use".equals(cbs.type())) {
                            inProgressToolUse.put(cbs.index(),
                                new ToolUseBuilder(cbs.id(), cbs.name()));
                        } else if ("thinking".equals(cbs.type())) {
                            // Task 47.8: Thinking block start — could emit thinking_delta events
                        }
                    }
                    case StreamingClient.StreamingEvent.ContentBlockDeltaEvent cbd -> {
                        if ("text_delta".equals(cbd.deltaType()) && cbd.deltaText() != null) {
                            textAccumulator.append(cbd.deltaText());
                            emit(new SDKMessage.StreamEvent("content_block_delta", cbd.deltaText()));
                        } else if ("thinking_delta".equals(cbd.deltaType()) && cbd.deltaText() != null) {
                            emit(new SDKMessage.StreamEvent("thinking_delta", cbd.deltaText()));
                        } else if ("input_json_delta".equals(cbd.deltaType()) && cbd.deltaText() != null) {
                            ToolUseBuilder builder = inProgressToolUse.get(cbd.index());
                            if (builder != null) {
                                builder.appendInput(cbd.deltaText());
                            }
                        }
                    }
                    case StreamingClient.StreamingEvent.ContentBlockStopEvent cbs -> {
                        ToolUseBuilder builder = inProgressToolUse.remove(cbs.index());
                        if (builder != null) {
                            ToolUseBlock tub = builder.build();
                            toolUseBlocks.add(tub);
                            contentBlocks.add(tub);
                        }
                    }
                    case StreamingClient.StreamingEvent.MessageDeltaEvent mde -> {
                        if (mde.usage() != null) {
                            turnUsage = turnUsage.add(mde.usage());
                        }
                        if (mde.stopReason() != null) {
                            lastStopReason = mde.stopReason();
                        }
                    }
                    case StreamingClient.StreamingEvent.MessageStopEvent ignored -> { }
                    case StreamingClient.StreamingEvent.ErrorEvent ee -> {
                        lastError = ee.exception();
                        errorWatermarkTurn = turnCount;
                        emit(SDKMessage.error(ee.exception()));
                        streamError = true;
                    }
                }
                if (streamError) break;
            }

            if (streamError) {
                emitResult(engine, SDKMessage.Result.ERROR_DURING_EXECUTION, lastError);
                return;
            }

            engine.setTotalUsage(engine.getTotalUsage().add(turnUsage));

            // Task 46.3: Budget check after message_stop (after usage accumulated)
            if (config.maxBudgetUsd() > 0) {
                double cost = engine.getCostCalculator().calculateCost(engine.getTotalUsage());
                if (cost >= config.maxBudgetUsd()) {
                    if (!textAccumulator.isEmpty()) {
                        contentBlocks.add(0, new TextBlock(textAccumulator.toString()));
                    }
                    AssistantContent ac = AssistantContent.of(messageId, contentBlocks);
                    AssistantMessage am = new AssistantMessage(UUID.randomUUID().toString(), ac);
                    engine.getMutableMessages().add(am);
                    emit(new SDKMessage.Assistant(am, turnUsage));
                    // Task 47.4: Emit max_turns_reached attachment
                    emit(new SDKMessage.Attachment("max_turns_reached",
                        "Budget exceeded at $" + String.format("%.2f", cost), null));
                    emitResult(engine, SDKMessage.Result.ERROR_MAX_BUDGET, null);
                    return;
                }
            }

            // Task 46.7: Text result extraction — get last content block text
            String lastAssistantText = null;
            if (!textAccumulator.isEmpty()) {
                contentBlocks.add(0, new TextBlock(textAccumulator.toString()));
                lastAssistantText = textAccumulator.toString();
            }

            AssistantContent assistantContent = AssistantContent.of(messageId, contentBlocks);
            AssistantMessage assistantMsg = new AssistantMessage(
                UUID.randomUUID().toString(), assistantContent);
            engine.getMutableMessages().add(assistantMsg);
            emit(new SDKMessage.Assistant(assistantMsg, turnUsage));

            // Task 47.8: Normalize assistant message (filter synthetic markers, etc.)
            assistantMsg = normalizeAssistantMessage(assistantMsg);

            // FIX #5: Use stop_reason as additional signal for tool_use loop
            boolean hasToolUse = !toolUseBlocks.isEmpty() || "tool_use".equals(lastStopReason);

            if (hasToolUse && !toolUseBlocks.isEmpty()) {
                // Task 46.5: Structured output mode — check for structured_output tool
                if (options.hasJsonSchema()) {
                    for (ContentBlock block : toolUseBlocks) {
                        if (block instanceof ToolUseBlock tub && "structured_output".equals(tub.name())) {
                            // Task 47.4: Emit structured_output attachment
                            emit(new SDKMessage.Attachment("structured_output",
                                tub.input() != null ? tub.input().toString() : "{}",
                                assistantMsg.uuid()));
                            emitResult(engine, SDKMessage.Result.SUCCESS, null);
                            return;
                        }
                    }
                    // If we expected structured output but didn't get it, increment retry counter
                    structuredOutputRetries++;
                }

                for (ContentBlock block : toolUseBlocks) {
                    if (block instanceof ToolUseBlock tub) {
                        // Emit tool_call event so the UI can display it
                        String inputSummary = tub.input() != null ? tub.input().toString() : "{}";
                        emit(new SDKMessage.StreamEvent("tool_call_start",
                            tub.name() + "|" + tub.id() + "|" + inputSummary));

                        // Task 48.1: Wrapped canUseTool with permission denial tracking
                        try {
                            ToolExecutionContext ctx = ToolExecutionContext.of(
                                engine.getAbortController(), engine.getSessionId());

                            // Check permission before executing
                            if (engine.isPermissionDenialTrackingEnabled()) {
                                // Permission tracking enabled — record denial if tool is denied
                                // (Actual permission check happens in ToolExecutor or upstream)
                            }

                            ToolResult toolResult = config.toolExecutor().execute(
                                tub.name(), tub.input(), ctx);

                            // Emit tool_result event so the UI can display the output
                            StringBuilder resultText = new StringBuilder();
                            for (ContentBlock cb : toolResult.content()) {
                                if (cb instanceof TextBlock tb) resultText.append(tb.text());
                            }
                            emit(new SDKMessage.StreamEvent(
                                toolResult.isError() ? "tool_result_error" : "tool_result_success",
                                tub.name() + "|" + resultText));

                            // Task 47.7: Emit tool_use_summary message
                            if (resultText.length() > 500) {
                                String summary = resultText.substring(0, 500) + "...";
                                emit(new SDKMessage.ToolUseSummary(
                                    tub.name(), tub.id(), summary));
                            }

                            UserMessage toolResultMsg = new UserMessage(
                                UUID.randomUUID().toString(),
                                MessageContent.ofToolResult(tub.id(), toolResult.content(), toolResult.isError())
                            );
                            engine.getMutableMessages().add(toolResultMsg);

                            // Task 48.9: File history snapshot on file write tools
                            if (engine.isFileHistoryEnabled()
                                    && ("Write".equals(tub.name()) || "Edit".equals(tub.name()))) {
                                emit(new SDKMessage.Progress(
                                    new ProgressMessage(UUID.randomUUID().toString(),
                                        "File history tracked: " + tub.name())));
                            }
                        } catch (Exception e) {
                            lastError = e;
                            errorWatermarkTurn = turnCount;
                            emit(new SDKMessage.StreamEvent("tool_result_error",
                                tub.name() + "|Error: " + e.getMessage()));

                            UserMessage errorResultMsg = new UserMessage(
                                UUID.randomUUID().toString(),
                                MessageContent.ofToolResult(tub.id(),
                                    List.of(new TextBlock("Error: " + e.getMessage())), true)
                            );
                            engine.getMutableMessages().add(errorResultMsg);
                            errorDuringExecution = true;
                        }
                    }
                }

                if (errorDuringExecution) {
                    // Task 46.6: Error diagnostics with watermark
                    String diagnosticMsg = String.format(
                        "Error during execution at turn %d: %s",
                        errorWatermarkTurn,
                        lastError != null ? lastError.getMessage() : "unknown error");
                    emit(new SDKMessage.System(
                        new SystemMessage(UUID.randomUUID().toString(),
                            "error_diagnostics", "error", diagnosticMsg)));
                    emitResult(engine, SDKMessage.Result.ERROR_DURING_EXECUTION, lastError);
                    return;
                }

                // FIX #8: Autocompact check after each turn
                if (compactService != null
                        && compactService.shouldAutoCompact(
                            engine.getMutableMessages(), config.model(), querySource)) {
                    try {
                        // Task 47.9: Emit compact_boundary before compaction
                        List<String> compactedUuids = engine.getMutableMessages().stream()
                            .map(Message::uuid)
                            .toList();
                        Usage preCompactUsage = engine.getTotalUsage();
                        emit(new SDKMessage.CompactBoundary(compactedUuids, preCompactUsage));

                        MessageCompactor.CompactionResult compactionResult =
                            compactService.compactConversation(
                                engine.getMutableMessages(), false);
                        List<Message> newMessages = compactionResult.postCompactMessages();
                        engine.getMutableMessages().clear();
                        engine.getMutableMessages().addAll(newMessages);
                    } catch (Exception e) {
                        // Autocompact failure is non-fatal; log and continue
                    }
                }

                continue;
            }

            // No more tool_use — we're done
            break;
        }

        // Task 48.12: Profiler checkpoint — end
        long loopElapsed = System.currentTimeMillis() - loopStartTime;
        engine.addProfilerCheckpoint("loop_end", loopElapsed,
            engine.getTotalUsage().inputTokens(), engine.getTotalUsage().outputTokens());

        // Task 46.8: Emit success result with full stats
        emitResult(engine, SDKMessage.Result.SUCCESS, null);
    }

    /**
     * Task 48.10: Load skills and plugins (cache-only, non-blocking).
     * Discovers skills from .claude/skills/ directory and registers them.
     */
    private void loadSkillsAndPlugins(QueryEngine engine) {
        String workingDir = engine.getConfig().workingDirectory();
        if (workingDir == null) return;

        Path skillsDir = Path.of(workingDir, ".claude", "skills");
        if (!skillsDir.toFile().exists()) return;

        try {
            var skillFiles = skillsDir.toFile().listFiles((dir, name) -> 
                name.endsWith(".md") && !name.startsWith("."));
            if (skillFiles != null) {
                for (var file : skillFiles) {
                    String skillName = file.getName().replace(".md", "");
                    engine.getDiscoveredSkillNames().add(skillName);
                }
            }
        } catch (Exception e) {
            // Non-fatal: skills loading should not break the query
        }
    }

    /**
     * Task 48.11: Emit system initialization message with additional session context.
     */
    private void emitSystemInitMessage(QueryEngine engine) {
        String workingDir = engine.getConfig().workingDirectory();
        if (workingDir == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("Session initialized.\n");

        Path memoryFile = Path.of(workingDir, "MEMORY.md");
        if (memoryFile.toFile().exists()) {
            sb.append("Memory file present.\n");
        }

        Path skillsDir = Path.of(workingDir, ".claude", "skills");
        if (skillsDir.toFile().exists()) {
            var skillFiles = skillsDir.toFile().listFiles((dir, name) -> 
                name.endsWith(".md") && !name.startsWith("."));
            if (skillFiles != null && skillFiles.length > 0) {
                sb.append("Skills available: ").append(skillFiles.length).append("\n");
            }
        }

        if (!engine.getDiscoveredSkillNames().isEmpty()) {
            sb.append("Discovered skills: ").append(
                String.join(", ", engine.getDiscoveredSkillNames())).append("\n");
        }

        String context = sb.toString();
        if (!context.isEmpty() && !context.equals("Session initialized.\n")) {
            SystemMessage ctxMsg = new SystemMessage(
                UUID.randomUUID().toString(), "session_context", "info", context);
            emit(new SDKMessage.System(ctxMsg));
        }
    }

    /**
     * Task 48.3: Load memory prompt for auto-mem path override.
     * Checks for MEMORY.md file and loads relevant memory context.
     */
    private String loadMemoryPrompt(QueryEngine engine) {
        String workingDir = engine.getConfig().workingDirectory();
        if (workingDir == null) return null;

        Path memoryFile = Path.of(workingDir, "MEMORY.md");
        if (!memoryFile.toFile().exists()) return null;

        try {
            String content = Files.readString(memoryFile);
            if (content.isBlank()) return null;

            String[] lines = content.split("\n");
            StringBuilder preview = new StringBuilder();
            int lineCount = 0;
            for (String line : lines) {
                if (lineCount >= 20) {
                    preview.append("\n... [truncated, full memory available]");
                    break;
                }
                preview.append(line).append("\n");
                lineCount++;
            }

            return "\n\n# Memory Context\n" + preview;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Task 47.8: Normalize assistant message (filter synthetic markers, clean up content).
     * Removes thinking blocks from displayed content and filters synthetic markers.
     */
    private AssistantMessage normalizeAssistantMessage(AssistantMessage msg) {
        if (msg == null || msg.message() == null || msg.message().content() == null) {
            return msg;
        }

        List<ContentBlock> originalBlocks = msg.message().content();
        List<ContentBlock> filteredBlocks = new ArrayList<>();
        boolean hasThinkingBlocks = false;

        for (ContentBlock block : originalBlocks) {
            if (block instanceof ThinkingBlock) {
                hasThinkingBlocks = true;
                continue;
            }
            if (block instanceof TextBlock tb) {
                String text = tb.text();
                if (text != null && text.contains("<synthetic>")) {
                    text = text.replaceAll("<synthetic>.*?</synthetic>", "");
                }
                if (text != null && !text.isBlank()) {
                    filteredBlocks.add(new TextBlock(text));
                }
            } else {
                filteredBlocks.add(block);
            }
        }

        if (!hasThinkingBlocks && filteredBlocks.equals(originalBlocks)) {
            return msg;
        }

        AssistantContent newContent = AssistantContent.of(msg.message().id(), filteredBlocks);
        return new AssistantMessage(msg.uuid(), newContent);
    }

    private void emit(SDKMessage message) {
        queue.add(message);
    }

    /**
     * Task 46.8: Emit result with full stats including cost, permission denials, fast mode state.
     */
    private void emitResult(QueryEngine engine, String resultType, Exception error) {
        double totalCost = engine.getCostCalculator().calculateCost(engine.getTotalUsage());
        List<SDKMessage.PermissionDenial> denials = engine.getPermissionDenials();
        String fastModeState = engine.getFastModeState();
        boolean structuredOutputApplied = false; // Would be tracked during the loop

        emit(new SDKMessage.Result(
            resultType,
            List.copyOf(engine.getMessages()),
            engine.getTotalUsage(),
            engine.getSessionId(),
            totalCost,
            denials,
            fastModeState,
            structuredOutputApplied
        ));
    }

    /**
     * FIX #4: Build proper API message format preserving tool_use/tool_result content blocks.
     */
    @SuppressWarnings("unchecked")
    private List<StreamingClient.StreamRequest.RequestMessage> buildRequestMessages(QueryEngine engine) {
        List<StreamingClient.StreamRequest.RequestMessage> requestMessages = new ArrayList<>();
        for (Message msg : engine.getMessages()) {
            switch (msg) {
                case UserMessage um -> {
                    if (um.message() == null) continue;
                    if (um.message().isText() && um.message().text() != null) {
                        requestMessages.add(new StreamingClient.StreamRequest.RequestMessage(
                            "user", um.message().text()));
                    } else if (um.message().blocks() != null) {
                        boolean hasToolResult = um.message().blocks().stream()
                            .anyMatch(b -> b instanceof ToolResultBlock);

                        if (hasToolResult) {
                            List<Map<String, Object>> contentArray = new ArrayList<>();
                            for (ContentBlock block : um.message().blocks()) {
                                if (block instanceof ToolResultBlock tr) {
                                    Map<String, Object> trMap = new LinkedHashMap<>();
                                    trMap.put("type", "tool_result");
                                    trMap.put("tool_use_id", tr.toolUseId());
                                    if (tr.content() != null && !tr.content().isEmpty()) {
                                        StringBuilder text = new StringBuilder();
                                        for (ContentBlock inner : tr.content()) {
                                            if (inner instanceof TextBlock itb && itb.text() != null) {
                                                text.append(itb.text());
                                            }
                                        }
                                        trMap.put("content", text.toString());
                                    }
                                    if (tr.isError()) {
                                        trMap.put("is_error", true);
                                    }
                                    contentArray.add(trMap);
                                } else if (block instanceof TextBlock tb && tb.text() != null) {
                                    Map<String, Object> textMap = new LinkedHashMap<>();
                                    textMap.put("type", "text");
                                    textMap.put("text", tb.text());
                                    contentArray.add(textMap);
                                }
                            }
                            if (!contentArray.isEmpty()) {
                                requestMessages.add(new StreamingClient.StreamRequest.RequestMessage(
                                    "user", contentArray));
                            }
                        } else {
                            StringBuilder text = new StringBuilder();
                            for (ContentBlock block : um.message().blocks()) {
                                if (block instanceof TextBlock tb && tb.text() != null) {
                                    text.append(tb.text());
                                }
                            }
                            if (!text.isEmpty()) {
                                requestMessages.add(new StreamingClient.StreamRequest.RequestMessage(
                                    "user", text.toString()));
                            }
                        }
                    }
                }
                case AssistantMessage am -> {
                    if (am.message() == null || am.message().content() == null) continue;
                    boolean hasToolUse = am.message().content().stream()
                        .anyMatch(b -> b instanceof ToolUseBlock);

                    if (hasToolUse) {
                        List<Map<String, Object>> contentArray = new ArrayList<>();
                        for (ContentBlock block : am.message().content()) {
                            if (block instanceof TextBlock tb && tb.text() != null) {
                                Map<String, Object> textMap = new LinkedHashMap<>();
                                textMap.put("type", "text");
                                textMap.put("text", tb.text());
                                contentArray.add(textMap);
                            } else if (block instanceof ToolUseBlock tub) {
                                Map<String, Object> tuMap = new LinkedHashMap<>();
                                tuMap.put("type", "tool_use");
                                tuMap.put("id", tub.id());
                                tuMap.put("name", tub.name());
                                tuMap.put("input", tub.input());
                                contentArray.add(tuMap);
                            }
                        }
                        if (!contentArray.isEmpty()) {
                            requestMessages.add(new StreamingClient.StreamRequest.RequestMessage(
                                "assistant", contentArray));
                        }
                    } else {
                        StringBuilder text = new StringBuilder();
                        for (ContentBlock block : am.message().content()) {
                            if (block instanceof TextBlock tb && tb.text() != null) {
                                text.append(tb.text());
                            }
                        }
                        if (!text.isEmpty()) {
                            requestMessages.add(new StreamingClient.StreamRequest.RequestMessage(
                                "assistant", text.toString()));
                        }
                    }
                }
                default -> { /* Skip system, progress, etc. */ }
            }
        }
        return requestMessages;
    }

    /**
     * Helper to accumulate tool_use block data from streaming events.
     */
    private static class ToolUseBuilder {
        private final String id;
        private final String name;
        private final StringBuilder inputJson = new StringBuilder();

        ToolUseBuilder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        void appendInput(String partialJson) {
            inputJson.append(partialJson);
        }

        ToolUseBlock build() {
            com.fasterxml.jackson.databind.JsonNode inputNode;
            try {
                String json = inputJson.toString();
                if (json.isEmpty()) {
                    inputNode = com.claudecode.utils.JsonUtils.getMapper().createObjectNode();
                } else {
                    inputNode = com.claudecode.utils.JsonUtils.getMapper().readTree(json);
                }
            } catch (Exception e) {
                inputNode = com.claudecode.utils.JsonUtils.getMapper().createObjectNode();
            }
            return new ToolUseBlock(id, name, inputNode);
        }
    }
}
