package game;

import java.awt.image.BufferedImage;

/** Simple cold-white snow surface inside the existing raster path mask. */
public final class PackedSnowPath {
    private static final int OUTLINE=0xff81919f;
    private static final int[] GROUNDS={0xff9eb8ce,0xffb2c9da,0xff6e8f9c,0xff7d8ea4};
    private static final int[][] NEIGHBORS={{1,0},{-1,0},{0,1},{0,-1}};
    private PackedSnowPath() { }

    public static final int ROAD_FILL = 0xffdce8ed;

    private static boolean palette(int rgb) {
        return rgb==OUTLINE||rgb==0xffd0d9d9||rgb==0xfff6efd9
            ||rgb==0xff7e8e9d||rgb==0xffa9b2b8||rgb==0xffeaeee3;
    }
    private static int ground(BufferedImage source) {
        int[] counts=new int[GROUNDS.length];
        for(int y=0;y<source.getHeight();y+=16)for(int x=0;x<source.getWidth();x+=16)
            for(int i=0;i<GROUNDS.length;i++)if(source.getRGB(x,y)==GROUNDS[i])counts[i]++;
        int selected=0;for(int i=1;i<counts.length;i++)if(counts[i]>counts[selected])selected=i;
        return GROUNDS[selected];
    }
    /** Returns an ARGB world buffer with exactly zero alpha outside the connected path. */
    public static BufferedImage createOverlay(BufferedImage source) {
        int width=source.getWidth(),height=source.getHeight();
        int[] original=source.getRGB(0,0,width,height,null,0,width),pixels=new int[original.length];
        boolean[] path=new boolean[original.length];int[] queue=new int[original.length];int head=0,tail=0;
        int snow=ground(source);
        // Seed from the existing outline facing exterior snow, then retain the raster union.
        // No tile rectangles, edge-distance bands or decoration clipping regions exist.
        for(int y=3;y<height-3;y++)for(int x=3;x<width-3;x++) {
            int index=y*width+x;if(original[index]!=OUTLINE)continue;boolean exposed=false;
            for(int[] d:NEIGHBORS)for(int n=1;n<=3;n++){
                int rgb=original[index+d[0]*n+d[1]*n*width];
                if(rgb==snow||rgb==0xffebf4f5)exposed=true;
            }
            if(exposed){path[index]=true;queue[tail++]=index;}
        }
        while(head<tail){int index=queue[head++],x=index%width,y=index/width;
            for(int[] d:NEIGHBORS){int nx=x+d[0],ny=y+d[1];
                if(nx<0||ny<0||nx>=width||ny>=height)continue;int next=ny*width+nx;
                if(!path[next]&&palette(original[next])){path[next]=true;queue[tail++]=next;}
            }
        }
        for(int i=0;i<pixels.length;i++)if(path[i])pixels[i]=ROAD_FILL;
        BufferedImage overlay=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        overlay.setRGB(0,0,width,height,pixels,0,width);return overlay;
    }
    public static BufferedImage polish(BufferedImage source) {
        BufferedImage result=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);
        var g=result.createGraphics();g.drawImage(source,0,0,null);g.drawImage(createOverlay(source),0,0,null);g.dispose();return result;
    }
    /** Existing item terrain repair adopts the path material; sprite/timeline remain untouched. */
    public static BufferedImage matchTerrainPatch(BufferedImage terrain,BufferedImage itemMask,BufferedImage map) {
        BufferedImage result=new BufferedImage(terrain.getWidth(),terrain.getHeight(),BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<terrain.getHeight();y++)for(int x=0;x<terrain.getWidth();x++)
            if((itemMask.getRGB(x,y)>>>24)!=0){int rgb=terrain.getRGB(x,y);result.setRGB(x,y,palette(rgb)?ROAD_FILL:rgb);}
        return result;
    }
}
