# Claude Code Java — Development Guide

Claude Code 的 Java 版本复刻，一比一迁移自 TypeScript/Bun 技术栈。

## Project Overview

| Item | Value |
|------|-------|
| Type | Maven multi-module Java project |
| Java Version | 21+ |
| Build Tool | Maven |
| Modules | 14 |
| Source Files | ~500+ Java files |
| Status | Phase 0-5 completed, Phase 6 (testing) in progress |

## Architecture

```
claude-code-java/
├── claude-code-utils/        # Common utilities (FileUtils, JsonUtils, StringUtils)
├── claude-code-core/         # Core models, types, state management, QueryEngine
├── claude-code-api/          # Anthropic API client (multi-SDK adapter)
├── claude-code-permissions/   # Permission system (allow/deny/ask)
├── claude-code-tools/        # Tool system: 54 built-in tools
├── claude-code-commands/     # Command system: 102 slash commands
├── claude-code-mcp/          # MCP (Model Context Protocol) integration
├── claude-code-bridge/        # IDE Bridge (WebSocket/SSE, CCR v1/v2)
├── claude-code-session/      # Session management (JSONL persistence)
├── claude-code-services/     # Service layer (compact, hooks, memory, etc.)
├── claude-code-ui/           # Terminal UI (JLine3 + ANSI rendering)
├── claude-code-lsp/          # LSP (Language Server Protocol) integration
├── claude-code-cli/          # CLI entry point (Picocli)
└── claude-code-app/          # Application assembly and packaging
```

### Module Dependencies

```
claude-code-cli
  ├── claude-code-ui
  │     ├── claude-code-commands
  │     └── claude-code-core
  ├── claude-code-tools (core + permissions + session)
  ├── claude-code-services (core + api + mcp + session + tools)
  ├── claude-code-bridge
  └── claude-code-utils

claude-code-api → claude-code-core + claude-code-utils
claude-code-tools → claude-code-core + claude-code-permissions + claude-code-session + claude-code-utils
claude-code-services → claude-code-core + claude-code-api + claude-code-mcp + claude-code-session + claude-code-tools + claude-code-utils
```

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Java 21 (Records, Sealed Classes, Pattern Matching, Virtual Threads) |
| Build | Maven (Kotlin DSL in parent pom.xml) |
| JSON | Jackson 2.17.2 (with JDK8 and JSR310 modules) |
| Terminal UI | JLine3 3.26.3 |
| CLI | Picocli 4.7.6 |
| Markdown | commonmark-java 0.22.0 |
| Logging | SLF4J 2.0.13 + Logback 1.5.8 |
| Utils | Guava 33.2.1, Commons Lang3 3.14.0, Commons IO 2.16.1 |
| LSP | Eclipse LSP4J 0.24.0 |
| Testing | JUnit 5.10.3, jqwik 1.9.0 (PBT) |

## Terminal UI System (claude-code-ui)

The terminal UI system replaces React+Ink with JLine3 + custom ANSI rendering.

### Core Components

| Component | File | Description |
|-----------|------|-------------|
| `TerminalRenderer` | `TerminalRenderer.java` | JLine3 Terminal wrapper, styled output, size monitoring |
| `ReplScreen` | `ReplScreen.java` | Main REPL orchestration, input dispatch, message rendering |
| `MessageRenderDispatcher` | `MessageRenderDispatcher.java` | SDKMessage → terminal rendering dispatcher |
| `Ansi` | `Ansi.java` | ANSI escape utilities, color/style detection |
| `AnsiColor` | `AnsiColor.java` | Foreground color constants (RED, GREEN, YELLOW, CYAN, etc.) |
| `AnsiStyle` | `AnsiStyle.java` | Style constants (BOLD, ITALIC, DIM, UNDERLINE, etc.) |
| `TerminalProtocols` | `TerminalProtocols.java` | iTerm2/Kitty/ANSI escape code utilities |
| `TerminalNotification` | `TerminalNotification.java` | Terminal notification and progress bar |
| `TranscriptScreen` | `TranscriptScreen.java` | Full-screen transcript viewer (Ctrl+O) |
| `StreamingTokenCounter` | `StreamingTokenCounter.java` | Real-time token counting during streaming |

