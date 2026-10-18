import game.*;import java.nio.file.*;import java.util.*;import java.awt.*;import java.awt.image.BufferedImage;

/** Exact authenticated Forest-only revision projected ahead of immutable older guards. */
final class Phase931PreservationSupport {
    static final Set<String> ASSETS=Set.of("assets/references/phase9_3_1_forest_reference.png","assets/world/forest_landmarks/tents.png","assets/world/forest_landmarks/crystals.png","assets/world/forest_landmarks/healing_shrine.png","assets/world/forest_landmarks/README.md");
    static byte[] previous(String path,byte[] current){
        current=ForestRefinementPreservationSupport.previous(path,current);
        try{
            Properties allowed=new Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_3_1_AUTHORIZED.properties"))){allowed.load(in);}
            if(!allowed.containsKey(path))return current;
            String hash=ForestPreservationSupport.hash(current);
            for(String prefix:java.util.List.of("PHASE9_3_1","PHASE9_2_1","PHASE9_2","QUEST_FLOW","JOURNEY_HOME","FINAL_CHAPTER","VILLAGE_INTEGRATION","VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")){
                byte[] older=JourneyHomePreservationSupport.archived(path,prefix);if(older!=null&&hash.equals(ForestPreservationSupport.hash(older)))return current;
            }
            if(!hash.equals(allowed.getProperty(path)))throw new AssertionError("Unauthenticated Forest presentation revision: "+path);
            return JourneyHomePreservationSupport.archived(path,"PHASE9_3_1");
        }catch(Exception failure){throw new AssertionError(failure);}
    }
    static BufferedImage fixture(String name)throws Exception{
        Properties hashes=new Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_3_1_RENDER_BEFORE.properties"))){hashes.load(in);}
        var file=Path.of("tests/fixtures/phase9-3-1-"+name+"-before.png");
        if(!ForestPreservationSupport.hash(Files.readAllBytes(file)).equals(hashes.getProperty(name)))throw new AssertionError("Unauthenticated original Forest pixels: "+name);
        return javax.imageio.ImageIO.read(file.toFile());
    }
    static boolean terrainRegion(int x,int y){
        if(new Rectangle(1120,690,485,310).contains(x,y))return true;
        for(int[] lamp:ForestEnvironmentLayers.LANTERNS)if(new Rectangle(lamp[0]-24,lamp[1]-66,48,90).contains(x,y))return true;
        for(var p:java.util.List.of(new WorldMap.Point(976,1520),new WorldMap.Point(2032,1040),new WorldMap.Point(2640,1712)))if(new Rectangle(p.x()-278,p.y()-218,556,436).contains(x,y))return true;
        return false;
    }
    static boolean frameRegion(int x,int y){
        if(terrainRegion(x,y))return true;
        for(int[] lamp:ForestEnvironmentLayers.LANTERNS)if(new Rectangle(lamp[0]-106,lamp[1]-153,212,212).contains(x,y))return true;
        for(var p:java.util.List.of(new WorldMap.Point(720,1168),new WorldMap.Point(1664,1280)))if(new Rectangle(p.x()-140,p.y()-135,280,183).contains(x,y))return true;
        return false;
    }
    static BufferedImage terrain(BufferedImage current)throws Exception{
        current=Phase1WorldPreservationSupport.terrain(true,current);
        var before=fixture("terrain");int changed=0;
        for(int y=0;y<2304;y++)for(int x=0;x<3072;x++)if(before.getRGB(x,y)!=current.getRGB(x,y)){if(!terrainRegion(x,y))throw new AssertionError("Unrelated Forest ground pixel: "+x+","+y);changed++;}
        if(changed==0)throw new AssertionError("Forest presentation did not change");return before;
    }
    static BufferedImage frame(int area,BufferedImage current)throws Exception{
        current=ForestRefinementPreservationSupport.frame(area,current);
        if(area!=1)return current;var before=fixture("frame-1");var camera=new Camera();camera.follow(1312,1157,1270,720,3072,2304);
        if(current.getWidth()!=1270||current.getHeight()!=720)throw new AssertionError("Unexpected historical Forest viewport");
        for(int y=0;y<720;y++)for(int x=0;x<1270;x++)if(before.getRGB(x,y)!=current.getRGB(x,y)&&!frameRegion(x+camera.getX(),y+camera.getY()))throw new AssertionError("Unrelated actual Forest frame pixel: "+x+","+y);
        return before;
    }
}
