package io.github.endx.rustedfabricapi.api.diagnostic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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
    private static final String DEVELOPMENT_EVIDENCE_DIRECTORY = "report/mapping-evidence";
    private static final String MAPPING_EVIDENCE_MANIFEST_RESOURCE =
            "/rustedfabricapi/mapping/mapping-evidence-manifest.csv";

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
    private static final String RUNTIME_REPAIR_RECLAIM_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_repair_reclaim_added_updated_rows_v0_40.csv";
    private static final String RUNTIME_REPAIR_RECLAIM_FLOW_MAP_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_repair_reclaim_flow_map_v0_40.csv";
    private static final String RUNTIME_REPAIR_RECLAIM_FAMILY_COVERAGE_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_repair_reclaim_family_coverage_v0_40.csv";
    private static final String RUNTIME_TRANSPORT_ATTACHMENT_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_transport_attachment_added_updated_rows_v0_41.csv";
    private static final String RUNTIME_TRANSPORT_ATTACHMENT_FLOW_MAP_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_transport_attachment_flow_map_v0_41.csv";
    private static final String RUNTIME_TRANSPORT_ATTACHMENT_FAMILY_COVERAGE_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_transport_attachment_family_coverage_v0_41.csv";
    private static final String ATTACHMENT_SLOT_SEMANTIC_HOTFIX_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_attachment_slot_semantic_hotfix_rows_v0_41.csv";
    private static final String RUNTIME_ACTION_COMMAND_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_action_command_added_rows_v0_42.csv";
    private static final String RUNTIME_ACTION_COMMAND_FLOW_MAP_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_action_command_flow_map_v0_42.csv";
    private static final String RUNTIME_ACTION_COMMAND_FAMILY_COVERAGE_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_action_command_family_coverage_v0_42.csv";
    private static final String RUNTIME_RESOURCE_ECONOMY_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_resource_economy_added_rows_v0_43.csv";
    private static final String RUNTIME_RESOURCE_ECONOMY_UPDATED_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_resource_economy_updated_rows_v0_43.csv";
    private static final String RUNTIME_RESOURCE_ECONOMY_FLOW_MAP_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_resource_economy_flow_map_v0_43.csv";
    private static final String RUNTIME_RESOURCE_ECONOMY_FAMILY_COVERAGE_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_resource_economy_family_coverage_v0_43.csv";
    private static final String RUNTIME_COMMAND_ISSUE_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_command_issue_merged_added_rows_v0_44.csv";
    private static final String RUNTIME_COMMAND_ISSUE_UPDATED_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_command_issue_merged_updated_rows_v0_44.csv";
    private static final String RUNTIME_COMMAND_ISSUE_EVIDENCE_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_command_issue_evidence_rows_v0_44.csv";
    private static final String RUNTIME_COMMAND_ISSUE_FLOW_MAP_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_command_issue_flow_map_v0_44.csv";
    private static final String RUNTIME_COMMAND_ISSUE_SKIPPED_BRANCH_ROLLBACKS_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_command_issue_skipped_branch_rollbacks_v0_44.csv";
    private static final String RUNTIME_TEAM_STATS_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_team_stats_added_updated_rows_v0_43.csv";
    private static final String RUNTIME_TEAM_STATS_HOTFIX_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_team_stats_hotfix_rows_v0_43.csv";
    private static final String RUNTIME_TEAM_STATS_FLOW_MAP_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_team_stats_flow_map_v0_43.csv";
    private static final String RUNTIME_VISIBILITY_SPATIAL_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_visibility_spatial_merge_added_rows_v0_46.csv";
    private static final String RUNTIME_VISIBILITY_SPATIAL_UPDATED_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_visibility_spatial_merge_updated_rows_v0_46.csv";
    private static final String RUNTIME_VISIBILITY_SPATIAL_FLOW_MAP_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_visibility_spatial_flow_map_v0_46.csv";
    private static final String RUNTIME_VISIBILITY_SPATIAL_BRANCH_UPDATE_REVIEW_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_visibility_spatial_branch_update_review_v0_46.csv";
    private static final String RUNTIME_VISIBILITY_SPATIAL_SKIPPED_BRANCH_ROLLBACKS_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_visibility_spatial_skipped_branch_rollbacks_v0_46.csv";
    private static final String RUNTIME_REPLAY_CHECKSUM_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_replay_checksum_added_rows_v0_47.csv";
    private static final String RUNTIME_REPLAY_CHECKSUM_UPDATED_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_replay_checksum_updated_rows_v0_47.csv";
    private static final String RUNTIME_REPLAY_CHECKSUM_FLOW_MAP_RESOURCE =
            "/rustedfabricapi/mapping/rw_runtime_replay_checksum_flow_map_v0_47.csv";
    private static final String NETWORK_CHECKSUM_BUCKET_EVIDENCE_RESOURCE =
            "/rustedfabricapi/mapping/rw_network_checksum_bucket_evidence_v0_47.csv";
    private static final String CUSTOM_LOGIC_STAT_BEHAVIOR_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_custom_logic_stat_behavior_added_rows_v0_52.csv";
    private static final String CUSTOM_LOGIC_STAT_BEHAVIOR_FLOW_MAP_RESOURCE =
            "/rustedfabricapi/mapping/rw_custom_logic_stat_behavior_flow_map_v0_52.csv";
    private static final String CUSTOM_LOGIC_STAT_BEHAVIOR_COVERAGE_RESOURCE =
            "/rustedfabricapi/mapping/rw_custom_logic_stat_behavior_coverage_v0_52.csv";
    private static final String CUSTOM_LOGIC_GEOMETRY_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_custom_logic_geometry_rows_v0_52.csv";
    private static final String CUSTOM_MUTABLE_STAT_WRITER_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_custom_mutable_stat_writer_rows_v0_52.csv";
    private static final String CUSTOM_MOVEMENT_MICRO_BEHAVIOR_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_custom_movement_micro_behavior_rows_v0_52.csv";
    private static final String MAP_TERRAIN_TILESET_MERGE_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_map_terrain_tileset_merge_added_rows_v0_53.csv";
    private static final String MAP_TERRAIN_TILESET_MERGE_UPDATED_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_map_terrain_tileset_merge_updated_rows_v0_53.csv";
    private static final String MAP_TERRAIN_TILESET_FLOW_MAP_RESOURCE =
            "/rustedfabricapi/mapping/rw_map_terrain_tileset_flow_map_v0_53.csv";
    private static final String MAP_TERRAIN_TILESET_BRANCH_SKIPPED_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_map_terrain_tileset_branch_skipped_rows_v0_53.csv";
    private static final String TILE_ATLAS_RENDER_CACHE_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_tileatlas_render_cache_rows_v0_53.csv";
    private static final String EFFECT_RUNTIME_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_effect_runtime_added_updated_rows_v0_54.csv";
    private static final String EFFECT_ENGINE_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_effect_engine_rows_v0_54.csv";
    private static final String EFFECT_INSTANCE_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_effect_instance_rows_v0_54.csv";
    private static final String EFFECT_ENUM_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_effect_enum_rows_v0_54.csv";
    private static final String EFFECT_RUNTIME_FLOW_MAP_RESOURCE =
            "/rustedfabricapi/mapping/rw_effect_runtime_flow_map_v0_54.csv";
    private static final String MISSION_TRIGGER_MAP_SCRIPT_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_mission_trigger_map_script_added_rows_v0_55.csv";
    private static final String MISSION_TRIGGER_MAP_SCRIPT_UPDATED_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_mission_trigger_map_script_updated_rows_v0_55.csv";
    private static final String MISSION_TRIGGER_MAP_SCRIPT_FLOW_MAP_RESOURCE =
            "/rustedfabricapi/mapping/rw_mission_trigger_map_script_flow_map_v0_55.csv";
    private static final String MISSION_TRIGGER_SEMANTIC_HOTFIX_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_mission_trigger_semantic_hotfix_rows_v0_55.csv";
    private static final String MISSION_TRIGGER_TYPE_ANONYMOUS_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_mission_trigger_type_anonymous_rows_v0_55.csv";
    private static final String RENDER_CANVAS_COMMAND_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_render_canvas_command_added_rows_v0_56.csv";
    private static final String RENDER_CANVAS_COMMAND_UPDATED_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_render_canvas_command_updated_rows_v0_56.csv";
    private static final String RENDER_CANVAS_COMMAND_SKIPPED_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_render_canvas_command_skipped_rows_v0_56.csv";
    private static final String RENDER_CANVAS_COMMAND_FLOW_MAP_RESOURCE =
            "/rustedfabricapi/mapping/rw_render_canvas_command_flow_map_v0_56.csv";
    private static final String RENDER_CANVAS_COMMAND_FAMILY_COVERAGE_RESOURCE =
            "/rustedfabricapi/mapping/rw_render_canvas_command_family_coverage_v0_56.csv";
    private static final String CANVAS_OPERATION_ENUM_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_canvas_operation_enum_rows_v0_56.csv";
    private static final String CANVAS_DRAWTARGET_COMMAND_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_canvas_drawtarget_command_rows_v0_56.csv";
    private static final String SHADER_PROGRAM_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_shader_program_rows_v0_56.csv";
    private static final String UI_MINIMAP_COMMAND_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_ui_minimap_command_merge_added_rows_v0_57.csv";
    private static final String UI_MINIMAP_COMMAND_FLOW_MAP_RESOURCE =
            "/rustedfabricapi/mapping/rw_ui_minimap_command_flow_map_v0_57.csv";
    private static final String UI_MINIMAP_COMMAND_FAMILY_COVERAGE_RESOURCE =
            "/rustedfabricapi/mapping/rw_ui_minimap_command_family_coverage_v0_57.csv";
    private static final String UI_MINIMAP_COMMAND_BRANCH_CONFLICTS_RESOURCE =
            "/rustedfabricapi/mapping/rw_ui_minimap_command_branch_conflicts_v0_57.csv";
    private static final String RENDER_GL_BACKEND_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_render_gl_backend_added_rows_v0_58.csv";
    private static final String RENDER_GL_BACKEND_CANVAS_SHADER_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_render_gl_backend_canvas_shader_rows_v0_58.csv";
    private static final String RENDER_GL_BACKEND_FLOW_MAP_RESOURCE =
            "/rustedfabricapi/mapping/rw_render_gl_backend_flow_map_v0_58.csv";
    private static final String RENDER_GL_BACKEND_SKIPPED_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_render_gl_backend_skipped_rows_v0_58.csv";
    private static final String RENDER_GL_BACKEND_TEXTURE_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_render_gl_backend_texture_rows_v0_58.csv";
    private static final String RENDER_GL_TEXT_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_render_gl_text_rows_v0_58.csv";
    private static final String FILESYSTEM_BACKEND_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_filesystem_backend_added_rows_v0_59.csv";
    private static final String FILESYSTEM_BACKEND_UPDATED_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_filesystem_backend_updated_rows_v0_59.csv";
    private static final String FILESYSTEM_BACKEND_SKIPPED_ROWS_RESOURCE =
            "/rustedfabricapi/mapping/rw_filesystem_backend_skipped_rows_v0_59.csv";
    private static final String FILESYSTEM_BACKEND_FLOW_MAP_RESOURCE =
            "/rustedfabricapi/mapping/rw_filesystem_backend_flow_map_v0_59.csv";
    private static final String FILESYSTEM_BACKEND_COVERAGE_RESOURCE =
            "/rustedfabricapi/mapping/rw_filesystem_backend_coverage_v0_59.csv";

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

    public static List<MappingEvidenceRow> allRuntimeRepairReclaimRows() {
        return Holder.RUNTIME_REPAIR_RECLAIM_ROWS;
    }

    public static List<MappingEvidenceRow> allRuntimeRepairReclaimFlowMap() {
        return Holder.RUNTIME_REPAIR_RECLAIM_FLOW_MAP;
    }

    public static List<MappingEvidenceRow> allRuntimeRepairReclaimFamilyCoverage() {
        return Holder.RUNTIME_REPAIR_RECLAIM_FAMILY_COVERAGE;
    }

    public static List<MappingEvidenceRow> allRuntimeTransportAttachmentRows() {
        return Holder.RUNTIME_TRANSPORT_ATTACHMENT_ROWS;
    }

    public static List<MappingEvidenceRow> allRuntimeTransportAttachmentFlowMap() {
        return Holder.RUNTIME_TRANSPORT_ATTACHMENT_FLOW_MAP;
    }

    public static List<MappingEvidenceRow> allRuntimeTransportAttachmentFamilyCoverage() {
        return Holder.RUNTIME_TRANSPORT_ATTACHMENT_FAMILY_COVERAGE;
    }

    public static List<MappingEvidenceRow> allAttachmentSlotSemanticHotfixRows() {
        return Holder.ATTACHMENT_SLOT_SEMANTIC_HOTFIX_ROWS;
    }

    public static List<MappingEvidenceRow> allRuntimeActionCommandRows() {
        return Holder.RUNTIME_ACTION_COMMAND_ROWS;
    }

    public static List<MappingEvidenceRow> allRuntimeActionCommandFlowMap() {
        return Holder.RUNTIME_ACTION_COMMAND_FLOW_MAP;
    }

    public static List<MappingEvidenceRow> allRuntimeActionCommandFamilyCoverage() {
        return Holder.RUNTIME_ACTION_COMMAND_FAMILY_COVERAGE;
    }

    public static List<MappingEvidenceRow> allRuntimeResourceEconomyRows() {
        return Holder.RUNTIME_RESOURCE_ECONOMY_ROWS;
    }

    public static List<MappingEvidenceRow> allRuntimeResourceEconomyUpdatedRows() {
        return Holder.RUNTIME_RESOURCE_ECONOMY_UPDATED_ROWS;
    }

    public static List<MappingEvidenceRow> allRuntimeResourceEconomyFlowMap() {
        return Holder.RUNTIME_RESOURCE_ECONOMY_FLOW_MAP;
    }

    public static List<MappingEvidenceRow> allRuntimeResourceEconomyFamilyCoverage() {
        return Holder.RUNTIME_RESOURCE_ECONOMY_FAMILY_COVERAGE;
    }

    public static List<MappingEvidenceRow> allRuntimeCommandIssueRows() {
        return Holder.RUNTIME_COMMAND_ISSUE_ROWS;
    }

    public static List<MappingEvidenceRow> allRuntimeCommandIssueUpdatedRows() {
        return Holder.RUNTIME_COMMAND_ISSUE_UPDATED_ROWS;
    }

    public static List<MappingEvidenceRow> allRuntimeCommandIssueEvidenceRows() {
        return Holder.RUNTIME_COMMAND_ISSUE_EVIDENCE_ROWS;
    }

    public static List<MappingEvidenceRow> allRuntimeCommandIssueFlowMap() {
        return Holder.RUNTIME_COMMAND_ISSUE_FLOW_MAP;
    }

    public static List<MappingEvidenceRow> allRuntimeCommandIssueSkippedBranchRollbacks() {
        return Holder.RUNTIME_COMMAND_ISSUE_SKIPPED_BRANCH_ROLLBACKS;
    }

    public static List<MappingEvidenceRow> allRuntimeTeamStatsRows() {
        return Holder.RUNTIME_TEAM_STATS_ROWS;
    }

    public static List<MappingEvidenceRow> allRuntimeTeamStatsHotfixRows() {
        return Holder.RUNTIME_TEAM_STATS_HOTFIX_ROWS;
    }

    public static List<MappingEvidenceRow> allRuntimeTeamStatsFlowMap() {
        return Holder.RUNTIME_TEAM_STATS_FLOW_MAP;
    }

    public static List<MappingEvidenceRow> allRuntimeVisibilitySpatialRows() {
        return Holder.RUNTIME_VISIBILITY_SPATIAL_ROWS;
    }

    public static List<MappingEvidenceRow> allRuntimeVisibilitySpatialUpdatedRows() {
        return Holder.RUNTIME_VISIBILITY_SPATIAL_UPDATED_ROWS;
    }

    public static List<MappingEvidenceRow> allRuntimeVisibilitySpatialFlowMap() {
        return Holder.RUNTIME_VISIBILITY_SPATIAL_FLOW_MAP;
    }

    public static List<MappingEvidenceRow> allRuntimeVisibilitySpatialBranchUpdateReview() {
        return Holder.RUNTIME_VISIBILITY_SPATIAL_BRANCH_UPDATE_REVIEW;
    }

    public static List<MappingEvidenceRow> allRuntimeVisibilitySpatialSkippedBranchRollbacks() {
        return Holder.RUNTIME_VISIBILITY_SPATIAL_SKIPPED_BRANCH_ROLLBACKS;
    }

    public static List<MappingEvidenceRow> allRuntimeReplayChecksumRows() {
        return Holder.RUNTIME_REPLAY_CHECKSUM_ROWS;
    }

    public static List<MappingEvidenceRow> allRuntimeReplayChecksumUpdatedRows() {
        return Holder.RUNTIME_REPLAY_CHECKSUM_UPDATED_ROWS;
    }

    public static List<MappingEvidenceRow> allRuntimeReplayChecksumFlowMap() {
        return Holder.RUNTIME_REPLAY_CHECKSUM_FLOW_MAP;
    }

    public static List<MappingEvidenceRow> allNetworkChecksumBucketEvidence() {
        return Holder.NETWORK_CHECKSUM_BUCKET_EVIDENCE;
    }

    public static List<MappingEvidenceRow> allCustomLogicStatBehaviorRows() {
        return Holder.CUSTOM_LOGIC_STAT_BEHAVIOR_ROWS;
    }

    public static List<MappingEvidenceRow> allCustomLogicStatBehaviorFlowMap() {
        return Holder.CUSTOM_LOGIC_STAT_BEHAVIOR_FLOW_MAP;
    }

    public static List<MappingEvidenceRow> allCustomLogicStatBehaviorCoverage() {
        return Holder.CUSTOM_LOGIC_STAT_BEHAVIOR_COVERAGE;
    }

    public static List<MappingEvidenceRow> allCustomLogicGeometryRows() {
        return Holder.CUSTOM_LOGIC_GEOMETRY_ROWS;
    }

    public static List<MappingEvidenceRow> allCustomMutableStatWriterRows() {
        return Holder.CUSTOM_MUTABLE_STAT_WRITER_ROWS;
    }

    public static List<MappingEvidenceRow> allCustomMovementMicroBehaviorRows() {
        return Holder.CUSTOM_MOVEMENT_MICRO_BEHAVIOR_ROWS;
    }

    public static List<MappingEvidenceRow> allMapTerrainTilesetMergeRows() {
        return Holder.MAP_TERRAIN_TILESET_MERGE_ROWS;
    }

    public static List<MappingEvidenceRow> allMapTerrainTilesetMergeUpdatedRows() {
        return Holder.MAP_TERRAIN_TILESET_MERGE_UPDATED_ROWS;
    }

    public static List<MappingEvidenceRow> allMapTerrainTilesetFlowMap() {
        return Holder.MAP_TERRAIN_TILESET_FLOW_MAP;
    }

    public static List<MappingEvidenceRow> allMapTerrainTilesetBranchSkippedRows() {
        return Holder.MAP_TERRAIN_TILESET_BRANCH_SKIPPED_ROWS;
    }

    public static List<MappingEvidenceRow> allTileAtlasRenderCacheRows() {
        return Holder.TILE_ATLAS_RENDER_CACHE_ROWS;
    }

    public static List<MappingEvidenceRow> allEffectRuntimeRows() {
        return Holder.EFFECT_RUNTIME_ROWS;
    }

    public static List<MappingEvidenceRow> allEffectEngineRows() {
        return Holder.EFFECT_ENGINE_ROWS;
    }

    public static List<MappingEvidenceRow> allEffectInstanceRows() {
        return Holder.EFFECT_INSTANCE_ROWS;
    }

    public static List<MappingEvidenceRow> allEffectEnumRows() {
        return Holder.EFFECT_ENUM_ROWS;
    }

    public static List<MappingEvidenceRow> allEffectRuntimeFlowMap() {
        return Holder.EFFECT_RUNTIME_FLOW_MAP;
    }

    public static List<MappingEvidenceRow> allMissionTriggerMapScriptRows() {
        return Holder.MISSION_TRIGGER_MAP_SCRIPT_ROWS;
    }

    public static List<MappingEvidenceRow> allMissionTriggerMapScriptUpdatedRows() {
        return Holder.MISSION_TRIGGER_MAP_SCRIPT_UPDATED_ROWS;
    }

    public static List<MappingEvidenceRow> allMissionTriggerMapScriptFlowMap() {
        return Holder.MISSION_TRIGGER_MAP_SCRIPT_FLOW_MAP;
    }

    public static List<MappingEvidenceRow> allMissionTriggerSemanticHotfixRows() {
        return Holder.MISSION_TRIGGER_SEMANTIC_HOTFIX_ROWS;
    }

    public static List<MappingEvidenceRow> allMissionTriggerTypeAnonymousRows() {
        return Holder.MISSION_TRIGGER_TYPE_ANONYMOUS_ROWS;
    }

    public static List<EvidenceManifestRow> allEvidenceManifestRows() {
        return Holder.EVIDENCE_MANIFEST_ROWS;
    }

    public static List<MappingEvidenceRow> allRenderCanvasCommandRows() {
        return Holder.RENDER_CANVAS_COMMAND_ROWS;
    }

    public static List<MappingEvidenceRow> allRenderCanvasCommandUpdatedRows() {
        return Holder.RENDER_CANVAS_COMMAND_UPDATED_ROWS;
    }

    public static List<MappingEvidenceRow> allRenderCanvasCommandSkippedRows() {
        return Holder.RENDER_CANVAS_COMMAND_SKIPPED_ROWS;
    }

    public static List<MappingEvidenceRow> allRenderCanvasCommandFlowMap() {
        return Holder.RENDER_CANVAS_COMMAND_FLOW_MAP;
    }

    public static List<MappingEvidenceRow> allRenderCanvasCommandFamilyCoverage() {
        return Holder.RENDER_CANVAS_COMMAND_FAMILY_COVERAGE;
    }

    public static List<MappingEvidenceRow> allCanvasOperationEnumRows() {
        return Holder.CANVAS_OPERATION_ENUM_ROWS;
    }

    public static List<MappingEvidenceRow> allCanvasDrawTargetCommandRows() {
        return Holder.CANVAS_DRAWTARGET_COMMAND_ROWS;
    }

    public static List<MappingEvidenceRow> allShaderProgramRows() {
        return Holder.SHADER_PROGRAM_ROWS;
    }

    public static List<MappingEvidenceRow> allUiMinimapCommandRows() {
        return Holder.UI_MINIMAP_COMMAND_ROWS;
    }

    public static List<MappingEvidenceRow> allUiMinimapCommandFlowMap() {
        return Holder.UI_MINIMAP_COMMAND_FLOW_MAP;
    }

    public static List<MappingEvidenceRow> allUiMinimapCommandFamilyCoverage() {
        return Holder.UI_MINIMAP_COMMAND_FAMILY_COVERAGE;
    }

    public static List<MappingEvidenceRow> allUiMinimapCommandBranchConflicts() {
        return Holder.UI_MINIMAP_COMMAND_BRANCH_CONFLICTS;
    }

    public static List<MappingEvidenceRow> allRenderGlBackendRows() {
        return Holder.RENDER_GL_BACKEND_ROWS;
    }

    public static List<MappingEvidenceRow> allRenderGlBackendCanvasShaderRows() {
        return Holder.RENDER_GL_BACKEND_CANVAS_SHADER_ROWS;
    }

    public static List<MappingEvidenceRow> allRenderGlBackendFlowMap() {
        return Holder.RENDER_GL_BACKEND_FLOW_MAP;
    }

    public static List<MappingEvidenceRow> allRenderGlBackendSkippedRows() {
        return Holder.RENDER_GL_BACKEND_SKIPPED_ROWS;
    }

    public static List<MappingEvidenceRow> allRenderGlBackendTextureRows() {
        return Holder.RENDER_GL_BACKEND_TEXTURE_ROWS;
    }

    public static List<MappingEvidenceRow> allRenderGlTextRows() {
        return Holder.RENDER_GL_TEXT_ROWS;
    }

    public static List<MappingEvidenceRow> allFileSystemBackendRows() {
        return Holder.FILESYSTEM_BACKEND_ROWS;
    }

    public static List<MappingEvidenceRow> allFileSystemBackendUpdatedRows() {
        return Holder.FILESYSTEM_BACKEND_UPDATED_ROWS;
    }

    public static List<MappingEvidenceRow> allFileSystemBackendSkippedRows() {
        return Holder.FILESYSTEM_BACKEND_SKIPPED_ROWS;
    }

    public static List<MappingEvidenceRow> allFileSystemBackendFlowMap() {
        return Holder.FILESYSTEM_BACKEND_FLOW_MAP;
    }

    public static List<MappingEvidenceRow> allFileSystemBackendCoverage() {
        return Holder.FILESYSTEM_BACKEND_COVERAGE;
    }

    public static List<MappingEvidenceRow> allAudioBackendRows() {
        return allEvidenceRows("audio_backend_added_rows_v0_60");
    }

    public static List<MappingEvidenceRow> allAudioBackendUpdatedRows() {
        return allEvidenceRows("audio_backend_updated_rows_v0_60");
    }

    public static List<MappingEvidenceRow> allAudioBackendFamilyHotfixRows() {
        return allEvidenceRows("audio_backend_family_hotfix_rows_v0_60");
    }

    public static List<MappingEvidenceRow> allAudioBackendFlowMap() {
        return allEvidenceRows("audio_backend_flow_map_v0_60");
    }

    public static List<MappingEvidenceRow> allAudioFactoryBridgeRows() {
        return allEvidenceRows("audio_factory_bridge_rows_v0_60");
    }

    public static List<MappingEvidenceRow> allAudioOpenAlRows() {
        return allEvidenceRows("audio_openal_rows_v0_60");
    }

    public static List<MappingEvidenceRow> allAudioUtilityRows() {
        return allEvidenceRows("audio_utility_rows_v0_60");
    }

    public static List<MappingEvidenceRow> allAudioFamilyCompletionRows() {
        return allEvidenceRows("audio_family_completion_added_rows_v0_61");
    }

    public static List<MappingEvidenceRow> allAudioFamilyCompletionUpdatedRows() {
        return allEvidenceRows("audio_family_completion_updated_rows_v0_61");
    }

    public static List<MappingEvidenceRow> allAudioFamilyCompletionFlowMap() {
        return allEvidenceRows("audio_family_completion_flow_map_v0_61");
    }

    public static List<MappingEvidenceRow> allInputKeybindingRows() {
        return allEvidenceRows("input_keybinding_added_rows_v0_62");
    }

    public static List<MappingEvidenceRow> allInputKeybindingUpdatedRows() {
        return allEvidenceRows("input_keybinding_updated_rows_v0_62");
    }

    public static List<MappingEvidenceRow> allInputKeybindingFlowMap() {
        return allEvidenceRows("input_keybinding_flow_map_v0_62");
    }

    public static List<MappingEvidenceRow> allInputActionNamingHotfixRows() {
        return allEvidenceRows("input_action_naming_hotfix_added_rows_v0_63_1");
    }

    public static List<MappingEvidenceRow> allInputActionNamingHotfixUpdatedRows() {
        return allEvidenceRows("input_action_naming_hotfix_updated_rows_v0_63_1");
    }

    public static List<MappingEvidenceRow> allInputActionDisplayGroupResidueRows() {
        return allEvidenceRows("current_action_display_group_residue_v0_63_1");
    }

    public static List<MappingEvidenceRow> allLibRocketUiScriptSurfaceRows() {
        return allEvidenceRows("librocket_ui_script_surface_added_rows_v0_64");
    }

    public static List<MappingEvidenceRow> allLibRocketUiScriptSurfaceUpdatedRows() {
        return allEvidenceRows("librocket_ui_script_surface_updated_rows_v0_64");
    }

    public static List<MappingEvidenceRow> allLibRocketUiScriptSurfaceFlowMap() {
        return allEvidenceRows("librocket_ui_script_surface_flow_map_v0_64");
    }

    public static List<MappingEvidenceRow> allLibRocketUiScriptSurfaceSkippedRows() {
        return allEvidenceRows("librocket_ui_script_surface_skipped_rows_v0_64");
    }

    public static List<MappingEvidenceRow> allLibRocketUiScriptSurfacePartialCoverageRows() {
        return allEvidenceRows("librocket_ui_script_surface_partial_coverage_after_v0_64");
    }

    public static List<MappingEvidenceRow> allNetworkHandshakeSyncRows() {
        return allEvidenceRows("network_handshake_sync_added_rows_v0_65");
    }

    public static List<MappingEvidenceRow> allNetworkHandshakeSyncUpdatedRows() {
        return allEvidenceRows("network_handshake_sync_updated_rows_v0_65");
    }

    public static List<MappingEvidenceRow> allNetworkHandshakeSyncFlowMap() {
        return allEvidenceRows("network_handshake_sync_flow_map_v0_65");
    }

    public static List<MappingEvidenceRow> allNetworkHandshakeSyncSkippedRows() {
        return allEvidenceRows("network_handshake_sync_skipped_rows_v0_65");
    }

    public static List<MappingEvidenceRow> allNetworkHandshakeSyncCoreCoverageRows() {
        return allEvidenceRows("network_handshake_sync_core_coverage_after_v0_65");
    }

    public static List<MappingEvidenceRow> allNetworkSyncDesyncRows() {
        return allEvidenceRows("network_sync_desync_added_rows_v0_66");
    }

    public static List<MappingEvidenceRow> allNetworkSyncDesyncUpdatedRows() {
        return allEvidenceRows("network_sync_desync_updated_rows_v0_66");
    }

    public static List<MappingEvidenceRow> allNetworkSyncDesyncFlowMap() {
        return allEvidenceRows("network_sync_desync_flow_map_v0_66");
    }

    public static List<MappingEvidenceRow> allNetworkSyncDesyncSkippedRows() {
        return allEvidenceRows("network_sync_desync_skipped_rows_v0_66");
    }

    public static List<MappingEvidenceRow> allNetworkSyncDesyncCoreCoverageRows() {
        return allEvidenceRows("network_sync_desync_core_coverage_after_v0_66");
    }

    public static List<MappingEvidenceRow> allNetworkLobbyChatCommandRows() {
        return allEvidenceRows("network_lobby_chat_command_added_rows_v0_67");
    }

    public static List<MappingEvidenceRow> allNetworkLobbyChatCommandFlowMap() {
        return allEvidenceRows("network_lobby_chat_command_flow_map_v0_67");
    }

    public static List<MappingEvidenceRow> allNetworkLobbyChatCommandCoreCoverageRows() {
        return allEvidenceRows("network_lobby_chat_command_core_coverage_after_v0_67");
    }

    public static List<MappingEvidenceRow> allNetworkDeepPacketBranchRows() {
        return allEvidenceRows("network_deep_packet_branch_added_rows_v0_68");
    }

    public static List<MappingEvidenceRow> allNetworkDeepPacketBranchFlowMap() {
        return allEvidenceRows("network_deep_packet_branch_flow_map_v0_68");
    }

    public static List<MappingEvidenceRow> allNetworkDeepPacketBranchSkippedRows() {
        return allEvidenceRows("network_deep_packet_branch_skipped_existing_rows_v0_68");
    }

    public static List<MappingEvidenceRow> allNetworkDeepPacketBranchPartialCoverageRows() {
        return allEvidenceRows("network_deep_packet_branch_partial_coverage_after_v0_68");
    }

    public static List<MappingEvidenceRow> allRenderImageTextureLifecycleRows() {
        return allEvidenceRows("render_image_texture_lifecycle_added_rows_v0_69");
    }

    public static List<MappingEvidenceRow> allRenderImageTextureLifecycleUpdatedRows() {
        return allEvidenceRows("render_image_texture_lifecycle_updated_rows_v0_69");
    }

    public static List<MappingEvidenceRow> allRenderImageTextureLifecycleFlowMap() {
        return allEvidenceRows("render_image_texture_lifecycle_flow_map_v0_69");
    }

    public static List<MappingEvidenceRow> allRenderImageTextureLifecycleSkippedRows() {
        return allEvidenceRows("render_image_texture_lifecycle_skipped_rows_v0_69");
    }

    public static List<MappingEvidenceRow> allRenderImageTextureLifecyclePartialCoverageRows() {
        return allEvidenceRows("render_image_texture_family_partial_coverage_after_v0_69");
    }

    public static List<MappingEvidenceRow> allHudCommandInterfaceRows() {
        return allEvidenceRows("hud_command_interface_added_rows_v0_70");
    }

    public static List<MappingEvidenceRow> allHudCommandInterfaceUpdatedRows() {
        return allEvidenceRows("hud_command_interface_updated_rows_v0_70");
    }

    public static List<MappingEvidenceRow> allHudCommandInterfaceFlowMap() {
        return allEvidenceRows("hud_command_interface_flow_map_v0_70");
    }

    public static List<MappingEvidenceRow> allHudCommandInterfaceSkippedRows() {
        return allEvidenceRows("hud_command_interface_skipped_rows_v0_70");
    }

    public static List<MappingEvidenceRow> allHudCommandInterfacePartialCoverageRows() {
        return allEvidenceRows("hud_command_interface_partial_coverage_after_v0_70");
    }

    public static List<MappingEvidenceRow> allCoreDebugStatsRows() {
        return allEvidenceRows("core_debug_stats_added_rows_v0_71");
    }

    public static List<MappingEvidenceRow> allCoreDebugStatsUpdatedRows() {
        return allEvidenceRows("core_debug_stats_updated_rows_v0_71");
    }

    public static List<MappingEvidenceRow> allCoreDebugStatsFlowMap() {
        return allEvidenceRows("core_debug_stats_flow_map_v0_71");
    }

    public static List<MappingEvidenceRow> allCoreDebugStatsSkippedRows() {
        return allEvidenceRows("core_debug_stats_skipped_rows_v0_71");
    }

    public static List<MappingEvidenceRow> allCoreDebugStatsPartialCoverageRows() {
        return allEvidenceRows("core_debug_stats_partial_coverage_after_v0_71");
    }

    public static List<MappingEvidenceRow> allSlickGraphicsBackendRows() {
        return allEvidenceRows("slick_graphics_backend_added_rows_v0_72");
    }

    public static List<MappingEvidenceRow> allSlickGraphicsBackendUpdatedRows() {
        return allEvidenceRows("slick_graphics_backend_updated_rows_v0_72");
    }

    public static List<MappingEvidenceRow> allSlickGraphicsBackendFlowMap() {
        return allEvidenceRows("slick_graphics_backend_flow_map_v0_72");
    }

    public static List<MappingEvidenceRow> allSlickGraphicsBackendSkippedRows() {
        return allEvidenceRows("slick_graphics_backend_skipped_rows_v0_72");
    }

    public static List<MappingEvidenceRow> allSlickGraphicsBackendPartialCoverageRows() {
        return allEvidenceRows("slick_graphics_backend_partial_coverage_after_v0_72");
    }

    public static List<MappingEvidenceRow> allCommonUtilsRows() {
        return allEvidenceRows("common_utils_added_rows_v0_73");
    }

    public static List<MappingEvidenceRow> allCommonUtilsUpdatedRows() {
        return allEvidenceRows("common_utils_updated_rows_v0_73");
    }

    public static List<MappingEvidenceRow> allCommonUtilsFlowMap() {
        return allEvidenceRows("common_utils_flow_map_v0_73");
    }

    public static List<MappingEvidenceRow> allCommonUtilsSkippedRows() {
        return allEvidenceRows("common_utils_skipped_rows_v0_73");
    }

    public static List<MappingEvidenceRow> allUnitActionCommandResidualRows() {
        return allEvidenceRows("unit_action_command_residual_added_rows_v0_74");
    }

    public static List<MappingEvidenceRow> allUnitActionCommandResidualUpdatedRows() {
        return allEvidenceRows("unit_action_command_residual_updated_rows_v0_74");
    }

    public static List<MappingEvidenceRow> allUnitActionCommandResidualFlowMap() {
        return allEvidenceRows("unit_action_command_residual_flow_map_v0_74");
    }

    public static List<MappingEvidenceRow> allUnitActionCommandResidualSkippedRows() {
        return allEvidenceRows("unit_action_command_residual_skipped_rows_v0_74");
    }

    public static List<MappingEvidenceRow> allUnitActionCommandResidualPartialCoverageRows() {
        return allEvidenceRows("unit_action_command_residual_partial_coverage_after_v0_74");
    }

    public static List<MappingEvidenceRow> allSaveReplayVersionedDataRows() {
        return allEvidenceRows("save_replay_versioned_data_added_rows_v0_75");
    }

    public static List<MappingEvidenceRow> allSaveReplayVersionedDataUpdatedRows() {
        return allEvidenceRows("save_replay_versioned_data_updated_rows_v0_75");
    }

    public static List<MappingEvidenceRow> allSaveReplayVersionedDataFlowMap() {
        return allEvidenceRows("save_replay_versioned_data_flow_map_v0_75");
    }

    public static List<MappingEvidenceRow> allSaveReplayVersionedDataSkippedRows() {
        return allEvidenceRows("save_replay_versioned_data_skipped_rows_v0_75");
    }

    public static List<MappingEvidenceRow> allSaveReplayVersionedDataPartialCoverageRows() {
        return allEvidenceRows("save_replay_versioned_data_partial_coverage_after_v0_75");
    }

    public static List<MappingEvidenceRow> allModCustomPipelineRows() {
        return allEvidenceRows("mod_custom_pipeline_added_rows_v0_76");
    }

    public static List<MappingEvidenceRow> allModCustomPipelineUpdatedRows() {
        return allEvidenceRows("mod_custom_pipeline_updated_rows_v0_76");
    }

    public static List<MappingEvidenceRow> allModCustomPipelineFlowMap() {
        return allEvidenceRows("mod_custom_pipeline_flow_map_v0_76");
    }

    public static List<MappingEvidenceRow> allModCustomPipelineSkippedRows() {
        return allEvidenceRows("mod_custom_pipeline_skipped_rows_v0_76");
    }

    public static List<MappingEvidenceRow> allModCustomPipelinePartialCoverageRows() {
        return allEvidenceRows("mod_custom_pipeline_partial_coverage_after_v0_76");
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

    public static List<MappingEvidenceRow> findRuntimeRepairReclaimRows(String text) {
        return findByText(Holder.RUNTIME_REPAIR_RECLAIM_ROWS, text);
    }

    public static List<MappingEvidenceRow> findRuntimeTransportAttachmentRows(String text) {
        return findByText(Holder.RUNTIME_TRANSPORT_ATTACHMENT_ROWS, text);
    }

    public static List<MappingEvidenceRow> findAttachmentSlotSemanticHotfixRows(String text) {
        return findByText(Holder.ATTACHMENT_SLOT_SEMANTIC_HOTFIX_ROWS, text);
    }

    public static List<MappingEvidenceRow> findRuntimeActionCommandRows(String text) {
        return findByText(Holder.RUNTIME_ACTION_COMMAND_ROWS, text);
    }

    public static List<MappingEvidenceRow> findRuntimeResourceEconomyRows(String text) {
        return findByText(Holder.RUNTIME_RESOURCE_ECONOMY_ROWS, text);
    }

    public static List<MappingEvidenceRow> findRuntimeResourceEconomyUpdatedRows(String text) {
        return findByText(Holder.RUNTIME_RESOURCE_ECONOMY_UPDATED_ROWS, text);
    }

    public static List<MappingEvidenceRow> findRuntimeCommandIssueRows(String text) {
        return findByText(Holder.RUNTIME_COMMAND_ISSUE_ROWS, text);
    }

    public static List<MappingEvidenceRow> findRuntimeCommandIssueUpdatedRows(String text) {
        return findByText(Holder.RUNTIME_COMMAND_ISSUE_UPDATED_ROWS, text);
    }

    public static List<MappingEvidenceRow> findRuntimeTeamStatsRows(String text) {
        return findByText(Holder.RUNTIME_TEAM_STATS_ROWS, text);
    }

    public static List<MappingEvidenceRow> findRuntimeTeamStatsHotfixRows(String text) {
        return findByText(Holder.RUNTIME_TEAM_STATS_HOTFIX_ROWS, text);
    }

    public static List<MappingEvidenceRow> findRuntimeVisibilitySpatialRows(String text) {
        return findByText(Holder.RUNTIME_VISIBILITY_SPATIAL_ROWS, text);
    }

    public static List<MappingEvidenceRow> findRuntimeVisibilitySpatialUpdatedRows(String text) {
        return findByText(Holder.RUNTIME_VISIBILITY_SPATIAL_UPDATED_ROWS, text);
    }

    public static List<MappingEvidenceRow> findRuntimeReplayChecksumRows(String text) {
        return findByText(Holder.RUNTIME_REPLAY_CHECKSUM_ROWS, text);
    }

    public static List<MappingEvidenceRow> findRuntimeReplayChecksumUpdatedRows(String text) {
        return findByText(Holder.RUNTIME_REPLAY_CHECKSUM_UPDATED_ROWS, text);
    }

    public static List<MappingEvidenceRow> findNetworkChecksumBucketEvidence(String text) {
        return findByText(Holder.NETWORK_CHECKSUM_BUCKET_EVIDENCE, text);
    }

    public static List<MappingEvidenceRow> findInputActionNamingHotfixRows(String text) {
        return findByText(allInputActionNamingHotfixRows(), text);
    }

    public static List<MappingEvidenceRow> findInputActionNamingHotfixUpdatedRows(String text) {
        return findByText(allInputActionNamingHotfixUpdatedRows(), text);
    }

    public static List<MappingEvidenceRow> findLibRocketUiScriptSurfaceRows(String text) {
        return findByText(allLibRocketUiScriptSurfaceRows(), text);
    }

    public static List<MappingEvidenceRow> findLibRocketUiScriptSurfaceUpdatedRows(String text) {
        return findByText(allLibRocketUiScriptSurfaceUpdatedRows(), text);
    }

    public static List<MappingEvidenceRow> findCustomLogicStatBehaviorRows(String text) {
        return findByText(Holder.CUSTOM_LOGIC_STAT_BEHAVIOR_ROWS, text);
    }

    public static List<MappingEvidenceRow> findCustomLogicGeometryRows(String text) {
        return findByText(Holder.CUSTOM_LOGIC_GEOMETRY_ROWS, text);
    }

    public static List<MappingEvidenceRow> findCustomMutableStatWriterRows(String text) {
        return findByText(Holder.CUSTOM_MUTABLE_STAT_WRITER_ROWS, text);
    }

    public static List<MappingEvidenceRow> findCustomMovementMicroBehaviorRows(String text) {
        return findByText(Holder.CUSTOM_MOVEMENT_MICRO_BEHAVIOR_ROWS, text);
    }

    public static List<MappingEvidenceRow> findMapTerrainTilesetMergeRows(String text) {
        return findByText(Holder.MAP_TERRAIN_TILESET_MERGE_ROWS, text);
    }

    public static List<MappingEvidenceRow> findMapTerrainTilesetMergeUpdatedRows(String text) {
        return findByText(Holder.MAP_TERRAIN_TILESET_MERGE_UPDATED_ROWS, text);
    }

    public static List<MappingEvidenceRow> findTileAtlasRenderCacheRows(String text) {
        return findByText(Holder.TILE_ATLAS_RENDER_CACHE_ROWS, text);
    }

    public static List<MappingEvidenceRow> findEffectRuntimeRows(String text) {
        return findByText(Holder.EFFECT_RUNTIME_ROWS, text);
    }

    public static List<MappingEvidenceRow> findEffectEngineRows(String text) {
        return findByText(Holder.EFFECT_ENGINE_ROWS, text);
    }

    public static List<MappingEvidenceRow> findEffectInstanceRows(String text) {
        return findByText(Holder.EFFECT_INSTANCE_ROWS, text);
    }

    public static List<MappingEvidenceRow> findEffectEnumRows(String text) {
        return findByText(Holder.EFFECT_ENUM_ROWS, text);
    }

    public static List<MappingEvidenceRow> findMissionTriggerMapScriptRows(String text) {
        return findByText(Holder.MISSION_TRIGGER_MAP_SCRIPT_ROWS, text);
    }

    public static List<MappingEvidenceRow> findMissionTriggerMapScriptUpdatedRows(String text) {
        return findByText(Holder.MISSION_TRIGGER_MAP_SCRIPT_UPDATED_ROWS, text);
    }

    public static List<MappingEvidenceRow> findMissionTriggerSemanticHotfixRows(String text) {
        return findByText(Holder.MISSION_TRIGGER_SEMANTIC_HOTFIX_ROWS, text);
    }

    public static List<MappingEvidenceRow> findMissionTriggerTypeAnonymousRows(String text) {
        return findByText(Holder.MISSION_TRIGGER_TYPE_ANONYMOUS_ROWS, text);
    }

    public static List<MappingEvidenceRow> findRenderCanvasCommandRows(String text) {
        return findByText(Holder.RENDER_CANVAS_COMMAND_ROWS, text);
    }

    public static List<MappingEvidenceRow> findCanvasOperationEnumRows(String text) {
        return findByText(Holder.CANVAS_OPERATION_ENUM_ROWS, text);
    }

    public static List<MappingEvidenceRow> findCanvasDrawTargetCommandRows(String text) {
        return findByText(Holder.CANVAS_DRAWTARGET_COMMAND_ROWS, text);
    }

    public static List<MappingEvidenceRow> findShaderProgramRows(String text) {
        return findByText(Holder.SHADER_PROGRAM_ROWS, text);
    }

    public static List<MappingEvidenceRow> findUiMinimapCommandRows(String text) {
        return findByText(Holder.UI_MINIMAP_COMMAND_ROWS, text);
    }

    public static List<MappingEvidenceRow> findUiMinimapCommandBranchConflicts(String text) {
        return findByText(Holder.UI_MINIMAP_COMMAND_BRANCH_CONFLICTS, text);
    }

    public static List<MappingEvidenceRow> findEvidenceRows(String resourceId, String text) {
        return findByText(allEvidenceRows(resourceId), text);
    }

    public static List<MappingEvidenceRow> findAudioBackendRows(String text) {
        return findByText(allAudioBackendRows(), text);
    }

    public static List<MappingEvidenceRow> findAudioFactoryBridgeRows(String text) {
        return findByText(allAudioFactoryBridgeRows(), text);
    }

    public static List<MappingEvidenceRow> findAudioOpenAlRows(String text) {
        return findByText(allAudioOpenAlRows(), text);
    }

    public static List<MappingEvidenceRow> findRenderImageTextureLifecycleRows(String text) {
        return findByText(allRenderImageTextureLifecycleRows(), text);
    }

    public static List<MappingEvidenceRow> findHudCommandInterfaceRows(String text) {
        return findByText(allHudCommandInterfaceRows(), text);
    }

    public static List<MappingEvidenceRow> findCoreDebugStatsRows(String text) {
        return findByText(allCoreDebugStatsRows(), text);
    }

    public static List<MappingEvidenceRow> findSlickGraphicsBackendRows(String text) {
        return findByText(allSlickGraphicsBackendRows(), text);
    }

    public static List<MappingEvidenceRow> findCommonUtilsRows(String text) {
        return findByText(allCommonUtilsRows(), text);
    }

    public static List<MappingEvidenceRow> findUnitActionCommandResidualRows(String text) {
        return findByText(allUnitActionCommandResidualRows(), text);
    }

    public static List<MappingEvidenceRow> findSaveReplayVersionedDataRows(String text) {
        return findByText(allSaveReplayVersionedDataRows(), text);
    }

    public static List<MappingEvidenceRow> findSaveReplayVersionedDataUpdatedRows(String text) {
        return findByText(allSaveReplayVersionedDataUpdatedRows(), text);
    }

    public static List<MappingEvidenceRow> findModCustomPipelineRows(String text) {
        return findByText(allModCustomPipelineRows(), text);
    }

    public static List<MappingEvidenceRow> findModCustomPipelineUpdatedRows(String text) {
        return findByText(allModCustomPipelineUpdatedRows(), text);
    }

    public static List<MappingEvidenceRow> findAudioUtilityRows(String text) {
        return findByText(allAudioUtilityRows(), text);
    }

    public static List<MappingEvidenceRow> findAudioFamilyCompletionRows(String text) {
        return findByText(allAudioFamilyCompletionRows(), text);
    }

    public static List<MappingEvidenceRow> findInputKeybindingRows(String text) {
        return findByText(allInputKeybindingRows(), text);
    }

    public static List<MappingEvidenceRow> findNetworkHandshakeSyncRows(String text) {
        return findByText(allNetworkHandshakeSyncRows(), text);
    }

    public static List<MappingEvidenceRow> findNetworkSyncDesyncRows(String text) {
        return findByText(allNetworkSyncDesyncRows(), text);
    }

    public static List<MappingEvidenceRow> findNetworkLobbyChatCommandRows(String text) {
        return findByText(allNetworkLobbyChatCommandRows(), text);
    }

    public static List<MappingEvidenceRow> findNetworkDeepPacketBranchRows(String text) {
        return findByText(allNetworkDeepPacketBranchRows(), text);
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
                    first(row, "kind", "new_kind", "old_kind", "change"),
                    first(row, "owner_official", "new_owner_official", "old_owner_official", "owner"),
                    first(row, "descriptor", "new_descriptor", "old_descriptor"),
                    first(row, "official_name", "new_official_name", "old_official_name", "member", "target"),
                    first(row, "new_intermediary", "new_intermediary_name", "intermediary_name",
                            "old_intermediary", "old_intermediary_name"),
                    first(row, "new_named_name", "new_named", "named_name", "mapped_name", "old_named_name"),
                    first(row, "new_source", "source", "mapping_source"),
                    first(row, "category", "runtime_stage", "stage", "family_named", "phase", "family"),
                    first(row, "confidence"),
                    first(row, "new_evidence", "evidence", "semantics", "contract", "effect"),
                    first(row, "new_notes", "notes", "reason")));
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
        result.put("runtime_repair_reclaim_rows", Holder.RUNTIME_REPAIR_RECLAIM_ROWS);
        result.put("runtime_repair_reclaim_flow_map", Holder.RUNTIME_REPAIR_RECLAIM_FLOW_MAP);
        result.put("runtime_repair_reclaim_family_coverage", Holder.RUNTIME_REPAIR_RECLAIM_FAMILY_COVERAGE);
        result.put("runtime_transport_attachment_rows", Holder.RUNTIME_TRANSPORT_ATTACHMENT_ROWS);
        result.put("runtime_transport_attachment_flow_map", Holder.RUNTIME_TRANSPORT_ATTACHMENT_FLOW_MAP);
        result.put("runtime_transport_attachment_family_coverage", Holder.RUNTIME_TRANSPORT_ATTACHMENT_FAMILY_COVERAGE);
        result.put("attachment_slot_semantic_hotfix_rows", Holder.ATTACHMENT_SLOT_SEMANTIC_HOTFIX_ROWS);
        result.put("runtime_action_command_rows", Holder.RUNTIME_ACTION_COMMAND_ROWS);
        result.put("runtime_action_command_flow_map", Holder.RUNTIME_ACTION_COMMAND_FLOW_MAP);
        result.put("runtime_action_command_family_coverage", Holder.RUNTIME_ACTION_COMMAND_FAMILY_COVERAGE);
        result.put("runtime_resource_economy_rows", Holder.RUNTIME_RESOURCE_ECONOMY_ROWS);
        result.put("runtime_resource_economy_updated_rows", Holder.RUNTIME_RESOURCE_ECONOMY_UPDATED_ROWS);
        result.put("runtime_resource_economy_flow_map", Holder.RUNTIME_RESOURCE_ECONOMY_FLOW_MAP);
        result.put("runtime_resource_economy_family_coverage", Holder.RUNTIME_RESOURCE_ECONOMY_FAMILY_COVERAGE);
        result.put("runtime_command_issue_rows", Holder.RUNTIME_COMMAND_ISSUE_ROWS);
        result.put("runtime_command_issue_updated_rows", Holder.RUNTIME_COMMAND_ISSUE_UPDATED_ROWS);
        result.put("runtime_command_issue_evidence_rows", Holder.RUNTIME_COMMAND_ISSUE_EVIDENCE_ROWS);
        result.put("runtime_command_issue_flow_map", Holder.RUNTIME_COMMAND_ISSUE_FLOW_MAP);
        result.put("runtime_command_issue_skipped_branch_rollbacks", Holder.RUNTIME_COMMAND_ISSUE_SKIPPED_BRANCH_ROLLBACKS);
        result.put("runtime_team_stats_rows", Holder.RUNTIME_TEAM_STATS_ROWS);
        result.put("runtime_team_stats_hotfix_rows", Holder.RUNTIME_TEAM_STATS_HOTFIX_ROWS);
        result.put("runtime_team_stats_flow_map", Holder.RUNTIME_TEAM_STATS_FLOW_MAP);
        result.put("runtime_visibility_spatial_rows", Holder.RUNTIME_VISIBILITY_SPATIAL_ROWS);
        result.put("runtime_visibility_spatial_updated_rows", Holder.RUNTIME_VISIBILITY_SPATIAL_UPDATED_ROWS);
        result.put("runtime_visibility_spatial_flow_map", Holder.RUNTIME_VISIBILITY_SPATIAL_FLOW_MAP);
        result.put("runtime_visibility_spatial_branch_update_review",
                Holder.RUNTIME_VISIBILITY_SPATIAL_BRANCH_UPDATE_REVIEW);
        result.put("runtime_visibility_spatial_skipped_branch_rollbacks",
                Holder.RUNTIME_VISIBILITY_SPATIAL_SKIPPED_BRANCH_ROLLBACKS);
        result.put("runtime_replay_checksum_rows", Holder.RUNTIME_REPLAY_CHECKSUM_ROWS);
        result.put("runtime_replay_checksum_updated_rows", Holder.RUNTIME_REPLAY_CHECKSUM_UPDATED_ROWS);
        result.put("runtime_replay_checksum_flow_map", Holder.RUNTIME_REPLAY_CHECKSUM_FLOW_MAP);
        result.put("network_checksum_bucket_evidence", Holder.NETWORK_CHECKSUM_BUCKET_EVIDENCE);
        result.put("custom_logic_stat_behavior_rows", Holder.CUSTOM_LOGIC_STAT_BEHAVIOR_ROWS);
        result.put("custom_logic_stat_behavior_flow_map", Holder.CUSTOM_LOGIC_STAT_BEHAVIOR_FLOW_MAP);
        result.put("custom_logic_stat_behavior_coverage", Holder.CUSTOM_LOGIC_STAT_BEHAVIOR_COVERAGE);
        result.put("custom_logic_geometry_rows", Holder.CUSTOM_LOGIC_GEOMETRY_ROWS);
        result.put("custom_mutable_stat_writer_rows", Holder.CUSTOM_MUTABLE_STAT_WRITER_ROWS);
        result.put("custom_movement_micro_behavior_rows", Holder.CUSTOM_MOVEMENT_MICRO_BEHAVIOR_ROWS);
        result.put("map_terrain_tileset_merge_rows", Holder.MAP_TERRAIN_TILESET_MERGE_ROWS);
        result.put("map_terrain_tileset_merge_updated_rows", Holder.MAP_TERRAIN_TILESET_MERGE_UPDATED_ROWS);
        result.put("map_terrain_tileset_flow_map", Holder.MAP_TERRAIN_TILESET_FLOW_MAP);
        result.put("map_terrain_tileset_branch_skipped_rows", Holder.MAP_TERRAIN_TILESET_BRANCH_SKIPPED_ROWS);
        result.put("tile_atlas_render_cache_rows", Holder.TILE_ATLAS_RENDER_CACHE_ROWS);
        result.put("effect_runtime_rows", Holder.EFFECT_RUNTIME_ROWS);
        result.put("effect_engine_rows", Holder.EFFECT_ENGINE_ROWS);
        result.put("effect_instance_rows", Holder.EFFECT_INSTANCE_ROWS);
        result.put("effect_enum_rows", Holder.EFFECT_ENUM_ROWS);
        result.put("effect_runtime_flow_map", Holder.EFFECT_RUNTIME_FLOW_MAP);
        result.put("mission_trigger_map_script_rows", Holder.MISSION_TRIGGER_MAP_SCRIPT_ROWS);
        result.put("mission_trigger_map_script_updated_rows", Holder.MISSION_TRIGGER_MAP_SCRIPT_UPDATED_ROWS);
        result.put("mission_trigger_map_script_flow_map", Holder.MISSION_TRIGGER_MAP_SCRIPT_FLOW_MAP);
        result.put("mission_trigger_semantic_hotfix_rows", Holder.MISSION_TRIGGER_SEMANTIC_HOTFIX_ROWS);
        result.put("mission_trigger_type_anonymous_rows", Holder.MISSION_TRIGGER_TYPE_ANONYMOUS_ROWS);
        result.put("render_canvas_command_rows", Holder.RENDER_CANVAS_COMMAND_ROWS);
        result.put("render_canvas_command_updated_rows", Holder.RENDER_CANVAS_COMMAND_UPDATED_ROWS);
        result.put("render_canvas_command_skipped_rows", Holder.RENDER_CANVAS_COMMAND_SKIPPED_ROWS);
        result.put("render_canvas_command_flow_map", Holder.RENDER_CANVAS_COMMAND_FLOW_MAP);
        result.put("render_canvas_command_family_coverage", Holder.RENDER_CANVAS_COMMAND_FAMILY_COVERAGE);
        result.put("canvas_operation_enum_rows", Holder.CANVAS_OPERATION_ENUM_ROWS);
        result.put("canvas_drawtarget_command_rows", Holder.CANVAS_DRAWTARGET_COMMAND_ROWS);
        result.put("shader_program_rows", Holder.SHADER_PROGRAM_ROWS);
        result.put("ui_minimap_command_rows", Holder.UI_MINIMAP_COMMAND_ROWS);
        result.put("ui_minimap_command_flow_map", Holder.UI_MINIMAP_COMMAND_FLOW_MAP);
        result.put("ui_minimap_command_family_coverage", Holder.UI_MINIMAP_COMMAND_FAMILY_COVERAGE);
        result.put("ui_minimap_command_branch_conflicts", Holder.UI_MINIMAP_COMMAND_BRANCH_CONFLICTS);
        result.put("render_gl_backend_rows", Holder.RENDER_GL_BACKEND_ROWS);
        result.put("render_gl_backend_added_rows_v0_58", Holder.RENDER_GL_BACKEND_ROWS);
        result.put("render_gl_backend_canvas_shader_rows", Holder.RENDER_GL_BACKEND_CANVAS_SHADER_ROWS);
        result.put("render_gl_backend_canvas_shader_rows_v0_58", Holder.RENDER_GL_BACKEND_CANVAS_SHADER_ROWS);
        result.put("render_gl_backend_flow_map", Holder.RENDER_GL_BACKEND_FLOW_MAP);
        result.put("render_gl_backend_flow_map_v0_58", Holder.RENDER_GL_BACKEND_FLOW_MAP);
        result.put("render_gl_backend_skipped_rows", Holder.RENDER_GL_BACKEND_SKIPPED_ROWS);
        result.put("render_gl_backend_skipped_rows_v0_58", Holder.RENDER_GL_BACKEND_SKIPPED_ROWS);
        result.put("render_gl_backend_texture_rows", Holder.RENDER_GL_BACKEND_TEXTURE_ROWS);
        result.put("render_gl_backend_texture_rows_v0_58", Holder.RENDER_GL_BACKEND_TEXTURE_ROWS);
        result.put("render_gl_text_rows", Holder.RENDER_GL_TEXT_ROWS);
        result.put("render_gl_text_rows_v0_58", Holder.RENDER_GL_TEXT_ROWS);
        result.put("filesystem_backend_rows", Holder.FILESYSTEM_BACKEND_ROWS);
        result.put("filesystem_backend_added_rows_v0_59", Holder.FILESYSTEM_BACKEND_ROWS);
        result.put("filesystem_backend_updated_rows", Holder.FILESYSTEM_BACKEND_UPDATED_ROWS);
        result.put("filesystem_backend_updated_rows_v0_59", Holder.FILESYSTEM_BACKEND_UPDATED_ROWS);
        result.put("filesystem_backend_skipped_rows", Holder.FILESYSTEM_BACKEND_SKIPPED_ROWS);
        result.put("filesystem_backend_skipped_rows_v0_59", Holder.FILESYSTEM_BACKEND_SKIPPED_ROWS);
        result.put("filesystem_backend_flow_map", Holder.FILESYSTEM_BACKEND_FLOW_MAP);
        result.put("filesystem_backend_flow_map_v0_59", Holder.FILESYSTEM_BACKEND_FLOW_MAP);
        result.put("filesystem_backend_coverage", Holder.FILESYSTEM_BACKEND_COVERAGE);
        result.put("filesystem_backend_coverage_v0_59", Holder.FILESYSTEM_BACKEND_COVERAGE);
        addManifestEvidenceRows(result);
        return Collections.unmodifiableMap(result);
    }

    private static void addManifestEvidenceRows(Map<String, List<MappingEvidenceRow>> result) {
        for (EvidenceManifestRow row : Holder.EVIDENCE_MANIFEST_ROWS) {
            String resourceId = normalizeResourceId(row.resourceId());
            if (resourceId.isEmpty() || row.fileName().isEmpty() || result.containsKey(resourceId)) {
                continue;
            }
            result.put(resourceId, loadRows("/rustedfabricapi/mapping/" + row.fileName()));
        }
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

    private static List<EvidenceManifestRow> loadEvidenceManifestRows(String resource) {
        List<Map<String, String>> rows = loadCsv(resource);
        List<EvidenceManifestRow> result = new ArrayList<EvidenceManifestRow>();
        for (Map<String, String> row : rows) {
            result.add(new EvidenceManifestRow(
                    row.get("resource_id"),
                    row.get("file_name"),
                    row.get("version"),
                    row.get("category")));
        }
        return Collections.unmodifiableList(result);
    }

    private static List<Map<String, String>> loadCsv(String resource) {
        try {
            InputStream inputStream = openEvidenceStream(resource);
            if (inputStream == null) {
                return Collections.emptyList();
            }

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

    private static InputStream openEvidenceStream(String resource) throws IOException {
        InputStream inputStream = MappingEvidenceDiagnostics.class.getResourceAsStream(resource);
        if (inputStream != null) {
            return inputStream;
        }

        File developmentFile = new File(DEVELOPMENT_EVIDENCE_DIRECTORY, fileName(resource));
        if (developmentFile.isFile()) {
            return new FileInputStream(developmentFile);
        }
        return null;
    }

    private static String fileName(String resource) {
        int slash = resource.lastIndexOf('/');
        return slash >= 0 ? resource.substring(slash + 1) : resource;
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
        private static final List<EvidenceManifestRow> EVIDENCE_MANIFEST_ROWS =
                loadEvidenceManifestRows(MAPPING_EVIDENCE_MANIFEST_RESOURCE);
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
        private static final List<MappingEvidenceRow> RUNTIME_REPAIR_RECLAIM_ROWS =
                loadRows(RUNTIME_REPAIR_RECLAIM_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_REPAIR_RECLAIM_FLOW_MAP =
                loadRows(RUNTIME_REPAIR_RECLAIM_FLOW_MAP_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_REPAIR_RECLAIM_FAMILY_COVERAGE =
                loadRows(RUNTIME_REPAIR_RECLAIM_FAMILY_COVERAGE_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_TRANSPORT_ATTACHMENT_ROWS =
                loadRows(RUNTIME_TRANSPORT_ATTACHMENT_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_TRANSPORT_ATTACHMENT_FLOW_MAP =
                loadRows(RUNTIME_TRANSPORT_ATTACHMENT_FLOW_MAP_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_TRANSPORT_ATTACHMENT_FAMILY_COVERAGE =
                loadRows(RUNTIME_TRANSPORT_ATTACHMENT_FAMILY_COVERAGE_RESOURCE);
        private static final List<MappingEvidenceRow> ATTACHMENT_SLOT_SEMANTIC_HOTFIX_ROWS =
                loadRows(ATTACHMENT_SLOT_SEMANTIC_HOTFIX_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_ACTION_COMMAND_ROWS =
                loadRows(RUNTIME_ACTION_COMMAND_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_ACTION_COMMAND_FLOW_MAP =
                loadRows(RUNTIME_ACTION_COMMAND_FLOW_MAP_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_ACTION_COMMAND_FAMILY_COVERAGE =
                loadRows(RUNTIME_ACTION_COMMAND_FAMILY_COVERAGE_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_RESOURCE_ECONOMY_ROWS =
                loadRows(RUNTIME_RESOURCE_ECONOMY_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_RESOURCE_ECONOMY_UPDATED_ROWS =
                loadRows(RUNTIME_RESOURCE_ECONOMY_UPDATED_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_RESOURCE_ECONOMY_FLOW_MAP =
                loadRows(RUNTIME_RESOURCE_ECONOMY_FLOW_MAP_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_RESOURCE_ECONOMY_FAMILY_COVERAGE =
                loadRows(RUNTIME_RESOURCE_ECONOMY_FAMILY_COVERAGE_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_COMMAND_ISSUE_ROWS =
                loadRows(RUNTIME_COMMAND_ISSUE_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_COMMAND_ISSUE_UPDATED_ROWS =
                loadRows(RUNTIME_COMMAND_ISSUE_UPDATED_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_COMMAND_ISSUE_EVIDENCE_ROWS =
                loadRows(RUNTIME_COMMAND_ISSUE_EVIDENCE_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_COMMAND_ISSUE_FLOW_MAP =
                loadRows(RUNTIME_COMMAND_ISSUE_FLOW_MAP_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_COMMAND_ISSUE_SKIPPED_BRANCH_ROLLBACKS =
                loadRows(RUNTIME_COMMAND_ISSUE_SKIPPED_BRANCH_ROLLBACKS_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_TEAM_STATS_ROWS =
                loadRows(RUNTIME_TEAM_STATS_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_TEAM_STATS_HOTFIX_ROWS =
                loadRows(RUNTIME_TEAM_STATS_HOTFIX_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_TEAM_STATS_FLOW_MAP =
                loadRows(RUNTIME_TEAM_STATS_FLOW_MAP_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_VISIBILITY_SPATIAL_ROWS =
                loadRows(RUNTIME_VISIBILITY_SPATIAL_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_VISIBILITY_SPATIAL_UPDATED_ROWS =
                loadRows(RUNTIME_VISIBILITY_SPATIAL_UPDATED_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_VISIBILITY_SPATIAL_FLOW_MAP =
                loadRows(RUNTIME_VISIBILITY_SPATIAL_FLOW_MAP_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_VISIBILITY_SPATIAL_BRANCH_UPDATE_REVIEW =
                loadRows(RUNTIME_VISIBILITY_SPATIAL_BRANCH_UPDATE_REVIEW_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_VISIBILITY_SPATIAL_SKIPPED_BRANCH_ROLLBACKS =
                loadRows(RUNTIME_VISIBILITY_SPATIAL_SKIPPED_BRANCH_ROLLBACKS_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_REPLAY_CHECKSUM_ROWS =
                loadRows(RUNTIME_REPLAY_CHECKSUM_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_REPLAY_CHECKSUM_UPDATED_ROWS =
                loadRows(RUNTIME_REPLAY_CHECKSUM_UPDATED_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RUNTIME_REPLAY_CHECKSUM_FLOW_MAP =
                loadRows(RUNTIME_REPLAY_CHECKSUM_FLOW_MAP_RESOURCE);
        private static final List<MappingEvidenceRow> NETWORK_CHECKSUM_BUCKET_EVIDENCE =
                loadRows(NETWORK_CHECKSUM_BUCKET_EVIDENCE_RESOURCE);
        private static final List<MappingEvidenceRow> CUSTOM_LOGIC_STAT_BEHAVIOR_ROWS =
                loadRows(CUSTOM_LOGIC_STAT_BEHAVIOR_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> CUSTOM_LOGIC_STAT_BEHAVIOR_FLOW_MAP =
                loadRows(CUSTOM_LOGIC_STAT_BEHAVIOR_FLOW_MAP_RESOURCE);
        private static final List<MappingEvidenceRow> CUSTOM_LOGIC_STAT_BEHAVIOR_COVERAGE =
                loadRows(CUSTOM_LOGIC_STAT_BEHAVIOR_COVERAGE_RESOURCE);
        private static final List<MappingEvidenceRow> CUSTOM_LOGIC_GEOMETRY_ROWS =
                loadRows(CUSTOM_LOGIC_GEOMETRY_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> CUSTOM_MUTABLE_STAT_WRITER_ROWS =
                loadRows(CUSTOM_MUTABLE_STAT_WRITER_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> CUSTOM_MOVEMENT_MICRO_BEHAVIOR_ROWS =
                loadRows(CUSTOM_MOVEMENT_MICRO_BEHAVIOR_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> MAP_TERRAIN_TILESET_MERGE_ROWS =
                loadRows(MAP_TERRAIN_TILESET_MERGE_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> MAP_TERRAIN_TILESET_MERGE_UPDATED_ROWS =
                loadRows(MAP_TERRAIN_TILESET_MERGE_UPDATED_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> MAP_TERRAIN_TILESET_FLOW_MAP =
                loadRows(MAP_TERRAIN_TILESET_FLOW_MAP_RESOURCE);
        private static final List<MappingEvidenceRow> MAP_TERRAIN_TILESET_BRANCH_SKIPPED_ROWS =
                loadRows(MAP_TERRAIN_TILESET_BRANCH_SKIPPED_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> TILE_ATLAS_RENDER_CACHE_ROWS =
                loadRows(TILE_ATLAS_RENDER_CACHE_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> EFFECT_RUNTIME_ROWS =
                loadRows(EFFECT_RUNTIME_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> EFFECT_ENGINE_ROWS =
                loadRows(EFFECT_ENGINE_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> EFFECT_INSTANCE_ROWS =
                loadRows(EFFECT_INSTANCE_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> EFFECT_ENUM_ROWS =
                loadRows(EFFECT_ENUM_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> EFFECT_RUNTIME_FLOW_MAP =
                loadRows(EFFECT_RUNTIME_FLOW_MAP_RESOURCE);
        private static final List<MappingEvidenceRow> MISSION_TRIGGER_MAP_SCRIPT_ROWS =
                loadRows(MISSION_TRIGGER_MAP_SCRIPT_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> MISSION_TRIGGER_MAP_SCRIPT_UPDATED_ROWS =
                loadRows(MISSION_TRIGGER_MAP_SCRIPT_UPDATED_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> MISSION_TRIGGER_MAP_SCRIPT_FLOW_MAP =
                loadRows(MISSION_TRIGGER_MAP_SCRIPT_FLOW_MAP_RESOURCE);
        private static final List<MappingEvidenceRow> MISSION_TRIGGER_SEMANTIC_HOTFIX_ROWS =
                loadRows(MISSION_TRIGGER_SEMANTIC_HOTFIX_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> MISSION_TRIGGER_TYPE_ANONYMOUS_ROWS =
                loadRows(MISSION_TRIGGER_TYPE_ANONYMOUS_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RENDER_CANVAS_COMMAND_ROWS =
                loadRows(RENDER_CANVAS_COMMAND_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RENDER_CANVAS_COMMAND_UPDATED_ROWS =
                loadRows(RENDER_CANVAS_COMMAND_UPDATED_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RENDER_CANVAS_COMMAND_SKIPPED_ROWS =
                loadRows(RENDER_CANVAS_COMMAND_SKIPPED_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RENDER_CANVAS_COMMAND_FLOW_MAP =
                loadRows(RENDER_CANVAS_COMMAND_FLOW_MAP_RESOURCE);
        private static final List<MappingEvidenceRow> RENDER_CANVAS_COMMAND_FAMILY_COVERAGE =
                loadRows(RENDER_CANVAS_COMMAND_FAMILY_COVERAGE_RESOURCE);
        private static final List<MappingEvidenceRow> CANVAS_OPERATION_ENUM_ROWS =
                loadRows(CANVAS_OPERATION_ENUM_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> CANVAS_DRAWTARGET_COMMAND_ROWS =
                loadRows(CANVAS_DRAWTARGET_COMMAND_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> SHADER_PROGRAM_ROWS =
                loadRows(SHADER_PROGRAM_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> UI_MINIMAP_COMMAND_ROWS =
                loadRows(UI_MINIMAP_COMMAND_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> UI_MINIMAP_COMMAND_FLOW_MAP =
                loadRows(UI_MINIMAP_COMMAND_FLOW_MAP_RESOURCE);
        private static final List<MappingEvidenceRow> UI_MINIMAP_COMMAND_FAMILY_COVERAGE =
                loadRows(UI_MINIMAP_COMMAND_FAMILY_COVERAGE_RESOURCE);
        private static final List<MappingEvidenceRow> UI_MINIMAP_COMMAND_BRANCH_CONFLICTS =
                loadRows(UI_MINIMAP_COMMAND_BRANCH_CONFLICTS_RESOURCE);
        private static final List<MappingEvidenceRow> RENDER_GL_BACKEND_ROWS =
                loadRows(RENDER_GL_BACKEND_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RENDER_GL_BACKEND_CANVAS_SHADER_ROWS =
                loadRows(RENDER_GL_BACKEND_CANVAS_SHADER_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RENDER_GL_BACKEND_FLOW_MAP =
                loadRows(RENDER_GL_BACKEND_FLOW_MAP_RESOURCE);
        private static final List<MappingEvidenceRow> RENDER_GL_BACKEND_SKIPPED_ROWS =
                loadRows(RENDER_GL_BACKEND_SKIPPED_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RENDER_GL_BACKEND_TEXTURE_ROWS =
                loadRows(RENDER_GL_BACKEND_TEXTURE_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> RENDER_GL_TEXT_ROWS =
                loadRows(RENDER_GL_TEXT_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> FILESYSTEM_BACKEND_ROWS =
                loadRows(FILESYSTEM_BACKEND_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> FILESYSTEM_BACKEND_UPDATED_ROWS =
                loadRows(FILESYSTEM_BACKEND_UPDATED_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> FILESYSTEM_BACKEND_SKIPPED_ROWS =
                loadRows(FILESYSTEM_BACKEND_SKIPPED_ROWS_RESOURCE);
        private static final List<MappingEvidenceRow> FILESYSTEM_BACKEND_FLOW_MAP =
                loadRows(FILESYSTEM_BACKEND_FLOW_MAP_RESOURCE);
        private static final List<MappingEvidenceRow> FILESYSTEM_BACKEND_COVERAGE =
                loadRows(FILESYSTEM_BACKEND_COVERAGE_RESOURCE);
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

    public static final class EvidenceManifestRow {
        private final String resourceId;
        private final String fileName;
        private final String version;
        private final String category;

        private EvidenceManifestRow(String resourceId, String fileName, String version, String category) {
            this.resourceId = resourceId != null ? resourceId : "";
            this.fileName = fileName != null ? fileName : "";
            this.version = version != null ? version : "";
            this.category = category != null ? category : "";
        }

        public String resourceId() {
            return resourceId;
        }

        public String fileName() {
            return fileName;
        }

        public String version() {
            return version;
        }

        public String category() {
            return category;
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
