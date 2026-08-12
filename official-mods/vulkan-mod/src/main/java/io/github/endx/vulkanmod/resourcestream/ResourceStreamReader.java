package io.github.endx.vulkanmod.resourcestream;

import io.github.endx.vulkanmod.framestream.FrameResourceHandle;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.CRC32;

/** Validates a reliable ResourceStream before resource-table or native allocation access. */
public final class ResourceStreamReader {
    private final ByteBuffer bytes;
    private final long firstSequence;
    private final int flags;
    private final long completionId;
    private final List<Record> records;

    private ResourceStreamReader(ByteBuffer bytes, long firstSequence, int flags,
                                 long completionId, List<Record> records) {
        this.bytes = bytes;
        this.firstSequence = firstSequence;
        this.flags = flags;
        this.completionId = completionId;
        this.records = Collections.unmodifiableList(records);
    }

    public static ResourceStreamReader read(ByteBuffer input) {
        if (input == null) throw new NullPointerException("input");
        ByteBuffer bytes = input.slice().order(ByteOrder.LITTLE_ENDIAN);
        require(bytes.remaining() >= ResourceStreamFormat.HEADER_BYTES, "truncated header");
        require(bytes.get(0) == ResourceStreamFormat.MAGIC_R
                        && bytes.get(1) == ResourceStreamFormat.MAGIC_V
                        && bytes.get(2) == ResourceStreamFormat.MAGIC_K
                        && bytes.get(3) == ResourceStreamFormat.MAGIC_RESOURCE,
                "invalid ResourceStream magic");
        int major = Short.toUnsignedInt(bytes.getShort(ResourceStreamFormat.OFFSET_MAJOR));
        int minor = Short.toUnsignedInt(bytes.getShort(ResourceStreamFormat.OFFSET_MINOR));
        require(major == ResourceStreamFormat.MAJOR_VERSION,
                "unsupported ResourceStream major version " + major);
        require(minor <= ResourceStreamFormat.MINOR_VERSION,
                "unsupported ResourceStream minor version " + minor);
        require(bytes.getInt(ResourceStreamFormat.OFFSET_HEADER_BYTES)
                        == ResourceStreamFormat.HEADER_BYTES,
                "invalid ResourceStream header length");
        int total = bytes.getInt(ResourceStreamFormat.OFFSET_TOTAL_BYTES);
        require(total >= ResourceStreamFormat.HEADER_BYTES
                        && total <= ResourceStreamFormat.MAX_STREAM_BYTES,
                "ResourceStream total length is out of range");
        require(total == bytes.remaining(), "ResourceStream total length mismatch");
        long firstSequence = bytes.getLong(ResourceStreamFormat.OFFSET_FIRST_SEQUENCE);
        require(firstSequence >= 0L, "negative first resource sequence");
        int count = bytes.getInt(ResourceStreamFormat.OFFSET_RECORD_COUNT);
        require(count > 0 && count <= ResourceStreamFormat.MAX_RECORDS,
                "resource record count is out of range");
        int flags = bytes.getInt(ResourceStreamFormat.OFFSET_FLAGS);
        require((flags & ~ResourceStreamFormat.KNOWN_FLAGS) == 0,
                "unknown ResourceStream flags");
        require(bytes.getInt(ResourceStreamFormat.OFFSET_RESERVED) == 0,
                "ResourceStream reserved field is not zero");
        long completionId = bytes.getLong(ResourceStreamFormat.OFFSET_COMPLETION_ID);
        boolean completion = (flags & ResourceStreamFormat.FLAG_REQUIRES_COMPLETION) != 0;
        require(completion == (completionId > 0L),
                "completion flag and ID disagree");
        if ((flags & ResourceStreamFormat.FLAG_HAS_PAYLOAD_CRC32) != 0) {
            require(bytes.getInt(ResourceStreamFormat.OFFSET_CRC32)
                            == crc32(bytes, ResourceStreamFormat.HEADER_BYTES, total),
                    "ResourceStream CRC32 mismatch");
        } else {
            require(bytes.getInt(ResourceStreamFormat.OFFSET_CRC32) == 0,
                    "ResourceStream CRC32 is set without its flag");
        }

        ArrayList<Record> records = new ArrayList<Record>(count);
        boolean expectsResult = false;
        boolean externalPayload = false;
        int offset = ResourceStreamFormat.HEADER_BYTES;
        for (int index = 0; index < count; index++) {
            require(offset <= total - ResourceStreamFormat.RECORD_HEADER_BYTES,
                    "truncated resource record header");
            int type = Short.toUnsignedInt(bytes.getShort(offset));
            int recordFlags = Short.toUnsignedInt(bytes.getShort(offset + 2));
            int headerBytes = bytes.getInt(offset + 4);
            int recordBytes = bytes.getInt(offset + 8);
            require(type != 0, "resource record type zero is invalid");
            require(ResourceStreamFormat.knownRecordType(type)
                            || (type & ResourceStreamFormat.REQUIRED_EXTENSION_BIT) == 0,
                    "unknown required resource record " + type);
            require((recordFlags & ~ResourceStreamFormat.KNOWN_RECORD_FLAGS) == 0,
                    "unknown resource record flags");
            require(headerBytes == ResourceStreamFormat.RECORD_HEADER_BYTES,
                    "invalid resource record header length");
            require(recordBytes >= headerBytes
                            && (recordBytes & (ResourceStreamFormat.ALIGNMENT - 1)) == 0,
                    "invalid aligned resource record length");
            require((long) offset + recordBytes <= total,
                    "resource record extends beyond stream");
            require(bytes.getInt(offset + 12) == 0,
                    "resource record reserved field is not zero");
            long sequence = bytes.getLong(offset + 16);
            long expected;
            try {
                expected = Math.addExact(firstSequence, index);
            } catch (ArithmeticException overflow) {
                throw invalid("resource sequence overflows");
            }
            require(sequence == expected, "resource sequence is not contiguous");
            long handle = bytes.getLong(offset + 24);
            validateHandle(type, handle);
            expectsResult |= (recordFlags & ResourceStreamFormat.RECORD_EXPECTS_RESULT) != 0;
            externalPayload |= (recordFlags
                    & ResourceStreamFormat.RECORD_HAS_EXTERNAL_PAYLOAD) != 0;
            records.add(new Record(type, recordFlags, headerBytes, recordBytes,
                    sequence, handle, offset));
            offset += recordBytes;
        }
        require(offset == total, "ResourceStream contains trailing bytes");
        require(!expectsResult || completion,
                "result-bearing records require a completion flag");
        require(!externalPayload || completion,
                "external resource payload requires a completion flag");
        ByteBuffer retained = bytes.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        retained.position(0).limit(total);
        ResourceStreamReader result = new ResourceStreamReader(retained, firstSequence, flags,
                completionId, records);
        ResourceStreamRecords.validateKnownRecords(result);
        return result;
    }

