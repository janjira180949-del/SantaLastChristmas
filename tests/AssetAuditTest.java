import java.nio.file.*;
import java.util.zip.ZipFile;
import java.util.Arrays;
import java.util.Set;
import java.util.HexFormat;
import java.security.MessageDigest;
import javax.imageio.ImageIO;

/** Compare retained raster assets to the pre-migration baseline, including dynamic frame sets. */
public class AssetAuditTest extends V2TestSupport {
    public static void main(String[] args)throws Exception {
        int count=0,added=0,menuCount=0,treeCount=0,townCount=0;
        Set<String> elias=Set.of("assets/characters/elias/idle/elias_idle_down_01.png",
            "assets/characters/elias/idle/elias_idle_up_01.png",
            "assets/characters/elias/idle/elias_idle_left_01.png",
            "assets/characters/elias/idle/elias_idle_right_01.png",
            "assets/characters/elias/portraits/elias_portrait_neutral.png");
        String manifest=Files.readString(Path.of("assets/characters/elias/asset_manifest.json"));
        String treeManifest=Files.readString(Path.of("docs/TOWN_TREE_ASSET_MANIFEST.json"));
        String townManifest=Files.readString(Path.of("docs/TOWN_FINAL_ASSET_MANIFEST.json"));
        String menuManifest=Files.readString(Path.of("docs/MAIN_MENU_ASSET_MANIFEST.json"));
        try(ZipFile baseline=new ZipFile("build/v2-baseline/before-rework.zip");var files=Files.walk(Path.of("assets"),FileVisitOption.FOLLOW_LINKS)) {
            for(Path p:files.filter(Files::isRegularFile).filter(p->p.toString().endsWith(".png")).toList()) {
                String path=p.toString().replace('\\','/');
                var entry=baseline.getEntry(path);
                if(Phase3PreservationSupport.assets().contains(path)){
                    check(ImageIO.read(p.toFile()).getColorModel().hasAlpha(),"Exact new Phase3 VFX PNG alpha");
                } else if(Phase2UIPreservationSupport.assets().contains(path)){
                    check(ImageIO.read(p.toFile()).getColorModel().hasAlpha(),"Illustrated Phase 2 PNG has alpha");
                } else if(Phase105PreservationSupport.assets().containsKey(path)){
                    check(ForestPreservationSupport.hash(Files.readAllBytes(p)).equals(Phase105PreservationSupport.assets().getProperty(path)),"Exact source-derived Titan cleanup export");
                } else if(Phase10PreservationSupport.assets().contains(path)){
                    Phase10PreservationSupport.authenticateAsset(path);check(true,"Exact approved two-boss artwork");
                } else if(Phase931PreservationSupport.ASSETS.contains(path)){
                    var approved=new java.util.Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_3_1_ASSETS.properties"))){approved.load(in);}
                    check(ForestPreservationSupport.hash(Files.readAllBytes(p)).equals(approved.getProperty(path)),"Exact authorized Forest artwork/reference");
                } else if(Phase92PreservationSupport.ASSETS.contains(path)){
                    var approved=new java.util.Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_2_ASSETS.properties"))){approved.load(in);}
                    check(ForestPreservationSupport.hash(Files.readAllBytes(p)).equals(approved.getProperty(path)),"Exact authorized Phase 9.2 artwork/reference");
                } else if(FinalChapterPreservationSupport.ASSETS.contains(path)) {
                    var approved=new java.util.Properties();try(var in=Files.newInputStream(Path.of("docs/FINAL_CHAPTER_ASSETS.properties"))){approved.load(in);}
                    check(ForestPreservationSupport.hash(Files.readAllBytes(p)).equals(approved.getProperty(path)),"Exact approved Final Chapter raster addition");
                } else if(VillageIntegrationPreservationSupport.ASSETS.contains(path)) {
                    var approved=new java.util.Properties();try(var in=Files.newInputStream(Path.of("docs/VILLAGE_INTEGRATION_ASSETS.properties"))){approved.load(in);}
                    check(ForestPreservationSupport.hash(Files.readAllBytes(p)).equals(approved.getProperty(path)),"Exact approved Village integration raster addition");
                } else if(JourneyHomePreservationSupport.ASSETS.contains(path)) {
                    var approved=new java.util.Properties();try(var in=Files.newInputStream(Path.of("docs/JOURNEY_HOME_ASSETS.properties"))){approved.load(in);}
                    check(ForestPreservationSupport.hash(Files.readAllBytes(p)).equals(approved.getProperty(path)),"Exact approved Journey Home raster addition");
                } else if(SantaBasePreservationSupport.newAssets().contains(path)) {
                    var approved=new java.util.Properties();try(var in=Files.newInputStream(Path.of("docs/SANTA_BASE_NEW_ASSETS.properties"))){approved.load(in);}
                    check(ForestPreservationSupport.hash(Files.readAllBytes(p)).equals(approved.getProperty(path)),"Exact approved Base raster addition");
                } else if(Set.of("assets/world/town_environment/spirit_plaza.png","assets/world/town_environment/festival_banner.png").contains(path)) {
                    String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p)));
                    check(townManifest.contains("\""+path+"\": \""+hash+"\""),"Approved transparent Town environment asset hash");townCount++;
                } else if(path.equals("assets/world/spirit_tree/restored_spirit_tree.png")) {
                    String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p)));
                    check(treeManifest.contains("\""+path+"\": \""+hash+"\""),"Approved distinct restored tree hash");treeCount++;
                } else if(path.equals("assets/menu/main_menu_reference.png")) {
                    String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p)));
                    check(menuManifest.contains("\""+path+"\": \""+hash+"\""),"Supplied menu reference retained exactly");menuCount++;
                } else if(path.equals("assets/references/santa_skills_reference.png")) {
                    var approved=new java.util.Properties();try(var in=Files.newInputStream(Path.of("docs/PHASE9_4_ASSETS.properties"))){approved.load(in);}
                    check(ForestPreservationSupport.hash(Files.readAllBytes(p)).equals(approved.getProperty(path)),"Exact user-approved skill reference, not runtime art");
                } else if(elias.contains(path)) {
                    String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p)));
                    check(manifest.contains("\""+path+"\": \""+hash+"\""),"Approved Elias export hash: "+p);added++;
                } else {
                    check(entry!=null,"Asset reused from baseline: "+p);
                    check(Arrays.equals(Files.readAllBytes(p),baseline.getInputStream(entry).readAllBytes()),"Artwork unchanged: "+p);count++;
                }
                check(ImageIO.read(p.toFile())!=null,"Resource decodes: "+p);
            }
        }
        for(String retired:new String[]{"screamer","giftthief"}) {
            try(var files=Files.walk(Path.of("assets/characters/enemies/"+retired))){check(files.noneMatch(Files::isRegularFile),"Retired dynamic prefix cleaned: "+retired);}
        }
        check(count==104&&added==5&&menuCount==1&&treeCount==1&&townCount==2,"All original assets and exact approved Elias/menu/tree/Town additions retained");
        System.out.println("PASS: "+count+" original PNGs unchanged; "+added+" approved Elias exports; one unchanged menu reference decodable");
    }
}
