package io.github.endx.rustedfabricexample;

import io.github.endx.rustedfabricapi.api.event.CommandEvents;
import io.github.endx.rustedfabricapi.api.event.AudioRuntimeEvents;
import io.github.endx.rustedfabricapi.api.event.BuildQueueEvents;
import io.github.endx.rustedfabricapi.api.event.CustomAssetEvents;
import io.github.endx.rustedfabricapi.api.event.CustomUnitEvents;
import io.github.endx.rustedfabricapi.api.event.CustomUnitLifecycleEvents;
import io.github.endx.rustedfabricapi.api.event.CustomUnitRenderEvents;
import io.github.endx.rustedfabricapi.api.event.CustomUnitRuntimeEvents;
import io.github.endx.rustedfabricapi.api.event.FileSystemEvents;
import io.github.endx.rustedfabricapi.api.event.GameLifecycleEvents;
import io.github.endx.rustedfabricapi.api.event.MapDiscoveryEvents;
import io.github.endx.rustedfabricapi.api.event.MapMissionEvents;
import io.github.endx.rustedfabricapi.api.event.MapSpawnEvents;
import io.github.endx.rustedfabricapi.api.event.NetworkHandshakeEvents;
import io.github.endx.rustedfabricapi.api.event.RepairReclaimEvents;
import io.github.endx.rustedfabricapi.api.event.ResourceRuntimeEvents;
import io.github.endx.rustedfabricapi.api.event.RustedCustomUnitRegistryEvents;
import io.github.endx.rustedfabricapi.api.event.RustedIniEvents;
import io.github.endx.rustedfabricapi.api.event.SaveSyncEvents;
import io.github.endx.rustedfabricapi.api.event.SelectionEvents;
import io.github.endx.rustedfabricapi.api.event.TransportEvents;
import io.github.endx.rustedfabricapi.api.event.UiScriptEvents;
import io.github.endx.rustedfabricapi.api.event.UnitDamageEvents;
import io.github.endx.rustedfabricapi.api.event.UnitLifecycleEvents;
import io.github.endx.rustedfabricapi.api.diagnostic.AudioRuntimeDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.BuildQueueDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.CommandDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.CustomUnitDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.NetworkRuntimeDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.ResourceEconomyDiagnostics;
import io.github.endx.rustedfabricapi.api.diagnostic.UnitRuntimeDiagnostics;
import io.github.endx.rustedfabricapi.api.ini.RustedIniDiagnostics;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.endx.rustedfabricexample.ExampleDebugOverlay.*;

final class ExampleEventProbes {
    private static final AtomicBoolean MAP_ENTRY_MESSAGE_REGISTERED = new AtomicBoolean();
    private static final AtomicBoolean EVENT_PROBE_MESSAGES_REGISTERED = new AtomicBoolean();
    private static volatile Object lastAudioObject;

    private ExampleEventProbes() {
    }

    static void registerMapEntryMessage(String stage) {
        ExampleDebugOverlay.registerRenderer(stage);

        if (!MAP_ENTRY_MESSAGE_REGISTERED.compareAndSet(false, true)) {
            return;
        }

        GameLifecycleEvents.AFTER_MAP_SETUP.register((minimap, map, fogEnabled) -> showMapEntryMessage(stage, map));
        ExampleMod.log("registered map entry message hook from " + stage);
    }

