import game.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public class WorldAtmosphereV22Test extends V2TestSupport {
    static BufferedImage weather(WorldAtmosphere fx,Area.Type area,int cx) {
        BufferedImage image=new BufferedImage(600,400,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        Camera camera=new Camera();WorldMap map=WorldMap.forArea(area);camera.follow(cx+300,700,600,400,map.width(),map.height());
        g.translate(-camera.getX(),-camera.getY());fx.drawDepth(g,area,map,camera,600,400);fx.drawLights(g,area,camera,600,400);fx.drawSnow(g,area,map,camera,600,400);g.dispose();return image;
    }
    public static void main(String[] args) {
        WorldAtmosphere fx=new WorldAtmosphere();
        for(Area.Type area:new Area.Type[]{Area.Type.FOREST,Area.Type.VILLAGE,Area.Type.CHRISTMAS_TOWN}) {
            var map=WorldMap.forArea(area);var points=java.util.Map.copyOf(map.points());var solids=map.solids().toString();
            var a=weather(fx,area,500);var b=weather(fx,area,600);
            check(Arrays.equals(a.getRGB(100,0,500,400,null,0,500),b.getRGB(0,0,500,400,null,0,500)),"Weather and lights retain world anchors during camera scrolling");
            check(Arrays.equals(a.getRGB(0,0,600,400,null,0,600),weather(fx,area,500).getRGB(0,0,600,400,null,0,600)),"Rendering does not advance weather");
            check(fx.particleBudget(area)<=192,"Weather particle budget bounded");
            for(int i=0;i<30;i++)fx.update();check(!Arrays.equals(a.getRGB(0,0,600,400,null,0,600),weather(fx,area,500).getRGB(0,0,600,400,null,0,600)),"Weather moves with update clock");
            check(points.equals(map.points())&&solids.equals(map.solids().toString()),"Effects never change navigation");
        }
        System.out.println("PASS: "+checks+" deterministic weather/world-space checks");
    }
}
