import game.*;
import game.VillageActivities.Site;
import java.awt.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/** Completed content remains byte-protected; only the finite additive Village layer changes pixels. */
public class VillageActivitiesPreservationTest extends V2TestSupport {
    public static void main(String[]args)throws Exception{
        Set<String> permitted=Set.of("src/game/GamePanel.java","src/ui/Portrait.java","srcs.txt",
            "tests/SantaBaseStoryPreservationSupport.java","tests/ForestPreservationTest.java",
            "tests/GameplayOverhaulPreservationTest.java","tests/SantaBaseStoryPreservationTest.java","tests/SantaBasePreservationTest.java");
        Properties allowed=new Properties();try(var in=Files.newInputStream(Path.of("docs/VILLAGE_ACTIVITIES_AUTHORIZED.properties"))){allowed.load(in);}
        check(allowed.stringPropertyNames().equals(permitted),"Exact eight integration/test owners; no broad exemptions");
        var matches=Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/VILLAGE_ACTIVITIES_BASELINE.json")));int count=0;
        while(matches.find()){
            byte[] current=VillageActivitiesPreservationSupport.previous(matches.group(1),FinalChapterPreservationSupport.readCurrent(matches.group(1)));
            check(ForestPreservationSupport.hash(current).equals(matches.group(2)),"Existing source/art/audio/reference/configuration protected: "+matches.group(1));count++;
        }
        Properties hashes=new Properties();try(var in=Files.newInputStream(Path.of("docs/VILLAGE_ACTIVITIES_RENDER_BASELINE.properties"))){hashes.load(in);}
        for(Area.Type area:Area.Type.values()){
            WorldMap map=WorldMap.forArea(area);String value=VillageIntegrationPreservationSupport.geometryBefore(map);
            check(ForestPreservationSupport.hash(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)).equals(hashes.getProperty(area.name())),"Actual area geometry and main gates remain exact: "+area);
        }
        VillageRestorationVisuals.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS);WorldMap village=WorldMap.forArea(Area.Type.VILLAGE);var terrain=new java.awt.image.BufferedImage(village.width(),village.height(),2);var g=terrain.createGraphics();village.draw(g,false);g.dispose();
        check(ForestPreservationTest.pixelHash(Phase92PreservationSupport.historicalTerrain(terrain)).equals(hashes.getProperty("village-terrain")),"Original actual cached Village terrain is pixel-identical");
        var before=ImageIO.read(Path.of("tests/fixtures/village-activities-before.png").toFile());
        check(ForestPreservationTest.pixelHash(before).equals(hashes.getProperty("village-before")),"Authenticated pre-change rendered image");
        var after=FinalEnvironmentReview.scene(0,1296,1160,1270,720);
        check(ForestPreservationTest.pixelHash(after).equals(VillageActivitiesPreservationSupport.villageHash()),"New authorized actual renderer hash is deterministic");
        Camera camera=new Camera();camera.follow(1312,1181,1270,720,2560,1920);
        var masks=new ArrayList<Rectangle>();masks.add(VillageActivityVisuals.trackerBounds(1270,720));
        for(Site site:Site.values()){
            Rectangle box=site.resident()?entity.VillageResident.bounds(site):site==Site.CAMPFIRE?new Rectangle(site.x-55,site.y-70,110,100):new Rectangle(site.x-72,site.y-176,144,253);
            box.translate(-camera.getX(),-camera.getY());masks.add(box);
        }
        var entrance=new WorldMap.Point(400,720); // Historical removed arch footprint, not a live gate.masks.add(new Rectangle(entrance.x()-88-camera.getX(),entrance.y()-111-camera.getY(),176,156));
        int changed=0,outside=0;
        for(int y=0;y<720;y++)for(int x=0;x<1270;x++)if(before.getRGB(x,y)!=after.getRGB(x,y)){
            changed++;boolean inside=false;for(Rectangle box:masks)if(box.contains(x,y)){inside=true;break;}if(!inside)outside++;
        }
        check(changed>100&&outside==0,"All new pixels confined to finite NPC/prop/tracker rectangles; no terrain or old HUD redraw (outside="+outside+")");
        check(new EncounterManager().getEncounters().size()==14&&SpiritTreeTransformation.DURATION==180,"No enemy population or restoration-timing change");
        Set<String> actual=new HashSet<>();try(var stream=Files.walk(Path.of("src"))){stream.filter(p->p.toString().endsWith(".java")).forEach(p->actual.add(p.toString().replace('\\','/')));}
        Set<String> manifest=new HashSet<>();for(String line:Files.readAllLines(Path.of("srcs.txt")))if(!line.isBlank())check(manifest.add(line.trim()),"Unique manifest entry");
        check(actual.equals(manifest)&&actual.size()==102,"IDE source list includes all five new owners");
        // Four-area full-footprint flood fill proves every side site is reachable without metadata edits.
        routes(village);
        System.out.println("PASS: "+checks+" preservation/navigation assertions across "+count+" pre-activity files; "+changed+" localized additive pixels");
    }
    private static void routes(WorldMap map){
        int cols=160,rows=120;boolean[] free=new boolean[cols*rows],seen=new boolean[cols*rows];var solids=map.solids();
        for(int y=0;y<rows;y++)for(int x=0;x<cols;x++)free[y*cols+x]=!util.Collision.intersectsAny(new java.awt.geom.Rectangle2D.Double(x*16,y*16-8,32,50),solids);
        int start=44*cols+36;check(free[start],"Original Village arrival flood-fill seed remains valid");var queue=new ArrayDeque<Integer>();queue.add(start);seen[start]=true;
        while(!queue.isEmpty()){
            int cell=queue.remove(),x=cell%cols,y=cell/cols;
            for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){int nx=x+d[0],ny=y+d[1];if(nx<0||nx>=cols||ny<0||ny>=rows)continue;int next=ny*cols+nx;
                if(free[next]&&!seen[next]){seen[next]=true;queue.add(next);}
            }
        }
        for(Site site:Site.values()){
            int px=site.x-16,py=site.y+(site==Site.IRIS?25:site==Site.TREE?16:site==Site.HOUSE?0:40)-21;
            boolean reached=false;
            // The authored interaction point can fall between grid cells; verify a reachable
            // collision-safe cell within one grid step of the actual safe approach.
            for(int gy=py/16;gy<=(py+15)/16;gy++)for(int gx=px/16;gx<=(px+15)/16;gx++)
                if(seen[gy*cols+gx])reached=true;
            check(reached,"Full actor reaches side activity approach from the original arrival: "+site);
        }
    }
}
