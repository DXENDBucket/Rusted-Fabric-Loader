package io.github.endx.rustedfabric.android.xposed.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.endx.rustedfabric.android.bootstrap.AndroidMappingProfile;
import io.github.endx.rustedfabric.android.mod.ModRegistry;
import io.github.endx.rustedfabric.android.mod.ModVerificationException;
import io.github.endx.rustedfabric.android.patcher.PatchException;
import io.github.endx.rustedfabric.android.xposed.R;
import io.github.endx.rustedfabric.android.xposed.patch.LocalPatchService;
import io.github.endx.rustedfabric.android.xposed.patch.PatchInstallReceiver;
import io.github.endx.rustedfabric.android.xposed.storage.InstalledGameVerifier;
import io.github.endx.rustedfabric.android.xposed.storage.InstalledPatchedGameVerifier;
import io.github.endx.rustedfabric.android.xposed.storage.ModContentProvider;
import io.github.endx.rustedfabric.android.xposed.storage.ModImportService;
import io.github.endx.rustedfabric.android.xposed.storage.ModStorage;
import io.github.endx.rustedfabricapi.api.multiplayer.MultiplayerMod;

/** Standalone Loader UI. It deliberately adds no screen or control to the game process. */
public final class ModManagerActivity extends Activity {
    private static final int REQUEST_IMPORT_MOD = 1001;
    private static final int REQUEST_PATCH_APK = 1002;
    private static final int REQUEST_INSTALL_PERMISSION = 1003;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private LinearLayout modList;
    private TextView gameStatus;
    private TextView operationStatus;
    private TextView multiplayerStatus;
    private ProgressBar progress;
    private Button patchButton;
    private Button importButton;
    private ModRegistry registry;
    private volatile boolean busy;
    private boolean firstResume = true;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        registry = ModStorage.registry(this);
        setContentView(createContent());
        refresh();
        if (state == null) {
            handleExternalImport(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleExternalImport(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (firstResume) {
            firstResume = false;
        } else if (!busy) {
            refresh();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMPORT_MOD && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            importMod(data.getData());
        } else if (requestCode == REQUEST_PATCH_APK && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            createLocalPatch(data.getData());
        } else if (requestCode == REQUEST_INSTALL_PERMISSION
                && (Build.VERSION.SDK_INT < 26
                || getPackageManager().canRequestPackageInstalls())) {
            choosePatchApk();
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
        scroll.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = text(getString(R.string.manager_title), 24, true);
        content.addView(title);
        TextView boundary = text(getString(R.string.manager_boundary), 14, false);
        boundary.setPadding(0, dp(8), 0, dp(12));
        content.addView(boundary);

        gameStatus = text(getString(R.string.game_status_checking), 16, true);
        content.addView(gameStatus);
        operationStatus = text(getString(R.string.restart_hint), 14, false);
        operationStatus.setPadding(0, dp(8), 0, dp(12));
        content.addView(operationStatus);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        progress.setVisibility(View.GONE);
        content.addView(progress, matchWidth());

        multiplayerStatus = text(getString(R.string.multiplayer_status_checking), 14, true);
        multiplayerStatus.setPadding(0, dp(8), 0, dp(12));
        content.addView(multiplayerStatus);

        patchButton = new Button(this);
        patchButton.setText(R.string.create_local_patch);
        patchButton.setOnClickListener(ignored -> choosePatchApk());
        content.addView(patchButton, matchWidth());
        TextView patchHint = text(getString(R.string.patch_source_hint), 13, false);
        patchHint.setPadding(0, dp(6), 0, dp(12));
        content.addView(patchHint);

        importButton = new Button(this);
        importButton.setText(R.string.import_mod);
        importButton.setOnClickListener(ignored -> chooseMod());
        content.addView(importButton, matchWidth());

        TextView warning = text(getString(R.string.trusted_code_warning), 13, false);
        warning.setPadding(0, dp(12), 0, dp(16));
        content.addView(warning);

        modList = new LinearLayout(this);
        modList.setOrientation(LinearLayout.VERTICAL);
        content.addView(modList, matchWidth());
        return scroll;
    }

    private void choosePatchApk() {
        if (Build.VERSION.SDK_INT >= 26
                && !getPackageManager().canRequestPackageInstalls()) {
            operationStatus.setText(R.string.allow_installs);
            Intent permission = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(permission, REQUEST_INSTALL_PERMISSION);
            return;
        }
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("application/vnd.android.package-archive");
        startActivityForResult(picker, REQUEST_PATCH_APK);
    }

    private void createLocalPatch(Uri source) {
        setBusy(true, getString(R.string.patch_stage_preparing));
        worker.execute(() -> {
            try {
                LocalPatchService.Result result = LocalPatchService.patchAndEnqueue(
                        this, source, stage -> runOnUiThread(() ->
                                setBusy(true, patchStageText(stage))));
                runOnUiThread(() -> setBusy(false,
                        getString(R.string.patch_queued, result.getSessionId())));
            } catch (Exception failure) {
                showFailure(getString(R.string.patch_failed, friendlyMessage(failure)));
            }
        });
    }

    private void chooseMod() {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("*/*");
        startActivityForResult(picker, REQUEST_IMPORT_MOD);
    }

    @SuppressWarnings("deprecation")
    private void handleExternalImport(Intent intent) {
        if (intent == null) {
            return;
        }
        Uri source = null;
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            source = intent.getData();
        } else if (Intent.ACTION_SEND.equals(intent.getAction())) {
            source = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        }
        if (source != null) {
            importMod(source);
        }
    }

    private void importMod(Uri source) {
        setBusy(true, getString(R.string.importing_mod));
        worker.execute(() -> {
            try {
                ModRegistry.Record record = ModImportService.importUri(this, source);
                notifyRegistryChanged();
                runOnUiThread(() -> {
                    setBusy(false, getString(R.string.import_succeeded, record.getName()));
                    Toast.makeText(this, getString(R.string.import_succeeded, record.getName()),
                            Toast.LENGTH_LONG).show();
                    refresh();
                });
            } catch (IOException | ModVerificationException failure) {
                showFailure(getString(R.string.import_failed, friendlyMessage(failure)));
            } catch (RuntimeException failure) {
                showFailure(getString(R.string.import_failed, friendlyMessage(failure)));
            }
        });
    }

    private void refresh() {
        if (!busy) operationStatus.setText(R.string.loading_mods);
        worker.execute(() -> {
            InstalledGameVerifier.Result game = InstalledGameVerifier.verify(this);
            InstalledPatchedGameVerifier.Result patchedGame =
                    InstalledPatchedGameVerifier.verify(this);
            List<ModRegistry.Record> records;
            String failure = null;
            try {
                records = registry.list();
            } catch (IOException unreadable) {
                records = Collections.emptyList();
                failure = safeMessage(unreadable);
            }
            List<ModRegistry.Record> finalRecords = records;
            String finalFailure = failure;
            runOnUiThread(() -> render(game, patchedGame, finalRecords, finalFailure));
        });
    }

    private void render(InstalledGameVerifier.Result game,
                        InstalledPatchedGameVerifier.Result patchedGame,
                        List<ModRegistry.Record> records,
                        String failure) {
        boolean runnableGame = game.isVerified() || patchedGame.isVerified();
        gameStatus.setText(game.isVerified()
                ? getString(R.string.game_status_verified, AndroidMappingProfile.VERSION_NAME)
                : patchedGame.isVerified()
                        ? getString(R.string.game_status_patch_verified)
                        : getString(R.string.game_status_unsupported,
                                game.getStatus() + "/" + patchedGame.getStatus()));
        if (!busy) {
            String installStatus = consumeInstallStatus();
            operationStatus.setText(installStatus != null ? installStatus : failure == null
                    ? getResources().getQuantityString(R.plurals.mod_count_restart,
                            records.size(), records.size())
                    : getString(R.string.registry_failed, failure));
        }
        renderMultiplayerStatus(records);
        modList.removeAllViews();
        if (records.isEmpty()) {
            TextView empty = text(getString(R.string.no_mods), 16, false);
            empty.setPadding(0, dp(16), 0, 0);
            modList.addView(empty);
            return;
        }
        for (ModRegistry.Record record : records) {
            modList.addView(createModCard(record, runnableGame), matchWidth());
        }
    }

    @SuppressWarnings("UseSwitchCompatOrMaterialCode")
    private View createModCard(ModRegistry.Record record, boolean gameVerified) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));

