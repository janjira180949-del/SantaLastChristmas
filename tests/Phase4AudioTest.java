import audio.*;import boss.*;import entity.*;import game.*;import java.nio.file.*;import java.awt.event.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
/** Real transaction/update clocks and actual decoded recordings; no human-listening claim. */
public class Phase4AudioTest extends V2TestSupport {
 static final List<java.awt.geom.Rectangle2D> EMPTY=List.of();
 static GameplayAudio observer(List<String> events){var g=new GameplayAudio(e->{});g.bindEvents(events::add,(id,owner)->events.add("owned:"+id),owner->events.add("stop:"+owner));return g;}
 static long count(List<String> list,String prefix){return list.stream().filter(s->s.startsWith(prefix)).count();}
 static void set(Object o,String n,Object v)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static void assets()throws Exception{
  var assets=Phase10PreservationSupport.properties("docs/PHASE4_ASSETS.properties");int n=0;long bytes=0;
  Set<String> hashes=new HashSet<>();
  for(String path:assets.stringPropertyNames()){
   check(ForestPreservationSupport.hash(Files.readAllBytes(Path.of(path))).equals(assets.getProperty(path)),"Authenticated delivered asset "+path);
   if(!path.endsWith(".wav"))continue;var file=new java.io.File(path);
   try(var stream=javax.sound.sampled.AudioSystem.getAudioInputStream(file)){
    var f=stream.getFormat();check(f.getSampleRate()==44100&&f.getSampleSizeInBits()==16&&f.getChannels()==2&&!f.isBigEndian()&&f.getEncoding()==javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED,"PCM contract "+path);
   }
   byte[] pcm=AudioResources.decode(path);check(pcm.length>4410&&pcm.length%4==0,"Real full Java Sound decode "+path);int peak=0,clipped=0,first=-1,last=0;
   for(int i=0;i<pcm.length;i+=2){int v=Math.abs((short)((pcm[i]&255)|(pcm[i+1]<<8)));peak=Math.max(peak,v);if(v>=32767)clipped++;if(v>32){if(first<0)first=i;last=i;}}
   check(peak>=2000&&peak<25000&&clipped==0,"Useful physical transient and unclipped headroom "+path);
   check(first<44100*.12*4&&pcm.length-last<44100*.25*4,"No excessive silent lead/tail "+path);
   check(hashes.add(ForestPreservationSupport.hash(pcm)),"Distinct delivered recording/composite "+path);bytes+=pcm.length;n++;
  }
  check(n==59&&bytes<16*1024*1024,"59 real WAVs within existing cache budget");
  var routes=Phase10PreservationSupport.properties("docs/PHASE4_ROUTES.properties");for(String id:routes.stringPropertyNames())check(AudioCatalog.cue(id)!=null&&AudioCatalog.cue(id).path().equals(PostPhase4Support.route(id)?PostPhase4Support.properties("ROUTES").getProperty(id):routes.getProperty(id)),"Exact event mapping "+id);
  for(String id:List.of("fang.claw.warning","titan.slam.warning","titan.charge.warning","titan.beam.warning"))check(AudioCatalog.cue(id).priority()>AudioCatalog.cue("star.enemy_hit").priority(),"Warnings outrank decorative hits "+id);
  check(!AudioCatalog.cue("fang.entrance").path().equals(AudioCatalog.cue("titan.entrance").path()),"Separate boss signature recordings");
  check(AudioCatalog.cue("mini_fang.hurt").gain()<AudioCatalog.cue("fang.hurt").gain(),"Mini voice quieter than boss");
  String meta=Files.readString(Path.of("assets/audio/phase4/manifest.json"));for(String key:List.of("source_url","creator","CC0-1.0","sha256","processing","original_filename","verified_date","components"))check(meta.contains("\""+key+"\"")||meta.contains(key),"Recorded provenance "+key);
 }
 static void contacts(){
  var events=new ArrayList<String>();var g=observer(events);Santa s=new Santa(0,0);Walker target=new Walker(45,5);List<Enemy> actors=new ArrayList<>(List.of(target));s.setAttackDirection(1,0);s.startAttack();
  CombatSystem.update(s,actors,g::swordContact);check(target.getHealth()==70&&events.equals(List.of("sword.body")),"Accepted basic damage has one physical impact");CombatSystem.update(s,actors,g::swordContact);check(events.size()==1,"Held attack cannot repeat successful hit");
  events.clear();s=new Santa(0,0);s.setAttackDirection(-1,0);s.startAttack();CombatSystem.update(s,new ArrayList<>(List.of(new Walker(90,0))),g::swordContact);check(events.isEmpty(),"Miss has no impact");
  var titan=new DarkTitan(45,0);s=new Santa(0,0);s.setAttackDirection(1,0);s.startAttack();CombatSystem.update(s,new ArrayList<>(List.of(titan)),g::swordContact);check(events.equals(List.of("sword.armor")),"Boss contact gets armor texture");
  for(boolean fang:new boolean[]{true,false}){events.clear();Enemy boss=fang?new FrostFang(200,200):new DarkTitan(200,200);s=new Santa(1000,1000);for(int i=0;i<40;i++){g.captureCombat(s,List.of(boss));boss.takeDamage(1);g.resolveCombat();g.resolveCombat();}check(count(events,fang?"fang.hurt":"titan.hurt")== (fang?4:3),"Boss hurt variants rate-limited by active updates, not paints "+fang);}
  events.clear();g=observer(events);var mini=new MiniFrostFang(200,200);s=new Santa(1000,1000);for(int i=0;i<30;i++){g.captureCombat(s,List.of(mini));mini.takeDamage(1);g.resolveCombat();}check(count(events,"mini_fang.hurt")==3&&count(events,"walker.hurt")==0,"Mini routed separately with restrained hurt rate");
 }
 static void attacks()throws Exception{
  for(var pattern:List.of(BossAttacks.Attack.DARK_FROST_BEAM,BossAttacks.Attack.GROUND_SLAM,BossAttacks.Attack.BEAST_CHARGE,BossAttacks.Attack.ICE_SPIKES,BossAttacks.Attack.BLACK_FROST_STORM,BossAttacks.Attack.ICE_BLAST,BossAttacks.Attack.FROST_WAVE,BossAttacks.Attack.FROST_DASH,BossAttacks.Attack.CLAW)){
   var events=new ArrayList<String>();var g=observer(events);boolean fang=pattern.ordinal()<BossAttacks.Attack.GROUND_SLAM.ordinal();Enemy boss=fang?new FrostFang(200,200):new DarkTitan(200,200);var a=((BossCombatant)boss).attacks();Santa s=new Santa(1200,1200);
   g.captureCombat(s,List.of(boss));a.begin(pattern,boss,s,EMPTY);g.resolveCombat();check(count(events,"stop:boss-attack")==1,"Previous sustain canceled on new attack "+pattern);int warning=a.getWarningFrames();
   for(int i=1;i<=warning;i++){events.clear();g.captureCombat(s,List.of(boss));a.update(boss,s,EMPTY);g.resolveCombat();if(i<warning)check(events.stream().noneMatch(x->x.endsWith(".execute")),"No early execution sound "+pattern+"/"+i);}
   check(events.stream().anyMatch(x->x.endsWith(".execute")),"Execution at original warning boundary "+pattern);
   if(pattern==BossAttacks.Attack.DARK_FROST_BEAM){check(events.contains("owned:titan.beam.sustain"),"Owned beam begins with actual release");events.clear();for(int i=0;i<44;i++){g.captureCombat(s,List.of(boss));a.update(boss,s,EMPTY);g.resolveCombat();}check(!events.contains("titan.beam.end"),"No early beam end");g.captureCombat(s,List.of(boss));a.update(boss,s,EMPTY);g.resolveCombat();check(events.contains("titan.beam.end")&&events.contains("stop:boss-attack"),"Beam stop/end at60+45");}
   if(pattern==BossAttacks.Attack.BEAST_CHARGE){events.clear();for(int i=0;i<14;i++){g.captureCombat(s,List.of(boss));a.update(boss,s,EMPTY);g.resolveCombat();}check(!events.contains("titan.charge.impact"),"No premature charge impact");g.captureCombat(s,List.of(boss));a.update(boss,s,EMPTY);g.resolveCombat();check(count(events,"titan.charge.impact")==1,"Impact at original launch+15");}
   if(pattern==BossAttacks.Attack.CLAW)check(!events.contains("fang.claw.hit"),"Claw miss cannot emit successful contact");
  }
  var events=new ArrayList<String>();var g=observer(events);var f=new FrostFang(200,200);Santa s=new Santa(250,207);var a=f.attacks();a.begin(BossAttacks.Attack.CLAW,f,s,EMPTY);for(int i=0;i<36;i++){g.captureCombat(s,List.of(f));a.update(f,s,EMPTY);g.resolveCombat();}check(a.hasHit()&&count(events,"fang.claw.hit")==1,"Actual claw contact gets one impact");
  events.clear();g=observer(events);var t=new DarkTitan(200,200);set(t,"entrance",0);set(t,"cooldown",1000);s=new Santa(1000,1000);long contacts=t.getFootstepSequence();for(int i=0;i<90;i++){g.captureCombat(s,List.of(t));t.advanceCombatTimers();t.update(s,EMPTY);g.resolveCombat();}check(t.getFootstepSequence()>contacts&&count(events,"titan.foot_contact")==t.getFootstepSequence()-contacts,"Heavy footsteps match actual approved foot contacts");
 }
 static void deliveriesAndPaint()throws Exception{
  GamePanel p=playing();try{AudioManager a=(AudioManager)field(p,"audio");var events=new ArrayList<String>();a.setEventObserver(events::add);
   for(var site:List.of(VillageActivities.Site.IRIS,VillageActivities.Site.CLARA,VillageActivities.Site.BEN)){int n=(int)count(events,"delivery.success");santa(p).setPosition(site.x-16,site.y+4);tap(p,KeyEvent.VK_E);check(count(events,"delivery.success")==n+1,"Success follows accepted delivery "+site);tick(p,GiftDeliveryPresentation.DURATION);closeDialogue(p);tap(p,KeyEvent.VK_E);closeDialogue(p);check(count(events,"delivery.success")==n+1,"Repeated conversation never replays delivery "+site);}
   int n=events.size();for(int i=0;i<4;i++)render(p);check(events.size()==n,"Actual GamePanel paints produce zero audio events");
   events.clear();tap(p,KeyEvent.VK_2);tick(p,8);check(count(events,"star.release.v")==3,"Actual key2 releases precisely three cues");
   int cd=((AbilitySystem)field(p,"abilitySystem")).skills().starCooldown();tap(p,KeyEvent.VK_ESCAPE);tick(p,30);check(((AbilitySystem)field(p,"abilitySystem")).skills().starCooldown()==cd,"Pause preserves cooldown and releases");tap(p,KeyEvent.VK_ESCAPE);
   events.clear();p.beginAdventure();tick(p,1);check(count(events,"delivery.success")==0,"Fresh Start cannot replay prior rewards");
  }finally{p.stopGame();}
 }
 static void crystalContact()throws Exception{
  GamePanel p=playing();try{investigationFixture(p);AudioManager audio=(AudioManager)field(p,"audio");var events=new ArrayList<String>();audio.setEventObserver(events::add);
   BlackFrostCrystal crystal=(BlackFrostCrystal)((List<?>)field(p,"crystals")).get(0);at(santa(p),crystal.getPosition());tick(p,1);
   at(santa(p),new WorldMap.Point(crystal.getPosition().x(),crystal.getPosition().y()+48));attack(p,crystal.getPosition().x(),crystal.getPosition().y());tick(p,8);
   check(count(events,"crystal.impact")==1&&count(events,"crystal.break")==0&&crystal.getState()==BlackFrostCrystal.State.GUARDED,"Guarded basic-attack contact has one hard impact without invented HP or break");
   tick(p,20);clearWave(p);events.clear();at(santa(p),new WorldMap.Point(crystal.getPosition().x(),crystal.getPosition().y()+48));attack(p,crystal.getPosition().x(),crystal.getPosition().y());tick(p,24);
   check(crystal.getState()==BlackFrostCrystal.State.BROKEN&&count(events,"crystal.break")==1,"Fresh basic attack retains authoritative one-time shatter");long impacts=count(events,"crystal.impact");attack(p,crystal.getPosition().x(),crystal.getPosition().y());tick(p,24);check(count(events,"crystal.break")==1&&count(events,"crystal.impact")==impacts,"Broken objective cannot replay hit or destroy audio");
  }finally{p.stopGame();}
 }
 static final class Sink implements AudioManager.Sink {final AtomicInteger blocks=new AtomicInteger(),peak=new AtomicInteger(),maxVoices=new AtomicInteger();final AtomicBoolean closed=new AtomicBoolean();volatile AudioManager owner;public void write(byte[] b){int p=0;for(int i=0;i<b.length;i+=2)p=Math.max(p,Math.abs((short)((b[i]&255)|(b[i+1]<<8))));peak.accumulateAndGet(p,Math::max);if(owner!=null)maxVoices.accumulateAndGet(owner.getActiveVoices(),Math::max);blocks.incrementAndGet();java.util.concurrent.locks.LockSupport.parkNanos(2_000_000);}public void close(){closed.set(true);}}
 static void cinematicCancellation()throws Exception{
  var sink=new Phase106CBossAudioTest.StepSink();byte[] pcm=new byte[176400];Arrays.fill(pcm,(byte)3);var m=new AudioManager(()->sink,path->pcm,null);
  try{m.start();Phase106AudioTest.await(()->sink.blocks.get()==1&&m.getDecodedCount()==Phase4PreservationSupport.preloadCount(),"Bounded preload before exact cancellation checks");
   m.beginUpdate();m.playOwned("sleigh.bells.move","cinematic",false);sink.step();check(m.getActiveVoices()==1,"Existing cinematic voice starts");m.cancelPresentation();sink.step();check(m.getActiveVoices()==0,"Cinematic travel/skip cancellation remains immediate; no new tail");
   m.beginUpdate();m.playOwned("titan.beam.sustain","boss-attack",true);sink.step();check(m.getActiveVoices()==1,"Gameplay sustain starts");m.stopOwner("boss-attack");sink.step();check(m.getActiveVoices()==1,"Only gameplay-owned sustain gets one short release block");sink.step();check(m.getActiveVoices()==0,"Gameplay sustain release completes after20ms");
  }finally{m.close();}
 }
 static void mixer()throws Exception{
  Sink sink=new Sink();AudioManager m=new AudioManager(()->sink,AudioResources::decode,null);sink.owner=m;
  try{m.playMusic(AudioCatalog.Music.TITAN3);m.start();Phase106AudioTest.await(()->m.getDecodedCount()>=Phase4PreservationSupport.preloadCount()+1,"All real new/approved cues preload");
   for(var ids:List.of(List.of("santa.swing","sword.body","fang.hurt"),List.of("bell.release","bell.hit","walker.hurt"),List.of("star.release.v01","star.release.v02","star.release.v03"),List.of("bomb.explosion","bomb.confirmed_hit","titan.hurt"),List.of("titan.foot_contact","titan.slam.warning","santa.damage"),List.of("titan.beam.warning","mini_fang.warning","titan.beam.execute"))){m.beginUpdate();for(String id:ids)m.playEvent(id);Thread.sleep(80);}
   m.beginUpdate();m.playOwned("titan.beam.sustain","boss-attack",true);Thread.sleep(30);m.stopOwner("boss-attack");Thread.sleep(30);check(m.getActiveVoices()<=8,"Eight voices including short owned release tail");m.cancelGameplay();Phase106AudioTest.await(()->m.getActiveVoices()==0,"Travel/retry/victory cancellation removes pending and sustained sounds");
   m.beginUpdate();m.playOwned("titan.beam.sustain","boss-attack",true);Thread.sleep(20);m.setGameplaySuspended(true);Phase106AudioTest.await(()->m.getActiveVoices()==0,"Pause/dialogue cancel active loop");m.setGameplaySuspended(false);Thread.sleep(20);check(m.getActiveVoices()==0,"No stale loop on resume");
   check(sink.peak.get()>200&&sink.peak.get()<32767&&sink.maxVoices.get()<=8,"Actual engine mixed real multi-cue recordings plus approved boss music without full-scale samples");check(m.getPeakRetainedBytes()<=AudioManager.CACHE_LIMIT&&AudioManager.CACHE_LIMIT==128L*1024*1024,"Original128MiB bound retained");
   Files.writeString(Path.of("build/phase4/mixed-evidence.json"),"{\"peak\":"+sink.peak.get()+",\"max_voices\":"+sink.maxVoices.get()+",\"peak_cache_bytes\":"+m.getPeakRetainedBytes()+",\"blocks\":"+sink.blocks.get()+",\"audible_device\":false}\n");
  }finally{m.close();}Phase106AudioTest.await(sink.closed::get,"Single output resources closed");
 }
 public static void main(String[]args)throws Exception{assets();contacts();attacks();deliveriesAndPaint();crystalContact();Phase106AudioTest.skills();cinematicCancellation();mixer();System.out.println("Phase4AudioTest PASS "+checks+" recording/event/mix assertions");}
}
