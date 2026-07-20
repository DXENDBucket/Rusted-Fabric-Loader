package io.github.endx.rustedfabricapi.api.chat.command;

import rustedwarfare.game.Team;
import rustedwarfare.network.NetworkConnection;
import rustedwarfare.network.NetworkEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Namespaced server-side commands usable by Loader and vanilla clients alike. */
public final class ChatCommands {
    private static final ConcurrentMap<String, ChatCommandHandler> HANDLERS =
            new ConcurrentHashMap<String, ChatCommandHandler>();

    private ChatCommands() {
    }

    public static Registration register(String name, ChatCommandHandler handler) {
        String key = validateName(name);
        ChatCommandHandler checked = Objects.requireNonNull(handler, "handler");
        ChatCommandHandler previous = HANDLERS.putIfAbsent(key, checked);
        if (previous != null) throw new IllegalStateException("Chat command is already registered: " + key);
        return new Registration(key, checked);
    }

    public static Optional<ChatCommandHandler> find(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(HANDLERS.get(name.trim().toLowerCase(Locale.ROOT)));
    }

    public static List<String> registeredNames() {
        ArrayList<String> result = new ArrayList<String>(HANDLERS.keySet());
        Collections.sort(result);
        return Collections.unmodifiableList(result);
    }

    /** Internal runtime hook. Returns true when a registered command consumed the message. */
    public static boolean dispatch(NetworkEngine network, NetworkConnection connection, Team team,
            String senderName, String message) {
        if (network == null || !network.isServer) return false;
        ParsedCommand parsed = parse(message);
        if (parsed == null) return false;
        ChatCommandHandler handler = HANDLERS.get(parsed.name);
        if (handler == null) return false;
        ChatCommandContext context = new ChatCommandContext(
                network, connection, team, senderName, message, parsed);
        if (ChatCommandEvents.BEFORE_EXECUTE.invoker().beforeExecute(context)) return true;
        int result = 0;
        RuntimeException failure = null;
        try {
            result = handler.execute(context);
        } catch (RuntimeException exception) {
            failure = exception;
            String detail = exception.getMessage();
            try {
                context.error("[Command failed" + (detail != null && !detail.isEmpty() ? ": " + detail : "") + "]");
            } catch (RuntimeException ignored) {
                // A failed error reply must not tear down the native server command path.
            }
        } finally {
            ChatCommandEvents.AFTER_EXECUTE.invoker().afterExecute(context, result, failure);
        }
        return true;
    }

    /** Internal broadcast filter used before the native command handler runs. */
    public static boolean isRegisteredMessage(String message) {
        ParsedCommand parsed = parse(message);
        return parsed != null && HANDLERS.containsKey(parsed.name);
    }

    static ParsedCommand parse(String message) {
        if (message == null) return null;
        String text = message.trim();
        if (text.length() < 2 || (text.charAt(0) != '-' && text.charAt(0) != '.'
                && text.charAt(0) != '_')) return null;
        String body = text.substring(1).trim();
        if (body.isEmpty()) return null;
        int split = 0;
        while (split < body.length() && !Character.isWhitespace(body.charAt(split))) split++;
        String name = body.substring(0, split).toLowerCase(Locale.ROOT);
        String rawArguments = split < body.length() ? body.substring(split).trim() : "";
        return new ParsedCommand(name, rawArguments, tokenize(rawArguments));
    }

    static List<String> tokenize(String rawArguments) {
        ArrayList<String> result = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        boolean escaped = false;
        boolean started = false;
        for (int i = 0; i < rawArguments.length(); i++) {
            char c = rawArguments.charAt(i);
            if (escaped) {
                current.append(c);
                escaped = false;
                started = true;
            } else if (c == '\\') {
                escaped = true;
                started = true;
            } else if (quote != 0) {
                if (c == quote) quote = 0;
                else current.append(c);
                started = true;
            } else if (c == '\'' || c == '"') {
                quote = c;
                started = true;
            } else if (Character.isWhitespace(c)) {
                if (started) {
                    result.add(current.toString());
                    current.setLength(0);
                    started = false;
                }
            } else {
                current.append(c);
                started = true;
            }
        }
        if (escaped) current.append('\\');
        if (started) result.add(current.toString());
        return result;
    }

    private static String validateName(String name) {
        if (name == null) throw new NullPointerException("name");
        String value = name.trim().toLowerCase(Locale.ROOT);
        int separator = value.indexOf(':');
        if (separator < 2 || separator != value.lastIndexOf(':') || separator == value.length() - 1
                || value.length() > 96) {
            throw new IllegalArgumentException("Command must use namespace:path form");
        }
        for (int i = 0; i < separator; i++) {
            char c = value.charAt(i);
            boolean valid = c >= 'a' && c <= 'z' || c >= '0' && c <= '9'
                    || c == '_' || c == '-' || c == '.';
            if (!valid) throw new IllegalArgumentException("Invalid command character: " + c);
        }
        String path = value.substring(separator + 1);
        if (path.startsWith("/") || path.endsWith("/") || path.contains("//")) {
            throw new IllegalArgumentException("Invalid command path: " + path);
        }
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            boolean valid = c >= 'a' && c <= 'z' || c >= '0' && c <= '9'
                    || c == '_' || c == '-' || c == '.' || c == '/';
            if (!valid) throw new IllegalArgumentException("Invalid command character: " + c);
        }
        return value;
    }

    static final class ParsedCommand {
        final String name;
        final String rawArguments;
        final List<String> arguments;

        ParsedCommand(String name, String rawArguments, List<String> arguments) {
            this.name = name;
            this.rawArguments = rawArguments;
            this.arguments = new ArrayList<String>(arguments);
        }
    }

    public static final class Registration implements AutoCloseable {
        private final String name;
        private final ChatCommandHandler handler;
        private boolean active = true;

        Registration(String name, ChatCommandHandler handler) {
            this.name = name;
            this.handler = handler;
        }

        public String name() { return name; }

        public synchronized boolean unregister() {
            if (!active) return false;
            active = false;
            return HANDLERS.remove(name, handler);
        }

        @Override
        public void close() { unregister(); }
    }
}
