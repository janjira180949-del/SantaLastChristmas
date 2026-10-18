import boss.*;
import game.*;
import entity.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.util.*;

/** Real combat/input/encounter clocks, with separate Forest and final-boss progression. */
public class Phase10BossTest extends V2TestSupport {
 static void update(Enemy boss,Santa santa,List<Rectangle2D> solids,int ticks){for(int i=0;i<ticks;i++){boss.advanceCombatTimers();boss.update(santa,solids);}}
 static void attack(BossCombatant boss,BossAttacks.Attack attack,Santa santa,List<Rectangle2D> solids){boss.attacks().begin(attack,(Enemy)boss,santa,solids);}
 static void execution(BossCombatant boss,Santa santa,List<Rectangle2D> solids,int ticks){for(int i=0;i<ticks;i++)boss.attacks().update((Enemy)boss,santa,solids);}
 public static void main(String[] args)throws Exception{
  BossArtwork.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS);
  GamePanel p=playing();investigationFixture(p);var story=story(p);var waves=encounters(p);
  check(!story.isFrostFangAvailable()&&!story.recordFrostFangDefeated(),"Cannot skip three cores");
  for(int i=1;i<=3;i++)story.recordCrystalDestroyed("crystal-"+i);
  check(story.isFrostFangAvailable()&&story.getStage()==quest.StoryQuestManager.Stage.DEFEAT_FROST_FANG,"Third core reveals predator stage");
  check(!areas(p).isConnectionAvailable(Area.Type.CHRISTMAS_TOWN)&&!story.acceptEliasQuest(),"Town and Elias remain locked");
  at(santa(p),FrostFang.ARENA);tick(p,1);check(waves.getActive().getId().equals("frost-fang")&&enemies(p).isEmpty(),"Fang encounter has initial warning");tick(p,60);
  check(enemies(p).stream().anyMatch(e->e instanceof FrostFang),"Existing encounter spawn lifecycle creates Frost Fang");closeOverlay(p);
  var fang=(FrostFang)enemies(p).stream().filter(e->e instanceof FrostFang).findFirst().orElseThrow();
  check(!util.Collision.intersectsAny(fang.getBounds(),WorldMap.forArea(Area.Type.FOREST).solids()),"Actual Fang spawn collision clear");
  int clock=fang.getAnimationFrame();tap(p,KeyEvent.VK_ESCAPE);int hp=fang.getHealth();tick(p,90);check(fang.getHealth()==hp&&fang.getAnimationFrame()==clock,"Pause freezes boss and animation");tap(p,KeyEvent.VK_ESCAPE);
  var overlay=(StoryOverlay)field(p,"storyOverlay");overlay.start("Diagnostic blocked story card");int frame=fang.getAnimationFrame();tick(p,80);check(fang.getAnimationFrame()==frame&&fang.getHealth()==hp,"Story dialogue blocks boss clocks and damage");closeOverlay(p);
  fang.takeDamage(450);santa(p).takeDamage(100);tick(p,1);tap(p,KeyEvent.VK_R);
  check(story.getCrystalsDestroyed()==3&&!story.isFrostFangDefeated()&&enemies(p).isEmpty()&&waves.getActive().getWarningFrames()==59,"Fang retry keeps cores and restarts full warning");tick(p,59);closeOverlay(p);
  var retryFang=(FrostFang)enemies(p).get(0);check(retryFang!=fang&&retryFang.getHealth()==900&&retryFang.getPhaseIndex()==0,"Fang retry resets HP phases and attack owner");
  call(p,"loadCurrentAreaContent");check(enemies(p).isEmpty()&&waves.getActive()==null&&waves.get("frost-fang").getState()==Encounter.State.READY,"Abandon clears boss actors and restarts encounter");at(santa(p),FrostFang.ARENA);tick(p,1);
  clearWave(p);check(story.isFrostFangDefeated()&&!story.isBossDefeated()&&!story.isEndingTriggered(),"Fang victory is independent of final victory");
  check(areas(p).isConnectionAvailable(Area.Type.CHRISTMAS_TOWN)&&!story.recordFrostFangDefeated(),"Town opens once, duplicate victory rejected");
  travelGate(p,Area.Type.CHRISTMAS_TOWN);story.acceptEliasQuest();at(santa(p),waves.get("town").getCenter());tick(p,1);clearWave(p);
  at(santa(p),(WorldMap.Point)call(p,"spiritTreeInteractionPoint"));tick(p,1);closeOverlay(p);
  check(enemies(p).stream().anyMatch(e->e instanceof DarkTitan)&&enemies(p).stream().noneMatch(e->e instanceof FrostFang),"Only Dark Titan guards Town tree");
  var walking=(DarkTitan)enemies(p).get(0);tick(p,60);walking.attacks().clear();
  var walkClock=DarkTitan.class.getDeclaredField("walkTicks");walkClock.setAccessible(true);walkClock.setInt(walking,0);var cool=DarkTitan.class.getDeclaredField("cooldown");cool.setAccessible(true);cool.setInt(walking,90);
  var safe=WorldMap.forArea(Area.Type.CHRISTMAS_TOWN).safePosition((int)walking.getX()+320,(int)walking.getY()+50,32,50);santa(p).setPosition(safe.x(),safe.y());santa(p).toggleDeveloperTestMode();tick(p,40);
  check(((Integer)field(p,"shakeFrames"))==6,"Foot contact emits short six-update world shake");var menu=field(p,"mainMenu");var reduced=menu.getClass().getDeclaredField("reducedMotion");reduced.setAccessible(true);reduced.setBoolean(menu,true);
  var camera=(Camera)field(p,"camera");var aim=p.getClass().getDeclaredMethod("aimInWorld",java.awt.Point.class);aim.setAccessible(true);var screen=new java.awt.Point(250,250);check(aim.invoke(p,screen).equals(camera.screenToWorld(screen)),"Reduced motion suppresses shake and aim offset");
  reduced.setBoolean(menu,false);check(!aim.invoke(p,screen).equals(camera.screenToWorld(screen)),"World shake has matching mouse aim correction");tap(p,KeyEvent.VK_ESCAPE);int pausedWalk=walking.getWalkTicks();tick(p,80);check(walking.getWalkTicks()==pausedWalk,"Pause freezes heavy walk and footsteps");tap(p,KeyEvent.VK_ESCAPE);santa(p).toggleDeveloperTestMode();
  clearWave(p);check(story.isBossDefeated()&&story.isFrostFangDefeated()&&!story.isEndingTriggered(),"Final victory allows original E restoration, without automatic ending");
  at(santa(p),(WorldMap.Point)call(p,"spiritTreeInteractionPoint"));tap(p,KeyEvent.VK_E);tick(p,179);check(!((SpiritTreeTransformation)field(p,"treeTransformation")).isComplete(),"Original restoration not early");tick(p,1);check(((SpiritTreeTransformation)field(p,"treeTransformation")).isComplete(),"Exactly180 restoration updates");
  p.beginAdventure();check(!story(p).isFrostFangDefeated()&&!story(p).isBossDefeated()&&story(p).getCrystalsDestroyed()==0&&field(p,"activeBoss")==null,"Fresh Start resets both independent boss flags and actors");p.stopGame();
  var titan=new DarkTitan(200,200);var player=new Santa(500,300);
  update(titan,player,List.of(),60);check(titan.getPhaseIndex()==0,"Titan independent phase1");
  update(titan,player,List.of(),40);check(titan.getFootstepSequence()==1&&titan.getWalkTicks()==40,"Heavy walk contact at frame9, one shake event");
  update(titan,player,List.of(),40);check(titan.getFootstepSequence()==2&&titan.getWalkTicks()==0,"Seamless80-update walk has exactly two foot contacts");
  titan.takeDamage(600);check(titan.getPhaseIndex()==1&&titan.getTransitionFrames()==72,"Half HP transition cancels danger and telegraphs phase2");
  titan.takeDamage(300);check(titan.getPhaseIndex()==2&&titan.attacks().getAttack()==BossAttacks.Attack.NONE,"Quarter HP phase3 has safe transition");
  var phased=new DarkTitan(200,200);var phaseTarget=new Santa(1100,300);update(phased,phaseTarget,List.of(),150);check(phased.attacks().getAttack()==BossAttacks.Attack.GROUND_SLAM,"Awakening starts with its slam");phased.takeDamage(600);update(phased,phaseTarget,List.of(),172);check(phased.attacks().getAttack()==BossAttacks.Attack.BEAST_CHARGE,"Rampage resets its pattern to the new charge");phased.takeDamage(300);update(phased,phaseTarget,List.of(),172);check(phased.attacks().getAttack()==BossAttacks.Attack.BLACK_FROST_STORM,"Catastrophe starts with its new storm after safe transition");
  var phasedFang=new FrostFang(200,200);update(phasedFang,phaseTarget,List.of(),115);phasedFang.takeDamage(450);update(phasedFang,phaseTarget,List.of(),140);check(phasedFang.attacks().getAttack()==BossAttacks.Attack.FROST_WAVE,"Ranged Rage begins with its wave");phasedFang.takeDamage(225);phaseTarget.setPosition(1800,300);update(phasedFang,phaseTarget,List.of(),140);check(phasedFang.attacks().getAttack()==BossAttacks.Attack.FROST_DASH,"Ranged Black Fang begins with its dash");
  var solid=List.<Rectangle2D>of(new Rectangle2D.Double(310,0,1,1000));var dash=new DarkTitan(200,200);var target=new Santa(480,230);
  attack(dash,BossAttacks.Attack.BEAST_CHARGE,target,solid);execution(dash,target,solid,44);check(dash.getX()==200,"Charge preparation/armsplant do not move");execution(dash,target,solid,30);
  check(dash.getX()+dash.getSize()<=310&&!util.Collision.intersectsAny(dash.getBounds(),solid),"Charge swept footprint cannot tunnel through1px wall");
  var launch=new DarkTitan(200,200);var distant=new Santa(600,230);attack(launch,BossAttacks.Attack.BEAST_CHARGE,distant,List.of());execution(launch,distant,List.of(),45);check(launch.getAnimationFrame()==9&&launch.getX()>200,"Frame10 synchronises forward launch");execution(launch,distant,List.of(),15);check(launch.getAnimationFrame()==12,"Frame13 enters heavy impact");execution(launch,distant,List.of(),10);check(launch.getAnimationFrame()==14&&launch.attacks().isRecovering(),"Frame15 enters damage-free recovery");
  var beam=new DarkTitan(200,200);var victim=new Santa(480,190);attack(beam,BossAttacks.Attack.DARK_FROST_BEAM,victim,solid);execution(beam,victim,solid,110);check(victim.getHealth()==100,"Beam stops at solid, no damage behind wall");
  var extending=new DarkTitan(200,200);var farVictim=new Santa(500,183);attack(extending,BossAttacks.Attack.DARK_FROST_BEAM,farVictim,List.of());execution(extending,farVictim,List.of(),60);check(farVictim.getHealth()==100,"No damage ahead of visible extending beam");execution(extending,farVictim,List.of(),6);check(farVictim.getHealth()==78,"Extending visible beam reaches target once");
  for(var kind:List.of(BossAttacks.Attack.CLAW,BossAttacks.Attack.GROUND_SLAM,BossAttacks.Attack.ICE_SPIKES,BossAttacks.Attack.DARK_FROST_BEAM,BossAttacks.Attack.BLACK_FROST_STORM)){
   Enemy actor=kind==BossAttacks.Attack.CLAW?new FrostFang(200,200):new DarkTitan(200,200);var b=(BossCombatant)actor;
   Santa s=new Santa(kind==BossAttacks.Attack.CLAW?255:kind==BossAttacks.Attack.GROUND_SLAM?300:kind==BossAttacks.Attack.BLACK_FROST_STORM?412:400,kind==BossAttacks.Attack.DARK_FROST_BEAM?183:227);
   attack(b,kind,s,List.of());int warning=b.attacks().getWarningFrames();execution(b,s,List.of(),warning-1);check(s.getHealth()==100,"No invisible damage before "+kind);
   execution(b,s,List.of(),b.attacks().getTotalFrames());check(s.getHealth()<100,"Visible execution damages for "+kind);
   int after=s.getHealth();execution(b,s,List.of(),100);check(s.getHealth()==after,"No lingering/repeat attack "+kind);
  }
  for(var kind:List.of(BossAttacks.Attack.ICE_BLAST,BossAttacks.Attack.FROST_WAVE,BossAttacks.Attack.FROST_DASH)){
   var hunter=new FrostFang(200,200);var prey=new Santa(420,217);attack(hunter,kind,prey,List.of());execution(hunter,prey,List.of(),hunter.attacks().getWarningFrames()-1);check(prey.getHealth()==100,"Fang telegraph has no damage "+kind);
   execution(hunter,prey,List.of(),hunter.attacks().getTotalFrames());check(prey.getHealth()<100,"Fang visible execution reaches prey "+kind);
   hunter.takeDamage(900);int remaining=prey.getHealth();execution(hunter,prey,List.of(),100);check(prey.getHealth()==remaining&&hunter.attacks().getAttack()==BossAttacks.Attack.NONE,"Fang death clears all future danger "+kind);
  }
  var safeStorm=new DarkTitan(200,200);var safePlayer=new Santa(232,227);attack(safeStorm,BossAttacks.Attack.BLACK_FROST_STORM,safePlayer,List.of());execution(safeStorm,safePlayer,List.of(),250);check(safePlayer.getHealth()==100,"Storm leaves a genuinely safe inner movement zone");
  var inherited=new Encounter("fang-reinforcements",Area.Type.FOREST,FrostFang.ARENA,Encounter.EnemyKind.FROST_FANG);var inheritedPlayer=new Santa(2576,1227);var members=new ArrayList<Enemy>();inherited.start(inheritedPlayer,members);var inheritedFang=(FrostFang)members.get(0);inheritedFang.takeDamage(450);inherited.advance(inheritedPlayer,members);
  check(inheritedFang.getCombatStatus().contains("reinforcements"),"Compact boss HUD reads the authoritative reinforcement warning");
  for(int i=0;i<59;i++)inherited.advance(inheritedPlayer,members);check(members.size()==1,"Inherited Fang reinforcement pair retains its full60-update warning");inherited.advance(inheritedPlayer,members);
  check(members.size()==3&&members.stream().anyMatch(e->e instanceof Walker)&&members.stream().anyMatch(e->e instanceof Runner),"Original one Walker/one Runner boss pair preserved in Forest");GameplayOverhaulEncounterTest.safe(inherited,inheritedPlayer,members);
  for(int i=0;i<300;i++)inherited.advance(inheritedPlayer,members);check(members.size()==3,"Fang summons remain bounded, no repeat allocation");inheritedFang.takeDamage(900);check(inherited.finishIfCleared()&&members.isEmpty(),"Fang victory cleans surviving original summons");
  for(Enemy actor:List.of(new FrostFang(200,200),new DarkTitan(200,200))){
   Santa s=new Santa(210,230);int health=actor.getHealth();var ability=new AbilitySystem();check(ability.use(1,s,new ArrayList<>(List.of(actor))),"Bell reaches both bosses");check(actor.getHealth()==health-75,"Bell requested75 boss damage");
   var skills=new SantaSkills();check(skills.cast(2,s,actor.getX()+48,actor.getY()+48,List.of()),"Star casts on boss");for(int i=0;i<40;i++)skills.update(new ArrayList<>(List.of(actor)),List.of());check(actor.getHealth()==health-129,"Star triple18 boss damage");
   check(skills.cast(3,s,actor.getX()+48,actor.getY()+48,List.of()),"Bomb casts on boss");for(int i=0;i<70;i++)skills.update(new ArrayList<>(List.of(actor)),List.of());check(actor.getHealth()==health-239,"Bomb requested110 boss damage");
  }
  check(new HealingPoint(Area.Type.FOREST,720,1168).getInteractionText(new Santa(704,1147),false).contains("40"),"Existing Forest shrine mechanics retained");
  System.out.println("PASS: "+checks+" two-boss story, attack, collision, animation and skill assertions");
 }
}
