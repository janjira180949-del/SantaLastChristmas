import java.nio.file.*;
import java.util.*;

/** All pre-fix assets, configuration and gameplay remain exact; only three owners differ. */
public class PngCrashPreservationTest extends V2TestSupport {
    public static void main(String[] args)throws Exception {
        var allowed=Phase10PreservationSupport.properties("docs/PNG_CRASH_FIX_AUTHORIZED.properties");
        check(allowed.stringPropertyNames().equals(Set.of("src/util/ImageAssets.java",
            "tests/Phase103PreservationSupport.java","tests/Phase103PreservationTest.java","src/game/Game.java")),"Exact minimal loader scope plus unchanged pre-existing import reorder");
        var changed=new HashSet<String>();
        var entries=java.util.regex.Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"")
            .matcher(Files.readString(Path.of("docs/PNG_CRASH_FIX_BASELINE.json")));
        while(entries.find()) {
            String path=entries.group(1);byte[] current=Phase105PreservationSupport.previous(path,Files.readAllBytes(Path.of(path)));
            String hash=ForestPreservationSupport.hash(current);
            if(!hash.equals(entries.group(2)))changed.add(path);
            check(hash.equals(allowed.getProperty(path,entries.group(2))),"Exact protected file or authenticated correction "+path);
            if(!path.equals("src/game/Game.java"))check(ForestPreservationSupport.hash(PngCrashPreservationSupport.previous(path,current)).equals(entries.group(2)),"Authenticated original retained "+path);
        }
        check(changed.equals(Set.of("src/util/ImageAssets.java","tests/Phase103PreservationSupport.java","tests/Phase103PreservationTest.java")),"No unrelated existing file changed; Game remains task-start exact");
        System.out.println("PASS: "+checks+" crash-fix inventory and original asset/gameplay/configuration assertions");
    }
}
