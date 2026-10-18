import game.*;
import ui.*;
import java.awt.Rectangle;
import java.awt.event.*;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Real renderer, shared draw/click geometry, viewport layouts and modal input isolation. */
public class QuestFlowPresentationTest extends QuestFlowTest {
    static QuestGuidance.Objective objective(GamePanel p)throws Exception{return (QuestGuidance.Objective)call(p,"currentObjective");}
    static void mouse(GamePanel p,int x,int y)throws Exception{
        var event=new MouseEvent(p,MouseEvent.MOUSE_PRESSED,0,0,x,y,1,false,MouseEvent.BUTTON1);
        for(var listener:p.getMouseListeners())listener.mousePressed(event);tick(p,1);
    }
    static void capture(GamePanel p,String name)throws Exception{
        call(p,"updateCamera");Path dir=Path.of("build/quest-flow/captures");Files.createDirectories(dir);
        ImageIO.write(render(p),"png",dir.resolve(name+".png").toFile());
    }
    public static void main(String[]args)throws Exception{
        GamePanel p=playing();var a=activities(p);var story=story(p);
        check(a.chapter.trackerCollapsed(),"Quest list starts compact before restoration unlock");
        for(int[] size:new int[][]{{800,600},{1024,768},{1270,720},{1600,900}}){
            p.setSize(size[0],size[1]);var view=objective(p);var box=HUD.questBounds(view,a.chapter,size[0],size[1]);var button=HUD.questToggleBounds(view,a.chapter,size[0],size[1]);
            check(box.contains(button)&&box.y>=114&&box.getMaxY()<=size[1]-10,"Compact quest fits viewport "+size[0]);
            int attack=santa(p).getAttackSequence();capture(p,"arrival-collapsed-"+size[0]);mouse(p,button.x+10,button.y+10);
            check(!a.chapter.trackerCollapsed()&&santa(p).getAttackSequence()==attack,"Visible expand control consumes mouse "+size[0]);
            box=HUD.questBounds(objective(p),a.chapter,size[0],size[1]);check(box.y>=114&&box.x+box.width<306,"Expanded optional list clears health and abilities");
            check(view.details().size()==3&&view.optional().isEmpty(),"Expanded optional data contains only undelivered recipients");
            capture(p,"arrival-expanded-"+size[0]);mouse(p,box.x+50,box.y+55);check(santa(p).getAttackSequence()==attack,"Body clicks do not attack");
            button=HUD.questToggleBounds(objective(p),a.chapter,size[0],size[1]);mouse(p,button.x+10,button.y+10);check(a.chapter.trackerCollapsed(),"Collapse control works");
        }
        p.setSize(1270,720);forestFixture(p);check(a.chapter.trackerCollapsed(),"Collapse state persists through actual round trip");
        a.chapter.toggleTracker();var view=objective(p);check(view.details().size()==4,"Four incomplete restoration objectives");
        for(int[] size:new int[][]{{800,600},{1024,768},{1270,720},{1600,900}}){p.setSize(size[0],size[1]);var box=HUD.questBounds(objective(p),a.chapter,size[0],size[1]);check(box.y>=114,"Largest restored quest panel clears top HUD");capture(p,"materials-expanded-"+size[0]);}
        p.setSize(1270,720);FinalChapterTest.collectMaterials(p);travelGate(p,Area.Type.VILLAGE);
        for(var r:FinalChapter.Restoration.values()){
            santa(p).setPosition(r.approach.x()-16,r.approach.y()+4);tap(p,KeyEvent.VK_E);
            check(objective(p).details().size()==4-a.chapter.completed(),"Completed objective removed immediately");
            capture(p,"restoration-"+a.chapter.completed());
        }
        check(objective(p).title().equals("Talk to Toby")&&objective(p).details().isEmpty(),"Completed tracker replaced by handoff, never premature Town instruction");
        receiveBlackFrostClue(p);travelGate(p,Area.Type.FOREST);
        check(objective(p).title().equals("Investigate the Black Frost")&&objective(p).target()!=null,"Arrow and title use same investigation snapshot");
        capture(p,"investigation-active");travelGate(p,Area.Type.VILLAGE);
        check(objective(p).optional().isEmpty()&&a.deliveries()==3,"Completed mandatory deliveries never clutter investigation HUD");
        call(p,"retryEncounter");check(!a.chapter.trackerCollapsed(),"Retry preserves expanded state");
        call(p,"resetSession");check(activities(p).chapter.trackerCollapsed(),"Fresh Start resets compact state");
        System.out.println("PASS: "+checks+" quest viewport, renderer and input assertions");
    }
}
