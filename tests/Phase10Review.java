import game.*;
import boss.*;
import entity.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Actual GamePanel captures and warmed offscreen paint, not native playtesting. */
public class Phase10Review extends V2TestSupport {
 static Path dir=Path.of("build/phase10/captures");
 static void shot(GamePanel p,String name)throws Exception{call(p,"updateCamera");ImageIO.write(render(p),"png",dir.resolve(name+"-"+p.getWidth()+"x"+p.getHeight()+".png").toFile());}
 static void pose(GamePanel p,Enemy actor)throws Exception{
  var map=WorldMap.forArea(areas(p).getCurrentArea().getType());var safe=map.safePosition((int)actor.getX()+actor.getSize()/2+174,(int)actor.getY()+actor.getSize()-91,32,50);
  santa(p).setPosition(safe.x(),safe.y()+8);tick(p,1);
 }
 static void cast(GamePanel p,Enemy actor,BossAttacks.Attack attack,String name)throws Exception{
  pose(p,actor);var skills=((BossCombatant)actor).attacks();skills.begin(attack,actor,santa(p),WorldMap.forArea(areas(p).getCurrentArea().getType()).solids());
  tick(p,12);shot(p,name+"-warning");tick(p,skills.getWarningFrames()+8);shot(p,name+"-execution");System.out.println(name+" boss="+actor.getX()+","+actor.getY()+" santa="+santa(p).getX()+","+santa(p).getY()+" beam="+field(skills,"beamLength"));skills.clear();
 }
 static String bench(GamePanel p,String name)throws Exception{
  var image=new BufferedImage(p.getWidth(),p.getHeight(),BufferedImage.TYPE_INT_ARGB);long[] times=new long[240];
  for(int i=0;i<280;i++){var g=image.createGraphics();long start=System.nanoTime();p.paint(g);long elapsed=System.nanoTime()-start;g.dispose();if(i>=40)times[i-40]=elapsed;}
  Arrays.sort(times);var camera=(Camera)field(p,"camera");return String.format(Locale.ROOT,"{\"scene\":\"%s\",\"viewport\":[%d,%d],\"camera\":[%d,%d],\"warmup\":40,\"samples\":240,\"median_ms\":%.3f,\"p95_ms\":%.3f}",name,p.getWidth(),p.getHeight(),camera.getX(),camera.getY(),times[120]/1e6,times[227]/1e6);
 }
 public static void main(String[] args)throws Exception{
  Files.createDirectories(dir);var timings=new ArrayList<String>();
  long start=System.nanoTime();BossArtwork.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS);long prep=System.nanoTime()-start;
  for(int[] size:new int[][]{{800,600},{1270,720}}){
   GamePanel p=playing();investigationFixture(p);for(int i=1;i<=3;i++)story(p).recordCrystalDestroyed("crystal-"+i);
   p.setSize(size[0],size[1]);at(santa(p),FrostFang.ARENA);tick(p,1);tick(p,60);closeOverlay(p);santa(p).toggleDeveloperTestMode();tick(p,45);
   Enemy fang=enemies(p).stream().filter(e->e instanceof FrostFang).findFirst().orElseThrow();pose(p,fang);tick(p,250);shot(p,"01-frost-fang");
   cast(p,fang,BossAttacks.Attack.CLAW,"02-claw");cast(p,fang,BossAttacks.Attack.ICE_BLAST,"03-ice-blast");
   fang.takeDamage(450);tick(p,25);shot(p,"20-fang-reinforcement-warning");tick(p,35);cast(p,fang,BossAttacks.Attack.FROST_WAVE,"04-frost-wave");fang.takeDamage(225);tick(p,60);cast(p,fang,BossAttacks.Attack.FROST_DASH,"05-frost-dash");
   timings.add(bench(p,"forest-fang-"+size[0]));fang.takeDamage(fang.getMaximumHealth());tick(p,1);tick(p,35);shot(p,"13-frost-fang-defeat");p.stopGame();
   p=playing();townFixture(p);p.setSize(size[0],size[1]);story(p).acceptEliasQuest();at(santa(p),encounters(p).get("town").getCenter());tick(p,1);clearWave(p);at(santa(p),(WorldMap.Point)call(p,"spiritTreeInteractionPoint"));tick(p,1);closeOverlay(p);santa(p).toggleDeveloperTestMode();tick(p,60);
   Enemy titan=enemies(p).stream().filter(e->e instanceof DarkTitan).findFirst().orElseThrow();pose(p,titan);tick(p,250);shot(p,"06-dark-titan");
   cast(p,titan,BossAttacks.Attack.GROUND_SLAM,"07-ground-slam");cast(p,titan,BossAttacks.Attack.ICE_SPIKES,"08-ice-spikes");
   titan.takeDamage(600);tick(p,72);cast(p,titan,BossAttacks.Attack.BEAST_CHARGE,"09-beast-charge");cast(p,titan,BossAttacks.Attack.DARK_FROST_BEAM,"10-dark-frost-beam");
   titan.takeDamage(300);tick(p,72);cast(p,titan,BossAttacks.Attack.BLACK_FROST_STORM,"11-black-frost-storm");timings.add(bench(p,"town-titan-storm-"+size[0]));
   ((BossCombatant)titan).attacks().clear();pose(p,titan);var attack=((BossCombatant)titan).attacks();attack.begin(BossAttacks.Attack.BEAST_CHARGE,titan,santa(p),WorldMap.forArea(Area.Type.CHRISTMAS_TOWN).solids());shot(p,"14-charge-preparation");tick(p,40);shot(p,"15-charge-planted-arms");tick(p,10);shot(p,"16-charge-launch");tick(p,10);shot(p,"17-charge-impact");tick(p,10);shot(p,"18-charge-recovery");attack.clear();
   var abilities=(AbilitySystem)field(p,"abilitySystem");abilities.use(1,santa(p),enemies(p),WorldMap.forArea(Area.Type.CHRISTMAS_TOWN).solids());shot(p,"12-santa-skills-hud");titan.takeDamage(titan.getMaximumHealth());tick(p,1);tick(p,35);shot(p,"19-dark-titan-defeat");p.stopGame();
  }
  Files.writeString(Path.of("build/phase10/render-performance.json"),"{\"boss_art_preparation_ms\":"+prep/1e6+",\"samples\":["+String.join(",",timings)+"]}");
  System.out.println("Actual GamePanel captures: "+Files.list(dir).filter(p->p.toString().endsWith(".png")).count());
 }
}
