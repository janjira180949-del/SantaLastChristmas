import java.nio.file.*;
import java.util.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/** Exact NPC-only revision authentication before the retained historical guard chain. */
final class Phase1051PreservationSupport {
 static Properties allowed()throws Exception{return Phase10PreservationSupport.properties("docs/PHASE10_5_1_AUTHORIZED.properties");}
 static Properties assets()throws Exception{return Phase10PreservationSupport.properties("docs/PHASE10_5_1_ASSETS.properties");}
 static byte[] previous(String path,byte[] current){try{
  current=Phase106PreservationSupport.previous(path,current);
  String expected=allowed().getProperty(path);if(expected==null)return current;
  byte[] before=Phase10PreservationSupport.archive(path,"PHASE10_5_1");
  if(before==null)throw new AssertionError("Missing NPC recovery owner "+path);
  String hash=ForestPreservationSupport.hash(current),prior=ForestPreservationSupport.hash(before);
  if(!Files.readString(Path.of("docs/PHASE10_5_1_BASELINE.json")).contains("\""+path+"\": \""+prior+"\""))throw new AssertionError("Unauthenticated NPC recovery "+path);
  if(hash.equals(expected))return before;
  if(hash.equals(prior))return current;
  // Later projections may already have authenticated and recovered an older immutable revision.
  for(String prefix:java.util.List.of("PHASE10_5","PHASE10_5_ADDITIONAL","PNG_CRASH_FIX","PHASE10_3","PHASE10_2F","PHASE10_2BE","PHASE10_1","PHASE10","PHASE9_4","PHASE9_3_2B","PHASE9_3_1_1","PHASE9_3_1","PHASE9_2_1","PHASE9_2","QUEST_FLOW","JOURNEY_HOME","FINAL_CHAPTER","VILLAGE_INTEGRATION","VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")){
   byte[] old=Phase10PreservationSupport.archive(path,prefix);if(old!=null&&hash.equals(ForestPreservationSupport.hash(old)))return current;
  }
  throw new AssertionError("Unauthenticated NPC edge revision "+path);
 }catch(Exception e){throw new AssertionError(e);}}
 static BufferedImage frame(int area,BufferedImage current)throws Exception{
  current=TownFinalFixPreservationSupport.frame(area,current);
  if(area!=0)return current;
  var hashes=Phase10PreservationSupport.properties("docs/PHASE10_5_1_RENDER.properties");
  BufferedImage before=fixture("before",hashes),approved=fixture("current",hashes);
  if(Phase105PreservationSupport.equal(current,before))return current;
  if(!Phase105PreservationSupport.equal(current,approved))throw new AssertionError("Village differs from exact NPC-only approved renderer fixture");
  var camera=new game.Camera();camera.follow(1312,1181,1270,720,2560,1920);
  for(int y=0;y<current.getHeight();y++)for(int x=0;x<current.getWidth();x++)if(current.getRGB(x,y)!=before.getRGB(x,y)){
   boolean inside=false;
   for(var site:game.VillageActivities.Site.values())if(site.resident()&&new Rectangle(site.x-24,site.y-34,48,60).contains(x+camera.getX(),y+camera.getY()))inside=true;
   if(!inside)throw new AssertionError("Changed Village pixel outside an original resident canvas "+x+","+y);
  }
  return before;
 }
 private static BufferedImage fixture(String state,Properties hashes)throws Exception{
  Path p=Path.of("tests/fixtures/phase10-5-1-village-"+state+".png");
  if(!ForestPreservationSupport.hash(Files.readAllBytes(p)).equals(hashes.getProperty(state)))throw new AssertionError("Changed NPC renderer fixture "+state);
  return javax.imageio.ImageIO.read(p.toFile());
 }
}
