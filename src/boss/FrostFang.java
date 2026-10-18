package boss;

import entity.Santa;
import java.awt.*;
import java.awt.geom.Rectangle2D;

/** Christmas Eater's tested health/phase/damage model, repurposed as a Forest predator. */
public final class FrostFang extends ChristmasEater implements BossCombatant {
    public static final game.WorldMap.Point ARENA=new game.WorldMap.Point(2592,1248);
    private final BossAttacks attacks=new BossAttacks();
    private int ticks,entrance=45,transition,cooldown=70,choice;
    private boolean facingRight;
    private java.util.function.IntSupplier reinforcementWarning=()->0;
    public FrostFang(double x,double y){super(x,y);BossArtwork.prepareAsync();}
    public FrostFang(double x,double y,java.util.function.IntSupplier warning){this(x,y);reinforcementWarning=warning;}
    @Override public String getCombatStatus(){return reinforcementWarning.getAsInt()>0?"Frost shifts - two reinforcements approach":BossCombatant.super.getCombatStatus();}
    public String getBossName(){return "FROST FANG";}public int getPhaseIndex(){return getPhase().ordinal();}
    public BossAttacks attacks(){return attacks;}public int getTransitionFrames(){return transition;}
    @Override public boolean takeDamage(int damage){int old=getPhaseIndex();boolean hit=super.takeDamage(damage);if(isDefeated())attacks.clear();else if(old!=getPhaseIndex()){attacks.clear();transition=60;cooldown=80;choice=getPhaseIndex();}return hit;}
    @Override public void update(Santa santa,Iterable<? extends Rectangle2D> solids){
        if(isDefeated()||santa.isDefeated())return;ticks++;facingRight=santa.getX()+16>getX()+38;
        if(entrance>0){entrance--;return;}if(transition>0){transition--;return;}
        if(attacks.getAttack()!=BossAttacks.Attack.NONE){if(attacks.update(this,santa,solids))notifyAttackExecuted();return;}
        double distance=distanceTo(santa.getX()+16,santa.getY()+21);
        if(--cooldown<=0){
            BossAttacks.Attack attack;if(distance<110)attack=BossAttacks.Attack.CLAW;
            else {int n=choice++%(getPhaseIndex()+1);attack=n==0?BossAttacks.Attack.ICE_BLAST:n==1?BossAttacks.Attack.FROST_WAVE:BossAttacks.Attack.FROST_DASH;}
            attacks.begin(attack,this,santa,solids);cooldown=getPhaseIndex()==0?90:65;
        }else if(distance>75)moveToward(santa.getX()+16,santa.getY()+21,solids);
    }
    @Override public boolean tryAttackSanta(Santa santa){return false;}
    public boolean moveAttack(double dx,double dy,Iterable<? extends Rectangle2D> solids){return moveSwept(dx,dy,solids);}
    public String getAnimationName(){if(isDefeated())return "defeat";if(entrance>0)return "entrance";if(getHitFlashFrames()>0)return "hurt";
        return switch(attacks.getAttack()){case CLAW->"claw_swipe";case ICE_BLAST->"ice_blast";case FROST_WAVE->"frost_wave";case FROST_DASH->"frost_dash";default->getVisualState()==entity.Enemy.VisualState.MOVING?(getPhaseIndex()>0?"run":"walk"):"idle";};}
    public int getAnimationFrame(){
        if(entrance>0)return Math.min(7,(45-entrance)*8/45);
        if(getHitFlashFrames()>0)return Math.min(7,8-getHitFlashFrames());
        if(attacks.getAttack()!=BossAttacks.Attack.NONE)return attacks.getFangAnimationFrame();
        return ticks/(getAnimationName().equals("run")?4:6)%8;
    }
    @Override public void drawAttackWarning(Graphics2D g){if(!attacks.isExecuting())attacks.draw(g);}
    @Override public void draw(Graphics2D g){
        BossArtwork.drawFang(g,getAnimationName(),getAnimationFrame(),getX()+38,getY()+76,facingRight);
        // The enlarged body must not hide a projectile or swipe at its unchanged origin.
        if(attacks.isExecuting())attacks.draw(g);
        if(transition>0)BossArtwork.effect(g,"wave",ticks/4,getX()+38,getY()+48,112,112,0);}
    /** Presentation-only facing snapshot; no combat state changes. */
    public boolean isFacingRight(){return facingRight;}
    public void drawDefeat(Graphics2D g,int frame){BossArtwork.drawDefeat(g,true,frame,getX()+38,getY()+76,facingRight);BossArtwork.effect(g,"defeat",Math.min(11,frame/6),getX()+38,getY()+58,180,120,0);}
}
