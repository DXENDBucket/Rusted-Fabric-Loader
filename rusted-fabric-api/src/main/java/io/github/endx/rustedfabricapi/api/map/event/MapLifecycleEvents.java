package io.github.endx.rustedfabricapi.api.map.event;

import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import rustedwarfare.core.GameEngine;
import rustedwarfare.game.GameLoadMode;
import rustedwarfare.map.MapEngine;

import java.io.InputStream;

/** Strongly typed map-loading boundaries. Input-stream listeners must not consume or close it. */
public final class MapLifecycleEvents {
    public static final RustedFabricEvent<BeforeCurrentMapLoad> BEFORE_CURRENT_MAP_LOAD =
            RustedFabricEvent.create(listeners -> (engine, optionA, optionB, mode) -> {
                boolean cancelled = false;
                for (BeforeCurrentMapLoad listener : listeners) {
                    cancelled |= listener.beforeCurrentMapLoad(engine, optionA, optionB, mode);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<BeforeMapStreamOpen> BEFORE_MAP_STREAM_OPEN =
            RustedFabricEvent.create(listeners -> path -> {
                boolean cancelled = false;
                for (BeforeMapStreamOpen listener : listeners) {
                    cancelled |= listener.beforeMapStreamOpen(path);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<BeforeDocumentParse> BEFORE_TMX_DOCUMENT_PARSE =
            RustedFabricEvent.create(listeners -> (map, input, newGame) -> {
                boolean cancelled = false;
                for (BeforeDocumentParse listener : listeners) {
                    cancelled |= listener.beforeDocumentParse(map, input, newGame);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<MapParsePhase> AFTER_MAP_ATTRIBUTES_READ = parsePhase();
    public static final RustedFabricEvent<MapParsePhase> AFTER_TILESETS_LOADED = parsePhase();
    public static final RustedFabricEvent<MapParsePhase> AFTER_MAP_LAYERS_LOADED = parsePhase();

    public static final RustedFabricEvent<AfterObjectGroupsLoaded> AFTER_MAP_OBJECT_GROUPS_LOADED =
            RustedFabricEvent.create(listeners -> map -> {
                for (AfterObjectGroupsLoaded listener : listeners) {
                    listener.afterObjectGroupsLoaded(map);
                }
            });

    public static final RustedFabricEvent<AfterMapLoaded> AFTER_CURRENT_MAP_LOADED_BEFORE_STARTING_UNITS =
            RustedFabricEvent.create(listeners -> (engine, map, optionA, optionB, mode) -> {
                for (AfterMapLoaded listener : listeners) {
                    listener.afterMapLoaded(engine, map, optionA, optionB, mode);
                }
            });

    public static final RustedFabricEvent<AfterMapStarted> AFTER_CURRENT_MAP_STARTED =
            RustedFabricEvent.create(listeners -> (engine, optionA, optionB, mode) -> {
                for (AfterMapStarted listener : listeners) {
                    listener.afterMapStarted(engine, optionA, optionB, mode);
                }
            });

    private MapLifecycleEvents() {
    }

    private static RustedFabricEvent<MapParsePhase> parsePhase() {
        return RustedFabricEvent.create(listeners -> (map, input, newGame) -> {
            for (MapParsePhase listener : listeners) {
                listener.afterPhase(map, input, newGame);
            }
        });
    }

    @FunctionalInterface
    public interface BeforeCurrentMapLoad {
        boolean beforeCurrentMapLoad(GameEngine engine, boolean optionA, boolean optionB,
                                     GameLoadMode mode);
    }

    @FunctionalInterface
    public interface BeforeMapStreamOpen {
        boolean beforeMapStreamOpen(String mapPath);
    }

    @FunctionalInterface
    public interface BeforeDocumentParse {
        boolean beforeDocumentParse(MapEngine map, InputStream input, boolean newGame);
    }

    @FunctionalInterface
    public interface MapParsePhase {
        void afterPhase(MapEngine map, InputStream input, boolean newGame);
    }

    @FunctionalInterface
    public interface AfterObjectGroupsLoaded {
        void afterObjectGroupsLoaded(MapEngine map);
    }

    @FunctionalInterface
    public interface AfterMapLoaded {
        void afterMapLoaded(GameEngine engine, MapEngine map, boolean optionA, boolean optionB,
                            GameLoadMode mode);
    }

    @FunctionalInterface
    public interface AfterMapStarted {
        void afterMapStarted(GameEngine engine, boolean optionA, boolean optionB, GameLoadMode mode);
    }
}
