import game.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

/** Revision adds exactly two markers; every unrelated source/asset/guard remains byte protected. */
public class VillageIntegrationPreservationTest extends V2TestSupport {
    public static void main(String[]args)throws Exception{
        Properties allowed=new Properties();try(var in=Files.newInputStream(Path.of("docs/VILLAGE_INTEGRATION_AUTHORIZED.properties"))){allowed.load(in);}
        check(allowed.size()==29,"Exactly 29 documented implementation, asset-audit, clearance and behavioral-test owners");
        var matches=Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/VILLAGE_INTEGRATION_BASELINE.json")));int count=0;
        while(matches.find()){
            String path=matches.group(1);byte[] current=FinalChapterPreservationSupport.previous(path,FinalChapterPreservationSupport.readCurrent(path));
            if(allowed.containsKey(path))check(ForestPreservationSupport.hash(current).equals(allowed.getProperty(path)),"Exact reviewed current bytes: "+path);
            byte[] previous=VillageIntegrationPreservationSupport.previous(path,current);
            check(ForestPreservationSupport.hash(previous).equals(matches.group(2)),"Protected prior source/asset/guard/config: "+path);count++;
        }
        Properties additions=new Properties();try(var in=Files.newInputStream(Path.of("docs/VILLAGE_INTEGRATION_ASSETS.properties"))){additions.load(in);}
        check(additions.stringPropertyNames().equals(VillageIntegrationPreservationSupport.ASSETS),"Exact three new asset files");
        for(String path:additions.stringPropertyNames())check(ForestPreservationSupport.hash(Files.readAllBytes(Path.of(path))).equals(additions.getProperty(path)),"Reviewed new asset: "+path);
        Properties geometry=new Properties();try(var in=Files.newInputStream(Path.of("docs/VILLAGE_ACTIVITIES_RENDER_BASELINE.properties"))){geometry.load(in);}
        for(Area.Type type:Area.Type.values()){
            var map=WorldMap.forArea(type);String projected=VillageIntegrationPreservationSupport.geometryBefore(map);
            check(ForestPreservationSupport.hash(projected.getBytes(java.nio.charset.StandardCharsets.UTF_8)).equals(geometry.getProperty(type.name())),"All dimensions, solids, regions and prior markers unchanged: "+type);
        }
        check(!WorldMap.forArea(Area.Type.VILLAGE).points().containsKey("exit/to_santa_base"),"Phase 8 removes only the Village return exit");
        check(WorldMap.forArea(Area.Type.SANTA_BASE).point("entry/from_village").equals(new WorldMap.Point(816,1040)),"Authored arrival marker");
        check(Arrays.stream(VillageActivities.Site.values()).noneMatch(s->s.name().equals("HEATER")),"No heater interaction registration");
        check(Arrays.stream(ui.VillageActivityUI.Mode.values()).noneMatch(s->s.name().equals("HEATER")),"No heater UI registration");
        check(!Files.exists(Path.of("src/game/VillagePuzzles.java")),"Pipe puzzle model removed");
        check(Arrays.stream(VillageActivities.class.getDeclaredFields()).noneMatch(f->f.getName().equals("heater")||f.getName().equals("pipes")),"Heater state removed");
        check(new EncounterManager().getEncounters().size()==14&&SpiritTreeTransformation.DURATION==180,"Population and restoration clock preserved");
        // A new reviewed full-frame hash cannot hide changes outside the actual present group.
        SantaBaseVisuals.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS);
        GamePanel p=SantaBaseNavigationTest.opening();p.setSize(1600,1280);var base=SantaSkillsPreservationSupport.frame(3,render(p));p.stopGame();
        var before=javax.imageio.ImageIO.read(Path.of("tests/fixtures/village-integration-base-before.png").toFile());
        Properties old=new Properties();try(var in=Files.newInputStream(Path.of("docs/SANTA_BASE_STORY_RENDER_BASELINE.properties"))){old.load(in);}
        check(ForestPreservationTest.pixelHash(before).equals(old.getProperty("base-overview")),"Prior Base renderer fixture authenticated by unchanged historical hash");
        int changed=0,outside=0;var storage=new java.awt.Rectangle(1256,278,144,65);
        for(int y=0;y<1280;y++)for(int x=0;x<1600;x++)if(base.getRGB(x,y)!=before.getRGB(x,y)){changed++;if(!storage.contains(x,y)&&!new java.awt.Rectangle(14,1086,280,180).contains(x,y))outside++;}
        check(changed>100&&outside==0,"Base changes confined to presents and the authorized opening objective; outside="+outside);
        var priorVillage=javax.imageio.ImageIO.read(Path.of("tests/fixtures/village-integration-village-before.png").toFile());
        Properties prior=new Properties();try(var in=Files.newInputStream(Path.of("docs/VILLAGE_ACTIVITIES_RENDER_BASELINE.properties"))){prior.load(in);}
        check(ForestPreservationTest.pixelHash(priorVillage).equals(prior.getProperty("village-after")),"Pre-revision Village fixture retains exact prior activity renderer hash");
        Path empty=Path.of("build/village-integration/missing-art").toAbsolutePath();Files.createDirectories(empty);
        String classpath=Arrays.stream(System.getProperty("java.class.path").split(java.io.File.pathSeparator)).map(s->Path.of(s).toAbsolutePath().toString()).collect(java.util.stream.Collectors.joining(java.io.File.pathSeparator));
        var process=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java.exe").toString(),"-Djava.awt.headless=true","-cp",classpath,"VillageIntegrationFallbackProbe").directory(empty.toFile()).redirectErrorStream(true).start();
        check(process.waitFor(30,java.util.concurrent.TimeUnit.SECONDS),"Missing-art probe finishes within watchdog");String output=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        check(process.exitValue()==0&&output.contains("PASS:"),"Actual missing-file worker and native fallback: "+output);
        System.out.println("PASS: "+checks+" revision preservation assertions across "+count+" completed files");
    }
}
