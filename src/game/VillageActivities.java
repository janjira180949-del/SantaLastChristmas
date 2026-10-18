package game;

import entity.Santa;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import ui.DialogueLine;
import ui.Portrait;

/** Existing session flags; main quests observe deliveries/restoration without duplicate counters. */
public final class VillageActivities {
    public enum Site {
        MARTHA("Martha",1008,808), OLIVER("Oliver",1440,808), NOAH("Noah",1776,1296),
        IRIS("Iris",560,624), CLARA("Clara",1104,608), BEN("Ben",2080,576),
        STREETLIGHTS("Restore the Streetlights",912,944), LANTERN_EAST("Streetlight",1712,944),
        LANTERN_SOUTH("Streetlight",1296,1552), CAMPFIRE("Light the Campfire",1888,1296),
        HOUSE("Repair the House",2016,1744), TREE("Restore the Christmas Tree",VillageRestorationVisuals.TREE_APPROACH.x(),VillageRestorationVisuals.TREE_APPROACH.y());
        public final String label;
        public final int x,y;
        Site(String label,int x,int y){this.label=label;this.x=x;this.y=y;}
        public boolean resident(){return ordinal()<6;}
        public boolean recipient(){return this==IRIS||this==CLARA||this==BEN;}
        public FinalChapter.Restoration restoration(){return switch(this){
            case STREETLIGHTS,LANTERN_EAST,LANTERN_SOUTH->FinalChapter.Restoration.STREETLIGHTS;
            case HOUSE->FinalChapter.Restoration.HOUSE;case TREE->FinalChapter.Restoration.TREE;case CAMPFIRE->FinalChapter.Restoration.CAMPFIRE;default->null;};}
    }
    public static final String[] CLUES={
        "For the neighbor who keeps a candle for absent travelers beside Toby's house.",
        "For the hands that fold paper stars outside the northern house.",
        "For the reader who carries books from the eastern library."};
    private static final Site[] RECIPIENTS={Site.IRIS,Site.CLARA,Site.BEN};
    private static final WorldMap MAP=WorldMap.forArea(Area.Type.VILLAGE);
    private static final List<Rectangle2D> SOLIDS=MAP.solids();
    private final boolean[] delivered=new boolean[3];
    public final FinalChapter chapter=new FinalChapter();
    private final int[] conversations=new int[6];
    private boolean presentsCollected;
    private String feedback;
    public boolean lantern(int index){return index>=0&&index<3&&chapter.restored(FinalChapter.Restoration.STREETLIGHTS);}
    public int lights(){return chapter.restored(FinalChapter.Restoration.STREETLIGHTS)?3:0;}
    public int deliveries(){int count=0;for(boolean value:delivered)if(value)count++;return count;}
    public boolean delivered(int index){return delivered[index];}
    public boolean presentsCollected(){return presentsCollected;}
    public boolean campfire(){return chapter.restored(FinalChapter.Restoration.CAMPFIRE);}
    public boolean collectPresents(){
        if(presentsCollected)return false;
        presentsCollected=true;feedback="Presents Collected 3/3";return true;
    }
    public int completionEvents(){return chapter.completed()+deliveries();}
    public boolean hopeful(){return chapter.isComplete();}
    public String pollFeedback(){String message=feedback;feedback=null;return message==null?chapter.pollFeedback():message;}
    public boolean lightCampfire(){return chapter.restore(FinalChapter.Restoration.CAMPFIRE);}
    private void completed(String message){feedback=message;}
    public boolean deliver(int present,Site recipient){
        if(!presentsCollected||present<0||present>=3){feedback="Collect the three presents from Gift Storage in Santa's Base first.";return false;}
        if(delivered[present]){feedback="That present has already found its home.";return false;}
        if(RECIPIENTS[present]!=recipient){feedback="A kind thought! This clue belongs to another neighbor. Try again.";return false;}
        delivered[present]=true;completed(deliveries()==3?"Christmas Presents Delivered! 3/3":recipient.label+" received a Christmas present. "+deliveries()+"/3");return true;
    }
    public int presentFor(Site recipient){for(int i=0;i<RECIPIENTS.length;i++)if(RECIPIENTS[i]==recipient)return i;return -1;}
    public boolean canDeliver(Site recipient){int i=presentFor(recipient);return presentsCollected&&i>=0&&!delivered[i];}
    private static DialogueLine santaLine(String text){return new DialogueLine("Santa",Portrait.Kind.SANTA,text);}
    public List<DialogueLine> deliveryThanks(Site recipient){
        var lines=new java.util.ArrayList<DialogueLine>();
        lines.add(santaLine("Merry Christmas, "+recipient.label+"! This present is for you."));
        switch(recipient){
            case IRIS->{lines.set(0,santaLine("Merry Christmas, Iris!\nThis one's for you!"));lines.add(line(recipient,"Santa! You really came!\nThank you so much!"));lines.add(santaLine("Of course! But...\nwhere did all the Christmas lights go?"));lines.add(line(recipient,"They went out a few nights ago.\nNo one knows why."));lines.add(santaLine("Then I'll find out!\nChristmas isn't over yet!"));}
            case CLARA->{lines.set(0,santaLine("Merry Christmas, Clara!\nI saved this present just for you!"));lines.add(line(recipient,"Santa! You remembered!\nI was starting to lose hope."));lines.add(santaLine("Of course I did!\nBut... what happened to the village?"));lines.add(line(recipient,"A strange dark frost appeared.\nIt damaged our homes and trees."));lines.add(santaLine("Dark frost?\nThat's definitely not on my Christmas list!"));lines.add(line(recipient,"And it's still spreading...\nPlease be careful, Santa."));}
            case BEN->{lines.set(0,santaLine("Merry Christmas, Ben! I have a present for you."));lines.add(line(recipient,"Wow! Thank you, Santa!"));lines.add(santaLine("Everyone seems worried. Do you know what's happening?"));lines.add(line(recipient,"Strange things have been happening near the forest."));lines.add(santaLine("Do you know who might understand it?"));lines.add(line(recipient,"Toby might. He went near the forest before everything got worse."));lines.add(santaLine("I should speak with him."));}
            default->throw new IllegalArgumentException("Not a recipient");
        }
        if(recipient==Site.BEN){lines.add(santaLine("I came to deliver presents... but this village needs more than gifts."));lines.add(santaLine("I should find Toby."));}
        return List.copyOf(lines);
    }
    public List<DialogueLine> conversation(Site site,quest.StoryQuestManager story){
        if(site==Site.OLIVER){
            String help=switch(story.getStage()){
                case DELIVER_PRESENTS->!presentsCollected?"Your presents are at Gift Storage in Santa's Base. Collect them before setting out.":remainingRecipientHints();
                case MEET_TOBY->"All three presents are home. Toby is beside the western house. He may know why the village has changed.";
                case CLEAR_GATE->"Toby needs his mother's gift. Clear the infected at the eastern gate to reach the Forest.";
                case FIND_GIFT->"Follow the northern road in Frozen Forest to find Toby's gift.";
                case RETURN_GIFT->"Bring the lost gift back to Toby beside the western house.";
                case RESTORE_VILLAGE->"Still to do: "+chapter.activeObjectives().stream().map(r->r.label).collect(java.util.stream.Collectors.joining(", "))+". The Forest paths hold the needed materials. Our campfire needs only a spark.";
                case TALK_TO_TOBY->"There is warmth here again, but Black Frost remains. Ask Toby what he saw deeper in the Forest.";
                case INVESTIGATE->"Investigate the three Black Frost Crystals. Defeat their guards, then break each core.";
                case TRAVEL_TOWN->"The way is open. Continue to Christmas Town and speak to Elias.";
                default->"Carry our hope to the Spirit Tree, Santa. We will keep a light for you.";
            };
            return List.of(line(site,"Oliver, Village Helper. Let's find your next step."),line(site,help));
        }
        if(site==Site.MARTHA)return List.of(line(site,chapter.isComplete()?"Our Christmas market is welcoming people again. Thank you for bringing a little hope back.":lights()==3?"With the streetlights warm again, I can open our market. The village still needs a sound roof, its Christmas tree and a gathering fire.":"I'm Martha. This was our Christmas market. Now the stalls are quiet and our streetlights are frozen."),line(site,explanation(site)),line(site,lore(site)));
        return conversation(site);
    }
    private String remainingRecipientHints(){
        var hints=new java.util.ArrayList<String>();
        if(!delivered[0])hints.add("Iris keeps a candle by Toby's house");
        if(!delivered[1])hints.add("Clara folds stars by the northern house");
        if(!delivered[2])hints.add("Ben reads near the eastern library");
        return "Still waiting: "+String.join("; ",hints)+". Bring each a present.";
    }
    public static boolean inReach(Santa santa,Site site){
        if(site==null||santa.isDefeated()||util.Collision.intersectsAny(santa.getBounds(),SOLIDS))return false;
        double x=santa.getX()+16.0,y=santa.getY()+21.0;
        if(Math.hypot(x-site.x,y-site.y)>70)return false;
        Line2D sight=new Line2D.Double(x,y,site.x,site.y);
        for(Rectangle2D solid:SOLIDS)if(sight.intersects(solid))return false;
        return true;
    }
    public static Site nearby(Santa santa){
        Site best=null;double distance=Double.MAX_VALUE;
        for(Site site:Site.values())if(inReach(santa,site)){
            double d=Math.hypot(santa.getX()+16.0-site.x,santa.getY()+21.0-site.y);
            if(d<distance){best=site;distance=d;}
        }
        return best;
    }
    private DialogueLine line(Site site,String text){return new DialogueLine(site.label,Portrait.Kind.valueOf(site.name()),text);}
    public List<DialogueLine> conversation(Site site){
        if(!site.resident())throw new IllegalArgumentException("Not a resident");
        int visits=conversations[site.ordinal()]++;
        boolean done=switch(site){case MARTHA->lights()==3;case OLIVER->deliveries()==3;case NOAH->false;default->delivered[site.ordinal()-3];};
        if(done)return List.of(line(site,switch(site){
            case MARTHA->"You brought back our street lights. We can find one another after dark again.";
            case OLIVER->"Every present found its home! Small kindnesses still reach people, even in this winter.";
            case NOAH->"We still have one another, Santa.";
            default->"You remembered me. The village feels less empty now. Thank you, Santa.";}),
            line(site,"There is still Black Frost beyond our doors. But tonight, we have something worth keeping."));
        if(visits==0)return List.of(line(site,switch(site){
            case MARTHA->"I'm Martha. I used to light these streets before the children walked home. The frost has silenced three lanterns.";
            case OLIVER->"Oliver, at your service! Iris, Clara and Ben are still here. I kept notes about the presents meant for them.";
            case NOAH->"Noah. I used to mend carts and toys here. These days, I keep the neighbors company while we wait out the cold.";
            case IRIS->"I'm Iris. I keep a candle beside Toby's house, for the neighbors who haven't come home.";
            case CLARA->"Clara. I still fold paper stars by the northern house. We used to hang one for every visitor.";
            case BEN->"I'm Ben. I carry books from the eastern library. Stories make the long nights a little shorter.";
            default->throw new IllegalArgumentException("Not a resident");}),
            line(site,explanation(site)),line(site,lore(site)));
        return List.of(line(site,explanation(site)),line(site,lore(site)));
    }
    private String explanation(Site site){return switch(site){
        case MARTHA->chapter.isUnlocked()?"Bring three Spirit Fragments from Frozen Forest to a streetlight, then press E. Ancient Wood can repair the south-east house; ornaments belong on our market-square Christmas tree.":"Return Toby's lost gift first, then help restore our village. Its old paths hold Spirit Fragments for our lights, Ancient Wood for the house and ornaments for our evergreen.";
        case OLIVER->presentsCollected?"Your three presents belong to Iris, Clara and Ben. Approach each neighbor and press E to give their present. Each neighbor is waiting for a Christmas present.":"Gift Storage prepared three presents for the journey. When you have them, my clues will help you find their homes. Bring each neighbor a Christmas present.";
        case NOAH->campfire()?"That fire gives us somewhere to sit together. Not everything broken needs a machine to mend it.":"We used to gather around the campfire after work. Its ashes are cold now. I miss hearing everyone laugh.";
        default->presentsCollected?"Perhaps one of your presents has my name hidden in its clue.":"Oliver remembers the delivery clues. The presents are still at Santa's Base.";};}
    private String lore(Site site){return switch(site){
        case MARTHA->"I don't know where Black Frost began. I only know that keeping a light for someone is a promise we can still make.";
        case OLIVER->"We used to argue over ribbons and wrapping. Now a familiar name on a present is enough to make someone smile.";
        case NOAH->"My father mended toys through every winter. I kept his tools, and his habit of listening. Black Frost hasn't taken that.";
        case IRIS->"A candle cannot stop the frost. It can tell a traveler that someone is waiting.";
        case CLARA->"The children taught me the crooked stars are the lucky ones. I keep theirs by the window.";
        case BEN->"The shelves are cold, but the stories haven't changed. We read them aloud for whoever can come.";
        default->throw new IllegalArgumentException("Not a resident");};}
}
