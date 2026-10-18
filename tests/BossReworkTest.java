import boss.ChristmasEater;
import entity.Santa;
import java.util.List;

public class BossReworkTest extends V2TestSupport {
    public static void main(String[] args) {
        ChristmasEater boss=new ChristmasEater(500,500);Santa s=new Santa(900,900);
        check(ChristmasEater.Phase.values().length==3,"Three distinct phases");
        boss.takeDamage(boss.getMaximumHealth()/2-1);check(boss.getPhase()==ChristmasEater.Phase.PHASE_1,"Phase 1 above 50%");
        boss.takeDamage(1);check(boss.getPhase()==ChristmasEater.Phase.PHASE_2,"Phase 2 at exactly 50%");
        int guard=300;while(boss.getWaveWarningFrames()==0&&guard-->0){boss.advanceCombatTimers();boss.update(s,List.of());}
        check(boss.getWaveWarningFrames()>0,"Frost warning begins");
        int warning=boss.getWaveWarningFrames();s.restoreForRetry((int)boss.getX()+20,(int)boss.getY()+20);int hp=s.getHealth();
        for(int i=0;i<warning-1;i++){boss.advanceCombatTimers();boss.update(s,List.of());check(s.getHealth()==hp,"No wave damage during warning");}
        boss.advanceCombatTimers();boss.update(s,List.of());check(s.getHealth()<hp&&boss.getRecoveryFrames()>0,"One pulse then recovery");
        double x=boss.getX(),y=boss.getY();int after=s.getHealth();
        for(int i=0;i<30;i++){boss.advanceCombatTimers();boss.update(s,List.of());boss.tryAttackSanta(s);s.update(false,false,false,false,List.of());}
        check(boss.getX()==x&&boss.getY()==y&&s.getHealth()==after,"Recovery is stationary and safe");
        ChristmasEater phase1=new ChristmasEater(500,500);Santa escape=new Santa(900,900);
        for(int i=0;i<180;i++){phase1.advanceCombatTimers();phase1.update(escape,List.of());}
        check(phase1.getWaveWarningFrames()==66,"Phase 1 clear warning timing");
        escape.setPosition((int)phase1.getX()+15,(int)phase1.getY()+15);
        for(int i=0;i<65;i++){phase1.advanceCombatTimers();phase1.update(escape,List.of());}
        escape.startDash(false,false,false,true);phase1.advanceCombatTimers();phase1.update(escape,List.of());
        check(escape.getHealth()==100,"Dash protects from final frost pulse");
        phase1.takeDamage(phase1.getMaximumHealth());check(phase1.isDefeated(),"Boss can be defeated");
        System.out.println("PASS: "+checks+" boss assertions");
    }
}
