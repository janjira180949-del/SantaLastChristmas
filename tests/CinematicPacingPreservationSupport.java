import java.nio.file.*;import java.util.*;
/** Exact cinematic-only revision projection. Every prior archive and behavioral guard remains active. */
final class CinematicPacingPreservationSupport{
 static Properties allowed()throws Exception{return Phase10PreservationSupport.properties("docs/CINEMATIC_PACING_AUTHORIZED.properties");}
 static byte[] previous(String path,byte[] now){try{
  now=Phase1WorldPreservationSupport.previous(path,now);
  String expected=allowed().getProperty(path);if(expected==null)return now;
  byte[] before=Phase10PreservationSupport.archive(path,"CINEMATIC_PACING");if(before==null)throw new AssertionError("Missing cinematic recovery "+path);
  String hash=ForestPreservationSupport.hash(now),prior=ForestPreservationSupport.hash(before);
  if(!Files.readString(Path.of("docs/CINEMATIC_PACING_BASELINE.json")).contains("\""+path+"\": \""+prior+"\""))throw new AssertionError("Unauthenticated cinematic recovery "+path);
  if(hash.equals(expected))return before;if(hash.equals(prior))return now;
  for(String prefix:List.of("TREE_COLLISION","TOWN_FINAL_FIXES","PHASE10_6C","PHASE10_6C_ADDITIONAL","PHASE10_6B","PHASE10_5_1","PHASE10_5","PHASE10_5_ADDITIONAL","PNG_CRASH_FIX","PHASE10_3","PHASE10_2F","PHASE10_2BE","PHASE10_1","PHASE10","PHASE9_4","PHASE9_3_2B","PHASE9_3_1_1","PHASE9_3_1","PHASE9_2_1","PHASE9_2","QUEST_FLOW","JOURNEY_HOME","FINAL_CHAPTER","VILLAGE_INTEGRATION","VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")){
   byte[] old=Phase10PreservationSupport.archive(path,prefix);if(old!=null&&hash.equals(ForestPreservationSupport.hash(old)))return now;
  }throw new AssertionError("Unreviewed cinematic revision "+path);
 }catch(Exception e){throw new AssertionError(e);}}
}
