package audio;

import java.awt.GraphicsEnvironment;
import java.util.*;
import java.util.concurrent.*;
import java.util.prefs.Preferences;
import javax.sound.sampled.*;
import static audio.AudioCatalog.*;

/** Presentation-only asynchronous PCM mixer. One output line; bounded, reused SFX voices.
 * Decoding/preferences run separately from output, never on Swing or the game loop.
 */
public final class AudioManager implements AutoCloseable {
    public interface Sink extends AutoCloseable {
        void write(byte[] pcm);
        @Override void close();
    }
    @FunctionalInterface public interface SinkFactory {Sink open() throws Exception;}
    @FunctionalInterface public interface Decoder {byte[] decode(String path) throws Exception;}
    private static final int BUFFER_BYTES=1764; // 10 ms blocks.
    private record MusicRequest(Music music,String path,int frames) { }
    private record EffectRequest(Effect effect,AudioCatalog.Cue cue,double gain,int priority,boolean gameplay,long generation,String owner,long ownerVersion,boolean loop) { }
    private static final class SfxVoice {byte[] data;String path,owner;int position,priority,releaseFrames;double gain;boolean gameplay,loop,cinematic;long generation,ownerVersion;}
    public static final long CACHE_LIMIT=128L*1024*1024;
    private static final Set<String> SFX_PATHS=AudioCatalog.cues().stream().filter(c->!c.loop()).map(AudioCatalog.Cue::path).collect(java.util.stream.Collectors.toUnmodifiableSet());
    private final LinkedHashMap<String,Integer> retained=new LinkedHashMap<>(32,.75f,true);
    private final String[] pins=new String[24],handoff=new String[24];private int handoffCount;
    private volatile long retainedBytes,peakRetainedBytes,generation,presentationGeneration;
    private long ownerSequence;
    private volatile boolean gameplaySuspended;
    private final ConcurrentHashMap<String,Long> owners=new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String,Integer> variants=new ConcurrentHashMap<>();
    private final Set<String> emitted=new HashSet<>();
    private boolean updateScoped;
    private volatile java.util.function.Consumer<String> eventObserver;
    private final String[] localPaths=new String[2];
    private volatile int activeVoices;
    public void setEventObserver(java.util.function.Consumer<String> observer){eventObserver=observer;}
    public synchronized void beginUpdate(){emitted.clear();updateScoped=true;}
    public synchronized void cancelGameplay(){generation++;effects.removeIf(EffectRequest::gameplay);owners.clear();}
    public synchronized void cancelPresentation(){presentationGeneration++;effects.removeIf(r->presentation(r.cue()));}
    private static boolean presentation(AudioCatalog.Cue cue){
        String id=cue.id();return !cue.gameplay()&&(id.startsWith("fang.")||id.startsWith("titan.")||id.startsWith("tree.")||id.startsWith("celebration.")||id.startsWith("sleigh.")||id.startsWith("home.")||id.startsWith("credits."));
    }
    private long epoch(AudioCatalog.Cue cue){return presentation(cue)?presentationGeneration:generation;}
    public void setGameplaySuspended(boolean value){if(gameplaySuspended!=value){gameplaySuspended=value;if(value)cancelGameplay();}}
    public boolean isGameplaySuspended(){return gameplaySuspended;}
    public long getRetainedBytes(){return retainedBytes;}
    public long getPeakRetainedBytes(){return peakRetainedBytes;}
    public int getActiveVoices(){return activeVoices;}
    public synchronized void stopOwner(String owner){owners.remove(owner);}
    public void playEvent(String id){playOwned(id,id.equals("titan.foot_contact")?"titan-footfall":id.equals("fang.hurt")||id.equals("titan.hurt")||id.equals("mini_fang.hurt")?id:null,false);}
    public synchronized void playOwned(String id,String owner,boolean loop){
        int sequence=variants.getOrDefault(id,0);AudioCatalog.Cue cue=AudioCatalog.variant(id,sequence);
        if(cue==null)return;
        if(cue.gameplay()&&gameplaySuspended)return;
        if(!emitted.add(id))return;variants.put(id,sequence+1);
        var observer=eventObserver;if(observer!=null)observer.accept(cue.id());
        if(!emitted.add("path:"+cue.path()+(owner==null?"":":"+owner)))return;
        long version=0;if(owner!=null){if(owners.size()>=24&&!owners.containsKey(owner))return;version=++ownerSequence;owners.put(owner,version);}
        enqueue(new EffectRequest(null,cue,cue.gain(),cue.priority(),cue.gameplay(),epoch(cue),owner,version,loop));
    }
    public synchronized void playLocal(int slot,String id){
        if(slot<0||slot>=2)return;AudioCatalog.Cue cue=id==null?null:AudioCatalog.cue(id);String path=cue==null?null:cue.path();
        if(Objects.equals(localPaths[slot],path))return;localPaths[slot]=path;if(path!=null&&running)prepare(path);
    }
    private synchronized boolean pinned(String path){
        if(SFX_PATHS.contains(path))return true; // Authored one-shots remain pinned inside the enforced total budget.
        if(Objects.equals(path,musicRequest.path())||Objects.equals(path,ambiencePath)||Objects.equals(path,localPaths[0])||Objects.equals(path,localPaths[1]))return true;
        for(String pin:pins)if(path.equals(pin))return true;
        for(int i=0;i<handoffCount;i++)if(path.equals(handoff[i]))return true;
        for(var request:effects)if(path.equals(request.cue().path()))return true;
        return false;
    }
    private synchronized byte[] retain(String path,byte[] bytes){
        while(retainedBytes+bytes.length>CACHE_LIMIT){
            String victim=null;for(String key:retained.keySet())if(!pinned(key)){victim=key;break;}
            if(victim==null){diagnostic="Audio cache admission deferred: pinned budget";cache.remove(path);return new byte[0];}
            retainedBytes-=retained.remove(victim);cache.remove(victim);
        }
        retained.put(path,bytes.length);retainedBytes+=bytes.length;peakRetainedBytes=Math.max(peakRetainedBytes,retainedBytes);return bytes;
    }
    private final SinkFactory sinkFactory;
    private final Decoder decoder;
    private final boolean persist,deviceEnabled;
    private volatile Preferences preferences;
    private ScheduledExecutorService loader;
    private ExecutorService output;
    private final Map<String,CompletableFuture<byte[]>> cache=new ConcurrentHashMap<>();
    private final ArrayBlockingQueue<EffectRequest> effects=new ArrayBlockingQueue<>(16);
    private volatile MusicRequest musicRequest=new MusicRequest(null,null,44100);
    private volatile String ambiencePath;
    private volatile Music playingMusic;
    private volatile boolean dialogueActive,combatActive,bossFalling;
    public void setBossFalling(boolean value){bossFalling=value;}
    private volatile boolean running,closed;
    private volatile int master=80,music=60,sfx=70;
    private volatile boolean muted;
    private volatile Sink sink;
    private volatile String diagnostic="Not started";
    private volatile long mixedBuffers,decodedCount;
    private ScheduledFuture<?> saveTask;
    private volatile boolean settingsChanged;
    private int settingsOverrides;

