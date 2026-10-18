import java.nio.file.*;
import java.util.*;

/** Exact pre-edit world, artwork, gameplay, configuration and audio ownership checks. */
public class Phase106PreservationTest extends V2TestSupport {
 public static void main(String[] args)throws Exception{
  var allowed=Phase106PreservationSupport.allowed();var changed=new HashSet<String>();
  var external=Phase10PreservationSupport.properties("docs/PHASE10_6B_EXTERNAL.properties");
  var entries=java.util.regex.Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/PHASE10_6B_BASELINE.json")));
  int protectedFiles=0;
  while(entries.find()){
   String path=entries.group(1),prior=entries.group(2);byte[] now=Phase106CPreservationSupport.previous(path,Files.readAllBytes(Path.of(path)));String hash=ForestPreservationSupport.hash(now);
   check(hash.equals(allowed.getProperty(path,external.getProperty(path,prior))),"Exact protected original, observed editor revision or approved audio revision "+path);
   check(ForestPreservationSupport.hash(Phase106PreservationSupport.previous(path,now)).equals(prior),"Authenticated prior source retained "+path);
   if(!hash.equals(prior)&&!external.containsKey(path))changed.add(path);protectedFiles++;
  }
  check(protectedFiles==1528&&changed.equals(allowed.stringPropertyNames()),"Complete inventory; exact change set only");
  for(String path:List.of("src/ui/HUD.java","src/game/Camera.java","src/util/ImageAssets.java",".vscode/launch.json",".vscode/settings.json","build.ps1","run.ps1"))check(!allowed.containsKey(path),"Sensitive renderer, PNG fix and launch configuration unchanged "+path);
  check(game.SpiritTreeTransformation.DURATION==180,"Restoration clock unchanged");
  check(Phase106PreservationSupport.assets().size()==106,"Every new WAV and provenance document authenticated");
  check(ForestPreservationSupport.hash(Files.readAllBytes(Path.of("assets/audio/runtime/music/wintery_loop.wav"))).equals("c27f9aa0423da9d186184f771a423e8f82c40aac19ae06bbc96e86df25923f55"),"Actual menu recording byte-identical");
  System.out.println("Phase106PreservationTest PASS "+checks+" assertions");
 }
}
