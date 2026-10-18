import java.nio.file.*;import java.util.*;import java.awt.*;import java.awt.image.BufferedImage;
final class SantaSkillsPreservationSupport {
 static byte[] previous(String path,byte[] bytes){bytes=Phase10PreservationSupport.previous(path,bytes);try{
  var props=new Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_4_AUTHORIZED.properties"))){props.load(in);}if(!props.containsKey(path))return bytes;
  byte[] old=JourneyHomePreservationSupport.archived(path,"PHASE9_4");String hash=ForestPreservationSupport.hash(bytes);
  if(hash.equals(props.getProperty(path)))return old;if(hash.equals(ForestPreservationSupport.hash(old)))return bytes;
  for(String prefix:java.util.List.of("PHASE9_3_2B","PHASE9_3_1_1","PHASE9_3_1","PHASE9_2_1","PHASE9_2","QUEST_FLOW","JOURNEY_HOME","FINAL_CHAPTER","VILLAGE_INTEGRATION","VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")){
   byte[] prior=JourneyHomePreservationSupport.archived(path,prefix);if(prior!=null&&hash.equals(ForestPreservationSupport.hash(prior)))return bytes;
  }throw new AssertionError("Unauthenticated skills revision owner: "+path);
 }catch(Exception e){throw new AssertionError(e);}}
 static BufferedImage frame(int area,BufferedImage current)throws Exception{
  current=Phase102BEPreservationSupport.frame(area,current);
  var hashes=new Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_4_RENDER.properties"))){hashes.load(in);}
  boolean overview=area==3&&current.getWidth()==1600&&current.getHeight()==1280;String key=overview?"base-overview":"frame-"+area;
  var path=Path.of("tests/fixtures/phase9-4-"+key+"-before.png");if(!ForestPreservationSupport.hash(Files.readAllBytes(path)).equals(hashes.getProperty(key)))throw new AssertionError("Unauthenticated skill HUD baseline");
  var before=javax.imageio.ImageIO.read(path.toFile());if(!overview&&(current.getWidth()!=1270||current.getHeight()!=720))throw new AssertionError("Unexpected skill baseline viewport");
  // Only the old/new ability card union may change. All world and other HUD pixels remain exact.
  var hud=new Rectangle(overview?573:408,current.getHeight()-118,454,104);for(int y=0;y<current.getHeight();y++)for(int x=0;x<current.getWidth();x++)if(before.getRGB(x,y)!=current.getRGB(x,y)&&!hud.contains(x,y))throw new AssertionError("Unrelated skill renderer pixel "+x+","+y);
  return before;
 }
}
