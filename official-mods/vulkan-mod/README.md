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
game thread. Native mode asks a platform text service to shape and rasterize individual glyphs into
reusable 1024x1024 atlas pages and emits indexed quads for visible glyphs. The Windows
implementation uses AWT inside the isolated desktop driver; no AWT font object crosses the shared
atlas boundary. Repeated strings and characters reuse atlas regions; the
older takeover compatibility path retains whole-string textures until that path is removed. Native
mode translates linked GLSL-130 vertex/fragment programs onto the
Vulkan texture ABI. Desktop built-ins and GDX attributes, custom float/vec uniforms shared across
both stages, custom float/vec varyings, and one shared secondary sampler are supported. The five
numeric-uniform limit is shared by the complete program. Custom draws retain their original local
coordinates and carry the active affine ModelView plus target projection into the vertex stage;
`gl_Vertex`, `gl_ModelViewMatrix`, `gl_ProjectionMatrix`, `gl_ModelViewProjectionMatrix`, and GDX
`u_projTrans` therefore keep their legacy order. Vertex texture sampling is supported. User-defined
`mat4` uniforms remain outside the contract because the game's `ShaderProgram` only publishes
one-, two-, and four-component parameter values.

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
CPU texture copies. Java mods can additionally attach compatible custom vertex/fragment pairs;
their uniforms are snapshotted per draw and pipelines are rebuilt across swapchain changes.

The shared renderer now collects each native frame as a platform-neutral frame graph: ordered
offscreen writes followed by their final presentation consumer. The desktop driver records graphs
of up to eight child passes into the presentation command buffer, so terrain, minimap and Canvas
dependencies use one graphics-queue submission per frame instead of one submission per target.
Deeper graphs retain an ordered correctness fallback. Independently fenced, persistently mapped
main/offscreen vertex rings protect reuse without a per-target device/queue idle; texture mutation
still performs the narrow shared-staging synchronization required for correctness. Dynamic
Canvas bitmap rebinding now reuses each image's single native target command stream, submits the
outgoing target, and carries the live transform/clip stack across the framebuffer switch. Native
render targets expose
synchronized RGBA readback and same-size CPU upload, so `GameImage` pixel reads/copies and explicit
pixel-buffer flushes retain their original semantics. Remaining shader work is broadening legacy
syntax beyond the parameter types exposed by the original game.
AWT is still the Windows rasterizer implementation, but it is no longer part of the shared text
cache contract and can be replaced by FreeType/Skia in an Android platform driver.

Desktop texture binding now shares one Vulkan sampler per filter mode instead of creating two
samplers for every image. Descriptor sets are allocated lazily on first sampling, cached for both
single- and dual-texture materials, and returned to a fence-safe recycler when their images retire.
Recycled sets are updated for the replacement image instead of being freed and allocated again;
repeated paired-material cache hits do not allocate Java lookup keys. Command recording also skips
a descriptor bind when a material or clip split keeps the same set and a compatible pipeline
layout. `descriptor.*` profiler counters expose allocations, recycler hits, cache hits/misses, and
executed/skipped binds.

Each render pass now records through one reusable command-state cache. Texture changes no longer
rebind an unchanged graphics pipeline, object submissions retain their shared vertex-buffer range,
and repeated clip rectangles, index-buffer ranges, and shader push constants are emitted only when
their effective value changes. The cache resets at every render-pass boundary, keeping standalone
offscreen work, frame-graph children, and presentation independent. `command.*Calls` and
`command.*Skips` counters expose the resulting pipeline, vertex/index, scissor, and push-constant
traffic. A real-device regression alternates 64 texture batches while requiring only one pipeline,
vertex-buffer, scissor, and push-constant command.

The isolated desktop backend has begun moving ownership out of its original monolithic session:
`VulkanCommandStateCache` owns pass-local command deduplication and its counters, while
`VulkanDescriptorAllocator` owns the shared sampler/layout/pool, lazy set allocation, and the
fence-released recycler. `Lwjgl3VulkanDriver` retains orchestration and image-to-descriptor cache
keys, but no longer implements those low-level lifecycles inline.

