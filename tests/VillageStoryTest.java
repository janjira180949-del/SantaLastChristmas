import game.*;
import quest.*;
import ui.*;
import java.awt.event.KeyEvent;

/** Actual production input across every delivery order and progression prerequisite. */
public class VillageStoryTest extends V2TestSupport {
    public static void main(String[] args)throws Exception {
        int[][] orders={{0,1,2},{0,2,1},{1,0,2},{1,2,0},{2,0,1},{2,1,0}};
        var recipients=new VillageActivities.Site[]{VillageActivities.Site.IRIS,VillageActivities.Site.CLARA,VillageActivities.Site.BEN};
        for(var order:orders){
            var p=playing();var a=(VillageActivities)field(p,"villageActivities");var q=story(p);
            check(q.getQuests().size()==5&&StoryQuest.Id.CHRISTMAS_DELIVERY.ordinal()==4,"Appended quest preserves old IDs");
            check(q.getActiveQuest()==q.getDeliveryQuest()&&!q.isStoryStarted(),"Locked Lost Gift does not count as started");
            check(!areas(p).tryTravel(Area.Type.FOREST)&&!a.chapter.isUnlocked(),"No early gate or restoration");
            QuestFlowTest.nearToby(p);tap(p,KeyEvent.VK_E);closeDialogue(p);
            check(!q.isStoryStarted()&&!q.canStartFor(p.getChildren().get(0)),"Early Toby reminds without starting Lost Gift");
            check(p.getChildren().get(0).getStoryState()==entity.Child.StoryState.NOT_MET,"Early reminder preserves Toby's original introduction");
            int spirit=((spirit.SpiritTree)field(p,"spiritTree")).getSpirit();
            for(int step=0;step<3;step++){
                var site=recipients[order[step]];santa(p).setPosition(site.x-16,site.y+4);tap(p,KeyEvent.VK_E);
                var visual=(GiftDeliveryPresentation)field(p,"giftDelivery");
                check(a.deliveries()==step+1&&visual.isActive(),"One E commits and starts handover");
                check(field(p,"areaMessage")==null,"An earlier recipient's notice cannot obscure this handover");
                check(!((VillageActivityUI)field(p,"activityWindow")).isOpen(),"No delivery modal");
                int x=santa(p).getX(),y=santa(p).getY();tap(p,KeyEvent.VK_SPACE);tap(p,KeyEvent.VK_1);attack(p,x+80,y);
                check(x==santa(p).getX()&&y==santa(p).getY()&&!santa(p).isAttacking(),"Handover isolates world input");
                tick(p,GiftDeliveryPresentation.DURATION);check(!visual.isActive(),"Handover ends in bounded time");
                check(((DialogueManager)field(p,"dialogueManager")).isActive(),"Existing dialogue follows handover");
                var lines=a.deliveryThanks(site);check(lines.size()==new int[]{5,6,9}[order[step]],"Approved recipient-specific pages and preserved Ben reflection work in any order");
                check(field(p,"areaMessage")==null&&q.getDeliveryQuest().getProgress()==step+1,"No delivery banner; quest progress follows the visual handover");
                if(site==VillageActivities.Site.BEN)check(lines.get(lines.size()-2).getText().startsWith("I came to deliver presents")&&lines.get(lines.size()-1).getText().equals("I should find Toby."),"Ben retains both original closing pages in every delivery order");
                closeDialogue(p);tap(p,KeyEvent.VK_E);closeDialogue(p);
                check(a.deliveries()==step+1&&!a.deliver(order[step],site),"Repeat cannot deliver twice");
                check(q.getDeliveryQuest().getProgress()==a.deliveries(),"Quest observes existing flags");
                if(step<2)check(!q.isDeliveryComplete()&&!q.isStoryStarted(),"First two never unlock Toby");
            }
            check(q.isDeliveryComplete()&&q.getStage()==StoryQuestManager.Stage.MEET_TOBY,"Every order enters Find Toby");
            check(!q.synchronizeDelivery(),"Observed completion cannot repeat");
            check(((spirit.SpiritTree)field(p,"spiritTree")).getSpirit()==spirit,"Deliveries add no Spirit");
            QuestFlowTest.nearToby(p);tap(p,KeyEvent.VK_E);closeDialogue(p);check(q.isStoryStarted(),"Original Toby introduction now starts Lost Gift");
            call(p,"retryEncounter");check(a.deliveries()==3&&q.isDeliveryComplete(),"Retry retains delivery state");
            call(p,"resetSession");check(story(p).getStage()==StoryQuestManager.Stage.DELIVER_PRESENTS,"Fresh start resets prerequisite");
        }
        var p=playing();var a=(VillageActivities)field(p,"villageActivities");var r=VillageActivities.Site.IRIS;
        santa(p).setPosition(r.x-16,r.y+4);tap(p,KeyEvent.VK_E);tap(p,KeyEvent.VK_ESCAPE);closeDialogue(p);
        check(!((GiftDeliveryPresentation)field(p,"giftDelivery")).isActive()&&p.getState()==GamePanel.State.PLAYING,"ESC skips handover without pause");
        r=VillageActivities.Site.CLARA;santa(p).setPosition(r.x-16,r.y+4);tap(p,KeyEvent.VK_E);call(p,"retryEncounter");
        check(a.deliveries()==2&&!((GiftDeliveryPresentation)field(p,"giftDelivery")).isActive(),"Interrupted handover retains committed gift and clears presentation");
        check(a.pollFeedback()==null,"Retry discards the canceled handover notice without deleting its delivery flag");
        a.chapter.unlock();santa(p).setPosition(VillageActivities.Site.CAMPFIRE.x-16,VillageActivities.Site.CAMPFIRE.y+4);tap(p,KeyEvent.VK_E);
        check(((String)field(p,"areaMessage")).startsWith("Campfire lit"),"A subsequent restoration shows its own feedback rather than the interrupted gift");
        System.out.println("PASS: "+checks+" delivery order, input and prerequisite assertions");
    }
}
