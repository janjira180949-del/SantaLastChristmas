import java.nio.file.*;import java.util.*;import java.awt.geom.Rectangle2D;import game.*;
/** Exact local tree correction; original world geometry and prior revisions stay authenticated. */
final class TreeCollisionPreservationSupport{
 static Properties allowed()throws Exception{return Phase10PreservationSupport.properties("docs/TREE_COLLISION_AUTHORIZED.properties");}
 static byte[] previous(String path,byte[] now){try{
  now=CinematicPacingPreservationSupport.previous(path,now);
  String expected=allowed().getProperty(path);if(expected==null)return now;
  byte[] before=Phase10PreservationSupport.archive(path,"TREE_COLLISION");if(before==null)throw new AssertionError("Missing tree recovery owner "+path);
  String hash=ForestPreservationSupport.hash(now),prior=ForestPreservationSupport.hash(before);
  if(!Files.readString(Path.of("docs/TREE_COLLISION_BASELINE.json")).contains("\""+path+"\": \""+prior+"\""))throw new AssertionError("Unauthenticated tree recovery "+path);
  if(hash.equals(expected))return before;if(hash.equals(prior))return now;
  for(String prefix:List.of("TOWN_FINAL_FIXES","PHASE10_6C","PHASE10_6C_ADDITIONAL","PHASE10_6B","PHASE10_5_1","PHASE10_5","PHASE10_5_ADDITIONAL","PNG_CRASH_FIX","PHASE10_3","PHASE10_2F","PHASE10_2BE","PHASE10_1","PHASE10","PHASE9_4","PHASE9_3_2B","PHASE9_3_1_1","PHASE9_3_1","PHASE9_2_1","PHASE9_2","QUEST_FLOW","JOURNEY_HOME","FINAL_CHAPTER","VILLAGE_INTEGRATION","VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")){
   byte[] old=Phase10PreservationSupport.archive(path,prefix);if(old!=null&&hash.equals(ForestPreservationSupport.hash(old)))return now;
  }throw new AssertionError("Unreviewed tree correction "+path);
 }catch(Exception e){throw new AssertionError(e);}}
 static java.util.List<Rectangle2D> previousSolids(WorldMap map){
  var result=new ArrayList<>(Phase1WorldPreservationSupport.previousSolids(map));if(map.id().equals("village")){
   var base=new java.awt.Rectangle(1264,856,64,24);
   if(result.stream().filter(base::equals).count()!=1)throw new AssertionError("Exactly one approved tree base required");
   result.remove(base);
  }return result;
 }
}
