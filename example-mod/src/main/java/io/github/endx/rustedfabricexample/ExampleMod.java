package io.github.endx.rustedfabricexample;

import android.graphics.Paint;
import android.graphics.Paint$Style;
import android.graphics.Rect;
import io.github.endx.rustedfabricapi.api.event.CommandEvents;
import io.github.endx.rustedfabricapi.api.event.CustomUnitEvents;
import io.github.endx.rustedfabricapi.api.event.CustomUnitRuntimeEvents;
import io.github.endx.rustedfabricapi.api.event.GameLifecycleEvents;
import io.github.endx.rustedfabricapi.api.event.MapDiscoveryEvents;
import io.github.endx.rustedfabricapi.api.event.MapMissionEvents;
import io.github.endx.rustedfabricapi.api.event.MapSpawnEvents;
import io.github.endx.rustedfabricapi.api.event.ResourceRuntimeEvents;
import io.github.endx.rustedfabricapi.api.event.SaveSyncEvents;
import io.github.endx.rustedfabricapi.api.event.SelectionEvents;
import io.github.endx.rustedfabricapi.api.event.UnitLifecycleEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import rustedwarfare.core.GameEngine;
import rustedwarfare.core.SettingsEngine;
import rustedwarfare.mod.ModManager;
import rustedwarfare.render.GraphicsEngine;
import rustedwarfare.ui.script.RootScript;
import rustedwarfare.ui.script.ScriptEngine;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ExampleMod implements ModInitializer, ClientModInitializer {
    public static final String MOD_ID = "rusted_fabric_example";
    private static final AtomicBoolean VISIBLE_SETTINGS_TWEAKS_STARTED = new AtomicBoolean();
    private static final AtomicBoolean MAIN_MENU_POPUP_STARTED = new AtomicBoolean();
    private static final AtomicBoolean MAIN_MENU_POPUP_SHOWN = new AtomicBoolean();
    private static final AtomicBoolean MAP_ENTRY_MESSAGE_REGISTERED = new AtomicBoolean();
    private static final AtomicBoolean EVENT_PROBE_MESSAGES_REGISTERED = new AtomicBoolean();
    private static final AtomicBoolean OVERLAY_RENDERER_REGISTERED = new AtomicBoolean();
    private static final AtomicBoolean OVERLAY_RENDER_HOOK_SEEN = new AtomicBoolean();
    private static final AtomicBoolean OVERLAY_SLICK_DRAW_SEEN = new AtomicBoolean();
    private static final AtomicBoolean OVERLAY_DIMENSIONS_LOGGED = new AtomicBoolean();
    private static final Object MAP_ENTRY_MESSAGE_LOCK = new Object();
    private static final Object EVENT_PROBE_MESSAGE_LOCK = new Object();
    private static final Object OVERLAY_LOCK = new Object();
    private static final int OVERLAY_BORDER_COLOR = 0xFF44FF66;
    private static final int OVERLAY_TEXT_COLOR = 0xFFFFFFFF;
    private static final int MAX_OVERLAY_MESSAGES = 5;
    private static final float SLICK_TEXT_SCALE = 0.78f;
    private static final List<OverlayMessage> OVERLAY_MESSAGES = new ArrayList<>();
    private static final Rect OVERLAY_RECT = new Rect();
    private static Paint overlayFillPaint;
    private static Paint overlayBorderPaint;
    private static Paint overlayTextPaint;
    private static Field slickGameContainerField;
    private static Field slickGraphicsField;
    private static Method slickGameContainerGetWidthMethod;
    private static Method slickGameContainerGetHeightMethod;
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
    private static String lastMapEntryMessageMapPath;
    private static long lastMapEntryMessageMillis;
    private static final Map<String, Long> LAST_EVENT_PROBE_MILLIS_BY_KEY = new HashMap<>();
    private static long lastOverlayDrawFailureLogMillis;

    @Override
    public void onInitialize() {
        log("main entrypoint");
        logNamedGameTypes("main");
        startVisibleSettingsTweaks("main");
        startMainMenuPopup("main");
        registerMapEntryMessage("main");
        registerEventProbeMessages("main");
        registerOverlayRenderer("main");
    }

    @Override
    public void onInitializeClient() {
        log("client entrypoint");
        logNamedGameTypes("client");
        startVisibleSettingsTweaks("client");
        startMainMenuPopup("client");
        registerMapEntryMessage("client");
        registerEventProbeMessages("client");
        registerOverlayRenderer("client");
    }

    static void logNamedGameTypes(String stage) {
        log(stage + " GameEngine class=" + GameEngine.class.getName());

        GameEngine engine = null;
        try {
            engine = GameEngine.getInstance();
        } catch (Throwable t) {
            log(stage + " GameEngine.getInstance() failed: " + t.getClass().getName() + ": " + t.getMessage());
        }

        if (engine == null) {
            log(stage + " GameEngine is not initialized yet");
            return;
        }

        SettingsEngine settings = engine.settings;
        ModManager modManager = engine.modManager;
        log(stage + " engine=" + engine.getClass().getName()
                + ", showFps=" + (settings != null && settings.showFps)
                + ", modManager=" + (modManager != null ? modManager.getClass().getName() : "null"));
    }

    static void startVisibleSettingsTweaks(String stage) {
        if (!VISIBLE_SETTINGS_TWEAKS_STARTED.compareAndSet(false, true)) {
            return;
        }

        Thread thread = new Thread(() -> waitForSettingsAndApplyTweaks(stage), "Rusted Fabric Example Tweaks");
        thread.setDaemon(true);
        thread.start();
    }

    static void startMainMenuPopup(String stage) {
        if (!MAIN_MENU_POPUP_STARTED.compareAndSet(false, true)) {
            return;
        }

        Thread thread = new Thread(() -> waitForMainMenuAndShowPopup(stage), "Rusted Fabric Example Main Menu Popup");
        thread.setDaemon(true);
        thread.start();
    }

    static void registerOverlayRenderer(String stage) {
        if (!OVERLAY_RENDERER_REGISTERED.compareAndSet(false, true)) {
            return;
        }

        GameLifecycleEvents.AFTER_FRAME_RENDER.register(ExampleMod::drawOverlayMessages);
        log("registered green overlay renderer from " + stage);
    }

    static void registerMapEntryMessage(String stage) {
        registerOverlayRenderer(stage);

        if (!MAP_ENTRY_MESSAGE_REGISTERED.compareAndSet(false, true)) {
            return;
        }

        GameLifecycleEvents.AFTER_MAP_SETUP.register((minimap, map, fogEnabled) -> showMapEntryMessage(stage, map));
        log("registered map entry message hook from " + stage);
    }

    static void registerEventProbeMessages(String stage) {
        registerOverlayRenderer(stage);

        if (!EVENT_PROBE_MESSAGES_REGISTERED.compareAndSet(false, true)) {
            return;
        }

        CustomUnitEvents.BEFORE_NATIVE_CUSTOM_UNIT_LOAD.register(() ->
                showEventProbeMessage(stage, "BeforeNativeCustomUnitLoad", null));

        CustomUnitEvents.AFTER_NATIVE_CUSTOM_UNIT_PARSE_BEFORE_ENABLE.register(() ->
                showEventProbeMessage(stage, "AfterNativeCustomUnitParseBeforeEnable", null));

        CustomUnitEvents.BEFORE_CUSTOM_UNIT_REGISTRY_REBUILD.register(includeDisabledMods ->
                showEventProbeMessage(stage, "BeforeCustomUnitRegistryRebuild includeDisabled=" + includeDisabledMods, null));

        CustomUnitEvents.AFTER_CUSTOM_UNIT_OVERRIDE_AND_REPLACE.register(() ->
                showEventProbeMessage(stage, "AfterCustomUnitOverrideAndReplace", null));

        CustomUnitEvents.AFTER_CUSTOM_UNIT_LINK_GRAPH_BUILT.register(() ->
                showEventProbeMessage(stage, "AfterCustomUnitLinkGraphBuilt", null));

        GameLifecycleEvents.AFTER_FRAME_UPDATE.register((renderer, gameContainer, delta) ->
                showEventProbeMessage(stage, "AfterFrameUpdate",
                        "AfterFrameUpdate delta=" + delta
                                + " renderer=" + describeObject(renderer),
                        renderer, 5000L));

        UnitLifecycleEvents.BEFORE_UNIT_REGISTER.register(unit ->
                showEventProbeMessage(stage, "BeforeUnitRegister",
                        "BeforeUnitRegister unit=" + describeObject(unit),
                        unit, 1000L));

        UnitLifecycleEvents.AFTER_UNIT_REGISTER.register(unit ->
                showEventProbeMessage(stage, "AfterUnitRegister",
                        "AfterUnitRegister unit=" + describeObject(unit),
                        unit, 1000L));

        UnitLifecycleEvents.BEFORE_UNIT_UNREGISTER.register(unit ->
                showEventProbeMessage(stage, "BeforeUnitUnregister",
                        "BeforeUnitUnregister unit=" + describeObject(unit),
                        unit, 1000L));

        UnitLifecycleEvents.AFTER_UNIT_UNREGISTER.register(unit ->
                showEventProbeMessage(stage, "AfterUnitUnregister",
                        "AfterUnitUnregister unit=" + describeObject(unit),
                        unit, 1000L));

        SelectionEvents.BEFORE_UNIT_SELECT.register((interfaceEngine, unit, append) -> {
            showEventProbeMessage(stage, "BeforeUnitSelect",
                    "BeforeUnitSelect append=" + append
                            + " unit=" + describeObject(unit),
                    unit, 300L);
            return false;
        });

        SelectionEvents.AFTER_UNIT_SELECT.register((interfaceEngine, unit, append) ->
                showEventProbeMessage(stage, "AfterUnitSelect",
                        "AfterUnitSelect append=" + append
                                + " unit=" + describeObject(unit),
                        unit, 300L));

        SelectionEvents.BEFORE_UNIT_ADDED_TO_SELECTION.register((interfaceEngine, unit) -> {
            showEventProbeMessage(stage, "BeforeUnitAddedToSelection",
                    "BeforeUnitAddedToSelection unit=" + describeObject(unit),
                    unit, 300L);
            return false;
        });

        SelectionEvents.AFTER_UNIT_ADDED_TO_SELECTION.register((interfaceEngine, unit, result) ->
                showEventProbeMessage(stage, "AfterUnitAddedToSelection",
                        "AfterUnitAddedToSelection result=" + result
                                + " unit=" + describeObject(unit),
                        unit, 300L));

        SelectionEvents.BEFORE_UNIT_DESELECT.register((interfaceEngine, unit) -> {
            showEventProbeMessage(stage, "BeforeUnitDeselect",
                    "BeforeUnitDeselect unit=" + describeObject(unit),
                    unit, 300L);
            return false;
        });

        SelectionEvents.AFTER_UNIT_DESELECT.register((interfaceEngine, unit) ->
                showEventProbeMessage(stage, "AfterUnitDeselect",
                        "AfterUnitDeselect unit=" + describeObject(unit),
                        unit, 300L));

        SelectionEvents.BEFORE_SELECTION_CLEAR.register(interfaceEngine -> {
            showEventProbeMessage(stage, "BeforeSelectionClear",
                    "BeforeSelectionClear interface=" + describeObject(interfaceEngine),
                    interfaceEngine, 300L);
            return false;
        });

        SelectionEvents.AFTER_SELECTION_CLEAR.register(interfaceEngine ->
                showEventProbeMessage(stage, "AfterSelectionClear",
                        "AfterSelectionClear interface=" + describeObject(interfaceEngine),
                        interfaceEngine, 300L));

        CommandEvents.BEFORE_COMMAND_ISSUE.register(command -> {
            showEventProbeMessage(stage, "BeforeCommandIssue",
                    "BeforeCommandIssue command=" + describeObject(command),
                    command, 500L);
            return false;
        });

        CommandEvents.AFTER_COMMAND_ISSUE.register(command ->
                showEventProbeMessage(stage, "AfterCommandIssue",
                        "AfterCommandIssue command=" + describeObject(command),
                        command, 500L));

        CustomUnitRuntimeEvents.BEFORE_CUSTOM_ACTION_EXECUTE.register((unit, action, targetPoint, targetUnit, recursionDepth) -> {
            showEventProbeMessage(stage, "BeforeCustomActionExecute",
                    "BeforeCustomActionExecute depth=" + recursionDepth
                            + " unit=" + describeObject(unit)
                            + " action=" + describeObject(action)
                            + " target=" + describeObject(targetUnit),
                    unit, 750L);
            return false;
        });

        CustomUnitRuntimeEvents.AFTER_CUSTOM_ACTION_EXECUTE.register((unit, action, targetPoint, targetUnit, recursionDepth, result) ->
                showEventProbeMessage(stage, "AfterCustomActionExecute",
                        "AfterCustomActionExecute result=" + result
                                + " depth=" + recursionDepth
                                + " unit=" + describeObject(unit)
                                + " action=" + describeObject(action),
                        unit, 750L));

        CustomUnitRuntimeEvents.BEFORE_CUSTOM_ACTION_EFFECT_EXECUTE.register((effect, unit, action, targetPoint, targetUnit, recursionDepth) -> {
            showEventProbeMessage(stage, "BeforeCustomActionEffectExecute",
                    "BeforeCustomActionEffectExecute depth=" + recursionDepth
                            + " effect=" + describeObject(effect)
                            + " unit=" + describeObject(unit)
                            + " action=" + describeObject(action),
                    effect, 750L);
            return false;
        });

        CustomUnitRuntimeEvents.AFTER_CUSTOM_ACTION_EFFECT_EXECUTE.register((effect, unit, action, targetPoint, targetUnit, recursionDepth, result) ->
                showEventProbeMessage(stage, "AfterCustomActionEffectExecute",
                        "AfterCustomActionEffectExecute result=" + result
                                + " depth=" + recursionDepth
                                + " effect=" + describeObject(effect)
                                + " unit=" + describeObject(unit),
                        effect, 750L));

        CustomUnitRuntimeEvents.BEFORE_CUSTOM_UNIT_CONVERT.register((unit, action, targetPoint, targetUnit, recursionDepth) -> {
            showEventProbeMessage(stage, "BeforeCustomUnitConvert",
                    "BeforeCustomUnitConvert depth=" + recursionDepth
                            + " unit=" + describeObject(unit)
                            + " action=" + describeObject(action)
                            + " target=" + describeObject(targetUnit),
                    unit, 750L);
            return false;
        });

        CustomUnitRuntimeEvents.AFTER_CUSTOM_UNIT_CONVERT.register((unit, action, targetPoint, targetUnit, recursionDepth) ->
                showEventProbeMessage(stage, "AfterCustomUnitConvert",
                        "AfterCustomUnitConvert depth=" + recursionDepth
                                + " unit=" + describeObject(unit)
                                + " action=" + describeObject(action),
                        unit, 750L));

        CustomUnitRuntimeEvents.BEFORE_TURRET_FIRE_AT_TARGET.register((unit, targetUnit, turretIndex) -> {
            showEventProbeMessage(stage, "BeforeTurretFireAtTarget",
                    "BeforeTurretFireAtTarget turret=" + turretIndex
                            + " unit=" + describeObject(unit)
                            + " target=" + describeObject(targetUnit),
                    unit, 500L);
            return false;
        });

        CustomUnitRuntimeEvents.AFTER_PROJECTILE_CREATED_FROM_TEMPLATE.register((projectile, targetUnit, turretIndex, template, x, y, height, direction) ->
                showEventProbeMessage(stage, "AfterProjectileCreatedFromTemplate",
                        "AfterProjectileCreatedFromTemplate turret=" + turretIndex
                                + " pos=" + formatPoint(x, y)
                                + " h=" + formatFloat(height)
                                + " dir=" + formatFloat(direction),
                        projectile, 500L));

        CustomUnitRuntimeEvents.AFTER_PROJECTILE_TEMPLATE_APPLIED.register((projectile, targetUnit, turretIndex, template, x, y, height, direction) ->
                showEventProbeMessage(stage, "AfterProjectileTemplateApplied",
                        "AfterProjectileTemplateApplied turret=" + turretIndex
                                + " pos=" + formatPoint(x, y)
                                + " h=" + formatFloat(height)
                                + " dir=" + formatFloat(direction)
                                + " template=" + describeObject(template),
                        projectile, 500L));

        CustomUnitRuntimeEvents.BEFORE_FIRE_PROJECTILE_AT_GROUND.register((unit, targetUnit, x, y, turretIndex, template, projectileCount) -> {
            showEventProbeMessage(stage, "BeforeFireProjectileAtGround",
                    "BeforeFireProjectileAtGround turret=" + turretIndex
                            + " count=" + projectileCount
                            + " pos=" + formatPoint(x, y)
                            + " unit=" + describeObject(unit),
                    unit, 500L);
            return false;
        });

        CustomUnitRuntimeEvents.BEFORE_RESOURCE_COST_PAID.register((resourceAmount, unit, operation) -> {
            showEventProbeMessage(stage, "BeforeResourceCostPaid",
                    "BeforeResourceCostPaid op=" + safeText(operation)
                            + " unit=" + describeObject(unit)
                            + " amount=" + describeObject(resourceAmount),
                    unit, 750L);
            return false;
        });

        CustomUnitRuntimeEvents.AFTER_MUTABLE_STATS_APPLIED.register((writerElement, unit) ->
                showEventProbeMessage(stage, "AfterMutableStatsApplied",
                        "AfterMutableStatsApplied writer=" + describeObject(writerElement)
                                + " unit=" + describeObject(unit),
                        unit, 750L));

        CustomUnitRuntimeEvents.AFTER_CUSTOM_UNIT_TRANSPORT_LOAD.register((unit, transportedUnit) ->
                showEventProbeMessage(stage, "AfterCustomUnitTransportLoad",
                        "AfterCustomUnitTransportLoad unit=" + describeObject(unit)
                                + " loaded=" + describeObject(transportedUnit),
                        unit, 750L));

        CustomUnitRuntimeEvents.AFTER_CUSTOM_UNIT_TRANSPORT_UNLOAD.register((unit, transportedUnit) ->
                showEventProbeMessage(stage, "AfterCustomUnitTransportUnload",
                        "AfterCustomUnitTransportUnload unit=" + describeObject(unit)
                                + " unloaded=" + describeObject(transportedUnit),
                        unit, 750L));

        CustomUnitRuntimeEvents.AFTER_CUSTOM_UNIT_KILLED.register(unit ->
                showEventProbeMessage(stage, "AfterCustomUnitKilled",
                        "AfterCustomUnitKilled unit=" + describeObject(unit),
                        unit, 750L));

        CustomUnitRuntimeEvents.AFTER_CUSTOM_UNIT_REMOVED.register(unit ->
                showEventProbeMessage(stage, "AfterCustomUnitRemoved",
                        "AfterCustomUnitRemoved unit=" + describeObject(unit),
                        unit, 750L));

        CustomUnitRuntimeEvents.AFTER_BUILD_QUEUE_ITEM_COMPLETE.register((unit, queueItem) ->
                showEventProbeMessage(stage, "AfterBuildQueueItemComplete",
                        "AfterBuildQueueItemComplete unit=" + describeObject(unit)
                                + " item=" + describeObject(queueItem),
                        unit, 750L));

        MapDiscoveryEvents.BEFORE_EXTRA_MAPS_FOR_PATH.register((modManager, originalMaps, mapPath) -> {
            showEventProbeMessage(stage, "BeforeExtraMapsForPath",
                    "BeforeExtraMapsForPath " + formatMapPath(mapPath)
                            + " original=" + countArray(originalMaps),
                    modManager, 750L);
            return false;
        });

        MapDiscoveryEvents.AFTER_EXTRA_MAPS_FOR_PATH.register((modManager, originalMaps, mapPath, currentResult) -> {
            showEventProbeMessage(stage, "AfterExtraMapsForPath",
                    "AfterExtraMapsForPath " + formatMapPath(mapPath)
                            + " result=" + countArray(currentResult),
                    modManager, 750L);
            return currentResult;
        });

        MapDiscoveryEvents.BEFORE_MAP_LIST_DIRECTORY_SCAN.register((path, includeDirectories) -> {
            showEventProbeMessage(stage, "BeforeMapListDirectoryScan",
                    "BeforeMapListDirectoryScan path=" + compactPath(path)
                            + " includeDirs=" + includeDirectories,
                    null, 750L);
            return false;
        });

        MapDiscoveryEvents.AFTER_MAP_LIST_DIRECTORY_SCAN.register((path, includeDirectories, currentResult) -> {
            showEventProbeMessage(stage, "AfterMapListDirectoryScan",
                    "AfterMapListDirectoryScan path=" + compactPath(path)
                            + " includeDirs=" + includeDirectories
                            + " result=" + countArray(currentResult),
                    null, 750L);
            return currentResult;
        });

        MapDiscoveryEvents.AFTER_EXTRA_MAP_RECORD_ADDED.register((modManager, originalPath, modInfo, extraMapRecord) ->
                showEventProbeMessage(stage, "AfterExtraMapRecordAdded",
                        "AfterExtraMapRecordAdded path=" + compactPath(originalPath)
                                + " mod=" + describeObject(modInfo)
                                + " record=" + describeObject(extraMapRecord),
                        extraMapRecord, 750L));

        MapDiscoveryEvents.AFTER_MULTIPLAYER_MAP_DROPDOWN_BUILT.register((multiplayerScript, rootElement, mapsElementId, typeElementId, rawMaps) ->
                showEventProbeMessage(stage, "AfterMultiplayerMapDropdownBuilt",
                        "AfterMultiplayerMapDropdownBuilt mapsId=" + safeText(mapsElementId)
                                + " typeId=" + safeText(typeElementId)
                                + " maps=" + countArray(rawMaps),
                        multiplayerScript, 750L));

        MapDiscoveryEvents.BEFORE_MAP_START_FROM_ANDROID_UI.register((mapPath, customMap, playerCount, aiDifficulty, fog, revealedMap) -> {
            showEventProbeMessage(stage, "BeforeMapStartFromAndroidUi",
                    "BeforeMapStartFromAndroidUi " + formatMapPath(mapPath)
                            + " players=" + playerCount
                            + " ai=" + aiDifficulty
                            + " fog=" + fog
                            + " revealed=" + revealedMap
                            + " custom=" + customMap,
                    null, 500L);
            return false;
        });

        MapDiscoveryEvents.BEFORE_NETWORK_MAP_PATH_RESOLVE.register((networkEngine, gameSetup, mapPath, mapType) -> {
            showEventProbeMessage(stage, "BeforeNetworkMapPathResolve",
                    "BeforeNetworkMapPathResolve " + formatMapPath(mapPath)
                            + " type=" + describeObject(mapType),
                    networkEngine, 750L);
            return null;
        });

        MapMissionEvents.BEFORE_CURRENT_MAP_LOAD.register((gameEngine, optionA, optionB, mode) -> {
            showEventProbeMessage(stage, "BeforeCurrentMapLoad",
                    "BeforeCurrentMapLoad optionA=" + optionA
                            + " optionB=" + optionB
                            + " mode=" + describeObject(mode),
                    gameEngine, 500L);
            return false;
        });

        MapMissionEvents.BEFORE_MAP_STREAM_OPEN.register(mapPath -> {
            showEventProbeMessage(stage, "BeforeMapStreamOpen",
                    "BeforeMapStreamOpen " + formatMapPath(mapPath),
                    null, 500L);
            return false;
        });

        MapMissionEvents.AFTER_MAP_OBJECT_GROUPS_LOADED.register(mapEngine ->
                showEventProbeMessage(stage, "AfterMapObjectGroupsLoaded",
                        "AfterMapObjectGroupsLoaded mapEngine=" + describeObject(mapEngine),
                        mapEngine, 500L));

        MapMissionEvents.BEFORE_MISSION_TRIGGERS_PARSE.register((missionEngine, mapObject) -> {
            showEventProbeMessage(stage, "BeforeMissionTriggersParse",
                    "BeforeMissionTriggersParse mission=" + describeObject(missionEngine)
                            + " object=" + describeObject(mapObject),
                    mapObject, 500L);
            return false;
        });

        MapMissionEvents.AFTER_MISSION_TRIGGERS_LINKED.register((missionEngine, trigger) ->
                showEventProbeMessage(stage, "AfterMissionTriggersLinked",
                        "AfterMissionTriggersLinked mission=" + describeObject(missionEngine)
                                + " trigger=" + describeObject(trigger),
                        trigger, 500L));

        MapMissionEvents.AFTER_CURRENT_MAP_STARTED.register((gameEngine, optionA, optionB, mode) ->
                showEventProbeMessage(stage, "AfterCurrentMapStarted",
                        "AfterCurrentMapStarted optionA=" + optionA
                                + " optionB=" + optionB
                                + " mode=" + describeObject(mode),
                        gameEngine, 500L));

        MapMissionEvents.BEFORE_TMX_DOCUMENT_PARSE.register((mapEngine, inputStream, newGame) -> {
            showEventProbeMessage(stage, "BeforeTmxDocumentParse",
                    "BeforeTmxDocumentParse newGame=" + newGame
                            + " mapEngine=" + describeObject(mapEngine),
                    mapEngine, 500L);
            return false;
        });

        MapMissionEvents.AFTER_MAP_ATTRIBUTES_READ.register((mapEngine, inputStream, newGame) ->
                showEventProbeMessage(stage, "AfterMapAttributesRead",
                        "AfterMapAttributesRead newGame=" + newGame
                                + " mapEngine=" + describeObject(mapEngine),
                        mapEngine, 500L));

        MapMissionEvents.AFTER_TILESETS_LOADED.register((mapEngine, inputStream, newGame) ->
                showEventProbeMessage(stage, "AfterTilesetsLoaded",
                        "AfterTilesetsLoaded newGame=" + newGame
                                + " mapEngine=" + describeObject(mapEngine),
                        mapEngine, 500L));

        MapMissionEvents.AFTER_MAP_LAYERS_LOADED.register((mapEngine, inputStream, newGame) ->
                showEventProbeMessage(stage, "AfterMapLayersLoaded",
                        "AfterMapLayersLoaded newGame=" + newGame
                                + " mapEngine=" + describeObject(mapEngine),
                        mapEngine, 500L));

        MapMissionEvents.AFTER_CURRENT_MAP_LOADED_BEFORE_STARTING_UNITS.register((gameEngine, mapEngine, optionA, optionB, mode) ->
                showEventProbeMessage(stage, "AfterCurrentMapLoadedBeforeStartingUnits",
                        "AfterCurrentMapLoadedBeforeStartingUnits optionA=" + optionA
                                + " optionB=" + optionB
                                + " mode=" + describeObject(mode),
                        mapEngine, 500L));

        MapSpawnEvents.BEFORE_MAP_OBJECT_SPAWN_UNIT.register((mapObject, mapEngine, objectGroup, properties, unitName, customUnitName, teamName) -> {
            showEventProbeMessage(stage, "BeforeMapObjectSpawnUnit",
                    "BeforeMapObjectSpawnUnit unit=" + safeText(unitName)
                            + " custom=" + safeText(customUnitName)
                            + " team=" + safeText(teamName)
                            + " props=" + countProperties(properties),
                    mapObject, 500L);
            return false;
        });

        MapSpawnEvents.MAP_OBJECT_CUSTOM_UNIT_RESOLVE.register((mapObject, mapEngine, objectGroup, customUnitName, currentMetadata) -> {
            showEventProbeMessage(stage, "MapObjectCustomUnitResolve",
                    "MapObjectCustomUnitResolve custom=" + safeText(customUnitName)
                            + " current=" + describeObject(currentMetadata),
                    mapObject, 500L);
            return currentMetadata;
        });

        MapSpawnEvents.AFTER_MAP_OBJECT_SPAWN_UNIT.register((mapObject, mapEngine, objectGroup, unit, properties) ->
                showEventProbeMessage(stage, "AfterMapObjectSpawnUnit",
                        "AfterMapObjectSpawnUnit unit=" + describeObject(unit)
                                + " props=" + countProperties(properties),
                        unit, 500L));

        MapSpawnEvents.BEFORE_TILE_PROPERTY_SPAWN_UNIT.register((tileset, properties, propertyName, propertyValue) -> {
            showEventProbeMessage(stage, "BeforeTilePropertySpawnUnit",
                    "BeforeTilePropertySpawnUnit " + safeText(propertyName)
                            + "=" + safeText(propertyValue)
                            + " props=" + countProperties(properties),
                    tileset, 500L);
            return false;
        });

        MapSpawnEvents.AFTER_TILE_PROPERTY_SPAWN_UNIT.register((tileset, properties, propertyName, propertyValue) ->
                showEventProbeMessage(stage, "AfterTilePropertySpawnUnit",
                        "AfterTilePropertySpawnUnit " + safeText(propertyName)
                                + "=" + safeText(propertyValue)
                                + " props=" + countProperties(properties),
                        tileset, 500L));

        MapSpawnEvents.BEFORE_STARTING_UNIT_SPAWN.register((unitType, x, y, direction, height, team) -> {
            showEventProbeMessage(stage, "BeforeStartingUnitSpawn",
                    "BeforeStartingUnitSpawn type=" + describeObject(unitType)
                            + " pos=" + formatPoint(x, y)
                            + " dir=" + formatFloat(direction)
                            + " h=" + formatFloat(height)
                            + " team=" + describeObject(team),
                    unitType, 500L);
            return false;
        });

        MapSpawnEvents.AFTER_STARTING_UNIT_SPAWN.register((unitType, x, y, direction, height, team, result) -> {
            showEventProbeMessage(stage, "AfterStartingUnitSpawn",
                    "AfterStartingUnitSpawn result=" + result
                            + " type=" + describeObject(unitType)
                            + " pos=" + formatPoint(x, y)
                            + " team=" + describeObject(team),
                    unitType, 500L);
            return result;
        });

        ResourceRuntimeEvents.AFTER_RESOURCE_AMOUNT_SUBTRACT.register((resourceAmount, unit, scale, scaled, operation) ->
                showEventProbeMessage(stage, "AfterResourceAmountSubtract",
                        "AfterResourceAmountSubtract op=" + safeText(operation)
                                + " scale=" + formatDouble(scale)
                                + " scaled=" + scaled
                                + " unit=" + describeObject(unit),
                        unit, 1000L));

        ResourceRuntimeEvents.AFTER_RESOURCE_AMOUNT_ADD.register((resourceAmount, unit, scale, scaled, operation) ->
                showEventProbeMessage(stage, "AfterResourceAmountAdd",
                        "AfterResourceAmountAdd op=" + safeText(operation)
                                + " scale=" + formatDouble(scale)
                                + " scaled=" + scaled
                                + " unit=" + describeObject(unit),
                        unit, 1000L));

        ResourceRuntimeEvents.AFTER_TAKE_RESOURCES_COLLECT.register((effect, unit, action, targetPoint, targetUnit, recursionDepth, result) ->
                showEventProbeMessage(stage, "AfterTakeResourcesCollect",
                        "AfterTakeResourcesCollect result=" + result
                                + " depth=" + recursionDepth
                                + " unit=" + describeObject(unit)
                                + " target=" + describeObject(targetUnit),
                        unit, 1000L));

        ResourceRuntimeEvents.AFTER_RESOURCE_CONVERSION.register((effect, unit, action, targetPoint, targetUnit, recursionDepth, result) ->
                showEventProbeMessage(stage, "AfterResourceConversion",
                        "AfterResourceConversion result=" + result
                                + " depth=" + recursionDepth
                                + " unit=" + describeObject(unit)
                                + " target=" + describeObject(targetUnit),
                        unit, 1000L));

        ResourceRuntimeEvents.RESOURCE_AVAILABILITY_CHECK.register((resourceAmount, unit, scale, scaled, operation, currentResult) -> {
            showEventProbeMessage(stage, "ResourceAvailabilityCheck",
                    "ResourceAvailabilityCheck result=" + currentResult
                            + " op=" + safeText(operation)
                            + " scale=" + formatDouble(scale)
                            + " unit=" + describeObject(unit),
                    unit, 1500L);
            return currentResult;
        });

        ResourceRuntimeEvents.AFTER_RESOURCE_RESERVE.register((resourceAmount, unit, lagHiding, operation, result) ->
                showEventProbeMessage(stage, "AfterResourceReserve",
                        "AfterResourceReserve result=" + result
                                + " op=" + safeText(operation)
                                + " lagHiding=" + lagHiding
                                + " unit=" + describeObject(unit),
                        unit, 1000L));

        SaveSyncEvents.BEFORE_SAVE_GAME_TO_FILE.register((gameSaver, saveName, autoSave) -> {
            showEventProbeMessage(stage, "BeforeSaveGameToFile",
                    "BeforeSaveGameToFile name=" + safeText(saveName)
                            + " auto=" + autoSave,
                    gameSaver, 500L);
            return false;
        });

        SaveSyncEvents.BEFORE_WRITE_SAVE_STREAM.register((gameSaver, outputStream) -> {
            showEventProbeMessage(stage, "BeforeWriteSaveStream",
                    "BeforeWriteSaveStream saver=" + describeObject(gameSaver)
                            + " out=" + describeObject(outputStream),
                    gameSaver, 500L);
            return false;
        });

        SaveSyncEvents.AFTER_WRITE_SAVE_STREAM.register((gameSaver, outputStream) ->
                showEventProbeMessage(stage, "AfterWriteSaveStream",
                        "AfterWriteSaveStream saver=" + describeObject(gameSaver)
                                + " out=" + describeObject(outputStream),
                        gameSaver, 500L));

        SaveSyncEvents.BEFORE_READ_SAVE_STREAM.register((gameSaver, inputStream, optionA, optionB, optionC) -> {
            showEventProbeMessage(stage, "BeforeReadSaveStream",
                    "BeforeReadSaveStream optionA=" + optionA
                            + " optionB=" + optionB
                            + " optionC=" + optionC,
                    gameSaver, 500L);
            return false;
        });

        SaveSyncEvents.AFTER_READ_SAVE_STREAM.register((gameSaver, inputStream, optionA, optionB, optionC, result) ->
                showEventProbeMessage(stage, "AfterReadSaveStream",
                        "AfterReadSaveStream result=" + result
                                + " optionA=" + optionA
                                + " optionB=" + optionB
                                + " optionC=" + optionC,
                        gameSaver, 500L));

        SaveSyncEvents.BEFORE_NETWORK_RESYNC_SAVE.register((networkEngine, connection, saveBytes, optionA, optionB, reloadCreatedSave, operation) -> {
            showEventProbeMessage(stage, "BeforeNetworkResyncSave",
                    "BeforeNetworkResyncSave bytes=" + countBytes(saveBytes)
                            + " reload=" + reloadCreatedSave
                            + " op=" + safeText(operation),
                    networkEngine, 1000L);
            return false;
        });

        SaveSyncEvents.AFTER_NETWORK_RESYNC_PACKET_CREATED.register((networkEngine, connection, packet, saveBytes, optionA, optionB, reloadCreatedSave, operation) ->
                showEventProbeMessage(stage, "AfterNetworkResyncPacketCreated",
                        "AfterNetworkResyncPacketCreated bytes=" + countBytes(saveBytes)
                                + " packet=" + describeObject(packet)
                                + " op=" + safeText(operation),
                        networkEngine, 1000L));

        SaveSyncEvents.BEFORE_REPLAY_RECORD_COMMAND.register((replayEngine, command, frame) -> {
            showEventProbeMessage(stage, "BeforeReplayRecordCommand",
                    "BeforeReplayRecordCommand frame=" + frame
                            + " command=" + describeObject(command),
                    replayEngine, 1000L);
            return false;
        });

        SaveSyncEvents.BEFORE_REPLAY_PLAYBACK_BLOCK.register(replayEngine -> {
            showEventProbeMessage(stage, "BeforeReplayPlaybackBlock",
                    "BeforeReplayPlaybackBlock replay=" + describeObject(replayEngine),
                    replayEngine, 1000L);
            return false;
        });

        SaveSyncEvents.BEFORE_CHECKSUM_SEND.register((networkEngine, packet, checksum, delta) -> {
            showEventProbeMessage(stage, "BeforeChecksumSend",
                    "BeforeChecksumSend delta=" + formatFloat(delta)
                            + " packet=" + describeObject(packet)
                            + " checksum=" + describeObject(checksum),
                    networkEngine, 1000L);
            return false;
        });

        SaveSyncEvents.BEFORE_GAME_OBJECT_SERIALIZE.register((gameObject, outputStream) -> {
            showEventProbeMessage(stage, "BeforeGameObjectSerialize",
                    "BeforeGameObjectSerialize object=" + describeObject(gameObject)
                            + " out=" + describeObject(outputStream),
                    gameObject, 250L);
            return false;
        });

        SaveSyncEvents.AFTER_GAME_OBJECT_DESERIALIZE.register((gameObject, inputStream) ->
                showEventProbeMessage(stage, "AfterGameObjectDeserialize",
                        "AfterGameObjectDeserialize object=" + describeObject(gameObject)
                                + " in=" + describeObject(inputStream),
                        gameObject, 250L));

        log("registered event probe messages from " + stage);
    }

    private static void waitForSettingsAndApplyTweaks(String stage) {
        Throwable lastFailure = null;
        boolean settingsApplied = false;
        boolean settingsTimeoutLogged = false;

        for (int attempt = 1; !settingsApplied; attempt++) {
            try {
                GameEngine engine = GameEngine.getInstance();
                if (engine != null && engine.settings != null) {
                    applyVisibleSettingsTweaks(stage, engine.settings);
                    settingsApplied = true;
                }

                if (!settingsApplied && !settingsTimeoutLogged && attempt >= 600) {
                    settingsTimeoutLogged = true;
                    if (lastFailure != null) {
                        log("visible settings tweaks still waiting, last failure="
                                + lastFailure.getClass().getName() + ": " + lastFailure.getMessage());
                    } else {
                        log("visible settings tweaks still waiting before SettingsEngine was ready");
                    }
                }
            } catch (Throwable t) {
                lastFailure = t;
            }

            try {
                Thread.sleep(250L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log("visible settings tweaks interrupted before GameEngine was ready");
                return;
            }
        }
    }

    private static void applyVisibleSettingsTweaks(String stage, SettingsEngine settings) {
        boolean oldShowFps = settings.showFps;
        boolean oldShowHp = settings.showHp;
        boolean oldShowHpChanges = settings.showHpChanges;
        boolean oldShowUnitIcons = settings.showUnitIcons;
        boolean oldShowWarLogOnScreen = settings.showWarLogOnScreen;

        settings.showFps = true;
        settings.showHp = true;
        settings.showHpChanges = true;
        settings.showUnitIcons = true;
        settings.showWarLogOnScreen = true;

        log("visible settings tweaks applied from " + stage
                + ": showFps " + oldShowFps + "->" + settings.showFps
                + ", showHp " + oldShowHp + "->" + settings.showHp
                + ", showHpChanges " + oldShowHpChanges + "->" + settings.showHpChanges
                + ", showUnitIcons " + oldShowUnitIcons + "->" + settings.showUnitIcons
                + ", showWarLogOnScreen " + oldShowWarLogOnScreen + "->" + settings.showWarLogOnScreen);
    }

    private static void waitForMainMenuAndShowPopup(String stage) {
        Throwable lastFailure = null;
        boolean timeoutLogged = false;

        for (int attempt = 1; !MAIN_MENU_POPUP_SHOWN.get(); attempt++) {
            try {
                ScriptEngine scriptEngine = ScriptEngine.getInstance();
                if (scriptEngine != null) {
                    scriptEngine.addRunnableToQueue(() -> tryShowMainMenuPopup(stage, scriptEngine));
                }

                if (!MAIN_MENU_POPUP_SHOWN.get() && !timeoutLogged && attempt >= 600) {
                    timeoutLogged = true;
                    if (lastFailure != null) {
                        log("main menu popup still waiting, last failure="
                                + lastFailure.getClass().getName() + ": " + lastFailure.getMessage());
                    } else {
                        log("main menu popup still waiting before mainMenu.rml was shown");
                    }
                }
            } catch (Throwable t) {
                lastFailure = t;
            }

            try {
                Thread.sleep(250L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log("main menu popup interrupted before mainMenu.rml was shown");
                return;
            }
        }
    }

    private static void tryShowMainMenuPopup(String stage, ScriptEngine scriptEngine) {
        if (MAIN_MENU_POPUP_SHOWN.get()) {
            return;
        }

        try {
            RootScript root = scriptEngine.getRootNoCheck();
            if (root == null) {
                return;
            }

            String documentPath = root.getCurrentDocumentPath();
            if (!isMainMenuDocument(documentPath)) {
                return;
            }

            if (MAIN_MENU_POPUP_SHOWN.compareAndSet(false, true)) {
                root.showAlert(formatOverlayMessage(stage, "MainMenuPopup"));
                log("showed main menu popup from " + stage);
            }
        } catch (Throwable t) {
            MAIN_MENU_POPUP_SHOWN.set(false);
            log("main menu popup failed: " + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    private static boolean isMainMenuDocument(String documentPath) {
        return documentPath != null
                && documentPath.replace('\\', '/').endsWith("mainMenu.rml");
    }

    private static void showMapEntryMessage(String stage, Object map) {
        String mapPath = getCurrentMapPathForLog();
        if (isDuplicateMapEntryMessage(mapPath)) {
            return;
        }

        enqueueOverlayMessage(stage, "AfterMapSetup " + formatMapPath(mapPath), map);
    }

    private static void showEventProbeMessage(String stage, String message, Object source) {
        showEventProbeMessage(stage, message, message, source, 1500L);
    }

    private static void showEventProbeMessage(String stage, String key, String message, Object source, long minIntervalMillis) {
        if (isDuplicateEventProbeMessage(key, minIntervalMillis)) {
            return;
        }

        enqueueOverlayMessage(stage, message, source);
    }

    private static void enqueueOverlayMessage(String stage, String message, Object source) {
        String text = formatOverlayMessage(stage, message);
        synchronized (OVERLAY_LOCK) {
            OVERLAY_MESSAGES.add(new OverlayMessage(text));
            while (OVERLAY_MESSAGES.size() > MAX_OVERLAY_MESSAGES) {
                OVERLAY_MESSAGES.remove(0);
            }
        }

        log("queued overlay message from " + stage
                + ": " + text
                + ", source=" + (source != null ? source.getClass().getName() : "null"));
    }

    private static void drawOverlayMessages(Object renderer) {
        try {
            if (OVERLAY_RENDER_HOOK_SEEN.compareAndSet(false, true)) {
                log("green overlay render hook reached: "
                        + (renderer != null ? renderer.getClass().getName() : "null"));
            }
            drawOverlayMessagesUnchecked(renderer);
        } catch (Throwable t) {
            logOverlayDrawFailure(t);
        }
    }

    private static void drawOverlayMessagesUnchecked(Object frameRenderer) throws ReflectiveOperationException {
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

    private static int[] getOverlaySurfaceSize(Object frameRenderer, GameEngine engine) throws ReflectiveOperationException {
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
            log("green overlay dimensions: surface=" + screenWidth + "x" + screenHeight
                    + ", engine=" + engine.bA + "x" + engine.bB
                    + ", frameRenderer=" + (frameRenderer != null ? frameRenderer.getClass().getName() : "null")
                    + ", gameContainer=" + (gameContainer != null ? gameContainer.getClass().getName() : "null"));
        }

        return new int[]{screenWidth, screenHeight};
    }

    private static Object getSlickGameContainer(Object frameRenderer) throws ReflectiveOperationException {
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

    private static Object getSlickGraphics(GraphicsEngine renderer) throws ReflectiveOperationException {
        if (renderer == null) {
            return null;
        }

        if (slickGraphicsField == null) {
            slickGraphicsField = renderer.getClass().getField("f");
        }
        return slickGraphicsField.get(renderer);
    }

    private static void drawSlickOverlay(Object graphics, List<OverlayMessage> messages,
                                         int left, int top, int boxWidth, int boxHeight, int gap, int maxTextWidth)
            throws ReflectiveOperationException {
        initSlickReflection(graphics);

        if (OVERLAY_SLICK_DRAW_SEEN.compareAndSet(false, true)) {
            log("green overlay drawing with Slick Graphics: " + graphics.getClass().getName());
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

    private static void drawScaledSlickText(Object graphics, String text, float x, float y)
            throws ReflectiveOperationException {
        slickPushTransformMethod.invoke(graphics);
        try {
            slickScaleMethod.invoke(graphics, SLICK_TEXT_SCALE, SLICK_TEXT_SCALE);
            slickDrawStringMethod.invoke(graphics, text, x / SLICK_TEXT_SCALE, y / SLICK_TEXT_SCALE);
        } finally {
            slickPopTransformMethod.invoke(graphics);
        }
    }

    private static void initSlickReflection(Object graphics) throws ReflectiveOperationException {
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

    private static String fitSlickOverlayText(Object graphics, String text, int maxWidth) throws ReflectiveOperationException {
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

    private static int getSlickTextWidth(Object graphics, String text) throws ReflectiveOperationException {
        Object font = slickGetFontMethod.invoke(graphics);
        if (slickFontGetWidthMethod == null) {
            slickFontGetWidthMethod = font.getClass().getMethod("getWidth", String.class);
        }
        return (Integer) slickFontGetWidthMethod.invoke(font, text);
    }

    private static void logOverlayDrawFailure(Throwable t) {
        long now = System.currentTimeMillis();
        if (now - lastOverlayDrawFailureLogMillis < 5000L) {
            return;
        }

        lastOverlayDrawFailureLogMillis = now;
        log("overlay draw failed: " + t.getClass().getName() + ": " + t.getMessage());
    }

    private static List<OverlayMessage> collectVisibleOverlayMessages() {
        synchronized (OVERLAY_LOCK) {
            return new ArrayList<>(OVERLAY_MESSAGES);
        }
    }

    private static void initOverlayPaints() {
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

    private static String fitOverlayText(GraphicsEngine renderer, String text, int maxWidth) {
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

    private static String formatOverlayMessage(String stage, String message) {
        return "[" + stage + "] " + message;
    }

    private static String formatMapPath(String mapPath) {
        if (mapPath == null || mapPath.trim().isEmpty()) {
            return "map=<unknown>";
        }

        return "map=" + compactPath(mapPath);
    }

    private static String compactPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "<unknown>";
        }

        String normalized = path.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
    }

    private static int countArray(String[] values) {
        return values != null ? values.length : 0;
    }

    private static int countBytes(byte[] values) {
        return values != null ? values.length : 0;
    }

    private static int countProperties(Properties properties) {
        return properties != null ? properties.size() : 0;
    }

    private static String safeText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "<none>";
        }

        return value.replace('\n', ' ').replace('\r', ' ');
    }

    private static String describeObject(Object value) {
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

    private static String formatPoint(float x, float y) {
        return formatFloat(x) + "," + formatFloat(y);
    }

    private static String formatFloat(float value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static String formatDouble(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static String getCurrentMapPathForLog() {
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

    private static boolean isDuplicateMapEntryMessage(String mapPath) {
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

    private static boolean isDuplicateEventProbeMessage(String key, long minIntervalMillis) {
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

    static void log(String message) {
        System.out.println("[Rusted Fabric Example] " + message);
    }

    private static final class OverlayMessage {
        final String text;

        OverlayMessage(String text) {
            this.text = text;
        }
    }
}
