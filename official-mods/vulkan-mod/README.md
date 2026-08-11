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
using a glyph atlas. Arbitrary Slick shader translation is still incomplete, so takeover remains
an opt-in developer mode.

`native` is the renderer-startup replacement path. The Vulkan mod registers as a renderer
provider during client-mod initialization, then RFL resolves the backend before invoking the game
main class. A bootstrap hook confirms that decision at the start of `AppGameContainer.setup()`,
before Slick calls `Display.create()`. The game constructs `VulkanGraphicsEngine` as its
`GraphicsEngine`, while the driver owns the Win32 window, input queue, swapchain and presentation
loop. `Display.create()` is cancelled and a runtime invariant verifies that no LWJGL 2 Display or
OpenGL context was created. LibRocket geometry, the software game cursor, game images, primitives,
text and map/minimap child images all feed Vulkan commands. Child `GraphicsEngine` instances use
sampled Vulkan color images and native framebuffers; the original `GameImage.flushPixelBufferToBitmap`
contract submits their pending render pass so the game's terrain-cache lifecycle remains valid
without a CPU/Java2D redraw.
Image draw commands also snapshot the game's mutable shader uniforms before crossing the driver
boundary. The native texture pipeline implements the stock `plain`, `error`,
`pureGreenTeamColor`, `hueAddTeamColor`, `hueShiftTeamColor`, `post_base`, and the two-texture
`post_displacement` fragment formulas through Vulkan push constants and paired image descriptors,
so shader-based team coloring and displacement effects no longer fall back to OpenGL or per-team
CPU texture copies.

Native offscreen submission currently waits conservatively for the graphics queue. This is an
intentional correctness boundary while replacement coverage is completed; batching child passes
into the top-level frame is performance work, not a return to a compatibility renderer. Dynamic
Canvas bitmap rebinding now reuses each image's single native target command stream, submits the
outgoing target, and carries the live transform/clip stack across the framebuffer switch. Remaining
functional gaps are general custom Slick shader translation. Native render targets expose
synchronized RGBA readback and same-size CPU upload, so `GameImage` pixel reads/copies and explicit
pixel-buffer flushes retain their original semantics.
Whole-string AWT rasterization is used for glyph pixels, but text presentation itself is Vulkan.

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
- `-Drusted.fabric.vulkan.debugRenderTargetPasses=true` logs the first native child passes, their
  sampled texture dependencies, the first main-frame child samples, and a one-time large-target
  GPU readback summary.

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
2. Native Win32 window/input ownership, swapchain resize recreation, frame command submission and
   the pre-`Display.create()` bootstrap are complete.
3. Native `GraphicsEngine` images, Vulkan offscreen framebuffers, terrain/minimap cache submission,
   common blend modes, texture filtering, LibRocket geometry, built-in team/post shaders, and the
   secondary-texture displacement path are complete. Native image readback/upload covers legacy
   pixel-buffer mutation and dynamic Canvas target switching; general custom shader translation
   remains.
4. After functional replacement, batch child render passes into the top-level submission, use
   persistent mapped vertex/index rings, and widen batching without changing draw order.
5. Replace whole-string AWT textures with a glyph atlas and remove the obsolete takeover-only
   compatibility surface after native parity is established.
6. Add the Android JNI platform driver, surface lifecycle and device-loss handling.

The mobile baseline should prefer Vulkan 1.1-era features and keep optional descriptor indexing or
timeline semaphore paths behind capability checks. The primary performance target is CPU submission
cost: draw collection and batching must happen before any platform-driver calls.
