import java.nio.file.*;
import java.util.*;

/** Exact authenticated projection; every previous behavioral and pixel guard still runs. */
final class Phase102FPreservationSupport {
 static Properties allowed()throws Exception{return Phase10PreservationSupport.properties("docs/PHASE10_2F_AUTHORIZED.properties");}
 static byte[] previous(String path,byte[] current){
  current=Phase103PreservationSupport.previous(path,current);
  try{
   String expected=allowed().getProperty(path);if(expected==null)return current;
   byte[] before=Phase10PreservationSupport.archive(path,"PHASE10_2F");
   if(before==null)throw new AssertionError("Missing cinematic baseline "+path);
   String hash=ForestPreservationSupport.hash(current);
   if(hash.equals(expected))return before;
   if(hash.equals(ForestPreservationSupport.hash(before)))return current;
   for(String prefix:List.of("PHASE10_2BE","PHASE10_1","PHASE10","PHASE9_4","PHASE9_3_2B","PHASE9_3_1_1","PHASE9_3_1","PHASE9_2_1","PHASE9_2","QUEST_FLOW","JOURNEY_HOME","FINAL_CHAPTER","VILLAGE_INTEGRATION","VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")){
    byte[] old=Phase10PreservationSupport.archive(path,prefix);
    if(old!=null&&hash.equals(ForestPreservationSupport.hash(old)))return current;
   }
   throw new AssertionError("Unauthenticated cinematic revision "+path);
  }catch(Exception e){throw new AssertionError(e);}
 }
}
