import game.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/** Authenticated raw preservation, localized terrain differences and actual unchanged area frames. */
public class ForestLandmarkPreservationTest extends V2TestSupport {
    static final Set<String> OWNERS=Set.of("src/game/ForestEnvironmentLayers.java","src/game/FrozenForestVisuals.java","src/game/BlackFrostCrystal.java","src/game/HealingPoint.java","src/game/GamePanel.java","srcs.txt","tests/AssetAuditTest.java","tests/Phase921PreservationSupport.java","tests/ForestPreservationTest.java","tests/GameplayOverhaulPreservationTest.java","tests/FinalChapterPreservationTest.java","tests/JourneyHomePreservationTest.java","tests/Phase92PreservationTest.java","tests/QuestFlowPreservationTest.java","tests/SantaBaseStoryPreservationTest.java","tests/VillageActivitiesPreservationTest.java","tests/VillageConsistencyPreservationTest.java");
    static BufferedImage raw(int area)throws Exception {
        var p=playing();p.setSize(1270,720);var manager=areas(p);var f=AreaManager.class.getDeclaredField("currentAreaIndex");f.setAccessible(true);f.setInt(manager,area);call(p,"loadCurrentAreaContent");
        var a=(VillageActivities)field(p,"villageActivities");f=VillageActivities.class.getDeclaredField("presentsCollected");f.setAccessible(true);f.setBoolean(a,false);
        f=FinalChapter.class.getDeclaredField("unlocked");f.setAccessible(true);f.setBoolean(a.chapter,false);f=GamePanel.class.getDeclaredField("areaMessage");f.setAccessible(true);f.set(p,null);
        santa(p).setPosition(1296,area==0?1160:1136);var image=render(p);p.stopGame();return image;
    }
    static String prior(String path)throws Exception{return new String(JourneyHomePreservationSupport.archived(path,"PHASE9_3_1"),StandardCharsets.UTF_8);}
    public static void main(String[] args)throws Exception {
        var allowed=new Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_3_1_AUTHORIZED.properties"))){allowed.load(in);}
        check(allowed.stringPropertyNames().equals(OWNERS),"Exact seventeen-owner integration/migration scope");
        var matcher=Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/PHASE9_3_1_BASELINE.json")));
        Set<String> actualChanges=new HashSet<>();int total=0;
        while(matcher.find()){
            String path=matcher.group(1),hash=matcher.group(2);byte[] bytes=ForestRefinementPreservationSupport.previous(path,Files.readAllBytes(Path.of(path)));
            if(!ForestPreservationSupport.hash(bytes).equals(hash))actualChanges.add(path);
            check(ForestPreservationSupport.hash(Phase931PreservationSupport.previous(path,bytes)).equals(hash),"Authenticated prior bytes "+path);total++;
        }
        check(total==958&&actualChanges.equals(OWNERS),"All baseline files retained; only exact authorized owners differ");
        var assets=new Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_3_1_ASSETS.properties"))){assets.load(in);}
        check(assets.stringPropertyNames().equals(Phase931PreservationSupport.ASSETS),"Exact new art/reference/provenance set");
        for(String path:assets.stringPropertyNames())check(ForestPreservationSupport.hash(Files.readAllBytes(Path.of(path))).equals(assets.getProperty(path)),"Approved asset hash "+path);
        for(String name:java.util.List.of("terrain","frame-0","frame-1","frame-2","frame-3","town-healing"))check(Phase931PreservationSupport.fixture(name)!=null,"Original renderer fixture authenticated "+name);
        for(String path:java.util.List.of("src/game/BlackFrostCrystal.java","src/game/HealingPoint.java")){
            String before=prior(path),after=new String(ForestGameplayPreservationSupport.previous(path,Files.readAllBytes(Path.of(path))),StandardCharsets.UTF_8);String marker=path.contains("Crystal")?"    public void draw(Graphics2D original)":"    public void draw(Graphics2D graphics)";
            String oldPrefix=before.substring(0,before.indexOf(marker));
            check(after.startsWith(oldPrefix),"All original model fields, bounds, update and interaction mechanics retained "+path);
        }
        String oldHeal=prior("src/game/HealingPoint.java"),newHeal=new String(ForestGameplayPreservationSupport.previous("src/game/HealingPoint.java",Files.readAllBytes(Path.of("src/game/HealingPoint.java"))),StandardCharsets.UTF_8);
        String townMarker="        Graphics2D g=(Graphics2D)graphics.create();\n        try {\n            int x=position.x(),y=position.y();";
        // Normalize line endings for the retained Town body, then require the complete original suffix.
        oldHeal=oldHeal.replace("\r\n","\n");newHeal=newHeal.replace("\r\n","\n");
        check(newHeal.endsWith(oldHeal.substring(oldHeal.indexOf(townMarker))),"Complete Town healing drawing body preserved");
        String beforePanel=prior("src/game/GamePanel.java"),afterPanel=new String(ForestGameplayPreservationSupport.previous("src/game/GamePanel.java",Files.readAllBytes(Path.of("src/game/GamePanel.java"))),StandardCharsets.UTF_8);
        int beforePaint=beforePanel.indexOf("protected synchronized void paintComponent"),afterPaint=afterPanel.indexOf("protected synchronized void paintComponent");
        check(beforePanel.substring(0,beforePaint).equals(afterPanel.substring(0,afterPaint)),"All GamePanel session, combat, quest, input and travel owners unchanged");
        var geometry=new Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_3_1_GEOMETRY_BEFORE.properties"))){geometry.load(in);}
        for(var type:Area.Type.values()){
            var map=WorldMap.forArea(type);String data=map.id()+map.width()+","+map.height()+ForestRefinementPreservationSupport.points(map)+TreeCollisionPreservationSupport.previousSolids(map)+map.combatRegions();
            check(ForestPreservationSupport.hash(data.getBytes(StandardCharsets.UTF_8)).equals(geometry.getProperty(type.name())),"Exact all-map dimensions/markers/solids/combat regions "+type);
        }
        var forest=WorldMap.forArea(Area.Type.FOREST);check(forest.width()==3072&&forest.height()==2304&&forest.solids().size()==496,"Original Forest geometry");
        for(var p:FinalChapter.PICKUPS)check(!util.Collision.intersectsAny(new java.awt.Rectangle(p.point().x()-16,p.point().y()-21,32,50),forest.solids()),"Material actor approach preserved "+p.id());
        var terrain=FrozenForestVisuals.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS);
        Phase931PreservationSupport.terrain(terrain);check(true,"Every changed prepared-ground pixel lies inside exact landmark regions");
        var oldTerrain=Phase931PreservationSupport.fixture("terrain");
        for(int[] p:new int[][]{{1285,795},{1450,812},{1510,941}}){int changed=0;for(int y=p[1];y<p[1]+33;y++)for(int x=p[0];x<p[0]+39;x++)if(oldTerrain.getRGB(x,y)!=terrain.getRGB(x,y))changed++;check(changed>500,"Old bench-pattern square removed at its source "+Arrays.toString(p));}
        String camp=Files.readString(Path.of("src/game/ForestEnvironmentLayers.java"));
        check(!camp.contains("getSubimage(40,40,40,18)")&&!camp.contains("{{1285,795},{1450,812},{1510,941}}"),"All three inappropriate bench-textured faces removed, not covered");
        for(int area=0;area<4;area++){
            var image=raw(area);if(area==1){Phase931PreservationSupport.frame(area,image);check(true,"Actual Forest differences confined to landmark regions");}
            else check(ForestPreservationTest.pixelHash(ForestRefinementPreservationSupport.frame(area,image)).equals(ForestPreservationTest.pixelHash(Phase931PreservationSupport.fixture("frame-"+area))),"Actual other-area presentation pixel-identical "+area);
        }
        var manifest=new HashSet<>(Files.readAllLines(Path.of("srcs.txt")));manifest.removeIf(String::isBlank);Set<String> sources=new HashSet<>();try(var stream=Files.walk(Path.of("src"))){stream.filter(p->p.toString().endsWith(".java")).forEach(p->sources.add(p.toString().replace('\\','/')));}
        check(manifest.equals(sources)&&sources.size()==102,"Only one prepared-art production owner added");
        check(SpiritTreeTransformation.DURATION==180,"Exact original Spirit Tree timeline");
        System.out.println("PASS: "+checks+" raw preservation, localized rendering, geometry and source-mechanics assertions");
    }
}
