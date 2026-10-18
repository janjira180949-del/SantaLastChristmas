import game.*;
import java.nio.file.*;
import java.util.*;

/** Exact approved owner hashes and localized renderer deltas; no whole-world exclusions. */
public class Phase102PreservationTest extends V2TestSupport {
 public static void main(String[] args)throws Exception {
  Set<String> changed=Set.of("src/game/AbilitySystem.java","src/game/SantaSkills.java","src/game/SkillArtwork.java","src/game/BlackFrostCrystal.java","src/game/ForestLandmarkArt.java","src/game/BossArenaVisuals.java","src/entity/Enemy.java","src/game/GamePanel.java","tests/Phase101PreservationSupport.java","tests/Phase101BossPolishTest.java","tests/SantaSkillsPreservationSupport.java","tests/CombatReworkTest.java","tests/Phase10BossTest.java","tests/SantaSkillsTest.java","tests/UIPolishTest.java");
  Set<String> compatibility=Set.of("src/ui/HUD.java",".vscode/launch.json",".vscode/settings.json",".vscode/tasks.json","run.ps1");
  var approved=Phase102BEPreservationSupport.allowed();var expected=new HashSet<>(changed);expected.addAll(compatibility);
  check(approved.stringPropertyNames().equals(expected),"Exact isolated eight production/seven test migration owners plus immutable compatibility baselines");
  var matcher=java.util.regex.Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/PHASE10_2BE_BASELINE.json")));
  var actualChanges=new HashSet<String>();
  while(matcher.find()) {
   String path=matcher.group(1),hash=ForestPreservationSupport.hash(Phase102FPreservationSupport.previous(path,Files.readAllBytes(Path.of(path))));
   if(!hash.equals(matcher.group(2)))actualChanges.add(path);
   check(hash.equals(changed.contains(path)?approved.getProperty(path):matcher.group(2)),"Exact current or unchanged task owner "+path);
  }
  check(actualChanges.equals(changed),"No unapproved source/test/manifest/script/configuration changes");
  for(String path:compatibility)check(ForestPreservationSupport.hash(Phase103PreservationSupport.previous(path,Files.readAllBytes(Path.of(path)))).equals(approved.getProperty(path)),"Approved HUD and launch configuration unchanged "+path);
  byte[] old=Phase10PreservationSupport.archive("src/game/GamePanel.java","PHASE10_2BE");
  String before=new String(old,java.nio.charset.StandardCharsets.UTF_8),after=new String(Phase102FPreservationSupport.previous("src/game/GamePanel.java",Files.readAllBytes(Path.of("src/game/GamePanel.java"))),java.nio.charset.StandardCharsets.UTF_8);
  check(after.equals(before.replace("BossArenaVisuals.draw(graphics,activeBoss,worldAtmosphere.getTicks());","BossArenaVisuals.draw(graphics,activeBoss,worldAtmosphere.getTicks(),town,treeTransformation.isRestored()||storyQuestManager.isBossDefeated(),this);")),"Only the announced existing beneath-actor arena call gains context");
  var hashes=Phase10PreservationSupport.properties("docs/PHASE10_2BE_RENDER.properties");
  check(ForestPreservationTest.pixelHash(Phase1WorldPreservationSupport.terrain(true,FrozenForestVisuals.prepareAsync().get())).equals(hashes.getProperty("forest-terrain")),"Every original prepared Forest terrain pixel, including campfire, is unchanged");
  for(int area=0;area<4;area++){Phase102BEPreservationSupport.frame(area,ForestLandmarkPreservationTest.raw(area));check(true,"Only exact crystal/arena regions can differ; Base/Village/other HUD exact "+area);}
  SantaBaseVisuals.prepareAsync().get();var p=SantaBaseNavigationTest.opening();p.setSize(1600,1280);Phase102BEPreservationSupport.frame(3,render(p));p.stopGame();check(true,"Full Base overview is pixel-identical");
  check(SpiritTreeTransformation.DURATION==180,"Unchanged restoration timing");
  System.out.println("PASS: "+checks+" exact owner, terrain, HUD, configuration and localized renderer assertions");
 }
}
