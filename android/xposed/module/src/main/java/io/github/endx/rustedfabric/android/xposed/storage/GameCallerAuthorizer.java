package io.github.endx.rustedfabric.android.xposed.storage;

import android.content.Context;
import android.content.pm.PackageManager;

import java.util.Arrays;

import io.github.endx.rustedfabric.android.bootstrap.AndroidMappingProfile;

final class GameCallerAuthorizer {
    private GameCallerAuthorizer() {
    }

    static void enforce(Context context, int callingUid) {
        PackageManager packages = context.getPackageManager();
        String[] names = packages.getPackagesForUid(callingUid);
        if (names == null || !Arrays.asList(names).contains(AndroidMappingProfile.PACKAGE_NAME)) {
            throw new SecurityException("Caller is not the supported game package");
        }
        InstalledGameVerifier.Result result = InstalledGameVerifier.verify(context);
        if (!result.isVerified()) {
            throw new SecurityException("Installed game profile is not verified");
        }
    }
}
