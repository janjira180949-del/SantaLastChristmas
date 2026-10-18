import audio.*;
import game.*;
import entity.*;
import boss.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.nio.file.*;
import java.util.*;

/** Actual PCM, accepted inputs, finite transactions, local movement and fall layout. */
public class PostPhase4PolishTest extends V2TestSupport {
 static final String ROOT="assets/audio/post_phase4/";
 static void mappings()throws Exception{
  String[] ids={"60004","715067","555420","442958","728185","555421","204912"};
  String[] routes={"santa.swing","delivery.success","walker.hurt","bomb.explosion","fang.entrance","fang.hurt.v02","titan.entrance"};
  for(int i=0;i<ids.length;i++){
   String selectedId=ids[i];try(var files=Files.list(Path.of("assets/audio/incoming"))){check(files.filter(p->p.getFileName().toString().startsWith(selectedId+"__")).count()==1,"Unique untouched selected source "+ids[i]);}
   var cue=AudioCatalog.cue(routes[i]);check(cue.path().startsWith(ROOT+ids[i]+"_"),"Exact selection "+routes[i]);
   try(var in=javax.sound.sampled.AudioSystem.getAudioInputStream(Path.of(cue.path()).toFile())){
    var f=in.getFormat();check(f.getSampleRate()==44100&&f.getChannels()==2&&f.getSampleSizeInBits()==16&&!f.isBigEndian(),"PCM format "+ids[i]);
   }
   byte[] pcm=AudioResources.decode(cue.path());int peak=0;long energy=0;for(int j=0;j<pcm.length;j+=2){int v=(short)((pcm[j]&255)|(pcm[j+1]<<8));peak=Math.max(peak,Math.abs(v));energy+=(long)v*v;}
   check(pcm.length>44100&&pcm.length%4==0&&peak>1000&&peak<32767&&energy>0,"Fully decoded nonempty unclipped recording "+ids[i]);
  }
  for(String kind:List.of("walker","runner"))for(int i=1;i<=3;i++)check(AudioCatalog.cue(kind+".hurt.v0"+i).path().contains("555420"),"All regular variants use Hit1");
  check(AudioCatalog.cue("mini_fang.hurt.v02").path().contains("555421"),"Inherited creature hurt02 mini variant");
  check(AudioCatalog.cue("fang.hurt.v01").path().contains("phase4/fang_hurt_1"),"Other Fang vocals preserved");
  check(AudioCatalog.cue("titan.hurt.v01").path().contains("phase4/titan_hurt_1"),"Titan combat vocal preserved");
  check(AudioManager.CACHE_LIMIT==128L*1024*1024,"Original cache budget");
 }
 static void actualInput()throws Exception{
  GamePanel p=playing();try{
   var events=new ArrayList<String>();((AudioManager)field(p,"audio")).setEventObserver(events::add);
   santa(p).setPosition(1280,1070);attack(p,1360,1091);check(events.stream().filter(s->s.startsWith("santa.swing")).count()==1,"Accepted LMB miss emits one swing");
   attack(p,1360,1091);check(events.stream().filter(s->s.startsWith("santa.swing")).count()==1,"Rejected cooldown attack emits no swing");
   check(events.stream().noneMatch(s->s.startsWith("sword.body")||s.startsWith("sword.armor")),"Miss has no successful-hit sound");
  }finally{p.stopGame();}
  Phase4AudioTest.contacts();Phase4AudioTest.deliveriesAndPaint();Phase4AudioTest.crystalContact();
  Phase106AudioTest.skills();
 }
 static void damageSources(){
  for(int skill=0;skill<4;skill++){
   Santa santa=new Santa(0,0);Enemy enemy=new Walker(60,5);var actors=new ArrayList<Enemy>(List.of(enemy));var events=new ArrayList<String>();var observer=Phase4AudioTest.observer(events);var abilities=new AbilitySystem();
   if(skill==0){santa.setAttackDirection(1,0);santa.startAttack();observer.captureCombat(santa,actors);CombatSystem.update(santa,actors);observer.resolveCombat();}
   else if(skill==1){observer.captureCombat(santa,actors);abilities.use(1,santa,actors,List.of());observer.resolveCombat();}
   else{abilities.skills().cast(skill==2?2:3,santa,76,26,List.of());for(int i=0;i<55;i++){observer.captureCombat(santa,actors);abilities.skills().update(actors,List.of());observer.resolveCombat();}}
   check(enemy.getHealth()<100&&events.stream().anyMatch(x->x.equals("walker.hurt")),"Actual sword/Bell/Star/Bomb damage emits hurt "+skill);
  }
 }
 static void hurts(){
  for(boolean runner:new boolean[]{false,true}){
   var events=new ArrayList<String>();var g=Phase4AudioTest.observer(events);Enemy e=runner?new Runner(200,200):new Walker(200,200);Santa s=new Santa(0,0);
   for(int i=0;i<14;i++){g.captureCombat(s,List.of(e));e.takeDamage(1);g.resolveCombat();}
   check(events.stream().filter(x->x.endsWith(".hurt")).count()==3,"Per-actor six-update retrigger, not global suppression");
   g.captureCombat(s,List.of(e));g.resolveCombat();check(events.stream().filter(x->x.endsWith(".hurt")).count()==3,"No hurt without actual damage");
  }
 }
 @SuppressWarnings("unchecked") static List<Rectangle2D> movementSolids(GamePanel p)throws Exception{
  var m=GamePanel.class.getDeclaredMethod("playerMovementSolids",List.class);m.setAccessible(true);return (List<Rectangle2D>)m.invoke(p,WorldMap.forArea(Area.Type.CHRISTMAS_TOWN).solids());
 }
 static void sleigh()throws Exception{
  GamePanel p=endingFixture();try{
   tick(p,180);JourneyHomeTest.completeCelebration(p);
   check(JourneyHomeVisuals.SLEIGH_ORIGIN.equals(new WorldMap.Point(1168,1360))&&JourneyHomeVisuals.SLEIGH_APPROACH.equals(new WorldMap.Point(1296,1526)),"Exact original anchors");
   var footprints=JourneyHomeVisuals.parkedFootprints();check(footprints.size()==3,"Separate body/two deer footprints");
   var solids=movementSolids(p);check(solids.size()==WorldMap.forArea(Area.Type.CHRISTMAS_TOWN).solids().size()+3,"Only three player-local parked additions; world geometry unchanged");
   for(var f:footprints){
    int cx=(int)f.getCenterX(),cy=(int)f.getCenterY();
    for(int[] d:new int[][]{{0,1},{0,-1},{1,0},{-1,0},{1,1},{-1,1},{1,-1},{-1,-1}}){
     Santa s=new Santa(cx-16-d[0]*120,cy-21-d[1]*120);boolean reached=false;
     for(int i=0;i<60;i++){s.update(d[1]<0,d[1]>0,d[0]<0,d[0]>0,List.of(f));check(!s.getBounds().intersects(f),"Actual movement cannot enter grounded solids from eight directions");if(Math.hypot(s.getX()+16-cx,s.getY()+21-cy)<Math.max(70,f.getWidth()/2+20))reached=true;}
     check(reached,"No oversized barrier before the actual body");
    }
   }
   santa(p).setPosition(1328,1486);check(!util.Collision.intersectsAny(santa(p).getBounds(),footprints),"Open harness gap remains walkable");
   santa(p).setPosition(1270,1300);check(JourneyHomeVisuals.parkedInFrontOf(santa(p)),"North stands behind branches/assembly");
   at(santa(p),JourneyHomeVisuals.SLEIGH_APPROACH);check(!JourneyHomeVisuals.parkedInFrontOf(santa(p)),"South actor draws in front");
   check(!util.Collision.intersectsAny(santa(p).getBounds(),solids),"Exact original E approach clear");
   check(FinalChapter.inReach(santa(p),WorldMap.forArea(Area.Type.CHRISTMAS_TOWN),JourneyHomeVisuals.SLEIGH_APPROACH),"Original distance and LOS still sufficient");
   JourneyHomeTest.takeSleigh(p);JourneyHomeTest.openLetters(p);
  }finally{p.stopGame();}
 }
 static void depthVisibility()throws Exception{
  JourneyHomeVisuals.prepareAsync().get();Santa actor=new Santa(1234,1419);
  var base=new java.awt.image.BufferedImage(500,300,2);var visible=new java.awt.image.BufferedImage(500,300,2);
  var g=base.createGraphics();g.translate(-1068,-1280);JourneyHomeVisuals.drawSleigh(g,actor,-1);g.dispose();
  g=visible.createGraphics();g.translate(-1068,-1280);JourneyHomeVisuals.drawSleigh(g,actor,-1);actor.draw(g);JourneyHomeVisuals.drawParkedForeground(g,actor);g.dispose();
  int headPixels=0;for(int y=actor.getY()-8;y<actor.getY()+18;y++)for(int x=actor.getX();x<actor.getX()+32;x++)if(base.getRGB(x-1068,y-1280)!=visible.getRGB(x-1068,y-1280))headPixels++;
  check(headPixels>30,"Close north-side Santa keeps a visible head/upper-body silhouette; parked art cannot swallow him");
  check(JourneyHomeVisuals.parkedInFrontOf(actor),"Foreground still owns lower body when actor stands behind it");
 }
 static void panels()throws Exception{
  for(boolean fang:new boolean[]{true,false})for(int[] size:new int[][]{{800,600},{1270,720}}){
   var f=new BossFallPresentation();Enemy e=fang?new FrostFang(200,200):new DarkTitan(200,200);f.start(e,new WorldMap.Point(200,200));
   var image=new java.awt.image.BufferedImage(size[0],size[1],2);var g=image.createGraphics();f.drawScreen(g,size[0],size[1]);g.dispose();
   check(f.getDuration()==(fang?240:300)&&f.getFrames()==0,"Panel painting never advances cinematic timing");
   int top=size[1]-170;check((image.getRGB(size[0]/2,top+30)>>>24)>0,"Approved parchment in bottom-center safe area");
   check((image.getRGB(size[0]/2,size[1]/2)>>>24)==0,"Cinematic center remains visible");
   f.finish();check(!f.isActive(),"Existing finish/skip clears fall");
  }
 }
 static void mixer()throws Exception{
  final class Output implements AudioManager.Sink{
   final java.util.concurrent.Semaphore permits=new java.util.concurrent.Semaphore(0);
   final java.util.concurrent.atomic.AtomicInteger blocks=new java.util.concurrent.atomic.AtomicInteger(),peak=new java.util.concurrent.atomic.AtomicInteger();
   public void write(byte[] b){int v=0;for(int i=0;i<b.length;i+=2)v=Math.max(v,Math.abs((short)((b[i]&255)|(b[i+1]<<8))));peak.accumulateAndGet(v,Math::max);blocks.incrementAndGet();try{permits.acquire();}catch(InterruptedException e){Thread.currentThread().interrupt();}}
   public void close(){permits.release();}
   void step()throws Exception{int n=blocks.get();permits.release();Phase106AudioTest.await(()->blocks.get()==n+1,"Actual mixer wrote next PCM block");}
  }
  var out=new Output();var m=new AudioManager(()->out,AudioResources::decode,null);
  try{m.setMasterVolume(100);m.setMusicVolume(0);m.setSfxVolume(100);m.start();Phase106AudioTest.await(()->out.blocks.get()==1&&m.getDecodedCount()==Phase4PreservationSupport.preloadCount(),"All real selected PCM preloaded off-thread");
   for(String id:List.of("santa.swing","delivery.success","walker.hurt","bomb.explosion","fang.entrance","fang.hurt.v02","titan.entrance")){
    m.cancelGameplay();m.cancelPresentation();out.step();out.peak.set(0);m.beginUpdate();if(id.equals("santa.swing"))m.playEffect(AudioCatalog.Effect.SANTA_ATTACK);else m.playEvent(id);
    for(int i=0;i<70;i++)out.step();check(out.peak.get()>400&&out.peak.get()<32767,"Correct nonzero PCM reaches existing mixer output "+id);check(m.getActiveVoices()<=8,"Eight voices unchanged");
   }
   check(m.getPeakRetainedBytes()<=AudioManager.CACHE_LIMIT,"Cache remains bounded");
  }finally{m.close();}
 }
 public static void main(String[]args)throws Exception{mappings();actualInput();damageSources();hurts();sleigh();depthVisibility();panels();mixer();System.out.println("PostPhase4PolishTest PASS "+checks+" PCM/event/movement/layout assertions");}
}
