# Vulkan Mod

`vulkan_mod` is an experimental, client-only official mod that will replace the desktop
Slick/OpenGL renderer with a Vulkan renderer. It is not a gameplay dependency and must never affect
multiplayer compatibility.

The foundation build probes Vulkan and, after Slick creates its Win32 window, creates a live
surface, presentation-capable device/queues, swapchain, render pass, framebuffers, command buffers,
and synchronization objects. The normal modes deliberately leave the existing renderer active.
Use `-Drusted.fabric.vulkan.mode=off|probe|frame_test|required`; `probe` is the development default,
`frame_test` presents one dark-blue Vulkan clear after an OpenGL frame, and `required` makes an
unavailable driver fail startup. The one-shot test is diagnostic only, not renderer takeover.

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
3. Implement GPU images, render targets, clipping, transforms, primitive drawing and a persistent
   mapped vertex/index ring; translate the game's `GraphicsEngine` calls into frame-local commands.
4. Batch sprites by render state and texture descriptors, record a small number of command buffers,
   and remove per-sprite Java-to-native submissions.
5. Replace Slick font and LibRocket glue, then remove the remaining OpenGL frame path.
6. Add the Android JNI platform driver, surface lifecycle and device-loss handling.

The mobile baseline should prefer Vulkan 1.1-era features and keep optional descriptor indexing or
timeline semaphore paths behind capability checks. The primary performance target is CPU submission
cost: draw collection and batching must happen before any platform-driver calls.
