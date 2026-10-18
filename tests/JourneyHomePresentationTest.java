import game.*;
import game.VillageActivities.Site;
import ui.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.nio.file.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;

/** Actual GamePanel captures, shared draw/click scaling, bounded staging and cache safety. */
public class JourneyHomePresentationTest extends JourneyHomeTest {
    static final Path CAPTURES=Path.of("build/journey-home/captures");
    static void shot(GamePanel p,String name)throws Exception{Files.createDirectories(CAPTURES);ImageIO.write(render(p),"png",CAPTURES.resolve(name+".png").toFile());}
    public static void main(String[]args)throws Exception{
        JourneyHomeVisuals.prepareAsync().get(30,TimeUnit.SECONDS);SantaBaseVisuals.prepareAsync().get(30,TimeUnit.SECONDS);FrozenForestVisuals.prepareAsync().get(30,TimeUnit.SECONDS);
        GamePanel p=SantaBaseNavigationTest.opening();at(santa(p),WorldMap.forArea(Area.Type.SANTA_BASE).point("exit/to_village"));tick(p,1);shot(p,"01-locked-departure");
        SantaBaseStoryInteractionTest.approach(p,WorkshopInteractions.Site.GIFT_STORAGE);shot(p,"02-gift-storage");tap(p,KeyEvent.VK_E);
        for(int[] size:SIZES){p.setSize(size[0],size[1]);shot(p,"storage-"+size[0]+"x"+size[1]);}
        check(!window(p).isOpen(),"Gift Storage collection opens no receipt modal");travelGate(p,Area.Type.VILLAGE);forestFixture(p);
        at(santa(p),FinalChapter.PICKUPS.get(0).point());call(p,"updateCamera");shot(p,"03-forest-materials-before-boss");
        for(int[] size:SIZES){p.setSize(size[0],size[1]);chapter(p).toggleTracker();shot(p,"tracker-expanded-"+size[0]+"x"+size[1]);
            var bounds=HUD.questBounds((QuestGuidance.Objective)call(p,"currentObjective"),chapter(p),size[0],size[1]);check(bounds.y>=113&&bounds.getMaxY()<=size[1]-14&&bounds.getMaxX()<306&&bounds.getMaxX()<=size[0],"Expanded tracker clears critical HUD at "+size[0]);chapter(p).toggleTracker();shot(p,"tracker-collapsed-"+size[0]+"x"+size[1]);}
        p.setSize(1270,720);collectMaterials(p);travelGate(p,Area.Type.VILLAGE);near(p,Site.STREETLIGHTS);tap(p,KeyEvent.VK_E);chapter(p).toggleTracker();shot(p,"04-partially-restored-tracker");
        restoreVillage(p);shot(p,"05-restored-tracker-hidden");p.setSize(2560,1920);santa(p).setPosition(1296,976);shot(p,"06-village-fully-restored");p.stopGame();
        p=endingFixture();tick(p,180);tick(p,100);shot(p,"07-celebration-gathering");tick(p,140);shot(p,"08-celebration-gratitude");for(int[] size:SIZES){p.setSize(size[0],size[1]);shot(p,"celebration-"+size[0]+"x"+size[1]);var camera=(Camera)field(p,"camera");for(var foot:JourneyHomeVisuals.CITIZEN_FEET)check(foot.y()-camera.getY()<size[1]-160,"All neighbors remain above the celebration dialogue at "+size[0]);}p.setSize(1270,720);
        var town=WorldMap.forArea(Area.Type.CHRISTMAS_TOWN);var solids=town.solids();
        for(int frame=0;frame<=240;frame+=5)for(int i=0;i<3;i++){var foot=JourneyHomeVisuals.citizenFeet(i,frame);check(!util.Collision.intersectsAny(new Rectangle2D.Double(foot.x()-16,foot.y()-50,32,50),solids),"Scripted citizen stays on clear ground: "+i+" frame "+frame);}
        var elias=JourneyHomeVisuals.ELIAS_FEET;check(!util.Collision.intersectsAny(new Rectangle2D.Double(elias.x()-16,elias.y()-50,32,50),solids),"Cinematic Elias anchor is clear");
        completeCelebration(p);at(santa(p),JourneyHomeVisuals.SLEIGH_APPROACH);call(p,"updateCamera");shot(p,"09-parked-sleigh");tap(p,KeyEvent.VK_E);
        for(int[] size:SIZES){p.setSize(size[0],size[1]);shot(p,"confirmation-"+size[0]+"x"+size[1]);var box=VillageActivityUI.windowBounds(size[0],size[1]);check(box.x>=0&&box.y>=0&&box.getMaxX()<=size[0]&&box.getMaxY()<=size[1],"Sleigh modal fits viewport");
            int x=santa(p).getX(),y=santa(p).getY(),attacks=santa(p).getAttackSequence();for(int key:new int[]{KeyEvent.VK_W,KeyEvent.VK_SPACE,KeyEvent.VK_1})tap(p,key);modalClick(p,450,280);check(santa(p).getX()==x&&santa(p).getY()==y&&santa(p).getAttackSequence()==attacks,"Confirmation artwork clicks and held gameplay input remain isolated");
            modalClick(p,680,427);check(!window(p).isOpen()&&!santa(p).isAttacking()&&!chapter(p).homeReturned(),"Scaled Stay hitbox cancels without an attack");tap(p,KeyEvent.VK_E);}
        p.setSize(1270,720);modalClick(p,180,427);tick(p,45);shot(p,"10-sleigh-departure");tick(p,EndingCutscene.SLEIGH_DURATION-45);shot(p,"11-base-home-arrival");SantaBaseStoryInteractionTest.approach(p,WorkshopInteractions.Site.LETTER_DESK);shot(p,"12-desk-with-letters");openLetters(p);
        for(int[] size:SIZES){p.setSize(size[0],size[1]);shot(p,"normal-letter-"+size[0]+"x"+size[1]);int attacks=santa(p).getAttackSequence(),x=santa(p).getX(),y=santa(p).getY();
            for(int key:new int[]{KeyEvent.VK_W,KeyEvent.VK_SPACE,KeyEvent.VK_1})tap(p,key);modalClick(p,450,270);check(santa(p).getAttackSequence()==attacks&&santa(p).getX()==x&&santa(p).getY()==y&&!santa(p).isDashing(),"Letter modal isolates world controls at "+size[0]);
            modalClick(p,736,427);check(window(p).letterIndex()==1,"Scaled Next reads Martha");modalClick(p,180,427);check(window(p).letterIndex()==0,"Scaled Previous returns to Toby");}
        for(int i=0;i<8;i++)tap(p,KeyEvent.VK_RIGHT);
        for(int[] size:SIZES){p.setSize(size[0],size[1]);shot(p,"collective-letter-"+size[0]+"x"+size[1]);}
        p.setSize(1270,720);modalClick(p,736,427);tick(p,30);shot(p,"13-workshop-fade");tick(p,130);shot(p,"14-home-for-christmas");tap(p,KeyEvent.VK_E);tick(p,100);shot(p,"15-final-promise");tap(p,KeyEvent.VK_E);tick(p,650);shot(p,"16-existing-credits");tick(p,1750);shot(p,"17-the-end");p.stopGame();
        var ready=java.util.Arrays.stream(JourneyHomeVisuals.class.getDeclaredMethods()).filter(m->m.getName().equals("ready")).findFirst().orElseThrow();ready.setAccessible(true);
        CompletableFuture<Object> pending=new CompletableFuture<>(),failed=new CompletableFuture<>(),canceled=new CompletableFuture<>();failed.completeExceptionally(new java.io.IOException("fixture"));canceled.cancel(false);
        for(var future:java.util.List.of(pending,failed,canceled)){long start=System.nanoTime();check(ready.invoke(null,future)==null&&System.nanoTime()-start<500_000_000L,"Pending/failed/canceled cache read returns without waiting");}
        check(JourneyHomeVisuals.sleighImage()==JourneyHomeVisuals.sleighImage()&&JourneyHomeVisuals.citizenImage(0)!=null,"Prepared finished assets are shared and reused");
        System.out.println("PASS: "+checks+" actual renderer, viewport, staging, modal and cache assertions");
    }
}
