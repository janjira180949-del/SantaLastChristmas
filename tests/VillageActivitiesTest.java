import game.*;
import game.VillageActivities.Site;
import ui.*;
import java.awt.event.*;

/** Original optional-delivery/input assertions plus authorized post-story restoration. */
public class VillageActivitiesTest extends FinalChapterTest {
    static void click(GamePanel p,int x,int y)throws Exception{
        var point=window(p).screenPoint(x,y,p.getWidth(),p.getHeight());
        var event=new MouseEvent(p,MouseEvent.MOUSE_PRESSED,0,0,point.x,point.y,1,false,MouseEvent.BUTTON1);
        for(var listener:p.getMouseListeners())listener.mousePressed(event);tick(p,1);
    }
    static void talk(GamePanel p,Site site)throws Exception{near(p,site);tap(p,KeyEvent.VK_E);if(((GiftDeliveryPresentation)field(p,"giftDelivery")).isActive())tick(p,GiftDeliveryPresentation.DURATION);check(((DialogueManager)field(p,"dialogueManager")).isActive(),"Existing resident dialogue opens: "+site);closeDialogue(p);}
    static void close(GamePanel p)throws Exception{if(window(p).isOpen())closeWindow(p);else check(p.getState()==GamePanel.State.PLAYING,"No obsolete modal to close");}
    static void campfire(GamePanel p)throws Exception{
        check(chapter(p).isUnlocked(),"Campfire helper requires post-story chapter fixture");
        talk(p,Site.NOAH);near(p,Site.CAMPFIRE);tap(p,KeyEvent.VK_E);
        check(activities(p).campfire()&&!window(p).isOpen()&&!((DialogueManager)field(p,"dialogueManager")).isActive(),"One E lights the existing campfire without a modal");
        int events=activities(p).completionEvents();tap(p,KeyEvent.VK_E);check(activities(p).completionEvents()==events,"Campfire cannot complete twice");
    }
    static void collect(GamePanel p)throws Exception{
        if(activities(p).presentsCollected())return;
        check(areas(p).getCurrentArea().getType()==Area.Type.SANTA_BASE,"Uncollected presents can only be acquired at Base before departure");
        collectOpeningPresents(p);travelGate(p,Area.Type.VILLAGE);
    }
    static void deliveries(GamePanel p)throws Exception{
        collect(p);talk(p,Site.OLIVER);close(p);
        Site[] recipients={Site.IRIS,Site.CLARA,Site.BEN};
        for(int i=0;i<3;i++){
            int count=activities(p).deliveries();check(!activities(p).deliver((i+1)%3,recipients[i])&&activities(p).deliveries()==count,"Wrong recipient mapping consumes nothing");
            near(p,recipients[i]);tap(p,KeyEvent.VK_E);if(((GiftDeliveryPresentation)field(p,"giftDelivery")).isActive())tick(p,GiftDeliveryPresentation.DURATION);
            check(activities(p).delivered(i)&&!window(p).isOpen()&&((DialogueManager)field(p,"dialogueManager")).isActive(),"One E delivers and shows existing thank-you dialogue");
            closeDialogue(p);check(!activities(p).deliver(i,recipients[i]),"Duplicate delivery cannot award again");
        }
    }
    public static void main(String[]args)throws Exception{
        GamePanel p=playing();var quest=story(p);int spirit=((spirit.SpiritTree)field(p,"spiritTree")).getSpirit();
        talk(p,Site.MARTHA);check(!window(p).isOpen(),"Martha narrative does not open an obsolete lantern introduction");
        near(p,Site.STREETLIGHTS);tap(p,KeyEvent.VK_E);check(window(p).mode()==VillageActivityUI.Mode.RESTORATION,"Locked streetlight uses a material description");close(p);
        check(!new VillageActivities().deliver(0,Site.IRIS),"No delivery before Base collection");deliveries(p);
        check(activities(p).deliveries()==3&&!chapter(p).isUnlocked()&&activities(p).completionEvents()==3,"Deliveries complete independently before restoration");
        check(story(p)==quest&&!quest.isStoryStarted()&&!quest.isVillageCleared()&&!quest.isGiftFound()&&((spirit.SpiritTree)field(p,"spiritTree")).getSpirit()==spirit,"Optional gifts retain original main quest/Spirit independence");
        for(Site site:new Site[]{Site.OLIVER,Site.IRIS,Site.CLARA,Site.BEN})check(activities(p).conversation(site).get(0).getText().contains(site==Site.OLIVER?"Every present":"remembered"),"Recipient completion dialogue remains meaningful");
        at(santa(p),WorldMap.forArea(Area.Type.VILLAGE).point("npc/Toby"));tap(p,KeyEvent.VK_E);closeDialogue(p);check(quest.isStoryStarted(),"Original Toby dialogue starts main quest");
        at(santa(p),encounters(p).get("village").getCenter());tick(p,1);clearWave(p);check(quest.isVillageCleared(),"Original two-Walker encounter opens Forest");
        var progress=activities(p);travelGate(p,Area.Type.FOREST);travelGate(p,Area.Type.VILLAGE);check(activities(p)==progress&&progress.deliveries()==3,"Travel retains delivery state");p.stopGame();
        GamePanel q=postStory();collectMaterials(q);restoreVillage(q);check(chapter(q).isComplete()&&activities(q).hopeful()&&activities(q).deliveries()==3,"Four village restorations create hope without requiring delivery");
        check(activities(q).conversation(Site.MARTHA).get(0).getText().contains("street lights")&&activities(q).conversation(Site.NOAH).stream().anyMatch(line->line.getText().contains("fire gives")),"Martha and story-only Noah react to restoration");
        deliveries(q);check(activities(q).completionEvents()==7,"Four restorations plus three optional deliveries complete once");
        var retained=activities(q);tick(q,24);santa(q).takeDamage(100);tick(q,1);tap(q,KeyEvent.VK_R);check(activities(q)==retained&&retained.hopeful(),"Retry preserves restored village and deliveries");
        q.beginAdventure();check(!activities(q).hopeful()&&!activities(q).presentsCollected()&&!chapter(q).isUnlocked(),"Fresh Start resets both chapter and optional deliveries");q.stopGame();
        System.out.println("PASS: "+checks+" retained Village delivery/NPC/input/progression and post-story restoration assertions");
    }
}
