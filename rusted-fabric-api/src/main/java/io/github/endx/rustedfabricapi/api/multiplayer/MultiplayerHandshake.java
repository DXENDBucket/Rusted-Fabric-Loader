package io.github.endx.rustedfabricapi.api.multiplayer;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Binary-safe envelope carried by both platform backends over the same game packet. */
public final class MultiplayerHandshake {
    public static final int GAME_PACKET_TYPE = 179;
    public static final int PROTOCOL_VERSION = 1;
    public static final int MAX_PAYLOAD_BYTES = 256 * 1024;
    private static final byte[] PREFIX = "RFH1\t1\n".getBytes(StandardCharsets.US_ASCII);

    private MultiplayerHandshake() {
    }

    public static byte[] encodeHello(MultiplayerManifest manifest) {
        if (manifest == null) throw new NullPointerException("manifest");
        byte[] body = manifest.encode().getBytes(StandardCharsets.UTF_8);
        if (body.length + PREFIX.length > MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Multiplayer handshake is too large");
        }
        byte[] result = Arrays.copyOf(PREFIX, PREFIX.length + body.length);
        System.arraycopy(body, 0, result, PREFIX.length, body.length);
        return result;
    }

    public static MultiplayerManifest decodeHello(byte[] payload) {
        if (payload == null || payload.length < PREFIX.length
                || payload.length > MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Multiplayer handshake is missing or too large");
        }
        for (int index = 0; index < PREFIX.length; index++) {
            if (payload[index] != PREFIX[index]) {
                throw new IllegalArgumentException("Unsupported multiplayer handshake");
            }
        }
        return MultiplayerManifest.decode(new String(payload, PREFIX.length,
                payload.length - PREFIX.length, StandardCharsets.UTF_8));
    }
}
