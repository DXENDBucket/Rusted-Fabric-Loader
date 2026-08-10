# Vulkan Mod

`vulkan_mod` is an experimental, client-only official mod that will replace the desktop
Slick/OpenGL renderer with a Vulkan renderer. It is not a gameplay dependency and must never affect
multiplayer compatibility.

The foundation build probes Vulkan and, after Slick creates its Win32 window, creates a live
surface, presentation-capable device/queues, swapchain, render pass, framebuffers, command buffers,
and synchronization objects. It also has a binding-neutral frame command list and batched
colored/textured quad and triangle paths backed by a growable host-visible vertex buffer. RGBA8
uploads use a staging buffer, device-local images, samplers, and per-texture descriptor sets;
compatible adjacent commands share a draw call without changing the original colored/textured
command order. The normal modes deliberately leave the existing renderer active.
Game-owned `GameImage` objects can now be read back into an identity/version-aware Vulkan texture
cache. Image reload and release hooks invalidate stale GPU copies. Draw commands also carry an
affine screen-space transform and optional scissor rectangle; transforms are baked while batching,
and scissor changes split otherwise compatible batches.
Use `-Drusted.fabric.vulkan.mode=off|probe|frame_test|takeover_test|native|required`; `probe` is the
development default, `frame_test` presents 300 solid diagnostic frames (red, green, then blue)
after OpenGL frames, and `required` makes an unavailable driver fail startup. `takeover_test` is
the first real presentation takeover. Its Win32 child surface starts hidden while draw calls are
mirrored without suppressing OpenGL. The first complete Vulkan frame is acquired, recorded and submitted
before the overlay is atomically revealed immediately ahead of presentation; suppression starts
only after that present succeeds, on the following game-loop frame. Every game-loop frame captures
Slick clears, image draws, transformed/tiled images,
rectangles, lines, circles and text into an ordered Vulkan frame. It also translates LibRocket's
indexed colored/textured geometry, including its scissor state, before presenting the completed
frame. Normal, additive, copy and modulation blend equations follow the game's Slick state, while
each texture has independently selectable linear and nearest-neighbour sampling. Slick-rendered
offscreen images remain a compatibility fallback and invalidate their Vulkan copy whenever they are
drawn into. A minimized or occluded window uses a bounded image-acquire wait so it cannot freeze the
game thread. The text path currently rasterizes and caches complete AWT string runs rather than
using a glyph atlas. Arbitrary Slick shaders, native Vulkan offscreen targets and a proper
frames-in-flight scheduler still need implementations, so takeover remains an opt-in developer mode.

`native` is the first renderer-startup migration stage. The Vulkan mod registers as a renderer
provider during client-mod initialization, then RFL resolves the backend before invoking the game
main class. A bootstrap hook confirms that decision at the start of `AppGameContainer.setup()`,
before Slick calls `Display.create()`. The game now constructs `VulkanGraphicsEngine` as its
`GraphicsEngine`; this first implementation delegates operations not yet migrated to Slick while
the proven Vulkan capture path presents them. This stage intentionally retains Slick's compatibility
window and OpenGL context for input, LibRocket and fallback assets. Removing that context requires
a native platform window/input loop and completion of the renderer methods, not another overlay
timing switch.

Useful takeover diagnostics are:

- `-Drusted.fabric.vulkan.debugMagentaClear=true` overrides captured clears with magenta.
- `-Drusted.fabric.vulkan.debugMarkerQuad=true` appends an opaque green quad to prove that the
  vertex upload and graphics pipeline reach the swapchain.
- `-Drusted.fabric.vulkan.debugDetachedOverlay=true` presents in a separate popup rather than an
  in-window child surface.
- `-Drusted.fabric.vulkan.renderWhenHidden=true` keeps the Slick loop rendering while its window is
  hidden or occluded.
- `-Drusted.fabric.vulkan.debugInfiniteAcquire=true` removes the normal 16 ms swapchain-acquire
  timeout. This can deliberately block the game thread and is only for isolating WSI diagnostics.

Vulkan validation and Debug Utils are not wired yet; they are the next diagnostic layer after the
solid-frame and safe-takeover sequence is confirmed.

## Boundary

- The main mod owns renderer-neutral textures, draw commands, batching, frame resources, and the
  implementation of Rusted Warfare's `GraphicsEngine` contract.
- `desktop-driver` owns LWJGL 3 and Vulkan binding objects. It is loaded child-first from embedded
  jars so LWJGL 3 cannot replace the game's LWJGL 2 classes.
- A later Android driver will implement the same small SPI through a Vulkan JNI bridge. No LWJGL or
  Win32 type may cross that boundary.
- Game-specific mixins, Slick compatibility and low-level takeover code stay in this mod. A hook is
  promoted to Rusted Fabric API only when another renderer or ordinary mod can reuse it safely.

## Stages

1. Probe the Vulkan loader, API version and physical devices without changing rendering.
2. Add native-window surface creation and queue-family selection while Slick still owns the desktop
   window and input loop. Swapchain resize recreation, basic render targets, command submission,
   synchronization, and an opt-in one-frame presentation test are complete.
3. Complete native offscreen render targets and shader translation. Common blend modes, texture
   filtering and dynamic fallback-texture invalidation are complete; the takeover already translates
   the commonly used `GraphicsEngine`, Slick primitive/image and LibRocket geometry paths into
   frame-local commands.
4. Replace the serialized safe baseline with persistent mapped vertex/index rings and multiple
   frames in flight, then widen batching without changing draw order.
5. Replace whole-string AWT textures with a glyph atlas and remove the remaining OpenGL fallback
   paths.
6. Add the Android JNI platform driver, surface lifecycle and device-loss handling.

The mobile baseline should prefer Vulkan 1.1-era features and keep optional descriptor indexing or
timeline semaphore paths behind capability checks. The primary performance target is CPU submission
cost: draw collection and batching must happen before any platform-driver calls.
