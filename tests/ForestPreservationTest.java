import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/** Raw current-baseline guards for every completed game owner, asset, reference and launch script. */
public class ForestPreservationTest extends V2TestSupport {
    static String pixelHash(java.awt.image.BufferedImage image)throws Exception {
        var digest=java.security.MessageDigest.getInstance("SHA-256");
        for(int c:image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth())) {
            digest.update((byte)(c>>>24));digest.update((byte)(c>>>16));digest.update((byte)(c>>>8));digest.update((byte)c);
        }
        return java.util.HexFormat.of().formatHex(digest.digest());
    }
    public static void main(String[] args)throws Exception {
        Set<String> permitted=Set.of("src/game/GamePanel.java","src/game/EncounterManager.java","src/quest/StoryQuestTarget.java","tests/V2TestSupport.java",
            "tests/OverhaulPreservationSupport.java","tests/GameplayOverhaulPreservationTest.java","tests/GameplayOverhaulEncounterTest.java");
        Properties changes=new Properties();try(var in=Files.newInputStream(Path.of("docs/FOREST_OVERHAUL_AUTHORIZED.properties"))){changes.load(in);}
        check(changes.stringPropertyNames().equals(permitted),"Integration/test authorization has an exact seven-owner scope");
        var matcher=Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/FOREST_OVERHAUL_BASELINE.json")));
        int count=0;Set<String> originalAssets=new HashSet<>();
        while(matcher.find()) {
            String path=matcher.group(1);if(path.startsWith("assets/"))originalAssets.add(path);
            if(path.equals("srcs.txt"))continue;
            byte[] current=SantaBasePreservationSupport.previous(path,Files.readAllBytes(Path.of(path)));
            if(permitted.contains(path))current=ForestPreservationSupport.previous(path,current);
            check(ForestPreservationSupport.hash(current).equals(matcher.group(2)),"Original game/reference bytes preserved: "+path);count++;
        }
        Set<String> assets=new HashSet<>();try(var stream=Files.walk(Path.of("assets"))){stream.filter(Files::isRegularFile).forEach(p->assets.add(p.toString().replace('\\','/')));}
        assets.removeAll(SantaBasePreservationSupport.newAssets());
        assets.removeAll(VillageIntegrationPreservationSupport.ASSETS);
        assets.removeAll(FinalChapterPreservationSupport.ASSETS);
        assets.removeAll(JourneyHomePreservationSupport.ASSETS);
        assets.removeAll(Phase92PreservationSupport.ASSETS);
        assets.removeAll(Phase931PreservationSupport.ASSETS);
        assets.remove("assets/references/santa_skills_reference.png");
        assets.removeAll(Phase10PreservationSupport.assets());
        assets.removeAll(Phase106PreservationSupport.assets());
        assets.removeAll(Phase106CPreservationSupport.assets());
        assets.removeAll(TownFinalFixPreservationSupport.assets());
        assets.removeAll(Phase2UIPreservationSupport.assets());
        assets.removeAll(Phase3PreservationSupport.assets());
        assets.removeAll(Phase4PreservationSupport.assets());
        assets.removeAll(PostPhase4Support.assets());
        check(assets.equals(originalAssets),"Every existing asset remains; only the named Base additions allowed");
        Set<String> sources=new HashSet<>();try(var stream=Files.walk(Path.of("src"))){stream.filter(p->p.toString().endsWith(".java")).forEach(p->sources.add(p.toString().replace('\\','/')));}
        Set<String> manifest=new HashSet<>();for(String line:Files.readAllLines(Path.of("srcs.txt"),StandardCharsets.UTF_8))if(!line.isBlank())check(manifest.add(line.trim().replace('\\','/')),"Unique source manifest entry");
        check(sources.equals(manifest)&&sources.size()==102,"All 89 production owners compile from the IDE manifest, including the five optional activity owners");
        check(game.SpiritTreeTransformation.DURATION==180,"Restoration duration unchanged");
        Properties renders=new Properties();try(var in=Files.newInputStream(Path.of("docs/FOREST_OVERHAUL_RENDER_BASELINE.properties"))){renders.load(in);}
        for(int area:new int[]{0,2}) {
            String name=area==0?"village":"town";
            var current=FinalEnvironmentReview.scene(area,1296,area==0?1160:1136,1270,720);
            check(pixelHash(current).equals((area==0?VillageActivitiesPreservationSupport.villageHash():renders.getProperty(name))),"Actual "+name+" renderer pixel-identical to completed baseline");
        }
        System.out.println("PASS: "+checks+" strict Forest preservation assertions across "+count+" baseline files");
    }
}
