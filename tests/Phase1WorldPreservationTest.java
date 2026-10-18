import game.*;import java.nio.file.*;import java.util.*;
/** Full snapshot, immutable animation/audio/geometry and exact local renderer authorization. */
public class Phase1WorldPreservationTest extends V2TestSupport{
 public static void main(String[]args)throws Exception{
  var allowed=Phase1WorldPreservationSupport.allowed();Set<String> changed=new TreeSet<>();int count=0;
  var matcher=java.util.regex.Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/PHASE1_WORLD_BASELINE.json")));
  while(matcher.find()){
   String p=matcher.group(1),before=matcher.group(2);byte[] now=Phase2UIPreservationSupport.previous(p,Files.readAllBytes(Path.of(p)));String hash=ForestPreservationSupport.hash(now);
   check(hash.equals(allowed.getProperty(p,before)),"Exact original or authorized owner "+p);
   check(ForestPreservationSupport.hash(Phase1WorldPreservationSupport.previous(p,now)).equals(before),"Original recovery bytes authenticated "+p);
   if(!hash.equals(before))changed.add(p);count++;
  }
  check(count==1699&&changed.equals(allowed.stringPropertyNames()),"Complete inventory and exact change set");
  var production=changed.stream().filter(p->p.startsWith("src/")).collect(java.util.stream.Collectors.toSet());
  check(production.equals(Set.of("src/game/SantaBaseEnvironmentLayers.java","src/game/HealingPoint.java","src/game/GamePanel.java","src/game/VillageBuildingVisuals.java","src/game/VillageEnvironmentLayers.java","src/game/VillageRestorationVisuals.java","src/game/ForestEnvironmentLayers.java","src/game/WorldMap.java","src/game/JourneyHomeVisuals.java","src/game/EndingCutscene.java")),"Exactly ten authorized placement/environment owners; no NPC, boss, skill, audio or UI owner changed");
  for(String p:changed)check(!p.startsWith("assets/")&&!p.startsWith(".vscode/")&&!p.startsWith("src/audio/")&&!p.startsWith("src/boss/")&&!p.startsWith("src/entity/"),"Approved artwork, audio, config, actors and animation unchanged "+p);
  check(SpiritTreeTransformation.DURATION==180&&EndingCutscene.SLEIGH_DURATION==360&&BossFallPresentation.FANG_DURATION==240&&BossFallPresentation.TITAN_DURATION==300,"All approved story/animation clocks retained");
  check(WorldMap.forArea(Area.Type.VILLAGE).solids().contains(new java.awt.Rectangle(1264,856,64,24)),"Approved Village evergreen base retained");
  for(int area=0;area<4;area++){
   var now=ForestLandmarkPreservationTest.raw(area);var prior=Phase1WorldPreservationSupport.frame(area,now);
   check(ForestPreservationTest.pixelHash(prior).equals(Phase10PreservationSupport.properties("docs/TREE_COLLISION_RENDER.properties").getProperty(Integer.toString(area))),"Neutral matched world pixels exact outside local gate/rest footprints "+area);
  }
  SantaBaseVisuals.prepareAsync().get();var base=SantaBaseNavigationTest.opening();base.setSize(1600,1280);Phase1WorldPreservationSupport.frame(3,render(base));base.stopGame();check(true,"Entire Base overview unchanged outside exact gate bounds");
  String before=new String(Phase10PreservationSupport.archive("src/game/HealingPoint.java","PHASE1_WORLD"),java.nio.charset.StandardCharsets.UTF_8).replace("\r\n","\n");String after=Files.readString(Path.of("src/game/HealingPoint.java")).replace("\r\n","\n");String a="    public boolean use(",b="    /** Forest presentation";
  check(before.substring(before.indexOf(a),before.indexOf(b)).equals(after.substring(after.indexOf(a),after.indexOf(b))),"Complete original healing transaction unchanged");
  System.out.println("Phase1WorldPreservationTest PASS "+checks+" assertions");
 }
}
