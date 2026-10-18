import game.*;import util.ImageAssets;import java.awt.*;import java.awt.image.*;import java.nio.file.*;import java.io.*;import java.util.*;import java.security.*;import javax.imageio.ImageIO;
public class SnowGroundIntegrationTest {
 static int checks;static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static Path dir=Path.of("build/snow-ground-integration");
 static boolean equal(BufferedImage a,BufferedImage b){return Arrays.equals(a.getRGB(0,0,a.getWidth(),a.getHeight(),null,0,a.getWidth()),b.getRGB(0,0,b.getWidth(),b.getHeight(),null,0,b.getWidth()));}
 static void save(BufferedImage im,String name)throws Exception{ImageIO.write(im,"png",dir.resolve(name+".png").toFile());}
 static BufferedImage scene(BufferedImage cache,int w,int h)throws Exception{var f=WorldMap.class.getDeclaredField("polished");f.setAccessible(true);f.set(WorldMap.forArea(Area.Type.VILLAGE),VillageRoadSurface.apply(cache,PackedSnowPath.createOverlay(ImageAssets.load("assets/world/village_clean.png"))));return ExistingGroundCapture.scene(w,h);}
 public static void main(String[]args)throws Exception {
  VillageRestorationVisuals.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS);
  Files.createDirectories(dir);
  var tile=ImageAssets.load("assets/world/ground/snow_base.png");boolean seams=true;for(int x=0;x<320;x++)seams&=tile.getRGB(x,0)==tile.getRGB(x,319);for(int y=0;y<320;y++)seams&=tile.getRGB(0,y)==tile.getRGB(319,y);check(seams,"tile opposite edges exact");
  long roadPixels=0,objects=0;int placements=0;
  for(var type:new Area.Type[]{Area.Type.FOREST,Area.Type.VILLAGE,Area.Type.CHRISTMAS_TOWN}){
   var map=WorldMap.forArea(type);String name=map.id()+((type==Area.Type.CHRISTMAS_TOWN)?"_corrupted":"")+"_clean.png";var src=ImageAssets.load("assets/world/"+name);var road=PackedSnowPath.polish(src);var mask=PackedSnowPath.createOverlay(src);var groundMask=ExistingSnowGround.groundMask(src,road);
   var old=RoadSnowTexture.apply(ExistingSnowGround.polish(src,road),mask);var next=RoadSnowTexture.apply(ApprovedSnowGround.polish(src,road,true),mask);var plain=ApprovedSnowGround.polish(src,road,false);
   check(equal(next,RoadSnowTexture.apply(ApprovedSnowGround.polish(src,road,true),mask)),"deterministic composite");check(ApprovedSnowGround.placements(src.getWidth(),src.getHeight(),groundMask).equals(ApprovedSnowGround.placements(src.getWidth(),src.getHeight(),groundMask)),"deterministic placements");
   boolean roadOK=true,objectOK=true,baseOK=true;for(int y=0;y<src.getHeight();y++)for(int x=0;x<src.getWidth();x++){int i=y*src.getWidth()+x;
    if((mask.getRGB(x,y)>>>24)!=0){roadPixels++;if(next.getRGB(x,y)!=old.getRGB(x,y))roadOK=false;}
    if(!groundMask[i]){objects++;if(next.getRGB(x,y)!=old.getRGB(x,y))objectOK=false;}else if(plain.getRGB(x,y)!=tile.getRGB(x%320,y%320))baseOK=false;
   }check(roadOK,"all road pixels identical");check(objectOK,"all pixels outside ground mask identical");check(baseOK,"base anchored exact 320 tile no scaling");placements+=ApprovedSnowGround.placements(src.getWidth(),src.getHeight(),groundMask).size();
   if(type==Area.Type.VILLAGE){var before=scene(old,1100,700);save(before,"01-before");save(scene(RoadSnowTexture.apply(plain,mask),1100,700),"02-base-snow-only");save(scene(next,1100,700),"03-base-plus-patches");save(scene(next,1100,700),"04-final-prototype");var comp=new BufferedImage(2200,700,2);var g=comp.createGraphics();g.drawImage(before,0,0,null);g.drawImage(scene(next,1100,700),1100,0,null);g.dispose();save(comp,"05-before-after");if(args.length>0&&args[0].equals("prototype"))break;
    save(scene(next,1280,800),"10-wide-gameplay");int[][]windows={{400,250},{600,850},{800,850},{600,500}};String[]titles={"06-near-house","07-near-fence","08-near-sign","09-road-and-ground"};for(int n=0;n<4;n++)save(next.getSubimage(Math.min(windows[n][0],next.getWidth()-650),Math.min(windows[n][1],next.getHeight()-450),650,450),titles[n]);
   }
   if(type==Area.Type.VILLAGE){var cache=WorldMap.class.getDeclaredField("polished");cache.setAccessible(true);cache.set(map,null);}
   if(args.length==0){var im=new BufferedImage(800,600,2);var g=im.createGraphics();g.translate(-200,-300);map.draw(g,false);g.dispose();check(equal(im,VillageEnvironmentPolishTest.material(type,ApprovedSnowGround.polish(src,road,true),mask).getSubimage(200,300,800,600)),"runtime camera world crop");}
  }
  System.out.println("PASS: "+checks+" snow integration assertions; "+roadPixels+" road pixels unchanged; "+objects+" outside-ground pixels unchanged; "+placements+" sparse placements");
 }
}