    public long firstSequence() { return firstSequence; }
    public long lastSequence() { return records.get(records.size() - 1).sequence; }
    public int flags() { return flags; }
    public long completionId() { return completionId; }
    public int recordCount() { return records.size(); }
    public List<Record> records() { return records; }
    public Record record(int index) { return records.get(index); }

    public ByteBuffer payload(int index) {
        Record record = records.get(index);
        ByteBuffer view = bytes.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        view.position(record.offset + record.headerBytes);
        view.limit(record.offset + record.recordBytes);
        return view.slice().order(ByteOrder.LITTLE_ENDIAN).asReadOnlyBuffer()
                .order(ByteOrder.LITTLE_ENDIAN);
    }

    private static void validateHandle(int recordType, long handle) {
        int expected = ResourceStreamWriter.expectedHandleType(recordType);
        if (expected == 0) {
            require(handle == 0L, "global resource record has a handle");
        } else {
            require(handle != 0L && FrameResourceHandle.type(handle) == expected
                            && FrameResourceHandle.generation(handle) != 0,
                    "resource record has an invalid typed handle");
        }
    }

    private static int crc32(ByteBuffer bytes, int start, int end) {
        CRC32 crc = new CRC32();
        for (int offset = start; offset < end; offset++) crc.update(bytes.get(offset) & 0xff);
        return (int) crc.getValue();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw invalid(message);
    }

    private static ResourceStreamFormatException invalid(String message) {
        return new ResourceStreamFormatException(message);
    }

    public static final class Record {
        private final int type;
        private final int flags;
        private final int headerBytes;
        private final int recordBytes;
        private final long sequence;
        private final long handle;
        private final int offset;

        private Record(int type, int flags, int headerBytes, int recordBytes,
                       long sequence, long handle, int offset) {
            this.type = type;
            this.flags = flags;
            this.headerBytes = headerBytes;
            this.recordBytes = recordBytes;
            this.sequence = sequence;
            this.handle = handle;
            this.offset = offset;
        }

        public int type() { return type; }
        public int flags() { return flags; }
        public int headerBytes() { return headerBytes; }
        public int recordBytes() { return recordBytes; }
        public int payloadBytes() { return recordBytes - headerBytes; }
        public long sequence() { return sequence; }
        public long handle() { return handle; }
    }
}
