# RustedVK FrameStream ABI

Status: the Java envelope, structural/record verifier, fixed arena pool, shared batching, vertex
encoder, and LWJGL3 desktop decoder are implemented. Native desktop mode consumes FrameStream by
default through three reusable direct arenas; dependent-target readback, custom-vertex, and
20,000-batch GPU tests cover the new path. Adjacent quads are encoded as four unique vertices and
six `uint16` indices, with deterministic batch splitting at the 16-bit vertex limit; the desktop
decoder consumes both vertex and index sections. Version 1 becomes frozen after wider in-game visual
equivalence testing.

## Purpose

Rusted Warfare game logic, Rusted Fabric Loader, Fabric API, and Java mods remain Java and remain
the same on Windows and Android. Platform renderers consume one complete frame description rather
than receiving one JNI call per draw operation.

```text
Game / Java mods
        |
VulkanGraphicsEngine
        |
shared FrameEncoder
        |
RustedVK FrameStream ABI
        +--------------------------+
        |                          |
desktop LWJGL3 decoder       Android JNI/C++ decoder
        |                          |
      Vulkan                     Vulkan
```

FrameStream is owned and versioned by RustedVK. It is not initially part of the public Rusted
Fabric API. The Android RFL runtime transports it and owns the Android surface lifecycle, but does
not interpret rendering semantics. Promotion to a public renderer API requires a separately
reviewed stability commitment.

## Decisions

The following decisions are normative:

1. Java builds a whole frame before crossing the platform boundary.
2. Three reusable direct-memory frame arenas are registered once. A hot-path submit transfers an
   arena index, used length, and frame ID; it does not pass Java render objects.
3. Display frames and reliable resource/control operations use separate ordered streams.
4. A Java arena is reusable after the native decoder has copied/encoded its contents. It does not
   wait for the GPU fence unless the GPU directly references that arena, which version 1 forbids.
5. The initial back-pressure policy blocks the Java game thread when every frame arena is owned by
   the decoder. The queue is bounded and may never grow by allocating more frame objects.
6. Draw order is semantic. The encoder may merge only consecutive compatible draws and may not
   globally sort transparent content by texture or pipeline.
7. Handles are typed and generation-checked. Zero is always null.
8. Every decoder validates all offsets, sizes, counts, enum values, handles, and arithmetic before
   reading or allocating.
9. Android native rendering has no runtime dependency on AWT image or font classes.

## Existing-code relationship

`VulkanFrameCommands`, `VulkanRenderTargetPass`, and `VulkanFrameSubmission` are the current
renderer-neutral object representation. During migration they remain the authoritative input to a
new shared `FrameEncoder`:

```text
VulkanFrameSubmission (ordered Java objects)
        |
FrameEncoder (shared batching and vertex packing)
        |
FrameStream (validated binary representation)
```

The current desktop driver's results define compatibility. In particular:

- render-target passes execute in list order;
- a pass may sample targets written by an earlier pass;
- transforms are applied without changing local custom-shader inputs;
- clip, blend mode, filter, shader state, primary texture, and secondary texture participate in
  batch compatibility;
- a Java textured-quad batch may contribute many indexed quads to one ordinary version-1 batch
  record; this reduces producer command objects without adding a text-specific wire record;
- only adjacent compatible draws become one batch.

Batching moves into the shared encoder before the Android decoder is considered complete. This
prevents Windows and Android from developing different clip, blend, and shader grouping rules.

## Stream separation

### FrameStream

FrameStream contains presentation work that is immutable after submission:

- ordered offscreen passes and the final swapchain pass;
- clear, viewport, and target information;
- ordered batches and material/shader snapshots;
- vertex and optional index data;
- the highest reliable resource sequence required by the frame.

A frame is replaceable only when it has no synchronous result and the decoder has not begun
reading it. Version 1 uses blocking back pressure; latest-wins replacement is a later opt-in
policy.

