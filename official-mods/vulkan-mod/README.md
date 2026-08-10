# Vulkan Mod

`vulkan_mod` is an experimental, client-only official mod that will replace the desktop
Slick/OpenGL renderer with a Vulkan renderer. It is not a gameplay dependency and must never affect
multiplayer compatibility.

The foundation build only probes Vulkan. It deliberately leaves the existing renderer active.
Use `-Drusted.fabric.vulkan.mode=off|probe|required`; `probe` is the development default, while
`required` makes an unavailable driver fail startup.

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
2. Add native-window surface creation, queue-family selection, swapchain recreation and frame
   synchronization while Slick still owns the desktop window and input loop.
3. Implement GPU images, render targets, clipping, transforms, primitive drawing and a persistent
   mapped vertex/index ring; translate the game's `GraphicsEngine` calls into frame-local commands.
4. Batch sprites by render state and texture descriptors, record a small number of command buffers,
   and remove per-sprite Java-to-native submissions.
5. Replace Slick font and LibRocket glue, then remove the remaining OpenGL frame path.
6. Add the Android JNI platform driver, surface lifecycle and device-loss handling.

The mobile baseline should prefer Vulkan 1.1-era features and keep optional descriptor indexing or
timeline semaphore paths behind capability checks. The primary performance target is CPU submission
cost: draw collection and batching must happen before any platform-driver calls.
