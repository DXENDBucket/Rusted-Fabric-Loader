package io.github.endx.rustedfabric.android.xposed.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.endx.rustedfabric.android.jvm.DesktopGameInspection;
import io.github.endx.rustedfabric.android.jvm.DesktopGameLayout;
import io.github.endx.rustedfabric.android.jvm.JvmBackendCapabilities;
import io.github.endx.rustedfabric.android.xposed.R;
import io.github.endx.rustedfabric.android.xposed.jvm.DesktopGameImportService;

/** Experimental FCL-style backend setup. Native JVM execution is intentionally fail-closed. */
public final class JvmLauncherActivity extends Activity {
    private static final int REQUEST_GAME_TREE = 2001;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView gameStatus;
    private TextView runtimeStatus;
    private TextView operationStatus;
    private ProgressBar progress;
    private Button importButton;
    private Button launchButton;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(createContent());
        refresh();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_GAME_TREE && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            Uri tree = data.getData();
            if ((data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
                try {
                    getContentResolver().takePersistableUriPermission(tree,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {
                    // The import happens immediately; persisted access is only an optimization.
                }
            }
            importDesktopGame(tree);
        }
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }

    private View createContent() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        content.setPadding(padding, padding, padding, padding);
        scroll.addView(content);

        TextView title = text(getString(R.string.jvm_title), 24);
        content.addView(title);
        TextView explanation = text(getString(R.string.jvm_boundary), 14);
        explanation.setPadding(0, dp(8), 0, dp(16));
        content.addView(explanation);

        gameStatus = text(getString(R.string.jvm_game_checking), 16);
        content.addView(gameStatus);
        runtimeStatus = text(getString(R.string.jvm_runtime_checking), 14);
        runtimeStatus.setPadding(0, dp(8), 0, dp(8));
        content.addView(runtimeStatus);
        operationStatus = text("", 13);
        content.addView(operationStatus);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        progress.setVisibility(View.GONE);
        content.addView(progress, matchWidth());

        importButton = new Button(this);
        importButton.setText(R.string.jvm_import_game);
        importButton.setOnClickListener(ignored -> chooseDesktopGame());
        content.addView(importButton, matchWidth());

        launchButton = new Button(this);
        launchButton.setText(R.string.jvm_launch_game);
        launchButton.setEnabled(false);
        launchButton.setOnClickListener(ignored -> Toast.makeText(this,
                R.string.jvm_runtime_not_ready, Toast.LENGTH_LONG).show());
        content.addView(launchButton, matchWidth());
        return scroll;
    }

    private void chooseDesktopGame() {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(picker, REQUEST_GAME_TREE);
    }

    private void importDesktopGame(Uri tree) {
        setBusy(true, getString(R.string.jvm_import_starting));
        worker.execute(() -> {
            try {
                DesktopGameImportService.Result result = DesktopGameImportService.importTree(
                        this, tree, (files, bytes, current) -> runOnUiThread(() ->
                                operationStatus.setText(getString(R.string.jvm_import_progress,
                                        files, Formatter.formatFileSize(this, bytes), current))));
                runOnUiThread(() -> {
                    setBusy(false, getString(R.string.jvm_import_complete, result.files(),
                            Formatter.formatFileSize(this, result.bytes())));
                    refresh();
                });
            } catch (Exception failure) {
                runOnUiThread(() -> {
                    setBusy(false, getString(R.string.jvm_import_failed,
                            safeMessage(failure)));
                    Toast.makeText(this, operationStatus.getText(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void refresh() {
        File root = DesktopGameImportService.importedRoot(this);
        DesktopGameInspection inspection = DesktopGameLayout.inspect(root.toPath());
        gameStatus.setText(inspection.isImportable()
                ? getString(R.string.jvm_game_ready)
                : getString(R.string.jvm_game_missing));
        JvmBackendCapabilities capabilities = JvmBackendCapabilities.unavailable();
        runtimeStatus.setText(getString(R.string.jvm_runtime_missing,
                android.text.TextUtils.join(", ", capabilities.missing())));
        launchButton.setEnabled(inspection.isImportable() && capabilities.isLaunchReady());
    }

    private void setBusy(boolean busy, String message) {
        operationStatus.setText(message);
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        importButton.setEnabled(!busy);
    }

    private TextView text(String value, int sp) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        return view;
    }

    private LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.trim().isEmpty()
                ? failure.getClass().getSimpleName() : message;
    }
}
