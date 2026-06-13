package io.github.endx.rustedfabricexample;

import android.graphics.Paint;
import android.graphics.Paint$Style;
import android.graphics.Rect;
import io.github.endx.rustedfabricapi.api.RustedCustomUnitRegistry;
import io.github.endx.rustedfabricapi.api.event.GameLifecycleEvents;
import io.github.endx.rustedfabricapi.api.ini.RustedIniDiagnostics;
import rustedwarfare.core.GameEngine;
import rustedwarfare.render.GraphicsEngine;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

final class ExampleDebugOverlay {
    private static final AtomicBoolean OVERLAY_RENDERER_REGISTERED = new AtomicBoolean();
    private static final AtomicBoolean JAVA_DEBUG_UNIT_REGISTERED = new AtomicBoolean();
    private static final AtomicBoolean OVERLAY_RENDER_HOOK_SEEN = new AtomicBoolean();
    private static final AtomicBoolean OVERLAY_SLICK_DRAW_SEEN = new AtomicBoolean();
    private static final AtomicBoolean DEBUG_PANEL_SLICK_DRAW_SEEN = new AtomicBoolean();
    private static final AtomicBoolean OVERLAY_DIMENSIONS_LOGGED = new AtomicBoolean();
    private static final Object MAP_ENTRY_MESSAGE_LOCK = new Object();
    private static final Object EVENT_PROBE_MESSAGE_LOCK = new Object();
    private static final Object OVERLAY_LOCK = new Object();
    private static final Object DEBUG_PANEL_LOCK = new Object();
    private static final int OVERLAY_BORDER_COLOR = 0xFF44FF66;
    private static final int OVERLAY_TEXT_COLOR = 0xFFFFFFFF;
    private static final int MAX_OVERLAY_MESSAGES = 5;
    private static final float SLICK_TEXT_SCALE = 0.78f;
    private static final List<OverlayMessage> OVERLAY_MESSAGES = new ArrayList<>();
    private static final Rect OVERLAY_RECT = new Rect();
    private static final Map<DebugProbeGroup, Boolean> DEBUG_PROBE_GROUPS = new EnumMap<>(DebugProbeGroup.class);
    private static final Map<DebugRenderPart, Boolean> DEBUG_RENDER_PARTS = new EnumMap<>(DebugRenderPart.class);
    private static volatile boolean debugPanelOpen;
    private static volatile boolean invincibleUnitsEnabled;
    private static Paint overlayFillPaint;
    private static Paint overlayBorderPaint;
    private static Paint overlayTextPaint;
    private static Field slickGameContainerField;
    private static Field slickGraphicsField;
    private static Method slickGameContainerGetWidthMethod;
    private static Method slickGameContainerGetHeightMethod;
    private static Method slickGameContainerGetInputMethod;
    private static Method slickInputGetMouseXMethod;
    private static Method slickInputGetMouseYMethod;
    private static Method slickInputIsMousePressedMethod;
    private static Method slickInputIsMouseButtonDownMethod;
    private static Method slickResetTransformMethod;
    private static Method slickResetFontMethod;
    private static Method slickPushTransformMethod;
    private static Method slickPopTransformMethod;
    private static Method slickScaleMethod;
    private static Method slickClearClipMethod;
    private static Method slickClearWorldClipMethod;
    private static Method slickSetColorMethod;
    private static Method slickFillRectMethod;
    private static Method slickDrawRectMethod;
    private static Method slickDrawStringMethod;
    private static Method slickSetLineWidthMethod;
    private static Method slickResetLineWidthMethod;
    private static Method slickFlushMethod;
    private static Method slickGetFontMethod;
    private static Method slickFontGetWidthMethod;
    private static Object slickFillColor;
    private static Object slickBorderColor;
    private static Object slickTextColor;
    private static volatile Path rustedGameDir;
    private static boolean slickMouseWasDown;
    private static String lastMapEntryMessageMapPath;
    private static long lastMapEntryMessageMillis;
    private static final Map<String, Long> LAST_EVENT_PROBE_MILLIS_BY_KEY = new HashMap<>();
    private static long lastOverlayDrawFailureLogMillis;
    private static long lastDebugPanelDrawFailureLogMillis;

    static {
        for (DebugProbeGroup group : DebugProbeGroup.values()) {
            DEBUG_PROBE_GROUPS.put(group, Boolean.TRUE);
        }
        for (DebugRenderPart part : DebugRenderPart.values()) {
            DEBUG_RENDER_PARTS.put(part, Boolean.TRUE);
        }
    }

    private ExampleDebugOverlay() {
    }

    static void rememberGameDir(Path gameDir) {
        if (gameDir != null) {
            rustedGameDir = gameDir;
        }
    }

    static void registerRenderer(String stage) {
        if (OVERLAY_RENDERER_REGISTERED.compareAndSet(false, true)) {
            GameLifecycleEvents.AFTER_FRAME_RENDER.register(ExampleDebugOverlay::drawFrameOverlays);
            ExampleMod.log("registered green overlay/frame debug renderer from " + stage);
        }
    }

