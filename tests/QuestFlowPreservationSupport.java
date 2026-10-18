import java.nio.file.*;
import java.util.*;

/** Authentic Phase 9.1 changes only; historical archives and expected hashes remain immutable. */
final class QuestFlowPreservationSupport {
    private QuestFlowPreservationSupport(){ }
    /** Historical full-frame hashes still execute. First prove that actual pixels outside
     * the explicitly replaced quest panels and direction markers match Phase 8 exactly.
     * This projection is test-only and is never used by the production renderer. */
    static java.awt.image.BufferedImage historicalFrame(int area,java.awt.image.BufferedImage current)throws Exception{
        current=Phase92PreservationSupport.historicalFrame(area,current);
        Properties hashes=new Properties();try(var in=Files.newInputStream(Path.of("docs/QUEST_FLOW_RENDER_BEFORE.properties"))){hashes.load(in);}
        Path path=Path.of("tests/fixtures/quest-flow-"+area+"-before.png");
        if(!ForestPreservationSupport.hash(Files.readAllBytes(path)).equals(hashes.getProperty(area+".file")))throw new AssertionError("Unauthenticated Phase 8 renderer fixture");
        var before=javax.imageio.ImageIO.read(path.toFile());
        if(!ForestPreservationTest.pixelHash(before).equals(hashes.getProperty(area+".pixels")))throw new AssertionError("Unauthenticated Phase 8 pixels");
        int w=current.getWidth(),h=current.getHeight();
        if(w!=before.getWidth()||h!=before.getHeight())throw new AssertionError("Historical viewport mismatch");
        var masks=new ArrayList<java.awt.Rectangle>();masks.add(new java.awt.Rectangle(14,h-194,280,180));
        if(area==0)masks.add(game.VillageActivityVisuals.trackerBounds(w,h));
        if(area<3){
            var camera=new game.Camera();camera.follow(1312,area==0?1181:1157,w,h,area==1?3072:2560,area==1?2304:1920);
            game.WorldMap.Point old=area==0?new game.WorldMap.Point(528,720):area==1?new game.WorldMap.Point(944,784):new game.WorldMap.Point(1376,419);
            game.WorldMap.Point next=area==0?new game.WorldMap.Point(528,784):area==1?new game.WorldMap.Point(16,1168):new game.WorldMap.Point(1296,16);
            masks.add(marker(old,area==0?"Toby":area==1?"Lost gift":"Elias",camera,w,h));
            masks.add(marker(next,area==0?"Toby":"Toby",camera,w,h));
        }
        var projected=new java.awt.image.BufferedImage(w,h,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            boolean allowed=false;for(var mask:masks)if(mask.contains(x,y)){allowed=true;break;}
            if(!allowed&&before.getRGB(x,y)!=current.getRGB(x,y))throw new AssertionError("Unrelated rendered pixel changed: area="+area+" at "+x+","+y);
            projected.setRGB(x,y,allowed?before.getRGB(x,y):current.getRGB(x,y));
        }
        return projected;
    }
    static java.awt.Rectangle marker(game.WorldMap.Point target,String label,game.Camera camera,int w,int h){
        int tx=target.x()-camera.getX(),ty=target.y()-camera.getY();
        if(tx>=35&&tx<w-35&&ty>=140&&ty<h-90)return new java.awt.Rectangle();
        int x=Math.max(55,Math.min(w-55,tx)),y=Math.max(150,Math.min(h-90,ty));
        if(x<310&&y>h-230)y=h-230;
        var im=new java.awt.image.BufferedImage(1,1,2);var g=im.createGraphics();g.setFont(new java.awt.Font("Dialog",1,11));
        int width=g.getFontMetrics().stringWidth(label);g.dispose();
        int textX=Math.max(6,Math.min(w-width-6,x-width/2));
        return new java.awt.Rectangle(Math.min(x-14,textX-4),y-14,Math.max(x+14,textX+width+4)-Math.min(x-14,textX-4)+1,47);
    }
    static byte[] previous(String path,byte[] current){
        current=Phase92PreservationSupport.previous(path,current);
        try{
            Properties allowed=new Properties();try(var in=Files.newInputStream(Path.of("docs/QUEST_FLOW_AUTHORIZED.properties"))){allowed.load(in);}
            String expected=allowed.getProperty(path);if(expected==null)return current;
            String actual=ForestPreservationSupport.hash(current);
            for(String prefix:List.of("QUEST_FLOW","JOURNEY_HOME","FINAL_CHAPTER","VILLAGE_INTEGRATION","VILLAGE_ACTIVITIES","SANTA_BASE_STORY","SANTA_BASE","FOREST_OVERHAUL","GAMEPLAY_OVERHAUL")){
                byte[] older=JourneyHomePreservationSupport.archived(path,prefix);
                if(older!=null&&actual.equals(ForestPreservationSupport.hash(older)))return current;
            }
            if(!actual.equals(expected))throw new AssertionError("Unreviewed Quest Flow owner: "+path);
            return JourneyHomePreservationSupport.archived(path,"QUEST_FLOW");
        }catch(Exception failure){throw new AssertionError(failure);}
    }
}
