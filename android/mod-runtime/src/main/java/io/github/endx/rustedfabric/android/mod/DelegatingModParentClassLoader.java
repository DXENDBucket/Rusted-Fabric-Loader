package io.github.endx.rustedfabric.android.mod;

import java.util.Objects;

/** Routes the stable common API to the module and every other parent lookup to the game loader. */
public final class DelegatingModParentClassLoader extends ClassLoader {
    public static final String COMMON_API_PREFIX = "io.github.endx.rustedfabricapi.api.";

    private final ClassLoader commonApiLoader;
    private final ClassLoader gameClassLoader;

    public DelegatingModParentClassLoader(ClassLoader commonApiLoader, ClassLoader gameClassLoader) {
        super(null);
        this.commonApiLoader = Objects.requireNonNull(commonApiLoader, "commonApiLoader");
        this.gameClassLoader = Objects.requireNonNull(gameClassLoader, "gameClassLoader");
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.startsWith(COMMON_API_PREFIX)) {
            return commonApiLoader.loadClass(name);
        }
        if (name.startsWith("io.github.endx.rustedfabric.android.")
                || name.startsWith("io.github.libxposed.")) {
            throw new ClassNotFoundException("Loader implementation packages are not mod API");
        }
        return gameClassLoader.loadClass(name);
    }
}
