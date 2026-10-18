import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipFile;

/** Exact before/after guards for owners intentionally superseded by this user-requested overhaul. */
final class OverhaulPreservationSupport {
    private OverhaulPreservationSupport() { }
    static byte[] previous(String path,byte[] current) {
        current=ForestPreservationSupport.previous(path,current);
        try {
            Properties allowed=new Properties();
            try(var input=Files.newInputStream(Path.of("docs/GAMEPLAY_OVERHAUL_AUTHORIZED.properties"))){allowed.load(input);}
            String expected=allowed.getProperty(path);
            if(expected==null)return current;
            String actual=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(current));
            if(!actual.equals(expected))throw new AssertionError("Unreviewed overhaul owner change: "+path);
            try(ZipFile zip=new ZipFile("docs/GAMEPLAY_OVERHAUL_PREVIOUS_SOURCES.zip")) {
                var entry=zip.getEntry(path);
                if(entry==null)throw new AssertionError("Missing audited prior owner: "+path);
                byte[] before=zip.getInputStream(entry).readAllBytes();
                String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(before));
                String baseline=Files.readString(Path.of("docs/GAMEPLAY_OVERHAUL_BASELINE.json"));
                if(!baseline.contains("\""+path+"\": \""+hash+"\""))throw new AssertionError("Prior owner does not match audit: "+path);
                return before;
            }
        } catch(java.io.IOException|java.security.NoSuchAlgorithmException error){throw new AssertionError(error);}
    }
    static String previousText(String path,String current) {
        return new String(previous(path,current.getBytes(StandardCharsets.UTF_8)),StandardCharsets.UTF_8).replace("\r\n","\n");
    }
}
