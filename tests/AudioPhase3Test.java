import audio.*;
import static audio.AudioCatalog.*;
import boss.ChristmasEater;
import entity.*;
import game.*;
import java.awt.event.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;

/** Behavioral PCM/envelope tests and actual game routing. No audible output or listening claims. */
public final class AudioPhase3Test extends V2TestSupport {
    private static void preservation()throws Exception {
        var matcher=Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/AUDIO_PHASE3_BASELINE.json")));
        int protectedFiles=0;
        while(matcher.find()) {
            String path=matcher.group(1);
            if(Set.of("src/audio/AudioManager.java","src/audio/AudioCatalog.java","src/audio/GameplayAudio.java").contains(path))continue;
            byte[] bytes=OverhaulPreservationSupport.previous(path,Files.readAllBytes(Path.of(path)));
            if(path.equals("src/game/GamePanel.java"))bytes=AudioPhase3PresentationSupport.withoutPhase3(new String(bytes,StandardCharsets.UTF_8).replace("\r\n","\n")).replace("\n","\r\n").getBytes(StandardCharsets.UTF_8);
            check(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).equals(matcher.group(2)),"Pre-Phase 3 gameplay, assets, settings and launch configuration preserved: "+path);protectedFiles++;
        }
        check(protectedFiles==526,"Every preexisting non-audio-owner file protected by exact hashes");
        check(SpiritTreeTransformation.DURATION==180,"Authoritative restoration duration preserved");
    }
    private static void policies() {
        check(AudioPolish.transitionMillis(Music.MENU,Music.VILLAGE)==1000,"Menu transition duration");
        check(AudioPolish.transitionMillis(Music.FOREST,Music.TOWN)==1250,"Area transition duration");
        check(AudioPolish.transitionMillis(Music.TOWN,Music.BOSS)==750,"Boss transition duration");
        check(AudioPolish.transitionMillis(Music.BOSS,Music.ENDING)==500,"Ending transition duration");
        check(AudioPolish.distanceGain(0,0,0,0)==1&&AudioPolish.distanceGain(0,0,96,0)==1,"Near attenuation boundary");
        check(Math.abs(AudioPolish.distanceGain(0,0,408,0)-.60)<.00001,"Smooth attenuation midpoint");
        check(Math.abs(AudioPolish.distanceGain(0,0,720,0)-.20)<.00001&&Math.abs(AudioPolish.distanceGain(0,0,100000,0)-.20)<.00001,"Distant attenuation floor");
        check(AudioPolish.distanceGain(100,300,-100,-300)==AudioPolish.distanceGain(-100,-300,100,300),"Attenuation uses symmetric 2D world coordinates");
        for(Effect effect:List.of(Effect.SANTA_ATTACK,Effect.ENEMY_HIT,Effect.ENEMY_DEFEAT,Effect.BOSS_DAMAGE)) {
            double previous=-1;
            for(int i=0;i<100;i++){double gain=AudioPolish.variationGain(effect,i);check(gain>=.91&&gain<=1&&gain!=previous,"Repeated combat cue has bounded nonrepeating gain: "+effect);previous=gain;}
        }
        for(Effect effect:List.of(Effect.CLICK,Effect.QUEST_COMPLETE,Effect.BOSS_DEFEAT,Effect.SPIRIT_TREE_CHIME))for(int i=0;i<4;i++)check(AudioPolish.variationGain(effect,i)==1,"Critical/UI event gain unchanged");
    }
    private static void worldObserver() {
        List<Effect> cues=new ArrayList<>();List<Double> gains=new ArrayList<>();
        GameplayAudio observer=new GameplayAudio((effect,gain)->{cues.add(effect);gains.add(gain);});Santa santa=new Santa(0,0);Walker near=new Walker(16,21),far=new Walker(1000,1000);
        observer.captureCombat(santa,List.of(near,far));near.takeDamage(1);far.takeDamage(1000);observer.resolveCombat();
        check(cues.equals(List.of(Effect.ENEMY_HIT,Effect.ENEMY_HIT,Effect.ENEMY_DEFEAT)),"Existing hit/defeat mappings and order preserved");
        check(gains.get(0)==1&&Math.abs(gains.get(1)-.20)<.00001&&gains.get(1).equals(gains.get(2)),"Real actor coordinates attenuate distant world hit and defeat");
        cues.clear();gains.clear();ChristmasEater boss=new ChristmasEater(1000,1000);
        observer.captureCombat(santa,List.of(boss));boss.takeDamage(1000);observer.resolveCombat();
        check(cues.contains(Effect.BOSS_DEFEAT)&&gains.stream().allMatch(g->g==1),"Far boss cues remain clearly audible");
        observer.playWorld(Effect.CRYSTAL_BREAK,santa,1000,1000);check(Math.abs(gains.get(gains.size()-1)-.20)<.00001,"Environmental interactions use the same attenuation");
        int size=cues.size();observer.resolveCombat();check(cues.size()==size,"Repeated observer resolve stays silent");
    }
    private static void gameRouting()throws Exception {
        GamePanel panel=new GamePanel();panel.setSize(1270,720);AudioManager audio=(AudioManager)field(panel,"audio");
        try {
            check(audio.getRequestedMusic()==Music.MENU,"Initial menu priority");panel.beginAdventure();check((boolean)field(audio,"dialogueActive"),"Existing story overlay requests dialogue ducking");closeOverlay(panel);tick(panel,1);
            check(!(boolean)field(audio,"dialogueActive"),"Closing story releases duck");enterVillage(panel);
            completeOpeningDeliveries(panel);var child=panel.getChildren().get(0);at(santa(panel),new WorldMap.Point(child.getX()+15,child.getY()+21));tap(panel,KeyEvent.VK_E);
            check((boolean)field(audio,"dialogueActive"),"Actual NPC interaction holds music duck");closeDialogue(panel);
            at(santa(panel),encounters(panel).get("village").getCenter());tick(panel,1);
            check((boolean)field(audio,"combatActive")&&audio.getRequestedMusic()==Music.COMBAT,"Actual encounter selects the approved new ordinary-combat scene");
            tick(panel,20);check(audio.getRequestedMusic()==Music.COMBAT,"Combat track remains stable throughout the active encounter");clearWave(panel);
            check(!(boolean)field(audio,"combatActive"),"Finishing active encounter releases combat mix");
            townFixture(panel);tick(panel,1);check(audio.getRequestedMusic()==Music.TOWN,"Town exploration route retained");
            var elias=(Elias)field(panel,"elias");at(santa(panel),new WorldMap.Point(elias.getX()+16,elias.getY()+24));tap(panel,KeyEvent.VK_E);closeDialogue(panel);
            at(santa(panel),encounters(panel).get("town").getCenter());tick(panel,1);clearWave(panel);
            at(santa(panel),(WorldMap.Point)call(panel,"spiritTreeInteractionPoint"));tick(panel,1);
            check(audio.getRequestedMusic()==Music.TITAN_INTRO&&!(boolean)field(audio,"combatActive"),"Living boss takes priority over area and ordinary combat mixing");
            closeOverlay(panel);tick(panel,61);check(audio.getRequestedMusic()==Music.BOSS,"Boss priority remains stable without per-frame restarts");clearWave(panel);
            check(audio.getRequestedMusic()==Music.TREE,"Boss defeat retains restoration music without hostile Town fallback");
            at(santa(panel),(WorldMap.Point)call(panel,"spiritTreeInteractionPoint"));tap(panel,KeyEvent.VK_E);
            check(audio.getRequestedMusic()==Music.TREE&&!(boolean)field(audio,"combatActive"),"Restoration ending priority overrides combat");
            tick(panel,179);check(panel.getState()==GamePanel.State.PLAYING,"Audio does not change restoration clock; restoration stays in-world");tick(panel,1);
            check(panel.getState()==GamePanel.State.ENDING&&((EndingCutscene)field(panel,"endingCutscene")).isCelebration(),"Restoration hands off to the automatic celebration");
            JourneyHomeTest.completeCelebration(panel);check(panel.getState()==GamePanel.State.PLAYING&&audio.getRequestedMusic()==Music.TOWN_PEACEFUL,"Celebration returns to peaceful Town music");
            FinalChapterTest.finishChapter(panel);tap(panel,KeyEvent.VK_E);tap(panel,KeyEvent.VK_E);tap(panel,KeyEvent.VK_E);check(audio.getRequestedMusic()==Music.CREDITS,"Epilogue/credits priority overrides area");
            EndingCutscene ending=(EndingCutscene)field(panel,"endingCutscene");tick(panel,EndingCutscene.duration(ending.getState())-ending.getFrames());tap(panel,KeyEvent.VK_ESCAPE);
            check(audio.getRequestedMusic()==Music.MENU&&!(boolean)field(audio,"dialogueActive")&&!(boolean)field(audio,"combatActive"),"Main menu clears temporary gameplay mix and restores menu music");
        }finally{panel.stopGame();}
    }
    public static void main(String[] args)throws Exception {
        preservation();policies();worldObserver();int pcmChecks=AudioPhase3MixerSupport.verify();gameRouting();
        System.out.println("PASS: "+checks+" Phase 3 policy/preservation/world assertions, "+pcmChecks+" deterministic envelope/PCM/worker assertions. Human listening still required.");
    }
}
