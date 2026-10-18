import game.*;
import java.awt.geom.Rectangle2D;
import java.util.*;

/** Test-only flood fill checks full actor reachability; runtime AI remains simple. */
public class MapReachabilityTest extends V2TestSupport {
    public static void main(String[] args) {
        for(Area.Type type:Area.Type.values()) {
            WorldMap map=WorldMap.forArea(type);int cols=map.width()/16,rows=map.height()/16;
            boolean[] free=new boolean[cols*rows],visited=new boolean[cols*rows];
            var solids=map.solids();
            for(int row=0;row<rows;row++)for(int col=0;col<cols;col++) {
                int cx=col*16+8,cy=row*16+8;
                free[row*cols+col]=!util.Collision.intersectsAny(new Rectangle2D.Double(cx-16,cy-29,32,50),solids);
            }
            WorldMap.Point spawn=map.point("spawn");int start=-1;
            for(int i=0;i<free.length;i++)if(free[i]&&Math.hypot(i%cols*16+8-spawn.x(),i/cols*16+8-spawn.y())<32){start=i;break;}
            check(start>=0,"Safe spawn for "+type);ArrayDeque<Integer> queue=new ArrayDeque<>();queue.add(start);visited[start]=true;
            while(!queue.isEmpty()) {
                int i=queue.remove(),col=i%cols,row=i/cols;
                for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                    int nc=col+d[0],nr=row+d[1];if(nc<0||nc>=cols||nr<0||nr>=rows)continue;
                    int ni=nr*cols+nc;if(!free[ni]||visited[ni])continue;
                    boolean swept=true;
                    for(int step=1;step<=16;step++)if(util.Collision.intersectsAny(new Rectangle2D.Double(col*16+8+d[0]*step-16,row*16+8+d[1]*step-29,32,50),solids)){swept=false;break;}
                    if(swept){visited[ni]=true;queue.add(ni);}
                }
            }
            List<WorldMap.Point> goals=new ArrayList<>();
            for(var point:map.points().entrySet())if(point.getKey().startsWith("entry/")||point.getKey().startsWith("exit/")||point.getKey().startsWith("npc/")||point.getKey().startsWith("items/"))goals.add(point.getValue());
            EncounterManager encounters=new EncounterManager();for(var encounter:encounters.getEncounters())if(encounter.getArea()==type)goals.add(encounter.getCenter());
            if(type==Area.Type.FOREST){goals.add(new WorldMap.Point(720,1168));goals.add(new WorldMap.Point(1664,1280));}
            if(type==Area.Type.CHRISTMAS_TOWN)for(var rest:HealingPoint.townRestPoints())goals.add(rest.getPosition());
            if(type==Area.Type.CHRISTMAS_TOWN){goals.add(new WorldMap.Point(1376,419));goals.add(new WorldMap.Point(1296,1136));}
            for(var goal:goals) {
                boolean treeEncounter=type==Area.Type.CHRISTMAS_TOWN&&goal.equals(encounters.get("boss").getCenter());
                // The approved emergence anchor is inside the unchanged Tree solid. Santa triggers
                // this encounter at 180px, then the existing policy selects a clear boss footprint.
                int approach=treeEncounter?180:58;
                if(treeEncounter){
                    var hero=new entity.Santa(1280,1115);
                    var origin=EncounterSpawnPolicy.find(type,goal.x()-48,goal.y()-48,96,hero,new ArrayList<>());
                    check(origin!=null&&!util.Collision.intersectsAny(new Rectangle2D.Double(origin.x(),origin.y(),96,96),solids),"Full Titan footprint remains collision-clear beside the Tree");
                    check(Math.hypot(origin.x()+48-goal.x(),origin.y()+48-goal.y())<=256,"Safe Titan spawn stays adjacent to the Tree, not the retired south arena");
                }
                boolean reachable=false;
                for(int i=0;i<visited.length&&!reachable;i++)if(visited[i]&&Math.hypot(i%cols*16+8-goal.x(),i/cols*16+8-goal.y())<approach)reachable=true;
                check(reachable,"Collision-safe route in "+type+" to "+goal);
            }
            System.out.println("PASS: full actor routes in "+type+" to "+goals.size()+" authored gates, NPCs and objectives");
        }
    }
}
