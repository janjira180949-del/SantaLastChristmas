import game.*;
import entity.*;
import java.awt.event.KeyEvent;
import ui.*;

public class ChildIntegrationTest extends V2TestSupport {
    public static void main(String[] args)throws Exception {
        GamePanel p=playing();completeOpeningDeliveries(p);Child toby=p.getChildren().get(0);
        check(p.getChildren().size()==1&&Child.Kind.values().length==1,"Single persistent Toby actor");
        check(toby.getX()==1281&&toby.getY()==987,"Authorized central plaza Toby position");
        santa(p).setPosition(toby.getX()+57,toby.getY());toby.updateInteraction(santa(p));check(!toby.canInteract(),"Outside interaction range");
        santa(p).setPosition(toby.getX()+56,toby.getY());toby.updateInteraction(santa(p));check(toby.canInteract(),"Inside interaction range");
        tap(p,KeyEvent.VK_E);DialogueManager d=(DialogueManager)field(p,"dialogueManager");check(d.isActive()&&toby.getVisualState()==Child.VisualState.TALK,"E opens Toby dialogue");
        int x=santa(p).getX(),y=santa(p).getY();
        p.keyPressed(new java.awt.event.KeyEvent(p,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_D,'d'));
        tap(p,KeyEvent.VK_SPACE);tap(p,KeyEvent.VK_1);attack(p,900,900);tick(p,20);
        check(santa(p).getX()==x&&santa(p).getY()==y&&!santa(p).isAttacking(),"Dialogue freezes gameplay input");
        closeDialogue(p);check(story(p).isStoryStarted()&&toby.getVisualState()==Child.VisualState.IDLE,"Close accepts quest and restores idle");
        check(p.getChildren().get(0)==toby,"Actor identity persists");
        Object sprites=field(toby,"SPRITES");var poses=(java.awt.image.BufferedImage[][])field(sprites,"frames");
        check(poses.length==1&&java.util.Arrays.stream(poses[0]).allMatch(v->v!=null),"All retained child poses cached");
        System.out.println("PASS: "+checks+" Toby/dialogue assertions");
    }
}
