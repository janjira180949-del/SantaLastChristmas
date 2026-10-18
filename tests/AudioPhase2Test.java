import audio.*;
import static audio.AudioCatalog.*;
import boss.ChristmasEater;
import entity.*;
import game.*;
import spirit.SpiritTree;
import java.awt.event.KeyEvent;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

/** Actual input/event boundaries plus PCM/priority tests. Does not produce audible output. */
public final class AudioPhase2Test extends V2TestSupport {
    private static final Path OUT=Path.of("build/audio-phase2");
    private static final List<Effect> heardRequests=new ArrayList<>(); // Requests, not human listening.
    private static final Set<Effect> exercised=EnumSet.noneOf(Effect.class);
    private static void record(Effect effect){heardRequests.add(effect);exercised.add(effect);}
    private static long count(Effect effect){return Collections.frequency(heardRequests,effect);}
    private static GamePanel recordedPlaying()throws Exception {
        GamePanel p=new GamePanel(AudioPhase2Test::record);p.setSize(1270,720);tap(p,KeyEvent.VK_ENTER);tick(p,6);tap(p,KeyEvent.VK_E);enterVillage(p);return p;
    }
    private static String hash(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    private static void preserve()throws Exception {
        var m=Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/AUDIO_PHASE2_BASELINE.json")));
        int sources=0,assets=0;
        while(m.find()) {
            String path=m.group(1);
            if(Set.of("src/audio/AudioManager.java","src/audio/AudioCatalog.java","assets/audio/AUDIO_CREDITS.md","assets/audio/LICENSES.md").contains(path))continue;
            byte[] bytes=AudioPhase2PresentationSupport.originalBytes(path,Files.readAllBytes(Path.of(path)));
            check(hash(bytes).equals(m.group(2)),"Entire pre-edit owner/asset preserved apart from enumerated audio hooks: "+path);
            if(path.endsWith(".java"))sources++;else assets++;
        }
        check(sources==63&&assets>400,"All 63 protected owners and every original download/Phase 1 WAV checked");
        for(Effect e:Effect.values())if(!e.isGameplay())check(e.gain()==1&&e.path().startsWith("assets/audio/phase10_6/sfx/"),"Phase 1 cue/gain unchanged: "+e);
        AudioManager defaults=new AudioManager();
        check(defaults.getMasterVolume()==80&&defaults.getMusicVolume()==60&&defaults.getSfxVolume()==70&&!defaults.isMuted(),"Original volume defaults unchanged");defaults.close();
        check(SpiritTreeTransformation.DURATION==180,"Restoration clock unchanged");
    }
    private static void assets()throws Exception {
        String provenance=Files.readString(Path.of("assets/audio/phase2_provenance.json"));
        var records=Pattern.compile("\"path\": \"([^\"]+)\",\\s*\"sha256\": \"([a-f0-9]{64})\",\\s*\"publisher_match\": true").matcher(provenance);
        int originals=0;while(records.find()){check(hash(Files.readAllBytes(Path.of(records.group(1)))).equals(records.group(2)),"Publisher-byte-verified original: "+records.group(1));originals++;}
        check(originals==190&&!provenance.contains("\"redistribution\": false"),"All supplied Phase 2 sources verified, no assumed magic variants");
        var outputs=Pattern.compile("\"output\": \"([^\"]+)\",\\s*\"sha256\": \"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("assets/audio/phase2_conversions.json")));
        int wavs=0;
        while(outputs.find()) {
            Path p=Path.of(outputs.group(1));check(hash(Files.readAllBytes(p)).equals(outputs.group(2)),"Runtime conversion hash: "+p);
            try(var stream=javax.sound.sampled.AudioSystem.getAudioInputStream(p.toFile())) {
                var f=stream.getFormat();check(f.matches(AudioResources.FORMAT)&&f.getEncoding()==javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED,"Actual PCM16 stereo 44100: "+p);
            }
            byte[] data=AudioResources.decode(p.toString().replace('\\','/'));
            boolean nonzero=false;for(byte b:data)if(b!=0){nonzero=true;break;}
            check(nonzero&&data.length%4==0,"Real runtime PCM loads and contains signal: "+p);wavs++;
        }
        check(wavs==16,"Sixteen new WAVs; restoration stages reuse three explicit paths");
        Set<String> unique=new HashSet<>();for(Effect e:Effect.values()){unique.add(e.path());check(AudioResources.decode(e.path()).length>0,"Every actual catalog path loads: "+e);}
        check(unique.size()==21,"All25 compatibility IDs retain valid recordings; exact Phase4 contact/Bell/boss replacements are distinct");
    }
    private static void isolatedCombat() {
        heardRequests.clear();GameplayAudio g=new GameplayAudio(AudioPhase2Test::record);Santa s=new Santa(0,0);Walker e=new Walker(20,20);List<Enemy> list=new ArrayList<>(List.of(e));
        g.captureCombat(s,list);check(e.takeDamage(20),"Real damage succeeds");g.resolveCombat();g.resolveCombat();check(count(Effect.ENEMY_HIT)==1,"Enemy hit once, repeated resolve silent");
        g.captureCombat(s,list);check(!e.takeDamage(0),"Zero damage refused");g.resolveCombat();check(count(Effect.ENEMY_HIT)==1,"No false hit cue");
        g.captureCombat(s,list);e.takeDamage(100);list.clear();g.resolveCombat();check(count(Effect.ENEMY_DEFEAT)==1,"Removed defeated actor still sounds once");
        g.captureCombat(s,list);check(s.takeDamage(10),"Santa actual damage succeeds");g.resolveCombat();check(count(Effect.SANTA_DAMAGE)==1,"Santa HP loss sounds once");
        g.captureCombat(s,list);check(!s.takeDamage(10),"Damage cooldown blocks repeated damage");g.resolveCombat();check(count(Effect.SANTA_DAMAGE)==1,"Invulnerability produces no cue");
        s.toggleDeveloperTestMode();g.captureCombat(s,list);check(!s.takeDamage(10),"Developer invulnerability blocks damage");g.resolveCombat();check(count(Effect.SANTA_DAMAGE)==1,"Developer invulnerability silent");
        s.toggleDeveloperTestMode();s.restoreForRetry(0,0);check(s.startDash(false,false,false,true),"Dash starts");g.captureCombat(s,list);check(!s.takeDamage(10),"Dash invulnerability blocks damage");g.resolveCombat();check(count(Effect.SANTA_DAMAGE)==1,"Dash invulnerability silent");
        ChristmasEater boss=new ChristmasEater(0,0);list.add(boss);
        g.captureCombat(s,list);boss.takeDamage(450);g.resolveCombat();check(count(Effect.BOSS_DAMAGE)==1&&count(Effect.BOSS_PHASE_TRANSITION)==1,"Actual phase boundary and damage each once");
        g.captureCombat(s,list);boss.takeDamage(1);g.resolveCombat();check(count(Effect.BOSS_PHASE_TRANSITION)==1,"No per-frame/repeated phase cue");
        g.captureCombat(s,list);boss.takeDamage(1000);list.clear();g.resolveCombat();g.resolveCombat();check(count(Effect.BOSS_DEFEAT)==1,"Boss defeat once after removal");
        // Melee strike can execute while missing or damaging an invulnerable Santa.
        list.clear();boss=new ChristmasEater(0,0);list.add(boss);s.restoreForRetry(22,17);s.toggleDeveloperTestMode();boss.tryAttackSanta(s);
        g.captureCombat(s,list);for(int i=0;i<48;i++){boss.advanceCombatTimers();boss.update(s,List.of());}check(!boss.tryAttackSanta(s),"Melee executes but invulnerability denies damage");g.resolveCombat();check(count(Effect.BOSS_ATTACK)==1,"Actual melee strike sounds even without HP loss");
        g.captureCombat(s,list);boss.tryAttackSanta(s);g.resolveCombat();check(count(Effect.BOSS_ATTACK)==1,"Boss cooldown has no attack sound");
        boss=new ChristmasEater(500,500);list.set(0,boss);s.restoreForRetry(1500,1500);
        for(int i=0;i<180;i++){boss.advanceCombatTimers();boss.update(s,List.of());}
        check(boss.getWaveWarningFrames()==66,"Existing wave warning retained");
        g.captureCombat(s,list);for(int i=0;i<65;i++){boss.advanceCombatTimers();boss.update(s,List.of());}g.resolveCombat();check(count(Effect.BOSS_ATTACK)==1,"Warning/cancelled melee do not fake attack");
        g.captureCombat(s,list);boss.advanceCombatTimers();boss.update(s,List.of());g.resolveCombat();check(count(Effect.BOSS_ATTACK)==2,"Actual frost pulse sounds even when Santa is outside radius");
        g.captureCombat(s,list);boss.update(s,List.of());g.resolveCombat();check(count(Effect.BOSS_ATTACK)==2,"Recovery does not repeat pulse");
    }
    private static void successfulInputs()throws Exception {
        GamePanel p=recordedPlaying();heardRequests.clear();Santa s=santa(p);
        attack(p,s.getX()+70,s.getY()+21);check(count(Effect.SANTA_ATTACK)==1,"Successful real mouse attack sounds once");
        attack(p,s.getX()+70,s.getY()+21);check(count(Effect.SANTA_ATTACK)==1,"Cooldown-refused real mouse attack silent");tick(p,24);
        tap(p,KeyEvent.VK_SPACE);check(count(Effect.DASH)==1,"Successful input dash sounds once");tap(p,KeyEvent.VK_SPACE);check(count(Effect.DASH)==1,"Failed dash silent");tick(p,10);
        tap(p,KeyEvent.VK_1);check(count(Effect.CHRISTMAS_BELL)==1,"Successful Bell once");tap(p,KeyEvent.VK_1);check(count(Effect.CHRISTMAS_BELL)==1,"Bell cooldown silent");
        long before=heardRequests.size();render(p);render(p);check(heardRequests.size()==before,"Paint cannot trigger gameplay sounds");
        // Actual melee damage including lethal early return from GamePanel.
        s.restoreForRetry(500,500);Walker e=new Walker(501,506);enemies(p).add(e);
        e.tryAttackSanta(s);for(int i=0;i<30;i++)e.advanceCombatTimers();tick(p,1);
        check(count(Effect.SANTA_DAMAGE)==1,"Production combat HP loss produces damage cue");
        enemies(p).clear();tick(p,24);s.takeDamage(s.getHealth()-5);
        for(int i=0;i<24;i++)s.update(false,false,false,false,List.of());
        Walker lethal=new Walker(s.getX()+1,s.getY()+6);enemies(p).add(lethal);
        lethal.tryAttackSanta(s);for(int i=0;i<30;i++)lethal.advanceCombatTimers();tick(p,1);
        check(p.getState()==GamePanel.State.GAME_OVER&&count(Effect.SANTA_DAMAGE)==2,"Lethal early Game Over return still delivers damage cue once");
        p.stopGame();
    }
    private static void worldRoute()throws Exception {
        GamePanel p=recordedPlaying();heardRequests.clear();Santa s=santa(p);var story=story(p);var toby=p.getChildren().get(0);
        completeOpeningDeliveries(p);heardRequests.clear();at(s,new WorldMap.Point(toby.getX()+15,toby.getY()+21));tap(p,KeyEvent.VK_E);
        check(count(Effect.NPC_INTERACTION)==1,"New Toby dialogue interaction cue");tick(p,20);check(count(Effect.NPC_INTERACTION)==1,"Dialogue frames silent");closeDialogue(p);check(count(Effect.NPC_INTERACTION)==1,"Dialogue page advances silent");
        story.recordVillageCleared();travelGate(p,Area.Type.FOREST);at(s,WorldMap.forArea(Area.Type.FOREST).point("items/Toby Gift"));tap(p,KeyEvent.VK_E);
        check(count(Effect.GIFT_PICKUP)==1&&count(Effect.QUEST_COMPLETE)==0,"Pickup cues without premature completion");tap(p,KeyEvent.VK_E);check(count(Effect.GIFT_PICKUP)==1,"Duplicate pickup silent");
        @SuppressWarnings("unchecked") List<HealingPoint> points=(List<HealingPoint>)field(p,"healingPoints");HealingPoint shelter=points.get(0);at(s,shelter.getPosition());tap(p,KeyEvent.VK_E);
        check(count(Effect.HEALING_SHRINE)==0&&!shelter.isUsed(),"Full health shelter silent/unused");s.takeDamage(60);tap(p,KeyEvent.VK_E);
        check(count(Effect.HEALING_SHRINE)==1&&s.getHealth()==80,"Successful 40 HP healing cues once");tap(p,KeyEvent.VK_E);check(count(Effect.HEALING_SHRINE)==1,"Used shelter silent");
        returnGift(p);check(count(Effect.QUEST_COMPLETE)==1,"Acknowledged return completes quest exactly once");
        travelGate(p,Area.Type.VILLAGE);at(s,new WorldMap.Point(toby.getX()+15,toby.getY()+21));tap(p,KeyEvent.VK_E);closeDialogue(p);check(count(Effect.QUEST_COMPLETE)==1,"Repeat NPC cannot repeat quest reward/cue");travelGate(p,Area.Type.FOREST);
        FinalChapterTest.collectMaterials(p);FinalChapterTest.restoreVillage(p);check(count(Effect.QUEST_COMPLETE)==2,"New restoration quest cues exactly once through existing observer");
        receiveBlackFrostClue(p);travelGate(p,Area.Type.FOREST);
        @SuppressWarnings("unchecked") List<BlackFrostCrystal> crystals=(List<BlackFrostCrystal>)field(p,"crystals");
        for(int i=0;i<3;i++) {
            var c=crystals.get(i);at(s,c.getPosition());tick(p,1);check(count(Effect.CRYSTAL_BREAK)==i,"Guarded crystal has no break sound");
            if(i==0) {
                at(s,shelter.getPosition());tap(p,KeyEvent.VK_E);check(count(Effect.HEALING_SHRINE)==1,"Combat blocks healing sound");
                tick(p,24);s.takeDamage(1000);tick(p,1);tap(p,KeyEvent.VK_R);
                check(count(Effect.QUEST_COMPLETE)==2&&shelter.isUsed(),"Retry retains completed quest and spent shelter without replay");
            }
            clearWave(p);at(s,new WorldMap.Point(c.getPosition().x(),c.getPosition().y()+48));attack(p,c.getPosition().x(),c.getPosition().y());tick(p,23);
            check(count(Effect.CRYSTAL_BREAK)==i+1,"Actual exposed-core destruction sounds once");attack(p,c.getPosition().x(),c.getPosition().y());tick(p,23);check(count(Effect.CRYSTAL_BREAK)==i+1,"Broken core cannot repeat sound");
        }
        check(count(Effect.QUEST_COMPLETE)==3&&!story.isChristmasTownLeadUnlocked(),"Third crystal quest sound preserved, Town waits for Fang");
        at(s,boss.FrostFang.ARENA);tick(p,1);clearWave(p);check(count(Effect.BOSS_DEFEAT)==1&&count(Effect.BOSS_PHASE_TRANSITION)==2,"Fang reuses exact boss audio routing once per event");FinalChapterTest.collectMaterials(p);travelGate(p,Area.Type.VILLAGE);FinalChapterTest.restoreVillage(p);travelGate(p,Area.Type.FOREST);travelGate(p,Area.Type.CHRISTMAS_TOWN);
        Elias elias=(Elias)field(p,"elias");long npc=count(Effect.NPC_INTERACTION);at(s,new WorldMap.Point(elias.getX()+16,elias.getY()+24));tap(p,KeyEvent.VK_E);tick(p,20);closeDialogue(p);check(count(Effect.NPC_INTERACTION)==npc+1,"Elias cue only on new dialogue");
        at(s,encounters(p).get("town").getCenter());tick(p,1);clearWave(p);at(s,(WorldMap.Point)call(p,"spiritTreeInteractionPoint"));tick(p,1);closeOverlay(p);
        var boss=(boss.DarkTitan)field(p,"activeBoss");check(boss!=null,"Actual boss encounter activated");
        // Let a production frost attack execute before clearing the original encounter.
        at(s,new WorldMap.Point(1600,1200));s.toggleDeveloperTestMode();tick(p,246);s.toggleDeveloperTestMode();
        check(count(Effect.BOSS_ATTACK)>0,"Actual boss encounter attacks cue");clearWave(p);
        check(count(Effect.BOSS_DAMAGE)>0&&count(Effect.BOSS_PHASE_TRANSITION)==4&&count(Effect.BOSS_DEFEAT)==2,"Production boss hits, two real phase transitions and defeat cues");tick(p,10);check(count(Effect.BOSS_DEFEAT)==2,"Defeated condition cannot repeat collapse");
        at(s,(WorldMap.Point)call(p,"spiritTreeInteractionPoint"));tap(p,KeyEvent.VK_E);var transform=(SpiritTreeTransformation)field(p,"treeTransformation");
        check(transform.getFrames()==0&&count(Effect.SPIRIT_TREE_RESTORATION)==1&&count(Effect.QUEST_COMPLETE)==4,"Tick zero energy and existing final quest event");
        for(int i=1;i<=180;i++) {
            tick(p,1);check(transform.getFrames()==i,"Same authoritative restoration tick: "+i);
            check(count(Effect.SPIRIT_TREE_RISING)==(i>=30?1:0),"Rising cue exact tick 30: "+i);
            check(count(Effect.SPIRIT_TREE_BURST)==(i>=90?1:0),"Burst cue exact tick 90: "+i);
            check(count(Effect.SPIRIT_TREE_CHIME)==(i>=120?1:0),"Chime cue exact tick 120: "+i);
            check(p.getState()==(i<180?GamePanel.State.PLAYING:GamePanel.State.ENDING),"In-world restoration then celebration retain exact input/state ownership: "+i);
        }
        check(((SpiritTree)field(p,"spiritTree")).getSpirit()==100,"Spirit reward unchanged");tick(p,20);check(count(Effect.SPIRIT_TREE_RESTORATION)==1&&count(Effect.QUEST_COMPLETE)==4,"Restored persistence cannot repeat cues");
        FinalChapterTest.finishChapter(p);tap(p,KeyEvent.VK_E);tap(p,KeyEvent.VK_E);tap(p,KeyEvent.VK_E);check(((EndingCutscene)field(p,"endingCutscene")).getState()==EndingCutscene.State.CREDITS,"Credits remain accessible");
        for(Effect e:Effect.values())if(e.isGameplay())check(exercised.contains(e),"All sixteen categories/four restoration stages exercised: "+e);
        p.stopGame();
    }
    private static void await(BooleanSupplier condition,String message)throws Exception {
        long until=System.nanoTime()+5_000_000_000L;while(!condition.getAsBoolean()&&System.nanoTime()<until)Thread.sleep(5);check(condition.getAsBoolean(),message);
    }
    private static final class ControlledSink implements AudioManager.Sink {
        final Semaphore steps=new Semaphore(0);final AtomicInteger blocks=new AtomicInteger(),peak=new AtomicInteger();final AtomicBoolean closed=new AtomicBoolean();
        @Override public void write(byte[] bytes) {
            int max=0;for(int i=0;i<bytes.length;i+=2)max=Math.max(max,Math.abs((short)((bytes[i]&255)|(bytes[i+1]<<8))));peak.set(max);blocks.incrementAndGet();
            try{steps.acquire();}catch(InterruptedException e){Thread.currentThread().interrupt();}
        }
        @Override public void close(){closed.set(true);steps.release();}
    }
    private static void mixer()throws Exception {
        ControlledSink sink=new ControlledSink();AtomicInteger decodes=new AtomicInteger(),opens=new AtomicInteger();byte[] pcm=new byte[44100*4];
        for(int i=0;i<pcm.length;i+=2){pcm[i]=(byte)0xd0;pcm[i+1]=7;} // deterministic 2000, not shipped audio.
        AudioManager manager=new AudioManager(()->{opens.incrementAndGet();return sink;},path->{decodes.incrementAndGet();return pcm;},null);
        try {
            manager.setMasterVolume(100);manager.setSfxVolume(100);manager.start();await(()->decodes.get()==Phase4PreservationSupport.preloadCount()&&sink.blocks.get()==1,"All unique effects preloaded once outside output/input thread");
            for(Effect e:List.of(Effect.SANTA_ATTACK,Effect.ENEMY_HIT,Effect.DASH,Effect.ENEMY_DEFEAT))manager.playEffect(e);
            sink.steps.release();await(()->sink.blocks.get()==2,"Four bounded low-priority voices mixed");check(sink.peak.get()==4020,"Actual PCM per-cue gains, unchanged channel defaults");
            manager.playEffect(Effect.BOSS_DEFEAT);sink.steps.release();await(()->sink.blocks.get()==3,"Critical effect mixed during four active frequent cues");check(sink.peak.get()==6020,"Critical cue joins the expanded eight-voice pool in actual PCM");
            manager.setMuted(true);manager.playEffect(Effect.SANTA_DAMAGE);sink.steps.release();await(()->sink.blocks.get()==4,"Muted output advances without blocking caller");check(sink.peak.get()==0,"Existing mute silences new effects too");
            manager.setMuted(false);manager.setSfxVolume(50);sink.steps.release();await(()->sink.blocks.get()==5,"Existing SFX volume applies immediately");check(sink.peak.get()==3010,"SFX software gain halves actual PCM");
            BlockingQueue<?> queue=(BlockingQueue<?>)field(manager,"effects");
            for(int i=0;i<1000;i++)manager.playEffect(Effect.ENEMY_HIT);check(queue.size()==16,"Bounded queue despite repeated combat requests");manager.playEffect(Effect.SANTA_DAMAGE);
            boolean criticalQueued=false;for(Object queued:queue)if(field(queued,"effect")==Effect.SANTA_DAMAGE)criticalQueued=true;
            check(queue.size()==16&&criticalQueued,"Critical event admitted by displacing queued frequent effect");
            check(opens.get()==1&&decodes.get()==Phase4PreservationSupport.preloadCount(),"One output/mixer, no repeated effect decoding");
        }finally{manager.close();}await(sink.closed::get,"Audio output resources close");
        var selector=AudioManager.class.getDeclaredMethod("voiceSlot",byte[][].class,Effect[].class,Effect.class);selector.setAccessible(true);
        byte[][] active={pcm,pcm,pcm,pcm};Effect[] critical={Effect.BOSS_DEFEAT,Effect.SANTA_DAMAGE,Effect.SPIRIT_TREE_BURST,Effect.SPIRIT_TREE_CHIME};
        check((int)selector.invoke(null,active,critical,Effect.ENEMY_HIT)==-1,"Frequent cue never steals critical voices");
        ControlledSink missingSink=new ControlledSink();AudioManager missing=new AudioManager(()->missingSink,path->{throw new java.io.IOException("missing test file");},null);
        try{missing.start();await(()->missingSink.blocks.get()==1,"Missing effects do not crash worker");missing.playEffect(Effect.BOSS_DEFEAT);missingSink.steps.release();await(()->missingSink.blocks.get()==2,"Missing critical asset stays silent");check(missingSink.peak.get()==0,"No substitute beep/generated sound");}finally{missing.close();}
    }
    private static void performance()throws Exception {
        GameplayAudio observer=new GameplayAudio(e->{ });Santa s=new Santa(0,0);List<Enemy> actors=List.of(new Walker(0,0),new Walker(80,80));
        for(int i=0;i<20000;i++){observer.captureCombat(s,actors);observer.resolveCombat();}
        long start=System.nanoTime();for(int i=0;i<100000;i++){observer.captureCombat(s,actors);observer.resolveCombat();}
        double us=(System.nanoTime()-start)/100000.0/1000;
        Files.writeString(OUT.resolve("observer-performance.json"),"{\"iterations\":100000,\"actors\":2,\"microseconds_per_capture_resolve\":"+us+",\"interactive_fps_measured\":false}\n");
        System.out.println("Observer sample: "+us+" us/frame (not an interactive FPS measurement)");
    }
    public static void main(String[] args)throws Exception {
        Files.createDirectories(OUT);preserve();assets();isolatedCombat();successfulInputs();worldRoute();mixer();performance();
        System.out.println("PASS: "+checks+" Audio Phase 2 event/preservation/PCM assertions. No human listening test performed.");
    }
}
