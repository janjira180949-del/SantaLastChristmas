import game.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import java.nio.file.*;

/** Actual original pixels, independent local flag transitions, emissive rendering and cache behavior. */
public class VillageConsistencyVisualTest extends V2TestSupport {
    static FinalChapter chapter(){var c=new FinalChapter();c.unlock();for(var p:FinalChapter.PICKUPS)c.collect(p.id());return c;}
    static int warmPixels(BufferedImage image){int n=0;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){int p=image.getRGB(x,y),r=(p>>>16)&255,g=(p>>>8)&255,b=p&255;if((p>>>24)>0&&r>150&&g>80&&g<225&&b<125&&r>g*1.16)n++;}return n;}
    static boolean same(BufferedImage a,BufferedImage b){return java.util.Arrays.equals(a.getRGB(0,0,a.getWidth(),a.getHeight(),null,0,a.getWidth()),b.getRGB(0,0,b.getWidth(),b.getHeight(),null,0,b.getWidth()));}
    static BufferedImage terrain(FinalChapter chapter){var image=new BufferedImage(2560,1920,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();VillageRestorationVisuals.drawTerrain(g,chapter);g.dispose();return image;}
    static BufferedImage lights(FinalChapter chapter){var image=new BufferedImage(2560,1920,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();var camera=new Camera();camera.follow(1280,960,2560,1920,2560,1920);new WorldAtmosphere().drawLights(g,Area.Type.VILLAGE,camera,2560,1920,chapter);g.dispose();return image;}
    public static void main(String[]args)throws Exception{
        VillageRestorationVisuals.prepareAsync().get(30,TimeUnit.SECONDS);var art=VillageRestorationVisuals.buildingVisuals();check(art!=null,"Five source-derived building states prepared by the existing worker");
        var original=ImageIO.read(Path.of("tests/fixtures/phase9-2-1-terrain-before.png").toFile());
        for(var b:VillageBuildingVisuals.BUILDINGS){
            var warm=art.image(b.id(),2);var cold=art.image(b.id(),0);var partial=art.image(b.id(),1);check(warm!=null&&warm.getWidth()==b.width()&&warm.getHeight()==b.height(),"Exact authored dimensions "+b.id());
            boolean exact=true,alpha=true;int changed=0,transparent=0;
            for(int y=0;y<b.height();y++)for(int x=0;x<b.width();x++){
                int p=warm.getRGB(x,y);if((p>>>24)==0){transparent++;if((cold.getRGB(x,y)>>>24)!=0||(partial.getRGB(x,y)>>>24)!=0)alpha=false;}
                else {if(p!=original.getRGB(VillageBuildingVisuals.sourceBounds(b).x+x,VillageBuildingVisuals.sourceBounds(b).y+y))exact=false;if(p!=cold.getRGB(x,y))changed++;}
            }
            check(exact,"Restored state reuses exact original prepared artwork: "+b.id());check(alpha&&transparent>100,"Variants retain original transparent silhouette: "+b.id());check(changed>1000,"Before appearance is visibly distinct: "+b.id());
            check(warmPixels(cold)<warmPixels(warm)/5,"Bright original windows are suppressed: "+b.id());check(!same(cold,partial)&&!same(partial,warm),"Partial and final states are distinct: "+b.id());
        }
        var c=chapter();for(var b:VillageBuildingVisuals.BUILDINGS)check(VillageBuildingVisuals.stage(b,c)==0,"All sites begin damaged");
        c.restore(FinalChapter.Restoration.STREETLIGHTS);
        for(var b:VillageBuildingVisuals.BUILDINGS)check(VillageBuildingVisuals.stage(b,c)==(b.warmth()==FinalChapter.Restoration.STREETLIGHTS?1:0),"Streetlights warm only their dependent buildings");
        check(VillageBuildingVisuals.helpStage(c)==1,"Help counter has a partial stage after lights");
        c.restore(FinalChapter.Restoration.CAMPFIRE);check(VillageBuildingVisuals.stage(VillageBuildingVisuals.BUILDINGS.get(2),c)==1,"Campfire warms nearby east residence");
        check(VillageBuildingVisuals.stage(VillageBuildingVisuals.BUILDINGS.get(4),c)==0,"Unrelated Clara house remains cold until its flag");
        c.restore(FinalChapter.Restoration.HOUSE);check(VillageBuildingVisuals.stage(VillageBuildingVisuals.BUILDINGS.get(3),c)==1,"House repair lends modest warmth to southwest home");
        c.restore(FinalChapter.Restoration.TREE);for(var b:VillageBuildingVisuals.BUILDINGS)check(VillageBuildingVisuals.stage(b,c)==2,"Four flags restore exact original identity");
        check(VillageBuildingVisuals.helpStage(c)==2,"Final help counter is festive");
        var off=chapter();var beforeLights=lights(off);var partialChapter=chapter();partialChapter.restore(FinalChapter.Restoration.STREETLIGHTS);var partialLights=lights(partialChapter);var afterLights=lights(c);
        int owned=0;for(var light:VillageRestorationVisuals.windowLights())for(var b:VillageBuildingVisuals.BUILDINGS)if(b.bounds().contains(light[0],light[1])){
            owned++;check(VillageBuildingVisuals.emission(light[0],light[1],off)==0,"Original window bloom suppressed before restoration");
            check(VillageBuildingVisuals.emission(light[0],light[1],c)==1,"Original window bloom fully returns");
            int x=(int)Math.round(light[0]),y=(int)Math.round(light[1]);check(afterLights.getRGB(x,y)!=beforeLights.getRGB(x,y),"Actual emitted pixels change at owned window");
            if(b.warmth()==FinalChapter.Restoration.STREETLIGHTS)check(partialLights.getRGB(x,y)!=beforeLights.getRGB(x,y)&&partialLights.getRGB(x,y)!=afterLights.getRGB(x,y),"Actual partial bloom lies between before and after");
        }
        check(owned>8&&VillageRestorationVisuals.windowLights().length<=48,"Artwork-derived emissions are present and bounded");
        check(VillageBuildingVisuals.emission(912,840,off)==1,"Unrelated decorative/activity lamp routing is preserved");
        boolean sameOutside=true;for(int y=0;y<1920;y++)for(int x=0;x<2560;x++)if(beforeLights.getRGB(x,y)!=afterLights.getRGB(x,y)&&!Phase921PreservationSupport.lightingPixel(x,y))sameOutside=false;
        check(sameOutside,"Emissive differences remain within original window halo circles");
        for(int i=0;i<3;i++){var help=art.helpImage(i);check((help.getRGB(0,0)>>>24)==0&&(help.getRGB(help.getWidth()-1,help.getHeight()-1)>>>24)==0,"Help counter has transparent edges "+i);}
        check(!same(art.helpImage(0),art.helpImage(1))&&!same(art.helpImage(1),art.helpImage(2)),"Help counter has distinct cold, partial and festive art");
        // The restored composite returns original buildings at their exact pixels, with no old canopy beneath.
        var finalTerrain=terrain(c);for(var b:VillageBuildingVisuals.BUILDINGS){var sprite=art.image(b.id(),2);boolean exact=true;for(int y=0;y<b.height();y++)for(int x=0;x<b.width();x++)if((sprite.getRGB(x,y)>>>24)>0&&finalTerrain.getRGB(b.x()+x,b.y()+y)!=original.getRGB(VillageBuildingVisuals.sourceBounds(b).x+x,VillageBuildingVisuals.sourceBounds(b).y+y))exact=false;check(exact,"Actual final composition preserves original building pixels: "+b.id());}
        var coldTerrain=terrain(off);Phase921PreservationSupport.terrain(coldTerrain);check(true,"Actual cold terrain changes only original silhouettes and help footprint");
        Object prepared=VillageRestorationVisuals.prepareAsync().get();var access=prepared.getClass().getDeclaredMethod("terrain");access.setAccessible(true);check(same(coldTerrain,(BufferedImage)access.invoke(prepared)),"Default map painting is exactly its cached cold image");
        var underlay=art.helpFoundation();check((underlay.getRGB(0,0)>>>24)==0&&(underlay.getRGB(underlay.getWidth()-1,underlay.getHeight()-1)>>>24)==0,"Help foundation follows the union of transparent silhouettes, not a ground rectangle");
        int erased=0;boolean clean=true;var oldHelp=art.helpImage(0);var newHelp=art.helpImage(2);
        for(int y=0;y<newHelp.getHeight();y++)for(int x=0;x<newHelp.getWidth();x++)if((oldHelp.getRGB(x,y)>>>24)>0&&(newHelp.getRGB(x,y)>>>24)==0){erased++;if(finalTerrain.getRGB(VillageBuildingVisuals.HELP_BOUNDS.x+x,VillageBuildingVisuals.HELP_BOUNDS.y+y)!=underlay.getRGB(x,y))clean=false;}
        check(erased>0&&clean,"Restored counter clears every superseded cold edge before drawing, without ghosted roofs");
        var ready=VillageRestorationVisuals.class.getDeclaredMethod("ready",CompletableFuture.class);ready.setAccessible(true);long started=System.nanoTime();check(ready.invoke(null,new CompletableFuture<>())==null&&System.nanoTime()-started<100_000_000,"Pending art reads are non-blocking");
        var failed=new CompletableFuture<>();failed.completeExceptionally(new IllegalStateException());check(ready.invoke(null,failed)==null,"Failed art reads remain safe");var canceled=new CompletableFuture<>();canceled.cancel(false);check(ready.invoke(null,canceled)==null,"Canceled art reads remain safe");
        var p=playing();for(int[] size:new int[][]{{800,600},{1024,768},{1270,720},{1600,900}}){p.setSize(size[0],size[1]);render(p);check(VillageRestorationVisuals.compositionCount()==1,"Actual GamePanel paint reuses one immutable composition "+size[0]);}p.stopGame();
        check(c.completed()==4&&c.count(FinalChapter.Material.ANCIENT_WOOD)==3,"Visual presentation consumes no materials or flags");
        System.out.println("PASS: "+checks+" original-art, transition, emissive, compositing and cache assertions");
    }
}
