import game.*;import java.awt.*;import java.awt.image.*;import java.nio.file.*;import java.io.*;import java.util.*;import java.security.*;import javax.imageio.ImageIO;
public class VillageEnvironmentPolishTest {
 static Path dir=Path.of("build/village-environment-polish");static int checks;
 static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static boolean equal(BufferedImage a,BufferedImage b){return Arrays.equals(a.getRGB(0,0,a.getWidth(),a.getHeight(),null,0,a.getWidth()),b.getRGB(0,0,b.getWidth(),b.getHeight(),null,0,b.getWidth()));}
 static BufferedImage panel(int x,int y,int w,int h)throws Exception{return FinalEnvironmentReview.scene(0,x,y,w,h);}
 static BufferedImage stage(BufferedImage material,java.util.List<VillageEnvironment.Prop> visible)throws Exception{
  var map=WorldMap.forArea(Area.Type.VILLAGE);var cache=WorldMap.class.getDeclaredField("polished");cache.setAccessible(true);var placement=VillageEnvironment.class.getDeclaredField("props");placement.setAccessible(true);Object prior=cache.get(map),priorProps=placement.get(null);
  try{cache.set(map,material);placement.set(null,visible);return panel(1296,1160,1280,800);}finally{cache.set(map,prior);placement.set(null,priorProps);}
 }
 static BufferedImage material(Area.Type type,BufferedImage ground,BufferedImage mask){var road=RoadSnowTexture.apply(ground,mask);return type==Area.Type.VILLAGE?currentTerrain():road;}
 static BufferedImage currentTerrain(){try{return Phase92PreservationSupport.terrain();}catch(Exception ex){throw new AssertionError(ex);}}
 static void save(BufferedImage im,String n)throws Exception{ImageIO.write(im,"png",dir.resolve(n+".png").toFile());}
 static BufferedImage overview(BufferedImage image,boolean props){var im=new BufferedImage(image.getWidth(),image.getHeight(),2);var g=im.createGraphics();g.drawImage(image,0,0,null);if(props)for(var p:VillageEnvironment.props())p.draw(g);g.dispose();return im;}
 public static void main(String[]args)throws Exception {
  VillageRestorationVisuals.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS);
  var map=WorldMap.forArea(Area.Type.VILLAGE);var src=util.ImageAssets.load("assets/world/village_clean.png");var mask=PackedSnowPath.createOverlay(src);var old=RoadSnowTexture.apply(ApprovedSnowGround.polish(src,PackedSnowPath.polish(src),true),mask);var road=VillageRoadSurface.apply(old,mask);
  check(equal(road,VillageRoadSurface.apply(old,mask)),"surface deterministic");long count=0,outside=0,quiet=0;boolean unchanged=true;var md=MessageDigest.getInstance("SHA-256");for(int y=0;y<src.getHeight();y++)for(int x=0;x<src.getWidth();x++){int alpha=mask.getRGB(x,y)>>>24;md.update((byte)alpha);if(alpha==0){outside++;if(road.getRGB(x,y)!=old.getRGB(x,y))unchanged=false;}else {count++;if(road.getRGB(x,y)==0xffdce8ed)quiet++;}}
  check(unchanged,"all outside-road material identical");check(quiet>count*.5,"dominant packed-snow base remains DCE8ED");check(Files.readAllLines(Path.of("build/light-snow-road/prior-mask-sha256.txt")).contains(map.id()+" "+HexFormat.of().formatHex(md.digest())),"mask hash identical");
  var props=VillageEnvironment.props();check(props==VillageEnvironment.props(),"same cached placements");var counts=props.stream().collect(java.util.stream.Collectors.groupingBy(VillageEnvironment.Prop::name,java.util.stream.Collectors.counting()));System.out.println(counts.toString());check(props.size()>=8&&props.size()<75,"controlled decoration density");
  boolean clean=true;for(var p:props){var bounds=p.bounds();for(int y=(int)bounds.getMinY();y<bounds.getMaxY();y++)for(int x=(int)bounds.getMinX();x<bounds.getMaxX();x++)if((mask.getRGB(x,y)>>>24)!=0)clean=false;
   for(var point:map.points().entrySet())if(point.getKey().startsWith("npc/")||point.getKey().startsWith("exit/")||point.getKey().startsWith("entry/")||point.getKey().equals("spawn")){var v=point.getValue();
    if(point.getKey().equals("exit/to_santa_base"))check(!bounds.intersects(v.x()-32,v.y()-24,64,96),"New internal return gate's full actor passage remains free; approved nearby decorative props retained");
    else check(!bounds.intersects(v.x()-135,v.y()-135,270,270),"Original marker clearance");}
  }check(clean,"all prop bounds outside roads");
  
