import game.*;
import entity.*;
import boss.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Actual art, active clocks, exact protected inventory and matched renderer evidence. */
public class Phase105VisualTest extends FinalChapterTest {
 static BufferedImage image(String path)throws Exception{return ImageIO.read(Path.of(path).toFile());}
 static void exact(String first,String second,String message)throws Exception{check(Phase105PreservationSupport.equal(image(first),image(second)),message);}
 static void inventory()throws Exception{
  var allowed=Phase105PreservationSupport.allowed();var actual=new HashSet<String>();
  var entries=java.util.regex.Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/PHASE10_5_BASELINE.json")));
  while(entries.find()){
   String p=entries.group(1),prior=entries.group(2);byte[] bytes=Phase1051PreservationSupport.previous(p,Files.readAllBytes(Path.of(p)));String hash=ForestPreservationSupport.hash(bytes);
   if(!hash.equals(prior))actual.add(p);
   check(hash.equals(allowed.getProperty(p,prior)),"Exact authorized current owner or protected original "+p);
   check(ForestPreservationSupport.hash(Phase105PreservationSupport.previous(p,bytes)).equals(prior),"Exact recovered pre-polish bytes "+p);
  }
  check(actual.equals(allowed.stringPropertyNames())&&actual.size()==14,"Only eight presentation owners and six scoped test migrations differ");
  check(!actual.contains("src/util/ImageAssets.java")&&!actual.contains("src/boss/DarkTitan.java")&&!actual.contains("src/boss/BossAttacks.java")&&!actual.contains("src/ui/HUD.java")&&!actual.contains("src/game/Camera.java"),"PNG fix, combat, HUD and normal Camera implementation exact");
  check(Child.Kind.values().length==1&&Child.Kind.values()[0]==Child.Kind.TOY,"The actual child roster is only the protected Toby");
  var p=playing();check(p.getChildren().size()==1&&p.getChildren().get(0).getName().equals("Toby"),"No other child exists or has been invented");p.stopGame();
  check(TownCelebration.names().equals(java.util.List.of("Martha","Oliver","Noah","Iris","Clara","Ben")),"Six original resident identities retained alongside Toby");
 }
 static void animation()throws Exception{
  BossArtwork.prepareAsync().get();
  for(String name:new String[]{"heavy_walk","beast_charge"}){
   var sheet=image("assets/phase10_5/titan/"+name+"_sheet.png");var hashes=new HashSet<String>();
   check(sheet.getWidth()==2048&&sheet.getHeight()==2048&&sheet.getColorModel().hasAlpha(),"Four by four RGBA sheet "+name);
   for(int f=0;f<16;f++){
    String path=String.format("assets/phase10_5/titan/%s/frame_%02d.png",name,f+1);var im=image(path);hashes.add(ForestPreservationTest.pixelHash(im));
    check(im.getWidth()==512&&im.getHeight()==512&&im.getColorModel().hasAlpha(),"512 RGBA source-derived pose "+name+f);
    int bottom=0;for(int y=0;y<512;y++)for(int x=0;x<512;x++)if((im.getRGB(x,y)>>>24)>40)bottom=Math.max(bottom,y);
    check(bottom>=492&&bottom<=496,"Stable approved feet anchor "+name+f);
    check((im.getRGB(0,0)>>>24)==0&&(im.getRGB(511,511)>>>24)==0,"Clear outer margins "+name+f);
    check(Phase105PreservationSupport.equal(im,sheet.getSubimage(f%4*512,f/4*512,512,512)),"Every sheet cell exactly matches its frame "+name+f);
    check(Phase105PreservationSupport.equal(util.PixelArt.scale(im,256,256),BossArtwork.frame("titan",name,f)),"Actual cached runtime uses corrected pose "+name+f);
   }check(hashes.size()==16,"Sixteen distinct source poses retained, not duplicates "+name);
  }
  check(Phase105PreservationSupport.assets().size()==35,"Exact two sequences, sheets and provenance file");
  for(String p:Phase105PreservationSupport.assets().stringPropertyNames())check(ForestPreservationSupport.hash(Files.readAllBytes(Path.of(p))).equals(Phase105PreservationSupport.assets().getProperty(p)),"Exact new asset "+p);
 }
 static void tree()throws Exception{
  TownChapterArt.prepareAsync().get();
  var cache=TownChapterArt.class.getDeclaredField("ART");cache.setAccessible(true);
  var future=(java.util.concurrent.CompletableFuture<?>)cache.get(null);Object art=future.get();
  var blended=(BufferedImage[][])field(art,"corruption");var restored=(BufferedImage[])field(art,"restored");
  check(blended.length==2&&blended[0].length==17,"Prepared full/compact crossfade states, no paint filtering");
  check(restored[0].getWidth()==384&&restored[0].getHeight()==320&&restored[1].getWidth()==326&&restored[1].getHeight()==272,"15 percent compact viewport treatment preserves aspect within rounding");
  var a=TownChapterArt.tree(0);var b=TownChapterArt.tree(1);var middle=blended[0][8];
  for(int y=0;y<320;y+=5)for(int x=0;x<384;x+=5)check(Math.abs((middle.getRGB(x,y)>>>24)-((a.getRGB(x,y)>>>24)+(b.getRGB(x,y)>>>24))/2.0)<=.5,"Mid-transition alpha has no layered-fade opacity dip");
  var root=WorldMap.forArea(Area.Type.CHRISTMAS_TOWN).point("landmarks/Corrupted Town Center");
  check(root.equals(new WorldMap.Point(1296,1008)),"Fixed authoritative Tree anchor");
  for(int height:new int[]{600,720}){
   var canvas=new BufferedImage(800,height,BufferedImage.TYPE_INT_ARGB);var g=canvas.createGraphics();g.setClip(0,0,800,height);g.translate(400-root.x(),400-root.y());TownChapterArt.drawRestoredTree(g,root);g.dispose();
   var expected=new BufferedImage(800,height,BufferedImage.TYPE_INT_ARGB);g=expected.createGraphics();var frame=restored[height==600?1:0];g.drawImage(frame,400-frame.getWidth()/2,464-frame.getHeight(),null);g.dispose();
   check(Phase105PreservationSupport.equal(canvas,expected),"Same ground anchor and aligned cached frame at "+height);
  }
  check(TownChapterArt.treeCameraLift(root.x()+600,root.y(),root)==0&&TownChapterArt.treeCameraLift(root.x(),root.y(),root)==140,"Camera adjustment is bounded and limited to the Tree vicinity");
  var town=new ChristmasTownVisuals();town.prepare();var hashes=Phase10PreservationSupport.properties("docs/PHASE10_5_RENDER.properties");
  for(String state:new String[]{"cold","restored"}){
   String p="build/phase10-5/before/town-"+state+".png";check(ForestPreservationSupport.hash(Files.readAllBytes(Path.of(p))).equals(hashes.getProperty("terrain-"+state)),"Exact baseline prepared floor authenticated");
   check(Phase105PreservationSupport.equal(image(p),(BufferedImage)field(town,state)),"Every Town terrain/road/building pixel remains identical "+state);
  }
 }
 static void endingAndCamera()throws Exception{
  var root=WorldMap.forArea(Area.Type.CHRISTMAS_TOWN).point("landmarks/Corrupted Town Center");var e=new EndingCutscene();e.startCelebration(root);
  var spirit=new spirit.SpiritTree();for(int i=0;i<239;i++)e.update(spirit);
  check(e.getFrames()==239&&e.getVisualFrames()==239&&e.isCelebration(),"Original 240-update gathering clock");e.update(spirit);
  check(e.getFrames()==0&&e.getVisualFrames()==240&&e.getState()==EndingCutscene.State.CELEBRATION_DIALOGUE,"Presentation clock stays continuous at gratitude handoff");
  e.update(spirit);int visual=e.getVisualFrames();e.advancePresentation();check(e.getVisualFrames()==visual&&e.getCinematicDialogue().getCurrentLine().getSpeakerName().equals("Santa"),"Unchanged next dialogue and continuous visual clock");e.advancePresentation();check(!e.hasStarted(),"Original two-page release of control");
  e.startSleighDeparture();for(int i=0;i<EndingCutscene.SLEIGH_DURATION-1;i++)e.update(spirit);check(e.getFrames()==EndingCutscene.SLEIGH_DURATION-1,"No shortening of sleigh sequence");e.update(spirit);check(!e.hasStarted()&&EndingCutscene.duration(EndingCutscene.State.SLEIGH_DEPARTURE)==360,"Authorized departure handoff");
  for(int[] size:new int[][]{{800,600},{1270,720}}){
   var p=Phase102FCinematicTest.entrance(false,size[0],size[1]);closeOverlay(p);tick(p,60);var titan=(DarkTitan)field(p,"activeBoss");titan.takeDamage(1200);tick(p,1);tick(p,BossFallPresentation.TITAN_DURATION-1);var c=(Camera)field(p,"camera");int y=c.getY();tick(p,1);
   check(Math.abs(c.getY()-y)<=1,"Defeat handoff meets normal Tree framing without a jump");Phase102FCinematicTest.normal(p);check(!((SpiritTreeTransformation)field(p,"treeTransformation")).isActive(),"Victory does not auto-restore");
   at(santa(p),(WorldMap.Point)call(p,"spiritTreeInteractionPoint"));tap(p,KeyEvent.VK_E);var transform=(SpiritTreeTransformation)field(p,"treeTransformation");check(transform.isActive()&&transform.getFrames()==0&&p.getState()==GamePanel.State.PLAYING,"E begins playable restoration");
   tap(p,KeyEvent.VK_ESCAPE);tick(p,12);check(transform.getFrames()==0,"Pause freezes restoration");tap(p,KeyEvent.VK_ESCAPE);tick(p,179-transform.getFrames());check(transform.isActive()&&!chapter(p).celebrationStarted(),"No premature celebration");tick(p,1);check(transform.isComplete()&&JourneyHomeTest.ending(p).isCelebration(),"Exactly 180 active updates; unchanged celebration");p.stopGame();
  }
 }
 static void renders()throws Exception{
  for(int area:new int[]{0,1,3}){
   exact("build/phase10-5/before/neutral-"+area+".png","build/phase10-5/after/neutral-"+area+".png","Matched Village/Forest/Base evidence identical "+area);
   check(Phase105PreservationSupport.equal(image("build/phase10-5/before/neutral-"+area+".png"),Phase1051PreservationSupport.frame(area,ForestLandmarkPreservationTest.raw(area))),"Current actual unaffected renderer exact after authenticated NPC-only projection "+area);
  }
  for(String size:new String[]{"800x600","1270x720"})for(String state:new String[]{"idle","talk","celebrate"})for(String light:new String[]{"0.15","1.0"}){
   String name="toby-"+state+"-"+light+"-"+size+".png";exact("build/phase10-5/before/"+name,"build/phase10-5/after/"+name,"Every protected Toby state on light/dark background exact "+name);
  }
  Phase105PreservationSupport.frame(2,ForestLandmarkPreservationTest.raw(2));check(true,"Every current neutral Town pixel matches its authenticated camera/presentation fixture");
 }
 public static void main(String[] args)throws Exception{inventory();animation();tree();endingAndCamera();renders();System.out.println("PASS: "+checks+" visual, alpha, source protection, camera and active-story-clock assertions");}
}
