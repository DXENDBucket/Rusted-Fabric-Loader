package io.github.endx.rustedfabric.android.inspector;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ApkInspectorMain {
    private ApkInspectorMain() {
    }

    public static void main(String[] args) throws Exception {
        Path apk = null;
        Path profiles = null;
        Path output = null;
        for (int i = 0; i < args.length; i++) {
            String argument = args[i];
            if ("--help".equals(argument) || "-h".equals(argument)) {
                usage();
                return;
            }
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("Missing value for " + argument);
            }
            Path value = Paths.get(args[++i]);
            if ("--apk".equals(argument)) {
                apk = value;
            } else if ("--profiles".equals(argument)) {
                profiles = value;
            } else if ("--output".equals(argument)) {
                output = value;
            } else {
                throw new IllegalArgumentException("Unknown argument: " + argument);
            }
        }
        if (apk == null) {
            throw new IllegalArgumentException("--apk is required");
        }
        String json = new ApkInspector().inspect(apk, profiles).toJson();
        if (output == null) {
            System.out.print(json);
        } else {
            Path parent = output.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(output, json.getBytes(StandardCharsets.UTF_8));
            System.out.println("Wrote privacy-safe compatibility report (input path omitted): "
                    + output.getFileName());
        }
    }

    private static void usage() {
        System.out.println("Usage: apk-inspector --apk <file> [--profiles <directory>] [--output <file>]");
    }
}
