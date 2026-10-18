# Phase 10 boss artwork

Runtime art only; no terrain or reference collage is part of this package.

## Frost Fang

`frost_fang/source/generated_pose_atlas.png` is newly generated transparent local
character artwork (OpenAI image generation, 2026-10-13). It depicts the same
icy wolf in sixteen action poses. `PreparePhase10Assets.java` extracts the
largest alpha-connected character in each cell, fits it into a 256x256 frame
with a bottom-centre anchor (128,240), and prepares ten four-frame sequences:
idle, walk, run, claw_swipe, ice_blast, frost_wave, frost_dash, hurt, entrance,
defeat. Intermediate mesh deformation supplies small limb/body movement;
the attack sequences also select different authored poses. Source atlas poses
include some attack energy; separate cached effects express gameplay hit timing.

## Dark Titan

The five approved PNG key poses in `dark_titan/` remain byte-identical. Their
original README/manifest describe those source poses, not the generated output.
`dark_titan/generated/` has two sixteen-frame, 512x512 RGBA sequences, exact
2048x2048 four-by-four sheets and animated GIF previews. Additional four-frame
idle/hurt/entrance/defeat/ground_slam/cast presentations are included.

Final Titan artwork is a source-pixel cutout rig, not thirty-two hand-drawn
poses. Torso, both arms and both legs have separate masks and pivots. The
opposite approved walk supplies the planted right boot. Charge uses approved
planted-arm artwork and the third approved pose for impact. Lowering, limb
rotation, foot lift and support stretch create genuinely different frames.
The rig preserves source identity but cannot reconstruct occluded limb surfaces.
Small joint seams, modest stride articulation and transitions between approved
charge poses remain quality limits; these are not certified production-quality
hand-drawn animation. Generated Titan pose studies were rejected because their
proportions drifted; they are retained only as build evidence and excluded here.

Walk: 12 fps, contacts at frames 01 and 09, eighty gameplay updates per loop.
Charge: five active updates per frame until frame 16; launch frame 10, impact
frame 13, recovery frames 15-16. Frame 16 holds through the remaining recovery.
Its game attack lasts 105 updates; GIFs show uniform preview timing, not a
recording of movement or damage. Sprite anchors are (256,496).

## Effects and timing

Twelve twelve-frame transparent 256x256 sequences: claw, blast, wave, dash,
hurt, defeat, slam, spikes, beam, storm, footstep, warning. They are newly authored,
seeded offline pixel facets, ice fragments, cracks and energy; no external asset
downloads or third-party artwork were introduced. Effects have separate visual
geometry from collision and use bounded update-driven indexing. The PNG frames
and sheets are authoritative; GIF palette conversion may simplify transparency.

Most attack effects advance every three active updates (20 fps); footstep bursts
advance each update (60 fps), defeat effects use the fall clock. Manifest fps
values for these previews describe GIF playback, while the Java owner defines
exact runtime timing. No effect grants additional damage.

Java loads only prepared sheets once through the existing common-pool cache,
scales with nearest neighbour, and paints BufferedImages. Generated directories
are separate from approved source files. Rebuild tool: `tools/PreparePhase10Assets.java`.
The project includes the generated assets; regenerating the original imagegen
poses is not required to build or run the game.

Phase 10.1 polish: `tools/PreparePhase101Assets.java` reuses the original offline
helpers and approved source pixels. Fang now has eight frames per animation,
a fixed (128,240) source ground anchor and a cached contact shadow. Its 269px
runtime canvas gives a 151-153px standing silhouette beside Santa's 55px
silhouette; combat bounds remain 76px. Titan retains sixteen walk/charge frames,
source-pixel shoulder/hip overlaps and proportionate support boots. These remain
cutout/mesh animations, not newly hand-drawn poses. Existing attack clocks,
foot contacts 01/09, charge launch 10 and impact 13 are unchanged.
