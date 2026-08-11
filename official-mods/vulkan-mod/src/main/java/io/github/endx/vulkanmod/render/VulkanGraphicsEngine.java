package io.github.endx.vulkanmod.render;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint$Align;
import android.graphics.Paint$Style;
import android.graphics.Bitmap;
import io.github.endx.vulkanmod.VulkanRuntime;
import io.github.endx.vulkanmod.spi.VulkanBlendMode;
import io.github.endx.vulkanmod.spi.VulkanBuiltInShaders;
import io.github.endx.vulkanmod.spi.VulkanClipRect;
import io.github.endx.vulkanmod.spi.VulkanColoredQuad;
import io.github.endx.vulkanmod.spi.VulkanDrawState;
import io.github.endx.vulkanmod.spi.VulkanFrameCommands;
import io.github.endx.vulkanmod.spi.VulkanTextureFilter;
import io.github.endx.vulkanmod.spi.VulkanTexturedQuad;
import io.github.endx.vulkanmod.spi.VulkanTransform2D;
import io.github.endx.vulkanmod.spi.VulkanShaderState;
import rustedwarfare.client.render.GameImage;
import rustedwarfare.client.render.SlickGraphicsBackend;
import rustedwarfare.render.AndroidGlRenderer;
import rustedwarfare.render.CanvasDrawTarget;
import rustedwarfare.render.DrawTimeCallback;
import rustedwarfare.render.GraphicsEngine;
import rustedwarfare.render.ShaderProgram;
import rustedwarfare.render.ShaderParameter;
import rustedwarfare.render.UniquePaint;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.WritableRaster;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import javax.imageio.ImageIO;
import java.util.concurrent.locks.Lock;
import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.Deque;
import rustedwarfare.util.CommonUtils;
import com.corrodinggames.rts.R$drawable;

/**
 * Renderer selected by the game in native Vulkan mode.
 *
 * <p>The root engine records directly into the native Vulkan frame owned by {@link VulkanRuntime}.
 * Child engines own Vulkan render targets with on-demand CPU readback for legacy pixel access,
 * without creating a Slick renderer, LWJGL Display, or OpenGL context.</p>
 */
public final class VulkanGraphicsEngine implements GraphicsEngine {
    private static boolean pointerDrawLogged;
    private final GraphicsEngine delegate;
    private final SlickGraphicsBackend slickDelegate;
    private final GameImage renderTarget;
    private final long nativeRenderTargetHandle;
    private final boolean ownsNativeRenderTarget;
    private VulkanFrameCommands.Builder offscreenBuilder;
    private int width;
    private int height;
    private GameImage errorImage;
    private VulkanTransform2D transform = VulkanTransform2D.IDENTITY;
    private VulkanClipRect clip;
    private BufferedImage cpuBufferedImage;
    private Graphics2D persistentCpuGraphics;
    private final Deque<NativeState> stateStack = new ArrayDeque<NativeState>();
    private final CanvasDrawTarget nativeCanvasTarget = new NativeCanvasTarget();

    public VulkanGraphicsEngine() {
        this.delegate = null;
        this.slickDelegate = null;
        this.renderTarget = null;
        this.nativeRenderTargetHandle = 0L;
        this.ownsNativeRenderTarget = false;
        VulkanRuntime.onGraphicsEngineInstalled(this);
    }

    private VulkanGraphicsEngine(GameImage renderTarget) {
        if (renderTarget == null) throw new NullPointerException("renderTarget");
        this.delegate = null;
        this.slickDelegate = null;
        this.renderTarget = renderTarget.getRealImage();
        this.width = renderTarget.getWidth();
        this.height = renderTarget.getHeight();
        long existing = this.renderTarget instanceof VulkanGameImage
                ? ((VulkanGameImage) this.renderTarget).nativeRenderTargetHandle() : 0L;
        this.nativeRenderTargetHandle = existing != 0L ? existing
                : VulkanRuntime.createNativeRenderTarget(width, height);
        this.ownsNativeRenderTarget = existing == 0L;
        if (this.renderTarget instanceof VulkanGameImage) {
            VulkanGameImage nativeImage = (VulkanGameImage) this.renderTarget;
            nativeImage.setNativeRenderTargetHandle(nativeRenderTargetHandle);
            nativeImage.setNativeRenderTargetFlusher(this::submitOffscreen);
            nativeImage.setNativeRenderTargetBackend(this);
        }
        resetOffscreenBuilder();
        offscreenBuilder.clear(0.0f, 0.0f, 0.0f, 0.0f);
    }

    public SlickGraphicsBackend compatibilityDelegate() {
        return slickDelegate;
    }

    /** Draws a top-level native image in physical screen space, independent of game transforms. */
    public void drawScreenImageRaw(GameImage image, float x, float y, Paint paint) {
        if (!nativeRoot()) return;
        VulkanTransform2D previousTransform = transform;
        VulkanClipRect previousClip = clip;
        try {
            transform = VulkanTransform2D.IDENTITY;
            clip = null;
            nativeImage(image, full(image), x, y,
                    x + image.getWidth(), y + image.getHeight(), paint, null);
        } finally {
            transform = previousTransform;
            clip = previousClip;
        }
    }

    public static GraphicsEngine unwrapCompatibility(GraphicsEngine engine) {
        if (!(engine instanceof VulkanGraphicsEngine)) return engine;
        GraphicsEngine delegate = ((VulkanGraphicsEngine) engine).delegate;
        return delegate == null ? engine : delegate;
    }

    private boolean nativeRoot() {
        return renderTarget == null && VulkanRuntime.isNativeRendererSelected();
    }

    private boolean cpuTarget() {
        // Retained as the historical child-target predicate while the remaining pixel-readback
        // helpers are migrated. Native children now record GPU commands before any CPU helper.
        return renderTarget != null;
    }

    private boolean nativeTarget() {
        return renderTarget != null && nativeRenderTargetHandle != 0L;
    }

    private void resetOffscreenBuilder() {
        if (!nativeTarget()) return;
        offscreenBuilder = VulkanFrameCommands.builder(width, height);
    }

    private void submitOffscreen() {
        if (nativeTarget() && offscreenBuilder != null) {
            VulkanFrameCommands pending = offscreenBuilder.build();
            if (pending.clearRequested() || !pending.commands().isEmpty()) {
                VulkanRuntime.renderNativeTarget(nativeRenderTargetHandle, pending);
                if (renderTarget instanceof VulkanGameImage) {
                    ((VulkanGameImage) renderTarget).markNativePixelsDirty();
                }
            }
            resetOffscreenBuilder();
        }
    }

    private void beforeNativeTargetMutation() {
        if (nativeTarget() && renderTarget instanceof VulkanGameImage) {
            ((VulkanGameImage) renderTarget).submitPendingNativeConsumers();
        }
    }

    private void recordColoredQuad(VulkanColoredQuad quad) {
        if (nativeTarget()) {
            beforeNativeTargetMutation();
            offscreenBuilder.coloredQuad(quad);
        }
        else VulkanRuntime.recordNativeColoredQuad(quad);
    }

    private void recordTexturedQuad(VulkanTexturedQuad quad) {
        if (nativeTarget()) {
            beforeNativeTargetMutation();
            offscreenBuilder.texturedQuad(quad);
        }
        else VulkanRuntime.recordNativeTexturedQuad(quad);
    }

    private VulkanDrawState state(Paint paint) {
        return state(paint, VulkanShaderState.DEFAULT);
    }

    private VulkanDrawState state(Paint paint, VulkanShaderState shaderState) {
        return new VulkanDrawState(transform, clip, VulkanBlendMode.NORMAL,
                paint != null && paint.c()
                        ? VulkanTextureFilter.LINEAR : VulkanTextureFilter.NEAREST,
                shaderState);
    }

