package io.github.endx.rustedfabric.android.xposed.mod;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import dalvik.system.DexClassLoader;

/** Child-first only for classes verified as definitions owned by this mod. */
final class AndroidModDexClassLoader extends DexClassLoader {
    private final Set<String> modClasses;

    AndroidModDexClassLoader(String dexPath, String optimizedDirectory, ClassLoader parent,
                             Set<String> modClasses) {
        super(dexPath, optimizedDirectory, null, parent);
        this.modClasses = Collections.unmodifiableSet(new LinkedHashSet<>(modClasses));
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!modClasses.contains(name)) {
            return super.loadClass(name, resolve);
        }
        // Android's public ClassLoader stubs do not expose getClassLoadingLock on every API level.
        synchronized (this) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                loaded = findClass(name);
            }
            if (resolve) {
                resolveClass(loaded);
            }
            return loaded;
        }
    }
}
