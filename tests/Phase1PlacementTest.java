import game.*;import java.awt.*;import java.awt.event.*;import java.nio.file.*;import javax.imageio.ImageIO;import java.util.*;
/** Authoritative screenshot-matched placement, true movement, original sprites and full homecoming. */
public class Phase1PlacementTest extends JourneyHomeTest {
 static final Rectangle OLD=new Rectangle(1824,992,384,256),FOUNDATION=new Rectangle(2200,1304,256,96);
 static void move(GamePanel p,int key,int n)throws Exception{TreeCollisionTest.move(p,key,n);}
 static void house()throws Exception{
  var map=WorldMap.forArea(Area.Type.VILLAGE);var b=VillageBuildingVisuals.BUILDINGS.stream().filter(x->x.id().equals("ben-route-house")).findFirst().orElseThrow();
  check(b.bounds().equals(new Rectangle(2176,1120,300,280))&&VillageBuildingVisuals.sourceBounds(b).equals(new Rectangle(1850,920,300,280)),"Same300x280 source-derived house translated beside verified spur, no resizing/mirroring");
  check(!map.solids().contains(OLD)&&map.solids().stream().filter(FOUNDATION::equals).count()==1&&map.solids().size()==286,"Replace only obsolete east-house collider; Village tree and every other solid retained");
  VillageRestorationVisuals.prepareAsync().get();for(int i=0;i<3;i++){
   var before=ImageIO.read(Path.of("build/phase1-world/placements-before/house-pose-"+i+".png").toFile());var after=VillageRestorationVisuals.buildingVisuals().image("ben-route-house",i);
   check(Phase105PreservationSupport.equal(before,after),"Every cold/partial/warm house sprite pixel exact "+i);
  }
  var oldSanta=new entity.Santa(1760,984);for(int i=0;i<120;i++)oldSanta.update(false,false,false,true,Phase1WorldPreservationSupport.previousSolids(map));check(oldSanta.getX()==1792,"Actual original collision reproduces screenshot's invisible walkway wall");
  var p=playing();try{
   santa(p).setPosition(1760,984);move(p,KeyEvent.VK_D,120);check(santa(p).getX()==2240,"Former blocked walkway traverses left-to-right");move(p,KeyEvent.VK_A,120);check(santa(p).getX()==1760,"Former blocked walkway traverses right-to-left");
   santa(p).setPosition(2000,850);move(p,KeyEvent.VK_S,125);check(santa(p).getY()==1350,"Former house area clears north-to-south");move(p,KeyEvent.VK_W,125);check(santa(p).getY()==850,"Former house area clears south-to-north");
   santa(p).setPosition(2290,1452);move(p,KeyEvent.VK_W,60);check(santa(p).getY()==1408,"Door/steps approached below actual foundation, no roof-sized solid");
   santa(p).setPosition(2290,1200);move(p,KeyEvent.VK_S,60);check(santa(p).getY()==1262,"Roof remains visual, foundation stops feet y="+santa(p).getY());
   santa(p).setPosition(2112,1344);move(p,KeyEvent.VK_D,80);check(santa(p).getX()==2168,"Left wall remains solid");santa(p).setPosition(2480,1344);move(p,KeyEvent.VK_A,80);check(santa(p).getX()==2456,"Right wall remains solid");
   santa(p).setPosition(2032,1452);move(p,KeyEvent.VK_D,116);check(santa(p).getX()==2496,"South alley stays open between relocated house and unchanged repair house");move(p,KeyEvent.VK_A,116);check(santa(p).getX()==2032,"South alley traverses in reverse");
   santa(p).setPosition(2168,1408);move(p,KeyEvent.VK_D,72);move(p,KeyEvent.VK_W,37);move(p,KeyEvent.VK_A,72);move(p,KeyEvent.VK_S,37);check(santa(p).getX()==2168&&santa(p).getY()==1408&&!util.Collision.intersectsAny(santa(p).getBounds(),map.solids()),"Full tight orbit around foundation has no invisible oversized collision");
   santa(p).setPosition(2112,1280);p.keyPressed(new KeyEvent(p,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_D,'d'));p.keyPressed(new KeyEvent(p,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_S,'s'));tick(p,30);p.keyReleased(new KeyEvent(p,KeyEvent.KEY_RELEASED,0,0,KeyEvent.VK_D,'d'));p.keyReleased(new KeyEvent(p,KeyEvent.KEY_RELEASED,0,0,KeyEvent.VK_S,'s'));check(santa(p).getX()<=2168&&santa(p).getY()>1280,"Existing diagonal wall sliding preserved");
   for(var site:java.util.List.of(VillageActivities.Site.BEN,VillageActivities.Site.NOAH,VillageActivities.Site.MARTHA,VillageActivities.Site.OLIVER)){near(p,site);tap(p,KeyEvent.VK_E);check(((ui.DialogueManager)field(p,"dialogueManager")).isActive()||((GiftDeliveryPresentation)field(p,"giftDelivery")).isActive(),"Unmoved neighbor reachable "+site);closeDialogue(p);}
  }finally{p.stopGame();}
 }
 static void sign()throws Exception{
  var before=ImageIO.read(Path.of("build/phase1-world/placements-before/forest-terrain.png").toFile());var now=FrozenForestVisuals.prepareAsync().get();var mask=util.PixelArt.scale(ImageIO.read(Path.of("SantaLastChristmas_MapPackage/assets/environment/decorations/sign.png").toFile()),75,110);int changed=0;
  for(int y=0;y<2304;y++)for(int x=0;x<3072;x++)if(now.getRGB(x,y)!=before.getRGB(x,y)){check(Phase1WorldPreservationSupport.signPixel(mask,x,y),"Only identified sign alpha footprint changed "+x+","+y);changed++;}
  check(changed>1500,"Wooden boards/post/fabric removed at their baked contribution, not covered by another prop");var map=WorldMap.forArea(Area.Type.FOREST);check(map.solids().stream().noneMatch(r->r.intersects(2635,1762,75,110)),"Sign had no dedicated collider; no unrelated Forest solid removed");
  var p=playing();try{forestFixture(p);santa(p).setPosition(2600,1840);move(p,KeyEvent.VK_D,30);check(santa(p).getX()==2720,"Actual movement crosses retired sign ground without a barrier");}finally{p.stopGame();}
 }
 static void sleigh()throws Exception{
  check(JourneyHomeVisuals.SLEIGH_ORIGIN.equals(new WorldMap.Point(1168,1360))&&JourneyHomeVisuals.SLEIGH_APPROACH.equals(new WorldMap.Point(1296,1526)),"Complete assembly placed at verified southern Town intersection with original approach offset");
  var map=WorldMap.forArea(Area.Type.CHRISTMAS_TOWN);check(!util.Collision.intersectsAny(new Rectangle(1168,1360,300,144),map.solids()),"Whole assembly clear of existing solids");
  for(int t:new int[]{0,48,60,105,165,240,330,360}){double v=EndingCutscene.sleighFlight(t),u=Math.max(0,Math.min(1,(t-105)/225.0));check(Math.abs(v-u*u*(3-2*u))<1e-9,"Original departure curve/timing exact "+t);}
  String old=new String(Phase10PreservationSupport.archive("src/game/EndingCutscene.java","PHASE1_WORLD"),java.nio.charset.StandardCharsets.UTF_8).replace("\r\n","\n"),now=new String(Phase2UIPreservationSupport.previous("src/game/EndingCutscene.java",Files.readAllBytes(Path.of("src/game/EndingCutscene.java"))),java.nio.charset.StandardCharsets.UTF_8).replace("\r\n","\n");
  check(now.equals(old.replace("x=1550+350*launch;y=1190-100*launch-120*launch*launch*launch;","x=JourneyHomeVisuals.SLEIGH_ORIGIN.x()+126+350*launch;y=JourneyHomeVisuals.SLEIGH_ORIGIN.y()+76-100*launch-120*launch*launch*launch;")),"Entire cinematic owner byte-equivalent except authorized world anchor references");
  String oldSleigh=new String(Phase10PreservationSupport.archive("src/game/JourneyHomeVisuals.java","PHASE1_WORLD"),java.nio.charset.StandardCharsets.UTF_8).replace("\r\n","\n"),newSleigh=new String(PostPhase4Support.previous("src/game/JourneyHomeVisuals.java",Files.readAllBytes(Path.of("src/game/JourneyHomeVisuals.java"))),java.nio.charset.StandardCharsets.UTF_8).replace("\r\n","\n");
  oldSleigh=oldSleigh.replace("    public static final WorldMap.Point SLEIGH_APPROACH=new WorldMap.Point(1552,1280);","    public static final WorldMap.Point SLEIGH_ORIGIN=new WorldMap.Point(1168,1360);\n    public static final WorldMap.Point SLEIGH_APPROACH=new WorldMap.Point(SLEIGH_ORIGIN.x()+128,SLEIGH_ORIGIN.y()+166);").replace("int x=1424+(int)(p*350),y=1114-(int)(p*100+p*p*p*120);","int x=SLEIGH_ORIGIN.x()+(int)(p*350),y=SLEIGH_ORIGIN.y()-(int)(p*100+p*p*p*120);");
  check(oldSleigh.equals(newSleigh),"Whole sleigh render/boarding/fade/animation code exact except necessary placement references");
  var p=endingFixture();try{tick(p,180);completeCelebration(p);tick(p,EndingCutscene.CAMERA_RETURN_DURATION);at(santa(p),new WorldMap.Point(1552,1280));tap(p,KeyEvent.VK_E);check(!window(p).isOpen()&&!ending(p).hasStarted(),"No obsolete parked interaction at old sleigh approach");
   santa(p).setPosition(1280,1569);move(p,KeyEvent.VK_W,12);check(FinalChapter.inReach(santa(p),map,JourneyHomeVisuals.SLEIGH_APPROACH),"Actual walking reaches new assembly");tap(p,KeyEvent.VK_E);check(window(p).mode()==ui.VillageActivityUI.Mode.SLEIGH,"E opens existing Journey Home confirmation at new anchor");modalClick(p,180,427);tick(p,359);check(!chapter(p).homeReturned()&&ending(p).getFrames()==359,"No shortened departure");tick(p,1);check(chapter(p).homeReturned()&&areas(p).getCurrentArea().getType()==Area.Type.SANTA_BASE,"Exact360-update home transition");openLetters(p);check(window(p).letters().size()==9,"All nine letters preserved");finishHomecoming(p);tick(p,60);tap(p,KeyEvent.VK_E);tap(p,KeyEvent.VK_E);check(ending(p).getState()==EndingCutscene.State.CREDITS,"Original title/message pages reach credits");tick(p,EndingCutscene.duration(EndingCutscene.State.CREDITS)-ending(p).getFrames());check(ending(p).getState()==EndingCutscene.State.END,"Existing timed credits reach final ending");
  }finally{p.stopGame();}
 }
 public static void main(String[]args)throws Exception{house();sign();sleigh();System.out.println("Phase1PlacementTest PASS "+checks+" assertions");}
}
