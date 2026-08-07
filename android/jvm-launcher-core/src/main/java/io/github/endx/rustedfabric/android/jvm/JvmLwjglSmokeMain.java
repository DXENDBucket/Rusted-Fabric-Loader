package io.github.endx.rustedfabric.android.jvm;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Exercises the same LWJGL2 entry points used by the desktop game without linking game code. */
public final class JvmLwjglSmokeMain {
    private JvmLwjglSmokeMain() {
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one result-file path");
        }
        Path result = Paths.get(arguments[0]).toAbsolutePath().normalize();
        try {
            run(result);
        } catch (Throwable failure) {
            Throwable cause = failure;
            while (cause instanceof InvocationTargetException
                    && ((InvocationTargetException) cause).getTargetException() != null) {
                cause = ((InvocationTargetException) cause).getTargetException();
            }
            String report = "rusted-fabric-lwjgl2-smoke=failed\n"
                    + "exception=" + cause.getClass().getName() + ": "
                    + String.valueOf(cause.getMessage()) + "\n";
            Files.write(result, report.getBytes(StandardCharsets.UTF_8));
            throw new RuntimeException(report.trim(), cause);
        }
    }

    private static void run(Path result) throws Exception {
        Class<?> display = Class.forName("org.lwjgl.opengl.Display");
        Class<?> gl11 = Class.forName("org.lwjgl.opengl.GL11");
        Method create = display.getMethod("create");
        Method glGetString = gl11.getMethod("glGetString", int.class);
        Method glClearColor = gl11.getMethod(
                "glClearColor", float.class, float.class, float.class, float.class);
        Method glClear = gl11.getMethod("glClear", int.class);
        Method swapBuffers = display.getMethod("swapBuffers");

        create.invoke(null);
        glClearColor.invoke(null, 0.31f, 0.12f, 0.42f, 1.0f);
        glClear.invoke(null, 0x00004000 | 0x00000100);
        swapBuffers.invoke(null);
        String vendor = String.valueOf(glGetString.invoke(null, 0x1F00));
        String renderer = String.valueOf(glGetString.invoke(null, 0x1F01));
        String version = String.valueOf(glGetString.invoke(null, 0x1F02));
        if ("null".equals(renderer) || "null".equals(version)) {
            throw new IllegalStateException("GL4ES has no current backing GLES context");
        }

        String report = "rusted-fabric-lwjgl2-smoke=ok\n"
                + "gl.vendor=" + vendor + "\n"
                + "gl.renderer=" + renderer + "\n"
                + "gl.version=" + version + "\n";
        Files.write(result, report.getBytes(StandardCharsets.UTF_8));
        System.out.print(report);
    }
}
