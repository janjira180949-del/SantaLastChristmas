import game.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/** Exact prior content protection, full-footprint navigation and real missing-art process. */
public class FinalChapterPreservationTest extends FinalChapterTest {
    public static void main(String[]args)throws Exception{
        Properties allowed=new Properties();try(var in=Files.newInputStream(Path.of("docs/FINAL_CHAPTER_AUTHORIZED.properties"))){allowed.load(in);}
        check(allowed.size()==33,"Exactly thirty-three documented owners; no blanket integration exemption");
        check("DELETED".equals(allowed.getProperty(FinalChapterPreservationSupport.REMOVED))&&!Files.exists(Path.of(FinalChapterPreservationSupport.REMOVED)),"Obsolete minigame implementation really removed");
        var matches=Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/FINAL_CHAPTER_BASELINE.json")));int protectedFiles=0;
        while(matches.find()){
            String path=matches.group(1);byte[] current=JourneyHomePreservationSupport.previous(path,FinalChapterPreservationSupport.readCurrent(path));
            if(allowed.containsKey(path)&&!path.equals(FinalChapterPreservationSupport.REMOVED))check(ForestPreservationSupport.hash(current).equals(allowed.getProperty(path)),"Reviewed exact current owner: "+path);
            byte[] before=FinalChapterPreservationSupport.previous(path,current);
            check(ForestPreservationSupport.hash(before).equals(matches.group(2)),"Prior artwork, audio, geometry, boss, controls, configuration and all historical guards protected: "+path);protectedFiles++;
        }
        check(protectedFiles==723,"Every completed baseline file is checked");
        Properties assets=new Properties();try(var in=Files.newInputStream(Path.of("docs/FINAL_CHAPTER_ASSETS.properties"))){assets.load(in);}
        check(assets.stringPropertyNames().equals(FinalChapterPreservationSupport.ASSETS),"Only the two generated chapter images and provenance are added");
        for(String path:assets.stringPropertyNames())check(ForestPreservationSupport.hash(Files.readAllBytes(Path.of(path))).equals(assets.getProperty(path)),"Inspected asset hash: "+path);
        var materials=ImageIO.read(Path.of("assets/world/final_chapter/materials_atlas.png").toFile());
        check(materials.getWidth()==2172&&materials.getHeight()==724&&materials.getColorModel().hasAlpha(),"Three transparent runtime material columns");
        var house=ImageIO.read(Path.of("assets/world/final_chapter/repaired_house.png").toFile());check(house.getWidth()==1254&&house.getHeight()==1254&&house.getColorModel().hasAlpha(),"Transparent repaired version of the actual supplied house");
        Set<String> sources=new HashSet<>();try(var paths=Files.walk(Path.of("src"))){paths.filter(p->p.toString().endsWith(".java")).forEach(p->sources.add(p.toString().replace('\\','/')));}
        Set<String> manifest=new HashSet<>();for(String line:Files.readAllLines(Path.of("srcs.txt")))if(!line.isBlank())check(manifest.add(line.trim()),"Unique source manifest owner");
        check(sources.equals(manifest)&&sources.size()==102,"Complete production manifest adds two chapter owners and removes one puzzle owner");
        check(Arrays.stream(ui.VillageActivityUI.Mode.values()).noneMatch(m->Set.of("MECHANISM","SEQUENCE","HEATER").contains(m.name())),"No puzzle modal can be reopened");
        check(Arrays.stream(VillageActivities.class.getDeclaredFields()).noneMatch(f->Set.of("mechanism","sequence","sealHits","heater","pipes").contains(f.getName())),"No obsolete puzzle or attack-completion state remains");
        check(new EncounterManager().getEncounters().size()==14&&SpiritTreeTransformation.DURATION==180,"Original encounters and exact restoration clock preserved");
        for(Area.Type area:Area.Type.values())routes(WorldMap.forArea(area));
        // Compare actual pre-story panels to the authenticated pre-chapter runtime, not concept art.
        var before=ImageIO.read(Path.of("tests/fixtures/final-chapter-village-before.png").toFile());
        Properties renderBefore=new Properties();try(var in=Files.newInputStream(Path.of("docs/FINAL_CHAPTER_RENDER_BEFORE.properties"))){renderBefore.load(in);}
        check(ForestPreservationTest.pixelHash(before).equals(renderBefore.getProperty("village-before")),"Archived baseline actual renderer fixture authenticated");
        var after=FinalEnvironmentReview.scene(0,1296,1160,1270,720);
        check(ForestPreservationTest.pixelHash(after).equals(FinalChapterPreservationSupport.renderHash("village-before-chapter")),"Current actual renderer is deterministic");
        Camera camera=new Camera();camera.follow(1312,1181,1270,720,2560,1920);
        Rectangle tracker=VillageActivityVisuals.trackerBounds(1270,720),seal=new Rectangle(1224-camera.getX(),1376-camera.getY(),144,253);
        int changed=0,outside=0;
        for(int y=0;y<720;y++)for(int x=0;x<1270;x++)if(before.getRGB(x,y)!=after.getRGB(x,y)){changed++;if(!tracker.contains(x,y)&&!seal.contains(x,y))outside++;}
        check(changed>100&&outside==0,"Pre-chapter pixels change only in superseded tracker and removed seal effect; outside="+outside);
        Path empty=Path.of("build/final-chapter/missing-art").toAbsolutePath();Files.createDirectories(empty);
        String classpath=Arrays.stream(System.getProperty("java.class.path").split(java.io.File.pathSeparator)).map(s->Path.of(s).toAbsolutePath().toString()).collect(java.util.stream.Collectors.joining(java.io.File.pathSeparator));
        var process=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java.exe").toString(),"-Djava.awt.headless=true","-cp",classpath,"FinalChapterFallbackProbe").directory(empty.toFile()).redirectErrorStream(true).start();
        check(process.waitFor(30,java.util.concurrent.TimeUnit.SECONDS),"Actual missing-asset child finishes without blocking");String output=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        check(process.exitValue()==0&&output.contains("PASS:"),"Unavailable chapter assets leave playable presentation: "+output);
        System.out.println("PASS: "+checks+" exact preservation, full actor routes and missing-art assertions across "+protectedFiles+" completed files");
    }
    private static void routes(WorldMap map){
        int cols=map.width()/16,rows=map.height()/16;boolean[] free=new boolean[cols*rows],seen=new boolean[cols*rows];var solids=map.solids();
        for(int y=0;y<rows;y++)for(int x=0;x<cols;x++)free[y*cols+x]=!util.Collision.intersectsAny(new Rectangle2D.Double(x*16,y*16-8,32,50),solids);
        var spawn=map.points().get("spawn");if(spawn==null)spawn=map.points().get("entry/from_village");
        if(spawn==null)spawn=map.points().get("entry/from_forest");
        check(spawn!=null,"Actual arrival flood-fill seed: "+map.id());
        int start=-1;for(int y=0;y<rows;y++)for(int x=0;x<cols;x++)if(free[y*cols+x]&&Math.hypot(x*16+16-spawn.x(),y*16+13-spawn.y())<24)start=y*cols+x;
        check(start>=0,"Collision-safe arrival cell: "+map.id());var queue=new ArrayDeque<Integer>();queue.add(start);seen[start]=true;
        while(!queue.isEmpty()){
            int cell=queue.remove(),x=cell%cols,y=cell/cols;
            for(int d:new int[]{-1,1,-cols,cols}){int next=cell+d;if(next<0||next>=seen.length||d==-1&&x==0||d==1&&x==cols-1||d==-cols&&y==0||d==cols&&y==rows-1)continue;
                if(free[next]&&!seen[next]){seen[next]=true;queue.add(next);}}
        }
        var targets=new ArrayList<WorldMap.Point>();
        if(map.id().equals("forest"))for(var pickup:FinalChapter.PICKUPS)targets.add(pickup.point());
        if(map.id().equals("village"))for(var objective:FinalChapter.Restoration.values())targets.add(objective.approach);
        for(String key:new String[]{"exit/to_village","exit/to_forest","exit/to_town","exit/to_santa_base","entry/from_village","entry/from_forest","entry/from_town"}){var point=map.points().get(key);if(point!=null)targets.add(point);}
        for(var point:targets){boolean reached=false;for(int y=0;y<rows;y++)for(int x=0;x<cols;x++)if(seen[y*cols+x]&&Math.hypot(x*16+16-point.x(),y*16+13-point.y())<70)reached=true;
            check(reached,"Full actor reaches authored pickup/restoration/gate: "+map.id()+" "+point);}
    }
}
