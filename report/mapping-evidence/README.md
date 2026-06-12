# Mapping Evidence CSVs

This directory holds large handoff/evidence CSV files used while growing the
Rusted Warfare mappings and API diagnostics.

These files are intentionally kept out of `rusted-fabric-api` runtime resources
so the packaged API jar stays small. The API keeps only
`rustedfabricapi/mapping/mapping-evidence-manifest.csv` as a lightweight index.

During local development, `MappingEvidenceDiagnostics` falls back to this
directory when a CSV is not present on the classpath. Packaged runtime use will
therefore return empty evidence rows unless the same report directory is also
present beside the working directory.
