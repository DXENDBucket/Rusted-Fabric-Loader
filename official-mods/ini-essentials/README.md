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

The machine-readable bilingual field catalog is stored at
`src/main/resources/ini_essentials/fields.csv`. The generated spreadsheet in `docs/` is intended for
the same community workflow as the original Rusted Warfare Unit Modding Reference.

`sync-schema.txt` is the canonical gameplay/protocol input for the multiplayer SHA-256. Descriptive
wording is deliberately excluded so documentation-only edits do not break compatibility.