### Input Components

| Component | File | Description |
|-----------|------|-------------|
| `InputReader` | `InputReader.java` | JLine3 LineReader wrapper, history, tab completion |
| `EnhancedInputReader` | `EnhancedInputReader.java` | Extended input with notifications, mode indicator |
| `EnhancedInputReaderV2` | `EnhancedInputReaderV2.java` | Enhanced input with history search, multi-line, @ mentions |
| `VimStateMachine` | `vim/VimStateMachine.java` | Full Vim state machine (INSERT/NORMAL/VISUAL/COMMAND) |
| `VimKeyMapBinder` | `vim/VimKeyMapBinder.java` | JLine3 KeyMap integration for Vim bindings |

### Rendering Components

| Component | File | Description |
|-----------|------|-------------|
| `MarkdownRenderer` | `MarkdownRenderer.java` | Commonmark + ANSI, tables, lists, code blocks |
| `SyntaxHighlighter` | `SyntaxHighlighter.java` | Multi-language syntax highlighting (Java, Python, JS, Bash) |
| `DiffRenderer` | `DiffRenderer.java` | Unified diff with ANSI colors, LCS-based computation |
| `Spinner` | `Spinner.java` | Animated progress indicator, multiple frame sets |
| `StreamingMarkdownRenderer` | `renderer/StreamingMarkdownRenderer.java` | Incremental rendering with caching |
| `ToolUseRenderer` | `renderer/ToolUseRenderer.java` | Tool call blocks with icons |
| `ThinkingBlockRenderer` | `renderer/ThinkingBlockRenderer.java` | Thinking blocks with shimmer animation |
| `CollapsibleGroupRenderer` | `renderer/CollapsibleGroupRenderer.java` | Expandable message groups |
| `RateLimitRenderer` | `renderer/RateLimitRenderer.java` | Rate limit warnings with progress |
| `ImageMessageRenderer` | `renderer/ImageMessageRenderer.java` | iTerm2/Kitty image display |
| `MessageBubbleRenderer` | `renderer/MessageBubbleRenderer.java` | Bubble chat UI for messages |

### Specialized Renderers (`renderer/` package)

| Component | Description |
|-----------|-------------|
| `StreamingMarkdownRenderer` | Incremental markdown rendering with stable prefix caching |
| `ToolUseRenderer` | Tool call blocks with icons and progress |
| `ThinkingBlockRenderer` | Thinking blocks with duration display |
| `CollapsibleGroupRenderer` | Expandable/collapsible message groups |
| `RateLimitRenderer` | Rate limit notification rendering |
| `HookProgressRenderer` | Hook execution progress display |
| `TeammateMessageRenderer` | Multi-agent message rendering |
| `ImageMessageRenderer` | Terminal image display (iTerm2/Kitty protocols) |
| `OrderedListRenderer` | Numbered list rendering |
| `PlainTextDetector` | Fast path for non-markdown text |
| `PromptXmlStripper` | XML tag stripping for prompts |
| `SelectionIndicator` | Message line selection |
| `VerboseToggle` | Verbose/detail toggle |

### Dialog Components

| Component | File | Description |
|-----------|------|-------------|
| `Dialog` | `Dialog.java` | Generic dialog framework |
| `PermissionDialog` | `PermissionDialog.java` | Base permission prompt (a/d/o/c) |
| `FilePermissionDialog` | `dialog/FilePermissionDialog.java` | File operation permission |
| `WebPermissionDialog` | `dialog/WebPermissionDialog.java` | Web access permission |
| `BashPermissionDialog` | `dialog/BashPermissionDialog.java` | Bash command permission |
| `PowerShellPermissionDialog` | `dialog/PowerShellPermissionDialog.java` | PowerShell permission |
| `SedEditPermissionDialog` | `dialog/SedEditPermissionDialog.java` | Sed edit permission |
| `SkillPermissionDialog` | `dialog/SkillPermissionDialog.java` | Skill execution permission |
| `ComputerPermissionDialog` | `dialog/ComputerPermissionDialog.java` | Computer use permission |

