import game.*;
import entity.*;
import quest.*;
import ui.*;
import spirit.SpiritTree;
import java.awt.event.KeyEvent;

/** Actual E/update handoffs, staged Forest visits, direct deliveries and session retention. */
public class QuestFlowTest extends V2TestSupport {
    static VillageActivities activities(GamePanel p)throws Exception{return (VillageActivities)field(p,"villageActivities");}
    static void nearToby(GamePanel p)throws Exception{var c=p.getChildren().get(0);at(santa(p),new WorldMap.Point(c.getX()+15,c.getY()+21));}
    public static void main(String[]args)throws Exception{
        GamePanel p=playing();var story=story(p);var a=activities(p);var santa=santa(p);
        check(StoryQuest.Id.LOST_GIFT.ordinal()==0&&StoryQuest.Id.BREAK_THE_FROST.ordinal()==1&&StoryQuest.Id.SAVE_CHRISTMAS.ordinal()==2&&StoryQuest.Id.RESTORE_THE_VILLAGE.ordinal()==3,"Existing audio completion bits retain ordinals");
        check(!a.chapter.isUnlocked()&&!story.isCrystalQuestActive(),"Future content locked at Village arrival");
        check(p.getChildren().get(0).getX()==1281&&p.getChildren().get(0).getY()==987,"Toby uses verified central plaza position");
        check(VillageActivities.Site.IRIS.x==560&&VillageActivities.Site.IRIS.y==624,"Iris uses separate northern approach");
        completeOpeningDeliveries(p);nearToby(p);tap(p,KeyEvent.VK_E);check(!story.isStoryStarted(),"Initial quest waits for final page");closeDialogue(p);
        at(santa,encounters(p).get("village").getCenter());tick(p,1);clearWave(p);travelGate(p,Area.Type.FOREST);
        for(var item:FinalChapter.PICKUPS){at(santa,item.point());tap(p,KeyEvent.VK_E);check(!a.chapter.isCollected(item.id()),"First Forest visit cannot collect material "+item.id());}
        @SuppressWarnings("unchecked") var crystals=(java.util.List<BlackFrostCrystal>)field(p,"crystals");
        for(var c:crystals){at(santa,c.getPosition());tick(p,1);check(c.getState()==BlackFrostCrystal.State.DORMANT&&encounters(p).get(c.getId()).getState()==Encounter.State.READY,"Unassigned crystal has no guardians");check(!c.tryBreak(santa)&&!story.recordCrystalDestroyed(c.getId()),"Dormant crystal cannot award progress");}
        at(santa,WorldMap.forArea(Area.Type.FOREST).point("items/Toby Gift"));tap(p,KeyEvent.VK_E);int spirit=((SpiritTree)field(p,"spiritTree")).getSpirit();
        travelGate(p,Area.Type.VILLAGE);nearToby(p);tap(p,KeyEvent.VK_E);
        check(!a.chapter.isUnlocked(),"Opening return page cannot unlock materials");closeDialogue(p);
        check(a.chapter.isUnlocked()&&story.getStage()==StoryQuestManager.Stage.RESTORE_VILLAGE&&!story.isCrystalQuestActive(),"Successful gift return assigns restoration only");
        check(((SpiritTree)field(p,"spiritTree")).getSpirit()==spirit+20,"Gift reward once");
        nearToby(p);tap(p,KeyEvent.VK_E);closeDialogue(p);check(((SpiritTree)field(p,"spiritTree")).getSpirit()==spirit+20,"Repeated Toby has no duplicate reward");
        travelGate(p,Area.Type.FOREST);FinalChapterTest.collectMaterials(p);FinalChapterTest.restoreVillage(p);
        check(story.getRestorationQuest().getProgress()==4&&story.isRestorationComplete()&&story.getStage()==StoryQuestManager.Stage.TALK_TO_TOBY,"Restoration observes sole chapter flags and enters real handoff state");
        check(!story.synchronizeRestoration()&&!story.isCrystalQuestActive(),"Completion synchronization idempotent; no automatic investigation");
        nearToby(p);tap(p,KeyEvent.VK_E);var dialogue=(DialogueManager)field(p,"dialogueManager");
        check(dialogue.isBlackFrostClueDialogueActive()&&!story.isCrystalQuestActive(),"Clue conversation pending");
        tap(p,KeyEvent.VK_E);check(!story.isCrystalQuestActive(),"Intermediate clue page remains locked");closeDialogue(p);
        check(story.isCrystalQuestActive()&&!story.acceptBlackFrostClue(p.getChildren().get(0),santa),"Final close activates investigation exactly once");
        check(a.deliveries()==3&&story.getStage()==StoryQuestManager.Stage.INVESTIGATE,"Required opening deliveries remain completed throughout investigation");
        travelGate(p,Area.Type.FOREST);check(a.chapter.completed()==4&&a.deliveries()==3,"Travel retains independent progress");
        check(!areas(p).tryTravel(Area.Type.CHRISTMAS_TOWN),"Town still requires all crystals");
        for(int i=1;i<=3;i++)check(story.recordCrystalDestroyed("crystal-"+i),"Original crystal IDs accepted only after handoff");
        check(!areas(p).isConnectionAvailable(Area.Type.CHRISTMAS_TOWN),"Three crystals keep Town locked until Fang falls");
        check(story.recordFrostFangDefeated()&&areas(p).isConnectionAvailable(Area.Type.CHRISTMAS_TOWN),"Fang opens the original Town gate");
        call(p,"retryEncounter");check(story.isRestorationComplete()&&a.deliveries()==3,"Retry retains story and optional state");
        call(p,"resetSession");check(areas(p).getCurrentArea().getType()==Area.Type.SANTA_BASE&&!activities(p).chapter.isUnlocked()&&activities(p).deliveries()==0,"Fresh Start resets full session");
        System.out.println("PASS: "+checks+" staged quest and direct delivery assertions");
    }
}
