# INI Essentials

INI Essentials adds opt-in fields and extensions to Rusted Warfare custom-unit INI files. Omitting
all documented extensions leaves native INI parsing and runtime behavior unchanged.

An extension can add a new key, extend a native key's accepted value range, or add a new textual
format. Native keys with native-valid values remain on the native parser path. Extended values and
formats must declare an explicit activation rule, so the extension cannot accidentally capture an
ordinary value.

## Current fields

```ini
[core]
allowNegativeHp: true
```

`allowNegativeHp` is optional and defaults to `false`. When enabled, overkill damage can leave that
custom unit's HP below zero. Installing INI Essentials alone remains multiplayer-optional; parsing
an enabled synchronized field promotes it to a required matching peer dependency.

Camera action effects are available in both visible and hidden custom actions:

```ini
[action_focus]
cameraCenterOn: actionTarget,0,-60
cameraTargetZoom: 1.25
cameraStopMovement: true
```

`cameraCenterAt`, `cameraCenterBy`, and `cameraCenterOn` are mutually exclusive inside one action.
`cameraCenterOn` accepts `self`, `target`, or `actionTarget`, followed by optional X/Y offsets. The
effects run only on the client locally controlling the acting unit's team; they do not alter
deterministic simulation state or another player's camera. A missing unit/action target makes the
corresponding contextual move a safe no-op.

The native `tookDamage` action event now receives typed damage context through the game's existing
`eventData(...)` function:

```ini
[hiddenAction_on_damage]
autoTriggerOnEvent: tookDamage
showMessageToPlayer: Damage: %{eventData(name="hpDamage", type="number")}, attacker: %{eventSource}
```

Available names are `damage`, `rawDamage`, `hpDamage`, `shieldDamage`, `remainingDamage`,
`hpBefore`, `hpAfter`, `shieldBefore`, `shieldAfter`, and `wasLethal`. `damage` is the value passed
to the native damage routine after attachment forwarding, immunity, and custom-unit armour;
`hpDamage` and `shieldDamage` are the actual non-negative reductions. Existing `eventSource` and
projectile-tag filtering retain their native behavior. Parsing any enhanced name activates the
matching multiplayer requirement because these values can influence synchronized actions.

The machine-readable bilingual field catalog is stored at
`src/main/resources/ini_essentials/fields.csv`; enhanced native event values are stored in
`src/main/resources/ini_essentials/event-data.csv`. The generated spreadsheet in `docs/` is intended
for the same community workflow as the original Rusted Warfare Unit Modding Reference.

The workbook is generated rather than edited by hand. Its English and Chinese pages use the
original reference's five-column-first layout and matching section colors (`[core]` green,
action/hiddenAction orange), followed by extension metadata columns. Colored section shortcuts at
the top of each page jump directly to their section, and every section links back to the index.
Like the community 1.15 enhanced reference, these controls use transparent DrawingML hyperlink
overlays: hovering shows a link pointer and clicking jumps immediately without selecting a cell.

```text
python docs/generate_reference.py
python docs/generate_reference.py --check
```

`sync-schema.txt` is the canonical gameplay/protocol input for the multiplayer SHA-256. Descriptive
wording is deliberately excluded so documentation-only edits do not break compatibility.
