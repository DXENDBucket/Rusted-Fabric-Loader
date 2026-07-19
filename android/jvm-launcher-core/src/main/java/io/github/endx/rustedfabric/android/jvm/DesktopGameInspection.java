package io.github.endx.rustedfabric.android.jvm;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of checking a user-owned Rusted Warfare desktop installation. */
public final class DesktopGameInspection {
    private final Path root;
    private final List<String> errors;
    private final List<String> warnings;

    DesktopGameInspection(Path root, List<String> errors, List<String> warnings) {
        this.root = root;
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
        this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    public Path root() {
        return root;
    }

    public boolean isImportable() {
        return errors.isEmpty();
    }

    public List<String> errors() {
        return errors;
    }

    public List<String> warnings() {
        return warnings;
    }
}
