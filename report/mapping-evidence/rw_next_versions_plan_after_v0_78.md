# Next versions plan after v0.78

## v0.79 — Custom attachment / leg / decal runtime residuals

Focus on custom-unit visual/runtime declaration helpers that are still close to the current line but separate from turret targeting.

Likely areas:

- leg/arm template residual fields and neighbor-index runtime helpers;
- attachment slot runtime links and parent/child unit relationship helpers;
- decal / image-layer declaration residuals with direct INI-key evidence;
- remaining construction-animation fields if `CustomUnitMetadata.bg` can be separated safely.

## v0.80 — Custom action/effect parser deep branch pass

Focus on action/effect parser branches with direct config-string evidence:

- custom action visibility/availability/cost/cooldown residuals;
- effect references triggered by actions/events;
- `autoTriggerOnEvent` parameter handling after v0.76's `AutoTriggerEventSpec` fix;
- conversion/spawn/transport action helpers.

## v0.81 — Projectile template selective pass

Focus only on projectile fields with clear parser + runtime evidence:

- projectile targeting/filtering fields;
- area/beam/trail effects;
- spawn-on-hit/death/split projectiles;
- interceptor and laser-defence projectile interactions.

## v0.82 — LogicBoolean selective parser/parameter pass

Focus on stable parser/helper classes used by custom units, actions, resources, and AI conditions.

Avoid a bulk pass over every anonymous/inner parser class unless it has clear string/call-site evidence.
