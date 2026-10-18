package game;

import entity.Santa;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import quest.StoryQuestManager;

/** One presentation snapshot for quest text, progress, and world direction. Owns no progress. */
public final class QuestGuidance {
    public record Objective(String title,String action,String progress,WorldMap.Point target,
            String targetLabel,List<String> details,List<String> optional) {
        public Objective { details=List.copyOf(details);optional=List.copyOf(optional); }
    }
    private QuestGuidance(){ }
    private static double distance(Santa santa,WorldMap.Point point){
        return Math.hypot(santa.getX()+16-point.x(),santa.getY()+21-point.y());
    }
    public static Objective resolve(StoryQuestManager story,VillageActivities activities,Area.Type area,
            Santa santa,EncounterManager encounters,List<BlackFrostCrystal> crystals){
        FinalChapter chapter=activities.chapter;WorldMap map=WorldMap.forArea(area);
        var details=new ArrayList<String>();var optional=new ArrayList<String>();
        String title,action,progress="",label;WorldMap.Point target;
        if(chapter.homeReturned()){
            title="Letters of Gratitude";action="Read the letters at the Letter Desk [E]";
            target=WorkshopInteractions.approach(WorkshopInteractions.Site.LETTER_DESK);label="Read the Letters";
        }else if(chapter.celebrated()){
            title="Return Home";action="Approach the sleigh [E]";target=JourneyHomeVisuals.SLEIGH_APPROACH;label="Return Home";
        }else if(area==Area.Type.SANTA_BASE){
            title=activities.presentsCollected()?"Travel to The Last Village":"Collect Christmas Presents";
            progress=activities.presentsCollected()?"":"0/3";action=activities.presentsCollected()?"Walk through the Departure Gate":"Gift Storage [E]";
            target=activities.presentsCollected()?map.point("exit/to_village"):WorkshopInteractions.approach(WorkshopInteractions.Site.GIFT_STORAGE);
            label=activities.presentsCollected()?"Departure Gate":"Gift Storage";
        }else switch(story.getStage()){
            case DELIVER_PRESENTS -> {
                title="A Christmas Delivery";progress=activities.deliveries()+"/3";action="Bring a present to each neighbor [E]";
                var remaining=java.util.Arrays.stream(VillageActivities.Site.values()).filter(VillageActivities.Site::recipient)
                    .filter(r->!activities.delivered(activities.presentFor(r))).toList();
                for(var r:remaining)details.add(r.label+" - Give Present [E]");
                var nearest=remaining.stream().min(Comparator.comparingDouble(r->distance(santa,new WorldMap.Point(r.x,r.y)))).orElseThrow();
                target=toward(area,Area.Type.VILLAGE,new WorldMap.Point(nearest.x,nearest.y));label=nearest.label;
            }
            case MEET_TOBY -> {title="Find Toby";action="Talk to Toby [E]";label="Toby";target=toward(area,Area.Type.VILLAGE,WorldMap.forArea(Area.Type.VILLAGE).point("npc/Toby"));}
            case CLEAR_GATE -> {title="Lost Gift";action="Clear the Forest gate encounter";target=encounters.get("village").getCenter();label="Gate encounter";}
            case FIND_GIFT -> {title="Lost Gift";action="Find Toby's lost gift in Frozen Forest [E]";target=toward(area,Area.Type.FOREST,WorldMap.forArea(Area.Type.FOREST).point("items/Toby Gift"));label=area==Area.Type.FOREST?"Toby's lost gift":"Frozen Forest";}
            case RETURN_GIFT -> {title="Lost Gift";action=area==Area.Type.FOREST?"Leave by the western Village gate; return to Toby in the plaza":"Return the gift to Toby in the plaza [E]";target=toward(area,Area.Type.VILLAGE,WorldMap.forArea(Area.Type.VILLAGE).point("npc/Toby"));label=area==Area.Type.FOREST?"Western Village gate":"Toby - central plaza";}
            case RESTORE_VILLAGE -> {
                title="Restore the Village";progress=chapter.completed()+"/4";
                for(var objective:FinalChapter.Restoration.values())if(!chapter.restored(objective))
                    details.add(objective.label+(objective.material==null?" [E]":" - "+chapter.count(objective.material)+"/3 "+objective.material.label));
                var ready=java.util.Arrays.stream(FinalChapter.Restoration.values()).filter(chapter::ready)
                    .min(Comparator.comparingDouble(r->distance(santa,r.approach))).orElse(null);
                var material=FinalChapter.PICKUPS.stream().filter(p->!chapter.isCollected(p.id()))
                    .filter(p->java.util.Arrays.stream(FinalChapter.Restoration.values()).anyMatch(r->!chapter.restored(r)&&r.material==p.material()))
                    .min(Comparator.comparingDouble(p->distance(santa,p.point()))).orElse(null);
                if(area==Area.Type.FOREST)material=chapter.guidedPickup(santa);
                if(area==Area.Type.VILLAGE&&ready!=null){action=ready.label+" [E]";target=ready.approach;label=ready.label;}
                else if(area==Area.Type.FOREST&&material!=null){String context=materialContext(material.id());action="Collect "+material.material().label+" - "+context+" [E]";target=material.point();label=material.material().label+" - "+context;}
                else if(material!=null){action="Collect restoration materials in Frozen Forest";target=toward(area,Area.Type.FOREST,material.point());label="Frozen Forest";}
                else {action="Return to The Last Village to restore it [E]";target=toward(area,Area.Type.VILLAGE,ready==null?map.point("spawn"):ready.approach);label=area==Area.Type.FOREST?"Western Village gate":"The Last Village";}
            }
            case TALK_TO_TOBY -> {title="Talk to Toby";action="Ask Toby about the remaining Black Frost [E]";target=toward(area,Area.Type.VILLAGE,WorldMap.forArea(Area.Type.VILLAGE).point("npc/Toby"));label="Toby";}
            case INVESTIGATE -> {
                title="Investigate the Black Frost";progress=story.getCrystalsDestroyed()+"/3";
                var active=encounters.getActive();
                var crystal=crystals.stream().filter(c->c.getState()!=BlackFrostCrystal.State.BROKEN)
                    .min(Comparator.<BlackFrostCrystal>comparingInt(c->active!=null&&active.getId().equals(c.getId())?0:c.getState()==BlackFrostCrystal.State.VULNERABLE?1:2)
                        .thenComparingDouble(c->distance(santa,c.getPosition()))).orElse(null);
                action=area==Area.Type.FOREST?"Defeat crystal guardians, then attack the core":"Investigate the crystals in Frozen Forest";
                target=toward(area,Area.Type.FOREST,crystal==null?null:crystal.getPosition());label=area==Area.Type.FOREST?"Black Frost Crystal":"Frozen Forest";
                if(area==Area.Type.FOREST&&crystal!=null){
                    if(active!=null&&active.getId().equals(crystal.getId())){
                        action=active.getRecoveryFrames()>0?"Recovery between waves":active.getWarningFrames()>0?(active.getWaveNumber()==1?"Crystal guardians approaching":"Reinforcements approaching"):
                            "Guardian wave "+active.getWaveNumber()+"/"+active.getWaveCount();label="Active crystal guardians";
                    }else if(crystal.getState()==BlackFrostCrystal.State.VULNERABLE){action="Core exposed \u2014 attack with LMB!";label="Exposed crystal core";}
                }
            }
            case DEFEAT_FROST_FANG -> {title="Frost Fang";action="Defeat Frost Fang to open Christmas Town";target=toward(area,Area.Type.FOREST,boss.FrostFang.ARENA);label="Frost Fang - eastern clearing";}
            case TRAVEL_TOWN -> {title="Save Christmas";action=area==Area.Type.CHRISTMAS_TOWN?"Talk to Elias [E]":"Continue to Christmas Town";target=toward(area,Area.Type.CHRISTMAS_TOWN,WorldMap.forArea(Area.Type.CHRISTMAS_TOWN).point("npc/Elias"));label=area==Area.Type.CHRISTMAS_TOWN?"Elias":"Christmas Town";}
            case REACH_TREE, DEFEAT_BOSS, RESTORE_TREE -> {
                title="Save Christmas";var tree=mapForTown().point("landmarks/Corrupted Town Center");
                boolean townCleared=encounters.isComplete("town");
                action=story.isBossDefeated()?"Restore the Spirit Tree [E]":story.isTreeReached()?"Defeat Dark Titan":townCleared?"Reach the Spirit Tree":"Clear the town encounter";
                target=toward(area,Area.Type.CHRISTMAS_TOWN,townCleared?new WorldMap.Point(tree.x(),tree.y()+128):encounters.get("town").getCenter());
                label=story.isBossDefeated()?"Restore the Spirit Tree":townCleared?"Spirit Tree":"Town encounter";
            }
            case CHRISTMAS_RESTORED -> {title="Christmas Returns";action="Celebrate beneath the Spirit Tree";target=null;label="";}
            default -> throw new IllegalStateException("Unknown story stage");
        }
        return new Objective(title,action,progress,target,label,details,optional);
    }
    private static String materialContext(int id){return switch(id){
        case 0->"western road";case 1->"northern ruins";case 2->"deep southern path";
        case 3->"Village approach";case 4->"abandoned camp";case 5->"eastern road";
        case 6->"south of camp";case 7->"monument route";case 8->"far eastern path";
        default->"Forest";};}
    private static WorldMap mapForTown(){return WorldMap.forArea(Area.Type.CHRISTMAS_TOWN);}
    /** Route labels and arrows follow existing gates; no new destination or teleport. */
    private static WorldMap.Point toward(Area.Type area,Area.Type destination,WorldMap.Point goal){
        if(area==destination)return goal;WorldMap map=WorldMap.forArea(area);
        return switch(area){
            case SANTA_BASE->map.point("exit/to_village");
            case VILLAGE->map.point("exit/to_forest");
            case FOREST->map.point(destination==Area.Type.VILLAGE?"exit/to_village":"exit/to_town");
            case CHRISTMAS_TOWN->map.point("exit/to_forest");
        };
    }
}
