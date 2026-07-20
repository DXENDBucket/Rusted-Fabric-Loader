package io.github.endx.rustedfabricapi.api.chat.command;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;

/** Dependency-free command registry, parsing, and event checks. */
public final class ChatCommandContractVerification {
    private ChatCommandContractVerification() {
    }

    public static void verify() {
        ChatCommands.ParsedCommand parsed = ChatCommands.parse(
                ".contract:test alpha \"two words\" 'three' escaped\\ value \"\"");
        require(parsed != null && "contract:test".equals(parsed.name),
                "namespaced chat command was not parsed");
        require("alpha \"two words\" 'three' escaped\\ value \"\"".equals(parsed.rawArguments),
                "raw chat command arguments changed");
        require(parsed.arguments.equals(Arrays.asList(
                        "alpha", "two words", "three", "escaped value", "")),
                "quoted chat command arguments were tokenized incorrectly");

        ChatCommands.Registration registration = ChatCommands.register(
                "Contract:Status/Now", context -> 7);
        require("contract:status/now".equals(registration.name()),
                "registered command was not normalized");
        require(ChatCommands.find("CONTRACT:STATUS/NOW").isPresent(),
                "registered command could not be found case-insensitively");
        require(ChatCommands.isRegisteredMessage("_contract:status/now ignored"),
                "registered underscore-prefixed command was not recognized");
        try {
            ChatCommands.register("contract:status/now", context -> 0);
            throw new AssertionError("duplicate chat command was accepted");
        } catch (IllegalStateException expected) {
            // Expected.
        }
        try {
            ChatCommands.register("contract/bad:path", context -> 0);
            throw new AssertionError("invalid chat command namespace was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
        require(registration.unregister(), "chat command did not unregister");
        require(!registration.unregister(), "chat command unregister was not idempotent");
        require(!ChatCommands.isRegisteredMessage("-contract:status/now"),
                "unregistered command remained visible");

        AtomicInteger calls = new AtomicInteger();
        RustedFabricEvent.Registration first = ChatCommandEvents.BEFORE_EXECUTE.subscribe(context -> {
            calls.incrementAndGet();
            return false;
        });
        RustedFabricEvent.Registration second = ChatCommandEvents.BEFORE_EXECUTE.subscribe(context -> {
            calls.incrementAndGet();
            return true;
        });
        require(ChatCommandEvents.BEFORE_EXECUTE.invoker().beforeExecute(null),
                "chat command cancellation was not aggregated");
        require(calls.get() == 2, "chat command cancellation skipped a listener");
        first.close();
        second.close();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
