package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.event.CommandEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "rustedwarfare.command.Command", remap = false)
public abstract class CommandIssueNamedMixin {
    @Inject(method = "issueCommand()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeCommandIssue(CallbackInfo ci) {
        if (CommandEvents.BEFORE_COMMAND_ISSUE.invoker().beforeCommandIssue(this)) {
            ci.cancel();
        }
    }

    @Inject(method = "issueCommand()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterCommandIssue(CallbackInfo ci) {
        CommandEvents.AFTER_COMMAND_ISSUE.invoker().afterCommandIssue(this);
    }
}
