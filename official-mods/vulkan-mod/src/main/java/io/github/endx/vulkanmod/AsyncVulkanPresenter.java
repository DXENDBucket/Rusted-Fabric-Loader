package io.github.endx.vulkanmod;

import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanSurfaceInfo;
import io.github.endx.vulkanmod.spi.VulkanTextureData;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Keeps Vulkan WSI waits off the Slick/LWJGL 2 window thread.
 *
 * <p>Only the newest complete frame is retained. A slow or temporarily blocked presentation
 * therefore cannot build a queue that the renderer must drain after an Alt-Tab or restore.</p>
 */
final class AsyncVulkanPresenter {
    interface Listener {
        void presented(VulkanSurfaceInfo surface, Submission submission);

        void failed(Throwable failure);
    }

    interface TextureUploadListener {
        void uploaded(long textureHandle);

        void failed(Throwable failure);
    }

    static final class Submission {
        private final VulkanFrameCommands frame;
        private final boolean reveal;
        private final int capturedCommands;
        private final int unsupportedCommands;

        private Submission(VulkanFrameCommands frame, boolean reveal,
                           int capturedCommands, int unsupportedCommands) {
            this.frame = frame;
            this.reveal = reveal;
            this.capturedCommands = capturedCommands;
            this.unsupportedCommands = unsupportedCommands;
        }

        VulkanFrameCommands frame() { return frame; }
        boolean reveal() { return reveal; }
        int capturedCommands() { return capturedCommands; }
        int unsupportedCommands() { return unsupportedCommands; }
    }

    private final Object lock = new Object();
    private final VulkanDriverLoader.LoadedDriver driver;
    private final Listener listener;
    private final Thread thread;
    private boolean running = true;
    private final Deque<Object> work = new ArrayDeque<Object>();

    AsyncVulkanPresenter(VulkanDriverLoader.LoadedDriver driver, Listener listener) {
        this.driver = driver;
        this.listener = listener;
        thread = new Thread(this::run, "RustedVK Presenter");
        thread.setDaemon(true);
        thread.start();
    }

    void offer(VulkanFrameCommands frame, boolean reveal,
               int capturedCommands, int unsupportedCommands) {
        synchronized (lock) {
            if (!running) return;
            // While the first reveal is pending, replacing it must not accidentally turn the
            // replacement into a hidden present and leave takeover permanently unarmed.
            Object last = work.peekLast();
            Submission previous = last instanceof Submission ? (Submission) last : null;
            boolean mustReveal = reveal || (previous != null && previous.reveal());
            if (previous != null) work.removeLast();
            work.addLast(new Submission(
                    frame, mustReveal, capturedCommands, unsupportedCommands));
            lock.notifyAll();
        }
    }

    void destroyTexture(long textureHandle) {
        if (textureHandle == 0L) return;
        synchronized (lock) {
            if (!running) return;
            // This boundary is intentionally not coalesced with frames on either side. Every
            // frame queued before it may still reference the handle; frames after it cannot.
            work.addLast(new TextureDestruction(textureHandle));
            lock.notifyAll();
        }
    }

    void uploadTexture(VulkanTextureData texture, TextureUploadListener uploadListener) {
        synchronized (lock) {
            if (!running) return;
            // Upload is an ordering boundary: frames after this item may use the new handle,
            // while frames before it remain valid with their existing handles.
            work.addLast(new TextureUpload(texture, uploadListener));
            lock.notifyAll();
        }
    }

    boolean stopAndWait(long timeoutMillis) {
        synchronized (lock) {
            running = false;
            work.clear();
            lock.notifyAll();
        }
        thread.interrupt();
        if (Thread.currentThread() == thread) return true;
        try {
            thread.join(timeoutMillis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        return !thread.isAlive();
    }

    private void run() {
        while (true) {
            Object next;
            synchronized (lock) {
                while (running && work.isEmpty()) {
                    try {
                        lock.wait();
                    } catch (InterruptedException ignored) {
                        // Re-check running. Interrupt is also used to make shutdown prompt.
                    }
                }
                if (!running) return;
                next = work.removeFirst();
            }
            try {
                if (next instanceof TextureDestruction) {
                    driver.destroyTexture(((TextureDestruction) next).textureHandle);
                    continue;
                }
                if (next instanceof TextureUpload) {
                    TextureUpload upload = (TextureUpload) next;
                    try {
                        upload.listener.uploaded(driver.uploadTexture(upload.texture));
                    } catch (Throwable failure) {
                        upload.listener.failed(failure);
                        listener.failed(failure);
                    }
                    continue;
                }
                Submission submission = (Submission) next;
                // Native window operations are deliberately absent here. ShowWindow and
                // SetWindowPos can synchronously call the owner thread and deadlock if that
                // thread is also waiting for this driver's Vulkan lock.
                VulkanSurfaceInfo surface = driver.presentFrame(submission.frame());
                if (surface != null) listener.presented(surface, submission);
            } catch (Throwable failure) {
                listener.failed(failure);
            }
        }
    }

    private static final class TextureDestruction {
        private final long textureHandle;

        private TextureDestruction(long textureHandle) {
            this.textureHandle = textureHandle;
        }
    }

    private static final class TextureUpload {
        private final VulkanTextureData texture;
        private final TextureUploadListener listener;

        private TextureUpload(VulkanTextureData texture, TextureUploadListener listener) {
            this.texture = texture;
            this.listener = listener;
        }
    }
}
