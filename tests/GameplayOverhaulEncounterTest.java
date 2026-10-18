import game.*;
import entity.*;
import java.util.*;
import java.awt.event.KeyEvent;
import static game.Encounter.EnemyKind.*;

/** Exercises real finite wave clocks, spawn footprints, retry, cleanup and dialogue suspension. */
public class GameplayOverhaulEncounterTest extends V2TestSupport {
    static void safe(Encounter e,Santa s,List<Enemy> enemies) {
        check(enemies.stream().filter(v->!v.isDefeated()).count()<=EncounterSpawnPolicy.limit(e.getArea()),"Area active cap");
        for(Enemy enemy:enemies)if(!enemy.isDefeated()) {
            check(!util.Collision.intersectsAny(enemy.getBounds(),WorldMap.forArea(e.getArea()).solids()),"Safe obstacle footprint: "+e.getId());
            check(Math.hypot(enemy.getX()+enemy.getSize()/2.0-s.getX()-16,enemy.getY()+enemy.getSize()/2.0-s.getY()-21)>=128,"Player spawn clearance");
            for(Enemy other:enemies)if(other!=enemy&&!other.isDefeated())check(!enemy.getBounds().intersects(other.getBounds()),"Distinct actor footprints");
        }
    }
    public static void main(String[] args)throws Exception {
        EncounterManager manager=new EncounterManager();check(manager.getEncounters().size()==14,"Thirteen prior groups plus Frost Fang");
        int total=0;
        for(Encounter e:manager.getEncounters()) {
            Santa s=new Santa(e.getCenter().x()-16,e.getCenter().y()-21);List<Enemy> actors=new ArrayList<>();
            check(e.start(s,actors)&&!e.start(s,actors),"Single activation: "+e.getId());
            int guard=400;
            while(!e.finishIfCleared()&&guard-->0) {
                safe(e,s,actors);
                for(Enemy enemy:actors)if(!enemy.isDefeated()){total++;enemy.takeDamage(enemy.getMaximumHealth());}
                actors.removeIf(Enemy::isDefeated);e.advance(s,actors);
            }
            check(guard>0&&e.getState()==Encounter.State.COMPLETE&&!e.start(s,actors),"Finite complete/no duplicate: "+e.getId());
        }
        check(total==45,"43 unchanged normal zombies plus two bosses; Forest normal groups contribute 31");
        Encounter wave=new Encounter("test",Area.Type.FOREST,new WorldMap.Point(976,1520),WALKER).withWave(WALKER,RUNNER);
        Santa s=new Santa(960,1499);List<Enemy> actors=new ArrayList<>();wave.start(s,actors);
        actors.get(0).takeDamage(1000);actors.clear();wave.advance(s,actors);
        check(!wave.finishIfCleared()&&wave.getRecoveryFrames()==60,"Objective waits for second wave with recovery interval");
        for(int i=0;i<60;i++)wave.advance(s,actors);check(wave.getWarningFrames()==60&&actors.isEmpty(),"Full visible warning before spawn");
        for(int i=0;i<59;i++)wave.advance(s,actors);check(actors.isEmpty(),"No premature reinforcements");
        wave.advance(s,actors);check(actors.size()==2&&wave.getWaveNumber()==2,"Exactly one mixed second wave");safe(wave,s,actors);
        Enemy old=actors.get(0);s.takeDamage(100);check(wave.retry(s,actors)&&actors.size()==1&&!actors.contains(old)&&wave.getWaveNumber()==1,"Retry resets finite wave lifecycle");
        wave.abandon(actors);check(actors.isEmpty()&&wave.getState()==Encounter.State.READY,"Area cleanup resets interrupted group");
        for(int i=0;i<6;i++)actors.add(new Walker(300+i*80,300));
        check(EncounterSpawnPolicy.find(Area.Type.FOREST,700,700,30,s,actors)==null,"Cap prevents additional allocation");
        GamePanel p=playing();completeOpeningDeliveries(p);story(p).startAfterDialogue(p.getChildren().get(0));story(p).recordVillageCleared();
        at(santa(p),encounters(p).get("village-outskirts").getCenter());tick(p,1);
        Encounter active=encounters(p).getActive();check(active!=null&&active.getWarningFrames()==60&&enemies(p).isEmpty(),"Optional outskirts warns before first wave");
        tap(p,KeyEvent.VK_ESCAPE);int timer=active.getWarningFrames();tick(p,90);check(active.getWarningFrames()==timer,"Pause freezes warning and spawning");
        tap(p,KeyEvent.VK_ESCAPE);call(p,"loadCurrentAreaContent");check(encounters(p).getActive()==null&&enemies(p).isEmpty(),"Area load removes pending wave clocks");
        at(santa(p),new WorldMap.Point(543,741));tap(p,KeyEvent.VK_E);tick(p,120);
        check(enemies(p).isEmpty(),"Toby dialogue region has no wave attacks");
        System.out.println("PASS: "+checks+" encounter and spawn assertions; 43 finite normal zombies");
    }
}
