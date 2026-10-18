import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;

/** Exact approved Step 7.1 bytes project to the authenticated, completed Step 6.2 sources. */
final class VillageActivitiesPreservationSupport {
    private VillageActivitiesPreservationSupport(){ }
    static byte[] previous(String path,byte[] current){
        current=VillageIntegrationPreservationSupport.previous(path,current);
        try {
            Properties allowed=new Properties();try(var in=Files.newInputStream(Path.of("docs/VILLAGE_ACTIVITIES_AUTHORIZED.properties"))){allowed.load(in);}
            String expected=allowed.getProperty(path);if(expected==null)return current;
            String actual=ForestPreservationSupport.hash(current);
            for(String prefix:List.of("VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")){
                try(ZipFile archive=new ZipFile("docs/"+prefix+"_PREVIOUS_SOURCES.zip")){
                    var entry=archive.getEntry(path);if(entry==null)continue;
                    byte[] bytes=archive.getInputStream(entry).readAllBytes();String hash=ForestPreservationSupport.hash(bytes);
                    if(!Files.readString(Path.of("docs/"+prefix+"_BASELINE.json")).contains("\""+path+"\": \""+hash+"\""))throw new AssertionError("Unauthenticated previous owner: "+path);
                    if(actual.equals(hash))return current;
                }
            }
            if(!actual.equals(expected))throw new AssertionError("Unreviewed Village activity change: "+path);
            try(ZipFile archive=new ZipFile("docs/VILLAGE_ACTIVITIES_PREVIOUS_SOURCES.zip")){
                var entry=archive.getEntry(path);if(entry==null)throw new AssertionError("Missing prior owner "+path);
                return archive.getInputStream(entry).readAllBytes();
            }
        }catch(Exception error){throw new AssertionError(error);}
    }
    static String villageHash()throws Exception {
        Properties hashes=new Properties();try(var in=Files.newInputStream(Path.of("docs/VILLAGE_ACTIVITIES_RENDER_BASELINE.properties"))){hashes.load(in);}
        return FinalChapterPreservationSupport.renderHash("village-before-chapter");
    }
}
