import game.*;
import game.VillageActivities.Site;
import ui.*;
import java.awt.event.KeyEvent;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

/** Actual collection/gate/dialogue/modal input and session lifecycle, without quest shortcuts. */
public class VillageIntegrationTest extends VillageActivitiesTest {
    static final Path CAPTURES=Path.of("build/village-integration/captures");
    static void shot(GamePanel p,String name)throws Exception{ImageIO.write(render(p),"png",CAPTURES.resolve(name+".png").toFile());}
    public static void main(String[]args)throws Exception{
        Files.createDirectories(CAPTURES);entity.VillageResident.prepareAsync().get(30,TimeUnit.SECONDS);VillageActivityVisuals.prepareAsync().get(30,TimeUnit.SECONDS);SantaBaseVisuals.prepareAsync().get(30,TimeUnit.SECONDS);
        for(Class<?> owner:new Class<?>[]{entity.VillageResident.class,VillageActivityVisuals.class}){
            var ready=owner.getDeclaredMethod("ready",java.util.concurrent.CompletableFuture.class);ready.setAccessible(true);
            var pending=new java.util.concurrent.CompletableFuture<>();long start=System.nanoTime();check(ready.invoke(null,pending)==null,"Pending art uses immediate native fallback");check(System.nanoTime()-start<500_000_000L,"No wait in actual cache-read function");
            pending.completeExceptionally(new java.io.IOException("fixture"));check(ready.invoke(null,pending)==null,"Failed art cannot break rendering");var canceled=new java.util.concurrent.CompletableFuture<>();canceled.cancel(true);check(ready.invoke(null,canceled)==null,"Canceled art cannot break rendering");
        }
        GamePanel p=SantaBaseNavigationTest.opening();var model=activities(p);var quest=story(p);
        SantaBaseStoryInteractionTest.approach(p,WorkshopInteractions.Site.GIFT_STORAGE);shot(p,"01-base-presents-before");
        check(!model.presentsCollected(),"Start never grants delivery presents");
        tap(p,KeyEvent.VK_E);check(model.presentsCollected()&&!window(p).isOpen(),"One E collects three presents at the actual Gift Storage front");shot(p,"02-storage-collection");
        int y=santa(p).getY();tap(p,KeyEvent.VK_S);check(santa(p).getY()>y,"Collection keeps movement responsive away from the shelf without a modal");shot(p,"03-base-presents-taken");
        check(model.completionEvents()==0&&model.deliveries()==0,"Collection has no completion reward or automatic delivery");
        tap(p,KeyEvent.VK_E);check(!((DialogueManager)field(p,"dialogueManager")).isActive(),"Repeated Gift Storage remains non-modal");closeDialogue(p);
        check(model.completionEvents()==0,"Repeated collection cannot grant another completion");
        travelGate(p,Area.Type.VILLAGE);check(story(p)==quest&&activities(p)==model,"Departure retains exact session owners");
        check(santa(p).getX()==576&&santa(p).getY()==699,"Original Village spawn remains exact");
        check(areas(p).getCurrentExitZones().stream().noneMatch(g->g.getDestination()==Area.Type.SANTA_BASE),"Opening is permanently one-way after actual collection");
        check(model.presentsCollected()&&!quest.isStoryStarted()&&!quest.isVillageCleared(),"Departure grants no main progression");
        for(Site site:new Site[]{Site.MARTHA,Site.OLIVER,Site.NOAH,Site.IRIS,Site.CLARA,Site.BEN}){
            near(p,site);shot(p,"npc-"+site.name().toLowerCase()+"-world");tap(p,KeyEvent.VK_E);if(((GiftDeliveryPresentation)field(p,"giftDelivery")).isActive())tick(p,GiftDeliveryPresentation.DURATION);shot(p,"npc-"+site.name().toLowerCase()+"-dialogue");closeDialogue(p);if(window(p).isOpen())close(p);
            check(entity.VillageResident.portrait(site.name())!=null,"Matching cached portrait exists: "+site);
        }
        for(var line:model.conversation(Site.NOAH))check(!line.getText().toLowerCase().contains("heater")&&!line.getText().contains("pipes"),"Noah has only revised story dialogue");
        near(p,Site.CAMPFIRE);shot(p,"04-campfire-off");chapter(p).unlock(); // Explicit isolated post-story presentation fixture.
        tap(p,KeyEvent.VK_E);shot(p,"05-campfire-on");
        check(model.campfire()&&!window(p).isOpen()&&!((DialogueManager)field(p,"dialogueManager")).isActive(),"Campfire lights immediately without a puzzle or dialogue");
        int events=model.completionEvents();tap(p,KeyEvent.VK_E);check(events==model.completionEvents(),"Campfire is a single completion");
        check(model.deliveries()==3,"Resident review used actual E deliveries for all three recipients");
        int deliveredBefore=model.deliveries();near(p,Site.IRIS);tap(p,KeyEvent.VK_E);shot(p,"06-recipient-completion-dialogue");
        check(!window(p).isOpen(),"Repeat recipient uses dialogue without confirmation");closeDialogue(p);
        check(model.deliveries()==deliveredBefore,"Repeat interaction never grants a second delivery");
        story(p).startAfterDialogue(p.getChildren().get(0));story(p).recordVillageCleared();travelGate(p,Area.Type.FOREST);travelGate(p,Area.Type.VILLAGE);
        check(model.delivered(0)&&model.campfire(),"Delivery and fire persist across retained Forest reverse gates");
        var gate=new WorldMap.Point(400,720);santa(p).setPosition(gate.x()+100,gate.y()+40);call(p,"updateCamera");shot(p,"08-village-return-gate-removed");
        for(int[] size:new int[][]{{800,600},{1024,768},{1270,720},{1600,900}}){
            p.setSize(size[0],size[1]);near(p,Site.OLIVER);tap(p,KeyEvent.VK_E);shot(p,"dialogue-"+size[0]+"x"+size[1]);closeDialogue(p);shot(p,"card-"+size[0]+"x"+size[1]);
            var rect=VillageActivityUI.windowBounds(size[0],size[1]);check(rect.x>=0&&rect.y>=0&&rect.getMaxX()<=size[0]&&rect.getMaxY()<=size[1],"Modal geometry fits requested viewport");close(p);
            int noticeWidth=Math.min(620,Math.max(320,size[0]-300));var notice=new java.awt.Rectangle((size[0]-noticeWidth)/2,VillageActivityVisuals.notificationY(size[0],150),noticeWidth,54);
            check(!notice.intersects(VillageActivityVisuals.trackerBounds(size[0],size[1])),"Optional completion notification clears activity tracker at requested viewport");
            near(p,Site.LANTERN_EAST);tap(p,KeyEvent.VK_E);shot(p,"requirements-"+size[0]+"x"+size[1]);close(p);
            near(p,Site.CAMPFIRE);shot(p,"tracker-"+size[0]+"x"+size[1]);
        }
        check(quest.isStoryStarted()&&quest.isVillageCleared()&&quest.getCrystalsDestroyed()==0,"Delivery screens cannot bypass Forest crystal progression");
        santa(p).takeDamage(100);tick(p,1);tap(p,KeyEvent.VK_R);check(activities(p)==model&&model.delivered(0)&&model.campfire(),"Retry preserves optional session progress");
        p.beginAdventure();tap(p,KeyEvent.VK_E);check(!activities(p).presentsCollected()&&!activities(p).campfire()&&activities(p).deliveries()==0,"Fresh Start clears every optional flag");
        check(!areas(p).isNextAreaAvailable(),"Base cannot be skipped without presents");collectOpeningPresents(p);travelGate(p,Area.Type.VILLAGE);talk(p,Site.OLIVER);check(activities(p).presentsCollected(),"Oliver retains actual collected presents and creates nothing");shot(p,"09-oliver-without-gifts");close(p);
        for(var line:activities(p).conversation(Site.OLIVER))if(line.getText().contains("Gift Storage"))check(line.getText().contains("Santa's Base"),"Oliver explains actual collection destination");
        talk(p,Site.IRIS);check(activities(p).delivered(0)&&!window(p).isOpen()&&!new VillageActivities().deliver(0,Site.IRIS),"Direct recipient interaction requires actual collection; model rejects missing presents");
        completeOpeningDeliveries(p);at(santa(p),WorldMap.forArea(Area.Type.VILLAGE).point("npc/Toby"));tap(p,KeyEvent.VK_E);closeDialogue(p);check(story(p).isStoryStarted(),"Skipping all activities preserves Toby's quest");
        at(santa(p),encounters(p).get("village").getCenter());tick(p,1);clearWave(p);travelGate(p,Area.Type.FOREST);check(areas(p).getCurrentArea().getType()==Area.Type.FOREST,"Forest gate still depends only on the original encounter");
        p.stopGame();System.out.println("PASS: "+checks+" revised collection/travel/input/NPC/campfire/progression assertions");
    }
}
