package io.github.endx.rustedfabric.android.xposed.patch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.widget.Toast;

import io.github.endx.rustedfabric.android.xposed.R;

/** Handles the final system confirmation/result without requiring the manager to stay open. */
public final class PatchInstallReceiver extends BroadcastReceiver {
    public static final String PREFS = "local_patch_status";
    public static final String PREF_STATUS = "status";
    public static final String PREF_DETAIL = "detail";
    public static final String PREF_UNREAD = "unread";
    static final String ACTION_STATUS =
            "io.github.endx.rustedfabric.android.xposed.LOCAL_PATCH_INSTALL_STATUS";

    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS,
                PackageInstaller.STATUS_FAILURE);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirmation;
            if (Build.VERSION.SDK_INT >= 33) {
                confirmation = intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent.class);
            } else {
                //noinspection deprecation
                confirmation = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            }
            if (confirmation != null) {
                confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(confirmation);
            }
            return;
        }
        int message = status == PackageInstaller.STATUS_SUCCESS
                ? R.string.patch_install_succeeded : R.string.patch_install_failed;
        String detail = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt(PREF_STATUS, status)
                .putString(PREF_DETAIL, detail == null ? "" : detail)
                .putBoolean(PREF_UNREAD, true)
                .apply();
        Toast.makeText(context, status == PackageInstaller.STATUS_SUCCESS
                        ? context.getString(message)
                        : context.getString(message, detail == null ? "unknown" : detail),
                Toast.LENGTH_LONG).show();
    }
}
