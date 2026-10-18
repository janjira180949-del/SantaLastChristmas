import game.*;
import entity.*;
import ui.*;
import java.awt.Color;
import java.awt.event.KeyEvent;

/** Interrupted handoffs, world input outside the HUD, collision lines and delivery retry safety. */
public class QuestFlowSafetyTest extends QuestFlowPresentationTest {
    public static void main(String[]args)throws Exception{
        GamePanel p=playing();var a=activities(p);var story=story(p);
        var iris=VillageActivities.Site.IRIS;santa(p).setPosition(iris.x-16,iris.y+19);tick(p,1);
        check(call(p,"nearbyVillageActivity")==iris&&!p.getChildren().get(0).canInteract(),"Iris is independently reachable and never suppressed by Toby");
        var presentFlag=VillageActivities.class.getDeclaredField("presentsCollected");presentFlag.setAccessible(true);presentFlag.setBoolean(a,false);
        tap(p,KeyEvent.VK_E);check(a.deliveries()==0&&((DialogueManager)field(p,"dialogueManager")).isActive(),"Without collected presents E offers story rather than delivery");closeDialogue(p);
        check(!a.presentsCollected()&&!story.isStoryStarted(),"Recipient conversation cannot grant presents or main progress");presentFlag.setBoolean(a,true);
        tap(p,KeyEvent.VK_E);check(a.delivered(0),"Direct delivery commits before thank-you closes");santa(p).takeDamage(100);tick(p,1);tap(p,KeyEvent.VK_R);
        check(a.delivered(0)&&!((DialogueManager)field(p,"dialogueManager")).isActive()&&p.isGameplayHudVisible(),"Retry retains delivery and clears interrupted thank-you");
        completeOpeningDeliveries(p);nearToby(p);tap(p,KeyEvent.VK_E);closeDialogue(p);story.recordVillageCleared();travelGate(p,Area.Type.FOREST);
        check(objective(p).target().equals(WorldMap.forArea(Area.Type.FOREST).point("items/Toby Gift")),"Lost Gift text and arrow share actual gift target");
        at(santa(p),WorldMap.forArea(Area.Type.FOREST).point("items/Toby Gift"));tap(p,KeyEvent.VK_E);
        check(objective(p).target().equals(WorldMap.forArea(Area.Type.FOREST).point("exit/to_village")),"Carrying gift guides through the actual return gate");
        travelGate(p,Area.Type.VILLAGE);nearToby(p);tap(p,KeyEvent.VK_E);santa(p).takeDamage(100);tick(p,1);tap(p,KeyEvent.VK_R);
        check(!story.isLostGiftComplete()&&!a.chapter.isUnlocked()&&!((DialogueManager)field(p,"dialogueManager")).isActive(),"Interrupted gift return cannot unlock restoration");
        nearToby(p);tap(p,KeyEvent.VK_E);closeDialogue(p);travelGate(p,Area.Type.FOREST);
        check(objective(p).target()!=null&&FinalChapter.PICKUPS.stream().anyMatch(item->item.point().equals(objectiveUnchecked(p).target())),"Restoration guidance selects a real needed material");
        FinalChapterTest.collectMaterials(p);FinalChapterTest.restoreVillage(p);
        nearToby(p);tap(p,KeyEvent.VK_E);santa(p).takeDamage(100);tick(p,1);tap(p,KeyEvent.VK_R);
        check(story.getStage()==quest.StoryQuestManager.Stage.TALK_TO_TOBY&&!story.isCrystalQuestActive(),"Interrupted clue preserves explicit handoff stage");
        check(!((DialogueManager)field(p,"dialogueManager")).isActive(),"Retry releases clue dialogue safely");receiveBlackFrostClue(p);
        var toby=p.getChildren().get(0);var far=new Santa(toby.getX()+100,toby.getY());
        check(!toby.isWithinInteractionRange(far),"Toby's range remains bounded");
        var solids=WorldMap.forArea(Area.Type.VILLAGE).solids();Santa outside=null;Child hidden=null;
        for(var solid:solids){
            if(solid.getMinX()<100||solid.getMinY()<100||solid.getMaxX()>2400||solid.getMaxY()>1800)continue;
            var candidate=new Santa((int)solid.getMinX()-33,(int)solid.getCenterY()-21);
            if(!util.Collision.intersectsAny(candidate.getBounds(),solids)){
                outside=candidate;hidden=new Child("Wall fixture",Child.Kind.TOY,(int)solid.getMinX()+8-15,(int)solid.getCenterY()-21,Color.WHITE);break;
            }
        }
        check(outside!=null&&hidden!=null,"Wall test finds an exterior full-footprint approach in actual Village geometry");
        check(Math.hypot(outside.getX()+16-hidden.getX()-15,outside.getY()+21-hidden.getY()-21)<58&&!hidden.isWithinInteractionRange(outside),"Solid line blocks an otherwise nearby NPC");
        at(santa(p),new WorldMap.Point(1296,976));tick(p,24);int sequence=santa(p).getAttackSequence();mouse(p,650,400);
        check(santa(p).getAttackSequence()==sequence+1,"Clicks outside quest UI retain directional attack");
        var view=objective(p);var bounds=HUD.questBounds(view,a.chapter,p.getWidth(),p.getHeight());tick(p,24);sequence=santa(p).getAttackSequence();mouse(p,bounds.x+20,bounds.y+50);
        check(santa(p).getAttackSequence()==sequence,"Quest body absorbs a ready attack click");
        System.out.println("PASS: "+checks+" interrupted story, collision and input safety assertions");
    }
    private static QuestGuidance.Objective objectiveUnchecked(GamePanel p){try{return objective(p);}catch(Exception e){throw new AssertionError(e);}}
}
