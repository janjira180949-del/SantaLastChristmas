# Current game audio runtime

Phase10.6B integrated the CC0 library; Phase10.6C replaces six boss combat routes and two fall accents. AUDIO_MANIFEST.csv maps 201 events to 106 WAVs: 21 music recordings including the original menu, 76 SFX and 9 ambience. Source-component records cover the 105 new mappings; MENU_LICENSE.json preserves the sole legacy exception unchanged. Superseded unreferenced WAVs remain historical assets, not fallbacks.

Fang uses related 16-bar Battle Theme A sections by cynicmusic (26.667s each); Titan uses Epic Fight Theme/Boss Battle sections by Tarush Singhal (34.286s each). Both are CC0. They use the existing -23dBFS RMS ceiling, not increased playback volume. Analysis supports stronger rhythm and different orchestration across sections; subjective suitability and musical loop quality are not human-approved. Three-repeat previews and source originals are under build/phase10-6c/sources/.

The six loops have a complementary 200ms circular join and a 128-sample endpoint de-click. These technical operations cannot certify musical phrasing. Dedicated Fang/Titan impact accents layer only newly licensed Phase10.6B creature/stone recordings, never rejected historical audio. Source HTML and rights notes are distributed in licenses/phase10_6c/. Full credits and component hashes accompany this directory.

The runtime retains the established Java Sound mixer, saved master/music/SFX/mute preferences, 128MiB PCM budget and eight SFX voices. Phase crossfades are 500ms. During a fall, the current phrase fades to 28% under the accent; at fall frame90 the next story bed begins a 750ms crossfade. Impacts follow actual grounded poses at Fang29/Titan57 on the unchanged 120-update fall clock. Normal completion preserves the sound tail; skip/travel/reset cancels stale presentation audio.

No direct listening, human artistic approval or headphone/speaker mix approval is claimed. Device writes and waveform tests demonstrate technical output only.

Town departure audio: sleigh bells by soundstack (https://freesound.org/people/soundstack/sounds/255104/) and Santa-style laughter by canucklovingbrit (https://freesound.org/people/canucklovingbrit/sounds/110642/), both CC0 1.0 Universal. Official public HQ previews were excerpted, attenuated and converted to 44,100 Hz, 16-bit stereo PCM. No specific actor imitation. Human listening approval remains pending. Source hashes, recipes and primary license pages accompany the runtime manifest.
