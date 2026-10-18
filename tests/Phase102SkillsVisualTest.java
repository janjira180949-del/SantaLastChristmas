import game.*;
import entity.*;
import boss.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** Exact approved balance, existing input ownership, bounded effects and localized arena. */
public class Phase102SkillsVisualTest extends V2TestSupport {
    static final List<Rectangle2D> EMPTY=List.of();
    static final class Regular extends Enemy {
        Regular(double x,double y){super(x,y,300,1,28);}
        @Override public void draw(java.awt.Graphics2D g){ }
    }
    static void advance(SantaSkills skills,List<Enemy> enemies,List<Rectangle2D> solids,int ticks){for(int i=0;i<ticks;i++)skills.update(enemies,solids);}
    static BufferedImage arena(Enemy boss,int ticks,boolean town,boolean restored,Object key){
        var image=new BufferedImage(2000,1700,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        BossArenaVisuals.draw(g,boss,ticks,town,restored,key);g.dispose();return image;
    }
    public static void main(String[] args)throws Exception {
        BossArtwork.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS);
        FrozenForestVisuals.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS);
        Santa santa=new Santa(100,100);AbilitySystem bell=new AbilitySystem();
        Walker near=new Walker(155,110),far=new Walker(500,110);double before=near.getX();
        check(bell.use(1,santa,List.of(near,far),EMPTY)&&near.getHealth()==25&&far.getHealth()==100,"Bell exactly75 per eligible target");
        check(near.getX()>before&&bell.getBellCooldownFrames()==90,"Original knockback and90 cooldown");
        for(int i=0;i<89;i++)bell.update();check(!bell.use(1,santa,List.of(),EMPTY)&&bell.getBellCooldownFrames()==1,"Bell not ready at89");
        bell.update();check(bell.use(1,santa,List.of(),EMPTY),"Bell ready at90");
        SantaSkills skills=new SantaSkills();Walker front=new Walker(170,107),back=new Walker(230,107);
        var enemies=new ArrayList<Enemy>(List.of(front,back));skills.update(enemies,EMPTY);
        check(skills.cast(2,santa,500,121,EMPTY)&&skills.stars().size()==3&&skills.starCooldown()==60,"Three stars, one60-update volley cooldown");
        advance(skills,enemies,EMPTY,40);
        check(front.getHealth()==46&&back.getHealth()==100&&skills.stars().isEmpty(),"54 volley, first contact consumes each projectile");
        advance(skills,enemies,EMPTY,60);check(front.getHealth()==46,"No duplicate projectile damage");
        skills.reset();enemies.clear();
        skills.cast(2,santa,1000,121,EMPTY);for(int i=0;i<1000;i++)check(!skills.cast(2,santa,1000,121,EMPTY),"Cooldown rejects repeated attempts without creating projectiles");
        check(skills.stars().size()==3&&skills.starCooldown()==60,"Fixed reusable pool, rejected casts do not accumulate or restart cooldown");
        advance(skills,enemies,EMPTY,48);check(skills.stars().isEmpty()&&skills.burstCount()<=24,"Finite range and bounded bursts");
        skills.reset();skills.cast(2,santa,1000,121,EMPTY);advance(skills,enemies,EMPTY,1);
        check(skills.stars().get(0).distance()==12&&skills.stars().get(1).distance()==0,"Immediate first release and staggered remaining stars");
        advance(skills,enemies,EMPTY,4);check(skills.stars().get(1).distance()==12&&skills.stars().get(2).distance()==0,"Second release after4 updates");
        advance(skills,enemies,EMPTY,4);check(skills.stars().get(2).distance()==12,"Third release after8 updates");
        var wall=List.<Rectangle2D>of(new Rectangle2D.Double(132,80,2,100));
        skills.reset();front=new Walker(160,107);enemies.add(front);skills.cast(2,santa,600,121,wall);
        advance(skills,enemies,wall,12);check(skills.stars().isEmpty()&&front.getHealth()==100,"All three stars respect thin walls");
        skills.reset();enemies.clear();enemies.add(new Regular(190,135));skills.update(enemies,EMPTY);skills.cast(2,santa,500,121,EMPTY);skills.update(enemies,EMPTY);
        check(skills.stars().get(0).dy()>0&&Math.abs(Math.atan2(skills.stars().get(0).dy(),skills.stars().get(0).dx()))<=.025001,"Gentle bounded steering toward forward living target");
        skills.reset();Regular regular=new Regular(250,107),outside=new Regular(297,107),boundary=new Regular(296,107);
        enemies=new ArrayList<>(List.of(regular,outside,boundary));skills.cast(3,santa,180,121,EMPTY);
        check(skills.bombCooldown()==180&&skills.bombs().get(0).x()==180,"Bomb180 and intended landing point");
        advance(skills,enemies,EMPTY,49);check(regular.getHealth()==300,"No early throw/fuse damage");
        skills.update(enemies,EMPTY);check(regular.getHealth()==190&&boundary.getHealth()==190&&outside.getHealth()==300,"Bomb110, true inclusive130 radius");
        check(regular.getGiftSlowFrames()==60,"Surviving regular enemy gets finite slow");
        advance(skills,enemies,EMPTY,129);check(!skills.cast(3,santa,180,121,EMPTY)&&skills.bombCooldown()==1,"Bomb not ready before180");
        skills.update(enemies,EMPTY);check(skills.cast(3,santa,180,121,EMPTY)&&regular.getHealth()==190,"Ready at180, no repeated old explosion");
        for(Enemy boss:List.of(new FrostFang(230,90),new DarkTitan(220,75),new ChristmasEater(230,90))){
            int hp=boss.getHealth();skills.reset();skills.cast(3,santa,180,121,EMPTY);advance(skills,new ArrayList<>(List.of(boss)),EMPTY,50);
            check(boss.getHealth()==hp-110&&boss.getGiftSlowFrames()==0,"Boss receives110, never slowed");
            boss.applyGiftSlow(60);check(boss.getGiftSlowFrames()==0,"Explicit slow excludes live and historical bosses");
        }
        Regular slowed=new Regular(0,0);slowed.applyGiftSlow(60);slowed.update(new Santa(200,0),EMPTY);
        check(Math.abs(Math.hypot(slowed.getX(),slowed.getY())-.65)<.00001,"Regular movement slow is65 percent");
        for(int i=0;i<60;i++)slowed.advanceCombatTimers();check(slowed.getGiftSlowFrames()==0,"Slow expires on existing combat clock");
        skills.reset();skills.cast(3,santa,2000,121,EMPTY);check(skills.bombs().get(0).x()==396,"Original280 throw clamp");
        skills.reset();skills.cast(3,santa,600,121,wall);check(skills.bombs().get(0).x()<=128,"Original solid-safe landing");
        skills.reset();regular=new Regular(145,107);skills.cast(3,santa,116,121,wall);advance(skills,List.of(regular),wall,50);check(regular.getHealth()==300,"Explosion never leaks through wall");

