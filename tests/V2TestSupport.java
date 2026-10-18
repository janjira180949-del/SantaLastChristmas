import game.*;
import entity.*;
import quest.*;
import spirit.SpiritTree;
import ui.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.util.List;

/** Shared deterministic fixtures. Production input/update/combat is used by integration tests. */
public class V2TestSupport {
    static int checks;
    static void check(boolean value,String message) { checks++;if(!value)throw new AssertionError(message); }
    static Object field(Object object,String name)throws Exception {
        Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);
    }
    static Object call(Object object,String name)throws Exception {
        Method m=object.getClass().getDeclaredMethod(name);m.setAccessible(true);return m.invoke(object);
    }
    static void tick(GamePanel panel,int count)throws Exception { for(int i=0;i<count;i++)call(panel,"update"); }
    static void tap(GamePanel panel,int key)throws Exception {
        panel.keyPressed(new KeyEvent(panel,KeyEvent.KEY_PRESSED,0,0,key,KeyEvent.CHAR_UNDEFINED));
        tick(panel,1);panel.keyReleased(new KeyEvent(panel,KeyEvent.KEY_RELEASED,0,0,key,KeyEvent.CHAR_UNDEFINED));
    }
    static Santa santa(GamePanel p)throws Exception { return (Santa)field(p,"santa"); }
    static StoryQuestManager story(GamePanel p)throws Exception { return (StoryQuestManager)field(p,"storyQuestManager"); }
    static AreaManager areas(GamePanel p)throws Exception { return (AreaManager)field(p,"areaManager"); }
    static EncounterManager encounters(GamePanel p)throws Exception { return (EncounterManager)field(p,"encounterManager"); }
    @SuppressWarnings("unchecked") static List<Enemy> enemies(GamePanel p)throws Exception { return (List<Enemy>)field(p,"enemies"); }
    static void at(Santa santa,WorldMap.Point p) { santa.setPosition(p.x()-16,p.y()-21); }
    static GamePanel playing()throws Exception {
        boss.BossArtwork.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS);
        GamePanel p=new GamePanel();game.VillageRestorationVisuals.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS);game.FinalChapterVisuals.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS);entity.VillageResident.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS);game.VillageActivityVisuals.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS);game.FrozenForestVisuals.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS);
        p.setSize(1270,720);tap(p,KeyEvent.VK_ENTER);tick(p,6);tap(p,KeyEvent.VK_E);enterVillage(p);
        // Reproduce the completed pre-Base renderer fixture clocks after real departure.
        var weather=(WorldAtmosphere)field(p,"worldAtmosphere");weather.reset();weather.update();weather.update();
        var visual=Santa.class.getDeclaredField("visualTicks");visual.setAccessible(true);visual.setInt(santa(p),0);
        var light=HealingPoint.class.getDeclaredField("lightFrame");light.setAccessible(true);
        for(var point:(java.util.List<?>)field(p,"healingPoints"))light.setInt(point,0);
        var crystalClock=BlackFrostCrystal.class.getDeclaredField("visualTicks");crystalClock.setAccessible(true);
        for(var crystal:(java.util.List<?>)field(p,"crystals"))crystalClock.setInt(crystal,0);
        ((AreaTitleOverlay)field(p,"areaTitle")).show(Area.Type.VILLAGE);return p;
    }
    static void closeDialogue(GamePanel p)throws Exception {
        if(((GiftDeliveryPresentation)field(p,"giftDelivery")).isActive())tick(p,GiftDeliveryPresentation.DURATION);
        DialogueManager d=(DialogueManager)field(p,"dialogueManager");int guard=30;
        while(d.isActive()&&guard-->0)tap(p,KeyEvent.VK_E);
        check(!d.isActive(),"Dialogue closes in bounded pages");
    }
    static void closeOverlay(GamePanel p)throws Exception {
        StoryOverlay o=(StoryOverlay)field(p,"storyOverlay");int guard=10;
        while(o.isActive()&&guard-->0)tap(p,KeyEvent.VK_E);
        check(!o.isActive(),"Lore closes in bounded pages");
    }
    static void attack(GamePanel p, double x,double y)throws Exception {
        call(p,"updateCamera");Camera camera=(Camera)field(p,"camera");
        MouseEvent click=new MouseEvent(p,MouseEvent.MOUSE_PRESSED,0,0,(int)x-camera.getX(),(int)y-camera.getY(),1,false,MouseEvent.BUTTON1);
        for(var listener:p.getMouseListeners())listener.mousePressed(click);
        tick(p,1);
    }
    static void clearWave(GamePanel p)throws Exception {
        Encounter active=encounters(p).getActive();check(active!=null,"An authored wave is active");
        closeOverlay(p);santa(p).toggleDeveloperTestMode();int guard=600;
        while(encounters(p).getActive()!=null&&guard-->0) {
            // The new warned Forest boss reveals its intro after the warning, not at start().
            if(((StoryOverlay)field(p,"storyOverlay")).isActive()){closeOverlay(p);continue;}
            Enemy e=enemies(p).stream().filter(v->!v.isDefeated()).findFirst().orElse(null);
            if(e==null){tick(p,1);continue;}
            WorldMap map=WorldMap.forArea(areas(p).getCurrentArea().getType());
            var near=map.safePosition((int)e.getX()-16,(int)e.getY()+e.getSize()/2+20,32,50);
            santa(p).setPosition(near.x(),near.y()+8);
            attack(p,e.getX()+e.getSize()/2.0,e.getY()+e.getSize()/2.0);tick(p,23);
        }
        santa(p).toggleDeveloperTestMode();check(encounters(p).getActive()==null,"Wave clears through directional mouse attacks");
        BossFallPresentation fall=(BossFallPresentation)field(p,"bossFall");if(fall.isActive())tick(p,BossFallPresentation.DURATION-fall.getFrames());
    }
    static void travelGate(GamePanel p,Area.Type destination)throws Exception {
        if(areas(p).getCurrentArea().getType()==Area.Type.SANTA_BASE&&!((VillageActivities)field(p,"villageActivities")).presentsCollected())collectOpeningPresents(p);
        var gate=areas(p).getCurrentExitZones().stream().filter(g->g.getDestination()==destination).findFirst().orElseThrow();
        var b=gate.getBounds();at(santa(p),new WorldMap.Point((int)b.getCenterX(),(int)b.getCenterY()));tick(p,1);
        check(areas(p).getCurrentArea().getType()==destination,"Real gate enters "+destination);
        var bounds=santa(p).getBounds();check(!util.Collision.intersectsAny(bounds,WorldMap.forArea(destination).solids()),"Arrival footprint is safe");
        tick(p,1);check(areas(p).getCurrentArea().getType()==destination,"Gate cannot bounce back");
    }
    /** New opening prerequisite is exercised with actual E/receipt/ESC, never granted by a fixture. */
    static void collectOpeningPresents(GamePanel p)throws Exception{
        var target=WorkshopInteractions.approach(WorkshopInteractions.Site.GIFT_STORAGE);
        santa(p).setPosition(target.x()-16,target.y()+4);tap(p,KeyEvent.VK_E);
        check(((VillageActivities)field(p,"villageActivities")).presentsCollected(),"Real Gift Storage collection satisfies opening departure");
        check(!((VillageActivityUI)field(p,"activityWindow")).isOpen(),"Opening collection is immediate without a modal");
    }
    static void enterVillage(GamePanel p)throws Exception {
        if(areas(p).getCurrentArea().getType()==Area.Type.SANTA_BASE)travelGate(p,Area.Type.VILLAGE);
    }
    static void completeOpeningDeliveries(GamePanel p)throws Exception {
        for(var site:java.util.List.of(VillageActivities.Site.IRIS,VillageActivities.Site.CLARA,VillageActivities.Site.BEN)){
            if(((VillageActivities)field(p,"villageActivities")).delivered(site.ordinal()-3))continue;
            santa(p).setPosition(site.x-16,site.y+4);tap(p,KeyEvent.VK_E);
            tick(p,GiftDeliveryPresentation.DURATION);closeDialogue(p);
        }
        check(story(p).isDeliveryComplete(),"Actual three recipient handovers complete opening quest");
    }
    static void forestFixture(GamePanel p)throws Exception {
        enterVillage(p);completeOpeningDeliveries(p);
        story(p).startAfterDialogue(p.getChildren().get(0));story(p).recordVillageCleared();
        check(areas(p).tryTravel(Area.Type.FOREST),"Fixture travels to Forest");call(p,"loadCurrentAreaContent");
        at(santa(p),WorldMap.forArea(Area.Type.FOREST).point("items/Toby Gift"));tap(p,KeyEvent.VK_E);
        returnGift(p);
    }
    static void returnGift(GamePanel p)throws Exception {
        travelGate(p,Area.Type.VILLAGE);
        Child toby=p.getChildren().get(0);at(santa(p),new WorldMap.Point(toby.getX()+15,toby.getY()+21));
        tap(p,KeyEvent.VK_E);closeDialogue(p);
        check(story(p).isLostGiftComplete(),"Gift return conversation completes Lost Gift");
        travelGate(p,Area.Type.FOREST);
    }
    /** Later-stage fixtures traverse the restoration and clue handoff, never activate crystals early. */
    static void receiveBlackFrostClue(GamePanel p)throws Exception {
        if(areas(p).getCurrentArea().getType()==Area.Type.FOREST)travelGate(p,Area.Type.VILLAGE);
        Child toby=p.getChildren().get(0);at(santa(p),new WorldMap.Point(toby.getX()+15,toby.getY()+21));
        tap(p,KeyEvent.VK_E);check(!story(p).isCrystalQuestActive(),"Clue commits only on final dialogue close");closeDialogue(p);
        check(story(p).isCrystalQuestActive(),"Actual Toby clue activates investigation");
    }
    static void investigationFixture(GamePanel p)throws Exception {
        forestFixture(p);FinalChapterTest.collectMaterials(p);FinalChapterTest.restoreVillage(p);
        receiveBlackFrostClue(p);travelGate(p,Area.Type.FOREST);
    }
    static void townFixture(GamePanel p)throws Exception {
        investigationFixture(p);for(int i=1;i<=3;i++)story(p).recordCrystalDestroyed("crystal-"+i);
        check(story(p).recordFrostFangDefeated(),"Later-stage fixture includes Forest boss victory");
        check(areas(p).tryTravel(Area.Type.CHRISTMAS_TOWN),"Fixture travels to Town");call(p,"loadCurrentAreaContent");
    }
    static GamePanel endingFixture()throws Exception {
        GamePanel p=playing();townFixture(p);story(p).acceptEliasQuest();story(p).reachSpiritTree();story(p).recordBossDefeated();
        for(String id:new String[]{"town","boss"}) {
            Encounter wave=encounters(p).get(id);wave.start(santa(p),enemies(p));
            int guard=400;
            while(!wave.finishIfCleared()&&guard-->0){for(Enemy enemy:wave.getMembers())enemy.takeDamage(enemy.getMaximumHealth());wave.advance(santa(p),enemies(p));}
            check(wave.getState()==Encounter.State.COMPLETE,"Fixture clears all finite waves");
        }
        enemies(p).clear();
        at(santa(p),(WorldMap.Point)call(p,"spiritTreeInteractionPoint"));tap(p,KeyEvent.VK_E);
        check(p.getState()==GamePanel.State.PLAYING,"Actual E starts the authorized in-world restoration");return p;
    }
    static BufferedImage render(GamePanel p) {
        BufferedImage im=new BufferedImage(p.getWidth(),p.getHeight(),BufferedImage.TYPE_INT_ARGB);
        var g=im.createGraphics();p.paint(g);g.dispose();return im;
    }
}
