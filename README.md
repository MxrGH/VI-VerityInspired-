# FasterGameplayV2

**Maintainer:** MxrMC

Minecraft Java 26.2 · NeoForge 26.2.0.88 · Java 25.

## Current behavior

This project currently applies a fixed graphics preset once when entering a world. It does not continuously sample FPS or automatically raise render distance. This is intended to avoid the previous adaptive behavior, which could raise rendering workload unexpectedly.

## Honest status

This version is **not equivalent to Sodium, FerriteCore, or ModernFix** and does not contain their engine-level optimizations. It does not yet implement a custom chunk renderer or memory deduplication. Do not expect a guaranteed FPS increase from this helper alone.

## Reusing other projects

- Sodium's repository uses the PolyForm Shield 1.0.0 license; its code cannot simply be copied and rebranded without complying with that license.
- FerriteCore uses the MIT License; reuse requires retaining its copyright and license notice.
- ModernFix uses LGPL-3.0; covered code reuse must follow that license, including its applicable source and redistribution requirements.

Any future port must preserve third-party notices and be clearly credited. MxrMC can be listed as maintainer/author of this project, but not as the original author of upstream code.

## Build

Use Java 25 and the configured Gradle/ModDevGradle project. Output: `build/libs/fastergameplayv2-1.0.0.jar`.
