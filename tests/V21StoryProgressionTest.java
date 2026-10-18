import game.*;
import entity.*;
import quest.*;
import spirit.SpiritTree;
import ui.DialogueManager;
import java.awt.event.KeyEvent;
import java.lang.reflect.Method;
import java.util.List;

/** Story event boundaries, reward timing and persistent state through real input/retry. */
public class V21StoryProgressionTest extends V2TestSupport {
    static boolean allowed(GamePanel p,String id)throws Exception {
        Method method=GamePanel.class.getDeclaredMethod("isEncounterAllowed",Encounter.class);
        method.setAccessible(true);return (boolean)method.invoke(p,encounters(p).get(id));
    }
    static int spirit(GamePanel p)throws Exception { return ((SpiritTree)field(p,"spiritTree")).getSpirit(); }
    public static void main(String[] args)throws Exception {
        GamePanel p=playing();var story=story(p);var toby=p.getChildren().get(0);
        check(!story.recordCrystalDestroyed("crystal-1")&&!story.acceptEliasQuest()&&!story.recordBossDefeated()&&!story.triggerEnding(),"Initial story cannot be skipped");
        check(!allowed(p,"crystal-1")&&!allowed(p,"boss"),"Encounters initially gated");
        completeOpeningDeliveries(p);
        at(santa(p),new WorldMap.Point(toby.getX()+15,toby.getY()+21));tap(p,KeyEvent.VK_E);
        var dialogue=(DialogueManager)field(p,"dialogueManager");boolean mother=false,promise=false;
        while(dialogue.isActive()) {
            String text=dialogue.getActiveDialogue().getCurrentLine().getText();
            mother|=text.contains("last thing I have");promise|=text.contains("bring it back");tap(p,KeyEvent.VK_E);
        }
        check(mother&&promise&&story.isStoryStarted(),"Toby's memory and Santa's promise are acknowledged");
        tap(p,KeyEvent.VK_E);closeDialogue(p);
        check(spirit(p)==0&&story.getQuests().get(0).getProgress()==0,"Repeated introductory dialogue cannot award Spirit or progress");
        at(santa(p),encounters(p).get("village").getCenter());tick(p,1);clearWave(p);travelGate(p,Area.Type.FOREST);
        check(!story.returnGiftToToby(toby,santa(p)),"Gift cannot be returned before pickup");
        at(santa(p),WorldMap.forArea(Area.Type.FOREST).point("items/Toby Gift"));tap(p,KeyEvent.VK_E);
        check(story.isGiftFound()&&!story.isLostGiftComplete()&&spirit(p)==0,"Pickup carries gift without completion or reward");
        check(story.getHudPresentation().objective().contains("Return the gift to Toby"),"Return objective is explicit");
        check(!"Toby's Gift Found! Return the Gift to Toby.".equals(field(p,"areaMessage"))&&story.isGiftFound(),"Gift pickup advances without the redundant banner");
        at(santa(p),encounters(p).get("crystal-1").getCenter());tick(p,1);
        check(encounters(p).getActive()==null&&!allowed(p,"crystal-1")&&!story.recordCrystalDestroyed("crystal-1"),"Guardians and crystal progress remain locked while carrying gift");
        check(!areas(p).tryTravel(Area.Type.CHRISTMAS_TOWN)&&!allowed(p,"boss"),"Town and boss locked before return");
        // Rest and Retry while carrying the gift, using the persistent in-game point.
        @SuppressWarnings("unchecked") List<HealingPoint> points=(List<HealingPoint>)field(p,"healingPoints");
        HealingPoint shelter=points.get(0);at(santa(p),shelter.getPosition());santa(p).takeDamage(60);tap(p,KeyEvent.VK_E);
        check(shelter.isUsed()&&santa(p).getHealth()==80&&field(p,"areaMessage").equals("HP Restored"),"E rests for 40 HP with feedback");
        tick(p,24);santa(p).takeDamage(100);tick(p,1);tap(p,KeyEvent.VK_R);
        check(story.isGiftFound()&&!story.isLostGiftComplete()&&shelter.isUsed()&&spirit(p)==0,"Retry preserves carried gift and spent shelter");
        at(santa(p),shelter.getPosition());tap(p,KeyEvent.VK_E);
        check(field(p,"areaMessage").equals("Shelter Already Used"),"Spent shelter interaction is acknowledged");
        travelGate(p,Area.Type.VILLAGE);at(santa(p),new WorldMap.Point(toby.getX()+15,toby.getY()+21));tap(p,KeyEvent.VK_E);
        check(dialogue.isGiftReturnDialogueActive()&&!story.isLostGiftComplete()&&spirit(p)==0,"Return waits for acknowledgement and lore pages");
        boolean thanks=false,anchors=false,tree=false,road=false;
        while(dialogue.isActive()) {
            String text=dialogue.getActiveDialogue().getCurrentLine().getText();
            thanks|=text.contains("Thank you");anchors|=text.contains("anchors");tree|=text.contains("Spirit Tree");road|=text.contains("blocking the road");
            check(!allowed(p,"crystal-1"),"Crystal quest stays locked through all return pages");tap(p,KeyEvent.VK_E);
        }
        check(thanks&&!anchors&&!tree&&!road,"Return dialogue acknowledges the gift and assigns restoration without premature crystal lore");
        check(story.isLostGiftComplete()&&!story.isCrystalQuestActive()&&spirit(p)==20&&!allowed(p,"crystal-1"),"Completed return rewards once and assigns restoration");
        check(!story.returnGiftToToby(toby,santa(p)),"Duplicate return event rejected");
        tap(p,KeyEvent.VK_E);closeDialogue(p);check(spirit(p)==20,"Repeat return dialogue cannot repeat reward");
        tick(p,24);santa(p).takeDamage(100);tick(p,1);tap(p,KeyEvent.VK_R);
        check(story.getRestorationQuest().getState()==StoryQuest.State.ACTIVE&&spirit(p)==20,"Retry preserves returned quest and reward");
        travelGate(p,Area.Type.FOREST);FinalChapterTest.collectMaterials(p);FinalChapterTest.restoreVillage(p);receiveBlackFrostClue(p);
        check(story.isCrystalQuestActive()&&allowed(p,"crystal-1"),"Restoration and actual clue dialogue activate guardians");
        check(!story.recordCrystalDestroyed(null)&&!story.recordCrystalDestroyed("unknown"),"Invalid crystal IDs rejected");
        check(story.recordCrystalDestroyed("crystal-1")&&!story.recordCrystalDestroyed("crystal-1"),"Unique crystal advances once");
        check(story.recordCrystalDestroyed("crystal-2")&&!story.isChristmasTownLeadUnlocked(),"Two crystals keep road closed");
        check(!story.acceptEliasQuest()&&!story.reachSpiritTree(),"Save Christmas cannot skip third crystal");
        check(story.recordCrystalDestroyed("crystal-3")&&!story.isChristmasTownLeadUnlocked(),"Third crystal reveals Fang, not Town access");
        check(story.recordFrostFangDefeated()&&story.isChristmasTownLeadUnlocked(),"Fang victory opens Town");
        check(!allowed(p,"boss")&&!story.recordBossDefeated()&&!story.triggerEnding(),"Town access does not activate boss or ending");
        check(story.acceptEliasQuest()&&!story.acceptEliasQuest(),"Elias accepts once");
        check(!allowed(p,"boss"),"Final town encounter and tree still required");
        check(story.reachSpiritTree()&&!story.reachSpiritTree(),"Tree event advances once");
        check(!allowed(p,"boss"),"Tree event alone cannot bypass final town encounter");
        check(story.recordBossDefeated()&&!story.recordBossDefeated(),"Boss story event advances once");
        check(story.triggerEnding()&&!story.triggerEnding(),"Ending story event triggers once");
        check(story.getQuests().stream().allMatch(q->q.getState()==StoryQuest.State.COMPLETED),"All story quests complete in order");
        System.out.println("PASS: "+checks+" V2.1 story/persistence assertions; full authored combat route covered by WorldMapIntegrationTest");
    }
}
