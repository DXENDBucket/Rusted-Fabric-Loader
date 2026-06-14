package io.github.endx.rustedfabricexample;

import io.github.endx.rustedfabricapi.api.diagnostic.FileSystemDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.GameEngineDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.InputRuntimeDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.LibRocketUiDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.MappingEvidenceDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.NetworkRuntimeDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.AudioRuntimeDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.RenderCanvasDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.RenderGlDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.SlickRuntimeDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.SteamRuntimeDiagnostics;

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
                            + " audioRows=" + MappingEvidenceDiagnostics.allAudioBackendRows().size()
                            + " netRows=" + MappingEvidenceDiagnostics.allNetworkHandshakeSyncRows().size()
                            + " syncRows=" + MappingEvidenceDiagnostics.allNetworkSyncDesyncRows().size()
                            + " lobbyRows=" + MappingEvidenceDiagnostics.allNetworkLobbyChatCommandRows().size()
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
                            + " chatOnly=" + network.get("chatOnlyMode")
                            + " teams=" + network.get("teamListSnapshotSize")
                            + " lock=" + ExampleDebugOverlay.describeObject(network.get("teamListLock")),
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

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?>) {
            return (Map<String, Object>) value;
        }
        return java.util.Collections.emptyMap();
    }
}
