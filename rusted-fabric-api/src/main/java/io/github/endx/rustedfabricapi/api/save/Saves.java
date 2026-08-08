package io.github.endx.rustedfabricapi.api.save;

import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import rustedwarfare.core.GameEngine;
import rustedwarfare.save.GameSaver;

import java.io.File;
import java.util.Locale;
import java.util.Objects;

/** Safe high-level access to ordinary local {@code .rwsave} files. */
public final class Saves {
    public static final String EXTENSION = ".rwsave";

    private Saves() {
    }

    public static GameSaver manager() {
        GameEngine engine = RustedWarfareClient.requireEngine();
        if (engine.gameSaver == null) {
            throw new IllegalStateException("The game save manager is not initialized");
        }
        return engine.gameSaver;
    }

    /** Returns a safe leaf filename and appends {@value #EXTENSION} when absent. */
    public static String normalizeName(String name) {
        Objects.requireNonNull(name, "name");
        String checked = name.trim();
        if (checked.isEmpty()) throw new IllegalArgumentException("save name must not be blank");
        if (checked.length() > 160) throw new IllegalArgumentException("save name is too long");
        if (checked.equals(".") || checked.equals("..") || checked.contains("..")
                || checked.indexOf('/') >= 0 || checked.indexOf('\\') >= 0
                || checked.indexOf(':') >= 0 || checked.indexOf('\0') >= 0
                || checked.indexOf('<') >= 0 || checked.indexOf('>') >= 0
                || checked.indexOf('"') >= 0 || checked.indexOf('|') >= 0
                || checked.indexOf('?') >= 0 || checked.indexOf('*') >= 0) {
            throw new IllegalArgumentException("save name must be a plain filename: " + checked);
        }
        for (int i = 0; i < checked.length(); i++) {
            if (Character.isISOControl(checked.charAt(i))) {
                throw new IllegalArgumentException("save name contains a control character");
            }
        }
        return checked.toLowerCase(Locale.ROOT).endsWith(EXTENSION)
                ? checked : checked + EXTENSION;
    }

    public static File file(String name) {
        return manager().getSaveFile(normalizeName(name), false);
    }

    public static boolean exists(String name) {
        File file = file(name);
        return file != null && file.isFile();
    }

    /** Saves through the game's temporary-file and replacement path. Run on the update thread. */
    public static void save(String name) {
        save(name, false);
    }

    public static void save(String name, boolean automatic) {
        manager().saveGameToFile(normalizeName(name), automatic);
    }

    /** Loads an ordinary local save with the same options used by the desktop save menu. */
    public static boolean load(String name) {
        return manager().loadGameFromFile(normalizeName(name), false);
    }

    public static boolean delete(String name) {
        return manager().deleteSave(normalizeName(name));
    }
}
