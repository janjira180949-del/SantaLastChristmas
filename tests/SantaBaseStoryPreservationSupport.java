import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;

/** Exact Step 6.2 projections precede the existing Step 6.1/Forest/gameplay guards. */
final class SantaBaseStoryPreservationSupport {
    private SantaBaseStoryPreservationSupport() { }
    static byte[] previous(String path,byte[] current) {
        current=VillageActivitiesPreservationSupport.previous(path,current);
        try {
            Properties allowed=new Properties();try(var in=Files.newInputStream(Path.of("docs/SANTA_BASE_STORY_AUTHORIZED.properties"))){allowed.load(in);}
            String expected=allowed.getProperty(path);if(expected==null)return current;
            String actual=ForestPreservationSupport.hash(current);
            try(ZipFile archive=new ZipFile("docs/SANTA_BASE_STORY_PREVIOUS_SOURCES.zip")) {
                var entry=archive.getEntry(path);if(entry==null)throw new AssertionError("Missing pre-story source: "+path);
                byte[] before=archive.getInputStream(entry).readAllBytes();String prior=ForestPreservationSupport.hash(before);
                if(!Files.readString(Path.of("docs/SANTA_BASE_STORY_BASELINE.json")).contains("\""+path+"\": \""+prior+"\""))throw new AssertionError("Unauthenticated pre-story source: "+path);
                if(actual.equals(prior))return current;
                // Layered historical guards can revisit already authenticated earlier bytes.
                for(String prefix:List.of("SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")) {
                    try(ZipFile older=new ZipFile("docs/"+prefix+"_PREVIOUS_SOURCES.zip")) {
                        var previous=older.getEntry(path);if(previous==null)continue;
                        String hash=ForestPreservationSupport.hash(older.getInputStream(previous).readAllBytes());
                        if(actual.equals(hash)&&Files.readString(Path.of("docs/"+prefix+"_BASELINE.json")).contains("\""+path+"\": \""+hash+"\""))return current;
                    }
                }
                if(!actual.equals(expected))throw new AssertionError("Unreviewed workshop narrative change: "+path);
                return before;
            }
        } catch(Exception error){throw new AssertionError(error);}
    }
}
