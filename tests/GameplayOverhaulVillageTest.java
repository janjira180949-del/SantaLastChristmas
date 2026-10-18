import game.*;
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Actual map composition, actor clearance and gameplay captures. */
public class GameplayOverhaulVillageTest extends V2TestSupport {
    public static void main(String[] args)throws Exception {
        WorldMap map=WorldMap.forArea(Area.Type.VILLAGE);
        check(!map.points().containsKey("landmarks/Damaged Village Tree"),"Obsolete tree marker removed");
        check(map.point("landmarks/Village Square").equals(new WorldMap.Point(1296,976)),"Square landmark replaces tree");
        var marketTreeBase=new Rectangle2D.Double(1264,856,64,24);
        check(map.solids().contains(marketTreeBase),"Requested compact evergreen base is authoritative");
        for(int y=864;y<1056;y+=16)for(int x=1184;x<1376;x+=16) {
            var footprint=new Rectangle2D.Double(x,y,30,42);
            check(util.Collision.intersectsAny(footprint,map.solids())==marketTreeBase.intersects(footprint),
                "Old square remains walkable except the exact new market-tree base at "+x+","+y);
        }
        BufferedImage source=util.ImageAssets.load("assets/world/village_clean.png");
        BufferedImage composition=VillageEnvironmentLayers.build(source),again=VillageEnvironmentLayers.build(source);
        check(java.util.Arrays.equals(composition.getRGB(0,0,2560,1920,null,0,2560),again.getRGB(0,0,2560,1920,null,0,2560)),"Composition deterministic");
        check(composition.getRGB(1296,976)!=source.getRGB(1296,976),"Old tree artwork replaced");
        check(VillageAtmosphere.lampPositions().size()==10,"Square lamps replace obsolete tree lights");
        GamePanel panel=playing();panel.setSize(1280,800);
        Path out=Path.of("build/gameplay-overhaul/captures");Files.createDirectories(out);
        ImageIO.write(composition,"png",out.resolve("village-overview.png").toFile());
        for(int[] p:new int[][]{{1296,1080},{528,736},{2100,976}}) {
            santa(panel).setPosition(p[0],p[1]);tick(panel,1);
            ImageIO.write(render(panel),"png",out.resolve("village-"+p[0]+".png").toFile());
        }
        System.out.println("PASS: "+checks+" Village overhaul assertions; actual composition and gameplay captures");
    }
}
