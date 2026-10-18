import game.*;
import game.FinalChapter.Material;
import game.FinalChapter.Restoration;
import game.VillageActivities.Site;
import ui.*;
import java.awt.event.KeyEvent;
import java.util.*;

/** Finite chapter model and real E/gate/dialogue lifecycle, with independent main-story checks. */
public class FinalChapterTest extends V2TestSupport {
    static VillageActivities activities(GamePanel p)throws Exception{return (VillageActivities)field(p,"villageActivities");}
    static FinalChapter chapter(GamePanel p)throws Exception{return activities(p).chapter;}
    static VillageActivityUI window(GamePanel p)throws Exception{return (VillageActivityUI)field(p,"activityWindow");}
    static void near(GamePanel p,Site site)throws Exception{
        int offset=site==Site.IRIS?25:site==Site.TREE?16:site==Site.HOUSE?0:40;
        at(santa(p),new WorldMap.Point(site.x,site.y+offset));call(p,"updateCamera");
        check(VillageActivities.inReach(santa(p),site),"Unobstructed existing full-actor approach: "+site);
    }
    static void closeWindow(GamePanel p)throws Exception{tap(p,KeyEvent.VK_ESCAPE);check(!window(p).isOpen()&&p.getState()==GamePanel.State.PLAYING,"Requirement/card ESC closes without pausing the adventure");}
    /** Authorized migration: restoration fixtures now occur during Forest progression before the boss. */
    static GamePanel postStory()throws Exception{
        GamePanel p=playing();forestFixture(p);FinalChapterVisuals.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS);
        check(chapter(p).isUnlocked()&&!story(p).isBossDefeated(),"Gift return unlocks assigned pre-boss restoration");return p;
    }
    static void collectMaterials(GamePanel p)throws Exception{
        if(areas(p).getCurrentArea().getType()==Area.Type.CHRISTMAS_TOWN)travelGate(p,Area.Type.FOREST);
        else if(areas(p).getCurrentArea().getType()==Area.Type.SANTA_BASE){travelGate(p,Area.Type.VILLAGE);travelGate(p,Area.Type.FOREST);}
        else if(areas(p).getCurrentArea().getType()==Area.Type.VILLAGE)travelGate(p,Area.Type.FOREST);
        for(var pickup:FinalChapter.PICKUPS)if(!chapter(p).isCollected(pickup.id())){
            at(santa(p),pickup.point());
            check(!util.Collision.intersectsAny(santa(p).getBounds(),WorldMap.forArea(Area.Type.FOREST).solids()),"Pickup approach has full actor clearance: "+pickup.id());
            check(chapter(p).nearby(santa(p))==pickup,"Visible uncollected pickup selected without overlap: "+pickup.id());
            int before=chapter(p).count(pickup.material());tap(p,KeyEvent.VK_E);
            check(chapter(p).isCollected(pickup.id())&&chapter(p).count(pickup.material())==before+1,"Actual E collects exactly one material: "+pickup.id());
            check(chapter(p).nearby(santa(p))==null&&!chapter(p).collect(pickup.id()),"Collected pickup disappears and rejects duplicate: "+pickup.id());
            check(encounters(p).getActive()==null,"Material itself does not trigger a new combat requirement: "+pickup.id());
        }
    }
    static void restoreVillage(GamePanel p)throws Exception{
        if(areas(p).getCurrentArea().getType()==Area.Type.FOREST)travelGate(p,Area.Type.VILLAGE);
        for(Restoration objective:Restoration.values())if(!chapter(p).restored(objective)){
            Site site=switch(objective){case STREETLIGHTS->Site.STREETLIGHTS;case HOUSE->Site.HOUSE;case TREE->Site.TREE;case CAMPFIRE->Site.CAMPFIRE;};
            near(p,site);int before=chapter(p).completed();tap(p,KeyEvent.VK_E);
            check(chapter(p).restored(objective)&&chapter(p).completed()==before+1&&!window(p).isOpen(),"Single E restores immediately, without puzzle or confirmation: "+objective);
            check(!chapter(p).restore(objective),"Restoration rejects duplicate completion: "+objective);
        }
    }
    /** Used by historical credits tests to reach the newly authorized final entry, not bypass it. */
    static void finishChapter(GamePanel p)throws Exception{JourneyHomeTest.finishHomecoming(p);}
    static void model(){
        var chapter=new FinalChapter();check(FinalChapter.PICKUPS.size()==9,"Exactly nine authored materials");
        for(int id=-1;id<=9;id++)check(!chapter.collect(id),"No pickup before restoration unlock");
        for(Restoration objective:Restoration.values())check(!chapter.restore(objective),"No village objective before tree unlock");
        check(!chapter.finishLetter()&&chapter.unlock()&&!chapter.unlock(),"Chapter unlock is once-only and letter is gated");
        for(Material kind:Material.values()){
            check(FinalChapter.PICKUPS.stream().filter(p->p.material()==kind).count()==3,"Exactly three of each material");
            for(var p:FinalChapter.PICKUPS)if(p.material()==kind){int before=chapter.count(kind);check(chapter.collect(p.id()),"Finite pickup accepted");check(chapter.count(kind)==before+1&&!chapter.collect(p.id()),"Monotonic count and duplicate suppression");}
        }
        check(!chapter.collect(-1)&&!chapter.collect(9),"Invalid pickup identifiers rejected");
        for(Restoration objective:Restoration.values()){check(chapter.ready(objective)&&chapter.restore(objective),"Complete material set restores objective");check(!chapter.restore(objective),"Restoration completed once");for(Material kind:Material.values())check(chapter.count(kind)==3,"Restoration preserves cumulative collected totals");}
        check(chapter.isComplete()&&chapter.completed()==4&&!chapter.finishLetter(),"Restoration alone cannot bypass celebration and sleigh");
        check(chapter.beginCelebration()&&!chapter.beginCelebration(),"Celebration begins once");chapter.completeCelebration();
        check(chapter.returnHome()&&!chapter.returnHome()&&chapter.finishLetter()&&!chapter.finishLetter(),"Homecoming gates once-only final-letter completion");
    }
    public static void main(String[]args)throws Exception{
        model();JourneyHomeTest.progression();System.out.println("PASS: "+checks+" retained finite material/restoration model and redesigned progression assertions");
    }
}
