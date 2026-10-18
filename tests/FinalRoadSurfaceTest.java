import game.*;import util.ImageAssets;import java.awt.*;import java.awt.image.*;import java.nio.file.*;import java.io.*;import java.util.*;import java.security.*;import javax.imageio.ImageIO;
public class FinalRoadSurfaceTest {
 static int checks;static Path dir=Path.of("build/final-road-surface");
 static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static boolean equal(BufferedImage a,BufferedImage b){return Arrays.equals(a.getRGB(0,0,a.getWidth(),a.getHeight(),null,0,a.getWidth()),b.getRGB(0,0,b.getWidth(),b.getHeight(),null,0,b.getWidth()));}
 static void save(BufferedImage im,String n)throws Exception{ImageIO.write(im,"png",dir.resolve(n+".png").toFile());}
 static BufferedImage scene(BufferedImage cache,int w,int h)throws Exception{var f=WorldMap.class.getDeclaredField("polished");f.setAccessible(true);f.set(WorldMap.forArea(Area.Type.VILLAGE),VillageRoadSurface.apply(cache,PackedSnowPath.createOverlay(ImageAssets.load("assets/world/village_clean.png"))));return ExistingGroundCapture.scene(w,h);}
 static BufferedImage previous(BufferedImage im,BufferedImage mask){int w=im.getWidth(),h=im.getHeight();var out=new BufferedImage(w,h,2);var g=out.createGraphics();g.drawImage(im,0,0,null);g.dispose();for(int y=0;y<h;y++)for(int x=0;x<w;x++)if((mask.getRGB(x,y)>>>24)!=0){int a=x/12,b=y/12;int z=a*0x1f123bb5^b*0x5f356495^0x63d92b71;z^=z>>>16;z*=0x7feb352d;z^=z>>>15;z*=0x846ca68b;z^=z>>>16;out.setRGB(x,y,ExistingSnowGround.texturePixel(x,y)==0xfff6efd9&&Integer.remainderUnsigned(z,100)<34?0xfff1f2e7:0xffdce8ed);}return out;}
 public static void main(String[]args)throws Exception{
  
  long outside=0,road=0,quiet=0;
  for(var type:new Area.Type[]{Area.Type.FOREST,Area.Type.VILLAGE,Area.Type.CHRISTMAS_TOWN}){
   var map=WorldMap.forArea(type);String n=map.id()+((type==Area.Type.CHRISTMAS_TOWN)?"_corrupted":"")+"_clean.png";var src=ImageAssets.load("assets/world/"+n);var mask=PackedSnowPath.createOverlay(src);var ground=ApprovedSnowGround.polish(src,PackedSnowPath.polish(src),true);var a=RoadSnowTexture.apply(ground,mask,35);var b=RoadSnowTexture.apply(ground,mask,50);
   check(equal(a,RoadSnowTexture.apply(ground,mask,35)),"35 deterministic");check(equal(b,RoadSnowTexture.apply(ground,mask,50)),"50 deterministic");boolean untouched=true,palette=true;long localRoad=0,localQuiet=0;var md=MessageDigest.getInstance("SHA-256");
   for(int y=0;y<src.getHeight();y++)for(int x=0;x<src.getWidth();x++){int alpha=mask.getRGB(x,y)>>>24;md.update((byte)alpha);if(alpha==0){outside++;if(a.getRGB(x,y)!=ground.getRGB(x,y)||b.getRGB(x,y)!=ground.getRGB(x,y))untouched=false;}else{road++;localRoad++;if(a.getRGB(x,y)==0xffdce8ed){quiet++;localQuiet++;}if((a.getRGB(x,y)>>>24)!=255)palette=false;}}
   check(untouched,"all nonroad pixels identical at both detail levels");check(palette,"opaque road material");check(localQuiet>localRoad*.5,"dominant unchanged DCE8ED base");
   if(true)check(Files.readAllLines(Path.of("build/light-snow-road/prior-mask-sha256.txt")).contains(map.id()+" "+HexFormat.of().formatHex(md.digest())),"mask unchanged");
   if(type==Area.Type.VILLAGE){save(scene(previous(ground,mask),1100,700),"01-current-road");var ia=scene(a,1100,700);save(ia,"02-road-texture-35");var ib=scene(b,1100,700);save(ib,"03-road-texture-50");var old=ImageIO.read(dir.resolve("01-current-road.png").toFile());var comp=new BufferedImage(3300,730,2);var g=comp.createGraphics();g.setColor(new Color(20,30,40));g.fillRect(0,0,3300,730);g.setColor(Color.WHITE);g.drawString("CURRENT",20,20);g.drawString("35%",1120,20);g.drawString("50%",2220,20);g.drawImage(old,0,30,null);g.drawImage(ia,1100,30,null);g.drawImage(ib,2200,30,null);g.dispose();save(comp,"04-comparison");
    if(args.length>0)break;var finalIm=RoadSnowTexture.apply(ground,mask);save(scene(finalIm,1100,700),"09-santa-on-road");save(scene(finalIm,1280,800),"10-wide-gameplay");save(finalIm.getSubimage(600,500,650,450),"08-road-near-lamp");
   }
   if(type==Area.Type.VILLAGE){var cache=WorldMap.class.getDeclaredField("polished");cache.setAccessible(true);cache.set(map,null);}
   if(args.length==0&&true){var im=new BufferedImage(800,600,2);var g=im.createGraphics();g.translate(-200,-300);map.draw(g,false);g.dispose();check(equal(im,VillageEnvironmentPolishTest.material(type,ground,mask).getSubimage(200,300,800,600)),"camera/cache world anchored");}
   if(args.length==0&&type==Area.Type.VILLAGE){var im=RoadSnowTexture.apply(ground,mask);save(im.getSubimage(800,200,800,600),"05-straight-road");save(im.getSubimage(1500,450,800,600),"06-curve");save(im.getSubimage(900,700,800,600),"07-intersection");}
  }
  System.out.println("PASS: "+checks+" final-road checks; "+outside+" outside-road pixels identical; "+quiet+" / "+road+" road pixels retain exact DCE8ED");
 }
}




