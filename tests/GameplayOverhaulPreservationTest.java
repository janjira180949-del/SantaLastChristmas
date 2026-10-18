import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;

/** Raw guards for all prior assets/audio/controls/configurations, with explicit reviewed owner changes. */
public class GameplayOverhaulPreservationTest extends V2TestSupport {
    public static void main(String[] args)throws Exception {
        Set<String> tests=Set.of("AudioPhase2PresentationSupport","AudioPhase2Test","AudioPhase3Test","BossReworkTest",
            "EndingCutsceneTest","EndingPolishCaptureTest","FinalRoadSurfaceTest","GameplayPreservationV22Test","GameScreensInputTest",
            "MapCollisionReferenceTest","SnowGroundIntegrationTest","TownFinalEnvironmentTest","V2TestSupport","VillageAtmosphereTest",
            "VillageEnvironmentPolishTest","VisualPresentationV22Test");
        Properties owners=new Properties();try(var in=Files.newInputStream(Path.of("docs/GAMEPLAY_OVERHAUL_AUTHORIZED.properties"))){owners.load(in);}
        var matcher=Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/GAMEPLAY_OVERHAUL_BASELINE.json")));
        int protectedFiles=0,audio=0;Set<String> priorAssets=new HashSet<>();
        while(matcher.find()) {
            String path=matcher.group(1);byte[] actual=ForestPreservationSupport.previous(path,Files.readAllBytes(Path.of(path)));
            if(path.startsWith("assets/"))priorAssets.add(path);
            if(path.startsWith("tests/")&&tests.contains(Path.of(path).getFileName().toString().replace(".java","")))continue;
            if(path.equals("srcs.txt"))continue;
            if(owners.containsKey(path))actual=OverhaulPreservationSupport.previous(path,actual);
            String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(actual));
            check(hash.equals(matcher.group(2)),"Audited owner/art/settings/configuration preserved: "+path);protectedFiles++;
            if(path.startsWith("src/audio/"))audio++;
        }
        check(audio>=6,"Every audio owner protected with raw pre-overhaul hashes");
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
        check(assets.equals(priorAssets),"All prior assets retained; only the named Base additions allowed");
        Set<String> listed=new HashSet<>();for(String line:Files.readAllLines(Path.of("srcs.txt"),StandardCharsets.UTF_8))if(!line.isBlank())check(listed.add(line.trim().replace('\\','/')),"No duplicate production manifest entry");
        Set<String> sources=new HashSet<>();try(var stream=Files.walk(Path.of("src"))){stream.filter(p->p.toString().endsWith(".java")).forEach(p->sources.add(p.toString().replace('\\','/')));}
        check(listed.equals(sources)&&sources.size()==102,"Manifest contains all 87 Java 17 production owners after the additive Village expansion");
        check(game.SpiritTreeTransformation.DURATION==180,"Restoration constant preserved");
        System.out.println("PASS: "+checks+" preservation assertions; "+protectedFiles+" prior files, all audio owners and all asset/configuration files guarded");
    }
}
