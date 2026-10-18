package audio;

import static audio.AudioCatalog.*;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.prefs.Preferences;
import java.util.concurrent.ExecutorService;
import java.util.Map;

/** Silent deterministic output sink: one permit advances exactly 441 stereo sample frames. */
public final class AudioPhase3MixerSupport {
    private static int checks;
    private AudioPhase3MixerSupport() { }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void near(double actual,double expected,String message){check(Math.abs(actual-expected)<.001,message+": "+actual+" vs "+expected);}
    private static void await(BooleanSupplier condition)throws Exception {
        long until=System.nanoTime()+5_000_000_000L;
        while(!condition.getAsBoolean()&&System.nanoTime()<until)Thread.sleep(1);
        check(condition.getAsBoolean(),"Audio worker completed bounded test step");
    }
    private static byte[] pcm(int left,int right,int frames) {
        byte[] data=new byte[frames*4];
        for(int i=0;i<data.length;i+=4){data[i]=(byte)left;data[i+1]=(byte)(left>>8);data[i+2]=(byte)right;data[i+3]=(byte)(right>>8);}
        return data;
    }
    private static Object field(Object object,String name)throws Exception {
        var member=object.getClass().getDeclaredField(name);member.setAccessible(true);return member.get(object);
    }
    private static void cleaned(AudioManager manager)throws Exception {
        ExecutorService loader=(ExecutorService)field(manager,"loader"),output=(ExecutorService)field(manager,"output");
        Map<?,?> cache=(Map<?,?>)field(manager,"cache");
        await(()->loader.isTerminated()&&output.isTerminated()&&cache.isEmpty());
        check(((java.util.Queue<?>)field(manager,"effects")).isEmpty(),"Shutdown clears cached PCM, queued events and both workers");
    }
    private static void advance(LoopLayer layer,int frames){for(int i=0;i<frames;i++)layer.advance();}
    private static void envelopes() {
        LoopLayer layer=new LoopLayer();byte[] a=pcm(10000,0,31),b=pcm(0,20000,47);
        layer.request("a",a,44100);advance(layer,22050);near(layer.left(),5000,"Music fades in over one second");
        advance(layer,22050);near(layer.left(),10000,"Fade completes exactly on its sample deadline");
        layer.request("b",b,55125);advance(layer,27562);
        check(layer.left()>4900&&layer.left()<5100&&layer.right()>9900&&layer.right()<10100,"Both actual PCM channels audible halfway through area crossfade");
        advance(layer,27563);near(layer.left(),0,"Previous track fully faded");near(layer.right(),20000,"New track fully faded in");
        check(!layer.has("a")&&layer.has("b"),"Completed crossfade releases previous PCM reference");
        layer.request(null,null,44100);advance(layer,44100);near(layer.right(),0,"Stopping loop fades to silence");check(!layer.has("b"),"Stopped loop released");

        byte[] sequence=pcm(1000,1000,3);sequence[4]=(byte)2000;sequence[5]=(byte)(2000>>8);sequence[8]=(byte)3000;sequence[9]=(byte)(3000>>8);
        layer.request("same",sequence,1);layer.advance();near(layer.left(),1000,"First loop frame");
        for(int i=0;i<1000;i++)layer.request("same",sequence,1);
        layer.advance();near(layer.left(),2000,"Same-track requests preserve playback cursor");
        layer.advance();near(layer.left(),3000,"Same-track requests preserve envelope");layer.advance();near(layer.left(),1000,"PCM loops on frame boundaries");

        layer.clear();a=pcm(1000,1000,7);b=pcm(2000,2000,9);byte[] c=pcm(3000,3000,11),d=pcm(4000,4000,13);
        layer.request("a",a,1);layer.advance();layer.request("b",b,100);advance(layer,25);
        double before=layer.left();layer.request("c",c,100);layer.advance();check(Math.abs(layer.left()-before)<41,"Third rapid target never abruptly evicts a track");
        advance(layer,25);layer.request("d",d,100);
        for(int i=0;i<250;i++){layer.request("d",d,100);layer.advance();check(layer.left()<=4000.001,"Rapid transition mix never exceeds full-track gain");}
        check(layer.has("d")&&!layer.has("a")&&!layer.has("b")&&!layer.has("c"),"Latest rapid request wins and stale destinations never start");near(layer.left(),4000,"Latest crossfade completes");
        layer.request("missing",null,100);advance(layer,100);near(layer.left(),4000,"Pending decoding keeps current track stable");
        layer.request("missing",new byte[0],100);advance(layer,100);near(layer.left(),0,"Failed decode fades old track out without substitution");
        layer.clear();layer.request("a",a,1);layer.advance();layer.request("b",b,100);advance(layer,50);
        before=layer.left();layer.request("a",a,100);layer.advance();check(Math.abs(layer.left()-before)<21,"Reversal during crossfade remains continuous");
        advance(layer,99);near(layer.left(),1000,"Interrupted transition reverses to existing track");

        AudioPolish.Dynamics dynamics=new AudioPolish.Dynamics();dynamics.duck(1000);double gain=1;
        for(int i=0;i<4410;i++)gain=dynamics.advance(false,false);near(gain,.80,"Ducking ramps to 20% reduction in 100 ms");
        dynamics.duck(2000);dynamics.duck(1000);
        for(int i=0;i<88200;i++)gain=dynamics.advance(false,false);near(gain,.80,"Overlapping cues extend hold without stacking");
        for(int i=0;i<35280;i++)gain=dynamics.advance(false,false);near(gain,1,"Ducking automatically recovers in 800 ms");
        for(int i=0;i<22050;i++)gain=dynamics.advance(true,true);near(gain,.80,"Dialogue and combat use strongest reduction only");
        for(int i=0;i<35280;i++)gain=dynamics.advance(false,true);near(gain,.94,"Dialogue interruption restores the combat mix");
        for(int i=0;i<88200+22050;i++)gain=dynamics.advance(false,false);near(gain,1,"Combat release holds two seconds then recovers smoothly");
    }
    private static final class StepSink implements AudioManager.Sink {
        final Semaphore permits=new Semaphore(0);
        final AtomicInteger blocks=new AtomicInteger(),closes=new AtomicInteger();
        volatile int lastLeft,lastRight,peak;
        @Override public void write(byte[] bytes) {
            int n=bytes.length;
            lastLeft=(short)((bytes[n-4]&255)|(bytes[n-3]<<8));lastRight=(short)((bytes[n-2]&255)|(bytes[n-1]<<8));
            int max=0;for(int i=0;i<n;i+=2)max=Math.max(max,Math.abs((short)((bytes[i]&255)|(bytes[i+1]<<8))));peak=max;
            blocks.incrementAndGet();try{permits.acquire();}catch(InterruptedException e){Thread.currentThread().interrupt();}
        }
        @Override public void close(){closes.incrementAndGet();permits.release();}
        void step()throws Exception{int previous=blocks.get();permits.release();await(()->blocks.get()==previous+1);}
        void steps(int count)throws Exception{for(int i=0;i<count;i++)step();}
    }
    private static long sfxCount(){return AudioCatalog.cues().stream().filter(c->!c.loop()).map(AudioCatalog.Cue::path).distinct().count();}
    private static void manager()throws Exception {
        StepSink sink=new StepSink();AtomicInteger opens=new AtomicInteger();
        byte[] music=pcm(10000,10000,4410),silent=pcm(0,0,4410);
        AudioManager manager=new AudioManager(()->{opens.incrementAndGet();return sink;},path->path.contains("/music/")?music:silent,null);
        try {
            manager.setMasterVolume(100);manager.setMusicVolume(100);manager.setSfxVolume(100);manager.playMusic(Music.MENU);manager.start();
            await(()->sink.blocks.get()==1&&manager.getDecodedCount()==sfxCount()+1);sink.steps(101);check(sink.lastLeft==10000,"Production mixer completes music fade");
            manager.setDialogueActive(true);sink.steps(11);check(sink.lastLeft==8000,"Production mixer ducks during dialogue");
            manager.setDialogueActive(false);sink.steps(81);check(sink.lastLeft==10000,"Interrupted dialogue recovers automatically");
            manager.playEffect(Effect.BOSS_PHASE_TRANSITION);sink.steps(11);check(sink.lastLeft==8000,"Actual important event ducks music");
            manager.playEffect(Effect.BOSS_DEFEAT);sink.steps(201);check(sink.lastLeft>=8000&&sink.lastLeft<8050,"New important event extends hold");
            sink.steps(81);check(sink.lastLeft==10000,"Event duck recovery completes");
            manager.playEffect(Effect.SANTA_ATTACK);sink.step();check(sink.lastLeft==10000,"Ordinary attacks never duck music");
            manager.setCombatActive(true);sink.steps(51);check(sink.lastLeft==9400,"Combat subtly mixes existing music");
            manager.setCombatActive(false);sink.steps(100);check(sink.lastLeft==9400,"Combat hysteresis resists rapid switching");
            sink.steps(151);check(sink.lastLeft==10000,"Combat music recovers without track restart");
            long decoded=manager.getDecodedCount();for(int i=0;i<1000;i++)manager.playMusic(Music.MENU);sink.step();check(manager.getDecodedCount()==decoded,"Same-track requests reuse decoded data");
            manager.playMusic(Music.CREDITS);await(()->manager.getDecodedCount()==sfxCount()+2);sink.steps(101);
            manager.playMusic(Music.ENDING);sink.step();check(manager.getPlayingMusic()==Music.ENDING&&sink.lastLeft==10000&&manager.getDecodedCount()==sfxCount()+2,"Shared Credits/Ending path changes identity without restarting or fading");
            manager.setMasterVolume(50);manager.setMusicVolume(50);manager.setDialogueActive(true);sink.steps(11);check(sink.lastLeft==2000,"Ducking multiplies selected volumes without changing them");
            check(manager.getMasterVolume()==50&&manager.getMusicVolume()==50&&manager.getSfxVolume()==100,"User preferences survive temporary mixing");
            manager.setMuted(true);sink.steps(2);check(sink.peak==0,"Mute silences music and all temporary envelopes");
            manager.setMuted(false);sink.step();check(sink.lastLeft==2000,"Unmute restores selected ducked gain");
            manager.setDialogueActive(false);sink.steps(81);check(sink.lastLeft==2500,"Recovery preserves user-selected volumes");
            check(opens.get()==1,"All phases use one output device");
        }finally{manager.close();}
        await(()->sink.closes.get()==1);cleaned(manager);int blocks=sink.blocks.get();manager.playMusic(Music.BOSS);manager.playEffect(Effect.BOSS_DEFEAT);manager.start();
        check(sink.blocks.get()==blocks&&manager.getRequestedMusic()==null&&manager.getPlayingMusic()==null,"Closed manager cannot revive stale music or output");
    }
    private static void rapidAndPending()throws Exception {
        StepSink sink=new StepSink();Semaphore decodeEntered=new Semaphore(0),decodeFinish=new Semaphore(0),edtReturned=new Semaphore(0);
        AtomicInteger opens=new AtomicInteger();
        AudioManager manager=new AudioManager(()->{opens.incrementAndGet();return sink;},path->{
            if(path.equals(Music.FOREST.path())){decodeEntered.release();decodeFinish.acquire();return pcm(0,20000,31);}
            if(path.equals(Music.MENU.path()))return pcm(10000,0,37);
            if(path.equals(Music.BOSS.path()))return pcm(3000,3000,41);
            if(path.equals(Music.ENDING.path()))return pcm(4000,4000,43);
            return pcm(0,0,47);
        },null);
        try {
            manager.setMasterVolume(100);manager.setMusicVolume(100);manager.playMusic(Music.MENU);manager.start();await(()->sink.blocks.get()==1&&manager.getDecodedCount()==sfxCount()+1);sink.steps(101);
            javax.swing.SwingUtilities.invokeLater(()->{manager.playMusic(Music.FOREST);edtReturned.release();});
            await(()->decodeEntered.availablePermits()==1&&edtReturned.availablePermits()==1);
            sink.steps(30);check(sink.lastLeft==10000&&sink.lastRight==0,"Blocked decoder never blocks EDT or cuts current music");
            decodeFinish.release();await(()->manager.getDecodedCount()==sfxCount()+2);sink.steps(50);
            check(sink.lastLeft==5000&&sink.lastRight==10000,"Production stereo PCM contains both tracks during crossfade");
            manager.playMusic(Music.BOSS);manager.playMusic(Music.TOWN);manager.playMusic(Music.ENDING);await(()->manager.getDecodedCount()==sfxCount()+5);
            for(int i=0;i<360;i++){sink.step();check(manager.getPlayingMusic()!=Music.BOSS&&manager.getPlayingMusic()!=Music.TOWN,"Superseded rapid music destinations never start");}
            check(sink.lastLeft==4000&&sink.lastRight==4000&&manager.getPlayingMusic()==Music.ENDING,"Latest rapid music transition completes with newest track");
            long decodes=manager.getDecodedCount();manager.playMusic(Music.FOREST);sink.steps(76);manager.playMusic(Music.ENDING);sink.steps(176);
            check(manager.getPlayingMusic()==Music.ENDING&&sink.lastLeft==4000&&manager.getDecodedCount()==decodes,"Rapid reversal reuses cached resources");
            check(opens.get()==1,"Rapid music never opens another output device");
        }finally{decodeFinish.release();manager.close();}await(()->sink.closes.get()==1);cleaned(manager);

        StepSink stoppedSink=new StepSink();Semaphore entered=new Semaphore(0),finish=new Semaphore(0);
        AudioManager stopped=new AudioManager(()->stoppedSink,path->{entered.release();finish.acquire();return pcm(1000,1000,7);},null);
        try {
            stopped.playMusic(Music.BOSS);stopped.start();await(()->stoppedSink.blocks.get()==1&&entered.availablePermits()==1);
            stopped.close();finish.release();await(()->stoppedSink.closes.get()==1);cleaned(stopped);
            check(stopped.getPlayingMusic()==null&&stopped.getRequestedMusic()==null,"Decode completion after shutdown cannot start stale transition");
        }finally{finish.release();stopped.close();}
    }
    private static void ambienceAndEffects()throws Exception {
        for(Ambience area:Ambience.values())check(area.path()!=null&&area.path().startsWith("assets/audio/phase10_6/ambience/"),"All six approved area beds registered: "+area);
        StepSink sink=new StepSink();byte[] signal=pcm(10000,10000,441);
        AudioManager manager=new AudioManager(()->sink,path->signal,null);
        try {
            manager.setMasterVolume(100);manager.setSfxVolume(100);manager.start();await(()->sink.blocks.get()==1&&manager.getDecodedCount()==sfxCount());
            manager.setAmbiencePath("test/ambient-a.wav");await(()->manager.getDecodedCount()==sfxCount()+1);sink.steps(101);check(sink.lastLeft==4500,"Ambient loop fades to 45% of pre-calibrated ambience PCM on the SFX bus");
            for(int i=0;i<1000;i++)manager.setAmbiencePath("test/ambient-a.wav");sink.steps(3);check(sink.lastLeft==4500&&manager.getDecodedCount()==sfxCount()+1,"Ambient loop reuses resource without restarts or redecoding");
            manager.setAmbiencePath("test/ambient-b.wav");await(()->manager.getDecodedCount()==sfxCount()+2);sink.steps(50);check(sink.lastLeft==4500,"Ambient area crossfade keeps summed gain bounded");
            sink.steps(51);manager.setSfxVolume(50);sink.step();check(sink.lastLeft==2250,"Ambience respects existing SFX control");
            manager.setMuted(true);sink.step();check(sink.peak==0,"Mute silences ambience");manager.setMuted(false);sink.step();check(sink.lastLeft==2250,"Unmute restores ambience settings");
            manager.playAmbience(null);sink.steps(101);check(sink.lastLeft==0,"Leaving area/menu stops ambient loop with fade");manager.setSfxVolume(100);
            int[] expected={9500,8930,9310,8645,9500}; // Selected sword route .95; original variation factors exact.
            for(int value:expected){manager.playEffect(Effect.SANTA_ATTACK);sink.step();check(sink.peak==value,"Actual PCM attack variation retains sample meaning and bounded gains");sink.step();}
            manager.playEffect(Effect.ENEMY_HIT,.20);sink.step();check(sink.peak==540,"Far world cue attenuated in actual PCM");sink.step();
            manager.playEffect(Effect.CLICK,.20);sink.step();check(sink.peak==10000,"UI cue ignores distance");sink.step();
            manager.playEffect(Effect.BOSS_DEFEAT,.20);sink.step();check(sink.peak==10000,"Boss-critical cue ignores distance");sink.step();
            manager.setAmbiencePath("test/ambient-a.wav");sink.steps(101);manager.setMasterVolume(0);sink.step();check(sink.peak==0,"Zero master silences ambience");
            manager.setMasterVolume(100);manager.setSfxVolume(0);sink.step();check(sink.peak==0,"Zero SFX silences ambience");
        }finally{manager.close();}await(()->sink.closes.get()==1);cleaned(manager);

        StepSink absent=new StepSink();AudioManager missing=new AudioManager(()->absent,path->{throw new IOException("absent fixture");},null);
        try {
            missing.playMusic(Music.MENU);missing.setAmbiencePath("test/missing.wav");missing.start();await(()->absent.blocks.get()==1);
            absent.steps(10);check(absent.peak==0&&missing.getPlayingMusic()==null,"Missing music/ambient/effects gracefully produce silence");
        }finally{missing.close();}await(()->absent.closes.get()==1);cleaned(missing);
        AudioManager noDevice=new AudioManager(()->{throw new javax.sound.sampled.LineUnavailableException("fixture has no device");},path->signal,null);
        try{noDevice.start();await(()->noDevice.getDiagnostic().contains("unavailable"));noDevice.playMusic(Music.BOSS);noDevice.playEffect(Effect.BOSS_DEFEAT);check(noDevice.getMixedBuffers()==0,"Unavailable output does not block caller or mix invalid device");}finally{noDevice.close();}cleaned(noDevice);

        StepSink limited=new StepSink();AudioManager limiter=new AudioManager(()->limited,path->pcm(30000,15000,4410),null);
        try {
            limiter.setMasterVolume(100);limiter.setMusicVolume(100);limiter.setSfxVolume(100);limiter.playMusic(Music.MENU);limiter.start();await(()->limited.blocks.get()==1&&limiter.getDecodedCount()==sfxCount()+1);limited.steps(101);
            for(Effect effect:new Effect[]{Effect.CLICK,Effect.BACK,Effect.SETTINGS,Effect.CONFIRM})limiter.playEffect(effect);limited.step();
            check(limited.peak==32767&&limited.lastLeft==32767&&Math.abs(limited.lastRight-16384)<=1,"Linked stereo limiter prevents overload and preserves balance");
        }finally{limiter.close();}await(()->limited.closes.get()==1);cleaned(limiter);
    }
    private static void savedSettings()throws Exception {
        Preferences store=Preferences.userRoot().node("SantaLastChristmas/audio-phase3-test-"+java.util.UUID.randomUUID());
        StepSink sink=new StepSink();AudioManager manager=new AudioManager(()->sink,path->pcm(1000,1000,441),store);
        try {
            manager.setMasterVolume(37);manager.setMusicVolume(42);manager.setSfxVolume(53);manager.start();await(()->sink.blocks.get()==1);
            manager.playMusic(Music.BOSS);manager.setDialogueActive(true);manager.setCombatActive(true);manager.playEffect(Effect.BOSS_DEFEAT);
            await(()->store.getInt("sfx",-1)==53);
            check(store.getInt("master",-1)==37&&store.getInt("music",-1)==42,"Temporary mix never writes reduced values to saved settings");
            manager.setMuted(true);await(()->store.getBoolean("muted",false));
        }finally{manager.close();}await(()->sink.closes.get()==1);cleaned(manager);
        StepSink restoredSink=new StepSink();AudioManager restored=new AudioManager(()->restoredSink,path->pcm(1000,1000,441),store);
        try{restored.start();await(()->restored.getMasterVolume()==37&&restored.getMusicVolume()==42&&restored.getSfxVolume()==53&&restored.isMuted());}
        finally{restored.close();}await(()->restoredSink.closes.get()==1);cleaned(restored);
        StepSink editedSink=new StepSink();AudioManager edited=new AudioManager(()->editedSink,path->pcm(1000,1000,441),store);
        try {
            edited.setMusicVolume(47);edited.start();
            await(()->edited.getMasterVolume()==37&&edited.getMusicVolume()==47&&edited.getSfxVolume()==53&&edited.isMuted());
            await(()->store.getInt("music",-1)==47);
            check(store.getInt("master",-1)==37&&store.getInt("sfx",-1)==53&&store.getBoolean("muted",false),"Early single-control edit never resets other saved preferences");
        }finally{edited.close();}await(()->editedSink.closes.get()==1);cleaned(edited);store.removeNode();
    }
    public static int verify()throws Exception{envelopes();manager();rapidAndPending();ambienceAndEffects();savedSettings();return checks;}
}
