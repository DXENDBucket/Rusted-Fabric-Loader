package io.github.endx.rustedfabricapi.api.diagnostic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MappingEvidenceDiagnostics {
    private static final String LOGIC_BOOLEAN_RESOURCE =
            "/rustedfabricapi/mapping/rw_logicboolean_member_expansion_v0_27.csv";
    private static final String PARSER_HELPER_RESOURCE =
            "/rustedfabricapi/mapping/rw_parser_helper_mapping_delta_v0_27.csv";

    private MappingEvidenceDiagnostics() {
    }

    public static List<MappingEvidenceRow> allLogicBooleanMembers() {
        return Holder.LOGIC_BOOLEAN_MEMBERS;
    }

    public static List<MappingEvidenceRow> allParserHelpers() {
        return Holder.PARSER_HELPERS;
    }

    public static List<MappingEvidenceRow> findLogicBooleanMembers(String text) {
        return findByText(Holder.LOGIC_BOOLEAN_MEMBERS, text);
    }

    public static List<MappingEvidenceRow> findParserHelpers(String text) {
        return findByText(Holder.PARSER_HELPERS, text);
    }

    public static List<MappingEvidenceRow> findParserHelpersByCategory(String category) {
        String expected = normalize(category);
        List<MappingEvidenceRow> result = new ArrayList<MappingEvidenceRow>();
        for (MappingEvidenceRow row : Holder.PARSER_HELPERS) {
            if (normalize(row.category()).equals(expected)) {
                result.add(row);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static List<String> parserHelperCategories() {
        List<String> result = new ArrayList<String>();
        for (MappingEvidenceRow row : Holder.PARSER_HELPERS) {
            String category = row.category();
            if (category != null && !category.isEmpty() && !result.contains(category)) {
                result.add(category);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static List<MappingEvidenceRow> findByText(List<MappingEvidenceRow> rows, String text) {
        String needle = normalize(text);
        if (needle.isEmpty()) {
            return rows;
        }

        List<MappingEvidenceRow> result = new ArrayList<MappingEvidenceRow>();
        for (MappingEvidenceRow row : rows) {
            if (normalize(row.ownerOfficial()).contains(needle)
                    || normalize(row.namedName()).contains(needle)
                    || normalize(row.officialName()).contains(needle)
                    || normalize(row.category()).contains(needle)
                    || normalize(row.notes()).contains(needle)) {
                result.add(row);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static List<MappingEvidenceRow> loadRows(String resource) {
        List<Map<String, String>> rows = loadCsv(resource);
        List<MappingEvidenceRow> result = new ArrayList<MappingEvidenceRow>();
        for (Map<String, String> row : rows) {
            result.add(new MappingEvidenceRow(
                    row.get("kind"),
                    row.get("owner_official"),
                    row.get("descriptor"),
                    row.get("official_name"),
                    row.get("intermediary_name"),
                    row.get("named_name"),
                    row.get("source"),
                    row.get("category"),
                    row.get("confidence"),
                    row.get("evidence"),
                    row.get("notes")));
        }
        return Collections.unmodifiableList(result);
    }

    private static List<Map<String, String>> loadCsv(String resource) {
        InputStream inputStream = MappingEvidenceDiagnostics.class.getResourceAsStream(resource);
        if (inputStream == null) {
            return Collections.emptyList();
        }

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            try {
                String headerLine = reader.readLine();
                if (headerLine == null) {
                    return Collections.emptyList();
                }
                List<String> headers = parseCsvLine(headerLine);
                List<Map<String, String>> rows = new ArrayList<Map<String, String>>();
                String line;
                while ((line = reader.readLine()) != null) {
                    List<String> values = parseCsvLine(line);
                    Map<String, String> row = new LinkedHashMap<String, String>();
                    for (int i = 0; i < headers.size(); i++) {
                        row.put(headers.get(i), i < values.size() ? values.get(i) : "");
                    }
                    rows.add(row);
                }
                return rows;
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not load mapping evidence resource " + resource, e);
        }
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<String>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        value.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    value.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                values.add(value.toString());
                value.setLength(0);
            } else {
                value.append(c);
            }
        }
        values.add(value.toString());
        return values;
    }

    private static String normalize(String value) {
        return value != null ? value.trim().toLowerCase(java.util.Locale.ROOT) : "";
    }

    private static final class Holder {
        private static final List<MappingEvidenceRow> LOGIC_BOOLEAN_MEMBERS = loadRows(LOGIC_BOOLEAN_RESOURCE);
        private static final List<MappingEvidenceRow> PARSER_HELPERS = loadRows(PARSER_HELPER_RESOURCE);
    }

    public static final class MappingEvidenceRow {
        private final String kind;
        private final String ownerOfficial;
        private final String descriptor;
        private final String officialName;
        private final String intermediaryName;
        private final String namedName;
        private final String source;
        private final String category;
        private final String confidence;
        private final String evidence;
        private final String notes;

        private MappingEvidenceRow(String kind, String ownerOfficial, String descriptor, String officialName,
                                   String intermediaryName, String namedName, String source, String category,
                                   String confidence, String evidence, String notes) {
            this.kind = nullToEmpty(kind);
            this.ownerOfficial = nullToEmpty(ownerOfficial);
            this.descriptor = nullToEmpty(descriptor);
            this.officialName = nullToEmpty(officialName);
            this.intermediaryName = nullToEmpty(intermediaryName);
            this.namedName = nullToEmpty(namedName);
            this.source = nullToEmpty(source);
            this.category = nullToEmpty(category);
            this.confidence = nullToEmpty(confidence);
            this.evidence = nullToEmpty(evidence);
            this.notes = nullToEmpty(notes);
        }

        public String kind() {
            return kind;
        }

        public String ownerOfficial() {
            return ownerOfficial;
        }

        public String descriptor() {
            return descriptor;
        }

        public String officialName() {
            return officialName;
        }

        public String intermediaryName() {
            return intermediaryName;
        }

        public String namedName() {
            return namedName;
        }

        public String source() {
            return source;
        }

        public String category() {
            return category;
        }

        public String confidence() {
            return confidence;
        }

        public String evidence() {
            return evidence;
        }

        public String notes() {
            return notes;
        }

        private static String nullToEmpty(String value) {
            return value != null ? value : "";
        }
    }
}
