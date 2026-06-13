package io.github.endx.rustedfabricexample;

import io.github.endx.rustedfabricapi.api.diagnostic.FileSystemDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.GameEngineDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.MappingEvidenceDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.RenderCanvasDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.RenderGlDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.SlickRuntimeDiagnostics;

import java.util.List;
import java.util.Map;

final class ExampleDiagnosticActions {
    private ExampleDiagnosticActions() {
    }

    static void showFileSystemSnapshot(String stage) {
        try {
            Map<String, Object> state = FileSystemDiagnostics.describeGameFileSystemState();
            Map<String, Object> active = castMap(state.get("activeBackendDescription"));
            Map<String, Object> capabilities = FileSystemDiagnostics.describeStorageBackendCapabilities(false);
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "FS active=" + ExampleDebugOverlay.safeText(String.valueOf(active.get("backendName")))
                            + " class=" + ExampleDebugOverlay.describeObject(state.get("activeBackend"))
                            + " direct=" + capabilities.get("directFileAccessAvailable")
                            + " abstract=" + active.get("useAbstractPaths")
                            + " ext=" + ExampleDebugOverlay.compactPath(String.valueOf(active.get("externalStoragePath"))),
                    state.get("activeBackend"));
        } catch (Throwable t) {
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "FS snapshot failed: " + t.getClass().getSimpleName()
                            + ": " + ExampleDebugOverlay.safeText(t.getMessage()),
                    null);
            ExampleMod.log("FS snapshot failed: " + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    static void showRenderSnapshot(String stage, Object frameRenderer) {
        try {
            Map<String, Object> engine = GameEngineDiagnostics.describeCurrentEngine();
            Object graphicsEngine = GameEngineDiagnostics.currentGraphicsEngine();
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Render engine hw=" + engine.get("useHardwareRendering")
                            + " desktopGl=" + engine.get("useDesktopOpenGL")
                            + " canvasGl=" + engine.get("useCanvasGl")
                            + " graphics=" + ExampleDebugOverlay.describeObject(graphicsEngine),
                    graphicsEngine);

            if (SlickRuntimeDiagnostics.isSlickGame(frameRenderer)) {
                showSlickRenderSnapshot(stage, frameRenderer);
            } else {
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "Render frameRenderer=" + ExampleDebugOverlay.describeObject(frameRenderer),
                        frameRenderer);
            }
        } catch (Throwable t) {
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Render snapshot failed: " + t.getClass().getSimpleName()
                            + ": " + ExampleDebugOverlay.safeText(t.getMessage()),
                    frameRenderer);
            ExampleMod.log("Render snapshot failed: " + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    static void showEvidenceSnapshot(String stage) {
        try {
            List<MappingEvidenceDiagnostics.EvidenceManifestRow> manifest =
                    MappingEvidenceDiagnostics.allEvidenceManifestRows();
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Evidence manifest=" + manifest.size()
                            + " fsRows=" + MappingEvidenceDiagnostics.allFileSystemBackendRows().size()
                            + " glRows=" + MappingEvidenceDiagnostics.allRenderGlBackendRows().size()
                            + " glTextRows=" + MappingEvidenceDiagnostics.allRenderGlTextRows().size()
                            + " ids=" + MappingEvidenceDiagnostics.evidenceResourceIds().size(),
                    null);
        } catch (Throwable t) {
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Evidence snapshot failed: " + t.getClass().getSimpleName()
                            + ": " + ExampleDebugOverlay.safeText(t.getMessage()),
                    null);
            ExampleMod.log("Evidence snapshot failed: " + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    private static void showSlickRenderSnapshot(String stage, Object frameRenderer) {
        Map<String, Object> slick = SlickRuntimeDiagnostics.describeSlickGame(frameRenderer);
        Object graphicsContext = SlickRuntimeDiagnostics.graphicsContext(frameRenderer);
        ExampleDebugOverlay.enqueueOverlayMessage(stage,
                "Slick delta=" + slick.get("lastDeltaMs")
                        + " loaded=" + slick.get("finishedInitialLoad")
                        + " ctx=" + ExampleDebugOverlay.describeObject(graphicsContext),
                graphicsContext);

        if (!RenderCanvasDiagnostics.isCanvasDrawTarget(graphicsContext)) {
            return;
        }

        Map<String, Object> target = RenderCanvasDiagnostics.describeCanvasDrawTarget(graphicsContext);
        ExampleDebugOverlay.enqueueOverlayMessage(stage,
                "Canvas queued=" + target.get("queued")
                        + " gl=" + target.get("glCanvasDrawTarget")
                        + " noOp=" + target.get("noOp")
                        + " recording=" + target.get("recordingEnabled"),
                graphicsContext);

        if (RenderCanvasDiagnostics.isGlCanvasDrawTarget(graphicsContext)) {
            Object glCanvas = RenderCanvasDiagnostics.glCanvasFromGlCanvasDrawTarget(graphicsContext);
            Object glRenderer = RenderCanvasDiagnostics.glRendererFromGlCanvasDrawTarget(graphicsContext);
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "GL field glCanvas=" + summarizeGlObject(glCanvas)
                            + " glRenderer=" + summarizeGlObject(glRenderer),
                    graphicsContext);
        }
    }

    private static String summarizeGlObject(Object value) {
        if (value == null) {
            return "null";
        }

        try {
            Map<String, Object> renderer = RenderGlDiagnostics.describeGlRenderer(value);
            return ExampleDebugOverlay.describeObject(value)
                    + "{cache=" + renderer.get("bitmapTextureCacheSize") + "}";
        } catch (RuntimeException ignored) {
        }

        try {
            Map<String, Object> canvas = RenderGlDiagnostics.describeGlCanvas(value);
            return ExampleDebugOverlay.describeObject(value)
                    + "{gles20=" + canvas.get("gles20Canvas") + "}";
        } catch (RuntimeException ignored) {
        }

        return ExampleDebugOverlay.describeObject(value);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?>) {
            return (Map<String, Object>) value;
        }
        return java.util.Collections.emptyMap();
    }
}
