package io.github.endx.rustedfabric.android.launcher.jvm;

import android.app.ActivityManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Debug;
import android.os.Process;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.github.endx.rustedfabric.android.jvm.DesktopGameInspection;
import io.github.endx.rustedfabric.android.jvm.DesktopGameLayout;
import io.github.endx.rustedfabric.android.jvm.ManagedContentLibrary;
import io.github.endx.rustedfabric.android.launcher.BuildConfig;
import io.github.endx.rustedfabric.android.launcher.ui.JvmRenderActivity;

/** Creates a privacy-conscious snapshot that remains usable when the game process is frozen. */
public final class DiagnosticReportExporter {
    private static final long MAX_LOG_BYTES = 8L * 1024L * 1024L;
    private static final int MAX_PROCESS_OUTPUT_BYTES = 4 * 1024 * 1024;
    private static final int MAX_THREADS = 512;

    private DiagnosticReportExporter() {
    }

    public static String suggestedFileName() {
        String time = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        return "rusted-fabric-report-" + time + ".zip";
    }

    public static Result export(Context context, Uri destination) throws IOException {
        if (destination == null) throw new IOException("No report destination was selected");
        List<AppProcess> processes = findAppProcesses(context);
        AppProcess gameProcess = null;
        for (AppProcess process : processes) {
            if (process.name.equals(context.getPackageName() + ":desktop_jvm")) {
                gameProcess = process;
                break;
            }
        }

        OutputStream raw = context.getContentResolver().openOutputStream(destination, "wt");
        if (raw == null) throw new IOException("Cannot open the selected report destination");
        int entries = 0;
        try (ZipOutputStream zip = new ZipOutputStream(raw)) {
            putText(zip, "report.txt", buildOverview(context, processes, gameProcess));
            entries++;
            putText(zip, "content/content-inventory.txt", buildContentInventory(context));
            entries++;
            putText(zip, "process/app-processes.txt", buildProcessList(processes));
            entries++;

            for (AppProcess process : processes) {
                putText(zip, "process/" + safeName(process.name) + "-proc.txt",
                        captureProcSnapshot(process));
                entries++;
                String logcat = captureLogcat(process.pid);
                if (!logcat.isEmpty()) {
                    putText(zip, "logs/logcat-" + safeName(process.name) + ".txt", logcat);
                    entries++;
                }
            }

            File gameRoot = DesktopGameImportService.importedRoot(context);
            File jvmLog = new File(gameRoot, ".rustedfabricloader/android-jvm.log");
            if (jvmLog.isFile()) {
                putFileTail(zip, "logs/android-jvm.log", jvmLog.toPath(), MAX_LOG_BYTES);
                entries++;
            }
            entries += addCrashLogs(zip, gameRoot.toPath());
        } catch (SecurityException denied) {
            throw new IOException("Android denied access to the report destination", denied);
        }
        return new Result(entries, gameProcess != null);
    }

    private static String buildOverview(Context context, List<AppProcess> processes,
                                        AppProcess gameProcess) {
        StringBuilder out = new StringBuilder(2048);
        out.append("Rusted Fabric Android diagnostic report\n")
                .append("Generated: ").append(new Date()).append('\n')
                .append("Privacy: contains logs, device details and content names, but no game or mod files.\n\n")
                .append("Launcher version: ").append(BuildConfig.VERSION_NAME)
                .append(" (").append(BuildConfig.VERSION_CODE).append(")\n")
                .append("Package: ").append(context.getPackageName()).append('\n')
                .append("Device: ").append(Build.MANUFACTURER).append(' ')
                .append(Build.MODEL).append(" / ").append(Build.DEVICE).append('\n')
                .append("Android: ").append(Build.VERSION.RELEASE)
                .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
                .append("ABIs: ").append(Arrays.toString(Build.SUPPORTED_ABIS)).append('\n')
                .append("Locale: ").append(Locale.getDefault()).append('\n')
                .append("Available processors: ").append(Runtime.getRuntime().availableProcessors())
                .append("\n\nGame process: ")
                .append(gameProcess == null ? "not found" : gameProcess.name + " pid=" + gameProcess.pid)
                .append('\n')
                .append("Runtime status: ").append(singleLine(JvmHostService.lastStatus(context)))
                .append('\n')
                .append("Renderer/game status: ")
                .append(singleLine(JvmRenderActivity.lastStatus(context))).append('\n');

        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
            manager.getMemoryInfo(memory);
            out.append("System memory available/total: ").append(memory.availMem).append('/')
                    .append(memory.totalMem).append(" bytes; lowMemory=")
                    .append(memory.lowMemory).append("; threshold=").append(memory.threshold)
                    .append('\n');
            int[] pids = new int[processes.size()];
            for (int index = 0; index < processes.size(); index++) pids[index] = processes.get(index).pid;
            if (pids.length > 0) {
                Debug.MemoryInfo[] details = manager.getProcessMemoryInfo(pids);
                for (int index = 0; index < details.length && index < processes.size(); index++) {
                    out.append("Process PSS ").append(processes.get(index).name).append(": ")
                            .append(details[index].getTotalPss()).append(" KiB\n");
                }
            }
        }

