import game.*;
import entity.*;
import ui.GameScreens;
import java.awt.event.MouseEvent;
import java.nio.file.*;
import javax.imageio.ImageIO;
import java.awt.event.KeyEvent;

/** Button hit regions, game/death screens and lightweight gameplay feedback captures. */
public class GameScreensInputTest extends V2TestSupport {
    static Path dir=Path.of("build/v2/gameplay");
    static void capture(GamePanel p,String name)throws Exception {ImageIO.write(render(p),"png",dir.resolve(name+".png").toFile());}
    static void click(GamePanel p,int row)throws Exception {
        boolean menu=p.getState()==GamePanel.State.MAIN_MENU;
        var r=menu?ui.CinematicMenu.button(p.getWidth(),p.getHeight(),row):GameScreens.button(p.getWidth(),p.getHeight(),row);
        var event=new MouseEvent(p,MouseEvent.MOUSE_PRESSED,0,0,(int)r.getCenterX(),(int)r.getCenterY(),1,false,MouseEvent.BUTTON1);
        for(var listener:p.getMouseListeners())listener.mousePressed(event);
        if(menu)for(var listener:p.getMouseListeners())listener.mouseReleased(new MouseEvent(p,MouseEvent.MOUSE_RELEASED,0,0,
            (int)r.getCenterX(),(int)r.getCenterY(),1,false,MouseEvent.BUTTON1));
        tick(p,menu?6:1);
    }
    public static void main(String[] args)throws Exception {
        Files.createDirectories(dir);GamePanel p=new GamePanel();p.setSize(1270,720);capture(p,"01-main-menu");
        var blank=new MouseEvent(p,MouseEvent.MOUSE_PRESSED,0,0,12,360,1,false,MouseEvent.BUTTON1);
        for(var listener:p.getMouseListeners())listener.mousePressed(blank);tick(p,6);
        check(p.getState()==GamePanel.State.MAIN_MENU,"Blank menu space cannot start adventure");
        click(p,0);check(p.getState()==GamePanel.State.PLAYING,"Visible Start button starts adventure");closeOverlay(p);check(areas(p).getCurrentArea().getType()==Area.Type.SANTA_BASE,"Actual Start opens Base");enterVillage(p);capture(p,"02-last-village");
        completeOpeningDeliveries(p);story(p).startAfterDialogue(p.getChildren().get(0));at(santa(p),encounters(p).get("village").getCenter());tick(p,1);
        Enemy e=enemies(p).get(0);at(santa(p),new WorldMap.Point((int)e.getX()+e.getSize()/2,(int)e.getY()+e.getSize()/2));tick(p,1);
        check(e.isWarning(),"Enemy warning is active for capture");capture(p,"03-attack-warning");
        tap(p,KeyEvent.VK_SPACE);capture(p,"04-dash-trail");tap(p,KeyEvent.VK_1);capture(p,"05-bell-wave");
        santa(p).takeDamage(100);for(int i=0;i<30&&!santa(p).isDefeated();i++){tick(p,1);santa(p).takeDamage(100);}tick(p,1);
        check(p.getState()==GamePanel.State.GAME_OVER,"Death screen appears");capture(p,"06-game-over");
        click(p,0);check(p.getState()==GamePanel.State.PLAYING&&santa(p).getHealth()==100,"Retry mouse button restores HP");
        santa(p).takeDamage(100);tick(p,1);click(p,1);check(p.getState()==GamePanel.State.MAIN_MENU,"Main Menu mouse button works");
        GamePanel forest=playing();investigationFixture(forest);var wave=encounters(forest).get("crystal-1");at(santa(forest),wave.getCenter());tick(forest,1);capture(forest,"07-guarded-crystal");
        for(Enemy member:wave.getMembers())member.takeDamage(member.getMaximumHealth());tick(forest,1);capture(forest,"08-exposed-crystal");
        GamePanel town=playing();townFixture(town);story(town).acceptEliasQuest();at(santa(town),encounters(town).get("town").getCenter());tick(town,1);
        clearWave(town);
        at(santa(town),(WorldMap.Point)call(town,"spiritTreeInteractionPoint"));tick(town,1);closeOverlay(town);
        var boss=(boss.DarkTitan)field(town,"activeBoss");capture(town,"09-boss-phase-1");
        boss.takeDamage(boss.getMaximumHealth()/2);tick(town,1);capture(town,"10-boss-phase-2");
        System.out.println("PASS: "+checks+" screen/input assertions and ten gameplay captures");
    }
}
