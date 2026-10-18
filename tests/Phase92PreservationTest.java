import game.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

/** Entire pre-edit game, exact authorized owners, original art and full-footprint navigation. */
public class Phase92PreservationTest extends V2TestSupport {
    public static void main(String[] args)throws Exception {
        Properties allowed=new Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_2_AUTHORIZED.properties"))){allowed.load(in);}
        Set<String> source=Set.of("src/entity/VillageResident.java","src/game/FinalChapter.java","src/game/FinalChapterVisuals.java","src/game/GamePanel.java","src/game/QuestGuidance.java","src/game/VillageActivities.java","src/game/VillageActivityVisuals.java","src/game/VillageEnvironmentLayers.java","src/game/WorkshopInteractions.java","src/game/WorldAtmosphere.java","src/game/WorldMap.java","src/game/WorldMapData.java","src/quest/StoryQuest.java","src/quest/StoryQuestManager.java","src/ui/DialogueManager.java");
        check(allowed.stringPropertyNames().stream().filter(s->s.startsWith("src/")).collect(java.util.stream.Collectors.toSet()).equals(source),"Exactly fifteen existing production integration owners");
        var entries=Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/PHASE9_2_BASELINE.json")));int count=0;
        while(entries.find()){
            String path=entries.group(1);var bytes=Files.readAllBytes(Path.of(path));
            check(ForestPreservationSupport.hash(Phase92PreservationSupport.previous(path,bytes)).equals(entries.group(2)),"Every completed source, asset, historical guard and configuration remains protected: "+path);
            if(allowed.containsKey(path))check(ForestPreservationSupport.hash(Phase921PreservationSupport.previous(path,bytes)).equals(allowed.getProperty(path)),"Reviewed exact revised bytes: "+path);
            count++;
        }
        check(count==805,"Complete immutable pre-implementation snapshot");
        Properties assets=new Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_2_ASSETS.properties"))){assets.load(in);}
        check(assets.stringPropertyNames().equals(Phase92PreservationSupport.ASSETS),"Exactly the supplied reference and three generated assets with provenance");
        for(var path:assets.stringPropertyNames())check(ForestPreservationSupport.hash(Files.readAllBytes(Path.of(path))).equals(assets.getProperty(path)),"Exact new asset: "+path);
        var sources=new HashSet<String>();try(var walk=Files.walk(Path.of("src"))){walk.filter(p->p.toString().endsWith(".java")).forEach(p->sources.add(p.toString().replace('\\','/')));}
        check(sources.size()==102&&sources.equals(new HashSet<>(Files.readAllLines(Path.of("srcs.txt")).stream().filter(s->!s.isBlank()).toList())),"All production sources are present in the manifest");
        var metadata=new ProcessBuilder("python","tools/verify_phase92_metadata.py").redirectErrorStream(true).start();String report=new String(metadata.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);check(metadata.waitFor()==0,"Only the authoritative tree landmark changes metadata: "+report);
        check(new EncounterManager().getEncounters().size()==14&&SpiritTreeTransformation.DURATION==180,"Encounter population and restoration duration remain exact");
        Phase92PreservationSupport.historicalTerrain(Phase92PreservationSupport.terrain());check(true,"Prepared terrain differs only in exact house, market and original lamp footprints");
        var map=WorldMap.forArea(Area.Type.VILLAGE);int cols=160,rows=120;boolean[] free=new boolean[cols*rows],seen=new boolean[free.length];
        for(int y=0;y<rows;y++)for(int x=0;x<cols;x++)free[y*cols+x]=!util.Collision.intersectsAny(new java.awt.geom.Rectangle2D.Double(x*16,y*16-8,32,50),map.solids());
        int start=44*cols+36;check(free[start],"Original arrival is safe");var queue=new ArrayDeque<Integer>();queue.add(start);seen[start]=true;
        while(!queue.isEmpty()){int cell=queue.remove(),x=cell%cols,y=cell/cols;for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){int nx=x+d[0],ny=y+d[1];if(nx<0||nx>=cols||ny<0||ny>=rows)continue;int n=ny*cols+nx;if(free[n]&&!seen[n]){seen[n]=true;queue.add(n);}}}
        for(var site:VillageActivities.Site.values()){
            boolean reachable=false;var santa=new entity.Santa(0,0);
            for(int y=0;y<rows&&!reachable;y++)for(int x=0;x<cols&&!reachable;x++)if(seen[y*cols+x]){santa.setPosition(x*16,y*16);reachable=VillageActivities.inReach(santa,site);}
            check(reachable,"Every unchanged resident and restoration, including relocated tree, is reachable: "+site);
        }
        var scratch=Files.createTempDirectory(Path.of("build/phase9-2"),"missing-art-").toAbsolutePath();String cp=Arrays.stream(System.getProperty("java.class.path").split(java.io.File.pathSeparator)).map(s->Path.of(s).toAbsolutePath().toString()).collect(java.util.stream.Collectors.joining(java.io.File.pathSeparator));
        var probe=new ProcessBuilder("java","-Djava.awt.headless=true","-cp",cp,"Phase92FallbackProbe").directory(scratch.toFile()).redirectErrorStream(true).start();String output=new String(probe.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);check(probe.waitFor()==0&&output.contains("PASS"),"Actual missing-file worker and native safety landmarks: "+output);
        System.out.println("PASS: "+checks+" exact preservation, geometry, navigation and fallback assertions");
    }
}
