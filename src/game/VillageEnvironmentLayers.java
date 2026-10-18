package game;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import util.ImageAssets;
import util.PixelArt;

/** Cached Village illustration; decoration remains outside the open fighting floor. */
public final class VillageEnvironmentLayers {
    private static final int[] TERRAIN={0xff9eb8ce,0xffebf4f5,0xffd0d9d9,0xff81919f,0xfff6efd9,0xffeaeee3};
    private VillageEnvironmentLayers() { }
    public static final Rectangle SQUARE = new Rectangle(856,730,880,590);
    public static final int[][] LAMPS={{940,840},{1640,840},{940,1230},{1640,1230}};
    private static final int[][] BAKED_LAMPS={{912,944},{1712,944},{1296,1552}};
    private static BufferedImage required(String path) {
        BufferedImage art=ImageAssets.load(path);
        if(art==null)throw new IllegalStateException("Missing Village illustration: "+path);
        return art;
    }
    private static int grade(int color) {
        int r=(color>>>16)&255,g=(color>>>8)&255,b=color&255;
        if(r>180&&g>85&&g<225&&b<125&&r>g*1.16)return color;
        return 0xff000000|((r*79/100)<<16)|((g*86/100)<<8)|(b*98/100);
    }
    public static BufferedImage build(BufferedImage source) {return build(source,null);}
    public static BufferedImage build(BufferedImage source,BufferedImage houseMask) {return build(source,houseMask,null);}
    public static BufferedImage build(BufferedImage source,BufferedImage houseMask,BufferedImage lampMask) {
        return build(source,houseMask,lampMask,null);
    }
    static BufferedImage build(BufferedImage source,BufferedImage houseMask,BufferedImage lampMask,VillageBuildingVisuals.Layout buildings) {
        int w=source.getWidth(),h=source.getHeight();
        BufferedImage snow=required("assets/world/ground/snow_base.png");
        BufferedImage plaza=required(TownEnvironmentLayers.PLAZA);
        BufferedImage paving=PixelArt.scale(plaza.getSubimage(580,400,376,240),160,100);
        BufferedImage result=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
            int c=snow.getRGB(x%snow.getWidth(),y%snow.getHeight());
            int r=(c>>>16)&255,g=(c>>>8)&255,b=c&255;
            result.setRGB(x,y,0xff000000|((r*60/100)<<16)|((g*70/100)<<8)|(b*86/100));
        }
        Graphics2D g=result.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new TexturePaint(paving,new Rectangle(0,0,160,100)));
            g.setStroke(new BasicStroke(82,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            // Connected curved road loop and branches follow the original navigable corridors.
            Path2D loop=new Path2D.Double();loop.moveTo(560,720);loop.curveTo(950,590,1580,570,1920,720);
            loop.curveTo(2130,920,2080,1330,1900,1530);loop.curveTo(1500,1620,950,1690,590,1580);
            loop.curveTo(420,1420,440,1060,560,720);g.draw(loop);
            Path2D east=new Path2D.Double();east.moveTo(550,900);east.curveTo(900,930,1080,970,1296,1025);
            east.curveTo(1680,1025,2110,976,2544,976);g.draw(east);
            for(int[] branch:new int[][]{{560,720,570,605},{880,650,1030,535},{1780,670,1950,520},{640,1530,650,1480},
                    {2020,1440,2110,1390},{1050,610,1070,525},{1970,1560,2080,1760}})
                g.drawLine(branch[0],branch[1],branch[2],branch[3]);
            // Existing illustrated plaza, with its low snow rim and textured stone center.
            g.drawImage(PixelArt.scale(plaza,SQUARE.width,SQUARE.height),SQUARE.x,SQUARE.y,null);
            BufferedImage objects=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                int c=source.getRGB(x,y);
                if(TownEnvironmentLayers.terrain(c)||villageTerrain(c))continue;
                // Audited baked tree, roots, shadow and ornament ring only.
                if(x>=1050&&x<1540&&y>=770&&y<1240)continue;
                if(houseMask!=null&&x>=2057&&x<2327&&y>=1484&&y<1744&&(houseMask.getRGB(x-2057,y-1484)>>>24)>0)continue;
                if(lampMask!=null&&bakedLampPixel(x,y,lampMask))continue;
                if(buildings!=null&&buildings.removesPixel(x,y))continue;
                objects.setRGB(x,y,grade(c));
            }
            g.drawImage(objects,0,0,null);
            for(int[] lamp:LAMPS)detail(g,"warm_lamp",lamp[0]-26,lamp[1]+55,52,100);
            detail(g,"bench",980,1290,95,54);detail(g,"bench",1500,1290,95,54);
            if(houseMask==null)stall(g,1010,820,true);
            if(buildings==null)stall(g,1440,820,false);
            // Facade details stay in existing solid footprints, keeping doors and routes clear.
            for(int[] home:new int[][]{{352,256,704,576},{1824,160,2240,480},{1824,992,2208,1248},
                    {288,1216,640,1536},{928,224,1248,512},{2048,1504,2336,1824}}) {
                if(buildings!=null&&home[0]==1824&&home[1]==992)home=new int[]{home[0]+326,home[1]+200,home[2]+326,home[3]+200};
                detail(g,"wooden_fence",home[0]+12,home[3]-12,105,42);
                detail(g,"winter_bush",home[2]-68,home[3]-8,62,45);
                detail(g,"present",home[0]+123,home[3]-17,25,25);
                BufferedImage banner=PixelArt.scale(required(TownEnvironmentLayers.BANNER),48,72);
                g.drawImage(banner,home[2]-95,home[1]+135,null);
            }
        } finally {g.dispose();}
        return result;
    }
    private static boolean bakedLampPixel(int x,int y,BufferedImage mask){
        for(var site:BAKED_LAMPS){
            int px=x-(site[0]-27),py=y-(site[1]-130);
            if(px>=0&&px<54&&py>=0&&py<130&&(mask.getRGB(px,py)>>>24)>0)return true;
        }return false;
    }
    private static boolean villageTerrain(int color) {
        int r=(color>>>16)&255,g=(color>>>8)&255,b=color&255;
        for(int base:TERRAIN)
            if(Math.abs(r-((base>>>16)&255))<=3&&Math.abs(g-((base>>>8)&255))<=3&&Math.abs(b-(base&255))<=3)return true;
        return false;
    }
    private static void detail(Graphics2D g,String name,int x,int foot,int w,int h) {
        g.drawImage(PixelArt.scale(required("assets/world/village_props/"+name+".png"),w,h),x,foot-h,null);
    }
    private static void stall(Graphics2D g,int x,int foot,boolean stocked) {
        // Small pixel-painted wooden counters and faded canvas reuse the settlement palette.
        g.setColor(new Color(17,26,40,90));g.fillOval(x-10,foot-8,170,28);
        g.setColor(new Color(76,53,48));g.fillRect(x+10,foot-85,5,84);g.fillRect(x+137,foot-85,5,84);
        g.setPaint(new TexturePaint(required("assets/world/village_props/bench.png").getSubimage(40,40,40,18),new Rectangle(x+8,foot-40,80,36)));g.fillRect(x+8,foot-40,136,36);
        g.setColor(new Color(201,182,143));g.fillRect(x+6,foot-43,140,4);
        g.setColor(new Color(58,43,45));for(int i=0;i<7;i++)g.fillRect(x+13+i*19,foot-37,2,32);
        for(int i=0;i<8;i++) {
            g.setColor(i%2==0?new Color(139,66,68):new Color(196,192,175));
            g.fillPolygon(new int[]{x+i*19,x+(i+1)*19,x+(i+1)*19-7,x+i*19+7},
                    new int[]{foot-66,foot-66,foot-94,foot-94},4);
            g.fillRect(x+i*19,foot-66,19,7);
        }
        g.setColor(new Color(211,226,234));g.fillRect(x+8,foot-96,136,5);
        if(stocked)for(int i=0;i<3;i++)detail(g,"present",x+25+i*34,foot-40,24,24);
        else detail(g,"winter_bush",x+30,foot-40,55,25);
    }
}