### Menu Components

| Component | File | Description |
|-----------|------|-------------|
| `AgentMenu` | `agent/AgentMenu.java` | Agent list/details/editor |
| `AgentConfigMenu` | `agent/AgentConfigMenu.java` | Agent model/tool/color selector |
| `AgentCreationWizard` | `agent/AgentCreationWizard.java` | Multi-step agent creation |
| `AgentProgressLine` | `agent/AgentProgressLine.java` | Agent progress indicator |
| `AgentStatusBar` | `agent/AgentStatusBar.java` | Current agent status |
| `McpSettingsMenu` | `mcp/McpSettingsMenu.java` | MCP configuration panel |
| `McpServerList` | `mcp/McpServerList.java` | MCP server table view |
| `McpToolViewer` | `mcp/McpToolViewer.java` | MCP tool list/detail |
| `McpServerApprovalDialog` | `mcp/McpServerApprovalDialog.java` | MCP server approval |
| `McpCapabilityPanel` | `mcp/McpCapabilityPanel.java` | MCP capabilities display |
| `McpServerMultiSelect` | `mcp/McpServerMultiSelect.java` | MCP server multi-select |
| `SettingsMenu` | `settings/SettingsMenu.java` | Settings panel |
| `TaskListMenu` | `task/TaskListMenu.java` | Task list view |
| `TaskDetailDialog` | `task/TaskDetailDialog.java` | Task detail dialog |
| `TaskDetailDialog` | `task/ShellTaskDetail.java` | Shell task output |
| `BackgroundTaskViewer` | `task/BackgroundTaskViewer.java` | Background task browser |

### Status & Utility Components

| Component | File | Description |
|-----------|------|-------------|
| `StatusLine` | `StatusLine.java` | Bottom status bar (model, tokens, cost, cwd) |
| `TerminalSize` | `TerminalSize.java` | Terminal dimensions |
| `InterruptHandler` | `InterruptHandler.java` | Ctrl+C / SIGINT handling |
| `VirtualScroller` | `VirtualScroller.java` | Large list virtualization |

## Vim Mode (67 sub-tasks completed)

Full Vim state machine implementation:

| Feature | Status | Description |
|---------|--------|-------------|
| INSERT mode | ✓ | Standard text insertion |
| NORMAL mode | ✓ | Commands, motions, operators |
| VISUAL mode | ✓ | Character/line/block selection |
| COMMAND mode | ✓ | `:w`, `:q`, `:s/` commands |
| Operators (d, c, y) | ✓ | Delete, change, yank |
| Motions (h,j,k,l,w,b,e) | ✓ | Basic navigation |
| Text objects (iw, aw, i", a") | ✓ | Inner/around word, quote, etc. |
| Count/multiplier (3w, 5j) | ✓ | Repeat commands |
| Dot-repeat (.) | ✓ | Repeat last change |
| Undo/redo (u, Ctrl+R) | ✓ | Full undo history |
| Find (f, F, t, T) | ✓ | Character search |
| Repeat find (; ,) | ✓ | Repeat last find |
| Named registers | ✓ | a-z, 0-9, " registers |
| Macros (q, @) | ✓ | Record/playback |
| Visual selection | ✓ | Shift+arrows, d, y |

## Development Standards

### Java 21 Features

- **Records** for immutable data classes (Message types, Tool inputs/outputs, Config)
- **Sealed interfaces** for closed type hierarchies (Message, ContentBlock, SDKMessage, PermissionDecision, HookCommand)
- **Pattern matching** with `switch` expressions
- **Virtual Threads** for async I/O (QueryMessageIterator, BashTool execution)

### Code Style

- Package-private by default; `public` only when necessary
- Records over classes for data carriers
- Sealed interfaces for union types (replacing TS union types)
- Builder pattern for complex object construction
- Try-with-resources for resource management

