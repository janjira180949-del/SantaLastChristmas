import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;

/** Exact before/after authentication, before any older preservation projection. */
final class Phase10PreservationSupport {
 static byte[] previous(String path,byte[] current){try{
  current=Phase101PreservationSupport.previous(path,current);
  var approved=properties("docs/PHASE10_AUTHORIZED.properties");if(!approved.containsKey(path))return current;
  String hash=ForestPreservationSupport.hash(current);
  byte[] before=archive(path,"PHASE10");if(before==null)throw new AssertionError("Missing Phase10 baseline "+path);
  if(hash.equals(ForestPreservationSupport.hash(before)))return current;
  if(hash.equals(approved.getProperty(path)))return before;
  for(String prefix:List.of("PHASE9_4","PHASE9_3_2B","PHASE9_3_1_1","PHASE9_3_1","PHASE9_2_1","PHASE9_2","QUEST_FLOW","JOURNEY_HOME","FINAL_CHAPTER","VILLAGE_INTEGRATION","VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")){
   byte[] old=archive(path,prefix);if(old!=null&&hash.equals(ForestPreservationSupport.hash(old)))return current;
  }throw new AssertionError("Unauthenticated Phase10 change "+path);
 }catch(Exception e){throw new AssertionError(e);}}
 static byte[] archive(String path,String prefix)throws Exception {try(var zip=new ZipFile("docs/"+prefix+"_PREVIOUS_SOURCES.zip")){var entry=zip.getEntry(path);return entry==null?null:zip.getInputStream(entry).readAllBytes();}}
 static Properties properties(String path)throws Exception{var p=new Properties();try(var in=Files.newInputStream(Path.of(path))){p.load(in);}return p;}
 static Set<String> assets(){try{var names=new HashSet<>(properties("docs/PHASE10_ASSETS.properties").stringPropertyNames());for(String path:Phase101PreservationSupport.allowed().stringPropertyNames())if(path.startsWith("assets/"))names.add(path);names.addAll(Phase103PreservationSupport.assets().stringPropertyNames());names.addAll(Phase105PreservationSupport.assets().stringPropertyNames());names.addAll(Phase1051PreservationSupport.assets().stringPropertyNames());return names;}catch(Exception e){throw new AssertionError(e);}}
 static void authenticateAsset(String path)throws Exception{if(Phase1051PreservationSupport.assets().containsKey(path)){if(!ForestPreservationSupport.hash(Files.readAllBytes(Path.of(path))).equals(Phase1051PreservationSupport.assets().getProperty(path)))throw new AssertionError("Changed NPC alpha cleanup export "+path);}else if(Phase105PreservationSupport.assets().containsKey(path)){if(!ForestPreservationSupport.hash(Files.readAllBytes(Path.of(path))).equals(Phase105PreservationSupport.assets().getProperty(path)))throw new AssertionError("Changed corrected Titan export "+path);}else if(Phase103PreservationSupport.assets().containsKey(path)){if(!ForestPreservationSupport.hash(Files.readAllBytes(Path.of(path))).equals(Phase103PreservationSupport.assets().getProperty(path)))throw new AssertionError("Changed Town artwork "+path);}else Phase101PreservationSupport.asset(path);}
}
