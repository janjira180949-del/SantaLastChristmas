import game.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

/** Run from an empty working directory to exercise real missing-file preparation. */
public class Phase92FallbackProbe {
    public static void main(String[] args)throws Exception {
        VillageRestorationVisuals.prepareAsync().get(30,TimeUnit.SECONDS);var chapter=new FinalChapter();
        var image=new BufferedImage(2560,1920,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        VillageRestorationVisuals.drawHouse(g,chapter);VillageRestorationVisuals.drawTree(g,chapter);VillageRestorationVisuals.drawMarket(g,chapter);g.dispose();
        for(var r:new java.awt.Rectangle[]{VillageRestorationVisuals.HOUSE_BOUNDS,VillageRestorationVisuals.TREE_BOUNDS,VillageRestorationVisuals.MARKET_BOUNDS}){
            int opaque=0;for(int y=r.y;y<r.y+r.height;y++)for(int x=r.x;x<r.x+r.width;x++)if((image.getRGB(x,y)>>>24)>0)opaque++;
            if(opaque<100)throw new AssertionError("Missing landmark fallback "+r);
        }
        System.out.println("PASS: missing optional art retains visible safety landmarks");
    }
}
