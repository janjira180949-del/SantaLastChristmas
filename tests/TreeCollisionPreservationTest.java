import java.nio.file.*;import java.util.*;import game.*;
public class TreeCollisionPreservationTest extends V2TestSupport{
 public static void main(String[] args)throws Exception{
  var allowed=TreeCollisionPreservationSupport.allowed();Set<String> changed=new HashSet<>();int count=0;
  var matcher=java.util.regex.Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/TREE_COLLISION_BASELINE.json")));
  while(matcher.find()){String path=matcher.group(1),prior=matcher.group(2);byte[] now=CinematicPacingPreservationSupport.previous(path,Files.readAllBytes(Path.of(path)));String hash=ForestPreservationSupport.hash(now);check(hash.equals(allowed.getProperty(path,prior)),"Exact protected original or authorized correction "+path);check(ForestPreservationSupport.hash(TreeCollisionPreservationSupport.previous(path,now)).equals(prior),"Original recovery bytes retained "+path);if(!hash.equals(prior))changed.add(path);count++;}
  check(count==1683&&changed.equals(allowed.stringPropertyNames()),"Entire pre-edit inventory and exact correction scope");
  check(allowed.stringPropertyNames().stream().filter(p->p.startsWith("src/")).collect(java.util.stream.Collectors.toSet()).equals(Set.of("src/game/WorldMap.java","src/game/GamePanel.java")),"Exactly two production owners change");
  for(var type:Area.Type.values()){
   var map=WorldMap.forArea(type);String geometry=map.id()+map.width()+","+map.height()+new TreeMap<>(map.points())+TreeCollisionPreservationSupport.previousSolids(map)+map.combatRegions();
   var props=Phase10PreservationSupport.properties("docs/TREE_COLLISION_GEOMETRY.properties");check(ForestPreservationSupport.hash(geometry.getBytes(java.nio.charset.StandardCharsets.UTF_8)).equals(props.getProperty(type.name())),"Every previous collider, marker, road boundary and encounter region retained "+type);
  }
  for(String p:allowed.stringPropertyNames())check(!p.startsWith("assets/")&&!p.startsWith(".vscode/")&&!p.startsWith("src/audio/")&&!p.startsWith("src/entity/"),"Art/audio/player/configuration unmodified "+p);
  check(SpiritTreeTransformation.DURATION==180&&EndingCutscene.duration(EndingCutscene.State.SLEIGH_DEPARTURE)==360,"Ending clocks retained");
  for(int i=0;i<4;i++)check(ForestPreservationTest.pixelHash(Phase1WorldPreservationSupport.frame(i,ForestLandmarkPreservationTest.raw(i))).equals(Phase10PreservationSupport.properties("docs/TREE_COLLISION_RENDER.properties").getProperty(Integer.toString(i))),"Matched neutral renderer exact "+i);
  System.out.println("TreeCollisionPreservationTest PASS "+checks+" assertions");
 }
}
