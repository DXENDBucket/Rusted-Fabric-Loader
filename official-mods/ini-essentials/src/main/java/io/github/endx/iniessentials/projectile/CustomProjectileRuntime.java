package io.github.endx.iniessentials.projectile;

import io.github.endx.rustedfabricapi.api.game.ProjectileImpactSnapshot;
import io.github.endx.rustedfabricapi.api.projectile.event.ProjectileEvents;
import io.github.endx.rustedfabricapi.api.projectile.motion.ProjectileMotion;
import io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileSpawnContext;
import io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileSpawnEvents;
import io.github.endx.rustedfabricapi.api.projectile.spawn.ProjectileSpawnSpec;
import rustedwarfare.custom.CustomUnit;
import rustedwarfare.game.Projectile;
import rustedwarfare.unit.Unit;
import rustedwarfare.util.UnitConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/** Dynamic motion, local memory, and lifecycle actions for independent projectiles. */
final class CustomProjectileRuntime {
    private static final Map<Projectile, CustomProjectileState> STATES =
            Collections.synchronizedMap(new WeakHashMap<Projectile, CustomProjectileState>());

    private CustomProjectileRuntime() { }

    static void register() {
        ProjectileSpawnEvents.AFTER_SPAWN.register(CustomProjectileRuntime::afterSpawn);
        ProjectileEvents.BEFORE_UPDATE.register(CustomProjectileRuntime::beforeUpdate);
        ProjectileEvents.AFTER_UPDATE.register(CustomProjectileRuntime::afterUpdate);
        ProjectileEvents.BEFORE_IMPACT.register(CustomProjectileRuntime::beforeImpact);
        ProjectileEvents.BEFORE_REMOVAL.register(CustomProjectileRuntime::beforeRemoval);
        ProjectileEvents.AFTER_REMOVAL.register((projectile, reason) -> {
            if (reason == ProjectileEvents.RemovalReason.REMOVED_FROM_GAME) {
                STATES.remove(projectile);
            }
        });
    }

    static void beginReload() {
        STATES.clear();
    }

    private static void afterSpawn(Projectile projectile, ProjectileSpawnSpec spec) {
        CustomProjectileDefinitions.Definition definition =
                CustomProjectileDefinitions.forTemplate(spec.template());
        if (definition == null || !(spec.context().source() instanceof CustomUnit)) return;
        CustomProjectileState state = new CustomProjectileState(definition, projectile,
                (CustomUnit) spec.context().source(), spec);
        STATES.put(projectile, state);
        definition.lifecycle().onCreate.execute(state);
        definition.motion().apply(state);
        definition.motion().applyPosition(state);
    }

    private static void beforeUpdate(Projectile projectile, float delta) {
        CustomProjectileState state = STATES.get(projectile);
        if (state == null) return;
        state.definition.lifecycle().onUpdate.execute(state);
        state.definition.motion().apply(state);
    }

    private static void afterUpdate(Projectile projectile, float delta) {
        CustomProjectileState state = STATES.get(projectile);
        if (state == null) return;
        state.definition.motion().applyPosition(state);
    }

    private static void beforeImpact(Projectile projectile, ProjectileImpactSnapshot ignored) {
        CustomProjectileState state = STATES.get(projectile);
        if (state == null || state.impactActionRan) return;
        state.impactActionRan = true;
        state.definition.lifecycle().onImpact.execute(state);
    }

    private static void beforeRemoval(Projectile projectile,
                                      ProjectileEvents.RemovalReason ignored) {
        CustomProjectileState state = STATES.get(projectile);
        if (state == null || state.removalActionRan) return;
        state.removalActionRan = true;
        state.definition.lifecycle().onRemove.execute(state);
    }

    static Parsed parse(UnitConfig config, String definitionId) {
        CustomProjectileExpression.MemorySchema memory = parseMemory(config, definitionId);
        MotionTemplate motion = MotionTemplate.parse(config, memory);
        LinkedHashMap<String, ActionTemplate> actions = new LinkedHashMap<String, ActionTemplate>();
        parseActions(config, "action_", memory, actions);
        parseActions(config, "hiddenAction_", memory, actions);
        Lifecycle lifecycle = Lifecycle.parse(config, actions);
        return new Parsed(motion, lifecycle);
    }

