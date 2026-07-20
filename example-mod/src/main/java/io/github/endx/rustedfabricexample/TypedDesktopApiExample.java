package io.github.endx.rustedfabricexample;

import io.github.endx.rustedfabricapi.api.audio.SoundEvents;
import io.github.endx.rustedfabricapi.api.asset.ModResources;
import io.github.endx.rustedfabricapi.api.asset.ModResourcePack;
import io.github.endx.rustedfabricapi.api.asset.reload.ModResourceReloader;
import io.github.endx.rustedfabricapi.api.asset.reload.ModResourceReloadEvents;
import io.github.endx.rustedfabricapi.api.asset.reload.ModResourceReloaders;
import io.github.endx.rustedfabricapi.api.chat.ChatEvents;
import io.github.endx.rustedfabricapi.api.chat.command.ChatCommands;
import io.github.endx.rustedfabricapi.api.client.Camera;
import io.github.endx.rustedfabricapi.api.client.RustedWarfareClient;
import io.github.endx.rustedfabricapi.api.client.Selection;
import io.github.endx.rustedfabricapi.api.client.event.ClientTickEvents;
import io.github.endx.rustedfabricapi.api.client.event.ClientRenderEvents;
import io.github.endx.rustedfabricapi.api.client.event.HudRenderEvents;
import io.github.endx.rustedfabricapi.api.client.event.WorldRenderEvents;
import io.github.endx.rustedfabricapi.api.client.event.ClientLifecycleEvents;
import io.github.endx.rustedfabricapi.api.client.event.SelectionEvents;
import io.github.endx.rustedfabricapi.api.client.input.KeyBindingEvents;
import io.github.endx.rustedfabricapi.api.client.input.ClientInputEvents;
import io.github.endx.rustedfabricapi.api.client.input.KeyBindings;
import io.github.endx.rustedfabricapi.api.client.input.ModKeyBinding;
import io.github.endx.rustedfabricapi.api.client.message.MessageEvents;
import io.github.endx.rustedfabricapi.api.client.option.ClientOptions;
import io.github.endx.rustedfabricapi.api.client.option.event.ClientOptionEvents;
import io.github.endx.rustedfabricapi.api.client.render.ArgbColor;
import io.github.endx.rustedfabricapi.api.client.render.DrawStyle;
import io.github.endx.rustedfabricapi.api.client.screen.ScreenEvents;
import io.github.endx.rustedfabricapi.api.client.screen.dialog.ClientDialogs;
import io.github.endx.rustedfabricapi.api.client.screen.dialog.DialogSpec;
import io.github.endx.rustedfabricapi.api.client.minimap.MinimapEvents;
import io.github.endx.rustedfabricapi.api.client.warlog.WarLogEvents;
import io.github.endx.rustedfabricapi.api.command.event.CommandEvents;
import io.github.endx.rustedfabricapi.api.config.ModConfigFile;
import io.github.endx.rustedfabricapi.api.config.ModConfigFiles;
import io.github.endx.rustedfabricapi.api.custom.CustomUnits;
import io.github.endx.rustedfabricapi.api.custom.action.event.CustomActionEffectEvents;
import io.github.endx.rustedfabricapi.api.custom.attachment.event.AttachmentEvents;
import io.github.endx.rustedfabricapi.api.custom.event.CustomUnitLifecycleEvents;
import io.github.endx.rustedfabricapi.api.custom.event.CustomUnitRegistryEvents;
import io.github.endx.rustedfabricapi.api.effect.event.EffectEvents;
import io.github.endx.rustedfabricapi.api.custom.event.CustomUnitTriggerEvents;
import io.github.endx.rustedfabricapi.api.data.PersistentData;
import io.github.endx.rustedfabricapi.api.data.PersistentDataCodec;
import io.github.endx.rustedfabricapi.api.data.PersistentDataKey;
import io.github.endx.rustedfabricapi.api.data.event.PersistentDataEvents;
import io.github.endx.rustedfabricapi.api.map.event.MapLifecycleEvents;
import io.github.endx.rustedfabricapi.api.map.event.MapObjectEvents;
import io.github.endx.rustedfabricapi.api.map.MapTiles;
import io.github.endx.rustedfabricapi.api.lobby.LobbyGameSetupEvents;
import io.github.endx.rustedfabricapi.api.lobby.LobbyPlayerEvents;
import io.github.endx.rustedfabricapi.api.mission.Missions;
import io.github.endx.rustedfabricapi.api.mission.event.MissionTriggerEvents;
import io.github.endx.rustedfabricapi.api.networking.ChannelId;
import io.github.endx.rustedfabricapi.api.networking.ClientNetworking;
import io.github.endx.rustedfabricapi.api.networking.PacketCodecs;
import io.github.endx.rustedfabricapi.api.networking.PacketCodec;
import io.github.endx.rustedfabricapi.api.networking.ServerNetworking;
import io.github.endx.rustedfabricapi.api.networking.event.ConnectionEvents;
import io.github.endx.rustedfabricapi.api.path.event.PathEvents;
import io.github.endx.rustedfabricapi.api.projectile.event.ProjectileEvents;
import io.github.endx.rustedfabricapi.api.registry.ModRegistries;
import io.github.endx.rustedfabricapi.api.registry.ModRegistry;
import io.github.endx.rustedfabricapi.api.registry.RegistryKey;
import io.github.endx.rustedfabricapi.api.registry.RegistryCodecs;
import io.github.endx.rustedfabricapi.api.registry.tag.RegistryTagJsonReloader;
import io.github.endx.rustedfabricapi.api.registry.tag.RegistryTagKey;
import io.github.endx.rustedfabricapi.api.registry.tag.RegistryTagReloaders;
import io.github.endx.rustedfabricapi.api.scheduler.GameTaskScope;
import io.github.endx.rustedfabricapi.api.scheduler.GameTickScheduler;
import io.github.endx.rustedfabricapi.api.resource.Resources;
import io.github.endx.rustedfabricapi.api.save.event.SaveEvents;
import io.github.endx.rustedfabricapi.api.replay.event.ReplayEvents;
import io.github.endx.rustedfabricapi.api.stats.event.StatisticsEvents;
import io.github.endx.rustedfabricapi.api.text.LanguageEvents;
import io.github.endx.rustedfabricapi.api.text.Translations;
import io.github.endx.rustedfabricapi.api.unit.Units;
import io.github.endx.rustedfabricapi.api.unit.Teams;
import io.github.endx.rustedfabricapi.api.unit.action.UnitActions;
import io.github.endx.rustedfabricapi.api.unit.action.JavaUnitAction;
import io.github.endx.rustedfabricapi.api.unit.action.JavaUnitActions;
import io.github.endx.rustedfabricapi.api.unit.event.UnitEvents;
import io.github.endx.rustedfabricapi.api.unit.event.UnitDamageEvents;
import io.github.endx.rustedfabricapi.api.unit.event.UnitSpawnEvents;
import io.github.endx.rustedfabricapi.api.unit.event.UnitTeamEvents;
import io.github.endx.rustedfabricapi.api.unit.event.TeamStateEvents;
import io.github.endx.rustedfabricapi.api.unit.type.UnitTypes;
import io.github.endx.rustedfabricapi.api.unit.type.event.UnitTypeEvents;
import io.github.endx.rustedfabricapi.api.unit.build.event.BuildQueueEvents;
import io.github.endx.rustedfabricapi.api.unit.combat.CombatUnits;
import io.github.endx.rustedfabricapi.api.unit.combat.event.CombatEvents;
import io.github.endx.rustedfabricapi.api.unit.order.UnitOrders;
import io.github.endx.rustedfabricapi.api.unit.repair.event.RepairReclaimEvents;
import io.github.endx.rustedfabricapi.api.unit.status.StatusEffects;
import io.github.endx.rustedfabricapi.api.unit.status.event.StatusEffectEvents;
import io.github.endx.rustedfabricapi.api.unit.attribute.CustomUnitStats;
import io.github.endx.rustedfabricapi.api.unit.attribute.UnitStat;
import io.github.endx.rustedfabricapi.api.unit.attribute.UnitVitals;
import io.github.endx.rustedfabricapi.api.unit.transport.TransportUnits;
import io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents;
import io.github.endx.rustedfabricapi.api.unit.tag.UnitTags;
import io.github.endx.rustedfabricapi.api.unit.tag.event.UnitTagEvents;
import io.github.endx.rustedfabricapi.api.world.GameWorld;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import io.github.endx.rustedfabricapi.api.util.Identifier;
import rustedwarfare.unit.OrderableUnit;
import rustedwarfare.custom.CustomUnit;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Properties;

