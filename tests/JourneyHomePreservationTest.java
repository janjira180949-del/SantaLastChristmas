import game.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.awt.geom.Rectangle2D;

/** Enumerated exact revisions, complete collision equality and reachable new approaches. */
public class JourneyHomePreservationTest extends JourneyHomeTest {
    public static void main(String[]args)throws Exception{
        Properties allowed=new Properties();try(var in=Files.newInputStream(Path.of("docs/JOURNEY_HOME_AUTHORIZED.properties"))){allowed.load(in);}
        var baseline=Files.readString(Path.of("docs/JOURNEY_HOME_BASELINE.json"));var entries=Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(baseline);int count=0;
        while(entries.find()){
            String path=entries.group(1);byte[] bytes=QuestFlowPreservationSupport.previous(path,Files.readAllBytes(Path.of(path)));
            if(allowed.containsKey(path))check(ForestPreservationSupport.hash(bytes).equals(allowed.getProperty(path)),"Exact reviewed Phase 8 owner: "+path);
            check(ForestPreservationSupport.hash(JourneyHomePreservationSupport.previous(path,bytes)).equals(entries.group(2)),"Protected baseline file: "+path);count++;
        }
        check(count==741,"All completed files, including historical guards and archives, remain protected");
        var metadata=new ProcessBuilder("python","tools/verify_journey_home_metadata.py").redirectErrorStream(true).start();
        check(metadata.waitFor(15,java.util.concurrent.TimeUnit.SECONDS),"Full metadata comparison finishes within watchdog");
        String metadataResult=new String(metadata.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        check(metadata.exitValue()==0&&metadataResult.contains("PASS:"),"Complete structured JSON equality outside the one authorized exit: "+metadataResult);
        Properties assets=new Properties();try(var in=Files.newInputStream(Path.of("docs/JOURNEY_HOME_ASSETS.properties"))){assets.load(in);}
        check(assets.stringPropertyNames().equals(JourneyHomePreservationSupport.ASSETS),"Only the two finished homecoming images and their provenance are added");
        for(String path:assets.stringPropertyNames())check(ForestPreservationSupport.hash(Files.readAllBytes(Path.of(path))).equals(assets.getProperty(path)),"Reviewed generated asset: "+path);
        // Exact previous live geometry includes solids expanded from border and authored rectangles.
        Properties geometry=new Properties();try(var in=Files.newInputStream(Path.of("docs/VILLAGE_ACTIVITIES_RENDER_BASELINE.properties"))){geometry.load(in);}
        for(Area.Type area:Area.Type.values())check(ForestPreservationSupport.hash(VillageIntegrationPreservationSupport.geometryBefore(WorldMap.forArea(area)).getBytes(java.nio.charset.StandardCharsets.UTF_8)).equals(geometry.getProperty(area.name())),"Every map dimension, solid and original marker preserved: "+area);
        check(!WorldMap.forArea(Area.Type.VILLAGE).points().containsKey("exit/to_santa_base"),"Only obsolete Village home exit removed");
        check(WorldMap.forArea(Area.Type.SANTA_BASE).point("exit/to_village").equals(new WorldMap.Point(816,1136)),"Original Base departure retained");
        var town=WorldMap.forArea(Area.Type.CHRISTMAS_TOWN);check(!util.Collision.intersectsAny(new Rectangle2D.Double(1536,1259,32,50),town.solids()),"Actual sleigh approach has full-footprint clearance");
        reachable(town,JourneyHomeVisuals.SLEIGH_APPROACH);
        reachable(WorldMap.forArea(Area.Type.SANTA_BASE),WorkshopInteractions.approach(WorkshopInteractions.Site.LETTER_DESK));
        check(new EncounterManager().getEncounters().size()==14&&SpiritTreeTransformation.DURATION==180,"Original population and restoration duration preserved");
        var files=new HashSet<String>();try(var stream=Files.walk(Path.of("src"))){stream.filter(p->p.toString().endsWith(".java")).forEach(p->files.add(p.toString().replace('\\','/')));}
        check(files.equals(Files.readAllLines(Path.of("srcs.txt")).stream().map(String::trim).filter(line->!line.isEmpty()).collect(java.util.stream.Collectors.toSet()))&&files.size()==102,"Exact 89-source production manifest");
        Path empty=Path.of("build/journey-home/missing-art").toAbsolutePath();Files.createDirectories(empty);
        String cp=Arrays.stream(System.getProperty("java.class.path").split(java.io.File.pathSeparator)).map(s->Path.of(s).toAbsolutePath().toString()).collect(java.util.stream.Collectors.joining(java.io.File.pathSeparator));
        var process=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java.exe").toString(),"-Djava.awt.headless=true","-cp",cp,"JourneyHomeFallbackProbe").directory(empty.toFile()).redirectErrorStream(true).start();
        check(process.waitFor(30,java.util.concurrent.TimeUnit.SECONDS),"Actual missing-art child finishes within watchdog");String result=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);check(process.exitValue()==0&&result.contains("PASS:"),"Actual missing-file preparation and safe render: "+result);
        System.out.println("PASS: "+checks+" Phase 8 preservation, navigation, manifest and missing-art assertions");
    }
    private static void reachable(WorldMap map,WorldMap.Point target){
        int cols=map.width()/16,rows=map.height()/16;var solids=map.solids();boolean[] free=new boolean[cols*rows],seen=new boolean[free.length];
        for(int y=0;y<rows;y++)for(int x=0;x<cols;x++)free[y*cols+x]=!util.Collision.intersectsAny(new Rectangle2D.Double(x*16,y*16-8,32,50),solids);
        var seed=map.points().get(map.id().equals("santa_base")?"spawn":"entry/from_forest");int start=-1;
        for(int y=0;y<rows;y++)for(int x=0;x<cols;x++)if(free[y*cols+x]&&Math.hypot(x*16+16-seed.x(),y*16+13-seed.y())<24)start=y*cols+x;
        check(start>=0,"Safe existing flood-fill seed");var queue=new ArrayDeque<Integer>();queue.add(start);seen[start]=true;
        while(!queue.isEmpty()){int cell=queue.remove(),x=cell%cols,y=cell/cols;for(int d:new int[]{-1,1,-cols,cols}){int next=cell+d;if(next<0||next>=seen.length||d==-1&&x==0||d==1&&x==cols-1||d==-cols&&y==0||d==cols&&y==rows-1)continue;if(free[next]&&!seen[next]){seen[next]=true;queue.add(next);}}}
        boolean reached=false;for(int y=0;y<rows;y++)for(int x=0;x<cols;x++)if(seen[y*cols+x]&&Math.hypot(x*16+16-target.x(),y*16+13-target.y())<48)reached=true;
        check(reached,"Existing full-actor route reaches new story approach: "+map.id()+" "+target);
    }
}
