package io.github.endx.rustedfabric.android.launcher.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.endx.rustedfabric.android.jvm.DesktopGameInspection;
import io.github.endx.rustedfabric.android.jvm.DesktopGameLayout;
import io.github.endx.rustedfabric.android.jvm.JvmBackendCapabilities;
import io.github.endx.rustedfabric.android.jvm.JvmRuntimeProbe;
import io.github.endx.rustedfabric.android.launcher.R;
import io.github.endx.rustedfabric.android.launcher.jvm.DesktopGameImportService;
import io.github.endx.rustedfabric.android.launcher.jvm.JvmHostService;
import io.github.endx.rustedfabric.android.launcher.jvm.JvmRuntimeImportService;
import io.github.endx.rustedfabric.android.launcher.jvm.NativeJvmHost;

/** User-facing launcher for the imported desktop game and its ARM64 JVM runtime. */
public final class JvmLauncherActivity extends Activity {
    private static final int REQUEST_GAME_ARCHIVE = 2001;
    private static final int REQUEST_GAME_TREE = 2002;
    private static final int REQUEST_JAVA_RUNTIME = 2003;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView readinessBadge;
    private TextView readinessMessage;
    private TextView gameStatus;
    private TextView runtimeStatus;
    private TextView operationStatus;
    private TextView diagnosticStatus;
    private ProgressBar progress;
    private Button importButton;
    private Button directoryButton;
    private Button runtimeButton;
    private Button smokeButton;
    private Button rendererButton;
    private Button launchButton;
    private Button advancedButton;
    private LinearLayout advancedPanel;
    private boolean busy;
    private boolean smokeReady;
    private boolean gameProbeReady;
    private boolean advancedVisible;
    private boolean receiverRegistered;

