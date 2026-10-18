import game.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

/** Exact Phase 8 bytes, single metadata change, finite unchanged encounters and reachable NPCs. */
public class QuestFlowPreservationTest extends V2TestSupport {
    public static void main(String[]args)throws Exception{
        Properties allowed=new Properties();try(var in=Files.newInputStream(Path.of("docs/QUEST_FLOW_AUTHORIZED.properties"))){allowed.load(in);}
        Set<String> production=Set.of("src/entity/Child.java","src/game/FinalChapter.java","src/game/GamePanel.java","src/game/VillageActivities.java",
            "src/game/VillageActivityVisuals.java","src/game/VillageEnvironment.java","src/game/WorldMapData.java","src/quest/StoryQuest.java",
            "src/quest/StoryQuestManager.java","src/ui/DialogueManager.java","src/ui/HUD.java","src/ui/VillageActivityUI.java");
        check(allowed.stringPropertyNames().stream().filter(p->p.startsWith("src/")).collect(java.util.stream.Collectors.toSet()).equals(production),"Exactly twelve minimal existing production integration owners");
        var entries=Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/QUEST_FLOW_BASELINE.json")));int count=0;
        while(entries.find()){
            String path=entries.group(1);byte[] current=Phase92PreservationSupport.previous(path,Files.readAllBytes(Path.of(path)));
            if(allowed.containsKey(path))check(ForestPreservationSupport.hash(current).equals(allowed.getProperty(path)),"Exact reviewed revision: "+path);
            check(ForestPreservationSupport.hash(QuestFlowPreservationSupport.previous(path,current)).equals(entries.group(2)),"Completed game, original snapshots and all assets protected: "+path);count++;
        }
        check(count==757,"Every completed baseline file remains guarded");
        var before=JourneyHomePreservationSupport.archived("assets/world/map_data.json","QUEST_FLOW");
        String prior=new String(before,java.nio.charset.StandardCharsets.UTF_8);
        String current=new String(Phase92PreservationSupport.previous("assets/world/map_data.json",Files.readAllBytes(Path.of("assets/world/map_data.json"))),java.nio.charset.StandardCharsets.UTF_8).replace("\r\n","\n");
        check(current.replace("\"Toby\": [\n          16,\n          24","\"Toby\": [\n          16,\n          22").equals(prior.replace("\r\n","\n")),"Entire authored metadata changes only Toby's tile anchor");
        check(new EncounterManager().getEncounters().size()==14&&SpiritTreeTransformation.DURATION==180,"All enemies, boss and original restoration clock retained");
        var files=new HashSet<String>();try(var walk=Files.walk(Path.of("src"))){walk.filter(p->p.toString().endsWith(".java")).forEach(p->files.add(p.toString().replace('\\','/')));}
        check(files.size()==102&&files.equals(new HashSet<>(Files.readAllLines(Path.of("srcs.txt")).stream().filter(s->!s.isBlank()).toList())),"Exact complete 89-source Java manifest");
        WorldMap map=WorldMap.forArea(Area.Type.VILLAGE);int cols=160,rows=120;boolean[] free=new boolean[cols*rows],seen=new boolean[free.length];var solids=map.solids();
        for(int y=0;y<rows;y++)for(int x=0;x<cols;x++)free[y*cols+x]=!util.Collision.intersectsAny(new java.awt.geom.Rectangle2D.Double(x*16,y*16-8,32,50),solids);
        int start=44*cols+36;check(free[start],"Original Village arrival is safe");var queue=new ArrayDeque<Integer>();queue.add(start);seen[start]=true;
        while(!queue.isEmpty()){
            int cell=queue.remove(),x=cell%cols,y=cell/cols;for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){int nx=x+d[0],ny=y+d[1];if(nx<0||nx>=cols||ny<0||ny>=rows)continue;int n=ny*cols+nx;if(free[n]&&!seen[n]){seen[n]=true;queue.add(n);}}
        }
        for(var site:VillageActivities.Site.values()){
            boolean reachable=false;var santa=new entity.Santa(0,0);
            for(int y=0;y<rows&&!reachable;y++)for(int x=0;x<cols&&!reachable;x++)if(seen[y*cols+x]){santa.setPosition(x*16,y*16);reachable=VillageActivities.inReach(santa,site);}
            check(reachable,"Full-footprint route and unobstructed interaction: "+site);
        }
        var panel=playing();var toby=panel.getChildren().get(0);boolean reached=false;
        for(int y=0;y<rows&&!reached;y++)for(int x=0;x<cols&&!reached;x++)if(seen[y*cols+x]){santa(panel).setPosition(x*16,y*16);reached=toby.isWithinInteractionRange(santa(panel));}
        check(reached,"New Toby anchor reachable with collision-safe interaction");
        System.out.println("PASS: "+checks+" exact preservation, manifest and navigation assertions");
    }
}
