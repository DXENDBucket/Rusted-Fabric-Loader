# Suggested route after v0.83

```text
v0.84  Custom unit animation / construction / conversion residuals
v0.85  Custom projectile damage/splash/apply-effect residuals
v0.86  Save/mod/custom-unit compatibility audit pass
v0.87  Mapping health audit: inherited warnings and same-name collision hardening
v0.88  Remaining UI/editor/custom-unit cross-surface cleanup
```

The next best pass is probably the custom-unit animation/construction/conversion branch. v0.83 cleaned the main projectile motion/laser/ballistic state; deeper damage modifiers and custom effect application should wait until the action/effect parser and runtime damage branch can be audited together.
