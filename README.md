# Verity-Inspired Horror — Minecraft Java 26.2

A NeoForge horror mod project targeting Minecraft 26.2 / NeoForge 26.2.0.88 / Java 25.

## Implemented in source
- Registered hostile entity: `verityinspired:verity`
- Summon with `/summon verityinspired:verity` (requires normal Minecraft command permission)
- 70 health, 8 attack damage, high knockback resistance, long follow range
- Hostile player targeting, melee pursuit, wandering, and looking behavior
- Stalking sound cues and occasional short-range teleporting when tracking a player
- A vanilla humanoid silhouette/texture so no external assets are required

## API keys and unfinished integrations
The existing `/api tts api-key <key>` and `/api ai api-key <key>` commands only store keys locally. **They do not call an AI or TTS provider yet.** Simple Voice Chat is listed as a dependency in mod metadata, but voice-chat audio routing and actual generated speech still need implementation.

Keep API keys private and only configure them on a server you control. Cloud AI/TTS usage may cost money.

## Build
Use JDK 25 and run `gradle build --no-daemon`. The compiled JAR should appear in `build/libs/`. The GitHub Actions workflow builds on pushes to `main` and uploads an artifact named `verityinspired-jar` when successful.

This source change has not yet been confirmed by a successful build; check the Actions run before installing the JAR.