    static void showMapEntryMessage(String stage, Object map) {
        if (!isDebugProbeGroupEnabled(DebugProbeGroup.MAP)) {
            return;
        }

        String mapPath = getCurrentMapPathForLog();
        if (isDuplicateMapEntryMessage(mapPath)) {
            return;
        }

        enqueueOverlayMessage(stage, "AfterMapSetup " + formatMapPath(mapPath), map);
    }

    static void showEventProbeMessage(String stage, String message, Object source) {
        showEventProbeMessage(stage, message, message, source, 1500L);
    }

    static void showEventProbeMessage(String stage, String key, String message, Object source, long minIntervalMillis) {
        if (!isDebugProbeGroupEnabled(classifyDebugProbeGroup(key))) {
            return;
        }

        if (isDuplicateEventProbeMessage(key, minIntervalMillis)) {
            return;
        }

        enqueueOverlayMessage(stage, message, source);
    }

    static void enqueueOverlayMessage(String stage, String message, Object source) {
        String text = formatOverlayMessage(stage, message);
        synchronized (OVERLAY_LOCK) {
            OVERLAY_MESSAGES.add(new OverlayMessage(text));
            while (OVERLAY_MESSAGES.size() > MAX_OVERLAY_MESSAGES) {
                OVERLAY_MESSAGES.remove(0);
            }
        }

        ExampleMod.log("queued overlay message from " + stage
                + ": " + text
                + ", source=" + (source != null ? source.getClass().getName() : "null"));
    }

    static void drawFrameOverlays(Object renderer) {
        drawOverlayMessages(renderer);
        drawDebugPanelFromFrame(renderer);
    }

    static void drawDebugPanelFromFrame(Object frameRenderer) {
        try {
            drawDebugPanelFromFrameUnchecked(frameRenderer);
        } catch (Throwable t) {
            logDebugPanelDrawFailure(t);
        }
    }

    static void drawDebugPanelFromFrameUnchecked(Object frameRenderer) throws ReflectiveOperationException {
        GameEngine engine = GameEngine.getInstance();
        if (engine == null || engine.bO == null) {
            return;
        }

        Object graphics = getSlickGraphics(engine.bO);
        Object gameContainer = getSlickGameContainer(frameRenderer);
        if (graphics == null || gameContainer == null) {
            return;
        }

        initSlickReflection(graphics);
        SlickInputState input = readSlickInputState(gameContainer);
        int[] surfaceSize = getOverlaySurfaceSize(frameRenderer, engine);
        int screenWidth = surfaceSize[0];
        int margin = 24;
        int left = margin;
        int top = 72;
        int buttonWidth = 160;
        int buttonHeight = 30;
        int panelWidth = Math.min(470, Math.max(340, screenWidth - margin * 2));
        int rowHeight = 28;
        int labelHeight = 26;
        int sectionGap = 18;
        int groupRows = (DebugProbeGroup.values().length + 1) / 2;
        int renderRows = (DebugRenderPart.values().length + 1) / 2;
        int actionRows = 6;
        int panelHeight = 14 + labelHeight + groupRows * rowHeight
                + sectionGap + labelHeight + renderRows * rowHeight
                + sectionGap + labelHeight + actionRows * rowHeight + 14;

        if (DEBUG_PANEL_SLICK_DRAW_SEEN.compareAndSet(false, true)) {
            ExampleMod.log("Java Debug panel drawing with Slick Graphics: " + graphics.getClass().getName());
        }

        slickResetTransformMethod.invoke(graphics);
        slickResetFontMethod.invoke(graphics);
        slickClearClipMethod.invoke(graphics);
        slickClearWorldClipMethod.invoke(graphics);

        if (drawSlickButton(graphics, input, left, top, buttonWidth, buttonHeight,
                debugPanelOpen ? "Java Debug: ON" : "Java Debug")) {
            debugPanelOpen = !debugPanelOpen;
            enqueueOverlayMessage("debug", "Java Debug panel " + (debugPanelOpen ? "opened" : "closed"), frameRenderer);
        }

        if (debugPanelOpen) {
            int panelTop = top + buttonHeight + 8;
            drawSlickBox(graphics, left, panelTop, panelWidth, panelHeight);

            int contentLeft = left + 10;
            int y = panelTop + 20;
            drawSlickLabel(graphics, "Event probe groups", contentLeft, y);
            y += labelHeight;
            drawSlickDebugProbeGroupButtons(graphics, input, contentLeft, y, panelWidth - 20, rowHeight);

            y += groupRows * rowHeight + sectionGap;
            drawSlickLabel(graphics, "Render parts", contentLeft, y);
            y += labelHeight;
            drawSlickDebugRenderPartButtons(graphics, input, contentLeft, y, panelWidth - 20, rowHeight);

            y += renderRows * rowHeight + sectionGap;
            drawSlickLabel(graphics, "Actions", contentLeft, y);
            y += labelHeight;
            drawSlickDebugActionButtons(graphics, input, contentLeft, y, panelWidth - 20, frameRenderer);
        }

        slickFlushMethod.invoke(graphics);
    }

