import game.*;import java.nio.file.*;import java.util.*;import java.util.regex.Pattern;
public class ForestGameplayPreservationTest extends V2TestSupport {
 public static void main(String[] args)throws Exception{
  var props=new Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_3_2B_AUTHORIZED.properties"))){props.load(in);}
  var matcher=Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/PHASE9_3_2B_BASELINE.json")));
  var changed=new HashSet<String>();int count=0;
  while(matcher.find()){String path=matcher.group(1);byte[] current=SantaSkillsPreservationSupport.previous(path,Files.readAllBytes(Path.of(path)));if(!ForestPreservationSupport.hash(current).equals(matcher.group(2)))changed.add(path);
   check(ForestPreservationSupport.hash(ForestGameplayPreservationSupport.previous(path,current)).equals(matcher.group(2)),"Authenticated baseline "+path);count++;
  }
  check(count==897&&changed.equals(props.stringPropertyNames()),"Complete exact revision inventory");
  check(changed.stream().noneMatch(p->p.startsWith("assets/")||p.endsWith(".ps1")||p.equals("srcs.txt")),"No art/map/scripts/source manifest changes");
  var geometry=new Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_3_1_GEOMETRY_BEFORE.properties"))){geometry.load(in);}
  for(var type:Area.Type.values()){var map=WorldMap.forArea(type);String data=map.id()+map.width()+","+map.height()+ForestRefinementPreservationSupport.points(map)+TreeCollisionPreservationSupport.previousSolids(map)+map.combatRegions();check(ForestPreservationSupport.hash(data.getBytes(java.nio.charset.StandardCharsets.UTF_8)).equals(geometry.getProperty(type.name())),"Exact runtime geometry "+type);}
  for(String path:List.of("src/game/FinalChapter.java","src/game/HealingPoint.java","src/game/EncounterManager.java")){
   String old=new String(JourneyHomePreservationSupport.archived(path,"PHASE9_3_2B"),java.nio.charset.StandardCharsets.UTF_8),now=new String(Phase10PreservationSupport.previous(path,Files.readAllBytes(Path.of(path))),java.nio.charset.StandardCharsets.UTF_8);
   if(path.contains("HealingPoint")){int begin=old.indexOf("    public boolean use(");int end=old.indexOf("    /** Forest presentation");check(old.substring(begin,end).equals(now.substring(now.indexOf("    public boolean use("),now.indexOf("    /** Forest presentation"))),"Exact healing mechanics");}
   if(path.contains("EncounterManager")){String projected=now.lines().map(line->line.contains("new Encounter(\"crystal-")?line.replace(".warned(),",","):line).collect(java.util.stream.Collectors.joining("\n"));check(old.lines().collect(java.util.stream.Collectors.joining("\n")).equals(projected),"Only initial guardian warning changed");}
  }
  System.out.println("PASS: "+checks+" exact file/art/mechanics preservation assertions");
 }
}

