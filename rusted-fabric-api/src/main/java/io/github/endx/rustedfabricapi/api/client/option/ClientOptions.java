package io.github.endx.rustedfabricapi.api.client.option;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import io.github.endx.rustedfabricapi.api.client.option.event.ClientOptionEvents;
import io.github.endx.rustedfabricapi.api.util.Identifier;
import rustedwarfare.core.GameEngine;
import rustedwarfare.core.SettingsEngine;

/** Fabric-style typed catalog and atomic update access for user-visible desktop settings. */
public final class ClientOptions {
    private static final List<ClientOption<?>> REGISTRY = new ArrayList<ClientOption<?>>();
    private static final Map<String, ClientOption<?>> BY_NATIVE_NAME =
            new LinkedHashMap<String, ClientOption<?>>();

    public static final ClientOption<Boolean> ENABLE_SOUNDS = bool("enable_sounds", "enableSounds",
            false, s -> s.enableSounds, (s, v) -> s.enableSounds = v);
    public static final ClientOption<Float> MASTER_VOLUME = volume("master_volume", "masterVolume",
            s -> s.masterVolume, (s, v) -> s.masterVolume = v);
    public static final ClientOption<Float> GAME_VOLUME = volume("game_volume", "gameVolume",
            s -> s.gameVolume, (s, v) -> s.gameVolume = v);
    public static final ClientOption<Float> INTERFACE_VOLUME = volume(
            "interface_volume", "interfaceVolume",
            s -> s.interfaceVolume, (s, v) -> s.interfaceVolume = v);
    public static final ClientOption<Float> MUSIC_VOLUME = volume("music_volume", "musicVolume",
            s -> s.musicVolume, (s, v) -> s.musicVolume = v);
    public static final ClientOption<Float> SCROLL_SPEED = nonNegativeFloat(
            "scroll_speed", "scrollSpeed", s -> s.scrollSpeed, (s, v) -> s.scrollSpeed = v);
    public static final ClientOption<Float> EDGE_SCROLL_SPEED = nonNegativeFloat(
            "edge_scroll_speed", "edgeScrollSpeed",
            s -> s.edgeScrollSpeed, (s, v) -> s.edgeScrollSpeed = v);
    public static final ClientOption<Float> UI_RENDER_SCALE = positiveFloat(
            "ui_render_scale", "uiRenderScale", true,
            s -> s.uiRenderScale, (s, v) -> s.uiRenderScale = v);

    public static final ClientOption<Boolean> MOUSE_CAPTURE = bool(
            "mouse_capture", "enableMouseCapture", false,
            s -> s.enableMouseCapture, (s, v) -> s.enableMouseCapture = v);
    public static final ClientOption<Boolean> FULLSCREEN = bool(
            "fullscreen", "slick2dFullScreen", true,
            s -> s.slick2dFullScreen, (s, v) -> s.slick2dFullScreen = v);
    public static final ClientOption<Boolean> BORDERLESS = bool(
            "borderless", "slick2dBorderless", true,
            s -> s.slick2dBorderless, (s, v) -> s.slick2dBorderless = v);
    public static final ClientOption<Boolean> VSYNC = bool("vsync", "renderVsync", true,
            s -> s.renderVsync, (s, v) -> s.renderVsync = v);
    public static final ClientOption<Boolean> SMOOTH_DELTA = bool(
            "smooth_delta", "renderSmoothDelta", false,
            s -> s.renderSmoothDelta, (s, v) -> s.renderSmoothDelta = v);
    public static final ClientOption<Boolean> HIGH_GRAPHICS = bool(
            "high_graphics", "highGraphics", false,
            s -> s.highGraphics, (s, v) -> s.highGraphics = v);
    public static final ClientOption<Boolean> ANTI_ALIASING = bool(
            "anti_aliasing", "renderAntiAlias", false,
            s -> s.renderAntiAlias, (s, v) -> s.renderAntiAlias = v);
    public static final ClientOption<Boolean> EXTRA_SHADOWS = bool(
            "extra_shadows", "renderExtraShadows", false,
            s -> s.renderExtraShadows, (s, v) -> s.renderExtraShadows = v);
    public static final ClientOption<Boolean> FANCY_WATER = bool(
            "fancy_water", "renderFancyWater", false,
            s -> s.renderFancyWater, (s, v) -> s.renderFancyWater = v);
    public static final ClientOption<Boolean> CLOUDS = bool("clouds", "renderClouds", false,
            s -> s.renderClouds, (s, v) -> s.renderClouds = v);
    public static final ClientOption<Boolean> SHADER_EFFECTS = bool(
            "shader_effects", "shaderEffects", true,
            s -> s.shaderEffects, (s, v) -> s.shaderEffects = v);
    public static final ClientOption<Boolean> TEAM_SHADERS = bool(
            "team_shaders", "teamShaders", true,
            s -> s.teamShaders, (s, v) -> s.teamShaders = v);

