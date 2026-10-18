import boss.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Real decoded frames, row-major sheets, anchors, source hashes and asynchronous cache. */
public class Phase10AssetTest extends V2TestSupport {
 static int different(java.awt.image.BufferedImage a,java.awt.image.BufferedImage b,int left,int top,int width,int height){int count=0;for(int y=top;y<top+height;y++)for(int x=left;x<left+width;x++)if(a.getRGB(x,y)!=b.getRGB(x,y))count++;return count;}
 public static void main(String[] args)throws Exception{
  var baseline=Files.readString(Path.of("docs/PHASE10_BASELINE.json"));
  for(String source:List.of("heavy_walk_key_01.png","heavy_walk_key_02.png","beast_charge_key_01.png","beast_charge_key_02.png","beast_charge_key_03.png")){
   String path="assets/dark_titan/"+source;String hash=ForestPreservationSupport.hash(Files.readAllBytes(Path.of(path)));
   check(baseline.contains("\""+path+"\": \""+hash+"\""),"Approved source exact bytes "+source);
  }
  for(String name:List.of("heavy_walk","beast_charge")){
   var sheet=ImageIO.read(Path.of("assets/dark_titan/generated/"+name+"_sheet.png").toFile());
   check(sheet.getWidth()==2048&&sheet.getHeight()==2048&&sheet.getColorModel().hasAlpha(),"4x4 RGBA sheet "+name);
   var unique=new HashSet<String>();
   for(int i=0;i<16;i++){
    var path=Path.of(String.format("assets/dark_titan/generated/%s/frame_%02d.png",name,i+1));var im=ImageIO.read(path.toFile());unique.add(ForestPreservationSupport.hash(Files.readAllBytes(path)));
    check(im.getWidth()==512&&im.getHeight()==512&&im.getColorModel().hasAlpha(),"Actual RGBA512 frame "+name+" "+i);
    check((im.getRGB(0,0)>>>24)==0&&(im.getRGB(511,511)>>>24)==0,"Transparent margins "+i);
    int painted=0;boolean exact=true;for(int y=0;y<512;y++)for(int x=0;x<512;x++){if((im.getRGB(x,y)>>>24)>24)painted++;if(im.getRGB(x,y)!=sheet.getRGB(i%4*512+x,i/4*512+y))exact=false;}
    check(painted>18000&&exact,"Meaningful silhouette and exact row-major cell "+name+" "+i);
   }
   check(unique.size()==16,"No duplicated frame files "+name);
   var first=ImageIO.read(Path.of("assets/dark_titan/generated/"+name+"/frame_01.png").toFile());
   var arms=ImageIO.read(Path.of("assets/dark_titan/generated/"+name+(name.equals("heavy_walk")?"/frame_05.png":"/frame_09.png")).toFile());
   var legs=ImageIO.read(Path.of("assets/dark_titan/generated/"+name+"/frame_09.png").toFile());
   check(different(first,arms,0,185,188,300)>500,"Left arm articulation changes "+name);
   check(different(first,arms,332,180,180,290)>500,"Right arm articulation changes "+name);
   check(different(first,legs,180,350,215,160)>500,"Leg articulation changes "+name);
   try(var in=ImageIO.createImageInputStream(Path.of("assets/dark_titan/generated/"+name+"_preview.gif").toFile())){var reader=ImageIO.getImageReaders(in).next();reader.setInput(in);check(reader.getNumImages(true)==16,"16-frame preview GIF "+name);reader.dispose();}
  }
  var manifest=Files.readString(Path.of("assets/dark_titan/generated/manifest.json"));check(manifest.contains("\"foot_contact_frames\":[1,9]")&&manifest.contains("\"launch_frame\":10")&&manifest.contains("\"impact_frame\":13")&&manifest.contains("\"recovery_frames\":[15,16]"),"Explicit update-synchronised event metadata");
  BossArtwork.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS);
  for(String name:List.of("idle","walk","run","claw_swipe","ice_blast","frost_wave","frost_dash","hurt","entrance","defeat"))for(int f=0;f<4;f++){
   check(BossArtwork.frame("fang",name,f)!=null,"All Fang animation frames prepared "+name);
   var im=ImageIO.read(Path.of(String.format("assets/bosses/frost_fang/%s/frame_%02d.png",name,f+1)).toFile());check(im.getWidth()==256&&im.getHeight()==256&&im.getColorModel().hasAlpha()&&(im.getRGB(0,0)>>>24)==0,"Fang source dimensions and transparent margins "+name);
  }
  for(String name:List.of("heavy_walk","beast_charge"))for(int f=0;f<16;f++)check(BossArtwork.frame("titan",name,f)!=null,"All Titan animation frames prepared "+name);
  for(String name:List.of("claw","blast","wave","dash","hurt","defeat","slam","spikes","beam","storm","footstep","warning")){
   var sheet=ImageIO.read(Path.of("assets/bosses/effects/"+name+"_sheet.png").toFile());check(sheet.getWidth()==1024&&sheet.getHeight()==768&&sheet.getColorModel().hasAlpha(),"12-frame cached pixel effect "+name);
  }
  for(String asset:Phase10PreservationSupport.assets())Phase10PreservationSupport.authenticateAsset(asset);
  System.out.println("PASS: "+checks+" actual frame/alpha/sheet/cache/source-preservation assertions");
 }
}
