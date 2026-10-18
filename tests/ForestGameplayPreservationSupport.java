import java.nio.file.*;import java.util.*;
/** Exact approved feedback/warning changes, authenticated before historical guards. */
final class ForestGameplayPreservationSupport {
 static byte[] previous(String path,byte[] current){current=SantaSkillsPreservationSupport.previous(path,current);try{
  var props=new Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_3_2B_AUTHORIZED.properties"))){props.load(in);}
  if(!props.containsKey(path))return current;
  byte[] old=JourneyHomePreservationSupport.archived(path,"PHASE9_3_2B");
  String hash=ForestPreservationSupport.hash(current);
  if(old!=null&&hash.equals(ForestPreservationSupport.hash(old)))return current;
  if(hash.equals(props.getProperty(path)))return old;
  // An earlier authenticated projection may already be in use.
  for(String prefix:List.of("PHASE9_3_1_1","PHASE9_3_1","PHASE9_2_1","PHASE9_2","QUEST_FLOW","JOURNEY_HOME","FINAL_CHAPTER","VILLAGE_INTEGRATION","VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")){
   byte[] prior=JourneyHomePreservationSupport.archived(path,prefix);if(prior!=null&&hash.equals(ForestPreservationSupport.hash(prior)))return current;
  }throw new AssertionError("Unauthenticated Forest gameplay feedback owner: "+path);
 }catch(Exception e){throw new AssertionError(e);}}
}
