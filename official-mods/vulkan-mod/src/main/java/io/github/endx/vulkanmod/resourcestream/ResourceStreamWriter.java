package io.github.endx.vulkanmod.resourcestream;

import io.github.endx.vulkanmod.framestream.FrameResourceHandle;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

/** Deterministic writer for reliable ordered resource records. */
public final class ResourceStreamWriter {
    private final long firstSequence;
    private final int flags;
    private final long completionId;
    private final List<PendingRecord> records = new ArrayList<PendingRecord>();

    public ResourceStreamWriter(long firstSequence, int flags, long completionId) {
        if (firstSequence < 0L) throw new IllegalArgumentException("firstSequence is negative");
        if ((flags & ~ResourceStreamFormat.KNOWN_FLAGS) != 0) {
            throw new IllegalArgumentException("unknown ResourceStream flags");
        }
        boolean completion = (flags & ResourceStreamFormat.FLAG_REQUIRES_COMPLETION) != 0;
        if (completion != (completionId > 0L)) {
            throw new IllegalArgumentException(
                    "completion flag requires one positive completion ID");
        }
        this.firstSequence = firstSequence;
        this.flags = flags;
        this.completionId = completionId;
    }

    public long completionId() { return completionId; }

    public ResourceStreamWriter record(int type, int recordFlags,
                                       long handle, byte[] payload) {
        if (payload == null) throw new NullPointerException("payload");
        return record(type, recordFlags, handle, ByteBuffer.wrap(payload));
    }

    public ResourceStreamWriter record(int type, int recordFlags,
                                       long handle, ByteBuffer payload) {
        validateRecordIdentity(type, recordFlags, handle);
        if (payload == null) throw new NullPointerException("payload");
        if (records.size() >= ResourceStreamFormat.MAX_RECORDS) {
            throw new IllegalStateException("too many resource records");
        }
        try {
            Math.addExact(firstSequence, records.size());
        } catch (ArithmeticException overflow) {
            throw new IllegalStateException("resource sequence overflows", overflow);
        }
        ByteBuffer source = payload.slice();
        byte[] copy = new byte[source.remaining()];
        source.get(copy);
        records.add(new PendingRecord(type, recordFlags, handle, copy));
        return this;
    }

    public int encodedSize() {
        if (records.isEmpty()) throw new IllegalStateException("ResourceStream has no records");
        validateCompletionRecords();
        int total = ResourceStreamFormat.HEADER_BYTES;
        for (PendingRecord record : records) {
            int unaligned = Math.addExact(ResourceStreamFormat.RECORD_HEADER_BYTES,
                    record.payload.length);
            total = Math.addExact(total, ResourceStreamFormat.align(unaligned));
            if (total > ResourceStreamFormat.MAX_STREAM_BYTES) {
                throw new ResourceStreamFormatException("ResourceStream exceeds maximum size");
            }
        }
        return total;
    }

    private void validateCompletionRecords() {
        boolean expectsResult = false;
        for (PendingRecord record : records) {
            expectsResult |= (record.flags & ResourceStreamFormat.RECORD_EXPECTS_RESULT) != 0;
        }
        boolean completion = (flags & ResourceStreamFormat.FLAG_REQUIRES_COMPLETION) != 0;
        if (expectsResult != completion) {
            throw new IllegalStateException(
                    "result-bearing records and stream completion flag must agree");
        }
    }

    public ByteBuffer toDirectBuffer() {
        ByteBuffer target = ByteBuffer.allocateDirect(encodedSize())
                .order(ByteOrder.LITTLE_ENDIAN);
        return writeTo(target);
    }

