import java.nio.file.*;
import java.util.*;

/** Exact current-revision authentication before immutable historical comparisons. */
final class PostPhase4Support {
 static Properties allowed,assets,routes;
 static final Map<String,byte[]> originals=new HashMap<>();
 static final Map<String,String> older=new HashMap<>();
 static Properties properties(String name)throws Exception{return Phase10PreservationSupport.properties("docs/POST_PHASE4_"+name+".properties");}
 static byte[] before(String path)throws Exception{
  byte[] b=originals.get(path);if(b==null){b=Phase10PreservationSupport.archive(path,"POST_PHASE4");if(b!=null)originals.put(path,b);}return b;
 }
 static byte[] previous(String path,byte[] current){try{current=FinalStorySupport.previous(path,current);
  if(allowed==null)allowed=properties("AUTHORIZED");String expected=allowed.getProperty(path);if(expected==null)return current;
  String hash=ForestPreservationSupport.hash(current);byte[] prior=before(path);
  if(prior==null)throw new AssertionError("Missing correction recovery "+path);
  if(hash.equals(expected))return prior;
  if(hash.equals(ForestPreservationSupport.hash(prior)))return current;
  for(String prefix:List.of("PHASE4","PHASE3","PHASE2_UI","PHASE1_WORLD","CINEMATIC_PACING","TREE_COLLISION","TOWN_FINAL_FIXES","PHASE10_6C","PHASE10_6B","PHASE10_5_1","PHASE10_5","PHASE10_5_ADDITIONAL","PNG_CRASH_FIX","PHASE10_3","PHASE10_2F","PHASE10_2BE","PHASE10_1","PHASE10","PHASE9_4","PHASE9_3_2B","PHASE9_3_1_1","PHASE9_3_1","PHASE9_2_1","PHASE9_2","QUEST_FLOW","JOURNEY_HOME","FINAL_CHAPTER","VILLAGE_INTEGRATION","VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")){
   String key=path+"@"+prefix,h=older.get(key);if(h==null){byte[] b=Phase10PreservationSupport.archive(path,prefix);h=b==null?"":ForestPreservationSupport.hash(b);older.put(key,h);}if(hash.equals(h))return current;
  }
  throw new AssertionError("Unauthenticated post-Phase4 revision "+path);
 }catch(Exception e){throw new AssertionError(e);}}
 static Set<String> assets()throws Exception{
  if(assets==null)assets=properties("ASSETS");
  if(assets.size()!=15)throw new AssertionError("Exactly seven incoming originals, seven processed WAVs and one manifest");
  for(String p:assets.stringPropertyNames())if(!(p.startsWith("assets/audio/incoming/")||p.startsWith("assets/audio/post_phase4/"))||!ForestPreservationSupport.hash(Files.readAllBytes(Path.of(p))).equals(assets.getProperty(p)))throw new AssertionError("Changed selected recording "+p);
  return assets.stringPropertyNames();
 }
 static boolean route(String id)throws Exception{
  if(routes==null)routes=properties("ROUTES");String p=routes.getProperty(id);if(p==null)return false;
  if(!p.equals(audio.AudioCatalog.cue(id).path())||!assets().contains(p))throw new AssertionError("Incorrect selected-recording route "+id);
  return true;
 }
}
