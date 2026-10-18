import game.*;
import java.nio.file.*;
import java.util.*;

/** Raw current hashes protect every unchanged asset/world/AI/dialogue and the exact five edited owners. */
public class Phase102FPreservationTest extends V2TestSupport {
 public static void main(String[] args)throws Exception{
  var allowed=Phase102FPreservationSupport.allowed();var changed=new HashSet<String>();
  var matcher=java.util.regex.Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/PHASE10_2F_BASELINE.json")));
  while(matcher.find()){
   String path=matcher.group(1),hash=ForestPreservationSupport.hash(Phase103PreservationSupport.previous(path,Files.readAllBytes(Path.of(path))));
   if(!hash.equals(matcher.group(2)))changed.add(path);
   check(hash.equals(allowed.getProperty(path,matcher.group(2))),"Exact approved revision or protected original "+path);
  }
  check(changed.equals(allowed.stringPropertyNames()),"Only the ten snapshotted skill/cinematic/test owners changed");
  check(allowed.stringPropertyNames().stream().filter(p->p.startsWith("src/")).count()==5,"Five production owners; Camera, AI, HUD, artwork, story unchanged");
  for(int area:new int[]{0,3}){
   var frame=ForestLandmarkPreservationTest.raw(area);Phase102BEPreservationSupport.frame(area,frame);check(true,"Approved Village/Base pixels identical, matched camera/state "+area);
  }
  var hashes=Phase10PreservationSupport.properties("docs/PHASE10_2BE_RENDER.properties");
  check(ForestPreservationTest.pixelHash(Phase1WorldPreservationSupport.terrain(true,FrozenForestVisuals.prepareAsync().get())).equals(hashes.getProperty("forest-terrain")),"Every prepared Forest/campfire terrain pixel unchanged");
  for(int area:new int[]{1,2}){Phase102BEPreservationSupport.frame(area,ForestLandmarkPreservationTest.raw(area));check(true,"Approved normal Forest/Town presentation unchanged");}
  SantaBaseVisuals.prepareAsync().get();var p=SantaBaseNavigationTest.opening();p.setSize(1600,1280);Phase102BEPreservationSupport.frame(3,render(p));p.stopGame();check(true,"Full Base overview unchanged");
  System.out.println("PASS: "+checks+" exact source/asset/configuration and matched renderer assertions");
 }
}