    private final BroadcastReceiver smokeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!JvmHostService.ACTION_RESULT.equals(intent.getAction())) return;
            boolean succeeded = intent.getBooleanExtra(JvmHostService.EXTRA_SUCCEEDED, false);
            String detail = intent.getStringExtra(JvmHostService.EXTRA_DETAIL);
            String message = getString(succeeded
                    ? R.string.jvm_smoke_succeeded : R.string.jvm_smoke_failed,
                    detail == null ? "" : detail);
            diagnosticStatus.setText(message);
            setBusy(false, message);
            refresh();
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_jvm_launcher);
        bindViews();
        bindActions();
        showAdvanced(false);
        refresh();
    }

    private void bindViews() {
        readinessBadge = findViewById(R.id.readiness_badge);
        readinessMessage = findViewById(R.id.readiness_message);
        gameStatus = findViewById(R.id.game_status);
        runtimeStatus = findViewById(R.id.runtime_status);
        operationStatus = findViewById(R.id.operation_status);
        diagnosticStatus = findViewById(R.id.diagnostic_status);
        progress = findViewById(R.id.operation_progress);
        importButton = findViewById(R.id.import_game_button);
        directoryButton = findViewById(R.id.import_directory_button);
        runtimeButton = findViewById(R.id.import_runtime_button);
        smokeButton = findViewById(R.id.test_runtime_button);
        rendererButton = findViewById(R.id.test_renderer_button);
        launchButton = findViewById(R.id.launch_button);
        advancedButton = findViewById(R.id.advanced_button);
        advancedPanel = findViewById(R.id.advanced_panel);
    }

    private void bindActions() {
        importButton.setOnClickListener(ignored -> chooseDesktopArchive());
        directoryButton.setOnClickListener(ignored -> chooseDesktopDirectory());
        runtimeButton.setOnClickListener(ignored -> chooseJavaRuntime());
        smokeButton.setOnClickListener(ignored -> testJavaRuntime());
        rendererButton.setOnClickListener(ignored ->
                startActivity(new Intent(this, JvmRenderActivity.class)));
        advancedButton.setOnClickListener(ignored -> showAdvanced(!advancedVisible));
        launchButton.setOnClickListener(ignored -> launchGame());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        if (requestCode != REQUEST_GAME_ARCHIVE && requestCode != REQUEST_GAME_TREE
                && requestCode != REQUEST_JAVA_RUNTIME) return;

        Uri source = data.getData();
        if ((data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
            try {
                getContentResolver().takePersistableUriPermission(source,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {
                // Import is immediate; persisted access is only an optimization.
            }
        }
        if (requestCode == REQUEST_GAME_ARCHIVE) importDesktopGame(source, true);
        else if (requestCode == REQUEST_GAME_TREE) importDesktopGame(source, false);
        else importJavaRuntime(source);
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(smokeReceiver, new IntentFilter(JvmHostService.ACTION_RESULT),
                RECEIVER_NOT_EXPORTED);
        receiverRegistered = true;
        refreshDiagnostics();
        refresh();
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

    private void launchGame() {
        if (!gameProbeReady) {
            Toast.makeText(this, R.string.jvm_setup_required, Toast.LENGTH_LONG).show();
            return;
        }
        Intent launch = new Intent(this, JvmRenderActivity.class);
        launch.putExtra(JvmRenderActivity.EXTRA_GAME_PROBE, true);
        startActivity(launch);
    }

    private void showAdvanced(boolean visible) {
        advancedVisible = visible;
        advancedPanel.setVisibility(visible ? View.VISIBLE : View.GONE);
        advancedButton.setText(visible
                ? R.string.jvm_hide_advanced : R.string.jvm_show_advanced);
    }

    private void refreshDiagnostics() {
        String runtimeResult = JvmHostService.lastStatus(this);
        String rendererResult = JvmRenderActivity.lastStatus(this);
        if (runtimeResult.isEmpty() && rendererResult.isEmpty()) {
            diagnosticStatus.setText(R.string.jvm_no_diagnostics);
        } else if (runtimeResult.isEmpty()) {
            diagnosticStatus.setText(getString(R.string.jvm_renderer_last, rendererResult));
        } else if (rendererResult.isEmpty()) {
            diagnosticStatus.setText(getString(R.string.jvm_smoke_last, runtimeResult));
        } else {
            diagnosticStatus.setText(getString(R.string.jvm_diagnostics_last,
                    runtimeResult, rendererResult));
        }
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

    private void importDesktopGame(Uri source, boolean archive) {
        setBusy(true, getString(R.string.jvm_import_starting));
        worker.execute(() -> {
            try {
                DesktopGameImportService.ProgressListener listener =
                        (files, bytes, current) -> runOnUiThread(() -> showOperation(
                                getString(R.string.jvm_import_progress, files,
                                        Formatter.formatFileSize(this, bytes), current)));
                DesktopGameImportService.Result result = archive
                        ? DesktopGameImportService.importArchive(this, source, listener)
                        : DesktopGameImportService.importTree(this, source, listener);
                runOnUiThread(() -> {
                    setBusy(false, getString(R.string.jvm_import_complete, result.files(),
                            Formatter.formatFileSize(this, result.bytes())));
                    refresh();
                });
            } catch (Exception failure) {
                runOnUiThread(() -> showFailure(R.string.jvm_import_failed, failure));
            }
        });
    }

    private void importJavaRuntime(Uri source) {
        setBusy(true, getString(R.string.jvm_runtime_import_starting));
        worker.execute(() -> {
            try {
                JvmRuntimeImportService.Result result = JvmRuntimeImportService.importArchive(
                        this, source, (files, bytes, current) -> runOnUiThread(() -> showOperation(
                                getString(R.string.jvm_import_progress, files,
                                        Formatter.formatFileSize(this, bytes), current))));
                runOnUiThread(() -> {
                    setBusy(false, getString(R.string.jvm_runtime_import_complete,
                            result.files(), Formatter.formatFileSize(this, result.bytes()),
                            result.archiveSha256().substring(0, 12)));
                    refresh();
                });
            } catch (Exception failure) {
                runOnUiThread(() -> showFailure(R.string.jvm_runtime_import_failed, failure));
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

    private void showFailure(int stringResource, Throwable failure) {
        String message = getString(stringResource, safeMessage(failure));
        setBusy(false, message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void refresh() {
        File gameRoot = DesktopGameImportService.importedRoot(this);
        DesktopGameInspection inspection = DesktopGameLayout.inspect(gameRoot.toPath());
        boolean gameReady = inspection.isImportable();

        File runtimeHome = new File(new File(getFilesDir(), "desktop-jvm"), "runtime");
        File nativeDirectory = new File(getApplicationInfo().nativeLibraryDir);
        JvmBackendCapabilities capabilities = JvmRuntimeProbe.inspect(
                runtimeHome.toPath(), nativeDirectory.toPath(), NativeJvmHost.isPackaged());
        String runtimeIssue = JvmRuntimeProbe.runtimeIssue(runtimeHome.toPath());
        boolean runtimeReady = runtimeIssue.isEmpty()
                && capabilities.hasJava17() && capabilities.hasJvmHost()
                && capabilities.hasLwjgl2();

        setComponentState(gameStatus, gameReady,
                R.string.jvm_game_ready, R.string.jvm_game_missing);
        if (runtimeReady) {
            setComponentState(runtimeStatus, true,
                    R.string.jvm_runtime_ready, R.string.jvm_runtime_ready);
        } else {
            String missing = TextUtils.join(", ", capabilities.missing());
            runtimeStatus.setText(runtimeIssue.isEmpty()
                    ? getString(R.string.jvm_runtime_missing, missing)
                    : getString(R.string.jvm_runtime_missing_detail, missing, runtimeIssue));
            runtimeStatus.setTextColor(getColor(R.color.rf_status_warning));
        }

        smokeReady = capabilities.hasJava17() && capabilities.hasJvmHost();
        gameProbeReady = gameReady && runtimeReady;
        readinessBadge.setText(gameProbeReady
                ? R.string.jvm_ready_badge : R.string.jvm_setup_badge);
        readinessBadge.setTextColor(getColor(gameProbeReady
                ? R.color.rf_status_ready : R.color.rf_status_warning));
        readinessMessage.setText(gameProbeReady
                ? R.string.jvm_ready_message : R.string.jvm_setup_message);
        launchButton.setEnabled(!busy && gameProbeReady);
        launchButton.setText(gameProbeReady
                ? R.string.jvm_launch_game : R.string.jvm_launch_unavailable);
        smokeButton.setEnabled(!busy && smokeReady);
    }

    private void setComponentState(TextView view, boolean ready, int readyText, int missingText) {
        view.setText(ready ? readyText : missingText);
        view.setTextColor(getColor(ready ? R.color.rf_status_ready : R.color.rf_status_warning));
    }

    private void setBusy(boolean busy, String message) {
        this.busy = busy;
        showOperation(message);
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        importButton.setEnabled(!busy);
        directoryButton.setEnabled(!busy);
        runtimeButton.setEnabled(!busy);
        rendererButton.setEnabled(!busy);
        advancedButton.setEnabled(!busy);
        smokeButton.setEnabled(!busy && smokeReady);
        launchButton.setEnabled(!busy && gameProbeReady);
    }

    private void showOperation(String message) {
        operationStatus.setText(message);
        operationStatus.setVisibility(message == null || message.isEmpty()
                ? View.GONE : View.VISIBLE);
    }

    private static String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.trim().isEmpty()
                ? failure.getClass().getSimpleName() : message;
    }
}
