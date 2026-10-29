# FasterGameplayV2

A lightweight client-side performance helper for Minecraft Java 26.2, NeoForge 26.2.0.88, and Java 25.

## Current features

- Applies a low-render-cost graphics preset when entering a world.
- Uses a small periodic FPS sample to adapt render distance: repeated low-FPS samples lower it gradually; sustained high-FPS samples raise it gradually.
- Keeps the F3/debug overlay untouched.
- No bundled libraries or extra runtime dependencies beyond NeoForge.

## Important scope

This is an early optimization layer, not yet a renderer rewrite. It does **not** currently implement Sodium-style chunk meshing/rendering, FerriteCore-style memory deduplication, or ModernFix-style targeted engine patches. Those require deeper changes and careful profiling, and should be added as separate tested modules rather than claimed prematurely.

FPS gains depend on hardware, world, mods, and drivers. The mod cannot guarantee a specific FPS increase or reduce network ping.

## Build

Use Java 25 and the configured Gradle/ModDevGradle project. Output: `build/libs/fastergameplayv2-1.0.0.jar`.
