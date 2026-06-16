# v0.77.1 isBuilder/useAsBuilder hotfix report

Base: v0.77 (`13287` rows)  
Output: v0.77.1 (`13287` rows)  
Added rows: `0`  
Updated rows: `2`  
Validation status: `pass`

## Fixed conflict

v0.77 had this unsafe named field collision:

```text
com/corrodinggames/rts/game/units/custom/l.fp:Z -> useAsBuilder
com/corrodinggames/rts/game/units/custom/l.fq:Z -> useAsBuilder
```

The correct split is:

```text
com/corrodinggames/rts/game/units/custom/l.fp:Z -> isBuilder
com/corrodinggames/rts/game/units/custom/l.fq:Z -> useAsBuilder
```

## Evidence

`CustomUnitLoader` reads `[core]isBuilder` into the value stored in `l.fp`, and reads `[ai]useAsBuilder` into the value stored in `l.fq`. The validation error string also labels them that way:

```text
Cannot tell AI to use a non-builder as builder [ai]useAsBuilder: ... [core]isBuilder: ...
```

The getters match the same split:

```text
CustomUnitMetadata.l()Z -> isBuilder -> fp
CustomUnitMetadata.m()Z -> useAsBuilder -> fq
```

## New validation guard

v0.77.1 adds a named-field collision check. It specifically catches the downstream remapper failure class:

```text
FIELDs owner/[officialA, officialB];;descriptor -> sameNamedName
```

Validation results:

```text
named_field_collisions: 0
named_method_collisions: 0
old_fp_useAsBuilder_residue: 0
custom_l_fp_isBuilder_ok: True
custom_l_fq_useAsBuilder_ok: True
```
