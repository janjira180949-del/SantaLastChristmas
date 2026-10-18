import game.*;
import spirit.SpiritTree;
import java.awt.event.KeyEvent;
/** The runtime restoration is 180 updates; the existing authored outcome remains optional. */
public class EndingCutsceneTest extends V2TestSupport {
    public static void main(String[] args)throws Exception {
        GamePanel p=endingFixture();var cut=(SpiritTreeTransformation)field(p,"treeTransformation");var tree=(SpiritTree)field(p,"spiritTree");
        int spirit=tree.getSpirit();tick(p,29);check(cut.lightProgress()==0&&!cut.isRestored(),"Old tree remains for first half-second");
        tick(p,1);check(cut.getFrames()==30,"Golden phase begins at frame 30");tick(p,30);
        check(cut.lightProgress()==.5&&!cut.isRestored(),"Golden light expands during second phase");tick(p,37);
        check(cut.flashOpacity()==1&&!cut.isRestored(),"White flash at 1.5 to 2 seconds");tick(p,23);
        check(cut.isRestored()&&cut.getFrames()==120,"Distinct tree reveal begins at 2 seconds");tick(p,59);
        check(tree.getSpirit()==spirit&&p.getState()==GamePanel.State.PLAYING,"Reward and control wait until exact end");tick(p,1);
        check(cut.isComplete()&&tree.getSpirit()==100&&((EndingCutscene)field(p,"endingCutscene")).isCelebration(),"At exactly 180 ticks cap Spirit and enter celebration");
        JourneyHomeTest.completeCelebration(p);check(p.getState()==GamePanel.State.PLAYING,"Celebration releases control");
        check(!cut.start(),"Cannot repeat transformation or reward");
        tap(p,KeyEvent.VK_E);var end=(EndingCutscene)field(p,"endingCutscene");
        tick(p,60);
        for(int[] size:new int[][]{{800,600},{1270,681},{1270,720}}) {
            p.setSize(size[0],size[1]);call(p,"updateCamera");Camera camera=(Camera)field(p,"camera");
            // Cinematic panel starts height-152; keep an additional eight-pixel gap.
            for(var feet:TownCelebration.feet())check(feet.y()-camera.getY()<size[1]-160,"Actual reused resident feet remain above gratitude dialogue at "+size[0]+"x"+size[1]);
        }
        check(end.getState()==EndingCutscene.State.CELEBRATION_DIALOGUE,"Restored-tree E opens concise gratitude, not the retired long outcome");
        check(end.getCinematicDialogue().getCurrentLine().getSpeakerName().equals("Elias"),"Town Keeper leads the preserved thank-you handoff");
        tap(p,KeyEvent.VK_E);check(end.getCinematicDialogue().getCurrentLine().getSpeakerName().equals("Santa"),"Santa answers the neighbors");tap(p,KeyEvent.VK_E);
        check(!end.hasStarted()&&p.getState()==GamePanel.State.PLAYING,"Established outcome returns control for the new chapter");
        FinalChapterTest.finishChapter(p);tick(p,60);check(end.getState()==EndingCutscene.State.TITLE,"Final Letter unlocks final title");
        tap(p,KeyEvent.VK_E);check(end.getState()==EndingCutscene.State.ENDING_MESSAGE,"Existing ending message retained");
        tap(p,KeyEvent.VK_E);check(end.getState()==EndingCutscene.State.CREDITS,"Existing credits accessible");
        tap(p,KeyEvent.VK_ESCAPE);check(p.getState()==GamePanel.State.PLAYING&&cut.isRestored()&&areas(p).getCurrentArea().getType()==Area.Type.SANTA_BASE,"Return to workshop from credits preserves restored Spirit Tree");
        System.out.println("PASS: "+checks+" short restoration and authored outcome assertions");
    }
}
