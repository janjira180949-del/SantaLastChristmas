import game.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;

/** Real GamePanel views plus deterministic cache, fallback, light/weather and camera checks. */
public class SantaBaseVisualTest extends V2TestSupport {
    static final Path OUTPUT=Path.of("build/santa-base/captures");
    public static void main(String[]args)throws Exception {
        Files.createDirectories(OUTPUT);long start=System.nanoTime();var future=SantaBaseVisuals.prepareAsync();BufferedImage art=future.get(30,TimeUnit.SECONDS);double cold=(System.nanoTime()-start)/1e6;
        check(art.getWidth()==1600&&art.getHeight()==1280,"Actual workshop composition is complete");
        check(SantaBaseVisuals.compositionCount()==1&&future==SantaBaseVisuals.prepareAsync()&&art==future.getNow(null),"Shared static cache composed once");
        for(int[] rectangle:new int[][]{{704,352,192,128},{940,340,70,100},{256,256,256,192},{1056,192,288,128},{288,672,224,128},{1056,704,224,128},{704,992,192,256}}) {
            Set<Integer> colors=new HashSet<>();for(int y=rectangle[1];y<rectangle[1]+rectangle[3];y+=3)for(int x=rectangle[0];x<rectangle[0]+rectangle[2];x+=3)colors.add(art.getRGB(x,y));
            check(colors.size()>70,"Landmark/material has detailed pixels: "+Arrays.toString(rectangle));
        }
        GamePanel p=SantaBaseNavigationTest.opening();
        int[][] positions={{800,731},{800,520},{960,500},{544,410},{1300,352},{520,805},{1008,850},{800,1020},{800,1064}};
        String[] names={"02-spawn","03-christmas-tree","04-spirit-lantern","05-machinery","06-gift-storage","07-letter-desk","08-journey-map","09-exterior","10-departure-gate"};
        p.setSize(1600,1280);save(p,"01-overview");
        p.setSize(1270,720);for(int i=0;i<positions.length;i++){santa(p).setPosition(positions[i][0],positions[i][1]);check(!util.Collision.intersectsAny(santa(p).getBounds(),WorldMap.forArea(Area.Type.SANTA_BASE).solids()),"Capture actor is not inside scenery: "+names[i]);save(p,names[i]);}
        for(int[] viewport:new int[][]{{800,600},{1270,720},{1800,1400}})for(int[] pos:new int[][]{{0,0},{1600,1280},{800,731}}){p.setSize(viewport[0],viewport[1]);santa(p).setPosition(pos[0],pos[1]);render(p);Camera camera=(Camera)field(p,"camera");check(camera.getX()>=0&&camera.getY()>=0&&camera.getX()<=Math.max(0,1600-viewport[0])&&camera.getY()<=Math.max(0,1280-viewport[1]),"Camera clamps for every map/viewport edge");}
        var pending=new CompletableFuture<BufferedImage>();var fx=new SantaBaseVisuals(pending);var out=new BufferedImage(800,600,2);var g=out.createGraphics();final var paintGraphics=g;
        GamePanel pendingPanel=new GamePanel(null,fx);pendingPanel.setSize(800,600);pendingPanel.beginAdventure();closeOverlay(pendingPanel);
        java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();Thread watchdog=new Thread(()->{try{javax.swing.SwingUtilities.invokeAndWait(()->pendingPanel.paint(paintGraphics));}catch(Throwable e){error.set(e);}},"base-edt-test");watchdog.setDaemon(true);watchdog.start();watchdog.join(1000);boolean nonblocking=!watchdog.isAlive()&&!pending.isDone();pending.complete(art);watchdog.join(1000);pendingPanel.stopGame();check(nonblocking&&error.get()==null,"Actual GamePanel EDT paint returns while composition is pending");
        for(boolean canceled:new boolean[]{false,true}){var unavailable=new CompletableFuture<BufferedImage>();if(canceled)unavailable.cancel(false);else unavailable.completeExceptionally(new IllegalStateException("missing art fixture"));new SantaBaseVisuals(unavailable).drawTerrain(g);check((out.getRGB(400,400)>>>24)==255,"Failed/canceled artwork keeps opaque playable fallback");}g.dispose();
        var effects=new BufferedImage(1600,1280,2);g=effects.createGraphics();Camera c=new Camera();fx.drawParticles(g,750,c,1600,1280);g.dispose();
        int upperPixels=0;for(int y=0;y<994;y++)for(int x=0;x<1600;x++)if((effects.getRGB(x,y)>>>24)>0){upperPixels++;check(x>=950&&x<=1005&&y>=320&&y<=399,"Only Spirit motes occur indoors");}
        check(upperPixels<=40&&SantaBaseVisuals.spiritMoteBudget()==10&&SantaBaseVisuals.snowBudget()==64,"Particles remain bounded and exterior snow cannot cover interior");
        var commonWeather=new WorldAtmosphere();var compatibility=new BufferedImage(800,600,2);var cg=compatibility.createGraphics();
        commonWeather.drawDepth(cg,Area.Type.SANTA_BASE,WorldMap.forArea(Area.Type.SANTA_BASE),c,800,600);
        commonWeather.drawLights(cg,Area.Type.SANTA_BASE,c,800,600);
        commonWeather.drawSnow(cg,Area.Type.SANTA_BASE,WorldMap.forArea(Area.Type.SANTA_BASE),c,800,600);cg.dispose();
        check(commonWeather.particleBudget(Area.Type.SANTA_BASE)==0&&Arrays.stream(compatibility.getRGB(0,0,800,600,null,0,800)).allMatch(v->v==0),"Generic weather safely leaves Base effects to its presentation owner");
        for(int i=0;i<20;i++){var repeat=out.createGraphics();fx.drawTerrain(repeat);repeat.dispose();}check(SantaBaseVisuals.compositionCount()==1,"Repeated paints never decode/recompose");
        p.setSize(1270,720);santa(p).setPosition(800,731);for(int i=0;i<25;i++)render(p);double[] times=new double[100];for(int i=0;i<times.length;i++){long t=System.nanoTime();render(p);times[i]=(System.nanoTime()-t)/1e6;}Arrays.sort(times);
        Files.writeString(Path.of("build/santa-base/render-performance.json"),"{\"cold_async_wait_ms\":"+cold+",\"median_paint_ms\":"+times[50]+",\"p95_paint_ms\":"+times[95]+",\"compositions\":"+SantaBaseVisuals.compositionCount()+",\"cached_floor_bytes\":8192000}\n");p.stopGame();
        System.out.println("PASS: "+checks+" Base renderer/cache/effects/camera assertions; 10 actual GamePanel views");
    }
    private static void save(GamePanel p,String name)throws Exception {ImageIO.write(render(p),"png",OUTPUT.resolve(name+".png").toFile());}
}
