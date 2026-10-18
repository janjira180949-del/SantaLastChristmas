import game.*;
import boss.ChristmasEater;
import audio.AudioCatalog.Effect;
import entity.*;
import spirit.SpiritTree;
import java.awt.event.KeyEvent;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Actual lethal combat, one-time fall, unchanged restoration cues and ordered epilogue. */
public class GameplayOverhaulEndingTest extends V2TestSupport {
    static final List<Effect> heard=new ArrayList<>();
    static int count(Effect e){return (int)heard.stream().filter(v->v==e).count();}
    static void capture(GamePanel p,String name)throws Exception {
        Path dir=Path.of("build/gameplay-overhaul/captures");Files.createDirectories(dir);
        ImageIO.write(render(p),"png",dir.resolve("ending-"+name+".png").toFile());
    }
    public static void main(String[] args)throws Exception {
        GamePanel p=new GamePanel(heard::add);p.setSize(1280,800);tap(p,KeyEvent.VK_ENTER);tick(p,6);tap(p,KeyEvent.VK_E);
        townFixture(p);story(p).acceptEliasQuest();at(santa(p),encounters(p).get("town").getCenter());tick(p,1);clearWave(p);
        at(santa(p),(WorldMap.Point)call(p,"spiritTreeInteractionPoint"));tick(p,1);closeOverlay(p);
        var boss=(boss.DarkTitan)field(p,"activeBoss");santa(p).toggleDeveloperTestMode();boss.takeDamage(boss.getMaximumHealth()-20);
        var near=WorldMap.forArea(Area.Type.CHRISTMAS_TOWN).safePosition((int)boss.getX()-16,(int)boss.getY()+boss.getSize()/2+20,32,50);
        santa(p).setPosition(near.x(),near.y()+8);attack(p,boss.getX()+38,boss.getY()+38);
        check(!story(p).isBossDefeated()&&count(Effect.BOSS_DEFEAT)==1,"Real lethal attack sounds once; progression waits for fall");
        BossFallPresentation fall=(BossFallPresentation)field(p,"bossFall");check(fall.isActive()&&!p.isGameplayHudVisible(),"Fall begins once before restoration");
        int x=santa(p).getX(),y=santa(p).getY();tap(p,KeyEvent.VK_D);tap(p,KeyEvent.VK_SPACE);tap(p,KeyEvent.VK_1);tick(p,27);
        check(santa(p).getX()==x&&santa(p).getY()==y&&!santa(p).isAttacking(),"Fall consumes gameplay input safely");capture(p,"fall");
        tick(p,BossFallPresentation.DURATION-fall.getFrames());
        check(story(p).isBossDefeated()&&!fall.isActive()&&p.getState()==GamePanel.State.PLAYING&&count(Effect.BOSS_DEFEAT)==1,"Fall terminates without repeating defeat");
        check(!fall.start(boss,WorldMap.forArea(Area.Type.CHRISTMAS_TOWN).point("landmarks/Corrupted Town Center")),"Once-only presentation cannot restart");
        at(santa(p),(WorldMap.Point)call(p,"spiritTreeInteractionPoint"));tap(p,KeyEvent.VK_E);
        SpiritTreeTransformation restoration=(SpiritTreeTransformation)field(p,"treeTransformation");
        check(restoration.getFrames()==0&&count(Effect.SPIRIT_TREE_RESTORATION)==1,"Original E trigger and tick-zero cue");
        tick(p,29);check(count(Effect.SPIRIT_TREE_RISING)==0,"No early rising cue");tick(p,1);check(count(Effect.SPIRIT_TREE_RISING)==1,"Rising exactly at 30");
        capture(p,"restoration-30");tick(p,60);check(count(Effect.SPIRIT_TREE_BURST)==1&&restoration.getFrames()==90,"Burst exactly at 90");
        tick(p,30);check(count(Effect.SPIRIT_TREE_CHIME)==1&&restoration.getFrames()==120,"Chime exactly at 120");capture(p,"restoration-120");
        tick(p,59);check(p.getState()==GamePanel.State.PLAYING,"Actual-world control remains available at179");tick(p,1);
        check(restoration.isComplete()&&((SpiritTree)field(p,"spiritTree")).getSpirit()==100&&((EndingCutscene)field(p,"endingCutscene")).isCelebration(),"Exactly 180 ticks cap Spirit and hand off to celebration");
        JourneyHomeTest.completeCelebration(p);check(p.getState()==GamePanel.State.PLAYING,"Celebration returns control");
        tap(p,KeyEvent.VK_E);EndingCutscene end=(EndingCutscene)field(p,"endingCutscene");
        check(end.getState()==EndingCutscene.State.CELEBRATION_DIALOGUE,"Restored-tree E reuses concise gratitude rather than the old long outcome");tick(p,60);capture(p,"neighbors-thank-santa");
        check(end.getCinematicDialogue().getCurrentLine().getSpeakerName().equals("Elias"),"Elias still speaks for the Town");
        tap(p,KeyEvent.VK_E);check(end.getCinematicDialogue().getCurrentLine().getSpeakerName().equals("Santa"),"Santa replies without changing quest ownership");tap(p,KeyEvent.VK_E);
        check(!end.hasStarted()&&p.getState()==GamePanel.State.PLAYING,"Short gratitude returns control for playable homecoming");
        FinalChapterTest.finishChapter(p);tick(p,60);tick(p,180);capture(p,"title");tap(p,KeyEvent.VK_E);tick(p,120);capture(p,"final-message");tap(p,KeyEvent.VK_E);tick(p,650);capture(p,"credits");
        tick(p,EndingCutscene.duration(end.getState())-end.getFrames());check(end.isComplete(),"Existing credits terminate after final letter");
        check(count(Effect.BOSS_DEFEAT)==1&&count(Effect.SPIRIT_TREE_RESTORATION)==1&&count(Effect.SPIRIT_TREE_CHIME)==1,"No repeated presentation rewards or audio");
        tap(p,KeyEvent.VK_ESCAPE);check(p.getState()==GamePanel.State.MAIN_MENU,"Completed ending returns to Main Menu");
        tap(p,KeyEvent.VK_ENTER);tick(p,6);tap(p,KeyEvent.VK_E);check(!story(p).isBossDefeated()&&!((BossFallPresentation)field(p,"bossFall")).isActive(),"New adventure clears all presentation state");p.stopGame();
        for(int[] size:new int[][]{{800,600},{1270,681},{1270,720}}) {
            GamePanel q=endingFixture();q.setSize(size[0],size[1]);tick(q,180);JourneyHomeTest.completeCelebration(q);tap(q,KeyEvent.VK_E);tick(q,60);capture(q,"spirit-"+size[0]+"x"+size[1]);
            tap(q,KeyEvent.VK_E);tick(q,120);capture(q,"world-"+size[0]+"x"+size[1]);
            int sx=santa(q).getX();tap(q,KeyEvent.VK_D);tap(q,KeyEvent.VK_SPACE);check(santa(q).getX()==sx,"Epilogue input remains frozen");
            tap(q,KeyEvent.VK_ESCAPE);check(q.getState()==GamePanel.State.PLAYING,"Interrupted scene returns safely to restored Town");tap(q,KeyEvent.VK_D);check(santa(q).getX()>sx,"Movement resumes after dismissal");q.stopGame();
        }
        System.out.println("PASS: "+checks+" ending chronology, input, event and viewport assertions");
    }
}
