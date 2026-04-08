package com.claudecode.commands;

import com.claudecode.commands.impl.*;
import com.claudecode.core.message.Usage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CommandRegistryTest {

    private CommandRegistry registry;
    private CommandContext context;

    @BeforeEach
    void setUp() {
        registry = new CommandRegistry();
        context = CommandContext.minimal();
    }

    // ---- Registration and Lookup ----

    @Nested
    class RegistrationAndLookup {

        @Test
        void registerAndFindByName() {
            registry.register(new ExitCommand());
            Optional<Command> found = registry.find("exit");
            assertTrue(found.isPresent());
            assertEquals("exit", found.get().name());
        }

        @Test
        void findIsCaseInsensitive() {
            registry.register(new ExitCommand());
            assertTrue(registry.find("EXIT").isPresent());
            assertTrue(registry.find("Exit").isPresent());
        }

        @Test
        void findByAlias() {
            registry.register(new ExitCommand());
            Optional<Command> found = registry.find("quit");
            assertTrue(found.isPresent());
            assertEquals("exit", found.get().name());
        }

        @Test
        void findUnknownReturnsEmpty() {
            assertTrue(registry.find("nonexistent").isEmpty());
        }

        @Test
        void findNullReturnsEmpty() {
            assertTrue(registry.find(null).isEmpty());
        }

        @Test
        void findBlankReturnsEmpty() {
            assertTrue(registry.find("  ").isEmpty());
        }

        @Test
        void getAllReturnsRegisteredCommands() {
            registry.register(new ExitCommand());
            registry.register(new ClearCommand());
            List<Command> all = registry.getAll();
            assertEquals(2, all.size());
        }

        @Test
        void getAllReturnsImmutableList() {
            registry.register(new ExitCommand());
            List<Command> all = registry.getAll();
            assertThrows(UnsupportedOperationException.class,
                () -> all.add(new ClearCommand()));
        }
    }

    // ---- Dispatch ----

    @Nested
    class Dispatch {

        @Test
        void dispatchKnownCommand() {
            registry.register(new ExitCommand());
            CommandResult result = registry.dispatch("/exit", context);
            assertTrue(result.shouldExit());
            assertEquals("Goodbye!", result.output());
        }

        @Test
        void dispatchUnknownCommand() {
            CommandResult result = registry.dispatch("/foo", context);
            assertFalse(result.shouldExit());
            assertTrue(result.output().contains("Unknown command"));
        }

        @Test
        void dispatchEmptyInput() {
            CommandResult result = registry.dispatch("", context);
            assertEquals("Empty command.", result.output());
        }

        @Test
        void dispatchNullInput() {
            CommandResult result = registry.dispatch(null, context);
            assertEquals("Empty command.", result.output());
        }

        @Test
        void dispatchNonSlashInput() {
            CommandResult result = registry.dispatch("hello", context);
            assertTrue(result.output().contains("Not a command"));
        }

        @Test
        void dispatchWithArgs() {
            registry.register(new ModelCommand());
            CommandResult result = registry.dispatch("/model claude-opus", context);
            assertTrue(result.output().contains("Model changed to: claude-opus"));
        }

        @Test
        void dispatchByAlias() {
            registry.register(new ExitCommand());
            CommandResult result = registry.dispatch("/quit", context);
            assertTrue(result.shouldExit());
        }

        @Test
        void dispatchUnavailableCommand() {
            Command unavailable = new Command() {
                @Override public String name() { return "secret"; }
                @Override public String description() { return "Secret"; }
                @Override public CommandResult execute(CommandContext ctx, String args) {
                    return CommandResult.of("secret!");
                }
                @Override public boolean isAvailable(CommandContext ctx) { return false; }
            };
            registry.register(unavailable);
            CommandResult result = registry.dispatch("/secret", context);
            assertTrue(result.output().contains("not available"));
        }
    }

    // ---- Command Parsing ----

    @Nested
    class Parsing {

        @Test
        void parseSimpleCommand() {
            var parsed = CommandRegistry.parseInput("/help");
            assertEquals("help", parsed.name());
            assertEquals("", parsed.args());
        }

        @Test
        void parseCommandWithArgs() {
            var parsed = CommandRegistry.parseInput("/model claude-opus");
            assertEquals("model", parsed.name());
            assertEquals("claude-opus", parsed.args());
        }

        @Test
        void parseCommandWithMultipleArgs() {
            var parsed = CommandRegistry.parseInput("/model claude-opus --fast");
            assertEquals("model", parsed.name());
            assertEquals("claude-opus --fast", parsed.args());
        }

        @Test
        void parseEmptyInput() {
            var parsed = CommandRegistry.parseInput("");
            assertEquals("", parsed.name());
            assertEquals("", parsed.args());
        }

        @Test
        void parseNullInput() {
            var parsed = CommandRegistry.parseInput(null);
            assertEquals("", parsed.name());
            assertEquals("", parsed.args());
        }

        @Test
        void parseWhitespaceInput() {
            var parsed = CommandRegistry.parseInput("   ");
            assertEquals("", parsed.name());
            assertEquals("", parsed.args());
        }

        @Test
        void parseCommandNameIsCaseInsensitive() {
            var parsed = CommandRegistry.parseInput("/HELP");
            assertEquals("help", parsed.name());
        }

        @Test
        void parseCommandWithLeadingWhitespace() {
            var parsed = CommandRegistry.parseInput("  /help  ");
            assertEquals("help", parsed.name());
        }

        @Test
        void parseCommandWithExtraSpaces() {
            var parsed = CommandRegistry.parseInput("/model   claude-opus");
            assertEquals("model", parsed.name());
            assertEquals("claude-opus", parsed.args());
        }
    }

    // ---- P0 Command Behavior ----

    @Nested
    class P0Commands {

        @Test
        void helpListsCommands() {
            CommandRegistry reg = CommandFactory.createDefault();
            CommandResult result = reg.dispatch("/help", context);
            assertFalse(result.shouldExit());
            assertTrue(result.output().contains("/help"));
            assertTrue(result.output().contains("/exit"));
            assertTrue(result.output().contains("/clear"));
            assertTrue(result.output().contains("/model"));
            assertTrue(result.output().contains("/cost"));
        }

        @Test
        void exitReturnsShouldExit() {
            registry.register(new ExitCommand());
            CommandResult result = registry.dispatch("/exit", context);
            assertTrue(result.shouldExit());
            assertEquals("Goodbye!", result.output());
        }

        @Test
        void clearClearsMessages() {
            List<Object> cleared = new ArrayList<>();
            CommandContext ctx = new CommandContext(
                "test-model",
                List::of,
                () -> cleared.add("cleared"),
                m -> {},
                () -> Usage.EMPTY,
                u -> 0.0,
                "/tmp",
                false
            );
            registry.register(new ClearCommand());
            CommandResult result = registry.dispatch("/clear", ctx);
            assertFalse(result.shouldExit());
            assertTrue(result.output().contains("Conversation cleared"));
            assertEquals(1, cleared.size());
        }

        @Test
        void modelShowsCurrentModel() {
            registry.register(new ModelCommand());
            CommandResult result = registry.dispatch("/model", context);
            assertTrue(result.output().contains("claude-sonnet-4-20250514"));
        }

        @Test
        void modelChangesModel() {
            AtomicReference<String> modelRef = new AtomicReference<>("old-model");
            CommandContext ctx = new CommandContext(
                "old-model",
                List::of,
                () -> {},
                modelRef::set,
                () -> Usage.EMPTY,
                u -> 0.0,
                "/tmp",
                false
            );
            registry.register(new ModelCommand());
            CommandResult result = registry.dispatch("/model new-model", ctx);
            assertTrue(result.output().contains("Model changed to: new-model"));
            assertEquals("new-model", modelRef.get());
        }

        @Test
        void costShowsUsage() {
            Usage usage = new Usage(1000, 500, 0, 0);
            CommandContext ctx = new CommandContext(
                "test-model",
                List::of,
                () -> {},
                m -> {},
                () -> usage,
                u -> 0.018,
                "/tmp",
                false
            );
            registry.register(new CostCommand());
            CommandResult result = registry.dispatch("/cost", ctx);
            assertTrue(result.output().contains("1000 input"));
            assertTrue(result.output().contains("500 output"));
            assertTrue(result.output().contains("$0.0180"));
        }

        @Test
        void compactWithNoMessages() {
            registry.register(new CompactCommand());
            CommandResult result = registry.dispatch("/compact", context);
            assertTrue(result.output().contains("No messages to compact"));
        }

        @Test
        void configShowsInfo() {
            registry.register(new ConfigCommand());
            CommandResult result = registry.dispatch("/config", context);
            assertTrue(result.output().contains("Model:"));
            assertTrue(result.output().contains("Working directory:"));
        }
    }

    // ---- Availability Checks ----

    @Nested
    class AvailabilityChecks {

        @Test
        void allDefaultCommandsAreAvailable() {
            CommandRegistry reg = CommandFactory.createDefault();
            List<Command> available = reg.getAvailable(context);
            assertFalse(available.isEmpty());
            // All default commands should be available in a normal context
            assertEquals(reg.getAll().size(), available.size());
        }

        @Test
        void unavailableCommandFilteredFromGetAvailable() {
            Command unavailable = new Command() {
                @Override public String name() { return "hidden"; }
                @Override public String description() { return "Hidden"; }
                @Override public CommandResult execute(CommandContext ctx, String args) {
                    return CommandResult.of("hidden");
                }
                @Override public boolean isAvailable(CommandContext ctx) { return false; }
            };
            registry.register(new ExitCommand());
            registry.register(unavailable);
            List<Command> available = registry.getAvailable(context);
            assertEquals(1, available.size());
            assertEquals("exit", available.get(0).name());
        }

        @Test
        void remoteContextFiltersBridgeUnsafeCommands() {
            CommandContext remoteCtx = new CommandContext(
                "test-model", List::of, () -> {}, m -> {},
                () -> Usage.EMPTY, u -> 0.0, "/tmp", true
            );
            Command bridgeUnsafe = new Command() {
                @Override public String name() { return "local-only"; }
                @Override public String description() { return "Local only"; }
                @Override public CommandResult execute(CommandContext ctx, String args) {
                    return CommandResult.of("local");
                }
                @Override public boolean isAvailable(CommandContext ctx) {
                    return !ctx.remoteMode() || isBridgeSafe();
                }
                @Override public boolean isBridgeSafe() { return false; }
            };
            registry.register(bridgeUnsafe);
            registry.register(new ExitCommand()); // bridge-safe

            List<Command> available = registry.getAvailable(remoteCtx);
            assertEquals(1, available.size());
            assertEquals("exit", available.get(0).name());
        }
    }

    // ---- Factory ----

    @Nested
    class Factory {

        @Test
        void defaultRegistryHasAllExpectedCommands() {
            CommandRegistry reg = CommandFactory.createDefault();
            List<Command> all = reg.getAll();
            assertTrue(all.size() >= 18, "Expected at least 18 commands, got " + all.size());

            // P0
            assertTrue(reg.find("help").isPresent());
            assertTrue(reg.find("exit").isPresent());
            assertTrue(reg.find("clear").isPresent());
            assertTrue(reg.find("compact").isPresent());
            assertTrue(reg.find("config").isPresent());
            assertTrue(reg.find("model").isPresent());
            assertTrue(reg.find("cost").isPresent());

            // P1 stubs
            assertTrue(reg.find("commit").isPresent());
            assertTrue(reg.find("diff").isPresent());
            assertTrue(reg.find("review").isPresent());
            assertTrue(reg.find("resume").isPresent());
            assertTrue(reg.find("share").isPresent());
            assertTrue(reg.find("export").isPresent());
            assertTrue(reg.find("memory").isPresent());
            assertTrue(reg.find("doctor").isPresent());
            assertTrue(reg.find("permissions").isPresent());
            assertTrue(reg.find("status").isPresent());
        }

        @Test
        void aliasesWorkInDefaultRegistry() {
            CommandRegistry reg = CommandFactory.createDefault();
            // "quit" is alias for "exit"
            assertTrue(reg.find("quit").isPresent());
            assertEquals("exit", reg.find("quit").get().name());
            // "?" is alias for "help"
            assertTrue(reg.find("?").isPresent());
            assertEquals("help", reg.find("?").get().name());
        }
    }
}