    public AudioManager() {
        sinkFactory=AudioManager::openJavaSound;decoder=AudioResources::decode;
        persist=true;deviceEnabled=!GraphicsEnvironment.isHeadless();
    }
    /** Inject an output/decoder/store for deterministic tests without an audio device. */
    public AudioManager(SinkFactory factory,Decoder decoder,Preferences store) {
        this.sinkFactory=Objects.requireNonNull(factory);this.decoder=Objects.requireNonNull(decoder);
        this.preferences=store;persist=store!=null;deviceEnabled=true;
    }
    private static Thread thread(Runnable task,String name) {Thread t=new Thread(task,name);t.setDaemon(true);return t;}
    public synchronized void start() {
        if(running||closed||loader!=null||!deviceEnabled)return;
        running=true;
        loader=Executors.newSingleThreadScheduledExecutor(r->thread(r,"audio-decode-settings"));
        output=Executors.newSingleThreadExecutor(r->thread(r,"audio-output"));
        loader.execute(this::loadPreferences);
        if(settingsChanged)save();
        if(musicRequest.path()!=null)prepare(musicRequest.path());
        if(ambiencePath!=null)prepare(ambiencePath);
        for(AudioCatalog.Cue cue:AudioCatalog.cues())if(!cue.loop())prepare(cue.path());
        output.execute(this::mix);
    }
    private synchronized CompletableFuture<byte[]> prepare(String path) {
        if(loader==null||closed)return null;
        return cache.computeIfAbsent(path,key->CompletableFuture.supplyAsync(()-> {
            try {
                if(closed)return new byte[0];
                byte[] bytes=decoder.decode(key);
                if(bytes==null||bytes.length%4!=0)throw new IllegalArgumentException("Invalid PCM frames");
                decodedCount++;return retain(key,bytes);
            }
            catch(Exception error) {diagnostic="Cannot load "+key+": "+error.getMessage();System.err.println(diagnostic);return new byte[0];}
        },loader));
    }
    public synchronized void playMusic(Music track) {
        if(closed)return;
        MusicRequest previous=musicRequest;
        if(previous.music()==track)return;
        String path=track==null?null:track.path();
        musicRequest=new MusicRequest(track,path,AudioPolish.transitionMillis(previous.music(),track)*44100/1000);
        if(Objects.equals(path,previous.path()))return; // ENDING and VILLAGE share the same actual resource.
        if(path!=null&&running)prepare(path);
    }
    public void stopMusic() {playMusic(null);}
    public synchronized void playAmbience(Ambience ambience) {setAmbiencePath(ambience==null?null:ambience.path());}
    /** Package scope: used by deterministic lifecycle tests. Production paths come only from the catalog. */
    synchronized void setAmbiencePath(String path) {
        if(closed||Objects.equals(ambiencePath,path))return;
        ambiencePath=path;if(path!=null&&running)prepare(path);
    }
    public void setDialogueActive(boolean value){if(!closed)dialogueActive=value;}
    public void setCombatActive(boolean value){if(!closed)combatActive=value;}
    public void playEffect(Effect effect) {
        playEffect(effect,1);
    }
    /** Event-local attenuation. UI and critical boss/story cues retain their established gain. */
    public synchronized void playEffect(Effect effect,double spatialGain) {
        if(effect==null)return;
        String id=effect.eventId();
        if(id.contains(".v"))id=id.substring(0,id.lastIndexOf(".v"));
        int sequence=variants.getOrDefault(id,0);var cue=AudioCatalog.variant(id,sequence);
        if(updateScoped&&!emitted.add("path:"+cue.path()))return;
        variants.put(id,sequence+1);
        var observer=eventObserver;if(observer!=null)observer.accept(cue.id());
        boolean spatial=effect==Effect.ENEMY_HIT||effect==Effect.ENEMY_DEFEAT||effect==Effect.CRYSTAL_BREAK;
        double gain=spatial&&Double.isFinite(spatialGain)?Math.max(.20,Math.min(1,spatialGain)):1;
        // Only the selected sword uses its corrected route gain/admission; other effect policies stay exact.
        enqueue(new EffectRequest(effect,cue,(effect==Effect.SANTA_ATTACK?cue.gain():effect.gain())*gain,effect==Effect.SANTA_ATTACK?cue.priority():effect.priority(),effect.isGameplay()&&cue.gameplay(),epoch(cue),null,0,false));
    }
    private void enqueue(EffectRequest request){
        if(!running||closed||muted||master<=0||sfx<=0||(request.gameplay()&&gameplaySuspended))return;
        prepare(request.cue().path());
        synchronized(effects){
            if(effects.offer(request))return;
            EffectRequest lowest=null;for(var queued:effects)if(lowest==null||queued.priority()<lowest.priority())lowest=queued;
            if(lowest!=null&&lowest.priority()<request.priority())effects.remove(lowest);
            effects.offer(request);
        }
    }
    public int getMasterVolume(){return master;}
    public int getMusicVolume(){return music;}
    public int getSfxVolume(){return sfx;}
    public boolean isMuted(){return muted;}
    public synchronized void setMasterVolume(int percent){master=clamp(percent);settingsOverrides|=1;save();}
    public synchronized void setMusicVolume(int percent){music=clamp(percent);settingsOverrides|=2;save();}
    public synchronized void setSfxVolume(int percent){sfx=clamp(percent);settingsOverrides|=4;save();}
    public synchronized void setMuted(boolean value){muted=value;settingsOverrides|=8;save();}
    private static int clamp(int value){return Math.max(0,Math.min(100,value));}
    public Music getRequestedMusic(){return musicRequest.music();}
    public Music getPlayingMusic(){return playingMusic;}
    public String getDiagnostic(){return diagnostic;}
    public long getMixedBuffers(){return mixedBuffers;}
    public long getDecodedCount(){return decodedCount;}
    private void loadPreferences() {
        if(!persist)return;
        try {
            if(preferences==null)preferences=Preferences.userRoot().node("SantaLastChristmas/audio-phase1");
            int m=clamp(preferences.getInt("master",80)),bg=clamp(preferences.getInt("music",60)),fx=clamp(preferences.getInt("sfx",70));
            boolean silent=preferences.getBoolean("muted",false);
            synchronized(this) {
                // An early edit overrides only that field, preserving the other saved preferences.
                if((settingsOverrides&1)==0)master=m;
                if((settingsOverrides&2)==0)music=bg;
                if((settingsOverrides&4)==0)sfx=fx;
                if((settingsOverrides&8)==0)muted=silent;
            }
        } catch(RuntimeException error){diagnostic="Audio preferences unavailable: "+error.getMessage();}
    }
    private synchronized void save() {
        settingsChanged=true;
        if(!persist||loader==null||closed)return;
        if(saveTask!=null)saveTask.cancel(false);
        saveTask=loader.schedule(this::storePreferences,200,TimeUnit.MILLISECONDS);
    }
    private void storePreferences() {
        if(preferences==null)return;
        try {
            preferences.putInt("master",master);preferences.putInt("music",music);preferences.putInt("sfx",sfx);
            preferences.putBoolean("muted",muted);preferences.flush();
        } catch(Exception error){diagnostic="Audio preferences not saved: "+error.getMessage();}
    }
    private synchronized byte[] ready(String path) {
        CompletableFuture<byte[]> future=cache.get(path);
        if(future==null||!future.isDone())return null;
        byte[] bytes=future.getNow(null);if(bytes!=null&&bytes.length>0){retained.get(path);if(handoffCount<handoff.length)handoff[handoffCount++]=path;}
        return bytes;
    }
    private static int sample(byte[] data,int offset) {return (short)((data[offset]&255)|(data[offset+1]<<8));}
    private void mix() {
        byte[] block=new byte[BUFFER_BYTES];SfxVoice[] voices=new SfxVoice[8];for(int i=0;i<8;i++)voices[i]=new SfxVoice();
        int[] variations=new int[Effect.values().length];
        LoopLayer musicLayer=new LoopLayer(),ambientLayer=new LoopLayer();LoopLayer[] locals={new LoopLayer(),new LoopLayer()};
        AudioPolish.Dynamics dynamics=new AudioPolish.Dynamics();String lastRequest=null;double fallGain=1;
        try {
            Sink opened=sinkFactory.open();
            synchronized(this){if(closed){opened.close();return;}sink=opened;}
            diagnostic="Java Sound output active";
            while(running&&!Thread.currentThread().isInterrupted()){
                MusicRequest request=musicRequest;String ambient=ambiencePath;
                byte[] musicData=request.path()==null?null:ready(request.path());
                if(!Objects.equals(lastRequest,request.path())&&request.frames()<=6615)musicLayer.request(null,null,request.frames());
                lastRequest=request.path();musicLayer.request(request.path(),musicData,request.frames());
                ambientLayer.request(ambient,ambient==null?null:ready(ambient),22050);
                int localCount=0;
                for(int i=0;i<2;i++){String path=localPaths[i];locals[i].request(path,path==null?null:ready(path),4410);if(path!=null||locals[i].isActive())localCount++;}
                int voiceLimit=8-localCount;
                playingMusic=musicLayer.has(request.path())?request.music():null;
                for(int i=0;i<voices.length;i++){
                    var v=voices[i];if(v.data!=null&&(i>=voiceLimit||(v.gameplay&&(gameplaySuspended||v.generation!=generation))||(v.cinematic&&v.generation!=presentationGeneration)||(v.owner!=null&&!Objects.equals(owners.get(v.owner),v.ownerVersion)))){
                        if(v.owner!=null&&v.gameplay&&i<voiceLimit){if(v.releaseFrames==0)v.releaseFrames=882;}else v.data=null;
                    }
                }
                for(int pending=effects.size();pending>0;pending--){
                    EffectRequest cue=effects.poll();if(cue==null)break;
                    if(cue.gameplay()&&(gameplaySuspended||cue.generation()!=generation))continue;
                    if(presentation(cue.cue())&&cue.generation()!=presentationGeneration)continue;
                    if(cue.owner()!=null&&!Objects.equals(owners.get(cue.owner()),cue.ownerVersion()))continue;
                    byte[] data=ready(cue.cue().path());if(data==null){effects.offer(cue);continue;}if(data.length==0)continue;
                    int slot=-1;
                    for(int i=0;i<voiceLimit;i++){var v=voices[i];if(v.data==null){slot=i;break;}if(v.priority<cue.priority()&&(slot<0||v.priority<voices[slot].priority))slot=i;}
                    if(slot<0)continue;var v=voices[slot];v.data=data;v.position=0;v.releaseFrames=0;v.path=cue.cue().path();v.priority=cue.priority();v.gain=cue.gain();v.gameplay=cue.gameplay();v.cinematic=presentation(cue.cue());v.owner=cue.owner();v.ownerVersion=cue.ownerVersion();v.generation=cue.generation();v.loop=cue.loop();
                    if(cue.cue().id().endsWith(".warning"))dynamics.duck(550);
                    if(cue.cue().id().equals("fang.fall")||cue.cue().id().equals("titan.collapse"))dynamics.duck(900);
                    if(cue.effect()!=null){v.gain*=AudioPolish.variationGain(cue.effect(),variations[cue.effect().ordinal()]++);dynamics.duck(AudioPolish.duckMillis(cue.effect()));}
                }
                synchronized(this){
                    Arrays.fill(pins,null);int n=musicLayer.copyPaths(pins,0);n=ambientLayer.copyPaths(pins,n);for(var layer:locals)n=layer.copyPaths(pins,n);
                    for(var v:voices)if(v.data!=null&&n<pins.length)pins[n++]=v.path;
                    Arrays.fill(handoff,null);handoffCount=0;
                }
                double mg=muted?0:master*.0001*music,sg=muted?0:master*.0001*sfx;
                boolean dialogue=dialogueActive,fighting=combatActive;
                for(int offset=0;offset<block.length;offset+=4){
                    musicLayer.advance();ambientLayer.advance();for(var layer:locals)layer.advance();
                    double fallTarget=bossFalling?.28:1;
                    fallGain+=Math.max(-1.0/15435,Math.min(1.0/22050,fallTarget-fallGain));
                    double musicGain=mg*dynamics.advance(dialogue,fighting)*fallGain;
                    double left=musicLayer.left()*musicGain+ambientLayer.left()*sg*.45,right=musicLayer.right()*musicGain+ambientLayer.right()*sg*.45;
                    for(var layer:locals){left+=layer.left()*sg*.3;right+=layer.right()*sg*.3;}
                    for(var v:voices)if(v.data!=null){double release=v.releaseFrames>0?v.releaseFrames/882.0:1;left+=sample(v.data,v.position)*sg*v.gain*release;right+=sample(v.data,v.position+2)*sg*v.gain*release;v.position+=4;if(v.position>=v.data.length){if(v.loop)v.position=0;else v.data=null;}if(v.releaseFrames>0&&--v.releaseFrames==0)v.data=null;}
                    double limit=Math.max(1,Math.max(Math.abs(left),Math.abs(right))/32767);int l=(int)Math.round(left/limit),r=(int)Math.round(right/limit);
                    block[offset]=(byte)l;block[offset+1]=(byte)(l>>8);block[offset+2]=(byte)r;block[offset+3]=(byte)(r>>8);
                }
                int count=localCount;for(var v:voices)if(v.data!=null)count++;activeVoices=count;
                opened.write(block);mixedBuffers++;
            }
        }catch(Exception error){diagnostic="Audio output unavailable: "+error.getMessage();System.err.println(diagnostic);}
        finally{running=false;musicLayer.clear();ambientLayer.clear();for(var layer:locals)layer.clear();effects.clear();activeVoices=0;closeOutput();playingMusic=null;}
    }
    private void closeOutput() {
        Sink old;
        synchronized(this){old=sink;sink=null;}
        if(old!=null)try{old.close();}catch(RuntimeException error){diagnostic="Audio close failed: "+error.getMessage();}
    }
    /** Never steal a critical voice for a lower/equal-priority request. No allocation. */
    private static int voiceSlot(byte[][] voices,Effect[] active,Effect incoming) {
        int lowest=-1;
        for(int i=0;i<voices.length;i++) {
            if(voices[i]==null)return i;
            if(active[i].priority()<incoming.priority()&&(lowest<0||active[i].priority()<active[lowest].priority()))lowest=i;
        }
        return lowest;
    }
    private static Sink openJavaSound()throws LineUnavailableException {
        SourceDataLine line=AudioSystem.getSourceDataLine(AudioResources.FORMAT);
        try {line.open(AudioResources.FORMAT,BUFFER_BYTES*4);line.start();}
        catch(LineUnavailableException|RuntimeException error){line.close();throw error;}
        return new Sink() {
            @Override public void write(byte[] bytes) {
                for(int offset=0;offset<bytes.length;) {
                    int written=line.write(bytes,offset,bytes.length-offset);
                    if(written<=0)break;offset+=written;
                }
            }
            @Override public void close(){line.stop();line.flush();line.close();}
        };
    }
    @Override public synchronized void close() {
        if(closed)return;closed=true;running=false;
        synchronized(effects){effects.clear();}
        musicRequest=new MusicRequest(null,null,44100);ambiencePath=null;Arrays.fill(localPaths,null);generation++;owners.clear();dialogueActive=combatActive=false;playingMusic=null;
        if(output!=null)output.shutdownNow();
        if(loader!=null) {
            if(saveTask!=null)saveTask.cancel(false);
            loader.execute(()->{storePreferences();closeOutput();cache.clear();synchronized(this){retained.clear();retainedBytes=0;Arrays.fill(pins,null);Arrays.fill(handoff,null);handoffCount=0;}});
            loader.shutdown();
        }
    }
}
