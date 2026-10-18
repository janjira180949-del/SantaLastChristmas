package game;

import entity.Santa;
import java.awt.geom.Rectangle2D;
import java.util.List;
import quest.StoryQuestManager;
import ui.DialogueLine;
import ui.Portrait;

/** Finite pre-boss restoration and homecoming progress. No inventory or combat ownership. */
public final class FinalChapter {
    private static final WorldMap FOREST=WorldMap.forArea(Area.Type.FOREST);
    private static final List<Rectangle2D> FOREST_SOLIDS=FOREST.solids();
    private static final WorldMap VILLAGE=WorldMap.forArea(Area.Type.VILLAGE);
    private static final List<Rectangle2D> VILLAGE_SOLIDS=VILLAGE.solids();
    public enum Material {
        SPIRIT_FRAGMENT("Spirit Fragments"), ANCIENT_WOOD("Ancient Wood"), ORNAMENT("Christmas Ornaments");
        public final String label;
        Material(String label){this.label=label;}
    }
    public enum Restoration {
        STREETLIGHTS("Restore the Streetlights",Material.SPIRIT_FRAGMENT,new WorldMap.Point(912,944)),
        HOUSE("Repair the House",Material.ANCIENT_WOOD,new WorldMap.Point(2016,1744)),
        TREE("Restore the Christmas Tree",Material.ORNAMENT,VillageRestorationVisuals.TREE_APPROACH),
        CAMPFIRE("Light the Campfire",null,new WorldMap.Point(1888,1296));
        public final String label;
        public final Material material;
        public final WorldMap.Point approach;
        Restoration(String label,Material material,WorldMap.Point approach){this.label=label;this.material=material;this.approach=approach;}
    }
    public record Pickup(int id,Material material,WorldMap.Point point) { }
    public static final List<Pickup> PICKUPS=List.of(
        new Pickup(0,Material.SPIRIT_FRAGMENT,new WorldMap.Point(880,1136)),
        new Pickup(1,Material.SPIRIT_FRAGMENT,new WorldMap.Point(1856,736)),
        new Pickup(2,Material.SPIRIT_FRAGMENT,new WorldMap.Point(2496,1456)),
        new Pickup(3,Material.ANCIENT_WOOD,new WorldMap.Point(432,1136)),
        new Pickup(4,Material.ANCIENT_WOOD,new WorldMap.Point(1440,944)),
        new Pickup(5,Material.ANCIENT_WOOD,new WorldMap.Point(2336,1168)),
        new Pickup(6,Material.ORNAMENT,new WorldMap.Point(1296,1120)),
        new Pickup(7,Material.ORNAMENT,new WorldMap.Point(2208,672)),
        new Pickup(8,Material.ORNAMENT,new WorldMap.Point(2848,1616)));
    private final boolean[] picked=new boolean[PICKUPS.size()],restored=new boolean[Restoration.values().length];
    private boolean unlocked,letterFinished,celebrationStarted,celebrated,homeReturned;
    private boolean trackerCollapsed=true;
    private int guidedPickup=-1; // Presentation preference only; never owns collection progress.
    public Pickup guidedPickup(Santa santa){
        if(!unlocked)return null;
        if(guidedPickup>=0&&!isCollected(guidedPickup))return PICKUPS.get(guidedPickup);
        Pickup next=PICKUPS.stream().filter(p->!isCollected(p.id())).min(java.util.Comparator.comparingDouble(p->Math.hypot(santa.getX()+16-p.point().x(),santa.getY()+21-p.point().y()))).orElse(null);
        guidedPickup=next==null?-1:next.id();return next;
    }
    private String feedback;
    public boolean unlock(){if(unlocked)return false;unlocked=true;return true;}
    public boolean isUnlocked(){return unlocked;}
    public boolean isCollected(int id){return id>=0&&id<picked.length&&picked[id];}
    /** Cumulative collection totals never decrease; restoration commits a complete set once. */
    public int count(Material material){int count=0;for(Pickup pickup:PICKUPS)if(pickup.material==material&&picked[pickup.id])count++;return count;}
    public boolean collect(int id){
        if(!unlocked||id<0||id>=picked.length||picked[id])return false;
        picked[id]=true;Pickup pickup=PICKUPS.get(id);
        int total=0;for(boolean value:picked)if(value)total++;
        if(total==9)feedback="All nine materials ready! Return through the western Village gate to restore the Village.";
        else if(count(pickup.material)==3){
            Restoration task=java.util.Arrays.stream(Restoration.values()).filter(v->v.material==pickup.material).findFirst().orElseThrow();
            feedback=pickup.material.label+" ready (3/3)! "+task.label+" in the Village.";
        }else feedback=pickup.material.label+" collected ("+count(pickup.material)+"/3).";return true;
    }
    public boolean restored(Restoration objective){return objective!=null&&restored[objective.ordinal()];}
    public boolean ready(Restoration objective){return unlocked&&objective!=null&&!restored(objective)&&(objective.material==null||count(objective.material)==3);}
    public boolean restore(Restoration objective){
        if(!ready(objective))return false;restored[objective.ordinal()]=true;
        feedback=isComplete()?"A Little Hope Returns - Martha: We can find one another again. Return to Toby to ask about the remaining Black Frost.":switch(objective){
            case STREETLIGHTS->"Streetlights restored. Warm light returns to the village.";
            case HOUSE->"House repaired. A sound roof and a little warmth for the winter.";
            case TREE->"Christmas tree restored. The village remembers its traditions.";
            case CAMPFIRE->"Campfire lit. There is a warm place to gather tonight.";};return true;
    }
    public int completed(){int count=0;for(boolean value:restored)if(value)count++;return count;}
    public boolean isComplete(){return unlocked&&completed()==4;}
    public boolean beginCelebration(){if(!isComplete()||celebrationStarted)return false;celebrationStarted=true;return true;}
    public void completeCelebration(){if(celebrationStarted)celebrated=true;}
    public boolean celebrationStarted(){return celebrationStarted;}
    public boolean celebrated(){return celebrated;}
    public boolean returnHome(){if(!celebrated||homeReturned)return false;homeReturned=true;return true;}
    public boolean homeReturned(){return homeReturned;}
    public boolean trackerCollapsed(){return trackerCollapsed;}
    public void toggleTracker(){trackerCollapsed=!trackerCollapsed;}
    public boolean trackerVisible(){return isUnlocked()&&!isComplete();}
    public List<Restoration> activeObjectives(){return java.util.Arrays.stream(Restoration.values()).filter(r->!restored(r)).toList();}
    public boolean finishLetter(){if(!homeReturned||letterFinished)return false;letterFinished=true;return true;}
    public boolean letterFinished(){return letterFinished;}
    public String pollFeedback(){String value=feedback;feedback=null;return value;}
    public static boolean inReach(Santa santa,WorldMap map,WorldMap.Point point){
        if(santa==null||santa.isDefeated()||point==null)return false;
        List<Rectangle2D> solids=map==FOREST?FOREST_SOLIDS:map==VILLAGE?VILLAGE_SOLIDS:map.solids();
        if(util.Collision.intersectsAny(santa.getBounds(),solids))return false;
        double x=santa.getX()+16,y=santa.getY()+21;
        if(Math.hypot(x-point.x(),y-point.y())>70)return false;
        for(Rectangle2D solid:solids)if(solid.intersectsLine(x,y,point.x(),point.y()))return false;
        return true;
    }
    public Pickup nearby(Santa santa){
        if(!unlocked)return null;WorldMap map=WorldMap.forArea(Area.Type.FOREST);
        Pickup result=null;double distance=Double.MAX_VALUE;
        for(Pickup pickup:PICKUPS)if(!picked[pickup.id]&&inReach(santa,map,pickup.point)){
            double d=Math.hypot(santa.getX()+16-pickup.point.x(),santa.getY()+21-pickup.point.y());
            if(d<distance){distance=d;result=pickup;}
        }return result;
    }
    public String requirement(Restoration objective){
        if(restored(objective))return "This restoration is already complete.";
        if(!unlocked)return "Return Toby's lost gift first. He will ask you to help restore the village.";
        if(objective.material==null)return "No materials are needed. Press E at the campfire to light it.";
        return "Bring 3 "+objective.material.label+" from Frozen Forest. Collected: "+count(objective.material)+" / 3. Press E at this place when the set is complete.";
    }
    public WorldMap.Point guide(Area.Type area,Santa santa){
        WorldMap map=WorldMap.forArea(area);
        if(area==Area.Type.CHRISTMAS_TOWN)return celebrated?JourneyHomeVisuals.SLEIGH_APPROACH:null;
        if(area==Area.Type.SANTA_BASE)return homeReturned?WorkshopInteractions.approach(WorkshopInteractions.Site.LETTER_DESK):map.point("exit/to_village");
        if(area==Area.Type.FOREST){
            if(isComplete())return map.point("exit/to_town");
            Pickup nearest=null;double distance=Double.MAX_VALUE;
            for(Pickup pickup:PICKUPS)if(!picked[pickup.id]){double d=Math.hypot(santa.getX()-pickup.point.x(),santa.getY()-pickup.point.y());if(d<distance){distance=d;nearest=pickup;}}
            return nearest==null?map.point("exit/to_village"):nearest.point;
        }
        if(isComplete())return map.point("exit/to_forest");
        for(Restoration objective:Restoration.values())if(ready(objective))return objective.approach;
        return map.point("exit/to_forest");
    }
    public String guideLabel(Area.Type area){
        if(homeReturned)return "Read the Letters";
        if(celebrated)return "Return Home";
        if(isComplete())return "Continue to Christmas Town";
        return area==Area.Type.FOREST?"Restoration materials":"Restore the Village";
    }
    public StoryQuestManager.HudPresentation hud(Area.Type area){
        boolean materialsReady=count(Material.SPIRIT_FRAGMENT)==3&&count(Material.ANCIENT_WOOD)==3&&count(Material.ORNAMENT)==3;
        String objective=homeReturned?"Read the Letters\nLetter Desk [E]":celebrated?"Return Home\nApproach the sleigh [E]":celebrationStarted?"Celebrate Christmas":isComplete()?"Continue to Christmas Town":
            area==Area.Type.FOREST?(materialsReady?"All restoration materials found\nReturn to The Last Village":"Collect Restoration Materials [E]\nThen return to The Last Village"):
            materialsReady?"Restore the Village [E]\nLights, house, tree and campfire":"Gather materials in Frozen Forest\nRestore lights, house, tree and fire";
        return new StoryQuestManager.HudPresentation(StoryQuestManager.HudState.NEXT_OBJECTIVE,"THE JOURNEY HOME",homeReturned?"Letters of Gratitude":celebrated?"Christmas Returns":isComplete()?"A Little Hope Returns":"Restore the Village",objective);
    }
    public record Letter(String sender,Portrait.Kind portrait,String text){ }
    public static List<Letter> gratitudeLetters(VillageActivities activities){return List.of(
        new Letter("Toby",Portrait.Kind.TOY,"Dear Santa,\nYou found my lost gift when I thought it was gone forever. Now the village lights are back, too. I will keep a little hope for someone else.\n- Toby"),
        new Letter("Martha",Portrait.Kind.MARTHA,"Dear Santa,\nTonight I walked our streets and counted three warm lights. People opened their doors to one another again. A light is a promise, and you helped us keep ours.\n- Martha"),
        new Letter("Oliver",Portrait.Kind.OLIVER,"Dear Santa,\nWe are sharing ribbons, stories and a place beside the fire again. Christmas has always belonged to the whole village. Thank you for reminding us.\n- Oliver"),
        new Letter("Noah",Portrait.Kind.NOAH,"Dear Santa,\nA sound roof. A warm fire. Neighbors who stay to talk. There is still work ahead, but our village can face the winter together now.\n- Noah"),
        new Letter("Iris",Portrait.Kind.IRIS,activities.delivered(0)?"Dear Santa,\nThank you for the candle. I lit it beside Toby's house. With the streetlights shining again, no traveler has to look for a welcome in the dark.\n- Iris":"Dear Santa,\nThe streetlights make my small candle feel less alone. Thank you for giving our neighbors a safe, warm place to find one another.\n- Iris"),
        new Letter("Clara",Portrait.Kind.CLARA,activities.delivered(1)?"Dear Santa,\nYour paper star hangs beside the crooked ones the children made. Our evergreen looks beautiful again. There is room for every kind of star here.\n- Clara":"Dear Santa,\nWe are hanging our old paper stars again. The decorated evergreen gives the children somewhere to gather. Thank you for keeping that tradition alive.\n- Clara"),
        new Letter("Ben",Portrait.Kind.BEN,activities.delivered(2)?"Dear Santa,\nI read your book aloud beside the new fire. Even the quiet neighbors stayed for another chapter. Thank you for bringing us a story worth sharing.\n- Ben":"Dear Santa,\nWe can read beside the fire again, instead of alone behind cold doors. You gave our village a place for stories to be heard.\n- Ben"),
        new Letter("Elias",Portrait.Kind.ELIAS,"Dear Santa,\nChristmas Town is gathering beneath the Spirit Tree. The guardian's burden is over. We will tend the light together, through this winter and the next.\n- Elias"),
        new Letter("Everyone",Portrait.Kind.SANTA,"Dear Santa,\n\nThank you for bringing Christmas home.\n\nEven when the lights went out,\nyou reminded us that hope was still here.\n\nWith love,\nEveryone."));}
    public static List<DialogueLine> finalLetter(){return List.of(
        new DialogueLine("THE FINAL LETTER",Portrait.Kind.SANTA,"Dear Santa,\nThank you for bringing Christmas home."),
        new DialogueLine("THE FINAL LETTER",Portrait.Kind.SANTA,"Even when the lights went out,\nyou reminded us that hope was still here.\n- The People of The Last Village"));}
}
