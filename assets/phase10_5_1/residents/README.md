# NPC alpha extraction cleanup

Six isolated RGBA idle sprites derived from the unchanged, supplied `assets/characters/village/residents_atlas.png`.

The original y=350 sprite cells include the lower edge of the portrait above. `tools/PreparePhase1051Npcs.java` locates the largest eight-connected body at alpha >24 and preserves its bounding rectangle plus three source pixels on every side. This deliberately retains fine detached hair/fur and all original transparency between limbs. Only samples outside those padded body bounds are cleared; retained RGB/alpha samples are copied exactly. No artwork was generated, recolored, blurred, resized or redesigned.

Each export retains the original extraction's full canvas dimensions, including newly transparent top margins. Village and celebration therefore retain their original scaling, poses and ground anchors. Portraits, the original shared atlas and Toby remain unchanged. Missing/invalid-sized exports use the existing original extraction fallback.

`manifest.json` records crop rectangles, dimensions and removed pixel counts. Six source identities: Martha, Oliver, Noah, Iris, Clara and Ben. Upstream provenance remains in `assets/characters/village/README.md`.
