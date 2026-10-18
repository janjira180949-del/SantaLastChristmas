import game.*;
import entity.*;
import quest.*;
import spirit.SpiritTree;
import java.awt.event.KeyEvent;
import java.util.*;

/** Whole three-map story through real dialogue, attack, gate and restoration input. */
public class WorldMapIntegrationTest extends V2TestSupport {
    public static void main(String[] args)throws Exception {
        GamePanel p=playing();StoryQuestManager story=story(p);EncounterManager waves=encounters(p);
        check(Area.Type.values().length==4&&areas(p).getAreas().size()==4,"Three original areas plus permanent Santa Base");
        check(p.getChildren().size()==1&&p.getChildren().get(0).getName().equals("Toby"),"Toby is the only child NPC");
        check(story.getQuests().size()==5&&!areas(p).tryTravel(Area.Type.FOREST),"Five sequential quests and initial exit lock");
        completeOpeningDeliveries(p);
        Child toby=p.getChildren().get(0);at(santa(p),new WorldMap.Point(toby.getX()+15,toby.getY()+21));tap(p,KeyEvent.VK_E);
        check(!story.isStoryStarted(),"Quest starts only after conversation closes");closeDialogue(p);check(story.isStoryStarted(),"Lost Gift accepted");
        at(santa(p),waves.get("village").getCenter());tick(p,1);check(enemies(p).size()==2,"Village predefined two Walkers");
        int spawned=enemies(p).size();tick(p,10);check(enemies(p).size()==spawned,"No duplicate activation");
        check(!call(p,"handleExitZone").equals(true),"No unintended transition during combat");
        clearWave(p);check(story.isVillageCleared(),"Intro encounter advances objective");
        travelGate(p,Area.Type.FOREST);at(santa(p),WorldMap.forArea(Area.Type.FOREST).point("items/Toby Gift"));tap(p,KeyEvent.VK_E);
        check(story.isGiftFound()&&!story.isLostGiftComplete()&&!story.isCrystalQuestActive(),"Pickup requires return before crystal quest");
        int spirit=((SpiritTree)field(p,"spiritTree")).getSpirit();tap(p,KeyEvent.VK_E);check(((SpiritTree)field(p,"spiritTree")).getSpirit()==spirit,"No duplicate gift reward");
        returnGift(p);
        check(!story.isCrystalQuestActive()&&story.getRestorationQuest().getState()==StoryQuest.State.ACTIVE&&((SpiritTree)field(p,"spiritTree")).getSpirit()==spirit+20,"Return unlocks restoration and awards Spirit once");
        check(!areas(p).isConnectionAvailable(Area.Type.CHRISTMAS_TOWN),"Restoration assignment cannot bypass Town gate");
        FinalChapterTest.collectMaterials(p);FinalChapterTest.restoreVillage(p);
        check(story.getStage()==StoryQuestManager.Stage.TALK_TO_TOBY&&!story.isCrystalQuestActive(),"Four restorations require Toby's narrative handoff");
        receiveBlackFrostClue(p);travelGate(p,Area.Type.FOREST);
        @SuppressWarnings("unchecked") List<BlackFrostCrystal> crystals=(List<BlackFrostCrystal>)field(p,"crystals");
        for(int i=0;i<3;i++) {
            BlackFrostCrystal c=crystals.get(i);at(santa(p),c.getPosition());
            check(!c.tryBreak(santa(p)),"Dormant core is protected");tick(p,1);check(c.getState()==BlackFrostCrystal.State.GUARDED,"Approach activates guarded state");
            check(enemies(p).isEmpty()&&waves.get(c.getId()).getWarningFrames()==60,"Initial crystal warning before actors");tick(p,60);
            int expected=i==0?2:i==1?3:4;check(enemies(p).size()==expected,"Defined Forest composition "+(i+1));
            santa(p).setAttackDirection(1,0);santa(p).startAttack();check(!c.tryBreak(santa(p)),"Active wave protects core");
            clearWave(p);check(c.getState()==BlackFrostCrystal.State.VULNERABLE,"Wave completion exposes core");
            at(santa(p),new WorldMap.Point(c.getPosition().x(),c.getPosition().y()+48));
            attack(p,c.getPosition().x(),c.getPosition().y());tick(p,23);
            check(c.getState()==BlackFrostCrystal.State.BROKEN&&story.getCrystalsDestroyed()==i+1,"One core break advances quest");
            check(!story.recordCrystalDestroyed(c.getId()),"Duplicate crystal event rejected");
        }
        check(!story.isChristmasTownLeadUnlocked()&&story.isFrostFangAvailable(),"Three cores reveal Fang; Town remains locked");
        at(santa(p),boss.FrostFang.ARENA);tick(p,1);clearWave(p);
        check(story.isFrostFangDefeated()&&!story.isBossDefeated()&&!story.isEndingTriggered(),"Real Frost Fang fight opens only Town");
        travelGate(p,Area.Type.VILLAGE);at(santa(p),waves.get("village").getCenter());tick(p,1);check(enemies(p).isEmpty(),"Cleared village cannot restart on revisit");
        travelGate(p,Area.Type.FOREST);for(var c:crystals)check(c.getState()==BlackFrostCrystal.State.BROKEN,"Broken cores persist on travel");
        check(story.isRestorationComplete(),"All restorations remain completed after investigation and travel");
        check(areas(p).isConnectionAvailable(Area.Type.CHRISTMAS_TOWN),"Original crystals plus all four restorations open Town");
        travelGate(p,Area.Type.CHRISTMAS_TOWN);
        var elias=(Elias)field(p,"elias");at(santa(p),new WorldMap.Point(elias.getX()+16,elias.getY()+24));tap(p,KeyEvent.VK_E);closeDialogue(p);
        check(story.isBossQuestAccepted(),"Elias accepts Save Christmas");
        at(santa(p),waves.get("town").getCenter());tick(p,1);check(enemies(p).isEmpty()&&waves.get("town").getWarningFrames()==60,"Town smoke preparation precedes unchanged wave");tick(p,60);check(enemies(p).size()==3,"Town has two Walkers and one Runner");clearWave(p);
        at(santa(p),(WorldMap.Point)call(p,"spiritTreeInteractionPoint"));tick(p,1);
        check(story.isTreeReached()&&waves.getActive()!=null&&waves.getActive().getId().equals("boss"),"Tree starts final encounter in Town");
        clearWave(p);check(story.isBossDefeated()&&field(p,"christmasEater")==null,"Guardian defeated and freed");
        at(santa(p),(WorldMap.Point)call(p,"spiritTreeInteractionPoint"));tap(p,KeyEvent.VK_E);
        check(p.getState()==GamePanel.State.PLAYING&&story.isEndingTriggered(),"Restoration triggers single ending");
        check(story.getQuests().stream().allMatch(q->q.getState()==StoryQuest.State.COMPLETED),"All four quests completed");
        tick(p,180);check(p.getState()==GamePanel.State.ENDING&&((EndingCutscene)field(p,"endingCutscene")).isCelebration(),"Exactly three-second restoration enters the single celebration");
        FinalChapterTest.finishChapter(p);tap(p,KeyEvent.VK_E);tap(p,KeyEvent.VK_E);tap(p,KeyEvent.VK_E);EndingCutscene cut=(EndingCutscene)field(p,"endingCutscene");
        check(cut.getState()==EndingCutscene.State.CREDITS,"Credits remain accessible after restoration");
        tick(p,EndingCutscene.duration(cut.getState())-cut.getFrames());
        check(cut.isComplete()&&((SpiritTree)field(p,"spiritTree")).getSpirit()==100,"Ending reaches credits and restores 100% Spirit");
        tap(p,KeyEvent.VK_ESCAPE);check(p.getState()==GamePanel.State.MAIN_MENU,"Ending returns to menu");
        System.out.println("PASS: "+checks+" full progression assertions");
    }
}
