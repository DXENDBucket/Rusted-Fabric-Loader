package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.SaveSyncEvents;
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
        if (SaveSyncEvents.BEFORE_SAVE_GAME_TO_FILE.invoker().beforeSaveGameToFile(this, saveName, autoSave)) {
            ci.cancel();
        }
    }

    @Inject(method = "writeSaveToStream(Lrustedwarfare/io/GameOutputStream;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeWriteSaveStream(@Coerce Object outputStream, CallbackInfo ci) {
        if (SaveSyncEvents.BEFORE_WRITE_SAVE_STREAM.invoker().beforeWriteSaveStream(this, outputStream)) {
            ci.cancel();
        }
    }

    @Inject(method = "writeSaveToStream(Lrustedwarfare/io/GameOutputStream;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterWriteSaveStream(@Coerce Object outputStream, CallbackInfo ci) {
        SaveSyncEvents.AFTER_WRITE_SAVE_STREAM.invoker().afterWriteSaveStream(this, outputStream);
    }

    @Inject(method = "readSaveFromStream(Lrustedwarfare/io/GameInputStream;ZZZ)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeReadSaveStream(@Coerce Object inputStream, boolean optionA, boolean optionB, boolean optionC, CallbackInfoReturnable<Boolean> cir) {
        if (SaveSyncEvents.BEFORE_READ_SAVE_STREAM.invoker().beforeReadSaveStream(this, inputStream, optionA, optionB, optionC)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "readSaveFromStream(Lrustedwarfare/io/GameInputStream;ZZZ)Z", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterReadSaveStream(@Coerce Object inputStream, boolean optionA, boolean optionB, boolean optionC, CallbackInfoReturnable<Boolean> cir) {
        SaveSyncEvents.AFTER_READ_SAVE_STREAM.invoker()
                .afterReadSaveStream(this, inputStream, optionA, optionB, optionC, Boolean.TRUE.equals(cir.getReturnValue()));
    }
}
