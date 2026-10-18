package game;

import java.awt.image.BufferedImage;
import java.util.Random;
import util.ImageAssets;

/** Village-only packed snow, built once through the existing road alpha mask. */
public final class VillageRoadSurface {
    private static final BufferedImage BASE=ImageAssets.load("assets/world/ground/snow_base.png");
    private static final BufferedImage PATCH=ImageAssets.load("assets/world/ground/snow_patch_01.png");
    private VillageRoadSurface() { }
    public static BufferedImage apply(BufferedImage input,BufferedImage mask){
        int w=input.getWidth(),h=input.getHeight();int[] rgb=input.getRGB(0,0,w,h,null,0,w);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)if((mask.getRGB(x,y)>>>24)!=0){
            // Continuous world-space coordinate offsets break a rigid repeating stamp grid.
            int sx=x+(int)Math.round(24*Math.sin(y/163.0)+19*Math.sin(x/277.0));
            int sy=y+(int)Math.round(21*Math.sin(x/197.0)+13*Math.sin(y/311.0));
            int art=BASE.getRGB(Math.floorMod(sx,320),Math.floorMod(sy,320)),r=(art>>>16)&255;
            if(r>=179&&r<=236){rgb[y*w+x]=PackedSnowPath.ROAD_FILL;continue;}
            int strength=r<159?44:30,color=0xff000000;
            for(int shift=16;shift>=0;shift-=8){int target=Math.min(249,Math.max(180,((art>>>shift)&255)+20));int base=(PackedSnowPath.ROAD_FILL>>>shift)&255;color|=(base+(target-base)*strength/100)<<shift;}
            rgb[y*w+x]=color;
        }
        // Rare shallow accumulations use actual irregular approved cutouts, never rectangles.
        Random random=new Random(0x56494c4c414745L);
        for(int n=0;n<10;n++){
            int x=random.nextInt(w-PATCH.getWidth()),y=random.nextInt(h-PATCH.getHeight());
            if((mask.getRGB(x+PATCH.getWidth()/2,y+PATCH.getHeight()/2)>>>24)==0)continue;
            for(int yy=0;yy<PATCH.getHeight();yy++)for(int xx=0;xx<PATCH.getWidth();xx++){
                int i=(y+yy)*w+x+xx;if((mask.getRGB(x+xx,y+yy)>>>24)==0)continue;
                int art=PATCH.getRGB(xx,yy),a=(art>>>24)*16/100;if(a==0)continue;int old=rgb[i],color=0xff000000;
                for(int shift=16;shift>=0;shift-=8){int target=Math.min(249,((art>>>shift)&255)+15);color|=((((old>>>shift)&255)*(255-a)+target*a+127)/255)<<shift;}rgb[i]=color;
            }
        }
        var result=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);result.setRGB(0,0,w,h,rgb,0,w);return result;
    }
}
