import game.*;
import boss.*;
import entity.*;
import ui.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.util.List;

/** Actual active-update/input paths; camera presentation never owns quest or boss clocks. */
public class Phase102FCinematicTest extends V2TestSupport {
 static final List<Rectangle2D> EMPTY=List.of();
 static Camera camera(GamePanel p)throws Exception{return (Camera)field(p,"camera");}
 static void normal(GamePanel p)throws Exception{
  Camera expected=new Camera();var map=WorldMap.forArea(areas(p).getCurrentArea().getType());
  double y=santa(p).getY()+21;
  if(field(p,"activeBoss") instanceof DarkTitan)y-=125;
  else if(areas(p).getCurrentArea().getType()==Area.Type.CHRISTMAS_TOWN)y-=TownChapterArt.treeCameraLift(santa(p).getX()+16,y,map.point("landmarks/Corrupted Town Center"));
  expected.follow(santa(p).getX()+16,y,p.getWidth(),p.getHeight(),map.width(),map.height());
  check(camera(p).getX()==expected.getX()&&camera(p).getY()==expected.getY(),"Exact normal camera restored");
 }
 static GamePanel entrance(boolean fang,int width,int height)throws Exception{
  var p=playing();p.setSize(width,height);
  if(fang){investigationFixture(p);for(int i=1;i<=3;i++)story(p).recordCrystalDestroyed("crystal-"+i);at(santa(p),FrostFang.ARENA);tick(p,1);tick(p,60);}
  else{townFixture(p);story(p).acceptEliasQuest();at(santa(p),encounters(p).get("town").getCenter());tick(p,1);clearWave(p);at(santa(p),(WorldMap.Point)call(p,"spiritTreeInteractionPoint"));tick(p,1);}
  check(((StoryOverlay)field(p,"storyOverlay")).isActive(),"Existing boss introduction opens");return p;
 }
 public static void main(String[] args)throws Exception{
  Santa s=new Santa(100,100);SantaSkills skills=new SantaSkills();Walker target=new Walker(170,107);
  skills.update(List.of(target),EMPTY);check(skills.cast(2,s,600,121,EMPTY),"First volley accepted");
  check(skills.stars().size()==3&&skills.starCooldown()==60&&SantaSkills.STAR_DAMAGE==18,"Three18-damage stars and one60-update cooldown");
  var hud=new AbilityHUD();var abilities=new AbilitySystem();abilities.skills().cast(2,s,600,121,EMPTY);
  var slot=hud.getSlots(s,abilities).get(3);check(slot.total()==60&&slot.cooldown()==60&&slot.status().equals("1.0s")&&slot.progress()==0,"HUD reads60 without artwork change");
  for(int i=0;i<30;i++)abilities.skills().update(List.of(),EMPTY);slot=hud.getSlots(s,abilities).get(3);check(slot.cooldown()==30&&slot.status().equals("0.5s")&&slot.progress()==.5,"HUD shows actual halfway cooldown");
  for(int i=1;i<=59;i++){skills.update(List.of(target),EMPTY);check(skills.starCooldown()==60-i&&!skills.cast(2,s,600,121,EMPTY),"No early volley or cooldown restart at "+i);}
  check(target.getHealth()==46&&skills.stars().isEmpty(),"One full volley deals54, no duplicate damage");
  skills.update(List.of(target),EMPTY);check(skills.cast(2,s,600,121,EMPTY)&&skills.starCooldown()==60,"Ready exactly at60; delayed releases did not restart cooldown");
  skills.clearEffects();check(skills.stars().isEmpty()&&skills.starCooldown()==60,"Travel clears damage but preserves existing cooldown semantics");
  skills.reset();check(skills.starCooldown()==0,"Retry/Fresh Start reset ready");
  check(AbilitySystem.BELL_COOLDOWN==90&&SantaSkills.COOLDOWN==180&&SantaSkills.BOMB_DAMAGE==110&&SantaSkills.BOMB_RADIUS==130,"Bell and gift unchanged");
  for(int[] size:new int[][]{{800,600},{1270,720}})for(boolean fang:new boolean[]{true,false}){
   GamePanel p=entrance(fang,size[0],size[1]);var intro=(StoryOverlay)field(p,"storyOverlay");var enemy=(Enemy)field(p,"activeBoss");
   int bossFrame=((BossCombatant)enemy).getAnimationFrame();int sx=santa(p).getX(),sy=santa(p).getY();
   int previousX=camera(p).getX(),previousY=camera(p).getY();var map=WorldMap.forArea(areas(p).getCurrentArea().getType());
   for(int i=0;i<StoryOverlay.revealDuration(fang);i++){
    tick(p,1);int x=camera(p).getX(),y=camera(p).getY();
    check(Math.abs(x-previousX)<=12&&Math.abs(y-previousY)<=12,"Bounded eased reveal step");
    check(x>=0&&y>=0&&x<=Math.max(0,map.width()-size[0])&&y<=Math.max(0,map.height()-size[1]),"World-clamped cinematic camera");
    previousX=x;previousY=y;
   }
   check(((BossCombatant)enemy).getAnimationFrame()==bossFrame&&santa(p).getX()==sx&&santa(p).getY()==sy,"Intro presentation leaves actor/entrance clocks frozen");
   int attack=santa(p).getAttackSequence();SantaSkillsTest.mouse(p,500,300);tap(p,KeyEvent.VK_2);check(SantaSkillsTest.skills(p).stars().isEmpty()&&attack==santa(p).getAttackSequence(),"Intro blocks skills and attacks");
   tap(p,KeyEvent.VK_E);previousX=camera(p).getX();previousY=camera(p).getY();
   for(int i=0;i<StoryOverlay.RETURN_DURATION;i++){tick(p,1);check(Math.abs(camera(p).getX()-previousX)<=18&&Math.abs(camera(p).getY()-previousY)<=18,"Eased second-page return");previousX=camera(p).getX();previousY=camera(p).getY();}
   normal(p);tap(p,KeyEvent.VK_E);check(!intro.isActive(),"Unchanged two-page E sequence closes");normal(p);
   tick(p,fang?45:60);int hp=enemy.getHealth();enemy.takeDamage(fang?450:600);check(((BossCombatant)enemy).getTransitionFrames()==(fang?60:72),"Exact phase thresholds and durations");
   tick(p,fang?60:72);check(((BossCombatant)enemy).getTransitionFrames()==0,"No extra wait from phase presentation");
   check(enemy.getHealth()==hp-(fang?450:600),"Phase visuals add no damage");
   enemy.takeDamage(enemy.getMaximumHealth());tick(p,1);var fall=(BossFallPresentation)field(p,"bossFall");check(fall.isActive()&&fall.getFrames()==0,"Existing defeat starts at0");
   int spirit=((spirit.SpiritTree)field(p,"spiritTree")).getSpirit();previousX=camera(p).getX();previousY=camera(p).getY();
   for(int i=0;i<fall.getDuration()-1;i++){tick(p,1);check(Math.abs(camera(p).getX()-previousX)<=18&&Math.abs(camera(p).getY()-previousY)<=18,"Smooth bounded defeat attention and return");previousX=camera(p).getX();previousY=camera(p).getY();}
   tick(p,1);check(!fall.isActive()&&fall.getFrames()==fall.getDuration(),"Authorized staged defeat duration");normal(p);
   check(fang?story(p).isFrostFangDefeated()&&!story(p).isBossDefeated():story(p).isBossDefeated(),"Separate unchanged victory flags");
   check(((spirit.SpiritTree)field(p,"spiritTree")).getSpirit()==spirit+(fang?0:15),"Unchanged once-only rewards");
   check(!((SpiritTreeTransformation)field(p,"treeTransformation")).isActive()&&SpiritTreeTransformation.DURATION==180,"Restoration remains player-triggered180 updates");
   Point mouse=new Point(350,250),world=camera(p).screenToWorld(mouse);check(world.x==350+camera(p).getX()&&world.y==250+camera(p).getY(),"Mouse mapping after handoff");
   SantaSkillsTest.mouse(p,mouse.x,mouse.y);tap(p,KeyEvent.VK_2);var star=SantaSkillsTest.skills(p).stars().get(0);
   double aim=Math.atan2(world.y-santa(p).getY()-21,world.x-santa(p).getX()-16);check(Math.abs(Math.atan2(star.dy(),star.dx())-aim)<.00001,"Actual skill input aims with restored camera");
   p.stopGame();
   p=entrance(fang,size[0],size[1]);tick(p,35);closeOverlay(p);normal(p);enemy=(Enemy)field(p,"activeBoss");enemy.takeDamage(enemy.getMaximumHealth());tick(p,1);tick(p,40);
   tap(p,KeyEvent.VK_ESCAPE);normal(p);check(!((BossFallPresentation)field(p,"bossFall")).isActive(),"Defeat skip restores camera and control immediately");
   spirit=((spirit.SpiritTree)field(p,"spiritTree")).getSpirit();tap(p,KeyEvent.VK_E);tick(p,2);check(((spirit.SpiritTree)field(p,"spiritTree")).getSpirit()==spirit,"Skip cannot duplicate boss rewards");
   check(!(Boolean)field(p,"upPressed")&&!(Boolean)field(p,"leftPressed"),"No stuck cinematic movement");
   p.stopGame();
  }
  var p=playing();tap(p,KeyEvent.VK_2);tap(p,KeyEvent.VK_ESCAPE);int cd=SantaSkillsTest.skills(p).starCooldown();tick(p,100);check(SantaSkillsTest.skills(p).starCooldown()==cd,"Pause freezes star cooldown");tap(p,KeyEvent.VK_ESCAPE);
  var dialogue=(DialogueManager)field(p,"dialogueManager");dialogue.startNarrative(List.of(new DialogueLine("Santa",Portrait.Kind.SANTA,"Test.")));cd=SantaSkillsTest.skills(p).starCooldown();tick(p,100);check(SantaSkillsTest.skills(p).starCooldown()==cd,"Dialogue freezes star cooldown");dialogue.close();p.stopGame();
  System.out.println("PASS: "+checks+" balance, cinematic, camera, input and progression assertions");
 }
}
