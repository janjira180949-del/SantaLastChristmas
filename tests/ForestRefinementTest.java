import game.*;import java.nio.file.*;import java.util.*;import java.awt.*;import java.awt.event.*;import java.nio.charset.StandardCharsets;

/** Actual proximity/input, bounded terrain differences and exact unrelated file protection. */
public class ForestRefinementTest extends V2TestSupport {
 public static void main(String[] args)throws Exception {
  var allowed=new Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_3_1_1_AUTHORIZED.properties"))){allowed.load(in);}
  check(allowed.stringPropertyNames().equals(Set.of("src/game/ForestEnvironmentLayers.java","src/game/WorldMapData.java","assets/world/map_data.json","tools/generate_world_maps.py","tests/Phase931PreservationSupport.java","tests/VillageIntegrationPreservationSupport.java","tests/ForestLandmarkPreservationTest.java","tests/VillageConsistencyPreservationTest.java","tests/QuestFlowTest.java","tests/ChildIntegrationTest.java","tests/TownFinalEnvironmentTest.java","tools/verify_journey_home_metadata.py","tools/verify_phase92_metadata.py")),"Exact two production owners, metadata/generator, seven historical Java migrations and two metadata verifiers");
  var matches=java.util.regex.Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/PHASE9_3_1_1_BASELINE.json")));
  var changed=new HashSet<String>();int count=0;
  while(matches.find()){
   String path=matches.group(1);byte[] bytes=ForestGameplayPreservationSupport.previous(path,Files.readAllBytes(Path.of(path)));
   if(!ForestPreservationSupport.hash(bytes).equals(matches.group(2)))changed.add(path);
   check(ForestPreservationSupport.hash(ForestRefinementPreservationSupport.previous(path,bytes)).equals(matches.group(2)),"Original protected bytes "+path);count++;
  }
  check(count==887&&changed.equals(allowed.stringPropertyNames()),"Entire baseline protected with exact authorized change set");
  var before=ForestRefinementPreservationSupport.fixture("terrain");var after=Phase1WorldPreservationSupport.terrain(true,FrozenForestVisuals.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS));int delta=0;
  var region=new Rectangle(1288,863,113,60);
  for(int y=0;y<2304;y++)for(int x=0;x<3072;x++)if(before.getRGB(x,y)!=after.getRGB(x,y)){check(region.contains(x,y),"Campfire-only changed pixel "+x+","+y);delta++;}
  check(delta>250&&delta<5000,"Subtle localized ground treatment, not a full terrain replacement");
  var p=playing();var toby=p.getChildren().get(0);var map=WorldMap.forArea(Area.Type.VILLAGE);
  check(map.point("npc/Toby").equals(new WorldMap.Point(1296,1008))&&toby.getX()==1281&&toby.getY()==987,"Authoritative marker and persistent actor agree");
  check(Math.abs((toby.getY()+42)-VillageEnvironmentLayers.SQUARE.getCenterY())==4,"Toby's feet align with actual circular plaza center");
  check(!util.Collision.intersectsAny(toby.getBounds(),map.solids()),"Toby stands on walkable stone");
  check(!toby.getBounds().intersects(santa(p).getBounds()),"Village arrival does not overlap Toby");
  completeOpeningDeliveries(p);santa(p).setPosition(1280,1066);tick(p,1);check(!toby.canInteract(),"Prompt absent outside original 58-pixel range");
  var key=new KeyEvent(p,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_W,'w');for(var l:p.getKeyListeners())l.keyPressed(key);
  tick(p,7);key=new KeyEvent(p,KeyEvent.KEY_RELEASED,0,0,KeyEvent.VK_W,'w');for(var l:p.getKeyListeners())l.keyReleased(key);tick(p,1);
  check(toby.canInteract()&&!util.Collision.intersectsAny(santa(p).getBounds(),map.solids()),"Santa walks up to Toby using normal input");
  check(!toby.getBounds().intersects(santa(p).getBounds()),"Interaction approach leaves both actors distinct");
  tap(p,KeyEvent.VK_E);check(((ui.DialogueManager)field(p,"dialogueManager")).isActive(),"Actual E opens existing Toby dialogue");closeDialogue(p);check(story(p).isStoryStarted(),"Original Lost Gift quest begins after dialogue closes");
  p.stopGame();ForestRefinementPreservationSupport.frame(0,ForestLandmarkPreservationTest.raw(0));check(true,"Actual Village pixels differ only at Toby and his navigation marker");
  var old=JourneyHomePreservationSupport.archived("src/game/ForestEnvironmentLayers.java","PHASE9_3_1_1");var source=Files.readString(Path.of("src/game/ForestEnvironmentLayers.java"));
  String previous=new String(old,StandardCharsets.UTF_8);String ring="g.setColor(new Color(22,30,43));g.fillOval(1304,872,78,37);";
  check(source.contains(ring)&&previous.contains(ring),"Original fire ring artwork and coordinates retained");
  var geometry=new Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_3_1_GEOMETRY_BEFORE.properties"))){geometry.load(in);}
  for(var type:Area.Type.values()){var m=WorldMap.forArea(type);String data=m.id()+m.width()+","+m.height()+ForestRefinementPreservationSupport.points(m)+TreeCollisionPreservationSupport.previousSolids(m)+m.combatRegions();check(ForestPreservationSupport.hash(data.getBytes(StandardCharsets.UTF_8)).equals(geometry.getProperty(type.name())),"All geometry exact except authorized Toby marker "+type);}
  System.out.println("PASS: "+checks+" campfire, plaza/input, geometry and authenticated preservation assertions; changed ground pixels="+delta);
 }
}
