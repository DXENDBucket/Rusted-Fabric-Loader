package io.github.endx.rustedfabricapi.api.asset.reload;

import io.github.endx.rustedfabricapi.api.development.DevelopmentWorkspaces;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Portable, game-thread polling used instead of platform-specific WatchService assumptions. */
final class DevelopmentWorkspaceReloadMonitor {
    private static final long POLL_NANOS = 750_000_000L;
    private static final long STABLE_NANOS = 500_000_000L;
    private static final int MAX_FILES = 8192;
    private static final int MAX_DEPTH = 32;
    private static final Map<String, State> STATES = new LinkedHashMap<String, State>();
    private static long nextPollNanos;

    private DevelopmentWorkspaceReloadMonitor() { }

    static synchronized void track(String modId) {
        if (!DevelopmentWorkspaces.automaticReloadEnabled()) return;
        State current = STATES.get(modId);
        if (current != null) {
            current.registrations++;
            return;
        }
        Optional<Path> root = DevelopmentWorkspaces.forMod(modId);
        if (!root.isPresent()) return;
        try {
            STATES.put(modId, new State(root.get(), signature(root.get())));
        } catch (IOException failure) {
            System.err.println("[Rusted Fabric API] Could not watch development workspace "
                    + modId + ": " + failure);
        }
    }

    static synchronized void untrack(String modId) {
        State state = STATES.get(modId);
        if (state == null) return;
        state.registrations--;
        if (state.registrations <= 0) STATES.remove(modId);
    }

    /** Returns true once one or more workspaces have remained changed across the debounce window. */
    static synchronized boolean poll(long nowNanos) {
        if (!DevelopmentWorkspaces.automaticReloadEnabled() || STATES.isEmpty()
                || nowNanos < nextPollNanos) return false;
        nextPollNanos = nowNanos + POLL_NANOS;
        boolean reload = false;
        for (Map.Entry<String, State> entry : STATES.entrySet()) {
            State state = entry.getValue();
            final long observed;
            try {
                observed = signature(state.root);
            } catch (IOException failure) {
                System.err.println("[Rusted Fabric API] Could not poll development workspace "
                        + entry.getKey() + ": " + failure);
                continue;
            }
            if (observed == state.appliedSignature) {
                state.pendingSignature = Long.MIN_VALUE;
                continue;
            }
            if (observed != state.pendingSignature) {
                state.pendingSignature = observed;
                state.pendingSinceNanos = nowNanos;
                continue;
            }
            if (nowNanos - state.pendingSinceNanos >= STABLE_NANOS) {
                state.appliedSignature = observed;
                state.pendingSignature = Long.MIN_VALUE;
                reload = true;
            }
        }
        return reload;
    }

    static synchronized void resetForTests() {
        STATES.clear();
        nextPollNanos = 0L;
    }

    private static long signature(Path root) throws IOException {
        ArrayList<Path> files = new ArrayList<Path>();
        Files.walkFileTree(root, Collections.emptySet(), MAX_DEPTH,
                new SimpleFileVisitor<Path>() {
                    @Override public FileVisitResult preVisitDirectory(
                            Path directory, BasicFileAttributes attributes) throws IOException {
                        if (!directory.equals(root)) {
                            String name = directory.getFileName().toString();
                            if (".git".equals(name) || ".gradle".equals(name)) {
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override public FileVisitResult visitFile(
                            Path file, BasicFileAttributes attributes) throws IOException {
                        if (included(root, file)) {
                            files.add(file);
                            if (files.size() > MAX_FILES) {
                                throw new IOException("workspace contains more than "
                                        + MAX_FILES + " reloadable files");
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
        if (files.size() > MAX_FILES) {
            throw new IOException("workspace contains more than " + MAX_FILES
                    + " reloadable files");
        }
        Collections.sort(files, Comparator.comparing(path ->
                root.relativize(path).toString().replace('\\', '/')));
        long hash = 0xcbf29ce484222325L;
        for (Path file : files) {
            String relative = root.relativize(file).toString().replace('\\', '/');
            BasicFileAttributes attributes = Files.readAttributes(file,
                    BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            hash = mix(hash, relative);
            hash = mix(hash, attributes.size());
            hash = mix(hash, attributes.lastModifiedTime().toMillis());
        }
        return hash;
    }

    private static boolean included(Path root, Path path) {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(path)) return false;
        Path relative = root.relativize(path);
        for (Path segment : relative) {
            String name = segment.toString();
            if (".git".equals(name) || ".gradle".equals(name)) return false;
        }
        String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        return !name.endsWith(".class") && !name.endsWith(".jar")
                && !name.endsWith(".java") && !name.endsWith(".kt")
                && !name.endsWith(".lock") && !name.endsWith(".tmp");
    }

    private static long mix(long hash, String value) {
        for (int index = 0; index < value.length(); index++) {
            hash ^= value.charAt(index);
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    private static long mix(long hash, long value) {
        for (int shift = 0; shift < 64; shift += 8) {
            hash ^= (value >>> shift) & 0xffL;
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    private static final class State {
        final Path root;
        long appliedSignature;
        long pendingSignature = Long.MIN_VALUE;
        long pendingSinceNanos;
        int registrations = 1;

        State(Path root, long appliedSignature) {
            this.root = root;
            this.appliedSignature = appliedSignature;
        }
    }
}
