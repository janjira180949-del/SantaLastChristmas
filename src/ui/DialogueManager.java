package ui;

import entity.Child;
import entity.Elias;
import entity.Santa;
import quest.StoryQuestManager;
import java.util.List;

/** Existing finite page dialogue system, reduced to Toby and Elias. */
public final class DialogueManager {
    private Dialogue activeDialogue;
    private boolean eliasDialogueActive;
    private boolean giftReturnDialogueActive, blackFrostClueDialogueActive, advancesChildStory;
    public boolean isEliasDialogueActive() { return eliasDialogueActive; }
    public boolean isGiftReturnDialogueActive() { return giftReturnDialogueActive; }
    public boolean isBlackFrostClueDialogueActive(){return blackFrostClueDialogueActive;}
    public boolean startFor(Child child) {
        if(isActive()||child==null||!child.canInteract())return false;
        activeDialogue=new Dialogue(child,List.of(
            new DialogueLine("Toby",Portrait.Kind.TOY,Portrait.Expression.WORRIED,"Santa... I lost the Christmas gift\nmy mother gave me."),
            new DialogueLine("Toby",Portrait.Kind.TOY,Portrait.Expression.WORRIED,"It's the last thing I have\nto remember her by.\nI lost it on the northern Forest road.\nCould you please help me?"),
            new DialogueLine("Santa",Portrait.Kind.SANTA,Portrait.Expression.CONCERNED,"Of course, Toby.\nI promise I'll bring it back.\nSome gifts are simply too precious to lose."),
            new DialogueLine("Toby",Portrait.Kind.TOY,Portrait.Expression.HAPPY,"Two infected guard the eastern gate. Click toward them to attack. Space dashes past a warning. Press 1 to ring your Bell.")));
        child.setTalking(true);eliasDialogueActive=false;giftReturnDialogueActive=false;blackFrostClueDialogueActive=false;advancesChildStory=true;return true;
    }
    /** Chooses a story-aware conversation; rewards are committed by the game after its last page. */
    public boolean startFor(Child child,Santa santa,StoryQuestManager story) {
        if(isActive()||child==null||!child.canInteract())return false;
        if(!story.isDeliveryComplete()){
            activeDialogue=new Dialogue(child,List.of(new DialogueLine("Toby",Portrait.Kind.TOY,"Santa! Iris, Clara and Ben are waiting for their Christmas presents. Please visit them first. I will be here.")));
            child.setTalking(true);eliasDialogueActive=false;giftReturnDialogueActive=false;blackFrostClueDialogueActive=false;advancesChildStory=false;return true;
        }
        if(story.canStartFor(child))return startFor(child);
        giftReturnDialogueActive=story.canReturnGiftTo(child,santa);
        blackFrostClueDialogueActive=story.canReceiveBlackFrostClue(child,santa);
        List<DialogueLine> lines=giftReturnDialogueActive?List.of(
            new DialogueLine("Santa",Portrait.Kind.SANTA,Portrait.Expression.HAPPY,"Here's your mother's gift, Toby.\nI promised I'd bring it back!"),
            new DialogueLine("Toby",Portrait.Kind.TOY,Portrait.Expression.HAPPY,"Thank you, Santa!\nI thought I'd lost it forever.\nIt means so much to me."),
            new DialogueLine("Toby",Portrait.Kind.TOY,Portrait.Expression.WORRIED,"But the village is still suffering.\nOur lights, houses, and Christmas tree\nare damaged. Even the campfire is out."),
            new DialogueLine("Toby",Portrait.Kind.TOY,"We need Spirit Fragments, Ancient Wood,\nand Christmas Ornaments from the forest.\nMartha knows how to use them."),
            new DialogueLine("Santa",Portrait.Kind.SANTA,Portrait.Expression.SERIOUS,"Then we'd better get to work!\nWe can't leave Christmas in the cold!")):
            blackFrostClueDialogueActive?List.of(
            new DialogueLine("Toby",Portrait.Kind.TOY,Portrait.Expression.HAPPY,"The lights are back, Santa!\nEveryone is smiling again.\nThank you for saving our village!"),
            new DialogueLine("Santa",Portrait.Kind.SANTA,"Finally! Now that's more like Christmas!\nBut... the strange frost is still here."),
            new DialogueLine("Toby",Portrait.Kind.TOY,Portrait.Expression.WORRIED,"I saw strange crystals deep in the forest.\nThe frost seemed to spread from them."),
            new DialogueLine("Santa",Portrait.Kind.SANTA,Portrait.Expression.CONCERNED,"Mysterious crystals in a frozen forest?\nThis Christmas just keeps getting weirder!\nCould they be connected to the Spirit Tree?"),
            new DialogueLine("Toby",Portrait.Kind.TOY,"There are three crystals.\nThey're blocking the road to Christmas Town.\nAnd something is guarding them."),
            new DialogueLine("Santa",Portrait.Kind.SANTA,Portrait.Expression.SERIOUS,"Then I'll destroy those crystals\nand find out what's behind all this.\nDon't worry, Toby. I'll bring Christmas back.")):
            List.of(new DialogueLine("Toby",Portrait.Kind.TOY,Portrait.Expression.HAPPY,
                story.isCrystalQuestActive()||story.isChristmasTownLeadUnlocked()?"You brought our hope back. Investigate the crystals and carry that hope to the Spirit Tree.":
                story.isLostGiftComplete()?"Martha knows what our village needs. Thank you for bringing back a little warmth, Santa.":
                "My mother's gift is on the northern Forest road. Please bring it back to me."));
        activeDialogue=new Dialogue(child,lines);child.setTalking(true);eliasDialogueActive=false;
        advancesChildStory=giftReturnDialogueActive;return true;
    }
    public boolean startFor(Elias elias,Santa santa,boolean questAccepted,boolean bossDefeated) {
        return startFor(elias,santa,questAccepted,bossDefeated,false);
    }
    public boolean startFor(Elias elias,Santa santa,boolean questAccepted,boolean bossDefeated,boolean roadCleared) {
        if(isActive()||!elias.canInteract(santa))return false;
        List<DialogueLine> lines=bossDefeated?List.of(line("The burden is lifting. Bring its last light back to the Spirit Tree.")):
            questAccepted?List.of(line("Clear the infected on the road, then reach the tree. Watch the guardian's warnings and strike while it recovers.")):
            List.of(line("Santa... you made it.\nThe Spirit Tree is still alive,\nbut its guardian is losing himself."),
                new DialogueLine("Santa",Portrait.Kind.SANTA,"The Dark Titan?\nPlease tell me that's just\na dramatic nickname."),
                line("The Spirit Tree created him to carry\nour sorrow and fear. He was never evil.\nBut the burden became too much."),
                line("Black Frost took control of him.\nFree him, and the Spirit Tree\nmay shine again."),
                new DialogueLine("Santa",Portrait.Kind.SANTA,"Then I'll free him.\nNo one should have to carry\nall that sorrow alone."),
                line(roadCleared?"The infected on the road are gone.\nReach the Spirit Tree\nand free its guardian.":"The road ahead is still dangerous.\nOnce it's clear, reach the Spirit Tree\nand free its guardian."));
        activeDialogue=new Dialogue(lines);eliasDialogueActive=true;giftReturnDialogueActive=false;blackFrostClueDialogueActive=false;advancesChildStory=false;return true;
    }
    private DialogueLine line(String text) { return new DialogueLine("Elias",Portrait.Kind.ELIAS,Portrait.Expression.WORRIED,text); }
    /** Reward-free world descriptions use the same finite pages and E controls as NPCs. */
    public boolean startNarrative(List<DialogueLine> lines) {
        if(isActive()||lines==null||lines.isEmpty())return false;
        activeDialogue=new Dialogue(lines);
        eliasDialogueActive=false;giftReturnDialogueActive=false;blackFrostClueDialogueActive=false;advancesChildStory=false;
        return true;
    }
    public boolean advance() {
        if(!isActive())return false;
        if(activeDialogue.advance())return true;
        close();return false;
    }
    public void close() {
        if(activeDialogue!=null&&activeDialogue.getChild()!=null){var child=activeDialogue.getChild();child.setTalking(false);if(advancesChildStory)child.advanceStoryState();}
        activeDialogue=null;eliasDialogueActive=false;giftReturnDialogueActive=false;blackFrostClueDialogueActive=false;advancesChildStory=false;
    }
    public boolean isActive() { return activeDialogue!=null; }
    public Dialogue getActiveDialogue() { return activeDialogue; }
}
