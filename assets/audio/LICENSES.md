# Audio redistribution licenses

Audit date: 2026-10-11. All currently supplied music and Kenney UI audio are verified **CC0 1.0 Universal**. Original download bytes, archive members and SHA-256 hashes are recorded in [provenance.json](provenance.json) and [kenney_provenance.json](kenney_provenance.json). Converted output hashes are in [conversions.json](conversions.json).

CC0 permits copying, modification and redistribution, including commercial use, without asking permission or requiring attribution. See the official [CC0 deed](https://creativecommons.org/publicdomain/zero/1.0/) and [CC0 legal code](https://creativecommons.org/publicdomain/zero/1.0/legalcode.en). These audio assets are licensed independently of this project's source/artwork.

## Publisher license evidence

- [Emma_MA — Wintery loop](https://opengameart.org/content/wintery-loop): CC0; publisher also states its public-domain release.
- [Cleyton Kauffman — Snow Theme / Through the Snow](https://opengameart.org/content/snow-theme): CC0; optional attribution. The local FLAC matches the member of the publisher's `snow_theme_1.zip` archive.
- [Synth-thetic — Beyond the Frozen Veil](https://opengameart.org/content/beyond-the-frozen-veil): CC0; publisher explicitly permits unrestricted use without required attribution. The complete local 7z archive matches the original download.
- [wipics — Winter Wind](https://opengameart.org/content/winter-wind): CC0 / public domain; exact short MP3 byte match.
- [Juhani Junkala / SubspaceAudio — Boss Battle Music / Epic Boss Battle](https://opengameart.org/content/boss-battle-music): CC0; exact supplied WAV byte match. This is the license of this release, not an inference from a title or third-party upload.
- [Kenney — Interface Sounds](https://kenney.nl/assets/interface-sounds): CC0; exact 100 OGGs, included license and two shortcut files verified against the official pack. The original [License.txt](sfx/ui/kenney_interface-sounds/License.txt) is preserved byte for byte.

## Public GitHub safety

Safe to commit: the **five exact verified original music downloads**, **100 original Kenney OGGs**, **11 converted runtime WAVs**, supplied Kenney license and these attribution/provenance/conversion documents. These counts describe Phase 1; the subsequent Phase 2 additions are listed below. No unverified track was integrated, and no original download was deleted or overwritten.

The project `.gitignore` denies new audio paths by default and allows these exact audited filenames. Generated validation/extraction material under `build/audio-phase1/` is ignored. Future downloads require their own provenance/license review before being added to the allowlist; an extension, filename or the term royalty-free is not proof of permission. Hash validation detects replacement of an allowed file with different content. `.gitignore` does not remove already tracked files and does not prevent `git add --force`.

There are currently **no missing or unverified supplied music tracks**. Nothing was committed or pushed automatically.

## Phase 2 verification and distribution

Verified on 2026-10-11 directly against the original publishers: [Kenney Impact Sounds](https://kenney.nl/assets/impact-sounds), [Kenney RPG Audio](https://kenney.nl/assets/rpg-audio), and [JaggedStone Magic Spell SFX](https://opengameart.org/content/magic-spell-sfx). Each release is CC0 1.0 Universal, permitting source/converted redistribution and modification without required attribution. Voluntary credit appears in AUDIO_CREDITS.md.

Preserve the byte-identical included [Impact license](sfx/combat/kenney_impact-sounds/License.txt) and [RPG license](sfx/combat/kenney_rpg-audio/License.txt). JaggedStone supplies no standalone license file; the original submission explicitly marks CC0 and says attribution is unnecessary. Exact publisher attachment URLs and hash matches are retained in [phase2_provenance.json](phase2_provenance.json).

Safe additions to public GitHub: **all 190 verified supplied Phase 2 pack files** (184 OGGs plus six license/shortcut files), **16 derived PCM WAVs** under runtime/sfx, these credits/licenses and the two Phase 2 provenance/conversion manifests. Original Phase 1 assets remain safe and unchanged. There are no unverified supplied Phase 2 files. There are no local magical_2/4/5/6/7 files; those are not dependencies and were not downloaded or invented.

The deny-by-default audio policy is retained; only exact verified paths were appended to .gitignore. Unknown future downloads remain ignored. Hash tests also guard all current originals and WAVs. Nothing was committed or pushed.
