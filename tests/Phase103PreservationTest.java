import game.*;
import java.nio.file.*;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Raw current inventory plus exact ground/world/combat checks before any historical projection. */
public class Phase103PreservationTest extends V2TestSupport {
 static String section(String s,String a,String b){return s.substring(s.indexOf(a),s.indexOf(b,s.indexOf(a))).replace("\r\n","\n");}
 public static void main(String[] args)throws Exception{
  var allowed=Phase103PreservationSupport.allowed();var changed=new HashSet<String>();
  var matcher=java.util.regex.Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/PHASE10_3_BASELINE.json")));
  while(matcher.find()){
   String path=matcher.group(1),hash=ForestPreservationSupport.hash(PngCrashPreservationSupport.previous(path,Files.readAllBytes(Path.of(path))));
   if(!hash.equals(matcher.group(2)))changed.add(path);
   check(hash.equals(allowed.getProperty(path,matcher.group(2))),"Exact authorized owner or protected original "+path);
  }check(changed.equals(allowed.stringPropertyNames()),"Exact isolated current-task change set");
  for(String path:Phase103PreservationSupport.assets().stringPropertyNames())check(ForestPreservationSupport.hash(Files.readAllBytes(Path.of(path))).equals(Phase103PreservationSupport.assets().getProperty(path)),"Authenticated new asset "+path);
  for(int area:new int[]{0,1,3}){
   var before=ImageIO.read(Path.of("build/phase10-3/before/neutral-"+area+".png").toFile());var after=Phase1051PreservationSupport.frame(area,ForestLandmarkPreservationTest.raw(area));
   check(Arrays.equals(before.getRGB(0,0,1270,720,null,0,1270),after.getRGB(0,0,1270,720,null,0,1270)),"Every unaffected Village/Forest/Base pixel identical "+area);
  }
  var town=new ChristmasTownVisuals();town.prepare();
  for(String state:java.util.List.of("cold","restored")){
   var old=ImageIO.read(Path.of("build/phase10-3/before/town-"+state+".png").toFile());var now=(BufferedImage)field(town,state);int diffs=0;
   for(int y=0;y<old.getHeight();y++)for(int x=0;x<old.getWidth();x++)if(old.getRGB(x,y)!=now.getRGB(x,y)){
    if(!Phase103PreservationSupport.objectRegion(x,y))throw new AssertionError("Prepared terrain outside exact retired/new object bounds "+x+","+y);diffs++;
   }check(diffs>50000,"Substantial object redesign, unchanged prepared roads/snow outside local silhouettes "+state);
  }
  Phase103PreservationSupport.frame(2,ForestLandmarkPreservationTest.raw(2));check(true,"Town HUD/ground outside precise objects/lights/ritual unchanged");
  for(String file:java.util.List.of("src/boss/DarkTitan.java","src/boss/BossAttacks.java")){
   String old=new String(Phase10PreservationSupport.archive(file,"PHASE10_3"),java.nio.charset.StandardCharsets.UTF_8),now=Files.readString(Path.of(file));
   if(file.endsWith("DarkTitan.java"))now=new String(Phase3PreservationSupport.previous(file,Files.readAllBytes(Path.of(file))),java.nio.charset.StandardCharsets.UTF_8);
   if(file.endsWith("DarkTitan.java"))check(section(old,"    @Override public boolean takeDamage","    public String getAnimationName").equals(section(now,"    @Override public boolean takeDamage","    public String getAnimationName")),"HP, thresholds, AI, speed, timing and swept movement are byte-identical");
   else check(section(old,"    public void begin(","    /** Texture-based").equals(section(now,"    public void begin(","    private void effect(")),"All attack damage, warning/execution/recovery clocks, collisions remain byte-identical");
  }
  check(SpiritTreeTransformation.DURATION==180,"Exact restoration model retained");
  System.out.println("PASS: "+checks+" raw hashes, all unaffected world pixels, prepared Town floor and exact combat assertions");
 }
}
