package io.github.endx.rustedfabric.android.xposed.storage;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;

import io.github.endx.rustedfabric.android.bootstrap.AndroidMappingProfile;
import io.github.endx.rustedfabric.android.bootstrap.Sha256;

public final class InstalledGameVerifier {
    private static final Object CACHE_LOCK = new Object();
    private static Cache cached;

    private InstalledGameVerifier() {
    }

    @SuppressWarnings("deprecation")
    public static Result verify(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    AndroidMappingProfile.PACKAGE_NAME, 0);
            long versionCode = Build.VERSION.SDK_INT >= 28
                    ? info.getLongVersionCode() : info.versionCode;
            String sourceDir = info.applicationInfo == null ? null : info.applicationInfo.sourceDir;
            if (sourceDir == null || sourceDir.isEmpty()) {
                return new Result(false, "APK_HASH_UNAVAILABLE");
            }
            File apk = new File(sourceDir);
            synchronized (CACHE_LOCK) {
                if (cached != null && cached.lastUpdateTime == info.lastUpdateTime
                        && cached.apkLength == apk.length()
                        && cached.apkLastModified == apk.lastModified()) {
                    return cached.result;
                }
                String hash;
                try (FileInputStream input = new FileInputStream(apk)) {
                    hash = Sha256.digest(input);
                }
                AndroidMappingProfile.Selection selection = AndroidMappingProfile.select(
                        info.packageName, info.versionName, versionCode, hash);
                Result result = new Result(selection.isVerified(), selection.diagnosticStatus());
                cached = new Cache(info.lastUpdateTime, apk.length(), apk.lastModified(), result);
                return result;
            }
        } catch (PackageManager.NameNotFoundException missing) {
            return new Result(false, "GAME_NOT_INSTALLED");
        } catch (IOException unreadable) {
            return new Result(false, "APK_HASH_UNAVAILABLE");
        }
    }

    private static final class Cache {
        private final long lastUpdateTime;
        private final long apkLength;
        private final long apkLastModified;
        private final Result result;

        private Cache(long lastUpdateTime, long apkLength, long apkLastModified, Result result) {
            this.lastUpdateTime = lastUpdateTime;
            this.apkLength = apkLength;
            this.apkLastModified = apkLastModified;
            this.result = result;
        }
    }

    public static final class Result {
        private final boolean verified;
        private final String status;

        private Result(boolean verified, String status) {
            this.verified = verified;
            this.status = status;
        }

        public boolean isVerified() {
            return verified;
        }

        public String getStatus() {
            return status;
        }
    }
}
