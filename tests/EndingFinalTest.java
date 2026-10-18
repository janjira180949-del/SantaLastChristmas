import game.*;
import spirit.SpiritTree;
import java.awt.event.KeyEvent;
/** Input locks for exactly three seconds, persistent restoration, and optional ending navigation. */
public class EndingFinalTest extends V2TestSupport {
    public static void main(String[] args)throws Exception {
        GamePanel p=endingFixture();var cut=(SpiritTreeTransformation)field(p,"treeTransformation");
        var tree=(SpiritTree)field(p,"spiritTree");int x=santa(p).getX(),y=santa(p).getY(),hp=santa(p).getHealth();
        var quests=story(p).getQuests().stream().map(q->q.getState()).toList();
        var positions=p.getChildren().stream().map(c->new WorldMap.Point(c.getX(),c.getY())).toList();
        for(int key:new int[]{KeyEvent.VK_1,KeyEvent.VK_E,KeyEvent.VK_C})tap(p,key);
        check(santa(p).getX()==x&&santa(p).getY()==y&&santa(p).getHealth()==hp&&!santa(p).isAttacking(),"Restoration still rejects combat/repeated E/credits actions");
        check(p.isGameplayHudVisible(),"In-world restoration retains gameplay HUD");tick(p,180-cut.getFrames());
        check(tree.getSpirit()==100&&cut.isComplete()&&!p.isGameplayHudVisible(),"Exact restoration completion enters celebration with capped Spirit");
        JourneyHomeTest.completeCelebration(p);check(p.isGameplayHudVisible(),"Celebration releases the normal HUD");
        check(quests.equals(story(p).getQuests().stream().map(q->q.getState()).toList()),"No duplicate quest events");
        check(positions.equals(p.getChildren().stream().map(c->new WorldMap.Point(c.getX(),c.getY())).toList()),"NPC world coordinates never change");
        tap(p,KeyEvent.VK_S);check(santa(p).getY()>y,"Movement returns immediately after celebration");
        FinalChapterTest.finishChapter(p);tap(p,KeyEvent.VK_E);tap(p,KeyEvent.VK_E);tap(p,KeyEvent.VK_E);var end=(EndingCutscene)field(p,"endingCutscene");
        check(end.getState()==EndingCutscene.State.CREDITS,"Credits shortcut works");
        tick(p,EndingCutscene.duration(end.getState())-end.getFrames());check(end.isComplete(),"Existing timed credits reach final ending state");
        check(tree.getSpirit()==100,"Credits cannot repeat rewards");
        tap(p,KeyEvent.VK_ESCAPE);check(p.getState()==GamePanel.State.MAIN_MENU,"Final state can return to menu");
        System.out.println("PASS: "+checks+" short cinematic input/state assertions");
    }
}
