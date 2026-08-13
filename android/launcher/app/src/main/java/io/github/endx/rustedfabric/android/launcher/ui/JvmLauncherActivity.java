package io.github.endx.rustedfabric.android.launcher.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.Settings;
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
import io.github.endx.rustedfabric.android.launcher.jvm.SharedContentWorkspace;

/** User-facing launcher for the imported desktop game and its ARM64 JVM runtime. */
public final class JvmLauncherActivity extends Activity {
    private static final String LAUNCHER_PREFERENCES = "launcher";
    private static final String DISCLAIMER_ACCEPTED = "third_party_code_disclaimer_v1";
    private static final int REQUEST_GAME_ARCHIVE = 2001;
    private static final int REQUEST_GAME_TREE = 2002;
    private static final int REQUEST_JAVA_RUNTIME = 2003;
    private static final int REQUEST_INI_MOD = 2004;
    private static final int REQUEST_MAP = 2005;
    private static final int REQUEST_JAVA_MOD = 2006;
    private static final int REQUEST_SHARED_STORAGE = 2007;
    private static final String STATE_PAGE = "selected_page";
    private static final int PAGE_LAUNCH = 0;
    private static final int PAGE_CONTENT = 1;
    private static final int PAGE_SETTINGS = 2;

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
    private Button openIniFolderButton;
    private Button openMapsFolderButton;
    private Button openJavaFolderButton;
    private LinearLayout advancedPanel;
    private LinearLayout contentPanel;
    private LinearLayout contentUnavailablePanel;
    private View launchPage;
    private View contentPage;
    private View settingsPage;
    private Button navLaunchButton;
    private Button navContentButton;
    private Button navSettingsButton;
    private TextView contentSummary;
    private TextView contentStorageStatus;
    private boolean busy;
    private boolean smokeReady;
    private boolean gameProbeReady;
    private boolean advancedVisible;
    private boolean receiverRegistered;
    private boolean gameImported;
    private boolean officialProvisioning;
    private boolean workspaceReady;
    private boolean workspacePreparing;
    private boolean storagePromptShown;
    private Runnable pendingWorkspaceAction;
    private int selectedPage = PAGE_LAUNCH;

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
        showPage(state == null ? PAGE_LAUNCH : state.getInt(STATE_PAGE, PAGE_LAUNCH));
        refresh();
        showRiskDisclaimerIfRequired();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putInt(STATE_PAGE, selectedPage);
        super.onSaveInstanceState(state);
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
        openIniFolderButton = findViewById(R.id.open_ini_folder_button);
        openMapsFolderButton = findViewById(R.id.open_maps_folder_button);
        openJavaFolderButton = findViewById(R.id.open_java_folder_button);
        advancedPanel = findViewById(R.id.advanced_panel);
        contentPanel = findViewById(R.id.content_panel);
        contentUnavailablePanel = findViewById(R.id.content_unavailable_panel);
        contentSummary = findViewById(R.id.content_summary);
        contentStorageStatus = findViewById(R.id.content_storage_status);
        launchPage = findViewById(R.id.launch_page);
        contentPage = findViewById(R.id.content_page);
        settingsPage = findViewById(R.id.settings_page);
        navLaunchButton = findViewById(R.id.nav_launch_button);
        navContentButton = findViewById(R.id.nav_content_button);
        navSettingsButton = findViewById(R.id.nav_settings_button);
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
        openIniFolderButton.setOnClickListener(ignored ->
                openSharedFolder(ManagedContentLibrary.Kind.INI_MOD));
        openMapsFolderButton.setOnClickListener(ignored ->
                openSharedFolder(ManagedContentLibrary.Kind.MAP));
        openJavaFolderButton.setOnClickListener(ignored ->
                openSharedFolder(ManagedContentLibrary.Kind.JAVA_MOD));
        navLaunchButton.setOnClickListener(ignored -> showPage(PAGE_LAUNCH));
        navContentButton.setOnClickListener(ignored -> showPage(PAGE_CONTENT));
        navSettingsButton.setOnClickListener(ignored -> showPage(PAGE_SETTINGS));
    }

    private void showPage(int page) {
        if (page < PAGE_LAUNCH || page > PAGE_SETTINGS) page = PAGE_LAUNCH;
        selectedPage = page;
        launchPage.setVisibility(page == PAGE_LAUNCH ? View.VISIBLE : View.GONE);
        contentPage.setVisibility(page == PAGE_CONTENT ? View.VISIBLE : View.GONE);
        settingsPage.setVisibility(page == PAGE_SETTINGS ? View.VISIBLE : View.GONE);
        navLaunchButton.setSelected(page == PAGE_LAUNCH);
        navContentButton.setSelected(page == PAGE_CONTENT);
        navSettingsButton.setSelected(page == PAGE_SETTINGS);
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

    private void showRiskDisclaimerIfRequired() {
        if (getSharedPreferences(LAUNCHER_PREFERENCES, MODE_PRIVATE)
                .getBoolean(DISCLAIMER_ACCEPTED, false)) return;
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.security_disclaimer_title)
                .setMessage(R.string.security_disclaimer_message)
                .setPositiveButton(R.string.security_disclaimer_accept, (ignored, which) ->
                        getSharedPreferences(LAUNCHER_PREFERENCES, MODE_PRIVATE).edit()
                                .putBoolean(DISCLAIMER_ACCEPTED, true).apply())
                .setNeutralButton(R.string.security_disclaimer_view_full, null)
                .setNegativeButton(R.string.security_disclaimer_exit,
                        (ignored, which) -> finish())
                .setCancelable(false)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                .setOnClickListener(button -> showPackagedDisclaimer()));
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void showPackagedLicense() {
        showPackagedText(R.string.jvm_full_license,
                "rusted-fabric/licenses/Rusted-Fabric-Android-GPL-3.0.txt");
    }

    private void showPackagedDisclaimer() {
        showPackagedText(R.string.security_disclaimer_title,
                "rusted-fabric/DISCLAIMER.md");
    }

    private void showPackagedText(int title, String assetPath) {
        TextView license = new TextView(this);
        int padding = Math.round(18 * getResources().getDisplayMetrics().density);
        license.setPadding(padding, padding, padding, padding);
        license.setTextIsSelectable(true);
        license.setText(readAssetText(assetPath));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(license);
        new AlertDialog.Builder(this)
                .setTitle(title)
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
        if (requestCode == REQUEST_SHARED_STORAGE) {
            prepareSharedWorkspace(pendingWorkspaceAction, false);
            return;
        }
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
        if (gameImported) prepareSharedWorkspace(null, true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_SHARED_STORAGE) return;
        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (granted) {
            prepareSharedWorkspace(pendingWorkspaceAction, false);
        } else {
            pendingWorkspaceAction = null;
            Toast.makeText(this, R.string.content_storage_permission_denied,
                    Toast.LENGTH_LONG).show();
            refresh();
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

    private void launchGame() {
        if (gameImported && !workspaceReady) {
            prepareSharedWorkspace(this::launchGame, false);
            return;
        }
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
                    prepareSharedWorkspace(null, true);
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
        workspaceReady = gameReady && SharedContentWorkspace.isReady(this);

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
        gameProbeReady = gameReady && runtimeReady && workspaceReady;
        readinessBadge.setText(gameProbeReady
                ? R.string.jvm_ready_badge : R.string.jvm_setup_badge);
        readinessBadge.setTextColor(getColor(gameProbeReady
                ? R.color.rf_status_ready : R.color.rf_status_warning));
        readinessMessage.setText(gameProbeReady
                ? R.string.jvm_ready_message
                : gameReady && runtimeReady && !workspaceReady
                    ? R.string.content_storage_permission_required
                    : R.string.jvm_setup_message);
        launchButton.setEnabled(!busy && gameProbeReady);
        launchButton.setText(gameProbeReady
                ? R.string.jvm_launch_game : R.string.jvm_launch_unavailable);
        smokeButton.setEnabled(!busy && smokeReady);
        contentPanel.setVisibility(gameReady ? View.VISIBLE : View.GONE);
        contentUnavailablePanel.setVisibility(gameReady ? View.GONE : View.VISIBLE);
        setContentButtonsEnabled(gameReady && !busy);
        if (gameReady) {
            if (workspaceReady) {
                contentStorageStatus.setText(getString(R.string.content_storage_ready,
                        SharedContentWorkspace.root().toString()));
            } else {
                contentStorageStatus.setText(R.string.content_storage_permission_required);
            }
            contentStorageStatus.setTextColor(getColor(workspaceReady
                    ? R.color.rf_status_ready : R.color.rf_status_warning));
            if (workspaceReady) refreshContentSummary();
            else contentSummary.setText(R.string.content_summary_empty);
        }
    }

    private void showContentManager(ManagedContentLibrary.Kind kind) {
        if (!gameImported || busy) return;
        if (!workspaceReady) {
            prepareSharedWorkspace(() -> showContentManager(kind), false);
            return;
        }
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

    private void openSharedFolder(ManagedContentLibrary.Kind kind) {
        if (!gameImported || busy) return;
        if (!workspaceReady) {
            prepareSharedWorkspace(() -> openSharedFolder(kind), false);
            return;
        }
        openFolderDocument(SharedContentWorkspace.documentId(kind));
    }

    private void openFolderDocument(String documentId) {
        Uri folder = DocumentsContract.buildDocumentUri(
                "com.android.externalstorage.documents", documentId);
        Intent view = new Intent(Intent.ACTION_VIEW);
        view.setDataAndType(folder, DocumentsContract.Document.MIME_TYPE_DIR);
        view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            startActivity(view);
        } catch (RuntimeException noFolderViewer) {
            Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            picker.putExtra(DocumentsContract.EXTRA_INITIAL_URI, folder);
            picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
            try {
                startActivity(picker);
            } catch (RuntimeException unavailable) {
                Toast.makeText(this, R.string.content_open_folder_unavailable,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void prepareSharedWorkspace(Runnable afterReady, boolean automatic) {
        if (!gameImported) return;
        if (afterReady != null) pendingWorkspaceAction = afterReady;
        if (workspaceReady) {
            Runnable action = pendingWorkspaceAction;
            pendingWorkspaceAction = null;
            ensureOfficialMods(action);
            return;
        }
        if (!SharedContentWorkspace.hasStorageAccess(this)) {
            requestSharedStorageAccess(automatic);
            return;
        }
        if (workspacePreparing) return;
        workspacePreparing = true;
        setBusy(true, getString(R.string.content_storage_preparing));
        worker.execute(() -> {
            try {
                SharedContentWorkspace.ensureReady(this);
                runOnUiThread(() -> {
                    workspacePreparing = false;
                    setBusy(false, getString(R.string.content_storage_ready,
                            SharedContentWorkspace.root().toString()));
                    refresh();
                    Runnable action = pendingWorkspaceAction;
                    pendingWorkspaceAction = null;
                    ensureOfficialMods(action);
                });
            } catch (Exception failure) {
                runOnUiThread(() -> {
                    workspacePreparing = false;
                    pendingWorkspaceAction = null;
                    showFailure(R.string.content_storage_failed, failure);
                    refresh();
                });
            }
        });
    }

    private void requestSharedStorageAccess(boolean automatic) {
        if (automatic && storagePromptShown) return;
        storagePromptShown = true;
        new AlertDialog.Builder(this)
                .setTitle(R.string.content_storage_permission_title)
                .setMessage(R.string.content_storage_permission_message)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    pendingWorkspaceAction = null;
                    refresh();
                })
                .setPositiveButton(R.string.content_storage_permission_open,
                        (dialog, which) -> openSharedStorageSettings())
                .show();
    }

    private void openSharedStorageSettings() {
        if (Build.VERSION.SDK_INT >= 30) {
            Intent settings = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            try {
                startActivityForResult(settings, REQUEST_SHARED_STORAGE);
            } catch (RuntimeException unavailable) {
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
                        REQUEST_SHARED_STORAGE);
            }
        } else {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_SHARED_STORAGE);
        }
    }

    private void chooseManagedContent(ManagedContentLibrary.Kind kind) {
        if (kind == ManagedContentLibrary.Kind.JAVA_MOD) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.security_disclaimer_import_title)
                    .setMessage(R.string.security_disclaimer_import_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.security_disclaimer_import_continue,
                            (ignored, which) -> openManagedContentPicker(kind))
                    .show();
            return;
        }
        openManagedContentPicker(kind);
    }

    private void openManagedContentPicker(ManagedContentLibrary.Kind kind) {
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
        ensureOfficialMods(null);
    }

    private void ensureOfficialMods(Runnable afterReady) {
        boolean currentAssets = getSharedPreferences(LAUNCHER_PREFERENCES, MODE_PRIVATE)
                .getInt("official_mod_assets", -1)
                == io.github.endx.rustedfabric.android.launcher.BuildConfig.VERSION_CODE;
        if (!gameImported || !workspaceReady || officialProvisioning || currentAssets) {
            if (afterReady != null && !officialProvisioning) afterReady.run();
            return;
        }
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
                    if (afterReady != null) afterReady.run();
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
        getSharedPreferences(LAUNCHER_PREFERENCES, MODE_PRIVATE).edit()
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
        openIniFolderButton.setEnabled(enabled);
        openMapsFolderButton.setEnabled(enabled);
        openJavaFolderButton.setEnabled(enabled);
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
