package game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import util.LightBloom;

/** One shared immutable workshop cache, nonblocking presentation and bounded effects. */
public final class SantaBaseVisuals {
    private static final AtomicInteger COMPOSITIONS=new AtomicInteger();
    private static final CompletableFuture<BufferedImage> PREPARED=CompletableFuture.supplyAsync(()-> {
        COMPOSITIONS.incrementAndGet();return SantaBaseEnvironmentLayers.build();
    });
    private static final int[][] LIGHTS={{976,366,86},{816,340,150},{240,515,95},{1357,531,95},{608,165,85},{992,165,85},{544,801,90},{1024,801,90},{474,656,75},{1250,698,75},{690,1005,85},{920,1005,85},{690,1131,80},{920,1131,80}};
    private static final Color MOTE=new Color(255,221,147,130), SNOW=new Color(222,235,247,125);
    private final CompletableFuture<BufferedImage> artwork;
    public SantaBaseVisuals(){this(PREPARED);}
    /** Alternate future supports deterministic pending/failed artwork fixtures. */
    public SantaBaseVisuals(CompletableFuture<BufferedImage> artwork){this.artwork=java.util.Objects.requireNonNull(artwork);}
    public static CompletableFuture<BufferedImage> prepareAsync(){return PREPARED;}
    public static int compositionCount(){return COMPOSITIONS.get();}
    public static int snowBudget(){return 64;}
    public static int spiritMoteBudget(){return 10;}
    public static void drawCachedTerrain(Graphics2D g){drawTerrain(g,PREPARED);}
    public void drawTerrain(Graphics2D g){drawTerrain(g,artwork);}
    private static void drawTerrain(Graphics2D g,CompletableFuture<BufferedImage> future) {
        BufferedImage image=null;
        try {image=future.getNow(null);} catch(CompletionException|CancellationException unavailable){ /* Playable fallback. */ }
        if(image!=null){g.drawImage(image,0,0,null);return;}
        // Geometry-aligned fallback keeps every wall and large object visible while preparing.
        g.setColor(new Color(73,96,127));g.fillRect(0,0,1600,1280);
        g.setColor(new Color(80,54,41));g.fillRect(224,160,1152,768);g.fillRect(704,928,192,64);
        g.setColor(new Color(35,34,42));
        for(var solid:WorldMap.forArea(Area.Type.SANTA_BASE).solids())g.fill(solid);
    }
    public void drawLights(Graphics2D g,int ticks,Camera camera,int width,int height) {
        for(int i=0;i<LIGHTS.length;i++) {
            int[] p=LIGHTS[i];if(p[0]+p[2]<camera.getX()||p[0]-p[2]>camera.getX()+width||p[1]+p[2]<camera.getY()||p[1]-p[2]>camera.getY()+height)continue;
            double pulse=.94+.06*Math.sin(ticks/95.0+i);
            LightBloom.draw(g,p[0],p[1],p[2],(i==0?.21:.12)*pulse,LightBloom.GOLD);
        }
    }
    public void drawParticles(Graphics2D g,int ticks,Camera camera,int width,int height) {
        g.setColor(SNOW);
        for(int i=0;i<snowBudget();i++) {
            int x=(int)((i*431+ticks*.18)%1600),y=994+(int)((i*113+ticks*.3)%286);
            if(x>=camera.getX()&&x<camera.getX()+width&&y>=camera.getY()&&y<camera.getY()+height)g.fillRect(x,y,i%5==0?2:1,2);
        }
        g.setColor(MOTE);
        for(int i=0;i<spiritMoteBudget();i++) {
            int x=976+(int)(Math.sin(ticks/110.0+i*2.1)*(14+i)),y=397-Math.floorMod(ticks/5+i*11,75);
            if(x>=camera.getX()&&x<camera.getX()+width&&y>=camera.getY()&&y<camera.getY()+height)g.fillOval(x,y,2,2);
        }
    }
}
