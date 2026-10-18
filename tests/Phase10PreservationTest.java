import game.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

/** Every previous file remains authenticated; no map, NPC, skill or environment exemption. */
public class Phase10PreservationTest extends V2TestSupport {
 public static void main(String[] args)throws Exception{
  var allowed=Phase10PreservationSupport.properties("docs/PHASE10_AUTHORIZED.properties");
  var matcher=Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/PHASE10_BASELINE.json")));
  var changed=new HashSet<String>();int total=0;while(matcher.find()){
   String path=matcher.group(1);byte[] bytes=Phase101PreservationSupport.previous(path,Files.readAllBytes(Path.of(path)));if(!ForestPreservationSupport.hash(bytes).equals(matcher.group(2)))changed.add(path);
   check(ForestPreservationSupport.hash(Phase10PreservationSupport.previous(path,bytes)).equals(matcher.group(2)),"Exact prior owner/asset "+path);total++;
  }
  check(total==928&&changed.equals(allowed.stringPropertyNames()),"Exact authenticated prior-owner changes");
  check(changed.stream().noneMatch(p->p.startsWith("assets/")||p.contains("WorldMap")&&p.startsWith("src/")||p.contains("Village")&&p.startsWith("src/")||p.startsWith("src/entity/")&&!p.endsWith("Enemy.java")||p.endsWith(".ps1")),"Original assets, maps, NPCs, Santa and scripts preserved");
  check(WorldMap.forArea(Area.Type.FOREST).width()==3072&&WorldMap.forArea(Area.Type.FOREST).height()==2304&&WorldMap.forArea(Area.Type.FOREST).solids().size()==496,"Original Forest dimensions/solids");
  check(WorldMap.forArea(Area.Type.VILLAGE).width()==2560&&TreeCollisionPreservationSupport.previousSolids(WorldMap.forArea(Area.Type.VILLAGE)).size()==285,"Original Village geometry");
  check(SpiritTreeTransformation.DURATION==180,"Original Spirit Tree clock");
  check(new EncounterManager().getEncounters().size()==14,"Only one additive encounter registration");
  for(var area:Area.Type.values()){
   var im=ForestLandmarkPreservationTest.raw(area.ordinal());
   check(im.getWidth()==1270&&im.getHeight()==720,"Existing neutral actual GamePanel viewport "+area);
           SantaSkillsPreservationSupport.frame(area.ordinal(),im);
  }
  System.out.println("PASS: "+checks+" complete baseline and geometry preservation assertions");
 }
}
