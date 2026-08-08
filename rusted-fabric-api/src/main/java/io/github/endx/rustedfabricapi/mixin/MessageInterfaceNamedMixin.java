package io.github.endx.rustedfabricapi.mixin;

import io.github.endx.rustedfabricapi.api.client.message.MessageEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rustedwarfare.ui.MessageInterface;
import rustedwarfare.ui.MessageLine;

@Mixin(value = MessageInterface.class, remap = false)
public abstract class MessageInterfaceNamedMixin {
    @Inject(method = "addMessage(Ljava/lang/String;Ljava/lang/String;)Lrustedwarfare/ui/MessageLine;",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void rustedfabricapi$beforeAdd(String sender, String message,
            CallbackInfoReturnable<MessageLine> cir) {
        if (MessageEvents.BEFORE_ADD.invoker().beforeAdd(history(), sender, message)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "addMessage(Ljava/lang/String;Ljava/lang/String;)Lrustedwarfare/ui/MessageLine;",
            at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterAdd(String sender, String message,
            CallbackInfoReturnable<MessageLine> cir) {
        MessageLine line = cir.getReturnValue();
        if (line != null) {
            MessageEvents.AFTER_ADD.invoker().afterAdd(history(), sender, message, line);
        }
    }

    @Inject(method = "clearMessages()V", at = @At("HEAD"), require = 1)
    private void rustedfabricapi$beforeClear(CallbackInfo ci) {
        MessageEvents.BEFORE_CLEAR.invoker().onClear(history());
    }

    @Inject(method = "clearMessages()V", at = @At("RETURN"), require = 1)
    private void rustedfabricapi$afterClear(CallbackInfo ci) {
        MessageEvents.AFTER_CLEAR.invoker().onClear(history());
    }

    private MessageInterface history() {
        return (MessageInterface) (Object) this;
    }
}
