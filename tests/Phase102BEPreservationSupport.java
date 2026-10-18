import java.nio.file.*;
import java.util.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/** Authenticate the exact approved revision before retaining all earlier guards. */
final class Phase102BEPreservationSupport {
 static Properties allowed() throws Exception {return Phase10PreservationSupport.properties("docs/PHASE10_2BE_AUTHORIZED.properties");}
 static byte[] previous(String path,byte[] current) {
  current=Phase102FPreservationSupport.previous(path,current);
  try {
   String expected=allowed().getProperty(path);if(expected==null)return current;
   byte[] before=Phase10PreservationSupport.archive(path,"PHASE10_2BE");
   if(before==null)throw new AssertionError("Missing exact task baseline "+path);
   String hash=ForestPreservationSupport.hash(current);
   if(hash.equals(expected))return before;
   if(hash.equals(ForestPreservationSupport.hash(before)))return current;
   for(String prefix:java.util.List.of("PHASE10_1","PHASE10","PHASE9_4","PHASE9_3_2B","PHASE9_3_1_1","PHASE9_3_1","PHASE9_2_1","PHASE9_2","QUEST_FLOW","JOURNEY_HOME","FINAL_CHAPTER","VILLAGE_INTEGRATION","VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")) {
    byte[] old=Phase10PreservationSupport.archive(path,prefix);
    if(old!=null&&hash.equals(ForestPreservationSupport.hash(old)))return current;
   }
   throw new AssertionError("Unauthenticated task revision "+path);
  }catch(Exception e){throw new AssertionError(e);}
 }
 static BufferedImage frame(int area,BufferedImage current)throws Exception {
  current=Phase103PreservationSupport.frame(area,current);
  boolean overview=area==3&&current.getWidth()==1600&&current.getHeight()==1280;
  if(!overview&&(current.getWidth()!=1270||current.getHeight()!=720))throw new AssertionError("Unexpected task baseline viewport");
  String key=overview?"base-overview":"neutral-"+area;
  Path path=Path.of("tests/fixtures/phase10-2be-"+key+".png");
  Properties hashes=Phase10PreservationSupport.properties("docs/PHASE10_2BE_RENDER.properties");
  if(!ForestPreservationSupport.hash(Files.readAllBytes(path)).equals(hashes.getProperty(key)))throw new AssertionError("Changed approved before renderer "+key);
  BufferedImage before=javax.imageio.ImageIO.read(path.toFile());
  // The neutral fixture uses the original camera follow, never a whole-map exemption.
  var camera=new game.Camera();camera.follow(1312,area==0?1181:1157,1270,720,area==1?3072:area==3?1600:2560,area==1?2304:area==3?1280:1920);
  for(int y=0;y<current.getHeight();y++)for(int x=0;x<current.getWidth();x++)if(current.getRGB(x,y)!=before.getRGB(x,y)) {
   int wx=x+camera.getX(),wy=y+camera.getY();boolean authorized=false;
   if(area==1)for(int[] p:new int[][]{{976,1520},{2032,1040},{2640,1712}})if(new Rectangle(p[0]-150,p[1]-180,300,270).contains(wx,wy))authorized=true;
   if(area==2)authorized=new Rectangle(976,1096,640,400).contains(wx,wy); // Exact historical arena after authenticated Phase 10.3 projection.
   if(!authorized)throw new AssertionError("Unrelated approved world/HUD pixel "+area+":"+x+","+y);
  }
  return before;
 }
}
