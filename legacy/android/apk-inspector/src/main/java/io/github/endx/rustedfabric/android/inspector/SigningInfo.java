package io.github.endx.rustedfabric.android.inspector;

import java.util.ArrayList;
import java.util.List;

final class SigningInfo {
    final List<String> v1CertificateSha256 = new ArrayList<>();
    boolean apkSigningBlockPresent;
}
