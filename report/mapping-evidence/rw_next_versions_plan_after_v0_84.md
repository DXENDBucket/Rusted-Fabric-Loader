# Suggested route after v0.84

```text
v0.85  Custom projectile damage / splash / apply-effect residuals
v0.86  Custom resource-conversion and resource-action effect internals
v0.87  Mapping health audit: inherited warnings and same-name collision hardening
v0.88  Save/mod/custom-unit compatibility audit pass
v0.89  Remaining UI/editor/custom-unit cross-surface cleanup
```

v0.84 keeps the construction/conversion branch conservative. The next useful pass is to return to projectile/custom-effect runtime behavior, but focus on damage/splash/apply-effect contracts rather than movement state, which v0.83 already covered.
