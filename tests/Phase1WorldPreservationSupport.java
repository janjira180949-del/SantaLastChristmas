import java.nio.file.*;import java.util.*;import java.awt.*;import java.awt.image.BufferedImage;
/** Authenticates exact new revisions and only the local gate/rest footprints before older checks. */
final class Phase1WorldPreservationSupport {
 static Properties allowed()throws Exception{return Phase10PreservationSupport.properties("docs/PHASE1_WORLD_AUTHORIZED.properties");}
 static byte[] previous(String path,byte[] now){try{
  now=Phase2UIPreservationSupport.previous(path,now);
  String expected=allowed().getProperty(path);if(expected==null)return now;
  byte[] before=Phase10PreservationSupport.archive(path,"PHASE1_WORLD");if(before==null)throw new AssertionError("Missing Phase1 recovery "+path);
  String hash=ForestPreservationSupport.hash(now),prior=ForestPreservationSupport.hash(before);
  if(!Files.readString(Path.of("docs/PHASE1_WORLD_BASELINE.json")).contains("\""+path+"\": \""+prior+"\""))throw new AssertionError("Unauthenticated Phase1 recovery "+path);
  if(hash.equals(expected))return before;if(hash.equals(prior))return now;
  for(String prefix:java.util.List.of("CINEMATIC_PACING","TREE_COLLISION","TOWN_FINAL_FIXES","PHASE10_6C","PHASE10_6C_ADDITIONAL","PHASE10_6B","PHASE10_5_1","PHASE10_5","PHASE10_5_ADDITIONAL","PNG_CRASH_FIX","PHASE10_3","PHASE10_2F","PHASE10_2BE","PHASE10_1","PHASE10","PHASE9_4","PHASE9_3_2B","PHASE9_3_1_1","PHASE9_3_1","PHASE9_2_1","PHASE9_2","QUEST_FLOW","JOURNEY_HOME","FINAL_CHAPTER","VILLAGE_INTEGRATION","VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")){
   byte[] old=Phase10PreservationSupport.archive(path,prefix);if(old!=null&&hash.equals(ForestPreservationSupport.hash(old)))return now;
  }throw new AssertionError("Unreviewed Phase1 revision "+path);
 }catch(Exception e){throw new AssertionError(e);}}
 static BufferedImage fixture(String key)throws Exception{
  var p=Path.of("tests/fixtures/phase1-world-"+key+"-before.png");var hashes=Phase10PreservationSupport.properties("docs/PHASE1_WORLD_RENDER.properties");
  if(!ForestPreservationSupport.hash(Files.readAllBytes(p)).equals(hashes.getProperty(key+".file")))throw new AssertionError("Changed Phase1 renderer baseline "+key);
  return javax.imageio.ImageIO.read(p.toFile());
 }
 static java.util.List<java.awt.geom.Rectangle2D> previousSolids(game.WorldMap map){
  var result=new java.util.ArrayList<>(map.solids());if(map.id().equals("village")){
   var revised=new Rectangle(2200,1304,256,96);int i=result.indexOf(revised);
   if(i<0||result.stream().filter(revised::equals).count()!=1)throw new AssertionError("Exactly one relocated house foundation");
   result.set(i,new java.awt.geom.Rectangle2D.Double(1824,992,384,256));
  }return result;
 }
 static boolean villageRegion(int x,int y){
  if(new Rectangle(1836,920,366,320).contains(x,y)||new Rectangle(2162,1120,366,320).contains(x,y))return true;
  for(var light:game.VillageRestorationVisuals.windowLights()){
   if(new Rectangle(2176,1120,300,280).contains(light[0],light[1])&&(Math.hypot(x-light[0],y-light[1])<=33||Math.hypot(x-light[0]+326,y-light[1]+200)<=33))return true;
  }return false;
 }
 static boolean signPixel(BufferedImage mask,int x,int y){
  if(x<2635||x>=2710||y<1762||y>=1872)return false;
  for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++){int px=x-2635+dx,py=y-1762+dy;if(px>=0&&px<75&&py>=0&&py<110&&(mask.getRGB(px,py)>>>24)>0)return true;}
  return false;
 }
 static BufferedImage terrain(boolean forest,BufferedImage current)throws Exception{
  String key=forest?"forest-terrain":"village-terrain";var before=fixture(key);var hashes=Phase10PreservationSupport.properties("docs/PHASE1_WORLD_RENDER.properties");String pixels=ForestPreservationTest.pixelHash(current);
  if(pixels.equals(hashes.getProperty(key+".before")))return current;
  if(!pixels.equals(hashes.getProperty(key+".after")))throw new AssertionError("Unauthenticated Phase1 terrain "+key);
  var sign=forest?util.PixelArt.scale(javax.imageio.ImageIO.read(Path.of("SantaLastChristmas_MapPackage/assets/environment/decorations/sign.png").toFile()),75,110):null;
  for(int y=0;y<current.getHeight();y++)for(int x=0;x<current.getWidth();x++)if(current.getRGB(x,y)!=before.getRGB(x,y)){
   boolean inside=forest?signPixel(sign,x,y):villageRegion(x,y);
   if(!inside)throw new AssertionError("Unrelated Phase1 terrain pixel "+key+":"+x+","+y);
  }return before;
 }
 static boolean region(int area,int x,int y){
  if(area==3)return new Rectangle(649,973,335,185).contains(x,y);
  if(area==0)return villageRegion(x,y);
  if(area==2){
   if(new Rectangle(1300,810,120,128).contains(x,y))return true; // Complete retired lamp incl. its baked-in pulse.
   for(var h:game.HealingPoint.townRestPoints()){var p=h.getPosition();if(new Rectangle(p.x()-58,p.y()-64,98,40).contains(x,y))return true;}
  }return false;
 }
 static BufferedImage frame(int area,BufferedImage current)throws Exception{
  current=Phase2UIPreservationSupport.frame(area,current);
  if(area!=0&&area!=2&&area!=3)return current;
  boolean overview=area==3&&current.getWidth()==1600&&current.getHeight()==1280;
  if(!overview&&(current.getWidth()!=1270||current.getHeight()!=720))throw new AssertionError("Unexpected Phase1 viewport");
  String key=overview?"base-overview":"neutral-"+area;var before=fixture(key);var hashes=Phase10PreservationSupport.properties("docs/PHASE1_WORLD_RENDER.properties");
  String pixels=ForestPreservationTest.pixelHash(current);
  if(pixels.equals(hashes.getProperty(key+".before")))return current;
  if(!pixels.equals(hashes.getProperty(key+".after")))throw new AssertionError("Unauthenticated Phase1 current renderer "+key);
  var camera=new game.Camera();
  double y=area==0?1181:1157;if(area==2)y-=game.TownChapterArt.treeCameraLift(1312,y,new game.WorldMap.Point(1296,1008));
  if(!overview)camera.follow(1312,y,1270,720,area==3?1600:2560,area==3?1280:1920);
  for(int py=0;py<current.getHeight();py++)for(int px=0;px<current.getWidth();px++)if(current.getRGB(px,py)!=before.getRGB(px,py)&&!region(area,px+camera.getX(),py+camera.getY()))throw new AssertionError("Unrelated Phase1 world/HUD pixel "+area+":"+px+","+py);
  return before;
 }
}