    public ByteBuffer writeTo(ByteBuffer target) {
        if (target == null) throw new NullPointerException("target");
        int total = encodedSize();
        if (target.remaining() < total) {
            throw new IllegalArgumentException("target has " + target.remaining()
                    + " bytes remaining, needs " + total);
        }
        int targetStart = target.position();
        ByteBuffer destination = target.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        destination.position(targetStart).limit(targetStart + total);
        ByteBuffer stream = destination.slice().order(ByteOrder.LITTLE_ENDIAN);
        stream.put(ResourceStreamFormat.OFFSET_MAGIC, ResourceStreamFormat.MAGIC_R);
        stream.put(ResourceStreamFormat.OFFSET_MAGIC + 1, ResourceStreamFormat.MAGIC_V);
        stream.put(ResourceStreamFormat.OFFSET_MAGIC + 2, ResourceStreamFormat.MAGIC_K);
        stream.put(ResourceStreamFormat.OFFSET_MAGIC + 3,
                ResourceStreamFormat.MAGIC_RESOURCE);
        stream.putShort(ResourceStreamFormat.OFFSET_MAJOR,
                (short) ResourceStreamFormat.MAJOR_VERSION);
        stream.putShort(ResourceStreamFormat.OFFSET_MINOR,
                (short) ResourceStreamFormat.MINOR_VERSION);
        stream.putInt(ResourceStreamFormat.OFFSET_HEADER_BYTES,
                ResourceStreamFormat.HEADER_BYTES);
        stream.putInt(ResourceStreamFormat.OFFSET_TOTAL_BYTES, total);
        stream.putLong(ResourceStreamFormat.OFFSET_FIRST_SEQUENCE, firstSequence);
        stream.putInt(ResourceStreamFormat.OFFSET_RECORD_COUNT, records.size());
        stream.putInt(ResourceStreamFormat.OFFSET_FLAGS, flags);
        stream.putInt(ResourceStreamFormat.OFFSET_CRC32, 0);
        stream.putInt(ResourceStreamFormat.OFFSET_RESERVED, 0);
        stream.putLong(ResourceStreamFormat.OFFSET_COMPLETION_ID, completionId);

        int offset = ResourceStreamFormat.HEADER_BYTES;
        for (int index = 0; index < records.size(); index++) {
            PendingRecord record = records.get(index);
            int recordBytes = ResourceStreamFormat.align(Math.addExact(
                    ResourceStreamFormat.RECORD_HEADER_BYTES, record.payload.length));
            stream.putShort(offset, (short) record.type);
            stream.putShort(offset + 2, (short) record.flags);
            stream.putInt(offset + 4, ResourceStreamFormat.RECORD_HEADER_BYTES);
            stream.putInt(offset + 8, recordBytes);
            stream.putInt(offset + 12, 0);
            stream.putLong(offset + 16, firstSequence + index);
            stream.putLong(offset + 24, record.handle);
            ByteBuffer payloadTarget = stream.duplicate();
            payloadTarget.position(offset + ResourceStreamFormat.RECORD_HEADER_BYTES);
            payloadTarget.put(record.payload);
            for (int padding = ResourceStreamFormat.RECORD_HEADER_BYTES
                    + record.payload.length; padding < recordBytes; padding++) {
                stream.put(offset + padding, (byte) 0);
            }
            offset += recordBytes;
        }
        if ((flags & ResourceStreamFormat.FLAG_HAS_PAYLOAD_CRC32) != 0) {
            stream.putInt(ResourceStreamFormat.OFFSET_CRC32,
                    crc32(stream, ResourceStreamFormat.HEADER_BYTES, total));
        }
        target.position(targetStart + total);
        ByteBuffer result = stream.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        result.position(0).limit(total);
        return result;
    }

    private static void validateRecordIdentity(int type, int flags, long handle) {
        if (type <= 0 || type > 0xffff) {
            throw new IllegalArgumentException("resource record type is invalid");
        }
        if ((flags & ~ResourceStreamFormat.KNOWN_RECORD_FLAGS) != 0) {
            throw new IllegalArgumentException("unknown resource record flags");
        }
        if (!ResourceStreamFormat.knownRecordType(type)
                && (type & ResourceStreamFormat.REQUIRED_EXTENSION_BIT) != 0) {
            throw new IllegalArgumentException("writer cannot emit an unknown required record");
        }
        int expectedType = expectedHandleType(type);
        if (expectedType == 0) {
            if (handle != 0L) throw new IllegalArgumentException(
                    "global resource record has a handle");
        } else {
            FrameResourceHandle.requireType(handle, expectedType);
        }
    }

    static int expectedHandleType(int recordType) {
        if (recordType >= ResourceStreamFormat.TEXTURE_CREATE
                && recordType <= ResourceStreamFormat.RENDER_TARGET_CREATE
                || recordType == ResourceStreamFormat.TEXTURE_READBACK) {
            return FrameResourceHandle.TYPE_TEXTURE;
        }
        if (recordType == ResourceStreamFormat.SHADER_PROGRAM_CREATE
                || recordType == ResourceStreamFormat.SHADER_PROGRAM_DESTROY) {
            return FrameResourceHandle.TYPE_SHADER_PROGRAM;
        }
        return 0;
    }

    private static int crc32(ByteBuffer bytes, int start, int end) {
        CRC32 crc = new CRC32();
        for (int offset = start; offset < end; offset++) crc.update(bytes.get(offset) & 0xff);
        return (int) crc.getValue();
    }

    private static final class PendingRecord {
        private final int type;
        private final int flags;
        private final long handle;
        private final byte[] payload;

        private PendingRecord(int type, int flags, long handle, byte[] payload) {
            this.type = type;
            this.flags = flags;
            this.handle = handle;
            this.payload = payload;
        }
    }
}
