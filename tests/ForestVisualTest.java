import game.*;
import entity.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

/** Actual Forest renderer, bounded world effects, static caching and unchanged navigable footprints. */
public class ForestVisualTest extends V2TestSupport {
    static final Path OUTPUT=Path.of("build/forest-overhaul/captures");
    static GamePanel forest()throws Exception {
        GamePanel p=playing();completeOpeningDeliveries(p);story(p).startAfterDialogue(p.getChildren().get(0));story(p).recordVillageCleared();
        check(areas(p).tryTravel(Area.Type.FOREST),"Existing Village to Forest destination");call(p,"loadCurrentAreaContent");return p;
    }
    static void position(GamePanel p,int x,int y)throws Exception {
        var safe=WorldMap.forArea(Area.Type.FOREST).safePosition(x-16,y-21,32,58);santa(p).setPosition(safe.x(),safe.y()+8);
    }
    static void save(GamePanel p,String name)throws Exception {ImageIO.write(render(p),"png",OUTPUT.resolve(name+".png").toFile());}
    static BufferedImage effects(FrozenForestVisuals fx,Camera camera,int ticks)throws Exception {
        BufferedImage image=new BufferedImage(800,600,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        g.translate(-camera.getX(),-camera.getY());fx.drawGroundEffects(g,camera,800,600,ticks,java.util.List.of());g.dispose();return image;
    }
    public static void main(String[]args)throws Exception {
        Files.createDirectories(OUTPUT);long start=System.nanoTime();var future=FrozenForestVisuals.prepareAsync();
        var art=future.get(30,TimeUnit.SECONDS);double preparationMs=(System.nanoTime()-start)/1e6;
        check(art.getWidth()==3072&&art.getHeight()==2304,"Full Forest art loads at actual dimensions");
        check(future==FrozenForestVisuals.prepareAsync()&&art==future.getNow(null)&&FrozenForestVisuals.buildCount()==1,"One shared prepared image and one composition job");
        check(FrozenForestVisuals.sourceForPresentation()!=null&&FrozenForestVisuals.artworkLightCount()<=48,"Quest source and bounded original emissive anchors share asynchronous preparation");
        // Inject a pending cache into the real draw path. A test watchdog releases it if painting blocks.
        var cache=FrozenForestVisuals.class.getDeclaredField("preparation");cache.setAccessible(true);
        var fallback=FrozenForestVisuals.class.getDeclaredField("source");fallback.setAccessible(true);Object oldSource=fallback.get(null);
        var pending=new java.util.concurrent.CompletableFuture<BufferedImage>();cache.set(null,pending);fallback.set(null,null);
        var placeholder=new BufferedImage(800,600,1);var pg=placeholder.createGraphics();
        java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
        Thread paint=new Thread(()->{try{javax.swing.SwingUtilities.invokeAndWait(()->new FrozenForestVisuals().drawTerrain(pg));}catch(Throwable error){failure.set(error);}},"forest-test-paint-watchdog");paint.setDaemon(true);
        try {
            paint.start();paint.join(1000);boolean returned=!paint.isAlive()&&!pending.isDone();pending.complete(art);paint.join(1000);
            check(returned&&failure.get()==null,"Actual EDT draw returns while preparation is still pending");
            var missing=new java.util.concurrent.CompletableFuture<BufferedImage>();missing.completeExceptionally(new IllegalStateException("Test missing asset"));cache.set(null,missing);
            var unavailable=new FrozenForestVisuals();unavailable.drawTerrain(pg);check(!unavailable.isPrepared()&&(placeholder.getRGB(400,300)>>>24)==255,"Unavailable cache draws graceful non-blocking fallback");
            var raced=new java.util.concurrent.CompletableFuture<BufferedImage>() {
                @Override public BufferedImage getNow(BufferedImage ignored){throw new java.util.concurrent.CompletionException(new IllegalStateException("Completion race fixture"));}
            };
            cache.set(null,raced);pg.setColor(Color.MAGENTA);pg.fillRect(0,0,800,600);new FrozenForestVisuals().drawTerrain(pg);
            check(placeholder.getRGB(400,300)==0xff486383,"Failure arriving during completed-image read is caught without breaking paint");
            var canceled=new java.util.concurrent.CompletableFuture<BufferedImage>();canceled.cancel(false);cache.set(null,canceled);new FrozenForestVisuals().drawTerrain(pg);
            check(placeholder.getRGB(400,300)==0xff486383,"Interrupted preparation also retains safe fallback");
            var target=new quest.StoryQuestManager().getTargets().get(0);
            check(target.collect()&&target.getPickupVisual().age()==0,"Unavailable source cannot block or prevent gift collection");
            for(int i=0;i<33;i++)target.updateVisual();check(!target.getPickupVisual().isActive(),"Missing-art pickup retains exact existing 33-tick lifecycle");
        } finally {cache.set(null,future);fallback.set(null,oldSource);pg.dispose();}
        var map=WorldMap.forArea(Area.Type.FOREST);var points=java.util.Map.copyOf(map.points());String solids=map.solids().toString();
        check(points.get("landmarks/Crystal 1").equals(new WorldMap.Point(976,1520)),"First objective unchanged");
        check(points.get("landmarks/Crystal 2").equals(new WorldMap.Point(2032,1040)),"Second objective unchanged");
        check(points.get("landmarks/Crystal 3").equals(new WorldMap.Point(2640,1712)),"Third objective unchanged");
        for(var entry:map.points().entrySet())if(entry.getKey().contains("Crystal")||entry.getKey().startsWith("exit/")||entry.getKey().startsWith("entry/")) {
            var point=entry.getValue();var safe=map.safePosition(point.x()-16,point.y()+35,32,58);
            check(!util.Collision.intersectsAny(new java.awt.geom.Rectangle2D.Double(safe.x(),safe.y(),32,58),map.solids()),"Physical approach remains free: "+entry.getKey());
        }
        FrozenForestVisuals fx=new FrozenForestVisuals();Camera camera=new Camera();camera.follow(1450,850,800,600,map.width(),map.height());
        var a=effects(fx,camera,0);var b=effects(fx,camera,240);
        check(!Arrays.equals(a.getRGB(0,0,800,600,null,0,800),b.getRGB(0,0,800,600,null,0,800)),"Actual slow fog moves with shared tick clock");
        int maximumAlpha=0;for(int c:a.getRGB(0,0,800,600,null,0,800))maximumAlpha=Math.max(maximumAlpha,c>>>24);
        check(maximumAlpha<100,"Ground light/fog stays translucent");
        for(int t=0;t<360000;t+=1739)check(fx.visibleFogCount(camera,800,600,t)<=FrozenForestVisuals.FOG_BUDGET,"Fixed fog budget over complete clock wrap");
        // Effect calls cannot spill out of a caller's clipping region or change graphics state.
        var clipped=new BufferedImage(800,600,2);var cg=clipped.createGraphics();cg.setClip(0,120,800,360);var before=cg.getComposite();
        fx.drawGroundEffects(cg,new Camera(),800,600,0,java.util.List.of());check(before.equals(cg.getComposite()),"Ground effects restore caller composite");cg.dispose();
        boolean clear=true;for(int y=0;y<100;y++)for(int x=0;x<800;x++)clear&=(clipped.getRGB(x,y)>>>24)==0;
        check(clear,"Effects respect clipping; HUD remains a subsequent screen pass");
        GamePanel p=forest();int[][] views={{250,1168},{620,620},{1320,890},{1570,1550},{2032,1095},{2640,1750},{2032,690},{2990,1168}};
        String[] names={"01-entrance","02-dense-woodland","03-abandoned-camp","04-dark-clearing","05-crystal-objective","06-black-frost","09-frozen-ruins","10-east-exit"};
        for(int i=0;i<views.length;i++){position(p,views[i][0],views[i][1]);save(p,names[i]);}
        var gift=story(p).getTargets().get(0);at(santa(p),map.point("items/Toby Gift"));tap(p,java.awt.event.KeyEvent.VK_E);
        check(gift.isCollected()&&story(p).isGiftFound()&&!story(p).isLostGiftComplete(),"Real gift pickup retains return-to-Toby requirement");save(p,"14-gift-found");tick(p,33);save(p,"15-gift-collected-terrain");
        check(!gift.getPickupVisual().isActive(),"Actual GamePanel pickup timeline is unchanged");
        var patch=ForestEnvironmentLayers.giftPatch();var repair=new BufferedImage(96,128,1);var rg=repair.createGraphics();
        rg.translate(-896,-696);new FrozenForestVisuals().drawCollectedPatch(rg,gift);rg.dispose();
        check(Arrays.equals(patch.getRGB(0,0,96,128,null,0,96),repair.getRGB(0,0,96,128,null,0,96)),"Collected gift uses exact cached Forest terrain without old white-road rectangle");
        var atmosphere=(WorldAtmosphere)field(p,"worldAtmosphere");var lightCache=WorldAtmosphere.class.getDeclaredField("lights");lightCache.setAccessible(true);
        check(((double[][][])lightCache.get(atmosphere))[Area.Type.FOREST.ordinal()]==null,"Live Forest paint does not run the old synchronous source decode/light scan");
        at(santa(p),encounters(p).get("forest-clearing").getCenter());tick(p,1);check(encounters(p).getActive()!=null,"Existing Forest combat renders in new environment");
        save(p,"07a-encounter-warning");tick(p,60);check(enemies(p).size()==3,"Actual mixed zombies appear after full warning");save(p,"07-zombie-encounter");
        call(p,"loadCurrentAreaContent");check(enemies(p).isEmpty()&&encounters(p).getActive()==null,"Leaving presentation drops active encounter and effect ownership");
        p.setSize(3072,2304);position(p,176,1168);save(p,"08-forest-overview");p.setSize(1270,720);position(p,1320,890);
        var image=new BufferedImage(1270,720,1);var g=image.createGraphics();for(int i=0;i<20;i++)p.paint(g);long[] timings=new long[80];
        for(int i=0;i<timings.length;i++){long n=System.nanoTime();p.paint(g);timings[i]=System.nanoTime()-n;}g.dispose();Arrays.sort(timings);
        Files.writeString(Path.of("build/forest-overhaul/forest-render-performance.json"),String.format(java.util.Locale.ROOT,"{\"width\":1270,\"height\":720,\"median_ms\":%.3f,\"p95_ms\":%.3f,\"preparation_wait_ms\":%.3f,\"static_rgb_bytes\":%d,\"compositions\":%d}%n",timings[40]/1e6,timings[76]/1e6,preparationMs,3072*2304*4,FrozenForestVisuals.buildCount()));
        check(FrozenForestVisuals.buildCount()==1,"Repeated full paint never decodes or recomposes Forest");
        check(points.equals(map.points())&&solids.equals(map.solids().toString()),"Presentation leaves every marker and collider identical");
        p.stopGame();check(FrozenForestVisuals.prepareAsync().isDone(),"Finished shared preparation leaves no per-panel job on shutdown");
        System.out.println("PASS: "+checks+" Forest renderer/atmosphere/cache/navigation assertions; actual review PNGs at "+OUTPUT);
    }
}
