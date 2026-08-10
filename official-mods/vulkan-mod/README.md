# Vulkan Mod

`vulkan_mod` is an experimental, client-only official mod that will replace the desktop
Slick/OpenGL renderer with a Vulkan renderer. It is not a gameplay dependency and must never affect
multiplayer compatibility.

The foundation build probes Vulkan and, after Slick creates its Win32 window, creates a live
surface, presentation-capable device/queues, swapchain, render pass, framebuffers, command buffers,
and synchronization objects. It also has a binding-neutral frame command list and a first batched
colored/textured-quad path backed by a growable host-visible vertex buffer. RGBA8 uploads use a
staging buffer, device-local images, samplers, and per-texture descriptor sets; consecutive quads
using the same texture share one draw call without changing the original colored/textured command
order. The normal modes deliberately leave the existing renderer active.
Game-owned `GameImage` objects can now be read back into an identity/version-aware Vulkan texture
cache. Image reload and release hooks invalidate stale GPU copies. Draw commands also carry an
affine screen-space transform and optional scissor rectangle; transforms are baked while batching,
and scissor changes split otherwise compatible batches.
Use `-Drusted.fabric.vulkan.mode=off|probe|frame_test|takeover_test|required`; `probe` is the
development default, `frame_test` presents one diagnostic Vulkan frame after an OpenGL frame, and
`required` makes an unavailable driver fail startup. `takeover_test` is the first real presentation
takeover: every visible game-loop frame captures supported Slick clears, filled rectangles and image
draws into an ordered Vulkan frame, then presents it after the legacy frame. Text, lines, circles,
tiling, shaders and direct Slick/LibRocket OpenGL still need Vulkan implementations, so this mode is
an intentionally incomplete developer experiment and is not suitable as the normal launcher mode.

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
3. Extend the current colored-quad path with GPU images, offscreen render targets, clipping,
   transforms, and a persistent mapped vertex/index ring; translate the game's `GraphicsEngine`
   calls into frame-local commands.
4. Batch sprites by render state and texture descriptors, record a small number of command buffers,
   and remove per-sprite Java-to-native submissions.
5. Replace Slick font and LibRocket glue, then remove the remaining OpenGL frame path.
6. Add the Android JNI platform driver, surface lifecycle and device-loss handling.

The mobile baseline should prefer Vulkan 1.1-era features and keep optional descriptor indexing or
timeline semaphore paths behind capability checks. The primary performance target is CPU submission
cost: draw collection and batching must happen before any platform-driver calls.
