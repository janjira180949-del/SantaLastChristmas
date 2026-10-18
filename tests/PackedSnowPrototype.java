import game.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Same-camera Village reference/current capture using the retained material pipeline. */
public class PackedSnowPrototype {
    static BufferedImage scene()throws Exception { return FinalEnvironmentReview.scene(0,2010,650,800,600); }
    public static void main(String[] args)throws Exception {
        File dir=new File("build/packed-road-prototype");dir.mkdirs();
        WorldMap map=WorldMap.forArea(Area.Type.VILLAGE);var cache=WorldMap.class.getDeclaredField("polished");cache.setAccessible(true);Object previous=cache.get(map);
        BufferedImage old;
        try {cache.set(map,util.ImageAssets.load("assets/world/village_clean.png"));old=scene();}finally{cache.set(map,previous);}
        BufferedImage next=scene();ImageIO.write(old,"png",new File(dir,"01-source-reference.png"));ImageIO.write(next,"png",new File(dir,"02-v2-gameplay.png"));
        BufferedImage comparison=new BufferedImage(1600,634,BufferedImage.TYPE_INT_ARGB);var g=comparison.createGraphics();
        g.setColor(new Color(22,34,45));g.fillRect(0,0,1600,634);g.setColor(Color.WHITE);g.drawString("SOURCE ART",20,24);g.drawString("V2 GAMEPLAY",820,24);g.drawImage(old,0,34,null);g.drawImage(next,800,34,null);g.dispose();
        ImageIO.write(comparison,"png",new File(dir,"03-comparison.png"));System.out.println("PASS: three same-camera Village reference/current captures");
    }
}
