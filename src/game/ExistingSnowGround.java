package game;

import java.awt.image.BufferedImage;
import util.ImageAssets;

/** Reuses Toby's existing two-color ground artwork; no procedural texture generation. */
public final class ExistingSnowGround {
    private static final BufferedImage TOBY=ImageAssets.load("assets/world/quest_patches/toby.png");
    private static final int[] BASES={0xffb2c9da,0xff9eb8ce};
    private static final int WHITE=0xffebf4f5;
    private static final int[][] DIRECTIONS={{1,0},{-1,0},{0,1},{0,-1}};
    private ExistingSnowGround() { }
    private static int mirrored(int coordinate,int size) {
        int index=Math.floorMod(coordinate,size*2);return index<size?index:size*2-1-index;
    }
    /** Exact pixels from the immutable asset, mirrored at repeat boundaries. */
    public static int texturePixel(int worldX,int worldY) {
        return TOBY.getRGB(mirrored(worldX,TOBY.getWidth()),mirrored(worldY,TOBY.getHeight()));
    }
    public static boolean[] groundMask(BufferedImage source,BufferedImage roadComposite) {
        if(TOBY==null)throw new IllegalStateException("Missing Toby ground reference");
        int width=source.getWidth(),height=source.getHeight();int[] original=source.getRGB(0,0,width,height,null,0,width);
        int base=BASES[0],a=0,b=0;for(int rgb:original){if(rgb==BASES[0])a++;if(rgb==BASES[1])b++;}if(b>a)base=BASES[1];
        if(Math.max(a,b)<original.length/5)return new boolean[original.length];
        int[] result=roadComposite.getRGB(0,0,width,height,null,0,width);
        boolean[] eligible=new boolean[original.length],visited=new boolean[original.length];int[] queue=new int[original.length];
        for(int i=0;i<original.length;i++)eligible[i]=original[i]==base&&result[i]==original[i];
        // Keep the conservative exterior-fleck mask; never treat attached prop snow as terrain.
        for(int start=0;start<original.length;start++){
            if(visited[start]||original[start]!=WHITE||result[start]!=WHITE)continue;
            int head=0,tail=1,border=0,groundBorder=0;queue[0]=start;visited[start]=true;
            while(head<tail){int i=queue[head++],x=i%width,y=i/width;
                for(int[]d:DIRECTIONS){int nx=x+d[0],ny=y+d[1];if(nx<0||ny<0||nx>=width||ny>=height)continue;int n=ny*width+nx;
                    if(original[n]==WHITE&&result[n]==WHITE){if(!visited[n]){visited[n]=true;queue[tail++]=n;}}
                    else {border++;if(original[n]==base)groundBorder++;}
                }
            }
            if(tail<=350&&border>0&&groundBorder>=border*.92)for(int j=0;j<tail;j++)eligible[queue[j]]=true;
        }
        return eligible;
    }
    /** Retained only for reference tests; runtime uses ApprovedSnowGround. */
    public static BufferedImage polish(BufferedImage source,BufferedImage roadComposite) {
        int width=source.getWidth(),height=source.getHeight();
        boolean[] eligible=groundMask(source,roadComposite);
        int[] result=roadComposite.getRGB(0,0,width,height,null,0,width);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++)if(eligible[y*width+x])result[y*width+x]=texturePixel(x,y);
        BufferedImage composite=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);composite.setRGB(0,0,width,height,result,0,width);return composite;
    }
}

