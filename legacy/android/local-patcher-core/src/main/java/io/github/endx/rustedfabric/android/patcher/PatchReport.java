package io.github.endx.rustedfabric.android.patcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PatchReport {
    private final String profileId;
    private final String sourceSha256;
    private final String outputSha256;
    private final String outputPackage;
    private final String bootstrapDexEntry;
    private final boolean signed;
    private final List<String> operations;

    PatchReport(String profileId, String sourceSha256, String outputSha256,
                String outputPackage, String bootstrapDexEntry, boolean signed,
                List<String> operations) {
        this.profileId = profileId;
        this.sourceSha256 = sourceSha256;
        this.outputSha256 = outputSha256;
        this.outputPackage = outputPackage;
        this.bootstrapDexEntry = bootstrapDexEntry;
        this.signed = signed;
        this.operations = Collections.unmodifiableList(new ArrayList<>(operations));
    }

    public String getProfileId() { return profileId; }
    public String getSourceSha256() { return sourceSha256; }
    public String getOutputSha256() { return outputSha256; }
    public String getOutputPackage() { return outputPackage; }
    public String getBootstrapDexEntry() { return bootstrapDexEntry; }
    public boolean isSigned() { return signed; }
    public List<String> getOperations() { return operations; }

    public PatchReport withSignedOutput(String signedOutputSha256) {
        List<String> signedOperations = new ArrayList<>(operations);
        signedOperations.add("apk-signature-v1-v2-v3");
        return new PatchReport(profileId, sourceSha256, signedOutputSha256, outputPackage,
                bootstrapDexEntry, true, signedOperations);
    }

    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n")
                .append("  \"schemaVersion\": 1,\n")
                .append("  \"profile\": \"").append(escape(profileId)).append("\",\n")
                .append("  \"sourceSha256\": \"").append(sourceSha256).append("\",\n")
                .append("  \"outputSha256\": \"").append(outputSha256).append("\",\n")
                .append("  \"outputPackage\": \"").append(escape(outputPackage)).append("\",\n")
                .append("  \"bootstrapDexEntry\": \"").append(bootstrapDexEntry).append("\",\n")
                .append("  \"signed\": ").append(signed).append(",\n")
                .append("  \"operations\": [");
        for (int index = 0; index < operations.size(); index++) {
            if (index > 0) json.append(", ");
            json.append('"').append(escape(operations.get(index))).append('"');
        }
        return json.append("]\n}\n").toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