        var p=playing();SantaSkillsTest.mouse(p,1000,360);tap(p,KeyEvent.VK_2);
        var live=((AbilitySystem)field(p,"abilitySystem")).skills();check(live.stars().size()==3&&live.starCooldown()==60,"Actual key2 releases volley immediately");
        tap(p,KeyEvent.VK_2);check(live.stars().size()==3,"Rapid repress obeys the authorized60-update star cooldown");
        SantaSkillsTest.press(p,KeyEvent.VK_2);tick(p,70);SantaSkillsTest.press(p,KeyEvent.VK_2);tick(p,1);check(live.stars().isEmpty(),"Held key does not repeat");
        p.keyReleased(new KeyEvent(p,KeyEvent.KEY_RELEASED,0,0,KeyEvent.VK_2,KeyEvent.CHAR_UNDEFINED));
        tap(p,KeyEvent.VK_2);tap(p,KeyEvent.VK_3);tap(p,KeyEvent.VK_ESCAPE);
        var frozen=live.stars();int age=live.bombs().get(0).age(),cooldown=live.bombCooldown();tick(p,100);
        check(live.stars().equals(frozen)&&live.bombs().get(0).age()==age&&live.bombCooldown()==cooldown,"Pause freezes projectile, fuse and cooldown clocks");
        tap(p,KeyEvent.VK_ESCAPE);call(p,"loadCurrentAreaContent");check(live.stars().isEmpty()&&live.bombs().isEmpty(),"Area cleanup removes pending volleys/bombs");
        var dialogue=(ui.DialogueManager)field(p,"dialogueManager");dialogue.startNarrative(List.of(new ui.DialogueLine("Santa",ui.Portrait.Kind.SANTA,"Diagnostic.")));
        tap(p,KeyEvent.VK_2);check(live.stars().isEmpty(),"Dialogue blocks casting");dialogue.close();p.stopGame();

