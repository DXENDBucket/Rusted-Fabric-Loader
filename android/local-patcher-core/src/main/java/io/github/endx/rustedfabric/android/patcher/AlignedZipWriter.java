package io.github.endx.rustedfabric.android.patcher;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** ZIP writer that aligns stored Android resources without external zipalign. */
final class AlignedZipWriter implements Closeable {
    private static final int ALIGN_EXTRA_ID = 0xd935;
    private static final long DOS_EPOCH_MILLIS = 315532800000L;

    private final CountingOutputStream counting;
    private final ZipOutputStream zip;

    AlignedZipWriter(OutputStream output) {
        this.counting = new CountingOutputStream(output);
        this.zip = new ZipOutputStream(counting, StandardCharsets.UTF_8);
        this.zip.setLevel(9);
    }

    void writeExisting(ZipEntry source, InputStream input) throws IOException {
        ZipEntry target = baseEntry(source.getName(), source.getMethod());
        if (source.getMethod() == ZipEntry.STORED) {
            target.setSize(source.getSize());
            target.setCompressedSize(source.getSize());
            target.setCrc(source.getCrc());
            target.setExtra(alignmentExtra(source.getName(), alignment(source.getName())));
        }
        zip.putNextEntry(target);
        transfer(input, zip);
        zip.closeEntry();
    }

    void writeBytes(String name, byte[] bytes, int method) throws IOException {
        ZipEntry target = baseEntry(name, method);
        if (method == ZipEntry.STORED) {
            CRC32 crc = new CRC32();
            crc.update(bytes);
            target.setSize(bytes.length);
            target.setCompressedSize(bytes.length);
            target.setCrc(crc.getValue());
            target.setExtra(alignmentExtra(name, alignment(name)));
        }
        zip.putNextEntry(target);
        zip.write(bytes);
        zip.closeEntry();
    }

    private ZipEntry baseEntry(String name, int method) {
        ZipEntry entry = new ZipEntry(name);
        entry.setMethod(method == ZipEntry.STORED ? ZipEntry.STORED : ZipEntry.DEFLATED);
        entry.setTime(DOS_EPOCH_MILLIS);
        return entry;
    }

    private byte[] alignmentExtra(String name, int alignment) {
        if (alignment <= 1) return null;
        int nameLength = name.getBytes(StandardCharsets.UTF_8).length;
        long baseAfterHeader = counting.count() + 30L + nameLength;
        int payload = (int) ((alignment - ((baseAfterHeader + 4L) % alignment)) % alignment);
        byte[] extra = new byte[4 + payload];
        extra[0] = (byte) ALIGN_EXTRA_ID;
        extra[1] = (byte) (ALIGN_EXTRA_ID >>> 8);
        extra[2] = (byte) payload;
        extra[3] = (byte) (payload >>> 8);
        return extra;
    }

    private static int alignment(String name) {
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        if (lower.startsWith("lib/") && lower.endsWith(".so")) return 4096;
        if (lower.equals("resources.arsc") || lower.matches("classes[0-9]*\\.dex")) return 4;
        return 4;
    }

    private static void transfer(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        for (int count = input.read(buffer); count >= 0; count = input.read(buffer)) {
            if (count > 0) output.write(buffer, 0, count);
        }
    }

    @Override
    public void close() throws IOException {
        zip.close();
    }
}
