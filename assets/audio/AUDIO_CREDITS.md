# Audio credits — Santa: Last Christmas

Verified against original publisher pages and downloads on 2026-10-11. All five music downloads and all 103 files in the supplied Kenney pack match the original publisher bytes. See [music provenance](provenance.json), [Kenney provenance](kenney_provenance.json), [PCM conversions](conversions.json) and [license information](LICENSES.md).

| Game use | Exact supplied download | Title / author | Original source | License / required attribution |
| --- | --- | --- | --- | --- |
| Main Menu | `music/wintery loop.wav` | Wintery loop — Emma_MA (embedded artist: Emma M. Andersson) | [Wintery loop](https://opengameart.org/content/wintery-loop) | CC0 1.0; attribution optional |
| The Last Village; ending and credits presentation | `music/BGM_Snow_Theme_Loop.flac` | Through the Snow, published as Snow Theme — Cleyton Kauffman / CleytonKauffman | [Snow Theme](https://opengameart.org/content/snow-theme) | CC0 1.0; attribution optional. Courtesy credit: Music by Cleyton Kauffman — https://soundcloud.com/cleytonkauffman |
| Frozen Forest | `music/synth-thetic_-_beyond_the_frozen_veil_-_cc0.7z` | Beyond the Frozen Veil — Synth-thetic | [Beyond the Frozen Veil](https://opengameart.org/content/beyond-the-frozen-veil) | CC0 1.0; attribution optional |
| Christmas Town | `music/winter-wind-short.mp3` | Winter Wind (short version) — wipics | [Winter Wind](https://opengameart.org/content/winter-wind) | CC0 1.0; attribution optional |
| Christmas Eater encounter | `music/Juhani Junkala - Epic Boss Battle [Seamlessly Looping].wav` | Epic Boss Battle — Juhani Junkala / SubspaceAudio | [Boss Battle Music](https://opengameart.org/content/boss-battle-music) | CC0 1.0 on this original release; attribution optional |
| Menu/UI | `sfx/ui/kenney_interface-sounds/Audio/*.ogg` (100 audited files) | Interface Sounds 1.0 — Kenney | [Interface Sounds](https://kenney.nl/assets/interface-sounds) | CC0 1.0; attribution optional |

The source publisher for Winter Wind is **wipics**. Unrelated recordings with the same title have different licenses; those licenses do not apply to this byte-matched download. Likewise, the verified original Epic Boss Battle release is CC0 even though other distributors describe their copies differently.

## Runtime files and modifications

Five runtime music files under `runtime/music/`: `wintery_loop.wav`, `snow_theme.wav`, `beyond_the_frozen_veil.wav`, `winter_wind.wav`, `epic_boss_battle.wav`.

Six runtime UI files under `runtime/ui/`:

| Cue | Converted original |
| --- | --- |
| Changed hover/keyboard selection | `select_001.ogg` → `select_001.wav` |
| Story/Settings/Credits button activation | `click_001.ogg` → `click_001.wav` |
| START | `confirmation_001.ogg` → `confirmation_001.wav` |
| BACK / Escape from a menu page | `back_001.ogg` → `back_001.wav` |
| Volume, motion and mute controls | `switch_001.ogg` → `switch_001.wav` |
| EXIT confirmation | `confirmation_002.ogg` → `confirmation_002.wav` |

All runtime copies are stereo, 44.1 kHz, signed 16-bit little-endian PCM WAV. Conversion changes format/channel representation/sample rate where needed, without composition edits or volume normalization. The supplied looping Forest FLAC is used; the longer non-looping variant is not integrated. Its actual duration is 142.221 seconds, despite the publisher's description giving a different duration. Originals remain untouched.

Credit is provided voluntarily. Keep this documentation and the supplied [Kenney License.txt](sfx/ui/kenney_interface-sounds/License.txt) with distributions. No endorsement by the audio authors is implied.

## Phase 2: combat, movement, world and restoration

The supplied Impact Sounds and RPG Audio files match the original Kenney release ZIP members byte for byte. RPG Audio actually contains 51 `Audio/` OGGs plus `Preview.ogg` (the publisher page labels the pack 50 files); all 55 local pack files were checked. Impact has 130 OGGs and three documents. The only two supplied magic effects are `magical_1.ogg` and `magical_3.ogg`. The first matches the publisher attachment named `magical_1_0.ogg`. All 190 local files are covered in [Phase 2 provenance](phase2_provenance.json); no filename alone was treated as license proof.

| Pack / author | Publisher | License / redistribution | Attribution |
| --- | --- | --- | --- |
| Impact Sounds / Kenney | [Original release](https://kenney.nl/assets/impact-sounds) | CC0 1.0; modification and public redistribution permitted | Optional |
| RPG Audio / Kenney (included license names Kenney Vleugels) | [Original release](https://kenney.nl/assets/rpg-audio) | CC0 1.0; modification and public redistribution permitted | Optional |
| Magic Spell SFX / JaggedStone | [Original release](https://opengameart.org/content/magic-spell-sfx) | CC0 1.0; modification and public redistribution permitted | None required by author |

Exact Phase 2 conversions (paths below are relative to `assets/audio/`):

| Runtime WAV | Original source | Duration |
| --- | --- | --- |
| `runtime/sfx/santa_attack.wav` | `sfx/combat/kenney_rpg-audio/Audio/knifeSlice.ogg` | 0.600 s |
| `runtime/sfx/enemy_hit.wav` | `sfx/combat/kenney_impact-sounds/Audio/impactPunch_medium_000.ogg` | 0.431 s |
| `runtime/sfx/santa_damage.wav` | `sfx/combat/kenney_impact-sounds/Audio/impactPunch_heavy_000.ogg` | 0.649 s |
| `runtime/sfx/enemy_defeat.wav` | `sfx/combat/kenney_impact-sounds/Audio/impactSoft_heavy_000.ogg` | 0.527 s |
| `runtime/sfx/dash.wav` | `sfx/combat/kenney_rpg-audio/Audio/cloth1.ogg` | 0.661 s |
| `runtime/sfx/christmas_bell.wav` | `sfx/combat/kenney_impact-sounds/Audio/impactBell_heavy_000.ogg` | 1.480 s |
| `runtime/sfx/crystal_break.wav` | `sfx/combat/kenney_impact-sounds/Audio/impactGlass_heavy_000.ogg` | 0.238 s |
| `runtime/sfx/quest_complete.wav` | `sfx/magic/magical_3.ogg` | 1.821 s |
| `runtime/sfx/gift_pickup.wav` | `sfx/combat/kenney_rpg-audio/Audio/handleCoins2.ogg` | 0.338 s |
| `runtime/sfx/healing_shrine.wav` | `sfx/magic/magical_1.ogg` | 1.609 s |
| `runtime/sfx/npc_interaction.wav` | `sfx/combat/kenney_rpg-audio/Audio/bookOpen.ogg` | 0.151 s |
| `runtime/sfx/boss_attack.wav` | `sfx/combat/kenney_impact-sounds/Audio/impactMetal_heavy_000.ogg` | 0.165 s |
| `runtime/sfx/boss_damage.wav` | `sfx/combat/kenney_impact-sounds/Audio/impactPlate_heavy_000.ogg` | 0.489 s |
| `runtime/sfx/boss_phase.wav` | `sfx/magic/magical_3.ogg` | 1.821 s |
| `runtime/sfx/boss_defeat.wav` | `sfx/magic/magical_1.ogg` | 1.609 s |
| `runtime/sfx/spirit_tree_restore.wav` | `sfx/magic/magical_1.ogg` | 1.609 s |

All 16 copies are PCM signed 16-bit, stereo, 44100 Hz. The existing ffmpeg development tool only decodes, resamples and expands channels; no normalization, pitch edits, synthesis, trimming or source overwrites. [Conversion records](phase2_conversions.json) include original hashes, runtime hashes and actual input codecs. Per-effect gain is applied by the game mixer. Tick-30 restoration reuses `boss_phase.wav`, tick-90 reuses `christmas_bell.wav`, and tick-120 reuses `quest_complete.wav`; initial energy uses `spirit_tree_restore.wav`. Shared source effects are explicitly credited rather than presented as bespoke recordings.
