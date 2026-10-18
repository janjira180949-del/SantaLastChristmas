package game;

import boss.ChristmasEater;
import entity.Enemy;
import entity.Runner;
import entity.Santa;
import entity.Walker;
import java.util.ArrayList;
import java.util.List;

/** Authored composition and one-time lifecycle of one survival encounter. */
public final class Encounter {
    public enum State { READY, ACTIVE, COMPLETE }
    public enum EnemyKind { WALKER, RUNNER, BOSS, FROST_FANG, DARK_TITAN }
    private final String id;
    private final Area.Type area;
    private final WorldMap.Point center;
    private final List<List<EnemyKind>> waves=new ArrayList<>();
    private final List<Enemy> members = new ArrayList<>();
    private State state = State.READY;
    private WorldMap.Point checkpoint;
    private int waveIndex,warningFrames,recoveryFrames;
    private boolean warnedStart,waiting,summonUsed;
    private int summonWarning;
    private List<Enemy> actorPool;
    public static final int TITAN_SUMMON_INTERVAL=600,MAX_MINI_FANGS=3;
    private int titanSummonClock,titanSummonWarning;
    private final List<Mist> mists=new ArrayList<>(8);
    private final List<entity.MiniFrostFang> pendingMinis=new ArrayList<>(2);
    private static final class Mist{double x,y;int age;Mist(double x,double y,int age){this.x=x;this.y=y;this.age=age;}}
    public int getSummonWarningFrames(){return titanSummonWarning;}
    public int getSummonClock(){return titanSummonClock;}
    public int getMistCount(){return mists.size();}
    private void mist(double x,double y,int age){if(mists.size()==8)mists.remove(0);mists.add(new Mist(x,y,age));}
    private void resetSummons(){titanSummonClock=titanSummonWarning=0;pendingMinis.clear();mists.clear();}
    private void preview(Santa santa,List<Enemy> enemies){
        int[][] offsets={{-105,-100},{105,-100},{-110,95},{115,95}};mists.clear();var kinds=waves.get(waveIndex);
        for(int i=0;i<kinds.size();i++){var k=kinds.get(i);if(k!=EnemyKind.WALKER&&k!=EnemyKind.RUNNER)continue;int size=k==EnemyKind.RUNNER?24:30;var p=EncounterSpawnPolicy.find(area,center.x()+offsets[i][0]-size/2,center.y()+offsets[i][1]-size/2,size,santa,enemies);if(p!=null)mist(p.x()+size/2.0,p.y()+size,0);}
    }
    private void advanceTitan(Santa santa,List<Enemy> enemies){
        if(waves.get(0).get(0)!=EnemyKind.DARK_TITAN)return;
        members.removeIf(e->e instanceof entity.MiniFrostFang&&e.isDefeated());
        var titan=members.stream().filter(e->e instanceof boss.DarkTitan).map(e->(boss.DarkTitan)e).findFirst().orElse(null);
        if(titan==null||titan.isDefeated()){pendingMinis.clear();titanSummonWarning=0;return;}
        if(titan.getTransitionFrames()>0){pendingMinis.clear();titanSummonWarning=0;mists.clear();return;}
        if(titanSummonWarning>0){
            if(--titanSummonWarning==0){mists.clear();for(var mini:pendingMinis){var p=EncounterSpawnPolicy.find(area,(int)mini.getX(),(int)mini.getY(),entity.MiniFrostFang.SIZE,santa,enemies);if(p!=null&&BossArenaVisuals.arenaBounds().contains(p.x(),p.y(),26,26)){var actor=new entity.MiniFrostFang(p.x(),p.y());members.add(actor);enemies.add(actor);mist(p.x()+13,p.y()+26,60);}}pendingMinis.clear();}return;
        }
        if(titan.getPhaseIndex()==0)return;
        titanSummonClock=Math.min(TITAN_SUMMON_INTERVAL,titanSummonClock+1);
        if(titanSummonClock<TITAN_SUMMON_INTERVAL||!titan.canSummon())return;
        long live=members.stream().filter(e->e instanceof entity.MiniFrostFang&&!e.isDefeated()).count();if(live>=MAX_MINI_FANGS)return;
        var reserved=new ArrayList<>(enemies);
        for(int i=0;i<Math.min(2,MAX_MINI_FANGS-live);i++){var p=EncounterSpawnPolicy.find(area,center.x()+(i==0?-180:180)-13,center.y()+50,26,santa,reserved);if(p==null||!BossArenaVisuals.arenaBounds().contains(p.x(),p.y(),26,26))continue;var mini=new entity.MiniFrostFang(p.x(),p.y());reserved.add(mini);pendingMinis.add(mini);mist(p.x()+13,p.y()+26,0);}
        if(!pendingMinis.isEmpty()){titan.prepareSummon();titanSummonWarning=60;titanSummonClock=0;}
    }
    public Encounter(String id, Area.Type area, WorldMap.Point center, EnemyKind... composition) {
        this.id=id; this.area=area; this.center=center; waves.add(List.of(composition));
        if (composition.length == 0 || composition.length > 4) throw new IllegalArgumentException("Empty encounter");
    }
    public Encounter withWave(EnemyKind... composition) {
        if(state!=State.READY||waves.size()>=2||composition.length==0||composition.length>4)throw new IllegalArgumentException("Bounded authored waves only");
        waves.add(List.of(composition));return this;
    }
    public Encounter warned() {warnedStart=true;return this;}
    public int getWaveCount(){return waves.size();}
    public int getWaveNumber(){return waveIndex+1;}
    public int getWarningFrames(){return warningFrames;}
    public int getRecoveryFrames(){return recoveryFrames;}
    public String getId() { return id; }
    public Area.Type getArea() { return area; }
    public WorldMap.Point getCenter() { return center; }
    public State getState() { return state; }
    public List<Enemy> getMembers() { return List.copyOf(members); }
    public boolean isNear(Santa santa) {
        return Math.hypot(santa.getX()+16-center.x(),santa.getY()+21-center.y()) <= 180;
    }
    public boolean start(Santa santa, List<Enemy> enemies) {
        if (state != State.READY) return false;
        checkpoint=new WorldMap.Point(santa.getX(),santa.getY());resetSummons();actorPool=enemies;summonUsed=false;summonWarning=0;
        state=State.ACTIVE;waveIndex=0;members.clear();recoveryFrames=0;waiting=warnedStart;
        warningFrames=warnedStart?60:0;
        if(waiting)preview(santa,enemies);else spawn(santa,enemies);return true;
    }
    private void spawn(Santa santa,List<Enemy> enemies) {
        mists.clear();
        int[][] offsets={{-105,-100},{105,-100},{-110,95},{115,95},{0,-140}};
        List<EnemyKind> composition=waves.get(waveIndex);
        for (int i=0;i<composition.size();i++) {
            EnemyKind kind=composition.get(i); int size=kind==EnemyKind.DARK_TITAN?96:kind==EnemyKind.BOSS||kind==EnemyKind.FROST_FANG?76:kind==EnemyKind.RUNNER?24:30;
            WorldMap.Point p=EncounterSpawnPolicy.find(area,center.x()+(kind==EnemyKind.DARK_TITAN?0:offsets[i][0])-size/2,
                    center.y()+(kind==EnemyKind.DARK_TITAN?0:offsets[i][1])-size/2,size,santa,enemies);
            if(p==null)continue;
            Enemy enemy=switch(kind) {case WALKER -> new Walker(p.x(),p.y());
                case RUNNER -> new Runner(p.x(),p.y()); case BOSS -> new ChristmasEater(p.x(),p.y());
                case FROST_FANG -> new boss.FrostFang(p.x(),p.y(),()->summonWarning);case DARK_TITAN -> new boss.DarkTitan(p.x(),p.y());};
            members.add(enemy); enemies.add(enemy);
            if(kind==EnemyKind.WALKER||kind==EnemyKind.RUNNER)mist(p.x()+size/2.0,p.y()+size,60);
        }
    }
    /** Called only while gameplay advances; dialogue/pause naturally freeze wave clocks. */
    public void advance(Santa santa,List<Enemy> enemies) {
        if(state!=State.ACTIVE)return;
        mists.removeIf(m->++m.age>=84);advanceTitan(santa,enemies);
        ChristmasEater boss=members.stream().filter(e->e instanceof ChristmasEater).map(e->(ChristmasEater)e).findFirst().orElse(null);
        if(boss!=null&&!boss.isDefeated()&&!summonUsed&&boss.getPhase()!=ChristmasEater.Phase.PHASE_1) {
            if(summonWarning==0)summonWarning=60;
            else if(--summonWarning==0) {
                summonUsed=true;
                for(int i=0;i<2;i++) {
                    WorldMap.Point p=EncounterSpawnPolicy.find(area,center.x()+(i==0?-230:230),center.y()+100,i==0?30:24,santa,enemies);
                    if(p==null)continue;
                    Enemy enemy=i==0?new Walker(p.x(),p.y()):new Runner(p.x(),p.y());members.add(enemy);enemies.add(enemy);
                }
            }
        }
        if(waiting) {
            if(recoveryFrames>0){if(--recoveryFrames==0){warningFrames=60;preview(santa,enemies);}return;}
            if(warningFrames>0&&--warningFrames>0)return;
            waiting=false;spawn(santa,enemies);return;
        }
        if(waveIndex+1<waves.size()&&members.stream().allMatch(Enemy::isDefeated)) {
            waveIndex++;waiting=true;recoveryFrames=60;warningFrames=0;
        }
    }
    public void drawWarning(java.awt.Graphics2D original) {
        if(summonWarning>0){var cue=(java.awt.Graphics2D)original.create();cue.setColor(new java.awt.Color(255,216,151));cue.drawString("BLACK FROST CALLS - TWO ZOMBIES APPROACH",center.x()-150,center.y()+210);cue.dispose();}
        for(var m:mists)Phase3Vfx.smoke(original,m.x,m.y,m.age,1,false);
    }
    public void drawSpawnForeground(java.awt.Graphics2D g){for(var m:mists)Phase3Vfx.smoke(g,m.x,m.y,m.age,1,true);}

