# Local external Mixin compatibility

Rusted Fabric Loader can adapt a user-supplied Mixin Jar compiled against another named mapping
set. The resulting compatibility mod is loaded by the ordinary RFL launcher, so the external
Mixins and RFL Java mods are applied to the same game process.

This workflow is intentionally local:

- RFL does not contain or distribute the external project, its mappings, or game files.
- The user must supply an external Mixin Jar and both layers of its Enigma mappings.
- The generated Jar belongs in the user's `javamods` directory and must not be committed.
- Only use software and mappings that you are authorized to use. A Java mod executes arbitrary
  code with the same permissions as the game.

## Prepare a compatibility mod

First build the external project's ordinary Mixin module. Do not use a statically mixed full-game
Jar: RFL needs the small Jar that contains the Mixin classes and `*.mixins.json` files.

Then run:

```powershell
.\gradlew.bat prepareExternalMixinMod `
  -PexternalMixinJar=C:\path\to\external-core.jar `
  -PexternalOfficialMappingsDir=C:\path\to\mappings\desktop `
  -PexternalNamedMappingsDir=C:\path\to\mappings\desktop_named `
  -PexternalNamespace=external_named `
  -PexternalModId=external_compat_local `
  "-PexternalModName=External compatibility (local)" `
  -PexternalModVersion=local `
  "-PgameDir=D:\path\to\Rusted Warfare"
```

The task composes the external two-layer Enigma mappings with RFL's Tiny mappings, remaps class
references, members, Mixin targets, selectors, `@At` targets, and shadows to the installed game's
runtime namespace, and writes:

```text
<gameDir>/javamods/external-compat-local.jar
```

Normal `RustedFabricLauncher.exe` startup then loads it automatically. Rename the generated Jar to
`.jar.disabled` to disable it. Regenerate it whenever the external Mixin Jar, external mappings,
RFL mappings, or target game version changes.

`-PexternalMixinModOutput=C:\somewhere\name.jar` can override the output path. The intermediate
combined mappings and default non-installed output remain under `build/rusted-dev/external-compat`
and are not distribution artifacts.

Compatibility depends on both projects targeting the same Rusted Warfare build. If either project
changes its targets or injection points, rebuild the bridge and repeat a startup/Mixin audit before
relying on it. This is only a low-level compatibility possibility: RFL does not claim built-in
support for any particular third-party overhaul.
