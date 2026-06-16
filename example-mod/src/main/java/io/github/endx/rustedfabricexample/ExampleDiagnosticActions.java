package io.github.endx.rustedfabricexample;

import io.github.endx.rustedfabricapi.api.diagnostic.FileSystemDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.GameEngineDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.CommonUtilsDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.CoreDebugStatsDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.InputRuntimeDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.HudCommandDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.LibRocketUiDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.MappingEvidenceDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.NetworkRuntimeDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.AudioRuntimeDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.RenderCanvasDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.RenderGlDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.RenderImageDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.SlickGraphicsBackendDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.SlickRuntimeDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.SteamRuntimeDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.UnitActionDiagnostics;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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

            if (graphicsEngine != null && RenderImageDiagnostics.isSlickGraphicsBackend(graphicsEngine)) {
                Map<String, Object> backend = RenderImageDiagnostics.describeSlickGraphicsBackend(graphicsEngine);
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "Image backend loaded=" + backend.get("loadedImageCount")
                                + " pendingDiscards=" + backend.get("pendingImageDataDiscardsSize")
                                + " live=" + RenderImageDiagnostics.liveSlickImagesSnapshot().size()
                                + " oom=" + summarizeImage(backend.get("outOfMemoryErrorImage")),
                        graphicsEngine);
            }

            if (graphicsEngine != null && SlickGraphicsBackendDiagnostics.isGraphicsEngine(graphicsEngine)) {
                showSlickGraphicsBackendSnapshot(stage, graphicsEngine);
            }

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
                            + " slickRows=" + MappingEvidenceDiagnostics.allSlickGraphicsBackendRows().size()
                            + " commonRows=" + MappingEvidenceDiagnostics.allCommonUtilsRows().size()
                            + " actionRows=" + MappingEvidenceDiagnostics.allUnitActionCommandResidualRows().size()
                            + " audioRows=" + MappingEvidenceDiagnostics.allAudioBackendRows().size()
                            + " netRows=" + MappingEvidenceDiagnostics.allNetworkHandshakeSyncRows().size()
                            + " syncRows=" + MappingEvidenceDiagnostics.allNetworkSyncDesyncRows().size()
                            + " lobbyRows=" + MappingEvidenceDiagnostics.allNetworkLobbyChatCommandRows().size()
                            + " deepNetRows=" + MappingEvidenceDiagnostics.allNetworkDeepPacketBranchRows().size()
                            + " imageRows=" + MappingEvidenceDiagnostics.allRenderImageTextureLifecycleRows().size()
                            + " hudRows=" + MappingEvidenceDiagnostics.allHudCommandInterfaceRows().size()
                            + " glTextRows=" + MappingEvidenceDiagnostics.allRenderGlTextRows().size()
                            + " inputHotfix=" + MappingEvidenceDiagnostics.allInputActionNamingHotfixRows().size()
                            + " uiRows=" + MappingEvidenceDiagnostics.allLibRocketUiScriptSurfaceRows().size()
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

    static void showCommonUtilsSnapshot(String stage) {
        try {
            Map<String, Object> state = CommonUtilsDiagnostics.describeCommonUtilsState();
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "CommonUtils rows=" + MappingEvidenceDiagnostics.allCommonUtilsRows().size()
                            + " skipped=" + MappingEvidenceDiagnostics.allCommonUtilsSkippedRows().size()
                            + " available=" + state.get("available")
                            + " cpu=" + CommonUtilsDiagnostics.getCpuCoreCount()
                            + " sqrtLut=" + state.get("sqrtIntLookupLength")
                            + " trig=" + state.get("sinTableLength") + "/" + state.get("cosTableLength"),
                    state.get("random"));

            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Common math dist=" + CommonUtilsDiagnostics.formatFloat2dp(
                            CommonUtilsDiagnostics.distance(0.0F, 0.0F, 3.0F, 4.0F))
                            + " angle=" + CommonUtilsDiagnostics.formatFloat(
                            CommonUtilsDiagnostics.angleTo(0.0F, 0.0F, 1.0F, 1.0F), 2)
                            + " fastSin90=" + CommonUtilsDiagnostics.formatFloat(
                            CommonUtilsDiagnostics.fastSin(90.0F), 3)
                            + " clamp=" + CommonUtilsDiagnostics.clampInt(300, 0, 255),
                    null);

            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Common text file=" + ExampleDebugOverlay.compactPath(
                            CommonUtilsDiagnostics.fileNameWithoutExtension("maps/skirmish/[p4]demo.tmx"))
                            + " parent=" + ExampleDebugOverlay.compactPath(
                            CommonUtilsDiagnostics.parentPath("mods/example/units/demo.ini"))
                            + " split=" + CommonUtilsDiagnostics.split("a,b,c", ',').length
                            + " xml=" + ExampleDebugOverlay.safeText(
                            CommonUtilsDiagnostics.escapeXml("<tag>${value}&")),
                    null);

            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Common hash md5=" + ExampleDebugOverlay.safeText(
                            CommonUtilsDiagnostics.md5Hex("Example").substring(0, 8))
                            + " sha4=" + ExampleDebugOverlay.safeText(
                            CommonUtilsDiagnostics.sha256HexShort4("Example"))
                            + " join=" + ExampleDebugOverlay.compactPath(
                            CommonUtilsDiagnostics.join("/", Arrays.asList("mods", "java", "unit.ini")))
                            + " read=" + ExampleDebugOverlay.safeText(CommonUtilsDiagnostics.readInputStreamUtf8AndClose(
                            new ByteArrayInputStream("utf8-ok".getBytes(StandardCharsets.UTF_8)))),
                    null);
        } catch (Throwable t) {
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "CommonUtils snapshot failed: " + t.getClass().getSimpleName()
                            + ": " + ExampleDebugOverlay.safeText(t.getMessage()),
                    null);
            ExampleMod.log("CommonUtils snapshot failed: " + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    static void showHudSnapshot(String stage) {
        try {
            Object interfaceEngine = GameEngineDiagnostics.currentInterfaceEngine();
            if (interfaceEngine == null) {
                ExampleDebugOverlay.enqueueOverlayMessage(stage, "HUD interface engine unavailable", null);
                return;
            }

            Map<String, Object> hud = HudCommandDiagnostics.describeInterfaceEngine(interfaceEngine);
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "HUD evidence rows=" + MappingEvidenceDiagnostics.allHudCommandInterfaceRows().size()
                            + " flow=" + MappingEvidenceDiagnostics.allHudCommandInterfaceFlowMap().size()
                            + " rev=" + hud.get("interfaceLayoutRevision")
                            + " dirty=" + hud.get("interfaceLayoutDirty")
                            + " lastPing=" + hud.get("lastMapPingBroadcastMillis"),
                    interfaceEngine);
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "HUD actions attackMove=" + summarizeActionKind(hud.get("attackMoveAction"))
                            + " guard=" + summarizeActionKind(hud.get("guardUnitAction"))
                            + " patrol=" + summarizeActionKind(hud.get("patrolAction"))
                            + " ping=" + summarizeActionKind(hud.get("pingMapAction")),
                    interfaceEngine);
        } catch (Throwable t) {
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "HUD snapshot failed: " + t.getClass().getSimpleName()
                            + ": " + ExampleDebugOverlay.safeText(t.getMessage()),
                    null);
            ExampleMod.log("HUD snapshot failed: " + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    static void showActionSnapshot(String stage) {
        try {
            Object interfaceEngine = GameEngineDiagnostics.currentInterfaceEngine();
            if (interfaceEngine == null) {
                ExampleDebugOverlay.enqueueOverlayMessage(stage, "Action snapshot interface engine unavailable", null);
                return;
            }

            Map<String, Object> hud = HudCommandDiagnostics.describeInterfaceEngine(interfaceEngine);
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Action evidence rows=" + MappingEvidenceDiagnostics.allUnitActionCommandResidualRows().size()
                            + " updated=" + MappingEvidenceDiagnostics.allUnitActionCommandResidualUpdatedRows().size()
                            + " flow=" + MappingEvidenceDiagnostics.allUnitActionCommandResidualFlowMap().size()
                            + " skipped=" + MappingEvidenceDiagnostics.allUnitActionCommandResidualSkippedRows().size()
                            + " partial=" + MappingEvidenceDiagnostics.allUnitActionCommandResidualPartialCoverageRows().size(),
                    interfaceEngine);

            showHudActionSummary(stage, "attackMove", hud.get("attackMoveAction"));
            showHudActionSummary(stage, "guard", hud.get("guardUnitAction"));
            showHudActionSummary(stage, "patrol", hud.get("patrolAction"));
            showHudActionSummary(stage, "attackMode", hud.get("attackModeAction"));
            showHudActionSummary(stage, "mapPingShortcut", hud.get("pingMapAction"));
            showHudActionSummary(stage, "mapPing", hud.get("mapPingAction"));

            Object attackModeAction = hud.get("attackModeAction");
            if (attackModeAction != null && UnitActionDiagnostics.isAttackModeAction(attackModeAction)) {
                Map<String, Object> details = UnitActionDiagnostics.describeAttackModeAction(attackModeAction);
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "AttackMode cache gen=" + details.get("lastSelectionGeneration")
                                + " cached=" + details.get("cachedAttackMode")
                                + " selected=" + details.get("selectedAttackModeCached"),
                        attackModeAction);
            }

            Object pingAction = hud.get("mapPingAction");
            if (pingAction != null && UnitActionDiagnostics.isPingMapAction(pingAction)) {
                Map<String, Object> ping = UnitActionDiagnostics.describePingMapAction(pingAction);
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "PingMap type=" + summarizePingType(ping.get("pingType"))
                                + " nested=" + ping.get("allPingActionsSize")
                                + " key=" + ExampleDebugOverlay.safeText(String.valueOf(ping.get("pingLocalizationKey"))),
                        pingAction);
            }

            List<Map<String, Object>> types = UnitActionDiagnostics.describePingMapTypes();
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Ping types " + summarizePingTypes(types),
                    null);
        } catch (Throwable t) {
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Action snapshot failed: " + t.getClass().getSimpleName()
                            + ": " + ExampleDebugOverlay.safeText(t.getMessage()),
                    null);
            ExampleMod.log("Action snapshot failed: " + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    static void showCoreStatsSnapshot(String stage) {
        try {
            Object statsEngine = CoreDebugStatsDiagnostics.currentStatsEngine();
            if (statsEngine == null) {
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "CoreStats engine unavailable from current GameEngine", null);
                return;
            }

            Map<String, Object> stats = CoreDebugStatsDiagnostics.describeStatsEngine(statsEngine);
            List<Object> teamStats = CoreDebugStatsDiagnostics.teamStatsSnapshot(statsEngine);
            List<Object> statsWithHistory = CoreDebugStatsDiagnostics.statsWithHistorySnapshot(statsEngine);
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "CoreStats evidence rows=" + MappingEvidenceDiagnostics.allCoreDebugStatsRows().size()
                            + " updated=" + MappingEvidenceDiagnostics.allCoreDebugStatsUpdatedRows().size()
                            + " flow=" + MappingEvidenceDiagnostics.allCoreDebugStatsFlowMap().size()
                            + " partial=" + MappingEvidenceDiagnostics.allCoreDebugStatsPartialCoverageRows().size()
                            + " metrics=" + CoreDebugStatsDiagnostics.statsHistoryMetricNames().size()
                            + " sources=" + CoreDebugStatsDiagnostics.teamStatValueSourceNames().size(),
                    statsEngine);

            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "CoreStats engine enabled=" + stats.get("enabled")
                            + " frame=" + stats.get("lastFrame")
                            + " recording=" + stats.get("historyRecordingEnabled")
                            + " teams=" + stats.get("teamStatsSize")
                            + " withHistory=" + statsWithHistory.size()
                            + " dispatcher=" + ExampleDebugOverlay.describeObject(stats.get("unitKillEventDispatcher")),
                    statsEngine);

            Object neutralStats = stats.get("neutralStats");
            if (neutralStats != null && CoreDebugStatsDiagnostics.isStatsTeamStats(neutralStats)) {
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "Neutral " + summarizeCoreTeamStats(neutralStats),
                        neutralStats);
            }

            if (!teamStats.isEmpty()) {
                Object firstStats = teamStats.get(0);
                if (CoreDebugStatsDiagnostics.isStatsTeamStats(firstStats)) {
                    ExampleDebugOverlay.enqueueOverlayMessage(stage,
                            "First team stats " + summarizeCoreTeamStats(firstStats),
                            firstStats);
                }
            }

            Object dispatcher = stats.get("unitKillEventDispatcher");
            if (dispatcher != null && CoreDebugStatsDiagnostics.isStatsEventDispatcher(dispatcher)) {
                Map<String, Object> dispatcherDetails =
                        CoreDebugStatsDiagnostics.describeStatsEventDispatcher(dispatcher);
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "Stats dispatcher listeners=" + dispatcherDetails.get("listenerCount"),
                        dispatcher);
            }
        } catch (Throwable t) {
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "CoreStats snapshot failed: " + t.getClass().getSimpleName()
                            + ": " + ExampleDebugOverlay.safeText(t.getMessage()),
                    null);
            ExampleMod.log("CoreStats snapshot failed: " + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    static void showProfilerSnapshot(String stage) {
        try {
            List<String> sections = CoreDebugStatsDiagnostics.profilerSectionNames();
            Object timer = CoreDebugStatsDiagnostics.newPerformanceTimer("example-profiler-snapshot", false);
            Map<String, Object> timerDetails = CoreDebugStatsDiagnostics.describePerformanceTimer(timer);

            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Profiler sections=" + sections.size()
                            + " first=" + firstItems(sections, 4)
                            + " timer=" + ExampleDebugOverlay.safeText(String.valueOf(timerDetails.get("summary"))),
                    timer);

            Object engine = GameEngineDiagnostics.currentEngineOrNull();
            if (engine != null) {
                Object profiler = CoreDebugStatsDiagnostics.newGameProfiler(engine);
                Map<String, Object> profilerDetails = CoreDebugStatsDiagnostics.describeGameProfiler(profiler);
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "Profiler maxSections=" + profilerDetails.get("maxProfilerSections")
                                + " data=" + ExampleDebugOverlay.describeObject(profilerDetails.get("sectionData")),
                        profiler);

                Object data = profilerDetails.get("sectionData");
                if (data != null && CoreDebugStatsDiagnostics.isProfilerSectionData(data)) {
                    Map<String, Object> dataDetails = CoreDebugStatsDiagnostics.describeProfilerSectionData(data);
                    ExampleDebugOverlay.enqueueOverlayMessage(stage,
                            "Profiler data starts=" + dataDetails.get("sectionStartTimesNanosSize")
                                    + " totals=" + dataDetails.get("sectionTotalTimesNanosSize")
                                    + " lastFrame=" + dataDetails.get("sectionLastFrameMillisSize")
                                    + " active=" + dataDetails.get("activeSectionCount"),
                            data);
                }
            }

            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "ANR title=" + ExampleDebugOverlay.safeText(safeAnrThreadTitle()),
                    null);
        } catch (Throwable t) {
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Profiler snapshot failed: " + t.getClass().getSimpleName()
                            + ": " + ExampleDebugOverlay.safeText(t.getMessage()),
                    null);
            ExampleMod.log("Profiler snapshot failed: " + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    static void showAudioSnapshot(String stage) {
        try {
            Object lastAudioObject = ExampleEventProbes.lastAudioObject();
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Audio evidence rows=" + MappingEvidenceDiagnostics.allAudioBackendRows().size()
                            + " openalRows=" + MappingEvidenceDiagnostics.allAudioOpenAlRows().size()
                            + " bridgeRows=" + MappingEvidenceDiagnostics.allAudioFactoryBridgeRows().size()
                            + " familyRows=" + MappingEvidenceDiagnostics.allAudioFamilyCompletionRows().size()
                            + " last=" + ExampleEventProbes.describeAudioObject(lastAudioObject),
                    lastAudioObject);

            if (lastAudioObject != null && AudioRuntimeDiagnostics.isGameSound(lastAudioObject)) {
                Map<String, Object> sound = AudioRuntimeDiagnostics.describeGameSound(lastAudioObject);
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "Audio gameSound name=" + ExampleDebugOverlay.compactPath(String.valueOf(sound.get("name")))
                                + " base=" + sound.get("baseVolume")
                                + " bytes=" + sound.get("bytesUsed"),
                        lastAudioObject);
            }
            if (lastAudioObject != null && AudioRuntimeDiagnostics.isSoundFactory(lastAudioObject)) {
                Map<String, Object> factory = AudioRuntimeDiagnostics.describeSoundFactory(lastAudioObject);
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "Audio soundFactory loaded=" + factory.get("loadedSoundsSize")
                                + " class=" + ExampleDebugOverlay.describeObject(lastAudioObject),
                        lastAudioObject);
            }
            if (lastAudioObject != null && AudioRuntimeDiagnostics.isMusicFactory(lastAudioObject)) {
                Map<String, Object> factory = AudioRuntimeDiagnostics.describeMusicFactory(lastAudioObject);
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "Audio musicFactory available=" + factory.get("available")
                                + " threaded=" + factory.get("usesMusicThread")
                                + " waitMs=" + factory.get("musicThreadWaitMillis"),
                        lastAudioObject);
            }
            if (lastAudioObject != null && AudioRuntimeDiagnostics.isMusicController(lastAudioObject)) {
                Map<String, Object> controller = AudioRuntimeDiagnostics.describeMusicController(lastAudioObject);
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "Audio controller track=" + ExampleDebugOverlay.compactPath(String.valueOf(controller.get("currentTrackPath")))
                                + " canPlay=" + controller.get("canPlayMusic")
                                + " fading=" + controller.get("crossFading"),
                        lastAudioObject);
            }
        } catch (Throwable t) {
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Audio snapshot failed: " + t.getClass().getSimpleName()
                            + ": " + ExampleDebugOverlay.safeText(t.getMessage()),
                    null);
            ExampleMod.log("Audio snapshot failed: " + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    static void showInputSnapshot(String stage) {
        try {
            Object registry = InputRuntimeDiagnostics.currentInputBindingRegistry();
            Object provider = InputRuntimeDiagnostics.currentInputDeviceProvider();
            Map<String, Object> bridge = InputRuntimeDiagnostics.describeKeycodeBridge();

            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Input evidence rows=" + MappingEvidenceDiagnostics.allInputKeybindingRows().size()
                            + " updated=" + MappingEvidenceDiagnostics.allInputKeybindingUpdatedRows().size()
                            + " actionHotfix=" + MappingEvidenceDiagnostics.allInputActionNamingHotfixRows().size()
                            + " residue=" + MappingEvidenceDiagnostics.allInputActionDisplayGroupResidueRows().size()
                            + " bridge slick=" + bridge.get("slickToAndroidCodesSize")
                            + " android=" + bridge.get("androidCodesByNameSize")
                            + " ENTER=" + bridge.get("enterAndroidCode"),
                    registry != null ? registry : provider);

            if (provider != null && InputRuntimeDiagnostics.isInputDeviceProvider(provider)) {
                Map<String, Object> providerDetails = InputRuntimeDiagnostics.describeInputDeviceProvider(provider);
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "Input provider controllers=" + providerDetails.get("controllerCount")
                                + " desktop=" + providerDetails.get("desktopProvider")
                                + " class=" + ExampleDebugOverlay.describeObject(provider),
                        provider);
            }

            if (registry == null) {
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "Input registry unavailable from current GameEngine", null);
                return;
            }

            Map<String, Object> registryDetails = InputRuntimeDiagnostics.describeInputBindingRegistry(registry);
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Input registry actions=" + registryDetails.get("actionsSize")
                            + " visible=" + registryDetails.get("visibleActionCount")
                            + " categories=" + registryDetails.get("categoryCount")
                            + " bound=" + registryDetails.get("boundActionCount"),
                    registry);

            showInputActionSummary(stage, "Shoot", registryDetails.get("shootAction"));
            showInputActionSummary(stage, "Menu", registryDetails.get("showMenuAction"));
            showInputActionSummary(stage, "Pause", registryDetails.get("pauseGameAction"));
            showInputActionSummary(stage, "Debug invincible", registryDetails.get("debugInvincibleUnitsAction"));
        } catch (Throwable t) {
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Input snapshot failed: " + t.getClass().getSimpleName()
                            + ": " + ExampleDebugOverlay.safeText(t.getMessage()),
                    null);
            ExampleMod.log("Input snapshot failed: " + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    static void showUiSnapshot(String stage) {
        try {
            Object controller = LibRocketUiDiagnostics.currentUiController();
            Object uiEngine = LibRocketUiDiagnostics.currentUiEngine();
            Object scriptEngine = LibRocketUiDiagnostics.currentScriptEngine();

            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "UI evidence rows=" + MappingEvidenceDiagnostics.allLibRocketUiScriptSurfaceRows().size()
                            + " updated=" + MappingEvidenceDiagnostics.allLibRocketUiScriptSurfaceUpdatedRows().size()
                            + " skipped=" + MappingEvidenceDiagnostics.allLibRocketUiScriptSurfaceSkippedRows().size()
                            + " partial=" + MappingEvidenceDiagnostics.allLibRocketUiScriptSurfacePartialCoverageRows().size(),
                    controller != null ? controller : uiEngine);

            if (controller != null && LibRocketUiDiagnostics.isLibRocketUiController(controller)) {
                Map<String, Object> details = LibRocketUiDiagnostics.describeUiController(controller);
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "UI controller open=" + details.get("uiOpen")
                                + " resume=" + details.get("resumeGameFlag")
                                + " keyMods=" + details.get("keyModifierState")
                                + " logLines=" + details.get("gameLogLineCount"),
                        controller);
            }

            if (uiEngine != null && LibRocketUiDiagnostics.isLibRocketUiEngine(uiEngine)) {
                Map<String, Object> details = LibRocketUiDiagnostics.describeUiEngine(uiEngine);
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "UI engine base=" + ExampleDebugOverlay.compactPath(String.valueOf(details.get("guiBasePath")))
                                + " insideEvent=" + details.get("insideEvent")
                                + " scissor=" + details.get("scissorEnabled")
                                + " noDoc=" + details.get("noDocumentOrPopupActive"),
                        uiEngine);
            }

            if (scriptEngine != null && LibRocketUiDiagnostics.isScriptEngine(scriptEngine)) {
                Map<String, Object> details = LibRocketUiDiagnostics.describeScriptEngine(scriptEngine);
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "ScriptEngine globals=" + details.get("globalsSize")
                                + " queued=" + details.get("queuedScriptsSize")
                                + " running=" + details.get("runningScriptsSize")
                                + " error=" + ExampleDebugOverlay.safeText(String.valueOf(details.get("scriptErrorMessage"))),
                        scriptEngine);
            }

            Object root = LibRocketUiDiagnostics.currentRootScript();
            if (root != null && LibRocketUiDiagnostics.isRootScript(root)) {
                Map<String, Object> details = LibRocketUiDiagnostics.describeRootScript(root);
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "Root doc=" + ExampleDebugOverlay.compactPath(String.valueOf(details.get("currentDocumentPath")))
                                + " popup=" + ExampleDebugOverlay.compactPath(String.valueOf(details.get("currentPopupPath")))
                                + " mods=" + details.get("modSupport")
                                + " workshop=" + details.get("workshopSupport"),
                        root);
            }

            Object multiplayer = LibRocketUiDiagnostics.currentMultiplayerScript();
            if (multiplayer != null && LibRocketUiDiagnostics.isMultiplayerScript(multiplayer)) {
                Map<String, Object> details = LibRocketUiDiagnostics.describeMultiplayerScript(multiplayer);
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "Multiplayer UI dropdown=" + details.get("currentDropdownRawArrayLength")
                                + " useMapDropdown=" + details.get("useMapDropdown")
                                + " lastPlayerTable=" + ExampleDebugOverlay.describeObject(details.get("lastPlayerTable")),
                        multiplayer);
            }

            Object debug = LibRocketUiDiagnostics.currentDebugScript();
            if (debug != null && LibRocketUiDiagnostics.isDebugScript(debug)) {
                Map<String, Object> details = LibRocketUiDiagnostics.describeDebugScript(debug);
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "DebugScript pid=" + details.get("currentPid")
                                + " net=" + details.get("networkGameActive")
                                + " players=" + details.get("humanPlayers")
                                + " conns=" + details.get("playerConnections")
                                + " desync=" + details.get("desyncErrors") + "/" + details.get("desyncPasses"),
                        debug);
            }
        } catch (Throwable t) {
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "UI snapshot failed: " + t.getClass().getSimpleName()
                            + ": " + ExampleDebugOverlay.safeText(t.getMessage()),
                    null);
            ExampleMod.log("UI snapshot failed: " + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    static void showNetworkSnapshot(String stage) {
        try {
            Object networkEngine = NetworkRuntimeDiagnostics.currentNetworkEngine();
            if (networkEngine == null) {
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "Network engine unavailable from current GameEngine", null);
                return;
            }

            Map<String, Object> network = NetworkRuntimeDiagnostics.describeNetworkEngine(networkEngine);
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Network server=" + network.get("isServer")
                            + " started=" + network.get("networkingStarted")
                            + " single=" + network.get("singlePlayerServer")
                            + " proxy=" + network.get("isProxyController")
                            + " serverCtrl=" + network.get("isServerController")
                            + " ctrl=" + network.get("serverOrProxyController")
                            + " protocol=" + network.get("networkProtocolVersion")
                            + " conns=" + network.get("connectionCount")
                            + " incoming=" + network.get("incomingPacketCount")
                            + " servers=" + network.get("serverListSize")
                            + " port=" + network.get("serverPort")
                            + " map=" + ExampleDebugOverlay.compactPath(String.valueOf(network.get("resolvedNetworkMapPath"))),
                    networkEngine);

            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Network lobby started=" + network.get("gameHasBeenStarted")
                            + " paused=" + network.get("gamePaused")
                            + " starting=" + network.get("gameStarting")
                            + " chatOnly=" + network.get("chatOnlyMode")
                            + " teams=" + network.get("teamListSnapshotSize")
                            + " cap=" + network.get("teamUnitCap") + "/" + network.get("maxTeamUnitCap")
                            + " lock=" + ExampleDebugOverlay.describeObject(network.get("teamListLock")),
                    networkEngine);

            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Network setup display map=" + ExampleDebugOverlay.safeText(String.valueOf(network.get("networkMapDisplayName")))
                            + " fog=" + ExampleDebugOverlay.safeText(String.valueOf(network.get("fogModeDisplayName")))
                            + " units=" + ExampleDebugOverlay.safeText(String.valueOf(network.get("startingUnitsDisplayName")))
                            + " credits=" + ExampleDebugOverlay.safeText(String.valueOf(network.get("startingCreditsDisplayName")))
                            + " step=" + network.get("currentStepRate"),
                    networkEngine);

            Object pingerTask = network.get("networkPingerTask");
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Network handshake sentRegister=" + network.get("sentRegisterConnection")
                            + " remoteId=" + ExampleDebugOverlay.safeText(String.valueOf(network.get("remoteServerId")))
                            + " challenge=" + network.get("serverChallengeNonce")
                            + " nextConn=" + network.get("nextConnectionId")
                            + " local=" + ExampleDebugOverlay.describeObject(network.get("localConnection"))
                            + " pinger=" + ExampleDebugOverlay.describeObject(pingerTask),
                    pingerTask != null ? pingerTask : networkEngine);

            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Network sync quick=" + network.get("quickResyncCommandQueued")
                            + " pending=" + network.get("hasPendingQuickResync")
                            + " enabled=" + network.get("enableQuickResync")
                            + " fixOff=" + network.get("disableDesyncFixing")
                            + " last=" + network.get("lastResyncTimer")
                            + " trigger=" + network.get("resyncTriggerTimer")
                            + " attempts=" + network.get("resyncAttemptCount")
                            + " frame=" + network.get("lastResyncFrame"),
                    networkEngine);

            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Network battleroom return=" + network.get("returnToBattleroomPending")
                            + " timer=" + network.get("returnToBattleroomTimerActive")
                            + " countdown=" + network.get("returnToBattleroomCountdownSeconds")
                            + " startFailed=" + network.get("startGameFailed")
                            + " bans=" + network.get("banEntriesCount"),
                    networkEngine);

            if (pingerTask != null && NetworkRuntimeDiagnostics.isNetworkPingerTask(pingerTask)) {
                Map<String, Object> pinger = NetworkRuntimeDiagnostics.describeNetworkPingerTask(pingerTask);
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "Pinger sendThisTick=" + pinger.get("sendPingThisTick")
                                + " lastRunMs=" + pinger.get("lastRunTimeMillis"),
                        pingerTask);
            }

            Object chatHistory = network.get("chatHistory");
            if (chatHistory != null && NetworkRuntimeDiagnostics.isNetworkChatHistory(chatHistory)) {
                Map<String, Object> chat = NetworkRuntimeDiagnostics.describeNetworkChatHistory(chatHistory);
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "Chat messages=" + chat.get("messagesSize")
                                + " plain=" + ExampleDebugOverlay.safeText(String.valueOf(chat.get("plainTextLog"))),
                        chatHistory);
            }

            Object tokenHelper = NetworkRuntimeDiagnostics.currentMasterServerAuthTokenHelper();
            if (tokenHelper != null && NetworkRuntimeDiagnostics.isMasterServerAuthTokenHelper(tokenHelper)) {
                Map<String, Object> token = NetworkRuntimeDiagnostics.describeMasterServerAuthTokenHelper(tokenHelper);
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "Master token helper enabled=" + token.get("enabled")
                                + " tx=" + ExampleDebugOverlay.safeText(String.valueOf(token.get("tokenKeyPrefix")))
                                + " ts=" + ExampleDebugOverlay.safeText(String.valueOf(token.get("timestampKeyPrefix"))),
                        tokenHelper);
            }

            Object gameSetup = network.get("gameSetup");
            if (gameSetup != null && NetworkRuntimeDiagnostics.isGameSetup(gameSetup)) {
                Map<String, Object> setup = NetworkRuntimeDiagnostics.describeGameSetup(gameSetup);
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "Game setup map=" + ExampleDebugOverlay.compactPath(String.valueOf(setup.get("mapPath")))
                                + " type=" + ExampleDebugOverlay.safeText(String.valueOf(setup.get("mapTypeName")))
                                + " credits=" + setup.get("startingCredits")
                                + " fog=" + setup.get("fogMode")
                                + " teamsLocked=" + setup.get("teamsLocked")
                                + " spectators=" + setup.get("allowSpectators")
                                + " locked=" + setup.get("lockedRoom")
                                + " seed=" + setup.get("randomSeed"),
                        gameSetup);
            }

            List<Object> connections = NetworkRuntimeDiagnostics.currentConnections();
            if (!connections.isEmpty()) {
                Object connection = connections.get(0);
                if (NetworkRuntimeDiagnostics.isNetworkConnection(connection)) {
                    Map<String, Object> details = NetworkRuntimeDiagnostics.describeNetworkConnection(connection);
                    ExampleDebugOverlay.enqueueOverlayMessage(stage,
                            "First connection id=" + details.get("connectionId")
                                    + " name=" + ExampleDebugOverlay.safeText(String.valueOf(details.get("displayName")))
                                    + " validated=" + details.get("validated")
                                    + " fwd=" + details.get("forwardingAllowed")
                                    + " multicast=" + details.get("multicastEnabled")
                                    + " open=" + details.get("open")
                                    + " queue=" + details.get("sendQueueSize")
                                    + " ping=" + details.get("lastPingMillis")
                                    + " nonce=" + details.get("challengeNonce")
                                    + " netVer=" + details.get("clientNetworkVersion")
                                    + " desyncs=" + details.get("desyncCount"),
                            connection);
                }
            }

            List<Object> servers = NetworkRuntimeDiagnostics.currentServerList();
            if (!servers.isEmpty()) {
                Object server = servers.get(0);
                if (NetworkRuntimeDiagnostics.isGameServerInfo(server)) {
                    Map<String, Object> details = NetworkRuntimeDiagnostics.describeGameServerInfo(server);
                    ExampleDebugOverlay.enqueueOverlayMessage(stage,
                            "First listed server map=" + ExampleDebugOverlay.safeText(String.valueOf(details.get("mapName")))
                                    + " players=" + details.get("playerCount") + "/" + details.get("maxPlayerCount")
                                    + " mode=" + ExampleDebugOverlay.safeText(String.valueOf(details.get("gameMode")))
                                    + " lan=" + details.get("isLan"),
                            server);
                }
            }
        } catch (Throwable t) {
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Network snapshot failed: " + t.getClass().getSimpleName()
                            + ": " + ExampleDebugOverlay.safeText(t.getMessage()),
                    null);
            ExampleMod.log("Network snapshot failed: " + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    static void showSteamSnapshot(String stage) {
        try {
            Object steamEngine = SteamRuntimeDiagnostics.currentSteamEngine();
            if (steamEngine == null) {
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "Steam engine unavailable", null);
                return;
            }

            Map<String, Object> steam = SteamRuntimeDiagnostics.describeSteamEngine(steamEngine);
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Steam enabled=" + steam.get("steamEnabled")
                            + " disabled=" + steam.get("steamDisabled")
                            + " initialized=" + steam.get("initialized")
                            + " sockets=" + steam.get("activeSteamSocketsSize")
                            + " lobbyHost=" + steam.get("isLobbyHost")
                            + " persona=" + ExampleDebugOverlay.safeText(String.valueOf(steam.get("personaName"))),
                    steamEngine);

            Object workshopManager = SteamRuntimeDiagnostics.currentWorkshopManager();
            if (workshopManager != null && SteamRuntimeDiagnostics.isSteamWorkshopManager(workshopManager)) {
                Map<String, Object> workshop = SteamRuntimeDiagnostics.describeWorkshopManager(workshopManager);
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "Workshop manager ugc=" + workshop.get("hasSteamUGC")
                                + " callback=" + workshop.get("hasUgcCallback")
                                + " class=" + ExampleDebugOverlay.describeObject(workshopManager),
                        workshopManager);
            }
        } catch (Throwable t) {
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Steam snapshot failed: " + t.getClass().getSimpleName()
                            + ": " + ExampleDebugOverlay.safeText(t.getMessage()),
                    null);
            ExampleMod.log("Steam snapshot failed: " + t.getClass().getName() + ": " + t.getMessage());
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
        showImageSummary(stage, "Slick loadingLogo", slick.get("loadingLogo"));
        showImageSummary(stage, "Slick pointerImage", slick.get("pointerImage"));

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

    private static void showSlickGraphicsBackendSnapshot(String stage, Object graphicsEngine) {
        Map<String, Object> generic = SlickGraphicsBackendDiagnostics.describeGraphicsBackend(graphicsEngine);
        ExampleDebugOverlay.enqueueOverlayMessage(stage,
                "Graphics backend size=" + generic.get("width") + "x" + generic.get("height")
                        + " slick=" + generic.get("slickGraphicsBackend")
                        + " canvas=" + generic.get("canvasGraphicsEngine")
                        + " null=" + generic.get("nullGraphicsEngine")
                        + " evidence=" + MappingEvidenceDiagnostics.allSlickGraphicsBackendRows().size(),
                graphicsEngine);

        if (!SlickGraphicsBackendDiagnostics.isSlickGraphicsBackend(graphicsEngine)) {
            return;
        }

        Map<String, Object> backend = SlickGraphicsBackendDiagnostics.describeSlickGraphicsBackend(graphicsEngine);
        ExampleDebugOverlay.enqueueOverlayMessage(stage,
                "Slick backend shaders=" + backend.get("shadersEnabled")
                        + " dirty=" + backend.get("paintStateDirty")
                        + " uiScale=" + backend.get("uiScale")
                        + " target=" + backend.get("targetWidth") + "x" + backend.get("targetHeight")
                        + " drawMode=" + backend.get("currentDrawMode")
                        + " lineWidth=" + backend.get("currentLineWidth"),
                graphicsEngine);
        ExampleDebugOverlay.enqueueOverlayMessage(stage,
                "Slick backend fonts=" + backend.get("fontCacheKeysSize")
                        + " transforms=" + backend.get("transformStackSize")
                        + " pool=" + backend.get("transformStatePoolSize")
                        + " pendingDiscards=" + backend.get("pendingImageDataDiscardsSize")
                        + " floatBuffer=" + backend.get("sharedFloatBufferCapacity")
                        + "/" + backend.get("sharedFloatBufferRemaining")
                        + " floatArray=" + backend.get("sharedFloatArrayLength"),
                graphicsEngine);

        Object transformState = backend.get("transformState");
        if (transformState != null && SlickGraphicsBackendDiagnostics.isSlickTransformState(transformState)) {
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Slick transform " + summarizeSlickTransform(transformState),
                    transformState);
        }

        List<Object> fontKeys = SlickGraphicsBackendDiagnostics.fontCacheKeysSnapshot(graphicsEngine);
        if (!fontKeys.isEmpty() && SlickGraphicsBackendDiagnostics.isSlickFontKey(fontKeys.get(0))) {
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Slick first font " + summarizeSlickFontKey(fontKeys.get(0)),
                    fontKeys.get(0));
        }
    }

    private static void showImageSummary(String stage, String label, Object image) {
        if (image == null || !RenderImageDiagnostics.isGameImage(image)) {
            return;
        }
        ExampleDebugOverlay.enqueueOverlayMessage(stage, label + " " + summarizeImage(image), image);
    }

    private static String summarizeImage(Object image) {
        if (image == null) {
            return "null";
        }

        try {
            Map<String, Object> details = RenderImageDiagnostics.describeGameImage(image);
            String name = String.valueOf(details.containsKey("nameFromGetter")
                    ? details.get("nameFromGetter") : details.get("name"));
            Object width = details.containsKey("widthFromGetter")
                    ? details.get("widthFromGetter") : details.get("width");
            Object height = details.containsKey("heightFromGetter")
                    ? details.get("heightFromGetter") : details.get("height");
            String result = ExampleDebugOverlay.describeObject(image)
                    + "{name=" + ExampleDebugOverlay.compactPath(name)
                    + ", size=" + width + "x" + height
                    + ", slick=" + details.get("slickBitmapOrTexture")
                    + ", lazy=" + details.get("lazyTeamColorImage");
            if (RenderImageDiagnostics.isSlickBitmapOrTexture(image)) {
                Map<String, Object> slick = RenderImageDiagnostics.describeSlickBitmapOrTexture(image);
                result += ", file=" + ExampleDebugOverlay.compactPath(String.valueOf(slick.get("filePath")))
                        + ", data=" + (slick.get("slickImageData") != null)
                        + ", buf=" + slick.get("imageByteBufferRemaining");
            }
            return result + "}";
        } catch (RuntimeException e) {
            return ExampleDebugOverlay.describeObject(image);
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

    private static String summarizeSlickTransform(Object transformState) {
        try {
            Map<String, Object> details = SlickGraphicsBackendDiagnostics.describeSlickTransformState(transformState);
            return "{xy=" + details.get("translateX") + "," + details.get("translateY")
                    + ", rot=" + details.get("rotationDegrees")
                    + ", scale=" + details.get("scaleX") + "," + details.get("scaleY")
                    + ", pivot=" + details.get("rotationPivotX") + "," + details.get("rotationPivotY")
                    + ", clip=" + ExampleDebugOverlay.describeObject(details.get("clipRect")) + "}";
        } catch (RuntimeException e) {
            return ExampleDebugOverlay.describeObject(transformState);
        }
    }

    private static String summarizeSlickFontKey(Object fontKey) {
        try {
            Map<String, Object> details = SlickGraphicsBackendDiagnostics.describeSlickFontKey(fontKey);
            return "{size=" + details.get("fontSize")
                    + ", bold=" + details.get("bold")
                    + ", fallback=" + details.get("fallback")
                    + ", recent=" + details.get("recentTextsUsed") + "/" + details.get("recentTextsLength")
                    + ", font=" + ExampleDebugOverlay.describeObject(details.get("font")) + "}";
        } catch (RuntimeException e) {
            return ExampleDebugOverlay.describeObject(fontKey);
        }
    }

    private static String summarizeActionKind(Object action) {
        if (action == null || !UnitActionDiagnostics.isUnitAction(action)) {
            return ExampleDebugOverlay.describeObject(action);
        }
        return UnitActionDiagnostics.actionKind(action);
    }

    private static void showHudActionSummary(String stage, String label, Object action) {
        if (action == null || !UnitActionDiagnostics.isUnitAction(action)) {
            return;
        }

        try {
            Map<String, Object> details = UnitActionDiagnostics.describeUnitAction(action);
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Action " + label
                            + " kind=" + details.get("actionKind")
                            + " id=" + ExampleDebugOverlay.safeText(String.valueOf(details.get("actionIdString")))
                            + " text=" + ExampleDebugOverlay.safeText(String.valueOf(details.get("text")))
                            + " cmd=" + details.get("actionCommandType")
                            + " display=" + details.get("displayType"),
                    action);

            Object filter = details.get("availabilityFilter");
            if (filter != null && UnitActionDiagnostics.isUnitActionFilter(filter)) {
                Map<String, Object> filterDetails = UnitActionDiagnostics.describeUnitActionFilter(filter);
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "Action " + label + " filter empty=" + filterDetails.get("emptyActionFilter")
                                + " editor=" + filterDetails.get("editorActionAvailabilityFilter")
                                + " class=" + ExampleDebugOverlay.describeObject(filter),
                        filter);
            }

            Object actionId = details.get("actionId");
            if (actionId != null && UnitActionDiagnostics.isUnitActionId(actionId)) {
                Map<String, Object> actionIdDetails = UnitActionDiagnostics.describeActionId(actionId);
                ExampleDebugOverlay.enqueueOverlayMessage(stage,
                        "Action " + label + " actionId "
                                + ExampleDebugOverlay.safeText(String.valueOf(actionIdDetails.get("asString")))
                                + " raw=" + ExampleDebugOverlay.safeText(String.valueOf(actionIdDetails.get("idString"))),
                        actionId);
            }
        } catch (RuntimeException e) {
            ExampleDebugOverlay.enqueueOverlayMessage(stage,
                    "Action " + label + " summary failed: " + e.getClass().getSimpleName()
                            + ": " + ExampleDebugOverlay.safeText(e.getMessage()),
                    action);
        }
    }

    private static String summarizePingType(Object pingType) {
        if (pingType == null || !UnitActionDiagnostics.isMapPingType(pingType)) {
            return ExampleDebugOverlay.describeObject(pingType);
        }

        Map<String, Object> details = UnitActionDiagnostics.describeMapPingType(pingType);
        return details.get("fieldName")
                + "/" + ExampleDebugOverlay.safeText(String.valueOf(details.get("displaySuffix")))
                + "/" + ExampleDebugOverlay.safeText(String.valueOf(details.get("localizationKey")));
    }

    private static String summarizePingTypes(List<Map<String, Object>> types) {
        StringBuilder result = new StringBuilder();
        int limit = Math.min(types.size(), 6);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                result.append(", ");
            }
            Map<String, Object> details = types.get(i);
            result.append(details.get("fieldName"));
            Object suffix = details.get("displaySuffix");
            if (suffix != null && !String.valueOf(suffix).isEmpty()) {
                result.append(':').append(ExampleDebugOverlay.safeText(String.valueOf(suffix)));
            }
        }
        if (types.size() > limit) {
            result.append(" +").append(types.size() - limit);
        }
        return result.toString();
    }

    private static void showInputActionSummary(String stage, String label, Object action) {
        if (action == null || !InputRuntimeDiagnostics.isInputAction(action)) {
            return;
        }

        Map<String, Object> details = InputRuntimeDiagnostics.describeInputAction(action);
        ExampleDebugOverlay.enqueueOverlayMessage(stage,
                "Input " + label
                        + " key=" + ExampleDebugOverlay.safeText(String.valueOf(details.get("primaryBindingDisplay")))
                        + " config=" + ExampleDebugOverlay.safeText(String.valueOf(details.get("configKey")))
                        + " bindings=" + details.get("bindingCount"),
                action);
    }

    private static String summarizeCoreTeamStats(Object stats) {
        try {
            Map<String, Object> details = CoreDebugStatsDiagnostics.describeStatsTeamStats(stats);
            Object history = details.get("history");
            String historyText = "null";
            if (history != null && CoreDebugStatsDiagnostics.isStatsHistory(history)) {
                Map<String, Object> historyDetails = CoreDebugStatsDiagnostics.describeStatsHistory(history);
                historyText = "{team=" + historyDetails.get("teamIndex")
                        + ", series=" + historyDetails.get("metricSeriesSize")
                        + ", has=" + historyDetails.get("hasHistory") + "}";
            }
            return "kills=" + details.get("unitsKilled") + "/" + details.get("buildingsKilled")
                    + "/" + details.get("experimentalsKilled")
                    + " lost=" + details.get("unitsLost") + "/" + details.get("buildingsLost")
                    + "/" + details.get("experimentalsLost")
                    + " ver=" + details.get("serializationVersion")
                    + " history=" + historyText;
        } catch (RuntimeException e) {
            return ExampleDebugOverlay.describeObject(stats);
        }
    }

    private static String firstItems(List<String> values, int limit) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.size() && i < limit; i++) {
            if (i > 0) {
                result.append(',');
            }
            result.append(values.get(i));
        }
        return result.toString();
    }

    private static String safeAnrThreadTitle() {
        try {
            return CoreDebugStatsDiagnostics.formatAnrThreadTitle(Thread.currentThread());
        } catch (RuntimeException e) {
            return "<unavailable>";
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?>) {
            return (Map<String, Object>) value;
        }
        return java.util.Collections.emptyMap();
    }
}