### Type Mapping (TS → Java)

| TypeScript | Java |
|------------|------|
| `async function*` | `Iterator<T>` + Virtual Thread producer |
| `union type` | `sealed interface` + `permits` |
| `interface` | `record` or `class` |
| `Partial<T>` | `Optional<T>` or builder with optional fields |
| `Record<K,V>` | `Map<K,V>` |
| `Array<T>` | `List<T>` or `T[]` |
| `Promise<T>` | `CompletableFuture<T>` |
| `EventEmitter` | `Store<T>` + listeners |
| `Zod` schema | JSON Schema + Jackson validation |
| `AsyncGenerator` | `Iterator<T>` via `BlockingQueue` |

### Async Model

TS AsyncGenerators are mapped using Virtual Threads + BlockingQueue:

```java
// Virtual Thread produces, main thread consumes
Iterator<SDKMessage> messages = queryEngine.submitMessage(prompt, options);
while (messages.hasNext()) {
    SDKMessage msg = messages.next();
    // process message
}
```

## Building

### Full Build

```bash
mvn clean install
```

### Single Module

```bash
mvn -pl claude-code-core clean install
mvn -pl claude-code-cli clean package
```

### Skip Tests

```bash
mvn clean install -DskipTests
```

### Run Application

```bash
java -jar claude-code-app/target/claude-code-java-*-jar-with-dependencies.jar
```

## Testing

### Run All Tests

```bash
mvn test
```

### Run Specific Module Tests

```bash
mvn -pl claude-code-core test
```

### Property-Based Testing

jqwik is configured for PBT tests. Test classes ending with `Properties.java` contain PBT tests validating correctness properties (CP-1 through CP-7).

### Correctness Properties (CP)

| ID | Property | Test Class |
|----|----------|------------|
| CP-1 | API protocol correctness | `MessageSerializationProperties` |
| CP-2 | Tool execution determinism | `ToolExecutionDeterminismProperties` |
| CP-3 | Permission decision consistency | `PermissionDecisionProperties` |
| CP-4 | Message serialization compatibility | `MessageSerializationProperties` |
| CP-5 | State management invariants | `StateManagementProperties` |
| CP-6 | Task lifecycle correctness | `TaskLifecycleProperties` |
| CP-7 | Session compaction safety | `CompactSafetyProperties` |

## Key Implementation Notes

### QueryEngine

`claude-code-core/src/main/java/com/claudecode/core/engine/QueryEngine.java`

- Strictly follows TS `src/QueryEngine.ts` logic
- Virtual Thread + BlockingQueue for streaming responses
- Handles tool_use loop, budget tracking, compaction, structured output

### Tool System

`claude-code-tools/src/main/java/com/claudecode/tools/`

- 54 built-in tools implemented
- P0: BashTool, FileReadTool, FileWriteTool, FileEditTool, GrepTool, GlobTool
- P1: AgentTool, WebFetchTool, WebSearchTool, MCPTool, Task* tools
- P2: NotebookEditTool, LSPTool, SkillTool, SendMessageTool, SyntheticOutputTool
- P3: TeamCreateTool, WorkflowTool, WebBrowserTool (feature-gated)

### Session Persistence

`claude-code-session/src/main/java/com/claudecode/session/SessionStorage.java`

- JSONL format compatible with TypeScript version
- Jackson ObjectMapper with polymorphic types
- File locking for concurrent safety

### Terminal UI (JLine3)

`claude-code-ui/src/main/java/com/claudecode/ui/`

- **TerminalRenderer**: JLine3 Terminal wrapper with styled output
- **ReplScreen**: Main REPL orchestration
- **MarkdownRenderer**: Commonmark + ANSI rendering with Caffeine cache
- **SyntaxHighlighter**: Multi-language syntax highlighting
- **Spinner**: Animated progress with dynamic verb/frame sets
- **VirtualScroller**: Large list virtualization
- **StatusLine**: Bottom status bar
- **VimStateMachine**: Full Vim mode (INSERT/NORMAL/VISUAL/COMMAND)
- **Dialog/PermissionDialog**: Interactive dialogs