    public static final ClientOption<Boolean> SHOW_FPS = bool("show_fps", "showFps", false,
            s -> s.showFps, (s, v) -> s.showFps = v);
    public static final ClientOption<Boolean> SHOW_HP = bool("show_hp", "showHp", false,
            s -> s.showHp, (s, v) -> s.showHp = v);
    public static final ClientOption<Boolean> SHOW_HP_CHANGES = bool(
            "show_hp_changes", "showHpChanges", false,
            s -> s.showHpChanges, (s, v) -> s.showHpChanges = v);
    public static final ClientOption<Boolean> SHOW_UNIT_ICONS = bool(
            "show_unit_icons", "showUnitIcons", false,
            s -> s.showUnitIcons, (s, v) -> s.showUnitIcons = v);
    public static final ClientOption<Boolean> SHOW_UNIT_WAYPOINTS = bool(
            "show_unit_waypoints", "showUnitWaypoints", false,
            s -> s.showUnitWaypoints, (s, v) -> s.showUnitWaypoints = v);
    public static final ClientOption<Boolean> MINIMAP_ALLY_COLORS = bool(
            "minimap_ally_colors", "useMinimapAllyColors", false,
            s -> s.useMinimapAllyColors, (s, v) -> s.useMinimapAllyColors = v);
    public static final ClientOption<Boolean> SHOW_WAR_LOG = bool(
            "show_war_log", "showWarLogOnScreen", false,
            s -> s.showWarLogOnScreen, (s, v) -> s.showWarLogOnScreen = v);
    public static final ClientOption<Boolean> CLASSIC_INTERFACE = bool(
            "classic_interface", "classicInterface", false,
            s -> s.classicInterface, (s, v) -> s.classicInterface = v);
    public static final ClientOption<Boolean> QUICK_RALLY = bool(
            "quick_rally", "quickRally", false,
            s -> s.quickRally, (s, v) -> s.quickRally = v);
    public static final ClientOption<Boolean> DOUBLE_CLICK_ATTACK_MOVE = bool(
            "double_click_attack_move", "doubleClickToAttackMove", false,
            s -> s.doubleClickToAttackMove, (s, v) -> s.doubleClickToAttackMove = v);
    public static final ClientOption<Boolean> SMART_SELECTION = bool(
            "smart_selection", "smartSelection_v2", false,
            s -> s.smartSelection_v2, (s, v) -> s.smartSelection_v2 = v);
    public static final ClientOption<Boolean> SHOW_SELECTED_UNITS_LIST = bool(
            "show_selected_units_list", "showSelectedUnitsList", false,
            s -> s.showSelectedUnitsList, (s, v) -> s.showSelectedUnitsList = v);
    public static final ClientOption<Boolean> SHOW_ACTION_INFO_NEAR_MOUSE = bool(
            "show_action_info_near_mouse", "showActionInfoHoverNearMouse", false,
            s -> s.showActionInfoHoverNearMouse, (s, v) -> s.showActionInfoHoverNearMouse = v);

    public static final ClientOption<Boolean> BATTLEFIELD_PINGS = bool(
            "battlefield_pings", "showMapPingsOnBattlefield", false,
            s -> s.showMapPingsOnBattlefield, (s, v) -> s.showMapPingsOnBattlefield = v);
    public static final ClientOption<Boolean> MINIMAP_PINGS = bool(
            "minimap_pings", "showMapPingsOnMinimap", false,
            s -> s.showMapPingsOnMinimap, (s, v) -> s.showMapPingsOnMinimap = v);
    public static final ClientOption<Boolean> PLAYER_CHAT = bool(
            "player_chat", "showPlayerChatInGame", false,
            s -> s.showPlayerChatInGame, (s, v) -> s.showPlayerChatInGame = v);
    public static final ClientOption<Boolean> SAVE_MULTIPLAYER_REPLAYS = bool(
            "save_multiplayer_replays", "saveMultiplayerReplays", false,
            s -> s.saveMultiplayerReplays, (s, v) -> s.saveMultiplayerReplays = v);
    public static final ClientOption<Boolean> AUTOSAVING = bool(
            "autosaving", "autosaving", false,
            s -> s.autosaving, (s, v) -> s.autosaving = v);

    private static final List<ClientOption<?>> ALL = Collections.unmodifiableList(
            new ArrayList<ClientOption<?>>(REGISTRY));

    private ClientOptions() {
    }

    public static SettingsEngine settings() {
        GameEngine engine = RustedWarfareClient.requireEngine();
        if (engine.settingsEngine == null) {
            throw new IllegalStateException("Client settings are not initialized");
        }
        return engine.settingsEngine;
    }

    public static List<ClientOption<?>> all() { return ALL; }