    private static CustomProjectileExpression.MemorySchema parseMemory(
            UnitConfig config, String definitionId) {
        LinkedHashMap<String, CustomProjectileExpression.MemoryType> declarations =
                new LinkedHashMap<String, CustomProjectileExpression.MemoryType>();
        for (Object rawItem : config.getKeysWithPrefix("core", "@memory ")) {
            String item = String.valueOf(rawItem).trim();
            String fullKey = item.toLowerCase(Locale.ROOT).startsWith("@memory ")
                    ? item : "@memory " + item;
            String name = fullKey.substring("@memory ".length()).trim().toLowerCase(Locale.ROOT);
            if (!name.matches("[a-z_][a-z0-9_]*")) {
                throw new IllegalArgumentException("invalid CustomProjectile memory name: " + name);
            }
            String rawType = config.getString("core", fullKey, null);
            if (rawType == null && !fullKey.equals(item)) {
                rawType = config.getString("core", item, null);
            }
            String type = rawType != null ? rawType.trim().toLowerCase(Locale.ROOT) : "";
            CustomProjectileExpression.MemoryType parsed;
            if ("float".equals(type) || "number".equals(type)) {
                parsed = CustomProjectileExpression.MemoryType.NUMBER;
            } else if ("bool".equals(type) || "boolean".equals(type)) {
                parsed = CustomProjectileExpression.MemoryType.BOOLEAN;
            } else {
                throw new IllegalArgumentException("CustomProjectile @memory " + name
                        + " supports float/number/bool/boolean, got: " + rawType);
            }
            if (declarations.put(name, parsed) != null) {
                throw new IllegalArgumentException("duplicate CustomProjectile memory: " + name);
            }
        }
        return CustomProjectileExpression.schema(definitionId, declarations);
    }

    private static void parseActions(UnitConfig config, String prefix,
                                     CustomProjectileExpression.MemorySchema memory,
                                     Map<String, ActionTemplate> actions) {
        for (Object rawSection : config.getNonMetaSectionsWithPrefix(prefix)) {
            String section = String.valueOf(rawSection);
            String name = normalizeAction(section.substring(prefix.length()));
            ActionTemplate action = ActionTemplate.parse(config, section, memory);
            if (actions.put(name, action) != null) {
                throw new IllegalArgumentException("duplicate projectile action: " + name);
            }
        }
    }

    private static String normalizeAction(String raw) {
        String result = raw.trim().toLowerCase(Locale.ROOT);
        if (!result.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("invalid projectile action name: " + raw);
        }
        return result;
    }

    private static String optional(UnitConfig config, String section, String key) {
        String result = config.getString(section, key, null);
        return result != null && !result.trim().isEmpty() ? result.trim() : null;
    }

    static final class Parsed {
        final MotionTemplate motion;
        final Lifecycle lifecycle;

        private Parsed(MotionTemplate motion, Lifecycle lifecycle) {
            this.motion = motion;
            this.lifecycle = lifecycle;
        }
    }

    static final class MotionTemplate {
        private final String speed, turnSpeed, dx, dy, offsetX, offsetY;
        private final Map<Object, CompiledMotion> compiled =
                Collections.synchronizedMap(new WeakHashMap<Object, CompiledMotion>());
        private final CustomProjectileExpression.MemorySchema memory;

        private MotionTemplate(String speed, String turnSpeed, String dx, String dy,
                               String offsetX, String offsetY,
                               CustomProjectileExpression.MemorySchema memory) {
            this.speed = speed; this.turnSpeed = turnSpeed; this.dx = dx; this.dy = dy;
            this.offsetX = offsetX; this.offsetY = offsetY;
            this.memory = memory;
        }

        static MotionTemplate parse(UnitConfig config,
                                    CustomProjectileExpression.MemorySchema memory) {
            return new MotionTemplate(optional(config, "motion", "speed"),
                    optional(config, "motion", "turnSpeed"),
                    optional(config, "motion", "dx"), optional(config, "motion", "dy"),
                    optional(config, "motion", "offsetX"),
                    optional(config, "motion", "offsetY"), memory);
        }

