import java.nio.file.*;import java.util.*;
public class Phase106CPreservationTest extends V2TestSupport{
 public static void main(String[] args)throws Exception{
  var allowed=Phase106CPreservationSupport.allowed();var changed=new HashSet<String>();
  var matcher=java.util.regex.Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/PHASE10_6C_BASELINE.json")));int count=0;
  while(matcher.find()){
   String path=matcher.group(1),prior=matcher.group(2);byte[] now=TownFinalFixPreservationSupport.previous(path,Files.readAllBytes(Path.of(path)));String hash=ForestPreservationSupport.hash(now);
   check(hash.equals(allowed.getProperty(path,prior)),"Exact baseline or authorized boss-presentation owner "+path);
   check(ForestPreservationSupport.hash(Phase106CPreservationSupport.previous(path,now)).equals(prior),"Exact authenticated prior bytes retained "+path);
   if(!hash.equals(prior))changed.add(path);count++;
  }
  check(count==1645&&changed.equals(allowed.stringPropertyNames()),"Complete exact C change set");
  for(String path:List.of("src/game/Camera.java","src/ui/HUD.java","src/util/ImageAssets.java",".vscode/settings.json",".vscode/launch.json","build.ps1","run.ps1","src/boss/BossAttacks.java","src/game/SantaSkills.java","src/quest/StoryQuestManager.java"))check(!allowed.containsKey(path),"Sensitive mechanics/configuration unchanged "+path);
  check(Phase106CPreservationSupport.assets().size()==11,"All new audio/license evidence authenticated");
  check(game.SpiritTreeTransformation.DURATION==180&&game.BossFallPresentation.FANG_DURATION==240&&game.BossFallPresentation.TITAN_DURATION==300,"Exact established presentation and restoration clocks");
  System.out.println("Phase106CPreservationTest PASS "+checks+" assertions");
 }
}
