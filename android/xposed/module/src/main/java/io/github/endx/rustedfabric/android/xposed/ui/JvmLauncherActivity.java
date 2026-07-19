package io.github.endx.rustedfabric.android.xposed.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import io.github.endx.rustedfabric.android.jvm.JvmRuntimeProbe;
import io.github.endx.rustedfabric.android.xposed.R;
import io.github.endx.rustedfabric.android.xposed.jvm.DesktopGameImportService;
import io.github.endx.rustedfabric.android.xposed.jvm.JvmHostService;
import io.github.endx.rustedfabric.android.xposed.jvm.JvmRuntimeImportService;

/** Experimental FCL-style backend setup. Native JVM execution is intentionally fail-closed. */
public final class JvmLauncherActivity extends Activity {
    private static final int REQUEST_GAME_ARCHIVE = 2001;
    private static final int REQUEST_GAME_TREE = 2002;
    private static final int REQUEST_JAVA_RUNTIME = 2003;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView gameStatus;
    private TextView runtimeStatus;
    private TextView operationStatus;
    private ProgressBar progress;
    private Button importButton;
    private Button directoryButton;
    private Button runtimeButton;
    private Button smokeButton;
    private Button launchButton;
    private boolean busy;
    private boolean smokeReady;
    private boolean receiverRegistered;
    private final BroadcastReceiver smokeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!JvmHostService.ACTION_RESULT.equals(intent.getAction())) return;
            boolean succeeded = intent.getBooleanExtra(JvmHostService.EXTRA_SUCCEEDED, false);
            String detail = intent.getStringExtra(JvmHostService.EXTRA_DETAIL);
            operationStatus.setText(getString(succeeded
                    ? R.string.jvm_smoke_succeeded : R.string.jvm_smoke_failed,
                    detail == null ? "" : detail));
            setBusy(false, operationStatus.getText().toString());
            refresh();
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(createContent());
        refresh();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null
                && (requestCode == REQUEST_GAME_ARCHIVE || requestCode == REQUEST_GAME_TREE
                || requestCode == REQUEST_JAVA_RUNTIME)) {
            Uri source = data.getData();
            if ((data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
                try {
                    getContentResolver().takePersistableUriPermission(source,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {
                    // The import happens immediately; persisted access is only an optimization.
                }
            }
            if (requestCode == REQUEST_GAME_ARCHIVE) importDesktopArchive(source);
            else if (requestCode == REQUEST_GAME_TREE) importDesktopTree(source);
            else importJavaRuntime(source);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(JvmHostService.ACTION_RESULT);
        registerReceiver(smokeReceiver, filter, RECEIVER_NOT_EXPORTED);
        receiverRegistered = true;
        String lastStatus = JvmHostService.lastStatus(this);
        if (!lastStatus.isEmpty()) {
            operationStatus.setText(getString(R.string.jvm_smoke_last, lastStatus));
        }
    }

    @Override
    protected void onStop() {
        if (receiverRegistered) {
            unregisterReceiver(smokeReceiver);
            receiverRegistered = false;
        }
        super.onStop();
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
        importButton.setText(R.string.jvm_import_archive);
        importButton.setOnClickListener(ignored -> chooseDesktopArchive());
        content.addView(importButton, matchWidth());

        directoryButton = new Button(this);
        directoryButton.setText(R.string.jvm_import_directory);
        directoryButton.setOnClickListener(ignored -> chooseDesktopDirectory());
        content.addView(directoryButton, matchWidth());

        runtimeButton = new Button(this);
        runtimeButton.setText(R.string.jvm_import_runtime);
        runtimeButton.setOnClickListener(ignored -> chooseJavaRuntime());
        content.addView(runtimeButton, matchWidth());

        smokeButton = new Button(this);
        smokeButton.setText(R.string.jvm_test_runtime);
        smokeButton.setEnabled(false);
        smokeButton.setOnClickListener(ignored -> testJavaRuntime());
        content.addView(smokeButton, matchWidth());

        launchButton = new Button(this);
        launchButton.setText(R.string.jvm_launch_game);
        launchButton.setEnabled(false);
        launchButton.setOnClickListener(ignored -> Toast.makeText(this,
                R.string.jvm_runtime_not_ready, Toast.LENGTH_LONG).show());
        content.addView(launchButton, matchWidth());
        return scroll;
    }

    private void chooseDesktopArchive() {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("application/zip");
        picker.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/zip", "application/x-zip-compressed", "application/octet-stream"
        });
        picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(picker, REQUEST_GAME_ARCHIVE);
    }

    private void chooseDesktopDirectory() {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(picker, REQUEST_GAME_TREE);
    }

    private void chooseJavaRuntime() {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("*/*");
        picker.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/zip", "application/x-zip-compressed", "application/x-xz",
                "application/x-tar", "application/octet-stream"
        });
        picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(picker, REQUEST_JAVA_RUNTIME);
    }

    private void importDesktopArchive(Uri archive) {
        importDesktopGame(archive, true);
    }

    private void importDesktopTree(Uri tree) {
        importDesktopGame(tree, false);
    }

    private void importDesktopGame(Uri source, boolean archive) {
        setBusy(true, getString(R.string.jvm_import_starting));
        worker.execute(() -> {
            try {
                DesktopGameImportService.ProgressListener progressListener =
                        (files, bytes, current) -> runOnUiThread(() -> operationStatus.setText(
                                getString(R.string.jvm_import_progress, files,
                                        Formatter.formatFileSize(this, bytes), current)));
                DesktopGameImportService.Result result = archive
                        ? DesktopGameImportService.importArchive(this, source, progressListener)
                        : DesktopGameImportService.importTree(this, source, progressListener);
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

    private void importJavaRuntime(Uri source) {
        setBusy(true, getString(R.string.jvm_runtime_import_starting));
        worker.execute(() -> {
            try {
                JvmRuntimeImportService.Result result = JvmRuntimeImportService.importArchive(
                        this, source, (files, bytes, current) -> runOnUiThread(() ->
                                operationStatus.setText(getString(R.string.jvm_import_progress,
                                        files, Formatter.formatFileSize(this, bytes), current))));
                runOnUiThread(() -> {
                    setBusy(false, getString(R.string.jvm_runtime_import_complete,
                            result.files(), Formatter.formatFileSize(this, result.bytes()),
                            result.archiveSha256().substring(0, 12)));
                    refresh();
                });
            } catch (Exception failure) {
                runOnUiThread(() -> {
                    setBusy(false, getString(R.string.jvm_runtime_import_failed,
                            safeMessage(failure)));
                    Toast.makeText(this, operationStatus.getText(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void testJavaRuntime() {
        setBusy(true, getString(R.string.jvm_smoke_starting));
        try {
            if (startService(new Intent(this, JvmHostService.class)) == null) {
                throw new IllegalStateException("Android did not start the JVM test process");
            }
        } catch (RuntimeException failure) {
            setBusy(false, getString(R.string.jvm_smoke_failed, safeMessage(failure)));
        }
    }

    private void refresh() {
        File root = DesktopGameImportService.importedRoot(this);
        DesktopGameInspection inspection = DesktopGameLayout.inspect(root.toPath());
        gameStatus.setText(inspection.isImportable()
                ? getString(R.string.jvm_game_ready)
                : getString(R.string.jvm_game_missing));
        File runtimeHome = new File(new File(getFilesDir(), "desktop-jvm"), "runtime");
        File nativeDirectory = new File(getApplicationInfo().nativeLibraryDir);
        JvmBackendCapabilities capabilities = JvmRuntimeProbe.inspect(
                runtimeHome.toPath(), nativeDirectory.toPath());
        String missing = android.text.TextUtils.join(", ", capabilities.missing());
        String runtimeIssue = JvmRuntimeProbe.runtimeIssue(runtimeHome.toPath());
        runtimeStatus.setText(runtimeIssue.isEmpty()
                ? getString(R.string.jvm_runtime_missing, missing)
                : getString(R.string.jvm_runtime_missing_detail, missing, runtimeIssue));
        smokeReady = capabilities.hasJava17() && capabilities.hasJvmHost();
        smokeButton.setEnabled(!busy && smokeReady);
        launchButton.setEnabled(inspection.isImportable() && capabilities.isLaunchReady());
    }

    private void setBusy(boolean busy, String message) {
        this.busy = busy;
        operationStatus.setText(message);
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        importButton.setEnabled(!busy);
        directoryButton.setEnabled(!busy);
        runtimeButton.setEnabled(!busy);
        smokeButton.setEnabled(!busy && smokeReady);
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