    static void registerEventProbeMessages(String stage) {
        ExampleDebugOverlay.registerRenderer(stage);

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

        RustedIniEvents.BEFORE_PARSE_STREAM.register(context ->
                showEventProbeMessage(stage, "BeforeParseStream",
                        "BeforeParseStream unit=" + safeText(context.unitId())
                                + " root=" + safeText(context.resourceRoot()),
                        context.modInfo(), 750L));

        RustedIniEvents.AFTER_PARSE_UNIT_CONFIG.register((unitConfig, inputStream) ->
                showEventProbeMessage(stage, "AfterParseUnitConfig",
                        "AfterParseUnitConfig config=" + describeObject(unitConfig),
                        unitConfig, 750L));

        RustedIniEvents.BEFORE_COPY_FROM.register((metadata, targetConfig, sourceConfig, copyFromPath, recursionDepth) -> {
            showEventProbeMessage(stage, "BeforeCopyFrom",
                    "BeforeCopyFrom depth=" + recursionDepth
                            + " path=" + safeText(copyFromPath),
                    metadata, 750L);
            return false;
        });

        RustedIniEvents.AFTER_COPY_FROM.register((metadata, targetConfig, sourceConfig, copyFromPath, recursionDepth) ->
                showEventProbeMessage(stage, "AfterCopyFrom",
                        "AfterCopyFrom depth=" + recursionDepth
                                + " path=" + safeText(copyFromPath),
                        metadata, 750L));

        RustedIniEvents.BEFORE_STATIC_VARIABLES.register((metadata, unitConfig) -> {
            showEventProbeMessage(stage, "BeforeStaticVariables",
                    "BeforeStaticVariables metadata=" + describeObject(metadata),
                    metadata, 750L);
            return false;
        });

        RustedIniEvents.AFTER_STATIC_VARIABLES.register((metadata, unitConfig) ->
                showEventProbeMessage(stage, "AfterStaticVariables",
                        "AfterStaticVariables metadata=" + describeObject(metadata),
                        metadata, 750L));

        RustedIniEvents.AFTER_KEY_READ.register(context ->
                showEventProbeMessage(stage, "AfterIniKeyRead."
                                + safeText(context.section()) + "." + safeText(context.key()),
                        "AfterIniKeyRead " + safeText(context.section()) + "." + safeText(context.key())
                                + " type=" + safeText(context.valueType())
                                + " raw=" + safeText(String.valueOf(context.rawValue())),
                        context.unitConfig(), 1000L));

        RustedIniEvents.BEFORE_UNUSED_KEY_CHECK.register(unitConfig ->
                showEventProbeMessage(stage, "BeforeUnusedKeyCheck",
                        "BeforeUnusedKeyCheck config=" + describeObject(unitConfig),
                        unitConfig, 1000L));

        RustedIniEvents.AFTER_UNUSED_KEY_CHECK.register(unitConfig ->
                showEventProbeMessage(stage, "AfterUnusedKeyCheck",
                        "AfterUnusedKeyCheck trace=" + RustedIniDiagnostics.isKeyReadTracingEnabled()
                                + " config=" + describeObject(unitConfig),
                        unitConfig, 1000L));

        UiScriptEvents.BEFORE_PASSWORD_PROMPT_POPUP.register((controller, passwordPrompt) ->
                showEventProbeMessage(stage, "BeforePasswordPromptPopup",
                        "BeforePasswordPromptPopup " + describePasswordPrompt(passwordPrompt),
                        passwordPrompt, 1000L));

        UiScriptEvents.AFTER_PASSWORD_PROMPT_POPUP_QUEUED.register((controller, passwordPrompt) ->
                showEventProbeMessage(stage, "AfterPasswordPromptPopupQueued",
                        "AfterPasswordPromptPopupQueued " + describePasswordPrompt(passwordPrompt),
                        passwordPrompt, 1000L));

        UiScriptEvents.AFTER_UI_DOCUMENT_LOADED.register((uiEngine, document) ->
                showEventProbeMessage(stage, "AfterUiDocumentLoaded",
                        "AfterUiDocumentLoaded doc=" + describeObject(document),
                        document, 1000L));

        UiScriptEvents.AFTER_UI_DOCUMENT_SHOWN.register((uiEngine, document) ->
                showEventProbeMessage(stage, "AfterUiDocumentShown",
                        "AfterUiDocumentShown doc=" + describeObject(document),
                        document, 1000L));

        NetworkHandshakeEvents.BEFORE_SEND_PRE_REGISTER_INFO_REQUEST.register((networkEngine, connection) ->
                showNetworkHandshakeProbe(stage, "BeforeSendPreRegisterInfoRequest", networkEngine, connection));

        NetworkHandshakeEvents.AFTER_SEND_PRE_REGISTER_INFO_REQUEST.register((networkEngine, connection) ->
                showNetworkHandshakeProbe(stage, "AfterSendPreRegisterInfoRequest", networkEngine, connection));

        NetworkHandshakeEvents.BEFORE_SEND_PRE_REGISTER_INFO.register((networkEngine, connection) ->
                showNetworkHandshakeProbe(stage, "BeforeSendPreRegisterInfo", networkEngine, connection));

        NetworkHandshakeEvents.AFTER_SEND_PRE_REGISTER_INFO.register((networkEngine, connection) ->
                showNetworkHandshakeProbe(stage, "AfterSendPreRegisterInfo", networkEngine, connection));

        NetworkHandshakeEvents.BEFORE_SEND_REGISTER_CONNECTION.register((networkEngine, connection) ->
                showNetworkHandshakeProbe(stage, "BeforeSendRegisterConnection", networkEngine, connection));

        NetworkHandshakeEvents.AFTER_SEND_REGISTER_CONNECTION.register((networkEngine, connection) ->
                showNetworkHandshakeProbe(stage, "AfterSendRegisterConnection", networkEngine, connection));

        NetworkHandshakeEvents.BEFORE_SEND_SERVER_INFO.register((networkEngine, connection) ->
                showNetworkHandshakeProbe(stage, "BeforeSendServerInfo", networkEngine, connection));

        NetworkHandshakeEvents.AFTER_SEND_SERVER_INFO.register((networkEngine, connection) ->
                showNetworkHandshakeProbe(stage, "AfterSendServerInfo", networkEngine, connection));

        NetworkHandshakeEvents.BEFORE_SEND_INCORRECT_PASSWORD.register((networkEngine, connection) ->
                showNetworkHandshakeProbe(stage, "BeforeSendIncorrectPassword", networkEngine, connection));

        NetworkHandshakeEvents.AFTER_SEND_INCORRECT_PASSWORD.register((networkEngine, connection) ->
                showNetworkHandshakeProbe(stage, "AfterSendIncorrectPassword", networkEngine, connection));

        NetworkHandshakeEvents.BEFORE_SEND_KICK.register((networkEngine, connection, reason) ->
                showNetworkHandshakeProbe(stage, "BeforeSendKick",
                        "reason=" + safeText(reason), networkEngine, connection));

        NetworkHandshakeEvents.AFTER_SEND_KICK.register((networkEngine, connection, reason) ->
                showNetworkHandshakeProbe(stage, "AfterSendKick",
                        "reason=" + safeText(reason), networkEngine, connection));

        NetworkHandshakeEvents.BEFORE_SEND_UPDATE_PLAYER.register((networkEngine, connection) ->
                showNetworkHandshakeProbe(stage, "BeforeSendUpdatePlayer", networkEngine, connection));

        NetworkHandshakeEvents.AFTER_SEND_UPDATE_PLAYER.register((networkEngine, connection) ->
                showNetworkHandshakeProbe(stage, "AfterSendUpdatePlayer", networkEngine, connection));

        RustedCustomUnitRegistryEvents.AFTER_METADATA_PARSED.register((context, metadata) -> {
            showEventProbeMessage(stage, "AfterMetadataParsed",
                    "AfterMetadataParsed unit=" + safeText(context.unitId())
                            + " metadata=" + describeObject(metadata)
                            + " repair=" + describeRepairMetadata(metadata)
                            + " transport=" + describeTransportMetadata(metadata),
                    metadata, 750L);
            return metadata;
        });

        RustedCustomUnitRegistryEvents.BEFORE_PENDING_REGISTER.register((metadata, pendingSnapshot) -> {
            showEventProbeMessage(stage, "BeforePendingRegister",
                    "BeforePendingRegister pending=" + pendingSnapshot.size()
                            + " metadata=" + describeObject(metadata),
                    metadata, 750L);
            return metadata;
        });

        RustedCustomUnitRegistryEvents.AFTER_PENDING_REGISTER.register((metadata, pendingSize) ->
                showEventProbeMessage(stage, "AfterPendingRegister",
                        "AfterPendingRegister pending=" + pendingSize
                                + " metadata=" + describeObject(metadata),
                        metadata, 750L));

        RustedCustomUnitRegistryEvents.BEFORE_COMMIT.register((pendingSnapshot, includeDisabledMods) -> {
            showEventProbeMessage(stage, "BeforeCustomUnitCommit",
                    "BeforeCustomUnitCommit pending=" + pendingSnapshot.size()
                            + " includeDisabled=" + includeDisabledMods,
                    null, 750L);
            return false;
        });

        RustedCustomUnitRegistryEvents.AFTER_COMMIT.register((activeSnapshot, result, replacementMap) ->
                showEventProbeMessage(stage, "AfterCustomUnitCommit",
                        "AfterCustomUnitCommit active=" + activeSnapshot.size()
                                + " replacements=" + replacementMap.size()
                                + " result=" + safeText(result),
                        null, 750L));

        RustedCustomUnitRegistryEvents.AFTER_REBUILD_LINKS.register((activeSnapshot, replacementMap) ->
                showEventProbeMessage(stage, "AfterRebuildCustomUnitLinks",
                        "AfterRebuildCustomUnitLinks active=" + activeSnapshot.size()
                                + " replacements=" + replacementMap.size(),
                        null, 750L));

        RustedCustomUnitRegistryEvents.AFTER_VALIDATE_LINKS.register((strict, currentResult) -> {
            showEventProbeMessage(stage, "AfterValidateCustomUnitLinks",
                    "AfterValidateCustomUnitLinks strict=" + strict
                            + " result=" + currentResult,
                    null, 750L);
            return currentResult;
        });

        CustomAssetEvents.BEFORE_LOAD_IMAGE.register((path, basePath, smooth, metadata, section, key) -> {
            showEventProbeMessage(stage, "BeforeLoadImage",
                    "BeforeLoadImage key=" + safeText(section) + "." + safeText(key)
                            + " path=" + compactPath(path)
                            + " smooth=" + smooth,
                    metadata, 1000L);
            return false;
        });

        CustomAssetEvents.AFTER_LOAD_IMAGE.register((path, basePath, smooth, metadata, section, key, image) -> {
            showEventProbeMessage(stage, "AfterLoadImage",
                    "AfterLoadImage key=" + safeText(section) + "." + safeText(key)
                            + " image=" + describeObject(image),
                    image, 1000L);
            return image;
        });

        CustomAssetEvents.AFTER_CREATE_TEAM_COLOR_IMAGES.register((metadata, sourceImage, teamColoringMode, images) -> {
            showEventProbeMessage(stage, "AfterCreateTeamColorImages",
                    "AfterCreateTeamColorImages mode=" + describeObject(teamColoringMode)
                            + " images=" + describeObject(images),
                    metadata, 1500L);
            return images;
        });

        CustomAssetEvents.BEFORE_LOAD_SOUND.register((basePath, soundPath, metadata) -> {
            showEventProbeMessage(stage, "BeforeLoadSound",
                    "BeforeLoadSound path=" + compactPath(soundPath),
                    metadata, 1000L);
            return false;
        });

        CustomAssetEvents.AFTER_LOAD_SOUND.register((basePath, soundPath, metadata, sound) -> {
            showEventProbeMessage(stage, "AfterLoadSound",
                    "AfterLoadSound path=" + compactPath(soundPath)
                            + " sound=" + describeObject(sound),
                    sound, 1000L);
            return sound;
        });

        CustomAssetEvents.AFTER_PARSE_SOUND_LIST.register((metadata, rawSoundList, soundList) -> {
            showEventProbeMessage(stage, "AfterParseSoundList",
                    "AfterParseSoundList raw=" + safeText(rawSoundList)
                            + " result=" + describeObject(soundList),
                    soundList, 1500L);
            return soundList;
        });

        AudioRuntimeEvents.AFTER_LOAD_SOUND_FROM_STREAM.register((factory, name, inputStream, strict, sound) -> {
            rememberAudioObject(sound != null ? sound : factory);
            showEventProbeMessage(stage, "AfterLoadSoundFromStream",
                    "AfterLoadSoundFromStream name=" + compactPath(name)
                            + " strict=" + strict
                            + " sound=" + describeAudioObject(sound),
                    sound, 1000L);
            return sound;
        });

        AudioRuntimeEvents.AFTER_LOAD_BUILTIN_SOUND.register((factory, resourceId, sound) -> {
            rememberAudioObject(sound != null ? sound : factory);
            showEventProbeMessage(stage, "AfterLoadBuiltinSound",
                    "AfterLoadBuiltinSound id=" + resourceId
                            + " sound=" + describeAudioObject(sound),
                    sound, 1000L);
            return sound;
        });

        AudioRuntimeEvents.AFTER_OPENAL_NEW_SOUND.register((audio, fileHandle, sound) -> {
            rememberAudioObject(sound != null ? sound : audio);
            showEventProbeMessage(stage, "AfterOpenALNewSound",
                    "AfterOpenALNewSound file=" + describeAudioObject(fileHandle)
                            + " sound=" + describeAudioObject(sound),
                    sound, 1000L);
            return sound;
        });

        AudioRuntimeEvents.AFTER_OPENAL_NEW_MUSIC.register((audio, fileHandle, music) -> {
            rememberAudioObject(music != null ? music : audio);
            showEventProbeMessage(stage, "AfterOpenALNewMusic",
                    "AfterOpenALNewMusic file=" + describeAudioObject(fileHandle)
                            + " music=" + describeAudioObject(music),
                    music, 1000L);
            return music;
        });

        AudioRuntimeEvents.BEFORE_GAME_SOUND_PLAY.register((gameSound, leftVolume, rightVolume, priority, loop, pitch) -> {
            rememberAudioObject(gameSound);
            showEventProbeMessage(stage, "BeforeGameSoundPlay",
                    "BeforeGameSoundPlay vol=" + formatFloat(leftVolume) + "/" + formatFloat(rightVolume)
                            + " pitch=" + formatFloat(pitch)
                            + " loop=" + loop
                            + " sound=" + describeAudioObject(gameSound),
                    gameSound, 300L);
            return false;
        });

        AudioRuntimeEvents.AFTER_GAME_SOUND_PLAY_NOW.register((gameSound, leftVolume, rightVolume, priority, loop, pitch) -> {
            rememberAudioObject(gameSound);
            showEventProbeMessage(stage, "AfterGameSoundPlayNow",
                    "AfterGameSoundPlayNow vol=" + formatFloat(leftVolume) + "/" + formatFloat(rightVolume)
                            + " pitch=" + formatFloat(pitch)
                            + " loop=" + loop
                            + " sound=" + describeAudioObject(gameSound),
                    gameSound, 300L);
        });

        AudioRuntimeEvents.AFTER_SOUND_PLAY_TASK_RUN.register(playTask -> {
            rememberAudioObject(playTask);
            showEventProbeMessage(stage, "AfterSoundPlayTaskRun",
                    "AfterSoundPlayTaskRun " + describeAudioObject(playTask),
                    playTask, 300L);
        });

        AudioRuntimeEvents.AFTER_MUSIC_TRACK_LOAD.register((factory, path, track) -> {
            rememberAudioObject(track != null ? track : factory);
            showEventProbeMessage(stage, "AfterMusicTrackLoad",
                    "AfterMusicTrackLoad path=" + compactPath(path)
                            + " track=" + describeAudioObject(track),
                    track, 1500L);
            return track;
        });

        AudioRuntimeEvents.AFTER_NEW_MUSIC_PLAYER.register((factory, player) -> {
            rememberAudioObject(player != null ? player : factory);
            showEventProbeMessage(stage, "AfterNewMusicPlayer",
                    "AfterNewMusicPlayer player=" + describeAudioObject(player),
                    player, 1500L);
            return player;
        });

        AudioRuntimeEvents.AFTER_MUSIC_PLAYER_SET_TRACK.register((player, track) -> {
            rememberAudioObject(player);
            showEventProbeMessage(stage, "AfterMusicPlayerSetTrack",
                    "AfterMusicPlayerSetTrack player=" + describeAudioObject(player)
                            + " track=" + describeAudioObject(track),
                    player, 1000L);
        });

        AudioRuntimeEvents.AFTER_MUSIC_PLAYER_QUEUE_PLAY.register((player, loop) -> {
            rememberAudioObject(player);
            showEventProbeMessage(stage, "AfterMusicPlayerQueuePlay",
                    "AfterMusicPlayerQueuePlay loop=" + loop
                            + " player=" + describeAudioObject(player),
                    player, 1000L);
        });

        AudioRuntimeEvents.AFTER_MUSIC_PLAYER_CONTROL.register((player, operation) -> {
            rememberAudioObject(player);
            showEventProbeMessage(stage, "AfterMusicPlayerControl." + safeText(operation),
                    "AfterMusicPlayerControl op=" + safeText(operation)
                            + " player=" + describeAudioObject(player),
                    player, 1000L);
        });

        CustomAssetEvents.AFTER_PARSE_PROJECTILE_SPAWN_LIST.register((metadata, rawList, section, key, requireSingle, projectileSpawnList) -> {
            showEventProbeMessage(stage, "AfterParseProjectileSpawnList",
                    "AfterParseProjectileSpawnList " + safeText(section) + "." + safeText(key)
                            + " single=" + requireSingle
                            + " raw=" + safeText(rawList)
                            + " result=" + describeObject(projectileSpawnList),
                    projectileSpawnList, 1500L);
            return projectileSpawnList;
        });

        FileSystemEvents.AFTER_RESOLVE_ABSTRACT_PATH.register((path, resolvedPath) -> {
            showEventProbeMessage(stage, "AfterResolveAbstractPath." + safeText(path),
                    "AfterResolveAbstractPath " + compactPath(path)
                            + " -> " + compactPath(resolvedPath),
                    null, 5000L);
            return resolvedPath;
        });

        FileSystemEvents.AFTER_OPEN_ASSET_CACHED.register((source, key, inputStream) -> {
            showEventProbeMessage(stage, "AfterOpenAssetCached." + safeText(source) + "." + safeText(key),
                    "AfterOpenAssetCached source=" + compactPath(source)
                            + " key=" + compactPath(key)
                            + " stream=" + describeObject(inputStream),
                    inputStream, 2000L);
            return inputStream;
        });

        FileSystemEvents.AFTER_LIST_CACHED_ASSET_DIRECTORY.register((source, key, entries) -> {
            showEventProbeMessage(stage, "AfterListCachedAssetDirectory." + safeText(source) + "." + safeText(key),
                    "AfterListCachedAssetDirectory source=" + compactPath(source)
                            + " key=" + compactPath(key)
                            + " entries=" + countArray(entries),
                    null, 2000L);
            return entries;
        });

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

        UnitDamageEvents.BEFORE_UNIT_APPLY_DAMAGE.register((unit, attacker, amount, projectile) -> {
            boolean blocked = isInvincibleUnitsEnabled();
            showEventProbeMessage(stage, "BeforeUnitApplyDamage",
                    "BeforeUnitApplyDamage amount=" + formatFloat(amount)
                            + (blocked ? " blocked" : "")
                            + " unit=" + describeObject(unit)
                            + " attacker=" + describeObject(attacker),
                    unit, 300L);
            return blocked;
        });

        UnitDamageEvents.AFTER_UNIT_APPLY_DAMAGE.register((unit, attacker, amount, projectile, appliedAmount) ->
                showEventProbeMessage(stage, "AfterUnitApplyDamage",
                        "AfterUnitApplyDamage amount=" + formatFloat(amount)
                                + " applied=" + formatFloat(appliedAmount)
                                + " unit=" + describeObject(unit),
                        unit, 300L));

        UnitDamageEvents.MODIFY_UNIT_DAMAGE_IMMUNITY.register((unit, currentResult) -> {
            showEventProbeMessage(stage, "ModifyUnitDamageImmunity",
                    "ModifyUnitDamageImmunity current=" + currentResult
                            + " unit=" + describeObject(unit),
                    unit, 500L);
            return null;
        });

        UnitDamageEvents.BEFORE_UNIT_DEATH_SEQUENCE.register(unit -> {
            showEventProbeMessage(stage, "BeforeUnitDeathSequence",
                    "BeforeUnitDeathSequence unit=" + describeObject(unit),
                    unit, 300L);
            return false;
        });

        UnitDamageEvents.AFTER_UNIT_DEATH_SEQUENCE.register(unit ->
                showEventProbeMessage(stage, "AfterUnitDeathSequence",
                        "AfterUnitDeathSequence unit=" + describeObject(unit),
                        unit, 300L));

        UnitDamageEvents.MODIFY_UNIT_DEATH_EFFECTS_RESULT.register((unit, currentKeepObject) -> {
            showEventProbeMessage(stage, "ModifyUnitDeathEffectsResult",
                    "ModifyUnitDeathEffectsResult keep=" + currentKeepObject
                            + " unit=" + describeObject(unit),
                    unit, 300L);
            return null;
        });

        RepairReclaimEvents.BEFORE_REPAIR_RECLAIM_ORDER_UPDATE.register((unit, delta, waypoint, waypointState) -> {
            showEventProbeMessage(stage, "BeforeRepairReclaimOrderUpdate",
                    "BeforeRepairReclaimOrderUpdate delta=" + formatFloat(delta)
                            + " unit=" + describeObject(unit)
                            + " waypoint=" + describeObject(waypoint)
                            + " state=" + describeObject(waypointState),
                    unit, 500L);
            return false;
        });

        RepairReclaimEvents.AFTER_REPAIR_RECLAIM_ORDER_UPDATE.register((unit, delta, waypoint, waypointState) ->
                showEventProbeMessage(stage, "AfterRepairReclaimOrderUpdate",
                        "AfterRepairReclaimOrderUpdate delta=" + formatFloat(delta)
                                + " unit=" + describeObject(unit)
                                + " activeDelta=" + describeActiveResourceDelta(unit),
                        unit, 500L));

        RepairReclaimEvents.MODIFY_CAN_REPAIR_TARGET.register((unit, target, currentResult) -> {
            showEventProbeMessage(stage, "ModifyCanRepairTarget",
                    "ModifyCanRepairTarget result=" + currentResult
                            + " unit=" + describeObject(unit)
                            + " target=" + describeObject(target),
                    unit, 750L);
            return null;
        });

        RepairReclaimEvents.MODIFY_CAN_RECLAIM_UNIT_TARGET.register((unit, target, currentResult) -> {
            showEventProbeMessage(stage, "ModifyCanReclaimUnitTarget",
                    "ModifyCanReclaimUnitTarget result=" + currentResult
                            + " unit=" + describeObject(unit)
                            + " target=" + describeObject(target),
                    unit, 750L);
            return null;
        });

        RepairReclaimEvents.MODIFY_BUILD_PROGRESS_SPEED.register((unit, target, currentSpeed) -> {
            showEventProbeMessage(stage, "ModifyBuildProgressSpeed",
                    "ModifyBuildProgressSpeed speed=" + formatFloat(currentSpeed)
                            + " unit=" + describeObject(unit)
                            + " target=" + describeObject(target),
                    unit, 750L);
            return null;
        });

        RepairReclaimEvents.MODIFY_UNBUILD_SPEED.register((unit, target, currentSpeed) -> {
            showEventProbeMessage(stage, "ModifyUnbuildSpeed",
                    "ModifyUnbuildSpeed speed=" + formatFloat(currentSpeed)
                            + " unit=" + describeObject(unit)
                            + " target=" + describeObject(target),
                    unit, 750L);
            return null;
        });

        RepairReclaimEvents.MODIFY_BUILD_PRICE_FOR_TARGET.register((unit, target, currentPrice) -> {
            showEventProbeMessage(stage, "ModifyBuildPriceForTarget",
                    "ModifyBuildPriceForTarget price=" + describeObject(currentPrice)
                            + " unit=" + describeObject(unit)
                            + " target=" + describeObject(target),
                    unit, 1000L);
            return currentPrice;
        });

        RepairReclaimEvents.MODIFY_BASE_RECLAIM_PRICE.register((unit, currentPrice) -> {
            showEventProbeMessage(stage, "ModifyBaseReclaimPrice",
                    "ModifyBaseReclaimPrice price=" + describeObject(currentPrice)
                            + " unit=" + describeObject(unit),
                    unit, 1000L);
            return currentPrice;
        });

        RepairReclaimEvents.MODIFY_RECLAIM_PRICE_OVERRIDE.register((unit, currentPrice) -> {
            showEventProbeMessage(stage, "ModifyReclaimPriceOverride",
                    "ModifyReclaimPriceOverride price=" + describeObject(currentPrice)
                            + " unit=" + describeObject(unit),
                    unit, 1000L);
            return currentPrice;
        });

        RepairReclaimEvents.MODIFY_SIMILAR_RESOURCES_TAG.register((unit, currentTags) -> {
            showEventProbeMessage(stage, "ModifySimilarResourcesTag",
                    "ModifySimilarResourcesTag tags=" + describeObject(currentTags)
                            + " unit=" + describeObject(unit),
                    unit, 1000L);
            return currentTags;
        });

        RepairReclaimEvents.BEFORE_CONSTRUCTION_PROGRESS_SET.register((unit, progress) -> {
            showEventProbeMessage(stage, "BeforeConstructionProgressSet",
                    "BeforeConstructionProgressSet progress=" + formatFloat(progress)
                            + " unit=" + describeObject(unit),
                    unit, 750L);
            return false;
        });

        RepairReclaimEvents.AFTER_CONSTRUCTION_PROGRESS_SET.register((unit, progress) ->
                showEventProbeMessage(stage, "AfterConstructionProgressSet",
                        "AfterConstructionProgressSet progress=" + formatFloat(progress)
                                + " unit=" + describeObject(unit),
                        unit, 750L));

        RepairReclaimEvents.AFTER_ACTIVE_RESOURCE_DELTA_REFRESH.register(unit ->
                showEventProbeMessage(stage, "AfterActiveResourceDeltaRefresh",
                        "AfterActiveResourceDeltaRefresh unit=" + describeObject(unit)
                                + " activeDelta=" + describeActiveResourceDelta(unit),
                        unit, 1000L));

        RepairReclaimEvents.MODIFY_BUILD_QUEUE_RESOURCE_DELTA.register((unit, currentDelta) -> {
            showEventProbeMessage(stage, "ModifyBuildQueueResourceDelta",
                    "ModifyBuildQueueResourceDelta delta=" + describeObject(currentDelta)
                            + " unit=" + describeObject(unit),
                    unit, 1000L);
            return currentDelta;
        });

        RepairReclaimEvents.MODIFY_REPAIR_RECLAIM_RESOURCE_DELTA.register((unit, currentDelta) -> {
            showEventProbeMessage(stage, "ModifyRepairReclaimResourceDelta",
                    "ModifyRepairReclaimResourceDelta delta=" + describeObject(currentDelta)
                            + " unit=" + describeObject(unit),
                    unit, 1000L);
            return currentDelta;
        });

        RepairReclaimEvents.MODIFY_NEAREST_RECLAIM_RESOURCE_TARGET.register((searcher, x, y, range, requiredTags, currentTarget) -> {
            showEventProbeMessage(stage, "ModifyNearestReclaimResourceTarget",
                    "ModifyNearestReclaimResourceTarget range=" + formatFloat(range)
                            + " pos=" + formatPoint(x, y)
                            + " tags=" + describeObject(requiredTags)
                            + " target=" + describeObject(currentTarget),
                    searcher, 1000L);
            return currentTarget;
        });

        CustomUnitLifecycleEvents.BEFORE_RUNTIME_UNIT_CREATE.register(metadata ->
                showEventProbeMessage(stage, "BeforeRuntimeUnitCreate",
                        "BeforeRuntimeUnitCreate metadata=" + describeObject(metadata),
                        metadata, 750L));

        CustomUnitLifecycleEvents.AFTER_RUNTIME_UNIT_CREATE.register((metadata, unit) -> {
            showEventProbeMessage(stage, "AfterRuntimeUnitCreate",
                    "AfterRuntimeUnitCreate unit=" + describeObject(unit)
                            + " metadata=" + describeObject(metadata),
                    unit, 750L);
            return unit;
        });

        CustomUnitLifecycleEvents.AFTER_RUNTIME_UNIT_CREATE_WITH_FLAG.register((metadata, createFlag, unit) -> {
            showEventProbeMessage(stage, "AfterRuntimeUnitCreateWithFlag",
                    "AfterRuntimeUnitCreateWithFlag flag=" + createFlag
                            + " unit=" + describeObject(unit),
                    unit, 750L);
            return unit;
        });

        CustomUnitLifecycleEvents.BEFORE_UNIT_METADATA_APPLY.register((unit, oldMetadata, newMetadata, conversion, initial, statOverrides) ->
                showEventProbeMessage(stage, "BeforeUnitMetadataApply",
                        "BeforeUnitMetadataApply conversion=" + conversion
                                + " initial=" + initial
                                + " old=" + describeObject(oldMetadata)
                                + " new=" + describeObject(newMetadata),
                        unit, 750L));

        CustomUnitLifecycleEvents.AFTER_UNIT_METADATA_APPLY.register((unit, oldMetadata, newMetadata, conversion, initial, statOverrides) ->
                showEventProbeMessage(stage, "AfterUnitMetadataApply",
                        "AfterUnitMetadataApply conversion=" + conversion
                                + " initial=" + initial
                                + " unit=" + describeObject(unit),
                        unit, 750L));

        CustomUnitLifecycleEvents.BEFORE_CUSTOM_UNIT_KILLED.register(unit -> {
            showEventProbeMessage(stage, "BeforeCustomUnitKilled",
                    "BeforeCustomUnitKilled unit=" + describeObject(unit),
                    unit, 750L);
            return false;
        });

        CustomUnitLifecycleEvents.BEFORE_CUSTOM_UNIT_REMOVED.register(unit -> {
            showEventProbeMessage(stage, "BeforeCustomUnitRemoved",
                    "BeforeCustomUnitRemoved unit=" + describeObject(unit),
                    unit, 750L);
            return false;
        });

        CustomUnitRenderEvents.AFTER_GET_BODY_IMAGE.register((unit, image) -> {
            showEventProbeMessage(stage, "AfterGetBodyImage",
                    "AfterGetBodyImage unit=" + describeObject(unit)
                            + " image=" + describeObject(image),
                    unit, 4000L);
            return image;
        });

        CustomUnitRenderEvents.AFTER_GET_ZOOMED_ICON_IMAGE.register((unit, image) -> {
            showEventProbeMessage(stage, "AfterGetZoomedIconImage",
                    "AfterGetZoomedIconImage image=" + describeObject(image),
                    unit, 4000L);
            return isDebugRenderPartEnabled(DebugRenderPart.ZOOM_ICON) ? image : null;
        });

        CustomUnitRenderEvents.AFTER_GET_SHADOW_IMAGE.register((unit, image) -> {
            showEventProbeMessage(stage, "AfterGetShadowImage",
                    "AfterGetShadowImage image=" + describeObject(image),
                    unit, 4000L);
            return isDebugRenderPartEnabled(DebugRenderPart.SHADOW_IMAGE) ? image : null;
        });

        CustomUnitRenderEvents.AFTER_GET_TURRET_IMAGE.register((unit, turretIndex, image) -> {
            showEventProbeMessage(stage, "AfterGetTurretImage",
                    "AfterGetTurretImage turret=" + turretIndex
                            + " image=" + describeObject(image),
                    unit, 4000L);
            return isDebugRenderPartEnabled(DebugRenderPart.TURRET_IMAGE) ? image : null;
        });

        CustomUnitRenderEvents.AFTER_GET_SHIELD_IMAGE.register((unit, image) -> {
            showEventProbeMessage(stage, "AfterGetShieldImage",
                    "AfterGetShieldImage image=" + describeObject(image),
                    unit, 4000L);
            return isDebugRenderPartEnabled(DebugRenderPart.SHIELD_IMAGE) ? image : null;
        });

        CustomUnitRenderEvents.BEFORE_DRAW_BACK_IMAGE.register((unit, renderDelta) -> {
            if (!isDebugRenderPartEnabled(DebugRenderPart.BACK_IMAGE)) {
                showEventProbeMessage(stage, "BeforeDrawBackImage",
                        "BeforeDrawBackImage cancelled by Java Debug",
                        unit, 1000L);
                return true;
            }
            return false;
        });

        CustomUnitRenderEvents.BEFORE_DRAW_OVERLAY.register((unit, renderDelta) -> {
            if (!isDebugRenderPartEnabled(DebugRenderPart.OVERLAY_LAYER)) {
                showEventProbeMessage(stage, "BeforeDrawOverlay",
                        "BeforeDrawOverlay cancelled by Java Debug",
                        unit, 1000L);
                return true;
            }
            return false;
        });

        CustomUnitRenderEvents.BEFORE_FRAME_SOURCE_RECT.register((unit, forShadow) -> {
            if (isDebugRenderPartEnabled(DebugRenderPart.FRAME_RECTS)) {
                showEventProbeMessage(stage, "BeforeFrameSourceRect",
                        "BeforeFrameSourceRect shadow=" + forShadow
                                + " unit=" + describeObject(unit),
                        unit, 4000L);
            }
        });

        CustomUnitRenderEvents.AFTER_FRAME_SOURCE_RECT.register((unit, forShadow, rect) -> {
            if (isDebugRenderPartEnabled(DebugRenderPart.FRAME_RECTS)) {
                showEventProbeMessage(stage, "AfterFrameSourceRect",
                        "AfterFrameSourceRect shadow=" + forShadow
                                + " rect=" + describeObject(rect),
                        unit, 4000L);
            }
            return rect;
        });

        CustomUnitRenderEvents.AFTER_IMAGE_DESTINATION_RECT.register((unit, rect) -> {
            if (isDebugRenderPartEnabled(DebugRenderPart.FRAME_RECTS)) {
                showEventProbeMessage(stage, "AfterImageDestinationRect",
                        "AfterImageDestinationRect rect=" + describeObject(rect),
                        unit, 4000L);
            }
            return rect;
        });

        CustomUnitRenderEvents.AFTER_TURRET_WORLD_TRANSFORM.register((unit, turretIndex, includeHeight, transform) -> {
            if (isDebugRenderPartEnabled(DebugRenderPart.TURRET_TRANSFORM)) {
                showEventProbeMessage(stage, "AfterTurretWorldTransform",
                        "AfterTurretWorldTransform turret=" + turretIndex
                                + " includeHeight=" + includeHeight
                                + " transform=" + describeObject(transform),
                        unit, 4000L);
            }
            return transform;
        });

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
                    "BeforeCommandIssue " + describeCommand(command),
                    command, 500L);
            return false;
        });

        CommandEvents.AFTER_COMMAND_ISSUE.register(command ->
                showEventProbeMessage(stage, "AfterCommandIssue",
                        "AfterCommandIssue " + describeCommand(command),
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

        TransportEvents.MODIFY_CAN_TRANSPORT_UNIT.register((carrier, candidate, allowPartial, currentResult) -> {
            showEventProbeMessage(stage, "ModifyCanTransportUnit",
                    "ModifyCanTransportUnit result=" + currentResult
                            + " partial=" + allowPartial
                            + " carrier=" + describeObject(carrier)
                            + " candidate=" + describeObject(candidate),
                    carrier, 1000L);
            return null;
        });

        TransportEvents.MODIFY_CAN_TRANSPORT_UNIT_IGNORING_CURRENT_CONTAINER.register((carrier, candidate, allowPartial, currentResult) -> {
            showEventProbeMessage(stage, "ModifyCanTransportUnitIgnoringCurrentContainer",
                    "ModifyCanTransportUnitIgnoringCurrentContainer result=" + currentResult
                            + " partial=" + allowPartial
                            + " carrier=" + describeObject(carrier)
                            + " candidate=" + describeObject(candidate),
                    carrier, 1000L);
            return null;
        });

        TransportEvents.BEFORE_TRY_ADD_UNIT_TO_TRANSPORT.register((carrier, candidate, allowPartial) -> {
            showEventProbeMessage(stage, "BeforeTryAddUnitToTransport",
                    "BeforeTryAddUnitToTransport partial=" + allowPartial
                            + " carrier=" + describeObject(carrier)
                            + " candidate=" + describeObject(candidate)
                            + " state=" + describeTransportState(carrier),
                    carrier, 750L);
            return false;
        });

        TransportEvents.AFTER_TRY_ADD_UNIT_TO_TRANSPORT.register((carrier, candidate, allowPartial, result) ->
                showEventProbeMessage(stage, "AfterTryAddUnitToTransport",
                        "AfterTryAddUnitToTransport result=" + result
                                + " partial=" + allowPartial
                                + " carrier=" + describeObject(carrier)
                                + " candidate=" + describeObject(candidate)
                                + " state=" + describeTransportState(carrier),
                        carrier, 750L));

        TransportEvents.BEFORE_ADD_UNIT_TO_TRANSPORT.register((carrier, transportedUnit) -> {
            showEventProbeMessage(stage, "BeforeAddUnitToTransport",
                    "BeforeAddUnitToTransport carrier=" + describeObject(carrier)
                            + " unit=" + describeObject(transportedUnit)
                            + " state=" + describeTransportState(carrier),
                    carrier, 750L);
            return false;
        });

        TransportEvents.AFTER_ADD_UNIT_TO_TRANSPORT.register((carrier, transportedUnit) ->
                showEventProbeMessage(stage, "AfterAddUnitToTransport",
                        "AfterAddUnitToTransport carrier=" + describeObject(carrier)
                                + " unit=" + describeObject(transportedUnit)
                                + " state=" + describeTransportState(carrier),
                        carrier, 750L));

        TransportEvents.BEFORE_REMOVE_UNIT_FROM_TRANSPORT.register((carrier, transportedUnit) -> {
            showEventProbeMessage(stage, "BeforeRemoveUnitFromTransport",
                    "BeforeRemoveUnitFromTransport carrier=" + describeObject(carrier)
                            + " unit=" + describeObject(transportedUnit),
                    carrier, 750L);
            return false;
        });

        TransportEvents.AFTER_REMOVE_UNIT_FROM_TRANSPORT.register((carrier, transportedUnit) ->
                showEventProbeMessage(stage, "AfterRemoveUnitFromTransport",
                        "AfterRemoveUnitFromTransport carrier=" + describeObject(carrier)
                                + " unit=" + describeObject(transportedUnit)
                                + " state=" + describeTransportState(carrier),
                        carrier, 750L));

        TransportEvents.MODIFY_HAS_TRANSPORT_CAPACITY.register((unit, currentResult) -> {
            showEventProbeMessage(stage, "ModifyHasTransportCapacity",
                    "ModifyHasTransportCapacity result=" + currentResult
                            + " unit=" + describeObject(unit)
                            + " state=" + describeTransportState(unit),
                    unit, 1500L);
            return null;
        });

        TransportEvents.MODIFY_TRANSPORT_SLOTS_NEEDED.register((unit, currentSlots) -> {
            showEventProbeMessage(stage, "ModifyTransportSlotsNeeded",
                    "ModifyTransportSlotsNeeded slots=" + currentSlots
                            + " unit=" + describeObject(unit),
                    unit, 1500L);
            return null;
        });

        TransportEvents.MODIFY_TRANSPORT_BAR_USED_SLOTS.register((unit, currentSlots) -> {
            showEventProbeMessage(stage, "ModifyTransportBarUsedSlots",
                    "ModifyTransportBarUsedSlots slots=" + currentSlots
                            + " unit=" + describeObject(unit),
                    unit, 1500L);
            return null;
        });

        TransportEvents.MODIFY_TRANSPORT_BAR_MAX_SLOTS.register((unit, currentSlots) -> {
            showEventProbeMessage(stage, "ModifyTransportBarMaxSlots",
                    "ModifyTransportBarMaxSlots slots=" + currentSlots
                            + " unit=" + describeObject(unit),
                    unit, 1500L);
            return null;
        });

        TransportEvents.MODIFY_TRANSPORTED_UNIT_COUNT.register((unit, currentCount) -> {
            showEventProbeMessage(stage, "ModifyTransportedUnitCount",
                    "ModifyTransportedUnitCount count=" + currentCount
                            + " unit=" + describeObject(unit),
                    unit, 1500L);
            return null;
        });

        TransportEvents.MODIFY_TRANSPORT_UNLOADING.register((unit, currentResult) -> {
            showEventProbeMessage(stage, "ModifyTransportUnloading",
                    "ModifyTransportUnloading result=" + currentResult
                            + " unit=" + describeObject(unit),
                    unit, 1500L);
            return null;
        });

        TransportEvents.MODIFY_CONTAINING_UNIT.register((unit, currentContainer) -> {
            showEventProbeMessage(stage, "ModifyContainingUnit",
                    "ModifyContainingUnit container=" + describeObject(currentContainer)
                            + " unit=" + describeObject(unit),
                    unit, 1500L);
            return currentContainer;
        });

        TransportEvents.MODIFY_ATTACHMENT_SLOT.register((unit, currentSlot) -> {
            showEventProbeMessage(stage, "ModifyAttachmentSlot",
                    "ModifyAttachmentSlot slot=" + describeObject(currentSlot)
                            + " details=" + describeAttachmentSlot(currentSlot),
                    unit, 1500L);
            return currentSlot;
        });

        TransportEvents.BEFORE_START_TRANSPORT_UNLOADING.register(unit -> {
            showEventProbeMessage(stage, "BeforeStartTransportUnloading",
                    "BeforeStartTransportUnloading unit=" + describeObject(unit)
                            + " state=" + describeTransportState(unit),
                    unit, 750L);
            return false;
        });

        TransportEvents.AFTER_START_TRANSPORT_UNLOADING.register(unit ->
                showEventProbeMessage(stage, "AfterStartTransportUnloading",
                        "AfterStartTransportUnloading unit=" + describeObject(unit)
                                + " state=" + describeTransportState(unit),
                        unit, 750L));

        TransportEvents.BEFORE_STOP_TRANSPORT_UNLOADING.register(unit -> {
            showEventProbeMessage(stage, "BeforeStopTransportUnloading",
                    "BeforeStopTransportUnloading unit=" + describeObject(unit)
                            + " state=" + describeTransportState(unit),
                    unit, 750L);
            return false;
        });

        TransportEvents.AFTER_STOP_TRANSPORT_UNLOADING.register(unit ->
                showEventProbeMessage(stage, "AfterStopTransportUnloading",
                        "AfterStopTransportUnloading unit=" + describeObject(unit)
                                + " state=" + describeTransportState(unit),
                        unit, 750L));

        TransportEvents.BEFORE_UNLOAD_NEXT_TRANSPORTED_UNIT.register((unit, forced) -> {
            showEventProbeMessage(stage, "BeforeUnloadNextTransportedUnit",
                    "BeforeUnloadNextTransportedUnit forced=" + forced
                            + " unit=" + describeObject(unit)
                            + " state=" + describeTransportState(unit),
                    unit, 750L);
            return false;
        });

        TransportEvents.AFTER_UNLOAD_NEXT_TRANSPORTED_UNIT.register((unit, forced, result) ->
                showEventProbeMessage(stage, "AfterUnloadNextTransportedUnit",
                        "AfterUnloadNextTransportedUnit result=" + result
                                + " forced=" + forced
                                + " unit=" + describeObject(unit)
                                + " state=" + describeTransportState(unit),
                        unit, 750L));

        TransportEvents.BEFORE_UNLOAD_SPECIFIC_TRANSPORTED_UNIT.register((unit, transportedUnit, optionA, optionB) -> {
            showEventProbeMessage(stage, "BeforeUnloadSpecificTransportedUnit",
                    "BeforeUnloadSpecificTransportedUnit unit=" + describeObject(unit)
                            + " cargo=" + describeObject(transportedUnit)
                            + " options=" + optionA + "/" + optionB,
                    unit, 750L);
            return false;
        });

        TransportEvents.AFTER_UNLOAD_SPECIFIC_TRANSPORTED_UNIT.register((unit, transportedUnit, optionA, optionB, result) ->
                showEventProbeMessage(stage, "AfterUnloadSpecificTransportedUnit",
                        "AfterUnloadSpecificTransportedUnit result=" + result
                                + " unit=" + describeObject(unit)
                                + " cargo=" + describeObject(transportedUnit),
                        unit, 750L));

        TransportEvents.BEFORE_RELEASE_ALL_TRANSPORTED_UNITS.register((unit, killUnits) -> {
            showEventProbeMessage(stage, "BeforeReleaseAllTransportedUnits",
                    "BeforeReleaseAllTransportedUnits kill=" + killUnits
                            + " unit=" + describeObject(unit)
                            + " state=" + describeTransportState(unit),
                    unit, 750L);
            return false;
        });

        TransportEvents.AFTER_RELEASE_ALL_TRANSPORTED_UNITS.register((unit, killUnits) ->
                showEventProbeMessage(stage, "AfterReleaseAllTransportedUnits",
                        "AfterReleaseAllTransportedUnits kill=" + killUnits
                                + " unit=" + describeObject(unit)
                                + " state=" + describeTransportState(unit),
                        unit, 750L));

        TransportEvents.BEFORE_TRANSPORT_DEATH_CARGO_CLEANUP.register(unit -> {
            showEventProbeMessage(stage, "BeforeTransportDeathCargoCleanup",
                    "BeforeTransportDeathCargoCleanup unit=" + describeObject(unit)
                            + " state=" + describeTransportState(unit),
                    unit, 750L);
            return false;
        });

        TransportEvents.AFTER_TRANSPORT_DEATH_CARGO_CLEANUP.register(unit ->
                showEventProbeMessage(stage, "AfterTransportDeathCargoCleanup",
                        "AfterTransportDeathCargoCleanup unit=" + describeObject(unit)
                                + " state=" + describeTransportState(unit),
                        unit, 750L));

        CustomUnitRuntimeEvents.AFTER_CUSTOM_UNIT_KILLED.register(unit ->
                showEventProbeMessage(stage, "AfterCustomUnitKilled",
                        "AfterCustomUnitKilled unit=" + describeObject(unit),
                        unit, 750L));

        CustomUnitRuntimeEvents.AFTER_CUSTOM_UNIT_REMOVED.register(unit ->
                showEventProbeMessage(stage, "AfterCustomUnitRemoved",
                        "AfterCustomUnitRemoved unit=" + describeObject(unit),
                        unit, 750L));

        BuildQueueEvents.BEFORE_QUEUE_ACTION_APPLY.register((queue, action, front, targetPoint, targetUnit) -> {
            showEventProbeMessage(stage, "BeforeQueueActionApply",
                    "BeforeQueueActionApply action=" + describeObject(action)
                            + " front=" + front
                            + " target=" + describeObject(targetUnit),
                    queue, 750L);
            return false;
        });

        BuildQueueEvents.AFTER_QUEUE_ACTION_APPLY.register((queue, action, front, targetPoint, targetUnit, queueItem) ->
                showEventProbeMessage(stage, "AfterQueueActionApply",
                        "AfterQueueActionApply item=" + describeBuildQueueItem(queueItem)
                                + " front=" + front
                                + " target=" + describeObject(targetUnit),
                        queue, 750L));

        BuildQueueEvents.AFTER_QUEUE_ITEM_ACTIVATE.register((queue, queueItem) ->
                showEventProbeMessage(stage, "AfterQueueItemActivate",
                        "AfterQueueItemActivate item=" + describeBuildQueueItem(queueItem),
                        queue, 750L));

        BuildQueueEvents.AFTER_QUEUE_ITEM_COMPLETE.register((queue, queueItem, spacing, useRallyPoint, spawnYOffset, producedUnit) ->
                showEventProbeMessage(stage, "AfterQueueItemComplete",
                        "AfterQueueItemComplete item=" + describeBuildQueueItem(queueItem)
                                + " produced=" + describeObject(producedUnit)
                                + " rally=" + useRallyPoint,
                        producedUnit != null ? producedUnit : queue, 750L));

        BuildQueueEvents.AFTER_NEWLY_PRODUCED_UNIT_POSITIONED.register((queue, unit, spacing, useRallyPoint) ->
                showEventProbeMessage(stage, "AfterNewlyProducedUnitPositioned",
                        "AfterNewlyProducedUnitPositioned unit=" + describeObject(unit)
                                + " spacing=" + formatFloat(spacing)
                                + " rally=" + useRallyPoint,
                        unit, 750L));

        BuildQueueEvents.AFTER_HOST_BUILD_QUEUE_ITEM_COMPLETE.register((host, queueItem) ->
                showEventProbeMessage(stage, "AfterHostBuildQueueItemComplete",
                        "AfterHostBuildQueueItemComplete host=" + describeObject(host)
                                + " item=" + describeBuildQueueItem(queueItem),
                        host, 750L));

        BuildQueueEvents.MODIFY_HOST_BUILD_QUEUE_ITEM_REFUNDABLE.register((host, queueItem, currentResult) -> {
            showEventProbeMessage(stage, "ModifyHostBuildQueueItemRefundable",
                    "ModifyHostBuildQueueItemRefundable result=" + currentResult
                            + " item=" + describeBuildQueueItem(queueItem),
                    host, 1000L);
            return Boolean.valueOf(currentResult);
        });

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
                                + " amount=" + describeResourceAmount(resourceAmount)
                                + " unit=" + describeObject(unit),
                        unit, 1000L));

        ResourceRuntimeEvents.AFTER_RESOURCE_AMOUNT_ADD.register((resourceAmount, unit, scale, scaled, operation) ->
                showEventProbeMessage(stage, "AfterResourceAmountAdd",
                        "AfterResourceAmountAdd op=" + safeText(operation)
                                + " scale=" + formatDouble(scale)
                                + " scaled=" + scaled
                                + " amount=" + describeResourceAmount(resourceAmount)
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
                            + " amount=" + describeResourceAmount(resourceAmount)
                            + " unit=" + describeObject(unit),
                    unit, 1500L);
            return currentResult;
        });

        ResourceRuntimeEvents.AFTER_RESOURCE_RESERVE.register((resourceAmount, unit, lagHiding, operation, result) ->
                showEventProbeMessage(stage, "AfterResourceReserve",
                        "AfterResourceReserve result=" + result
                                + " op=" + safeText(operation)
                                + " lagHiding=" + lagHiding
                                + " amount=" + describeResourceAmount(resourceAmount)
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

        ExampleMod.log("registered event probe messages from " + stage);
    }

    static Object lastAudioObject() {
        return lastAudioObject;
    }

    static void rememberAudioObject(Object value) {
        if (value != null) {
            lastAudioObject = value;
        }
    }

    static String describeAudioObject(Object value) {
        if (value == null) {
            return "null";
        }

        try {
            if (AudioRuntimeDiagnostics.isAudioFileHandle(value)) {
                Map<String, Object> details = AudioRuntimeDiagnostics.describeAudioFileHandle(value);
                return describeObject(value)
                        + "{ext=" + safeText(String.valueOf(details.get("extension")))
                        + ", path=" + compactPath(String.valueOf(details.get("path"))) + "}";
            }
            if (AudioRuntimeDiagnostics.isOpenALGameSound(value)) {
                Map<String, Object> details = AudioRuntimeDiagnostics.describeOpenALGameSound(value);
                return describeObject(value)
                        + "{bytes=" + details.get("bytesUsed")
                        + ", sound=" + describeObject(details.get("sound")) + "}";
            }
            if (AudioRuntimeDiagnostics.isAndroidSound(value)) {
                Map<String, Object> details = AudioRuntimeDiagnostics.describeAndroidSound(value);
                return describeObject(value)
                        + "{name=" + compactPath(String.valueOf(details.get("name")))
                        + ", id=" + details.get("soundId")
                        + ", queue=" + describeObject(details.get("queueFactory")) + "}";
            }
            if (AudioRuntimeDiagnostics.isNullSound(value)) {
                Map<String, Object> details = AudioRuntimeDiagnostics.describeNullSound(value);
                return describeObject(value)
                        + "{name=" + compactPath(String.valueOf(details.get("name")))
                        + ", bytes=" + details.get("bytesUsed") + "}";
            }
            if (AudioRuntimeDiagnostics.isGameSound(value)) {
                Map<String, Object> details = AudioRuntimeDiagnostics.describeGameSound(value);
                return describeObject(value)
                        + "{name=" + compactPath(String.valueOf(details.get("name")))
                        + ", base=" + details.get("baseVolume")
                        + ", bytes=" + details.get("bytesUsed") + "}";
            }
            if (AudioRuntimeDiagnostics.isOpenALSoundPlayTask(value)) {
                Map<String, Object> details = AudioRuntimeDiagnostics.describeOpenALSoundPlayTask(value);
                return describeObject(value)
                        + "{vol=" + details.get("leftVolume") + "/" + details.get("rightVolume")
                        + ", pitch=" + details.get("pitch")
                        + ", loop=" + details.get("loop") + "}";
            }
            if (AudioRuntimeDiagnostics.isSoundPlayRequest(value)) {
                Map<String, Object> details = AudioRuntimeDiagnostics.describeSoundPlayRequest(value);
                return describeObject(value)
                        + "{vol=" + details.get("leftVolume") + "/" + details.get("rightVolume")
                        + ", pitch=" + details.get("pitch")
                        + ", sound=" + describeObject(details.get("sound")) + "}";
            }
            if (AudioRuntimeDiagnostics.isSoundQueueThread(value)) {
                Map<String, Object> details = AudioRuntimeDiagnostics.describeSoundQueueThread(value);
                return describeObject(value)
                        + "{factory=" + describeObject(details.get("soundFactory")) + "}";
            }
            if (AudioRuntimeDiagnostics.isOpenALSoundFactory(value)) {
                Map<String, Object> details = AudioRuntimeDiagnostics.describeOpenALSoundFactory(value);
                return describeObject(value)
                        + "{pool=" + details.get("soundPoolSize")
                        + ", queue=" + details.get("playQueueSize")
                        + ", audio=" + describeObject(details.get("openALAudio")) + "}";
            }
            if (AudioRuntimeDiagnostics.isAndroidSoundFactory(value)) {
                Map<String, Object> details = AudioRuntimeDiagnostics.describeAndroidSoundFactory(value);
                return describeObject(value)
                        + "{pool=" + details.get("soundPoolSize")
                        + ", queue=" + details.get("playQueueSize")
                        + ", priority=" + details.get("nextSoundPriority") + "}";
            }
            if (AudioRuntimeDiagnostics.isNullSoundFactory(value)) {
                return describeObject(value) + "{nullSoundBackend}";
            }
            if (AudioRuntimeDiagnostics.isSoundFactory(value)) {
                Map<String, Object> details = AudioRuntimeDiagnostics.describeSoundFactory(value);
                return describeObject(value)
                        + "{loaded=" + details.get("loadedSoundsSize") + "}";
            }
            if (AudioRuntimeDiagnostics.isOpenALAudio(value)) {
                Map<String, Object> details = AudioRuntimeDiagnostics.describeOpenALAudio(value);
                return describeObject(value)
                        + "{noDevice=" + details.get("noDevice")
                        + ", idle=" + details.get("idleSourcesSize")
                        + ", music=" + details.get("musicSize") + "}";
            }
            if (AudioRuntimeDiagnostics.isOpenALSound(value)) {
                Map<String, Object> details = AudioRuntimeDiagnostics.describeOpenALSound(value);
                return describeObject(value)
                        + "{buffer=" + details.get("bufferID")
                        + ", bytes=" + details.get("bytesUsed")
                        + ", duration=" + details.get("duration") + "}";
            }
            if (AudioRuntimeDiagnostics.isOpenALMusic(value)) {
                Map<String, Object> details = AudioRuntimeDiagnostics.describeOpenALMusic(value);
                return describeObject(value)
                        + "{source=" + details.get("sourceID")
                        + ", playing=" + details.get("isPlaying")
                        + ", pos=" + details.get("position") + "}";
            }
            if (AudioRuntimeDiagnostics.isMusicController(value)) {
                Map<String, Object> details = AudioRuntimeDiagnostics.describeMusicController(value);
                return describeObject(value)
                        + "{track=" + compactPath(String.valueOf(details.get("currentTrackPath")))
                        + ", canPlay=" + details.get("canPlayMusic")
                        + ", fade=" + details.get("crossFading")
                        + ", cache=" + details.get("musicTrackCacheSize") + "}";
            }
            if (AudioRuntimeDiagnostics.isMusicTrack(value)) {
                Map<String, Object> details = AudioRuntimeDiagnostics.describeMusicTrack(value);
                return describeObject(value)
                        + "{path=" + compactPath(String.valueOf(details.get("trackPath"))) + "}";
            }
            if (AudioRuntimeDiagnostics.isMusicPlayer(value)) {
                Map<String, Object> details = AudioRuntimeDiagnostics.describeMusicPlayer(value);
                return describeObject(value)
                        + "{playing=" + details.get("playing")
                        + ", queued=" + details.get("playQueued")
                        + ", track=" + describeObject(details.get("track")) + "}";
            }
            if (AudioRuntimeDiagnostics.isMusicFactory(value)) {
                Map<String, Object> details = AudioRuntimeDiagnostics.describeMusicFactory(value);
                return describeObject(value)
                        + "{threaded=" + details.get("usesMusicThread")
                        + ", waitMs=" + details.get("musicThreadWaitMillis")
                        + ", available=" + details.get("available") + "}";
            }
            if (AudioRuntimeDiagnostics.isMusicCategory(value)) {
                Map<String, Object> details = AudioRuntimeDiagnostics.describeMusicCategory(value);
                return describeObject(value)
                        + "{folder=" + compactPath(String.valueOf(details.get("folderPath")))
                        + ", tracks=" + details.get("trackNamesLength") + "}";
            }
        } catch (RuntimeException ignored) {
        }

        return describeObject(value);
    }

    private static String describeBuildQueueItem(Object queueItem) {
        if (queueItem == null) {
            return "null";
        }
        try {
            Map<String, Object> details = BuildQueueDiagnostics.describeBuildQueueItem(queueItem);
            return describeObject(queueItem)
                    + "{qty=" + details.get("quantity")
                    + ", action=" + describeObject(details.get("actionId"))
                    + ", unitType=" + describeObject(details.get("producedUnitType"))
                    + ", high=" + details.get("highPriority")
                    + "}";
        } catch (RuntimeException e) {
            return describeObject(queueItem);
        }
    }

    private static String describeActiveResourceDelta(Object unit) {
        try {
            return describeObject(UnitRuntimeDiagnostics.getActiveResourceDelta(unit));
        } catch (RuntimeException e) {
            return "<unavailable>";
        }
    }

    private static String describePasswordPrompt(Object passwordPrompt) {
        if (passwordPrompt == null) {
            return "prompt=null";
        }
        try {
            if (NetworkRuntimeDiagnostics.isPasswordPrompt(passwordPrompt)) {
                Map<String, Object> details = NetworkRuntimeDiagnostics.describePasswordPrompt(passwordPrompt);
                return "prompt=" + describeObject(passwordPrompt)
                        + " title=" + safeText(String.valueOf(details.get("customTitle")))
                        + " message=" + safeText(String.valueOf(details.get("promptMessage")))
                        + " positive=" + safeText(String.valueOf(details.get("positiveButtonText")))
                        + " negative=" + safeText(String.valueOf(details.get("negativeButtonText")));
            }
        } catch (RuntimeException ignored) {
        }
        return "prompt=" + describeObject(passwordPrompt);
    }

    private static void showNetworkHandshakeProbe(String stage, String key,
                                                  Object networkEngine, Object connection) {
        showNetworkHandshakeProbe(stage, key, "", networkEngine, connection);
    }

    private static void showNetworkHandshakeProbe(String stage, String key, String extra,
                                                  Object networkEngine, Object connection) {
        String message = "NetworkHandshake." + key
                + (extra != null && !extra.isEmpty() ? " " + extra : "")
                + " " + describeNetworkConnection(connection);
        showEventProbeMessage(stage, "NetworkHandshake." + key, message,
                connection != null ? connection : networkEngine, 1000L);
    }

    private static String describeNetworkConnection(Object connection) {
        if (connection == null) {
            return "conn=null";
        }
        try {
            if (NetworkRuntimeDiagnostics.isNetworkConnection(connection)) {
                Map<String, Object> details = NetworkRuntimeDiagnostics.describeNetworkConnection(connection);
                return "conn=" + describeObject(connection)
                        + "{id=" + details.get("connectionId")
                        + ", playerId=" + details.get("playerId")
                        + ", validated=" + details.get("validated")
                        + ", open=" + details.get("open")
                        + ", q=" + details.get("sendQueueSize")
                        + ", ping=" + details.get("lastPingMillis")
                        + ", nonce=" + details.get("challengeNonce")
                        + ", addr=" + safeText(String.valueOf(details.get("addressDisplay"))) + "}";
            }
        } catch (RuntimeException ignored) {
        }
        return "conn=" + describeObject(connection);
    }

    private static String describeCommand(Object command) {
        if (command == null) {
            return "command=null";
        }
        try {
            Map<String, Object> details = CommandDiagnostics.describeCommand(command);
            return "command=" + describeObject(command)
                    + " team=" + describeObject(details.get("team"))
                    + " selected=" + details.get("selectedUnitReferenceCount")
                    + " append=" + details.get("appendToExistingOrders")
                    + " clear=" + details.get("clearExistingOrdersBeforeIssue")
                    + " replaceMove=" + details.get("replaceMatchingMoveWaypoint")
                    + " system=" + details.get("hasSystemCommand")
                    + " code=" + details.get("systemActionCode")
                    + " paths=" + countCollection(details.get("sharedPathCacheEntries"));
        } catch (RuntimeException e) {
            return "command=" + describeObject(command);
        }
    }

    private static int countCollection(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof java.util.Collection<?>) {
            return ((java.util.Collection<?>) value).size();
        }
        if (value.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(value);
        }
        return 1;
    }

    private static String describeRepairMetadata(Object metadata) {
        try {
            Map<String, Object> details = CustomUnitDiagnostics.describeCustomUnitMetadata(metadata);
            return "{nano=" + details.get("nanoUnbuildSpeed")
                    + ", reclaim=" + describeObject(details.get("reclaimPrice"))
                    + ", repairTags=" + describeObject(details.get("canRepairUnitsOnlyWithTags"))
                    + ", reclaimTags=" + describeObject(details.get("canReclaimUnitsOnlyWithTags"))
                    + "}";
        } catch (RuntimeException e) {
            return "<unavailable>";
        }
    }

    private static String describeTransportMetadata(Object metadata) {
        try {
            Map<String, Object> details = CustomUnitDiagnostics.describeTransportMetadata(metadata);
            return "{max=" + details.get("maxTransportingUnits")
                    + ", slots=" + details.get("transportSlotsNeeded")
                    + ", addUnload=" + details.get("transportUnitsAddUnloadOption")
                    + ", kill=" + describeObject(details.get("transportUnitsKillOnDeath"))
                    + "}";
        } catch (RuntimeException e) {
            return "<unavailable>";
        }
    }

    private static String describeTransportState(Object unit) {
        try {
            return "{count=" + UnitRuntimeDiagnostics.getTransportedUnitCount(unit)
                    + ", unloading=" + UnitRuntimeDiagnostics.isTransportUnloading(unit)
                    + ", bar=" + UnitRuntimeDiagnostics.getTransportBarUsedSlots(unit)
                    + "/" + UnitRuntimeDiagnostics.getTransportBarMaxSlots(unit)
                    + "}";
        } catch (RuntimeException e) {
            return "<unavailable>";
        }
    }

    private static String describeResourceAmount(Object resourceAmount) {
        if (resourceAmount == null) {
            return "null";
        }
        try {
            Map<String, Object> details = ResourceEconomyDiagnostics.describeResourceAmount(resourceAmount);
            return "{credits=" + details.get("credits")
                    + ", energy=" + details.get("energy")
                    + ", hp=" + details.get("hp")
                    + ", shield=" + details.get("shield")
                    + ", ammo=" + details.get("ammo")
                    + ", custom=" + describeObject(details.get("customResources"))
                    + "}";
        } catch (RuntimeException e) {
            return describeObject(resourceAmount);
        }
    }

    private static String describeAttachmentSlot(Object attachmentSlot) {
        if (attachmentSlot == null) {
            return "null";
        }
        try {
            Map<String, Object> details = CustomUnitDiagnostics.describeAttachmentSlot(attachmentSlot);
            return "{name=" + safeText(String.valueOf(details.get("name")))
                    + ", addCargo=" + details.get("addTransportedUnits")
                    + ", unloadHere=" + details.get("unloadInCurrentPosition")
                    + ", hidden=" + details.get("hidden")
                    + "}";
        } catch (RuntimeException e) {
            return describeObject(attachmentSlot);
        }
    }

}
