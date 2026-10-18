import java.nio.file.*;import java.util.*;
/** Only exact authenticated Phase10.6C revisions project to the retained prior expectations. */
final class Phase106CPreservationSupport{
 static Properties allowed()throws Exception{return Phase10PreservationSupport.properties("docs/PHASE10_6C_AUTHORIZED.properties");}
 static byte[] previous(String path,byte[] now){try{
  now=TownFinalFixPreservationSupport.previous(path,now);
  String expected=allowed().getProperty(path);if(expected==null)return now;
  byte[] before=Phase10PreservationSupport.archive(path,"PHASE10_6C");if(before==null)before=Phase10PreservationSupport.archive(path,"PHASE10_6C_ADDITIONAL");if(before==null)throw new AssertionError("Missing C baseline "+path);
  String hash=ForestPreservationSupport.hash(now),prior=ForestPreservationSupport.hash(before);
  if(!Files.readString(Path.of("docs/PHASE10_6C_BASELINE.json")).contains("\""+path+"\": \""+prior+"\""))throw new AssertionError("Unauthenticated C archive "+path);
  if(hash.equals(expected))return before;if(hash.equals(prior))return now;
  // Older chained projections are accepted only if their exact archived bytes match.
  for(String prefix:List.of("PHASE10_6B","PHASE10_5_1","PHASE10_5","PHASE10_5_ADDITIONAL","PNG_CRASH_FIX","PHASE10_3","PHASE10_2F","PHASE10_2BE","PHASE10_1","PHASE10","PHASE9_4","PHASE9_3_2B","PHASE9_3_1_1","PHASE9_3_1","PHASE9_2_1","PHASE9_2","QUEST_FLOW","JOURNEY_HOME","FINAL_CHAPTER","VILLAGE_INTEGRATION","VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")){
   byte[] old=Phase10PreservationSupport.archive(path,prefix);if(old!=null&&hash.equals(ForestPreservationSupport.hash(old)))return now;
  }throw new AssertionError("Unreviewed C revision "+path);
 }catch(Exception e){throw new AssertionError(e);}}
 static Set<String> assets()throws Exception{
  var p=Phase10PreservationSupport.properties("docs/PHASE10_6C_ASSETS.properties");
  if(p.size()!=11)throw new AssertionError("Eight new WAVs and three license evidence files");
  for(String path:p.stringPropertyNames())if(!path.startsWith("assets/audio/phase10_6/")||!ForestPreservationSupport.hash(Files.readAllBytes(Path.of(path))).equals(p.getProperty(path)))throw new AssertionError("Unauthenticated C asset "+path);
  return p.stringPropertyNames();
 }
}
