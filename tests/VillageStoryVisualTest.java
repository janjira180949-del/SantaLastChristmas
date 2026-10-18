import game.*;
import ui.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.*;

/** Prepared art states, actual viewport input, finite effects and unchanged navigable geometry. */
public class VillageStoryVisualTest extends V2TestSupport {
    interface Painter {void paint(Graphics2D g);}
    static BufferedImage layer(Painter painter){var im=new BufferedImage(2560,1920,BufferedImage.TYPE_INT_ARGB);var g=im.createGraphics();painter.paint(g);g.dispose();return im;}
    static boolean differs(BufferedImage a,BufferedImage b,Rectangle box){for(int y=box.y;y<box.y+box.height;y++)for(int x=box.x;x<box.x+box.width;x++)if(a.getRGB(x,y)!=b.getRGB(x,y))return true;return false;}
    static void verifyNotice(GamePanel p,BufferedImage visible,BufferedImage hidden,String text,boolean dialogue)throws Exception{
        var g=visible.createGraphics();g.setFont(new Font("Dialog",Font.PLAIN,13));var camera=(Camera)field(p,"camera");
        var actor=new Rectangle(santa(p).getX()-camera.getX()-35,santa(p).getY()-camera.getY()-55,102,110);
        var bounds=ChristmasUI.safeNotificationBounds(g,text,p.getWidth(),p.getHeight(),dialogue,actor);g.dispose();
        check(differs(visible,hidden,bounds),"Actual delivery/restoration notice renders in the illustrated lower lane "+p.getWidth());
        check(new Rectangle(0,0,p.getWidth(),p.getHeight()).contains(bounds)&&!bounds.intersects(AbilityHUD.stripBounds(p.getWidth(),p.getHeight())),"Notice fits viewport and clears skills");
        if(dialogue)check(bounds.getMaxY()<p.getHeight()-204,"Delivery notice clears the approved Iris frame");
        boolean confined=true;for(int y=0;y<visible.getHeight();y++)for(int x=0;x<visible.getWidth();x++)if(visible.getRGB(x,y)!=hidden.getRGB(x,y)&&!bounds.contains(x,y))confined=false;
        check(confined,"Showing a notice changes only its exact artwork/text rectangle, never world/dialogue/other HUD pixels");
    }
    public static void main(String[] args)throws Exception {
        VillageRestorationVisuals.prepareAsync().get(30,TimeUnit.SECONDS);VillageActivityVisuals.prepareAsync().get(30,TimeUnit.SECONDS);
        var chapter=new FinalChapter();chapter.unlock();for(var pickup:FinalChapter.PICKUPS)check(chapter.collect(pickup.id()),"Existing material fixture "+pickup.id());
        var coldHouse=layer(g->VillageRestorationVisuals.drawHouse(g,chapter));var coldTree=layer(g->VillageRestorationVisuals.drawTree(g,chapter));var market=layer(g->VillageRestorationVisuals.drawMarket(g,chapter));
        check(VillageRestorationVisuals.marketState(chapter)==0,"Cold market derives incomplete lights");
        chapter.restore(FinalChapter.Restoration.STREETLIGHTS);var litMarket=layer(g->VillageRestorationVisuals.drawMarket(g,chapter));
        check(VillageRestorationVisuals.marketState(chapter)==1&&differs(market,litMarket,VillageRestorationVisuals.MARKET_BOUNDS),"Lights create distinct intermediate market art");
        chapter.restore(FinalChapter.Restoration.HOUSE);chapter.restore(FinalChapter.Restoration.TREE);
        check(differs(coldHouse,layer(g->VillageRestorationVisuals.drawHouse(g,chapter)),VillageRestorationVisuals.HOUSE_BOUNDS),"Matched actual house art changes");
        check(differs(coldTree,layer(g->VillageRestorationVisuals.drawTree(g,chapter)),VillageRestorationVisuals.TREE_BOUNDS),"Matched actual tree art changes");
        chapter.restore(FinalChapter.Restoration.CAMPFIRE);check(VillageRestorationVisuals.marketState(chapter)==2&&differs(litMarket,layer(g->VillageRestorationVisuals.drawMarket(g,chapter)),VillageRestorationVisuals.MARKET_BOUNDS),"Only existing four flags create festive market");
        var a=new VillageActivities();a.chapter.unlock();a.lightCampfire();var fire=VillageActivities.Site.CAMPFIRE;
        for(int i=0;i<3;i++)for(boolean lit:new boolean[]{false,true}){
            var image=VillageActivityVisuals.lanternImage(i,lit);int transparent=0;
            for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if((image.getRGB(x,y)>>>24)==0)transparent++;
            check(transparent>image.getWidth()*image.getHeight()/5,"Prepared lantern retains an actual transparent background "+i+" "+lit);
            check((image.getRGB(0,0)>>>24)==0&&(image.getRGB(image.getWidth()-1,image.getHeight()-1)>>>24)==0,"Lantern bounds do not create an opaque rectangle");
        }
        check(differs(layer(g->VillageActivityVisuals.drawProps(g,a,0)),layer(g->VillageActivityVisuals.drawProps(g,a,9)),new Rectangle(fire.x-55,fire.y-70,110,100)),"Normal prepared campfire animates, not only fallback");
        var response=new VillageRestorationVisuals();response.respond(FinalChapter.Restoration.TREE,100);
        check(response.responseActive(100)&&response.responseActive(189)&&!response.responseActive(190),"Finite ninety-tick restoration response");response.clearResponse();check(!response.responseActive(110),"Area cleanup clears local effects");
        var method=VillageRestorationVisuals.class.getDeclaredMethod("ready",CompletableFuture.class);method.setAccessible(true);
        var pending=new CompletableFuture<>();long start=System.nanoTime();check(method.invoke(null,pending)==null&&(System.nanoTime()-start)<100_000_000,"Pending preparation reads do not wait");
        var failed=new CompletableFuture<>();failed.completeExceptionally(new IllegalStateException("Fixture"));check(method.invoke(null,failed)==null,"Failed preparation fallback");var canceled=new CompletableFuture<>();canceled.cancel(false);check(method.invoke(null,canceled)==null,"Canceled preparation fallback");
        var p=SantaBaseNavigationTest.opening();SantaBaseStoryInteractionTest.approach(p,WorkshopInteractions.Site.GIFT_STORAGE);tap(p,KeyEvent.VK_E);
        check(((VillageActivities)field(p,"villageActivities")).presentsCollected()&&!((VillageActivityUI)field(p,"activityWindow")).isOpen(),"Actual storage collection opens no modal");int events=((VillageActivities)field(p,"villageActivities")).completionEvents();tap(p,KeyEvent.VK_E);check(events==((VillageActivities)field(p,"villageActivities")).completionEvents(),"Repeated collection gives no reward");
        travelGate(p,Area.Type.VILLAGE);check(!((DialogueManager)field(p,"dialogueManager")).isActive()&&((String)field(p,"areaMessage")).equals("The letter was right...\nThe Christmas lights are fading.\nI should check on everyone."),"Arrival line is non-blocking");
        for(int[] size:new int[][]{{800,600},{1024,768},{1270,720},{1600,900}}){
            p.setSize(size[0],size[1]);var view=(QuestGuidance.Objective)call(p,"currentObjective");var state=((VillageActivities)field(p,"villageActivities")).chapter;
            check(view.title().equals("A Christmas Delivery")&&view.details().size()==3&&view.optional().isEmpty(),"Mandatory recipient list uses main guidance");
            var button=HUD.questToggleBounds(view,state,size[0],size[1]);int attacks=santa(p).getAttackSequence();QuestFlowPresentationTest.mouse(p,button.x+8,button.y+8);
            check(!state.trackerCollapsed()&&santa(p).getAttackSequence()==attacks,"Expand consumes ready attack click");var box=HUD.questBounds(view,state,size[0],size[1]);check(box.y>=114&&box.getMaxY()<size[1],"Expanded incomplete recipients fit viewport");
            button=HUD.questToggleBounds(view,state,size[0],size[1]);QuestFlowPresentationTest.mouse(p,button.x+8,button.y+8);check(state.trackerCollapsed(),"Collapse round trip");
        }
        santa(p).setPosition(VillageActivities.Site.IRIS.x-16,VillageActivities.Site.IRIS.y+4);tap(p,KeyEvent.VK_E);tick(p,GiftDeliveryPresentation.DURATION);
        var message=GamePanel.class.getDeclaredField("areaMessage");message.setAccessible(true);
        for(int[] size:new int[][]{{800,600},{1024,768},{1270,720},{1600,900}}){
            p.setSize(size[0],size[1]);var notice=render(p);Object text=message.get(p);message.set(p,null);var hidden=render(p);message.set(p,text);
            check(text==null,"Successful delivery has no redundant banner at "+size[0]);
            check(!differs(notice,hidden,new Rectangle(0,0,size[0],size[1])),"Silent delivery has no notification pixels or changes to dialogue/world/HUD");
            check(((VillageActivities)field(p,"villageActivities")).delivered(0)&&((quest.StoryQuestManager)field(p,"storyQuestManager")).getDeliveryQuest().getProgress()==1,"Silent Iris delivery retains its transaction and quest progress");
            check(((DialogueManager)field(p,"dialogueManager")).isActive(),"Original delivery dialogue remains active without a banner");
        }
        closeDialogue(p);completeOpeningDeliveries(p);check(((QuestGuidance.Objective)call(p,"currentObjective")).details().isEmpty(),"Completed deliveries disappear from active HUD");
        santa(p).setPosition(1815-16,1296-21);check(call(p,"nearbyVillageActivity")==VillageActivities.Site.NOAH,"Noah has a clear selected prompt");
        santa(p).setPosition(1836-16,1296-21);check(call(p,"nearbyVillageActivity")==VillageActivities.Site.NOAH,"Small movement preserves the selected target in the overlapping ranges");tap(p,KeyEvent.VK_E);
        check(field(p,"activityDialogueSite")==VillageActivities.Site.NOAH&&((DialogueManager)field(p,"dialogueManager")).isActive(),"E acts on the exact displayed Noah target");closeDialogue(p);
        santa(p).setPosition(1860-16,1296-21);check(call(p,"nearbyVillageActivity")==VillageActivities.Site.CAMPFIRE,"Moving toward campfire selects the separate object");tap(p,KeyEvent.VK_E);
        check(!((DialogueManager)field(p,"dialogueManager")).isActive()&&!((VillageActivities)field(p,"villageActivities")).campfire(),"Campfire E uses its locked restoration path rather than Noah dialogue");
        ((VillageActivities)field(p,"villageActivities")).chapter.unlock();santa(p).setPosition(VillageActivities.Site.CAMPFIRE.x-16,VillageActivities.Site.CAMPFIRE.y+4);tap(p,KeyEvent.VK_E);
        for(int[] size:new int[][]{{800,600},{1024,768},{1270,720},{1600,900}}){
            p.setSize(size[0],size[1]);var notice=render(p);Object text=message.get(p);message.set(p,null);var hidden=render(p);message.set(p,text);
            var camera=(Camera)field(p,"camera");var actor=new Rectangle(santa(p).getX()-camera.getX()-16,santa(p).getY()-camera.getY()-15,64,86);
            check(!differs(notice,hidden,actor),"Completion notice leaves Santa and the nearby approach readable "+size[0]);
            verifyNotice(p,notice,hidden,(String)text,false);
        }
        int builds=VillageRestorationVisuals.compositionCount();render(p);render(p);check(builds==1&&VillageRestorationVisuals.compositionCount()==builds,"Repeated GamePanel paints reuse exactly one composition");
        var map=WorldMap.forArea(Area.Type.VILLAGE);var point=FinalChapter.Restoration.TREE.approach;santa(p).setPosition(point.x()-16,point.y()+4);
        check(VillageActivities.inReach(santa(p),VillageActivities.Site.TREE)&&!util.Collision.intersectsAny(santa(p).getBounds(),map.solids()),"Authoritative relocated tree interaction has clear full footprint");
        check(map.solids().size()==286&&map.solids().contains(new Rectangle(1264,856,64,24))&&map.combatRegions().isEmpty(),"Tree adds only the approved64x24 grounded base; the square and combat regions remain unchanged");
        System.out.println("PASS: "+checks+" actual art, cache, viewport and effect assertions");
    }
}
