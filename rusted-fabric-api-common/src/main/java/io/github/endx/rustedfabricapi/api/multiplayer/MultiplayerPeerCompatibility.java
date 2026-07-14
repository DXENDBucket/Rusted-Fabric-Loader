package io.github.endx.rustedfabricapi.api.multiplayer;

import java.util.Objects;
import java.util.Optional;

import io.github.endx.rustedfabricapi.api.event.MultiplayerCompatibilityEvents;

/** Compatibility result tied to one live game connection. */
public final class MultiplayerPeerCompatibility {
    public enum PeerType { RUSTED_FABRIC, VANILLA_OR_LEGACY }

    private final String peerId;
    private final PeerType peerType;
    private final MultiplayerManifest remoteManifest;
    private final MultiplayerCompatibility.Report report;

    private MultiplayerPeerCompatibility(String peerId, PeerType peerType,
            MultiplayerManifest remoteManifest, MultiplayerCompatibility.Report report) {
        this.peerId = Objects.requireNonNull(peerId, "peerId");
        this.peerType = Objects.requireNonNull(peerType, "peerType");
        this.remoteManifest = remoteManifest;
        this.report = Objects.requireNonNull(report, "report");
    }

    public static MultiplayerPeerCompatibility evaluate(String peerId,
            MultiplayerManifest local, MultiplayerManifest remote) {
        MultiplayerPeerCompatibility result = new MultiplayerPeerCompatibility(peerId,
                PeerType.RUSTED_FABRIC, remote,
                MultiplayerCompatibility.evaluate(local, remote));
        MultiplayerCompatibilityEvents.PEER_EVALUATED.dispatch(result);
        return result;
    }

    public static MultiplayerPeerCompatibility evaluateVanilla(String peerId,
            MultiplayerManifest local) {
        MultiplayerPeerCompatibility result = new MultiplayerPeerCompatibility(peerId,
                PeerType.VANILLA_OR_LEGACY, null,
                MultiplayerCompatibility.evaluateVanillaPeer(local));
        MultiplayerCompatibilityEvents.PEER_EVALUATED.dispatch(result);
        return result;
    }

    public String peerId() { return peerId; }
    public PeerType peerType() { return peerType; }
    public Optional<MultiplayerManifest> remoteManifest() {
        return Optional.ofNullable(remoteManifest);
    }
    public MultiplayerCompatibility.Report report() { return report; }
    public boolean compatible() { return report.compatible(); }
}