### Hooks System

`claude-code-services/src/main/java/com/claudecode/services/hooks/`

- 27 hook event types
- 4 hook command types: BashCommandHook, PromptHook, HttpHook, AgentHook
- Supports if conditions, matchers, once/async/asyncRewake modes

### Compact System

`claude-code-services/src/main/java/com/claudecode/services/compact/`

- MicroCompact: per-round tool output truncation
- AutoCompact: token threshold triggered (93% context window)
- ManualCompact: `/compact` command
- PartialCompact, SnipCompact, ReactiveCompact, SessionMemoryCompact

## Configuration

- API Key: `ANTHROPIC_API_KEY` environment variable or `~/.claude/credentials.json`
- Settings: `~/.claude/settings.json` (JSONC format)
- Project instructions: `./CLAUDE.md`
- Session storage: `~/.claude/sessions/`

## Common Tasks

### Add a New Tool

1. Create `claude-code-tools/src/main/java/com/claudecode/tools/YourTool.java`
2. Extend `Tool<I, O>` abstract class
3. Implement `name()`, `description()`, `inputSchema()`, `call()`, `checkPermissions()`
4. Register in `ToolRegistry`
5. Add tests in `claude-code-tools/src/test/java/`

### Add a New Command

1. Create `claude-code-commands/src/main/java/com/claudecode/commands/impl/YourCommand.java`
2. Implement `Command` interface
3. Register in `CommandFactory`

### Add a New Service

1. Add to appropriate module under `claude-code-services/src/main/java/com/claudecode/services/`
2. Follow package conventions (e.g., `services/hooks/`, `services/memory/`)
3. Add tests in corresponding test package

### Add a New UI Component

1. Determine component type:
   - Core: `ui/` package
   - Renderer: `ui/renderer/` package
   - Dialog: `ui/dialog/` package
   - Menu: `ui/agent/`, `ui/mcp/`, `ui/task/`, `ui/settings/` packages
2. Follow existing patterns (Ansi styling, JLine3 integration)
3. Add tests in `ui/src/test/java/com/claudecode/ui/`

## Known Differences from TypeScript Version

1. **Terminal UI**: React+Ink → JLine3 + manual ANSI rendering
2. **Dynamic types**: TypeScript union types → Java sealed interfaces
3. **npm packages**: Replaced with Maven dependencies
4. **Bun runtime**: JVM with Virtual Threads
5. **Buddy animations**: Not implemented (decorative feature)

## Terminal UI Gaps Analysis (vs Claude Code)

Based on Claude Code changelog analysis, the following features need implementation:

### Critical Gaps (用户体验核心)

| Feature | Priority | Description | Implementation Plan |
|---------|----------|-------------|-------------------|
| **Transcript Mode** | P0 | Ctrl+O toggle full-screen transcript view | Add `TranscriptScreen` mode to `ReplScreen` |
| **Image Display Protocol** | P0 | iTerm2/Kitty direct mode for inline images | Add `ImageProtocol` enum + `TerminalImageRenderer` |
| **Notification System** | P1 | OSC 9;4 progress bar, iTerm2/Kitty/Ghostty notifications | Add `TerminalNotification` class |
| **Hyperlink Support** | P1 | OSC 8 clickable links for files/URLs | Extend `Ansi` with hyperlink codes |
| **Shimmer Animation** | P1 | Subtle shimmer effect for thinking blocks | Add to `ThinkingBlockRenderer` |
| **Ghost Text Suggestions** | P2 | Bash command ghost text in input | Enhance `EnhancedInputReader` |
| **Message Bubble UI** | P2 | User/assistant bubble styling | Add `MessageBubbleRenderer` |

### Input Enhancements

| Feature | Priority | Description |
|---------|----------|-------------|
| **Ctrl+R History Search** | P1 | Interactive history search in input |
| **@ File Mention** | P2 | File autocomplete in input |
| **Shift+Enter Multi-line** | P1 | Multi-line input support |
| **Image Paste** | P2 | Clipboard image detection and paste |

