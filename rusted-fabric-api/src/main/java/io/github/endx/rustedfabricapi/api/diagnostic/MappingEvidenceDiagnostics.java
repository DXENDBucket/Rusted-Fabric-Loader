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
    private static final String ACTION_PROJECTILE_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_action_projectile_added_rows_v0_29.csv";
    private static final String ACTION_PROJECTILE_KEY_BINDINGS_RESOURCE =
            "/rustedfabricapi/mapping/rw_action_projectile_effect_turret_key_field_binding_v0_29.csv";
    private static final String ACTION_PROJECTILE_RUNTIME_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_action_projectile_runtime_added_rows_v0_30.csv";
    private static final String ACTION_PROJECTILE_RUNTIME_FIELD_BINDINGS_RESOURCE =
            "/rustedfabricapi/mapping/rw_action_projectile_runtime_field_binding_v0_30.csv";
    private static final String DEFERRED_AMBIGUOUS_TURRET_FIELDS_RESOURCE =
            "/rustedfabricapi/mapping/rw_deferred_ambiguous_turret_fields_v0_30.csv";
    private static final String RUNTIME_PATHING_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_pathing_added_rows_v0_31.csv";
    private static final String RUNTIME_FORMATION_TARGET_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_formation_target_added_rows_v0_33.csv";
    private static final String AUDIT_HOTFIX_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_audit_hotfix_rows_v0_33.csv";
    private static final String RUNTIME_ORDER_UPDATE_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_order_update_added_rows_v0_34.csv";
    private static final String PRIOR_WORK_SEMANTIC_FIXES_RESOURCE =
            "/rustedfabricapi/mapping/rw_prior_work_semantic_fixes_v0_34.csv";
    private static final String RUNTIME_FIRE_FAMILY_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_fire_family_added_updated_rows_v0_35.csv";
    private static final String RUNTIME_FIRE_FAMILY_OVERRIDE_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_fire_family_override_rows_v0_35.csv";
    private static final String RUNTIME_PROJECTILE_DAMAGE_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_projectile_damage_added_updated_rows_v0_36.csv";
    private static final String PRIOR_WORK_RUNTIME_FAMILY_FIXES_RESOURCE =
            "/rustedfabricapi/mapping/rw_prior_work_runtime_family_fixes_v0_36.csv";
    private static final String RUNTIME_DAMAGE_DEATH_FAMILY_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_damage_death_family_added_updated_rows_v0_37.csv";
    private static final String RUNTIME_DAMAGE_DEATH_FLOW_MAP_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_damage_death_flow_map_v0_37.csv";
    private static final String RUNTIME_DAMAGE_DEATH_FAMILY_COVERAGE_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_damage_death_family_coverage_v0_37.csv";
    private static final String RUNTIME_LIFECYCLE_DRAW_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_lifecycle_draw_added_updated_rows_v0_38.csv";
    private static final String RUNTIME_LIFECYCLE_DRAW_FLOW_MAP_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_lifecycle_draw_flow_map_v0_38.csv";
    private static final String RUNTIME_LIFECYCLE_DRAW_FAMILY_COVERAGE_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_lifecycle_draw_family_coverage_v0_38.csv";
    private static final String RUNTIME_BUILD_QUEUE_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_build_queue_added_updated_rows_v0_39.csv";
    private static final String RUNTIME_BUILD_QUEUE_FLOW_MAP_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_build_queue_flow_map_v0_39.csv";
    private static final String RUNTIME_BUILD_QUEUE_FAMILY_COVERAGE_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_build_queue_family_coverage_v0_39.csv";

    private MappingEvidenceDiagnostics() {
    }

    public static List<MappingEvidenceRow> allLogicBooleanMembers() {
        return Holder.LOGIC_BOOLEAN_MEMBERS;
    }

    public static List<MappingEvidenceRow> allParserHelpers() {
        return Holder.PARSER_HELPERS;
    }

    public static List<MappingEvidenceRow> allActionProjectileRows() {
        return Holder.ACTION_PROJECTILE_ROWS;
    }

    public static List<KeyFieldBindingRow> allActionProjectileKeyFieldBindings() {
        return Holder.ACTION_PROJECTILE_KEY_BINDINGS;
    }

    public static List<MappingEvidenceRow> allActionProjectileRuntimeRows() {
        return Holder.ACTION_PROJECTILE_RUNTIME_ROWS;
    }

    public static List<RuntimeFieldBindingRow> allActionProjectileRuntimeFieldBindings() {
        return Holder.ACTION_PROJECTILE_RUNTIME_FIELD_BINDINGS;
    }

    public static List<DeferredMemberRow> allDeferredAmbiguousTurretFields() {
        return Holder.DEFERRED_AMBIGUOUS_TURRET_FIELDS;
    }

    public static List<MappingEvidenceRow> allRuntimePathingRows() {
        return Holder.RUNTIME_PATHING_ROWS;
    }

    public static List<MappingEvidenceRow> allRuntimeFormationTargetRows() {
        return Holder.RUNTIME_FORMATION_TARGET_ROWS;
    }

    public static List<MappingEvidenceRow> allAuditHotfixRows() {
        return Holder.AUDIT_HOTFIX_ROWS;
    }

    public static List<MappingEvidenceRow> allRuntimeOrderUpdateRows() {
        return Holder.RUNTIME_ORDER_UPDATE_ROWS;
    }

    public static List<MappingEvidenceRow> allPriorWorkSemanticFixes() {
        return Holder.PRIOR_WORK_SEMANTIC_FIXES;
    }

    public static List<MappingEvidenceRow> allRuntimeFireFamilyRows() {
        return Holder.RUNTIME_FIRE_FAMILY_ROWS;
    }

    public static List<MappingEvidenceRow> allRuntimeFireFamilyOverrideRows() {
        return Holder.RUNTIME_FIRE_FAMILY_OVERRIDE_ROWS;
    }

    public static List<MappingEvidenceRow> allRuntimeProjectileDamageRows() {
        return Holder.RUNTIME_PROJECTILE_DAMAGE_ROWS;
    }

    public static List<MappingEvidenceRow> allPriorWorkRuntimeFamilyFixes() {
        return Holder.PRIOR_WORK_RUNTIME_FAMILY_FIXES;
    }

    public static List<MappingEvidenceRow> allRuntimeDamageDeathFamilyRows() {
        return Holder.RUNTIME_DAMAGE_DEATH_FAMILY_ROWS;
    }

    public static List<MappingEvidenceRow> allRuntimeDamageDeathFlowMap() {
        return Holder.RUNTIME_DAMAGE_DEATH_FLOW_MAP;
    }

    public static List<MappingEvidenceRow> allRuntimeDamageDeathFamilyCoverage() {
        return Holder.RUNTIME_DAMAGE_DEATH_FAMILY_COVERAGE;
    }

    public static List<MappingEvidenceRow> allRuntimeLifecycleDrawRows() {
        return Holder.RUNTIME_LIFECYCLE_DRAW_ROWS;
    }

    public static List<MappingEvidenceRow> allRuntimeLifecycleDrawFlowMap() {
        return Holder.RUNTIME_LIFECYCLE_DRAW_FLOW_MAP;
    }

    public static List<MappingEvidenceRow> allRuntimeLifecycleDrawFamilyCoverage() {
        return Holder.RUNTIME_LIFECYCLE_DRAW_FAMILY_COVERAGE;
    }

    public static List<MappingEvidenceRow> allRuntimeBuildQueueRows() {
        return Holder.RUNTIME_BUILD_QUEUE_ROWS;
    }

    public static List<MappingEvidenceRow> allRuntimeBuildQueueFlowMap() {
        return Holder.RUNTIME_BUILD_QUEUE_FLOW_MAP;
    }

    public static List<MappingEvidenceRow> allRuntimeBuildQueueFamilyCoverage() {
        return Holder.RUNTIME_BUILD_QUEUE_FAMILY_COVERAGE;
    }

    public static List<String> evidenceResourceIds() {
        return Holder.EVIDENCE_RESOURCE_IDS;
    }

    public static Map<String, List<MappingEvidenceRow>> allEvidenceRowsByResource() {
        return Holder.EVIDENCE_ROWS_BY_ID;
    }

    public static List<MappingEvidenceRow> allEvidenceRows(String resourceId) {
        List<MappingEvidenceRow> rows = Holder.EVIDENCE_ROWS_BY_ID.get(normalizeResourceId(resourceId));
        return rows != null ? rows : Collections.<MappingEvidenceRow>emptyList();
    }

    public static List<MappingEvidenceRow> findLogicBooleanMembers(String text) {
        return findByText(Holder.LOGIC_BOOLEAN_MEMBERS, text);
    }

    public static List<MappingEvidenceRow> findParserHelpers(String text) {
        return findByText(Holder.PARSER_HELPERS, text);
    }

    public static List<MappingEvidenceRow> findActionProjectileRows(String text) {
        return findByText(Holder.ACTION_PROJECTILE_ROWS, text);
    }

    public static List<KeyFieldBindingRow> findActionProjectileKeyFieldBindings(String text) {
        return findKeyFieldBindingsByText(Holder.ACTION_PROJECTILE_KEY_BINDINGS, text);
    }

    public static List<MappingEvidenceRow> findActionProjectileRuntimeRows(String text) {
        return findByText(Holder.ACTION_PROJECTILE_RUNTIME_ROWS, text);
    }

    public static List<RuntimeFieldBindingRow> findActionProjectileRuntimeFieldBindings(String text) {
        return findRuntimeFieldBindingsByText(Holder.ACTION_PROJECTILE_RUNTIME_FIELD_BINDINGS, text);
    }

    public static List<MappingEvidenceRow> findRuntimePathingRows(String text) {
        return findByText(Holder.RUNTIME_PATHING_ROWS, text);
    }

    public static List<MappingEvidenceRow> findRuntimeProjectileDamageRows(String text) {
        return findByText(Holder.RUNTIME_PROJECTILE_DAMAGE_ROWS, text);
    }

    public static List<MappingEvidenceRow> findRuntimeDamageDeathFamilyRows(String text) {
        return findByText(Holder.RUNTIME_DAMAGE_DEATH_FAMILY_ROWS, text);
    }

    public static List<MappingEvidenceRow> findRuntimeLifecycleDrawRows(String text) {
        return findByText(Holder.RUNTIME_LIFECYCLE_DRAW_ROWS, text);
    }

    public static List<MappingEvidenceRow> findRuntimeBuildQueueRows(String text) {
        return findByText(Holder.RUNTIME_BUILD_QUEUE_ROWS, text);
    }

    public static List<MappingEvidenceRow> findEvidenceRows(String resourceId, String text) {
        return findByText(allEvidenceRows(resourceId), text);
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

    private static List<RuntimeFieldBindingRow> findRuntimeFieldBindingsByText(List<RuntimeFieldBindingRow> rows,
                                                                               String text) {
        String needle = normalize(text);
        if (needle.isEmpty()) {
            return rows;
        }

        List<RuntimeFieldBindingRow> result = new ArrayList<RuntimeFieldBindingRow>();
        for (RuntimeFieldBindingRow row : rows) {
            if (normalize(row.domain()).contains(needle)
                    || normalize(row.ownerOfficial()).contains(needle)
                    || normalize(row.fieldOfficial()).contains(needle)
                    || normalize(row.descriptor()).contains(needle)
                    || normalize(row.fieldNamed()).contains(needle)
                    || normalize(row.confidence()).contains(needle)
                    || normalize(row.evidence()).contains(needle)) {
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

    private static List<RuntimeFieldBindingRow> loadRuntimeFieldBindingRows(String resource) {
        List<Map<String, String>> rows = loadCsv(resource);
        List<RuntimeFieldBindingRow> result = new ArrayList<RuntimeFieldBindingRow>();
        for (Map<String, String> row : rows) {
            result.add(new RuntimeFieldBindingRow(
                    row.get("domain"),
                    row.get("owner_official"),
                    row.get("field_official"),
                    row.get("descriptor"),
                    row.get("field_named"),
                    row.get("confidence"),
                    row.get("evidence")));
        }
        return Collections.unmodifiableList(result);
    }

    private static List<DeferredMemberRow> loadDeferredMemberRows(String resource) {
        List<Map<String, String>> rows = loadCsv(resource);
        List<DeferredMemberRow> result = new ArrayList<DeferredMemberRow>();
        for (Map<String, String> row : rows) {
            result.add(new DeferredMemberRow(
                    row.get("owner_official"),
                    row.get("official_name"),
                    row.get("descriptor"),
                    row.get("reason")));
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
                    || normalize(row.descriptor()).contains(needle)
                    || normalize(row.source()).contains(needle)
                    || normalize(row.namedName()).contains(needle)
                    || normalize(row.officialName()).contains(needle)
                    || normalize(row.intermediaryName()).contains(needle)
                    || normalize(row.category()).contains(needle)
                    || normalize(row.evidence()).contains(needle)
                    || normalize(row.notes()).contains(needle)) {
                result.add(row);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static List<KeyFieldBindingRow> findKeyFieldBindingsByText(List<KeyFieldBindingRow> rows, String text) {
        String needle = normalize(text);
        if (needle.isEmpty()) {
            return rows;
        }

        List<KeyFieldBindingRow> result = new ArrayList<KeyFieldBindingRow>();
        for (KeyFieldBindingRow row : rows) {
            if (normalize(row.domain()).contains(needle)
                    || normalize(row.iniKey()).contains(needle)
                    || normalize(row.ownerOfficial()).contains(needle)
                    || normalize(row.fieldOfficial()).contains(needle)
                    || normalize(row.descriptor()).contains(needle)
                    || normalize(row.fieldNamed()).contains(needle)
                    || normalize(row.mappingSource()).contains(needle)
                    || normalize(row.evidence()).contains(needle)) {
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
                    first(row, "kind"),
                    first(row, "owner_official", "owner"),
                    first(row, "descriptor"),
                    first(row, "official_name", "member", "target"),
                    first(row, "intermediary_name", "new_intermediary", "old_intermediary"),
                    first(row, "named_name", "new_named_name", "new_named", "named_name", "mapped_name"),
                    first(row, "source", "mapping_source"),
                    first(row, "category", "runtime_stage", "stage", "family_named", "phase", "family"),
                    first(row, "confidence"),
                    first(row, "evidence", "semantics", "contract"),
                    first(row, "notes", "reason")));
        }
        return Collections.unmodifiableList(result);
    }

    private static Map<String, List<MappingEvidenceRow>> createEvidenceRowsById() {
        Map<String, List<MappingEvidenceRow>> result = new LinkedHashMap<String, List<MappingEvidenceRow>>();
        result.put("logic_boolean_members", Holder.LOGIC_BOOLEAN_MEMBERS);
        result.put("parser_helpers", Holder.PARSER_HELPERS);
        result.put("action_projectile_rows", Holder.ACTION_PROJECTILE_ROWS);
        result.put("action_projectile_runtime_rows", Holder.ACTION_PROJECTILE_RUNTIME_ROWS);
        result.put("runtime_pathing_rows", Holder.RUNTIME_PATHING_ROWS);
        result.put("runtime_formation_target_rows", Holder.RUNTIME_FORMATION_TARGET_ROWS);
        result.put("audit_hotfix_rows", Holder.AUDIT_HOTFIX_ROWS);
        result.put("runtime_order_update_rows", Holder.RUNTIME_ORDER_UPDATE_ROWS);
        result.put("prior_work_semantic_fixes", Holder.PRIOR_WORK_SEMANTIC_FIXES);
        result.put("runtime_fire_family_rows", Holder.RUNTIME_FIRE_FAMILY_ROWS);
        result.put("runtime_fire_family_override_rows", Holder.RUNTIME_FIRE_FAMILY_OVERRIDE_ROWS);
        result.put("runtime_projectile_damage_rows", Holder.RUNTIME_PROJECTILE_DAMAGE_ROWS);
        result.put("prior_work_runtime_family_fixes", Holder.PRIOR_WORK_RUNTIME_FAMILY_FIXES);
        result.put("runtime_damage_death_family_rows", Holder.RUNTIME_DAMAGE_DEATH_FAMILY_ROWS);
        result.put("runtime_damage_death_flow_map", Holder.RUNTIME_DAMAGE_DEATH_FLOW_MAP);
        result.put("runtime_damage_death_family_coverage", Holder.RUNTIME_DAMAGE_DEATH_FAMILY_COVERAGE);
        result.put("runtime_lifecycle_draw_rows", Holder.RUNTIME_LIFECYCLE_DRAW_ROWS);
        result.put("runtime_lifecycle_draw_flow_map", Holder.RUNTIME_LIFECYCLE_DRAW_FLOW_MAP);
        result.put("runtime_lifecycle_draw_family_coverage", Holder.RUNTIME_LIFECYCLE_DRAW_FAMILY_COVERAGE);
        result.put("runtime_build_queue_rows", Holder.RUNTIME_BUILD_QUEUE_ROWS);
        result.put("runtime_build_queue_flow_map", Holder.RUNTIME_BUILD_QUEUE_FLOW_MAP);
        result.put("runtime_build_queue_family_coverage", Holder.RUNTIME_BUILD_QUEUE_FAMILY_COVERAGE);
        return Collections.unmodifiableMap(result);
    }

    private static String first(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String value = row.get(key);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private static List<KeyFieldBindingRow> loadKeyFieldBindingRows(String resource) {
        List<Map<String, String>> rows = loadCsv(resource);
        List<KeyFieldBindingRow> result = new ArrayList<KeyFieldBindingRow>();
        for (Map<String, String> row : rows) {
            result.add(new KeyFieldBindingRow(
                    row.get("domain"),
                    row.get("ini_key"),
                    row.get("owner_official"),
                    row.get("field_official"),
                    row.get("descriptor"),
                    row.get("field_named"),
                    row.get("mapping_source"),
                    row.get("evidence")));
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

    private static String normalizeResourceId(String value) {
        return normalize(value).replace('-', '_');
    }

    private static final class Holder {
        private static final List<MappingEvidenceRow> LOGIC_BOOLEAN_MEMBERS = loadRows(LOGIC_BOOLEAN_RESOURCE);
        private static final List<MappingEvidenceRow> PARSER_HELPERS = loadRows(PARSER_HELPER_RESOURCE);
        private static final List<MappingEvidenceRow> ACTION_PROJECTILE_ROWS = loadRows(ACTION_PROJECTILE_ROWS_RESOURCE);
        private static final List<KeyFieldBindingRow> ACTION_PROJECTILE_KEY_BINDINGS =
                loadKeyFieldBindingRows(ACTION_PROJECTILE_KEY_BINDINGS_RESOURCE);
        private static final List<MappingEvidenceRow> ACTION_PROJECTILE_RUNTIME_ROWS =
                loadRows(ACTION_PROJECTILE_RUNTIME_ROWS_RESOURCE);
        private static final List<RuntimeFieldBindingRow> ACTION_PROJECTILE_RUNTIME_FIELD_BINDINGS =
                loadRuntimeFieldBindingRows(ACTION_PROJECTILE_RUNTIME_FIELD_BINDINGS_RESOURCE);
        private static final List<DeferredMemberRow> DEFERRED_AMBIGUOUS_TURRET_FIELDS =
                loadDeferredMemberRows(DEFERRED_AMBIGUOUS_TURRET_FIELDS_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_PATHING_ROWS =
                loadRows(RUNTIME_PATHING_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_FORMATION_TARGET_ROWS =
                loadRows(RUNTIME_FORMATION_TARGET_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> AUDIT_HOTFIX_ROWS =
                loadRows(AUDIT_HOTFIX_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_ORDER_UPDATE_ROWS =
                loadRows(RUNTIME_ORDER_UPDATE_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> PRIOR_WORK_SEMANTIC_FIXES =
                loadRows(PRIOR_WORK_SEMANTIC_FIXES_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_FIRE_FAMILY_ROWS =
                loadRows(RUNTIME_FIRE_FAMILY_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_FIRE_FAMILY_OVERRIDE_ROWS =
                loadRows(RUNTIME_FIRE_FAMILY_OVERRIDE_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_PROJECTILE_DAMAGE_ROWS =
                loadRows(RUNTIME_PROJECTILE_DAMAGE_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> PRIOR_WORK_RUNTIME_FAMILY_FIXES =
                loadRows(PRIOR_WORK_RUNTIME_FAMILY_FIXES_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_DAMAGE_DEATH_FAMILY_ROWS =
                loadRows(RUNTIME_DAMAGE_DEATH_FAMILY_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_DAMAGE_DEATH_FLOW_MAP =
                loadRows(RUNTIME_DAMAGE_DEATH_FLOW_MAP_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_DAMAGE_DEATH_FAMILY_COVERAGE =
                loadRows(RUNTIME_DAMAGE_DEATH_FAMILY_COVERAGE_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_LIFECYCLE_DRAW_ROWS =
                loadRows(RUNTIME_LIFECYCLE_DRAW_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_LIFECYCLE_DRAW_FLOW_MAP =
                loadRows(RUNTIME_LIFECYCLE_DRAW_FLOW_MAP_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_LIFECYCLE_DRAW_FAMILY_COVERAGE =
                loadRows(RUNTIME_LIFECYCLE_DRAW_FAMILY_COVERAGE_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_BUILD_QUEUE_ROWS =
                loadRows(RUNTIME_BUILD_QUEUE_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_BUILD_QUEUE_FLOW_MAP =
                loadRows(RUNTIME_BUILD_QUEUE_FLOW_MAP_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_BUILD_QUEUE_FAMILY_COVERAGE =
                loadRows(RUNTIME_BUILD_QUEUE_FAMILY_COVERAGE_RESOURCE);
        private static final Map<String, List<MappingEvidenceRow>> EVIDENCE_ROWS_BY_ID =
                createEvidenceRowsById();
        private static final List<String> EVIDENCE_RESOURCE_IDS =
                Collections.unmodifiableList(new ArrayList<String>(EVIDENCE_ROWS_BY_ID.keySet()));
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

    public static final class KeyFieldBindingRow {
        private final String domain;
        private final String iniKey;
        private final String ownerOfficial;
        private final String fieldOfficial;
        private final String descriptor;
        private final String fieldNamed;
        private final String mappingSource;
        private final String evidence;

        private KeyFieldBindingRow(String domain, String iniKey, String ownerOfficial, String fieldOfficial,
                                   String descriptor, String fieldNamed, String mappingSource, String evidence) {
            this.domain = nullToEmpty(domain);
            this.iniKey = nullToEmpty(iniKey);
            this.ownerOfficial = nullToEmpty(ownerOfficial);
            this.fieldOfficial = nullToEmpty(fieldOfficial);
            this.descriptor = nullToEmpty(descriptor);
            this.fieldNamed = nullToEmpty(fieldNamed);
            this.mappingSource = nullToEmpty(mappingSource);
            this.evidence = nullToEmpty(evidence);
        }

        public String domain() {
            return domain;
        }

        public String iniKey() {
            return iniKey;
        }

        public String ownerOfficial() {
            return ownerOfficial;
        }

        public String fieldOfficial() {
            return fieldOfficial;
        }

        public String descriptor() {
            return descriptor;
        }

        public String fieldNamed() {
            return fieldNamed;
        }

        public String mappingSource() {
            return mappingSource;
        }

        public String evidence() {
            return evidence;
        }

        private static String nullToEmpty(String value) {
            return value != null ? value : "";
        }
    }

    public static final class RuntimeFieldBindingRow {
        private final String domain;
        private final String ownerOfficial;
        private final String fieldOfficial;
        private final String descriptor;
        private final String fieldNamed;
        private final String confidence;
        private final String evidence;

        private RuntimeFieldBindingRow(String domain, String ownerOfficial, String fieldOfficial,
                                       String descriptor, String fieldNamed, String confidence, String evidence) {
            this.domain = nullToEmpty(domain);
            this.ownerOfficial = nullToEmpty(ownerOfficial);
            this.fieldOfficial = nullToEmpty(fieldOfficial);
            this.descriptor = nullToEmpty(descriptor);
            this.fieldNamed = nullToEmpty(fieldNamed);
            this.confidence = nullToEmpty(confidence);
            this.evidence = nullToEmpty(evidence);
        }

        public String domain() {
            return domain;
        }

        public String ownerOfficial() {
            return ownerOfficial;
        }

        public String fieldOfficial() {
            return fieldOfficial;
        }

        public String descriptor() {
            return descriptor;
        }

        public String fieldNamed() {
            return fieldNamed;
        }

        public String confidence() {
            return confidence;
        }

        public String evidence() {
            return evidence;
        }

        private static String nullToEmpty(String value) {
            return value != null ? value : "";
        }
    }

    public static final class DeferredMemberRow {
        private final String ownerOfficial;
        private final String officialName;
        private final String descriptor;
        private final String reason;

        private DeferredMemberRow(String ownerOfficial, String officialName, String descriptor, String reason) {
            this.ownerOfficial = nullToEmpty(ownerOfficial);
            this.officialName = nullToEmpty(officialName);
            this.descriptor = nullToEmpty(descriptor);
            this.reason = nullToEmpty(reason);
        }

        public String ownerOfficial() {
            return ownerOfficial;
        }

        public String officialName() {
            return officialName;
        }

        public String descriptor() {
            return descriptor;
        }

        public String reason() {
            return reason;
        }

        private static String nullToEmpty(String value) {
            return value != null ? value : "";
        }
    }
}