    static void drawSlickDebugProbeGroupButtons(Object graphics, SlickInputState input,
                                                        int left, int top, int width, int rowHeight)
            throws ReflectiveOperationException {
        DebugProbeGroup[] groups = DebugProbeGroup.values();
        int gap = 6;
        int cellWidth = (width - gap) / 2;
        for (int i = 0; i < groups.length; i++) {
            DebugProbeGroup group = groups[i];
            int x = left + (i % 2) * (cellWidth + gap);
            int y = top + (i / 2) * rowHeight;
            boolean enabled = isDebugProbeGroupEnabled(group);
            if (drawSlickButton(graphics, input, x, y, cellWidth, 24,
                    (enabled ? "[x] " : "[ ] ") + group.label)) {
                setDebugProbeGroupEnabled(group, !enabled);
                enqueueOverlayMessage("debug", group.label + " probes " + (!enabled ? "enabled" : "disabled"), graphics);
            }
        }
    }

    static void drawSlickDebugRenderPartButtons(Object graphics, SlickInputState input,
                                                        int left, int top, int width, int rowHeight)
            throws ReflectiveOperationException {
        DebugRenderPart[] parts = DebugRenderPart.values();
        int gap = 6;
        int cellWidth = (width - gap) / 2;
        for (int i = 0; i < parts.length; i++) {
            DebugRenderPart part = parts[i];
            int x = left + (i % 2) * (cellWidth + gap);
            int y = top + (i / 2) * rowHeight;
            boolean enabled = isDebugRenderPartEnabled(part);
            if (drawSlickButton(graphics, input, x, y, cellWidth, 24,
                    (enabled ? "[x] " : "[ ] ") + part.label)) {
                setDebugRenderPartEnabled(part, !enabled);
                enqueueOverlayMessage("debug", part.label + " " + (!enabled ? "enabled" : "disabled"), graphics);
            }
        }
    }

    static void drawSlickDebugActionButtons(Object graphics, SlickInputState input,
                                                    int left, int top, int width, Object frameRenderer)
            throws ReflectiveOperationException {
        int gap = 6;
        int cellWidth = (width - gap) / 2;
        String label = JAVA_DEBUG_UNIT_REGISTERED.get() ? "Java unit registered" : "Register Java unit";
        if (drawSlickButton(graphics, input, left, top, cellWidth, 24, label)) {
            registerJavaDebugUnitFromPanel();
        }

        boolean tracing = RustedIniDiagnostics.isKeyReadTracingEnabled();
        if (drawSlickButton(graphics, input, left + cellWidth + gap, top, cellWidth, 24,
                (tracing ? "[x] " : "[ ] ") + "INI trace")) {
            RustedIniDiagnostics.setKeyReadTracingEnabled(!tracing);
            enqueueOverlayMessage("debug", "INI key trace " + (!tracing ? "enabled" : "disabled"), graphics);
        }

        if (drawSlickButton(graphics, input, left, top + 28, cellWidth, 24,
                (invincibleUnitsEnabled ? "[x] " : "[ ] ") + "Invincible units")) {
            invincibleUnitsEnabled = !invincibleUnitsEnabled;
            enqueueOverlayMessage("debug",
                    "Invincible units " + (invincibleUnitsEnabled ? "enabled" : "disabled"),
                    graphics);
        }

        if (drawSlickButton(graphics, input, left + cellWidth + gap, top + 28, cellWidth, 24,
                "FS snapshot")) {
            ExampleDiagnosticActions.showFileSystemSnapshot("debug");
        }

        if (drawSlickButton(graphics, input, left, top + 56, cellWidth, 24,
                "Render snapshot")) {
            ExampleDiagnosticActions.showRenderSnapshot("debug", frameRenderer);
        }

        if (drawSlickButton(graphics, input, left + cellWidth + gap, top + 56, cellWidth, 24,
                "Evidence")) {
            ExampleDiagnosticActions.showEvidenceSnapshot("debug");
        }

        if (drawSlickButton(graphics, input, left, top + 84, cellWidth, 24,
                "Audio snapshot")) {
            ExampleDiagnosticActions.showAudioSnapshot("debug");
        }

        if (drawSlickButton(graphics, input, left + cellWidth + gap, top + 84, cellWidth, 24,
                "Input snapshot")) {
            ExampleDiagnosticActions.showInputSnapshot("debug");
        }

        if (drawSlickButton(graphics, input, left, top + 112, cellWidth, 24,
                "Network snapshot")) {
            ExampleDiagnosticActions.showNetworkSnapshot("debug");
        }

        if (drawSlickButton(graphics, input, left + cellWidth + gap, top + 112, cellWidth, 24,
                "Steam snapshot")) {
            ExampleDiagnosticActions.showSteamSnapshot("debug");
        }

        if (drawSlickButton(graphics, input, left, top + 140, cellWidth, 24,
                "UI snapshot")) {
            ExampleDiagnosticActions.showUiSnapshot("debug");
        }
    }

