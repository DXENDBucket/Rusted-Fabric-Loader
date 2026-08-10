package io.github.endx.vulkanmod.mixin;

import io.github.endx.vulkanmod.VulkanRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Confirms the RFL renderer decision before Slick attempts Display.create(). */
@Mixin(targets = "org.newdawn.slick.AppGameContainer", remap = false)
public abstract class AppGameContainerRendererBootstrapMixin {
    @Inject(method = "setup()V", at = @At("HEAD"), require = 1)
    private void vulkanmod$beforeDisplayCreation(CallbackInfo callback) {
        int[] size = targetDisplaySize(this);
        VulkanRuntime.beforeLegacyDisplayCreation(size[0], size[1]);
    }

    private static int[] targetDisplaySize(Object container) {
        try {
            Field field = findField(container.getClass(), "targetDisplayMode");
            field.setAccessible(true);
            Object mode = field.get(container);
            if (mode == null) return new int[] { 640, 480 };
            Method width = mode.getClass().getMethod("getWidth");
            Method height = mode.getClass().getMethod("getHeight");
            return new int[] {
                    ((Number) width.invoke(mode)).intValue(),
                    ((Number) height.invoke(mode)).intValue()
            };
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not inspect Slick target display mode", failure);
        }
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