    private static VulkanShaderState shaderState(GameImage image, Paint paint) {
        // Match SlickGraphicsBackend: a shader explicitly attached to UniquePaint overrides the
        // image shader. Team-color wrappers use the latter, post-processing composites the former.
        ShaderProgram shader = paint instanceof UniquePaint
                ? ((UniquePaint) paint).getShader() : null;
        if (shader == null) shader = image.getShader();
        if (shader == null) return VulkanShaderState.DEFAULT;
        shader.applyForImage(paint, image);
        String name = shader.name == null ? "" : shader.name;
        int effect = VulkanBuiltInShaders.effectForName(name);
        if (effect < 0) {
            return customShaderState(shader);
        }
        float red = 1.0f;
        float green = 1.0f;
        float blue = 1.0f;
        float alpha = 1.0f;
        float amount = 0.15f;
        long secondaryTexture = 0L;
        float screenBaseWidth = 1.0f;
        float screenBaseHeight = 1.0f;
        float resolutionWidth = 1.0f;
        float resolutionHeight = 1.0f;
        float displacementOffset = 0.0f;
        float uiScaling = 1.0f;
        for (ShaderParameter parameter : shader.parameters) {
            if (parameter == null) continue;
            if (effect == VulkanShaderState.POST_DISPLACEMENT
                    && parameter.texture != null) {
                GameImage parameterImage = parameter.texture.getRealImage();
                if (parameterImage == null) parameterImage = parameter.texture;
                if (parameterImage instanceof VulkanGameImage) {
                    ((VulkanGameImage) parameterImage).submitPendingNativeDraws();
                }
                if ("screenBase".equals(parameter.name) && !parameter.secondaryTexture) {
                    secondaryTexture = VulkanRuntime.textureForGameImage(parameterImage);
                    screenBaseWidth = Math.max(1, parameterImage.getWidth());
                    screenBaseHeight = Math.max(1, parameterImage.getHeight());
                } else if ("screenBaseSize".equals(parameter.name)
                        && parameter.secondaryTexture) {
                    screenBaseWidth = Math.max(1, parameterImage.getWidth());
                    screenBaseHeight = Math.max(1, parameterImage.getHeight());
                }
            }
            if (parameter.floatValues == null) continue;
            if ("teamColor".equals(parameter.name) && parameter.floatValues.length >= 4) {
                red = parameter.floatValues[0];
                green = parameter.floatValues[1];
                blue = parameter.floatValues[2];
                alpha = parameter.floatValues[3];
            } else if ("teamColorAmount".equals(parameter.name)
                    && parameter.floatValues.length >= 1) {
                amount = parameter.floatValues[0];
            } else if (effect == VulkanShaderState.POST_DISPLACEMENT
                    && "u_resolution".equals(parameter.name)
                    && parameter.floatValues.length >= 2) {
                resolutionWidth = positiveOr(parameter.floatValues[0], 1.0f);
                resolutionHeight = positiveOr(parameter.floatValues[1], 1.0f);
            } else if (effect == VulkanShaderState.POST_DISPLACEMENT
                    && "u_offsetBy".equals(parameter.name)
                    && parameter.floatValues.length >= 1) {
                displacementOffset = finiteOr(parameter.floatValues[0], 0.0f);
            } else if (effect == VulkanShaderState.POST_DISPLACEMENT
                    && "u_uiScaling".equals(parameter.name)
                    && parameter.floatValues.length >= 1) {
                uiScaling = positiveOr(parameter.floatValues[0], 1.0f);
            }
        }
        if (effect == VulkanShaderState.POST_DISPLACEMENT) {
            if (secondaryTexture == 0L) return VulkanShaderState.DEFAULT;
            return new VulkanShaderState(effect, red, green, blue, alpha, amount,
                    secondaryTexture, screenBaseWidth, screenBaseHeight,
                    resolutionWidth, resolutionHeight, displacementOffset, uiScaling);
        }
        return new VulkanShaderState(effect, red, green, blue, alpha, amount);
    }

    private static VulkanShaderState customShaderState(ShaderProgram shader) {
        try {
            NativeCustomShaderBinding binding = shader.backendShaderObject
                    instanceof NativeCustomShaderBinding
                    ? (NativeCustomShaderBinding) shader.backendShaderObject : null;
            if (binding == null || !binding.matches(shader) || shader.reloadPending) {
                if (binding != null) binding.destroy();
                binding = NativeCustomShaderBinding.compile(shader);
                shader.backendShaderObject = binding;
                shader.reloadPending = false;
                shader.compileFailureState = 0;
            }
            return binding.snapshot(shader);
        } catch (RuntimeException failure) {
            shader.logWarningAndMarkFailed("Vulkan compile failed: "
                    + failure.getMessage());
            return VulkanShaderState.DEFAULT;
        }
    }

