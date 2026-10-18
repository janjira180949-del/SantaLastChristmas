package game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import util.ImageAssets;
import util.PixelArt;

/** One-time illustration/material composition. Has no gameplay objects, solids or update clock. */
public final class TownEnvironmentLayers {
    public static final String PLAZA="assets/world/town_environment/spirit_plaza.png";
    public static final String BANNER="assets/world/town_environment/festival_banner.png";
    public record Layers(BufferedImage cold,BufferedImage restored) { }
    public record Detail(String name,Rectangle bounds,boolean mountedOnExistingLamp) { }
    private final List<Detail> details=new ArrayList<>();
    public List<Detail> details(){return List.copyOf(details);}
    private static BufferedImage required(String path) {
        BufferedImage art=ImageAssets.load(path);
        if(art==null)throw new IllegalStateException("Missing Town environment artwork: "+path);
        return art;
    }
    static boolean terrain(int color) {
        return color==0xff7d8ea4||color==0xff244151||color==0xff81919f||color==0xffd0d9d9
            ||color==0xfff6efd9||color==0xff7e8e9d||color==0xffa9b2b8||color==0xffeaeee3||color==0xff3f4d5e;
    }
    private static boolean road(int color){return color==0xff7e8e9d||color==0xff81919f||color==0xffa9b2b8;}
    private static int grade(int color,boolean warm,boolean object) {
        int r=(color>>>16)&255,g=(color>>>8)&255,b=color&255;
        boolean emission=object&&r>180&&g>90&&g<225&&b<125&&r>g*1.16;
        double red=object?(warm?.88:.56):(warm?.78:.58);
        double green=object?(warm?.92:.65):(warm?.83:.69);
        double blue=object?(warm?.98:.83):(warm?.98:.91);
        if(emission&&!warm){red=.23;green=.30;blue=.47;}
        if(emission&&warm){red=1;green=1;blue=1;}
        return (color&0xff000000)|((int)(r*red)<<16)|((int)(g*green)<<8)|(int)(b*blue);
    }
    /** Separable box filter is preparation only; it feathers roads without moving their centerlines. */
    private static int[] soften(int[] input,int width,int height,int radius) {
        int[] horizontal=new int[input.length],output=new int[input.length];int diameter=radius*2+1;
        for(int y=0;y<height;y++) {
            int sum=0;for(int k=-radius;k<=radius;k++)sum+=input[y*width+Math.max(0,Math.min(width-1,k))];
            for(int x=0;x<width;x++) {
                horizontal[y*width+x]=sum/diameter;
                sum+=input[y*width+Math.min(width-1,x+radius+1)]-input[y*width+Math.max(0,x-radius)];
            }
        }
        for(int x=0;x<width;x++) {
            int sum=0;for(int k=-radius;k<=radius;k++)sum+=horizontal[Math.max(0,Math.min(height-1,k))*width+x];
            for(int y=0;y<height;y++) {
                output[y*width+x]=sum/diameter;
                sum+=horizontal[Math.min(height-1,y+radius+1)*width+x]-horizontal[Math.max(0,y-radius)*width+x];
            }
        }
        return output;
    }
    private static int mix(int snow,int stone,int amount) {
        int result=0xff000000;
        for(int shift=16;shift>=0;shift-=8) {
            int a=(snow>>>shift)&255,b=(stone>>>shift)&255;
            result|=(a+(b-a)*amount/255)<<shift;
        }
        return result;
    }
    /** Blend only tile joins; the interior artwork retains its original sharp detail. */
    private static int tiled(BufferedImage tile,int x,int y) {
        int width=tile.getWidth(),height=tile.getHeight();
        int px=Math.floorMod(x,width),py=Math.floorMod(y,height),color=tile.getRGB(px,py);
        int edge=12;
        if(px<edge)color=mix(tile.getRGB(width-edge+px,py),color,128+px*127/edge);
        else if(px>=width-edge)color=mix(color,tile.getRGB(px-width+edge,py),(px-width+edge)*127/edge);
        if(py<edge)color=mix(tile.getRGB(px,height-edge+py),color,128+py*127/edge);
        else if(py>=height-edge)color=mix(color,tile.getRGB(px,py-height+edge),(py-height+edge)*127/edge);
        return color;
    }
    private static BufferedImage toned(BufferedImage art,boolean warm) {
        BufferedImage result=new BufferedImage(art.getWidth(),art.getHeight(),BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<art.getHeight();y++)for(int x=0;x<art.getWidth();x++)result.setRGB(x,y,grade(art.getRGB(x,y),warm,true));
        return result;
    }
    private void detail(Graphics2D cold,Graphics2D restored,String name,BufferedImage art,int x,int foot,boolean lamp) {
        int y=foot-art.getHeight();details.add(new Detail(name,new Rectangle(x,y,art.getWidth(),art.getHeight()),lamp));
        cold.drawImage(toned(art,name.startsWith("redesigned-")||name.startsWith("festival-prop")),x,y,null);restored.drawImage(toned(art,true),x,y,null);
    }
    public Layers build(BufferedImage source) {
        if(!details.isEmpty())throw new IllegalStateException("Environment layers already prepared");
        TownChapterArt.prepareAsync().join();
        int width=source.getWidth(),height=source.getHeight();
        BufferedImage plaza=required(PLAZA),snow=required("assets/world/ground/snow_base.png");
        // Paving comes from the new floor's unobstructed center, not an invented rectangle.
        BufferedImage paving=PixelArt.scale(plaza.getSubimage(580,400,376,240),160,100);
        int[] pixels=source.getRGB(0,0,width,height,null,0,width),roads=new int[pixels.length];
        for(int i=0;i<pixels.length;i++)roads[i]=road(pixels[i])?255:0;
        roads=soften(roads,width,height,22);
        // Close small baked marker holes, then soften the snow shoulder instead of retaining square patches.
        for(int i=0;i<roads.length;i++)roads[i]=roads[i]>88?255:0;
        roads=soften(roads,width,height,22);
        BufferedImage cold=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        BufferedImage restored=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        int[] before=new int[pixels.length],after=new int[pixels.length];
        for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
            int s=tiled(snow,x,y),r=(s>>>16)&255,g=(s>>>8)&255,b=s&255;
            // Cold field snow; two samples stop a single small repeated patch dominating the town.
            int field=0xff000000|((r*35/100)<<16)|((g*44/100)<<8)|(b*66/100);
            int stone=tiled(paving,x+17,y+37);
            int floor=mix(field,grade(stone,false,false),roads[y*width+x]);
            before[y*width+x]=floor;
            after[y*width+x]=mix(field,grade(stone,true,false),roads[y*width+x]);
        }
        cold.setRGB(0,0,width,height,before,0,width);restored.setRGB(0,0,width,height,after,0,width);
        Graphics2D cg=cold.createGraphics(),wg=restored.createGraphics();
        try {
            WorldMap.Point tree=WorldMap.forArea(Area.Type.CHRISTMAS_TOWN).point("landmarks/Corrupted Town Center");
            BufferedImage floor=PixelArt.scale(plaza,840,560);
            int plazaX=tree.x()-420,plazaY=tree.y()+64-295;
            cg.drawImage(toned(floor,false),plazaX,plazaY,null);wg.drawImage(floor,plazaX,plazaY,null);
            // Restore the actual authored object silhouettes above the newly illustrated floor.
            BufferedImage oldObjects=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
            BufferedImage liveObjects=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
                int color=pixels[y*width+x];if(terrain(color)||TownChapterArt.retiredObject(x,y))continue;
                oldObjects.setRGB(x,y,grade(color,false,true));
                // Only the verified old tree/shadow footprint retires after restoration.
                if(x>=1112&&x<1480&&y>=784&&y<1120)continue;
                liveObjects.setRGB(x,y,grade(color,true,true));
            }
            cg.drawImage(oldObjects,0,0,null);wg.drawImage(liveObjects,0,0,null);
            BufferedImage banner=PixelArt.scale(required(BANNER),88,132);
            for(int[] mount:new int[][]{{934,822},{1570,822},{934,1510},{1570,1510}})
                detail(cg,wg,"existing-lamp-banner",banner,mount[0],mount[1]+132,true);
            // Replace the original contribution, never paint a new facade over an old one.
            int index=0;
            for(int[] home:TownChapterArt.HOMES){
                BufferedImage building=TownChapterArt.fit(TownChapterArt.building(index++),home[2]-home[0]-16,home[3]-home[1]-16);
                detail(cg,wg,"redesigned-building",building,home[0]+(home[2]-home[0]-building.getWidth())/2,home[3]-8,false);
            }
            for(int[] p:new int[][]{{0,636,1240,132,110},{1,1776,1240,132,110},{2,790,540,72,106},{2,1780,540,72,106},
                {2,800,1530,72,106},{2,1770,1530,72,106},{3,868,1110,42,86},{3,1710,1110,42,86},
                {4,678,1270,48,32},{4,1840,1270,48,32},{5,658,1480,114,45},{5,1760,1480,114,45},
                {6,726,1226,38,61},{7,776,1268,42,52},{7,1724,1268,42,52}}){
                detail(cg,wg,"festival-prop-"+p[0],TownChapterArt.fit(TownChapterArt.prop(p[0]),p[3],p[4]),p[1],p[2],false);
            }
            BufferedImage frost=required("assets/world/ground/ice_patch_01.png");
            BufferedImage small=PixelArt.scale(frost,94,75);cg.setComposite(AlphaComposite.SrcOver.derive(.30f));
            for(int[] patch:new int[][]{{1080,1240},{1450,1190},{1190,640},{1340,1550}})cg.drawImage(small,patch[0],patch[1],null);
        } finally {cg.dispose();wg.dispose();}
        return new Layers(cold,restored);
    }
}
