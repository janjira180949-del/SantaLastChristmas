import game.*;
import entity.*;
import quest.*;
import spirit.SpiritTree;
import java.awt.event.KeyEvent;
import java.util.*;

public class SurvivalRetryTest extends V2TestSupport {
    public static void main(String[] args)throws Exception {
        GamePanel p=playing();completeOpeningDeliveries(p);story(p).startAfterDialogue(p.getChildren().get(0));
        at(santa(p),encounters(p).get("village").getCenter());tick(p,1);
        Encounter wave=encounters(p).getActive();Enemy original=enemies(p).get(0);
        original.takeDamage(25);int checkpointX=santa(p).getX(),checkpointY=santa(p).getY();
        santa(p).takeDamage(100);tick(p,1);check(p.getState()==GamePanel.State.GAME_OVER,"Death enters Game Over");
        double enemyX=original.getX();int enemyHealth=original.getHealth();
        tick(p,100);check(original.getX()==enemyX&&original.getHealth()==enemyHealth,"Gameplay stops on Game Over");
        tap(p,KeyEvent.VK_R);check(p.getState()==GamePanel.State.PLAYING&&santa(p).getHealth()==100,"Retry restores HP");
        check(santa(p).getX()==checkpointX&&santa(p).getY()==checkpointY,"Retry starts at encounter checkpoint");
        check(enemies(p).size()==2&&enemies(p).stream().allMatch(e->e.getHealth()==e.getMaximumHealth()),"Retry resets enemies without duplication");
        check(encounters(p).getActive()==wave&&!enemies(p).contains(original)&&story(p).isStoryStarted(),"Same incomplete wave, preserved story");
        for(int i=0;i<3;i++){santa(p).takeDamage(100);tick(p,1);tap(p,KeyEvent.VK_R);check(enemies(p).size()==2,"Repeated retry remains bounded");}
        clearWave(p);travelGate(p,Area.Type.FOREST);at(santa(p),WorldMap.forArea(Area.Type.FOREST).point("items/Toby Gift"));tap(p,KeyEvent.VK_E);
        returnGift(p);FinalChapterTest.collectMaterials(p);FinalChapterTest.restoreVillage(p);receiveBlackFrostClue(p);travelGate(p,Area.Type.FOREST);
        at(santa(p),encounters(p).get("crystal-1").getCenter());tick(p,1);clearWave(p);
        @SuppressWarnings("unchecked") List<BlackFrostCrystal> crystals=(List<BlackFrostCrystal>)field(p,"crystals");
        var crystal=crystals.get(0);at(santa(p),new WorldMap.Point(crystal.getPosition().x(),crystal.getPosition().y()+48));attack(p,crystal.getPosition().x(),crystal.getPosition().y());tick(p,24);
        at(santa(p),encounters(p).get("crystal-2").getCenter());tick(p,1);santa(p).takeDamage(100);tick(p,1);tap(p,KeyEvent.VK_R);
        check(story(p).isLostGiftComplete()&&story(p).getCrystalsDestroyed()==1&&crystal.getState()==BlackFrostCrystal.State.BROKEN,"Completed objectives survive later death");
        check(encounters(p).get("crystal-2").getWarningFrames()==59&&enemies(p).isEmpty(),"Retry resumes initial guardian warning in the same gameplay update");tick(p,59);
        check(encounters(p).isComplete("crystal-1")&&enemies(p).size()==3,"Earlier wave stays complete and current wave restarts");
        int spirit=((SpiritTree)field(p,"spiritTree")).getSpirit();tick(p,2);check(((SpiritTree)field(p,"spiritTree")).getSpirit()==spirit,"Retry cannot duplicate Spirit rewards");
        // Fixed healing has no inventory, cannot heal a fallen Santa or during combat.
        Santa s=new Santa(100,100);s.takeDamage(50);HealingPoint heal=new HealingPoint(Area.Type.VILLAGE,116,121);
        check(!heal.use(s,true)&&!heal.isUsed(),"Combat prevents resting");check(heal.use(s,false)&&s.getHealth()==90,"Fixed shelter heals 40");
        check(!heal.use(s,false)&&heal.isUsed(),"Shelter is limited to one use");
        santa(p).takeDamage(100);tick(p,1);tap(p,KeyEvent.VK_ESCAPE);check(p.getState()==GamePanel.State.MAIN_MENU,"Game Over returns to menu");
        tap(p,KeyEvent.VK_ENTER);tick(p,6);tap(p,KeyEvent.VK_E);check(santa(p).getHealth()==100&&!story(p).isStoryStarted()&&enemies(p).isEmpty(),"New adventure resets session");
        GamePanel bossPanel=playing();townFixture(bossPanel);story(bossPanel).acceptEliasQuest();
        at(santa(bossPanel),encounters(bossPanel).get("town").getCenter());tick(bossPanel,1);clearWave(bossPanel);
        at(santa(bossPanel),(WorldMap.Point)call(bossPanel,"spiritTreeInteractionPoint"));tick(bossPanel,1);closeOverlay(bossPanel);
        var originalBoss=(boss.DarkTitan)field(bossPanel,"activeBoss");originalBoss.takeDamage(originalBoss.getMaximumHealth()/2);
        check(originalBoss.getPhaseIndex()==1,"Boss retry fixture reaches Phase 2");
        santa(bossPanel).takeDamage(100);tick(bossPanel,1);tap(bossPanel,KeyEvent.VK_R);
        var restoredBoss=(boss.DarkTitan)field(bossPanel,"activeBoss");
        check(restoredBoss!=originalBoss&&restoredBoss.getPhaseIndex()==0&&restoredBoss.getHealth()==restoredBoss.getMaximumHealth(),"Boss retry resets HP, phase and timers");
        check(enemies(bossPanel).size()==1&&story(bossPanel).isTreeReached()&&story(bossPanel).isChristmasTownLeadUnlocked(),"Boss retry preserves prior story and cannot duplicate enemies");
        System.out.println("PASS: "+checks+" survival/retry assertions");
    }
}
