# SANTA: LAST CHRISTMAS

**Developer:** JANJIRA WANGKAOOM  
**Course:** Object-Oriented Programming  
**Programming language:** Java  
**GUI:** Java Swing / Java 2D

An action-adventure game built around a Java Swing interface, a 60 Hz game loop, object-oriented gameplay systems, sprite animation and asynchronous audio.

> **Source-only university submission:** this repository contains all **102 current production Java source files**, build scripts, current Java test sources and asset attribution/provenance records. Runtime image and audio binaries are distributed separately in the instructor's portable release. **A fresh clone is not independently playable.** See [external asset requirements](docs/ASSETS.md).

## Game overview and story

On Christmas Eve, Santa sets out from the Last Workshop with presents for the Last Village. His delivery becomes a journey through a world affected by Black Frost. Santa helps the village, explores the Frozen Forest, confronts Frost Fang and Dark Titan, and works to restore the Spirit Tree before returning home.

The repository preserves the current finished implementation. It does not introduce a new game version or change gameplay.

## Key features

- Santa movement, collision, cursor-directed attacks, dash and three abilities.
- Sequential story quests, gift deliveries, NPC dialogue and optional conversations.
- Village restoration, forest material collection and crystal encounters.
- Walker and Runner enemies, Frost Fang and Dark Titan boss encounters, and Mini Frost Fang summons.
- World maps, area transitions, sprite animation, GUI/HUD, quest tracking and pause/retry flows.
- Story scenes, Spirit Tree restoration, homecoming, ending and scrolling credits.
- Asynchronous music/SFX decoding, Java Sound output and per-user volume/mute settings.

Current balancing is preserved: `TITAN_SUMMON_INTERVAL = 600` and `MAX_MINI_FANGS = 3`. At the existing 60 Hz update rate, 600 updates represent the current nominal 10-second summon interval.

## Controls

| Action | Input |
| --- | --- |
| Move | WASD / arrow keys |
| Attack toward cursor | Left mouse button |
| Dash | Space |
| Christmas Bell | 1 |
| Magic Star toward cursor | 2 |
| Gift Bomb toward cursor | 3 |
| Interact / advance dialogue | E |
| Pause / resume | Escape |
| Resume from pause | Enter / Resume button |
| Retry after Game Over | R |
| Menu selection | Mouse, or Up/Down and Enter |

Select START to begin at Santa's Base. Press E at Gift Storage to collect the presents before leaving through the Departure Gate.

## Java requirements

- **Compilation target:** Java 17, using `javac --release 17` and UTF-8 source encoding.
- **Actually tested portable-release runtime:** Oracle Java **26.0.1**, Windows 64-bit.
- Java 17 runtime compatibility **has not been tested**. The build target does not certify execution on an older runtime. Other Java versions/vendors and operating systems are not certified here.
- Building requires a JDK with `javac`. Playing the portable release requires a Java runtime. VS Code is optional.
- Production code uses the Java standard library; no external Java libraries are required.

## Build from source

Open PowerShell in the repository root:

```powershell
java -version
javac -version
.\build.ps1
```

The unchanged script discovers all production Java sources, compiles them with `--release 17 -encoding UTF-8 -Xlint:all`, and writes generated classes to `out/`. Binary assets are not needed for compilation.

The existing source manifest also supports:

```powershell
javac --release 17 -encoding UTF-8 -d out '@srcs.txt'
```

The actual main class is **`game.Game`**.

## Run after supplying external assets

Obtain the separately submitted portable release from the instructor/project author. Copy its complete `assets/` and `SantaLastChristmas_MapPackage/` folders into this checkout, preserving every relative path. These include images, maps, sprites, animation sheets, GUI artwork, music, SFX and attribution files. See [the resource checklist](docs/RUNTIME_RESOURCES.csv).

Then, from this repository root:

```powershell
.\run.ps1
```

Or, after building:

```powershell
java -cp out game.Game
```

Run from the repository root: some existing visual systems use direct relative filesystem paths. Missing artwork can produce placeholders and missing audio can produce diagnostics; that is not a complete playable installation.

### Separately submitted executable JAR

The portable folder contains `SantaLastChristmas.jar`, `StartGame.bat`, `assets/`, `SantaLastChristmas_MapPackage/`, launch instructions and license records. Open that folder and run:

```powershell
java -jar SantaLastChristmas.jar
```

