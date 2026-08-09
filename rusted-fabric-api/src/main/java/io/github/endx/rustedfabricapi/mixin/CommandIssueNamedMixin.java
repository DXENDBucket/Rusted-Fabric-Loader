package io.github.endx.rustedfabricapi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "rustedwarfare.command.Command", remap = false)
public abstract class CommandIssueNamedMixin {
    @Inject(method = "issueCommand()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeCommandIssue(CallbackInfo ci) {
        boolean cancelled = io.github.endx.rustedfabricapi.api.command.event.CommandEvents.BEFORE_ISSUE.invoker()
                .beforeIssue((rustedwarfare.command.Command) (Object) this);
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "issueCommand()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterCommandIssue(CallbackInfo ci) {
        io.github.endx.rustedfabricapi.api.command.event.CommandEvents.AFTER_ISSUE.invoker()
                .afterIssue((rustedwarfare.command.Command) (Object) this);
    }
}
