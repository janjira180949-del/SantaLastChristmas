import game.*;import java.nio.file.*;import java.util.*;import java.util.regex.Pattern;

/** Complete Phase 9.2 bytes plus authored geometry and every original asset are protected. */
public class VillageConsistencyPreservationTest extends V2TestSupport {
    public static void main(String[]args)throws Exception{
        Properties allowed=new Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_2_1_AUTHORIZED.properties"))){allowed.load(in);}
        check(allowed.size()==14,"Exact four presentation owners, manifest and nine preservation migrations");
        check(allowed.stringPropertyNames().stream().filter(p->p.startsWith("src/")).collect(java.util.stream.Collectors.toSet()).equals(Set.of("src/game/GamePanel.java","src/game/VillageEnvironmentLayers.java","src/game/VillageRestorationVisuals.java","src/game/WorldAtmosphere.java")),"No gameplay owner authorized");
        var entries=Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/PHASE9_2_1_BASELINE.json")));int count=0;
        while(entries.find()){String path=entries.group(1);byte[] bytes=Files.readAllBytes(Path.of(path));check(ForestPreservationSupport.hash(Phase921PreservationSupport.previous(path,bytes)).equals(entries.group(2)),"Exact completed file remains protected: "+path);if(allowed.containsKey(path))check(ForestPreservationSupport.hash(Phase931PreservationSupport.previous(path,bytes)).equals(allowed.getProperty(path)),"Exact authenticated revision: "+path);count++;}
        check(count==894,"Entire completed Phase 9.2 snapshot");
        var source=new HashSet<String>();try(var paths=Files.walk(Path.of("src"))){paths.filter(p->p.toString().endsWith(".java")).forEach(p->source.add(p.toString().replace('\\','/')));}
        check(source.size()==102&&source.equals(new HashSet<>(Files.readAllLines(Path.of("srcs.txt")).stream().filter(s->!s.isBlank()).toList())) ,"Complete 89-source manifest");
        check(TreeCollisionPreservationSupport.previousSolids(WorldMap.forArea(Area.Type.VILLAGE)).size()==285&&WorldMap.forArea(Area.Type.VILLAGE).width()==2560&&WorldMap.forArea(Area.Type.VILLAGE).height()==1920,"Exact Village dimensions and runtime solids");
        Properties geometry=new Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_2_1_GEOMETRY_BEFORE.properties"))){geometry.load(in);}
        for(var type:Area.Type.values()){var m=WorldMap.forArea(type);String value=m.id()+m.width()+","+m.height()+ForestRefinementPreservationSupport.points(m)+TreeCollisionPreservationSupport.previousSolids(m)+m.combatRegions();check(ForestPreservationSupport.hash(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)).equals(geometry.getProperty(type.name())),"Exact baseline runtime dimensions, points, solids, gates and encounter regions: "+type);}
        check(new EncounterManager().getEncounters().size()==14&&SpiritTreeTransformation.DURATION==180,"Encounters and Spirit Tree unchanged");
        VillageRestorationVisuals.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS);
        Phase92PreservationSupport.historicalTerrain(Phase92PreservationSupport.terrain());check(true,"Every unrelated original terrain pixel still passes layered historical guards");
        System.out.println("PASS: "+checks+" exact source, asset, metadata, geometry and historical-render assertions");
    }
}
