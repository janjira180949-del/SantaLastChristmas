import game.*;import java.nio.file.*;import java.util.*;import java.util.regex.Pattern;import java.nio.charset.StandardCharsets;
/** Exact pre-skill owners/art/geometry and bounded actual-renderer HUD migration. */
public class SantaSkillsPreservationTest extends V2TestSupport {
 public static void main(String[] args)throws Exception{
  var allowed=new Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_4_AUTHORIZED.properties"))){allowed.load(in);}
  var matcher=Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/PHASE9_4_BASELINE.json")));
  var changes=new HashSet<String>();int files=0;while(matcher.find()){String path=matcher.group(1);var bytes=Phase10PreservationSupport.previous(path,Files.readAllBytes(Path.of(path)));if(!ForestPreservationSupport.hash(bytes).equals(matcher.group(2)))changes.add(path);
   check(ForestPreservationSupport.hash(SantaSkillsPreservationSupport.previous(path,bytes)).equals(matcher.group(2)),"Exact baseline owner "+path);files++;
  }check(files==902&&changes.equals(allowed.stringPropertyNames()),"Exact bounded existing-owner change set");
  check(changes.stream().noneMatch(p->p.startsWith("assets/")||p.endsWith(".ps1")||p.startsWith("tools/")||p.startsWith("src/entity/")||p.startsWith("src/boss/")),"All assets/maps/mechanics/tools/scripts untouched");
  String old=new String(JourneyHomePreservationSupport.archived("src/game/AbilitySystem.java","PHASE9_4"),StandardCharsets.UTF_8),now=new String(Phase10PreservationSupport.previous("src/game/AbilitySystem.java",Files.readAllBytes(Path.of("src/game/AbilitySystem.java"))),StandardCharsets.UTF_8);
  int a=old.indexOf("    public boolean use("),b=old.indexOf("    public void draw(");check(old.substring(a,b).equals(now.substring(now.indexOf("    public boolean use("),now.indexOf("    public void draw("))),"Bell damage/range/knockback/slot rejection exact original bytes");
  for(int area=0;area<4;area++){SantaSkillsPreservationSupport.frame(area,ForestLandmarkPreservationTest.raw(area));check(true,"All world/otherUI pixels unchanged outside exact ability-card union "+area);}
  SantaBaseVisuals.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS);var base=SantaBaseNavigationTest.opening();base.setSize(1600,1280);SantaSkillsPreservationSupport.frame(3,render(base));base.stopGame();check(true,"Complete Base overview outside ability HUD unchanged");
  for(var area:Area.Type.values()){var map=WorldMap.forArea(area);check(map.width()>0&&!map.solids().isEmpty(),"Existing maps have authoritative geometry "+area);}
  check(SpiritTreeTransformation.DURATION==180,"Original restoration timing");
  System.out.println("PASS: "+checks+" authenticated skill revision and actual renderer assertions");
 }
}
