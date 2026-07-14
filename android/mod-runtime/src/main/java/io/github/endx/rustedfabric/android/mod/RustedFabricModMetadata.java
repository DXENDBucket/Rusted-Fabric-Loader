package io.github.endx.rustedfabric.android.mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RustedFabricModMetadata {
    private final int schemaVersion;
    private final String id;
    private final String version;
    private final String name;
    private final String entrypoint;
    private final String apiVersion;
    private final List<String> mappingProfiles;
    private final List<String> capabilities;

    RustedFabricModMetadata(int schemaVersion, String id, String version, String name,
                            String entrypoint, String apiVersion, List<String> mappingProfiles,
                            List<String> capabilities) {
        this.schemaVersion = schemaVersion;
        this.id = id;
        this.version = version;
        this.name = name;
        this.entrypoint = entrypoint;
        this.apiVersion = apiVersion;
        this.mappingProfiles = immutableCopy(mappingProfiles);
        this.capabilities = immutableCopy(capabilities);
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public String getEntrypoint() {
        return entrypoint;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public List<String> getMappingProfiles() {
        return mappingProfiles;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }

    public boolean supportsMappingProfile(String profileId) {
        return profileId != null && mappingProfiles.contains(profileId);
    }

    private static List<String> immutableCopy(List<String> values) {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