  var field=VillageEnvironment.class.getDeclaredField("props");field.setAccessible(true);field.set(null,null);var regenerated=VillageEnvironment.props();
  check(props.stream().map(p->p.name()+p.bounds()).toList().equals(regenerated.stream().map(p->p.name()+p.bounds()).toList()),"independent regeneration same world placements");
  var stateImage=new BufferedImage(300,300,2);var stateGraphics=stateImage.createGraphics();var state=stateGraphics.getComposite();var canopy=props.stream().filter(p->p.name().startsWith("pine")).findFirst().orElseThrow();stateGraphics.translate(-canopy.x(),-canopy.y()+canopy.image().getHeight());canopy.draw(stateGraphics,true);check(state.equals(stateGraphics.getComposite()),"conditional canopy fade restores Graphics2D state");stateGraphics.dispose();int maxAlpha=0;for(int y=0;y<300;y++)for(int x=0;x<300;x++)maxAlpha=Math.max(maxAlpha,stateImage.getRGB(x,y)>>>24);check(maxAlpha>0&&maxAlpha<=148,"actual canopy fades only for occlusion readability");
  var cameraImage=new BufferedImage(800,600,2);var cg=cameraImage.createGraphics();cg.translate(-200,-300);map.draw(cg,false);cg.dispose();check(equal(cameraImage,currentTerrain().getSubimage(200,300,800,600)),"world material follows camera crop exactly");
  java.util.List<VillageEnvironment.DrawRow> queue=new ArrayList<>();java.util.List<Integer> ordered=new ArrayList<>();queue.add(new VillageEnvironment.DrawRow(70,()->ordered.add(70)));queue.add(new VillageEnvironment.DrawRow(20,()->ordered.add(20)));queue.add(new VillageEnvironment.DrawRow(40,()->ordered.add(40)));VillageEnvironment.drawSorted(queue);check(ordered.equals(java.util.List.of(20,40,70)),"actors and tree props sort by feet");
  var beforeGameplay=stage(old,java.util.List.of());var afterGameplay=stage(road,props);save(beforeGameplay,"01-before");save(stage(road,java.util.List.of()),"02-road-redesign");save(afterGameplay,"03-decoration-pass");save(overview(road,true),"04-full-village");save(panel(1300,530,1100,700),"05-road-closeup");var cluster=props.stream().filter(p->p.name().equals("pine_medium")).min(Comparator.comparingInt(p->Math.abs(p.x()-810)+Math.abs(p.y()-1410))).orElseThrow();save(panel(cluster.x()+90,cluster.y()-50,1100,700),"06-tree-cluster");save(panel(528,736,1100,700),"07-toby-area");save(panel(1296,980,1100,700),"08-intersection");var full=afterGameplay;var comparison=new BufferedImage(beforeGameplay.getWidth()*2,beforeGameplay.getHeight()+32,2);var g=comparison.createGraphics();g.setColor(new Color(20,30,40));g.fillRect(0,0,comparison.getWidth(),comparison.getHeight());g.setColor(Color.WHITE);g.drawString("BEFORE",20,22);g.drawString("POLISHED",beforeGameplay.getWidth()+20,22);g.drawImage(beforeGameplay,0,32,null);g.drawImage(full,beforeGameplay.getWidth(),32,null);g.dispose();save(comparison,"09-wide-comparison");check(equal(panel(1296,1160,1280,800),panel(1296,1160,1280,800)),"gameplay repeatable");
  System.out.println("PASS: "+checks+" Village checks; "+props.size()+" reused props; "+count+" road-mask pixels unchanged in extent; "+outside+" outside-road surface pixels unchanged; nine review captures");
 }
}