The shared Java producer's arena path writes the fixed header, directory, pass records, batch
records, packed vertices, and materials directly into the acquired direct arena. Its pass, batch,
and material metadata are retained as a grow-on-demand workspace and reused after warm-up. The
allocating `encode()` implementation remains as a reference/standalone convenience path; ABI tests
require `encodeTo()` to remain byte-identical to it. Low-frequency diagnostics expose encoded
bytes/time, arena capacity retries, and workspace growth counts.

### ResourceStream

ResourceStream is reliable, ordered, and never dropped. It contains:

- texture and render-target creation, upload, mutation, and destruction;
- glyph-atlas region uploads;
- shader-program registration and destruction;
- pipeline-cache control;
- explicit readback and flush operations;
- lifecycle barriers that affect later frames.

Large pixel payloads use separately registered resource arenas. They do not consume the three
frame arenas. Each resource record receives a monotonically increasing 64-bit sequence. A frame
with `requiredResourceSequence = N` cannot be decoded until every resource record through `N` has
been applied.

The shared reference client owns three 16 MiB resource arenas by default and uses external records
for transfers of at least 256 KiB. The count, initial size, and threshold are diagnostic properties
`resourceArenaCount`, `resourceArenaMiB`, and `externalUploadKiB` under the
`rusted.fabric.vulkan` namespace. Arenas grow geometrically at a lease-free safe point up to
256 MiB each. Every external record requires a consumption completion ID. Its arena lease remains
owned after submission and becomes reusable only after the ordered decoder has copied the
referenced range into driver-owned memory and that completion has been observed. If all arenas are
owned, the next writer waits for the oldest completion; arena growth first drains every owner.
Registration now returns an opaque arena descriptor containing the verified ID, capacity, and an
optional stable native base address. The LWJGL3 reference driver exposes a real address; shared
Java code only range-checks it and never dereferences it. A JNI backend obtains and owns the same
address through `GetDirectBufferAddress` for the full registration lifetime.

Desktop ResourceStream decoding is performed by one ordered daemon worker behind a fair bounded
64-submission gate. The submitting thread validates the complete envelope and reserves its
contiguous sequence before enqueueing. A FrameStream waits when its `requiredResourceSequence` has
been accepted but not decoded; a sequence beyond the accepted tail is rejected immediately.
Decode failure faults the resource channel and wakes every dependency/completion waiter.

The LWJGL3 reference driver stages decoded texture transfers through persistently mapped,
geometrically growing upload buffers owned by individual frame/offscreen/readback submission
slots. Reusing or growing a slot is allowed only after that slot's fence completes. Initial data
for a newly created image therefore does not drain unrelated in-flight work; a mutation of an
already initialized image still waits for all submissions that could be sampling the old contents.
The driver exposes decode queue, dependency wait, arena wait, upload volume, slot growth, and
mutation-fence counters through `VulkanPlatformDriver.performanceStatistics()`.

Destruction is logically ordered but physically deferred until no queued frame references the
handle and all relevant native GPU frame slots have completed.

ResourceStream uses the same scalar and alignment rules as FrameStream. Its fixed 48-byte header is:

| Offset | Size | Field | Meaning |
|---:|---:|---|---|
| 0 | 4 | magic | ASCII `RVKR` |
| 4 | 2 | majorVersion | initially `1` |
| 6 | 2 | minorVersion | initially `0` |
| 8 | 4 | headerBytes | initially `48` |
| 12 | 4 | totalBytes | complete resource-stream length |
| 16 | 8 | firstSequence | sequence of the first record |
| 24 | 4 | recordCount | ordered record count |
| 28 | 4 | flags | integrity/completion flags |
| 32 | 4 | payloadCrc32 | optional debug checksum |
| 36 | 4 | reserved | must be zero |
| 40 | 8 | completionId | zero unless a reliable result is requested |

Every ordered resource record begins with a 32-byte header:

