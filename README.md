# Verity-Inspired Horror — Minecraft Java 26.2

Target: Minecraft Java 26.2, NeoForge 26.2.0.88, Java 25.

## Gameplay
- Hostile entity: `verityinspired:verity`
- Summon it with `/summon verityinspired:verity`.
- Verity has 70 health, strong melee damage, a long detection range, stalking sound cues, and occasional teleporting while tracking a player.
- It uses a vanilla humanoid texture as a placeholder visual.

## AI dialogue and voice
The mod includes OpenAI-compatible service calls against OpenAI's API:
- `/verity ai <prompt>` asks the AI for a Verity-style response in chat.
- `/verity speak <text>` generates speech and attempts to play it as positional audio through Simple Voice Chat.
- `/verity ask <prompt>` asks the AI, then generates and plays the answer through Simple Voice Chat.

## Defaults (API keys are optional)
The mod is designed to work without API keys:
- **Default AI behavior:** a built-in, offline set of Verity-style replies is used when `ai.api-key` is blank. This is a lightweight scripted fallback, not a full language model.
- **Default voice:** on Windows, Verity uses the built-in Windows speech synthesizer at a slower rate for an eerie delivery. The exact voice depends on which Windows speech voice is installed.
- **Default speech recognition:** on Windows, the mod tries the built-in offline Windows speech recognizer when `ai.api-key` is blank. This requires a compatible Windows speech recognition language pack/recognizer. If unavailable, configure an AI key for online transcription.
- On non-Windows systems, the offline Windows voice/recognition fallback is unavailable; configure the relevant API key for those features.

Optional enhanced AI and TTS can be enabled with operator commands:
- `/api ai api-key <your-api-key>` enables online transcription and AI-generated replies.
- `/api tts api-key <your-api-key>` enables online generated speech.

Keys are written to `config/verityinspired-secrets.properties`, not echoed in chat. Keep this file private. Do not enter keys on servers you do not control. API usage may incur charges. These commands currently target OpenAI's Responses and audio endpoints.

## Required dependency
Install a Simple Voice Chat version compatible with the exact Minecraft/NeoForge version and API 2.6.0 or newer. The server and listeners who should hear generated voice need a working Simple Voice Chat setup. Its server UDP port must be configured as described in the official documentation: https://dev.modrepo.de/minecraft/voicechat

## Build and verification
Use JDK 25 and run `gradle build --no-daemon`. The JAR should be produced in `build/libs/`. GitHub Actions is configured to upload the `verityinspired-jar` artifact on a successful build.

**Important:** The new AI/TTS/voice-chat code has been committed, but its first build and in-game test still need confirmation. If the build fails, inspect the GitHub Actions logs before installing the mod.
