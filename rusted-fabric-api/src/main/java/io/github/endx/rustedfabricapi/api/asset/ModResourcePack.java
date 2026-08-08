package io.github.endx.rustedfabricapi.api.asset;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** A mod-owned, read-only resource source backed by a Fabric container or class loader. */
public final class ModResourcePack {
    private final String modId;
    private final Source source;
    private final Finder finder;

    ModResourcePack(String modId, Source source, Finder finder) {
        this.modId = modId;
        this.source = source;
        this.finder = finder;
    }

    public String modId() { return modId; }

    public ModResource resource(String relativePath) {
        return new ModResource(this, ModResources.validatePath(relativePath));
    }

    /** True when this pack can enumerate resources rather than only opening known paths. */
    public boolean supportsDiscovery() { return finder != null; }

    /** Returns a deterministic path-sorted snapshot below the given relative prefix. */
    public List<ModResource> find(String prefix) throws IOException {
        return find(prefix, path -> true);
    }

    /** The predicate receives each full Jar-relative path with forward slashes. */
    public List<ModResource> find(String prefix, Predicate<String> predicate) throws IOException {
        if (finder == null) {
            throw new UnsupportedOperationException(
                    "Resource discovery is unavailable for this class-loader-backed pack");
        }
        Path checkedPrefix = ModResources.validatePath(prefix);
        Predicate<String> checkedPredicate = java.util.Objects.requireNonNull(predicate, "predicate");
        ArrayList<ModResource> result = new ArrayList<ModResource>();
        for (Path path : finder.find(checkedPrefix)) {
            Path checkedPath = ModResources.validatePath(path.toString().replace('\\', '/'));
            if (!checkedPath.startsWith(checkedPrefix)) {
                throw new IOException("Discovered resource escaped requested prefix: " + checkedPath);
            }
            String name = checkedPath.toString().replace('\\', '/');
            if (checkedPredicate.test(name)) result.add(new ModResource(this, checkedPath));
        }
        Collections.sort(result, (left, right) -> left.relativePath().toString()
                .compareTo(right.relativePath().toString()));
        return Collections.unmodifiableList(result);
    }

    Optional<InputStream> open(Path path) throws IOException {
        return source.open(path);
    }

    @Override
    public String toString() { return "ModResourcePack{" + modId + '}'; }

    @FunctionalInterface
    interface Source {
        Optional<InputStream> open(Path path) throws IOException;
    }

    @FunctionalInterface
    interface Finder {
        List<Path> find(Path prefix) throws IOException;
    }
}
