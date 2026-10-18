import game.*;import java.awt.image.BufferedImage;import java.nio.file.*;import java.security.*;import java.util.*;import javax.imageio.ImageIO;import java.io.File;
public class ExistingGroundRestorationTest {
 static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static byte[] sha(Path p)throws Exception{return MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p));}
 static boolean equal(BufferedImage a,BufferedImage b){return Arrays.equals(a.getRGB(0,0,a.getWidth(),a.getHeight(),null,0,a.getWidth()),b.getRGB(0,0,b.getWidth(),b.getHeight(),null,0,b.getWidth()));}
 static int reflect(int p){int v=Math.floorMod(p,192);return v<96?v:191-v;}
 static String geometry(WorldMap m){return m.width()+","+m.height()+new TreeMap<>(m.points())+m.solids()+m.combatRegions().toString();}
 public static void main(String[]args)throws Exception { game.VillageRestorationVisuals.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS);
  var protectedFiles=new LinkedHashMap<Path,byte[]>();for(String p:new String[]{"assets/world/quest_patches/toby.png","src/game/PackedSnowPath.java","src/game/WorldMapData.java","src/game/GamePanel.java","src/game/Camera.java","src/quest/StoryQuestManager.java","src/quest/StoryQuestTarget.java","src/util/PickupVisual.java","src/game/EndingCutscene.java","src/game/EndingTownVisuals.java"})protectedFiles.put(Path.of(p),sha(Path.of(p)));
  var patch=util.ImageAssets.load("assets/world/quest_patches/toby.png");int cream=0,base=0;boolean palette=true,exact=true,seams=true;
  for(int y=0;y<96;y++)for(int x=0;x<96;x++){int rgb=patch.getRGB(x,y);if(rgb==0xffd0d9d9)base++;else if(rgb==0xfff6efd9)cream++;else palette=false;}
  check(palette&&base==8136&&cream==1080,"exact existing Toby palette and density");
  for(int y=-193;y<385;y++)for(int x=-193;x<385;x++)if(ExistingSnowGround.texturePixel(x,y)!=patch.getRGB(reflect(x),reflect(y)))exact=false;
  check(exact,"all samples are unmodified reference pixels, no generated pattern");
  for(int p=-200;p<600;p++)for(int edge=0;edge<=480;edge+=96)if(ExistingSnowGround.texturePixel(edge-1,p)!=ExistingSnowGround.texturePixel(edge,p)||ExistingSnowGround.texturePixel(p,edge-1)!=ExistingSnowGround.texturePixel(p,edge))seams=false;
  check(seams,"repeat joins have identical adjacent pixels in both axes");
  var flat=new BufferedImage(384,384,2);var g=flat.createGraphics();g.setColor(new java.awt.Color(0xffb2c9da,true));g.fillRect(0,0,384,384);g.dispose();var sample=ExistingSnowGround.polish(flat,flat);int dots=0;
  for(int y=0;y<384;y++)for(int x=0;x<384;x++)if(sample.getRGB(x,y)==0xfff6efd9)dots++;
  check(dots==1080*16,"full repeating material retains reference's exact 11.71875% density");
  long changed=0,roadCount=0,protectedCount=0;
  for(var type:new Area.Type[]{Area.Type.FOREST,Area.Type.VILLAGE,Area.Type.CHRISTMAS_TOWN}){
   var m=WorldMap.forArea(type);String art=m.id()+((type==Area.Type.CHRISTMAS_TOWN)?"_corrupted":"")+"_clean.png";var src=util.ImageAssets.load("assets/world/"+art);var road=PackedSnowPath.polish(src);var overlay=PackedSnowPath.createOverlay(src);var ground=ExistingSnowGround.polish(src,road);String before=geometry(m);
   check(Arrays.equals(sha(Path.of("assets/world",art)),sha(Path.of("SantaLastChristmas_MapPackage/maps",art))),"source map bytes unchanged");check(equal(ground,ExistingSnowGround.polish(src,road)),"world deterministic");
   boolean roadOK=true,propsOK=true,artOK=true;long changes=0;
   int originalBase=0xff9eb8ce;
   for(int y=0;y<src.getHeight();y++)for(int x=0;x<src.getWidth();x++){
    int old=src.getRGB(x,y),r=road.getRGB(x,y),n=ground.getRGB(x,y);
    if((overlay.getRGB(x,y)>>>24)!=0){roadCount++;if(n!=r||n!=0xffdce8ed)roadOK=false;}
    else if(old!=originalBase&&old!=0xffebf4f5){protectedCount++;if(n!=r)propsOK=false;}
    if(n!=r){changes++;if(n!=ExistingSnowGround.texturePixel(x,y))artOK=false;}
   }
   check(roadOK,"road fill and raster mask untouched");check(propsOK,"protected non-ground art unchanged");check(artOK,"all changed pixels taken directly from Toby reference");
   if(type==Area.Type.VILLAGE)check(changes>100000,"reference covers eligible snowy terrain");else check(equal(ground,road),"other terrain climates untouched");
   if(true){var image=new BufferedImage(800,600,2);g=image.createGraphics();g.translate(-200,-300);m.draw(g,false);g.dispose();check(equal(image,VillageEnvironmentPolishTest.material(type,ApprovedSnowGround.polish(src,road,true),overlay).getSubimage(200,300,800,600)),"camera/cache equals world crop");}
   check(before.equals(geometry(m)),"bounds solids and exits unchanged");changed+=changes;
  }
  for(var entry:protectedFiles.entrySet())check(Arrays.equals(entry.getValue(),sha(entry.getKey())),"quest patches and protected code unchanged");
  check(!Files.exists(Path.of("src/game/SnowGroundTexture.java")),"procedural ground renderer removed");
  var scene=ExistingGroundCapture.scene(1100,700);check(equal(scene,ExistingGroundCapture.scene(1100,700)),"actual gameplay deterministic");check(scene.getWidth()==1100,"runtime capture uses requested viewport");
  System.out.println("PASS: "+checks+" existing-ground assertions; "+changed+" pixels reuse Toby art; "+roadCount+" candidate road pixels unchanged; "+protectedCount+" non-ground art pixels unchanged; exact reference palette/density, repeat continuity and world anchoring");
 }
}




