import java.nio.file.*;import java.util.*;
/** Exact authenticated audio-only revisions; immutable historical fixtures stay intact. */
final class Phase4PreservationSupport {
 private static Properties allowed;private static final Map<String,byte[]> originals=new HashMap<>();
 private static final Map<String,String> older=new HashMap<>();
 static synchronized Properties allowed()throws Exception{if(allowed==null)allowed=Phase10PreservationSupport.properties("docs/PHASE4_AUTHORIZED.properties");return allowed;}
 static byte[] before(String path)throws Exception{byte[] value=originals.get(path);if(value==null){value=Phase10PreservationSupport.archive(path,"PHASE4");if(value!=null)originals.put(path,value);}return value;}
 static byte[] previous(String path,byte[] now){try{now=PostPhase4Support.previous(path,now);
  String expected=allowed().getProperty(path);if(expected==null)return now;
  byte[] before=before(path);if(before==null)throw new AssertionError("Missing Phase4 recovery "+path);
  String hash=ForestPreservationSupport.hash(now);if(hash.equals(expected))return before;
  if(hash.equals(ForestPreservationSupport.hash(before)))return now;
  for(String prefix:List.of("PHASE3","PHASE2_UI","PHASE1_WORLD","CINEMATIC_PACING","TREE_COLLISION","TOWN_FINAL_FIXES","PHASE10_6C","PHASE10_6B","PHASE10_5_1","PHASE10_5","PHASE10_5_ADDITIONAL","PNG_CRASH_FIX","PHASE10_3","PHASE10_2F","PHASE10_2BE","PHASE10_1","PHASE10","PHASE9_4","PHASE9_3_2B","PHASE9_3_1_1","PHASE9_3_1","PHASE9_2_1","PHASE9_2","QUEST_FLOW","JOURNEY_HOME","FINAL_CHAPTER","VILLAGE_INTEGRATION","VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")){
   String key=path+"@"+prefix,oldHash=older.get(key);if(oldHash==null){byte[] old=Phase10PreservationSupport.archive(path,prefix);oldHash=old==null?"":ForestPreservationSupport.hash(old);older.put(key,oldHash);}if(hash.equals(oldHash))return now;
  }throw new AssertionError("Unauthenticated Phase4 revision "+path);
 }catch(Exception e){throw new AssertionError(e);}}
 static String route(String id,String previous)throws Exception{
  if(PostPhase4Support.route(id))return audio.AudioCatalog.cue(id).path();
  String now=audio.AudioCatalog.cue(id).path();
  String approved=Phase10PreservationSupport.properties("docs/PHASE4_ROUTES.properties").getProperty(id,previous);
  if(!now.equals(approved))throw new AssertionError("Unexpected Phase4 event mapping "+id);
  if(now.equals(previous))return previous;
  var assets=Phase10PreservationSupport.properties("docs/PHASE4_ASSETS.properties");
  if(!now.startsWith("assets/audio/phase4/")||!ForestPreservationSupport.hash(Files.readAllBytes(Path.of(now))).equals(assets.getProperty(now)))throw new AssertionError("Unauthenticated audio route "+id);
  return now;
 }
 static int preloadCount(){return (int)audio.AudioCatalog.cues().stream().filter(c->!c.loop()).map(audio.AudioCatalog.Cue::path).distinct().count();}
 static Set<String> assets()throws Exception{var assets=Phase10PreservationSupport.properties("docs/PHASE4_ASSETS.properties");if(assets.size()!=62)throw new AssertionError("59 WAVs and three provenance documents required");for(String p:assets.stringPropertyNames())if(!p.startsWith("assets/audio/phase4/")||!ForestPreservationSupport.hash(Files.readAllBytes(Path.of(p))).equals(assets.getProperty(p)))throw new AssertionError("Unreviewed Phase4 asset "+p);return assets.stringPropertyNames();}
}
