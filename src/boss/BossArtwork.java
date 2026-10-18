package boss;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import util.ImageAssets;
import util.PixelArt;

/** One shared worker, small finished frames; paint only reads the completed cache. */
public final class BossArtwork {
    // 55px Santa silhouette * 2.75 / (144px Fang silhouette / 256px source canvas).
    public static final int FANG_RENDER_SIZE=269, FANG_FRAME_COUNT=8;
    private record Art(Map<String,BufferedImage[]> frames,Map<String,BufferedImage[]> effects){}
    private static final CompletableFuture<Art> PREPARED=CompletableFuture.supplyAsync(BossArtwork::prepare);
    private static final String[] FANG={"idle","walk","run","claw_swipe","ice_blast","frost_wave","frost_dash","hurt","entrance","defeat"};
    private static final String[] TITAN={"heavy_walk","beast_charge","idle","hurt","entrance","defeat","ground_slam","cast"};
    private static final String[] FX={"claw","blast","wave","dash","hurt","defeat","slam","spikes","beam","storm","footstep","warning"};
    private BossArtwork(){}
    public static CompletableFuture<?> prepareAsync(){return PREPARED;}
    private static Art prepare(){
        var frames=new HashMap<String,BufferedImage[]>();var effects=new HashMap<String,BufferedImage[]>();
        for(String name:FANG)frames.put("fang/"+name,load("assets/bosses/frost_fang/"+name+"_sheet.png",FANG_FRAME_COUNT,256,FANG_RENDER_SIZE));
        for(String name:TITAN){
            boolean polished=name.equals("heavy_walk")||name.equals("beast_charge");
            frames.put("titan/"+name,load("assets/"+(polished?"phase10_5":"phase10_3")+"/titan/"+name+"_sheet.png",polished?16:4,512,256));
        }
        frames.put("titan/phase_transition",frames.get("titan/cast"));
        frames.put("fang/fall_blend",fallFrames(frames.get("fang/defeat")));
        frames.put("titan/fall_blend",fallFrames(frames.get("titan/defeat")));
        for(String name:FX){
            var f=load("assets/bosses/effects/"+name+"_sheet.png",12,256,256);effects.put(name,f);
            var crimson=new BufferedImage[f.length];for(int i=0;i<f.length;i++)if(f[i]!=null){
                var im=new BufferedImage(f[i].getWidth(),f[i].getHeight(),BufferedImage.TYPE_INT_ARGB);
                for(int y=0;y<im.getHeight();y++)for(int x=0;x<im.getWidth();x++){
                    int c=f[i].getRGB(x,y),r=(c>>>16)&255,g=(c>>>8)&255,b=c&255;
                    if(!name.equals("warning")&&b>r&&g>r)c=(c&0xff000000)|(Math.min(255,b)<<16)|((g/2)<<8)|Math.min(255,r+55);
                    im.setRGB(x,y,c);
                }crimson[i]=im;
            }effects.put("titan_"+name,crimson);
        }
        BufferedImage shadow=ImageAssets.load("assets/bosses/frost_fang/contact_shadow.png");
        effects.put("fang_contact",new BufferedImage[]{shadow==null?null:PixelArt.scale(shadow,FANG_RENDER_SIZE,FANG_RENDER_SIZE/4)});
        return new Art(Map.copyOf(frames),Map.copyOf(effects));
    }
    private static BufferedImage[] load(String path,int count,int cell,int size){
        BufferedImage sheet=ImageAssets.load(path);var out=new BufferedImage[count];
        if(sheet==null)return out;
        int cols=sheet.getWidth()/cell;
        for(int i=0;i<count;i++)if(cols>0&&(i/cols+1)*cell<=sheet.getHeight())out[i]=PixelArt.scale(sheet.getSubimage(i%cols*cell,i/cols*cell,cell,cell),size,size);
        return out;
    }
    private static Art ready(){try{return PREPARED.getNow(null);}catch(java.util.concurrent.CompletionException|java.util.concurrent.CancellationException e){return null;}}
    public static BufferedImage frame(String boss,String animation,int index){var a=ready();if(a==null)return null;var f=a.frames.get(boss+"/"+animation);return f==null?null:f[Math.floorMod(index,f.length)];}
    public static void draw(Graphics2D g,String boss,String animation,int frame,double x,double foot){
        BufferedImage im=frame(boss,animation,frame);if(im==null)return;
        g.drawImage(im,(int)Math.round(x-im.getWidth()/2.0),(int)Math.round(foot-im.getHeight()+im.getHeight()*16.0/(boss.equals("titan")?512:256)),null);
    }
    public static void effect(Graphics2D original,String name,int frame,double x,double y,double width,double height,double angle){
        var a=ready();if(a==null)return;var f=a.effects.get(name);if(f==null)return;var im=f[Math.floorMod(frame,f.length)];if(im==null)return;
        var g=(Graphics2D)original.create();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.translate(x,y);g.rotate(angle);g.drawImage(im,(int)(-width/2),(int)(-height/2),(int)width,(int)height,null);g.dispose();
    }
    public static void drawFang(Graphics2D g,String animation,int index,double x,double foot,boolean right){
        BufferedImage im=frame("fang",animation,index);if(im==null)return;
        int left=(int)Math.round(x-im.getWidth()/2.0),top=(int)Math.round(foot-im.getHeight()*240.0/256);
        if(right)g.drawImage(im,left+im.getWidth(),top,-im.getWidth(),im.getHeight(),null);
        else g.drawImage(im,left,top,null);
    }
    /** Alpha-correct cached pose dissolves: opaque armour stays opaque throughout. */
    private static BufferedImage[] fallFrames(BufferedImage[] poses){
        BufferedImage[] result=new BufferedImage[poses.length*8];
        for(int i=0;i<poses.length;i++)for(int t=0;t<8;t++){
            BufferedImage b=poses[i],a=poses[Math.max(0,i-1)];
            if(b==null||a==null)continue;
            if(t==7||i==0){result[i*8+t]=b;continue;}
            var blend=new BufferedImage(b.getWidth(),b.getHeight(),BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<b.getHeight();y++)for(int x=0;x<b.getWidth();x++){
                int u=a.getRGB(x,y),v=b.getRGB(x,y),au=(u>>>24)*(7-t),av=(v>>>24)*t,sum=au+av,c=((sum+3)/7)<<24;
                if(sum>0)for(int shift=0;shift<=16;shift+=8)c|=((((u>>>shift)&255)*au+((v>>>shift)&255)*av+sum/2)/sum)<<shift;
                blend.setRGB(x,y,c);
            }result[i*8+t]=blend;
        }return result;
    }
    public static void drawDefeat(Graphics2D g,boolean fang,int tick,double x,double foot,boolean right){
        int hold=fang?11:25,count=fang?8:4,index=Math.min(count-1,tick/hold),step=tick>=count*hold?7:Math.min(7,tick%hold);
        if(fang)drawFang(g,"fall_blend",index*8+step,x,foot,right);
        else draw(g,"titan","fall_blend",index*8+step,x,foot);
    }
    public static void drawFangContact(Graphics2D g,double x,double foot){
        var a=ready();if(a==null)return;var im=a.effects.get("fang_contact")[0];if(im!=null)g.drawImage(im,(int)Math.round(x-im.getWidth()/2.0),(int)Math.round(foot-im.getHeight()/2.0),null);
    }
}
