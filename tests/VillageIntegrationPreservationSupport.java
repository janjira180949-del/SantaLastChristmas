import game.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;

/** Authenticated revision layer; earlier snapshots remain intact and continue to execute. */
final class VillageIntegrationPreservationSupport {
    static final Set<String> ASSETS=Set.of("assets/characters/village/residents_atlas.png","assets/characters/village/activities_props_atlas.png","assets/characters/village/README.md");
    private VillageIntegrationPreservationSupport(){ }
    static byte[] previous(String path,byte[] current){
        current=FinalChapterPreservationSupport.previous(path,current);
        try {
            Properties allowed=new Properties();try(var in=Files.newInputStream(Path.of("docs/VILLAGE_INTEGRATION_AUTHORIZED.properties"))){allowed.load(in);}
            String expected=allowed.getProperty(path);if(expected==null)return current;
            String actual=ForestPreservationSupport.hash(current);
            for(String prefix:List.of("VILLAGE_INTEGRATION","VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")){
                try(ZipFile archive=new ZipFile("docs/"+prefix+"_PREVIOUS_SOURCES.zip")){
                    var entry=archive.getEntry(path);if(entry==null)continue;
                    byte[] before=archive.getInputStream(entry).readAllBytes();String hash=ForestPreservationSupport.hash(before);
                    if(!Files.readString(Path.of("docs/"+prefix+"_BASELINE.json")).contains("\""+path+"\": \""+hash+"\""))throw new AssertionError("Unauthenticated revision source: "+path);
                    if(actual.equals(hash))return current;
                }
            }
            if(!actual.equals(expected))throw new AssertionError("Unreviewed Village revision: "+path);
            try(ZipFile archive=new ZipFile("docs/VILLAGE_INTEGRATION_PREVIOUS_SOURCES.zip")){
                var entry=archive.getEntry(path);if(entry==null)throw new AssertionError("Missing prior revision owner: "+path);
                return archive.getInputStream(entry).readAllBytes();
            }
        }catch(Exception error){throw new AssertionError(error);}
    }
    static String geometryBefore(WorldMap map){
        var points=ForestRefinementPreservationSupport.points(map);
        if(map.id().equals("village")){
            points.remove("exit/to_santa_base");
            if(!points.remove("landmarks/Christmas Tree").equals(new WorldMap.Point(1296,880)))throw new AssertionError("Unreviewed restoration tree anchor");
            if(!points.get("npc/Toby").equals(new WorldMap.Point(528,784)))throw new AssertionError("Unreviewed Toby anchor");
            points.put("npc/Toby",new WorldMap.Point(528,720));
        }
        if(map.id().equals("santa_base"))points.remove("entry/from_village");
        return map.width()+","+map.height()+points+TreeCollisionPreservationSupport.previousSolids(map)+map.combatRegions().toString();
    }
    static String renderHash(String key)throws Exception {
        Properties values=new Properties();try(var in=Files.newInputStream(Path.of("docs/VILLAGE_INTEGRATION_RENDER.properties"))){values.load(in);}return values.getProperty(key);
    }
}
