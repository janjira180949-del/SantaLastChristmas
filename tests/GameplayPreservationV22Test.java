import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;

/** Byte-for-byte protection of gameplay owners, geometry and approved Village systems. */
public class GameplayPreservationV22Test extends V2TestSupport {
    public static void main(String[] args)throws Exception {
        try(ZipFile baseline=new ZipFile("build/v2.2-baseline/before-visual-overhaul.zip")) {
            for(String path:List.of("src/quest/StoryQuestManager.java","src/quest/StoryQuestTarget.java","src/quest/StoryQuest.java",
                    "src/game/EncounterManager.java","src/game/Encounter.java","src/game/AreaManager.java","src/game/Area.java",
                    "src/game/CombatSystem.java","src/game/HealingPoint.java","src/game/WorldMap.java","src/game/WorldMapData.java",
                    "src/game/VillageEnvironment.java","src/game/VillageAtmosphere.java","src/game/ApprovedSnowGround.java",
                    "src/game/ExistingSnowGround.java","src/game/PackedSnowPath.java","src/game/RoadSnowTexture.java",
                    "src/util/Collision.java","src/util/ImageAssets.java","src/spirit/SpiritTree.java","src/ui/DialogueManager.java",
                    "src/ui/Dialogue.java","src/ui/DialogueLine.java","src/game/EndingCredits.java")) {
                var entry=baseline.getEntry(path);check(entry!=null,"Required V2.1 source in baseline: "+path);
                check(Arrays.equals(OverhaulPreservationSupport.previous(path,Files.readAllBytes(Path.of(path))),baseline.getInputStream(entry).readAllBytes()),"Gameplay/geometry/art loader unchanged: "+path);
            }
        }
        System.out.println("PASS: "+checks+" baseline assertions; 24 historical gameplay, geometry, loader and credit owners verified; authorized overhaul owners checked against exact before/after guards");
    }
}
