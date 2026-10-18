# Journey Home artwork provenance

Created 2026-10-12 using the built-in OpenAI image generation tool. No external art/audio was downloaded. Original references, maps, character artwork and credits are unchanged. Generation provenance does not establish copyright exclusivity or an independently verified third-party license.

- `sleigh.png`: 1672×941 RGBA, empty red/gold sleigh, leather harness and exactly two reindeer facing right. Generated as a separate transparent production prop, using the ending reference for design and the actual restored Town raster for perspective/material style. Runtime trims/scales once to 300×144.
- `town_citizens.png`: 1536×1024 RGBA, three full-body Town neighbors: russet/ivory baker, teal/cream woman and indigo/ochre boy. Generated using the existing Santa sprite for proportions and the celebration board for mood. These are Town residents, not relocated Village NPC identities. Inspected figure boundaries are x0–584,584–1072,1072–1536; runtime crops each and scales once to 48×60.

Sleigh prompt: a finished hand-painted top-down/three-quarter snowy game cutout, deep-red empty seat, antique-gold scrollwork, warm lanterns, snow caps and small rear gifts, connected to exactly two side-by-side brown reindeer, facing right; no Santa, terrain, UI, text or reference-board integration.

Citizen prompt: one transparent row of three distinct complete idle winter neighbors matching Santa's hand-painted chibi scale, muted russet/teal/indigo coats and cream/ochre scarves, calm hopeful expressions; no named Village resident, Santa, weapons, labels or terrain.

Source PNGs stay in the workspace. A shared asynchronous worker prepares the small immutable sprites; painting reads completed caches without waiting. Missing/invalid optional art uses bounded native exception fallbacks so the game remains playable. Normal final presentation uses the generated artwork. Desk envelopes and parchment/UI borders are native Java2D artwork using the existing palette and UI owners. Citizens have one pose; the departure animates the combined harnessed assembly rather than individual leg cycles.
