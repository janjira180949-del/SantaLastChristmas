import java.nio.file.*;import java.util.*;import game.*;
public class TownFinalFixPreservationTest extends V2TestSupport{
 public static void main(String[] args)throws Exception{
  var allowed=TownFinalFixPreservationSupport.allowed();Set<String> changed=new HashSet<>();int n=0;
  var matcher=java.util.regex.Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/TOWN_FINAL_FIXES_BASELINE.json")));
  while(matcher.find()){String path=matcher.group(1),prior=matcher.group(2);byte[] current=TreeCollisionPreservationSupport.previous(path,Files.readAllBytes(Path.of(path)));String hash=ForestPreservationSupport.hash(current);
   check(hash.equals(allowed.getProperty(path,prior)),"Exact original or reviewed current bytes "+path);
   check(ForestPreservationSupport.hash(TownFinalFixPreservationSupport.previous(path,current)).equals(prior),"Verified recoverable original "+path);if(!hash.equals(prior))changed.add(path);n++;
  }
  check(n==1665&&changed.equals(allowed.stringPropertyNames()),"Complete protected inventory and exact authorized change set");check(TownFinalFixPreservationSupport.assets().size()==5,"Three licensed WAVs and two primary source pages");
  for(String path:List.of("src/ui/HUD.java","src/game/Camera.java","src/util/ImageAssets.java",".vscode/settings.json",".vscode/launch.json","build.ps1","run.ps1","src/boss/BossAttacks.java","src/game/SantaSkills.java","src/game/AbilitySystem.java","src/quest/StoryQuestManager.java","src/audio/AudioManager.java"))check(!allowed.containsKey(path),"Sensitive mechanics, artwork loading, music mixing and configuration unchanged "+path);
  for(int area=0;area<4;area++){var now=Phase1WorldPreservationSupport.frame(area,ForestLandmarkPreservationTest.raw(area));var old=TownFinalFixPreservationSupport.fixture(area);check(Phase105PreservationSupport.equal(old,area==2?TownFinalFixPreservationSupport.frame(area,now):now),"Actual matched renderer unchanged outside keeper canvas "+area);}
  check(SpiritTreeTransformation.DURATION==180&&EndingCutscene.duration(EndingCutscene.State.SLEIGH_DEPARTURE)==360,"Exact Tree/departure clocks retained");
  System.out.println("TownFinalFixPreservationTest PASS "+checks+" assertions");
 }
}
