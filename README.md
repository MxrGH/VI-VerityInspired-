# Verity-Inspired Horror — Minecraft 26.2

An unofficial original horror-mod starter project for Minecraft Java 26.2, NeoForge 26.2.0.88, and Java 25. This is **not** the official Verity JE mod and includes no protected code, models, textures, dialogue, or actor voice.

## Intended features
- A mysterious orb-like entity that becomes more unsettling over time
- Story phases: Welcome → Unease → Interference → Presence
- Atmospheric events, distant sounds, and persistent world progression
- Required Simple Voice Chat dependency for intended multiplayer play
- Configurable AI and TTS provider adapters with API keys kept local

## Current status
This repository has been reset to a **source scaffold**, not a finished, compiled, or tested mod JAR. Entity registration, gameplay, AI/TTS adapters, and Simple Voice Chat audio routing still need implementation and validation against the exact Minecraft 26.2 APIs.

## Requirements
- Java 25
- Minecraft Java 26.2 with NeoForge 26.2.0.88
- Simple Voice Chat build compatible with NeoForge / Minecraft 26.2

Simple Voice Chat: https://modrinth.com/mod/simple-voice-chat  
Documentation: https://modrepo.de/minecraft/voicechat/wiki

## AI and TTS
Cloud AI and TTS integrations require your own provider API keys and may cost money. Keep keys out of source code and logs. The original series' exact voice is not included; use it only with authorized voice assets or permission.

## Build
Once the missing integrations and version-specific API wiring are implemented and validated, use Java 25 and run `gradle build`. The JAR should be generated in `build/libs/`.
