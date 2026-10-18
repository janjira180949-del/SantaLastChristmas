import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;

/** Authenticate only the crash-fix bytes before running unchanged historical checks. */
final class PngCrashPreservationSupport {
    static byte[] previous(String path,byte[] current) {
        try {
            current=Phase105PreservationSupport.previous(path,current);
            var allowed=new Properties();
            try(var in=Files.newInputStream(Path.of("docs/PNG_CRASH_FIX_AUTHORIZED.properties"))){allowed.load(in);}
            String expected=allowed.getProperty(path);if(expected==null)return current;
            try(var zip=new ZipFile("docs/PNG_CRASH_FIX_PREVIOUS_SOURCES.zip")) {
                byte[] before=zip.getInputStream(zip.getEntry(path)).readAllBytes();
                String prior=ForestPreservationSupport.hash(before);
                // The pre-existing Game import reorder is unchanged by this fix.
                String inventory=path.equals("src/game/Game.java")?"docs/PHASE10_3_BASELINE.json":"docs/PNG_CRASH_FIX_BASELINE.json";
                if(!Files.readString(Path.of(inventory)).contains("\""+path+"\": \""+prior+"\""))throw new AssertionError("Unauthenticated PNG recovery "+path);
                String actual=ForestPreservationSupport.hash(current);
                if(actual.equals(prior))return current;
                if(!actual.equals(expected))throw new AssertionError("Unapproved PNG crash-fix change "+path);
                return before;
            }
        }catch(Exception e){throw new AssertionError(e);}
    }
}
