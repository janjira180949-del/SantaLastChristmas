package audio;

import boss.ChristmasEater;
import entity.Enemy;
import entity.Santa;
import quest.StoryQuest;
import quest.StoryQuestManager;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import static audio.AudioCatalog.Effect.*;

/** Presentation observer. Snapshots existing state; never changes an event outcome.
 * Fixed actor slots, no files, decoding, timers or render notifications.
 */
public final class GameplayAudio {
    private final BiConsumer<AudioCatalog.Effect,Double> playback;
    private final Enemy[] actors=new Enemy[16];
    private final int[] health=new int[16];
    private final long[] attacks=new long[16];
    private final ChristmasEater.Phase[] phases=new ChristmasEater.Phase[16];
    private final int[] bossPhases=new int[16];
    private Santa santa;
    private int santaHealth,count,completedQuests,restorationCues;
    private Consumer<String> events,stopOwner;
    private BiConsumer<String,String> owned;
    private final boolean[] warned=new boolean[16],executing=new boolean[16],recovering=new boolean[16],projectiles=new boolean[16];
    private final boss.BossAttacks.Attack[] patterns=new boss.BossAttacks.Attack[16];
    private final int[] ages=new int[16];private final long[] footfalls=new long[16];
    private final boolean[] contacts=new boolean[16];
    private final java.util.IdentityHashMap<Enemy,Long> regularHurts=new java.util.IdentityHashMap<>();
    private long activeUpdates,lastFangHurt=-100,lastTitanHurt=-100,lastMiniHurt=-100;
    /** Called only after the basic attack damage API accepts contact. */
    public void swordContact(Enemy enemy){if(events!=null)events.accept(enemy instanceof boss.BossCombatant||enemy instanceof ChristmasEater?"sword.armor":"sword.body");}
    public void bindEvents(Consumer<String> events,BiConsumer<String,String> owned,Consumer<String> stop){this.events=events;this.owned=owned;this.stopOwner=stop;}
    public GameplayAudio(Consumer<AudioCatalog.Effect> playback) {
        Objects.requireNonNull(playback);this.playback=(effect,gain)->playback.accept(effect);
    }
    public GameplayAudio(BiConsumer<AudioCatalog.Effect,Double> playback) {this.playback=Objects.requireNonNull(playback);}
    public void play(AudioCatalog.Effect effect) {playback.accept(effect,1.0);}
    public void playWorld(AudioCatalog.Effect effect,Santa player,double x,double y) {
        playback.accept(effect,AudioPolish.distanceGain(player.getX()+16,player.getY()+21,x,y));
    }
    public void reset() {clearCombat();completedQuests=0;restorationCues=0;activeUpdates=0;regularHurts.clear();lastFangHurt=lastTitanHurt=lastMiniHurt=-100;}
    public void captureCombat(Santa player,List<Enemy> enemies) {
        activeUpdates++;regularHurts.keySet().removeIf(e->!enemies.contains(e));
        clearCombat();santa=player;santaHealth=player.getHealth();count=Math.min(actors.length,enemies.size());
        for(int i=0;i<count;i++) {
            Enemy e=enemies.get(i);actors[i]=e;health[i]=e.getHealth();attacks[i]=e.getAttackEventSequence();
            phases[i]=e instanceof ChristmasEater boss?boss.getPhase():null;
            bossPhases[i]=e instanceof boss.BossCombatant actor?actor.getPhaseIndex():-1;
            warned[i]=e.isWarning();
            if(e instanceof boss.BossCombatant actor){var a=actor.attacks();patterns[i]=a.getAttack();executing[i]=a.isExecuting();recovering[i]=a.isRecovering();ages[i]=a.getExecutionAge();projectiles[i]=a.isProjectileActive();footfalls[i]=actor.getFootstepSequence();contacts[i]=a.hasHit();}
        }
    }
    /** Resolve in update's finally block, including lethal early returns and removed actors. */
    public void resolveCombat() {
        if(santa==null)return;
        if(santa.getHealth()<santaHealth)play(SANTA_DAMAGE);
        if(events!=null&&santaHealth>0&&santa.isDefeated())events.accept("santa.defeat");
        for(int i=0;i<count;i++) {
            Enemy e=actors[i];boolean isBoss=e instanceof ChristmasEater||e instanceof boss.BossCombatant;
            if(events!=null){
                if(e instanceof boss.BossCombatant actor){resolveBoss(i,e,actor);continue;}
                if(!isBoss){
                    boolean mini=e instanceof entity.MiniFrostFang;
                    String kind=mini?"mini_fang":e instanceof entity.Runner?"runner":"walker";
                    if(!warned[i]&&e.isWarning())events.accept(kind+".warning");
                    if(e.getAttackEventSequence()!=attacks[i])events.accept(kind+".strike");
                    if(e.getHealth()<health[i]){if(mini?activeUpdates-lastMiniHurt>=12:activeUpdates-regularHurts.getOrDefault(e,-100L)>=6){events.accept(kind+".hurt");if(mini)lastMiniHurt=activeUpdates;else regularHurts.put(e,activeUpdates);}if(e.isDefeated())events.accept(kind+".defeat");}
                    continue;
                }
            }
            if(isBoss&&e.getAttackEventSequence()!=attacks[i])play(BOSS_ATTACK);
            if(e.getHealth()<health[i]) {
                if(isBoss)play(BOSS_DAMAGE);else playWorld(ENEMY_HIT,santa,e.getX()+e.getSize()/2.0,e.getY()+e.getSize()/2.0);
                if(e.isDefeated()) {
                    if(isBoss)play(BOSS_DEFEAT);else playWorld(ENEMY_DEFEAT,santa,e.getX()+e.getSize()/2.0,e.getY()+e.getSize()/2.0);
                }
            }
            if(e instanceof boss.BossCombatant actor){if(actor.getPhaseIndex()!=bossPhases[i])play(BOSS_PHASE_TRANSITION);}
            else if(e instanceof ChristmasEater actor&&actor.getPhase()!=phases[i])play(BOSS_PHASE_TRANSITION);
        }
        clearCombat();
    }
    private static String attackId(boss.BossAttacks.Attack attack){return switch(attack){
        case CLAW->"fang.claw";case ICE_BLAST->"fang.ice_blast";case FROST_WAVE->"fang.frost_wave";case FROST_DASH->"fang.frost_dash";
        case GROUND_SLAM->"titan.slam";case ICE_SPIKES->"titan.spikes";case BEAST_CHARGE->"titan.charge";case DARK_FROST_BEAM->"titan.beam";case BLACK_FROST_STORM->"titan.storm";default->"";};}
    private void resolveBoss(int i,Enemy enemy,boss.BossCombatant actor){
        String kind=enemy instanceof boss.FrostFang?"fang":"titan";var a=actor.attacks();var pattern=a.getAttack();String id=attackId(pattern);
        if(enemy.getHealth()<health[i]){boolean fang=enemy instanceof boss.FrostFang;if(activeUpdates-(fang?lastFangHurt:lastTitanHurt)>=(fang?12:18)){events.accept(kind+".hurt");if(fang)lastFangHurt=activeUpdates;else lastTitanHurt=activeUpdates;}if(enemy.isDefeated())stopOwner.accept("boss-attack"); // Fall impact belongs to the authoritative cinematic contact frame.
        }
        if(!enemy.isDefeated()&&actor.getPhaseIndex()!=bossPhases[i]){stopOwner.accept("boss-attack");events.accept(kind+".phase"+(actor.getPhaseIndex()+1));}
        if(actor.getFootstepSequence()!=footfalls[i]&&pattern!=boss.BossAttacks.Attack.BEAST_CHARGE)events.accept("titan.foot_contact");
        if(pattern==boss.BossAttacks.Attack.CLAW&&!contacts[i]&&a.hasHit())events.accept("fang.claw.hit");
        if(pattern!=patterns[i]){
            stopOwner.accept("boss-attack");
            if(pattern!=boss.BossAttacks.Attack.NONE){events.accept(id+".warning");if(pattern==boss.BossAttacks.Attack.BLACK_FROST_STORM)events.accept("titan.storm.gather");}
        }
        if(a.isExecuting()&&(!executing[i]||pattern!=patterns[i])){
            events.accept(id+".execute");
            switch(pattern){
                case ICE_SPIKES->events.accept("titan.spikes.emerge");case GROUND_SLAM->events.accept("titan.slam.wave");
                case BEAST_CHARGE->events.accept("titan.charge.launch");
                case DARK_FROST_BEAM->{events.accept("titan.beam.start");owned.accept("titan.beam.sustain","boss-attack");}
                case BLACK_FROST_STORM->owned.accept("titan.storm.sustain","boss-attack");
                case FROST_WAVE->owned.accept("fang.frost_wave.sustain","boss-attack");
                case FROST_DASH->owned.accept("fang.frost_dash.sustain","boss-attack");default->{}
            }
        }
        if(pattern==boss.BossAttacks.Attack.BEAST_CHARGE&&ages[i]<15&&a.getExecutionAge()>=15)events.accept("titan.charge.impact");
        if(pattern==boss.BossAttacks.Attack.ICE_BLAST&&projectiles[i]&&!a.isProjectileActive())events.accept("fang.ice_blast.impact");
        if(a.isRecovering()&&!recovering[i]){stopOwner.accept("boss-attack");switch(pattern){case DARK_FROST_BEAM->events.accept("titan.beam.end");case BLACK_FROST_STORM->events.accept("titan.storm.end");case ICE_SPIKES->events.accept("titan.spikes.collapse");default->{}}}
    }
    private void clearCombat() {
        for(int i=0;i<count;i++){actors[i]=null;phases[i]=null;}
        santa=null;count=0;
    }
    /** Completion IDs survive Retry/area travel; reset only for a new adventure. */
    public void observeQuests(StoryQuestManager story) {
        for(int i=0;i<story.getQuests().size();i++) {
            StoryQuest quest=story.getQuests().get(i);int bit=1<<quest.getId().ordinal();
            if(quest.getState()==StoryQuest.State.COMPLETED&&(completedQuests&bit)==0){completedQuests|=bit;play(QUEST_COMPLETE);}
        }
    }
    /** Uses the authoritative transformation clock. No additional timer. */
    public void restorationTick(int tick) {
        int bit;AudioCatalog.Effect effect;
        switch(tick) {
            case 0 -> {bit=1;effect=SPIRIT_TREE_RESTORATION;}
            case 30 -> {bit=2;effect=SPIRIT_TREE_RISING;}
            case 90 -> {bit=4;effect=SPIRIT_TREE_BURST;}
            case 120 -> {bit=8;effect=SPIRIT_TREE_CHIME;}
            case 180 -> {if((restorationCues&16)==0){restorationCues|=16;if(events!=null)events.accept("tree.complete");}return;}
            default -> {return;}
        }
        if((restorationCues&bit)==0){restorationCues|=bit;play(effect);}
    }
}