| Offset | Size | Field |
|---:|---:|---|
| 0 | 2 | recordType |
| 2 | 2 | flags |
| 4 | 4 | headerBytes |
| 8 | 4 | recordBytes, including header and aligned payload |
| 12 | 4 | reserved |
| 16 | 8 | sequence |
| 24 | 8 | typed resource handle, or zero for global control records |

Records are contiguous, 8-byte aligned, and carry type-specific payloads bounded by
`recordBytes`. Initial record types cover texture/render-target create, full upload, region update,
destroy, custom-program create/destroy, readback, flush, and lifecycle barrier. Exact type payloads
are frozen alongside the version-1 encoder rather than inferred from C++ structures.

Initial record type values are:

| Value | Record |
|---:|---|
| 1 | texture create |
| 2 | full texture upload |
| 3 | texture region update |
| 4 | texture destroy |
| 5 | render-target create |
| 6 | shader-program create |
| 7 | shader-program destroy |
| 8 | texture readback |
| 9 | flush |
| 10 | lifecycle barrier |

Record flags `HAS_EXTERNAL_PAYLOAD=1` and `EXPECTS_RESULT=2` are defined in version 1. A stream
containing any `EXPECTS_RESULT` record must set `REQUIRES_COMPLETION` and carry a positive
`completionId`; the inverse is also required. Types with record bit 15 set are required extensions.

Version-1 texture-create and render-target-create payloads are 32 bytes:

| Offset | Size | Field |
|---:|---:|---|
| 0 | 4 | width |
| 4 | 4 | height |
| 8 | 4 | mip levels |
| 12 | 4 | format; `1=RGBA8_UNORM` |
| 16 | 4 | usage bits: sampled/color attachment/transfer source/transfer destination |
| 20 | 4 | sampler bits: nearest/clamp-to-edge |
| 24 | 8 | reserved, zero |

Full uploads and region updates share a 48-byte prefix. It contains signed-origin-checked X/Y,
width/height, row stride, format, exact data byte count, a zero reserved word, then a 64-bit upload
arena ID and byte offset. Inline records set both arena fields to zero and append exactly
`dataBytes`; external records set `HAS_EXTERNAL_PAYLOAD`, carry no inline bytes, and identify a
previously registered bounded upload arena. Version 1 requires `dataBytes=rowStride*height`.

Readback uses a 32-byte region/format/row-stride payload and must set `EXPECTS_RESULT`. Lifecycle
barriers use `{scope:u32, flags:u32, waitThroughSequence:u64}`. Destroy and flush have no payload.

Shader-program create begins with a 24-byte prefix containing language (`1=Vulkan GLSL`), flags,
UTF-8 name length, vertex-source length, fragment-source length, and a zero reserved word. The
three un-terminated byte strings follow in that order. A fragment-only program omits vertex bytes;
`SHADER_HAS_VERTEX_SOURCE=1` requires a non-empty vertex source. Invalid UTF-8 is rejected before
the platform compiler sees it.

Logical handles are allocated by the shared Java resource manager before a create record is
submitted. Native mirrors the typed slot/generation table. Java may immediately reference the
handle in a later frame because `requiredResourceSequence` prevents that frame from overtaking its
create. An asynchronous create failure faults the renderer backend and produces a structured
diagnostic; it never leaves a silently usable half-created handle.

## Scalar representation

- Byte order: little-endian.
- Integer representation: unsigned unless a field explicitly says signed.
- Floating point: IEEE-754 binary32.
- Boolean representation: flag bits, never JVM boolean layout.
- Record and section starts: 8-byte aligned.
- Strings: UTF-8 with an explicit byte length and no required terminator.
- All padding bytes: zero in a conforming encoder and ignored by compatible decoders.
- Maximum version-1 stream size: 256 MiB. Implementations should configure a lower normal limit.

No native structure is cast directly over untrusted bytes. Decoders use checked little-endian
loads so C/C++ padding and compiler ABI choices cannot change the format.

## Frame header

