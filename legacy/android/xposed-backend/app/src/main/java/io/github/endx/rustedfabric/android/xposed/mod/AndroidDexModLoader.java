package io.github.endx.rustedfabric.android.xposed.mod;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Objects;

import io.github.endx.rustedfabric.android.mod.DelegatingModParentClassLoader;
import io.github.endx.rustedfabric.android.mod.ModVerificationException;
import io.github.endx.rustedfabric.android.mod.RustedFabricModMetadata;
import io.github.endx.rustedfabric.android.mod.RustedFabricModVerifier;
import io.github.endx.rustedfabric.android.mod.VerifiedModArchive;
import io.github.endx.rustedfabricapi.api.RustedFabricAPIContext;
import io.github.endx.rustedfabricapi.api.RustedFabricModEntrypoint;

/** Verifies and initializes one app-private, code-only Android mod archive. */
public final class AndroidDexModLoader {
    public static final String SUPPORTED_API_VERSION = "0.1";

    private final RustedFabricModVerifier verifier;

    public AndroidDexModLoader() {
        this(new RustedFabricModVerifier());
    }

    AndroidDexModLoader(RustedFabricModVerifier verifier) {
        this.verifier = Objects.requireNonNull(verifier, "verifier");
    }

    public LoadedAndroidMod load(File installedArchive, File optimizedDirectory,
                                 RustedFabricAPIContext context, ClassLoader gameClassLoader)
            throws AndroidModLoadException {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(gameClassLoader, "gameClassLoader");
        VerifiedModArchive verified = verify(installedArchive);
        return loadVerified(verified, optimizedDirectory, context, gameClassLoader);
    }

    public LoadedAndroidMod loadVerified(VerifiedModArchive verified, File optimizedDirectory,
                                         RustedFabricAPIContext context,
                                         ClassLoader gameClassLoader)
            throws AndroidModLoadException {
        Objects.requireNonNull(verified, "verified");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(gameClassLoader, "gameClassLoader");
        RustedFabricModMetadata metadata = verified.getMetadata();
        validateCompatibility(metadata, context);
        ensureCacheDirectory(optimizedDirectory);

        ClassLoader apiClassLoader = RustedFabricAPIContext.class.getClassLoader();
        if (apiClassLoader == null) {
            throw new AndroidModLoadException(AndroidModLoadException.Reason.ENTRYPOINT_INVALID,
                    "Common API ClassLoader is unavailable");
        }
        DelegatingModParentClassLoader parent = new DelegatingModParentClassLoader(
                apiClassLoader, gameClassLoader);
        AndroidModDexClassLoader modClassLoader = new AndroidModDexClassLoader(
                verified.getArchivePath().toString(), optimizedDirectory.getAbsolutePath(), parent,
                verified.getDefinedClasses());
        RustedFabricModEntrypoint entrypoint = instantiateEntrypoint(
                metadata.getEntrypoint(), modClassLoader);
        try {
            entrypoint.onInitialize(context);
        } catch (ThreadDeath | VirtualMachineError critical) {
            throw critical;
        } catch (Throwable modFailure) {
            throw new AndroidModLoadException(AndroidModLoadException.Reason.ENTRYPOINT_FAILED,
                    "Mod entrypoint failed: " + metadata.getId(), modFailure);
        }
        return new LoadedAndroidMod(metadata, verified.getArchiveSha256(),
                verified.getDexSha256(), entrypoint, modClassLoader);
    }

    private VerifiedModArchive verify(File installedArchive) throws AndroidModLoadException {
        if (installedArchive == null) {
            throw new AndroidModLoadException(AndroidModLoadException.Reason.VERIFICATION_FAILED,
                    "Installed mod archive is missing");
        }
        try {
            return verifier.verify(installedArchive.toPath());
        } catch (ModVerificationException failure) {
            throw new AndroidModLoadException(AndroidModLoadException.Reason.VERIFICATION_FAILED,
                    "Mod verification failed: " + failure.getReason(), failure);
        }
    }

    private static void validateCompatibility(RustedFabricModMetadata metadata,
                                              RustedFabricAPIContext context)
            throws AndroidModLoadException {
        if (!SUPPORTED_API_VERSION.equals(metadata.getApiVersion())) {
            throw new AndroidModLoadException(AndroidModLoadException.Reason.API_VERSION_MISMATCH,
                    "Mod requires unsupported API version: " + metadata.getApiVersion());
        }
        if (!metadata.supportsMappingProfile(context.mappingProfileId())) {
            throw new AndroidModLoadException(
                    AndroidModLoadException.Reason.MAPPING_PROFILE_MISMATCH,
                    "Mod does not support the active mapping profile");
        }
        for (String capability : metadata.getCapabilities()) {
            if (!context.hasCapability(capability)) {
                throw new AndroidModLoadException(AndroidModLoadException.Reason.CAPABILITY_MISSING,
                        "Required runtime capability is unavailable: " + capability);
            }
        }
    }

    private static void ensureCacheDirectory(File directory) throws AndroidModLoadException {
        if (directory == null || (!directory.isDirectory() && !directory.mkdirs())
                || !directory.canWrite()) {
            throw new AndroidModLoadException(
                    AndroidModLoadException.Reason.CACHE_DIRECTORY_INVALID,
                    "Private DEX cache directory is unavailable");
        }
    }

    private static RustedFabricModEntrypoint instantiateEntrypoint(
            String binaryName, AndroidModDexClassLoader classLoader)
            throws AndroidModLoadException {
        try {
            Class<?> entrypointClass = classLoader.loadClass(binaryName);
            if (entrypointClass.getClassLoader() != classLoader
                    || !RustedFabricModEntrypoint.class.isAssignableFrom(entrypointClass)
                    || !Modifier.isPublic(entrypointClass.getModifiers())
                    || Modifier.isAbstract(entrypointClass.getModifiers())) {
                throw new AndroidModLoadException(AndroidModLoadException.Reason.ENTRYPOINT_INVALID,
                        "Mod entrypoint must be a public concrete RustedFabricModEntrypoint");
            }
            Constructor<?> constructor = entrypointClass.getConstructor();
            Object instance = constructor.newInstance();
            return (RustedFabricModEntrypoint) instance;
        } catch (AndroidModLoadException expected) {
            throw expected;
        } catch (ReflectiveOperationException | LinkageError failure) {
            throw new AndroidModLoadException(AndroidModLoadException.Reason.ENTRYPOINT_INVALID,
                    "Mod entrypoint cannot be loaded", failure);
        }
    }
}
