import game.*;
import entity.Elias;
import java.awt.event.KeyEvent;
import ui.DialogueManager;

public class TownKeeperTest extends V2TestSupport {
    public static void main(String[] args)throws Exception {
        GamePanel p=playing();check(!story(p).acceptEliasQuest()&&!story(p).reachSpiritTree(),"Town story cannot be skipped");
        townFixture(p);Elias elias=(Elias)field(p,"elias");
        at(santa(p),new WorldMap.Point(elias.getX()+16,elias.getY()+24));tap(p,KeyEvent.VK_E);
        check(!story(p).isBossQuestAccepted(),"Acceptance waits for dialogue close");
        DialogueManager d=(DialogueManager)field(p,"dialogueManager");boolean guardian=false;
        while(d.isActive()) {guardian|=d.getActiveDialogue().getCurrentLine().getText().contains("never evil");tap(p,KeyEvent.VK_E);}
        check(guardian&&story(p).isBossQuestAccepted(),"Guardian story preserved through Elias");
        tap(p,KeyEvent.VK_E);closeDialogue(p);check(story(p).isBossQuestAccepted(),"Repeat conversation cannot reset progress");
        at(santa(p),(WorldMap.Point)call(p,"spiritTreeInteractionPoint"));tick(p,1);
        check(!story(p).isTreeReached()&&encounters(p).getActive()==null,"Final town encounter is required before boss");
        at(santa(p),encounters(p).get("town").getCenter());tick(p,1);clearWave(p);
        at(santa(p),(WorldMap.Point)call(p,"spiritTreeInteractionPoint"));tick(p,1);closeOverlay(p);
        check(story(p).isTreeReached()&&encounters(p).getActive().getId().equals("boss"),"Tree begins boss in same Town");
        System.out.println("PASS: "+checks+" Elias assertions");
    }
}
