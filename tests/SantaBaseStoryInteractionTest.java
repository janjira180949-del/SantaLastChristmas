import game.*;
import ui.*;
import spirit.SpiritTree;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.util.List;

/** Optional narrative through real E/movement input, without quest or reward side effects. */
public class SantaBaseStoryInteractionTest extends V2TestSupport {
    static DialogueManager dialogues(GamePanel p)throws Exception {return (DialogueManager)field(p,"dialogueManager");}
    static GamePanel opening()throws Exception {return SantaBaseNavigationTest.opening();}
    static void approach(GamePanel p,WorkshopInteractions.Site site)throws Exception {
        var target=WorkshopInteractions.approach(site);santa(p).setPosition(target.x()-16,target.y()+4);tick(p,1);
        check(!util.Collision.intersectsAny(santa(p).getBounds(),WorldMap.forArea(Area.Type.SANTA_BASE).solids()),"Actual furniture approach is safe: "+site);
    }
    public static void main(String[]args)throws Exception {
        GamePanel p=opening();var story=story(p);var toby=p.getChildren().get(0);var childState=toby.getStoryState();
        check(areas(p).getCurrentArea().getType()==Area.Type.SANTA_BASE,"Real Start still enters Base");
        check(!((StoryOverlay)field(p,"storyOverlay")).isActive(),"Existing one-card E skip gives control");
        check(call(p,"nearbyWorkshopSite")==null,"No interaction at central spawn");
        int spirit=((SpiritTree)field(p,"spiritTree")).getSpirit();
        for(var site:WorkshopInteractions.Site.values()) {
            if(site==WorkshopInteractions.Site.GIFT_STORAGE)continue; // Collection is tested through the new real storage interaction.
            var target=WorkshopInteractions.approach(site);var marker=WorldMap.forArea(Area.Type.SANTA_BASE).point("landmarks/"+site.landmark());
            var solid=WorldMap.forArea(Area.Type.SANTA_BASE).solids().stream().filter(s->s.contains(marker.x(),marker.y())).findFirst().orElseThrow();
            check(target.x()>=solid.getMinX()+16&&target.x()<=solid.getMaxX()-16&&target.y()==solid.getMaxY()+4,"Approach derived from actual marker and solid: "+site);
            approach(p,site);check(call(p,"nearbyWorkshopSite")==site,"Only nearby object gets prompt: "+site);
            double x=santa(p).getX(),y=santa(p).getY();
            p.keyPressed(new KeyEvent(p,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_W,'w'));
            p.keyPressed(new KeyEvent(p,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_E,'e'));tick(p,1);
            check(dialogues(p).isActive()&&dialogues(p).getActiveDialogue().getLineCount()==site.lines().size(),"Existing dialogue opens exact pages: "+site);
            check(call(p,"nearbyWorkshopSite")==null&&!p.isGameplayHudVisible(),"Prompt/HUD disappear in modal dialogue");
            for(int i=0;i<5;i++){p.keyPressed(new KeyEvent(p,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_E,'e'));tick(p,1);}
            check(dialogues(p).getActiveDialogue().getCurrentLineNumber()==1,"Held E cannot skip multiple pages");
            p.keyReleased(new KeyEvent(p,KeyEvent.KEY_RELEASED,0,0,KeyEvent.VK_E,'e'));
            p.keyReleased(new KeyEvent(p,KeyEvent.KEY_RELEASED,0,0,KeyEvent.VK_W,'w'));
            check(santa(p).getX()==x&&santa(p).getY()==y,"Movement freezes and queued movement clears");
            check(dialogues(p).getActiveDialogue().getChild()==null&&!dialogues(p).isEliasDialogueActive()&&!dialogues(p).isGiftReturnDialogueActive(),"Narrative cannot take an NPC reward path");
            for(int page=0;page<site.lines().size();page++) {
                check(dialogues(p).getActiveDialogue().getCurrentLine().getText().equals(site.lines().get(page).getText()),"Correct page order: "+site+" "+page);
                tap(p,KeyEvent.VK_SPACE);tap(p,KeyEvent.VK_1);attack(p,x+150,y);tick(p,4);
                check(dialogues(p).getActiveDialogue().getCurrentLineNumber()==page+1&&!santa(p).isAttacking(),"Combat input cannot act or advance narrative");
                tap(p,KeyEvent.VK_E);
            }
            check(!dialogues(p).isActive()&&p.isGameplayHudVisible(),"Final E closes and restores HUD");
            tick(p,2);check(santa(p).getX()==x&&santa(p).getY()==y,"No buffered movement/combat leaks after close");
            tap(p,KeyEvent.VK_E);check(dialogues(p).isActive()&&dialogues(p).getActiveDialogue().getCurrentLineNumber()==1,"Optional description may be reread from page one");closeDialogue(p);
            check(story(p)==story&&!story.isStoryStarted()&&!story.isGiftFound()&&!story.isVillageCleared()&&story.getCrystalsDestroyed()==0,"Narrative never advances existing quests");
            check(((SpiritTree)field(p,"spiritTree")).getSpirit()==spirit&&toby.getStoryState()==childState,"Rereading gives no Spirit reward or Toby state change");
        }
        var letter=WorkshopInteractions.Site.LETTER_DESK.lines();
        check(letter.size()==4&&letter.get(0).getText().equals("Dear Santa,\nChristmas is almost here!\nBut something strange is happening\nin our village.")&&letter.get(3).getSpeakerName().equals("Santa"),"Delivery invitation keeps four-page structure with the approved invitation");
        var route=WorkshopInteractions.Site.JOURNEY_MAP.lines().get(0).getText();
        check(route.equals("THE LAST VILLAGE\n\u2193\nFROZEN FOREST\n\u2193\nCHRISTMAS TOWN"),"Route shows only three established destinations");
        var target=WorkshopInteractions.approach(WorkshopInteractions.Site.LETTER_DESK);
        santa(p).setPosition(target.x()-16,target.y()+59);check(WorkshopInteractions.nearby(santa(p))==WorkshopInteractions.Site.LETTER_DESK,"Exact 80-pixel boundary works");
        santa(p).setPosition(target.x()-16,target.y()+60);check(WorkshopInteractions.nearby(santa(p))==null,"Beyond range hides prompt");tap(p,KeyEvent.VK_E);check(!dialogues(p).isActive(),"Out-of-range E cannot open interaction");
        santa(p).setPosition(996,398);check(!util.Collision.intersectsAny(santa(p).getBounds(),WorldMap.forArea(Area.Type.SANTA_BASE).solids()),"Blocked Lantern fixture is a valid side gap");
        var lantern=WorkshopInteractions.approach(WorkshopInteractions.Site.SPIRIT_LANTERN);
        check(Math.hypot(santa(p).getX()+16-lantern.x(),santa(p).getY()+21-lantern.y())<80,"Blocked example is inside proximity radius");
        check(WorkshopInteractions.nearby(santa(p))==null,"Actual solid pole blocks line of sight to front face");tap(p,KeyEvent.VK_E);check(!dialogues(p).isActive(),"E cannot interact through solid furniture");
        santa(p).setPosition(384,731);check(WorkshopInteractions.nearby(santa(p))==null,"Invalid actor inside furniture is rejected");
        approach(p,WorkshopInteractions.Site.SPIRIT_LANTERN);tap(p,KeyEvent.VK_E);santa(p).takeDamage(100);tick(p,1);tap(p,KeyEvent.VK_R);
        check(!dialogues(p).isActive()&&areas(p).getCurrentArea().getType()==Area.Type.SANTA_BASE&&santa(p).getX()==800&&santa(p).getY()==731,"Base retry clears interrupted narrative and keeps checkpoint");
        p.beginAdventure();closeOverlay(p);check(call(p,"nearbyWorkshopSite")==null&&!dialogues(p).isActive()&&!story(p).isStoryStarted(),"Fresh Start clears narrative state");
        travelGate(p,Area.Type.VILLAGE);check(santa(p).getX()==576&&santa(p).getY()==699&&!story(p).isStoryStarted(),"Skipping every optional interaction still departs to exact original Village spawn");
        completeOpeningDeliveries(p);at(santa(p),WorldMap.forArea(Area.Type.VILLAGE).point("npc/Toby"));tap(p,KeyEvent.VK_E);closeDialogue(p);
        check(story(p).isStoryStarted()&&story(p).getActiveQuest().getId()==quest.StoryQuest.Id.LOST_GIFT,"Toby alone starts original quest");p.stopGame();
        // All four reads also preserve real Village/Forest progression and single original rewards.
        p=opening();for(var site:WorkshopInteractions.Site.values()){approach(p,site);tap(p,KeyEvent.VK_E);closeDialogue(p);if(VillageActivitiesTest.window(p).isOpen())VillageActivitiesTest.close(p);}travelGate(p,Area.Type.VILLAGE);
        completeOpeningDeliveries(p);at(santa(p),WorldMap.forArea(Area.Type.VILLAGE).point("npc/Toby"));tap(p,KeyEvent.VK_E);closeDialogue(p);
        at(santa(p),encounters(p).get("village").getCenter());tick(p,1);clearWave(p);travelGate(p,Area.Type.FOREST);
        at(santa(p),WorldMap.forArea(Area.Type.FOREST).point("items/Toby Gift"));tap(p,KeyEvent.VK_E);returnGift(p);
        check(story(p).isLostGiftComplete()&&!story(p).isCrystalQuestActive()&&story(p).getRestorationQuest().getState()==quest.StoryQuest.State.ACTIVE&&((SpiritTree)field(p,"spiritTree")).getSpirit()==20,"All optional reads retain original gift reward/progression");p.stopGame();
        System.out.println("PASS: "+checks+" optional workshop narrative/input/quest/gate/retry assertions");
    }
}