    static void registerJavaDebugUnitFromPanel() {
        if (!JAVA_DEBUG_UNIT_REGISTERED.compareAndSet(false, true)) {
            enqueueOverlayMessage("debug", "Java debug unit already registered", null);
            return;
        }

        try {
            Path gameDir = rustedGameDir != null ? rustedGameDir : new java.io.File(".").toPath();
            String unitRoot = "assets/builtin_mods/mega_builders/mega_builder/";
            String unitConfigPath = unitRoot + "mega_builder.ini";
            Path unitDir = gameDir.resolve("assets").resolve("builtin_mods")
                    .resolve("mega_builders").resolve("mega_builder");
            Path iniPath = unitDir.resolve("mega_builder.ini");
            String ini = new String(Files.readAllBytes(iniPath), StandardCharsets.UTF_8);
            ini = ini.replace("name: megaBuilder", "name: javaDebugBuilder");
            ini = ini.replace("overrideAndReplace: builder", "# overrideAndReplace removed by Java debug registration");
            ini = ini.replace("displayText: Mega Builder", "displayText: Java Debug Builder");
            ini = ini.replace("-From the example mod", "-Registered from Java through RustedCustomUnitRegistry");
            ini = ini.replace("#builtFrom_1_name: commandCenter", "builtFrom_1_name: commandCenter");
            ini = ini.replace("#builtFrom_1_pos:  1", "builtFrom_1_pos:  1");
            ini = ini.replace("#builtFrom_2_name: landFactory", "builtFrom_2_name: landFactory");
            ini = ini.replace("#builtFrom_2_pos:  1", "builtFrom_2_pos:  1");

            Object metadata = RustedCustomUnitRegistry.registerIniUnit(
                    unitConfigPath,
                    ini.getBytes(StandardCharsets.UTF_8),
                    null,
                    assetPath -> Files.newInputStream(gameDir.resolve(assetPath.replace('/', java.io.File.separatorChar))));
            String result = RustedCustomUnitRegistry.commitPendingCustomUnits(false);
            enqueueOverlayMessage("debug",
                    "Registered Java unit metadata=" + describeObject(metadata)
                            + " result=" + safeText(result),
                    metadata);
        } catch (Throwable t) {
            JAVA_DEBUG_UNIT_REGISTERED.set(false);
            String error = t.getClass().getSimpleName() + ": " + safeText(t.getMessage());
            enqueueOverlayMessage("debug", "Java unit registration failed: " + error, null);
            ExampleMod.log("Java unit registration failed: " + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    static DebugProbeGroup classifyDebugProbeGroup(String key) {
        String text = key != null ? key : "";
        if (containsAny(text, "Resource", "TakeResources", "Repair", "Reclaim", "Unbuild")) {
            return DebugProbeGroup.RESOURCE;
        }
        if (containsAny(text, "Save", "Replay", "Checksum", "Serialize", "Deserialize", "WriteSave", "ReadSave", "NetworkResync")) {
            return DebugProbeGroup.SAVE;
        }
        if (containsAny(text, "Map", "Tmx", "Tileset", "Mission", "StartingUnitSpawn", "TileProperty", "ExtraMaps", "NetworkMap")) {
            return DebugProbeGroup.MAP;
        }
        if (containsAny(text, "Audio", "Sound", "Music", "OpenAL")) {
            return DebugProbeGroup.AUDIO;
        }
        if (containsAny(text, "Input", "KeyBinding", "Keycode", "KeyInput", "ControllerInput")) {
            return DebugProbeGroup.INPUT;
        }
        if (containsAny(text, "Ui", "UI", "LibRocket", "RootScript", "ScriptEngine", "DocumentLoaded",
                "DocumentShown", "PasswordPromptPopup")) {
            return DebugProbeGroup.UI;
        }
        if (containsAny(text, "Network", "Steam", "Lobby", "ServerList", "MasterServer", "PasswordPrompt",
                "ForwardedSocket", "ForwardedPacket")) {
            return DebugProbeGroup.NETWORK;
        }
        if (containsAny(text, "LoadImage", "TeamColor", "LoadSound", "ParseSoundList",
                "NativeCustomUnit", "CustomUnitRegistry", "CustomUnitOverride", "CustomUnitLink",
                "ResolveAbstractPath", "AssetCached", "CachedAssetDirectory", "FileSystem")) {
            return DebugProbeGroup.ASSET;
        }
        if (containsAny(text, "ParseStream", "ParseUnitConfig", "CopyFrom", "StaticVariables", "MetadataParsed", "PendingRegister", "CustomUnitCommit", "RebuildCustomUnitLinks", "ValidateCustomUnitLinks")) {
            return DebugProbeGroup.LIFECYCLE;
        }
        if (containsAny(text, "GetBodyImage", "GetTurretImage", "GetShieldImage", "GetShadowImage", "GetZoomed", "FrameSourceRect", "ImageDestinationRect", "Draw", "TurretWorldTransform", "FrameRender", "FrameUpdate")) {
            return DebugProbeGroup.RENDER;
        }
        if (containsAny(text, "Select", "Selection", "CommandIssue")) {
            return DebugProbeGroup.SELECTION;
        }
        if (containsAny(text, "Action", "TurretFire", "Projectile", "FireProjectile", "Convert", "Transport", "BuildQueue", "MutableStats")) {
            return DebugProbeGroup.ACTION;
        }
        if (containsAny(text, "Unit", "Metadata", "Killed", "Removed", "Register")) {
            return DebugProbeGroup.LIFECYCLE;
        }
        return DebugProbeGroup.GAME;
    }

    static boolean containsAny(String value, String... patterns) {
        for (String pattern : patterns) {
            if (value.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    static boolean isDebugProbeGroupEnabled(DebugProbeGroup group) {
        synchronized (DEBUG_PANEL_LOCK) {
            return Boolean.TRUE.equals(DEBUG_PROBE_GROUPS.get(group));
        }
    }

    static void setDebugProbeGroupEnabled(DebugProbeGroup group, boolean enabled) {
        synchronized (DEBUG_PANEL_LOCK) {
            DEBUG_PROBE_GROUPS.put(group, Boolean.valueOf(enabled));
        }
    }

    static boolean isDebugRenderPartEnabled(DebugRenderPart part) {
        synchronized (DEBUG_PANEL_LOCK) {
            return Boolean.TRUE.equals(DEBUG_RENDER_PARTS.get(part));
        }
    }

    static void setDebugRenderPartEnabled(DebugRenderPart part, boolean enabled) {
        synchronized (DEBUG_PANEL_LOCK) {
            DEBUG_RENDER_PARTS.put(part, Boolean.valueOf(enabled));
        }
    }

    static boolean isInvincibleUnitsEnabled() {
        return invincibleUnitsEnabled;
    }

    static void logDebugPanelDrawFailure(Throwable t) {
        long now = System.currentTimeMillis();
        if (now - lastDebugPanelDrawFailureLogMillis < 5000L) {
            return;
        }

        lastDebugPanelDrawFailureLogMillis = now;
        ExampleMod.log("debug panel draw failed: " + t.getClass().getName() + ": " + t.getMessage());
    }

    static void drawOverlayMessages(Object renderer) {
        try {
            if (OVERLAY_RENDER_HOOK_SEEN.compareAndSet(false, true)) {
                ExampleMod.log("green overlay render hook reached: "
                        + (renderer != null ? renderer.getClass().getName() : "null"));
            }
            drawOverlayMessagesUnchecked(renderer);
        } catch (Throwable t) {
            logOverlayDrawFailure(t);
        }
    }

    static void drawOverlayMessagesUnchecked(Object frameRenderer) throws ReflectiveOperationException {
        GameEngine engine = GameEngine.getInstance();
        if (engine == null || engine.bO == null) {
            return;
        }

        List<OverlayMessage> visibleMessages = collectVisibleOverlayMessages();
        if (visibleMessages.isEmpty()) {
            return;
        }

        GraphicsEngine renderer = engine.bO;
        int[] surfaceSize = getOverlaySurfaceSize(frameRenderer, engine);
        int screenWidth = surfaceSize[0];
        int screenHeight = surfaceSize[1];
        int margin = 24;
        int boxWidth = Math.min(screenWidth - margin * 2, Math.max(760, (int) (screenWidth * 0.82f)));
        int boxHeight = 32;
        int gap = 6;
        int left = margin;
        int totalHeight = visibleMessages.size() * boxHeight + Math.max(0, visibleMessages.size() - 1) * gap;
        int top = Math.max(margin, screenHeight - margin - totalHeight);
        int maxTextWidth = boxWidth - 20;

        Object slickGraphics = getSlickGraphics(renderer);
        if (slickGraphics != null) {
            drawSlickOverlay(slickGraphics, visibleMessages, left, top, boxWidth, boxHeight, gap, maxTextWidth);
            return;
        }

        initOverlayPaints();
        for (int i = 0; i < visibleMessages.size(); i++) {
            OverlayMessage message = visibleMessages.get(i);
            int yTop = top + i * (boxHeight + gap);
            OVERLAY_RECT.a(left, yTop, left + boxWidth, yTop + boxHeight);
            renderer.b(OVERLAY_RECT, overlayFillPaint);
            renderer.b(OVERLAY_RECT, overlayBorderPaint);
            renderer.a(fitOverlayText(renderer, message.text, maxTextWidth),
                    left + 10.0f, yTop + 22.0f, overlayTextPaint);
        }
    }

    static int[] getOverlaySurfaceSize(Object frameRenderer, GameEngine engine) throws ReflectiveOperationException {
        int screenWidth = 0;
        int screenHeight = 0;
        Object gameContainer = getSlickGameContainer(frameRenderer);
        if (gameContainer != null) {
            if (slickGameContainerGetWidthMethod == null) {
                Class<?> containerClass = gameContainer.getClass();
                slickGameContainerGetWidthMethod = containerClass.getMethod("getWidth");
                slickGameContainerGetHeightMethod = containerClass.getMethod("getHeight");
            }
            screenWidth = (Integer) slickGameContainerGetWidthMethod.invoke(gameContainer);
            screenHeight = (Integer) slickGameContainerGetHeightMethod.invoke(gameContainer);
        }

        if (screenWidth <= 0) {
            screenWidth = engine.bA > 0 ? engine.bA : 1280;
        }
        if (screenHeight <= 0) {
            screenHeight = engine.bB > 0 ? engine.bB : 720;
        }

        if (OVERLAY_DIMENSIONS_LOGGED.compareAndSet(false, true)) {
            ExampleMod.log("green overlay dimensions: surface=" + screenWidth + "x" + screenHeight
                    + ", engine=" + engine.bA + "x" + engine.bB
                    + ", frameRenderer=" + (frameRenderer != null ? frameRenderer.getClass().getName() : "null")
                    + ", gameContainer=" + (gameContainer != null ? gameContainer.getClass().getName() : "null"));
        }

        return new int[]{screenWidth, screenHeight};
    }

    static Object getSlickGameContainer(Object frameRenderer) throws ReflectiveOperationException {
        if (frameRenderer == null) {
            return null;
        }

        if (slickGameContainerField == null) {
            for (Field field : frameRenderer.getClass().getDeclaredFields()) {
                if ("org.newdawn.slick.GameContainer".equals(field.getType().getName())) {
                    field.setAccessible(true);
                    slickGameContainerField = field;
                    break;
                }
            }
        }
        return slickGameContainerField != null ? slickGameContainerField.get(frameRenderer) : null;
    }

    static Object getSlickGraphics(GraphicsEngine renderer) throws ReflectiveOperationException {
        if (renderer == null) {
            return null;
        }

        if (slickGraphicsField == null) {
            slickGraphicsField = renderer.getClass().getField("f");
        }
        return slickGraphicsField.get(renderer);
    }

    static void drawSlickBox(Object graphics, int left, int top, int width, int height)
            throws ReflectiveOperationException {
        slickSetColorMethod.invoke(graphics, slickFillColor);
        slickFillRectMethod.invoke(graphics, (float) left, (float) top, (float) width, (float) height);

        slickSetColorMethod.invoke(graphics, slickBorderColor);
        slickSetLineWidthMethod.invoke(graphics, 2.0f);
        slickDrawRectMethod.invoke(graphics, (float) left, (float) top, (float) width, (float) height);
        slickResetLineWidthMethod.invoke(graphics);
    }

    static void drawSlickLabel(Object graphics, String text, int x, int y)
            throws ReflectiveOperationException {
        slickSetColorMethod.invoke(graphics, slickTextColor);
        slickResetFontMethod.invoke(graphics);
        drawScaledSlickText(graphics, text, x, y);
    }

    static boolean drawSlickButton(Object graphics, SlickInputState input,
                                           int left, int top, int width, int height, String label)
            throws ReflectiveOperationException {
        drawSlickBox(graphics, left, top, width, height);
        drawSlickLabel(graphics, fitSlickOverlayText(graphics, label, width - 14),
                left + 7, top + Math.max(5, (height - 14) / 2));
        return input != null && input.clicked
                && input.mouseX >= left && input.mouseX <= left + width
                && input.mouseY >= top && input.mouseY <= top + height;
    }

    static SlickInputState readSlickInputState(Object gameContainer) throws ReflectiveOperationException {
        Object input = getSlickInput(gameContainer);
        if (input == null) {
            return null;
        }

        int mouseX = (Integer) slickInputGetMouseXMethod.invoke(input);
        int mouseY = (Integer) slickInputGetMouseYMethod.invoke(input);
        boolean down = false;
        if (slickInputIsMouseButtonDownMethod != null) {
            down = Boolean.TRUE.equals(slickInputIsMouseButtonDownMethod.invoke(input, 0));
        }

        boolean clicked;
        if (slickInputIsMousePressedMethod != null) {
            clicked = Boolean.TRUE.equals(slickInputIsMousePressedMethod.invoke(input, 0));
        } else {
            clicked = down && !slickMouseWasDown;
        }
        slickMouseWasDown = down;
        return new SlickInputState(mouseX, mouseY, clicked);
    }

    static Object getSlickInput(Object gameContainer) throws ReflectiveOperationException {
        if (gameContainer == null) {
            return null;
        }

        if (slickGameContainerGetInputMethod == null) {
            slickGameContainerGetInputMethod = gameContainer.getClass().getMethod("getInput");
        }

        Object input = slickGameContainerGetInputMethod.invoke(gameContainer);
        if (input == null) {
            return null;
        }

        if (slickInputGetMouseXMethod == null) {
            Class<?> inputClass = input.getClass();
            slickInputGetMouseXMethod = inputClass.getMethod("getMouseX");
            slickInputGetMouseYMethod = inputClass.getMethod("getMouseY");
            try {
                slickInputIsMousePressedMethod = inputClass.getMethod("isMousePressed", int.class);
            } catch (NoSuchMethodException ignored) {
            }
            try {
                slickInputIsMouseButtonDownMethod = inputClass.getMethod("isMouseButtonDown", int.class);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return input;
    }

    static void drawSlickOverlay(Object graphics, List<OverlayMessage> messages,
                                         int left, int top, int boxWidth, int boxHeight, int gap, int maxTextWidth)
            throws ReflectiveOperationException {
        initSlickReflection(graphics);

        if (OVERLAY_SLICK_DRAW_SEEN.compareAndSet(false, true)) {
            ExampleMod.log("green overlay drawing with Slick Graphics: " + graphics.getClass().getName());
        }

        slickResetTransformMethod.invoke(graphics);
        slickResetFontMethod.invoke(graphics);
        slickClearClipMethod.invoke(graphics);
        slickClearWorldClipMethod.invoke(graphics);

        for (int i = 0; i < messages.size(); i++) {
            OverlayMessage message = messages.get(i);
            float yTop = top + i * (boxHeight + gap);

            slickSetColorMethod.invoke(graphics, slickFillColor);
            slickFillRectMethod.invoke(graphics, (float) left, yTop, (float) boxWidth, (float) boxHeight);

            slickSetColorMethod.invoke(graphics, slickBorderColor);
            slickSetLineWidthMethod.invoke(graphics, 2.0f);
            slickDrawRectMethod.invoke(graphics, (float) left, yTop, (float) boxWidth, (float) boxHeight);
            slickResetLineWidthMethod.invoke(graphics);

            slickSetColorMethod.invoke(graphics, slickTextColor);
            slickResetFontMethod.invoke(graphics);
            drawScaledSlickText(graphics, fitSlickOverlayText(graphics, message.text, maxTextWidth),
                    left + 10.0f, yTop + 7.0f);
        }

        slickFlushMethod.invoke(graphics);
    }

    static void drawScaledSlickText(Object graphics, String text, float x, float y)
            throws ReflectiveOperationException {
        slickPushTransformMethod.invoke(graphics);
        try {
            slickScaleMethod.invoke(graphics, SLICK_TEXT_SCALE, SLICK_TEXT_SCALE);
            slickDrawStringMethod.invoke(graphics, text, x / SLICK_TEXT_SCALE, y / SLICK_TEXT_SCALE);
        } finally {
            slickPopTransformMethod.invoke(graphics);
        }
    }

    static void initSlickReflection(Object graphics) throws ReflectiveOperationException {
        if (slickSetColorMethod != null) {
            return;
        }

        Class<?> graphicsClass = graphics.getClass();
        Class<?> colorClass = Class.forName("org.newdawn.slick.Color", false, graphicsClass.getClassLoader());
        Constructor<?> colorConstructor = colorClass.getConstructor(int.class, int.class, int.class, int.class);

        slickFillColor = colorConstructor.newInstance(6, 26, 11, 210);
        slickBorderColor = colorConstructor.newInstance(68, 255, 102, 255);
        slickTextColor = colorConstructor.newInstance(255, 255, 255, 255);

        slickResetTransformMethod = graphicsClass.getMethod("resetTransform");
        slickResetFontMethod = graphicsClass.getMethod("resetFont");
        slickPushTransformMethod = graphicsClass.getMethod("pushTransform");
        slickPopTransformMethod = graphicsClass.getMethod("popTransform");
        slickScaleMethod = graphicsClass.getMethod("scale", float.class, float.class);
        slickClearClipMethod = graphicsClass.getMethod("clearClip");
        slickClearWorldClipMethod = graphicsClass.getMethod("clearWorldClip");
        slickSetColorMethod = graphicsClass.getMethod("setColor", colorClass);
        slickFillRectMethod = graphicsClass.getMethod("fillRect", float.class, float.class, float.class, float.class);
        slickDrawRectMethod = graphicsClass.getMethod("drawRect", float.class, float.class, float.class, float.class);
        slickDrawStringMethod = graphicsClass.getMethod("drawString", String.class, float.class, float.class);
        slickSetLineWidthMethod = graphicsClass.getMethod("setLineWidth", float.class);
        slickResetLineWidthMethod = graphicsClass.getMethod("resetLineWidth");
        slickFlushMethod = graphicsClass.getMethod("flush");
        slickGetFontMethod = graphicsClass.getMethod("getFont");
    }

    static String fitSlickOverlayText(Object graphics, String text, int maxWidth) throws ReflectiveOperationException {
        if (text == null) {
            return "";
        }

        int scaledMaxWidth = (int) (maxWidth / SLICK_TEXT_SCALE);
        if (getSlickTextWidth(graphics, text) <= scaledMaxWidth) {
            return text;
        }

        String suffix = "...";
        int low = 0;
        int high = text.length();
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if (getSlickTextWidth(graphics, text.substring(0, mid) + suffix) <= scaledMaxWidth) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low > 0 ? text.substring(0, low) + suffix : suffix;
    }

    static int getSlickTextWidth(Object graphics, String text) throws ReflectiveOperationException {
        Object font = slickGetFontMethod.invoke(graphics);
        if (slickFontGetWidthMethod == null) {
            slickFontGetWidthMethod = font.getClass().getMethod("getWidth", String.class);
        }
        return (Integer) slickFontGetWidthMethod.invoke(font, text);
    }

    static void logOverlayDrawFailure(Throwable t) {
        long now = System.currentTimeMillis();
        if (now - lastOverlayDrawFailureLogMillis < 5000L) {
            return;
        }

        lastOverlayDrawFailureLogMillis = now;
        ExampleMod.log("overlay draw failed: " + t.getClass().getName() + ": " + t.getMessage());
    }

    static List<OverlayMessage> collectVisibleOverlayMessages() {
        synchronized (OVERLAY_LOCK) {
            return new ArrayList<>(OVERLAY_MESSAGES);
        }
    }

    static void initOverlayPaints() {
        if (overlayFillPaint != null) {
            return;
        }

        overlayFillPaint = new Paint();
        overlayFillPaint.a(Paint$Style.a);
        overlayFillPaint.a(175, 4, 18, 8);

        overlayBorderPaint = new Paint();
        overlayBorderPaint.a(Paint$Style.b);
        overlayBorderPaint.a(true);
        overlayBorderPaint.b(OVERLAY_BORDER_COLOR);
        overlayBorderPaint.a(2.0f);

        overlayTextPaint = new Paint();
        overlayTextPaint.a(true);
        overlayTextPaint.b(OVERLAY_TEXT_COLOR);
        overlayTextPaint.b(13.0f);
    }

    static String fitOverlayText(GraphicsEngine renderer, String text, int maxWidth) {
        if (text == null) {
            return "";
        }

        if (renderer.a(text, overlayTextPaint) <= maxWidth) {
            return text;
        }

        String suffix = "...";
        int end = text.length();
        while (end > 0 && renderer.a(text.substring(0, end) + suffix, overlayTextPaint) > maxWidth) {
            end--;
        }
        return end > 0 ? text.substring(0, end) + suffix : suffix;
    }

    static String formatOverlayMessage(String stage, String message) {
        return "[" + stage + "] " + message;
    }

    static String formatMapPath(String mapPath) {
        if (mapPath == null || mapPath.trim().isEmpty()) {
            return "map=<unknown>";
        }

        return "map=" + compactPath(mapPath);
    }

    static String compactPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "<unknown>";
        }

        String normalized = path.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
    }

    static int countArray(String[] values) {
        return values != null ? values.length : 0;
    }

    static int countBytes(byte[] values) {
        return values != null ? values.length : 0;
    }

    static int countProperties(Properties properties) {
        return properties != null ? properties.size() : 0;
    }

    static String safeText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "<none>";
        }

        return value.replace('\n', ' ').replace('\r', ' ');
    }

    static String describeObject(Object value) {
        if (value == null) {
            return "null";
        }

        try {
            String className = value.getClass().getName();
            int lastDot = className.lastIndexOf('.');
            String simpleName = lastDot >= 0 ? className.substring(lastDot + 1) : className;
            String text = String.valueOf(value);
            if (text == null || text.trim().isEmpty() || text.startsWith(className + "@")) {
                return simpleName;
            }

            text = text.replace('\n', ' ').replace('\r', ' ');
            if (text.length() > 48) {
                text = text.substring(0, 45) + "...";
            }
            return simpleName + ":" + text;
        } catch (Throwable t) {
            return value.getClass().getSimpleName();
        }
    }

    static String formatPoint(float x, float y) {
        return formatFloat(x) + "," + formatFloat(y);
    }

    static String formatFloat(float value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    static String formatDouble(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    static String getCurrentMapPathForLog() {
        try {
            GameEngine engine = GameEngine.getInstance();
            if (engine == null) {
                return null;
            }

            String mapPath = engine.getCurrentMapPath();
            if (mapPath == null || mapPath.trim().isEmpty()) {
                mapPath = engine.currentMapPath;
            }
            return mapPath;
        } catch (Throwable t) {
            return null;
        }
    }

    static boolean isDuplicateMapEntryMessage(String mapPath) {
        long now = System.currentTimeMillis();
        String key = mapPath != null ? mapPath : "<unknown>";
        synchronized (MAP_ENTRY_MESSAGE_LOCK) {
            if (key.equals(lastMapEntryMessageMapPath) && now - lastMapEntryMessageMillis < 1500L) {
                return true;
            }

            lastMapEntryMessageMapPath = key;
            lastMapEntryMessageMillis = now;
            return false;
        }
    }

    static boolean isDuplicateEventProbeMessage(String key, long minIntervalMillis) {
        long now = System.currentTimeMillis();
        synchronized (EVENT_PROBE_MESSAGE_LOCK) {
            Long lastMillis = LAST_EVENT_PROBE_MILLIS_BY_KEY.get(key);
            if (lastMillis != null && now - lastMillis < minIntervalMillis) {
                return true;
            }

            LAST_EVENT_PROBE_MILLIS_BY_KEY.put(key, now);
            return false;
        }
    }

    enum DebugProbeGroup {
        GAME("Game"),
        ASSET("Asset"),
        LIFECYCLE("Lifecycle"),
        RENDER("Render"),
        ACTION("Action"),
        SELECTION("Selection"),
        MAP("Map"),
        RESOURCE("Resource"),
        SAVE("Save"),
        AUDIO("Audio"),
        INPUT("Input"),
        UI("UI"),
        NETWORK("Network");

        final String label;

        DebugProbeGroup(String label) {
            this.label = label;
        }
    }

    enum DebugRenderPart {
        BACK_IMAGE("Back image"),
        SHADOW_IMAGE("Shadow image"),
        TURRET_IMAGE("Turret image"),
        SHIELD_IMAGE("Shield image"),
        ZOOM_ICON("Zoom icon"),
        OVERLAY_LAYER("Overlay layer"),
        FRAME_RECTS("Frame rects"),
        TURRET_TRANSFORM("Turret transform");

        final String label;

        DebugRenderPart(String label) {
            this.label = label;
        }
    }

    static final class OverlayMessage {
        final String text;

        OverlayMessage(String text) {
            this.text = text;
        }
    }

    static final class SlickInputState {
        final int mouseX;
        final int mouseY;
        final boolean clicked;

        SlickInputState(int mouseX, int mouseY, boolean clicked) {
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.clicked = clicked;
        }
    }
}
