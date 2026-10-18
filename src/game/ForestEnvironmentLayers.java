package game;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import util.ImageAssets;
import util.PixelArt;

/** Forest-only, one-time illustration. All tall additions occupy existing obstacle footprints. */
public final class ForestEnvironmentLayers {
    public static final WorldMap.Point CAMP=new WorldMap.Point(1320,810);
    public static final WorldMap.Point DARK_CLEARING=new WorldMap.Point(1570,1550);
    public static final WorldMap.Point RUINS=new WorldMap.Point(2032,624);
    public static final int[][] LANTERNS={{1270,870},{1460,820},{1990,690},{2840,1180}};
    private static final int[] TERRAIN={0xff6e8f9c,0xff244151,0xff81919f,0xffd0d9d9,0xfff6efd9,0xffeaeee3};
    private static volatile BufferedImage giftPatch;
    public static BufferedImage giftPatch(){return giftPatch;}
    private ForestEnvironmentLayers() { }
    private static BufferedImage required(String path) {
        BufferedImage art=ImageAssets.load(path);
        if(art==null)throw new IllegalStateException("Missing Forest illustration: "+path);
        return art;
    }
    private static boolean terrain(int color) {
        int r=(color>>>16)&255,g=(color>>>8)&255,b=color&255;
        for(int c:TERRAIN)if(Math.abs(r-((c>>>16)&255))<=4&&Math.abs(g-((c>>>8)&255))<=4&&Math.abs(b-(c&255))<=4)return true;
        return false;
    }
    private static int grade(int c,double red,double green,double blue) {
        return (c&0xff000000)|((int)(((c>>>16)&255)*red)<<16)|((int)(((c>>>8)&255)*green)<<8)|(int)((c&255)*blue);
    }
    private static int mix(int a,int b,int amount) {
        int c=0xff000000;for(int shift=0;shift<=16;shift+=8)c|=(((a>>>shift)&255)+((((b>>>shift)&255)-((a>>>shift)&255))*amount/255))<<shift;
        return c;
    }
    private static int[] soften(int[] source,int w,int h,int radius) {
        int[] row=new int[source.length],out=new int[source.length];int span=radius*2+1;
        for(int y=0;y<h;y++) {
            int sum=0;for(int k=-radius;k<=radius;k++)sum+=source[y*w+Math.max(0,k)];
            for(int x=0;x<w;x++){row[y*w+x]=sum/span;sum+=source[y*w+Math.min(w-1,x+radius+1)]-source[y*w+Math.max(0,x-radius)];}
        }
        for(int x=0;x<w;x++) {
            int sum=0;for(int k=-radius;k<=radius;k++)sum+=row[Math.max(0,k)*w+x];
            for(int y=0;y<h;y++){out[y*w+x]=sum/span;sum+=row[Math.min(h-1,y+radius+1)*w+x]-row[Math.max(0,y-radius)*w+x];}
        }
        return out;
    }
    private static BufferedImage toned(BufferedImage image,int width,int height,double shade) {
        BufferedImage scaled=PixelArt.scale(image,width,height);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++)scaled.setRGB(x,y,grade(scaled.getRGB(x,y),.66*shade,.77*shade,.96*shade));
        return scaled;
    }
    private static boolean protectedPoint(WorldMap map,int x,int y,int radius) {
        for(var entry:map.points().entrySet()) {
            if(entry.getKey().contains("Crystal")||entry.getKey().startsWith("entry/")||entry.getKey().startsWith("exit/")||entry.getKey().startsWith("items/"))
                if(Math.hypot(x-entry.getValue().x(),y-entry.getValue().y())<radius)return true;
        }
        return Math.hypot(x-720,y-1168)<radius||Math.hypot(x-1664,y-1280)<radius;
    }
    /** Includes only the source sign's two-pixel resampling fringe, never a rectangular snow patch. */
    private static boolean retiredSignPixel(BufferedImage mask,int x,int y) {
        if(x<2635||x>=2710||y<1762||y>=1872)return false;
        int px=x-2635,py=y-1762;
        for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++){
            int sx=px+dx,sy=py+dy;
            if(sx>=0&&sx<75&&sy>=0&&sy<110&&(mask.getRGB(sx,sy)>>>24)>0)return true;
        }return false;
    }
    public static BufferedImage build(BufferedImage source) {
        ForestLandmarkArt.prepareNow();
        int w=source.getWidth(),h=source.getHeight();WorldMap map=WorldMap.forArea(Area.Type.FOREST);
        if(w!=map.width()||h!=map.height())throw new IllegalArgumentException("Forest map dimensions");
        BufferedImage snow=required("assets/world/ground/snow_base.png"),mask=PackedSnowPath.createOverlay(source);
        int[] paths=mask.getRGB(0,0,w,h,null,0,w);
        for(int i=0;i<paths.length;i++)paths[i]=paths[i]>>>24;
        paths=soften(paths,w,h,18);
        BufferedImage floor=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        BufferedImage cleanGift=new BufferedImage(96,128,BufferedImage.TYPE_INT_RGB);
        // Retire only the baked direction sign below Crystal 3; the source PNG remains immutable.
        BufferedImage retiredSign=PixelArt.scale(required("SantaLastChristmas_MapPackage/assets/environment/decorations/sign.png"),75,110);
        int[] pixels=source.getRGB(0,0,w,h,null,0,w),result=new int[pixels.length];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
            int i=y*w+x,s=snow.getRGB(x%320,y%320),s2=snow.getRGB((x/2+137)%320,(y/2+79)%320);
            double depth=.98-.13*x/w;
            int field=grade(mix(s,s2,70),.32*depth,.43*depth,.64*depth);
            int path=grade(mix(s,s2,160),.58,.66,.79);
            int wear=(int)(22+17*Math.sin(x*.013+y*.009)+11*Math.sin(x*.028-y*.015));
            path=mix(path,0xff647386,Math.max(0,wear));
            result[i]=mix(field,path,paths[i]);
            if(x>=896&&x<992&&y>=696&&y<824)cleanGift.setRGB(x-896,y-696,result[i]);
            boolean sign=retiredSignPixel(retiredSign,x,y);
            if(!sign&&!terrain(pixels[i]))result[i]=grade(pixels[i],.56,.68,.88);
        }
        floor.setRGB(0,0,w,h,result,0,w);
        Graphics2D g=floor.createGraphics();Random random=new Random(0x46524f5354L);
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            // The actual east gate has always existed. A narrow worn trail makes it readable.
            Path2D east=new Path2D.Double();east.moveTo(2200,1115);east.curveTo(2480,1080,2740,1215,3056,1168);
            BufferedImage trail=toned(snow,320,320,.85);
            g.setPaint(new TexturePaint(trail,new Rectangle(0,0,320,320)));
            g.setComposite(AlphaComposite.SrcOver.derive(.22f));g.setStroke(new BasicStroke(106,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.draw(east);
            g.setComposite(AlphaComposite.SrcOver.derive(.50f));g.setStroke(new BasicStroke(68,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.draw(east);
            g.setComposite(AlphaComposite.SrcOver);
            for(int i=0;i<1800;i++) {
                int x=64+random.nextInt(w-128),y=64+random.nextInt(h-128);
                if(paths[y*w+x]<110)continue;
                g.setColor(new Color(73,92,116,35+random.nextInt(28)));
                if(i%3==0){g.fillOval(x,y,3,6);g.fillOval(x+7,y+7,3,6);}else g.fillOval(x,y,2+random.nextInt(5),2);
            }
            BufferedImage ice=toned(required("assets/world/ground/ice_patch_01.png"),126,68,1);
            for(int[] p:new int[][]{{490,1150},{1190,1440},{1830,1010},{2460,1610},{2800,1180}}) {
                g.setComposite(AlphaComposite.SrcOver.derive(.30f));g.drawImage(ice,p[0],p[1],null);
            }
            g.setComposite(AlphaComposite.SrcOver);
            // Dense silhouettes remain rooted in audited solid footprints; actors render above this cache.
            BufferedImage pine=required("assets/world/village_props/pine_medium.png"),dead=required("assets/world/village_props/dead_tree.png");
            BufferedImage corrupt=toned(required("SantaLastChristmas_MapPackage/assets/environment/nature/corrupt_tree.png"),175,220,.9);
            BufferedImage[] trees=new BufferedImage[12];
            for(int i=0;i<trees.length;i++)trees[i]=toned(i%4==3?dead:pine,115+(i%3)*25,172+(i%3)*36,.80+(i%4)*.065);
            for(Rectangle2D solid:map.solids()) {
                if(solid.getWidth()<64||solid.getWidth()>160||solid.getHeight()<48||solid.getHeight()>100||solid.getX()<40||solid.getY()<40||solid.getMaxX()>w-40||solid.getMaxY()>h-40)continue;
                int x=(int)solid.getCenterX(),foot=(int)solid.getMaxY();
                if(protectedPoint(map,x,foot,155))continue;
                BufferedImage tree=trees[random.nextInt(trees.length)];
                for(int crystal=1;crystal<=3;crystal++) {
                    var center=map.point("landmarks/Crystal "+crystal);
                    if(Math.hypot(x-center.x(),foot-center.y())<420&&random.nextBoolean()){tree=corrupt;break;}
                }
                g.setPaint(new RadialGradientPaint(x+12,foot+5,100,new float[]{0,1},new Color[]{new Color(8,18,37,120),new Color(8,18,37,0)}));
                g.fillOval(x-90,foot-48,210,112);
                g.drawImage(tree,x-tree.getWidth()/2,foot-tree.getHeight(),null);
                if(random.nextBoolean())g.drawImage(trees[0],x-80,foot-170,null);
            }
            BufferedImage bush=toned(required("assets/world/village_props/winter_bush.png"),52,37,.95);
            for(int i=0;i<280;i++) {
                int x=80+random.nextInt(w-160),y=80+random.nextInt(h-160);
                if(paths[y*w+x]>25||protectedPoint(map,x,y,145))continue;
                if(i%3==0){g.setColor(new Color(30,42,58,90));g.setStroke(new BasicStroke(3));g.drawLine(x,y,x+28,y+8);g.drawLine(x+9,y+3,x+16,y-6);}
                else g.drawImage(bush,x-26,y-24,null);
            }
            camp(g);ruins(g);cliff(g);
            // Crystal fractures are small prepared overlays that follow the actual core state.
            // Dark clearing has a sparse, low silhouette. No false interactive marker is added.
            g.setPaint(new RadialGradientPaint(1570,1550,340,new float[]{0,.6f,1},new Color[]{new Color(14,18,40,120),new Color(28,27,54,48),new Color(28,27,54,0)}));
            g.fillOval(1230,1250,680,600);
            for(int[] lamp:LANTERNS)lantern(g,lamp[0],lamp[1]);
        } finally {g.dispose();}
        giftPatch=cleanGift;return floor;
    }
    private static void detail(Graphics2D g,String name,int x,int foot,int width,int height) {
        BufferedImage art=required("assets/world/village_props/"+name+".png");g.drawImage(toned(art,width,height,.95),x,foot-height,null);
    }
    private static void tent(Graphics2D g,int x,int y,boolean collapsed) {
        var sprite=ForestLandmarkArt.tent(collapsed?1:0);
        if(sprite!=null){ForestLandmarkArt.contact(g,x+75,y+2,146);g.drawImage(sprite,x,y-sprite.getHeight(),null);return;}
        fallbackTent(g,x,y,collapsed);
    }
    private static void fallbackTent(Graphics2D g,int x,int y,boolean collapsed) {
        int peak=collapsed?54:118;
        g.setPaint(new RadialGradientPaint(x+75,y+5,105,new float[]{0,1},new Color[]{new Color(8,17,33,140),new Color(8,17,33,0)}));g.fillOval(x-30,y-40,215,83);
        Polygon left=new Polygon(new int[]{x,x+76,x+119,x+149},new int[]{y-4,y-peak,y-22,y+3},4);
        Polygon right=new Polygon(new int[]{x+76,x+149,x+113},new int[]{y-peak,y+3,y-3},3);
        g.setPaint(new GradientPaint(x,y-peak,new Color(102,112,128),x+115,y,new Color(52,65,83)));g.fill(left);
        g.setPaint(new GradientPaint(x+76,y-peak,new Color(69,74,91),x+149,y,new Color(29,39,57)));g.fill(right);
        Shape clip=g.getClip();g.clip(left);
        Random grain=new Random(x);
        for(int i=0;i<780;i++){g.setColor(i%3==0?new Color(181,191,210,28):new Color(17,27,41,30));g.fillRect(x+grain.nextInt(155),y-peak+grain.nextInt(peak+5),1+grain.nextInt(3),1);}
        g.setClip(clip);
        g.setStroke(new BasicStroke(1));g.setColor(new Color(161,174,190,95));
        for(int i=0;i<6;i++)g.drawLine(x+76,y-peak+4,x+18+i*18,y-7);
        g.setColor(new Color(12,22,39));g.fillPolygon(new int[]{x+76,x+109,x+96,x+63},new int[]{y-peak+17,y-3,y-3,y-7},4);
        g.setColor(new Color(97,118,139));g.drawLine(x+75,y-peak+18,x+64,y-7);
        g.setStroke(new BasicStroke(2));g.setColor(new Color(89,99,115));g.drawLine(x+76,y-peak,x+178,y+18);g.drawLine(x+3,y-2,x-18,y+12);
        g.setColor(new Color(53,48,48));g.fillRect(x+175,y+11,4,11);g.fillRect(x-19,y+8,4,10);
        g.setColor(new Color(170,193,218));g.fillPolygon(new int[]{x+43,x+75,x+99,x+116,x+109,x+95,x+80,x+64},new int[]{y-peak+45,y-peak-3,y-peak+29,y-peak+61,y-peak+53,y-peak+41,y-peak+24,y-peak+27},8);
        g.setColor(new Color(149,177,202));g.setStroke(new BasicStroke(3));g.drawLine(x+2,y-3,x+22,y-3);g.drawLine(x+121,y,x+143,y+1);
        g.setColor(new Color(23,32,47));g.setStroke(new BasicStroke(1));g.drawLine(x+27,y-19,x+31,y-38);g.drawLine(x+30,y-22,x+37,y-35);
    }
    private static void camp(Graphics2D g) {
        tent(g,1175,850,false);tent(g,1390,766,true);
        detail(g,"bench",1150,920,118,65);detail(g,"wooden_fence",1450,903,122,43);
        BufferedImage cart=required("SantaLastChristmas_MapPackage/assets/environment/decorations/cart.png");
        ForestLandmarkArt.contact(g,1474,966,138);
        g.drawImage(toned(cart,149,115,.85),1400,858,null);
        // Local contact/melt treatment is prepared beneath the unchanged cold fire-ring art.
        campfireGround(g);
        // Snow-filled fire ring and split logs retain their original position and artwork.
        g.setColor(new Color(22,30,43));g.fillOval(1304,872,78,37);
        for(int i=0;i<9;i++){double a=i*Math.PI*2/9;detail(g,"snow_rock",1335+(int)(Math.cos(a)*34),895+(int)(Math.sin(a)*15),20,13);}
        g.setColor(new Color(54,48,49));g.setStroke(new BasicStroke(6));g.drawLine(1323,884,1357,899);g.drawLine(1326,896,1358,884);
        g.setColor(new Color(130,150,173,95));g.setStroke(new BasicStroke(2));g.drawLine(1328,884,1354,897);
        // Three inappropriate bench-crop boxes are omitted, rather than covered.
        detail(g,"snow_rock",1280,946,48,28);
        BufferedImage drift=toned(required("assets/world/ground/snow_drift_03.png"),100,25,.80);
        g.drawImage(drift,1188,845,null);g.drawImage(drift,1412,760,90,18,null);
    }
    private static void campfireGround(Graphics2D g) {
        ForestLandmarkArt.contact(g,1343,904,103);
        // Irregular, feathered pixel treatment preserves the native snow grain. There is
        // no rectangular floor patch, radial bloom, live fire or new animation owner.
        Random pixels=new Random(0x46495245534e4f57L);
        for(int y=863;y<923;y++)for(int x=1288;x<1401;x++) {
            double dx=(x-1343)/56.0,dy=(y-894)/29.0;
            double edge=1-dx*dx-dy*dy;
            if(edge<=0)continue;
            int alpha=(int)(edge*edge*(12+pixels.nextInt(12)));
            g.setColor(new Color(72,66,61,alpha));g.fillRect(x,y,1,1);
            // Restrained amber reflection on the near snow, not an orange light disk.
            if(y>900&&pixels.nextInt(3)==0){g.setColor(new Color(211,165,104,alpha/2));g.fillRect(x,y,2,1);}
        }
        // Small uneven snow lips outside the stones tie their bases to the terrain.
        g.setColor(new Color(100,128,158,125));
        g.fillPolygon(new int[]{1295,1303,1311,1317,1311,1300},new int[]{894,899,898,903,907,902},6);
        g.fillPolygon(new int[]{1367,1375,1386,1391,1381,1373},new int[]{899,897,900,905,909,905},6);
        g.setColor(new Color(170,192,211,150));
        g.fillPolygon(new int[]{1296,1302,1309,1314,1307,1300},new int[]{894,895,893,899,901,898},6);
        g.fillPolygon(new int[]{1370,1377,1385,1389,1381,1374},new int[]{898,896,898,902,904,901},6);
        g.fillRect(1320,908,5,2);g.fillRect(1358,909,7,2);
    }
    private static void ruins(Graphics2D g) {
        detail(g,"wooden_fence",1850,735,130,47);detail(g,"snow_rock",2150,744,100,64);
        BufferedImage monument=required("SantaLastChristmas_MapPackage/assets/environment/landmarks/monument.png");
        BufferedImage stone=toned(monument.getSubimage(120,240,60,64),48,83,.93);
        for(int i=0;i<3;i++) {
            int x=1905+i*81,y=585+i*36;
            g.setPaint(new RadialGradientPaint(x+20,y+25,55,new float[]{0,1},new Color[]{new Color(8,19,37,100),new Color(8,19,37,0)}));g.fillOval(x-30,y-2,108,65);
            g.setPaint(new TexturePaint(stone,new Rectangle(x,y-48,48,83)));
            g.fillPolygon(new int[]{x,x+4,x+9,x+17,x+22,x+35,x+43,x+38,x+33,x+7},new int[]{y+24,y-39,y-44,y-50,y-43,y-53,y-22,y+25,y+34,y+31},10);
            g.setColor(new Color(143,169,198));g.fillPolygon(new int[]{x+5,x+9,x+18,x+23,x+35,x+37,x+28,x+17},new int[]{y-38,y-45,y-51,y-43,y-54,y-45,y-38,y-40},8);
            g.setColor(new Color(13,27,43));g.setStroke(new BasicStroke(1));g.drawLine(x+18,y-40,x+13,y-10);g.drawLine(x+13,y-10,x+22,y+7);g.drawLine(x+22,y+7,x+15,y+27);
            detail(g,"snow_rock",x-8,y+40,41,24);
        }
    }
    private static void cliff(Graphics2D g) {
        // This is the existing large rock's blocked rectangle, not a new wall or waterfall route.
        g.setPaint(new RadialGradientPaint(2440,1035,105,new float[]{0,1},new Color[]{new Color(8,20,39,120),new Color(8,20,39,0)}));g.fillOval(2340,998,200,85);
        g.setColor(new Color(27,50,78));g.fillPolygon(new int[]{2340,2355,2415,2465,2508,2518,2482,2410,2350},new int[]{950,904,879,900,923,999,1066,1068,1030},9);
        for(int i=0;i<14;i++) {
            int x=2344+i*12,top=907+(int)(17*Math.sin(i*.7))-i*2,bottom=1034+(int)(19*Math.sin(i*.9));
            g.setPaint(new GradientPaint(x,top,new Color(146,194,221),x,bottom,new Color(37,82,116)));
            g.fillPolygon(new int[]{x,x+10,x+15,x+10,x+4,x-3},new int[]{top,top-7,top+24,bottom-4,bottom+14,bottom-12},6);
            g.setColor(new Color(173,214,231,110));g.drawLine(x+3,top+17,x+5,bottom-8);
            g.setColor(new Color(45,73,99,100));g.drawLine(x+4,top+38,x+8,top+57);g.drawLine(x+8,top+57,x+2,top+76);
        }
        detail(g,"snow_rock",2340,944,175,94);detail(g,"snow_rock",2378,1070,100,57);
    }
    private static void lantern(Graphics2D g,int x,int y) {
        var sprite=ForestLandmarkArt.lamp();
        if(sprite!=null){ForestLandmarkArt.contact(g,x,y,32);g.drawImage(sprite,x-15,y-60,null);return;}
        g.setColor(new Color(37,40,50));g.fillRect(x-2,y-40,4,40);g.fillRect(x-10,y-57,20,19);
        g.setColor(new Color(226,174,103));g.fillRect(x-6,y-54,12,13);g.setColor(new Color(255,218,144));g.fillRect(x-2,y-52,4,9);
        g.setColor(new Color(163,189,211));g.fillRect(x-12,y-60,24,4);
    }
}
