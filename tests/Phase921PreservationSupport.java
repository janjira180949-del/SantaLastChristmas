import game.*;
import java.nio.file.*;
import java.util.*;
import java.awt.image.BufferedImage;

/** Authenticated local visual revision; every older snapshot remains immutable. */
final class Phase921PreservationSupport {
    static byte[] previous(String path,byte[] current){
        current=Phase931PreservationSupport.previous(path,current);
        try{
            Properties authorized=new Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_2_1_AUTHORIZED.properties"))){authorized.load(in);}
            String expected=authorized.getProperty(path);if(expected==null)return current;
            String hash=ForestPreservationSupport.hash(current);
            for(String prefix:List.of("PHASE9_2_1","PHASE9_2","QUEST_FLOW","JOURNEY_HOME","FINAL_CHAPTER","VILLAGE_INTEGRATION","VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")){
                byte[] archived=JourneyHomePreservationSupport.archived(path,prefix);
                if(archived!=null&&hash.equals(ForestPreservationSupport.hash(archived)))return current;
            }
            if(!hash.equals(expected))throw new AssertionError("Unreviewed Phase 9.2.1 source: "+path);
            return JourneyHomePreservationSupport.archived(path,"PHASE9_2_1");
        }catch(Exception failure){throw new AssertionError(failure);}
    }
    private static BufferedImage fixture(String name)throws Exception{
        Properties hashes=new Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_2_1_RENDER_BEFORE.properties"))){hashes.load(in);}
        var path=Path.of("tests/fixtures/phase9-2-1-"+name+"-before.png");
        if(!ForestPreservationSupport.hash(Files.readAllBytes(path)).equals(hashes.getProperty(name)))throw new AssertionError("Unauthenticated Phase 9.2 original pixels: "+name);
        return javax.imageio.ImageIO.read(path.toFile());
    }
    private static final BufferedImage[] MASKS=masks();
    private static BufferedImage[] masks(){
        var result=new BufferedImage[VillageBuildingVisuals.BUILDINGS.size()];
        for(int i=0;i<result.length;i++)try{var b=VillageBuildingVisuals.BUILDINGS.get(i);result[i]=util.PixelArt.scale(javax.imageio.ImageIO.read(Path.of("SantaLastChristmas_MapPackage/assets/environment/buildings/"+b.asset()+".png").toFile()),b.width(),b.height());}catch(Exception error){throw new AssertionError(error);}
        return result;
    }
    static boolean artworkPixel(int x,int y){
        if(VillageBuildingVisuals.HELP_BOUNDS.contains(x,y))return true;
        for(int i=0;i<MASKS.length;i++){var b=VillageBuildingVisuals.BUILDINGS.get(i);var origin=VillageBuildingVisuals.sourceBounds(b);int px=x-origin.x,py=y-origin.y;if(px>=0&&py>=0&&px<b.width()&&py<b.height()&&(MASKS[i].getRGB(px,py)>>>24)>0)return true;}return false;
    }
    static boolean lightingPixel(int x,int y){
        var lights=VillageRestorationVisuals.windowLights();if(lights==null)return false;
        for(var light:lights)for(var b:VillageBuildingVisuals.BUILDINGS)if(b.bounds().contains(light[0],light[1])){
            if(Math.hypot(x-Math.round(light[0]),y-Math.round(light[1]))<=33)return true;
            if(b.id().equals("ben-route-house")&&Math.hypot(x-Math.round(light[0]-326),y-Math.round(light[1]-200))<=33)return true;
        }
        return false;
    }
    static BufferedImage terrain(BufferedImage current)throws Exception{
        current=Phase1WorldPreservationSupport.terrain(false,current);
        var before=fixture("terrain");
        for(int y=0;y<1920;y++)for(int x=0;x<2560;x++)if(current.getRGB(x,y)!=before.getRGB(x,y)&&!artworkPixel(x,y))throw new AssertionError("Unrelated terrain changed: "+x+","+y);
        return before;
    }
    static BufferedImage frame(int area,BufferedImage current)throws Exception{
        current=Phase931PreservationSupport.frame(area,current);
        if(area!=0)return current;
        var before=fixture("frame");var camera=new Camera();camera.follow(1312,1181,1270,720,2560,1920);
        if(current.getWidth()!=1270||current.getHeight()!=720)throw new AssertionError("Unexpected historical Village viewport");
        for(int y=0;y<720;y++)for(int x=0;x<1270;x++)if(current.getRGB(x,y)!=before.getRGB(x,y)){
            int wx=x+camera.getX(),wy=y+camera.getY();
            if(!artworkPixel(wx,wy)&&!lightingPixel(wx,wy))throw new AssertionError("Unrelated rendered Village pixel: "+x+","+y);
        }
        return before;
    }
}
