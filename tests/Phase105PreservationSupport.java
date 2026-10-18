import java.nio.file.*;
import java.util.*;
import java.awt.image.BufferedImage;

/** Exact Phase 10.5 authentication before retaining the entire historical guard chain. */
final class Phase105PreservationSupport {
 static Properties allowed()throws Exception{return Phase10PreservationSupport.properties("docs/PHASE10_5_AUTHORIZED.properties");}
 static Properties assets()throws Exception{return Phase10PreservationSupport.properties("docs/PHASE10_5_ASSETS.properties");}
 static byte[] previous(String path,byte[] current){try{
  current=Phase1051PreservationSupport.previous(path,current);
  String expected=allowed().getProperty(path);if(expected==null)return current;
  byte[] before=Phase10PreservationSupport.archive(path,"PHASE10_5");
  if(before==null)before=Phase10PreservationSupport.archive(path,"PHASE10_5_ADDITIONAL");
  if(before==null)throw new AssertionError("Missing polish recovery owner "+path);
  String prior=ForestPreservationSupport.hash(before);
  if(!Files.readString(Path.of("docs/PHASE10_5_BASELINE.json")).contains("\""+path+"\": \""+prior+"\""))throw new AssertionError("Unauthenticated polish recovery "+path);
  String hash=ForestPreservationSupport.hash(current);
  if(hash.equals(expected))return before;
  if(hash.equals(prior))return current;
  for(String prefix:List.of("PNG_CRASH_FIX","PHASE10_3","PHASE10_2F","PHASE10_2BE","PHASE10_1","PHASE10","PHASE9_4","PHASE9_3_2B","PHASE9_3_1_1","PHASE9_3_1","PHASE9_2_1","PHASE9_2","QUEST_FLOW","JOURNEY_HOME","FINAL_CHAPTER","VILLAGE_INTEGRATION","VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")){
   byte[] old=Phase10PreservationSupport.archive(path,prefix);
   if(old!=null&&hash.equals(ForestPreservationSupport.hash(old)))return current;
  }
  throw new AssertionError("Unauthenticated visual polish "+path);
 }catch(Exception e){throw new AssertionError(e);}}
 static BufferedImage frame(int area,BufferedImage current)throws Exception{
  current=Phase1051PreservationSupport.frame(area,current);
  if(area!=2)return current;
  Properties hashes=Phase10PreservationSupport.properties("docs/PHASE10_5_RENDER.properties");
  BufferedImage before=fixture("before",hashes),approved=fixture("current",hashes);
  if(equal(current,before))return current;
  if(!equal(current,approved))throw new AssertionError("Town differs from its exact authorized local-tree/camera frame");
  return before;
 }
 private static BufferedImage fixture(String state,Properties hashes)throws Exception{
  Path p=Path.of("tests/fixtures/phase10-5-town-"+state+".png");
  if(!ForestPreservationSupport.hash(Files.readAllBytes(p)).equals(hashes.getProperty(state)))throw new AssertionError("Changed polish renderer fixture "+state);
  return javax.imageio.ImageIO.read(p.toFile());
 }
 static boolean equal(BufferedImage a,BufferedImage b){return a.getWidth()==b.getWidth()&&a.getHeight()==b.getHeight()&&Arrays.equals(a.getRGB(0,0,a.getWidth(),a.getHeight(),null,0,a.getWidth()),b.getRGB(0,0,b.getWidth(),b.getHeight(),null,0,b.getWidth()));}
}
