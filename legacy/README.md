# Legacy components

This directory contains frozen experiments from the discontinued native Android APK route. They
are retained for design history and possible code reference, but are excluded from active Gradle
settings, release artifacts, and verification tasks.

Archived components include:

- Modern Xposed hooks and Android DEX mod management;
- the local APK patch/sign/install pipeline and patched bootstrap;
- Android APK inspection and the Android 1.15 mapping profile;
- the former Android-specific API adapter;
- the dual Jar/DEX portable example and its build script;
- documentation for `.javamod`, local APK patching, and the old portable build.

The supported Android direction is `android/launcher`: it runs user-imported desktop files in an
ARM64 JVM and therefore uses the single `rusted-fabric-api` module just like the Windows runtime.
