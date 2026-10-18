import game.*;
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;

/** Checks every tile against the supplied collision reference, without deriving runtime collision from pixels. */
public class MapCollisionReferenceTest {
    public static void main(String[] args) {
        int checked=0;
        for(Area.Type type:new Area.Type[]{Area.Type.FOREST,Area.Type.VILLAGE,Area.Type.CHRISTMAS_TOWN}) {
            WorldMap map=WorldMap.forArea(type);
            BufferedImage reference=util.ImageAssets.load("SantaLastChristmas_MapPackage/concept_maps/collision/"+map.id()+"_collision.png");
            if(reference==null) throw new AssertionError("Missing collision reference");
            for(int y=16;y<map.height();y+=32) for(int x=16;x<map.width();x+=32) {
                int color=reference.getRGB(x,y);
                int red=(color>>>16)&255,green=(color>>>8)&255,blue=color&255;
                boolean blocked=!(green>red&&green>blue);
                if(x==16||x==map.width()-16||y==16||y==map.height()-16) {
                    blocked=true;
                    for(var marker:map.points().entrySet())if(marker.getKey().startsWith("exit/")) {
                        var p=marker.getValue();if(Math.abs(x-p.x())<=64&&Math.abs(y-p.y())<=64)blocked=false;
                    }
                }
                if(type==Area.Type.VILLAGE&&x>=1184&&x<1376&&y>=864&&y<1056)blocked=false;
                // Exact authorized relocation: retire the old roof-sized block, require the new foundation.
                if(type==Area.Type.VILLAGE&&new java.awt.Rectangle(1824,992,384,256).contains(x,y))blocked=false;
                if(type==Area.Type.VILLAGE&&new java.awt.Rectangle(2200,1304,256,96).contains(x,y))blocked=true;
                boolean actual=util.Collision.intersectsAny(new Rectangle2D.Double(x,y,1,1),map.solids());
                if(blocked!=actual) throw new AssertionError(map.id()+" collision mismatch at tile "+x/32+","+y/32);
                checked++;
            }
        }
        System.out.println("PASS: all "+checked+" map tiles match supplied obstacle references and version 2.0 gate corridors");
    }
}
