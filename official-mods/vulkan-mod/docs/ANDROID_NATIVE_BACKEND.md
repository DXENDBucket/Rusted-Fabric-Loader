# Android native Vulkan backend

Status: implementation plan. This backend does not exist yet; the current Android desktop-JVM port
uses its working LWJGL2/GL4ES compatibility renderer.

The binary renderer contract is defined in [FRAME_STREAM_ABI.md](FRAME_STREAM_ABI.md).

## Goal

Run the same desktop Java game, Fabric Loader, Rusted Fabric API, and Java mods on Android while
moving graphics submission, Vulkan WSI, and GPU resource ownership into an ARM64 NDK backend.

Rewriting game logic in C++, patching a user APK, and creating Android-only Java mod APIs are out of
scope.

## Ownership boundary

The Android APK/runtime owns platform facilities:

```text
Android RFL runtime
|- Activity and SurfaceView
|- Surface lifecycle and display/cutout information
|- isolated desktop-JVM process
|- ANativeWindow bridge
|- packaged arm64-v8a native Vulkan backend
`- crash and capability diagnostics
```

RustedVK owns rendering semantics:

```text
vulkan-mod.jar (same Java mod on Windows and Android)
|- VulkanGraphicsEngine
|- FrameEncoder and FrameStream version
|- shader/material semantics
|- resource dependency model
`- backend negotiation
```

The `.so` is packaged by the APK because Android owns native-library ABI packaging and the
`ANativeWindow` lifetime. It remains an optional backend: when RustedVK is disabled or capability
negotiation fails, the library is not loaded. Ordinary third-party Java mods never package native
renderer libraries.

The host/runtime boundary supplies a `Surface`; native obtains and reference-counts its
`ANativeWindow`. Java never exposes an integer-cast native pointer to mods.

## Runtime flow

```text
SurfaceView.surfaceCreated(surface)
        |
RFL host registers Surface in :desktop_jvm process
        |
RustedVK selects Android Vulkan backend before legacy Display.create()
        |
JNI obtains ANativeWindow and creates VkSurfaceKHR
        |
capability/FrameStream negotiation
        |
Java registers frame and resource arenas
        |
game thread encodes frames ----> C++ render thread ----> Vulkan queue/present
```

The device and long-lived GPU resources survive ordinary Surface loss. Surface, swapchain, image
views, and presentation-dependent objects are recreated when the Surface returns. Device loss is a
separate fatal/recovery path and may require rebuilding every native resource from Java-owned
source data.

## JNI surface

JNI stays narrow and coarse-grained. Expected operation groups are:

- backend probe and ABI negotiation;
- Surface attach/change/detach;
- one-time frame/resource arena registration;
- frame-arena acquire and one submit per frame;
- reliable resource-stream submit;
- rare synchronous completion/readback;
- statistics polling and shutdown.

There is no JNI entry point for a quad, triangle, clip change, texture bind, uniform, or draw call.
Native caches direct-buffer addresses during registration and validates the used length on every
submit.

## Thread model

The Java game thread remains responsible for game update, mods, UI semantics, and FrameStream
encoding. A native render thread validates and decodes submitted arenas, records Vulkan command
buffers, and presents them.

```text
Java game thread:   update N+1 | encode N+1 | bounded wait/submit
Native render:      decode N   | record N   | submit N
GPU:                             execute N-1
```

Version 1 begins with blocking arena back pressure. The game cannot queue an unlimited number of
frames. Native releases a Java arena after decoding/copying, while its own frame slots and Vulkan
fences protect GPU-visible allocations.

Platform lifecycle callbacks never destroy Vulkan objects concurrently with the render thread.
They enqueue typed control messages and wait only at the documented attach/detach boundary.

## Android graphics baseline

Recommended native path:

- Android 10 / API 29 or newer;
- arm64-v8a;
- Vulkan 1.1;
- required formats and limits compatible with the selected RustedVK baseline;
- a device/driver not present in the reviewed deny list.

This is a recommended capability tier, not necessarily the APK minimum. Runtime probing chooses:

```text
Vulkan capability and driver accepted -> RustedVK native
not accepted or user disabled          -> current OpenGL/GL4ES renderer
```

Descriptor indexing, timeline semaphores, dynamic rendering, 16-bit shader arithmetic, and other
newer features remain optional capability tiers. The baseline must not accidentally depend on a
desktop-only Vulkan feature.

## Presentation and frame pacing

The backend integrates Android Game Development Kit Frame Pacing (Swappy) after basic correctness.
It does not present as fast as the CPU can submit.

Initial policy:

- 60 Hz display: target 60 FPS;
- 90 Hz display: target 90 FPS when sustained performance permits;
- 120 Hz display: default to a user-selected 60/90/120 tier rather than forcing 120;
- background or non-visible Surface: stop expensive game/render frames;
- thermal pressure: permit a lower stable tier rather than oscillating frequency.

Acquire and present are not used as implicit synchronization for unrelated work. Explicit
semaphores, fences, and barriers retain the same correctness responsibilities as the desktop
driver.

## Orientation and physical pixels

Swapchain creation uses the Surface capability's current transform and implements Vulkan
pre-rotation. Viewport, scissor, input coordinates, display cutouts, and logical game dimensions
derive from one explicit physical-pixel transform.

