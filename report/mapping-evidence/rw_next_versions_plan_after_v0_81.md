# Suggested route after v0.81

```text
v0.82  Custom projectile/effect runtime behavior residuals
v0.83  Custom unit animation / construction / conversion residuals
v0.84  Save/mod/custom-unit compatibility audit pass
v0.85  Mapping health audit: inherited warnings and same-name collision hardening
v0.86  Remaining UI/editor/custom-unit cross-surface cleanup
```

Recommended next step: move from resource/tag/memory helpers into projectile/effect runtime behavior, but continue the same high-confidence rule: only map rows backed by INI keys, direct strings, or clear callsite behavior.
