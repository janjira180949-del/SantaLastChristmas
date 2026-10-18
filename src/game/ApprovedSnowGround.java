package game;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import util.ImageAssets;

/** Approved artwork, composited once in world space through the existing terrain mask. */
public final class ApprovedSnowGround {
    private static final String[] NAMES={"snow_base","snow_patch_01","snow_patch_02","snow_patch_03","snow_patch_04","ice_patch_01","ice_patch_02","snow_drift_01","snow_drift_02","snow_drift_03","snow_details"};
    private static final BufferedImage[] ART=new BufferedImage[NAMES.length];
    private static final List<BufferedImage> DETAILS=new ArrayList<>();
    public record Placement(int asset,int x,int y,int detail) { }
    static {
        for(int i=0;i<NAMES.length;i++){
            ART[i]=ImageAssets.load("assets/world/ground/"+NAMES[i]+".png");
            if(ART[i]==null)throw new IllegalStateException("Missing snow asset: "+NAMES[i]);
        }
        // Individual original elements from the details sheet's two rows; never draw the sheet.
        BufferedImage details=ART[10];
        // The lower row contains faint ground marks rather than the larger mound silhouettes.
        int begin=-1,quiet=0;
        for(int x=0;x<=details.getWidth()+8;x++){
            boolean ink=false;
            if(x<details.getWidth())for(int y=155;y<details.getHeight();y++)if((details.getRGB(x,y)>>>24)>8){ink=true;break;}
            if(ink){if(begin<0)begin=x;quiet=0;}else if(begin>=0&&++quiet>=8){
                int right=x-quiet,y0=details.getHeight(),y1=-1;
                for(int xx=begin;xx<=right;xx++)for(int y=155;y<details.getHeight();y++)if((details.getRGB(xx,y)>>>24)>8){y0=Math.min(y0,y);y1=Math.max(y1,y);}
                if(y1>=0)DETAILS.add(details.getSubimage(begin,y0,right-begin+1,y1-y0+1));begin=-1;quiet=0;
            }
        }
    }    private ApprovedSnowGround() { }
    public static List<Placement> placements(int w,int h,boolean[] mask) {
        Random rng=new Random(0x534e4f57L^((long)w<<32)^h);List<Placement> result=new ArrayList<>();
        int budget=Math.max(3,w*h/480000),last=-1;
        for(int n=0;n<budget;n++){
            int asset;int roll=rng.nextInt(100);
            if(roll<55)asset=1+rng.nextInt(4);else if(roll<65)asset=5+rng.nextInt(2);else if(roll<80)asset=7+rng.nextInt(3);else asset=10;
            if(asset==last&&asset<5)asset=1+asset%4;
            int detail=rng.nextInt(DETAILS.size());BufferedImage art=asset==10?DETAILS.get(detail):ART[asset];
            for(int attempt=0;attempt<100;attempt++){
                int x=rng.nextInt(Math.max(1,w-art.getWidth())),y=rng.nextInt(Math.max(1,h-art.getHeight()));
                int cx=x+art.getWidth()/2,cy=y+art.getHeight()/2;if(!mask[cy*w+cx])continue;
                int valid=0,total=0;boolean nearObject=false;
                for(int yy=0;yy<art.getHeight();yy+=8)for(int xx=0;xx<art.getWidth();xx+=8){total++;if(mask[(y+yy)*w+x+xx])valid++;else nearObject=true;}
                // Broad shapes stay on open terrain; a selected drift can approach an authored boundary.
                if(valid<total*.94||asset>=7&&asset<=9&&!nearObject)continue;
                boolean spaced=true;for(var p:result)if(Math.abs(p.x-x)<400&&Math.abs(p.y-y)<300)spaced=false;
                if(!spaced)continue;result.add(new Placement(asset,x,y,detail));last=asset;break;
            }
        }
        return List.copyOf(result);
    }
    public static BufferedImage polish(BufferedImage source,BufferedImage roadComposite,boolean decorate) {
        int w=source.getWidth(),h=source.getHeight();boolean[] mask=ExistingSnowGround.groundMask(source,roadComposite);
        int[] pixels=roadComposite.getRGB(0,0,w,h,null,0,w);BufferedImage base=ART[0];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)if(mask[y*w+x])pixels[y*w+x]=base.getRGB(x%base.getWidth(),y%base.getHeight());
        if(decorate)for(var p:placements(w,h,mask)){
            BufferedImage art=p.asset==10?DETAILS.get(p.detail):ART[p.asset];
            for(int y=0;y<art.getHeight();y++)for(int x=0;x<art.getWidth();x++){
                int i=(p.y+y)*w+p.x+x;if(!mask[i])continue;int rgb=art.getRGB(x,y),alpha=rgb>>>24;if(alpha==0)continue;
                int old=pixels[i],out=0xff000000;for(int shift:new int[]{16,8,0})out|=((((rgb>>>shift)&255)*alpha+((old>>>shift)&255)*(255-alpha)+127)/255)<<shift;pixels[i]=out;
            }
        }
        BufferedImage result=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);result.setRGB(0,0,w,h,pixels,0,w);return result;
    }
}

