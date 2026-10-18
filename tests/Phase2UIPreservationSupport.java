import java.nio.file.*;import java.util.*;import java.awt.*;import java.awt.image.BufferedImage;
/** Exact Phase 2 source authentication and UI-only frame projection; earlier guards stay active. */
final class Phase2UIPreservationSupport {
 static Properties allowed()throws Exception{return Phase10PreservationSupport.properties("docs/PHASE2_UI_AUTHORIZED.properties");}
 static byte[] previous(String path,byte[] now){try{
  now=Phase3PreservationSupport.previous(path,now);
  String expected=allowed().getProperty(path);if(expected==null)return now;
  byte[] before=Phase10PreservationSupport.archive(path,"PHASE2_UI");if(before==null)throw new AssertionError("Missing Phase2 UI recovery "+path);
  String hash=ForestPreservationSupport.hash(now),prior=ForestPreservationSupport.hash(before);
  if(!Files.readString(Path.of("docs/PHASE2_UI_BASELINE.json")).contains("\""+path+"\": \""+prior+"\""))throw new AssertionError("Unauthenticated Phase2 UI recovery "+path);
  if(hash.equals(expected))return before;if(hash.equals(prior))return now;
  for(String prefix:java.util.List.of("PHASE1_WORLD","CINEMATIC_PACING","TREE_COLLISION","TOWN_FINAL_FIXES","PHASE10_6C","PHASE10_6C_ADDITIONAL","PHASE10_6B","PHASE10_5_1","PHASE10_5","PHASE10_5_ADDITIONAL","PNG_CRASH_FIX","PHASE10_3","PHASE10_2F","PHASE10_2BE","PHASE10_1","PHASE10","PHASE9_4","PHASE9_3_2B","PHASE9_3_1_1","PHASE9_3_1","PHASE9_2_1","PHASE9_2","QUEST_FLOW","JOURNEY_HOME","FINAL_CHAPTER","VILLAGE_INTEGRATION","VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")){
   byte[] old=Phase10PreservationSupport.archive(path,prefix);if(old!=null&&hash.equals(ForestPreservationSupport.hash(old)))return now;
  }throw new AssertionError("Unreviewed Phase2 UI revision "+path);
 }catch(Exception e){throw new AssertionError(e);}}

 private static Set<String> verifiedAssets;
 static synchronized Set<String> assets()throws Exception{
  if(verifiedAssets!=null)return verifiedAssets;
  var props=Phase10PreservationSupport.properties("docs/PHASE2_UI_ASSETS.properties");if(props.size()!=16)throw new AssertionError("Exactly 15 illustrated RGBA PNGs and provenance README");
  for(String path:props.stringPropertyNames())if(!path.startsWith("assets/ui/phase2/")||!ForestPreservationSupport.hash(Files.readAllBytes(Path.of(path))).equals(props.getProperty(path)))throw new AssertionError("Changed illustrated artwork "+path);
  verifiedAssets=Set.copyOf(props.stringPropertyNames());return verifiedAssets;
 }
 static Rectangle rectangle(Properties p,String key){String[]v=p.getProperty(key).split(",");return new Rectangle(Integer.parseInt(v[0]),Integer.parseInt(v[1]),Integer.parseInt(v[2]),Integer.parseInt(v[3]));}
 static BufferedImage frame(int area,BufferedImage current)throws Exception{
  boolean overview=area==3&&current.getWidth()==1600&&current.getHeight()==1280;
  if(!overview&&(current.getWidth()!=1270||current.getHeight()!=720))throw new AssertionError("Unexpected UI fixture viewport");
  String key=overview?"base-overview":"neutral-"+area;var props=Phase10PreservationSupport.properties("docs/PHASE2_UI_RENDER.properties");
  String hash=ForestPreservationTest.pixelHash(current);if(hash.equals(props.getProperty(key+".before")))return current;
  if(!hash.equals(props.getProperty(key+".after"))){
   // Recognize only exact immutable images already projected by an earlier guard.
   try(var files=Files.list(Path.of("docs"))){for(var file:files.filter(p->p.toString().endsWith(".properties")&&p.getFileName().toString().contains("RENDER")).toList()){
    if(Phase10PreservationSupport.properties(file.toString()).containsValue(hash))return current;
   }}throw new AssertionError("Unauthenticated UI frame "+key);
  }
  Path path=Path.of("tests/fixtures/phase2-ui-"+key+"-before.png");if(!ForestPreservationSupport.hash(Files.readAllBytes(path)).equals(props.getProperty(key+".file")))throw new AssertionError("Changed UI baseline "+key);
  var before=javax.imageio.ImageIO.read(path.toFile());Rectangle prior=rectangle(props,key+".quest-before"),next=rectangle(props,key+".quest-after");
  Rectangle skills=new Rectangle(overview?573:408,current.getHeight()-118,454,104);
  if(!props.getProperty(key+".arrow-before").equals(props.getProperty(key+".arrow-after")))throw new AssertionError("Neutral objective marker moved unexpectedly "+key);
  for(int y=0;y<current.getHeight();y++)for(int x=0;x<current.getWidth();x++)if(current.getRGB(x,y)!=before.getRGB(x,y)&&!prior.contains(x,y)&&!next.contains(x,y)&&!skills.contains(x,y))throw new AssertionError("World/HP/boss HUD changed outside exact UI bounds "+key+":"+x+","+y);
  return before;
 }
}