        void apply(CustomProjectileState state) {
            Object metadata = state.source.unitMetadata;
            CompiledMotion value = compiled.get(metadata);
            if (value == null) {
                value = new CompiledMotion(number(metadata, memory, speed),
                        number(metadata, memory, turnSpeed), number(metadata, memory, dx),
                        number(metadata, memory, dy), number(metadata, memory, offsetX),
                        number(metadata, memory, offsetY));
                compiled.put(metadata, value);
            }
            if (value.speed != null) ProjectileMotion.setFlightSpeed(
                    state.projectile, value.speed.evaluate(state));
            if (value.turnSpeed != null) ProjectileMotion.setTurnSpeed(
                    state.projectile, value.turnSpeed.evaluate(state));
            float resolvedDx = value.dx != null ? value.dx.evaluate(state)
                    : state.projectile.initialUnguidedSpeedX;
            float resolvedDy = value.dy != null ? value.dy.evaluate(state)
                    : state.projectile.initialUnguidedSpeedY;
            if (value.dx != null || value.dy != null) {
                ProjectileMotion.setVelocity(state.projectile, resolvedDx, resolvedDy);
            }
        }

        void applyPosition(CustomProjectileState state) {
            Object metadata = state.source.unitMetadata;
            CompiledMotion value = compiled.get(metadata);
            if (value == null) {
                apply(state);
                value = compiled.get(metadata);
            }
            if (value.offsetX != null) {
                ProjectileMotion.setPosition(state.projectile,
                        state.originX + value.offsetX.evaluate(state), state.projectile.y);
                state.requestedOffsetX = null;
            } else if (state.requestedOffsetX != null) {
                ProjectileMotion.setPosition(state.projectile,
                        state.originX + state.requestedOffsetX.floatValue(), state.projectile.y);
                state.requestedOffsetX = null;
            }
            if (value.offsetY != null) {
                ProjectileMotion.setPosition(state.projectile, state.projectile.x,
                        state.originY + value.offsetY.evaluate(state));
                state.requestedOffsetY = null;
            } else if (state.requestedOffsetY != null) {
                ProjectileMotion.setPosition(state.projectile, state.projectile.x,
                        state.originY + state.requestedOffsetY.floatValue());
                state.requestedOffsetY = null;
            }
        }
    }

    private static final class CompiledMotion {
        private final CustomProjectileExpression.Numeric speed, turnSpeed, dx, dy, offsetX, offsetY;

        private CompiledMotion(CustomProjectileExpression.Numeric speed,
                               CustomProjectileExpression.Numeric turnSpeed,
                               CustomProjectileExpression.Numeric dx,
                               CustomProjectileExpression.Numeric dy,
                               CustomProjectileExpression.Numeric offsetX,
                               CustomProjectileExpression.Numeric offsetY) {
            this.speed = speed; this.turnSpeed = turnSpeed; this.dx = dx; this.dy = dy;
            this.offsetX = offsetX; this.offsetY = offsetY;
        }
    }

    static final class Lifecycle {
        private final ActionTemplate onCreate, onUpdate, onImpact, onRemove;

        private Lifecycle(ActionTemplate onCreate, ActionTemplate onUpdate,
                          ActionTemplate onImpact, ActionTemplate onRemove) {
            this.onCreate = onCreate; this.onUpdate = onUpdate;
            this.onImpact = onImpact; this.onRemove = onRemove;
        }

        static Lifecycle parse(UnitConfig config, Map<String, ActionTemplate> actions) {
            ActionTemplate noOp = ActionTemplate.NO_OP;
            return new Lifecycle(resolve(config, actions, "onCreate", noOp),
                    resolve(config, actions, "onUpdate", noOp),
                    resolve(config, actions, "onImpact", noOp),
                    resolve(config, actions, "onRemove", noOp));
        }

        private static ActionTemplate resolve(UnitConfig config,
                                              Map<String, ActionTemplate> actions,
                                              String key, ActionTemplate fallback) {
            String raw = optional(config, "lifecycle", key);
            if (raw == null) return fallback;
            ActionTemplate result = actions.get(normalizeAction(raw));
            if (result == null) throw new IllegalArgumentException(
                    "[lifecycle] " + key + " references unknown projectile action: " + raw);
            return result;
        }
    }

