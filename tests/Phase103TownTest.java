import game.*;
import boss.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Actual production clocks/input, replacement art, same combat and complete homecoming. */
public class Phase103TownTest extends FinalChapterTest {
 static final Path OUTPUT=Path.of("build/phase10-3/captures");
 static void shot(GamePanel p,String name)throws Exception{
  // Keep every exact renderer capture; inexpensive lossless encoding avoids host-load timeouts.
  var writer=ImageIO.getImageWritersByFormatName("png").next();var options=writer.getDefaultWriteParam();options.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);options.setCompressionQuality(.9f);
  try(var output=ImageIO.createImageOutputStream(OUTPUT.resolve(name+"-"+p.getWidth()+"x"+p.getHeight()+".png").toFile())){writer.setOutput(output);writer.write(null,new javax.imageio.IIOImage(render(p),null,null),options);}finally{writer.dispose();}
 }
 static void art()throws Exception{
  TownChapterArt.prepareAsync().get();TownCelebration.prepareAsync().get();BossArtwork.prepareAsync().get();
  for(String name:java.util.List.of("heavy_walk","beast_charge")){
   var sheet=ImageIO.read(Path.of("assets/phase10_3/titan/"+name+"_sheet.png").toFile());var unique=new HashSet<String>();
   check(sheet.getWidth()==2048&&sheet.getHeight()==2048,"Sixteen cells in a 4x4 sheet "+name);
   for(int f=0;f<16;f++){
    Path path=Path.of(String.format("assets/phase10_3/titan/%s/frame_%02d.png",name,f+1));var im=ImageIO.read(path.toFile());unique.add(ForestPreservationSupport.hash(Files.readAllBytes(path)));
    check(im.getWidth()==512&&im.getHeight()==512&&im.getColorModel().hasAlpha(),"512 RGBA frame "+name+f);
    check((im.getRGB(0,0)>>>24)==0&&(im.getRGB(511,511)>>>24)==0,"Transparent margins "+name+f);
    int bottom=0;for(int y=0;y<512;y++)for(int x=0;x<512;x++)if((im.getRGB(x,y)>>>24)>40)bottom=Math.max(bottom,y);
    check(bottom>=492&&bottom<=496,"Fixed visible ground anchor "+name+f);
    check(BossArtwork.frame("titan",name,f)!=null,"Prepared actual runtime frame "+name+f);
    for(int y=0;y<512;y+=7)for(int x=0;x<512;x+=7)check(im.getRGB(x,y)==sheet.getRGB(f%4*512+x,f/4*512+y),"Sheet cell exact "+name+f);
   }check(unique.size()==16,"Genuinely distinct authored poses, no duplicated frames "+name);
  }
  check(TownCelebration.names().equals(java.util.List.of("Martha","Oliver","Noah","Iris","Clara","Ben")),"Six actual resident identities; the existing Toby actor is additionally reused");
  var root=WorldMap.forArea(Area.Type.CHRISTMAS_TOWN).point("landmarks/Corrupted Town Center");
  check(root.equals(new WorldMap.Point(1296,1008))&&new EncounterManager().get("boss").getCenter().equals(root),"Authoritative tree, not the obsolete southern marker");
 }
 static void lifecycle(int w,int h)throws Exception{
  GamePanel p=playing();p.setSize(w,h);townFixture(p);check(p.getChildren().size()==1&&p.getChildren().get(0).getName().equals("Toby"),"Only the actual existing Village child joins the six residents; no invented duplicates");check(enemies(p).isEmpty(),"No arrival boss/celebration");
  at(santa(p),new WorldMap.Point(992,1088));call(p,"updateCamera");shot(p,"tree-normal");
  at(santa(p),new WorldMap.Point(620,580));call(p,"updateCamera");shot(p,"town-buildings");
  at(santa(p),new WorldMap.Point(828,1296));call(p,"updateCamera");shot(p,"town-market-street");
  at(santa(p),(WorldMap.Point)call(p,"spiritTreeInteractionPoint"));tick(p,1);check(field(p,"activeBoss")==null,"Tree cannot bypass Elias/road prerequisites");
  story(p).acceptEliasQuest();at(santa(p),encounters(p).get("town").getCenter());tick(p,1);clearWave(p);
  at(santa(p),(WorldMap.Point)call(p,"spiritTreeInteractionPoint"));call(p,"updateCamera");tick(p,1);
  var titan=(DarkTitan)field(p,"activeBoss");var intro=(StoryOverlay)field(p,"storyOverlay");
  check(intro.isActive()&&titan!=null&&titan.getHealth()==1200,"Original story ownership opens tree emergence");
  check(!util.Collision.intersectsAny(titan.getBounds(),WorldMap.forArea(Area.Type.CHRISTMAS_TOWN).solids()),"Original tree collider remains; spawn uses collision-clear nearby plaza");
  check(Math.hypot(titan.getX()+48-1296,titan.getY()+48-1008)<=256,"Actual actor adjacent to tree, no unexplained southern spawn");
  check(Math.hypot(titan.getX()+48-santa(p).getX()-16,titan.getY()+48-santa(p).getY()-21)>=128,"Safe opening distance");
  for(int frame:new int[]{0,20,45,65,80,95,110,120}){tick(p,frame-intro.getFrames());shot(p,"emergence-"+frame);}
  int hp=santa(p).getHealth();tick(p,20);check(santa(p).getHealth()==hp,"No cinematic damage");
  closeOverlay(p);check(!intro.isActive()&&p.isGameplayHudVisible(),"E skip restores control/HUD");
  tick(p,60);var message=GamePanel.class.getDeclaredField("areaMessage");message.setAccessible(true);message.set(p,null);for(int paint=0;paint<7;paint++){render(p);Thread.sleep(50);}shot(p,"titan-idle-hud");
  for(int i=0;i<16;i++){tick(p,5);shot(p,"titan-walk-consecutive-"+i);}
  santa(p).toggleDeveloperTestMode();
  for(var kind:java.util.List.of(BossAttacks.Attack.GROUND_SLAM,BossAttacks.Attack.ICE_SPIKES,BossAttacks.Attack.BEAST_CHARGE,BossAttacks.Attack.DARK_FROST_BEAM,BossAttacks.Attack.BLACK_FROST_STORM)){
   titan.attacks().begin(kind,titan,santa(p),WorldMap.forArea(Area.Type.CHRISTMAS_TOWN).solids());shot(p,kind+"-warning");
   int warning=titan.attacks().getWarningFrames();for(int i=0;i<warning;i++)titan.attacks().update(titan,santa(p),WorldMap.forArea(Area.Type.CHRISTMAS_TOWN).solids());
   for(int i=0;i<6;i++){titan.attacks().update(titan,santa(p),WorldMap.forArea(Area.Type.CHRISTMAS_TOWN).solids());shot(p,kind+"-execution-"+i);}
   titan.attacks().clear();
  }
  titan.takeDamage(600);check(titan.getTransitionFrames()==72,"Same phase threshold and transition clock");
  for(int i=0;i<8;i++){tick(p,1);shot(p,"phase-transition-"+i);}
  titan.takeDamage(1200);tick(p,1);var fall=(BossFallPresentation)field(p,"bossFall");
  for(int age:new int[]{0,1,2,3,60,100,150,BossFallPresentation.TITAN_DURATION-1}){tick(p,age-fall.getFrames());shot(p,"defeat-"+age);}
  tick(p,1);check(story(p).isBossDefeated()&&!((SpiritTreeTransformation)field(p,"treeTransformation")).isActive(),"Victory never auto-restores");
  at(santa(p),(WorldMap.Point)call(p,"spiritTreeInteractionPoint"));tap(p,KeyEvent.VK_E);var transform=(SpiritTreeTransformation)field(p,"treeTransformation");
  check(transform.getFrames()==0&&p.getState()==GamePanel.State.PLAYING&&p.isGameplayHudVisible(),"Restoration stays in actual playable world, no fullscreen cutscene");
  int x=santa(p).getX();tap(p,KeyEvent.VK_D);check(santa(p).getX()>x,"Movement remains responsive during restoration");
  tap(p,KeyEvent.VK_ESCAPE);int clock=transform.getFrames();tick(p,20);check(transform.getFrames()==clock,"Pause freezes active restoration updates");tap(p,KeyEvent.VK_ESCAPE);
  for(int age:new int[]{30,60,90,120,150,179}){tick(p,age-transform.getFrames());shot(p,"restoration-"+age);}
  check(transform.isActive()&&!chapter(p).celebrationStarted(),"No premature celebration at179");tick(p,1);
  check(transform.isComplete()&&chapter(p).celebrationStarted()&&JourneyHomeTest.ending(p).isCelebration(),"Exactly180 updates commit original restoration and start gathering");
  for(int age:new int[]{0,1,2,3,30,90,150,239}){tick(p,age-JourneyHomeTest.ending(p).getFrames());shot(p,"celebration-"+age);}
  int celebrationX=((Camera)field(p,"camera")).getX(),celebrationY=((Camera)field(p,"camera")).getY();
  tick(p,1);check(((Camera)field(p,"camera")).getX()==celebrationX&&((Camera)field(p,"camera")).getY()==celebrationY,"Gratitude page keeps the settled celebration camera, no frame-zero reset");shot(p,"villagers-gratitude");JourneyHomeTest.completeCelebration(p);
  at(santa(p),JourneyHomeVisuals.SLEIGH_APPROACH);tap(p,KeyEvent.VK_E);JourneyHomeTest.modalClick(p,180,427);
  for(int age:new int[]{0,1,2,3,48,60,105,165,240,300,330,EndingCutscene.SLEIGH_DURATION-1}){tick(p,age-JourneyHomeTest.ending(p).getFrames());shot(p,"departure-peaceful-town-"+age);}
  tick(p,1);check(areas(p).getCurrentArea().getType()==Area.Type.SANTA_BASE&&chapter(p).homeReturned()&&p.isGameplayHudVisible(),"Authorized six-second departure handoff retains playable homecoming");shot(p,"return-home");
  JourneyHomeTest.openLetters(p);check(JourneyHomeTest.window(p).letters().size()==9,"All nine gratitude letters remain playable");
  JourneyHomeTest.finishHomecoming(p);tick(p,60);tap(p,KeyEvent.VK_E);tap(p,KeyEvent.VK_E);check(JourneyHomeTest.ending(p).getState()==EndingCutscene.State.CREDITS,"Only final letter completion reaches credits");
  p.beginAdventure();p.stopGame();check(!chapter(p).celebrationStarted()&&!story(p).isBossDefeated(),"Fresh Start resets authoritative celebration and boss state");
 }
 public static void main(String[] args)throws Exception{Files.createDirectories(OUTPUT);art();for(int[] s:new int[][]{{800,600},{1270,720}})lifecycle(s[0],s[1]);System.out.println("PASS: "+checks+" Town art, tree emergence, restoration, celebration and ending assertions");}
}
