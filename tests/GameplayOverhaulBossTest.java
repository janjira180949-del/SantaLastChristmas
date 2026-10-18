import boss.ChristmasEater;
import entity.*;
import game.*;
import java.util.*;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Damage geometry, clocks and actual arena captures, without a listening claim. */
public class GameplayOverhaulBossTest extends V2TestSupport {
    static void step(ChristmasEater b,Santa s,int n){for(int i=0;i<n;i++){b.advanceCombatTimers();b.update(s,List.of());}}
    static ChristmasEater pulse(Santa s) {
        ChristmasEater b=new ChristmasEater(500,500);b.takeDamage(675);int guard=400;
        while(b.getWaveWarningFrames()==0&&guard-->0)step(b,s,1);
        check(guard>0&&b.getWaveWarningFrames()==72,"Final signature has a 72-tick charge");s.restoreForRetry(s.getX(),s.getY());return b;
    }
    public static void main(String[] args)throws Exception {
        ChristmasEater b=new ChristmasEater(500,500);b.takeDamage(449);check(b.getPhase()==ChristmasEater.Phase.PHASE_1,"Original half-HP boundary");
        b.takeDamage(1);check(b.getPhase()==ChristmasEater.Phase.PHASE_2,"Phase 2 at 50%");b.takeDamage(224);check(b.getPhase()==ChristmasEater.Phase.PHASE_2,"Phase 2 above quarter HP");
        b.takeDamage(1);check(b.getPhase()==ChristmasEater.Phase.PHASE_3&&b.getMaximumHealth()==900,"New Phase 3 at 25%, original HP");
        Santa s=new Santa(550,525);b=new ChristmasEater(500,500);b.tryAttackSanta(s);check(b.getHeavyWarningFrames()==48,"Directional heavy preparation");
        double x=b.getX(),y=b.getY();s.setPosition(499,579);step(b,s,47);check(s.getHealth()==100,"Heavy telegraph does no damage");
        step(b,s,1);check(s.getHealth()==100&&b.getAttackEventSequence()==1&&b.getRecoveryFrames()==42,"Locked sweep can be sidestepped and enters recovery");
        step(b,s,30);check(b.getX()==x&&b.getY()==y,"Counterattack window is stationary");
        s=new Santa(900,900);b=new ChristmasEater(500,500);b.takeDamage(450);int guard=100;
        while(b.getHazardCount()==0&&guard-->0)step(b,s,1);
        check(b.getHazardCount()==3&&b.getHazardWarningFrames()==60,"Only three warned frost patches");
        step(b,s,59);check(s.getHealth()==100,"No damage before hazard activation");step(b,s,1);check(s.getHealth()==100&&b.getHazardLifeFrames()==90,"Activation starts bounded lifetime");
        step(b,s,1);check(s.getHealth()==82,"Active patch damages its real world position");
        s.setPosition(1400,1400);step(b,s,90);check(b.getHazardCount()==0,"Hazards expire and clear");
        s=new Santa(900,900);b=pulse(s);double px=b.getPulseX(),py=b.getPulseY();
        check(!b.isPulseDangerous(px,py)&&!b.isPulseDangerous(px+220,py)&&!b.isPulseDangerous(px,py+220),"Center and cross lanes are actually safe");
        check(b.isPulseDangerous(px+140,py+140)&&!b.isPulseDangerous(px+281,py),"Danger quadrants and outer boundary agree with telegraph");
        s.setPosition((int)px-16,(int)py+150-21);step(b,s,72);
        check(s.getHealth()==100&&b.getRecoveryFrames()==100&&b.getHazardCount()==0,"Safe lane survives actual pulse; long clean counterattack window");
        s=new Santa(900,900);b=pulse(s);s.setPosition((int)b.getPulseX()+140-16,(int)b.getPulseY()+140-21);step(b,s,72);
        check(s.getHealth()==76,"Danger quadrant damages once");b.takeDamage(1000);step(b,s,180);check(b.isDefeated()&&b.getHazardCount()==0&&b.getWaveWarningFrames()==0,"Defeat cancels effects and future attacks");
        Encounter encounter=new Encounter("boss-test",Area.Type.CHRISTMAS_TOWN,new WorldMap.Point(1296,1296),Encounter.EnemyKind.BOSS);
        s=new Santa(1280,1275);List<Enemy> actors=new ArrayList<>();encounter.start(s,actors);b=(ChristmasEater)actors.get(0);b.takeDamage(450);
        encounter.advance(s,actors);for(int i=0;i<59;i++)encounter.advance(s,actors);check(actors.size()==1,"Reinforcement warning precedes spawn");
        encounter.advance(s,actors);check(actors.size()==3,"One bounded Walker/Runner reinforcement group");GameplayOverhaulEncounterTest.safe(encounter,s,actors);
        for(int i=0;i<500;i++)encounter.advance(s,actors);check(actors.size()==3,"No endless boss reinforcements");
        check(encounter.retry(s,actors)&&actors.size()==1&&((ChristmasEater)actors.get(0)).getPhase()==ChristmasEater.Phase.PHASE_1,"Retry clears summons and phases");
        b=(ChristmasEater)actors.get(0);b.takeDamage(450);for(int i=0;i<61;i++)encounter.advance(s,actors);b.takeDamage(1000);
        check(encounter.finishIfCleared()&&actors.isEmpty(),"Boss defeat immediately cleans surviving summons");
        Encounter edge=new Encounter("boss-clearance",Area.Type.CHRISTMAS_TOWN,new WorldMap.Point(1296,1296),Encounter.EnemyKind.BOSS);
        Santa edgePlayer=new Santa(1395,1387);List<Enemy> edgeActors=new ArrayList<>();edge.start(edgePlayer,edgeActors);
        ((ChristmasEater)edgeActors.get(0)).takeDamage(450);for(int i=0;i<61;i++)edge.advance(edgePlayer,edgeActors);
        check(edgeActors.size()==3,"Boundary fixture summons the full group");GameplayOverhaulEncounterTest.safe(edge,edgePlayer,edgeActors);
        GamePanel panel=playing();panel.setSize(1280,800);townFixture(panel);story(panel).acceptEliasQuest();
        at(santa(panel),encounters(panel).get("town").getCenter());tick(panel,1);clearWave(panel);
        at(santa(panel),(WorldMap.Point)call(panel,"spiritTreeInteractionPoint"));tick(panel,1);closeOverlay(panel);santa(panel).toggleDeveloperTestMode();
        var titan=(boss.DarkTitan)field(panel,"activeBoss");Path out=Path.of("build/gameplay-overhaul/captures");Files.createDirectories(out);
        santa(panel).setPosition((int)titan.getX()+60,(int)titan.getY()+20);tick(panel,1);
        guard=200;while(titan.attacks().getWarningFrames()==0&&guard-->0)tick(panel,1);
        check(guard>0&&titan.attacks().getAttack()==boss.BossAttacks.Attack.GROUND_SLAM,"Actual Titan Phase 1 slam warning captured");
        ImageIO.write(render(panel),"png",out.resolve("boss-phase1.png").toFile());titan.takeDamage(600);tick(panel,47);
        ImageIO.write(render(panel),"png",out.resolve("boss-phase2-frost.png").toFile());titan.takeDamage(300);guard=450;
        while(titan.attacks().getWarningFrames()==0&&guard-->0)tick(panel,1);check(guard>0,"Actual final phase reaches signature");
        ImageIO.write(render(panel),"png",out.resolve("boss-phase3-pulse.png").toFile());tick(panel,titan.attacks().getWarningFrames()+18);
        ImageIO.write(render(panel),"png",out.resolve("boss-phase3-recovery.png").toFile());panel.stopGame();
        System.out.println("PASS: "+checks+" boss mechanics, safety and presentation assertions");
    }
}
