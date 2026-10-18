package game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;

/** Village-only dusk lighting; cached glows, independent of terrain/gameplay data. */
public final class VillageAtmosphere {
    private static final BufferedImage HALO=glow(144,48);
    private static final BufferedImage LANTERN=glow(48,150);
    private static final BufferedImage COLD_HALO=coldGlow(144,40),COLD_LANTERN=coldGlow(48,135);
    private static BufferedImage coldGlow(int size,int alpha){
        var image=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();float r=size/2f;
        g.setPaint(new RadialGradientPaint(r,r,r,new float[]{0,.3f,1},new Color[]{new Color(131,189,234,alpha),new Color(87,143,207,alpha/2),new Color(54,93,157,0)}));g.fillRect(0,0,size,size);g.dispose();return image;
    }
    private static List<Point> lamps;
    private VillageAtmosphere() { }
    private static BufferedImage glow(int size,int alpha){
        var image=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();float radius=size/2f;
        g.setPaint(new RadialGradientPaint(radius,radius,radius,new float[]{0,.22f,.58f,1},
                new Color[]{new Color(255,211,131,alpha),new Color(255,200,111,alpha*3/4),new Color(245,182,93,alpha/4),new Color(245,182,93,0)}));
        g.fillRect(0,0,size,size);g.dispose();return image;
    }
    public static synchronized List<Point> lampPositions(){
        if(lamps==null){var list=new ArrayList<Point>();
            // Original map-data lamp centers: tile center x, feet y minus lantern height.
            list.add(new Point(912,840));list.add(new Point(1712,840));list.add(new Point(1296,1448));
            for(var prop:VillageEnvironment.props())if(prop.name().equals("warm_lamp"))
                list.add(new Point(prop.x()+prop.image().getWidth()/2,prop.y()-prop.image().getHeight()+32));
            for(int[] p:VillageEnvironmentLayers.LAMPS)list.add(new Point(p[0],p[1]));
            lamps=List.copyOf(list);
        }return lamps;
    }
    public static void draw(Graphics2D graphics,int cameraX,int cameraY,int width,int height,int brightness){
        draw(graphics,cameraX,cameraY,width,height,brightness,null);
    }
    public static void draw(Graphics2D graphics,int cameraX,int cameraY,int width,int height,int brightness,VillageActivities activities){
        if(brightness==100)return;
        var g=(Graphics2D)graphics.create();
        g.setColor(new Color(24,43,64,Math.round(255*(1-brightness/100f))));
        g.fillRect(cameraX,cameraY,width,height);
        for(var lamp:lampPositions()){
            int index=lamp.x==912&&lamp.y==840?0:lamp.x==1712&&lamp.y==840?1:lamp.x==1296&&lamp.y==1448?2:-1;
            boolean cold=activities!=null&&index>=0&&!activities.lantern(index);
            g.drawImage(cold?COLD_HALO:HALO,lamp.x-72,lamp.y-72,null);
            g.drawImage(cold?COLD_HALO:HALO,lamp.x-72,lamp.y+5,null);
            g.drawImage(cold?COLD_LANTERN:LANTERN,lamp.x-24,lamp.y-24,null);
        }
        g.dispose();
    }
}