### Interactive Features

| Feature | Priority | Description |
|---------|----------|-------------|
| **Menu Navigation** | P1 | Full Tab/Arrow/Enter navigation for all menus |
| **Permission Queue** | P1 | Multiple pending permissions display |
| **Teammate Tree UI** | P2 | Hierarchical agent status tree |
| **Context Menu** | P2 | Right-click style context menus |

### Rendering Improvements

| Feature | Priority | Description |
|---------|----------|-------------|
| **Streaming Token Counter** | P1 | Real-time token count during streaming |
| **Search Highlight** | P1 | Real-time match highlighting in search |
| **Alt Screen Mode** | P2 | Full-screen alternate buffer rendering |
| **Rate Limit UI** | P1 | Enhanced rate limit warnings with reset time |

### Planned Implementation Order

1. **Phase 6.1**: ✅ Transcript Mode + Image Protocols (P0) — `TranscriptScreen.java`, `TerminalProtocols.java`, `ImageMessageRenderer.java`
2. **Phase 6.2**: ✅ Notification System + Hyperlinks (P1) — `TerminalNotification.java`, `TerminalProtocols.java`
3. **Phase 6.3**: ✅ Input Enhancements (P1) — `EnhancedInputReaderV2.java`, history search, multi-line support
4. **Phase 6.4**: Menu Navigation + Permission Queue (P1) — TODO
5. **Phase 6.5**: ✅ Streaming improvements + Shimmer (P1) — `StreamingTokenCounter.java`, enhanced `ThinkingBlockRenderer.java`
6. **Phase 6.6**: ✅ Bubble UI + Rate Limit (P2) — `MessageBubbleRenderer.java`, `RateLimitRenderer.java`

## Remaining Work

- [ ] Phase 6: Complete test coverage (80%+ for core modules)
- [ ] End-to-end integration tests
- [ ] Cross-platform compatibility tests
- [ ] GraalVM native-image optimization
- [ ] Phase 6.1-6.6: Terminal UI enhancements

## UI Component Usage Examples

### Render Markdown

```java
MarkdownRenderer renderer = new MarkdownRenderer();
String output = renderer.render("# Hello\n\nThis is **bold** text.");
terminal.println(output);
```

### Show Permission Dialog

```java
PermissionDialog dialog = new PermissionDialog(terminal.writer());
PermissionDialog.Decision decision = dialog.showBashPermission(
    "rm -rf /tmp/*", "tool_use_123");
```

### Vim Input

```java
VimStateMachine vim = new VimStateMachine();
vim.processKey('i');  // Enter INSERT mode
vim.processKey('x');  // Exit to NORMAL mode
String modeIndicator = vim.getModeIndicator();  // "-- INSERT --"
```

### Spinner Animation

```java
Spinner spinner = new Spinner(terminal.writer(), "Processing...");
spinner.start();
spinner.setVerb("searching");
spinner.setActiveToolCount(2);
// ... do work ...
spinner.stop();
```

### Virtual Scrolling

```java
VirtualScroller<Message> scroller = new VirtualScroller<>(20);
scroller.setItems(messages);
List<Message> visible = scroller.getVisibleItems();
scroller.scrollUp(5);
```

### iTerm2 Image Display

```java
// OSC 1337 file path
String itermImage = "\u001B]1337;File=path=%s:height=0.5\u0007";

// OSC 8 Hyperlink
String hyperlink = "\u001B]8;;" + url + "\u001B\\" + text + "\u001B]8;;\u001B\\";
```

### Terminal Notification (OSC 9;4)

```java
// Progress bar
String progress = "\u001B]9;4;" + percentage + ";" + title + "\u0007";

// Clear progress
String clear = "\u001B]9;4;\u0007";
```

### Streaming Message Renderer

```java
StreamingMarkdownRenderer streaming = new StreamingMarkdownRenderer();
StreamingMarkdownRenderer.RenderedOutput output = streaming.renderIncremental(newContent);
if (output.hasChange()) {
    terminal.print(output.unstableSuffix());
}
```

