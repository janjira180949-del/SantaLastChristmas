package game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;
import util.ImageAssets;
import util.PixelArt;

/** Town-only prepared object art; no terrain, collision, quest or simulation ownership. */
public final class TownChapterArt {
    public static final String ROOT="assets/phase10_3/";
    public static final int[][] HOMES={{224,256,576,608},{1888,224,2272,608},{224,1024,608,1440},
        {1888,1056,2304,1472},{736,160,1056,448},{1536,160,1856,448}};
    public static final Rectangle TREE_FOOTPRINT=new Rectangle(1112,784,368,336);
    private record Art(BufferedImage[] buildings,BufferedImage[] props,BufferedImage[] trees,
            BufferedImage[][] corruption,BufferedImage[][] restoration,BufferedImage[] restored){}
    private static final CompletableFuture<Art> ART=CompletableFuture.supplyAsync(TownChapterArt::load);
    private TownChapterArt(){}
    public static CompletableFuture<?> prepareAsync(){return ART;}
    private static BufferedImage[] images(String name,int count){
        var out=new BufferedImage[count];for(int i=0;i<count;i++)out[i]=ImageAssets.load(ROOT+name+"/frame_"+String.format("%02d",i+1)+".png");return out;
    }
    private static Art load(){
        var trees=images("tree",3);var corruption=new BufferedImage[2][17];var restoration=new BufferedImage[2][17];
        var restored=new BufferedImage[]{trees[2],PixelArt.scale(trees[2],326,272)};
        var aftermath=blend(trees[0],trees[1],.55);
        for(int i=0;i<=16;i++){
            corruption[0][i]=blend(trees[0],trees[1],i/16.0);
            restoration[0][i]=blend(aftermath,trees[2],i/16.0);
            corruption[1][i]=PixelArt.scale(corruption[0][i],326,272);
            restoration[1][i]=PixelArt.scale(restoration[0][i],326,272);
        }
        return new Art(images("buildings",6),images("props",8),trees,corruption,restoration,restored);
    }
    /** Premultiplied-alpha interpolation prevents the opacity dip of stacked fading cutouts. Preparation only. */
    private static BufferedImage blend(BufferedImage a,BufferedImage b,double amount){
        var result=new BufferedImage(a.getWidth(),a.getHeight(),BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<a.getHeight();y++)for(int x=0;x<a.getWidth();x++){
            int ca=a.getRGB(x,y),cb=b.getRGB(x,y);double aa=(ca>>>24)*(1-amount),ab=(cb>>>24)*amount,alpha=aa+ab;
            int c=(int)Math.round(alpha)<<24;
            if(alpha>0)for(int shift=16;shift>=0;shift-=8)c|=(int)Math.round((((ca>>>shift)&255)*aa+((cb>>>shift)&255)*ab)/alpha)<<shift;
            result.setRGB(x,y,c);
        }return result;
    }
    public static double treeCameraLift(double playerX,double playerY,WorldMap.Point root){
        double p=Math.max(0,Math.min(1,(500-Math.hypot(playerX-root.x(),playerY-root.y()))/300));
        return 140*p*p*(3-2*p);
    }
    private static int treeViewport(Graphics2D g){Rectangle clip=g.getClipBounds();return clip!=null&&clip.height<=650?1:0;}
    public static void drawRestoredTree(Graphics2D g,WorldMap.Point root){var a=ready();if(a!=null)drawTreeLayer(g,a.restored[treeViewport(g)],root,1);}
    private static Art ready(){try{return ART.getNow(null);}catch(java.util.concurrent.CompletionException e){return null;}}
    public static BufferedImage building(int i){var a=ready();return a==null?null:a.buildings[i];}
    public static BufferedImage prop(int i){var a=ready();return a==null?null:a.props[i];}
    public static BufferedImage tree(int i){var a=ready();return a==null?null:a.trees[i];}
    public static boolean retiredObject(int x,int y){
        if(TREE_FOOTPRINT.contains(x,y))return true;
        for(int[] h:HOMES)if(x>=h[0]&&x<h[2]&&y>=h[1]&&y<h[3])return true;
        return false;
    }
    public static BufferedImage fit(BufferedImage source,int width,int height){
        if(source==null)throw new IllegalStateException("Town chapter artwork not prepared");
        double s=Math.min(width/(double)source.getWidth(),height/(double)source.getHeight());
        return PixelArt.scale(source,Math.max(1,(int)Math.round(source.getWidth()*s)),Math.max(1,(int)Math.round(source.getHeight()*s)));
    }
    /** State derives solely from the existing boss/story/restoration clocks. */
    public static void drawTree(Graphics2D original,WorldMap.Point root,entity.Enemy boss,
            StoryOverlay intro,boolean defeated,SpiritTreeTransformation restoration,int ticks,int fallFrames){
        var art=ready();if(art==null)return;
        boolean emerging=boss instanceof boss.DarkTitan&&intro.isActive()&&intro.isBossIntro();
        if(boss instanceof boss.DarkTitan&&!emerging)return; // Never hide live combat telegraphs or the Titan.
        double corruption=emerging?Math.min(1,intro.getFrames()/60.0):defeated&&!restoration.isComplete()? .55:0;
        if(fallFrames>=0)corruption=1-.45*Math.min(1,fallFrames/(double)BossFallPresentation.TITAN_DURATION);
        double recovery=restoration.isActive()?restoration.getFrames()/180.0:restoration.isComplete()?1:0;
        Graphics2D g=(Graphics2D)original.create();
        float bodyAlpha=emerging?(float)(1-Math.max(0,Math.min(1,(intro.getFrames()-70)/40.0))):1;
        int viewport=treeViewport(g);
        BufferedImage body=recovery>0?art.restoration[viewport][(int)Math.round(recovery*16)]:art.corruption[viewport][(int)Math.round(corruption*16)];
        drawTreeLayer(g,body,root,bodyAlpha);
        g.setComposite(AlphaComposite.SrcOver);
        for(int i=0;i<14;i++){
            double age=Math.floorMod(ticks+i*19,210)/210.0;
            double fade=Math.sin(age*Math.PI)*bodyAlpha;
            int x=root.x()+(int)(Math.sin(i*2.4+age)*(viewport==1?110:130)),y=root.y()+40-(int)(age*(viewport==1?240:285));
            g.setColor(recovery>.5?new Color(255,222,145,(int)(85*fade)):corruption>.5?new Color(237,85,160,(int)(75*fade)):new Color(233,224,184,(int)(45*fade)));
            if(bodyAlpha>.1)g.fillRect(x,y,2,3);
        }
        g.dispose();
    }
    private static void drawTreeLayer(Graphics2D g,BufferedImage image,WorldMap.Point root,float alpha){
        if(alpha<=0)return;g.setComposite(AlphaComposite.SrcOver.derive(Math.min(1,alpha)));
        g.drawImage(image,root.x()-image.getWidth()/2,root.y()+64-image.getHeight(),null);
    }
}
