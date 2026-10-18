package game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import util.ImageAssets;
import util.LightBloom;

/** Shared asynchronous static art; bounded ground effects use the existing world clock. */
public final class FrozenForestVisuals {
    public static final int FOG_BUDGET=12;
    private static final Color FALLBACK=new Color(72,99,131);
    private static final Color OBJECTIVE_BACKING=new Color(8,17,33,155);
    private static final AtomicInteger BUILDS=new AtomicInteger();
    private static volatile BufferedImage source;
    private static volatile int[][] artworkLights=new int[0][2];
    private static CompletableFuture<BufferedImage> preparation;
    private static final BufferedImage MIST=mist();
    private static final AlphaComposite[] ALPHAS=new AlphaComposite[256];
    static {for(int i=0;i<ALPHAS.length;i++)ALPHAS[i]=AlphaComposite.SrcOver.derive(i/255f);}
    public FrozenForestVisuals(){prepareAsync();}
    public static synchronized CompletableFuture<BufferedImage> prepareAsync() {
        if(preparation==null)preparation=CompletableFuture.supplyAsync(()->{
            source=ImageAssets.load("assets/world/forest_clean.png");
            if(source==null)throw new IllegalStateException("Missing Forest artwork");
            artworkLights=scanArtworkLights(source);
            BufferedImage result=ForestEnvironmentLayers.build(source);BUILDS.incrementAndGet();return result;
        });
        return preparation;
    }
    public static int buildCount(){return BUILDS.get();}
    /** Same original emissive-pixel criterion, prepared from the already decoded Forest source. */
    private static int[][] scanArtworkLights(BufferedImage art) {
        int[][] anchors=new int[48][2];int count=0;
        for(int y=0;y<art.getHeight()&&count<48;y+=96)for(int x=0;x<art.getWidth()&&count<48;x+=96) {
            int pixels=0,sx=0,sy=0;
            for(int py=y;py<Math.min(y+96,art.getHeight());py+=3)for(int px=x;px<Math.min(x+96,art.getWidth());px+=3) {
                int c=art.getRGB(px,py),r=(c>>>16)&255,green=(c>>>8)&255,b=c&255;
                if(r>175&&green>85&&green<205&&r>green*1.2&&b<75){pixels++;sx+=px;sy+=py;}
            }
            if(pixels>=9){anchors[count][0]=sx/pixels;anchors[count++][1]=sy/pixels;}
        }
        return java.util.Arrays.copyOf(anchors,count);
    }
    public static int artworkLightCount(){return artworkLights.length;}
    /** Existing quest presentation can reuse this asynchronous decode without blocking collection. */
    public static BufferedImage sourceForPresentation(){prepareAsync();return source;}
    public void drawCollectedPatch(Graphics2D g,quest.StoryQuestTarget target) {
        if(!target.isCollected())return;
        BufferedImage patch=ForestEnvironmentLayers.giftPatch();
        if(isPrepared()&&patch!=null) {
            var bounds=target.getBounds();g.drawImage(patch,(int)bounds.getCenterX()-48,(int)bounds.getCenterY()-88,null);
        } else target.drawCollectedPatch(g);
    }
    public boolean isPrepared(){return prepareAsync().isDone()&&!prepareAsync().isCompletedExceptionally();}
    /** No wait, decoding or static composition is permitted in a paint callback. */
    public void drawTerrain(Graphics2D g) {
        CompletableFuture<BufferedImage> work=prepareAsync();
        BufferedImage ready=null;
        try {ready=work.getNow(null);}
        catch(java.util.concurrent.CompletionException|java.util.concurrent.CancellationException unavailable) {
            // Completion can fail between paint's reads; preserve the source/fallback in that race too.
        }
        BufferedImage selected=ready!=null?ready:source;
        if(selected!=null)g.drawImage(selected,0,0,null);
        else {g.setColor(FALLBACK);g.fillRect(0,0,3072,2304);}
    }
    private static BufferedImage mist() {
        BufferedImage image=new BufferedImage(384,112,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
        for(int i=0;i<5;i++) {
            int x=35+i*75,y=50+(i%2)*15;
            g.setPaint(new RadialGradientPaint(x,y,65,new float[]{0,1},new Color[]{new Color(166,193,217,17),new Color(166,193,217,0)}));
            g.fillOval(x-65,y-65,130,130);
        }
        g.dispose();return image;
    }
    public int visibleFogCount(Camera camera,int width,int height,int ticks) {
        int count=0;
        for(int i=0;i<FOG_BUDGET;i++)if(visible(camera,width,height,fogX(i,ticks),fogY(i),620,120))count++;
        return count;
    }
    private static int fogX(int i,int ticks){return (int)((i*529+ticks*.075)%3552)-480;}
    private static int fogY(int i){return 540+(i*173)%1500;}
    private static boolean visible(Camera c,int w,int h,int x,int y,int imageW,int imageH) {
        return x<c.getX()+w&&x+imageW>c.getX()&&y<c.getY()+h&&y+imageH>c.getY();
    }
    /** Called below actors, attack warnings, crystal sprites and all screen-space UI. */
    public void drawGroundEffects(Graphics2D original,Camera camera,int width,int height,int ticks,List<BlackFrostCrystal> crystals) {
        Graphics2D g=(Graphics2D)original.create();
        try {
            for(int i=0;i<FOG_BUDGET;i++) {
                int x=fogX(i,ticks),y=fogY(i);
                if(visible(camera,width,height,x,y,620,120)) {
                    g.setComposite(ALPHAS[(int)(255*(.76+.12*Math.sin(ticks/180.0+i)))]);
                    g.drawImage(MIST,x,y,620,120,null);
                }
            }
            g.setComposite(AlphaComposite.SrcOver);
            int index=0;
            for(int[] light:artworkLights) {
                if(visible(camera,width,height,light[0]-60,light[1]-60,120,120))
                    LightBloom.draw(g,light[0],light[1],32,.035*(.9+.1*Math.sin(ticks/75.0+index*1.7)),LightBloom.GOLD);
                index++;
            }
            for(var crystal:crystals) {
                var p=crystal.getPosition();if(!visible(camera,width,height,p.x()-170,p.y()-135,340,270))continue;
                ForestLandmarkArt.drawFractures(g,crystal);
                if(crystal.getState()!=BlackFrostCrystal.State.BROKEN) {
                    LightBloom.draw(g,p.x(),p.y()+10,44,.07+.01*Math.sin(ticks/100.0+p.x()),LightBloom.CYAN);
                    g.setColor(OBJECTIVE_BACKING);g.fillRoundRect(p.x()-135,ForestLandmarkArt.labelY(crystal.getId(),p.y())-18,270,26,10,10);
                }
            }
            for(int[] lamp:ForestEnvironmentLayers.LANTERNS)if(visible(camera,width,height,lamp[0]-56,lamp[1]-103,112,112)) {
                double pulse=.95+.05*Math.sin(ticks/85.0+lamp[0]);
                LightBloom.draw(g,lamp[0],lamp[1]-47,56,.14*pulse,LightBloom.GOLD);
            }
        } finally {g.dispose();}
    }
}
