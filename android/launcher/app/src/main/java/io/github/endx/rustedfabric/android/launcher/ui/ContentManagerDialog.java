package io.github.endx.rustedfabric.android.launcher.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import io.github.endx.rustedfabric.android.jvm.ManagedContentLibrary;
import io.github.endx.rustedfabric.android.launcher.R;

/** Native Android content-management sheet backed by shared folders linked into the game root. */
final class ContentManagerDialog {
    interface Listener {
        void importContent(ManagedContentLibrary.Kind kind);
        void contentChanged();
    }

    private ContentManagerDialog() {
    }

    static void show(Activity activity, ExecutorService worker, File gameRoot,
                     ManagedContentLibrary.Kind kind, Listener listener) {
        Map<Path, Boolean> pendingEnabled = new LinkedHashMap<>();
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(activity, 16);
        content.setPadding(padding, dp(activity, 8), padding, dp(activity, 8));

        TextView description = new TextView(activity);
        description.setText(description(kind));
        description.setTextColor(activity.getColor(R.color.rf_text_secondary));
        description.setTextSize(13);
        description.setPadding(0, 0, 0, dp(activity, 10));
        content.addView(description);

        LinearLayout rows = new LinearLayout(activity);
        rows.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(rows);
        int listHeight = Math.min(dp(activity, 390),
                Math.round(activity.getResources().getDisplayMetrics().heightPixels * 0.55F));
        content.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, listHeight));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(title(kind))
                .setView(content)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.content_import, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        render(activity, worker, gameRoot, kind, listener, rows, dialog, pendingEnabled);
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button ->
                    applyChanges(activity, worker, gameRoot, kind, listener, rows, dialog,
                            pendingEnabled, null));
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(button ->
                    applyChanges(activity, worker, gameRoot, kind, listener, rows, dialog,
                            pendingEnabled, () -> listener.importContent(kind)));
        });
        dialog.show();
    }

    private static void render(Activity activity, ExecutorService worker, File gameRoot,
                               ManagedContentLibrary.Kind kind, Listener listener,
                               LinearLayout rows, AlertDialog dialog,
                               Map<Path, Boolean> pendingEnabled) {
        rows.removeAllViews();
        final List<ManagedContentLibrary.Item> items;
        try {
            items = ManagedContentLibrary.list(gameRoot.toPath(), kind);
        } catch (Exception failure) {
            TextView error = text(activity, safeMessage(failure), 14, R.color.rf_status_warning);
            rows.addView(error);
            return;
        }
        if (items.isEmpty()) {
            TextView empty = text(activity, activity.getString(R.string.content_empty),
                    14, R.color.rf_text_muted);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(activity, 36), 0, dp(activity, 36));
            rows.addView(empty);
            return;
        }
        for (ManagedContentLibrary.Item item : items) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, dp(activity, 10), 0, dp(activity, 10));

            TextView name = text(activity, item.name(), 16, R.color.rf_text_primary);
            name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            row.addView(name);

            String detail = item.detail();
            if (item.official()) detail = activity.getString(R.string.content_official) + " · " + detail;
            TextView details = text(activity, detail, 12, R.color.rf_text_secondary);
            details.setPadding(0, dp(activity, 2), 0, dp(activity, 6));
            row.addView(details);

            LinearLayout controls = new LinearLayout(activity);
            controls.setGravity(Gravity.CENTER_VERTICAL);
            controls.setOrientation(LinearLayout.HORIZONTAL);
            if (kind == ManagedContentLibrary.Kind.INI_MOD) {
                TextView state = text(activity, activity.getString(
                        R.string.content_ini_game_managed), 12, R.color.rf_text_muted);
                controls.addView(state, new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            } else {
                Switch enabled = new Switch(activity);
                boolean desired = pendingEnabled.containsKey(item.path())
                        ? pendingEnabled.get(item.path()) : item.enabled();
                enabled.setText(desired ? R.string.content_enabled : R.string.content_disabled);
                enabled.setChecked(desired);
                enabled.setOnCheckedChangeListener((button, checked) -> {
                    button.setText(checked ? R.string.content_enabled : R.string.content_disabled);
                    if (checked == item.enabled()) pendingEnabled.remove(item.path());
                    else pendingEnabled.put(item.path(), checked);
                });
                controls.addView(enabled, new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            }

            Button delete = new Button(activity);
            delete.setText(R.string.content_delete);
            delete.setTextSize(12);
            delete.setAllCaps(false);
            delete.setOnClickListener(ignored -> new AlertDialog.Builder(activity)
                    .setTitle(R.string.content_delete_title)
                    .setMessage(activity.getString(R.string.content_delete_confirm, item.name()))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.content_delete, (confirm, which) -> worker.execute(() -> {
                        try {
                            ManagedContentLibrary.delete(gameRoot.toPath(), item);
                            activity.runOnUiThread(() -> {
                                pendingEnabled.remove(item.path());
                                listener.contentChanged();
                                render(activity, worker, gameRoot, kind, listener, rows, dialog,
                                        pendingEnabled);
                            });
                        } catch (Exception failure) {
                            activity.runOnUiThread(() -> Toast.makeText(activity,
                                    safeMessage(failure), Toast.LENGTH_LONG).show());
                        }
                    })).show());
            controls.addView(delete);
            row.addView(controls);
            rows.addView(row);

            View divider = new View(activity);
            divider.setBackgroundColor(activity.getColor(R.color.rf_divider));
            rows.addView(divider, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1));
        }
    }

    private static void applyChanges(Activity activity, ExecutorService worker, File gameRoot,
                                     ManagedContentLibrary.Kind kind, Listener listener,
                                     LinearLayout rows, AlertDialog dialog,
                                     Map<Path, Boolean> pendingEnabled, Runnable afterSave) {
        if (pendingEnabled.isEmpty()) {
            dialog.dismiss();
            if (afterSave != null) afterSave.run();
            return;
        }
        dialog.setCancelable(false);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(false);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
        worker.execute(() -> {
            try {
                for (ManagedContentLibrary.Item item : ManagedContentLibrary.list(
                        gameRoot.toPath(), kind)) {
                    Boolean desired = pendingEnabled.get(item.path());
                    if (desired != null && desired != item.enabled()) {
                        ManagedContentLibrary.setEnabled(gameRoot.toPath(), item, desired);
                    }
                }
                activity.runOnUiThread(() -> {
                    pendingEnabled.clear();
                    listener.contentChanged();
                    dialog.dismiss();
                    if (afterSave != null) afterSave.run();
                });
            } catch (Exception failure) {
                activity.runOnUiThread(() -> {
                    pendingEnabled.clear();
                    listener.contentChanged();
                    dialog.setCancelable(true);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(true);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
                    Toast.makeText(activity, safeMessage(failure), Toast.LENGTH_LONG).show();
                    render(activity, worker, gameRoot, kind, listener, rows, dialog,
                            pendingEnabled);
                });
            }
        });
    }

    private static TextView text(Activity activity, String value, int size, int color) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(activity.getColor(color));
        return view;
    }

    private static int title(ManagedContentLibrary.Kind kind) {
        switch (kind) {
            case INI_MOD: return R.string.content_ini_title;
            case MAP: return R.string.content_maps_title;
            case JAVA_MOD: return R.string.content_java_title;
            default: throw new IllegalArgumentException("Unknown content kind");
        }
    }

    private static int description(ManagedContentLibrary.Kind kind) {
        switch (kind) {
            case INI_MOD: return R.string.content_ini_description;
            case MAP: return R.string.content_maps_description;
            case JAVA_MOD: return R.string.content_java_description;
            default: throw new IllegalArgumentException("Unknown content kind");
        }
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.trim().isEmpty()
                ? failure.getClass().getSimpleName() : message;
    }
}
