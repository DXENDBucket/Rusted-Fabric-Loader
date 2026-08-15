package io.github.endx.rustedfabricapi.verification;

import io.github.endx.rustedfabricapi.api.audio.SoundEvents;
import io.github.endx.rustedfabricapi.api.asset.AssetTextContractVerification;
import io.github.endx.rustedfabricapi.api.asset.condition.ResourceConditionContractVerification;
import io.github.endx.rustedfabricapi.api.asset.reload.ResourceReloadContractVerification;
import io.github.endx.rustedfabricapi.api.client.event.ClientRenderEvents;
import io.github.endx.rustedfabricapi.api.client.option.ClientOptionContractVerification;
import io.github.endx.rustedfabricapi.api.client.render.ClientRenderContractVerification;
import io.github.endx.rustedfabricapi.api.client.event.SelectionEvents;
import io.github.endx.rustedfabricapi.api.client.input.KeyBindings;
import io.github.endx.rustedfabricapi.api.client.input.ClientInputContractVerification;
import io.github.endx.rustedfabricapi.api.client.input.ModKeyBinding;
import io.github.endx.rustedfabricapi.api.client.screen.ScreenContractVerification;
import io.github.endx.rustedfabricapi.api.client.screen.dialog.DialogContractVerification;
import io.github.endx.rustedfabricapi.api.client.message.MessageEvents;
import io.github.endx.rustedfabricapi.api.client.minimap.MinimapEvents;
import io.github.endx.rustedfabricapi.api.client.minimap.MinimapMarkerKind;
import io.github.endx.rustedfabricapi.api.client.minimap.ScreenPoint;
import io.github.endx.rustedfabricapi.api.client.warlog.WarLogEntryKind;
import io.github.endx.rustedfabricapi.api.client.warlog.WarLogEvents;
import io.github.endx.rustedfabricapi.api.chat.ChatEvents;
import io.github.endx.rustedfabricapi.api.chat.command.ChatCommandContractVerification;
import io.github.endx.rustedfabricapi.api.config.ConfigEvents;
import io.github.endx.rustedfabricapi.api.config.ModConfigFile;
import io.github.endx.rustedfabricapi.api.config.ModConfigFiles;
import io.github.endx.rustedfabricapi.api.custom.action.event.CustomActionEffectEvents;
import io.github.endx.rustedfabricapi.api.custom.attachment.event.AttachmentEvents;
import io.github.endx.rustedfabricapi.api.custom.event.CustomUnitTriggerEvents;
import io.github.endx.rustedfabricapi.api.custom.event.CustomUnitOperationEvents;
import io.github.endx.rustedfabricapi.api.data.PersistentData;
import io.github.endx.rustedfabricapi.api.data.PersistentDataCodec;
import io.github.endx.rustedfabricapi.api.data.PersistentDataKey;
import io.github.endx.rustedfabricapi.api.datagen.DataGenerationContractVerification;
import io.github.endx.rustedfabricapi.api.event.RustedFabricEvent;
import io.github.endx.rustedfabricapi.api.map.event.MapLifecycleEvents;
import io.github.endx.rustedfabricapi.api.map.MapObjectContractVerification;
import io.github.endx.rustedfabricapi.api.map.MapTileContractVerification;
import io.github.endx.rustedfabricapi.api.lobby.LobbyContractVerification;
import io.github.endx.rustedfabricapi.api.mission.event.MissionTriggerEvents;
import io.github.endx.rustedfabricapi.api.networking.NamedChannelContractVerification;
import io.github.endx.rustedfabricapi.api.networking.PacketCodecs;
import io.github.endx.rustedfabricapi.api.networking.event.ConnectionEvents;
import io.github.endx.rustedfabricapi.api.path.PathQuery;
import io.github.endx.rustedfabricapi.api.path.event.PathEvents;
import io.github.endx.rustedfabricapi.api.save.Saves;
import io.github.endx.rustedfabricapi.api.save.event.SaveEvents;
import io.github.endx.rustedfabricapi.api.scheduler.GameTickSchedulerContractVerification;
import io.github.endx.rustedfabricapi.api.replay.Replays;
import io.github.endx.rustedfabricapi.api.replay.event.ReplayEvents;
import io.github.endx.rustedfabricapi.api.registry.RegistryContractVerification;
import io.github.endx.rustedfabricapi.api.registry.tag.RegistryTagContractVerification;
import io.github.endx.rustedfabricapi.api.stats.event.StatisticsEvents;
import io.github.endx.rustedfabricapi.api.unit.event.UnitDamageEvents;
import io.github.endx.rustedfabricapi.api.unit.event.UnitSpawnEvents;
import io.github.endx.rustedfabricapi.api.unit.event.UnitTeamEvents;
import io.github.endx.rustedfabricapi.api.unit.TeamCreditChangeSource;
import io.github.endx.rustedfabricapi.api.unit.TeamOutcome;
import io.github.endx.rustedfabricapi.api.unit.movement.UnitMovementMode;
import io.github.endx.rustedfabricapi.api.unit.action.JavaUnitActionContractVerification;
import io.github.endx.rustedfabricapi.api.unit.action.BuildingPlacementContractVerification;
import io.github.endx.rustedfabricapi.api.unit.event.TeamStateEvents;
import io.github.endx.rustedfabricapi.api.unit.type.event.UnitTypeEvents;
import io.github.endx.rustedfabricapi.api.unit.build.event.BuildQueueEvents;
import io.github.endx.rustedfabricapi.api.unit.combat.event.CombatEvents;
import io.github.endx.rustedfabricapi.api.unit.repair.event.RepairReclaimEvents;
import io.github.endx.rustedfabricapi.api.unit.status.event.StatusEffectEvents;
import io.github.endx.rustedfabricapi.api.unit.status.StatusEffectKind;
import io.github.endx.rustedfabricapi.api.unit.status.StatusEffectSnapshot;
import io.github.endx.rustedfabricapi.api.unit.status.StatusEffects;
import io.github.endx.rustedfabricapi.api.unit.attribute.CustomUnitStats;
import io.github.endx.rustedfabricapi.api.unit.attribute.UnitStat;
import io.github.endx.rustedfabricapi.api.unit.attribute.UnitStatModifier;
import io.github.endx.rustedfabricapi.api.unit.attribute.UnitStatOperation;
import io.github.endx.rustedfabricapi.api.unit.attribute.event.UnitStatEvents;
import io.github.endx.rustedfabricapi.api.unit.transport.event.TransportEvents;
import io.github.endx.rustedfabricapi.api.unit.tag.UnitTags;
import io.github.endx.rustedfabricapi.api.unit.tag.event.UnitTagEvents;
import rustedwarfare.custom.CustomTagList;
import rustedwarfare.custom.MutableUnitStats;
import rustedwarfare.unit.status.MovementSpeedStatusEffect;
import io.github.endx.rustedfabricapi.api.world.WorldPoint;
import io.github.endx.rustedfabricapi.api.util.Identifier;
import rustedwarfare.io.GameInputStream;
import rustedwarfare.io.GameOutputStream;
import rustedwarfare.unit.MovementType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class MappedApiContractVerification {
    private MappedApiContractVerification() {
    }

    public static void main(String[] args) {
        AndroidShaderCompatibilityContractVerification.verify();
        GameTickSchedulerContractVerification.verify();
        DataGenerationContractVerification.verify();
        ResourceConditionContractVerification.verify();
        verifyRegistrationOrderAndRemoval();
        ClientOptionContractVerification.verify();
        ClientRenderContractVerification.verify();
        ClientInputContractVerification.verify();
        ScreenContractVerification.verify();
        DialogContractVerification.verify();
        MapObjectContractVerification.verify();
        MapTileContractVerification.verify();
        JavaUnitActionContractVerification.verify();
        BuildingPlacementContractVerification.verify();
        verifyCancellationAggregation();
        verifyModifierChaining();
        verifyMapCancellationAggregation();
        verifyUnitTypeSpawnResultChaining();
        verifyBuildQueueModifierChaining();
        verifyTransportModifierChaining();
        verifyCustomActionEffectCancellationAggregation();
        verifyCombatModifierChaining();
        verifyCombatFireCancellationAggregation();
        verifyRepairReclaimEventContracts();
        verifyStatusEffectEventContracts();
        verifyStatusEffectClassification();
        verifyUnitStatModifierContract();
        verifyPathQueryAndEventContracts();
        verifyUnitMovementModes();
        verifyPersistentDataRoundTrip();
        verifyUnitTagSetSemantics();
        verifyExtensionCancellationAggregation();
        NamedChannelContractVerification.verify();
        verifyConnectionLifecycleEvents();
        verifyUnitSpawnCancellationAndTeamEvents();
        verifyTeamStateEventContracts();
        verifyWorldPointValueContract();
        verifyKeyBindingRegistrationContract();
        verifySoundCancellationAggregation();
        verifySaveContract();
        verifyMessageAndMissionCancellation();
        verifyReplayAndClientUtilityContracts();
        verifyStatisticsEventContract();
        verifyConfigAndChatContracts();
        ChatCommandContractVerification.verify();
        LobbyContractVerification.verify();
        AssetTextContractVerification.verify();
        ResourceReloadContractVerification.verify();
        RegistryContractVerification.verify();
        RegistryTagContractVerification.verify();
        System.out.println("Mapped Rusted Fabric API contract verification passed");
    }

    private static void verifyUnitMovementModes() {
        require(UnitMovementMode.parse("ground") == UnitMovementMode.LAND
                        && UnitMovementMode.parse("naval") == UnitMovementMode.WATER
                        && UnitMovementMode.parse("overCliffWater")
                                == UnitMovementMode.OVER_CLIFF_WATER
                        && UnitMovementMode.parse("structure") == UnitMovementMode.BUILDING,
                "unit movement aliases changed");
        require(UnitMovementMode.AIR.movementType() == MovementType.air
                        && UnitMovementMode.BUILDING.movementType() == MovementType.building
                        && UnitMovementMode.BUILDING.building(false)
                        && !UnitMovementMode.LAND.building(true)
                        && UnitMovementMode.NATIVE.building(true),
                "unit movement native mapping changed");
        try {
            UnitMovementMode.parse("underwater");
            throw new AssertionError("underwater incorrectly became a separate movement type");
        } catch (IllegalArgumentException expected) {
            // Native submerging remains water movement plus negative height.
        }
    }

    private static void verifyTeamStateEventContracts() {
        List<Double> inputs = new ArrayList<Double>();
        RustedFabricEvent.Registration modifierFirst =
                TeamStateEvents.MODIFY_SET_CREDITS.subscribe((team, current, requested) -> {
                    inputs.add(Double.valueOf(requested));
                    return requested + 5.0;
                });
        RustedFabricEvent.Registration modifierSecond =
                TeamStateEvents.MODIFY_SET_CREDITS.subscribe((team, current, requested) -> {
                    inputs.add(Double.valueOf(requested));
                    return requested * 2.0;
                });
        double modified = TeamStateEvents.MODIFY_SET_CREDITS.invoker()
                .modify(null, 100.0, 20.0);
        require(modified == 50.0,
                "team credit modifiers returned the wrong value");
        require(inputs.equals(Arrays.asList(Double.valueOf(20.0), Double.valueOf(25.0))),
                "team credit modifiers did not chain in registration order");
        modifierFirst.close();
        modifierSecond.close();

        AtomicInteger calls = new AtomicInteger();
        RustedFabricEvent.Registration beforeFirst =
                TeamStateEvents.BEFORE_SET_CREDITS.subscribe((team, current, requested) -> {
                    calls.incrementAndGet();
                    return false;
                });
        RustedFabricEvent.Registration beforeSecond =
                TeamStateEvents.BEFORE_SET_CREDITS.subscribe((team, current, requested) -> {
                    calls.incrementAndGet();
                    return true;
                });
        require(TeamStateEvents.BEFORE_SET_CREDITS.invoker()
                        .beforeSet(null, 10.0, 20.0),
                "team credit cancellation was not aggregated");
        require(calls.get() == 2, "team credit cancellation skipped a listener");
        beforeFirst.close();
        beforeSecond.close();

        calls.set(0);
        RustedFabricEvent.Registration changed =
                TeamStateEvents.AFTER_CREDITS_CHANGED.subscribe(
                        (team, previous, current, source) -> {
                            if (source == TeamCreditChangeSource.NATIVE_RECORDED_INCOME
                                    && previous == 4.0 && current == 7.0) {
                                calls.incrementAndGet();
                            }
                        });
        RustedFabricEvent.Registration outcome =
                TeamStateEvents.OUTCOME_ANNOUNCED.subscribe((network, team, result) -> {
                    if (result == TeamOutcome.WIPED_OUT) calls.addAndGet(10);
                });
        TeamStateEvents.AFTER_CREDITS_CHANGED.invoker().afterChange(null, 4.0, 7.0,
                TeamCreditChangeSource.NATIVE_RECORDED_INCOME);
        TeamStateEvents.OUTCOME_ANNOUNCED.invoker().onOutcome(
                null, null, TeamOutcome.WIPED_OUT);
        require(calls.get() == 11,
                "team economy or outcome event was not dispatched");
        changed.close();
        outcome.close();
    }

    private static void verifyPathQueryAndEventContracts() {
        PathQuery base = PathQuery.betweenTiles(MovementType.land, 1, 2, 30, 40);
        PathQuery configured = base.withEndRadius(3).withStartDirection(90.0f)
                .lowPriority(true).refreshCosts(true);
        require(base.endRadius() == 0 && !base.lowPriority() && !base.refreshCosts(),
                "path query mutation changed the original value");
        require(configured.movementType() == MovementType.land
                        && configured.startTileX() == 1 && configured.startTileY() == 2
                        && configured.endTileX() == 30 && configured.endTileY() == 40
                        && configured.endRadius() == 3 && configured.lowPriority()
                        && configured.refreshCosts()
                        && Float.valueOf(90.0f).equals(configured.startDirection()),
                "configured path query lost an option");
        try {
            PathQuery.betweenTiles(MovementType.land, -1, 0, 1, 1);
            throw new AssertionError("negative path tile coordinate was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }

        AtomicInteger calls = new AtomicInteger();
        RustedFabricEvent.Registration queuing = PathEvents.QUEUING.subscribe(
                (engine, request, refresh) -> calls.addAndGet(refresh ? 1 : 100));
        RustedFabricEvent.Registration queued = PathEvents.QUEUED.subscribe(
                (engine, request, refresh) -> calls.addAndGet(refresh ? 10 : 1000));
        PathEvents.QUEUING.invoker().onQueuing(null, null, true);
        PathEvents.QUEUED.invoker().onQueued(null, null, true);
        require(calls.get() == 11, "path queue lifecycle events were not dispatched");
        queuing.close();
        queued.close();
    }

    private static void verifyConfigAndChatContracts() {
        ModConfigFile file = ModConfigFiles.file("contract_mod", "nested/settings.properties");
        require("nested/settings.properties".equals(file.relativePath().toString().replace('\\', '/')),
                "mod configuration relative path changed");
        require(file.equals(ModConfigFiles.file("CONTRACT_MOD", "nested/settings.properties")),
                "equivalent mod configuration handles were not equal");
        try {
            ModConfigFiles.file("contract_mod", "folder/../escape.properties");
            throw new AssertionError("ambiguous mod configuration traversal was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
        try {
            ModConfigFiles.file("contract_mod", "CON.txt");
            throw new AssertionError("Windows device configuration name was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }

        AtomicInteger calls = new AtomicInteger();
        RustedFabricEvent.Registration first = ConfigEvents.BEFORE_WRITE.subscribe(config -> {
            calls.incrementAndGet();
            return false;
        });
        RustedFabricEvent.Registration second = ConfigEvents.BEFORE_WRITE.subscribe(config -> {
            calls.incrementAndGet();
            return true;
        });
        require(ConfigEvents.BEFORE_WRITE.invoker().beforeMutation(file),
                "configuration write cancellation was not aggregated");
        require(calls.get() == 2, "configuration write cancellation skipped a listener");
        first.close();
        second.close();

        calls.set(0);
        RustedFabricEvent.Registration outgoingFirst = ChatEvents.BEFORE_OUTGOING.subscribe(
                (network, message) -> {
                    calls.incrementAndGet();
                    return false;
                });
        RustedFabricEvent.Registration outgoingSecond = ChatEvents.BEFORE_OUTGOING.subscribe(
                (network, message) -> {
                    calls.incrementAndGet();
                    return true;
                });
        require(ChatEvents.BEFORE_OUTGOING.invoker().onOutgoing(null, "test"),
                "outgoing chat cancellation was not aggregated");
        require(calls.get() == 2, "outgoing chat cancellation skipped a listener");
        outgoingFirst.close();
        outgoingSecond.close();
    }

    private static void verifyUnitStatModifierContract() {
        require(UnitStat.values().length == 19, "native mutable-stat catalog is incomplete");
        for (UnitStat stat : UnitStat.values()) {
            require(MutableUnitStats.getMutableStatAccessorById(stat.nativeId()) != null,
                    "native mutable-stat accessor is missing: " + stat);
            require(MutableUnitStats.getMutableStatAccessorById(stat.nativeId()).isRuntimeField()
                            == stat.runtimeValue(),
                    "runtime mutable-stat classification drifted: " + stat);
        }
        List<UnitStatModifier> modifiers = Arrays.asList(
                UnitStatModifier.of("test:z_total", UnitStatOperation.MULTIPLY_TOTAL, 0.5),
                UnitStatModifier.of("test:a_add", UnitStatOperation.ADD_VALUE, 10.0),
                UnitStatModifier.of("test:m_base", UnitStatOperation.ADD_MULTIPLIED_BASE, 0.25));
        require(CustomUnitStats.evaluate(100.0, modifiers) == 202.5,
                "unit stat modifier stages were evaluated out of order");

        AtomicInteger calls = new AtomicInteger();
        RustedFabricEvent.Registration first = UnitStatEvents.MODIFY_SET_VALUE.subscribe(
                (unit, stat, value) -> {
                    calls.incrementAndGet();
                    return value + 2.0;
                });
        RustedFabricEvent.Registration second = UnitStatEvents.MODIFY_SET_VALUE.subscribe(
                (unit, stat, value) -> {
                    calls.incrementAndGet();
                    return value * 3.0;
                });
        require(UnitStatEvents.MODIFY_SET_VALUE.invoker().modify(null, null, 4.0) == 18.0,
                "unit stat set modifiers did not chain in registration order");
        require(calls.get() == 2, "unit stat set modifier skipped a listener");
        first.close();
        second.close();

        try {
            UnitStatModifier.of("test:not_finite", UnitStatOperation.ADD_VALUE,
                    Double.NaN);
            throw new AssertionError("non-finite unit stat modifier was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void verifySaveContract() {
        require("slot one.rwsave".equals(Saves.normalizeName(" slot one ")),
                "save extension was not normalized");
        require("SLOT.RWSAVE".equals(Saves.normalizeName("SLOT.RWSAVE")),
                "existing save extension was changed");
        try {
            Saves.normalizeName("../outside");
            throw new AssertionError("unsafe save path was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }

        AtomicInteger calls = new AtomicInteger();
        RustedFabricEvent.Registration first = SaveEvents.BEFORE_SAVE.subscribe(
                (manager, name, automatic) -> {
                    calls.incrementAndGet();
                    return false;
                });
        RustedFabricEvent.Registration second = SaveEvents.BEFORE_SAVE.subscribe(
                (manager, name, automatic) -> {
                    calls.incrementAndGet();
                    return true;
                });
        require(SaveEvents.BEFORE_SAVE.invoker().beforeSave(null, "slot.rwsave", false),
                "save cancellation was not aggregated");
        require(calls.get() == 2, "save cancellation skipped a listener");
        first.close();
        second.close();
    }

    private static void verifyMessageAndMissionCancellation() {
        AtomicInteger calls = new AtomicInteger();
        RustedFabricEvent.Registration message = MessageEvents.BEFORE_ADD.subscribe(
                (history, sender, text) -> {
                    calls.incrementAndGet();
                    return true;
                });
        RustedFabricEvent.Registration mission = MissionTriggerEvents.BEFORE_ACTIVATE.subscribe(
                (engine, trigger) -> {
                    calls.addAndGet(10);
                    return true;
                });
        require(MessageEvents.BEFORE_ADD.invoker().beforeAdd(null, null, "message"),
                "message cancellation was not dispatched");
        require(MissionTriggerEvents.BEFORE_ACTIVATE.invoker().beforeActivate(null, null),
                "mission-trigger cancellation was not dispatched");
        require(calls.get() == 11, "message or mission-trigger event was not dispatched");
        message.close();
        mission.close();
    }

    private static void verifyReplayAndClientUtilityContracts() {
        require("match one.replay".equals(Replays.normalizeName(" match one ")),
                "replay extension was not normalized");
        require("MATCH.REPLAY".equals(Replays.normalizeName("MATCH.REPLAY")),
                "existing replay extension was changed");
        try {
            Replays.normalizeName("../outside");
            throw new AssertionError("unsafe replay path was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }

        AtomicInteger calls = new AtomicInteger();
        RustedFabricEvent.Registration replayFirst = ReplayEvents.BEFORE_PLAY.subscribe(
                (manager, name) -> {
                    calls.incrementAndGet();
                    return false;
                });
        RustedFabricEvent.Registration replaySecond = ReplayEvents.BEFORE_PLAY.subscribe(
                (manager, name) -> {
                    calls.incrementAndGet();
                    return true;
                });
        require(ReplayEvents.BEFORE_PLAY.invoker().beforeOperation(null, "test.replay"),
                "replay cancellation was not aggregated");
        require(calls.get() == 2, "replay cancellation skipped a listener");
        replayFirst.close();
        replaySecond.close();

        RustedFabricEvent.Registration warLog = WarLogEvents.BEFORE_UNIT_ENTRY.subscribe(
                (log, kind, unit) -> kind == WarLogEntryKind.UNIT_DAMAGED);
        require(WarLogEvents.BEFORE_UNIT_ENTRY.invoker()
                        .beforeUnitEntry(null, WarLogEntryKind.UNIT_DAMAGED, null),
                "war-log cancellation was not dispatched");
        warLog.close();

        RustedFabricEvent.Registration minimap = MinimapEvents.BEFORE_MARKER.subscribe(
                (map, x, y, kind) -> kind == MinimapMarkerKind.MESSAGE);
        require(MinimapEvents.BEFORE_MARKER.invoker()
                        .beforeMarker(null, 1, 2, MinimapMarkerKind.MESSAGE),
                "minimap-marker cancellation was not dispatched");
        minimap.close();

        require(new ScreenPoint(3, 4).equals(new ScreenPoint(3, 4)),
                "screen-point value equality was incorrect");
    }

    private static void verifyStatisticsEventContract() {
        AtomicInteger calls = new AtomicInteger();
        RustedFabricEvent.Registration reset = StatisticsEvents.AFTER_RESET.subscribe(
                manager -> calls.incrementAndGet());
        RustedFabricEvent.Registration killed = StatisticsEvents.AFTER_UNIT_KILLED.subscribe(
                (dispatcher, unit, attacker) -> calls.addAndGet(10));
        StatisticsEvents.AFTER_RESET.invoker().onStatistics(null);
        StatisticsEvents.AFTER_UNIT_KILLED.invoker().onUnitKilled(null, null, null);
        require(calls.get() == 11, "typed statistics events were not dispatched");
        reset.close();
        killed.close();
    }

    private static void verifyKeyBindingRegistrationContract() {
        ModKeyBinding binding = KeyBindings.register(
                "contract:test_action", "Contract Test Action", "Contract Tests", "CTRL+K");
        require(binding == KeyBindings.register(
                        "contract:test_action", "Contract Test Action", "Contract Tests", "CTRL+K"),
                "identical key binding registration was not idempotent");
        require(KeyBindings.find("contract:test_action").orElse(null) == binding,
                "registered key binding could not be found");
        require(KeyBindings.customBindings().contains(binding),
                "registered key binding was absent from the snapshot");
        try {
            KeyBindings.register("Invalid Id", "Invalid", "NONE");
            throw new AssertionError("invalid key binding id was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void verifySoundCancellationAggregation() {
        AtomicInteger calls = new AtomicInteger();
        RustedFabricEvent.Registration first = SoundEvents.BEFORE_PLAY.subscribe(
                (engine, playback) -> {
                    calls.incrementAndGet();
                    return false;
                });
        RustedFabricEvent.Registration second = SoundEvents.BEFORE_PLAY.subscribe(
                (engine, playback) -> {
                    calls.incrementAndGet();
                    return true;
                });
        require(SoundEvents.BEFORE_PLAY.invoker().beforePlay(null, null),
                "sound cancellation was not aggregated");
        require(calls.get() == 2, "sound cancellation skipped a listener");
        first.close();
        second.close();
    }

    private static void verifyUnitSpawnCancellationAndTeamEvents() {
        AtomicInteger spawnCalls = new AtomicInteger();
        RustedFabricEvent.Registration first = UnitSpawnEvents.BEFORE_SPAWN.subscribe(
                (type, team, x, y, height, direction) -> {
                    spawnCalls.incrementAndGet();
                    return false;
                });
        RustedFabricEvent.Registration second = UnitSpawnEvents.BEFORE_SPAWN.subscribe(
                (type, team, x, y, height, direction) -> {
                    spawnCalls.incrementAndGet();
                    return true;
                });
        require(UnitSpawnEvents.BEFORE_SPAWN.invoker()
                        .beforeSpawn(null, null, 0.0F, 0.0F, 0.0F, 0.0F),
                "unit spawn cancellation was not aggregated");
        require(spawnCalls.get() == 2, "unit spawn cancellation skipped a listener");
        first.close();
        second.close();

        AtomicInteger teamCalls = new AtomicInteger();
        RustedFabricEvent.Registration before = UnitTeamEvents.BEFORE_CHANGE.subscribe(
                (unit, oldTeam, newTeam) -> teamCalls.incrementAndGet());
        RustedFabricEvent.Registration after = UnitTeamEvents.AFTER_CHANGE.subscribe(
                (unit, newTeam) -> teamCalls.incrementAndGet());
        UnitTeamEvents.BEFORE_CHANGE.invoker().beforeChange(null, null, null);
        UnitTeamEvents.AFTER_CHANGE.invoker().afterChange(null, null);
        require(teamCalls.get() == 2, "unit team-change events were not dispatched");
        before.close();
        after.close();
    }

    private static void verifyWorldPointValueContract() {
        WorldPoint point = new WorldPoint(3.0F, 4.0F);
        require(point.equals(new WorldPoint(3.0F, 4.0F)),
                "equal world points were not equal");
        require(point.distanceSquared(new WorldPoint(0.0F, 0.0F)) == 25.0F,
                "world-point distance was incorrect");
        try {
            new WorldPoint(Float.NaN, 0.0F);
            throw new AssertionError("non-finite world point was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void verifyConnectionLifecycleEvents() {
        AtomicInteger calls = new AtomicInteger();
        RustedFabricEvent.Registration ready = ConnectionEvents.CLIENT_CONNECTION_READY.subscribe(
                (engine, connection) -> calls.incrementAndGet());
        RustedFabricEvent.Registration removed = ConnectionEvents.CONNECTION_REMOVED.subscribe(
                (engine, connection) -> calls.addAndGet(10));
        ConnectionEvents.CLIENT_CONNECTION_READY.invoker().onReady(null, null);
        ConnectionEvents.CONNECTION_REMOVED.invoker().onRemoved(null, null);
        require(calls.get() == 11, "typed connection lifecycle events were not dispatched");
        ready.close();
        removed.close();
    }

    private static void verifyRegistrationOrderAndRemoval() {
        List<Integer> order = new ArrayList<Integer>();
        RustedFabricEvent.Registration first = ClientRenderEvents.END_CLIENT_RENDER.subscribe(
                (engine, graphics) -> order.add(Integer.valueOf(1)));
        RustedFabricEvent.Registration second = ClientRenderEvents.END_CLIENT_RENDER.subscribe(
                (engine, graphics) -> order.add(Integer.valueOf(2)));

        ClientRenderEvents.END_CLIENT_RENDER.invoker().onRender(null, null);
        require(order.equals(Arrays.asList(Integer.valueOf(1), Integer.valueOf(2))),
                "render listeners did not retain registration order");

        require(first.unregister(), "first unregister should report removal");
        require(!first.unregister(), "registration removal must be idempotent");
        order.clear();
        ClientRenderEvents.END_CLIENT_RENDER.invoker().onRender(null, null);
        require(order.equals(Arrays.asList(Integer.valueOf(2))),
                "removed render listener was still invoked");
        second.close();
    }

    private static void verifyCancellationAggregation() {
        AtomicInteger calls = new AtomicInteger();
        RustedFabricEvent.Registration first = SelectionEvents.BEFORE_CLEAR.subscribe(gameInterface -> {
            calls.incrementAndGet();
            return true;
        });
        RustedFabricEvent.Registration second = SelectionEvents.BEFORE_CLEAR.subscribe(gameInterface -> {
            calls.incrementAndGet();
            return false;
        });

        require(SelectionEvents.BEFORE_CLEAR.invoker().beforeClear(null),
                "selection cancellation was not aggregated");
        require(calls.get() == 2, "selection cancellation short-circuited later listeners");
        first.close();
        second.close();
    }

    private static void verifyModifierChaining() {
        List<Boolean> inputs = new ArrayList<Boolean>();
        RustedFabricEvent.Registration first = UnitDamageEvents.MODIFY_DAMAGE_IMMUNITY.subscribe(
                (unit, current) -> {
                    inputs.add(Boolean.valueOf(current));
                    return Boolean.TRUE;
                });
        RustedFabricEvent.Registration second = UnitDamageEvents.MODIFY_DAMAGE_IMMUNITY.subscribe(
                (unit, current) -> {
                    inputs.add(Boolean.valueOf(current));
                    return null;
                });
        RustedFabricEvent.Registration third = UnitDamageEvents.MODIFY_DAMAGE_IMMUNITY.subscribe(
                (unit, current) -> {
                    inputs.add(Boolean.valueOf(current));
                    return Boolean.FALSE;
                });

        Boolean result = UnitDamageEvents.MODIFY_DAMAGE_IMMUNITY.invoker().modify(null, false);
        require(Boolean.FALSE.equals(result), "damage immunity modifier returned the wrong result");
        require(inputs.equals(Arrays.asList(Boolean.FALSE, Boolean.TRUE, Boolean.TRUE)),
                "damage immunity modifiers were not chained");
        first.close();
        second.close();
        third.close();
    }

    private static void verifyMapCancellationAggregation() {
        AtomicInteger calls = new AtomicInteger();
        RustedFabricEvent.Registration first = MapLifecycleEvents.BEFORE_MAP_STREAM_OPEN.subscribe(path -> {
            calls.incrementAndGet();
            return false;
        });
        RustedFabricEvent.Registration second = MapLifecycleEvents.BEFORE_MAP_STREAM_OPEN.subscribe(path -> {
            calls.incrementAndGet();
            return true;
        });
        require(MapLifecycleEvents.BEFORE_MAP_STREAM_OPEN.invoker()
                        .beforeMapStreamOpen("maps/example.tmx"),
                "map stream cancellation was not aggregated");
        require(calls.get() == 2, "map stream cancellation skipped a listener");
        first.close();
        second.close();
    }

    private static void verifyUnitTypeSpawnResultChaining() {
        List<Boolean> inputs = new ArrayList<Boolean>();
        RustedFabricEvent.Registration first = UnitTypeEvents.AFTER_STARTING_SPAWN.subscribe(
                (type, x, y, direction, height, team, current) -> {
                    inputs.add(Boolean.valueOf(current));
                    return false;
                });
        RustedFabricEvent.Registration second = UnitTypeEvents.AFTER_STARTING_SPAWN.subscribe(
                (type, x, y, direction, height, team, current) -> {
                    inputs.add(Boolean.valueOf(current));
                    return true;
                });
        boolean result = UnitTypeEvents.AFTER_STARTING_SPAWN.invoker()
                .afterSpawn(null, 0.0F, 0.0F, 0.0F, 0.0F, null, true);
        require(result, "starting-spawn result modifiers returned the wrong result");
        require(inputs.equals(Arrays.asList(Boolean.TRUE, Boolean.FALSE)),
                "starting-spawn result modifiers were not chained");
        first.close();
        second.close();
    }

    private static void verifyBuildQueueModifierChaining() {
        List<Boolean> inputs = new ArrayList<Boolean>();
        RustedFabricEvent.Registration first =
                BuildQueueEvents.MODIFY_HOST_ITEM_REFUNDABLE.subscribe((host, item, current) -> {
                    inputs.add(Boolean.valueOf(current));
                    return Boolean.TRUE;
                });
        RustedFabricEvent.Registration second =
                BuildQueueEvents.MODIFY_HOST_ITEM_REFUNDABLE.subscribe((host, item, current) -> {
                    inputs.add(Boolean.valueOf(current));
                    return null;
                });
        Boolean result = BuildQueueEvents.MODIFY_HOST_ITEM_REFUNDABLE.invoker()
                .modify(null, null, false);
        require(Boolean.TRUE.equals(result), "build-queue refundable modifier returned wrong result");
        require(inputs.equals(Arrays.asList(Boolean.FALSE, Boolean.TRUE)),
                "build-queue refundable modifiers were not chained");
        first.close();
        second.close();
    }

    private static void verifyTransportModifierChaining() {
        List<Integer> inputs = new ArrayList<Integer>();
        RustedFabricEvent.Registration first = TransportEvents.MODIFY_USED_SLOTS.subscribe(
                (unit, current) -> {
                    inputs.add(Integer.valueOf(current));
                    return Integer.valueOf(current + 2);
                });
        RustedFabricEvent.Registration second = TransportEvents.MODIFY_USED_SLOTS.subscribe(
                (unit, current) -> {
                    inputs.add(Integer.valueOf(current));
                    return null;
                });
        RustedFabricEvent.Registration third = TransportEvents.MODIFY_USED_SLOTS.subscribe(
                (unit, current) -> {
                    inputs.add(Integer.valueOf(current));
                    return Integer.valueOf(current * 3);
                });
        Integer result = TransportEvents.MODIFY_USED_SLOTS.invoker().modify(null, 1);
        require(Integer.valueOf(9).equals(result),
                "transport slot modifiers returned the wrong result");
        require(inputs.equals(Arrays.asList(Integer.valueOf(1), Integer.valueOf(3), Integer.valueOf(3))),
                "transport slot modifiers were not chained");
        first.close();
        second.close();
        third.close();
    }

    private static void verifyCustomActionEffectCancellationAggregation() {
        AtomicInteger calls = new AtomicInteger();
        RustedFabricEvent.Registration first = CustomActionEffectEvents.BEFORE_EXECUTE.subscribe(
                (effect, actor, action, x, y, hasPoint, target, depth) -> {
                    calls.incrementAndGet();
                    return true;
                });
        RustedFabricEvent.Registration second = CustomActionEffectEvents.BEFORE_EXECUTE.subscribe(
                (effect, actor, action, x, y, hasPoint, target, depth) -> {
                    calls.incrementAndGet();
                    return false;
                });
        require(CustomActionEffectEvents.BEFORE_EXECUTE.invoker()
                        .beforeExecute(null, null, null, Float.NaN, Float.NaN,
                                false, null, 0),
                "custom action-effect cancellation was not aggregated");
        require(calls.get() == 2,
                "custom action-effect cancellation skipped a listener");
        first.close();
        second.close();
    }

    private static void verifyCombatModifierChaining() {
        List<Boolean> inputs = new ArrayList<Boolean>();
        RustedFabricEvent.Registration first = CombatEvents.MODIFY_TARGET_IN_RANGE.subscribe(
                (attacker, target, current) -> {
                    inputs.add(Boolean.valueOf(current));
                    return Boolean.TRUE;
                });
        RustedFabricEvent.Registration second = CombatEvents.MODIFY_TARGET_IN_RANGE.subscribe(
                (attacker, target, current) -> {
                    inputs.add(Boolean.valueOf(current));
                    return null;
                });
        RustedFabricEvent.Registration third = CombatEvents.MODIFY_TARGET_IN_RANGE.subscribe(
                (attacker, target, current) -> {
                    inputs.add(Boolean.valueOf(current));
                    return Boolean.FALSE;
                });
        Boolean result = CombatEvents.MODIFY_TARGET_IN_RANGE.invoker().modify(null, null, false);
        require(Boolean.FALSE.equals(result), "combat range modifier returned the wrong result");
        require(inputs.equals(Arrays.asList(Boolean.FALSE, Boolean.TRUE, Boolean.TRUE)),
                "combat range modifiers were not chained");
        first.close();
        second.close();
        third.close();
    }

    private static void verifyCombatFireCancellationAggregation() {
        AtomicInteger calls = new AtomicInteger();
        RustedFabricEvent.Registration first = CombatEvents.BEFORE_TRY_FIRE.subscribe(
                (attacker, delta, target, turretIndex) -> {
                    calls.incrementAndGet();
                    return false;
                });
        RustedFabricEvent.Registration second = CombatEvents.BEFORE_TRY_FIRE.subscribe(
                (attacker, delta, target, turretIndex) -> {
                    calls.incrementAndGet();
                    return true;
                });
        require(CombatEvents.BEFORE_TRY_FIRE.invoker().beforeTryFire(null, 1.0F, null, 0),
                "combat fire cancellation was not aggregated");
        require(calls.get() == 2, "combat fire cancellation skipped a listener");
        first.close();
        second.close();
    }

    private static void verifyRepairReclaimEventContracts() {
        List<Boolean> inputs = new ArrayList<Boolean>();
        RustedFabricEvent.Registration modifierFirst =
                RepairReclaimEvents.MODIFY_CAN_REPAIR.subscribe((unit, target, current) -> {
                    inputs.add(Boolean.valueOf(current));
                    return Boolean.TRUE;
                });
        RustedFabricEvent.Registration modifierSecond =
                RepairReclaimEvents.MODIFY_CAN_REPAIR.subscribe((unit, target, current) -> {
                    inputs.add(Boolean.valueOf(current));
                    return Boolean.FALSE;
                });
        Boolean result = RepairReclaimEvents.MODIFY_CAN_REPAIR.invoker().modify(null, null, false);
        require(Boolean.FALSE.equals(result), "repair decision modifiers returned wrong result");
        require(inputs.equals(Arrays.asList(Boolean.FALSE, Boolean.TRUE)),
                "repair decision modifiers were not chained");
        modifierFirst.close();
        modifierSecond.close();

        AtomicInteger calls = new AtomicInteger();
        RustedFabricEvent.Registration beforeFirst =
                RepairReclaimEvents.BEFORE_ORDER_UPDATE.subscribe((unit, delta, order) -> {
                    calls.incrementAndGet();
                    return false;
                });
        RustedFabricEvent.Registration beforeSecond =
                RepairReclaimEvents.BEFORE_ORDER_UPDATE.subscribe((unit, delta, order) -> {
                    calls.incrementAndGet();
                    return true;
                });
        require(RepairReclaimEvents.BEFORE_ORDER_UPDATE.invoker()
                        .beforeUpdate(null, 1.0F, null),
                "repair/reclaim order cancellation was not aggregated");
        require(calls.get() == 2, "repair/reclaim cancellation skipped a listener");
        beforeFirst.close();
        beforeSecond.close();

        AtomicInteger parityCalls = new AtomicInteger();
        RustedFabricEvent.Registration resourceDelta =
                RepairReclaimEvents.MODIFY_BUILD_QUEUE_RESOURCE_DELTA.subscribe((unit, current) -> {
                    parityCalls.incrementAndGet();
                    return current;
                });
        require(RepairReclaimEvents.MODIFY_BUILD_QUEUE_RESOURCE_DELTA.invoker()
                        .modify(null, null) == null,
                "build-queue resource delta did not retain a null value");
        RustedFabricEvent.Registration refresh =
                RepairReclaimEvents.AFTER_ACTIVE_RESOURCE_DELTA_REFRESH.subscribe(
                        unit -> parityCalls.incrementAndGet());
        RepairReclaimEvents.AFTER_ACTIVE_RESOURCE_DELTA_REFRESH.invoker().onUnit(null);
        require(parityCalls.get() == 2,
                "typed repair/reclaim parity events were not dispatched");
        resourceDelta.close();
        refresh.close();
    }

    private static void verifyStatusEffectEventContracts() {
        AtomicInteger calls = new AtomicInteger();
        RustedFabricEvent.Registration first = StatusEffectEvents.BEFORE_ADD.subscribe(
                (unit, effect) -> {
                    calls.incrementAndGet();
                    return false;
                });
        RustedFabricEvent.Registration second = StatusEffectEvents.BEFORE_ADD.subscribe(
                (unit, effect) -> {
                    calls.incrementAndGet();
                    return true;
                });
        require(StatusEffectEvents.BEFORE_ADD.invoker().beforeAdd(null, null),
                "status-effect add cancellation was not aggregated");
        require(calls.get() == 2, "status-effect add cancellation skipped a listener");
        first.close();
        second.close();

        AtomicInteger lifecycleCalls = new AtomicInteger();
        RustedFabricEvent.Registration afterAdd = StatusEffectEvents.AFTER_ADD.subscribe(
                (unit, effect, added) -> {
                    if (added) lifecycleCalls.incrementAndGet();
                });
        RustedFabricEvent.Registration expired = StatusEffectEvents.EXPIRED.subscribe(
                (unit, effect) -> lifecycleCalls.addAndGet(10));
        StatusEffectEvents.AFTER_ADD.invoker().afterAdd(null, null, true);
        StatusEffectEvents.EXPIRED.invoker().onExpired(null, null);
        require(lifecycleCalls.get() == 11,
                "status-effect lifecycle events were not dispatched");
        afterAdd.close();
        expired.close();
    }

    private static void verifyStatusEffectClassification() {
        MovementSpeedStatusEffect effect = new MovementSpeedStatusEffect();
        require(StatusEffects.kindOf(effect) == StatusEffectKind.MOVEMENT_SPEED,
                "movement-speed status effect had the wrong public kind");
        StatusEffectSnapshot snapshot = StatusEffectSnapshot.capture(effect, 0);
        require(snapshot.effect() == effect && snapshot.kind() == StatusEffectKind.MOVEMENT_SPEED,
                "status-effect snapshot did not preserve identity or kind");
    }

    private static void verifyPersistentDataRoundTrip() {
        PersistentDataKey<Integer> key = PersistentData.register(
                Identifier.of("contract", "round_trip"), 3,
                PersistentDataCodec.of(PacketCodecs.VAR_INT));
        PersistentDataKey<Integer> untouched = PersistentData.register(
                Identifier.of("contract", "untouched"), 1,
                PersistentDataCodec.of(PacketCodecs.VAR_INT));
        PersistentData.setGlobal(key, Integer.valueOf(73));
        PersistentData.setGlobal(untouched, Integer.valueOf(91));
        GameOutputStream output = new GameOutputStream();
        PersistentData.writeSaveExtension(output);
        byte[] bytes = output.toByteArray();
        PersistentData.clearRuntime();
        require(!PersistentData.getGlobal(key).isPresent(),
                "persistent runtime clear retained a global value");
        int restored = PersistentData.readSaveExtension(new GameInputStream(bytes));
        require(restored == 2, "persistent extension restored the wrong entry count");
        require(Integer.valueOf(73).equals(PersistentData.getGlobal(key).orElse(null)),
                "persistent global value did not survive a byte round trip");

        GameOutputStream secondOutput = new GameOutputStream();
        PersistentData.writeSaveExtension(secondOutput);
        PersistentData.clearRuntime();
        PersistentData.readSaveExtension(new GameInputStream(secondOutput.toByteArray()));
        require(Integer.valueOf(91).equals(PersistentData.getGlobal(untouched).orElse(null)),
                "untouched raw persistent data was not preserved across a resave");

        byte[] vanillaTail = new byte[12];
        vanillaTail[3] = 7;
        GameInputStream vanilla = new GameInputStream(vanillaTail);
        require(PersistentData.readSaveExtension(vanilla) == 0,
                "non-Loader trailing data was treated as a persistent block");
        require(vanilla.readInt() == 7,
                "non-Loader trailing data was consumed while probing for the extension");
        PersistentData.clearRuntime();
    }

    private static void verifyUnitTagSetSemantics() {
        CustomTagList available = UnitTags.of("armored", "ground", "builder");
        CustomTagList required = UnitTags.of("ground", "builder");
        CustomTagList missing = UnitTags.of("air");
        require(UnitTags.containsAll(available, required),
                "required unit tags were not recognized as a subset");
        require(!UnitTags.containsAll(required, available),
                "unit tag subset direction was reversed");
        require(!UnitTags.containsAll(available, missing),
                "missing unit tag was reported as available");
        require(UnitTags.anyMatches(available, UnitTags.of("air", "builder")),
                "unit tag intersection was not detected");
        require(UnitTags.names(available).size() == 3,
                "unit tag name snapshot had the wrong size");
    }

    private static void verifyExtensionCancellationAggregation() {
        AtomicInteger calls = new AtomicInteger();
        RustedFabricEvent.Registration tagFirst = UnitTagEvents.BEFORE_SET.subscribe(
                (unit, current, replacement, skipRefresh) -> {
                    calls.incrementAndGet();
                    return true;
                });
        RustedFabricEvent.Registration tagSecond = UnitTagEvents.BEFORE_SET.subscribe(
                (unit, current, replacement, skipRefresh) -> {
                    calls.incrementAndGet();
                    return false;
                });
        require(UnitTagEvents.BEFORE_SET.invoker()
                        .beforeSet(null, null, null, false),
                "unit tag cancellation was not aggregated");
        require(calls.get() == 2, "unit tag cancellation skipped a listener");
        tagFirst.close();
        tagSecond.close();

        RustedFabricEvent.Registration attachment = AttachmentEvents.BEFORE_ATTACH.subscribe(
                (parent, child, slot) -> true);
        require(AttachmentEvents.BEFORE_ATTACH.invoker().beforeAttach(null, null, null),
                "attachment cancellation was not propagated");
        attachment.close();

        RustedFabricEvent.Registration trigger = CustomUnitTriggerEvents.BEFORE_TRIGGER.subscribe(
                (unit, eventType) -> true);
        require(CustomUnitTriggerEvents.BEFORE_TRIGGER.invoker().beforeTrigger(null, null),
                "custom-unit trigger cancellation was not propagated");
        trigger.close();

        AtomicInteger operationCalls = new AtomicInteger();
        RustedFabricEvent.Registration operationFirst =
                CustomUnitOperationEvents.BEFORE_EVENT.subscribe(
                        context -> operationCalls.incrementAndGet());
        RustedFabricEvent.Registration operationSecond =
                CustomUnitOperationEvents.BEFORE_EVENT.subscribe(
                        context -> operationCalls.incrementAndGet());
        CustomUnitOperationEvents.BEFORE_EVENT.invoker().beforeEvent(null);
        require(operationCalls.get() == 2,
                "mutable custom-unit operation event skipped a listener");
        operationFirst.close();
        operationSecond.close();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