Version 1 uses a 64-byte fixed header followed by a section directory.

| Offset | Size | Field | Meaning |
|---:|---:|---|---|
| 0 | 4 | magic | ASCII `RVKF` |
| 4 | 2 | majorVersion | incompatible format version; initially `1` |
| 6 | 2 | minorVersion | backward-compatible additions; initially `0` |
| 8 | 4 | headerBytes | header plus section-directory bytes |
| 12 | 4 | totalBytes | complete validated stream length |
| 16 | 8 | frameId | monotonically increasing, never reused in one process |
| 24 | 8 | requiredResourceSequence | reliable resource dependency |
| 32 | 4 | flags | frame feature and synchronization flags |
| 36 | 4 | width | logical presentation width in physical pixels |
| 40 | 4 | height | logical presentation height in physical pixels |
| 44 | 4 | sectionCount | number of directory entries |
| 48 | 4 | passCount | total pass records |
| 52 | 4 | batchCount | total ordered batch records |
| 56 | 4 | payloadCrc32 | zero normally; optional debug integrity check |
| 60 | 4 | reserved | must be zero |

Every section-directory entry is 16 bytes:

| Offset | Size | Field |
|---:|---:|---|
| 0 | 4 | sectionType |
| 4 | 4 | offset |
| 8 | 4 | byteLength |
| 12 | 4 | elementCount |

Required version-1 sections are `PASSES`, `BATCHES`, `VERTICES`, and `MATERIALS`. `INDICES` and
`DEBUG_LABELS` are optional. Unknown optional sections are skipped; unknown required sections fail
the frame. Section ranges may not overlap and must remain within `totalBytes`.

Initial section type values are:

| Value | Section |
|---:|---|
| 1 | `PASSES` |
| 2 | `BATCHES` |
| 3 | `VERTICES` |
| 4 | `INDICES` |
| 5 | `MATERIALS` |
| 6 | `DEBUG_LABELS` |

Types with bit 31 set are required extensions; an unknown required type rejects the frame. Unknown
types without bit 31 set are optional and skipped after their ranges have still been validated.

Initial frame flags are:

| Bit | Flag | Meaning |
|---:|---|---|
| 0 | `HAS_PAYLOAD_CRC32` | `payloadCrc32` covers bytes from `headerBytes` to `totalBytes` |
| 1 | `REQUIRES_COMPLETION` | completion is a reliable synchronization point |
| 2 | `REPLACEABLE_PRESENT` | eligible for a negotiated future latest-wins policy |
| 3 | `HAS_DEBUG_LABELS` | debug labels are present and validated |

All other version-1 bits must be zero unless negotiated as a minor-version feature.

## Passes

Passes are stored in dependency order. Target handle zero means the final platform swapchain;
nonzero means a render-target texture. Exactly one swapchain pass exists and it is last.

Each pass describes:

- typed target handle;
- first batch and batch count;
- clear/load/store flags and RGBA clear value;
- viewport in physical pixels;
- pre-rotation/target-orientation flags;
- optional debug label index.

Each version-1 pass record is 64 bytes:

| Offset | Size | Field |
|---:|---:|---|
| 0 | 8 | typed target handle; zero for swapchain |
| 8 | 4 | first batch index |
| 12 | 4 | batch count |
| 16 | 4 | flags: `CLEAR_COLOR=1`, `STORE=2`, `SWAPCHAIN=4` |
| 20 | 4 | viewport X, signed pixels |
| 24 | 4 | viewport Y, signed pixels |
| 28 | 4 | viewport width |
| 32 | 4 | viewport height |
| 36 | 16 | clear RGBA, four binary32 values |
| 52 | 4 | debug-label index, `0xffffffff` for none |
| 56 | 4 | target orientation; zero in version 1 |
| 60 | 4 | reserved, zero |

The encoder removes a pass only when it proves that the pass has no draws, no required clear or
side effect, and no later consumer. It may combine compatible target work without violating the
ordered dependency graph.