### Terminal Notifications

```java
TerminalNotification notification = new TerminalNotification(terminal.writer());

// Show progress bar
notification.showProgress("Processing files...", 50);

// Show indeterminate progress (spinner)
notification.showIndeterminate("Loading...");

// Show notification
notification.notify("Task Complete", "All files processed");

// Clear
notification.clear();
```

### Transcript Mode

```java
TranscriptScreen transcript = new TranscriptScreen(terminal, renderer, dispatcher);

// Enter transcript mode
transcript.enter();

// Add messages
transcript.addMessage(sdkMessage);

// Handle keyboard navigation
transcript.handleKey('j');  // scroll down
transcript.handleKey('/');  // start search
transcript.setSearchQuery("error");
transcript.searchNext();  // n
transcript.searchPrevious();  // N

// Exit
transcript.exit();
```

### Image Display

```java
ImageMessageRenderer imageRenderer = new ImageMessageRenderer();

// Render image file
String output = imageRenderer.render(Path.of("/path/to/image.png"));

// Render clipboard image
String clipboardOutput = imageRenderer.renderClipboardImage(pngBytes);
```

### Token Counter

```java
StreamingTokenCounter counter = new StreamingTokenCounter();
counter.start();

// During streaming, update tokens
counter.updateTokens(inputTokens, outputTokens, cacheRead, cacheWrite);

// Build status string for spinner
String status = counter.buildSpinnerSuffix();
```

### Bubble Chat UI

```java
MessageBubbleRenderer bubbleRenderer = new MessageBubbleRenderer(terminalWidth, true, true);
bubbleRenderer.setTheme(BubbleTheme.ROUNDED);

// Render user message
terminal.println(bubbleRenderer.renderUserMessage(userMessage));

// Render assistant message
terminal.println(bubbleRenderer.renderAssistantMessage(assistantMessage, "claude-sonnet-4"));

// Render tool group
terminal.println(bubbleRenderer.renderToolGroup(toolUses, results));
```

## Terminal ANSI Escape Code Reference

Claude Code uses standard ANSI escape codes for terminal control:

| Escape | Code | Usage |
|--------|------|-------|
| Reset | `\u001B[0m` | Reset all attributes |
| Bold | `\u001B[1m` | Bold text |
| Dim | `\u001B[2m` | Dim/faded text |
| Italic | `\u001B[3m` | Italic text |
| Underline | `\u001B[4m` | Underlined text |
| Black | `\u001B[30m` | Foreground color |
| Red | `\u001B[31m` | Foreground color |
| Green | `\u001B[32m` | Foreground color |
| Yellow | `\u001B[33m` | Foreground color |
| Blue | `\u001B[34m` | Foreground color |
| Magenta | `\u001B[35m` | Foreground color |
| Cyan | `\u001B[36m` | Foreground color |
| White | `\u001B[37m` | Foreground color |
| Default | `\u001B[39m` | Default foreground |
| BgBlack | `\u001B[40m` | Background color |
| Clear Line | `\u001B[K` | Clear from cursor to EOL |
| Save Cursor | `\u001B[s` | Save cursor position |
| Restore Cursor | `\u001B[u` | Restore cursor position |
| Move Cursor | `\u001B[{row};{col}H` | Move cursor to position |
| OSC 8 Link | `\u001B]8;;{url}\u001B\\{text}\u001B]8;;\u001B\\` | Hyperlink |
| OSC 1337 Image | `\u001B]1337;File=...` | iTerm2 inline image |
| OSC 9;4 Progress | `\u001B]9;4;{pct};{title}\u0007` | Progress bar |
| OSC 9;4 Clear | `\u001B]9;4;\u0007` | Clear progress bar |
| Bell | `\u0007` | Terminal bell |

## References

- Requirements: `requirements.md`
- Design: `design.md`
- Task List: `tasks.md`
- TypeScript Source: `claude-code-rev/` (if available)
