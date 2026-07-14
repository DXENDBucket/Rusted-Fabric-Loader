package io.github.endx.rustedfabric.android.xposed.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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
import io.github.endx.rustedfabric.android.xposed.R;
import io.github.endx.rustedfabric.android.xposed.storage.InstalledGameVerifier;
import io.github.endx.rustedfabric.android.xposed.storage.ModContentProvider;
import io.github.endx.rustedfabric.android.xposed.storage.ModImportService;
import io.github.endx.rustedfabric.android.xposed.storage.ModStorage;

/** Standalone Loader UI. It deliberately adds no screen or control to the game process. */
public final class ModManagerActivity extends Activity {
    private static final int REQUEST_IMPORT_MOD = 1001;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private LinearLayout modList;
    private TextView gameStatus;
    private TextView operationStatus;
    private ModRegistry registry;

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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMPORT_MOD && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            importMod(data.getData());
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

        Button importButton = new Button(this);
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
        operationStatus.setText(R.string.importing_mod);
        worker.execute(() -> {
            try {
                ModRegistry.Record record = ModImportService.importUri(this, source);
                notifyRegistryChanged();
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.import_succeeded, record.getName()),
                            Toast.LENGTH_LONG).show();
                    refresh();
                });
            } catch (IOException | ModVerificationException failure) {
                showFailure(getString(R.string.import_failed, safeMessage(failure)));
            } catch (RuntimeException failure) {
                showFailure(getString(R.string.import_failed, safeMessage(failure)));
            }
        });
    }

    private void refresh() {
        operationStatus.setText(R.string.loading_mods);
        worker.execute(() -> {
            InstalledGameVerifier.Result game = InstalledGameVerifier.verify(this);
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
            runOnUiThread(() -> render(game, finalRecords, finalFailure));
        });
    }

    private void render(InstalledGameVerifier.Result game, List<ModRegistry.Record> records,
                        String failure) {
        gameStatus.setText(game.isVerified()
                ? getString(R.string.game_status_verified, AndroidMappingProfile.VERSION_NAME)
                : getString(R.string.game_status_unsupported, game.getStatus()));
        operationStatus.setText(failure == null
                ? getResources().getQuantityString(R.plurals.mod_count_restart,
                        records.size(), records.size())
                : getString(R.string.registry_failed, failure));
        modList.removeAllViews();
        if (records.isEmpty()) {
            TextView empty = text(getString(R.string.no_mods), 16, false);
            empty.setPadding(0, dp(16), 0, 0);
            modList.addView(empty);
            return;
        }
        for (ModRegistry.Record record : records) {
            modList.addView(createModCard(record, game.isVerified()), matchWidth());
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
            operationStatus.setText(message);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            refresh();
        });
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
