package io.github.endx.rustedfabric.android.launcher.ui;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

import io.github.endx.rustedfabric.android.jvm.JvmLaunchPlan;

/** Persistent, launcher-owned game and renderer options applied on the next JVM start. */
final class GameLaunchPreferences {
    static final String PREFERENCES = "game_launch";
    static final String KEY_RENDERER = "renderer";
    static final String KEY_MAX_FPS = "max_fps";
    static final String KEY_VULKAN_PROFILE = "vulkan_profile";

    static final String RENDERER_VULKAN = "vulkan";
    static final String RENDERER_OPENGL = "opengl";
    static final String PROFILE_MEMORY = "memory";
    static final String PROFILE_BALANCED = "balanced";
    static final String PROFILE_THROUGHPUT = "throughput";

    private GameLaunchPreferences() {
    }

    static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    static String renderer(SharedPreferences preferences) {
        String value = preferences.getString(KEY_RENDERER, RENDERER_VULKAN);
        return RENDERER_OPENGL.equals(value) ? RENDERER_OPENGL : RENDERER_VULKAN;
    }

    static int maximumFps(SharedPreferences preferences) {
        int value = preferences.getInt(KEY_MAX_FPS, 0);
        return value == 300 ? value : 0;
    }

    static String vulkanProfile(SharedPreferences preferences) {
        String value = preferences.getString(KEY_VULKAN_PROFILE, PROFILE_BALANCED);
        if (PROFILE_MEMORY.equals(value) || PROFILE_THROUGHPUT.equals(value)) return value;
        return PROFILE_BALANCED;
    }

    static JvmLaunchPlan apply(Context context, JvmLaunchPlan plan) {
        SharedPreferences preferences = preferences(context);
        List<String> arguments = new ArrayList<>();
        String renderer = renderer(preferences);
        arguments.add("-Drusted.fabric.renderer=" + renderer);
        arguments.add("-Drusted.fabric.vulkan.mode="
                + (RENDERER_OPENGL.equals(renderer) ? "off" : "native"));
        arguments.add("-Drusted.fabric.maxFps=" + maximumFps(preferences));
        switch (vulkanProfile(preferences)) {
            case PROFILE_MEMORY:
                arguments.add("-Drusted.fabric.vulkan.frameArenaMiB=8");
                arguments.add("-Drusted.fabric.vulkan.resourceArenaMiB=8");
                arguments.add("-Drusted.fabric.vulkan.resourceArenaCount=2");
                break;
            case PROFILE_THROUGHPUT:
                arguments.add("-Drusted.fabric.vulkan.frameArenaMiB=32");
                arguments.add("-Drusted.fabric.vulkan.resourceArenaMiB=32");
                arguments.add("-Drusted.fabric.vulkan.resourceArenaCount=4");
                break;
            default:
                arguments.add("-Drusted.fabric.vulkan.frameArenaMiB=16");
                arguments.add("-Drusted.fabric.vulkan.resourceArenaMiB=16");
                arguments.add("-Drusted.fabric.vulkan.resourceArenaCount=3");
                break;
        }
        return plan.withAdditionalVirtualMachineArguments(arguments);
    }

    static void reset(Context context) {
        preferences(context).edit().clear().apply();
    }
}
