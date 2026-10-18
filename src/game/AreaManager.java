package game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Owns the existing areas and their authored bidirectional connections. */
public class AreaManager {
    private final List<Area> areas = new ArrayList<>();
    private int currentAreaIndex;

    private final quest.StoryQuestManager story;
    private final VillageActivities activities;
    public AreaManager() { this(new quest.StoryQuestManager()); }
    public AreaManager(quest.StoryQuestManager story) {
        this(story,new VillageActivities());
    }
    public AreaManager(quest.StoryQuestManager story,VillageActivities activities) {
        this.story = story;
        this.activities=java.util.Objects.requireNonNull(activities);
        areas.add(new Area(Area.Type.VILLAGE,"The Last Village",true));
        areas.add(new Area(Area.Type.FOREST,"Frozen Forest",false));
        areas.add(new Area(Area.Type.CHRISTMAS_TOWN,"Christmas Town",false));
        areas.add(new Area(Area.Type.SANTA_BASE,"Santa's Base",true));
        currentAreaIndex=indexOf(Area.Type.SANTA_BASE);
    }
    public Area getCurrentArea() { return areas.get(currentAreaIndex); }
    public List<Area> getAreas() { return Collections.unmodifiableList(areas); }
    public Area getNextArea() {
        Area.Type destination=switch(getCurrentArea().getType()) {
            case SANTA_BASE -> Area.Type.VILLAGE;
            case VILLAGE -> Area.Type.FOREST;
            case FOREST -> Area.Type.CHRISTMAS_TOWN;
            case CHRISTMAS_TOWN -> null;
        };
        return destination==null?null:areas.get(indexOf(destination));
    }
    public void updateUnlockedAreas() {
        if(story.isVillageCleared())areas.get(indexOf(Area.Type.FOREST)).unlock();
        if(story.isChristmasTownLeadUnlocked()&&activities.chapter.isComplete())areas.get(indexOf(Area.Type.CHRISTMAS_TOWN)).unlock();
    }
    public boolean isNextAreaAvailable() { Area next=getNextArea();return next!=null&&isConnectionAvailable(next.getType()); }
    public List<ExitZone> getCurrentExitZones() {
        WorldMap map=WorldMap.forArea(getCurrentArea().getType());
        List<ExitZone> result=new ArrayList<>();
        for(var entry:map.points().entrySet())if(entry.getKey().startsWith("exit/")) {
            Area.Type destination=entry.getKey().contains("santa_base")?Area.Type.SANTA_BASE:entry.getKey().contains("village")?Area.Type.VILLAGE:
                    entry.getKey().contains("forest")?Area.Type.FOREST:Area.Type.CHRISTMAS_TOWN;
            WorldMap.Point p=entry.getValue();boolean side=p.x()<32||p.x()>map.width()-32;
            double width=side?32:96,height=side?96:32;
            result.add(new ExitZone(p.x()-width/2,p.y()-height/2,width,height,destination,areas.get(indexOf(destination)).getName()));
        }
        return List.copyOf(result);
    }
    public boolean isConnectionAvailable(Area.Type destination) {
        updateUnlockedAreas();
        if(getCurrentArea().getType()==Area.Type.SANTA_BASE&&(!activities.presentsCollected()||activities.chapter.homeReturned()))return false;
        return areas.get(indexOf(destination)).isUnlocked()
                &&getCurrentExitZones().stream().anyMatch(g->g.getDestination()==destination);
    }
    public boolean tryTravel(Area.Type destination) {
        if(!isConnectionAvailable(destination)) return false;
        currentAreaIndex=indexOf(destination);
        return true;
    }
    public boolean hasOpeningPresents(){return activities.presentsCollected();}
    /** The guarded final journey uses the same area/loading/checkpoint lifecycle as gates. */
    public boolean returnHome(){
        if(getCurrentArea().getType()!=Area.Type.CHRISTMAS_TOWN||!story.isEndingTriggered()||!activities.chapter.celebrated()||activities.chapter.homeReturned())return false;
        if(!activities.chapter.returnHome())return false;
        currentAreaIndex=indexOf(Area.Type.SANTA_BASE);return true;
    }
    /** Destination entry marker, moved inward only if the actor overlaps a gate. */
    public WorldMap.Point arrivalFrom(Area.Type source) {
        WorldMap map=WorldMap.forArea(getCurrentArea().getType());
        if(source==Area.Type.SANTA_BASE && getCurrentArea().getType()==Area.Type.VILLAGE) {
            WorldMap.Point spawn=map.point("spawn");
            WorldMap.Point arrival=new WorldMap.Point(spawn.x()-16,spawn.y()-21);
            var footprint=new java.awt.geom.Rectangle2D.Double(arrival.x(),arrival.y()-8,32,50);
            if(util.Collision.intersectsAny(footprint,map.solids()) || getCurrentExitZones().stream().anyMatch(g->g.intersects(footprint)))
                throw new IllegalStateException("Unsafe workshop departure arrival");
            return arrival;
        }
        String sourceId=switch(source) {
            case SANTA_BASE -> "santa_base";
            case VILLAGE -> "village";
            case FOREST -> "forest";
            case CHRISTMAS_TOWN -> "town";
        };
        WorldMap.Point p=map.point("entry/from_"+sourceId);
        int x=p.x()-16, y=p.y()-21;
        for(int attempt=0;attempt<8;attempt++) {
            java.awt.geom.Rectangle2D bounds=new java.awt.geom.Rectangle2D.Double(x,y-8,32,50);
            boolean gateOverlap=getCurrentExitZones().stream().anyMatch(g->g.intersects(bounds));
            if(!gateOverlap && !util.Collision.intersectsAny(bounds,map.solids())) return new WorldMap.Point(x,y);
            if(p.x()<96) x+=32; else if(p.x()>map.width()-96) x-=32;
            else if(p.y()<96) y+=32; else y-=32;
        }
        throw new IllegalStateException("Unsafe arrival in "+map.id());
    }
    private int indexOf(Area.Type type) {
        for(int i=0;i<areas.size();i++) if(areas.get(i).getType()==type) return i;
        throw new IllegalArgumentException("Unknown area "+type);
    }
}