    static final class ActionTemplate {
        private static final ActionTemplate NO_OP = new ActionTemplate(null, null, null,
                null, null, null, null, null, null, Collections.emptyList());
        private final String condition, speed, turnSpeed, dx, dy, offsetX, offsetY;
        private final CustomProjectileDefinitions.Reference emit;
        private final CustomProjectileExpression.MemorySchema memory;
        private final List<RawAssignment> assignments;
        private final Map<Object, CompiledAction> compiled =
                Collections.synchronizedMap(new WeakHashMap<Object, CompiledAction>());

        private ActionTemplate(String condition, String speed, String turnSpeed,
                               String dx, String dy, String offsetX, String offsetY,
                               CustomProjectileDefinitions.Reference emit,
                               CustomProjectileExpression.MemorySchema memory,
                               List<RawAssignment> assignments) {
            this.condition = condition; this.speed = speed; this.turnSpeed = turnSpeed;
            this.dx = dx; this.dy = dy; this.emit = emit; this.memory = memory;
            this.offsetX = offsetX; this.offsetY = offsetY;
            this.assignments = assignments;
        }

        static ActionTemplate parse(UnitConfig config, String section,
                                    CustomProjectileExpression.MemorySchema memory) {
            String emitRaw = optional(config, section, "emitProjectilePattern");
            String spawnRaw = optional(config, section, "spawnCustomProjectile");
            if (emitRaw != null && spawnRaw != null) {
                throw new IllegalArgumentException("[" + section + "] cannot use both "
                        + "spawnCustomProjectile and emitProjectilePattern");
            }
            String referenceRaw = spawnRaw != null ? spawnRaw : emitRaw;
            CustomProjectileDefinitions.Reference emit = referenceRaw != null
                    ? CustomProjectileDefinitions.Reference.parse(referenceRaw) : null;
            if (emit != null) CustomProjectileDefinitions.noteReference(emit);
            return new ActionTemplate(optional(config, section, "ifCondition"),
                    optional(config, section, "setSpeed"),
                    optional(config, section, "setTurnSpeed"),
                    optional(config, section, "setDx"), optional(config, section, "setDy"),
                    optional(config, section, "setOffsetX"),
                    optional(config, section, "setOffsetY"),
                    emit, memory, parseAssignments(optional(config, section, "setMemory"), memory));
        }

        void execute(CustomProjectileState state) {
            if (this == NO_OP) return;
            Object metadata = state.source.unitMetadata;
            CompiledAction action = compiled.get(metadata);
            if (action == null) {
                action = compile(metadata);
                compiled.put(metadata, action);
            }
            if (action.condition != null && !action.condition.evaluate(state)) return;
            for (MemoryAssignment assignment : action.assignments) assignment.apply(state);
            if (action.speed != null) ProjectileMotion.setFlightSpeed(
                    state.projectile, action.speed.evaluate(state));
            if (action.turnSpeed != null) ProjectileMotion.setTurnSpeed(
                    state.projectile, action.turnSpeed.evaluate(state));
            float resolvedDx = action.dx != null ? action.dx.evaluate(state)
                    : state.projectile.initialUnguidedSpeedX;
            float resolvedDy = action.dy != null ? action.dy.evaluate(state)
                    : state.projectile.initialUnguidedSpeedY;
            if (action.dx != null || action.dy != null) {
                ProjectileMotion.setVelocity(state.projectile, resolvedDx, resolvedDy);
            }
            if (action.offsetX != null) {
                state.requestedOffsetX = Float.valueOf(action.offsetX.evaluate(state));
            }
            if (action.offsetY != null) {
                state.requestedOffsetY = Float.valueOf(action.offsetY.evaluate(state));
            }
            if (emit != null) emitFromProjectile(state, emit);
        }

