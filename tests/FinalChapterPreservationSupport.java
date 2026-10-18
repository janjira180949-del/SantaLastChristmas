import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;

/** Authenticate only enumerated chapter edits, retaining every earlier snapshot. */
final class FinalChapterPreservationSupport {
    static final Set<String> ASSETS=Set.of("assets/world/final_chapter/materials_atlas.png","assets/world/final_chapter/repaired_house.png","assets/world/final_chapter/README.md");
    static final String REMOVED="src/game/VillagePuzzles.java";
    private FinalChapterPreservationSupport(){ }
    static byte[] readCurrent(String path)throws Exception{
        if(path.equals(REMOVED)&&!Files.exists(Path.of(path)))return archived(path);
        return Files.readAllBytes(Path.of(path));
    }
    private static byte[] archived(String path)throws Exception{
        try(ZipFile archive=new ZipFile("docs/FINAL_CHAPTER_PREVIOUS_SOURCES.zip")){
            var entry=archive.getEntry(path);if(entry==null)throw new AssertionError("Missing chapter prior owner: "+path);
            byte[] bytes=archive.getInputStream(entry).readAllBytes();String hash=ForestPreservationSupport.hash(bytes);
            if(!Files.readString(Path.of("docs/FINAL_CHAPTER_BASELINE.json")).contains("\""+path+"\": \""+hash+"\""))throw new AssertionError("Unauthenticated chapter prior owner: "+path);
            return bytes;
        }
    }
    static byte[] previous(String path,byte[] current){
        current=JourneyHomePreservationSupport.previous(path,current);
        try{
            Properties allowed=new Properties();try(var in=Files.newInputStream(Path.of("docs/FINAL_CHAPTER_AUTHORIZED.properties"))){allowed.load(in);}
            String expected=allowed.getProperty(path);if(expected==null)return current;
            byte[] before=archived(path);String actual=ForestPreservationSupport.hash(current);
            if(actual.equals(ForestPreservationSupport.hash(before)))return current;
            for(String prefix:List.of("VILLAGE_INTEGRATION","VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL"))try(ZipFile archive=new ZipFile("docs/"+prefix+"_PREVIOUS_SOURCES.zip")){
                var entry=archive.getEntry(path);if(entry==null)continue;byte[] older=archive.getInputStream(entry).readAllBytes();String hash=ForestPreservationSupport.hash(older);
                if(!Files.readString(Path.of("docs/"+prefix+"_BASELINE.json")).contains("\""+path+"\": \""+hash+"\""))throw new AssertionError("Unauthenticated older chapter projection: "+path);
                if(actual.equals(hash))return current;
            }
            if(!actual.equals(expected))throw new AssertionError("Unreviewed final chapter edit: "+path);
            return before;
        }catch(Exception error){throw new AssertionError(error);}
    }
    static String renderHash(String key)throws Exception{
        Properties p=new Properties();try(var in=Files.newInputStream(Path.of("docs/FINAL_CHAPTER_RENDER.properties"))){p.load(in);}return p.getProperty(key);
    }
}