        var encounter=new Encounter("crystal-1",Area.Type.FOREST,new WorldMap.Point(976,1520),Encounter.EnemyKind.WALKER);
        BlackFrostCrystal core=new BlackFrostCrystal(encounter);Santa hunter=new Santa(950,1520);hunter.setAttackDirection(1,0);
        check(core.getBounds().equals(new Rectangle2D.Double(958,1490,36,60)),"Original objective bounds/position");
        hunter.startAttack();core.update(hunter);check(core.getImpactFrames()>0&&!core.tryBreak(hunter),"Blocked contact reacts without new crystal HP or bypass");
        var state=Encounter.class.getDeclaredField("state");state.setAccessible(true);state.set(encounter,Encounter.State.COMPLETE);core.update(hunter);
        check(core.getState()==BlackFrostCrystal.State.VULNERABLE&&!core.tryBreak(hunter),"Exposed critical visual retains subsequent-attack gate");
        for(int i=0;i<24;i++)hunter.update(false,false,false,false,EMPTY);hunter.startAttack();core.update(hunter);
        check(core.tryBreak(hunter)&&core.getState()==BlackFrostCrystal.State.BROKEN,"Fresh basic attack still shatters core");
        check(!core.tryBreak(hunter),"No duplicate crystal break");
        for(String id:List.of("crystal-1","crystal-2","crystal-3")){var image=ForestLandmarkArt.core(id,false);check(image.getHeight()>=132&&image.getWidth()>=96,"Three enlarged source-derived silhouettes");}
        check(ForestLandmarkArt.isPrepared()&&FrozenForestVisuals.buildCount()==1,"Prepared core artwork, one Forest cache");

        Object scene=new Object();var dormant=arena(null,10,true,false,scene);Rectangle bounds=BossArenaVisuals.arenaBounds();int inside=0,outsideCount=0;
        for(int y=0;y<dormant.getHeight();y++)for(int x=0;x<dormant.getWidth();x++)if(dormant.getRGB(x,y)!=0){if(bounds.contains(x,y))inside++;else outsideCount++;}
        check(inside>5000&&outsideCount==0,"Arena presentation localized to exact640x400 region");
        DarkTitan titan=new DarkTitan(1248,1248);arena(titan,20,true,false,scene);titan.takeDamage(1200);
        var fading=arena(null,21,true,false,scene);var faded=arena(null,21+BossFallPresentation.TITAN_DURATION,true,false,scene);int activePixels=0,fadedPixels=0;
        for(int y=bounds.y;y<bounds.y+bounds.height;y++)for(int x=bounds.x;x<bounds.x+bounds.width;x++){if(fading.getRGB(x,y)!=0)activePixels++;if(faded.getRGB(x,y)!=0)fadedPixels++;}
        check(activePixels>0&&fadedPixels==0,"Energy and crystals fully fade at the end of the staged Titan defeat");
        check(arena(null,120,false,false,new Object()).getRGB(1296,1296)==0,"No arena in Village/Forest/Base");
        check(arena(null,120,true,true,new Object()).getRGB(1296,1296)==0,"No corrupted arena after restoration");
        System.out.println("PASS: "+checks+" balance, input, collision, crystal and arena assertions");
    }
}