/** Small compile-time example of the mapped, desktop-only API surface. */
final class TypedDesktopApiExample {
    private static final ChannelId STATUS_CHANNEL = ChannelId.of("rustedfabricexample", "status");
    private static final ChannelId MODE_CHANNEL = ChannelId.of("rustedfabricexample", "mode");
    private static final RegistryKey<String> EXAMPLE_MODE_KEY = RegistryKey.of(
            "rustedfabricexample:modes", String.class);
    private static final ModRegistry<String> EXAMPLE_MODES =
            ModRegistries.create(EXAMPLE_MODE_KEY);
    private static final PacketCodec<String> EXAMPLE_MODE_CODEC =
            RegistryCodecs.value(EXAMPLE_MODES);
    private static final RegistryTagKey<String> INTERACTIVE_MODES = RegistryTagKey.of(
            EXAMPLE_MODE_KEY, "rustedfabricexample:interactive");
    private static final ModConfigFile CONFIG = ModConfigFiles.file(
            "rustedfabricexample", "example.properties");
    private static final ModKeyBinding INSPECT_SELECTION = KeyBindings.register(
            "rustedfabricexample:inspect_selection", "Inspect selected units",
            "Rusted Fabric Example", "CTRL+K");
    private static final JavaUnitAction REPORT_STATUS = JavaUnitAction.builder(
            "rustedfabricexample:report_status", "Report status",
            "Writes this tank's current status to the Loader log",
            context -> ExampleMod.log("typed API: Java action unit=" + context.unit()
                    + ", queued=" + context.queued()))
            .availableWhen(unit -> !unit.dead)
            .displayPriority(20.0F)
            .build();
    private static final JavaUnitAction MARK_POSITION = JavaUnitAction.builder(
            "rustedfabricexample:mark_position", "Mark position",
            "Selects a synchronized point on the map",
            context -> context.targetPoint().ifPresent(point -> ExampleMod.log(
                    "typed API: Java point action unit=" + context.unit()
                            + ", point=" + point)))
            .targetPointWhen((unit, point) -> point.x() >= 0.0F && point.y() >= 0.0F)
            .displayPriority(21.0F)
            .build();
    private static final JavaUnitAction PAID_SIGNAL = JavaUnitAction.builder(
            "rustedfabricexample:paid_signal", "Paid signal",
            "Spends 5 credits for each tank that executes this action",
            context -> ExampleMod.log("typed API: paid Java action unit=" + context.unit()))
            .textForUnit(unit -> "Paid signal (" + Math.round(unit.hp) + " HP)")
            .descriptionForUnit(unit -> unit.dead
                    ? "This unit can no longer send a signal"
                    : "Spends 5 credits for this unit")
            .creditCost(5)
            .cooldownMillis(3_000)
            .displayPriority(22.0F)
            .build();
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final AtomicBoolean FIRST_STARTED_TICK = new AtomicBoolean();
    private static final DrawStyle HUD_TEXT = DrawStyle.text(ArgbColor.WHITE, 14.0F);
    private static final DrawStyle HUD_BACKGROUND = DrawStyle.fill(
            ArgbColor.argb(150, 0, 0, 0));
    private static final DrawStyle WORLD_CENTER_RING = DrawStyle.stroke(
            ArgbColor.argb(180, 90, 210, 255), 2.0F);
    private static final AtomicBoolean FIRST_UNIT = new AtomicBoolean();
    private static final AtomicBoolean FIRST_COMMAND = new AtomicBoolean();
    private static final AtomicBoolean FIRST_PROJECTILE = new AtomicBoolean();
    private static final AtomicBoolean FIRST_DAMAGE = new AtomicBoolean();
    private static final AtomicBoolean FIRST_SELECTION = new AtomicBoolean();
    private static final AtomicBoolean FIRST_MAP = new AtomicBoolean();
    private static final AtomicBoolean FIRST_RENDER = new AtomicBoolean();
    private static final AtomicBoolean FIRST_SCREEN = new AtomicBoolean();
    private static final AtomicBoolean FIRST_STARTING_SPAWN = new AtomicBoolean();
    private static final AtomicBoolean FIRST_CUSTOM_REGISTRY = new AtomicBoolean();
    private static final AtomicBoolean FIRST_CUSTOM_METADATA = new AtomicBoolean();
    private static final AtomicBoolean FIRST_BUILD_QUEUE = new AtomicBoolean();
    private static final AtomicBoolean FIRST_TRANSPORT = new AtomicBoolean();
    private static final AtomicBoolean FIRST_CUSTOM_EFFECT = new AtomicBoolean();
    private static final AtomicBoolean FIRST_TURRET_FIRE = new AtomicBoolean();
    private static final AtomicBoolean FIRST_SOUND = new AtomicBoolean();
    private static final AtomicBoolean FIRST_EFFECT = new AtomicBoolean();
    private static final AtomicBoolean FIRST_MESSAGE = new AtomicBoolean();
    private static final AtomicBoolean FIRST_CHAT = new AtomicBoolean();
    private static final AtomicBoolean FIRST_LOBBY_SETUP = new AtomicBoolean();
    private static final AtomicBoolean FIRST_AI_ADD = new AtomicBoolean();
    private static final AtomicBoolean FIRST_TEAM_OUTCOME = new AtomicBoolean();
    private static final AtomicBoolean FIRST_OPTION_CHANGE = new AtomicBoolean();
    private static final AtomicBoolean FIRST_POINTER_INPUT = new AtomicBoolean();
    private static final AtomicBoolean FIRST_MISSION_TRIGGER = new AtomicBoolean();
    private static final AtomicBoolean FIRST_SAVE = new AtomicBoolean();
    private static final AtomicBoolean FIRST_REPLAY = new AtomicBoolean();
    private static final AtomicBoolean FIRST_STAT_KILL = new AtomicBoolean();
    private static final AtomicBoolean FIRST_WAR_LOG = new AtomicBoolean();
    private static final AtomicBoolean FIRST_MINIMAP_MARKER = new AtomicBoolean();
    private static final AtomicBoolean FIRST_TAG_CHANGE = new AtomicBoolean();
    private static final AtomicBoolean FIRST_ATTACHMENT = new AtomicBoolean();
    private static final AtomicBoolean FIRST_CUSTOM_TRIGGER = new AtomicBoolean();
    private static final AtomicBoolean FIRST_REPAIR_RECLAIM = new AtomicBoolean();
    private static final AtomicBoolean FIRST_STATUS_EFFECT = new AtomicBoolean();
    private static final AtomicBoolean FIRST_PATH = new AtomicBoolean();
    private static final PersistentDataKey<Integer> START_COUNT = PersistentData.register(
            Identifier.of("rustedfabricexample", "start_count"), 1,
            PersistentDataCodec.of(PacketCodecs.VAR_INT));

