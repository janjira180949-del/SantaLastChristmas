# OOP and GUI architecture

The source contains 102 current production Java files. The application entry point is `game.Game`; it creates a Swing JFrame and GamePanel on the event-dispatch thread. GamePanel implements Runnable and KeyListener and coordinates the existing 60 Hz game loop, game state, input queues, Update and Render work.

## Object-oriented concepts in the current code

| Concept | Concrete example |
| --- | --- |
| Constructors | Santa, Enemy subclasses, GamePanel and their collaborating managers initialize object state and dependencies through constructors. |
| Encapsulation | GamePanel and AudioManager keep internal state in private fields and expose selected operations/query methods. |
| Composition | GamePanel owns Santa, AreaManager, StoryQuestManager, presentation systems and AudioManager; those objects collaborate within a game session. |
| Inheritance | The abstract Enemy class is extended by Walker, Runner, ChristmasEater, DarkTitan and MiniFrostFang. FrostFang extends ChristmasEater. |
| Abstraction | Enemy defines the shared enemy abstraction; BossCombatant defines a contract for boss combat behavior. |
| Polymorphism | Concrete Enemy implementations participate through the shared Enemy type, and FrostFang/DarkTitan implement the BossCombatant interface. |

## GUI and Event Handling

`game.Game` creates the JFrame and installs a window-closing listener. `game.GamePanel` extends JPanel, draws through Java2D, implements keyboard handling, and installs mouse listeners. Menus, dialogue, HUD and ability/quest panels are implemented under `src/ui`. Existing input is coordinated with game states such as MAIN_MENU, PLAYING, PAUSED and GAME_OVER.

## Important systems

- `CombatSystem`, `AbilitySystem`, `SantaSkills` and `BossAttacks` coordinate attacks, abilities, cooldowns and effects.
- `Encounter`, `EncounterManager` and `EncounterSpawnPolicy` control authored encounters and spawn placement.
- `AreaManager`, `WorldMap`, `WorldMapData` and `Collision` provide transitions, authored geometry and collision helpers.
- `StoryQuestManager`, `VillageActivities`, `FinalChapter` and dialogue classes coordinate current progression and interactions.
- `AudioCatalog` maps final music/ambience/effect/cue routes; `AudioManager` asynchronously decodes/mixes audio using `AudioResources` and Java Sound.
- `ImageAssets` first resolves classpath artwork, then supported filesystem locations; selected visual systems also use direct relative file reads.
- `EndingCutscene` and `EndingCredits` implement the current ending/homecoming and credit presentation.

This document describes the existing implementation. It does not add APIs or alter Class relationships. Runtime media and historical test fixtures are external; see ASSETS.md and TESTING.md.