    public static Optional<ClientOption<?>> findByNativeName(String nativeName) {
        if (nativeName == null) return Optional.empty();
        return Optional.ofNullable(BY_NATIVE_NAME.get(nativeName));
    }

    public static <T> T get(ClientOption<T> option) {
        return Objects.requireNonNull(option, "option").get(settings());
    }

    public static ClientOptionSnapshot snapshot() {
        return ClientOptionSnapshot.capture(settings());
    }

    public static <T> ClientOptionUpdateResult set(ClientOption<T> option, T value) {
        return update(transaction -> transaction.set(option, value), true);
    }

    public static ClientOptionUpdateResult update(Consumer<ClientOptionTransaction> changes) {
        return update(changes, true);
    }

    /** Applies a validated transaction and optionally invokes the native settings save path. */
    public static ClientOptionUpdateResult update(Consumer<ClientOptionTransaction> changes,
            boolean persist) {
        Objects.requireNonNull(changes, "changes");
        SettingsEngine settings = settings();
        synchronized (settings) {
            ClientOptionTransaction transaction = new ClientOptionTransaction(settings);
            changes.accept(transaction);
            ClientOptionChangeSet changeSet = transaction.changes();
            if (changeSet.isEmpty()) {
                return new ClientOptionUpdateResult(changeSet, false, false,
                        false, false);
            }
            if (ClientOptionEvents.BEFORE_UPDATE.invoker()
                    .beforeUpdate(settings, changeSet)) {
                ClientOptionUpdateResult result = new ClientOptionUpdateResult(
                        changeSet, false, true, false, false);
                ClientOptionEvents.AFTER_UPDATE.invoker().afterUpdate(settings, result);
                return result;
            }

            apply(settings, changeSet);
            boolean persisted = false;
            try {
                if (persist) persisted = settings.save();
            } catch (RuntimeException | Error failure) {
                ClientOptionUpdateResult result = new ClientOptionUpdateResult(
                        changeSet, true, false, persist, false);
                ClientOptionEvents.AFTER_UPDATE.invoker().afterUpdate(settings, result);
                throw failure;
            }
            ClientOptionUpdateResult result = new ClientOptionUpdateResult(
                    changeSet, true, false, persist, persisted);
            ClientOptionEvents.AFTER_UPDATE.invoker().afterUpdate(settings, result);
            return result;
        }
    }

    public static boolean save() {
        return settings().save();
    }

    private static void apply(SettingsEngine settings, ClientOptionChangeSet changes) {
        for (ClientOptionChange<?> change : changes.changes()) apply(settings, change);
    }

    private static <T> void apply(SettingsEngine settings, ClientOptionChange<T> change) {
        change.option().set(settings, change.requestedValue());
    }

    private static ClientOption<Boolean> bool(String id, String nativeName,
            boolean restartRequired, Function<SettingsEngine, Boolean> reader,
            BiConsumer<SettingsEngine, Boolean> writer) {
        return register(id, nativeName, Boolean.class, restartRequired, reader, writer,
                value -> true, "a boolean");
    }

    private static ClientOption<Float> volume(String id, String nativeName,
            Function<SettingsEngine, Float> reader, BiConsumer<SettingsEngine, Float> writer) {
        return register(id, nativeName, Float.class, false, reader, writer,
                value -> Float.isFinite(value) && value >= 0.0F && value <= 1.0F,
                "a finite number from 0.0 to 1.0");
    }

    private static ClientOption<Float> nonNegativeFloat(String id, String nativeName,
            Function<SettingsEngine, Float> reader, BiConsumer<SettingsEngine, Float> writer) {
        return register(id, nativeName, Float.class, false, reader, writer,
                value -> Float.isFinite(value) && value >= 0.0F,
                "a finite non-negative number");
    }

    private static ClientOption<Float> positiveFloat(String id, String nativeName,
            boolean restartRequired, Function<SettingsEngine, Float> reader,
            BiConsumer<SettingsEngine, Float> writer) {
        return register(id, nativeName, Float.class, restartRequired, reader, writer,
                value -> Float.isFinite(value) && value > 0.0F,
                "a finite positive number");
    }

    private static <T> ClientOption<T> register(String id, String nativeName, Class<T> type,
            boolean restartRequired, Function<SettingsEngine, T> reader,
            BiConsumer<SettingsEngine, T> writer, Predicate<T> validator,
            String validationDescription) {
        ClientOption<T> option = new ClientOption<T>(Identifier.of("rustedwarfare", id),
                nativeName, type, restartRequired, reader, writer, validator,
                validationDescription);
        if (BY_NATIVE_NAME.put(nativeName, option) != null) {
            throw new IllegalStateException("Duplicate native client option: " + nativeName);
        }
        REGISTRY.add(option);
        return option;
    }
}