    private static float finiteOr(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

    private static float positiveOr(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }

    private static float[] color(int argb) {
        return new float[] {
                ((argb >>> 16) & 255) / 255.0f,
                ((argb >>> 8) & 255) / 255.0f,
                (argb & 255) / 255.0f,
                ((argb >>> 24) & 255) / 255.0f
        };
    }

    private void nativeQuad(float x, float y, float width, float height, Paint paint) {
        if (width < 0.0f || height < 0.0f) return;
        float[] rgba = color(paint == null ? 0xffffffff : paint.e());
        recordColoredQuad(new VulkanColoredQuad(
                x, y, width, height, rgba[0], rgba[1], rgba[2], rgba[3], state(paint)));
    }

    private void nativeImage(GameImage image, Rect source, float left, float top,
                             float right, float bottom, Paint paint,
                             VulkanTransform2D localTransform) {
        if (image == null || source == null || right < left || bottom < top) return;
        GameImage real = image.getRealImage();
        if (real == null) real = image;
        boolean samplesNativeRenderTarget = real instanceof VulkanGameImage
                && ((VulkanGameImage) real).nativeRenderTargetHandle() != 0L;
        if (real instanceof VulkanGameImage && real != renderTarget) {
            // Slick makes render-to-texture draws visible before a later draw samples that image.
            // Native child backends batch commands, so explicitly close the producer pass here.
            ((VulkanGameImage) real).submitPendingNativeDraws();
        }
        // LazyTeamColorImage attaches its shared ShaderProgram while resolving the real source.
        // Snapshot uniforms afterwards, but still apply them against the wrapper that owns the
        // team color rather than against the unwrapped source image.
        VulkanShaderState shaderState = shaderState(image, paint);
        int imageWidth = real.getWidth();
        int imageHeight = real.getHeight();
        if (imageWidth <= 0 || imageHeight <= 0) return;
        long texture = VulkanRuntime.textureForGameImage(real);
        if (texture == 0L) return;
        if (!pointerDrawLogged && image.getName() != null
                && image.getName().toLowerCase(java.util.Locale.ROOT).contains("pointer")) {
            pointerDrawLogged = true;
            System.out.println("[Vulkan Mod] Native pointer recorded: " + image.getName()
                    + " " + imageWidth + "x" + imageHeight + " at " + left + "," + top
                    + ", texture=" + texture);
        }
        float[] tint = color(paint == null ? 0xffffffff : paint.e());
        VulkanDrawState base = state(paint, shaderState);
        VulkanDrawState drawState = localTransform == null
                ? base : new VulkanDrawState(localTransform.then(base.transform()), base.clip(),
                        base.blendMode(), base.textureFilter(), base.shaderState());
        recordTexturedQuad(new VulkanTexturedQuad(texture,
                left, top, right - left, bottom - top,
                source.a / (float) imageWidth, source.b / (float) imageHeight,
                source.c / (float) imageWidth, source.d / (float) imageHeight,
                tint[0], tint[1], tint[2], tint[3], drawState));
        if (nativeTarget() && samplesNativeRenderTarget && real != renderTarget) {
            // Delay the consumer normally, but make the source aware of it. If the game reuses
            // that scratch image, its next mutation submits this consumer first. Stable render
            // targets remain fully batched and avoid per-image queue-idle stalls during loading.
            ((VulkanGameImage) real).registerPendingNativeConsumer(this,
                    this::submitOffscreen);
        }
    }

    @Override public GraphicsEngine createBackendForImage(GameImage image) {
        return backendForImage(image);
    }

    @Override public GraphicsEngine createChildBackendForImage(GameImage image) {
        return backendForImage(image);
    }

    private VulkanGraphicsEngine backendForImage(GameImage image) {
        if (image == null) throw new NullPointerException("image");
        VulkanRuntime.markRenderTargetImage(image);
        GameImage real = image.getRealImage();
        if (real == null) real = image;
        if (real != image) VulkanRuntime.markRenderTargetImage(real);
        if (real instanceof VulkanGameImage) {
            VulkanGraphicsEngine existing = ((VulkanGameImage) real).nativeRenderTargetBackend();
            if (existing != null) return existing;
        }
        return new VulkanGraphicsEngine(real);
    }

    @Override public boolean a() { return renderTarget != null || nativeRoot() || delegate.a(); }
    @Override public void a(Context context) {
        // Desktop native loading resolves resource ids through CommonUtils.resourceIdToPath.
    }
    @Override public void b() {
        errorImage = a(R$drawable.error_outmem, true);
        errorImage.setName("Out of memory");
    }
    @Override public CanvasDrawTarget d() {
        return nativeRoot() || cpuTarget() ? nativeCanvasTarget : delegate.d();
    }
    @Override public void a(CanvasDrawTarget target) {
        if (!nativeRoot() && !cpuTarget()) delegate.a(target);
    }
    @Override public void a(AndroidGlRenderer renderer) {
        if (!nativeRoot() && !cpuTarget()) delegate.a(renderer);
    }
    @Override public GameImage a(int resourceId) { return a(resourceId, true); }
    @Override public GameImage a(int resourceId, boolean option) {
        String path = CommonUtils.resourceIdToPath(resourceId);
        try (InputStream input = new FileInputStream(path)) {
            GameImage image = loadImageFromStream(input, option);
            image.setName(path);
            return image;
        } catch (IOException failure) {
            throw new IllegalStateException("Could not load native image resource " + path,
                    failure);
        }
    }
    @Override public GameImage loadImageFromStream(InputStream input, boolean option) {
        if (input == null) throw new NullPointerException("input");
        try {
            BufferedImage decoded = ImageIO.read(input);
            if (decoded == null) throw new IOException("unsupported image format");
            int width = decoded.getWidth();
            int height = decoded.getHeight();
            int[] argb = decoded.getRGB(0, 0, width, height, null, 0, width);
            return new VulkanGameImage(width, height, argb,
                    !decoded.getColorModel().hasAlpha());
        } catch (IOException failure) {
            throw new IllegalStateException("Could not decode native image", failure);
        }
    }
    @Override public GameImage createImage(int width, int height, boolean alpha) {
        return VulkanGameImage.empty(width, height, alpha);
    }
    @Override public GameImage b(int width, int height, boolean alpha) {
        return VulkanGameImage.empty(width, height, alpha);
    }
    @Override public void e() { if (renderTarget == null && !nativeRoot()) delegate.e(); }
    @Override public void drawImageRotated(GameImage image, float x, float y, float angle,
                                           Paint paint) {
        // This API receives Rusted Warfare's body/shadow angle relative to "up". The original
        // Slick backend converts it to image-space by adding 90 degrees here. Section rotation
        // intentionally uses its angle directly and must not share this adjustment.
        float imageAngle = angle + 90.0f;
        if (renderTarget != null) {
            float left = x - image.getWidth() * 0.5f;
            float top = y - image.getHeight() * 0.5f;
            drawImageCpu(image, full(image), left, top,
                    left + image.getWidth(), top + image.getHeight(), paint,
                    safeRotation(imageAngle, x, y));
        } else if (nativeRoot()) {
            float left = x - image.getWidth() * 0.5f;
            float top = y - image.getHeight() * 0.5f;
            nativeImage(image, full(image), left, top, left + image.getWidth(),
                    top + image.getHeight(), paint,
                    safeRotation(imageAngle, x, y));
        } else delegate.drawImageRotated(image, x, y, angle, paint);
    }
    @Override public void drawImageSectionRotated(GameImage image, Rect source, float x, float y,
                                                  float angle, Paint paint) {
        if (renderTarget != null) {
            float width = source.c - source.a;
            float height = source.d - source.b;
            drawImageCpu(image, source, x - width * 0.5f, y - height * 0.5f,
                    x + width * 0.5f, y + height * 0.5f, paint,
                    safeRotation(angle, x, y));
        } else if (nativeRoot()) {
            float width = source.c - source.a;
            float height = source.d - source.b;
            nativeImage(image, source, x - width * 0.5f, y - height * 0.5f,
                    x + width * 0.5f, y + height * 0.5f, paint,
                    safeRotation(angle, x, y));
        } else delegate.drawImageSectionRotated(image, source, x, y, angle, paint);
    }
    @Override public void drawImage(GameImage image, Rect source, Rect destination, Paint paint) {
        if (renderTarget != null) {
            drawImageCpu(image, source, destination.a, destination.b,
                    destination.c, destination.d, paint, null);
        } else if (nativeRoot()) nativeImage(image, source, destination.a, destination.b,
                destination.c, destination.d, paint, null);
        else delegate.drawImage(image, source, destination, paint);
    }
    @Override public void drawImage(GameImage image, Rect source, RectF destination, Paint paint) {
        if (renderTarget != null) {
            drawImageCpu(image, source, destination.a, destination.b,
                    destination.c, destination.d, paint, null);
        } else if (nativeRoot()) nativeImage(image, source, destination.a, destination.b,
                destination.c, destination.d, paint, null);
        else delegate.drawImage(image, source, destination, paint);
    }
    @Override public void drawImageCentered(GameImage image, float x, float y, Paint paint) {
        if (renderTarget != null) {
            drawImageCpu(image, full(image), x - image.getWidth() * 0.5f,
                    y - image.getHeight() * 0.5f, x + image.getWidth() * 0.5f,
                    y + image.getHeight() * 0.5f, paint, null);
        } else if (nativeRoot()) nativeImage(image, full(image),
                x - image.getWidth() * 0.5f, y - image.getHeight() * 0.5f,
                x + image.getWidth() * 0.5f, y + image.getHeight() * 0.5f, paint, null);
        else delegate.drawImageCentered(image, x, y, paint);
    }
    @Override public void drawImageTransformed(GameImage image, float x, float y, Paint paint,
                                               float scale, float angle) {
        if (renderTarget != null) {
            float scaledWidth = image.getWidth() * scale;
            float scaledHeight = image.getHeight() * scale;
            drawImageCpu(image, full(image), x - scaledWidth * 0.5f,
                    y - scaledHeight * 0.5f, x + scaledWidth * 0.5f,
                    y + scaledHeight * 0.5f, paint,
                    safeRotation(angle, x, y));
        } else if (nativeRoot()) {
            float w = image.getWidth() * scale;
            float h = image.getHeight() * scale;
            nativeImage(image, full(image), x - w * 0.5f, y - h * 0.5f,
                    x + w * 0.5f, y + h * 0.5f, paint,
                    safeRotation(angle, x, y));
        } else delegate.drawImageTransformed(image, x, y, paint, scale, angle);
    }
    @Override public void drawImageRaw(GameImage image, float x, float y, Paint paint) {
        if (renderTarget != null) {
            drawImageCpu(image, full(image), x, y,
                    x + image.getWidth(), y + image.getHeight(), paint, null);
        } else if (nativeRoot()) nativeImage(image, full(image), x, y,
                x + image.getWidth(), y + image.getHeight(), paint, null);
        else delegate.drawImageRaw(image, x, y, paint);
    }
    @Override public void drawImageDirect(GameImage image, Rect source, Rect destination,
                                          Paint paint) {
        if (renderTarget != null) drawImage(image, source, destination, paint);
        else if (nativeRoot()) drawImage(image, source, destination, paint);
        else delegate.drawImageDirect(image, source, destination, paint);
    }
    @Override public void drawRect(Rect rect, Paint paint) {
        if (renderTarget != null) drawCpuRect(rect.a, rect.b, rect.c, rect.d, paint);
        else if (nativeRoot()) nativeRect(rect.a, rect.b, rect.c, rect.d, paint);
        else delegate.drawRect(rect, paint);
    }
    @Override public void a(boolean value) {
        if (renderTarget == null && !nativeRoot()) delegate.a(value);
    }
    @Override public void f() {
        if (renderTarget == null && !nativeRoot()) delegate.f();
    }
    @Override public void drawTiledImage(GameImage image, Rect destination, Paint paint) {
        if (renderTarget != null) {
            drawTiledImage(image, destination, paint, 0, 0,
                    image.getWidth(), image.getHeight());
        } else if (nativeRoot()) drawTiledImage(image, destination, paint, 0, 0,
                image.getWidth(), image.getHeight());
        else delegate.drawTiledImage(image, destination, paint);
    }
    @Override public void drawTiledImage(GameImage image, Rect destination, Paint paint,
                                         int offsetX, int offsetY,
                                         int overlapX, int overlapY) {
        if (nativeRoot() || nativeTarget()) {
            drawTiledImageNative(image, destination, paint,
                    offsetX, offsetY, overlapX, overlapY);
        } else delegate.drawTiledImage(image, destination, paint,
                offsetX, offsetY, overlapX, overlapY);
    }
    @Override public void drawTiledImage(GameImage image, RectF destination, Paint paint,
                                         float offsetX, float offsetY, int stepX, int stepY) {
        if (renderTarget != null) {
            Rect integer = new Rect(Math.round(destination.a), Math.round(destination.b),
                    Math.round(destination.c), Math.round(destination.d));
            drawTiledImage(image, integer, paint, Math.round(offsetX), Math.round(offsetY),
                    stepX, stepY);
        } else if (nativeRoot()) {
            Rect integer = new Rect(Math.round(destination.a), Math.round(destination.b),
                    Math.round(destination.c), Math.round(destination.d));
            drawTiledImage(image, integer, paint, Math.round(offsetX), Math.round(offsetY),
                    stepX, stepY);
        } else delegate.drawTiledImage(image, destination, paint,
                offsetX, offsetY, stepX, stepY);
    }

    /** Exact native equivalent of the game's RenderUtils tiled-image clipping algorithm. */
    private void drawTiledImageNative(GameImage image, Rect destination, Paint paint,
                                      int offsetX, int offsetY,
                                      int overlapX, int overlapY) {
        if (image == null || destination == null) return;
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        if (imageWidth <= 0 || imageHeight <= 0) return;
        offsetX %= imageWidth;
        offsetY %= imageHeight;
        if (offsetX < 0) offsetX += imageWidth;
        if (offsetY < 0) offsetY += imageHeight;
        int stepX = imageWidth - overlapX;
        int stepY = imageHeight - overlapY;
        if (stepX <= 0 || stepY <= 0) return;
        int startX = destination.a - offsetX;
        int draws = 0;
        for (int x = startX; x < destination.c; x += stepX) {
            int startY = destination.b - offsetY;
            for (int y = startY; y < destination.d; y += stepY) {
                if (++draws > 2000) return;
                int drawWidth = Math.min(imageWidth, destination.c - x);
                int drawHeight = Math.min(imageHeight, destination.d - y);
                if (drawWidth <= 0 || drawHeight <= 0) break;
                int sourceLeft = 0;
                int sourceTop = 0;
                int drawLeft = x;
                int drawTop = y;
                int drawRight = x + drawWidth;
                int drawBottom = y + drawHeight;
                if (drawLeft < destination.a) {
                    sourceLeft += destination.a - drawLeft;
                    drawLeft = destination.a;
                }
                if (drawTop < destination.b) {
                    sourceTop += destination.b - drawTop;
                    drawTop = destination.b;
                }
                Rect source = new Rect(sourceLeft, sourceTop, drawWidth, drawHeight);
                if (nativeTarget()) {
                    drawImageCpu(image, source, drawLeft, drawTop,
                            drawRight, drawBottom, paint, null);
                } else {
                    nativeImage(image, source, drawLeft, drawTop,
                            drawRight, drawBottom, paint, null);
                }
            }
        }
    }
    @Override public void drawColor(int color) {
        if (renderTarget != null) fillTarget(color);
        else if (nativeRoot()) VulkanRuntime.clearNativeFrame(color);
        else delegate.drawColor(color);
    }
    @Override public void drawColor(int color, PorterDuff.Mode mode) {
        if (renderTarget != null) fillTarget(color);
        else if (nativeRoot()) VulkanRuntime.clearNativeFrame(color);
        else delegate.drawColor(color, mode);
    }
    @Override public void drawTextWithBackground(String text, float x, float y, Paint textPaint,
                                                 Paint backgroundPaint, float padding) {
        if (nativeRoot() || cpuTarget()) {
            float width = getTextWidth(text, textPaint);
            float height = getTextHeight(text, textPaint);
            nativeRect(x - padding, y - height - padding,
                    x + width + padding, y + padding, backgroundPaint);
            drawText(text, x, y, textPaint);
        } else delegate.drawTextWithBackground(text, x, y, textPaint,
                backgroundPaint, padding);
    }
    @Override public void drawText(String text, float x, float y, Paint paint) {
        if (nativeRoot()) VulkanRuntime.recordNativeText(text, x, y, paint, state(paint));
        else if (nativeTarget()) {
            beforeNativeTargetMutation();
            VulkanRuntime.recordNativeText(offscreenBuilder, text, x, y, paint, state(paint));
        }
        else if (cpuTarget()) drawCpuText(text, x, y, paint);
        else delegate.drawText(text, x, y, paint);
    }
    @Override public void drawRectDirect(Rect rect, Paint paint) {
        if (renderTarget != null) drawCpuRect(rect.a, rect.b, rect.c, rect.d, paint);
        else if (nativeRoot()) nativeRect(rect.a, rect.b, rect.c, rect.d, paint);
        else delegate.drawRectDirect(rect, paint);
    }
    @Override public void drawRect(RectF rect, Paint paint) {
        if (renderTarget != null) drawCpuRect(rect.a, rect.b, rect.c, rect.d, paint);
        else if (nativeRoot()) nativeRect(rect.a, rect.b, rect.c, rect.d, paint);
        else delegate.drawRect(rect, paint);
    }
    @Override public void beginFrame() {
        if (nativeTarget()) {
            beforeNativeTargetMutation();
            resetOffscreenBuilder();
        }
        else if (renderTarget == null && !nativeRoot()) delegate.beginFrame();
    }
    @Override public void endFrame() {
        if (nativeTarget()) {
            submitOffscreen();
            renderTarget.version++;
        } else if (renderTarget != null) renderTarget.version++;
        else if (!nativeRoot()) delegate.endFrame();
    }
    @Override public void drawRectFromSize(Rect rect, Paint paint) {
        if (nativeRoot()) nativeRect(rect.a, rect.b, rect.a + rect.c, rect.b + rect.d, paint);
        else if (cpuTarget()) drawCpuRect(rect.a, rect.b,
                rect.a + rect.c, rect.b + rect.d, paint);
        else delegate.drawRectFromSize(rect, paint);
    }
    @Override public void setClipRect(Rect rect) {
        if (nativeRoot() || cpuTarget()) clip = rect == null ? null : transformedClip(
                rect.a, rect.b, rect.c, rect.d);
        else delegate.setClipRect(rect);
    }
    @Override public void setClipRect(RectF rect) {
        if (nativeRoot() || cpuTarget()) clip = rect == null ? null : transformedClip(
                rect.a, rect.b, rect.c, rect.d);
        else delegate.setClipRect(rect);
    }
    @Override public void drawCircle(float x, float y, float radius, Paint paint) {
        if (nativeRoot()) nativeCircle(x, y, radius, paint);
        else if (cpuTarget()) drawCpuCircle(x, y, radius, paint);
        else delegate.drawCircle(x, y, radius, paint);
    }
    @Override public void drawCircleDirect(float x, float y, float radius, Paint paint) {
        if (nativeRoot()) nativeCircle(x, y, radius, paint);
        else if (cpuTarget()) drawCpuCircle(x, y, radius, paint);
        else delegate.drawCircleDirect(x, y, radius, paint);
    }
    @Override public void drawLines(float[] points, int offset, int count, Paint paint) {
        if (nativeRoot() || nativeTarget()) {
            if (points == null) return;
            int end = Math.min(points.length, Math.max(0, offset) + Math.max(0, count));
            float size = Math.max(1.0f, paint == null ? 1.0f : paint.g());
            for (int i = Math.max(0, offset); i + 1 < end; i += 2) {
                nativeQuad(points[i] - size * 0.5f, points[i + 1] - size * 0.5f,
                        size, size, paint);
            }
        } else if (cpuTarget()) {
            if (points == null) return;
            int end = Math.min(points.length, Math.max(0, offset) + Math.max(0, count));
            Graphics2D graphics = cpuGraphics(paint, false);
            try {
                for (int i = Math.max(0, offset); i + 3 < end; i += 4) {
                    graphics.draw(new java.awt.geom.Line2D.Float(
                            points[i], points[i + 1], points[i + 2], points[i + 3]));
                }
            } finally {
                finishCpuDraw(graphics);
            }
        } else delegate.drawLines(points, offset, count, paint);
    }
    @Override public void save() {
        if (nativeRoot() || cpuTarget()) stateStack.push(new NativeState(transform, clip));
        else delegate.save();
    }
    @Override public void restore() {
        if (nativeRoot() || cpuTarget()) restoreNativeState();
        else delegate.restore();
    }
    @Override public void saveTransform() { save(); }
    @Override public void restoreTransform() { restore(); }
    @Override public void rotate(float angle, float pivotX, float pivotY) {
        if (nativeRoot() || cpuTarget()) {
            if (!Float.isFinite(angle) || !Float.isFinite(pivotX)
                    || !Float.isFinite(pivotY)) {
                collapseInvalidTransform();
            } else {
                transform = VulkanTransform2D.rotationAround(
                        angle, pivotX, pivotY).then(transform);
            }
        }
        else delegate.rotate(angle, pivotX, pivotY);
    }
    @Override public void scale(float x, float y) {
        if (nativeRoot() || cpuTarget()) {
            if (!Float.isFinite(x) || !Float.isFinite(y)) collapseInvalidTransform();
            else transform = VulkanTransform2D.scale(x, y).then(transform);
        }
        else delegate.scale(x, y);
    }
    @Override public void scaleAround(float x, float y, float pivotX, float pivotY) {
        if (nativeRoot() || cpuTarget()) {
            if (!Float.isFinite(x) || !Float.isFinite(y)
                    || !Float.isFinite(pivotX) || !Float.isFinite(pivotY)) {
                collapseInvalidTransform();
            } else {
                transform = VulkanTransform2D.translation(-pivotX, -pivotY)
                        .then(VulkanTransform2D.scale(x, y))
                        .then(VulkanTransform2D.translation(pivotX, pivotY))
                        .then(transform);
            }
        }
        else delegate.scaleAround(x, y, pivotX, pivotY);
    }
    @Override public void translate(float x, float y) {
        if (nativeRoot() || cpuTarget()) {
            if (!Float.isFinite(x) || !Float.isFinite(y)) collapseInvalidTransform();
            else transform = VulkanTransform2D.translation(x, y).then(transform);
        }
        else delegate.translate(x, y);
    }
    @Override public void runDrawTimeCallback(DrawTimeCallback callback) {
        if (nativeRoot() || cpuTarget()) callback.run(this);
        else delegate.runDrawTimeCallback(callback);
    }
    @Override public void drawLine(float startX, float startY, float endX, float endY,
                                   Paint paint) {
        if (nativeRoot()) nativeLine(startX, startY, endX, endY, paint);
        else if (cpuTarget()) drawCpuLine(startX, startY, endX, endY, paint);
        else delegate.drawLine(startX, startY, endX, endY, paint);
    }
    @Override public int getWidth() {
        if (renderTarget != null) return width;
        if (nativeRoot()) return VulkanRuntime.surfaceInfo().map(info -> info.width()).orElse(1);
        return delegate.getWidth();
    }
    @Override public int getHeight() {
        if (renderTarget != null) return height;
        if (nativeRoot()) return VulkanRuntime.surfaceInfo().map(info -> info.height()).orElse(1);
        return delegate.getHeight();
    }
    @Override public void setSize(int width, int height) {
        if (renderTarget == null && !nativeRoot()) delegate.setSize(width, height);
        else {
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
        }
    }
    @Override public void clearAlphaMap() {
        if (renderTarget != null) fillTarget(0);
        else if (nativeRoot()) VulkanRuntime.clearNativeFrame(0);
        else delegate.clearAlphaMap();
    }
    @Override public void prepareFontForPaint(Paint paint) {
        // Glyph rasterization happens when VulkanTextTextureCache first sees a string.
    }
    @Override public void flush() {
        if (nativeTarget()) {
            submitOffscreen();
            renderTarget.version++;
        } else if (renderTarget != null) renderTarget.version++;
    }
    @Override public void dispose() {
        if (persistentCpuGraphics != null) {
            persistentCpuGraphics.dispose();
            persistentCpuGraphics = null;
        }
        if (renderTarget instanceof VulkanGameImage) {
            ((VulkanGameImage) renderTarget).clearNativeRenderTargetBackend(this);
        }
        if (ownsNativeRenderTarget) {
            VulkanRuntime.destroyNativeRenderTarget(nativeRenderTargetHandle);
            if (renderTarget instanceof VulkanGameImage) {
                VulkanGameImage nativeImage = (VulkanGameImage) renderTarget;
                nativeImage.setNativeRenderTargetFlusher(null);
                nativeImage.setNativeRenderTargetHandle(0L);
            }
        }
        if (renderTarget == null && !nativeRoot()) delegate.dispose();
    }
    @Override public int getTextHeight(String text, Paint paint) {
        return fontMetrics(paint).getHeight();
    }
    @Override public int getTextWidth(String text, Paint paint) {
        return fontMetrics(paint).stringWidth(text == null ? "" : text);
    }
    @Override public GameImage getErrorImage() { return errorImage; }
    @Override public void saveImageToFile(GameImage image, File file) {
        if (nativeRoot() || cpuTarget()) {
            if (image == null || file == null) return;
            GameImage real = image.getRealImage();
            if (real == null) real = image;
            try {
                ImageIO.write(wrapPixels(real), "png", file);
            } catch (IOException failure) {
                throw new IllegalStateException("Could not save native image to " + file,
                        failure);
            }
        } else delegate.saveImageToFile(image, file);
    }
    @Override public void enterLock(Lock lock) {
        if (cpuTarget()) lock.lock();
        else if (!nativeRoot()) delegate.enterLock(lock);
    }
    @Override public void leaveLock(Lock lock) {
        if (cpuTarget()) lock.unlock();
        else if (!nativeRoot()) delegate.leaveLock(lock);
    }
    @Override public void compileShader(ShaderProgram shader) {
        if (nativeRoot() || cpuTarget()) {
            if (shader != null && VulkanBuiltInShaders.effectForName(shader.name) < 0) {
                customShaderState(shader);
            }
        } else delegate.compileShader(shader);
    }
    @Override public float getUiScale() {
        return nativeRoot() || cpuTarget() ? 1.0f : delegate.getUiScale();
    }

    private static FontMetrics fontMetrics(Paint paint) {
        int size = Math.max(1, Math.round(paint == null ? 16.0f : paint.k()));
        boolean bold = paint != null && paint.i() != null && paint.i().a();
        BufferedImage target = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();
        try {
            Font font = new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size);
            return graphics.getFontMetrics(font);
        } finally {
            graphics.dispose();
        }
    }