    private TypedDesktopApiExample() {
    }

    static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }

        ModResourcePack resources = ModResources.forMod("rusted_fabric_example");
        Translations.register("rustedfabricexample", resources);
        JavaUnitActions.attach("tank", REPORT_STATUS);
        JavaUnitActions.attach("tank", MARK_POSITION);
        JavaUnitActions.attach("tank", PAID_SIGNAL);
        ModResourceReloaders.register("rustedfabricexample:settings", resources,
                new ModResourceReloader<Properties>() {
                    @Override public Properties prepare(ModResourcePack pack) throws Exception {
                        return pack.resource("assets/rustedfabricexample/data/example.properties")
                                .readPropertiesUtf8();
                    }

                    @Override public void apply(Properties prepared) {
                        ExampleMod.log("typed API: reloaded example resource, greeting="
                                + prepared.getProperty("greeting"));
                    }
                });
        ModResourceReloadEvents.AFTER_RELOAD.register(report -> {
            if (!report.successful()) {
                ExampleMod.log("typed API: resource reload failures=" + report.results());
            }
        });
        EXAMPLE_MODES.events().AFTER_ENTRY_ADDED.register((registry, entry) ->
                ExampleMod.log("typed API: registered example mode " + entry.id()
                        + " as raw ID " + entry.rawId()));
        EXAMPLE_MODES.register("rustedfabricexample:status", "status");
        EXAMPLE_MODES.register("rustedfabricexample:inspection", "inspection");
        EXAMPLE_MODES.freeze();
        EXAMPLE_MODES.tags().events().AFTER_APPLY.register((contributor, tags) ->
                EXAMPLE_MODES.tags().get(INTERACTIVE_MODES).ifPresent(tag ->
                        ExampleMod.log("typed API: interactive mode tag=" + tag.ids())));
        RegistryTagJsonReloader<String> modeTags = RegistryTagReloaders.json(
                EXAMPLE_MODES, "rustedfabricexample:mode_tags", "rustedfabricexample");
        ModResourceReloaders.register("rustedfabricexample:mode_tags", resources, modeTags,
                "rustedfabricexample:settings");
        GameTickScheduler.schedule("rustedfabricexample:ready_tick", 1,
                GameTaskScope.MAP, () -> System.out.println(
                        "[Rusted Fabric Example] first scheduled simulation tick"));
        ExampleMod.log("typed API: example registry layout="
                + EXAMPLE_MODES.snapshot().layoutFingerprint());
        LanguageEvents.AFTER_RELOAD.register(language -> ExampleMod.log(
                Translations.translate("rustedfabricexample:language_reloaded", language)));
        ExampleMod.log(Translations.translate(
                "rustedfabricexample:loader_ready", "Rusted Fabric Example"));

        ChatCommands.register("rustedfabricexample:status", context -> {
            context.reply("Rusted Fabric example: tick=" + GameWorld.tick()
                    + ", aliveUnits=" + Units.alive().size()
                    + ", config=" + CONFIG.path());
            return 1;
        });
        ChatEvents.AFTER_RECEIVED.register((network, connection, teamId, sender, message) -> {
            if (FIRST_CHAT.compareAndSet(false, true)) {
                ExampleMod.log("typed API: first received chat sender=" + sender
                        + ", team=" + teamId + ", text=" + message);
            }
        });
        LobbyGameSetupEvents.AFTER_NATIVE_APPLY.register((network, requested) -> {
            if (FIRST_LOBBY_SETUP.compareAndSet(false, true)) {
                ExampleMod.log("typed API: lobby setup applied=" + requested);
            }
        });
        LobbyPlayerEvents.AFTER_ADD_AI.register(network -> {
            if (FIRST_AI_ADD.compareAndSet(false, true)) {
                ExampleMod.log("typed API: AI player added");
            }
        });
        TeamStateEvents.OUTCOME_ANNOUNCED.register((network, team, outcome) -> {
            if (FIRST_TEAM_OUTCOME.compareAndSet(false, true)) {
                ExampleMod.log("typed API: team outcome=" + outcome
                        + ", team=" + Teams.snapshotState(team));
            }
        });
        ClientOptionEvents.AFTER_NATIVE_DYNAMIC_CHANGE.register((settings, change) -> {
            if (FIRST_OPTION_CHANGE.compareAndSet(false, true)) {
                ExampleMod.log("typed API: client option changed=" + change);
            }
        });
        HudRenderEvents.AFTER_HUD.register((gameInterface, context) ->
                context.drawTextWithBackground("Rusted Fabric API example",
                        8.0F, context.height() - 10.0F,
                        HUD_TEXT, HUD_BACKGROUND, 4.0F));
        WorldRenderEvents.AFTER_WORLD.register(context -> {
            WorldPoint center = context.viewport().center();
            context.drawCircle(center.x(), center.y(), 20.0F, WORLD_CENTER_RING);
        });

        ClientTickEvents.END_CLIENT_TICK.register(engine -> {
            if (engine != null && engine.isGameStarted
                    && FIRST_STARTED_TICK.compareAndSet(false, true)) {
                ExampleMod.log("typed API: game started, units=" + Units.snapshot().size()
                        + ", unitTypes=" + UnitTypes.all().size()
                        + ", customTypes=" + CustomUnits.activeTypes().size()
                        + ", displayResources=" + Resources.activeDisplayResources().size()
                        + ", teams=" + Teams.snapshotStates(true).size()
                        + ", showFps=" + ClientOptions.get(ClientOptions.SHOW_FPS)
                        + ", clientOptions=" + ClientOptions.snapshot().size()
                        + ", localTeam=" + RustedWarfareClient.getPlayerTeam()
                        + ", tick=" + GameWorld.tick()
                        + ", camera=" + Camera.snapshot()
                        + ", mission=" + Missions.snapshot());
                int starts = PersistentData.getGlobal(START_COUNT).orElse(Integer.valueOf(0));
                PersistentData.setGlobal(START_COUNT, Integer.valueOf(starts + 1));
                ExampleMod.log("typed API: persistent start count=" + (starts + 1));
                Units.alive().stream().findFirst().ifPresent(unit -> {
                    int turretCount = unit instanceof OrderableUnit
                            ? CombatUnits.turretCount((OrderableUnit) unit) : 0;
                    int orderCount = unit instanceof OrderableUnit
                            ? UnitOrders.size((OrderableUnit) unit) : 0;
                    ExampleMod.log("typed API: first live unit actions="
                            + UnitActions.forUnit(unit).size()
                            + ", turrets=" + turretCount
                            + ", orders=" + orderCount
                            + ", statusEffects=" + (unit instanceof OrderableUnit
                                    ? StatusEffects.count((OrderableUnit) unit) : 0)
                            + ", tags=" + UnitTags.names(UnitTags.runtime(unit)).size()
                            + ", transportedBy=" + TransportUnits.containingUnit(unit).orElse(null));
                });
            }
        });

        ClientLifecycleEvents.AFTER_ENGINE_INITIALIZATION.register(engine ->
                ExampleMod.log("typed API: engine initialized=" + engine.getClass().getName()));
        ScreenEvents.OPENED.register(document -> {
            if (FIRST_SCREEN.compareAndSet(false, true)) {
                ExampleMod.log("typed API: first screen kind=" + document.kind()
                        + ", path=" + document.path());
            }
        });

        KeyBindingEvents.PRESSED.register(binding -> {
            if (binding == INSPECT_SELECTION) {
                ExampleMod.log("typed API: inspect key pressed, selected="
                        + Selection.snapshot().size());
                ClientDialogs.show(DialogSpec.builder("Selection inspection",
                                "Selected units: " + Selection.snapshot().size())
                                .primaryButton("OK")
                                .secondaryButton("Dismiss")
                                .build(),
                        result -> ExampleMod.log("typed API: dialog result=" + result));
            }
        });
        ClientInputEvents.MOUSE_PRESSED.register(input -> {
            if (input.insideWorldViewport()
                    && FIRST_POINTER_INPUT.compareAndSet(false, true)) {
                ExampleMod.log("typed API: first world pointer="
                        + input.worldPosition().orElse(null)
                        + ", button=" + input.button()
                        + ", modifiers=" + input.modifiers());
            }
        });

        SoundEvents.AFTER_PLAY.register((engine, playback) -> {
            if (FIRST_SOUND.compareAndSet(false, true)) {
                ExampleMod.log("typed API: first sound=" + playback.sound().name
                        + ", scope=" + playback.scope());
            }
        });

        EffectEvents.AFTER_LIGHT.register((engine, effect, x, y, height, color) -> {
            if (FIRST_EFFECT.compareAndSet(false, true)) {
                ExampleMod.log("typed API: first light effect=" + effect
                        + " at=" + x + "," + y);
            }
        });

        MessageEvents.AFTER_ADD.register((history, sender, message, line) -> {
            if (FIRST_MESSAGE.compareAndSet(false, true)) {
                ExampleMod.log("typed API: first local message sender=" + sender
                        + ", text=" + message);
            }
        });

        MissionTriggerEvents.AFTER_ACTIVATE.register((engine, trigger) -> {
            if (FIRST_MISSION_TRIGGER.compareAndSet(false, true)) {
                ExampleMod.log("typed API: first mission trigger id=" + trigger.id
                        + ", name=" + trigger.name);
            }
        });

        SaveEvents.AFTER_SAVE.register((manager, name, automatic) -> {
            if (FIRST_SAVE.compareAndSet(false, true)) {
                ExampleMod.log("typed API: save completed name=" + name
                        + ", automatic=" + automatic);
            }
        });

        ReplayEvents.AFTER_RECORD.register((manager, name, success) -> {
            if (FIRST_REPLAY.compareAndSet(false, true)) {
                ExampleMod.log("typed API: replay recording name=" + name + ", success=" + success);
            }
        });

        StatisticsEvents.AFTER_UNIT_KILLED.register((dispatcher, killed, attacker) -> {
            if (FIRST_STAT_KILL.compareAndSet(false, true)) {
                ExampleMod.log("typed API: first recorded kill unit=" + killed + ", attacker=" + attacker);
            }
        });

        PathEvents.SOLVED.register((engine, request, result) -> {
            if (FIRST_PATH.compareAndSet(false, true)) {
                ExampleMod.log("typed API: first solved path success=" + result.successful()
                        + ", steps=" + result.steps().size()
                        + ", nativeSolveTime=" + result.elapsedSolveTime());
            }
        });

        WarLogEvents.AFTER_TEXT.register((log, text, durationMillis) -> {
            if (FIRST_WAR_LOG.compareAndSet(false, true)) {
                ExampleMod.log("typed API: first war-log text=" + text
                        + ", duration=" + durationMillis);
            }
        });

        MinimapEvents.AFTER_MARKER.register((minimap, x, y, kind) -> {
            if (FIRST_MINIMAP_MARKER.compareAndSet(false, true)) {
                ExampleMod.log("typed API: first minimap marker=" + kind + " at=" + x + "," + y);
            }
        });

        UnitEvents.AFTER_REGISTER.register(unit -> {
            if (FIRST_UNIT.compareAndSet(false, true)) {
                ExampleMod.log("typed API: first registered unit hp=" + unit.hp
                        + "/" + UnitVitals.snapshot(unit).maxHealth() + ", team=" + unit.team
                        + (unit instanceof CustomUnit
                                ? ", moveSpeed=" + CustomUnitStats.get(
                                        (CustomUnit) unit, UnitStat.MOVE_SPEED)
                                : ""));
            }
        });

        UnitSpawnEvents.AFTER_SPAWN.register((unit, type, team) ->
                ExampleMod.log("typed API: API-spawned unit=" + type.getInternalName()
                        + " at=" + unit.x + "," + unit.y));
        UnitTeamEvents.AFTER_CHANGE.register((unit, newTeam) ->
                ExampleMod.log("typed API: unit owner changed unit=" + unit.id
                        + ", team=" + newTeam));

        CommandEvents.AFTER_ISSUE.register(command -> {
            if (FIRST_COMMAND.compareAndSet(false, true)) {
                ExampleMod.log("typed API: first issued command team=" + command.getTeam()
                        + ", units=" + command.getSelectedUnitReferenceCount());
            }
        });

        ProjectileEvents.AFTER_CREATED.register((projectile, source) -> {
            if (FIRST_PROJECTILE.compareAndSet(false, true)) {
                ExampleMod.log("typed API: first projectile source=" + source
                        + ", target=" + projectile.targetUnit);
            }
        });

        UnitDamageEvents.AFTER_DAMAGE.register((unit, attacker, requested, projectile, applied) -> {
            if (FIRST_DAMAGE.compareAndSet(false, true)) {
                ExampleMod.log("typed API: first damage requested=" + requested
                        + ", applied=" + applied + ", remainingHp=" + unit.hp);
            }
        });

        SelectionEvents.AFTER_ADD.register((gameInterface, unit, added) -> {
            if (added && FIRST_SELECTION.compareAndSet(false, true)) {
                ExampleMod.log("typed API: first selected unit=" + unit
                        + ", selectionSize=" + Selection.snapshot().size());
            }
        });

        MapLifecycleEvents.AFTER_CURRENT_MAP_STARTED.register((engine, optionA, optionB, mode) -> {
            if (FIRST_MAP.compareAndSet(false, true)) {
                ExampleMod.log("typed API: map started path=" + engine.getCurrentMapPath()
                        + ", mode=" + mode);
            }
        });
        MapObjectEvents.AFTER_LOAD.register(catalog -> ExampleMod.log(
                "typed API: TMX object groups=" + catalog.groupCount()
                        + ", objects=" + catalog.objectCount()
                        + ", tileLayers=" + MapTiles.layers().size()));

        ClientRenderEvents.END_CLIENT_RENDER.register((engine, graphics) -> {
            if (engine != null && graphics != null && FIRST_RENDER.compareAndSet(false, true)) {
                ExampleMod.log("typed API: first render graphics=" + graphics.getClass().getName());
            }
        });

        UnitTypeEvents.AFTER_STARTING_SPAWN.register(
                (type, x, y, direction, height, team, result) -> {
                    if (FIRST_STARTING_SPAWN.compareAndSet(false, true)) {
                        ExampleMod.log("typed API: starting spawn type="
                                + (type != null ? type.getInternalName() : "null")
                                + ", result=" + result);
                    }
                    return result;
                });

        CustomUnitRegistryEvents.AFTER_ACTION_LINKS_BUILT.register(activeTypes -> {
            if (FIRST_CUSTOM_REGISTRY.compareAndSet(false, true)) {
                ExampleMod.log("typed API: custom registry linked types=" + activeTypes.size());
            }
        });

        CustomUnitLifecycleEvents.AFTER_METADATA_APPLY.register(
                (unit, oldMetadata, newMetadata, conversion, initial, overrides) -> {
                    if (FIRST_CUSTOM_METADATA.compareAndSet(false, true)) {
                        ExampleMod.log("typed API: custom metadata applied type="
                                + (newMetadata != null ? newMetadata.getInternalName() : "null")
                                + ", conversion=" + conversion + ", initial=" + initial);
                    }
                });

        BuildQueueEvents.AFTER_ACTION_APPLY.register(
                (queue, action, front, targetX, targetY, hasTargetPoint, target, item) -> {
                    if (FIRST_BUILD_QUEUE.compareAndSet(false, true)) {
                        ExampleMod.log("typed API: first queue action="
                                + (action != null ? action.getActionIdString() : "null")
                                + ", accepted=" + (item != null));
                    }
                });

        TransportEvents.AFTER_TRY_LOAD.register((carrier, cargo, allowPartial, result) -> {
            if (FIRST_TRANSPORT.compareAndSet(false, true)) {
                ExampleMod.log("typed API: first transport attempt carrier=" + carrier
                        + ", cargo=" + cargo + ", accepted=" + result);
            }
        });

        CustomActionEffectEvents.AFTER_EXECUTE.register(
                (effect, actor, action, targetX, targetY, hasTargetPoint,
                 target, recursionDepth, result) -> {
                    if (FIRST_CUSTOM_EFFECT.compareAndSet(false, true)) {
                        ExampleMod.log("typed API: first custom effect="
                                + effect.getClass().getSimpleName()
                                + ", depth=" + recursionDepth + ", result=" + result);
                    }
                });

        CombatEvents.AFTER_TRY_FIRE.register((attacker, delta, target, turretIndex, fired) -> {
            if (fired && FIRST_TURRET_FIRE.compareAndSet(false, true)) {
                ExampleMod.log("typed API: first turret fire attacker=" + attacker
                        + ", target=" + target + ", turret=" + turretIndex);
            }
        });

        RepairReclaimEvents.AFTER_ORDER_UPDATE.register((unit, delta, order) -> {
            if (FIRST_REPAIR_RECLAIM.compareAndSet(false, true)) {
                ExampleMod.log("typed API: first repair/reclaim update unit=" + unit
                        + ", order=" + order.getOrderType()
                        + ", target=" + order.getTargetUnit());
            }
        });

        StatusEffectEvents.AFTER_ADD.register((unit, effect, added) -> {
            if (added && FIRST_STATUS_EFFECT.compareAndSet(false, true)) {
                ExampleMod.log("typed API: first status effect unit=" + unit
                        + ", kind=" + StatusEffects.kindOf(effect)
                        + ", expiresAt=" + effect.getExpireFrame());
            }
        });
        StatusEffectEvents.EXPIRED.register((unit, effect) ->
                ExampleMod.log("typed API: status effect expired unit=" + unit
                        + ", kind=" + StatusEffects.kindOf(effect)));

        PersistentDataEvents.AFTER_READ.register((formatVersion, entries) ->
                ExampleMod.log("typed API: persistent block restored format="
                        + formatVersion + ", entries=" + entries));

        UnitTagEvents.AFTER_SET.register((unit, tags, skipTeamIndexRefresh) -> {
            if (FIRST_TAG_CHANGE.compareAndSet(false, true)) {
                ExampleMod.log("typed API: first tag change unit=" + unit
                        + ", tags=" + UnitTags.names(tags));
            }
        });

        AttachmentEvents.AFTER_ATTACH.register((parent, child, slot, attached) -> {
            if (FIRST_ATTACHMENT.compareAndSet(false, true)) {
                ExampleMod.log("typed API: first attachment parent=" + parent
                        + ", child=" + child + ", slot="
                        + (slot != null ? slot.getName() : "null") + ", attached=" + attached);
            }
        });

        CustomUnitTriggerEvents.AFTER_TRIGGER.register((unit, eventType) -> {
            if (FIRST_CUSTOM_TRIGGER.compareAndSet(false, true)) {
                ExampleMod.log("typed API: first custom trigger unit=" + unit
                        + ", event=" + eventType);
            }
        });

        ClientNetworking.registerGlobalReceiver(STATUS_CHANNEL, PacketCodecs.UTF8,
                (engine, connection, channel, message) ->
                        ExampleMod.log("typed API: client payload " + channel
                                + " message=" + message));
        ServerNetworking.registerGlobalReceiver(STATUS_CHANNEL, PacketCodecs.UTF8,
                (engine, sender, channel, message) ->
                        ExampleMod.log("typed API: server payload " + channel
                                + " sender=" + sender.getDisplayName()
                                + " message=" + message));
        ClientNetworking.registerGlobalReceiver(MODE_CHANNEL, EXAMPLE_MODE_CODEC,
                (engine, connection, channel, mode) ->
                        ExampleMod.log("typed API: client registry mode=" + mode));
        ServerNetworking.registerGlobalReceiver(MODE_CHANNEL, EXAMPLE_MODE_CODEC,
                (engine, sender, channel, mode) ->
                        ExampleMod.log("typed API: server registry mode=" + mode
                                + ", sender=" + sender.getDisplayName()));

        ConnectionEvents.SERVER_PLAYER_REGISTERED.register((connection, playerName, playerId) ->
                ExampleMod.log("typed API: player registered name=" + playerName
                        + ", loaderPeer=" + ServerNetworking.canSend(connection)));
        ConnectionEvents.CONNECTION_REMOVED.register((engine, connection) ->
                ExampleMod.log("typed API: connection removed=" + connection.connectionId));
    }
}
