import game.*;
import entity.*;
import java.awt.event.KeyEvent;
import java.util.*;

/** Real finite clocks/spawn policy and GamePanel quest rewards, including the authored clearing ambush. */
public class ForestEncounterTest extends V2TestSupport {
    static void safe(Encounter encounter,Santa player,List<Enemy> actors) {
        GameplayOverhaulEncounterTest.safe(encounter,player,actors);
        for(Enemy actor:actors)if(!actor.isDefeated()) {
            double x=actor.getX()+actor.getSize()/2.0,y=actor.getY()+actor.getSize()/2.0;
            check(Math.hypot(x-720,y-1168)>=160&&Math.hypot(x-1664,y-1280)>=160,"Shelter exclusion retained");
            check(Math.hypot(x-944,y-784)>=150,"Gift exclusion retained");
        }
    }
    public static void main(String[]args)throws Exception {
        EncounterManager manager=new EncounterManager();int count=0,groups=0,waves=0;
        for(Encounter e:manager.getEncounters())if(e.getArea()==Area.Type.FOREST&&!e.getId().equals("frost-fang")) {
            groups++;waves+=e.getWaveCount();Santa s=new Santa(e.getCenter().x()-16,e.getCenter().y()-21);List<Enemy> actors=new ArrayList<>();
            check(e.start(s,actors),"Every authored Forest group starts: "+e.getId());int guard=400;
            while(!e.finishIfCleared()&&guard-->0) {
                safe(e,s,actors);for(Enemy actor:actors)if(!actor.isDefeated()){count++;actor.takeDamage(actor.getMaximumHealth());}
                actors.removeIf(Enemy::isDefeated);e.advance(s,actors);
            }
            check(guard>0&&!e.start(s,actors),"Finite complete and no duplicate trigger: "+e.getId());
        }
        check(groups==8&&waves==10&&count==31,"Exactly eight Forest groups, ten finite waves and 31 zombies (previously five/six/18)");
        manager=new EncounterManager();Encounter ambush=manager.get("forest-ambush");Santa s=new Santa(1554,1529);List<Enemy> actors=new ArrayList<>();
        manager.startNearby(Area.Type.FOREST,s,actors,e->true);check(manager.getActive()==ambush&&actors.isEmpty()&&ambush.getWarningFrames()==60,"Clearing entry begins full fair warning");
        for(int i=0;i<59;i++)manager.advance(s,actors);check(actors.isEmpty()&&s.getHealth()==100,"No premature spawn or instant ambush damage");
        manager.advance(s,actors);check(actors.size()==4,"Bounded two-Walker/two-Runner ambush");safe(ambush,s,actors);
        Enemy prior=actors.get(0);s.takeDamage(100);check(manager.retry(s,actors)&&s.getHealth()==100&&actors.isEmpty()&&ambush.getWarningFrames()==60,"Retry restores player and gives complete warning again");
        for(int i=0;i<60;i++)manager.advance(s,actors);check(actors.size()==4&&!actors.contains(prior),"Retry replaces all old actors");safe(ambush,s,actors);
        for(Enemy actor:actors)actor.takeDamage(actor.getMaximumHealth());check(manager.finishActive()==ambush,"Clearing ambush completes once");actors.clear();
        manager.leaveArea(actors);manager.startNearby(Area.Type.FOREST,s,actors,e->true);check(manager.getActive()==null&&manager.isComplete("forest-ambush"),"Completion persists across area cleanup and re-entry");
        EncounterManager pending=new EncounterManager();pending.startNearby(Area.Type.FOREST,s,actors,e->true);pending.leaveArea(actors);
        check(pending.getActive()==null&&pending.get("forest-ambush").getState()==Encounter.State.READY&&pending.get("forest-ambush").getWarningFrames()==0,"Pending ambush clock is cleared on departure");
        for(int i=0;i<6;i++)actors.add(new Walker(350+i*70,400));check(EncounterSpawnPolicy.find(Area.Type.FOREST,1600,1500,30,s,actors)==null,"Unchanged six-enemy live cap prevents allocation");
        GamePanel p=ForestVisualTest.forest();at(santa(p),encounters(p).get("forest-ambush").getCenter());tick(p,1);
        ForestVisualTest.save(p,"11-clearing-ambush-warning");
        Encounter actual=encounters(p).getActive();int warning=actual.getWarningFrames();((StoryOverlay)field(p,"storyOverlay")).start("A suspended presentation.");tick(p,100);
        check(actual.getWarningFrames()==warning&&enemies(p).isEmpty(),"Story presentation freezes warning/spawns");closeOverlay(p);
        tap(p,KeyEvent.VK_ESCAPE);warning=actual.getWarningFrames();tick(p,90);check(actual.getWarningFrames()==warning,"Pause freezes ambush");tap(p,KeyEvent.VK_ESCAPE);
        tick(p,actual.getWarningFrames());check(enemies(p).size()==4,"GamePanel spawns the complete clearing ambush");ForestVisualTest.save(p,"12-clearing-ambush-zombies");
        int spirit=((spirit.SpiritTree)field(p,"spiritTree")).getSpirit();clearWave(p);
        check(story(p).getCrystalsDestroyed()==0&&((spirit.SpiritTree)field(p,"spiritTree")).getSpirit()==spirit,"Optional ambush grants no crystal/Spirit reward");
        check(!"The road is quiet again.".equals(field(p,"areaMessage"))&&actual.getState()==Encounter.State.COMPLETE,"Optional encounter genuinely completes without the redundant road banner");
        p=playing();investigationFixture(p);Encounter crystal=encounters(p).get("crystal-2");at(santa(p),crystal.getCenter());tick(p,1);tick(p,crystal.getWarningFrames());
        for(Enemy actor:enemies(p))actor.takeDamage(actor.getMaximumHealth());tick(p,1);
        @SuppressWarnings("unchecked") List<BlackFrostCrystal> cores=(List<BlackFrostCrystal>)field(p,"crystals");BlackFrostCrystal core=cores.get(1);
        check(core.getState()==BlackFrostCrystal.State.GUARDED&&!crystal.finishIfCleared()&&crystal.getRecoveryFrames()==60,"Second crystal remains protected through recovery");
        tick(p,60);check(crystal.getWarningFrames()==60&&enemies(p).isEmpty(),"Crystal reinforcement has distinct recovery and warning");tick(p,59);check(enemies(p).isEmpty(),"Crystal defenders cannot arrive early");tick(p,1);
        check(enemies(p).size()==3&&enemies(p).stream().filter(e->e instanceof Runner).count()==1,"Authored mixed second crystal defense arrives");
        ForestVisualTest.save(p,"13-crystal-two-reinforcements");
        clearWave(p);check(core.getState()==BlackFrostCrystal.State.VULNERABLE,"Both waves must clear to expose core");
        spirit=((spirit.SpiritTree)field(p,"spiritTree")).getSpirit();at(santa(p),new WorldMap.Point(2032,1088));attack(p,2032,1040);tick(p,24);
        check(core.getState()==BlackFrostCrystal.State.BROKEN&&story(p).getCrystalsDestroyed()==1&&((spirit.SpiritTree)field(p,"spiritTree")).getSpirit()==spirit+15,"Real attack awards original crystal reward exactly once");
        attack(p,2032,1040);tick(p,24);check(story(p).getCrystalsDestroyed()==1&&((spirit.SpiritTree)field(p,"spiritTree")).getSpirit()==spirit+15,"Repeated attack cannot duplicate quest reward");
        call(p,"loadCurrentAreaContent");check(encounters(p).isComplete("crystal-2")&&enemies(p).isEmpty(),"Cleared crystal encounter survives area cleanup");p.stopGame();
        System.out.println("PASS: "+checks+" Forest population/ambush/wave/retry/quest assertions; 31 finite Forest zombies");
    }
}
