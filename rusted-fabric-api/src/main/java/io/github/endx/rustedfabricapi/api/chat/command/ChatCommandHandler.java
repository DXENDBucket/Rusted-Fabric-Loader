package io.github.endx.rustedfabricapi.api.chat.command;

@FunctionalInterface
public interface ChatCommandHandler {
    /** Executes on the server network/game thread. The integer result is exposed to observers. */
    int execute(ChatCommandContext context);
}
