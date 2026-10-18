import game.*;
import entity.*;
import java.util.*;
import java.awt.geom.Rectangle2D;
import java.awt.event.KeyEvent;

public class CombatReworkTest extends V2TestSupport {
    public static void main(String[] args)throws Exception {
        Santa s=new Santa(100,100);
        check(s.startDash(false,false,false,true),"Dash starts");check(!s.startDash(false,false,false,true),"Dash spam rejected");
        check(!s.takeDamage(50),"Dash protection");
        for(int i=0;i<10;i++)s.update(false,false,false,false,List.of());
        check(s.getX()==240&&s.getY()==100,"Short directional dash distance");
        for(int i=10;i<Santa.DASH_COOLDOWN-1;i++)s.update(false,false,false,false,List.of());
        check(!s.startDash(false,false,false,true),"Cooldown remains before boundary");s.update(false,false,false,false,List.of());
        check(s.startDash(false,false,false,false),"Facing fallback dash after exact cooldown");
        Santa wall=new Santa(100,100);wall.startDash(false,false,false,true);
        List<Rectangle2D> solids=List.of(new Rectangle2D.Double(145,0,1,500));
        for(int i=0;i<10;i++)wall.update(false,false,false,false,solids);
        check(wall.getX()==113&&!util.Collision.intersectsAny(wall.getBounds(),solids),"Dash cannot tunnel through one-pixel wall");
        for(Area.Type type:Area.Type.values()) {
            WorldMap map=WorldMap.forArea(type);Santa edge=new Santa(map.width()-34,700);
            edge.startDash(false,false,false,true);for(int i=0;i<10;i++)edge.update(false,false,false,false,map.solids());
            check(edge.getBounds().getMaxX()<=map.width(),"Map limits during dash: "+type);
        }
        Walker walker=new Walker(110,110);Santa victim=new Santa(110,110);
        check(!walker.tryAttackSanta(victim)&&walker.isWarning()&&victim.getHealth()==100,"Warning precedes damage");
        for(int i=0;i<29;i++){walker.advanceCombatTimers();check(!walker.tryAttackSanta(victim),"No premature strike");}
        walker.advanceCombatTimers();check(walker.tryAttackSanta(victim)&&victim.getHealth()==92,"Strike after windup");
        for(int i=0;i<54;i++){walker.advanceCombatTimers();walker.tryAttackSanta(victim);victim.update(false,false,false,false,List.of());}
        check(victim.getHealth()==92,"Enemy damage cannot repeat every frame");
        walker.advanceCombatTimers();walker.tryAttackSanta(victim);check(walker.isWarning(),"New warning after cooldown");
        victim.setPosition(500,500);for(int i=0;i<30;i++){walker.advanceCombatTimers();walker.tryAttackSanta(victim);}
        check(victim.getHealth()==92,"Walking out avoids warned attack");
        double x=walker.getX();walker.update(victim,List.of());check(walker.getX()!=x,"Detection chases nearby Santa");
        victim.setPosition(2000,2000);x=walker.getX();walker.advanceCombatTimers();walker.update(victim,List.of());check(walker.getX()==x,"Outside detection stays idle");
        Santa bellSanta=new Santa(100,100);Walker near=new Walker(155,110);Runner far=new Runner(1000,1000);
        AbilitySystem bell=new AbilitySystem();List<Enemy> list=new ArrayList<>(List.of(near,far));
        double before=near.getX();check(bell.use(1,bellSanta,list,solids),"Bell activates");
        check(near.getHealth()==25&&far.getHealth()==70&&near.getX()>before,"Bell range/damage/knockback");
        check(!bell.use(1,bellSanta,list)&&!bell.use(2,bellSanta,list)&&!bell.use(3,bellSanta,list),"Cooldown and removed slots");
        for(int i=0;i<AbilitySystem.BELL_COOLDOWN;i++)bell.update();check(bell.use(1,bellSanta,list),"Bell becomes ready");
        Santa melee=new Santa(100,100);melee.setAttackDirection(1,0);melee.startAttack();
        Walker front=new Walker(140,110),behind=new Walker(50,110);var foes=new ArrayList<Enemy>(List.of(front,behind));
        CombatSystem.update(melee,foes);CombatSystem.update(melee,foes);
        check(front.getHealth()==70&&behind.getHealth()==100,"Directional hitbox hits only once and only forward");
        GamePanel panel=playing();tap(panel,KeyEvent.VK_SPACE);int cd=santa(panel).getDashCooldownFrames();
        panel.keyPressed(new KeyEvent(panel,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_SPACE,' '));tick(panel,100);
        panel.keyPressed(new KeyEvent(panel,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_SPACE,' '));tick(panel,1);
        check(santa(panel).getDashCooldownFrames()==0&&cd>0,"Held Space cannot auto-repeat a dash");
        System.out.println("PASS: "+checks+" combat assertions");
    }
}
