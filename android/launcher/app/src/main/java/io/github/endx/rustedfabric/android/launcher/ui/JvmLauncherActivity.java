package io.github.endx.rustedfabric.android.launcher.ui;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.endx.rustedfabric.android.jvm.DesktopGameInspection;
import io.github.endx.rustedfabric.android.jvm.DesktopGameLayout;
import io.github.endx.rustedfabric.android.jvm.JvmBackendCapabilities;
import io.github.endx.rustedfabric.android.jvm.JvmRuntimeProbe;
import io.github.endx.rustedfabric.android.jvm.ManagedContentLibrary;
import io.github.endx.rustedfabric.android.launcher.R;
import io.github.endx.rustedfabric.android.launcher.jvm.DesktopGameImportService;
import io.github.endx.rustedfabric.android.launcher.jvm.JvmHostService;
import io.github.endx.rustedfabric.android.launcher.jvm.JvmRuntimeImportService;
import io.github.endx.rustedfabric.android.launcher.jvm.ManagedContentImportService;
import io.github.endx.rustedfabric.android.launcher.jvm.NativeJvmHost;
import io.github.endx.rustedfabric.android.launcher.jvm.OfficialModProvisioner;

/** User-facing launcher for the imported desktop game and its ARM64 JVM runtime. */
public final class JvmLauncherActivity extends Activity {
    private static final int REQUEST_GAME_ARCHIVE = 2001;
    private static final int REQUEST_GAME_TREE = 2002;
    private static final int REQUEST_JAVA_RUNTIME = 2003;
    private static final int REQUEST_INI_MOD = 2004;
    private static final int REQUEST_MAP = 2005;
    private static final int REQUEST_JAVA_MOD = 2006;

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
    private Button licenseButton;
    private Button iniModsButton;
    private Button mapsButton;
    private Button javaModsButton;
    private LinearLayout advancedPanel;
    private LinearLayout contentPanel;
    private TextView contentSummary;
    private boolean busy;
    private boolean smokeReady;
    private boolean gameProbeReady;
    private boolean advancedVisible;
    private boolean receiverRegistered;
    private boolean gameImported;
    private boolean officialModsReady;
    private boolean officialProvisioning;

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
        licenseButton = findViewById(R.id.license_button);
        iniModsButton = findViewById(R.id.manage_ini_mods_button);
        mapsButton = findViewById(R.id.manage_maps_button);
        javaModsButton = findViewById(R.id.manage_java_mods_button);
        advancedPanel = findViewById(R.id.advanced_panel);
        contentPanel = findViewById(R.id.content_panel);
        contentSummary = findViewById(R.id.content_summary);
    }

    private void bindActions() {
        importButton.setOnClickListener(ignored -> chooseDesktopArchive());
        directoryButton.setOnClickListener(ignored -> chooseDesktopDirectory());
        runtimeButton.setOnClickListener(ignored -> chooseJavaRuntime());
        smokeButton.setOnClickListener(ignored -> testJavaRuntime());
        rendererButton.setOnClickListener(ignored ->
                startActivity(new Intent(this, JvmRenderActivity.class)));
        advancedButton.setOnClickListener(ignored -> showAdvanced(!advancedVisible));
        licenseButton.setOnClickListener(ignored -> showOpenSourceNotice());
        launchButton.setOnClickListener(ignored -> launchGame());
        iniModsButton.setOnClickListener(ignored ->
                showContentManager(ManagedContentLibrary.Kind.INI_MOD));
        mapsButton.setOnClickListener(ignored ->
                showContentManager(ManagedContentLibrary.Kind.MAP));
        javaModsButton.setOnClickListener(ignored ->
                showContentManager(ManagedContentLibrary.Kind.JAVA_MOD));
    }

    private void showOpenSourceNotice() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.jvm_license_title)
                .setMessage(R.string.jvm_license_notice)
                .setNeutralButton(R.string.jvm_full_license,
                        (dialog, which) -> showPackagedLicense())
                .setNegativeButton(R.string.jvm_source_code, (dialog, which) ->
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                                "https://github.com/DXENDBucket/Rusted-Fabric-Loader"))))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showPackagedLicense() {
        TextView license = new TextView(this);
        int padding = Math.round(18 * getResources().getDisplayMetrics().density);
        license.setPadding(padding, padding, padding, padding);
        license.setTextIsSelectable(true);
        license.setText(readAssetText(
                "rusted-fabric/licenses/Rusted-Fabric-Android-GPL-3.0.txt"));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(license);
        new AlertDialog.Builder(this)
                .setTitle(R.string.jvm_full_license)
                .setView(scroll)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private String readAssetText(String path) {
        try (InputStream input = getAssets().open(path);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            return getString(R.string.jvm_license_unavailable);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        if (requestCode != REQUEST_GAME_ARCHIVE && requestCode != REQUEST_GAME_TREE
                && requestCode != REQUEST_JAVA_RUNTIME && requestCode != REQUEST_INI_MOD
                && requestCode != REQUEST_MAP && requestCode != REQUEST_JAVA_MOD) return;

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
        else if (requestCode == REQUEST_JAVA_RUNTIME) importJavaRuntime(source);
        else importManagedContent(source, contentKind(requestCode));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(smokeReceiver, new IntentFilter(JvmHostService.ACTION_RESULT),
                RECEIVER_NOT_EXPORTED);
        receiverRegistered = true;
        refreshDiagnostics();
        refresh();
        ensureOfficialMods();
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
                OfficialModProvisioner.provision(this);
                markOfficialModsProvisioned();
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
        gameImported = gameReady;
        officialModsReady = gameReady && inspectOfficialMods();

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
        gameProbeReady = gameReady && runtimeReady && officialModsReady;
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
        contentPanel.setVisibility(gameReady ? View.VISIBLE : View.GONE);
        setContentButtonsEnabled(gameReady && !busy);
        if (gameReady) refreshContentSummary();
    }

    private void showContentManager(ManagedContentLibrary.Kind kind) {
        if (!gameImported || busy) return;
        ContentManagerDialog.show(this, worker, DesktopGameImportService.importedRoot(this), kind,
                new ContentManagerDialog.Listener() {
                    @Override
                    public void importContent(ManagedContentLibrary.Kind selected) {
                        chooseManagedContent(selected);
                    }

                    @Override
                    public void contentChanged() {
                        refresh();
                    }
                });
    }

    private void chooseManagedContent(ManagedContentLibrary.Kind kind) {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("*/*");
        if (kind == ManagedContentLibrary.Kind.JAVA_MOD) {
            picker.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "application/java-archive", "application/zip", "application/octet-stream"
            });
        } else if (kind == ManagedContentLibrary.Kind.MAP) {
            picker.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "application/zip", "application/octet-stream", "text/xml", "text/plain"
            });
        } else {
            picker.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "application/zip", "application/octet-stream", "text/plain"
            });
        }
        picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(picker, requestCode(kind));
    }

    private void importManagedContent(Uri source, ManagedContentLibrary.Kind kind) {
        setBusy(true, getString(R.string.content_import_starting));
        worker.execute(() -> {
            try {
                ManagedContentLibrary.Item item = ManagedContentImportService.importDocument(
                        this, source, kind, bytes -> runOnUiThread(() -> showOperation(
                                getString(R.string.content_import_progress,
                                        Formatter.formatFileSize(this, bytes)))));
                runOnUiThread(() -> {
                    setBusy(false, getString(R.string.content_import_complete, item.name()));
                    refresh();
                    showContentManager(kind);
                });
            } catch (Exception failure) {
                runOnUiThread(() -> showFailure(R.string.content_import_failed, failure));
            }
        });
    }

    private void ensureOfficialMods() {
        boolean currentAssets = getSharedPreferences("launcher", MODE_PRIVATE)
                .getInt("official_mod_assets", -1)
                == io.github.endx.rustedfabric.android.launcher.BuildConfig.VERSION_CODE;
        if (!gameImported || officialProvisioning || (officialModsReady && currentAssets)) return;
        officialProvisioning = true;
        setBusy(true, getString(R.string.content_official_installing));
        worker.execute(() -> {
            try {
                OfficialModProvisioner.provision(this);
                markOfficialModsProvisioned();
                runOnUiThread(() -> {
                    officialProvisioning = false;
                    setBusy(false, getString(R.string.content_official_ready));
                    refresh();
                });
            } catch (Exception failure) {
                runOnUiThread(() -> {
                    officialProvisioning = false;
                    showFailure(R.string.content_official_failed, failure);
                    refresh();
                });
            }
        });
    }

    private boolean inspectOfficialMods() {
        boolean api = false;
        boolean menu = false;
        boolean essentials = false;
        try {
            for (ManagedContentLibrary.Item item : ManagedContentLibrary.list(
                    DesktopGameImportService.importedRoot(this).toPath(),
                    ManagedContentLibrary.Kind.JAVA_MOD)) {
                if ("rusted_fabric_api".equals(item.id())) api = item.enabled();
                else if ("java_mod_menu".equals(item.id())) menu = true;
                else if ("ini_essentials".equals(item.id())) essentials = true;
            }
        } catch (IOException ignored) {
            return false;
        }
        return api && menu && essentials;
    }

    private void refreshContentSummary() {
        try {
            int ini = ManagedContentLibrary.list(DesktopGameImportService.importedRoot(this).toPath(),
                    ManagedContentLibrary.Kind.INI_MOD).size();
            List<ManagedContentLibrary.Item> maps = ManagedContentLibrary.list(
                    DesktopGameImportService.importedRoot(this).toPath(),
                    ManagedContentLibrary.Kind.MAP);
            List<ManagedContentLibrary.Item> javaMods = ManagedContentLibrary.list(
                    DesktopGameImportService.importedRoot(this).toPath(),
                    ManagedContentLibrary.Kind.JAVA_MOD);
            long enabledMaps = maps.stream().filter(ManagedContentLibrary.Item::enabled).count();
            long enabledJava = javaMods.stream().filter(ManagedContentLibrary.Item::enabled).count();
            contentSummary.setText(getString(R.string.content_summary, ini,
                    enabledMaps, maps.size(), enabledJava, javaMods.size()));
        } catch (IOException failure) {
            contentSummary.setText(getString(R.string.content_summary_failed,
                    safeMessage(failure)));
        }
    }

    private void markOfficialModsProvisioned() {
        getSharedPreferences("launcher", MODE_PRIVATE).edit()
                .putInt("official_mod_assets", io.github.endx.rustedfabric.android.launcher.BuildConfig.VERSION_CODE)
                .apply();
    }

    private static int requestCode(ManagedContentLibrary.Kind kind) {
        switch (kind) {
            case INI_MOD: return REQUEST_INI_MOD;
            case MAP: return REQUEST_MAP;
            case JAVA_MOD: return REQUEST_JAVA_MOD;
            default: throw new IllegalArgumentException("Unknown content kind");
        }
    }

    private static ManagedContentLibrary.Kind contentKind(int requestCode) {
        if (requestCode == REQUEST_INI_MOD) return ManagedContentLibrary.Kind.INI_MOD;
        if (requestCode == REQUEST_MAP) return ManagedContentLibrary.Kind.MAP;
        if (requestCode == REQUEST_JAVA_MOD) return ManagedContentLibrary.Kind.JAVA_MOD;
        throw new IllegalArgumentException("Unknown content request");
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
        setContentButtonsEnabled(!busy && gameImported);
    }

    private void setContentButtonsEnabled(boolean enabled) {
        iniModsButton.setEnabled(enabled);
        mapsButton.setEnabled(enabled);
        javaModsButton.setEnabled(enabled);
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
