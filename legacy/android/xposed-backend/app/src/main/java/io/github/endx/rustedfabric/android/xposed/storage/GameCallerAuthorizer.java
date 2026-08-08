package io.github.endx.rustedfabric.android.xposed.storage;

import android.content.Context;
import android.content.pm.PackageManager;

import java.util.Arrays;

import io.github.endx.rustedfabric.android.bootstrap.AndroidMappingProfile;
import io.github.endx.rustedfabric.android.patcher.PatchProfile;

final class GameCallerAuthorizer {
    private GameCallerAuthorizer() {
    }

    static void enforce(Context context, int callingUid) {
        PackageManager packages = context.getPackageManager();
        String[] names = packages.getPackagesForUid(callingUid);
        if (names == null) {
            throw new SecurityException("Caller is not the supported game package");
        }
        if (Arrays.asList(names).contains(AndroidMappingProfile.PACKAGE_NAME)
                && InstalledGameVerifier.verify(context).isVerified()) {
            return;
        }
        if (Arrays.asList(names).contains(PatchProfile.DEFAULT_CLONE_PACKAGE)
                && InstalledPatchedGameVerifier.verify(context).isVerified()) {
            return;
        }
        throw new SecurityException("Calling game package or signing profile is not verified");
    }
}
