import java.nio.file.*;import java.util.*;import java.awt.image.BufferedImage;
/** Exact Town refinements; preceding hashes, archives and behavioral guards remain intact. */
final class TownFinalFixPreservationSupport {
 static Properties allowed()throws Exception{return Phase10PreservationSupport.properties("docs/TOWN_FINAL_FIXES_AUTHORIZED.properties");}
 static byte[] previous(String path,byte[] now){try{
  now=TreeCollisionPreservationSupport.previous(path,now);
  String expected=allowed().getProperty(path);if(expected==null)return now;
  byte[] before=Phase10PreservationSupport.archive(path,"TOWN_FINAL_FIXES");if(before==null)throw new AssertionError("Missing Town recovery owner "+path);
  String hash=ForestPreservationSupport.hash(now),prior=ForestPreservationSupport.hash(before);
  if(!Files.readString(Path.of("docs/TOWN_FINAL_FIXES_BASELINE.json")).contains("\""+path+"\": \""+prior+"\""))throw new AssertionError("Unauthenticated Town recovery "+path);
  if(hash.equals(expected))return before;if(hash.equals(prior))return now;
  for(String prefix:List.of("PHASE10_6C","PHASE10_6C_ADDITIONAL","PHASE10_6B","PHASE10_5_1","PHASE10_5","PHASE10_5_ADDITIONAL","PNG_CRASH_FIX","PHASE10_3","PHASE10_2F","PHASE10_2BE","PHASE10_1","PHASE10","PHASE9_4","PHASE9_3_2B","PHASE9_3_1_1","PHASE9_3_1","PHASE9_2_1","PHASE9_2","QUEST_FLOW","JOURNEY_HOME","FINAL_CHAPTER","VILLAGE_INTEGRATION","VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")){
   byte[] old=Phase10PreservationSupport.archive(path,prefix);if(old!=null&&hash.equals(ForestPreservationSupport.hash(old)))return now;
  }throw new AssertionError("Unreviewed Town refinement "+path);
 }catch(Exception e){throw new AssertionError(e);}}
 static Set<String> assets()throws Exception{
  var p=Phase10PreservationSupport.properties("docs/TOWN_FINAL_FIXES_ASSETS.properties");if(p.size()!=5)throw new AssertionError("Three real WAVs and two primary license pages");
  for(String path:p.stringPropertyNames())if(!path.startsWith("assets/audio/phase10_6/")||!ForestPreservationSupport.hash(Files.readAllBytes(Path.of(path))).equals(p.getProperty(path)))throw new AssertionError("Changed Town departure resource "+path);
  return p.stringPropertyNames();
 }
 static BufferedImage fixture(int area)throws Exception{
  Path p=Path.of("tests/fixtures/town-final-before-"+area+".png");var hashes=Phase10PreservationSupport.properties("docs/TOWN_FINAL_FIXES_RENDER.properties");
  if(!ForestPreservationSupport.hash(Files.readAllBytes(p)).equals(hashes.getProperty(Integer.toString(area))))throw new AssertionError("Changed immutable pre-task renderer "+area);
  return javax.imageio.ImageIO.read(p.toFile());
 }
 static BufferedImage frame(int area,BufferedImage current)throws Exception{
  current=Phase1WorldPreservationSupport.frame(area,current);
  if(area!=2)return current;var before=fixture(area);if(Phase105PreservationSupport.equal(current,before))return current;
  if(current.getWidth()!=1270||current.getHeight()!=720)throw new AssertionError("Unexpected neutral viewport");
  // Same raw fixture Santa(1296,1136), normal approved140-pixel Tree camera lift.
  game.Camera camera=new game.Camera();var root=game.WorldMap.forArea(game.Area.Type.CHRISTMAS_TOWN).point("landmarks/Corrupted Town Center");
  camera.follow(1312,1157-game.TownChapterArt.treeCameraLift(1312,1157,root),1270,720,2560,1920);
  var canvas=new java.awt.Rectangle(1716,1330,160,105);
  for(int y=0;y<720;y++)for(int x=0;x<1270;x++)if(before.getRGB(x,y)!=current.getRGB(x,y)&&!canvas.contains(x+camera.getX(),y+camera.getY()))throw new AssertionError("Unrelated Town rendering change "+x+","+y);
  return before;
 }
}
