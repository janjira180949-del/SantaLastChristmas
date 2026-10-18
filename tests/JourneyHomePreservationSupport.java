import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;

/** Authenticate exactly the Phase 8 revisions before applying any historical projection. */
final class JourneyHomePreservationSupport {
    static final Set<String> ASSETS=Set.of("assets/world/journey_home/sleigh.png","assets/world/journey_home/town_citizens.png","assets/world/journey_home/README.md");
    private JourneyHomePreservationSupport(){ }
    static byte[] previous(String path,byte[] current){
        current=QuestFlowPreservationSupport.previous(path,current);
        try{
            Properties allowed=new Properties();try(var in=Files.newInputStream(Path.of("docs/JOURNEY_HOME_AUTHORIZED.properties"))){allowed.load(in);}
            String expected=allowed.getProperty(path);if(expected==null)return current;
            byte[] before=archived(path,"JOURNEY_HOME");String actual=ForestPreservationSupport.hash(current);
            if(actual.equals(ForestPreservationSupport.hash(before)))return current;
            for(String prefix:List.of("FINAL_CHAPTER","VILLAGE_INTEGRATION","VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")){
                byte[] older=archived(path,prefix);if(older!=null&&actual.equals(ForestPreservationSupport.hash(older)))return current;
            }
            if(!actual.equals(expected))throw new AssertionError("Unreviewed Journey Home owner: "+path);
            return before;
        }catch(Exception failure){throw new AssertionError(failure);}
    }
    static byte[] archived(String path,String prefix)throws Exception{
        try(ZipFile zip=new ZipFile("docs/"+prefix+"_PREVIOUS_SOURCES.zip")){
            var entry=zip.getEntry(path);if(entry==null)return null;byte[] data=zip.getInputStream(entry).readAllBytes();String hash=ForestPreservationSupport.hash(data);
            if(!Files.readString(Path.of("docs/"+prefix+"_BASELINE.json")).contains("\""+path+"\": \""+hash+"\""))throw new AssertionError("Unauthenticated projection: "+path);
            return data;
        }
    }
    static String renderHash(String key)throws Exception{
        Properties values=new Properties();try(var in=Files.newInputStream(Path.of("docs/JOURNEY_HOME_RENDER.properties"))){values.load(in);}return values.getProperty(key);
    }
}
