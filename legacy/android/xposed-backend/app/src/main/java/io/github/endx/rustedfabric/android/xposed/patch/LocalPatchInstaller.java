package io.github.endx.rustedfabric.android.xposed.patch;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.os.Build;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import io.github.endx.rustedfabric.android.patcher.PatchProfile;

/** Streams a completed local patch into Android's user-confirmed package installer. */
final class LocalPatchInstaller {
    private LocalPatchInstaller() {
    }

    static int enqueue(Context context, Path apk) throws Exception {
        PackageInstaller installer = context.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(PatchProfile.DEFAULT_CLONE_PACKAGE);
        params.setSize(Files.size(apk));
        int sessionId = installer.createSession(params);
        try (PackageInstaller.Session session = installer.openSession(sessionId)) {
            try (InputStream input = Files.newInputStream(apk);
                 OutputStream output = session.openWrite("base.apk", 0, Files.size(apk))) {
                byte[] buffer = new byte[64 * 1024];
                for (int count = input.read(buffer); count >= 0; count = input.read(buffer)) {
                    if (count > 0) output.write(buffer, 0, count);
                }
                session.fsync(output);
            }
            Intent callback = new Intent(context, PatchInstallReceiver.class)
                    .setAction(PatchInstallReceiver.ACTION_STATUS)
                    .putExtra(PackageInstaller.EXTRA_SESSION_ID, sessionId);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 31) flags |= PendingIntent.FLAG_MUTABLE;
            IntentSender statusReceiver = PendingIntent.getBroadcast(
                    context, sessionId, callback, flags).getIntentSender();
            session.commit(statusReceiver);
            return sessionId;
        } catch (Exception failure) {
            installer.abandonSession(sessionId);
            throw failure;
        }
    }
}
