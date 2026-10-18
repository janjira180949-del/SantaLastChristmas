import java.nio.file.*;
import java.util.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/** Exact revision authentication, followed by all retained historical checks. */
final class Phase103PreservationSupport {
 static Properties allowed()throws Exception{return Phase10PreservationSupport.properties("docs/PHASE10_3_AUTHORIZED.properties");}
 static Properties assets()throws Exception{return Phase10PreservationSupport.properties("docs/PHASE10_3_ASSETS.properties");}
 static byte[] previous(String path,byte[] current){try{
  current=PngCrashPreservationSupport.previous(path,current);
  String expected=allowed().getProperty(path);if(expected==null)return current;
  byte[] before=Phase10PreservationSupport.archive(path,"PHASE10_3");
  if(before==null)throw new AssertionError("Missing Town recovery owner "+path);
  String hash=ForestPreservationSupport.hash(current);
  if(hash.equals(expected))return before;
  if(hash.equals(ForestPreservationSupport.hash(before)))return current;
  for(String prefix:java.util.List.of("PHASE10_2F","PHASE10_2BE","PHASE10_1","PHASE10","PHASE9_4","PHASE9_3_2B","PHASE9_3_1_1","PHASE9_3_1","PHASE9_2_1","PHASE9_2","QUEST_FLOW","JOURNEY_HOME","FINAL_CHAPTER","VILLAGE_INTEGRATION","VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")){
   byte[] old=Phase10PreservationSupport.archive(path,prefix);if(old!=null&&hash.equals(ForestPreservationSupport.hash(old)))return current;
  }
  throw new AssertionError("Unauthenticated Town revision "+path);
 }catch(Exception e){throw new AssertionError(e);}}
 static boolean objectRegion(int x,int y){
  if(new Rectangle(1104,752,384,368).contains(x,y))return true;
  for(int[] h:game.TownChapterArt.HOMES)if(new Rectangle(h[0],h[1],h[2]-h[0],h[3]-h[1]).contains(x,y))return true;
  for(int[] p:new int[][]{{636,1240,132,110},{1776,1240,132,110},{790,540,72,106},{1780,540,72,106},{800,1530,72,106},{1770,1530,72,106},
    {868,1110,42,86},{1710,1110,42,86},{678,1270,48,32},{1840,1270,48,32},{658,1480,114,45},{1760,1480,114,45},{726,1226,38,61},{776,1268,42,52},{1724,1268,42,52}})
   if(new Rectangle(p[0],p[1]-p[3],p[2],p[3]).contains(x,y))return true;
  return false;
 }
 static BufferedImage frame(int area,BufferedImage current)throws Exception{
  current=Phase105PreservationSupport.frame(area,current);
  if(area!=2)return current;
  Path path=Path.of("tests/fixtures/phase10-3-neutral-2-before.png");
  Properties hashes=Phase10PreservationSupport.properties("docs/PHASE10_3_RENDER.properties");
  if(!ForestPreservationSupport.hash(Files.readAllBytes(path)).equals(hashes.getProperty("neutral-2")))throw new AssertionError("Changed authenticated Town before frame");
  var before=javax.imageio.ImageIO.read(path.toFile());if(before.getWidth()!=current.getWidth()||before.getHeight()!=current.getHeight())throw new AssertionError("Mismatched Town camera fixture");
  game.Camera camera=new game.Camera();camera.follow(1312,1157,1270,720,2560,1920);
  var visual=new game.ChristmasTownVisuals();visual.prepare();int[][] windows=(int[][])V2TestSupport.field(visual,"windows");
  int[][] lamps={{912,800},{1680,800},{912,1488},{1680,1488},{1960,1250},{430,1135},{920,240},{1715,195},{2025,310},{380,400},{2048,1198},{2178,1264},{372,1146},{501,1189}};
  for(int y=0;y<current.getHeight();y++)for(int x=0;x<current.getWidth();x++)if(current.getRGB(x,y)!=before.getRGB(x,y)){
   int wx=x+camera.getX(),wy=y+camera.getY();boolean approved=objectRegion(wx,wy)||new Rectangle(976,1096,640,400).contains(wx,wy)||game.BossArenaVisuals.arenaBounds().contains(wx,wy);
   for(int[] p:lamps)if(Math.hypot(wx-p[0],wy-p[1]-42)<=150||Math.hypot(wx-p[0],wy-p[1])<=65)approved=true;
   for(int[] p:windows)if(Math.hypot(wx-p[0],wy-p[1])<=42)approved=true;
   if(!approved)throw new AssertionError("Town change outside exact objects/local lighting/old-new ritual "+wx+","+wy);
  }
  return before;
 }
}
