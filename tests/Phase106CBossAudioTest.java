import audio.*;import boss.*;import entity.*;import game.*;import java.util.*;import java.nio.file.*;import java.awt.*;import java.awt.event.KeyEvent;import java.awt.image.BufferedImage;
/** Real cinematic clocks/input and downloaded resource decoding; no subjective audition claim. */
public class Phase106CBossAudioTest extends V2TestSupport{
 static long difference(BufferedImage a,BufferedImage b){long d=0;for(int y=0;y<a.getHeight();y++)for(int x=0;x<a.getWidth();x++){int u=a.getRGB(x,y),v=b.getRGB(x,y);d+=Math.abs((u>>>24)-(v>>>24));for(int s:new int[]{0,8,16})d+=Math.abs((u>>s&255)*(u>>>24)/255-(v>>s&255)*(v>>>24)/255);}return d;}
 static BufferedImage pose(boolean fang,int frame){var a=new BufferedImage(400,340,BufferedImage.TYPE_INT_ARGB);var g=a.createGraphics();BossArtwork.drawDefeat(g,fang,frame,200,300,false);g.dispose();return a;}
 static void continuity()throws Exception{
  BossArtwork.prepareAsync().get();
  for(boolean fang:new boolean[]{true,false}){
   int hold=fang?11:25,count=fang?8:4,changed=0;
   for(int i=1;i<count;i++){
    int boundary=hold*i;long first=difference(pose(fang,boundary-1),pose(fang,boundary));long span=difference(pose(fang,boundary-1),pose(fang,boundary+7));
    check(first==0,"Pose boundary holds exact previous approved frame "+fang+"/"+i);if(span>0)changed++;
    for(int t=1;t<=7;t++)check((span==0?difference(pose(fang,boundary+t-1),pose(fang,boundary+t))==0:difference(pose(fang,boundary+t-1),pose(fang,boundary+t))<span),"Seven-update bounded pose dissolve "+fang+"/"+i+"/"+t);
   }
   check(changed>=count-2,"All distinct approved poses remain; existing final held frame is permitted");
   check(difference(pose(fang,fang?87:99),pose(fang,fang?100:110))==0,"Last approved grounded pose holds without cycling");
  }
 }
 static void cinematics()throws Exception{
  for(boolean fang:new boolean[]{true,false})for(boolean skip:new boolean[]{false,true}){
   GamePanel p=Phase102FCinematicTest.entrance(fang,800,600);try{
    var audio=(AudioManager)field(p,"audio");var events=new ArrayList<String>();audio.setEventObserver(events::add);
    closeOverlay(p);if(!santa(p).isDeveloperTestMode())santa(p).toggleDeveloperTestMode();tick(p,65);
    Enemy enemy=(Enemy)field(p,"activeBoss");int max=enemy.getMaximumHealth();check(max==(fang?900:1200),"Boss HP unchanged");
    AudioCatalog.Music[] tracks=fang?new AudioCatalog.Music[]{AudioCatalog.Music.FANG1,AudioCatalog.Music.FANG2,AudioCatalog.Music.FANG3}:new AudioCatalog.Music[]{AudioCatalog.Music.BOSS,AudioCatalog.Music.TITAN2,AudioCatalog.Music.TITAN3};
    check(audio.getRequestedMusic()==tracks[0],"Phase1 uses energetic replacement");enemy.takeDamage(max/2);tick(p,1);check(audio.getRequestedMusic()==tracks[1],"Authoritative phase2 immediate routing");
    enemy.takeDamage(max/4);tick(p,1);check(audio.getRequestedMusic()==tracks[2],"Authoritative phase3 immediate routing");
    var tree=(SpiritTreeTransformation)field(p,"treeTransformation");check(!tree.isActive(),"No automatic restoration before victory");
    events.clear();enemy.takeDamage(max);tick(p,1);var fall=(BossFallPresentation)field(p,"bossFall");String impact=fang?"fang.fall":"titan.collapse";
    check(fall.isActive()&&fall.getFrames()==0,"Staged fall begins at0");check(!events.contains(impact),"No premature impact at lethal hit");check(audio.getRequestedMusic()==tracks[2],"Encounter phrase held during initial fall");
    var stopped=(java.util.Map<?,?>)field(audio,"owners");check(!stopped.containsKey("boss-attack"),"Attack sustains canceled on death");
    if(skip){tick(p,20);tap(p,KeyEvent.VK_E);check(!fall.isActive()&&!events.contains(impact),"Early skip cannot play a later impact");}
    else{
     int lastX=Phase102FCinematicTest.camera(p).getX(),lastY=Phase102FCinematicTest.camera(p).getY();
     for(int t=1;t<=fall.getDuration();t++){
      tick(p,1);long n=events.stream().filter(impact::equals).count();check(n==(t>=fall.getImpactFrame()?1:0),"Exactly one impact at actual grounded pose frame "+t);
      if(t<fall.getResolveFrame())check(audio.getRequestedMusic()==tracks[2],"No early scene switch "+t);
      else check(audio.getRequestedMusic()==(fang?AudioCatalog.Music.FOREST_INVESTIGATION:AudioCatalog.Music.TREE),"Resolving next-scene music selected "+t);
      Camera camera=Phase102FCinematicTest.camera(p);check(Math.abs(camera.getX()-lastX)<=18&&Math.abs(camera.getY()-lastY)<=18,"Existing bounded camera path "+t);lastX=camera.getX();lastY=camera.getY();
     }
     check(!fall.isActive(),"Fall completes at its authoritative staged duration");
    }
    Phase102FCinematicTest.normal(p);check(fang?story(p).isFrostFangDefeated()&&!story(p).isBossDefeated():story(p).isBossDefeated(),"Independent victory flags preserved");
    check(!tree.isActive()&&tree.getFrames()==0,"Tree still requires player E");int spirit=((spirit.SpiritTree)field(p,"spiritTree")).getSpirit();tick(p,5);check(((spirit.SpiritTree)field(p,"spiritTree")).getSpirit()==spirit,"No duplicate reward after fall");
    check(audio.getRequestedMusic()==(fang?AudioCatalog.Music.FOREST_INVESTIGATION:AudioCatalog.Music.TREE),"No defeated-boss music restart");
   }finally{p.stopGame();}
  }
 }
 static final class StepSink implements AudioManager.Sink{
  final java.util.concurrent.Semaphore permits=new java.util.concurrent.Semaphore(0);final java.util.concurrent.atomic.AtomicInteger blocks=new java.util.concurrent.atomic.AtomicInteger();volatile int sample;
  public void write(byte[] b){sample=(short)((b[b.length-4]&255)|(b[b.length-3]<<8));blocks.incrementAndGet();try{permits.acquire();}catch(InterruptedException e){Thread.currentThread().interrupt();}}
  public void close(){permits.release();}
  void step()throws Exception{int n=blocks.get();permits.release();Phase106AudioTest.await(()->blocks.get()==n+1,"Output advanced exactly one block");}
  void steps(int count)throws Exception{for(int i=0;i<count;i++)step();}
 }
 static void mix()throws Exception{
  StepSink sink=new StepSink();byte[] signal=new byte[1764],silent=new byte[1764];for(int i=0;i<signal.length;i+=4){signal[i]=16;signal[i+1]=39;signal[i+2]=16;signal[i+3]=39;}
  AudioManager manager=new AudioManager(()->sink,path->path.contains("/music/")?signal:silent,null);
  try{
   manager.setMasterVolume(100);manager.setMusicVolume(100);manager.setSfxVolume(100);manager.playMusic(AudioCatalog.Music.FANG1);manager.start();Phase106AudioTest.await(()->sink.blocks.get()==1&&manager.getDecodedCount()>=77,"Critical cues prepared");sink.steps(101);check(sink.sample==10000,"Normal gain unchanged");
   manager.setBossFalling(true);sink.step();check(sink.sample>9000&&sink.sample<10000,"Fall attenuation begins gradually");sink.steps(40);check(sink.sample==2800,"Controlled28percent music bed under impact");
   manager.beginUpdate();manager.playEvent("fang.claw.warning");sink.steps(11);check(sink.sample==2240,"Typed attack warning gets clear20percent music duck");sink.steps(140);check(sink.sample==2800,"Warning duck resolves without changing settings");
   manager.setBossFalling(false);sink.steps(51);check(sink.sample==10000,"Smooth gain handoff restores normal mix");check(manager.getMasterVolume()==100&&manager.getMusicVolume()==100&&manager.getSfxVolume()==100,"Temporary envelope never changes saved-volume settings");
   manager.setMuted(true);sink.step();check(sink.sample==0,"Mute includes fall envelope");manager.setMuted(false);sink.step();check(sink.sample==10000,"Unmute remains correct");
  }finally{manager.close();}
 }
 static void resources()throws Exception{
  var phase=java.util.List.of(AudioCatalog.Music.FANG1,AudioCatalog.Music.FANG2,AudioCatalog.Music.FANG3,AudioCatalog.Music.BOSS,AudioCatalog.Music.TITAN2,AudioCatalog.Music.TITAN3);Set<String> hashes=new HashSet<>();
  for(var music:phase){byte[] pcm=AudioResources.decode(music.path());check(pcm.length>20*44100*4&&pcm.length<40*44100*4,"Prepared bounded phrase duration "+music);check(hashes.add(ForestPreservationSupport.hash(pcm)),"Distinct actual phase music "+music);check(pcm[0]==pcm[pcm.length-4]&&pcm[1]==pcm[pcm.length-3],"De-clicked PCM endpoint "+music);}
  for(String id:java.util.List.of("fang.fall","titan.collapse"))check(AudioResources.decode(AudioCatalog.cue(id).path()).length>44100*3,"Real nonempty physical fall accent "+id);
  check(AudioPolish.transitionMillis(AudioCatalog.Music.FANG1,AudioCatalog.Music.FANG2)==500&&AudioPolish.transitionMillis(AudioCatalog.Music.TITAN2,AudioCatalog.Music.TITAN3)==500,"Responsive half-second phase crossfades");
  check(AudioPolish.transitionMillis(AudioCatalog.Music.TITAN3,AudioCatalog.Music.TREE)==750,"Controlled resolving crossfade");
  check(AudioManager.CACHE_LIMIT==128L*1024*1024,"No cache budget increase");
  String licenses=Files.readString(Path.of("assets/audio/phase10_6/LICENSE_EVIDENCE.json"));check(licenses.contains("battle-theme-a")&&licenses.contains("Tarush Singhal")&&licenses.contains("CC0-1.0"),"Actual new composer/license evidence distributed");
 }
 public static void main(String[] args)throws Exception{resources();continuity();mix();cinematics();System.out.println("Phase106CBossAudioTest PASS "+checks+" assertions");}
}
