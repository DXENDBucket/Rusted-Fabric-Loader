package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.client.warlog.WarLogEntryKind;
import io.github.endx.rustedfabricapi.api.client.warlog.WarLogEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustedwarfare.ui.WarLogInterface;
import rustedwarfare.unit.Unit;

@Mixin(targets = "rustedwarfare.ui.WarLogInterface", remap = false)
public abstract class WarLogInterfaceNamedMixin {
    @Inject(method = "addTextEntry(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeText(String text, CallbackInfo ci) {
        if (WarLogEvents.BEFORE_TEXT.invoker().beforeText(rustedfabricapi$self(), text, -1)) ci.cancel();
    }

    @Inject(method = "addTextEntry(Ljava/lang/String;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterText(String text, CallbackInfo ci) {
        WarLogEvents.AFTER_TEXT.invoker().afterText(rustedfabricapi$self(), text, -1);
    }

    @Inject(method = "addTimedTextEntry(Ljava/lang/String;I)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeTimedText(String text, int durationMillis, CallbackInfo ci) {
        if (WarLogEvents.BEFORE_TEXT.invoker().beforeText(rustedfabricapi$self(), text, durationMillis)) ci.cancel();
    }

    @Inject(method = "addTimedTextEntry(Ljava/lang/String;I)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterTimedText(String text, int durationMillis, CallbackInfo ci) {
        WarLogEvents.AFTER_TEXT.invoker().afterText(rustedfabricapi$self(), text, durationMillis);
    }

    @Inject(method = "addUnitCreated(Lrustedwarfare/unit/Unit;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeCreated(Unit unit, CallbackInfo ci) {
        rustedfabricapi$beforeUnit(WarLogEntryKind.UNIT_CREATED, unit, ci);
    }

    @Inject(method = "addUnitCreated(Lrustedwarfare/unit/Unit;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterCreated(Unit unit, CallbackInfo ci) {
        rustedfabricapi$afterUnit(WarLogEntryKind.UNIT_CREATED, unit);
    }

    @Inject(method = "addUpgradeCompleted(Lrustedwarfare/unit/Unit;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeUpgrade(Unit unit, CallbackInfo ci) {
        rustedfabricapi$beforeUnit(WarLogEntryKind.UPGRADE_COMPLETED, unit, ci);
    }

    @Inject(method = "addUpgradeCompleted(Lrustedwarfare/unit/Unit;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterUpgrade(Unit unit, CallbackInfo ci) {
        rustedfabricapi$afterUnit(WarLogEntryKind.UPGRADE_COMPLETED, unit);
    }

    @Inject(method = "addUnitDamaged(Lrustedwarfare/unit/Unit;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeDamaged(Unit unit, CallbackInfo ci) {
        rustedfabricapi$beforeUnit(WarLogEntryKind.UNIT_DAMAGED, unit, ci);
    }

    @Inject(method = "addUnitDamaged(Lrustedwarfare/unit/Unit;)V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterDamaged(Unit unit, CallbackInfo ci) {
        rustedfabricapi$afterUnit(WarLogEntryKind.UNIT_DAMAGED, unit);
    }

    @Inject(method = "clearEntries()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeClear(CallbackInfo ci) {
        WarLogEvents.BEFORE_CLEAR.invoker().onClear(rustedfabricapi$self());
    }

    @Inject(method = "clearEntries()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterClear(CallbackInfo ci) {
        WarLogEvents.AFTER_CLEAR.invoker().onClear(rustedfabricapi$self());
    }

    @Unique
    private void rustedfabricapi$beforeUnit(WarLogEntryKind kind, Unit unit, CallbackInfo ci) {
        if (WarLogEvents.BEFORE_UNIT_ENTRY.invoker().beforeUnitEntry(rustedfabricapi$self(), kind, unit)) ci.cancel();
    }

    @Unique
    private void rustedfabricapi$afterUnit(WarLogEntryKind kind, Unit unit) {
        WarLogEvents.AFTER_UNIT_ENTRY.invoker().afterUnitEntry(rustedfabricapi$self(), kind, unit);
    }

    @Unique
    private WarLogInterface rustedfabricapi$self() {
        return (WarLogInterface) (Object) this;
    }
}