    public void abandon(List<Enemy> enemies) {
        if(state!=State.ACTIVE)return;
        resetSummons();enemies.removeAll(members);members.clear();state=State.READY;waiting=false;summonUsed=false;summonWarning=0;warningFrames=recoveryFrames=waveIndex=0;actorPool=null;
    }
    public boolean finishIfCleared() {
        if(state==State.ACTIVE&&members.stream().anyMatch(e->(e instanceof ChristmasEater||e instanceof boss.BossCombatant)&&e.isDefeated())) {
            if(actorPool!=null)actorPool.removeAll(members);
            resetSummons();summonWarning=0;state=State.COMPLETE;return true;
        }
        if (state != State.ACTIVE || waiting || waveIndex+1<waves.size() || members.stream().anyMatch(e -> !e.isDefeated())) return false;
        resetSummons();state=State.COMPLETE; return true;
    }
    public boolean retry(Santa santa, List<Enemy> enemies) {
        if (state != State.ACTIVE) return false;
        resetSummons();enemies.removeAll(members);members.clear();summonUsed=false;summonWarning=0;waveIndex=0;recoveryFrames=0;waiting=warnedStart;warningFrames=warnedStart?60:0;
        santa.restoreForRetry(checkpoint.x(),checkpoint.y());if(waiting)preview(santa,enemies);else spawn(santa,enemies);return true;
    }
}
