import game.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

/** All completed Base terrain/geometry/controls/adventure/audio remain authenticated. */
public class SantaBaseStoryPreservationTest extends V2TestSupport {
    public static void main(String[]args)throws Exception {
        Properties changes=new Properties();try(var in=Files.newInputStream(Path.of("docs/SANTA_BASE_STORY_AUTHORIZED.properties"))){changes.load(in);}
        Set<String> expected=Set.of("src/game/GamePanel.java","src/ui/DialogueManager.java","src/ui/DialogueBox.java","srcs.txt","README.md",
            "tests/SantaBasePreservationSupport.java","tests/ForestPreservationTest.java","tests/GameplayOverhaulPreservationTest.java");
        check(changes.stringPropertyNames().equals(expected),"Exact eight shared integration/document/test owners");
        var matcher=Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/SANTA_BASE_STORY_BASELINE.json")));int count=0;
        while(matcher.find()) {
            String path=matcher.group(1);byte[] original=SantaBaseStoryPreservationSupport.previous(path,Files.readAllBytes(Path.of(path)));
            check(ForestPreservationSupport.hash(original).equals(matcher.group(2)),"Completed owner/asset/guard unchanged outside narrow narrative integration: "+path);count++;
        }
        Properties rendering=new Properties();try(var in=Files.newInputStream(Path.of("docs/SANTA_BASE_STORY_RENDER_BASELINE.properties"))){rendering.load(in);}
        for(Area.Type type:Area.Type.values()) {
            WorldMap map=WorldMap.forArea(type);String value=VillageIntegrationPreservationSupport.geometryBefore(map);
            check(ForestPreservationSupport.hash(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)).equals(rendering.getProperty(type.name())),"Actual map dimensions/markers/solids/regions preserved: "+type);
        }
        SantaBaseVisuals.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS);GamePanel p=SantaBaseNavigationTest.opening();p.setSize(1600,1280);
        check(ForestPreservationTest.pixelHash(QuestFlowPreservationSupport.historicalFrame(3,render(p))).equals(JourneyHomePreservationSupport.renderHash("base-overview")),"Base renderer matches the authenticated Phase 8 present prerequisite presentation");p.stopGame();
        Set<String> listed=new HashSet<>();for(String line:Files.readAllLines(Path.of("srcs.txt")))if(!line.isBlank())check(listed.add(line.trim()),"Unique source manifest entry");
        Set<String> sources=new HashSet<>();try(var stream=Files.walk(Path.of("src"))){stream.filter(f->f.toString().endsWith(".java")).forEach(f->sources.add(f.toString().replace('\\','/')));}
        check(listed.equals(sources)&&listed.size()==102,"IDE manifest retains the narrative owner and includes the five activity owners");
        check(new EncounterManager().getEncounters().size()==14&&SpiritTreeTransformation.DURATION==180,"No encounter or restoration clock change");
        System.out.println("PASS: "+checks+" story preservation assertions across "+count+" completed files and four actual maps");
    }
}
