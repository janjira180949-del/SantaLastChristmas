import java.nio.file.*;import java.util.*;import game.*;
/** Complete current inventory, immutable art/audio/maps/config, and matched neutral rendering. */
public class CinematicPacingPreservationTest extends V2TestSupport{
 public static void main(String[]args)throws Exception{
  var allowed=CinematicPacingPreservationSupport.allowed();Set<String> changed=new TreeSet<>();int n=0;
  var matcher=java.util.regex.Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/CINEMATIC_PACING_BASELINE.json")));
  while(matcher.find()){
   String path=matcher.group(1),prior=matcher.group(2);byte[] now=Phase1WorldPreservationSupport.previous(path,Files.readAllBytes(Path.of(path)));String hash=ForestPreservationSupport.hash(now);
   check(hash.equals(allowed.getProperty(path,prior)),"Exact protected original or authorized cinematic change "+path);
   check(ForestPreservationSupport.hash(CinematicPacingPreservationSupport.previous(path,now)).equals(prior),"Authenticated original recovery "+path);
   if(!hash.equals(prior))changed.add(path);n++;
  }
  check(n==1692&&changed.equals(allowed.stringPropertyNames()),"Entire pre-edit inventory and exact authorized owner set");
  var production=allowed.stringPropertyNames().stream().filter(p->p.startsWith("src/")).collect(java.util.stream.Collectors.toSet());
  check(production.equals(Set.of("src/game/BossFallPresentation.java","src/game/EndingCutscene.java","src/game/JourneyHomeVisuals.java","src/game/GamePanel.java","src/game/AdventureAudio.java","src/game/StoryOverlay.java","src/game/TownChapterArt.java","src/game/BossArenaVisuals.java","src/boss/FrostFang.java")),"Exactly nine presentation owners; no camera, HUD, skills, AI or quest owner");
  for(String p:allowed.stringPropertyNames())check(!p.startsWith("assets/")&&!p.startsWith(".vscode/")&&!p.startsWith("src/audio/")&&!p.startsWith("src/entity/"),"Existing artwork/audio/config/player untouched "+p);
  String fang=Files.readString(Path.of("src/boss/FrostFang.java")).replace("\r\n","\n").replace("    /** Presentation-only facing snapshot; no combat state changes. */\n    public boolean isFacingRight(){return facingRight;}\n","");
  check(fang.equals(new String(Phase10PreservationSupport.archive("src/boss/FrostFang.java","CINEMATIC_PACING"),java.nio.charset.StandardCharsets.UTF_8).replace("\r\n","\n")),"Fang only adds read-only presentation getter; AI is exact");
  check(SpiritTreeTransformation.DURATION==180&&AbilitySystem.BELL_COOLDOWN==90&&SantaSkills.STAR_DAMAGE==18&&SantaSkills.BOMB_DAMAGE==110,"Authoritative gameplay clocks and balance preserved");
  for(int i=0;i<4;i++)check(ForestPreservationTest.pixelHash(Phase1WorldPreservationSupport.frame(i,ForestLandmarkPreservationTest.raw(i))).equals(Phase10PreservationSupport.properties("docs/TREE_COLLISION_RENDER.properties").getProperty(Integer.toString(i))),"Matched neutral Village/Forest/Town/Base pixels exact "+i);
  System.out.println("CinematicPacingPreservationTest PASS "+checks+" assertions");
 }
}
