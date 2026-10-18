import java.nio.file.*;
import java.util.*;

/** Exact reviewed Phase 9.2 projection, ahead of the unchanged historical guard chain. */
final class Phase92PreservationSupport {
    static final Set<String> ASSETS=Set.of("assets/references/phase9_2_village_reference.png","assets/world/phase9_2/damaged_house.png","assets/world/phase9_2/tree_states.png","assets/world/phase9_2/market_states.png","assets/world/phase9_2/README.md");
    static byte[] previous(String path,byte[] current){
        current=Phase921PreservationSupport.previous(path,current);
        try{
            Properties allowed=new Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_2_AUTHORIZED.properties"))){allowed.load(in);}
            String expected=allowed.getProperty(path);if(expected==null)return current;
            String actual=ForestPreservationSupport.hash(current);
            for(String prefix:List.of("PHASE9_2","QUEST_FLOW","JOURNEY_HOME","FINAL_CHAPTER","VILLAGE_INTEGRATION","VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")){
                byte[] older=JourneyHomePreservationSupport.archived(path,prefix);
                if(older!=null&&actual.equals(ForestPreservationSupport.hash(older)))return current;
            }
            if(!actual.equals(expected))throw new AssertionError("Unreviewed Phase 9.2 owner: "+path);
            return JourneyHomePreservationSupport.archived(path,"PHASE9_2");
        }catch(Exception failure){throw new AssertionError(failure);}
    }
    static java.awt.image.BufferedImage terrain() throws Exception {
        Object art=game.VillageRestorationVisuals.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS);
        var accessor=art.getClass().getDeclaredMethod("terrain");accessor.setAccessible(true);return (java.awt.image.BufferedImage)accessor.invoke(art);
    }
    static java.awt.image.BufferedImage historicalTerrain(java.awt.image.BufferedImage current)throws Exception{
        current=Phase921PreservationSupport.terrain(current);
        Properties hashes=new Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_2_RENDER_BEFORE.properties"))){hashes.load(in);}
        var path=Path.of("tests/fixtures/phase9-2-terrain-before.png");
        if(!ForestPreservationSupport.hash(Files.readAllBytes(path)).equals(hashes.getProperty("terrain.file")))throw new AssertionError("Unauthenticated pre-revision terrain");
        var before=javax.imageio.ImageIO.read(path.toFile());
        var house=game.VillageRestorationVisuals.HOUSE_BOUNDS;var market=new java.awt.Rectangle(988,650,192,200);
        for(int y=0;y<1920;y++)for(int x=0;x<2560;x++)if(!house.contains(x,y)&&!market.contains(x,y)&&!game.VillageRestorationVisuals.replacedLamp(x,y)&&current.getRGB(x,y)!=before.getRGB(x,y))throw new AssertionError("Unrelated terrain pixel changed at "+x+","+y);
        return before;
    }
    static java.awt.image.BufferedImage historicalFrame(int area,java.awt.image.BufferedImage current)throws Exception{
        current=Phase921PreservationSupport.frame(area,current);
        Properties hashes=new Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_2_RENDER_BEFORE.properties"))){hashes.load(in);}
        boolean overview=area==3&&current.getWidth()==1600;
        Path path=Path.of(overview?"tests/fixtures/phase9-2-base-overview-before.png":"tests/fixtures/phase9-2-"+area+"-before.png");
        if(!ForestPreservationSupport.hash(Files.readAllBytes(path)).equals(hashes.getProperty(overview?"base-overview.file":area+".file")))throw new AssertionError("Unauthenticated Phase 9.1 capture");
        var before=javax.imageio.ImageIO.read(path.toFile());int w=current.getWidth(),h=current.getHeight();
        var masks=new ArrayList<java.awt.Rectangle>();masks.add(new java.awt.Rectangle(14,h-214,280,200));
        masks.add(new java.awt.Rectangle(0,130,165,70));
        var edgeCamera=new game.Camera();edgeCamera.follow(1312,area==0?1181:1157,w,h,area==1?3072:2560,area==1?2304:1920);
        if(area<3){var point=area==0?new game.WorldMap.Point(1104,608):game.WorldMap.forArea(area==1?game.Area.Type.FOREST:game.Area.Type.CHRISTMAS_TOWN).point("exit/"+(area==1?"to_village":"to_forest"));masks.add(QuestFlowPreservationSupport.marker(point,"Clara",edgeCamera,w,h));} // Old Toby/new remaining recipient marker at the viewport edge.
        if(area==0){
            var camera=new game.Camera();camera.follow(1312,1181,w,h,2560,1920);
            var tree=new java.awt.Rectangle(game.VillageRestorationVisuals.TREE_BOUNDS);tree.translate(-camera.getX(),-camera.getY());masks.add(tree);
            masks.add(new java.awt.Rectangle(988-camera.getX(),650-camera.getY(),192,200));
            for(var site:List.of(game.VillageActivities.Site.STREETLIGHTS,game.VillageActivities.Site.LANTERN_EAST,game.VillageActivities.Site.LANTERN_SOUTH))masks.add(new java.awt.Rectangle(site.x-39-camera.getX(),site.y-130-camera.getY(),78,139));
        }
        if(w!=before.getWidth()||h!=before.getHeight())throw new AssertionError("Historical viewport mismatch");
        var result=new java.awt.image.BufferedImage(w,h,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            boolean allowed=false;for(var mask:masks)if(mask.contains(x,y)){allowed=true;break;}
            if(!allowed&&before.getRGB(x,y)!=current.getRGB(x,y))throw new AssertionError("Unrelated Phase 9.1 pixel changed: area="+area+" at "+x+","+y);
            result.setRGB(x,y,allowed?before.getRGB(x,y):current.getRGB(x,y));
        }
        return result;
    }
}
