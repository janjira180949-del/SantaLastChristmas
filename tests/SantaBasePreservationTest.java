import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import game.*;

/** Completed Forest/game/audio/reference baseline stays exact outside reviewed Base integration. */
public class SantaBasePreservationTest extends V2TestSupport {
    public static void main(String[]args)throws Exception {
        Properties allowed=new Properties();try(var in=Files.newInputStream(Path.of("docs/SANTA_BASE_AUTHORIZED.properties"))){allowed.load(in);}
        check(allowed.size()==29,"Exact enumerated Base integration/test migration scope");
        Properties additions=new Properties();try(var in=Files.newInputStream(Path.of("docs/SANTA_BASE_NEW_ASSETS.properties"))){additions.load(in);}
        check(additions.stringPropertyNames().equals(SantaBasePreservationSupport.newAssets()),"Only the three named Base asset files are introduced");
        for(String path:additions.stringPropertyNames())check(ForestPreservationSupport.hash(Files.readAllBytes(Path.of(path))).equals(additions.getProperty(path)),"Reviewed new Base asset bytes: "+path);
        var matcher=Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/SANTA_BASE_BASELINE.json")));int count=0;
        while(matcher.find()) {
            String path=matcher.group(1);byte[] original=SantaBasePreservationSupport.previous(path,Files.readAllBytes(Path.of(path)));
            check(ForestPreservationSupport.hash(original).equals(matcher.group(2)),"Completed game/reference/configuration preserved: "+path);count++;
        }
        Properties renders=new Properties();try(var in=Files.newInputStream(Path.of("docs/SANTA_BASE_RENDER_BASELINE.properties"))){renders.load(in);}
        String[] names={"village","forest","town"};for(int i=0;i<3;i++)check(ForestPreservationTest.pixelHash(FinalEnvironmentReview.scene(i,1296,i==0?1160:1136,1270,720)).equals((i==0?VillageActivitiesPreservationSupport.villageHash():renders.getProperty(names[i]))),"Actual completed "+names[i]+" pixels unchanged");
        Properties geometry=new Properties();try(var in=Files.newInputStream(Path.of("docs/SANTA_BASE_GEOMETRY_BASELINE.properties"))){geometry.load(in);}
        for(Area.Type area:new Area.Type[]{Area.Type.VILLAGE,Area.Type.FOREST,Area.Type.CHRISTMAS_TOWN}) {
            WorldMap m=WorldMap.forArea(area);String value=VillageIntegrationPreservationSupport.geometryBefore(m);
            check(ForestPreservationSupport.hash(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)).equals(geometry.getProperty(area.name())),"All actual original markers/collision/regions unchanged: "+area);
        }
        check(new EncounterManager().getEncounters().size()==14,"Encounter populations/groups unchanged");
        check(SpiritTreeTransformation.DURATION==180,"Spirit restoration retains exact clock");
        var panel=SantaBaseNavigationTest.opening();var mixer=(audio.AudioManager)field(panel,"audio");check(mixer.getRequestedMusic()==audio.AudioCatalog.Music.BASE,"Base selects the approved dedicated new Base recording");travelGate(panel,Area.Type.VILLAGE);check(mixer.getRequestedMusic()==audio.AudioCatalog.Music.VILLAGE,"Departure selects the new frozen Village recording");panel.stopGame();
        System.out.println("PASS: "+checks+" Base preservation assertions across "+count+" completed files and three actual render hashes");
    }
}
