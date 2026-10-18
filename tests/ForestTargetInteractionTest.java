import game.*;
import entity.*;
import quest.*;
import java.awt.event.KeyEvent;

public class ForestTargetInteractionTest extends V2TestSupport {
    public static void main(String[] args)throws Exception {
        GamePanel p=playing();StoryQuestManager story=story(p);var gift=story.getTargets().get(0);
        at(santa(p),WorldMap.forArea(Area.Type.FOREST).point("items/Toby Gift"));
        check(!story.completeTarget(gift,santa(p)),"Gift cannot skip Toby");completeOpeningDeliveries(p);story.startAfterDialogue(p.getChildren().get(0));
        check(!story.completeTarget(gift,santa(p)),"Gift cannot skip introductory encounter");story.recordVillageCleared();
        santa(p).setPosition(100,100);check(!story.completeTarget(gift,santa(p)),"Gift requires proximity");
        var bounds=gift.getBounds();at(santa(p),new WorldMap.Point((int)bounds.getCenterX(),(int)bounds.getCenterY()));
        check(story.completeTarget(gift,santa(p))&&story.isGiftFound()&&!story.isLostGiftComplete(),"Gift pickup leaves return objective active");
        check(!story.completeTarget(gift,santa(p))&&gift.isCollected(),"Duplicate pickup blocked");
        check(!story.recordCrystalDestroyed("crystal-1"),"Crystals cannot progress before gift return");
        var toby=p.getChildren().get(0);
        check(!story.returnGiftToToby(toby,santa(p)),"Cannot return gift from the forest");
        at(santa(p),new WorldMap.Point(toby.getX()+15,toby.getY()+21));
        check(story.returnGiftToToby(toby,santa(p))&&story.isLostGiftComplete(),"Return completes quest");
        check(!story.returnGiftToToby(toby,santa(p)),"Duplicate return blocked");
        check(!story.isCrystalQuestActive(),"Returning gift keeps crystals locked for restoration");
        var chapter=((VillageActivities)field(p,"villageActivities")).chapter;chapter.unlock();for(var pickup:FinalChapter.PICKUPS)chapter.collect(pickup.id());for(var r:FinalChapter.Restoration.values())chapter.restore(r);
        check(story.synchronizeRestoration()&&story.acceptBlackFrostClue(toby,santa(p)),"Isolated core fixture completes both authorized handoffs");
        Encounter wave=encounters(p).get("crystal-1");BlackFrostCrystal crystal=new BlackFrostCrystal(wave);
        at(santa(p),wave.getCenter());santa(p).setAttackDirection(1,0);santa(p).startAttack();
        check(!crystal.tryBreak(santa(p)),"Dormant core protected");wave.start(santa(p),enemies(p));for(int i=0;i<60;i++)wave.advance(santa(p),enemies(p));crystal.update(santa(p));
        check(!crystal.tryBreak(santa(p)),"Guarded core protected");
        for(Enemy e:wave.getMembers())e.takeDamage(e.getMaximumHealth());check(wave.finishIfCleared(),"Defined members clear wave");
        crystal.update(santa(p));check(!crystal.tryBreak(santa(p)),"Killing swing cannot instantly break newly exposed core");
        for(int i=0;i<24;i++)santa(p).update(false,false,false,false,java.util.List.of());
        at(santa(p),new WorldMap.Point(wave.getCenter().x(),wave.getCenter().y()+40));santa(p).setAttackDirection(0,-1);santa(p).startAttack();
        check(crystal.tryBreak(santa(p))&&!crystal.tryBreak(santa(p)),"New attack breaks core exactly once");
        check(story.recordCrystalDestroyed(crystal.getId())&&!story.recordCrystalDestroyed(crystal.getId()),"Quest completion idempotent");
        check(!story.recordCrystalDestroyed("unknown"),"Unknown crystal cannot forge progression");
        System.out.println("PASS: "+checks+" gift/crystal assertions");
    }
}
