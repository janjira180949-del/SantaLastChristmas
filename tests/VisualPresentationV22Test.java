import game.*;
import entity.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import ui.*;

/** Actual input, render and progression fixtures at every supported viewport. */
public class VisualPresentationV22Test extends V2TestSupport {
    static final Path OUTPUT=Path.of("build/v2.2/captures/gameplay");
    static final StringBuilder timings=new StringBuilder("[\n");
    static void capture(GamePanel p,String name)throws Exception {
        ImageIO.write(render(p),"png",OUTPUT.resolve(p.getWidth()+"x"+p.getHeight()+"-"+name+".png").toFile());
    }
    static void benchmark(GamePanel p,String name)throws Exception {
        BufferedImage target=new BufferedImage(p.getWidth(),p.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=target.createGraphics();
        for(int i=0;i<20;i++)p.paint(g);long[] times=new long[40];
        for(int i=0;i<times.length;i++){long start=System.nanoTime();p.paint(g);times[i]=System.nanoTime()-start;}
        g.dispose();java.util.Arrays.sort(times);
        if(timings.length()>2)timings.append(",\n");
        timings.append(String.format(java.util.Locale.ROOT,"  {\"scene\":\"%s\",\"width\":%d,\"height\":%d,\"median_ms\":%.3f,\"p95_ms\":%.3f}",name,p.getWidth(),p.getHeight(),times[20]/1e6,times[38]/1e6));
    }
    public static void main(String[] args)throws Exception {
        Files.createDirectories(OUTPUT);
        for(int[] size:new int[][]{{800,600},{1270,681},{1270,720}}) {
            GamePanel p=new GamePanel();p.setSize(size[0],size[1]);capture(p,"01-main-menu");
            tap(p,KeyEvent.VK_ENTER);tick(p,96);capture(p,"02-opening");
            Santa player=santa(p);int sx=player.getX(),sy=player.getY();
            check(((StoryOverlay)field(p,"storyOverlay")).getFrames()>=90&&player.getX()==sx&&player.getY()==sy,"Opening camera never moves gameplay actors");
            tap(p,KeyEvent.VK_E);check(!((StoryOverlay)field(p,"storyOverlay")).isActive(),"Opening can be skipped immediately");
            check(areas(p).getCurrentArea().getType()==Area.Type.SANTA_BASE,"New opening is the permanent workshop");enterVillage(p);
            tick(p,35);capture(p,"03-village-title");tick(p,180);benchmark(p,"village");
            tap(p,KeyEvent.VK_SPACE);int cooldown=player.getDashCooldownFrames();
            var escape=new KeyEvent(p,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_ESCAPE,KeyEvent.CHAR_UNDEFINED);
            p.keyPressed(escape);tick(p,1);p.keyPressed(escape);tick(p,1);
            check(p.getState()==GamePanel.State.PAUSED,"Escape repeat cannot toggle pause repeatedly");
            p.keyReleased(new KeyEvent(p,KeyEvent.KEY_RELEASED,0,0,KeyEvent.VK_ESCAPE,KeyEvent.CHAR_UNDEFINED));
            sx=player.getX();sy=player.getY();int spirit=((spirit.SpiritTree)field(p,"spiritTree")).getSpirit();
            capture(p,"04-pause");tap(p,KeyEvent.VK_D);tap(p,KeyEvent.VK_1);tick(p,120);
            check(player.getX()==sx&&player.getY()==sy&&player.getDashCooldownFrames()==cooldown&&((spirit.SpiritTree)field(p,"spiritTree")).getSpirit()==spirit,"Pause freezes movement, cooldowns and rewards");
            var button=GameScreens.button(p.getWidth(),p.getHeight(),0);var click=new MouseEvent(p,MouseEvent.MOUSE_PRESSED,0,0,(int)button.getCenterX(),(int)button.getCenterY(),1,false,MouseEvent.BUTTON1);
            for(var listener:p.getMouseListeners())listener.mousePressed(click);tick(p,1);check(p.getState()==GamePanel.State.PLAYING,"Pause Resume button works");
            completeOpeningDeliveries(p);tick(p,15);Child toby=p.getChildren().get(0);at(player,new WorldMap.Point(toby.getX()+15,toby.getY()+21));tick(p,90);tap(p,KeyEvent.VK_E);capture(p,"05-toby-dialogue");closeDialogue(p);
            story(p).recordVillageCleared();travelGate(p,Area.Type.FOREST);
            at(player,WorldMap.forArea(Area.Type.FOREST).point("items/Toby Gift"));tap(p,KeyEvent.VK_E);capture(p,"06-gift-found");
            check(story(p).isGiftFound()&&!story(p).isLostGiftComplete(),"Finding gift preserves required return objective");
            travelGate(p,Area.Type.VILLAGE);at(player,new WorldMap.Point(toby.getX()+15,toby.getY()+21));tap(p,KeyEvent.VK_E);capture(p,"07-gift-return");closeDialogue(p);travelGate(p,Area.Type.FOREST);
            check(!story(p).isCrystalQuestActive(),"Gift return activates restoration before crystals");
            FinalChapterTest.collectMaterials(p);FinalChapterTest.restoreVillage(p);receiveBlackFrostClue(p);travelGate(p,Area.Type.FOREST);
            check(story(p).isCrystalQuestActive(),"Actual restoration and clue dialogue activate investigation");
            Encounter wave=encounters(p).get("crystal-1");at(player,wave.getCenter());tick(p,1);capture(p,"08-crystal-guarded");
            check(enemies(p).isEmpty()&&wave.getWarningFrames()==60,"Initial guardian warning precedes combat captures");tick(p,60);
            Enemy foe=enemies(p).get(0);attack(p,foe.getX()+foe.getSize()/2.0,foe.getY()+foe.getSize()/2.0);capture(p,"09-attack");
            tap(p,KeyEvent.VK_SPACE);capture(p,"10-dash");tap(p,KeyEvent.VK_1);capture(p,"11-bell");
            for(Enemy member:wave.getMembers())member.takeDamage(member.getMaximumHealth());tick(p,1);capture(p,"12-crystal-exposed");
            tick(p,30);at(player,new WorldMap.Point(wave.getCenter().x(),wave.getCenter().y()+55));attack(p,wave.getCenter().x(),wave.getCenter().y());capture(p,"13-crystal-break");benchmark(p,"forest");
            check(story(p).getCrystalsDestroyed()==1,"Break capture represents actual crystal destruction and reward");
            check(((CombatVisuals)field(p,"combatVisuals")).activeParticles()<=48,"Combat particles remain bounded");
            at(player,new WorldMap.Point(760,1168));tick(p,30);capture(p,"13a-warm-shelter");
            player.takeDamage(100);tick(p,1);check(p.getState()==GamePanel.State.GAME_OVER,"Game Over presentation follows actual defeat");capture(p,"13b-game-over");
            tap(p,KeyEvent.VK_R);check(p.getState()==GamePanel.State.PLAYING&&story(p).getCrystalsDestroyed()==1,"Retry preserves completed crystal");capture(p,"13c-retry");
            p=playing();p.setSize(size[0],size[1]);townFixture(p);entity.Elias keeper=(entity.Elias)field(p,"elias");at(santa(p),new WorldMap.Point(keeper.getX()-26,keeper.getY()+21));tap(p,KeyEvent.VK_E);capture(p,"14-elias-dialogue");closeDialogue(p);
            at(santa(p),encounters(p).get("town").getCenter());tick(p,1);
            clearWave(p);
            at(santa(p),(WorldMap.Point)call(p,"spiritTreeInteractionPoint"));tick(p,1);tick(p,90);capture(p,"15-boss-introduction");closeOverlay(p);
            boss.DarkTitan eater=(boss.DarkTitan)field(p,"activeBoss");check(eater!=null,"Boss appears after established story gates");
            double bx=eater.getX(),by=eater.getY();capture(p,"16-boss-phase1");
            check(eater.getX()==bx&&eater.getY()==by,"Boss composition never changes gameplay coordinates");
            santa(p).toggleDeveloperTestMode();int guard=300;
            while(eater.attacks().getWarningFrames()==0&&guard-->0)tick(p,1);
            santa(p).toggleDeveloperTestMode();check(eater.attacks().getWarningFrames()>0,"Capture uses the actual frost warning");
            capture(p,"16b-boss-wave-warning");eater.takeDamage(eater.getMaximumHealth()/2);tick(p,1);capture(p,"17-phase-transition");
            check(eater.getTransitionFrames()>0&&eater.getPhaseIndex()==1,"Phase transition effect observes original half-HP trigger");
            benchmark(p,"town-boss");
            p=endingFixture();p.setSize(size[0],size[1]);tick(p,75);benchmark(p,"ending-restoration");
        }
        Files.writeString(Path.of("build/v2.2/render-performance.json"),timings.append("\n]\n").toString());
        System.out.println("PASS: "+checks+" presentation/input assertions; captures at "+OUTPUT+"; warmed offscreen timings recorded (not an interactive FPS guarantee)");
    }
}
