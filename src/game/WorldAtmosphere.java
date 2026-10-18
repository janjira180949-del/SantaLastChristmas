package game;

import java.awt.*;
import java.awt.image.BufferedImage;
import util.ImageAssets;
import util.LightBloom;

/** Deterministic, bounded world-space weather and artwork-derived window lighting. */
public final class WorldAtmosphere {
    private int ticks;
    private final double[][][] lights=new double[3][][];
    private static final BufferedImage MIST=mist();
    private static final Color SNOW_NEAR=new Color(226,237,248,130),SNOW_FAR=new Color(226,237,248,85),FROST=new Color(162,210,227,90);
    public int getTicks() {return ticks;}
    public void update() {ticks=(ticks+1)%360000;}
    public void reset() {ticks=0;}
    public int particleBudget(Area.Type area) {return area==Area.Type.SANTA_BASE?0:area==Area.Type.VILLAGE?192:area==Area.Type.FOREST?160:112;}
    private static BufferedImage mist() {
        BufferedImage image=new BufferedImage(320,64,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();
        for(int y=0;y<64;y++)for(int x=0;x<320;x+=4) {
            double edge=Math.sin(Math.PI*x/320)*Math.sin(Math.PI*y/64);
            g.setColor(new Color(160,203,217,(int)(edge*edge*12)));g.fillRect(x,y,4,1);
        }
        g.dispose();return image;
    }
    private void prepare(Area.Type area) {
        if(lights[area.ordinal()]!=null)return;
        if(area==Area.Type.VILLAGE){lights[area.ordinal()]=VillageRestorationVisuals.windowLights();return;}
        String name=area==Area.Type.VILLAGE?"village":area==Area.Type.FOREST?"forest":"christmas_town_corrupted";
        BufferedImage art=ImageAssets.load("assets/world/"+name+"_clean.png");
        double[][] anchors=new double[48][2];int count=0;
        // Scan actual orange emissive artwork, not filenames or invented decorations.
        for(int y=0;y<art.getHeight()&&count<48;y+=96)for(int x=0;x<art.getWidth()&&count<48;x+=96) {
            int pixels=0,sumX=0,sumY=0;
            for(int py=y;py<Math.min(y+96,art.getHeight());py+=3)for(int px=x;px<Math.min(x+96,art.getWidth());px+=3) {
                if(area==Area.Type.VILLAGE&&px>=1050&&px<1540&&py>=770&&py<1240)continue;
                int rgb=art.getRGB(px,py),r=(rgb>>>16)&255,green=(rgb>>>8)&255,b=rgb&255;
                if(r>175&&green>85&&green<205&&r>green*1.2&&b<75){pixels++;sumX+=px;sumY+=py;}
            }
            if(pixels>=9){anchors[count][0]=sumX/(double)pixels;anchors[count++][1]=sumY/(double)pixels;}
        }
        lights[area.ordinal()]=java.util.Arrays.copyOf(anchors,count);
    }
    public void drawDepth(Graphics2D original,Area.Type area,WorldMap map,Camera camera,int width,int height) {
        Graphics2D g=(Graphics2D)original.create();
        try {
            if(area==Area.Type.CHRISTMAS_TOWN){g.setColor(new Color(9,19,39,28));g.fillRect(camera.getX(),camera.getY(),width,height);}
            if(area==Area.Type.FOREST||area==Area.Type.CHRISTMAS_TOWN) {
                for(int i=0;i<8;i++) {
                    int x=(int)((i*417+ticks*.13)%map.width())-160,y=420+(i*233)%(map.height()-600);
                    g.drawImage(MIST,x,y,480,72,null);
                }
            }
        } finally {g.dispose();}
    }
    public void drawLights(Graphics2D original,Area.Type area,Camera camera,int width,int height) {
        drawLights(original,area,camera,width,height,null);
    }
    public void drawLights(Graphics2D original,Area.Type area,Camera camera,int width,int height,FinalChapter chapter) {
        if(area==Area.Type.SANTA_BASE)return; // Workshop lighting has its own bounded presentation owner.
        prepare(area);Graphics2D g=(Graphics2D)original.create();
        try {
            double[][] anchors=lights[area.ordinal()];
            if(anchors==null)return; // Pending asynchronous Village preparation is never awaited by painting.
            for(int i=0;i<anchors.length;i++) {
                double x=anchors[i][0],y=anchors[i][1];
                if(area==Area.Type.VILLAGE&&(VillageRestorationVisuals.HOUSE_BOUNDS.contains(x,y)||VillageRestorationVisuals.replacedLamp(x,y)))continue;
                if(x<camera.getX()-60||x>camera.getX()+width+60||y<camera.getY()-60||y>camera.getY()+height+60)continue;
                double pulse=.9+.1*Math.sin(ticks/75.0+i*1.7);
                double strength=area==Area.Type.CHRISTMAS_TOWN?(i%3==0?.08:.025):area==Area.Type.FOREST?.035:.12;
                if(area==Area.Type.VILLAGE)strength*=VillageBuildingVisuals.emission(x,y,chapter);
                LightBloom.draw(g,x,y,32,strength*pulse,LightBloom.GOLD);
            }
            if(area==Area.Type.CHRISTMAS_TOWN) {
                WorldMap.Point tree=WorldMap.forArea(area).point("landmarks/Corrupted Town Center");
                LightBloom.draw(g,tree.x(),tree.y()+25,86,.20+.03*Math.sin(ticks/60.0),LightBloom.CYAN);
            }
        } finally {g.dispose();}
    }
    public void drawSnow(Graphics2D original,Area.Type area,WorldMap map,Camera camera,int width,int height) {
        Graphics2D g=(Graphics2D)original.create();
        try {
            for(int i=0;i<particleBudget(area);i++) {
                int x=(int)((i*431+ticks*.19+Math.sin(ticks/160.0+i)*8)%map.width());
                int y=(int)((i*307+ticks*(i%3==0?.42:.25))%map.height());
                if(x<camera.getX()-3||x>camera.getX()+width||y<camera.getY()-3||y>camera.getY()+height)continue;
                g.setColor(area==Area.Type.CHRISTMAS_TOWN?FROST:i%3==0?SNOW_NEAR:SNOW_FAR);
                g.fillRect(x,y,i%4==0?2:1,i%4==0?2:1);
            }
        } finally {g.dispose();}
    }
}
