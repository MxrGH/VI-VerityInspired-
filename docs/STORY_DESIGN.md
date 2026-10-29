# Story design

This is an original fan-inspired interpretation, not canonical dialogue or a claim of 100% official accuracy.

## Phase 0 — Welcome
The Visitor appears in an unusual structure and seems helpful.

## Phase 1 — Unease
Environmental details become subtly wrong after exploration: a light flicker, a distant sound, or a changed object.

## Phase 2 — Interference
The Visitor reacts only to game events that the mod actually knows about. It must not claim to see private information.

## Phase 3 — Presence
The entity appears at a distance, disappears when approached, and uses sparse dialogue instead of constant jump scares.

## Implementation rules
- Persist story phase and event history per world.
- Run AI/TTS calls asynchronously with timeouts and rate limits.
- Keep API keys out of source code and logs.
- Do not upload microphone audio by default.
- Use authored fallback dialogue if an API fails.
