package game;

import entity.Enemy;
import entity.Santa;
import java.util.List;
import java.util.function.Predicate;
import static game.Encounter.EnemyKind.*;

/** Fixed waves, serialized activation and persistent completion across map travel. */
public final class EncounterManager {
    private final List<Encounter> encounters=List.of(
        new Encounter("village",Area.Type.VILLAGE,new WorldMap.Point(2100,976),WALKER,WALKER),
        new Encounter("crystal-1",Area.Type.FOREST,WorldMap.forArea(Area.Type.FOREST).point("landmarks/Crystal 1"),WALKER,WALKER).warned(),
        new Encounter("crystal-2",Area.Type.FOREST,WorldMap.forArea(Area.Type.FOREST).point("landmarks/Crystal 2"),WALKER,RUNNER,RUNNER).withWave(WALKER,WALKER,RUNNER).warned(),
        new Encounter("crystal-3",Area.Type.FOREST,WorldMap.forArea(Area.Type.FOREST).point("landmarks/Crystal 3"),WALKER,WALKER,RUNNER,RUNNER).withWave(WALKER,WALKER,RUNNER).warned(),
        new Encounter("town",Area.Type.CHRISTMAS_TOWN,new WorldMap.Point(1296,640),WALKER,WALKER,RUNNER).withWave(WALKER,RUNNER).warned(),
        new Encounter("boss",Area.Type.CHRISTMAS_TOWN,WorldMap.forArea(Area.Type.CHRISTMAS_TOWN).point("landmarks/Corrupted Town Center"),DARK_TITAN),
        new Encounter("frost-fang",Area.Type.FOREST,boss.FrostFang.ARENA,FROST_FANG).warned(),
        new Encounter("village-outskirts",Area.Type.VILLAGE,new WorldMap.Point(1660,1620),WALKER,WALKER,RUNNER).warned(),
        new Encounter("forest-road",Area.Type.FOREST,new WorldMap.Point(624,1460),WALKER,WALKER,RUNNER).warned(),
        new Encounter("forest-clearing",Area.Type.FOREST,new WorldMap.Point(1530,740),WALKER,RUNNER,RUNNER).warned(),
        new Encounter("town-side-road",Area.Type.CHRISTMAS_TOWN,new WorldMap.Point(1700,660),WALKER,RUNNER).warned(),
        new Encounter("forest-camp",Area.Type.FOREST,new WorldMap.Point(1160,920),WALKER,WALKER,WALKER).warned(),
        new Encounter("forest-ambush",Area.Type.FOREST,ForestEnvironmentLayers.DARK_CLEARING,WALKER,WALKER,RUNNER,RUNNER).warned(),
        new Encounter("forest-exit",Area.Type.FOREST,new WorldMap.Point(2770,1210),WALKER,WALKER,RUNNER).warned());
    private Encounter active;
    public List<Encounter> getEncounters() { return encounters; }
    public Encounter get(String id) { return encounters.stream().filter(e->e.getId().equals(id)).findFirst().orElseThrow(); }
    public Encounter getActive() { return active; }
    public void advance(Santa santa,List<Enemy> enemies){if(active!=null)active.advance(santa,enemies);}
    public void leaveArea(List<Enemy> enemies){if(active!=null)active.abandon(enemies);active=null;enemies.clear();}
    public boolean isComplete(String id) { return get(id).getState()==Encounter.State.COMPLETE; }
    public void startNearby(Area.Type area,Santa santa,List<Enemy> enemies,Predicate<Encounter> allowed) {
        if (active != null || santa.isDefeated()) return;
        for(Encounter encounter:encounters) {
            if(encounter.getArea()==area && encounter.getState()==Encounter.State.READY
                    && encounter.isNear(santa) && allowed.test(encounter)) {
                encounter.start(santa,enemies); active=encounter; break;
            }
        }
    }
    public Encounter finishActive() {
        if (active==null || !active.finishIfCleared()) return null;
        Encounter completed=active; active=null; return completed;
    }
    public boolean retry(Santa santa,List<Enemy> enemies) { return active!=null && active.retry(santa,enemies); }
}