## Batches

A batch is an ordered run of primitives with compatible state. It contains:

- material record index;
- primary and optional secondary typed texture handles;
- clip rectangle or an explicit no-clip flag;
- blend mode and texture filter;
- vertex offset and count;
- optional index offset, count, and index type;
- primitive topology;
- feature flags needed by the selected vertex layout.

Each version-1 batch record is 64 bytes:

| Offset | Size | Field |
|---:|---:|---|
| 0 | 4 | material index |
| 4 | 4 | flags: `HAS_CLIP=1`, `TEXTURED=2`, `INDEXED=4` |
| 8 | 8 | typed primary texture handle, or zero |
| 16 | 8 | typed secondary texture handle, or zero |
| 24 | 4 | byte offset in `VERTICES` |
| 28 | 4 | vertex count |
| 32 | 4 | byte offset in `INDICES`, zero when not indexed |
| 36 | 4 | index count, zero when not indexed |
| 40 | 16 | clip X/Y/width/height as binary32; all zero when disabled |
| 56 | 2 | topology; `1` is triangle list |
| 58 | 2 | index type: none/uint16/uint32 = `0/1/2` |
| 60 | 2 | vertex layout |
| 62 | 2 | reserved, zero |

Version 1 supports triangle lists. Quads are encoded as indexed or expanded triangles according to
the shared encoder's selected vertex format. A decoder cannot reinterpret batch order.

The shared encoder bakes ordinary affine transforms into packed vertex positions. Custom vertex
programs retain local coordinates and carry the six affine coefficients plus frame dimensions in
each expanded vertex, matching current RustedVK behavior.

Version-1 vertex layouts are:

| Value | Bytes | Binary32 components |
|---:|---:|---|
| 1 | 24 | NDC position XY, color RGBA |
| 2 | 32 | NDC position XY, UV, color RGBA |
| 3 | 64 | local XY, UV, color RGBA, affine 2x3, frame width/height |

`VERTICES.elementCount` is the total vertex count, not its byte length. Batch byte ranges are
tightly ordered in version 1; a decoder rejects gaps, overlap, or unreferenced trailing bytes.

## Materials and shaders

A material is an immutable snapshot used by one or more batches. It includes the existing
`VulkanShaderState` semantics:

- built-in effect or typed custom-program handle;
- tint and alpha;
- team-color amount;
- screen base, resolution, displacement offset, and UI scaling;
- bounded custom scalar/vector values;

Texture handles, including the optional secondary sampler, live in the batch so one material can
be reused with different images. Each version-1 material record is 160 bytes:

| Offset | Size | Field |
|---:|---:|---|
| 0 | 4 | flags, zero in version 1 |
| 4 | 4 | blend: normal/additive/copy/modulate = `0..3` |
| 8 | 4 | filter: linear/nearest = `0/1` |
| 12 | 4 | `VulkanShaderState` effect |
| 16 | 8 | typed custom shader-program handle, or zero |
| 24 | 44 | tint RGBA, team amount, screen-base WH, resolution WH, displacement, UI scaling |
| 68 | 4 | custom-value count, at most 20 |
| 72 | 8 | reserved, zero |
| 80 | 80 | 20 binary32 custom-value slots; unused slots are zero |

Material records are deduplicated within one frame only. Native caches may intern equivalent
pipeline objects across frames, but may not retain pointers into a Java arena.

Custom shader compilation is a reliable ResourceStream operation. A frame references only the
resulting typed, generation-checked program handle.

## Resource handles

Version 1 handles are unsigned 64-bit values:

```text
63              56 55                    32 31                         0
+-----------------+------------------------+----------------------------+
| resource type   | generation (24 bits)   | slot index (32 bits)       |
+-----------------+------------------------+----------------------------+
```

Initial resource types are:

