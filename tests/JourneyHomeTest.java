import game.*;
import game.FinalChapter.Restoration;
import game.VillageActivities.Site;
import ui.*;
import java.awt.*;
import java.awt.event.*;

/** Real E, gate, mouse, keyboard and presentation lifecycle for the redesigned adventure. */
public class JourneyHomeTest extends FinalChapterTest {
    static final int[][] SIZES={{800,600},{1024,768},{1270,720},{1600,900}};
    static EndingCutscene ending(GamePanel p)throws Exception{return (EndingCutscene)field(p,"endingCutscene");}
    static void mouse(GamePanel p,int x,int y)throws Exception{
        var event=new MouseEvent(p,MouseEvent.MOUSE_PRESSED,0,0,x,y,1,false,MouseEvent.BUTTON1);
        for(var listener:p.getMouseListeners())listener.mousePressed(event);tick(p,1);
    }
    static void modalClick(GamePanel p,int x,int y)throws Exception{Point target=window(p).screenPoint(x,y,p.getWidth(),p.getHeight());mouse(p,target.x,target.y);}
    static void completeCelebration(GamePanel p)throws Exception{
        check(((SpiritTreeTransformation)field(p,"treeTransformation")).isComplete(),"Celebration requires the completed original restoration");
        if(ending(p).isCelebration()){
            tap(p,KeyEvent.VK_E);if(ending(p).isCelebration())tap(p,KeyEvent.VK_E);if(ending(p).isCelebration())tap(p,KeyEvent.VK_E);
        }else if(ending(p).hasStarted())tap(p,KeyEvent.VK_ESCAPE);
        check(chapter(p).celebrated()&&!ending(p).hasStarted()&&p.getState()==GamePanel.State.PLAYING,"Gathering returns control exactly once");
    }
    static void takeSleigh(GamePanel p)throws Exception{
        completeCelebration(p);at(santa(p),JourneyHomeVisuals.SLEIGH_APPROACH);tap(p,KeyEvent.VK_E);
        check(window(p).mode()==VillageActivityUI.Mode.SLEIGH,"Actual E at the safe sleigh approach opens confirmation");
        modalClick(p,180,427);check(ending(p).getState()==EndingCutscene.State.SLEIGH_DEPARTURE&&!chapter(p).homeReturned(),"Confirmation starts departure, not credits or teleport");
        tick(p,EndingCutscene.SLEIGH_DURATION);check(areas(p).getCurrentArea().getType()==Area.Type.SANTA_BASE&&chapter(p).homeReturned()&&p.isGameplayHudVisible(),"Departure completes through the production area loader");
        check(santa(p).getX()==800&&santa(p).getY()==731&&!util.Collision.intersectsAny(santa(p).getBounds(),WorldMap.forArea(Area.Type.SANTA_BASE).solids()),"Home arrival has full-actor clearance at the original spawn");
        check(!ending(p).hasStarted()&&!((StoryOverlay)field(p,"storyOverlay")).isActive()&&!areas(p).isNextAreaAvailable(),"Home arrival neither starts credits/intro nor reopens departure");
    }
    static void openLetters(GamePanel p)throws Exception{
        SantaBaseStoryInteractionTest.approach(p,WorkshopInteractions.Site.LETTER_DESK);tap(p,KeyEvent.VK_E);
        check(window(p).mode()==VillageActivityUI.Mode.LETTERS&&window(p).letters().size()==9,"Actual Letter Desk opens nine contributor letters");
    }
    static void finishHomecoming(GamePanel p)throws Exception{
        if(!chapter(p).homeReturned())takeSleigh(p);
        if(ending(p).hasStarted())tap(p,KeyEvent.VK_ESCAPE);
        if(!window(p).isOpen())openLetters(p);
        int guard=12;while(window(p).mode()==VillageActivityUI.Mode.LETTERS&&guard-->0)tap(p,KeyEvent.VK_E);
        check(chapter(p).letterFinished()&&ending(p).getState()==EndingCutscene.State.HOME_FADE,"Only final Finish Reading enters the workshop fade");
    }
    static void progression()throws Exception{
        GamePanel p=SantaBaseNavigationTest.opening();var model=activities(p);var quest=story(p);
        check(areas(p).getCurrentArea().getType()==Area.Type.SANTA_BASE&&!areas(p).isNextAreaAvailable(),"Fresh Start has a locked present prerequisite");
        at(santa(p),WorldMap.forArea(Area.Type.SANTA_BASE).point("exit/to_village"));tick(p,1);
        check(areas(p).getCurrentArea().getType()==Area.Type.SANTA_BASE&&((String)field(p,"areaMessage")).contains("Don't forget"),"Actual automatic gate cannot depart without collection");
        check(!quest.isStoryStarted()&&!chapter(p).isUnlocked(),"Locked departure cannot award or start a quest");
        collectOpeningPresents(p);travelGate(p,Area.Type.VILLAGE);
        check(activities(p)==model&&story(p)==quest&&model.presentsCollected()&&model.deliveries()==0,"Actual collection/departure preserves original owners; delivery optional");
        check(!WorldMap.forArea(Area.Type.VILLAGE).points().containsKey("exit/to_santa_base")&&areas(p).getCurrentExitZones().stream().noneMatch(g->g.getDestination()==Area.Type.SANTA_BASE),"Obsolete return gate is absent in metadata and runtime");
        at(santa(p),new WorldMap.Point(400,720));tick(p,60);check(areas(p).getCurrentArea().getType()==Area.Type.VILLAGE,"Old arch position has no invisible transition");
        completeOpeningDeliveries(p);var toby=p.getChildren().get(0);at(santa(p),new WorldMap.Point(toby.getX()+15,toby.getY()+21));tap(p,KeyEvent.VK_E);closeDialogue(p);
        at(santa(p),encounters(p).get("village").getCenter());tick(p,1);clearWave(p);travelGate(p,Area.Type.FOREST);
        check(!chapter(p).isUnlocked()&&!story(p).isCrystalQuestActive(),"First Forest visit holds only Lost Gift");
        at(santa(p),WorldMap.forArea(Area.Type.FOREST).point("items/Toby Gift"));tap(p,KeyEvent.VK_E);returnGift(p);
        check(chapter(p).isUnlocked()&&!story(p).isBossDefeated()&&!((SpiritTreeTransformation)field(p,"treeTransformation")).isComplete(),"Gift return unlocks materials before boss/tree");
        collectMaterials(p);restoreVillage(p);
        check(chapter(p).isComplete()&&model.deliveries()==3&&!areas(p).getAreas().get(2).isUnlocked(),"Four restorations do not bypass Toby's new clue or crystals");
        receiveBlackFrostClue(p);travelGate(p,Area.Type.FOREST);check(!areas(p).isConnectionAvailable(Area.Type.CHRISTMAS_TOWN),"Town is still blocked with no crystals");
        for(int i=1;i<=3;i++){story(p).recordCrystalDestroyed("crystal-"+i);check(areas(p).isConnectionAvailable(Area.Type.CHRISTMAS_TOWN)==false,"Crystals cannot bypass Frost Fang: "+i);}
        check(story(p).recordFrostFangDefeated()&&areas(p).isConnectionAvailable(Area.Type.CHRISTMAS_TOWN),"Fang victory retains original gate lifecycle");
        travelGate(p,Area.Type.CHRISTMAS_TOWN);check(chapter(p).hud(Area.Type.FOREST).objective().equals("Continue to Christmas Town"),"Completed restoration gives only the onward main objective");
        var counts=java.util.Arrays.stream(FinalChapter.Material.values()).map(chapter(p)::count).toList();tap(p,KeyEvent.VK_C);check(!ending(p).hasStarted(),"C cannot bypass boss, sleigh or letters");
        travelGate(p,Area.Type.FOREST);travelGate(p,Area.Type.VILLAGE);check(counts.equals(java.util.Arrays.stream(FinalChapter.Material.values()).map(chapter(p)::count).toList()),"Return travel preserves all collected totals and flags");
        tick(p,24);santa(p).takeDamage(100);tick(p,1);tap(p,KeyEvent.VK_R);check(activities(p)==model&&chapter(p).isComplete(),"Retry retains completed restoration");
        p.beginAdventure();check(!activities(p).presentsCollected()&&!chapter(p).isUnlocked()&&!chapter(p).isComplete()&&chapter(p).trackerCollapsed(),"Fresh Start resets progress and compact UI only at session boundary");p.stopGame();
        // A separate crystal-complete but unrestored model cannot enter Town either.
        var story=new quest.StoryQuestManager();var activities=new VillageActivities();var manager=new AreaManager(story,activities);
        activities.collectPresents();manager.tryTravel(Area.Type.VILLAGE);story.startAfterDialogue(new entity.Child("Toby",entity.Child.Kind.TOY,0,0,Color.WHITE));story.recordVillageCleared();
        manager.tryTravel(Area.Type.FOREST); // Main story still owns the original Lost Gift prerequisite.
        check(!manager.isConnectionAvailable(Area.Type.CHRISTMAS_TOWN),"Town cannot open from an unqualified activity-only session");
    }
    static void tracker()throws Exception{
        GamePanel p=postStory();var progress=chapter(p);check(progress.trackerCollapsed(),"Restoration starts compact");
        for(int[] size:SIZES){p.setSize(size[0],size[1]);var view=(QuestGuidance.Objective)call(p,"currentObjective");var box=HUD.questBounds(view,progress,size[0],size[1]);check(box.height<150&&box.getMaxX()<=size[0],"Collapsed title/progress is compact at "+size[0]);
            var toggle=HUD.questToggleBounds(view,progress,size[0],size[1]);int attacks=santa(p).getAttackSequence();mouse(p,toggle.x+11,toggle.y+11);
            check(!progress.trackerCollapsed()&&HUD.questBounds(view,progress,size[0],size[1]).height>box.height&&santa(p).getAttackSequence()==attacks,"Scaled header click expands without attacking");
            mouse(p,box.x+20,box.y+60);check(santa(p).getAttackSequence()==attacks,"Tracker body click is consumed");toggle=HUD.questToggleBounds(view,progress,size[0],size[1]);mouse(p,toggle.x+11,toggle.y+11);check(progress.trackerCollapsed(),"Header collapses without changing quest data");
        }
        progress.toggleTracker();travelGate(p,Area.Type.VILLAGE);travelGate(p,Area.Type.FOREST);check(!progress.trackerCollapsed(),"Expansion persists across actual area travel");
        collectMaterials(p);travelGate(p,Area.Type.VILLAGE);near(p,Site.STREETLIGHTS);tap(p,KeyEvent.VK_E);
        check(progress.activeObjectives().size()==3&&!progress.activeObjectives().contains(Restoration.STREETLIGHTS)&&progress.count(FinalChapter.Material.SPIRIT_FRAGMENT)==3,"Completed objective disappears without deleting collected totals");
        check(((QuestGuidance.Objective)call(p,"currentObjective")).details().size()==3,"Expanded list removes completed objective and now irrelevant fragment row");
        restoreVillage(p);check(!progress.trackerVisible()&&HUD.restorationBounds(progress,1270,720).isEmpty()&&progress.completed()==4,"Four completions hide the entire restoration tracker without deleting flags");
        p.beginAdventure();check(chapter(p).trackerCollapsed()&&!chapter(p).trackerVisible(),"Fresh Start resets expanded UI state");p.stopGame();
    }
    static void endingFlow()throws Exception{
        GamePanel p=endingFixture();tick(p,179);check(((SpiritTreeTransformation)field(p,"treeTransformation")).isActive()&&!chapter(p).celebrationStarted(),"Restoration still needs exactly 180 updates");tick(p,1);
        check(ending(p).isCelebration()&&chapter(p).celebrationStarted()&&!chapter(p).celebrated(),"Completion automatically triggers one gathering, not materials or credits");
        tick(p,240);check(ending(p).getState()==EndingCutscene.State.CELEBRATION_DIALOGUE,"Bounded scripted gathering reaches gratitude dialogue");
        check(ending(p).getCinematicDialogue().getCurrentLine().getSpeakerName().equals("Elias"),"Existing Town Keeper leads gratitude");completeCelebration(p);
        at(santa(p),JourneyHomeVisuals.SLEIGH_APPROACH);tap(p,KeyEvent.VK_E);int attacks=santa(p).getAttackSequence(),x=santa(p).getX(),y=santa(p).getY();
        for(int key:new int[]{KeyEvent.VK_W,KeyEvent.VK_SPACE,KeyEvent.VK_1})tap(p,key);tick(p,5);check(santa(p).getX()==x&&santa(p).getY()==y&&santa(p).getAttackSequence()==attacks&&!santa(p).isDashing(),"Confirmation freezes movement/combat/abilities");
        tap(p,KeyEvent.VK_ESCAPE);check(!window(p).isOpen()&&!chapter(p).homeReturned()&&p.isGameplayHudVisible(),"ESC cancels without advancing ending");
        tap(p,KeyEvent.VK_E);modalClick(p,680,427);check(!window(p).isOpen()&&!chapter(p).homeReturned(),"Stay resumes Town");
        takeSleigh(p);at(santa(p),WorldMap.forArea(Area.Type.SANTA_BASE).point("exit/to_village"));tick(p,60);check(areas(p).getCurrentArea().getType()==Area.Type.SANTA_BASE&&!ending(p).hasStarted(),"Returned Base cannot bounce to Village or auto-enter credits");
        openLetters(p);check(window(p).letters().stream().map(FinalChapter.Letter::sender).toList().equals(java.util.List.of("Toby","Martha","Oliver","Noah","Iris","Clara","Ben","Elias","Everyone")),"Every named contributor represented");
        check(window(p).letters().get(4).text().contains("Thank you for the candle")&&window(p).letters().get(5).text().contains("Your paper star")&&window(p).letters().get(6).text().contains("your book"),"Normal mandatory delivery route includes all three delivered letters");
        var undelivered=FinalChapter.gratitudeLetters(new VillageActivities());
        check(!undelivered.get(4).text().contains("Thank you for the candle")&&!undelivered.get(5).text().contains("Your paper star")&&!undelivered.get(6).text().contains("your book"),"Undelivered fixture still selects honest conditional variants");
        tap(p,KeyEvent.VK_RIGHT);tap(p,KeyEvent.VK_RIGHT);tap(p,KeyEvent.VK_LEFT);check(window(p).letterIndex()==1,"Keyboard previous/next navigates letters");modalClick(p,180,427);check(window(p).letterIndex()==0,"Mouse previous shares scaled hitbox");
        closeWindow(p);check(!chapter(p).letterFinished()&&!ending(p).hasStarted(),"Early close cannot begin credits");openLetters(p);
        for(int i=0;i<8;i++)tap(p,KeyEvent.VK_RIGHT);check(window(p).letters().get(8).text().equals("Dear Santa,\n\nThank you for bringing Christmas home.\n\nEven when the lights went out,\nyou reminded us that hope was still here.\n\nWith love,\nEveryone."),"Final collective letter is verbatim");
        tap(p,KeyEvent.VK_RIGHT);check(window(p).isOpen()&&!chapter(p).letterFinished(),"Navigation on last letter cannot accidentally finish");modalClick(p,736,427);check(chapter(p).letterFinished()&&ending(p).getState()==EndingCutscene.State.HOME_FADE,"Explicit Finish Reading starts ending");
        tick(p,60);check(ending(p).getState()==EndingCutscene.State.TITLE,"Workshop fade reaches Home for Christmas");tap(p,KeyEvent.VK_E);tap(p,KeyEvent.VK_E);check(ending(p).getState()==EndingCutscene.State.CREDITS,"Existing credits follow final promise");tick(p,2400);check(ending(p).isComplete(),"Timed credits reach THE END");tap(p,KeyEvent.VK_ESCAPE);check(p.getState()==GamePanel.State.MAIN_MENU,"THE END returns to menu");p.stopGame();
        var delivered=new VillageActivities();delivered.collectPresents();for(int i=0;i<3;i++)delivered.deliver(i,new Site[]{Site.IRIS,Site.CLARA,Site.BEN}[i]);var letters=FinalChapter.gratitudeLetters(delivered);
        check(letters.get(4).text().contains("Thank you for the candle")&&letters.get(5).text().contains("Your paper star")&&letters.get(6).text().contains("your book"),"Delivered-letter variants reflect each actual optional flag");
    }
    public static void main(String[]args)throws Exception{progression();tracker();endingFlow();System.out.println("PASS: "+checks+" Journey Home progression, tracker, input, letters and ending assertions");}
}
