package game;

import audio.*;
import entity.*;
import boss.BossCombatant;
import quest.StoryQuestManager;
import ui.VillageActivityUI;
import java.util.List;
import static audio.AudioCatalog.*;

/** Audio-only observer of existing story/world owners. Never writes gameplay state. */
public final class AdventureAudio {
    private final AudioManager audio;
    private FinalChapter chapter;
    private Area.Type lastArea;
    private GamePanel.State lastState;
    private EndingCutscene.State lastEnding=EndingCutscene.State.NOT_STARTED;
    private VillageActivityUI.Mode lastMode=VillageActivityUI.Mode.NONE;
    private final boolean[] picked=new boolean[9],repairs=new boolean[4];
    private final BlackFrostCrystal.State[] crystalStates=new BlackFrostCrystal.State[3];
    private boolean home,letter,dialogue,fallActive,fangWon,titanWon,introActive;
    private int deliveries,lastLetterIndex,lastX,lastY,stepFrames;
    private double walking;
    private Enemy introduced;
    private Music fallMusic;private int lastFallFrame=-1;private boolean fallImpact;private int sleighCues;
    public AdventureAudio(AudioManager audio){this.audio=audio;}
    public void reset(){chapter=null;lastArea=null;lastState=null;lastEnding=EndingCutscene.State.NOT_STARTED;lastMode=VillageActivityUI.Mode.NONE;introduced=null;walking=0;stepFrames=0;introActive=false;sleighCues=0;fallMusic=null;lastFallFrame=-1;fallImpact=false;audio.setBossFalling(false);audio.cancelGameplay();audio.cancelPresentation();}
    public static Music scene(GamePanel.State state,Area.Type area,StoryQuestManager story,FinalChapter chapter,
            EndingCutscene ending,SpiritTreeTransformation tree,Enemy boss,boolean intro,String falling,boolean combat){
        if(state==GamePanel.State.MAIN_MENU)return Music.MENU;
        if(state==GamePanel.State.GAME_OVER)return Music.GAME_OVER;
        if(!falling.isEmpty())return falling.equals("frost-fang")?Music.FOREST_INVESTIGATION:Music.TREE;
        if(tree.isActive())return Music.TREE;
        if(ending.hasStarted())return switch(ending.getState()){
            case SLEIGH_DEPARTURE->Music.SLEIGH;case CREDITS->Music.CREDITS;case END->Music.END;
            case HOME_FADE,TITLE,ENDING_MESSAGE->ending.isHomeEnding()?Music.HOME:Music.ENDING;
            case SPIRIT_RETURNS,WORLD_RECOVERY->Music.TREE;default->Music.ENDING;};
        if(boss instanceof BossCombatant actor&&!boss.isDefeated()){
            boolean fang=boss instanceof boss.FrostFang;
            if(intro||actor.getAnimationName().equals("entrance"))return fang?Music.FANG_INTRO:Music.TITAN_INTRO;
            return fang?switch(actor.getPhaseIndex()){case 0->Music.FANG1;case 1->Music.FANG2;default->Music.FANG3;}:
                switch(actor.getPhaseIndex()){case 0->Music.BOSS;case 1->Music.TITAN2;default->Music.TITAN3;};
        }
        if(combat&&!story.isBossDefeated())return Music.COMBAT;
        return switch(area){
            case SANTA_BASE->chapter.homeReturned()?Music.HOME:Music.BASE;
            case VILLAGE->chapter.isComplete()?Music.VILLAGE_RESTORED:Music.VILLAGE;
            case FOREST->switch(story.getStage()){
                case RESTORE_VILLAGE,TALK_TO_TOBY->Music.FOREST_MATERIALS;
                case INVESTIGATE,DEFEAT_FROST_FANG,TRAVEL_TOWN,REACH_TREE,DEFEAT_BOSS,RESTORE_TREE,CHRISTMAS_RESTORED->Music.FOREST_INVESTIGATION;
                default->Music.FOREST;};
            case CHRISTMAS_TOWN->tree.isComplete()?Music.TOWN_PEACEFUL:story.isBossDefeated()?Music.TREE:Music.TOWN;
        };
    }
    public void sync(GamePanel.State state,Area.Type area,Santa santa,StoryQuestManager story,VillageActivities village,
            EndingCutscene ending,SpiritTreeTransformation tree,Enemy boss,boolean overlay,boolean speaking,
            VillageActivityUI ui,String falling,List<BlackFrostCrystal> crystals,Encounter encounter){
        sync(state,area,santa,story,village,ending,tree,boss,overlay,speaking,ui,falling,null,crystals,encounter);
    }
    public void sync(GamePanel.State state,Area.Type area,Santa santa,StoryQuestManager story,VillageActivities village,
            EndingCutscene ending,SpiritTreeTransformation tree,Enemy boss,boolean overlay,boolean speaking,
            VillageActivityUI ui,String falling,BossFallPresentation fall,List<BlackFrostCrystal> crystals,Encounter encounter){
        FinalChapter current=village.chapter;
        boolean fresh=chapter!=current;
        if(fresh){
            chapter=current;for(int i=0;i<9;i++)picked[i]=current.isCollected(i);
            for(var r:FinalChapter.Restoration.values())repairs[r.ordinal()]=current.restored(r);
            java.util.Arrays.fill(crystalStates,BlackFrostCrystal.State.DORMANT);
            deliveries=village.deliveries();home=current.homeReturned();letter=current.letterFinished();fangWon=story.isFrostFangDefeated();titanWon=story.isBossDefeated();
            introduced=null;lastX=santa.getX();lastY=santa.getY();
        }
        boolean movedArea=lastArea!=null&&lastArea!=area;
        boolean intro=boss instanceof BossCombatant actor&&!boss.isDefeated()&&(overlay||actor.getAnimationName().equals("entrance"));
        if(movedArea||state==GamePanel.State.MAIN_MENU&&lastState!=state||fallActive&&falling.isEmpty()&&lastFallFrame<(fall==null?BossFallPresentation.DURATION:fall.getDuration())-1||introActive&&!intro||lastEnding!=EndingCutscene.State.NOT_STARTED&&lastEnding!=ending.getState())audio.cancelPresentation();
        boolean blocked=state!=GamePanel.State.PLAYING||overlay||speaking||ui.isOpen()||!falling.isEmpty()||ending.hasStarted();
        if(movedArea||(!falling.isEmpty()&&!fallActive)||state==GamePanel.State.MAIN_MENU&&lastState!=state)audio.cancelGameplay();
        audio.setGameplaySuspended(blocked);
        if(lastState!=null&&lastState!=state){
            if(state==GamePanel.State.PAUSED)audio.playEvent("ui.pause");
            if(lastState==GamePanel.State.PAUSED&&state==GamePanel.State.PLAYING)audio.playEvent("ui.resume");
            if(lastState==GamePanel.State.GAME_OVER&&state==GamePanel.State.PLAYING)audio.playEvent("ui.retry");
        }
        int fallFrame=fall==null?-1:fall.getFrames();
        if(!falling.isEmpty()&&!fallActive){fallMusic=audio.getRequestedMusic();fallImpact=false;}
        audio.setBossFalling(!falling.isEmpty());
        if(fall!=null&&!falling.isEmpty()&&!fallImpact&&fallFrame>=fall.getImpactFrame()&&fallFrame<fall.getDuration()){
            audio.playEvent(falling.equals("frost-fang")?"fang.fall":"titan.collapse");fallImpact=true;
        }
        Music track=scene(state,area,story,current,ending,tree,boss,overlay,falling,encounter!=null);
        // Hold the encounter's own phrase under the fall, then begin its resolving crossfade.
        // A skip selects the authoritative next scene immediately; it never restarts the boss.
        if(!falling.isEmpty()&&fallFrame>=0&&fallFrame<(fall==null?90:fall.getResolveFrame())&&fallMusic!=null)track=fallMusic;
        audio.playMusic(track);
        lastFallFrame=fallFrame;
        audio.playAmbience(state==GamePanel.State.MAIN_MENU?null:switch(area){
            case SANTA_BASE->Ambience.BASE;case VILLAGE->current.isComplete()?Ambience.VILLAGE_RESTORED:Ambience.VILLAGE;
            case FOREST->Ambience.FOREST;case CHRISTMAS_TOWN->tree.isComplete()?Ambience.TOWN_PEACEFUL:Ambience.TOWN;});
        audio.setDialogueActive(speaking||overlay||state==GamePanel.State.PAUSED);
        audio.setCombatActive(state==GamePanel.State.PLAYING&&encounter!=null&&boss==null);
        if(boss!=null&&boss!=introduced&&boss instanceof BossCombatant){
            introduced=boss;audio.playEvent(boss instanceof boss.FrostFang?"fang.entrance":"titan.entrance");
            if(!(boss instanceof boss.FrostFang))audio.playEvent("titan.core");
        }
        // Cinematic cues are accepted presentation edges, despite gameplay being suspended.
        if(!fresh){
            for(var p:FinalChapter.PICKUPS)if(current.isCollected(p.id())&&!picked[p.id()]){
                picked[p.id()]=true;audio.playEvent(switch(p.material()){case SPIRIT_FRAGMENT->"pickup.spirit";case ANCIENT_WOOD->"pickup.wood";case ORNAMENT->"pickup.ornament";});
                if(current.count(p.material())==3){int total=0;for(var material:FinalChapter.Material.values())total+=current.count(material);audio.playEvent(total==9?"materials.all_ready":"materials.category_ready");}
            }
            for(var r:FinalChapter.Restoration.values())if(current.restored(r)&&!repairs[r.ordinal()]){repairs[r.ordinal()]=true;audio.playEvent(switch(r){case STREETLIGHTS->"village.restore_lights";case HOUSE->"village.restore_house";case TREE->"village.restore_tree";case CAMPFIRE->"village.restore_campfire";});}
            if(village.deliveries()>deliveries)audio.playEvent("delivery.success");
            if(current.homeReturned()&&!home)audio.playEvent("home.arrive");
            if(current.letterFinished()&&!letter)audio.playEvent("letters.ninth_ack");
            if(story.isFrostFangDefeated()&&!fangWon)audio.playEvent("fang.victory");
            if(story.isBossDefeated()&&!titanWon)audio.playEvent("titan.final_victory");
        }
        for(int i=0;i<crystals.size();i++){
            var crystal=crystals.get(i);var next=crystal.getState();
            if(area==Area.Type.FOREST&&next!=crystalStates[i]){
                if(next==BlackFrostCrystal.State.GUARDED)audio.playEvent("crystal.guarded");
                if(next==BlackFrostCrystal.State.VULNERABLE)audio.playEvent("crystal.exposed");
                if(next==BlackFrostCrystal.State.BROKEN)audio.playEvent("crystal.collapse");
            }crystalStates[i]=next;
        }
        if(ui.mode()!=lastMode){
            if(ui.mode()==VillageActivityUI.Mode.LETTERS)audio.playEvent("letters.open");
            else if(lastMode==VillageActivityUI.Mode.LETTERS)audio.playEvent("letters.close");
            else audio.playEvent(ui.isOpen()?"ui.modal_open":"ui.modal_close");
        }else if(ui.mode()==VillageActivityUI.Mode.LETTERS&&ui.letterIndex()!=lastLetterIndex)audio.playEvent("letters.turn");
        if(ending.getState()!=lastEnding){
            switch(ending.getState()){
                case CELEBRATION->audio.playEvent("celebration.gather");case CELEBRATION_DIALOGUE->audio.playEvent("celebration.gratitude");
                case SLEIGH_DEPARTURE->{audio.playEvent("sleigh.launch");audio.playOwned("sleigh.board","sleigh-board",false);sleighCues=1;}case CREDITS->audio.playEvent("credits.enter");case END->audio.playEvent("credits.end");default->{}
            }
        }
        if(ending.getState()==EndingCutscene.State.SLEIGH_DEPARTURE){
            // Three finite authorized bell takes cover preparation through flight; each has its own natural tail.
            int frame=ending.getFrames();
            for(int i=0;i<3;i++){int bit=2<<i;if(frame>=EndingCutscene.SLEIGH_BELLS_START+i*EndingCutscene.SLEIGH_BELL_PERIOD&&(sleighCues&bit)==0){audio.playOwned("sleigh.bells.move","sleigh-movement",false);sleighCues|=bit;}}
            if(frame>=EndingCutscene.SLEIGH_TAKEOFF&&(sleighCues&16)==0){audio.playOwned("sleigh.laugh","sleigh-laughter",false);sleighCues|=16;}
        }else sleighCues=0;
        if(!blocked&&!movedArea){
            double distance=Math.hypot(santa.getX()-lastX,santa.getY()-lastY);
            if(distance>0&&distance<=16&&!santa.isDashing()){walking+=distance;if(++stepFrames>=12&&walking>=48){audio.playEvent(surface(area,santa));walking=0;stepFrames=0;}}
        }else{walking=0;stepFrames=0;}
        String local=null;
        if(area==Area.Type.FOREST)for(var crystal:crystals)if(crystal.getState()!=BlackFrostCrystal.State.BROKEN&&crystal.getState()!=BlackFrostCrystal.State.DORMANT&&near(santa,crystal.getPosition().x(),crystal.getPosition().y(),260))local="ambience.local_crystal";
        if(area==Area.Type.VILLAGE&&current.restored(FinalChapter.Restoration.CAMPFIRE)&&near(santa,VillageActivities.Site.CAMPFIRE.x,VillageActivities.Site.CAMPFIRE.y,240))local="ambience.local_village_campfire";
        if(area==Area.Type.CHRISTMAS_TOWN&&!story.isBossDefeated()&&falling.isEmpty()&&near(santa,1296,1008,360))local="ambience.local_titan_ritual";
        audio.playLocal(0,state==GamePanel.State.MAIN_MENU?null:local);audio.playLocal(1,null);
        lastX=santa.getX();lastY=santa.getY();lastArea=area;lastState=state;lastMode=ui.mode();lastLetterIndex=ui.letterIndex();
        deliveries=village.deliveries();home=current.homeReturned();letter=current.letterFinished();fangWon=story.isFrostFangDefeated();titanWon=story.isBossDefeated();lastEnding=ending.getState();fallActive=!falling.isEmpty();dialogue=speaking;introActive=intro;
    }
    private static boolean near(Santa santa,double x,double y,double radius){return Math.hypot(santa.getX()+16-x,santa.getY()+21-y)<radius;}
    /** Authoring footprints only; no terrain pixel reads or movement/collision changes. */
    public static String surface(Area.Type area,Santa santa){
        double x=santa.getX()+16,y=santa.getY()+42;
        if(area==Area.Type.SANTA_BASE&&x>=224&&x<1376&&y>=160&&y<928)return "footstep.wood";
        if(area==Area.Type.VILLAGE&&VillageEnvironmentLayers.SQUARE.contains(x,y))return "footstep.stone";
        if(area==Area.Type.CHRISTMAS_TOWN&&BossArenaVisuals.arenaBounds().contains(x,y))return "footstep.stone";
        return "footstep.snow";
    }
}
