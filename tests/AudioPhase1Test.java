import audio.*;
import static audio.AudioCatalog.*;
import game.*;
import ui.CinematicMenu;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;
import java.util.prefs.Preferences;
import java.security.MessageDigest;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/** Real PCM decoding and output-buffer checks. Test sink does not produce audible sound. */
public final class AudioPhase1Test extends V2TestSupport {
    private static final Path OUT=Path.of("build/audio-phase1");
    private static void await(BooleanSupplier condition,String message)throws Exception {
        long until=System.nanoTime()+5_000_000_000L;
        while(!condition.getAsBoolean()&&System.nanoTime()<until)Thread.sleep(10);
        check(condition.getAsBoolean(),message);
    }
    private static final class CaptureSink implements AudioManager.Sink {
        final AtomicInteger peak=new AtomicInteger();final AtomicBoolean closed=new AtomicBoolean();
        volatile long blocks;
        @Override public void write(byte[] bytes) {
            int highest=0;
            for(int i=0;i<bytes.length;i+=2)highest=Math.max(highest,Math.abs((short)((bytes[i]&255)|(bytes[i+1]<<8))));
            peak.set(highest);blocks++;
            java.util.concurrent.locks.LockSupport.parkNanos(10_000_000L);
        }
        @Override public void close(){closed.set(true);}
    }
    private static void preserve()throws Exception {
        var matcher=Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/AUDIO_PHASE1_BASELINE.json")));
        int sources=0,assets=0;
        while(matcher.find()) {
            String path=matcher.group(1);if(path.equals("src/ui/CinematicMenu.java"))continue;
            byte[] bytes=AudioPhase2PresentationSupport.originalBytes(path,Files.readAllBytes(Path.of(path)));
            if(path.equals("src/game/GamePanel.java"))bytes=AudioPresentationSupport.withoutAudio(new String(bytes,java.nio.charset.StandardCharsets.UTF_8).replace("\r\n","\n")).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            check(hash.equals(matcher.group(2)),"Pre-audio source/artwork/download unchanged: "+path);
            if(path.endsWith(".java"))sources++;else assets++;
        }
        check(sources==61&&assets>220,"All existing gameplay/render owners preserved apart from menu controls and exact audio observer");
        check(SpiritTreeTransformation.DURATION==180,"Transformation remains three seconds");
    }
    private static void decodedAssets()throws Exception {
        Set<String> paths=new HashSet<>();for(Music track:Music.values())paths.add(track.path());
        for(Effect effect:Effect.values())if(!effect.isGameplay())paths.add(effect.path());
        long bytes=0;
        for(String path:paths) {
            byte[] pcm=AudioResources.decode(path);bytes+=pcm.length;
            check(pcm.length>0&&pcm.length%4==0,"Java Sound decodes real PCM: "+path);
        }
        check(paths.size()==27,"Twenty-one distinct selected music recordings and six replaced UI effects");
        boolean missing=false;try{AudioResources.decode("assets/audio/not-present.wav");}catch(java.io.IOException expected){missing=true;}
        check(missing,"Missing-file resource decoder has a clear recoverable error");
        System.out.println("Real PCM decoded: "+bytes+" bytes (no listening test)");
    }
    private static void mixerAndPreferences()throws Exception {
        CaptureSink sink=new CaptureSink();AtomicInteger opens=new AtomicInteger(),decodes=new AtomicInteger();
        byte[] constant=new byte[17640];for(int i=0;i<constant.length;i+=2){constant[i]=0x10;constant[i+1]=0x27;}
        Preferences store=Preferences.userRoot().node("SantaLastChristmas/audio-test-"+UUID.randomUUID());
        AudioManager manager=new AudioManager(()->{opens.incrementAndGet();return sink;},path->{decodes.incrementAndGet();return constant;},store);
        try {
            manager.playMusic(Music.MENU);manager.start();manager.setMasterVolume(100);manager.setMusicVolume(100);manager.setSfxVolume(100);
            await(()->sink.peak.get()==10000,"Music fades into full software gain");
            long count=manager.getDecodedCount();for(int i=0;i<1000;i++)manager.playMusic(Music.MENU);
            Thread.sleep(50);check(opens.get()==1&&manager.getDecodedCount()==count,"Same track does not open duplicate output or decode again");
            manager.setMasterVolume(50);await(()->sink.peak.get()==5000,"Master gain takes effect in actual PCM");
            manager.setMusicVolume(50);await(()->sink.peak.get()==2500,"Music gain multiplies master gain");
            manager.setMuted(true);await(()->sink.peak.get()==0,"Mute outputs zero PCM");
            manager.setMuted(false);await(()->sink.peak.get()==2500,"Unmute restores prior volume");
            manager.setMasterVolume(200);manager.setSfxVolume(-1);check(manager.getMasterVolume()==100&&manager.getSfxVolume()==0,"Volumes clamp to 0–100");
            manager.stopMusic();await(()->sink.peak.get()==0&&manager.getPlayingMusic()==null,"Stop fades music to silence");
            manager.setSfxVolume(100);manager.playEffect(Effect.CLICK);await(()->sink.peak.get()==10000,"SFX use master and SFX gain independently of music");
            await(()->sink.peak.get()==0,"First one-shot finishes before independent SFX gain check");
            manager.setSfxVolume(25);manager.playEffect(Effect.SETTINGS);await(()->sink.peak.get()==2500,"SFX volume changes actual samples");
            for(Music track:Music.values()) {
                manager.playMusic(track);await(()->manager.getPlayingMusic()==track,"Track switch and loop: "+track);
            }
            check(opens.get()==1,"All states share a single output line");
            manager.setMasterVolume(37);manager.setMusicVolume(42);manager.setSfxVolume(53);manager.setMuted(true);
            await(()->store.getInt("master",-1)==37&&store.getBoolean("muted",false),"Preferences save on worker");
        } finally {manager.close();}
        await(sink.closed::get,"Audio resources close");
        AudioManager restored=new AudioManager(CaptureSink::new,path->constant,store);
        try {
            restored.start();await(()->restored.getMasterVolume()==37&&restored.getMusicVolume()==42&&restored.getSfxVolume()==53&&restored.isMuted(),"Persisted settings load on a new manager");
        } finally {restored.close();}
        Thread.sleep(100);store.removeNode();
        CaptureSink errors=new CaptureSink();AudioManager missing=new AudioManager(()->errors,path->{throw new java.io.IOException("missing fixture");},null);
        try {
            missing.playMusic(Music.MENU);missing.start();await(()->missing.getMixedBuffers()>5,"Missing audio never stops output/game");
            check(missing.getPlayingMusic()==null&&errors.peak.get()==0,"Missing track stays silent without substitution");
        } finally{missing.close();}
        AudioManager absentDevice=new AudioManager(()->{throw new javax.sound.sampled.LineUnavailableException("no device fixture");},path->constant,null);
        try {absentDevice.start();await(()->absentDevice.getDiagnostic().contains("unavailable"),"Unsupported/missing hardware is graceful");}finally{absentDevice.close();}
        // Exercise actual downloaded music through the production mixer, not just decoding.
        CaptureSink realSink=new CaptureSink();AudioManager real=new AudioManager(()->realSink,AudioResources::decode,null);
        try {
            real.start();for(Music track:Music.values()) {
                real.playMusic(track);await(()->real.getPlayingMusic()==track,"Real downloaded PCM reaches mixer: "+track);
                await(()->realSink.peak.get()>0,"Real track produces nonzero samples: "+track);
            }
            check(real.getDecodedCount()>=94,"All twenty-one music recordings and73 SFX decode; bounded music eviction may re-decode shared recordings");
        } finally{real.close();}
    }
    private static void mapping()throws Exception {
        GamePanel p=new GamePanel();p.setSize(1270,720);AudioManager audio=(AudioManager)field(p,"audio");
        check(audio.getRequestedMusic()==Music.MENU,"Initial menu requests Wintery Loop");
        p.beginAdventure();check(audio.getRequestedMusic()==Music.BASE,"START selects new Base music");closeOverlay(p);
        townFixture(p);tick(p,1);check(audio.getRequestedMusic()==Music.TOWN,"Town entry requests Winter Wind");
        travelGate(p,Area.Type.FOREST);check(audio.getRequestedMusic()==Music.FOREST_INVESTIGATION,"Forest entry selects investigation music for current prerequisites");
        travelGate(p,Area.Type.CHRISTMAS_TOWN);
        var elias=(entity.Elias)field(p,"elias");at(santa(p),new WorldMap.Point(elias.getX()+16,elias.getY()+24));tap(p,KeyEvent.VK_E);closeDialogue(p);
        at(santa(p),encounters(p).get("town").getCenter());tick(p,1);clearWave(p);
        at(santa(p),(WorldMap.Point)call(p,"spiritTreeInteractionPoint"));tick(p,1);
        check(audio.getRequestedMusic()==Music.TITAN_INTRO,"Actual boss introduction selects its reveal music");
        clearWave(p);check(audio.getRequestedMusic()==Music.TREE,"Boss defeat stops hostile music and selects restoration bed");
        at(santa(p),(WorldMap.Point)call(p,"spiritTreeInteractionPoint"));tap(p,KeyEvent.VK_E);
        check(audio.getRequestedMusic()==Music.TREE,"Restoration uses its independent tree music");
        tick(p,179);check(p.getState()==GamePanel.State.PLAYING,"Audio does not shorten transformation; restoration stays in-world");tick(p,1);
        check(p.getState()==GamePanel.State.ENDING&&((EndingCutscene)field(p,"endingCutscene")).isCelebration()&&audio.getRequestedMusic()==Music.ENDING,"Exact restoration completion enters the automatic celebration");
        JourneyHomeTest.completeCelebration(p);check(p.getState()==GamePanel.State.PLAYING&&audio.getRequestedMusic()==Music.TOWN_PEACEFUL,"Celebration dismissal selects peaceful Town");
        FinalChapterTest.finishChapter(p);tap(p,KeyEvent.VK_E);tap(p,KeyEvent.VK_E);tap(p,KeyEvent.VK_E);check(audio.getRequestedMusic()==Music.CREDITS,"Credits use the new credits scene");
        EndingCutscene ending=(EndingCutscene)field(p,"endingCutscene");tick(p,EndingCutscene.duration(ending.getState())-ending.getFrames());
        tap(p,KeyEvent.VK_ESCAPE);check(audio.getRequestedMusic()==Music.MENU,"Ending returns to menu music");
        p.stopGame();
    }
    private static void key(CinematicMenu menu,int code){menu.keyPressed(code);menu.keyReleased(code);for(int i=0;i<6;i++)menu.update();}
    private static void settings()throws Exception {
        CaptureSink sink=new CaptureSink();AtomicInteger calls=new AtomicInteger();byte[] signal=new byte[17640];for(int i=1;i<signal.length;i+=2)signal[i]=20;
        AudioManager audio=new AudioManager(()->sink,path->{calls.incrementAndGet();return signal;},null);audio.start();
        try {
            await(()->calls.get()==Phase4PreservationSupport.preloadCount(),"All distinct current SFX preload asynchronously");CinematicMenu menu=new CinematicMenu(audio);
            key(menu,KeyEvent.VK_DOWN);key(menu,KeyEvent.VK_DOWN);key(menu,KeyEvent.VK_ENTER);
            check(menu.getPage()==CinematicMenu.Page.SETTINGS,"Existing SETTINGS page opens");
            key(menu,KeyEvent.VK_DOWN);int master=audio.getMasterVolume();key(menu,KeyEvent.VK_LEFT);
            check(audio.getMasterVolume()==master-5,"Keyboard master adjustment");
            key(menu,KeyEvent.VK_DOWN);key(menu,KeyEvent.VK_LEFT);check(audio.getMusicVolume()==55,"Keyboard music adjustment");
            key(menu,KeyEvent.VK_DOWN);key(menu,KeyEvent.VK_LEFT);check(audio.getSfxVolume()==65,"Keyboard SFX adjustment");
            key(menu,KeyEvent.VK_DOWN);key(menu,KeyEvent.VK_ENTER);check(audio.isMuted(),"Keyboard mute toggle");key(menu,KeyEvent.VK_ENTER);check(!audio.isMuted(),"Keyboard unmute toggle");
            for(int[] size:new int[][]{{800,600},{1270,681},{1270,720}}) {
                int w=size[0],h=size[1];Rectangle last=menu.motionButton(w,h);
                for(int i=0;i<4;i++) {
                    Rectangle row=menu.audioButton(w,h,i);check(!row.intersects(last)&&row.y>=0&&row.y+row.height<h,"Audio row layout: "+i+" / "+w+"x"+h);last=row;
                    if(i<3) {
                        int x=row.x+row.width-62,y=(int)row.getCenterY();menu.mousePressed(x,y,w,h);menu.mouseReleased(x,y,w,h);
                        check((i==0?audio.getMasterVolume():i==1?audio.getMusicVolume():audio.getSfxVolume())==100,"Mouse slider reaches 100%: "+i);
                        menu.mousePressed(row.x+row.width/2,y,w,h);menu.mouseMoved(x,y,w,h);menu.mouseReleased(x,y,w,h);
                        check((i==0?audio.getMasterVolume():i==1?audio.getMusicVolume():audio.getSfxVolume())==100,"Mouse drag adjusts slider: "+i);
                    }
                }
                check(!last.intersects(menu.backButton(w,h)),"No settings/back overlap");
                var image=new java.awt.image.BufferedImage(w,h,java.awt.image.BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();menu.draw(g,w,h);g.dispose();
                ImageIO.write(image,"png",OUT.resolve("settings-"+w+"x"+h+".png").toFile());
            }
            // Repeated mouse motion over one selection must enqueue a single hover sound.
            key(menu,KeyEvent.VK_ESCAPE);Rectangle row=CinematicMenu.button(800,600,3);
            Thread.sleep(200);menu.mouseMoved((int)row.getCenterX(),(int)row.getCenterY(),800,600);
            @SuppressWarnings("unchecked") var queue=(java.util.concurrent.BlockingQueue<Effect>)field(audio,"effects");
            int queued=queue.size();for(int i=0;i<200;i++)menu.mouseMoved((int)row.getCenterX(),(int)row.getCenterY(),800,600);
            check(queue.size()<=queued,"Repeated hover does not enqueue sounds each frame");
        } finally{audio.close();}
    }
    public static void main(String[] args)throws Exception {
        Files.createDirectories(OUT);preserve();decodedAssets();mixerAndPreferences();mapping();settings();
        System.out.println("PASS: "+checks+" audio/sample/settings/gameplay assertions; three settings captures. No audio listening claimed.");
    }
}