This avoids an implicit full-screen compositor rotation and prevents the one-pixel clip/input
differences that arise when Windows and Android independently round coordinates.

Surface resize and orientation changes produce an ordered control event. Swapchain recreation does
not change the FrameStream ABI or resource handles.

## Mobile GPU policy

Android GPUs are commonly tile based. The backend therefore prioritizes:

- one combined command submission for the ordered frame graph when dependencies allow;
- merging compatible work on the same attachment;
- avoiding empty passes and unnecessary attachment load/store operations;
- no depth attachment for the ordinary 2D path unless a feature demonstrably needs it;
- persistently mapped bounded staging/vertex rings;
- batched texture uploads outside the presentation hot path;
- no normal-path texture readback;
- compact vertex formats and reduced precision only after visual/profiling validation;
- pipeline and shader caches keyed by device, driver, ABI, and shader version.

Global texture sorting remains forbidden because Rusted Warfare relies on ordered alpha blending.
Only adjacent compatible work is combined.

## Text and image services

AWT classes are forbidden from the Android native rendering path:

```text
java.awt.image.BufferedImage
java.awt.Graphics2D
javax.imageio.ImageIO
java.awt.Font
```

The current whole-string AWT texture cache is replaced before Android Vulkan is considered
production ready. The target text path is:

```text
layout request
  -> platform glyph rasterizer
  -> shared glyph-atlas allocator
  -> reliable atlas-region upload
  -> ordinary glyph batches in FrameStream
```

The rasterizer must support the game's Latin and CJK fallback fonts. Glyphs are reused across
strings; a new sentence does not create a new GPU texture. Atlas eviction is generation-safe and
cannot invalidate a queued frame.

Image decoding also sits behind a platform-neutral service. Android may use a reviewed native or
Android decoder, but the renderer-facing result is explicit RGBA data or a supported compressed
format. No AWT object crosses FrameEncoder.

ASTC or other compressed assets are optional optimizations for Loader-owned resources. Arbitrary
user mods and hot-reloaded images must continue to work without offline conversion.

## Resource and memory lifecycle

Java-visible renderer resources use the typed generation handles specified by FrameStream. Native
tables own Vulkan images, memory, samplers, descriptors, shader modules, and pipelines.

- Create/update/destroy commands are reliable and ordered.
- A frame declares its required resource sequence.
- Destruction is deferred beyond every referencing queued frame and GPU fence.
- Surface loss does not destroy device-local texture tables.
- Device loss invalidates the native table as one generation epoch and triggers controlled rebuild
  or a diagnostic fallback.
- Resource arenas and frame arenas have independent limits and back pressure.
- Memory-budget pressure is reported to Java before an unrecoverable allocation where possible.

No Java direct buffer is assumed to be Vulkan-allocatable or suitable for direct GPU access. The
baseline copies encoded data into native-owned Vulkan-visible rings.

## Packaging and updates

The APK contains ABI-specific native libraries under the normal Android packaging rules. It still
contains no Rusted Warfare game files. The user-owned desktop game ZIP/directory and imported Java
runtime remain separate as documented by the Android launcher.

`vulkan-mod.jar` can update Java protocol logic through the ordinary official-mod provisioning
path only when its required native ABI is supported by the installed launcher. Otherwise the mod
reports the minimum launcher/backend version and disables Native mode cleanly.

Launcher updates may replace the packaged native backend without forcing the user to re-import the
game or Java runtime.

## Diagnostics

Developer builds expose:

- selected GPU, Vulkan version, extensions, formats, and capability tier;
- FrameStream/native ABI negotiation;
- Java encode and arena-wait time;
- native validation, decode, command-record, queue-submit, GPU, and present time;
- frame/resource queue depth and high-water marks;
- swapchain recreation, Surface loss, thermal/frame-rate tier changes;
- native allocation counts and memory budgets;
- rate-limited driver and stale-handle failures.

Validation layers are opt-in development components and never enabled in a release performance
run. Android GPU Inspector, RenderDoc where supported, Perfetto, and the existing performance
profiler provide complementary evidence.

## Delivery stages

1. Finalize and test FrameStream on Windows; Android code is not started against an unstable object
   boundary.
2. Add an inert ARM64 native backend to the APK and verify ABI negotiation plus Surface lifecycle.
3. Decode and present clear-only FrameStreams.
4. Add colored/textured batches and reliable texture resources.
5. Add ordered offscreen passes, terrain cache, minimap, and Canvas targets.
6. Add built-in and custom shaders, readback, and hot mutation.
7. Replace AWT text/image hot paths and complete the glyph atlas.
8. Enable the native render thread, triple arenas, Swappy, and pre-rotation.
9. Run long-session, background/foreground, resize, thermal, low-memory, and multiple-GPU-vendor
   tests before making Native Vulkan a user-facing default.

## Definition of done

The Android backend is complete only when:

- the same `vulkan-mod.jar` and Java mods run on Windows and Android;
- no per-draw JNI calls or unbounded frame queue exist;
- the launcher still contains no game payload;
- terrain, minimap, UI, text, blending, filters, built-in/custom shaders, hot reload, and readback
  match the desktop reference;
- Surface recreation and repeated background/foreground cycles do not lose resources or hang;
- pacing is stable at the selected tier without queue stuffing;
- unsupported devices fall back with a useful diagnostic;
- performance is measured on physical Adreno, Mali, and at least one additional driver family.