| Value | Type |
|---:|---|
| 0 | invalid/null |
| 1 | sampled texture or render target |
| 2 | custom shader program |
| 3 | pipeline-family resource |
| 4 | font/glyph-atlas resource |

The namespace implied by a record field must agree with the encoded type. Slot reuse increments
the generation. A stale generation, wrong type, unknown slot, or reference to a resource whose
create sequence is newer than `requiredResourceSequence` rejects the frame with a diagnostic.

Generation wrap does not silently make an old handle valid. A table whose generation would wrap
must retire that slot or reset the renderer process at a controlled boundary.

## Frame arena ownership

Three frame arenas are allocated with `ByteBuffer.allocateDirect`, forced to little-endian, and
registered once with the native backend. Registration caches their stable address and capacity.

The synchronous LWJGL3 decoder already reuses this bounded set and creates no per-frame direct
buffer. Its default is three 16 MiB arenas, configurable for diagnostics with
`-Drusted.fabric.vulkan.frameArenaMiB=N`. JNI address registration begins with the asynchronous
native decoder; until then desktop receives a read-only view of the same stable arena.

```text
FREE
  -> WRITING       Java acquired the arena
  -> QUEUED        submit transferred ownership to native
  -> DECODING      native validates and records/copies the frame
  -> FREE          native no longer reads the arena
```

The GPU never reads the Java arena. Vulkan vertex/index data is copied into a persistently mapped
native ring or another native-owned allocation before `FREE` is signalled. GPU fence ownership is
therefore tracked independently by native frame slots.

The Java/native boundary is conceptually:

```java
nativeRegisterFrameArena(int index, ByteBuffer memory, int capacity);
int nativeAcquireFrameArena(long timeoutNanos);
nativeSubmitFrame(int index, int usedBytes, long frameId);
```

The exact Java method names are not ABI. The ownership behavior is.

An arena may not be resized while registered. If a valid frame exceeds capacity:

1. finish or cancel all current owners at a safe renderer boundary;
2. allocate a geometrically larger bounded set;
3. register the replacement set atomically;
4. release the old direct buffers only after native unregisters them.

Normal rendering never allocates an unbounded fourth arena. Malformed or unreasonable growth
requests fail with a clear diagnostic rather than risking native memory exhaustion.

## Back pressure and latency

Version 1 defaults to `BLOCK`:

- at most one arena is being written by Java;
- at most two are queued/decoded;
- the game thread waits when none are free;
- no hidden list of pending frames may grow behind the three arenas.

This preserves the current game's update/render coupling while the asynchronous decoder is proved.
A future `LATEST_WINS` policy may replace the oldest `QUEUED` frame only when:

- native has not entered `DECODING`;
- the frame has no readback, flush, screenshot, or other completion result;
- all reliable resource dependencies remain independently ordered;
- no retained offscreen result is consumed outside the replaceable presentation sequence.

The runtime exposes queue wait, encode, decode, command-record, submit, GPU, and present timing to
the performance profiler. Arena starvation is reported explicitly rather than appearing as an
unexplained game-loop stall.

## Reliable synchronization points

The following operations force ordered progress and cannot be dropped:

- texture or render-target readback;
- explicit CPU pixel-buffer flush;
- screenshot completion;
- surface/device shutdown;
- renderer backend switch;
- resource-table reset;
- any API that promises the caller a result from preceding drawing.

The common fast path stays asynchronous. A synchronization point carries a 64-bit completion ID;
native signals success or a structured failure only after its documented dependency is complete.
Submission acknowledgement and completion are separate SPI states. `pending(sequence, id)` means
the ordered records were accepted but their result is not ready; `poll` is non-blocking and
`await(id, timeout)` is the bounded blocking path. The desktop reference driver now dispatches
texture readback to a daemon completion worker and performs the actual Vulkan wait under the
driver lock. It waits on a fence dedicated to that readback submission instead of idling the whole
device/queue, so this state transition is covered rather than simulated by an immediate result.

## Decoder validation

