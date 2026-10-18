# External runtime assets

This is a **source-only repository**, not a self-contained playable game download. The `assets/` directory here contains textual metadata, licenses and provenance records. It contains no PNG/GIF images, WAV/OGG/MP3 recordings or original media archives.

## Restore a playable checkout

Copy the **complete** `assets/` and `SantaLastChristmas_MapPackage/` folders from the separately submitted portable release into the repository root. Preserve their names, relative paths and original bytes. Then run `build.ps1` and `run.ps1` from the repository root.

Required content includes Santa/NPC/enemy/boss sprites, animation sheets, area maps, GUI and dialogue artwork, village/forest/restoration resources, Spirit Tree and ending/homecoming artwork, menu/area/boss music and sound effects. Optional, boss and ending resources must remain included even if they are not visible immediately after Start.

[RUNTIME_RESOURCES.csv](RUNTIME_RESOURCES.csv) lists 209 traced runtime image paths and 152 distinct final AudioCatalog audio paths with sizes and SHA-256 hashes. It is a deployment checklist, not permission to redistribute an asset. The complete portable trees provide additional retained originals and provenance records.

## Size and publication decision

The portable release contains 877,195,244 bytes of copied resource/credit files. Inspection of the original two resource trees found **no individual file above 100 MiB**. File-size limits alone do not force this publication to exclude every asset. GitHub documents its ordinary [large-file limits](https://docs.github.com/en/repositories/working-with-files/managing-large-files/about-large-files-on-github) and [repository/push limits](https://docs.github.com/en/repositories/creating-and-managing-repositories/repository-limits).

Some manifests describe OpenAI-generated exports, while others identify user-supplied reference artwork and extracted pixels without an explicit public redistribution license. A general in-game art credit is not treated as proof of a redistribution grant for every supplied reference. The full visual collection therefore does not have a conclusively documented public redistribution basis in the reviewed records. The user authorized source-only publication in that case; the portable game will be submitted separately to the instructor.

To keep that publication consistent and avoid an incomplete large binary distribution, **all runtime media binaries, including the separately credited audio, remain in the portable submission**. Existing license, creator, source URL, output hash and component records are retained as text. No original asset was deleted, altered or re-encoded.

## License/provenance records

- [Current runtime audio credits](FINAL_STORY_AUDIO_CREDITS.md)
- [Audio licenses](../assets/audio/LICENSES.md) and [credits](../assets/audio/AUDIO_CREDITS.md)
- [Phase 10.6 licenses/sources](../assets/audio/phase10_6/LICENSES_AND_SOURCES.md), [license evidence](../assets/audio/phase10_6/LICENSE_EVIDENCE.json), [source components](../assets/audio/phase10_6/SOURCE_COMPONENTS.json), and [audio manifest](../assets/audio/phase10_6/AUDIO_MANIFEST.csv)
- Original Kenney license texts and other retained manifest/license files under `assets/`

Two public conversion-metadata copies omit `ffmpeg_input` console-output fields that contained private workstation paths. Their source/output paths, hashes, processing details and attribution fields remain. The original project documents are unchanged. Raw publisher HTML snapshots, obsolete archives, downloaded pack binaries and local extraction/debug outputs are not republished; source/license URLs and structured evidence remain available.

No Git LFS, paid service or billing activation is used. Public source availability does not grant a project-wide license or permission to republish the separately supplied artwork.
