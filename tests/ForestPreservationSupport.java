import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipFile;

/** Authenticate the precise Forest changes before applying older historical presentation guards. */
final class ForestPreservationSupport {
    private ForestPreservationSupport() { }
    static byte[] previous(String path,byte[] current) {
        current=SantaBasePreservationSupport.previous(path,current);
        try {
            Properties allowed=new Properties();
            try(var in=Files.newInputStream(Path.of("docs/FOREST_OVERHAUL_AUTHORIZED.properties"))){allowed.load(in);}
            String expected=allowed.getProperty(path);if(expected==null)return current;
            try(ZipFile archive=new ZipFile("docs/FOREST_OVERHAUL_PREVIOUS_SOURCES.zip")) {
                var entry=archive.getEntry(path);if(entry==null)throw new AssertionError("Missing Forest prior source: "+path);
                byte[] before=archive.getInputStream(entry).readAllBytes();
                String hash=hash(before),baseline=Files.readString(Path.of("docs/FOREST_OVERHAUL_BASELINE.json"));
                if(!baseline.contains("\""+path+"\": \""+hash+"\""))throw new AssertionError("Unauthenticated Forest prior source: "+path);
                String actual=hash(current);
                if(actual.equals(hash))return current; // A prior projection already supplied these exact authenticated bytes.
                if(!actual.equals(expected))throw new AssertionError("Unreviewed Forest integration/test change: "+path);
                return before;
            }
        } catch(Exception error){throw new AssertionError(error);}
    }
    static String hash(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
}
