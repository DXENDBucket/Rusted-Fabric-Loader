# Next version plan after v0.79

Recommended route:

```text
v0.80  Custom action/effect deep branch pass
       - construction/conversion action residuals
       - self-mutation/action target helpers
       - memory/tag/resource action effect fields

v0.81  Projectile/effect runtime residual pass
       - projectile spawn/search callbacks
       - EffectTemplate runtime parameters
       - SoundEffectList / EffectList helper completion

v0.82  Custom resource / tech-tree / placement residuals
       - PlacementRule residuals
       - custom resource link fields
       - tech-tree visibility and lock conditions

v0.83  Save/replay deeper version gates
       - GameSaver headers
       - embedded setup fallback branches
       - remaining versioned block helpers
```

v0.80 should stay on the custom-unit line, but avoid mapping every action-effect class at once. The safest scope is to start with fields and helpers that have direct INI key strings or stable runtime branch evidence.
