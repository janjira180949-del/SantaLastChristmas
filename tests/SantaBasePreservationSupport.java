import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;

/** Exact, authenticated Base integration projections; unrelated source/asset edits still fail. */
final class SantaBasePreservationSupport {
    private SantaBasePreservationSupport() { }
    static Set<String> newAssets() {return Set.of("assets/world/santa_base/workshop_props.png","assets/world/santa_base/workshop_materials.png","assets/world/santa_base/README.md");}
    static byte[] previous(String path,byte[] current) {
        current=SantaBaseStoryPreservationSupport.previous(path,current);
        try {
            Properties allowed=new Properties();try(var in=Files.newInputStream(Path.of("docs/SANTA_BASE_AUTHORIZED.properties"))){allowed.load(in);}
            String expected=allowed.getProperty(path);if(expected==null)return current;
            try(ZipFile archive=new ZipFile("docs/SANTA_BASE_PREVIOUS_SOURCES.zip")) {
                var entry=archive.getEntry(path);if(entry==null)throw new AssertionError("Missing Base prior owner: "+path);
                byte[] before=archive.getInputStream(entry).readAllBytes();String prior=ForestPreservationSupport.hash(before);
                if(!Files.readString(Path.of("docs/SANTA_BASE_BASELINE.json")).contains("\""+path+"\": \""+prior+"\""))throw new AssertionError("Unauthenticated Base prior owner: "+path);
                String actual=ForestPreservationSupport.hash(current);
                if(actual.equals(prior))return current;
                // Older guards may apply this adapter again after an authenticated Forest projection.
                try(ZipFile forest=new ZipFile("docs/FOREST_OVERHAUL_PREVIOUS_SOURCES.zip")) {
                    var older=forest.getEntry(path);
                    if(older!=null) {
                        String olderHash=ForestPreservationSupport.hash(forest.getInputStream(older).readAllBytes());
                        if(actual.equals(olderHash)&&Files.readString(Path.of("docs/FOREST_OVERHAUL_BASELINE.json")).contains("\""+path+"\": \""+olderHash+"\""))return current;
                    }
                }
                if(!actual.equals(expected))throw new AssertionError("Unreviewed Base owner change: "+path);
                return before;
            }
        } catch(Exception error){throw new AssertionError(error);}
    }
}
