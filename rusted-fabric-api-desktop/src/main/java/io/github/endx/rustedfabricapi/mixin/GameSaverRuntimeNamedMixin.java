package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.SaveSyncEvents;
import io.github.endx.rustedfabricapi.api.save.event.SaveEvents;
import io.github.endx.rustedfabricapi.api.data.PersistentData;
import io.github.endx.rustedfabricapi.api.unit.attribute.CustomUnitStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rustedwarfare.save.GameSaver", remap = false)
public abstract class GameSaverRuntimeNamedMixin {
    @Inject(method = "saveGameToFile(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeSaveGameToFile(String saveName, boolean autoSave, CallbackInfo ci) {
        boolean cancelled = SaveSyncEvents.BEFORE_SAVE_GAME_TO_FILE.invoker()
                .beforeSaveGameToFile(this, saveName, autoSave);
        cancelled |= SaveEvents.BEFORE_SAVE.invoker().beforeSave(
                (rustedwarfare.save.GameSaver) (Object) this, saveName, autoSave);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "saveGameToFile(Ljava/lang/String;Z)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterSaveGameToFile(String saveName, boolean autoSave,
            CallbackInfo ci) {
        SaveEvents.AFTER_SAVE.invoker().afterSave(
                (rustedwarfare.save.GameSaver) (Object) this, saveName, autoSave);
    }

    @Inject(method = "loadGameFromFile(Ljava/lang/String;Z)Z", at = @At("HEAD"),
            cancellable = true, require = 1)
    private void rustedfabricapi$beforeLoadGameFromFile(String saveName, boolean option,
            CallbackInfoReturnable<Boolean> cir) {
        if (SaveEvents.BEFORE_LOAD.invoker().beforeLoad(
                (rustedwarfare.save.GameSaver) (Object) this, saveName)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "loadGameFromFile(Ljava/lang/String;Z)Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterLoadGameFromFile(String saveName, boolean option,
            CallbackInfoReturnable<Boolean> cir) {
        SaveEvents.AFTER_LOAD.invoker().afterLoad(
                (rustedwarfare.save.GameSaver) (Object) this, saveName,
                Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(method = "deleteSave(Ljava/lang/String;)Z", at = @At("HEAD"),
            cancellable = true, require = 1)
    private void rustedfabricapi$beforeDeleteSave(String saveName,
            CallbackInfoReturnable<Boolean> cir) {
        if (SaveEvents.BEFORE_DELETE.invoker().beforeDelete(
                (rustedwarfare.save.GameSaver) (Object) this, saveName)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "deleteSave(Ljava/lang/String;)Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterDeleteSave(String saveName,
            CallbackInfoReturnable<Boolean> cir) {
        SaveEvents.AFTER_DELETE.invoker().afterDelete(
                (rustedwarfare.save.GameSaver) (Object) this, saveName,
                Boolean.TRUE.equals(cir.getReturnValue()));
    }

    @Inject(method = "writeSaveToStream(Lrustedwarfare/io/GameOutputStream;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeWriteSaveStream(@Coerce Object outputStream, CallbackInfo ci) {
        if (SaveSyncEvents.BEFORE_WRITE_SAVE_STREAM.invoker().beforeWriteSaveStream(this, outputStream)) {
            ci.cancel();
        } else {
            CustomUnitStats.suspendForSerialization();
        }
    }

    @Inject(method = "writeSaveToStream(Lrustedwarfare/io/GameOutputStream;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterWriteSaveStream(@Coerce Object outputStream, CallbackInfo ci) {
        CustomUnitStats.resumeAfterSerialization();
        PersistentData.writeSaveExtension((rustedwarfare.io.GameOutputStream) outputStream);
        SaveSyncEvents.AFTER_WRITE_SAVE_STREAM.invoker().afterWriteSaveStream(this, outputStream);
    }

    @Inject(method = "readSaveFromStream(Lrustedwarfare/io/GameInputStream;ZZZ)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeReadSaveStream(@Coerce Object inputStream, boolean optionA, boolean optionB, boolean optionC, CallbackInfoReturnable<Boolean> cir) {
        if (SaveSyncEvents.BEFORE_READ_SAVE_STREAM.invoker().beforeReadSaveStream(this, inputStream, optionA, optionB, optionC)) {
            cir.setReturnValue(Boolean.FALSE);
        } else {
            PersistentData.clearRuntime();
            CustomUnitStats.clearRuntime();
        }
    }

    @Inject(method = "readSaveFromStream(Lrustedwarfare/io/GameInputStream;ZZZ)Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterReadSaveStream(@Coerce Object inputStream, boolean optionA, boolean optionB, boolean optionC, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            PersistentData.readSaveExtension((rustedwarfare.io.GameInputStream) inputStream);
        }
        SaveSyncEvents.AFTER_READ_SAVE_STREAM.invoker()
                .afterReadSaveStream(this, inputStream, optionA, optionB, optionC, Boolean.TRUE.equals(cir.getReturnValue()));
    }
}
