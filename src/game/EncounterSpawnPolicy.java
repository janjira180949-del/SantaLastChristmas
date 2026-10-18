package game;

import entity.Enemy;
import entity.Santa;
import java.awt.geom.Rectangle2D;
import java.util.List;
import util.Collision;

/** Finite deterministic spawn search shared by authored groups and boss reinforcements. */
public final class EncounterSpawnPolicy {
    public static final int MIN_PLAYER_DISTANCE=128;
    private EncounterSpawnPolicy() { }
    public static int limit(Area.Type area) {return area==Area.Type.VILLAGE?4:6;}
    public static WorldMap.Point find(Area.Type area,int x,int y,int size,Santa santa,List<Enemy> enemies) {
        if(enemies.stream().filter(e->!e.isDefeated()).count()>=limit(area))return null;
        WorldMap map=WorldMap.forArea(area);
        for(int radius=0;radius<=384;radius+=32) {
            for(int dy=-radius;dy<=radius;dy+=32)for(int dx=-radius;dx<=radius;dx+=32) {
                if(Math.max(Math.abs(dx),Math.abs(dy))!=radius)continue;
                int px=x+dx,py=y+dy;double cx=px+size/2.0,cy=py+size/2.0;
                if(Math.hypot(cx-santa.getX()-16,cy-santa.getY()-21)<MIN_PLAYER_DISTANCE)continue;
                Rectangle2D bounds=new Rectangle2D.Double(px-8,py-8,size+16,size+16);
                if(px<48||py<48||px+size>map.width()-48||py+size>map.height()-48||Collision.intersectsAny(bounds,map.solids()))continue;
                boolean crowded=false;
                for(Enemy enemy:enemies)if(!enemy.isDefeated()&&Math.hypot(cx-enemy.getX()-enemy.getSize()/2.0,cy-enemy.getY()-enemy.getSize()/2.0)<64){crowded=true;break;}
                for(var marker:map.points().entrySet()) {
                    String key=marker.getKey();var p=marker.getValue();
                    int clearance=key.startsWith("npc/")?240:key.startsWith("items/")?150:key.startsWith("exit/")||key.startsWith("entry/")?100:0;
                    if(clearance>0&&Math.hypot(cx-p.x(),cy-p.y())<clearance){crowded=true;break;}
                }
                if(area==Area.Type.FOREST&&(Math.hypot(cx-720,cy-1168)<160||Math.hypot(cx-1664,cy-1280)<160))crowded=true;
                if(area==Area.Type.CHRISTMAS_TOWN&&Math.hypot(cx-1360,cy-880)<160)crowded=true;
                if(area==Area.Type.VILLAGE&&bounds.intersects(980,720,620,110))crowded=true;
                if(area==Area.Type.VILLAGE)for(var prop:VillageEnvironment.props())if(bounds.intersects(prop.bounds())){crowded=true;break;}
                if(!crowded)return new WorldMap.Point(px,py);
            }
        }
        return null; // Exhausted search is harmless; never fall back to an unsafe position.
    }
}
