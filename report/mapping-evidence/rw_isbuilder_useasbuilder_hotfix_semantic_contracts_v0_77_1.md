# v0.77.1 isBuilder/useAsBuilder hotfix semantic contract

`com/corrodinggames/rts/game/units/custom/l.fp:Z` is `[core]isBuilder`.

`com/corrodinggames/rts/game/units/custom/l.fq:Z` is `[ai]useAsBuilder`.

Loader evidence from `CustomUnitLoader`:

- it reads the `useAsBuilder` key from the AI section into local boolean 55;
- it reads the `isBuilder` key from the core section into local boolean 56;
- it stores local 56 into `l.fp`;
- it stores local 55 into `l.fq`;
- the validation error string prints `Cannot tell AI to use a non-builder as builder [ai]useAsBuilder:` from `l.fq` and ` [core]isBuilder:` from `l.fp`.

Getter evidence from `CustomUnitMetadata`:

- `l()Z -> isBuilder` returns `fp`;
- `m()Z -> useAsBuilder` returns `fq`.
