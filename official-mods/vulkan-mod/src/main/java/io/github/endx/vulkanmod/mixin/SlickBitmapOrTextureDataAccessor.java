package io.github.endx.vulkanmod.mixin;

import java.nio.ByteBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes Slick's CPU readback buffer without depending on Slick classes at compile time. */
@Mixin(targets = "rustedwarfare.client.render.SlickBitmapOrTexture", remap = false)
public interface SlickBitmapOrTextureDataAccessor {
    @Accessor("imageByteBuffer") ByteBuffer vulkanmod$getImageByteBuffer();
    @Accessor("bytesPerPixel") int vulkanmod$getBytesPerPixel();
    @Accessor("bytesPerPixel") void vulkanmod$setBytesPerPixel(int bytesPerPixel);
}
