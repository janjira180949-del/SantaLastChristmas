import audio.*;
import game.*;
import entity.*;
import boss.*;
import java.nio.file.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;
import java.util.concurrent.locks.LockSupport;

/** Phase10.6B actual event clocks, bounded PCM and authoritative scene tests. No listening claim. */
public final class Phase106AudioTest extends V2TestSupport {
    static void await(BooleanSupplier test,String message)throws Exception{long end=System.nanoTime()+8_000_000_000L;while(!test.getAsBoolean()&&System.nanoTime()<end)Thread.sleep(5);check(test.getAsBoolean(),message);}
    static List<String> csv(String line){List<String> out=new ArrayList<>();StringBuilder value=new StringBuilder();boolean quoted=false;for(int i=0;i<line.length();i++){char c=line.charAt(i);if(c=='"'){if(quoted&&i+1<line.length()&&line.charAt(i+1)=='"'){value.append(c);i++;}else quoted=!quoted;}else if(c==','&&!quoted){out.add(value.toString());value.setLength(0);}else value.append(c);}out.add(value.toString());return out;}
    static void catalog()throws Exception{
        var lines=Files.readAllLines(Path.of("assets/audio/phase10_6/AUDIO_MANIFEST.csv"));var header=csv(lines.get(0).replace("\ufeff",""));Set<String> paths=new HashSet<>();int music=0,cues=0;
        for(String line:lines.subList(1,lines.size())){
            var row=csv(line);String id=row.get(header.indexOf("event_id")),path=row.get(header.indexOf("file"));paths.add(path);
            check(Files.isRegularFile(Path.of(path)),"Mapped runtime resource exists "+id);
            check(ForestPreservationSupport.hash(Files.readAllBytes(Path.of(path))).equals(row.get(header.indexOf("output_sha256"))),"Exact approved runtime bytes "+id);
            if(id.matches("M[0-9]+"))music++;else{cues++;check(AudioCatalog.cue(id)!=null&&AudioCatalog.cue(id).path().equals(Phase4PreservationSupport.route(id,path)),"Registered original or exact authenticated Phase4 cue "+id);}
        }
        check(music==25&&cues==176&&paths.size()==106,"25 music /176 cues;105 mapped new files plus original menu");
        for(String path:paths){byte[] pcm=AudioResources.decode(path);check(pcm.length>0&&pcm.length%4==0,"Independent runtime decode "+path);}
        check(AudioCatalog.Music.MENU.path().equals("assets/audio/runtime/music/wintery_loop.wav"),"Original menu route");
        check(ForestPreservationSupport.hash(Files.readAllBytes(Path.of(AudioCatalog.Music.MENU.path()))).equals("c27f9aa0423da9d186184f771a423e8f82c40aac19ae06bbc96e86df25923f55"),"Original menu file exact");
        for(var track:AudioCatalog.Music.values())check(track==AudioCatalog.Music.MENU||track.path().startsWith("assets/audio/phase10_6/"),"No other legacy music "+track);
        for(var effect:AudioCatalog.Effect.values())check(PostPhase4Support.route(effect.eventId())||effect.path().startsWith("assets/audio/phase10_6/")||effect.path().startsWith("assets/audio/phase4/")&&effect.path().equals(Phase4PreservationSupport.route(effect.eventId(),effect.path())),"Only licensed new runtime SFX aliases "+effect);
        check(AudioCatalog.Music.TITAN3.path().contains("titan_battle_phase_3"),"Correct replacement M17; no paused Final Battle");
    }
    static void skills(){
        Santa santa=new Santa(0,0);SantaSkills skill=new SantaSkills();List<String> events=new ArrayList<>(),stops=new ArrayList<>();
        skill.bindAudio(events::add,(id,owner)->events.add(id),stops::add);
        check(skill.cast(2,santa,400,21,List.of()),"Accepted volley");check(events.equals(List.of("star.cast","star.release.v01")),"Cast and first release at0");
        check(!skill.cast(2,santa,400,21,List.of()),"Cooldown attempt rejected");check(events.size()==2,"No rejected cast sound");
        for(int i=0;i<3;i++)skill.update(List.of(),List.of());check(!events.contains("star.release.v02"),"No early second release");
        skill.update(List.of(),List.of());check(events.contains("star.release.v02"),"Second release4");
        for(int i=0;i<4;i++)skill.update(List.of(),List.of());check(events.contains("star.release.v03"),"Third release8");
        events.clear();skill.reset();skill.cast(2,santa,400,21,List.of());events.clear();for(int i=0;i<55;i++)skill.update(List.of(),List.of());check(events.stream().noneMatch(id->id.contains("hit")),"Range expiration has no fake impact sound");
        skill.reset();events.clear();var wall=new java.awt.geom.Rectangle2D.Double(40,-50,1,150);skill.cast(2,santa,400,21,List.of(wall));for(int i=0;i<15;i++)skill.update(List.of(),List.of(wall));check(events.contains("star.wall_hit"),"Actual wall collision cue");
        skill.reset();events.clear();stops.clear();Walker target=new Walker(60,10);skill.cast(3,santa,70,21,List.of());
        check(events.equals(List.of("bomb.throw")),"One accepted throw");for(int i=0;i<19;i++)skill.update(List.of(target),List.of());check(!events.contains("bomb.land"),"No early landing");skill.update(List.of(target),List.of());check(events.contains("bomb.land")&&events.contains("bomb.fuse"),"Landing/fuse at20");
        for(int i=0;i<29;i++)skill.update(List.of(target),List.of());check(!events.contains("bomb.explosion"),"No early explosion");skill.update(List.of(target),List.of());check(events.contains("bomb.explosion")&&events.contains("bomb.confirmed_hit")&&stops.contains("bomb-fuse"),"Explosion/hit/fuse stop at50");
        long n=events.stream().filter(x->x.equals("bomb.confirmed_hit")).count();for(int i=0;i<10;i++)skill.update(List.of(target),List.of());check(events.stream().filter(x->x.equals("bomb.confirmed_hit")).count()==n,"No duplicate bomb impact");
        skill.clearEffects();check(stops.size()>=2,"Cleanup cancels fuse ownership");
        AbilitySystem bell=new AbilitySystem();List<String> hits=new ArrayList<>();bell.skills().bindAudio(hits::add,(id,owner)->{},owner->{});Walker e=new Walker(50,10);bell.use(1,santa,List.of(e));check(e.getHealth()==25&&bell.getBellCooldownFrames()==90&&hits.contains("bell.hit")&&hits.contains("bell.gather"),"Bell damage75/cooldown90 immediate audio exact");
    }
    static final class Sink implements AudioManager.Sink{final AtomicInteger blocks=new AtomicInteger();final AtomicBoolean closed=new AtomicBoolean();public void write(byte[] pcm){blocks.incrementAndGet();LockSupport.parkNanos(1_000_000);}public void close(){closed.set(true);}}
    static void manager()throws Exception{
        Sink sink=new Sink();AudioManager manager=new AudioManager(()->sink,path->{byte[] b=new byte[path.contains("/music/")?12*1024*1024:1764];for(int i=0;i<b.length;i+=4){b[i]=1;b[i+2]=2;}return b;},null);
        List<String> events=new ArrayList<>();manager.setEventObserver(events::add);
        try{
            manager.playMusic(AudioCatalog.Music.MENU);manager.start();await(()->manager.getDecodedCount()>=77,"Critical SFX preload asynchronously");
            for(var track:AudioCatalog.Music.values()){manager.playMusic(track);await(()->manager.getPlayingMusic()==track,"Authoritative scene reaches shared mixer "+track);check(manager.getRetainedBytes()<=AudioManager.CACHE_LIMIT,"128MiB cache bounded");}
            check(manager.getPeakRetainedBytes()<=AudioManager.CACHE_LIMIT&&manager.getPeakRetainedBytes()>100*1024*1024,"Eviction exercised under actual decode pressure");
            manager.beginUpdate();manager.playEvent("star.enemy_hit");manager.playEvent("star.enemy_hit");check(events.size()==1,"Repeated same-update contacts coalesce");
            manager.beginUpdate();manager.playEvent("star.enemy_hit");check(events.size()==2&&!events.get(0).equals(events.get(1)),"Original alternate takes rotate");
            manager.beginUpdate();manager.playOwned("bomb.fuse","bomb-fuse",true);Thread.sleep(40);manager.stopOwner("bomb-fuse");Thread.sleep(20);check(manager.getActiveVoices()<=8,"Eight-voice pool bounded");
            manager.beginUpdate();manager.playOwned("titan.beam.sustain","boss-attack",true);manager.setGameplaySuspended(true);await(()->manager.getActiveVoices()==0,"Pause/cancel stops gameplay sustains and stale requests");
            int before=events.size();manager.playEvent("star.cast");check(events.size()==before,"No cast audio while suspended");manager.setGameplaySuspended(false);Thread.sleep(20);check(events.size()==before,"Resume never replays old audio");
            manager.beginUpdate();manager.playOwned("titan.collapse","cinematic-test",true);await(()->manager.getActiveVoices()>0,"Cinematic output begins");
            manager.cancelPresentation();await(()->manager.getActiveVoices()==0,"Cinematic skip cancels active presentation PCM and queued requests");
            manager.beginUpdate();manager.playLocal(0,"ambience.local_crystal");manager.playLocal(1,"ambience.local_titan_ritual");Thread.sleep(20);for(var cue:AudioCatalog.cues())if(!cue.loop())manager.playEvent(cue.id());Thread.sleep(40);check(manager.getActiveVoices()<=8,"Local layers reserve slots inside eight voices");manager.playLocal(0,null);manager.playLocal(1,null);
        }finally{manager.close();}
        await(sink.closed::get,"One real lifecycle closes injected output");
    }
    static void gameplay()throws Exception{
        GamePanel p=playing();AudioManager audio=(AudioManager)field(p,"audio");List<String> events=new ArrayList<>();audio.setEventObserver(events::add);
        try{
            check(audio.getRequestedMusic()==AudioCatalog.Music.VILLAGE,"Village route");
            var before=santa(p).getX();tap(p,KeyEvent.VK_D);check(santa(p).getX()>=before,"Movement remains valid");
            tap(p,KeyEvent.VK_2);check(events.contains("star.cast"),"Actual key2 produces cast");tick(p,3);int cooldown=((AbilitySystem)field(p,"abilitySystem")).skills().starCooldown();
            tap(p,KeyEvent.VK_ESCAPE);tick(p,20);check(((AbilitySystem)field(p,"abilitySystem")).skills().starCooldown()==cooldown,"Pause freezes existing skill clock");tap(p,KeyEvent.VK_ESCAPE);
            check(events.contains("star.release.v02"),"Release4 is audible on the first resumed active update");
            townFixture(p);tick(p,1);check(audio.getRequestedMusic()==AudioCatalog.Music.TOWN,"Corrupted Town scene");
            var c=new FinalChapter();var q=new quest.StoryQuestManager();var ending=new EndingCutscene();var tree=new SpiritTreeTransformation();Santa s=new Santa(0,0);FrostFang fang=new FrostFang(200,100);DarkTitan titan=new DarkTitan(200,100);
            check(AdventureAudio.scene(GamePanel.State.PLAYING,Area.Type.FOREST,q,c,ending,tree,fang,true,"",false)==AudioCatalog.Music.FANG_INTRO,"Fang intro real identity");
            check(AdventureAudio.scene(GamePanel.State.PLAYING,Area.Type.CHRISTMAS_TOWN,q,c,ending,tree,titan,true,"",false)==AudioCatalog.Music.TITAN_INTRO,"Titan intro real identity");
            check(AdventureAudio.scene(GamePanel.State.PLAYING,Area.Type.CHRISTMAS_TOWN,q,c,ending,tree,null,false,"boss",false)==AudioCatalog.Music.TREE,"No hostile music during Titan fall");
            check(AdventureAudio.scene(GamePanel.State.PLAYING,Area.Type.FOREST,q,c,ending,tree,null,false,"frost-fang",false)==AudioCatalog.Music.FOREST_INVESTIGATION,"Fang fall not final ending");
            List<String> ritual=new ArrayList<>();GameplayAudio g=new GameplayAudio(e->{});g.bindEvents(ritual::add,(id,owner)->{},owner->{});for(int t:new int[]{0,30,90,120,180,180})g.restorationTick(t);check(ritual.equals(List.of("tree.complete")),"Completion edge once; other ritual cues retain existing explicit aliases");check(SpiritTreeTransformation.DURATION==180,"Tree180 unchanged");
            check(EndingCredits.ENTRIES.stream().anyMatch(e->e.credit().contains("Emma"))&&EndingCredits.ENTRIES.stream().anyMatch(e->e.credit().equals("PagDev")),"Actual menu/new creators accessible in ending credits");
        }finally{p.stopGame();}
    }
    public static void main(String[] args)throws Exception{catalog();skills();manager();gameplay();System.out.println("Phase106AudioTest PASS "+checks+" assertions");}
}
