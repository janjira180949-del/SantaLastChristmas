import game.*;
import entity.Santa;
import util.ImageAssets;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import javax.imageio.ImageIO;

/** Direct deterministic packed-path pixels and physical-world invariants; no navigation. */
public class PackedSnowPathTest {
    private static int assertions;
    private static void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
    private static byte[] hash(Path path)throws Exception{return MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));}
    private static boolean equal(BufferedImage a,BufferedImage b){return Arrays.equals(a.getRGB(0,0,a.getWidth(),a.getHeight(),null,0,a.getWidth()),b.getRGB(0,0,b.getWidth(),b.getHeight(),null,0,b.getWidth()));}
    private static boolean palette(int rgb){return rgb==0xff81919f||rgb==0xffd0d9d9||rgb==0xfff6efd9||rgb==0xff7e8e9d||rgb==0xffa9b2b8||rgb==0xffeaeee3;}
    private static int distance(int a,int b){return Math.max(Math.abs(((a>>>16)&255)-((b>>>16)&255)),Math.max(Math.abs(((a>>>8)&255)-((b>>>8)&255)),Math.abs((a&255)-(b&255))));}
    private static String geometry(WorldMap m){return m.width()+","+m.height()+new TreeMap<>(m.points())+m.solids()+m.combatRegions().toString();}
    private static BufferedImage render(WorldMap m,int x,int y){var im=new BufferedImage(800,600,2);var g=im.createGraphics();g.translate(-x,-y);m.draw(g,false);g.dispose();return im;}
    private static void save(BufferedImage im,String name)throws Exception{ImageIO.write(im,"png",new File("build/light-snow-road/"+name+".png"));}
    private static BufferedImage gameplay(int width,int height,int x,int y)throws Exception{
        var panel=new GamePanel();panel.setSize(width,height);var f=GamePanel.class.getDeclaredField("areaManager");f.setAccessible(true);var areas=(AreaManager)f.get(panel);
        f=AreaManager.class.getDeclaredField("currentAreaIndex");f.setAccessible(true);f.setInt(areas,1);
        var load=GamePanel.class.getDeclaredMethod("loadCurrentAreaContent");load.setAccessible(true);load.invoke(panel);
        f=GamePanel.class.getDeclaredField("santa");f.setAccessible(true);((Santa)f.get(panel)).setPosition(x,y);
        var im=new BufferedImage(width,height,2);var g=im.createGraphics();panel.paint(g);g.dispose();return im;
    }
    public static void main(String[]args)throws Exception { game.VillageRestorationVisuals.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS);
        Files.createDirectories(Path.of("build/light-snow-road"));
        var protectedFiles=new LinkedHashMap<Path,byte[]>();
        for(String name:new String[]{"src/game/WorldMapData.java","src/game/GamePanel.java","src/game/Camera.java","src/game/AreaManager.java","src/quest/StoryQuestManager.java","src/util/PickupVisual.java","src/game/EndingCutscene.java","src/game/EndingTownVisuals.java","src/entity/Enemy.java","assets/world/map_data.json"}){var p=Path.of(name);protectedFiles.put(p,hash(p));}
        long transparent=0,unchanged=0,packedPixels=0,texturePixels=0;
        for(var type:new Area.Type[]{Area.Type.FOREST,Area.Type.VILLAGE,Area.Type.CHRISTMAS_TOWN}) {
            var map=WorldMap.forArea(type);String name=map.id()+((type==Area.Type.CHRISTMAS_TOWN)?"_corrupted":"")+"_clean.png";
            Path file=Path.of("assets/world",name);
            check(Arrays.equals(hash(file),hash(Path.of("SantaLastChristmas_MapPackage/maps",name))),"source road/art bytes unchanged");
            var source=ImageAssets.load(file.toString());String before=geometry(map);
            var overlay=PackedSnowPath.createOverlay(source);var composite=PackedSnowPath.polish(source);
            check(equal(overlay,PackedSnowPath.createOverlay(source)),"overlay deterministic");
            check(equal(composite,PackedSnowPath.polish(source)),"composite deterministic");
            int snow=type==Area.Type.VILLAGE?0xff9eb8ce:type==Area.Type.FOREST?0xff6e8f9c:0xff7d8ea4;
            int expected=PackedSnowPath.ROAD_FILL;
            boolean zeroOutside=true,unchangedOutside=true,subtle=true,noWhite=true,opaque=true;long count=0,details=0;
            for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++){
                int original=source.getRGB(x,y),pixel=overlay.getRGB(x,y),result=composite.getRGB(x,y);
                if((pixel>>>24)==0){transparent++;if(result!=original)unchangedOutside=false;else unchanged++;}
                else {
                    count++;if(!palette(original))zeroOutside=false;
                    if((pixel>>>24)!=255)opaque=false;
                    if(pixel!=expected)subtle=false;
                    if(pixel==0xffe7f1f4||pixel==0xff81919f)noWhite=false;
                    if(pixel!=expected)details++;
                }
                if(!palette(original)&&pixel!=0)zeroOutside=false;
            }
            check(zeroOutside,"alpha outside authored material mask exactly zero, including props and rectangular buffer bounds");
            check(unchangedOutside,"all exterior/prop pixels outside mask byte-identical");
            check(opaque,"no partial-alpha blur bands");
            check(subtle,"every masked pixel uses only the cold-white road fill");
            check(((expected>>>16)&255)>((snow>>>16)&255)+30 && expected!=0xffffffff,"road is visibly lighter, without pure white");
            var maskDigest=MessageDigest.getInstance("SHA-256");
            for(int y=0;y<overlay.getHeight();y++)for(int x=0;x<overlay.getWidth();x++)maskDigest.update((byte)(overlay.getRGB(x,y)>>>24));
            String maskHash=HexFormat.of().formatHex(maskDigest.digest());
            check(Files.readAllLines(Path.of("build/light-snow-road/prior-mask-sha256.txt")).contains(map.id()+" "+maskHash),"road alpha mask identical to previous accepted geometry");
            check(noWhite,"no old gray outline or white edge teeth");
            check(count>100000&&details==0,"uniform restoration; all procedural texture removed");
            check(equal(render(map,500,300),VillageEnvironmentPolishTest.material(type,ApprovedSnowGround.polish(source,composite,true),overlay).getSubimage(500,300,800,600)),"camera crop and world overlay align exactly");
            check(equal(render(map,500,300),render(map,500,300)),"cached repeated rendering identical");
            check(before.equals(geometry(map)),"bounds collision combat regions exits unchanged");
            var original=new BufferedImage(800,600,2);var g=original.createGraphics();map.drawOriginal(g,false);g.dispose();
            check(equal(original,source.getSubimage(0,0,800,600)),"ending original rendering unchanged");
            packedPixels+=count;texturePixels+=details;
        }
        for(var entry:protectedFiles.entrySet())check(Arrays.equals(entry.getValue(),hash(entry.getKey())),"protected data/code unchanged during rendering");
        check(Area.Type.values().length==4,"runtime metadata retains three adventure maps plus the workshop");
        // High-contrast object shapes remain exactly the original pixels at these review windows.
        var village=WorldMap.forArea(Area.Type.VILLAGE);
        save(render(village,800,200),"01-straight-road");save(render(village,1500,450),"02-curved-road");save(render(village,900,700),"03-intersection");
        save(PackedSnowPrototype.scene(),"04-santa-on-road");save(gameplay(1280,800,1296,1160),"05-wide-gameplay");
        var scene=PackedSnowPrototype.scene();check(equal(scene,PackedSnowPrototype.scene()),"prototype GamePanel repeatable");
        check(PackedSnowPath.ROAD_FILL==0xffdce8ed,"accepted road color remains locked");
        check(!Files.exists(Path.of("src/game/SnowRoadEdges.java")),"obsolete edge renderer removed, not layered");
        System.out.println("PASS: "+assertions+" light-snow road assertions; "+packedPixels+" masked path pixels; "+transparent+" fully transparent exterior pixels; "+unchanged+" exterior pixels unchanged; "+texturePixels+" subtle texture pixels; five review captures");
    }
}



