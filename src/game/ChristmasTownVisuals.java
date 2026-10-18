package game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import util.ImageAssets;
import util.LightBloom;
import util.PixelArt;

/** Cached Town illustration and state-aware light/weather. Never owns gameplay state. */
public final class ChristmasTownVisuals {
    public static final String RESTORED_TREE="assets/world/spirit_tree/restored_spirit_tree.png";
    private BufferedImage cold,restored,tree;
    private int treeFeet;
    private int[][] windows;
    private final TownEnvironmentLayers environment=new TownEnvironmentLayers();
    private final WorldMap map=WorldMap.forArea(Area.Type.CHRISTMAS_TOWN);
    private final WorldMap.Point root=map.point("landmarks/Corrupted Town Center");
    private static final Color SNOW=new Color(231,240,249,145),FROST=new Color(147,199,223,100);
    private static final Color[] MOTES=new Color[151];
    static {for(int i=0;i<MOTES.length;i++)MOTES[i]=new Color(255,225,157,i);}
    private static final int[][] LIGHTS={{912,800},{1680,800},{912,1488},{1680,1488},
        {1960,1250},{430,1135},{920,240},{1715,195},{2025,310},{380,400},
        {2048,1198},{2178,1264},{372,1146},{501,1189}};
    public synchronized void prepare() {
        if(cold!=null)return;
        BufferedImage source=ImageAssets.load("assets/world/christmas_town_corrupted_clean.png");
        TownChapterArt.prepareAsync().join();
        BufferedImage supplied=TownChapterArt.tree(2);
        if(source==null||supplied==null)throw new IllegalStateException("Missing Town or restored Spirit Tree artwork");
        tree=PixelArt.scale(supplied,384,Math.max(1,384*supplied.getHeight()/supplied.getWidth()));
        for(int y=0;y<tree.getHeight();y++)for(int x=0;x<tree.getWidth();x++)
            if((tree.getRGB(x,y)>>>24)>20)treeFeet=y+1;
        var layers=environment.build(source);restored=layers.restored();
        // Locate actual window/decor emissive pixels once, rather than invented lighting positions.
        var anchors=new ArrayList<int[]>();
        for(int y=0;y<source.getHeight();y+=96)for(int x=0;x<source.getWidth();x+=96) {
            int count=0,sx=0,sy=0;
            for(int py=y;py<Math.min(y+96,source.getHeight());py+=3)for(int px=x;px<Math.min(x+96,source.getWidth());px+=3) {
                int color=source.getRGB(px,py),r=(color>>>16)&255,g=(color>>>8)&255,b=color&255;
                if(r>185&&g>90&&g<220&&b<110&&r>g*1.18){count++;sx+=px;sy+=py;}
            }
            if(count>=10&&anchors.size()<48)anchors.add(new int[]{sx/count,sy/count});
        }
        windows=anchors.toArray(int[][]::new);cold=layers.cold();
    }
    public void drawTerrain(Graphics2D g,boolean isRestored){prepare();g.drawImage(isRestored?restored:cold,0,0,null);}
    public void drawTree(Graphics2D original,boolean isRestored) {
        if(!isRestored)return;prepare();Graphics2D g=(Graphics2D)original.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        TownChapterArt.drawRestoredTree(g,root);g.dispose();
    }
    private static boolean visible(Rectangle clip,int x,int y,int radius) {
        return clip==null||clip.intersects(x-radius,y-radius,radius*2,radius*2);
    }
    public void drawGroundLights(Graphics2D original,boolean isRestored,int ticks) {
        Graphics2D g=(Graphics2D)original.create();Rectangle clip=g.getClipBounds();
        for(int i=0;i<LIGHTS.length;i++) {
            if(!visible(clip,LIGHTS[i][0],LIGHTS[i][1]+42,150))continue;
            double pulse=.96+.04*Math.sin(ticks/43.0+i*1.7);
            LightBloom.draw(g,LIGHTS[i][0],LIGHTS[i][1]+42,150,(isRestored?.26:.09)*pulse,LightBloom.GOLD);
        }
        g.dispose();
    }
    public void drawForeground(Graphics2D original,boolean isRestored,int ticks) {
        prepare();Graphics2D g=(Graphics2D)original.create();Rectangle clip=g.getClipBounds();
        for(int i=0;i<LIGHTS.length;i++) {
            if(!visible(clip,LIGHTS[i][0],LIGHTS[i][1],65))continue;
            double pulse=.96+.04*Math.sin(ticks/43.0+i*1.7);
            LightBloom.draw(g,LIGHTS[i][0],LIGHTS[i][1],65,(isRestored?.28:.10)*pulse,LightBloom.GOLD);
        }
        for(int i=0;i<windows.length;i++) {
            if(!visible(clip,windows[i][0],windows[i][1],42))continue;
            LightBloom.draw(g,windows[i][0],windows[i][1],42,isRestored?.16:.05,LightBloom.GOLD);
        }
        // Small chimney wisps stay inside each established building footprint.
        for(int i=0;i<TownChapterArt.HOMES.length;i++){
            int[] h=TownChapterArt.HOMES[i];int cx=(h[0]+h[2])/2+18,cy=h[1]+65;
            if(!visible(clip,cx,cy,24))continue;
            for(int puff=0;puff<3;puff++){
                int age=Math.floorMod(ticks/3+puff*17+i*11,60);
                if(age>=38)continue;
                int x=cx+(int)(Math.sin(age/13.0+i)*5),y=cy-age;
                g.setColor(new Color(192,210,224,Math.max(0,35-age/2)));
                g.fillRect(x,y,3+age/12,2+age/15);
            }
        }
        if(isRestored) {
            LightBloom.draw(g,root.x(),root.y()-94,100,.10+.015*Math.sin(ticks/65.0),LightBloom.GOLD);
            for(int i=0;i<20;i++) {
                double age=((ticks+i*17)%180)/180.0;
                int x=root.x()+(int)(Math.sin(i*2.4+age*1.8)*(40+i%5*16));
                int y=root.y()+30-(int)(age*230);
                g.setColor(MOTES[(int)(Math.sin(age*Math.PI)*150)]);g.fillRect(x,y,2,2);
            }
        }
        g.dispose();
    }
    /** Ordinary snow after restoration; cold frost flakes before. No new simulation clock. */
    public void drawSnow(Graphics2D original,boolean isRestored,int ticks,Camera camera,int width,int height) {
        Graphics2D g=(Graphics2D)original.create();g.setColor(isRestored?SNOW:FROST);
        for(int i=0;i<112;i++) {
            int x=(int)((i*431+ticks*.19+Math.sin(ticks/160.0+i)*8)%map.width());
            int y=(int)((i*307+ticks*(i%3==0?.42:.25))%map.height());
            if(x<camera.getX()-3||x>camera.getX()+width||y<camera.getY()-3||y>camera.getY()+height)continue;
            g.fillRect(x,y,i%4==0?2:1,i%4==0?2:1);
        }
        g.dispose();
    }
}