    private void nativeRect(float left, float top, float right, float bottom, Paint paint) {
        if (paint == null || right < left || bottom < top) return;
        if (paint.d() != Paint$Style.b) {
            nativeQuad(left, top, right - left, bottom - top, paint);
        }
        if (paint.d() == Paint$Style.b || paint.d() == Paint$Style.c) {
            float thickness = Math.max(1.0f, paint.g());
            nativeQuad(left, top, right - left, thickness, paint);
            nativeQuad(left, bottom - thickness, right - left, thickness, paint);
            nativeQuad(left, top + thickness, thickness,
                    Math.max(0.0f, bottom - top - thickness * 2.0f), paint);
            nativeQuad(right - thickness, top + thickness, thickness,
                    Math.max(0.0f, bottom - top - thickness * 2.0f), paint);
        }
    }

    private void nativeLine(float x1, float y1, float x2, float y2, Paint paint) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.hypot(dx, dy);
        float thickness = Math.max(1.0f, paint == null ? 1.0f : paint.g());
        if (length < 0.0001f) {
            nativeQuad(x1 - thickness * 0.5f, y1 - thickness * 0.5f,
                    thickness, thickness, paint);
            return;
        }
        VulkanTransform2D previous = transform;
        try {
            transform = safeRotation(
                    (float) Math.toDegrees(Math.atan2(dy, dx)), x1, y1).then(transform);
            nativeQuad(x1, y1 - thickness * 0.5f, length, thickness, paint);
        } finally {
            transform = previous;
        }
    }

    private void nativeCircle(float x, float y, float radius, Paint paint) {
        if (radius < 0.0f || !Float.isFinite(radius)) return;
        int segments = Math.max(16, Math.min(64, (int) Math.ceil(radius)));
        if (paint != null && paint.d() == Paint$Style.b) {
            float previousX = x + radius;
            float previousY = y;
            for (int i = 1; i <= segments; i++) {
                double angle = i * Math.PI * 2.0 / segments;
                float nextX = x + (float) Math.cos(angle) * radius;
                float nextY = y + (float) Math.sin(angle) * radius;
                nativeLine(previousX, previousY, nextX, nextY, paint);
                previousX = nextX;
                previousY = nextY;
            }
            return;
        }
        float stripHeight = radius * 2.0f / segments;
        for (int i = 0; i < segments; i++) {
            float relativeY = -radius + (i + 0.5f) * stripHeight;
            float halfWidth = (float) Math.sqrt(Math.max(0.0f,
                    radius * radius - relativeY * relativeY));
            nativeQuad(x - halfWidth, y + relativeY - stripHeight * 0.5f,
                    halfWidth * 2.0f, stripHeight, paint);
        }
    }

    private void restoreNativeState() {
        if (stateStack.isEmpty()) {
            transform = VulkanTransform2D.IDENTITY;
            clip = null;
            return;
        }
        NativeState restored = stateStack.pop();
        transform = restored.transform;
        clip = restored.clip;
    }

    private void collapseInvalidTransform() {
        // Slick/OpenGL tolerates NaN/Infinity in a single model transform by producing no useful
        // fragments. Do the same without poisoning Vulkan vertices or terminating the game.
        transform = VulkanTransform2D.scale(0.0f, 0.0f).then(transform);
    }

    private static VulkanTransform2D safeRotation(float angle, float pivotX, float pivotY) {
        if (!Float.isFinite(angle) || !Float.isFinite(pivotX)
                || !Float.isFinite(pivotY)) {
            return VulkanTransform2D.scale(0.0f, 0.0f);
        }
        return VulkanTransform2D.rotationAround(angle, pivotX, pivotY);
    }

    private VulkanClipRect transformedClip(float left, float top, float right, float bottom) {
        float x0 = transform.transformX(left, top);
        float y0 = transform.transformY(left, top);
        float x1 = transform.transformX(right, top);
        float y1 = transform.transformY(right, top);
        float x2 = transform.transformX(left, bottom);
        float y2 = transform.transformY(left, bottom);
        float x3 = transform.transformX(right, bottom);
        float y3 = transform.transformY(right, bottom);
        float minX = Math.min(Math.min(x0, x1), Math.min(x2, x3));
        float minY = Math.min(Math.min(y0, y1), Math.min(y2, y3));
        float maxX = Math.max(Math.max(x0, x1), Math.max(x2, x3));
        float maxY = Math.max(Math.max(y0, y1), Math.max(y2, y3));
        return new VulkanClipRect(minX, minY,
                Math.max(0.0f, maxX - minX), Math.max(0.0f, maxY - minY));
    }

    private static final class NativeState {
        final VulkanTransform2D transform;
        final VulkanClipRect clip;

        NativeState(VulkanTransform2D transform, VulkanClipRect clip) {
            this.transform = transform;
            this.clip = clip;
        }
    }

    private final class NativeCanvasTarget implements CanvasDrawTarget {
        private VulkanGraphicsEngine active = VulkanGraphicsEngine.this;

        private VulkanGraphicsEngine target() {
            return active;
        }

        private void bind(GameImage image) {
            VulkanGraphicsEngine next = image == null
                    ? VulkanGraphicsEngine.this : backendForImage(image);
            if (next == active) return;

            // GL's Canvas target closes the outgoing framebuffer batch before rebinding. Do the
            // same here so a later target can safely sample everything drawn before the switch.
            if (active.nativeTarget()) active.submitOffscreen();

            // Canvas save/transform/clip state belongs to the command stream, not to the bitmap.
            // Carry it over just as the original GL canvas does when only its framebuffer changes.
            next.transform = active.transform;
            next.clip = active.clip;
            next.stateStack.clear();
            next.stateStack.addAll(active.stateStack);
            active = next;
        }

        @Override public void a(boolean value) { target().a(value); }
        @Override public boolean c() { return true; }
        @Override public void clipRect(Rect rect) { target().setClipRect(rect); }
        @Override public void clipRect(RectF rect) { target().setClipRect(rect); }
        @Override public void drawImage(GameImage image, float x, float y, Paint paint) {
            target().drawImageRaw(image, x, y, paint);
        }
        @Override public void drawImage(GameImage image, Rect source, Rect destination,
                                        Paint paint) {
            target().drawImage(image, source, destination, paint);
        }
        @Override public void drawImage(GameImage image, Rect source, RectF destination,
                                        Paint paint) {
            target().drawImage(image, source, destination, paint);
        }
        @Override public void drawCircle(float x, float y, float radius, Paint paint) {
            target().drawCircle(x, y, radius, paint);
        }
        @Override public void drawColor(int color, PorterDuff.Mode mode) {
            target().drawColor(color, mode);
        }
        @Override public void drawColor(int color) { target().drawColor(color); }
        @Override public void drawLine(float x1, float y1, float x2, float y2, Paint paint) {
            target().drawLine(x1, y1, x2, y2, paint);
        }
        @Override public void drawLines(float[] points, int offset, int count, Paint paint) {
            target().drawLines(points, offset, count, paint);
        }
        @Override public void drawRect(Rect rect, Paint paint) {
            target().drawRect(rect, paint);
        }
        @Override public void drawRect(RectF rect, Paint paint) {
            target().drawRect(rect, paint);
        }
        @Override public void drawText(String text, float x, float y, Paint paint) {
            target().drawText(text, x, y, paint);
        }
        @Override public void restore() { target().restore(); }
        @Override public void rotate(float angle, float x, float y) {
            target().rotate(angle, x, y);
        }
        @Override public void save() { target().save(); }
        @Override public void scale(float x, float y) { target().scale(x, y); }
        @Override public void scale(float x, float y, float pivotX, float pivotY) {
            target().scaleAround(x, y, pivotX, pivotY);
        }
        @Override public void setBitmapFromImage(GameImage image) { bind(image); }
        @Override public void translate(float x, float y) {
            target().translate(x, y);
        }
        @Override public void runDrawTimeCallback(DrawTimeCallback callback) {
            target().runDrawTimeCallback(callback);
        }
        @Override public void flushBitmap(Bitmap bitmap) { }
        @Override public void enterLock(Lock lock) { target().enterLock(lock); }
        @Override public void leaveLock(Lock lock) { target().leaveLock(lock); }
        @Override public boolean compileShader(ShaderProgram shader) {
            target().compileShader(shader);
            return true;
        }
    }

    private void fillTarget(int argb) {
        if (nativeTarget()) {
            beforeNativeTargetMutation();
            // A full-target clear supersedes every command not submitted yet. Later flushes use
            // a LOAD pass, so discarding this pending prefix also preserves the original ordering.
            resetOffscreenBuilder();
            offscreenBuilder.clear(((argb >>> 16) & 255) / 255.0f,
                    ((argb >>> 8) & 255) / 255.0f,
                    (argb & 255) / 255.0f,
                    ((argb >>> 24) & 255) / 255.0f);
            return;
        }
        renderTarget.ensurePixelBuffer();
        if (renderTarget.pixelBuffer == null) {
            renderTarget.pixelBuffer = new int[Math.multiplyExact(
                    renderTarget.getWidth(), renderTarget.getHeight())];
        }
        Arrays.fill(renderTarget.pixelBuffer, argb);
        renderTarget.version++;
    }

    private static Rect full(GameImage image) {
        return new Rect(0, 0, image.getWidth(), image.getHeight());
    }

    private void drawImageCpu(GameImage sourceImage, Rect source,
                              float left, float top, float right, float bottom,
                              Paint paint, VulkanTransform2D localTransform) {
        if (nativeTarget()) {
            nativeImage(sourceImage, source, left, top, right, bottom, paint, localTransform);
            return;
        }
        if (sourceImage == null || source == null || right <= left || bottom <= top) return;
        GameImage sourceImageReal = sourceImage.getRealImage();
        if (sourceImageReal == null) sourceImageReal = sourceImage;
        sourceImageReal.ensurePixelBuffer();
        if (sourceImageReal.pixelBuffer == null) return;
        if (localTransform == null && tryFastCpuImageBlit(sourceImageReal, source,
                left, top, right, bottom, paint)) {
            renderTarget.version++;
            return;
        }
        VulkanTransform2D previous = transform;
        if (localTransform != null) transform = localTransform.then(transform);
        Graphics2D graphics = cpuGraphics(paint, true);
        try {
            BufferedImage sourcePixels = wrapPixels(sourceImageReal);
            graphics.drawImage(sourcePixels,
                    Math.round(left), Math.round(top), Math.round(right), Math.round(bottom),
                    source.a, source.b, source.c, source.d, null);
        } finally {
            transform = previous;
            finishCpuDraw(graphics);
        }
    }

    private boolean tryFastCpuImageBlit(GameImage sourceImage, Rect source,
                                        float left, float top, float right, float bottom,
                                        Paint paint) {
        // Terrain-cache rendering is overwhelmingly axis-aligned image-section copies. Java2D's
        // per-call setup dominates when thousands of tiles rebuild after a large zoom change, so
        // handle this common case directly against the stable ARGB arrays.
        if (sourceImage == renderTarget) return false;
        if (Math.abs(transform.m01()) > 0.00001f || Math.abs(transform.m10()) > 0.00001f
                || transform.m00() <= 0.0f || transform.m11() <= 0.0f) return false;
        int sourceWidth = source.c - source.a;
        int sourceHeight = source.d - source.b;
        if (sourceWidth <= 0 || sourceHeight <= 0) return true;

        float transformedLeft = transform.transformX(Math.round(left), Math.round(top));
        float transformedTop = transform.transformY(Math.round(left), Math.round(top));
        float transformedRight = transform.transformX(Math.round(right), Math.round(bottom));
        float transformedBottom = transform.transformY(Math.round(right), Math.round(bottom));
        if (!(transformedRight > transformedLeft) || !(transformedBottom > transformedTop)) {
            return true;
        }
        int firstX = Math.max(0, (int) Math.floor(transformedLeft));
        int firstY = Math.max(0, (int) Math.floor(transformedTop));
        int lastX = Math.min(renderTarget.getWidth(), (int) Math.ceil(transformedRight));
        int lastY = Math.min(renderTarget.getHeight(), (int) Math.ceil(transformedBottom));
        if (clip != null) {
            firstX = Math.max(firstX, (int) Math.floor(clip.x()));
            firstY = Math.max(firstY, (int) Math.floor(clip.y()));
            lastX = Math.min(lastX, (int) Math.ceil(clip.x() + clip.width()));
            lastY = Math.min(lastY, (int) Math.ceil(clip.y() + clip.height()));
        }
        if (lastX <= firstX || lastY <= firstY) return true;

        int[] sourcePixels = sourceImage.pixelBuffer;
        int[] destinationPixels = renderTarget.pixelBuffer;
        if (destinationPixels == null) {
            renderTarget.pixelBuffer = destinationPixels = new int[Math.multiplyExact(
                    renderTarget.getWidth(), renderTarget.getHeight())];
        }
        int sourceImageWidth = sourceImage.getWidth();
        int sourceImageHeight = sourceImage.getHeight();
        float inverseWidth = 1.0f / (transformedRight - transformedLeft);
        float inverseHeight = 1.0f / (transformedBottom - transformedTop);
        int paintAlpha = paint == null ? 255 : (paint.e() >>> 24) & 255;
        int destinationStride = renderTarget.getWidth();
        boolean opaqueSource = paintAlpha == 255
                && sourceImage instanceof VulkanGameImage
                && ((VulkanGameImage) sourceImage).isOpaque();
        int exactLeft = Math.round(transformedLeft);
        int exactTop = Math.round(transformedTop);
        boolean exactCopy = opaqueSource
                && Math.abs(transformedLeft - exactLeft) < 0.0001f
                && Math.abs(transformedTop - exactTop) < 0.0001f
                && Math.abs((transformedRight - transformedLeft) - sourceWidth) < 0.0001f
                && Math.abs((transformedBottom - transformedTop) - sourceHeight) < 0.0001f;
        if (exactCopy) {
            int sourceX = source.a + firstX - exactLeft;
            int sourceY = source.b + firstY - exactTop;
            int columns = lastX - firstX;
            if (sourceX >= 0 && sourceY >= 0
                    && sourceX + columns <= sourceImageWidth
                    && sourceY + (lastY - firstY) <= sourceImageHeight) {
                for (int y = firstY; y < lastY; y++, sourceY++) {
                    System.arraycopy(sourcePixels, sourceY * sourceImageWidth + sourceX,
                            destinationPixels, y * destinationStride + firstX, columns);
                }
                return true;
            }
        }
        for (int y = firstY; y < lastY; y++) {
            int sourceY = source.b + Math.min(sourceHeight - 1, Math.max(0,
                    (int) ((y + 0.5f - transformedTop) * inverseHeight * sourceHeight)));
            if (sourceY < 0 || sourceY >= sourceImageHeight) continue;
            int sourceRow = sourceY * sourceImageWidth;
            int destinationOffset = y * destinationStride + firstX;
            for (int x = firstX; x < lastX; x++, destinationOffset++) {
                int sourceX = source.a + Math.min(sourceWidth - 1, Math.max(0,
                        (int) ((x + 0.5f - transformedLeft) * inverseWidth * sourceWidth)));
                if (sourceX < 0 || sourceX >= sourceImageWidth) continue;
                int sourceArgb = sourcePixels[sourceRow + sourceX];
                if (opaqueSource) {
                    destinationPixels[destinationOffset] = sourceArgb | 0xff000000;
                    continue;
                }
                int alpha = ((sourceArgb >>> 24) & 255) * paintAlpha / 255;
                if (alpha == 255) {
                    destinationPixels[destinationOffset] = sourceArgb | 0xff000000;
                } else if (alpha != 0) {
                    destinationPixels[destinationOffset] = blendSrcOver(
                            sourceArgb, destinationPixels[destinationOffset], alpha);
                }
            }
        }
        return true;
    }

    private static int blendSrcOver(int sourceArgb, int destinationArgb, int sourceAlpha) {
        int inverse = 255 - sourceAlpha;
        int destinationAlpha = (destinationArgb >>> 24) & 255;
        int outputAlpha = sourceAlpha + (destinationAlpha * inverse + 127) / 255;
        if (outputAlpha <= 0) return 0;
        int destinationWeight = (destinationAlpha * inverse + 127) / 255;
        int red = ((((sourceArgb >>> 16) & 255) * sourceAlpha)
                + (((destinationArgb >>> 16) & 255) * destinationWeight)) / outputAlpha;
        int green = ((((sourceArgb >>> 8) & 255) * sourceAlpha)
                + (((destinationArgb >>> 8) & 255) * destinationWeight)) / outputAlpha;
        int blue = (((sourceArgb & 255) * sourceAlpha)
                + ((destinationArgb & 255) * destinationWeight)) / outputAlpha;
        return outputAlpha << 24 | red << 16 | green << 8 | blue;
    }

    private void drawCpuRect(float left, float top, float right, float bottom, Paint paint) {
        if (nativeTarget()) {
            nativeRect(left, top, right, bottom, paint);
            return;
        }
        if (right < left || bottom < top) return;
        Graphics2D graphics = cpuGraphics(paint, false);
        try {
            Rectangle2D.Float rectangle = new Rectangle2D.Float(
                    left, top, right - left, bottom - top);
            Paint$Style style = paint == null ? Paint$Style.a : paint.d();
            if (style != Paint$Style.b) graphics.fill(rectangle);
            if (style == Paint$Style.b || style == Paint$Style.c) graphics.draw(rectangle);
        } finally {
            finishCpuDraw(graphics);
        }
    }

    private void drawCpuCircle(float x, float y, float radius, Paint paint) {
        if (nativeTarget()) {
            nativeCircle(x, y, radius, paint);
            return;
        }
        if (radius < 0.0f || !Float.isFinite(radius)) return;
        Graphics2D graphics = cpuGraphics(paint, false);
        try {
            java.awt.geom.Ellipse2D.Float circle = new java.awt.geom.Ellipse2D.Float(
                    x - radius, y - radius, radius * 2.0f, radius * 2.0f);
            Paint$Style style = paint == null ? Paint$Style.a : paint.d();
            if (style != Paint$Style.b) graphics.fill(circle);
            if (style == Paint$Style.b || style == Paint$Style.c) graphics.draw(circle);
        } finally {
            finishCpuDraw(graphics);
        }
    }

    private void drawCpuLine(float x1, float y1, float x2, float y2, Paint paint) {
        if (nativeTarget()) {
            nativeLine(x1, y1, x2, y2, paint);
            return;
        }
        Graphics2D graphics = cpuGraphics(paint, false);
        try {
            graphics.draw(new java.awt.geom.Line2D.Float(x1, y1, x2, y2));
        } finally {
            finishCpuDraw(graphics);
        }
    }

    private void drawCpuText(String text, float x, float y, Paint paint) {
        if (nativeTarget()) {
            beforeNativeTargetMutation();
            VulkanRuntime.recordNativeText(offscreenBuilder, text, x, y, paint, state(paint));
            return;
        }
        if (text == null || text.isEmpty()) return;
        Graphics2D graphics = cpuGraphics(paint, false);
        try {
            int size = Math.max(1, Math.round(paint == null ? 16.0f : paint.k()));
            boolean bold = paint != null && paint.i() != null && paint.i().a();
            graphics.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size));
            FontMetrics metrics = graphics.getFontMetrics();
            float drawX = x;
            if (paint != null && paint.j() == Paint$Align.b) {
                drawX -= metrics.stringWidth(text) * 0.5f;
            } else if (paint != null && paint.j() == Paint$Align.c) {
                drawX -= metrics.stringWidth(text);
            }
            graphics.drawString(text, drawX, y);
        } finally {
            finishCpuDraw(graphics);
        }
    }

    private Graphics2D cpuGraphics(Paint paint, boolean image) {
        if (cpuBufferedImage == null) cpuBufferedImage = wrapPixels(renderTarget);
        if (persistentCpuGraphics == null) {
            persistentCpuGraphics = cpuBufferedImage.createGraphics();
        }
        Graphics2D graphics = persistentCpuGraphics;
        // Wheel zoom can redraw hundreds of cached terrain tiles. Reusing the target Graphics2D
        // avoids constructing and disposing an entire Java2D pipeline for each tile. Reset all
        // mutable state so reuse remains equivalent to BufferedImage.createGraphics().
        graphics.setTransform(new AffineTransform());
        graphics.setClip(null);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                paint != null && paint.c()
                        ? RenderingHints.VALUE_ANTIALIAS_ON
                        : RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                paint != null && paint.c()
                        ? RenderingHints.VALUE_INTERPOLATION_BILINEAR
                        : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (clip != null) {
            graphics.setClip(Math.round(clip.x()), Math.round(clip.y()),
                    Math.round(clip.width()), Math.round(clip.height()));
        }
        graphics.transform(new AffineTransform(
                transform.m00(), transform.m10(), transform.m01(), transform.m11(),
                transform.m02(), transform.m12()));
        int argb = paint == null ? 0xffffffff : paint.e();
        graphics.setColor(new Color(argb, true));
        graphics.setStroke(new BasicStroke(Math.max(1.0f,
                paint == null ? 1.0f : paint.g()), BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        if (image) {
            graphics.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, ((argb >>> 24) & 255) / 255.0f));
        } else {
            graphics.setComposite(AlphaComposite.SrcOver);
        }
        return graphics;
    }

    private void finishCpuDraw(Graphics2D graphics) {
        // The child backend owns this Graphics2D and releases it from dispose().
        renderTarget.version++;
    }

    private static BufferedImage wrapPixels(GameImage image) {
        if (image instanceof VulkanGameImage) {
            return ((VulkanGameImage) image).bufferedImage();
        }
        image.ensurePixelBuffer();
        int width = image.getWidth();
        int height = image.getHeight();
        if (image.pixelBuffer == null || image.pixelBuffer.length < width * height) {
            image.pixelBuffer = new int[Math.multiplyExact(width, height)];
        }
        DataBufferInt buffer = new DataBufferInt(image.pixelBuffer, width * height);
        int[] masks = { 0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000 };
        WritableRaster raster = java.awt.image.Raster.createPackedRaster(
                buffer, width, height, width, masks, null);
        DirectColorModel colors = new DirectColorModel(
                ColorSpace.getInstance(ColorSpace.CS_sRGB), 32,
                masks[0], masks[1], masks[2], masks[3], false, DataBuffer.TYPE_INT);
        return new BufferedImage(colors, raster, false, null);
    }
}
