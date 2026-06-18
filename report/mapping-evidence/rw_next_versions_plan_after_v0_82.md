# Suggested route after v0.82

```text
v0.83  Custom projectile motion / laser / ballistic residual audit
v0.84  Custom unit animation / construction / conversion residuals
v0.85  Save/mod/custom-unit compatibility audit pass
v0.86  Mapping health audit: inherited warnings and same-name collision hardening
v0.87  Remaining UI/editor/custom-unit cross-surface cleanup
```

The immediate next pass should focus on `Projectile` residual fields (`C/E/F/N/O/T/aj/as/at/aA/aB/aC/aK/aL/aO/bn`) only after collecting enough callsite evidence from update/draw/serialization and projectile-template application paths.
