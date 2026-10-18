import java.nio.file.*;
import java.util.*;

/** Authenticate this small polish revision before retaining the complete Phase10 guards. */
final class Phase101PreservationSupport {
 static Properties allowed()throws Exception{return Phase10PreservationSupport.properties("docs/PHASE10_1_AUTHORIZED.properties");}
 static byte[] previous(String path,byte[] current){try{
  current=Phase102BEPreservationSupport.previous(path,current);
  String expected=allowed().getProperty(path);if(expected==null||path.startsWith("assets/"))return current;
  byte[] before=Phase10PreservationSupport.archive(path,"PHASE10_1");
  if(before==null)throw new AssertionError("Missing polish baseline "+path);
  String hash=ForestPreservationSupport.hash(current);
  if(hash.equals(ForestPreservationSupport.hash(before)))return current;
  if(hash.equals(expected))return before;
  // Already projected historical bytes must remain accepted by their older guards.
  for(String prefix:List.of("PHASE10","PHASE9_4","PHASE9_3_2B","PHASE9_3_1_1","PHASE9_3_1","PHASE9_2_1","PHASE9_2","QUEST_FLOW","JOURNEY_HOME","FINAL_CHAPTER","VILLAGE_INTEGRATION","VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")){
   byte[] old=Phase10PreservationSupport.archive(path,prefix);if(old!=null&&hash.equals(ForestPreservationSupport.hash(old)))return current;
  }
  throw new AssertionError("Unauthenticated polish owner "+path);
 }catch(Exception e){throw new AssertionError(e);}}
 static void asset(String path)throws Exception{
  String current=ForestPreservationSupport.hash(Files.readAllBytes(Path.of(path)));
  String revised=allowed().getProperty(path);
  if(revised==null){if(!current.equals(Phase10PreservationSupport.properties("docs/PHASE10_ASSETS.properties").getProperty(path)))throw new AssertionError("Changed original boss asset "+path);return;}
  if(!current.equals(revised))throw new AssertionError("Unauthenticated polished asset "+path);
  String old=Phase10PreservationSupport.properties("docs/PHASE10_ASSETS.properties").getProperty(path);
  if(old!=null&&!Files.readString(Path.of("docs/PHASE10_1_BASELINE.json")).contains("\""+path+"\": \""+old+"\""))throw new AssertionError("Polish baseline does not match Phase10 asset "+path);
 }
}
