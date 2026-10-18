import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;

/** Authenticates exact audio-only revisions before retaining all historical guards. */
final class Phase106PreservationSupport {
 private static Properties allowed;
 static Properties allowed()throws Exception{
  if(allowed==null)allowed=Phase10PreservationSupport.properties("docs/PHASE10_6B_AUTHORIZED.properties");
  return allowed;
 }
 static Set<String> assets()throws Exception{
  Properties assets=Phase10PreservationSupport.properties("docs/PHASE10_6B_ASSETS.properties");
  if(assets.size()!=106)throw new AssertionError("Exact99 WAVs and seven runtime documents required");
  for(String path:assets.stringPropertyNames()){
   if(!path.startsWith("assets/audio/phase10_6/")||!ForestPreservationSupport.hash(Phase106CPreservationSupport.previous(path,Files.readAllBytes(Path.of(path)))).equals(assets.getProperty(path)))throw new AssertionError("Unreviewed new runtime audio asset "+path);
  }
  return assets.stringPropertyNames();
 }
 static byte[] previous(String path,byte[] current){try{
  current=Phase106CPreservationSupport.previous(path,current);
  String expected=allowed().getProperty(path);
  if(expected==null)expected=Phase10PreservationSupport.properties("docs/PHASE10_6B_EXTERNAL.properties").getProperty(path);
  if(expected==null)return current;
  byte[] before=Phase10PreservationSupport.archive(path,"PHASE10_6B");
  if(before==null)throw new AssertionError("Missing audio recovery owner "+path);
  String hash=ForestPreservationSupport.hash(current),prior=ForestPreservationSupport.hash(before);
  if(!Files.readString(Path.of("docs/PHASE10_6B_BASELINE.json")).contains("\""+path+"\": \""+prior+"\""))throw new AssertionError("Unauthenticated audio recovery "+path);
  if(hash.equals(expected))return before;
  if(hash.equals(prior))return current;
  // Older guards can pass an already authenticated historical revision onward.
  for(String prefix:List.of("PHASE10_5_1","PHASE10_5","PHASE10_5_ADDITIONAL","PNG_CRASH_FIX","PHASE10_3","PHASE10_2F","PHASE10_2BE","PHASE10_1","PHASE10","PHASE9_4","PHASE9_3_2B","PHASE9_3_1_1","PHASE9_3_1","PHASE9_2_1","PHASE9_2","QUEST_FLOW","JOURNEY_HOME","FINAL_CHAPTER","VILLAGE_INTEGRATION","VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")){
   byte[] old=Phase10PreservationSupport.archive(path,prefix);if(old!=null&&hash.equals(ForestPreservationSupport.hash(old)))return current;
  }
  throw new AssertionError("Unreviewed audio revision "+path);
 }catch(Exception e){throw new AssertionError(e);}}
}
