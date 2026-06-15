package io.github.endx.rustedfabricapi.api.event;

public final class HudCommandEvents {
    public static final RustedFabricEvent<BeforeWorldPointCommand> BEFORE_ISSUE_MOVE_COMMAND_AT_WORLD_POSITION =
            RustedFabricEvent.create(listeners -> (interfaceEngine, worldX, worldY, screenPoint) -> {
                boolean cancelled = false;
                for (BeforeWorldPointCommand listener : listeners) {
                    cancelled |= listener.beforeWorldPointCommand(interfaceEngine, worldX, worldY, screenPoint);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterWorldPointCommand> AFTER_ISSUE_MOVE_COMMAND_AT_WORLD_POSITION =
            RustedFabricEvent.create(listeners -> (interfaceEngine, worldX, worldY, screenPoint) -> {
                for (AfterWorldPointCommand listener : listeners) {
                    listener.afterWorldPointCommand(interfaceEngine, worldX, worldY, screenPoint);
                }
            });

    public static final RustedFabricEvent<BeforeWorldPointCommand> BEFORE_ISSUE_DEFAULT_MOVE_OR_ATTACK_MOVE =
            RustedFabricEvent.create(listeners -> (interfaceEngine, worldX, worldY, screenPoint) -> {
                boolean cancelled = false;
                for (BeforeWorldPointCommand listener : listeners) {
                    cancelled |= listener.beforeWorldPointCommand(interfaceEngine, worldX, worldY, screenPoint);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterWorldPointCommand> AFTER_ISSUE_DEFAULT_MOVE_OR_ATTACK_MOVE =
            RustedFabricEvent.create(listeners -> (interfaceEngine, worldX, worldY, screenPoint) -> {
                for (AfterWorldPointCommand listener : listeners) {
                    listener.afterWorldPointCommand(interfaceEngine, worldX, worldY, screenPoint);
                }
            });

    public static final RustedFabricEvent<BeforeWorldPointCommand> BEFORE_ISSUE_ATTACK_MOVE_AT_WORLD_POSITION =
            RustedFabricEvent.create(listeners -> (interfaceEngine, worldX, worldY, screenPoint) -> {
                boolean cancelled = false;
                for (BeforeWorldPointCommand listener : listeners) {
                    cancelled |= listener.beforeWorldPointCommand(interfaceEngine, worldX, worldY, screenPoint);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterWorldPointCommand> AFTER_ISSUE_ATTACK_MOVE_AT_WORLD_POSITION =
            RustedFabricEvent.create(listeners -> (interfaceEngine, worldX, worldY, screenPoint) -> {
                for (AfterWorldPointCommand listener : listeners) {
                    listener.afterWorldPointCommand(interfaceEngine, worldX, worldY, screenPoint);
                }
            });

    public static final RustedFabricEvent<BeforeWorldCommand> BEFORE_ISSUE_QUICK_RALLY_AT_WORLD_POSITION =
            RustedFabricEvent.create(listeners -> (interfaceEngine, worldX, worldY) -> {
                boolean cancelled = false;
                for (BeforeWorldCommand listener : listeners) {
                    cancelled |= listener.beforeWorldCommand(interfaceEngine, worldX, worldY);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterWorldCommand> AFTER_ISSUE_QUICK_RALLY_AT_WORLD_POSITION =
            RustedFabricEvent.create(listeners -> (interfaceEngine, worldX, worldY) -> {
                for (AfterWorldCommand listener : listeners) {
                    listener.afterWorldCommand(interfaceEngine, worldX, worldY);
                }
            });

    public static final RustedFabricEvent<BeforeSendMapPing> BEFORE_SEND_MAP_PING_AT_WORLD_POSITION =
            RustedFabricEvent.create(listeners -> (interfaceEngine, worldX, worldY, screenPoint, pingAction) -> {
                boolean cancelled = false;
                for (BeforeSendMapPing listener : listeners) {
                    cancelled |= listener.beforeSendMapPing(interfaceEngine, worldX, worldY, screenPoint, pingAction);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterSendMapPing> AFTER_SEND_MAP_PING_AT_WORLD_POSITION =
            RustedFabricEvent.create(listeners -> (interfaceEngine, worldX, worldY, screenPoint, pingAction) -> {
                for (AfterSendMapPing listener : listeners) {
                    listener.afterSendMapPing(interfaceEngine, worldX, worldY, screenPoint, pingAction);
                }
            });

    public static final RustedFabricEvent<BeforeShowMapPingEffect> BEFORE_SHOW_MAP_PING_EFFECT =
            RustedFabricEvent.create(listeners -> (interfaceEngine, worldX, worldY, team, pingAction) -> {
                boolean cancelled = false;
                for (BeforeShowMapPingEffect listener : listeners) {
                    cancelled |= listener.beforeShowMapPingEffect(interfaceEngine, worldX, worldY, team, pingAction);
                }
                return cancelled;
            });

    public static final RustedFabricEvent<AfterShowMapPingEffect> AFTER_SHOW_MAP_PING_EFFECT =
            RustedFabricEvent.create(listeners -> (interfaceEngine, worldX, worldY, team, pingAction) -> {
                for (AfterShowMapPingEffect listener : listeners) {
                    listener.afterShowMapPingEffect(interfaceEngine, worldX, worldY, team, pingAction);
                }
            });

    private HudCommandEvents() {
    }

    @FunctionalInterface
    public interface BeforeWorldPointCommand {
        boolean beforeWorldPointCommand(Object interfaceEngine, float worldX, float worldY, Object screenPoint);
    }

    @FunctionalInterface
    public interface AfterWorldPointCommand {
        void afterWorldPointCommand(Object interfaceEngine, float worldX, float worldY, Object screenPoint);
    }

    @FunctionalInterface
    public interface BeforeWorldCommand {
        boolean beforeWorldCommand(Object interfaceEngine, float worldX, float worldY);
    }

    @FunctionalInterface
    public interface AfterWorldCommand {
        void afterWorldCommand(Object interfaceEngine, float worldX, float worldY);
    }

    @FunctionalInterface
    public interface BeforeSendMapPing {
        boolean beforeSendMapPing(Object interfaceEngine, float worldX, float worldY, Object screenPoint,
                                  Object pingAction);
    }

    @FunctionalInterface
    public interface AfterSendMapPing {
        void afterSendMapPing(Object interfaceEngine, float worldX, float worldY, Object screenPoint,
                              Object pingAction);
    }

    @FunctionalInterface
    public interface BeforeShowMapPingEffect {
        boolean beforeShowMapPingEffect(Object interfaceEngine, float worldX, float worldY, Object team,
                                        Object pingAction);
    }

    @FunctionalInterface
    public interface AfterShowMapPingEffect {
        void afterShowMapPingEffect(Object interfaceEngine, float worldX, float worldY, Object team,
                                    Object pingAction);
    }
}
