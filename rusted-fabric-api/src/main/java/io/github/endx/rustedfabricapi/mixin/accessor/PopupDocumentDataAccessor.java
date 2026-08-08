package io.github.endx.rustedfabricapi.mixin.accessor;

import com.ElementDocument;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "rustedwarfare.ui.PopupDocumentData", remap = false)
public interface PopupDocumentDataAccessor {
    @Accessor("document") ElementDocument rustedfabricapi$getDocument();
    @Accessor("title") String rustedfabricapi$getTitle();
    @Accessor("message") String rustedfabricapi$getMessage();
    @Accessor("inputDefaultValue") String rustedfabricapi$getInputDefaultValue();
    @Accessor("showBackButton") boolean rustedfabricapi$getShowBackButton();
}
