import game.*;import java.nio.file.*;import java.util.*;import java.awt.*;import java.awt.image.BufferedImage;

/** Authenticates the two authorized refinements before projecting immutable historical guards. */
final class ForestRefinementPreservationSupport {
 static byte[] previous(String path,byte[] bytes){
  bytes=ForestGameplayPreservationSupport.previous(path,bytes);
  try {
   var allowed=new Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_3_1_1_AUTHORIZED.properties"))){allowed.load(in);}
   if(!allowed.containsKey(path))return bytes;
   String hash=ForestPreservationSupport.hash(bytes);
   for(String prefix:java.util.List.of("PHASE9_3_1_1","PHASE9_3_1","PHASE9_2_1","PHASE9_2","QUEST_FLOW","JOURNEY_HOME","FINAL_CHAPTER","VILLAGE_INTEGRATION","VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")){
    byte[] old=JourneyHomePreservationSupport.archived(path,prefix);if(old!=null&&hash.equals(ForestPreservationSupport.hash(old)))return bytes;
   }
   if(!hash.equals(allowed.getProperty(path)))throw new AssertionError("Unauthenticated campfire/Toby refinement: "+path);
   return JourneyHomePreservationSupport.archived(path,"PHASE9_3_1_1");
  }catch(Exception e){throw new AssertionError(e);}
 }
 static TreeMap<String,WorldMap.Point> points(WorldMap map){
  var points=new TreeMap<>(map.points());if(map.id().equals("christmas_town")){
   if(!new WorldMap.Point(1776,1392).equals(points.remove("npc/Elias")))throw new AssertionError("Unreviewed deep Town keeper position");
  }if(map.id().equals("village")){
   if(!points.get("npc/Toby").equals(new WorldMap.Point(1296,1008)))throw new AssertionError("Unreviewed plaza Toby position");
   points.put("npc/Toby",new WorldMap.Point(528,784));
  }return points;
 }
 static BufferedImage fixture(String name)throws Exception {
  var hashes=new Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_3_1_1_RENDER.properties"))){hashes.load(in);}
  var file=Path.of("tests/fixtures/phase9-3-1-1-"+name+"-before.png");
  if(!ForestPreservationSupport.hash(Files.readAllBytes(file)).equals(hashes.getProperty(name)))throw new AssertionError("Unauthenticated refinement renderer baseline");
  return javax.imageio.ImageIO.read(file.toFile());
 }
 static BufferedImage frame(int area,BufferedImage image)throws Exception {
  image=SantaSkillsPreservationSupport.frame(area,image);
  if(area!=0)return image;
  var before=fixture("frame-0");var camera=new Camera();camera.follow(1312,1181,1270,720,2560,1920);
  var oldArrow=QuestFlowPreservationSupport.marker(new WorldMap.Point(528,784),"Toby",camera,1270,720);
  var newArrow=QuestFlowPreservationSupport.marker(new WorldMap.Point(1296,1008),"Toby",camera,1270,720);
  var body=new Rectangle(1240,935,112,125);
  if(image.getWidth()!=1270||image.getHeight()!=720)throw new AssertionError("Unexpected historical refinement viewport");
  for(int y=0;y<720;y++)for(int x=0;x<1270;x++)if(before.getRGB(x,y)!=image.getRGB(x,y)&&!body.contains(x+camera.getX(),y+camera.getY())&&!oldArrow.contains(x,y)&&!newArrow.contains(x,y))throw new AssertionError("Unrelated Village refinement pixel "+x+","+y);
  return before;
 }
}
