package game;

import java.awt.image.BufferedImage;
import util.ImageAssets;

/** Cached packed-snow material sampled from approved art, strictly inside the original mask. */
public final class RoadSnowTexture {
    public static final int DETAIL_LEVEL=35;
    private static final BufferedImage BASE=ImageAssets.load("assets/world/ground/snow_base.png");
    private RoadSnowTexture() { }
    public static int pixel(int x,int y){return pixel(x,y,DETAIL_LEVEL);}
    public static int pixel(int x,int y,int level) {
        int sample=BASE.getRGB(Math.floorMod(x+71,320),Math.floorMod(y+103,320));
        int red=(sample>>>16)&255;
        // Select the artwork's irregular pale/cool formations; midtones remain quiet packed snow.
        if(red>=180&&red<=240)return PackedSnowPath.ROAD_FILL;
        int color=0xff000000;
        for(int shift=16;shift>=0;shift-=8){int target=Math.min(248,Math.max(180,((sample>>>shift)&255)+18));int base=(PackedSnowPath.ROAD_FILL>>>shift)&255;color|=(base+(target-base)*level/100)<<shift;}
        return color;
    }
    public static BufferedImage apply(BufferedImage composite,BufferedImage mask){return apply(composite,mask,DETAIL_LEVEL);}
    public static BufferedImage apply(BufferedImage composite,BufferedImage mask,int level) {
        int w=composite.getWidth(),h=composite.getHeight();int[] pixels=composite.getRGB(0,0,w,h,null,0,w);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)if((mask.getRGB(x,y)>>>24)!=0)pixels[y*w+x]=pixel(x,y,level);
        BufferedImage result=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);result.setRGB(0,0,w,h,pixels,0,w);return result;
    }
}