Native frame construction uses a thread-confined command arena. Its growable command array and
quad/triangle command objects are retained at peak capacity and recycled after the synchronous
platform-driver submission, while the ordinary public `builder(...)` path remains an immutable
snapshot for third-party drivers and tests. Typed compatibility lists are generated lazily from the
single ordered command stream instead of five eagerly copied lists. Consecutive equivalent
draw states are also shared, and desktop vertex packing writes colored and textured vertices in one
command traversal. The pooled builder now collapses adjacent ordinary sprites with the same
texture/material/clip into a single recyclable run even when their tint and affine transforms
differ. Its primitive arrays remain at peak capacity across frames, and an intervening draw always
ends the run so alpha ordering is unchanged. Driver-side frame-upload and draw-batch metadata are
retained at peak demand and
recycled after command recording; a large-batch regression verifies that repeated 20,000-command
frames do not allocate more metadata after their first frame. LibRocket geometry also reuses one
set of triangle scratch arrays for an entire geometry submission instead of allocating them per
triangle.
FrameStream quad runs now use four unique vertices plus six `uint16` indices per quad. Compatible
adjacent quads share one indexed batch, with an automatic split at the 65,536-vertex limit; mixed
triangle, line and circle work retains the ordinary non-indexed path. The desktop decoder uploads
the pass-local vertex and index ranges into the same persistently mapped frame slot and issues
`vkCmdDrawIndexed`, matching the future Android decoder contract while reducing quad stream bytes.
Atlas text additionally stores reusable relative glyph geometry and records one Java command per
consecutive atlas page rather than one command object per glyph. Cumulative `text.runs`,
`text.glyphQuads`, and `text.batchCommands` counters expose the achieved compaction to the profiler.
Ordinary sprite compaction is reported separately as `sprite.quads` and `sprite.runCommands`.

The pre-OpenGL native bootstrap reproduces the original loading screen directly through
`GraphicsEngine`: black background, centered game logo, animated `Loading` dots and the live loader
status. Desktop 1.15 loads synchronously despite the legacy method name, so Native mode performs
throttled immediate progress presents from the original status callbacks. This keeps the window
responsive and visible without moving game initialization onto a different thread. Loading text is
drawn from Slick's original AngelCode `defaultfont.fnt`/`defaultfont.png` atlas with its fixed-width
padding and integer placement, rather than approximated with the ordinary game UI font.

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
- `-Drusted.fabric.vulkan.debugFrameGraph=true` logs child passes encoded into the combined native
  frame submission.

Vulkan validation and Debug Utils are available with
`-Drusted.fabric.vulkan.validation=true`. Warning/error callbacks are the default; the additional
`-Drusted.fabric.vulkan.validationVerbose=true` enables info/verbose messages. A machine without
`VK_LAYER_KHRONOS_validation` reports that fact and continues without validation, so release and
performance runs remain unaffected.

## Boundary

- The main mod owns renderer-neutral textures, draw commands, batching, frame resources, and the
  implementation of Rusted Warfare's `GraphicsEngine` contract.
- `desktop-driver` owns LWJGL 3 and Vulkan binding objects. It is loaded child-first from embedded
  jars so LWJGL 3 cannot replace the game's LWJGL 2 classes.
- The current object SPI is a desktop implementation boundary, not the future JNI ABI. Shared
  batching will encode a validated whole-frame `FrameStream`; the Android adapter will submit
  registered arena indices to an NDK Vulkan decoder. No LWJGL or Win32 type crosses that boundary.
- Game-specific mixins, Slick compatibility and low-level takeover code stay in this mod. A hook is
  promoted to Rusted Fabric API only when another renderer or ordinary mod can reuse it safely.

## Stages

1. Probe the Vulkan loader, API version and physical devices without changing rendering.
2. Native Win32 window/input ownership, swapchain resize recreation, frame command submission and
   the pre-`Display.create()` bootstrap are complete.
3. Native `GraphicsEngine` images, Vulkan offscreen framebuffers, terrain/minimap cache submission,
   common blend modes, texture filtering, LibRocket geometry, built-in team/post shaders, and the
   secondary-texture displacement path are complete. Native image readback/upload covers legacy
   pixel-buffer mutation and dynamic Canvas target switching; compatible linked custom
   vertex/fragment programs are translated and run in native Vulkan pipelines.
4. Persistently mapped main/offscreen vertex/index rings, asynchronous standalone child submissions,
   combined frame-graph submission, reusable Java frame-command arenas and recyclable driver-side
   draw-batch metadata are complete. Indexed quad batches now reduce repeated vertices without
   changing order. Ordinary sprites with independent transforms/tints can now cross the
   Java/FrameStream boundary as a single recycled run command; a 12,000-sprite multi-frame GPU
   regression verifies stable producer, encoder, and driver metadata after warm-up. Next, continue
   widening compatible non-text batches and profile descriptors.
5. Native mode now uses a reusable glyph atlas with a bounded page count and per-frame glyph upload
   limit. Layout/rasterization is now a platform SPI; desktop AWT lives in the isolated driver and
   the common atlas is ready for an Android FreeType/Skia implementation. Next, add that Android
   implementation and remove the obsolete takeover-only whole-string compatibility surface after
   native parity is established.
6. Add the Android JNI platform driver, surface lifecycle and device-loss handling.

The mobile baseline should prefer Vulkan 1.1-era features and keep optional descriptor indexing or
timeline semaphore paths behind capability checks. The primary performance target is CPU submission
cost: draw collection and batching must happen before any platform-driver calls.

The next cross-platform boundary is specified in
[FrameStream ABI](docs/FRAME_STREAM_ABI.md). Android ownership, JNI, lifecycle, pacing, and
AWT-free rendering requirements are specified separately in the
[Android native backend plan](docs/ANDROID_NATIVE_BACKEND.md).
