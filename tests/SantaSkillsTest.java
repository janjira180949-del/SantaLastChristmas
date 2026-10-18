import game.*;import entity.*;import ui.*;import java.awt.*;import java.awt.event.*;import java.awt.geom.*;import java.util.*;
/** Real input paths plus deterministic swept collision, timing and reset checks. */
@SuppressWarnings("unchecked")
public class SantaSkillsTest extends V2TestSupport {
 static SantaSkills skills(GamePanel p)throws Exception{return ((AbilitySystem)field(p,"abilitySystem")).skills();}
 static void mouse(GamePanel p,int x,int y){var e=new MouseEvent(p,MouseEvent.MOUSE_MOVED,0,0,x,y,0,false);for(var l:p.getMouseMotionListeners())l.mouseMoved(e);}
 static void press(GamePanel p,int key){p.keyPressed(new KeyEvent(p,KeyEvent.KEY_PRESSED,0,0,key,KeyEvent.CHAR_UNDEFINED));}
 public static void main(String[] args)throws Exception{
  var s=new Santa(100,100);var skills=new SantaSkills();var empty=java.util.List.<Rectangle2D>of();var enemies=new ArrayList<Enemy>();
  check(skills.cast(2,s,500,121,empty),"Star ready immediately");var star=skills.stars().get(0);check(star.dx()==1&&star.dy()==0,"Mouse world direction");
  skills.update(enemies,empty);check(skills.stars().get(0).x()==128,"12 pixels per active update");
  check(skills.starCooldown()==59&&skills.stars().size()==3,"Star cooldown advances once; three projectiles per activation");
  var target=new Walker(150,107);var behind=new Walker(180,107);enemies.add(target);enemies.add(behind);
  for(int i=0;i<16;i++)skills.update(enemies,empty);check(target.getHealth()==46&&behind.getHealth()==100&&skills.stars().isEmpty(),"Single enemy contact and removal");
  for(int i=0;i<30;i++)skills.update(enemies,empty);check(target.getHealth()==46,"No repeated star damage");
  skills.reset();var wall=java.util.List.<Rectangle2D>of(new Rectangle2D.Double(132,80,2,80));skills.cast(2,s,400,121,wall);for(int i=0;i<12;i++)skills.update(enemies,wall);check(skills.stars().isEmpty()&&target.getHealth()==46,"Thin wall swept collision wins");
  skills.reset();skills.cast(2,s,2000,121,empty);for(int i=0;i<39;i++)skills.update(new ArrayList<>(),empty);check(skills.stars().get(0).distance()==468,"Range not expired early");skills.update(new ArrayList<>(),empty);check(skills.stars().stream().noneMatch(v->v.distance()>=480),"480 maximum range per projectile");for(int i=0;i<8;i++)skills.update(new ArrayList<>(),empty);check(skills.stars().isEmpty(),"All staggered projectiles expire");
  for(int i=40;i<180;i++)skills.update(new ArrayList<>(),empty);check(skills.starCooldown()==0,"Star cooldown expires through active updates");
  skills.reset();skills.cast(3,s,2000,121,empty);var bomb=skills.bombs().get(0);check(bomb.x()==396&&bomb.y()==121,"280 throw clamp");check(skills.bombCooldown()==180,"Bomb exact cooldown");
  skills.reset();skills.cast(3,s,400,121,wall);check(skills.bombs().get(0).x()<=132-4,"Throw does not pass blocking world geometry");
  skills.reset();enemies.clear();target=new Walker(150,107);behind=new Walker(700,107);enemies.add(target);enemies.add(behind);skills.cast(3,s,170,121,empty);
  for(int i=0;i<20;i++)skills.update(enemies,empty);check(skills.bombs().get(0).age()==20&&target.getHealth()==100,"Landing after20; no damage");
  for(int i=20;i<49;i++)skills.update(enemies,empty);check(target.getHealth()==100&&!skills.bombs().isEmpty(),"Fuse warning full30 updates");skills.update(enemies,empty);check(target.getHealth()==0&&behind.getHealth()==100&&skills.bombs().isEmpty(),"One AoE explosion at50");
  for(int i=0;i<130;i++)skills.update(enemies,empty);check(target.getHealth()==0&&skills.bombCooldown()==0&&skills.burstCount()==0,"No repeat damage; exact cooldown; finite burst");
  skills.reset();target=new Walker(145,107);enemies.clear();enemies.add(target);skills.cast(3,s,116,121,wall);for(int i=0;i<50;i++)skills.update(enemies,wall);check(target.getHealth()==100,"Bomb blast cannot pass wall");
  var boss=new boss.ChristmasEater(170,100);enemies.clear();enemies.add(boss);skills.reset();skills.cast(2,s,200,121,empty);int hp=boss.getHealth();for(int i=0;i<16;i++)skills.update(enemies,empty);check(boss.getHealth()==hp-54,"Existing polymorphic boss damage API");
  var bell=new AbilitySystem();target=new Walker(155,110);enemies.clear();enemies.add(target);check(bell.use(1,s,enemies,empty)&&target.getHealth()==25&&bell.getBellCooldownFrames()==90,"Bell requested75 damage/90 cooldown");
  for(int i=0;i<24;i++){bell.update();var im=new java.awt.image.BufferedImage(300,300,2);var g=im.createGraphics();bell.draw(g);g.dispose();}check(target.getHealth()==25&&bell.getBellCooldownFrames()==66,"Bell repaint adds no damage or timing");
  var p=playing();mouse(p,1000,360);tap(p,KeyEvent.VK_2);check(skills(p).starCooldown()==60&&!skills(p).stars().isEmpty(),"Key2 immediate in realGamePanel");
  int seq=santa(p).getAttackSequence();tap(p,KeyEvent.VK_3);check(skills(p).bombCooldown()==180&&santa(p).getAttackSequence()==seq,"Key3 immediate; no basic attack identity");
  press(p,KeyEvent.VK_2);tick(p,200);press(p,KeyEvent.VK_2);tick(p,1);check(skills(p).starCooldown()==0,"Held2 never recasts after expiry");p.keyReleased(new KeyEvent(p,KeyEvent.KEY_RELEASED,0,0,KeyEvent.VK_2,KeyEvent.CHAR_UNDEFINED));
  tap(p,KeyEvent.VK_2);tap(p,KeyEvent.VK_ESCAPE);int cd=skills(p).starCooldown();tick(p,200);tap(p,KeyEvent.VK_3);check(skills(p).starCooldown()==cd&&skills(p).bombCooldown()==0,"Pause freezes cooldown and blocks skill");tap(p,KeyEvent.VK_ESCAPE);
  tap(p,KeyEvent.VK_SPACE);check(santa(p).isDashing(),"Space Dash preserved");
  call(p,"loadCurrentAreaContent");check(skills(p).stars().isEmpty()&&skills(p).bombs().isEmpty()&&skills(p).burstCount()==0,"Area cleanup no lingering damage");
  ((AbilitySystem)field(p,"abilitySystem")).reset();check(skills(p).starCooldown()==0&&skills(p).bombCooldown()==0,"Reset ready state");
  for(int[] size:new int[][]{{800,600},{1270,720}}){p.setSize(size[0],size[1]);var hud=new AbilityHUD();var slots=hud.getSlots(santa(p),(AbilitySystem)field(p,"abilitySystem"));check(slots.size()==5&&slots.get(3).input().equals("2")&&slots.get(4).input().equals("3"),"All controls in HUD");for(int i=0;i<5;i++){var r=hud.bounds(size[0],size[1],i);check(new Rectangle(0,0,size[0],size[1]).contains(r)&&r.x>=306,"All slots clear quest/viewport");if(i>0)check(!r.intersects(hud.bounds(size[0],size[1],i-1)),"No slot overlap");}var r=hud.bounds(size[0],size[1],3);int before=santa(p).getAttackSequence();QuestFlowPresentationTest.mouse(p,r.x+20,r.y+20);check(before==santa(p).getAttackSequence(),"HUD click does not attack");}
  var dialogue=(ui.DialogueManager)field(p,"dialogueManager");dialogue.startNarrative(java.util.List.of(new ui.DialogueLine("Santa",ui.Portrait.Kind.SANTA,"A quiet Christmas.")));tap(p,KeyEvent.VK_2);tap(p,KeyEvent.VK_3);check(skills(p).starCooldown()==0&&skills(p).bombCooldown()==0,"Dialogue blocks both new skills");dialogue.close();
  dialogue.startNarrative(java.util.List.of(new ui.DialogueLine("Santa",ui.Portrait.Kind.SANTA,"Hold your fire.")));press(p,KeyEvent.VK_3);tick(p,5);dialogue.close();press(p,KeyEvent.VK_3);tick(p,1);check(skills(p).bombCooldown()==0,"Blocked held3 cannot cast after dialogue closes");p.keyReleased(new KeyEvent(p,KeyEvent.KEY_RELEASED,0,0,KeyEvent.VK_3,KeyEvent.CHAR_UNDEFINED));
  tap(p,KeyEvent.VK_3);check(skills(p).bombCooldown()==180,"Released3 may cast again");press(p,KeyEvent.VK_3);tick(p,200);press(p,KeyEvent.VK_3);tick(p,1);check(skills(p).bombCooldown()==0,"Holding3 does not repeat after cooldown");
  p.keyReleased(new KeyEvent(p,KeyEvent.KEY_RELEASED,0,0,KeyEvent.VK_3,KeyEvent.CHAR_UNDEFINED));tap(p,KeyEvent.VK_3);tap(p,KeyEvent.VK_ESCAPE);int age=skills(p).bombs().get(0).age();tick(p,100);check(skills(p).bombs().get(0).age()==age,"Pause freezes throw/fuse, not just cooldown");tap(p,KeyEvent.VK_ESCAPE);
  ((AbilitySystem)field(p,"abilitySystem")).reset();var window=(ui.VillageActivityUI)field(p,"activityWindow");window.open(ui.VillageActivityUI.Mode.RESTORATION,VillageActivities.Site.STREETLIGHTS,(VillageActivities)field(p,"villageActivities"));tap(p,KeyEvent.VK_2);tap(p,KeyEvent.VK_3);check(skills(p).starCooldown()==0&&skills(p).bombCooldown()==0,"Activity modal blocks both immediate casts");window.close();
  var hudBefore=santa(p).getAttackSequence();attack(p,santa(p).getX()+50,santa(p).getY()+21);check(santa(p).getAttackSequence()==hudBefore+1,"Left mouse remains basic attack");
  tap(p,KeyEvent.VK_3);check(!skills(p).bombs().isEmpty(),"Spell active before ending transition");
  var stateField=GamePanel.class.getDeclaredField("state");stateField.setAccessible(true);stateField.set(p,GamePanel.State.ENDING);call(p,"clearCutsceneInput");check(skills(p).bombs().isEmpty()&&skills(p).stars().isEmpty(),"Ending clears lingering damage");
  p.stopGame();
  p=playing();investigationFixture(p);var core=((java.util.List<BlackFrostCrystal>)field(p,"crystals")).get(0);at(santa(p),core.getPosition());tick(p,60);while(((EncounterManager)field(p,"encounterManager")).getActive()!=null)clearWave(p);tick(p,1);
  int spirit=((spirit.SpiritTree)field(p,"spiritTree")).getSpirit();tap(p,KeyEvent.VK_2);tap(p,KeyEvent.VK_3);tick(p,55);check(core.getState()==BlackFrostCrystal.State.VULNERABLE&&((spirit.SpiritTree)field(p,"spiritTree")).getSpirit()==spirit,"Skills cannot bypass fresh-basic crystal requirement");
  p.stopGame();System.out.println("PASS: "+checks+" skill collision/timing/input/preservation assertions");
 }
}