        private CompiledAction compile(Object metadata) {
            List<MemoryAssignment> result = new ArrayList<MemoryAssignment>();
            for (RawAssignment assignment : assignments) {
                if (memory.typeOf(assignment.name)
                        == CustomProjectileExpression.MemoryType.BOOLEAN) {
                    CustomProjectileExpression.Condition expression =
                            CustomProjectileExpression.compileBoolean(
                                    metadata, memory, assignment.value);
                    result.add(state -> state.setMemory(
                            assignment.name, expression.evaluate(state) ? 1.0F : 0.0F));
                } else {
                    CustomProjectileExpression.Numeric expression =
                            CustomProjectileExpression.compileNumber(
                                    metadata, memory, assignment.value);
                    result.add(state -> state.setMemory(
                            assignment.name, expression.evaluate(state)));
                }
            }
            return new CompiledAction(condition != null
                    ? CustomProjectileExpression.compileBoolean(metadata, memory, condition) : null,
                    number(metadata, memory, speed), number(metadata, memory, turnSpeed),
                    number(metadata, memory, dx), number(metadata, memory, dy),
                    number(metadata, memory, offsetX), number(metadata, memory, offsetY), result);
        }

        private static List<RawAssignment> parseAssignments(
                String raw, CustomProjectileExpression.MemorySchema memory) {
            if (raw == null) return Collections.emptyList();
            List<RawAssignment> result = new ArrayList<RawAssignment>();
            for (String item : splitTopLevel(raw)) {
                int equals = item.indexOf('=');
                if (equals <= 0 || equals == item.length() - 1) {
                    throw new IllegalArgumentException("setMemory requires name=expression: " + item);
                }
                String name = item.substring(0, equals).trim().toLowerCase(Locale.ROOT);
                if (!memory.contains(name)) {
                    throw new IllegalArgumentException("setMemory uses undeclared @memory: " + name);
                }
                result.add(new RawAssignment(name, item.substring(equals + 1).trim()));
            }
            return Collections.unmodifiableList(result);
        }
    }

    private static final class CompiledAction {
        private final CustomProjectileExpression.Condition condition;
        private final CustomProjectileExpression.Numeric speed, turnSpeed, dx, dy, offsetX, offsetY;
        private final List<MemoryAssignment> assignments;

        private CompiledAction(CustomProjectileExpression.Condition condition,
                               CustomProjectileExpression.Numeric speed,
                               CustomProjectileExpression.Numeric turnSpeed,
                               CustomProjectileExpression.Numeric dx,
                               CustomProjectileExpression.Numeric dy,
                               CustomProjectileExpression.Numeric offsetX,
                               CustomProjectileExpression.Numeric offsetY,
                               List<MemoryAssignment> assignments) {
            this.condition = condition; this.speed = speed; this.turnSpeed = turnSpeed;
            this.dx = dx; this.dy = dy; this.offsetX = offsetX; this.offsetY = offsetY;
            this.assignments = assignments;
        }
    }

    private static CustomProjectileExpression.Numeric number(
            Object metadata, CustomProjectileExpression.MemorySchema memory, String source) {
        return source != null ? CustomProjectileExpression.compileNumber(metadata, memory, source) : null;
    }

    private static void emitFromProjectile(CustomProjectileState state,
                                           CustomProjectileDefinitions.Reference reference) {
        Unit target = state.projectile.targetUnit;
        CustomProjectileEmitter.emit(reference, state.source, state.projectile.x,
                state.projectile.y, state.projectile.height, state.projectile.direction,
                target, true, state.projectile.targetX, state.projectile.targetY,
                state.projectile.targetHeight,
                ProjectileSpawnContext.Cause.PROJECTILE,
                state.projectile.spawnRecursionDepth + 1);
    }

    private static List<String> splitTopLevel(String raw) {
        List<String> result = new ArrayList<String>();
        int depth = 0;
        int start = 0;
        for (int index = 0; index < raw.length(); index++) {
            char value = raw.charAt(index);
            if (value == '(' || value == '[') depth++;
            else if (value == ')' || value == ']') depth--;
            else if (value == ',' && depth == 0) {
                result.add(raw.substring(start, index).trim());
                start = index + 1;
            }
            if (depth < 0) throw new IllegalArgumentException("unbalanced setMemory expression");
        }
        if (depth != 0) throw new IllegalArgumentException("unbalanced setMemory expression");
        result.add(raw.substring(start).trim());
        return result;
    }

    private static final class RawAssignment {
        private final String name, value;
        private RawAssignment(String name, String value) { this.name = name; this.value = value; }
    }

    private interface MemoryAssignment { void apply(CustomProjectileState state); }
}
