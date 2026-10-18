import game.*;import boss.*;import entity.*;import audio.*;import java.util.*;import java.awt.event.KeyEvent;
/** Production clocks, real input and state transitions; no wall-time animation assumptions. */
public class CinematicPacingTest extends V2TestSupport{
 static void fall(boolean fang,boolean skip)throws Exception{
  GamePanel p=Phase102FCinematicTest.entrance(fang,800,600);try{
   closeOverlay(p);santa(p).toggleDeveloperTestMode();tick(p,65);Enemy enemy=(Enemy)field(p,"activeBoss");enemy.takeDamage(enemy.getMaximumHealth());tick(p,1);
   BossFallPresentation fall=(BossFallPresentation)field(p,"bossFall");var audio=(AudioManager)field(p,"audio");var events=new ArrayList<String>();audio.setEventObserver(events::add);String impact=fang?"fang.fall":"titan.collapse";
   check(fall.getDuration()==(fang?240:300)&&fall.getImpactFrame()==(fang?104:150),"Distinct authored duration and grounded contact");
   int spirit=((spirit.SpiritTree)field(p,"spiritTree")).getSpirit(),oldPose=-1,cx=Phase102FCinematicTest.camera(p).getX(),cy=Phase102FCinematicTest.camera(p).getY();
   for(int t=1;t<=(skip?50:fall.getDuration());t++){
    tick(p,1);int pose=fall.getPoseTick();if(t==fall.getImpactFrame()-1)check(pose<(fang?29:57),"No fully grounded source pose before impact");if(t==fall.getImpactFrame())check(pose==(fang?29:57),"Grounded pose and audio contact share exact update");check(pose>=oldPose,"Approved pose sequence never reverses/cycles");oldPose=pose;
    check(Collections.frequency(events,impact)==(t>=fall.getImpactFrame()?1:0),"Exactly one grounded impact; no early cue");
    var c=Phase102FCinematicTest.camera(p);check(Math.abs(c.getX()-cx)<=12&&Math.abs(c.getY()-cy)<=12,"Gentle bounded camera step");cx=c.getX();cy=c.getY();
    if(t<fall.getDuration())check(fall.isActive()&&!(fang?story(p).isFrostFangDefeated():story(p).isBossDefeated()),"No early flag/reward commit");
    if(t==40){int frame=fall.getFrames();var state=GamePanel.class.getDeclaredField("state");state.setAccessible(true);state.set(p,GamePanel.State.PAUSED);tick(p,25);check(fall.getFrames()==frame,"Pause freezes staged pose/camera clock");state.set(p,GamePanel.State.PLAYING);}
   }
   if(skip)tap(p,KeyEvent.VK_E);
   check(!fall.isActive()&&(fang?story(p).isFrostFangDefeated()&&!story(p).isBossDefeated():story(p).isBossDefeated()),"Completion/skip commits only appropriate victory");Phase102FCinematicTest.normal(p);
   check(((spirit.SpiritTree)field(p,"spiritTree")).getSpirit()==spirit+(fang?0:15),"Original once-only reward");tick(p,10);check(((spirit.SpiritTree)field(p,"spiritTree")).getSpirit()==spirit+(fang?0:15),"No duplicate reward");
   check(!((SpiritTreeTransformation)field(p,"treeTransformation")).isActive(),"Restoration still requires player E");
   var c=Phase102FCinematicTest.camera(p);var point=c.screenToWorld(new java.awt.Point(200,200));check(point.x==200+c.getX()&&point.y==200+c.getY(),"Mouse aiming uses restored camera");
   p.beginAdventure();check(!((BossFallPresentation)field(p,"bossFall")).isActive()&&!story(p).isBossDefeated()&&!story(p).isFrostFangDefeated(),"Fresh Start clears all fall/flags");
  }finally{p.stopGame();}
 }
 static void departure(boolean skip)throws Exception{
  GamePanel p=JourneyHomeTest.endingFixture();try{
   tick(p,180);JourneyHomeTest.completeCelebration(p);at(santa(p),JourneyHomeVisuals.SLEIGH_APPROACH);tap(p,KeyEvent.VK_E);JourneyHomeTest.modalClick(p,180,427);
   var end=JourneyHomeTest.ending(p);var audio=(AudioManager)field(p,"audio");var events=new ArrayList<String>();audio.setEventObserver(events::add);
   double previous=0;int cx=Phase102FCinematicTest.camera(p).getX(),cy=Phase102FCinematicTest.camera(p).getY();double originX=cx+p.getWidth()/2.0,originY=cy+p.getHeight()/2.0;
   for(int t=1;t<=(skip?170:359);t++){
    tick(p,1);double flight=EndingCutscene.sleighFlight(t);check(flight>=previous&&flight-previous<=.007,"Smooth accelerating/decelerating flight");previous=flight;
    check(t>EndingCutscene.SLEIGH_MOVE||flight==0,"Boarding and preparation precede movement");
    check(Collections.frequency(events,"sleigh.laugh")== (t>=EndingCutscene.SLEIGH_TAKEOFF?1:0),"One laugh at takeoff; no stretch/loop");
    int bells=t<60?0:t<159?1:t<258?2:3;check(Collections.frequency(events,"sleigh.bells.move")==bells,"Three finite bell takes cover six-second departure");
    check(!JourneyHomeTest.chapter(p).homeReturned()&&end.getFrames()==t,"No early home transition");var c=Phase102FCinematicTest.camera(p);double blend=Math.max(0,Math.min(1,t/90.0));blend=blend*blend*(3-2*blend);var expectedCamera=new Camera();var map=WorldMap.forArea(Area.Type.CHRISTMAS_TOWN);double targetX=JourneyHomeVisuals.SLEIGH_ORIGIN.x()+126+350*flight,targetY=JourneyHomeVisuals.SLEIGH_ORIGIN.y()+76-100*flight-120*flight*flight*flight;expectedCamera.follow(originX+(targetX-originX)*blend,originY+(targetY-originY)*blend,p.getWidth(),p.getHeight(),map.width(),map.height());check(c.getX()==expectedCamera.getX()&&c.getY()==expectedCamera.getY(),"Exact original90-update easing/flight/clamp with authorized relocated start anchor");if(t>90)check(Math.abs(c.getX()-cx)<=5&&Math.abs(c.getY()-cy)<=5,"Original flight camera remains bounded without return snap");cx=c.getX();cy=c.getY();
    if(t==110){var state=GamePanel.class.getDeclaredField("state");state.setAccessible(true);state.set(p,GamePanel.State.PAUSED);tick(p,50);check(end.getFrames()==110,"Pause freezes takeoff and event timestamps");state.set(p,GamePanel.State.ENDING);}
   }
   if(skip)tap(p,KeyEvent.VK_ESCAPE);else tick(p,1);
   check(JourneyHomeTest.chapter(p).homeReturned()&&areas(p).getCurrentArea().getType()==Area.Type.SANTA_BASE&&!end.hasStarted(),"Normal and skipped departure restore playable home");Phase102FCinematicTest.normal(p);
   check(((java.util.Map<?,?>)field(audio,"owners")).isEmpty(),"Home clears owned cinematic voices");int n=events.size();tick(p,10);check(events.subList(n,events.size()).stream().noneMatch(e->e.startsWith("sleigh.")),"No stale delayed audio");
   JourneyHomeTest.openLetters(p);check(JourneyHomeTest.window(p).letters().size()==9,"Nine letters remain available");
  }finally{p.stopGame();}
 }
 static void gatheringCamera()throws Exception{
  for(int[] size:new int[][]{{800,600},{1270,720}}){GamePanel p=JourneyHomeTest.endingFixture();try{
   p.setSize(size[0],size[1]);tick(p,180);tick(p,240);tick(p,180);var c=Phase102FCinematicTest.camera(p);
   for(var feet:TownCelebration.feet())check(feet.y()-c.getY()<size[1]-160,"All residents clear the gratitude panel");
   tap(p,KeyEvent.VK_E);tap(p,KeyEvent.VK_E);check(p.getState()==GamePanel.State.PLAYING&&!JourneyHomeTest.ending(p).hasStarted(),"Control returns without waiting for camera");
   int x=c.getX(),y=c.getY();
   var state=GamePanel.class.getDeclaredField("state");state.setAccessible(true);state.set(p,GamePanel.State.PAUSED);int clock=(Integer)field(p,"celebrationCameraReturn");tick(p,20);check((Integer)field(p,"celebrationCameraReturn")==clock,"Pause freezes finite camera handoff");state.set(p,GamePanel.State.PLAYING);
   for(int t=0;t<EndingCutscene.CAMERA_RETURN_DURATION;t++){tick(p,1);check(Math.abs(c.getX()-x)<=5&&Math.abs(c.getY()-y)<=5,"No gratitude-to-player camera jump");x=c.getX();y=c.getY();}
   Phase102FCinematicTest.normal(p);check((Integer)field(p,"celebrationCameraReturn")==0,"Finite camera owner releases completely");call(p,"returnCelebrationCamera");call(p,"loadCurrentAreaContent");call(p,"updateCamera");check((Integer)field(p,"celebrationCameraReturn")==0,"Area exit cancels camera ownership");Phase102FCinematicTest.normal(p);
   call(p,"returnCelebrationCamera");call(p,"retryEncounter");check((Integer)field(p,"celebrationCameraReturn")==0,"Retry cancels camera ownership");Phase102FCinematicTest.normal(p);
   call(p,"returnCelebrationCamera");p.beginAdventure();check((Integer)field(p,"celebrationCameraReturn")==0,"Fresh Start clears camera return");
  }finally{p.stopGame();}}
 }
 public static void main(String[]args)throws Exception{for(boolean fang:new boolean[]{true,false})for(boolean skip:new boolean[]{false,true})fall(fang,skip);departure(false);departure(true);gatheringCamera();System.out.println("CinematicPacingTest PASS "+checks+" assertions");}
}