Before recording Vulkan commands, the decoder must verify at least:

- header magic, supported version, flags, and zeroed reserved fields;
- total length and all aligned non-overlapping section ranges;
- multiplication/addition without integer overflow;
- declared counts against section byte lengths and configured maxima;
- pass and batch ranges, including the final swapchain-pass invariant;
- finite coordinates, colors, UVs, matrices, and shader values;
- valid enum and topology values;
- clip and viewport conversion without signed overflow;
- handle type, slot, generation, lifetime, and resource sequence;
- vertex/index ranges and every index value;
- shader/material limits before pipeline lookup or allocation.

Validation failures reject the complete frame. Release builds log a rate-limited structured error;
developer builds may additionally dump the frame header and section directory. A bad Java mod must
not turn malformed drawing state into an unchecked native memory access.

## Version negotiation

The Java renderer and native backend exchange:

- supported major/minor range;
- supported section and feature bits;
- maximum arena and resource-upload sizes;
- supported vertex layouts, index types, shaders, formats, and synchronization modes.

Major-version mismatch disables the backend cleanly. Minor additions are usable only when the
corresponding feature bit is accepted. Silent reinterpretation is forbidden.

## Implementation stages

1. **Done:** constants and a checked Java FrameStream writer/reader with golden-byte tests.
2. **Done:** encode the current `VulkanFrameSubmission` into ordered passes and batches.
3. **Done:** move adjacent batching and vertex packing into the shared `FrameStreamEncoder`.
4. **Done:** make the LWJGL3 desktop driver decode FrameStream directly into persistent mapped
   vertex/index buffers, including validated indexed quad batches. The old object submission is
   available only with the diagnostic JVM property
   `-Drusted.fabric.vulkan.objectSubmission=true` while in-game captures are compared.
5. **Desktop done:** add three fixed direct arenas, live bounded submission, geometric safe-point
   growth, and blocking ownership/back-pressure tests. JNI address registration follows with the
   asynchronous native decoder.
6. **Desktop resource path live:** add the reliable ResourceStream header/record writer,
   hostile-input verifier, typed handles, CRC, completion, contiguous sequence tests, and exact
   texture/shader/control payload codecs. The shared client now allocates generation-checked
   logical texture and shader-program handles; desktop synchronously decodes texture
   create/upload/full update/render-target create/destroy and shader create/destroy records, maps
   them to native Vulkan resources, and rejects a FrameStream whose `requiredResourceSequence` is
   not applied. Registered bounded external upload arenas, ordered partial RGBA8 updates, and
   completion-ID full and partial-region texture readback results are live and covered by real-GPU
   tests. Arena
   registration returns a stable native-address descriptor. Desktop ResourceStream decoding now
   uses a bounded ordered worker, frames wait for decoded resource dependencies, and external arena
   leases remain owned through their consumption completion. Readback separately exercises the
   accepted/pending/ready protocol and a dedicated Vulkan fence. The actual JNI registration calls,
   C++ decoding remain.
7. Implement the Android C++ verifier/decoder against the same golden files.
8. Add asynchronous native recording only after synchronous decoding is visually equivalent.

The Windows driver remains the reference renderer during stages 1-6. A future shared C++ renderer
for Windows and Android is optional and does not block the Android backend.

## Acceptance criteria

- Golden streams decode identically in Java, LWJGL3, and C++ tests.
- Unknown, truncated, overlapping, overflowing, and stale-handle inputs fail deterministically.
- Repeating a peak workload allocates no new frame arena or per-batch metadata after warm-up.
- Queue depth remains bounded under an intentionally stalled decoder.
- A freed arena is never read again by native and is independent from GPU fence completion.
- Resource create/update/destroy order survives dropped or blocked presentation frames.
- Existing terrain, minimap, LibRocket, blend, clip, built-in shader, custom shader, and readback
  verification suites pass through the encoded path.
- Android background/foreground and Surface recreation do not invalidate Java resource handles.
