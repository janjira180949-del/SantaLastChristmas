import game.*;
import game.VillageActivities.Site;
import ui.*;
import java.awt.event.*;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Actual panel captures, modal controls, viewport transforms and readable progress surfaces. */
public class VillageActivitiesPresentationTest extends VillageActivitiesTest {
    private static final Path CAPTURES=Path.of("build/village-activities/captures");
    static void capture(GamePanel p,String name)throws Exception{ImageIO.write(render(p),"png",CAPTURES.resolve(name+".png").toFile());}
    public static void main(String[]args)throws Exception{
        Files.createDirectories(CAPTURES);GamePanel p=playing();var progress=activities(p);
        near(p,Site.MARTHA);capture(p,"01-martha-prompt");tap(p,KeyEvent.VK_E);capture(p,"02-martha-dialogue");closeDialogue(p);
        near(p,Site.STREETLIGHTS);tap(p,KeyEvent.VK_E);capture(p,"03-frozen-mechanism");
        int x=santa(p).getX(),y=santa(p).getY(),attacks=santa(p).getAttackSequence();
        for(int key:new int[]{KeyEvent.VK_W,KeyEvent.VK_SPACE,KeyEvent.VK_1,KeyEvent.VK_E})tap(p,key);
        click(p,230,267);tick(p,12);
        check(santa(p).getX()==x&&santa(p).getY()==y&&!santa(p).isDashing()&&santa(p).getAttackSequence()==attacks,"Modal window blocks movement, dash, Bell, world E and attack leakage");
        check(encounters(p).getActive()==null,"Puzzle update does not activate world encounters");
        p.keyPressed(new KeyEvent(p,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_E,'e'));
        click(p,806,480);
        p.keyPressed(new KeyEvent(p,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_E,'e'));tick(p,1);
        check(!window(p).isOpen()&&santa(p).getAttackSequence()==attacks,"Mouse Close consumes its click; held E cannot reopen or leak into world");
        p.keyReleased(new KeyEvent(p,KeyEvent.KEY_RELEASED,0,0,KeyEvent.VK_E,'e'));
        tap(p,KeyEvent.VK_E);check(window(p).isOpen(),"E works again after release");
        close(p);check(p.isGameplayHudVisible(),"Closing restores original HUD and gameplay");
        p.keyPressed(new KeyEvent(p,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_W,'w'));tick(p,1);p.keyReleased(new KeyEvent(p,KeyEvent.KEY_RELEASED,0,0,KeyEvent.VK_W,'w'));
        check(santa(p).getY()<y,"Normal movement works after closing");
        near(p,Site.LANTERN_EAST);tap(p,KeyEvent.VK_E);capture(p,"04-spirit-sequence-observe");tick(p,240);capture(p,"05-spirit-sequence-repeat");close(p);
        near(p,Site.LANTERN_SOUTH);capture(p,"06-black-frost-seal");
        talk(p,Site.OLIVER);capture(p,"07-christmas-delivery-card");close(p);
        collect(p);near(p,Site.IRIS);tap(p,KeyEvent.VK_E);capture(p,"08-direct-delivery-thanks");check(activities(p).delivered(0)&&!window(p).isOpen(),"Direct delivery bypasses modal selection");closeDialogue(p);
        near(p,Site.CAMPFIRE);capture(p,"09-campfire-off");near(p,Site.STREETLIGHTS);tap(p,KeyEvent.VK_E);
        for(int[] size:new int[][]{{800,600},{1024,768},{1270,720},{1600,900}}){
            p.setSize(size[0],size[1]);var box=VillageActivityUI.windowBounds(size[0],size[1]);
            check(box.x>=0&&box.y>=0&&box.getMaxX()<=size[0]&&box.getMaxY()<=size[1],"Activity window stays inside viewport "+size[0]);
            var point=window(p).screenPoint(230,267,size[0],size[1]);check(box.contains(point),"Scaled hit target matches rendered board");
            int count=progress.chapter.completed();click(p,230,267);check(progress.chapter.completed()==count,"Former puzzle mouse targets cannot restore or consume materials");
            if(size[0]==800)capture(p,"10-lantern-800x600");
        }
        close(p);p.setSize(1270,720);
        // Reach and prompt safety use the real full actor and the existing solids.
        for(Site site:Site.values()){
            near(p,site);check(VillageActivities.nearby(santa(p))==site,"Nearest site is unambiguous at authored approach: "+site);
        }
        santa(p).setPosition(1200,1800);check(VillageActivities.nearby(santa(p))==null,"Prompts disappear outside all interaction radii");
        santa(p).setPosition(1872,1200);check(!VillageActivities.inReach(santa(p),Site.CAMPFIRE),"No campfire interaction through the solid house");
        near(p,Site.NOAH);tap(p,KeyEvent.VK_E);check(((DialogueManager)field(p,"dialogueManager")).isActive(),"Story-only Noah uses existing narrative pages");
        santa(p).takeDamage(100);tick(p,1);tap(p,KeyEvent.VK_R);
        check(!((DialogueManager)field(p,"dialogueManager")).isActive()&&p.getState()==GamePanel.State.PLAYING,"Retry clears seal narration as well as NPC and puzzle presentations");
        var before=progress;near(p,Site.STREETLIGHTS);tap(p,KeyEvent.VK_E);santa(p).takeDamage(100);tick(p,1);
        check(p.getState()==GamePanel.State.GAME_OVER&&!window(p).isOpen(),"Defeat cleans transient modal UI");tap(p,KeyEvent.VK_R);
        check(activities(p)==before&&!window(p).isOpen()&&!((DialogueManager)field(p,"dialogueManager")).isActive(),"Retry preserves state and restores controls");
        // Real active combat keeps priority over optional dialogue/puzzles.
        completeOpeningDeliveries(p);story(p).startAfterDialogue(p.getChildren().get(0));at(santa(p),encounters(p).get("village").getCenter());tick(p,1);
        check(encounters(p).getActive()!=null,"Original mandatory encounter activates");near(p,Site.STREETLIGHTS);tap(p,KeyEvent.VK_E);
        check(!window(p).isOpen()&&!((DialogueManager)field(p,"dialogueManager")).isActive(),"Activities cannot freeze an active encounter");
        // A separate complete playthrough captures all visible reactions from real interactions.
        p.stopGame();p=postStory();collectMaterials(p);restoreVillage(p);deliveries(p);
        near(p,Site.NOAH);capture(p,"11-campfire-active");tap(p,KeyEvent.VK_E);capture(p,"12-noah-story-dialogue");closeDialogue(p);
        near(p,Site.LANTERN_SOUTH);capture(p,"13-southern-lantern-restored");
        santa(p).setPosition(1296,1160);capture(p,"14-hopeful-village");
        p.setSize(2560,1920);santa(p).setPosition(1296,976);capture(p,"15-village-activities-overview");
        final GamePanel edtPanel=p;long[] paintNanos={0};
        javax.swing.SwingUtilities.invokeAndWait(()->{long start=System.nanoTime();render(edtPanel);paintNanos[0]=System.nanoTime()-start;});
        check(paintNanos[0]<2_000_000_000L,"Actual warmed Swing paint finishes without blocking on activity work");
        p.stopGame();System.out.println("PASS: "+checks+" actual-render, viewport, input-isolation and cleanup assertions; 15 captures");
    }
}