Alternatively, double-click its `StartGame.bat`, which selects the correct working directory. **The JAR requires its external asset folders.** Neither the JAR, a bundled Java runtime, nor the portable asset bundle is published in this source repository.

## Project structure

```text
src/
  audio/       Audio catalogs, resources, mixer and event routing
  boss/        Boss contracts, combat, attacks and artwork handling
  entity/      Santa, NPCs, enemies and sprite handling
  game/        Initialization, loop, maps, encounters, story and ending
  item/        Item classes
  quest/       Story quests, objectives and progression
  spirit/      Spirit Tree model
  ui/          Swing/Java2D GUI, HUD, menus and dialogue
  util/        Collision, image loading and presentation utilities
tests/         Current Java test sources; historical fixtures are external
tools/         Existing test runner and metadata-check helpers
assets/        Textual asset metadata, licenses and provenance only
SantaLastChristmas_MapPackage/
  map_data.json  Authored textual map metadata; image resources are external
docs/          Architecture, assets, testing and attribution documentation
build.ps1      Compile production source
run.ps1        Compile and run game.Game
test.ps1       Existing full-development regression entry point
srcs.txt       Existing production source manifest
```

## Main Java classes

| Class / Interface | Responsibility |
| --- | --- |
| `game.Game` | Swing window and application entry point |
| `game.GamePanel` | Game loop, input handling, game states and rendering coordination |
| `entity.Santa` | Player model, movement and combat state |
| `entity.Enemy`, `Walker`, `Runner`, `MiniFrostFang` | Shared enemy model and concrete enemy behavior |
| `boss.BossCombatant`, `FrostFang`, `DarkTitan` | Boss contract and current boss behavior |
| `game.CombatSystem`, `AbilitySystem`, `SantaSkills` | Combat and abilities |
| `game.Encounter`, `EncounterManager`, `EncounterSpawnPolicy` | Encounter progression and spawn rules |
| `quest.StoryQuest`, `StoryQuestManager`, `StoryQuestTarget` | Quest model, state and current objective |
| `game.AreaManager`, `WorldMap`, `WorldMapData` | Area transitions, authored map geometry and world data |
| `ui.DialogueManager`, `DialogueBox`, `HUD`, `CinematicMenu` | Dialogue, HUD and menus |
| `audio.AudioCatalog`, `AudioManager`, `AudioResources` | Audio routes, decoding, mixing and resource access |
| `game.FinalChapter`, `EndingCutscene`, `EndingCredits` | Final chapter, homecoming/ending and credits |
| `util.ImageAssets`, `Collision` | Image decoding and collision helpers |

See [OOP architecture](docs/ARCHITECTURE.md) and the [102-file source inventory](docs/SOURCE_INVENTORY.csv).

## Testing and known limitations

The latest recorded complete original-project regression run was **84/128 entry points passed; 44 failed**. Forty-three failures involve historical preservation guards rejecting the unchanged `Encounter.java` revision; `Phase3GameplayTest` separately fails the mini arena/boss/solid footprint assertion. These failures are not reported as passing or fixed.

The current portable release was separately verified for compilation, actual executable-JAR launch, main menu, gameplay startup, original menu music/audio initialization, and 209 traced image / 152 registered audio resource paths. A moved copy was tested outside the development-project directory. Those launch/resource checks do **not** mean the complete regression suite passed or that a full manual playthrough was performed.

The original test sources and scripts are retained for academic inspection. Their full regression environment also requires historical preservation archives, fixture screenshots and manifests intentionally excluded from this public repository. **Do not expect `test.ps1` in a fresh source-only clone to reproduce 84/128.** See [testing requirements](docs/TESTING.md).

## Credits and licenses

Developed by **JANJIRA WANGKAOOM** for **Object-Oriented Programming**. The current game credits identify programming assistance by OpenAI Codex and artwork/visual assistance by ChatGPT by OpenAI. Full in-game credit entries are preserved in [`EndingCredits.java`](src/game/EndingCredits.java).

See [credits](docs/CREDITS.md), [runtime audio attribution](docs/FINAL_STORY_AUDIO_CREDITS.md), and the included asset license/provenance records. Audio metadata identifies third-party recordings under CC0 1.0; that dedication does not license unrelated project material. Some visual records reference supplied artwork without a complete public redistribution grant, so the binary asset collection is distributed separately for the university submission.

No new project-wide open-source license is granted by this publication. See [license information](LICENSE.md). No Git LFS, paid storage or billing service is configured.
