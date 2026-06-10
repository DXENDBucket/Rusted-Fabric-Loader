package io.github.endx.rustedfabricapi.api.ini;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RustedIniDiagnostics {
    private static final String KEY_SEMANTICS_RESOURCE =
            "/rustedfabricapi/ini/rw_ini_key_semantics_actionable_subset_v0_26.csv";
    private static final String FIELD_BINDINGS_RESOURCE =
            "/rustedfabricapi/ini/rw_ini_key_field_binding_delta_v0_26.csv";
    private static final String SECTION_COVERAGE_RESOURCE =
            "/rustedfabricapi/ini/rw_ini_key_coverage_by_section_v0_26.csv";

    private static volatile boolean keyReadTracingEnabled;

    private RustedIniDiagnostics() {
    }

    public static boolean isKeyReadTracingEnabled() {
        return keyReadTracingEnabled;
    }

    public static void setKeyReadTracingEnabled(boolean enabled) {
        keyReadTracingEnabled = enabled;
    }

    public static List<IniKeyInfo> allKeys() {
        return Holder.KEYS;
    }

    public static List<FieldBindingInfo> allFieldBindings() {
        return Holder.FIELD_BINDINGS;
    }

    public static List<SectionCoverage> allSectionCoverage() {
        return Holder.SECTION_COVERAGE;
    }

    public static List<IniKeyInfo> findKeys(String section, String key) {
        String normalizedSection = normalizeSectionContext(section);
        String normalizedKey = normalizeKey(key);
        List<IniKeyInfo> result = new ArrayList<IniKeyInfo>();
        for (IniKeyInfo info : Holder.KEYS) {
            if (!matchesSectionContext(info.sectionContext(), normalizedSection)) {
                continue;
            }
            if (normalizedKey.equals(normalizeKey(info.keyNormalized()))
                    || normalizedKey.equals(normalizeKey(info.keyCandidate()))
                    || normalizedKey.equals(normalizeKey(stripTrailingColon(info.keyRaw())))) {
                result.add(info);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static IniKeyInfo findBestKey(String section, String key) {
        List<IniKeyInfo> matches = findKeys(section, key);
        if (matches.isEmpty()) {
            return null;
        }
        return matches.get(0);
    }

    public static List<FieldBindingInfo> findFieldBindings(String section, String key) {
        String normalizedSection = normalizeSectionContext(section);
        String normalizedKey = normalizeKey(key);
        List<FieldBindingInfo> result = new ArrayList<FieldBindingInfo>();
        for (FieldBindingInfo info : Holder.FIELD_BINDINGS) {
            if (!matchesSectionContext(info.sectionContext(), normalizedSection)) {
                continue;
            }
            if (normalizedKey.equals(normalizeKey(info.key()))) {
                result.add(info);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static SectionCoverage findSectionCoverage(String section) {
        String normalizedSection = normalizeSectionContext(section);
        for (SectionCoverage coverage : Holder.SECTION_COVERAGE) {
            if (matchesSectionContext(coverage.sectionContext(), normalizedSection)) {
                return coverage;
            }
        }
        return null;
    }

    public static List<String> suggestKeys(String section, String prefix, int limit) {
        String normalizedSection = normalizeSectionContext(section);
        String normalizedPrefix = normalizeKey(prefix);
        int max = limit > 0 ? limit : 64;
        List<String> result = new ArrayList<String>();
        for (IniKeyInfo info : Holder.KEYS) {
            if (!matchesSectionContext(info.sectionContext(), normalizedSection)) {
                continue;
            }
            String key = emptyToFallback(info.keyNormalized(), stripTrailingColon(info.keyRaw()));
            if (key == null || key.isEmpty()) {
                continue;
            }
            if (!normalizeKey(key).startsWith(normalizedPrefix)) {
                continue;
            }
            if (!result.contains(key)) {
                result.add(key);
            }
            if (result.size() >= max) {
                break;
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static String describeKey(String section, String key) {
        IniKeyInfo info = findBestKey(section, key);
        if (info == null) {
            return "[" + nullToEmpty(section) + "] " + nullToEmpty(key) + ": no v0.26 INI key metadata";
        }

        StringBuilder description = new StringBuilder();
        description.append(info.sectionContext()).append(' ');
        description.append(stripTrailingColon(info.keyRaw()));
        if (!isEmpty(info.valueType())) {
            description.append(" : ").append(info.valueType());
        }
        if (!isEmpty(info.description())) {
            description.append(" - ").append(info.description());
        }
        if (!isEmpty(info.evidenceStatus())) {
            description.append(" (").append(info.evidenceStatus());
            if (!isEmpty(info.semanticConfidence())) {
                description.append(", ").append(info.semanticConfidence());
            }
            description.append(')');
        }
        return description.toString();
    }

    public static String normalizeSectionContext(String section) {
        if (section == null) {
            return "";
        }

        String value = section.trim();
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1);
        }
        if (value.isEmpty()) {
            return "";
        }

        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("action_")) {
            return "[action_NAME]";
        }
        if (lower.startsWith("hiddenaction_")) {
            return "[hiddenAction_NAME]";
        }
        if (lower.startsWith("turret_")) {
            return "[turret_NAME]";
        }
        if (lower.startsWith("projectile_")) {
            return "[projectile_NAME]";
        }
        if (lower.startsWith("effect_")) {
            return "[effect_NAME]";
        }
        if (lower.startsWith("canbuild_")) {
            return "[canBuild_NAME]";
        }
        if (lower.startsWith("attachment_")) {
            return "[attachment_NAME]";
        }
        if (lower.startsWith("animation_")) {
            return "[animation_NAME]";
        }
        if (lower.startsWith("decal_")) {
            return "[decal_name]";
        }
        if (lower.startsWith("resource_")) {
            return "[resource_NAME]";
        }
        if (lower.startsWith("global_resource_")) {
            return "[global_resource_NAME]";
        }
        if (lower.startsWith("placementrule_")) {
            return "[placementRule_NAME]";
        }
        if (lower.startsWith("leg_")) {
            return "[leg_#]";
        }
        if (lower.startsWith("arm_")) {
            return "[arm_#]";
        }
        if (value.indexOf(':') >= 0) {
            return value;
        }
        return "[" + value + "]";
    }

    private static boolean matchesSectionContext(String tableContext, String requestedContext) {
        if (isEmpty(tableContext) || isEmpty(requestedContext)) {
            return false;
        }
        if (tableContext.equals(requestedContext)) {
            return true;
        }

        String[] parts = tableContext.split("/");
        for (String part : parts) {
            if (part.trim().equals(requestedContext)) {
                return true;
            }
        }
        return false;
    }

    private static List<IniKeyInfo> loadKeys() {
        List<Map<String, String>> rows = loadCsv(KEY_SEMANTICS_RESOURCE);
        List<IniKeyInfo> result = new ArrayList<IniKeyInfo>();
        for (Map<String, String> row : rows) {
            result.add(new IniKeyInfo(
                    row.get("official_row"),
                    row.get("section_context"),
                    row.get("context_type"),
                    row.get("category"),
                    row.get("key_raw"),
                    row.get("key_normalized"),
                    row.get("key_candidate"),
                    row.get("value_type"),
                    row.get("description"),
                    row.get("example"),
                    row.get("evidence_status"),
                    row.get("best_parser_owner_named"),
                    row.get("best_parser_domain"),
                    row.get("semantic_confidence"),
                    row.get("implementation_note"),
                    row.get("best_method_named")));
        }
        return Collections.unmodifiableList(result);
    }

    private static List<FieldBindingInfo> loadFieldBindings() {
        List<Map<String, String>> rows = loadCsv(FIELD_BINDINGS_RESOURCE);
        List<FieldBindingInfo> result = new ArrayList<FieldBindingInfo>();
        for (Map<String, String> row : rows) {
            result.add(new FieldBindingInfo(
                    row.get("section_context"),
                    row.get("key"),
                    row.get("owner_official"),
                    emptyToFallback(row.get("owner_named"), row.get("owner_official")),
                    row.get("field_official"),
                    row.get("field_named"),
                    row.get("descriptor"),
                    row.get("value_type"),
                    row.get("confidence"),
                    row.get("note")));
        }
        return Collections.unmodifiableList(result);
    }

    private static List<SectionCoverage> loadSectionCoverage() {
        List<Map<String, String>> rows = loadCsv(SECTION_COVERAGE_RESOURCE);
        List<SectionCoverage> result = new ArrayList<SectionCoverage>();
        for (Map<String, String> row : rows) {
            result.add(new SectionCoverage(
                    row.get("section_context"),
                    row.get("context_type"),
                    parseInt(row.get("official_entries")),
                    parseInt(row.get("matched_entries")),
                    parseInt(row.get("high_method_entries")),
                    parseInt(row.get("official_only_entries")),
                    parseDouble(row.get("coverage_pct")),
                    parseDouble(row.get("high_confidence_pct"))));
        }
        return Collections.unmodifiableList(result);
    }

    private static List<Map<String, String>> loadCsv(String resource) {
        InputStream inputStream = RustedIniDiagnostics.class.getResourceAsStream(resource);
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
            throw new IllegalStateException("Could not load INI diagnostics resource " + resource, e);
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

    private static String normalizeKey(String key) {
        return stripTrailingColon(nullToEmpty(key)).trim().toLowerCase(Locale.ROOT);
    }

    private static String stripTrailingColon(String value) {
        String text = nullToEmpty(value).trim();
        return text.endsWith(":") ? text.substring(0, text.length() - 1) : text;
    }

    private static String emptyToFallback(String value, String fallback) {
        return isEmpty(value) ? nullToEmpty(fallback) : value;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(nullToEmpty(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(nullToEmpty(value));
        } catch (NumberFormatException e) {
            return 0.0D;
        }
    }

    private static final class Holder {
        private static final List<IniKeyInfo> KEYS = loadKeys();
        private static final List<FieldBindingInfo> FIELD_BINDINGS = loadFieldBindings();
        private static final List<SectionCoverage> SECTION_COVERAGE = loadSectionCoverage();
    }

    public static final class IniKeyInfo {
        private final String officialRow;
        private final String sectionContext;
        private final String contextType;
        private final String category;
        private final String keyRaw;
        private final String keyNormalized;
        private final String keyCandidate;
        private final String valueType;
        private final String description;
        private final String example;
        private final String evidenceStatus;
        private final String parserOwnerNamed;
        private final String parserDomain;
        private final String semanticConfidence;
        private final String implementationNote;
        private final String methodNamed;

        private IniKeyInfo(String officialRow, String sectionContext, String contextType, String category,
                           String keyRaw, String keyNormalized, String keyCandidate, String valueType,
                           String description, String example, String evidenceStatus, String parserOwnerNamed,
                           String parserDomain, String semanticConfidence, String implementationNote,
                           String methodNamed) {
            this.officialRow = nullToEmpty(officialRow);
            this.sectionContext = nullToEmpty(sectionContext);
            this.contextType = nullToEmpty(contextType);
            this.category = nullToEmpty(category);
            this.keyRaw = nullToEmpty(keyRaw);
            this.keyNormalized = nullToEmpty(keyNormalized);
            this.keyCandidate = nullToEmpty(keyCandidate);
            this.valueType = nullToEmpty(valueType);
            this.description = nullToEmpty(description);
            this.example = nullToEmpty(example);
            this.evidenceStatus = nullToEmpty(evidenceStatus);
            this.parserOwnerNamed = nullToEmpty(parserOwnerNamed);
            this.parserDomain = nullToEmpty(parserDomain);
            this.semanticConfidence = nullToEmpty(semanticConfidence);
            this.implementationNote = nullToEmpty(implementationNote);
            this.methodNamed = nullToEmpty(methodNamed);
        }

        public String officialRow() {
            return officialRow;
        }

        public String sectionContext() {
            return sectionContext;
        }

        public String contextType() {
            return contextType;
        }

        public String category() {
            return category;
        }

        public String keyRaw() {
            return keyRaw;
        }

        public String keyNormalized() {
            return keyNormalized;
        }

        public String keyCandidate() {
            return keyCandidate;
        }

        public String valueType() {
            return valueType;
        }

        public String description() {
            return description;
        }

        public String example() {
            return example;
        }

        public String evidenceStatus() {
            return evidenceStatus;
        }

        public String parserOwnerNamed() {
            return parserOwnerNamed;
        }

        public String parserDomain() {
            return parserDomain;
        }

        public String semanticConfidence() {
            return semanticConfidence;
        }

        public String implementationNote() {
            return implementationNote;
        }

        public String methodNamed() {
            return methodNamed;
        }
    }

    public static final class FieldBindingInfo {
        private final String sectionContext;
        private final String key;
        private final String ownerOfficial;
        private final String ownerNamed;
        private final String fieldOfficial;
        private final String fieldNamed;
        private final String descriptor;
        private final String valueType;
        private final String confidence;
        private final String note;

        private FieldBindingInfo(String sectionContext, String key, String ownerOfficial, String ownerNamed,
                                 String fieldOfficial, String fieldNamed, String descriptor, String valueType,
                                 String confidence, String note) {
            this.sectionContext = nullToEmpty(sectionContext);
            this.key = nullToEmpty(key);
            this.ownerOfficial = nullToEmpty(ownerOfficial);
            this.ownerNamed = nullToEmpty(ownerNamed);
            this.fieldOfficial = nullToEmpty(fieldOfficial);
            this.fieldNamed = nullToEmpty(fieldNamed);
            this.descriptor = nullToEmpty(descriptor);
            this.valueType = nullToEmpty(valueType);
            this.confidence = nullToEmpty(confidence);
            this.note = nullToEmpty(note);
        }

        public String sectionContext() {
            return sectionContext;
        }

        public String key() {
            return key;
        }

        public String ownerOfficial() {
            return ownerOfficial;
        }

        public String ownerNamed() {
            return ownerNamed;
        }

        public String fieldOfficial() {
            return fieldOfficial;
        }

        public String fieldNamed() {
            return fieldNamed;
        }

        public String descriptor() {
            return descriptor;
        }

        public String valueType() {
            return valueType;
        }

        public String confidence() {
            return confidence;
        }

        public String note() {
            return note;
        }
    }

    public static final class SectionCoverage {
        private final String sectionContext;
        private final String contextType;
        private final int officialEntries;
        private final int matchedEntries;
        private final int highMethodEntries;
        private final int officialOnlyEntries;
        private final double coveragePct;
        private final double highConfidencePct;

        private SectionCoverage(String sectionContext, String contextType, int officialEntries, int matchedEntries,
                                int highMethodEntries, int officialOnlyEntries,
                                double coveragePct, double highConfidencePct) {
            this.sectionContext = nullToEmpty(sectionContext);
            this.contextType = nullToEmpty(contextType);
            this.officialEntries = officialEntries;
            this.matchedEntries = matchedEntries;
            this.highMethodEntries = highMethodEntries;
            this.officialOnlyEntries = officialOnlyEntries;
            this.coveragePct = coveragePct;
            this.highConfidencePct = highConfidencePct;
        }

        public String sectionContext() {
            return sectionContext;
        }

        public String contextType() {
            return contextType;
        }

        public int officialEntries() {
            return officialEntries;
        }

        public int matchedEntries() {
            return matchedEntries;
        }

        public int highMethodEntries() {
            return highMethodEntries;
        }

        public int officialOnlyEntries() {
            return officialOnlyEntries;
        }

        public double coveragePct() {
            return coveragePct;
        }

        public double highConfidencePct() {
            return highConfidencePct;
        }
    }
}