        File gameRoot = DesktopGameImportService.importedRoot(context);
        DesktopGameInspection inspection = DesktopGameLayout.inspect(gameRoot.toPath());
        out.append("\nImported game root exists: ").append(gameRoot.isDirectory()).append('\n')
                .append("Game layout importable: ").append(inspection.isImportable()).append('\n')
                .append("Game layout errors: ").append(inspection.errors()).append('\n')
                .append("Game layout warnings: ").append(inspection.warnings()).append('\n')
                .append("Shared content access: ").append(SharedContentWorkspace.hasStorageAccess(context))
                .append('\n')
                .append("Shared content ready: ").append(SharedContentWorkspace.isReady(context))
                .append('\n')
                .append("Shared content root: ").append(SharedContentWorkspace.root()).append('\n');
        return out.toString();
    }

    private static String buildContentInventory(Context context) {
        StringBuilder out = new StringBuilder(2048);
        out.append("Only names and metadata are listed; content files are not included.\n");
        if (!SharedContentWorkspace.hasStorageAccess(context)) {
            return out.append("Shared storage permission is not available.\n").toString();
        }
        Path gameRoot = DesktopGameImportService.importedRoot(context).toPath();
        try {
            SharedContentWorkspace.configureManagedContent();
            for (ManagedContentLibrary.Kind kind : ManagedContentLibrary.Kind.values()) {
                out.append("\n[").append(kind).append("]\n");
                for (ManagedContentLibrary.Item item : ManagedContentLibrary.list(gameRoot, kind)) {
                    out.append(item.enabled() ? "enabled" : "disabled")
                            .append(item.official() ? " official" : " user")
                            .append(" | name=").append(item.name())
                            .append(" | id=").append(item.id())
                            .append(" | version=").append(item.version())
                            .append(" | entry=").append(item.path().getFileName())
                            .append(" | size=").append(safeSize(item.path()))
                            .append(" | modified=").append(safeModified(item.path()))
                            .append(" | detail=").append(singleLine(item.detail())).append('\n');
                }
            }
        } catch (Throwable failure) {
            out.append("Inventory failed: ").append(safeMessage(failure)).append('\n');
        }
        return out.toString();
    }

    private static List<AppProcess> findAppProcesses(Context context) {
        List<AppProcess> result = new ArrayList<>();
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> running = manager == null
                ? null : manager.getRunningAppProcesses();
        String packagePrefix = context.getPackageName();
        if (running != null) {
            for (ActivityManager.RunningAppProcessInfo process : running) {
                if (process.processName != null
                        && (process.processName.equals(packagePrefix)
                        || process.processName.startsWith(packagePrefix + ":"))) {
                    result.add(new AppProcess(process.processName, process.pid, process.importance));
                }
            }
        }
        boolean currentFound = false;
        for (AppProcess process : result) {
            if (process.pid == Process.myPid()) currentFound = true;
        }
        if (!currentFound) result.add(new AppProcess(packagePrefix, Process.myPid(), -1));
        return result;
    }

    private static String buildProcessList(List<AppProcess> processes) {
        StringBuilder out = new StringBuilder();
        for (AppProcess process : processes) {
            out.append(process.name).append(" pid=").append(process.pid)
                    .append(" importance=").append(process.importance).append('\n');
        }
        return out.toString();
    }

    private static String captureProcSnapshot(AppProcess process) {
        StringBuilder out = new StringBuilder(64 * 1024);
        Path proc = Paths.get("/proc", Integer.toString(process.pid));
        out.append("process=").append(process.name).append(" pid=").append(process.pid).append('\n');
        appendProcFile(out, proc.resolve("status"), "process status");
        appendProcFile(out, proc.resolve("stat"), "process stat");
        appendProcFile(out, proc.resolve("wchan"), "process wchan");
        Path tasks = proc.resolve("task");
        int count = 0;
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(tasks)) {
            for (Path task : entries) {
                if (count++ >= MAX_THREADS) {
                    out.append("\nThread list truncated after ").append(MAX_THREADS).append(" entries.\n");
                    break;
                }
                out.append("\n--- thread ").append(task.getFileName()).append(" ---\n");
                appendProcFile(out, task.resolve("status"), null);
                appendProcFile(out, task.resolve("stat"), null);
                appendProcFile(out, task.resolve("wchan"), null);
            }
        } catch (Throwable failure) {
            out.append("\nCannot list threads: ").append(safeMessage(failure)).append('\n');
        }
        return out.toString();
    }

    private static void appendProcFile(StringBuilder out, Path path, String heading) {
        try {
            if (heading != null) out.append("\n--- ").append(heading).append(" ---\n");
            byte[] bytes = Files.readAllBytes(path);
            out.append(new String(bytes, StandardCharsets.UTF_8));
            if (bytes.length > 0 && bytes[bytes.length - 1] != '\n') out.append('\n');
        } catch (Throwable failure) {
            out.append("Cannot read ").append(path.getFileName()).append(": ")
                    .append(safeMessage(failure)).append('\n');
        }
    }

    private static String captureLogcat(int pid) {
        java.lang.Process child = null;
        try {
            child = new ProcessBuilder("logcat", "-d", "-v", "threadtime", "-t", "2000",
                    "--pid=" + pid).redirectErrorStream(true).start();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (InputStream input = child.getInputStream()) {
                byte[] buffer = new byte[16 * 1024];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    int accepted = Math.min(read, MAX_PROCESS_OUTPUT_BYTES - output.size());
                    if (accepted > 0) output.write(buffer, 0, accepted);
                    if (output.size() >= MAX_PROCESS_OUTPUT_BYTES) {
                        child.destroy();
                        break;
                    }
                }
            }
            child.waitFor(5, TimeUnit.SECONDS);
            return output.toString(StandardCharsets.UTF_8.name());
        } catch (Throwable failure) {
            return "logcat capture failed: " + safeMessage(failure) + "\n";
        } finally {
            if (child != null && child.isAlive()) child.destroyForcibly();
        }
    }

    private static int addCrashLogs(ZipOutputStream zip, Path gameRoot) throws IOException {
        if (!Files.isDirectory(gameRoot)) return 0;
        int added = 0;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(gameRoot, "hs_err_pid*.log")) {
            for (Path file : files) {
                if (added >= 8) break;
                if (!Files.isRegularFile(file)) continue;
                putFileTail(zip, "logs/" + safeName(file.getFileName().toString()), file,
                        MAX_LOG_BYTES);
                added++;
            }
        }
        return added;
    }

    private static void putText(ZipOutputStream zip, String name, String text) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(text.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static void putFileTail(ZipOutputStream zip, String name, Path source, long maxBytes)
            throws IOException {
        long size = Files.size(source);
        long skip = Math.max(0L, size - maxBytes);
        zip.putNextEntry(new ZipEntry(name));
        if (skip > 0L) {
            zip.write(("[Earlier bytes omitted; original size=" + size + "]\n")
                    .getBytes(StandardCharsets.UTF_8));
        }
        try (InputStream input = Files.newInputStream(source, StandardOpenOption.READ)) {
            while (skip > 0L) {
                long skipped = input.skip(skip);
                if (skipped <= 0L) {
                    if (input.read() < 0) break;
                    skipped = 1L;
                }
                skip -= skipped;
            }
            byte[] buffer = new byte[32 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) zip.write(buffer, 0, read);
        }
        zip.closeEntry();
    }

    private static String safeName(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String singleLine(String value) {
        if (value == null || value.trim().isEmpty()) return "none";
        return value.replace('\r', ' ').replace('\n', ' ').trim();
    }

    private static long safeSize(Path path) {
        try {
            return Files.isRegularFile(path) ? Files.size(path) : -1L;
        } catch (IOException ignored) {
            return -1L;
        }
    }

    private static long safeModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ignored) {
            return -1L;
        }
    }

    private static String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.trim().isEmpty()
                ? failure.getClass().getSimpleName() : message;
    }

    private static final class AppProcess {
        final String name;
        final int pid;
        final int importance;

        AppProcess(String name, int pid, int importance) {
            this.name = name;
            this.pid = pid;
            this.importance = importance;
        }
    }

    public static final class Result {
        private final int entries;
        private final boolean gameProcessCaptured;

        Result(int entries, boolean gameProcessCaptured) {
            this.entries = entries;
            this.gameProcessCaptured = gameProcessCaptured;
        }

        public int entries() {
            return entries;
        }

        public boolean gameProcessCaptured() {
            return gameProcessCaptured;
        }
    }
}
