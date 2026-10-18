# Phase 10.3 artwork

New Town buildings/props, three Spirit Tree states, and the horned crimson Dark Titan are generated local game art using the two approved references as direction. Original environmental and earlier boss PNGs are unchanged.

Sources: `town_atlas.png`, `props_atlas.png`, `tree_atlas.png`, `titan-walk_atlas.png`, `titan-charge_atlas.png`, `titan-poses_atlas.png`. OpenAI image generation created the sources; one background-removal edit prepared the Town atlas. No third-party downloads. The references are not runtime textures.

Re-export: compile/run `tools/PreparePhase103Assets.java` from the project root. Unequal building/prop atlas columns use inspected object bounds. Java trims alpha and uses nearest-neighbor scaling, stable bottom anchors and uniform per-sequence scale. Runtime loads only dedicated transparent cutouts and frames, through existing prepared caches.

`manifest.json` records frame order, anchors and contacts. Walk/charge each have 16 distinct articulated poses in a 2048x2048 sheet. Six four-pose sequences supply idle/entrance/slam/cast/hurt/defeat; the three ranged attacks and phase reaction share cast poses with different authoritative effects. Update-driven timing, damage and collision remain unchanged.

These are AI-authored sprite poses, not hand-drawn animation. Small changes in limb proportions, ornament details and cycle transitions remain; human art and motion approval is not claimed. Actual GamePanel sequences are under `build/phase10-3/captures/`.