        TextView name = text(record.getName() + "  " + record.getVersion(), 18, true);
        card.addView(name);
        boolean compatible = record.getMappingProfiles().contains(AndroidMappingProfile.ID);
        TextView details = text(getString(R.string.mod_details, record.getId(),
                compatible ? getString(R.string.compatible) : getString(R.string.incompatible),
                record.getArchiveSha256().substring(0, 12)), 13, false);
        details.setPadding(0, dp(4), 0, dp(6));
        card.addView(details);
        TextView network = text(getString(R.string.mod_multiplayer,
                multiplayerModeText(record.getMultiplayer().mode())), 13, false);
        card.addView(network);

        Switch enabled = new Switch(this);
        enabled.setText(R.string.enable_mod);
        enabled.setChecked(record.isEnabled());
        enabled.setEnabled(gameVerified && compatible);
        enabled.setOnCheckedChangeListener((button, checked) -> setEnabled(record.getId(), checked));
        card.addView(enabled);

        Button remove = new Button(this);
        remove.setText(R.string.remove_mod);
        remove.setOnClickListener(ignored -> confirmRemove(record));
        card.addView(remove);
        return card;
    }

    private void setEnabled(String id, boolean enabled) {
        worker.execute(() -> {
            try {
                registry.setEnabled(id, enabled);
                notifyRegistryChanged();
                runOnUiThread(this::refresh);
            } catch (IOException failure) {
                showFailure(getString(R.string.operation_failed, safeMessage(failure)));
            }
        });
    }

    private void confirmRemove(ModRegistry.Record record) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.remove_mod)
                .setMessage(getString(R.string.remove_confirmation, record.getName()))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.remove_mod, (dialog, which) -> remove(record.getId()))
                .show();
    }

    private void remove(String id) {
        worker.execute(() -> {
            try {
                registry.remove(id);
                notifyRegistryChanged();
                runOnUiThread(this::refresh);
            } catch (IOException failure) {
                showFailure(getString(R.string.operation_failed, safeMessage(failure)));
            }
        });
    }

    private void notifyRegistryChanged() {
        getContentResolver().notifyChange(ModContentProvider.ENABLED_MODS_URI, null);
    }

    private void showFailure(String message) {
        runOnUiThread(() -> {
            setBusy(false, message);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            new AlertDialog.Builder(this)
                    .setTitle(R.string.operation_failed_title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        });
    }

    private void setBusy(boolean value, String message) {
        busy = value;
        operationStatus.setText(message);
        progress.setVisibility(value ? View.VISIBLE : View.GONE);
        patchButton.setEnabled(!value);
        importButton.setEnabled(!value);
    }

    private String patchStageText(LocalPatchService.Stage stage) {
        switch (stage) {
            case COPYING_SOURCE: return getString(R.string.patch_stage_copying);
            case VERIFYING_AND_WEAVING: return getString(R.string.patch_stage_weaving);
            case SIGNING: return getString(R.string.patch_stage_signing);
            case REQUESTING_INSTALL: return getString(R.string.patch_stage_installing);
            case COMPLETE: return getString(R.string.patch_stage_complete);
            case PREPARING:
            default: return getString(R.string.patch_stage_preparing);
        }
    }

    private void renderMultiplayerStatus(List<ModRegistry.Record> records) {
        int required = 0;
        int clientOnly = 0;
        int unsafe = 0;
        for (ModRegistry.Record record : records) {
            if (!record.isEnabled()) continue;
            switch (record.getMultiplayer().mode()) {
                case REQUIRED: required++; break;
                case CLIENT_ONLY: clientOnly++; break;
                case UNSAFE: unsafe++; break;
                default: break;
            }
        }
        multiplayerStatus.setText(unsafe > 0
                ? getString(R.string.multiplayer_blocked, unsafe)
                : getString(R.string.multiplayer_ready, required, clientOnly));
    }

    private String multiplayerModeText(MultiplayerMod.Mode mode) {
        switch (mode) {
            case REQUIRED: return getString(R.string.multiplayer_mode_required);
            case CLIENT_ONLY: return getString(R.string.multiplayer_mode_client_only);
            case UNSAFE:
            default: return getString(R.string.multiplayer_mode_unsafe);
        }
    }

    private String consumeInstallStatus() {
        android.content.SharedPreferences preferences = getSharedPreferences(
                PatchInstallReceiver.PREFS, MODE_PRIVATE);
        if (!preferences.getBoolean(PatchInstallReceiver.PREF_UNREAD, false)) return null;
        int status = preferences.getInt(PatchInstallReceiver.PREF_STATUS,
                PackageInstaller.STATUS_FAILURE);
        String detail = preferences.getString(PatchInstallReceiver.PREF_DETAIL, "");
        preferences.edit().putBoolean(PatchInstallReceiver.PREF_UNREAD, false).apply();
        return status == PackageInstaller.STATUS_SUCCESS
                ? getString(R.string.patch_install_succeeded)
                : getString(R.string.patch_install_failed,
                detail == null || detail.isEmpty() ? getString(R.string.unknown_error) : detail);
    }

    private String friendlyMessage(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null && current.getCause() != current) {
            if (current instanceof PatchException
                    || current instanceof ModVerificationException) break;
            current = current.getCause();
        }
        if (current instanceof PatchException) {
            PatchException.Reason reason = ((PatchException) current).getReason();
            switch (reason) {
                case PROFILE_MISMATCH: return getString(R.string.error_apk_profile_mismatch);
                case DEX_WEAVE_FAILED: return getString(R.string.error_dex_weave);
                case SIGNING_FAILED:
                case SIGNATURE_INVALID: return getString(R.string.error_signing);
                case INPUT_MISSING: return getString(R.string.error_input_missing);
                case INPUT_TOO_LARGE: return getString(R.string.error_input_too_large);
                default: return getString(R.string.error_patch_invalid);
            }
        }
        if (current instanceof ModVerificationException) {
            return getString(R.string.error_mod_invalid, safeMessage(current));
        }
        if (current instanceof SecurityException) return getString(R.string.error_permission);
        return safeMessage(current);
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
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
