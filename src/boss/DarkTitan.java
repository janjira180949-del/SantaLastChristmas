package boss;

import entity.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;

/** Independent final boss: no Frost Fang flags, phase clocks or attacks are shared. */
public final class DarkTitan extends Enemy implements BossCombatant {
    public static final int HEALTH=1200,SIZE=96;
    private final BossAttacks attacks=new BossAttacks();
    private int phase,ticks,walkTicks,entrance=60,transition,cooldown=90,choice,stepEffect;
    private long footstepSequence;
    public DarkTitan(double x,double y){super(x,y,HEALTH,.7,SIZE);BossArtwork.prepareAsync();}
    public String getBossName(){return "DARK TITAN";}public int getPhaseIndex(){return phase;}public BossAttacks attacks(){return attacks;}
    public int getTransitionFrames(){return transition;}public long getFootstepSequence(){return footstepSequence;}
    public int getWalkTicks(){return walkTicks;}
    @Override public boolean takeDamage(int damage){boolean hit=super.takeDamage(damage);int next=getHealth()<=HEALTH/4?2:getHealth()<=HEALTH/2?1:0;
        if(isDefeated())attacks.clear();else if(next!=phase){phase=next;transition=72;attacks.clear();cooldown=100;choice=0;}return hit;}
    @Override protected double movementMultiplier(){return phase==0?1:phase==1?1.4:1.8;}
    @Override public void update(Santa santa,Iterable<? extends Rectangle2D> solids){
        if(isDefeated()||santa.isDefeated())return;ticks++;if(stepEffect>0)stepEffect--;
        if(entrance>0){entrance--;return;}if(transition>0){transition--;return;}
        if(attacks.getAttack()!=BossAttacks.Attack.NONE){if(attacks.update(this,santa,solids))notifyAttackExecuted();return;}
        if(--cooldown<=0){
            var pattern=phase==0?new BossAttacks.Attack[]{BossAttacks.Attack.GROUND_SLAM,BossAttacks.Attack.ICE_SPIKES}:phase==1?new BossAttacks.Attack[]{BossAttacks.Attack.BEAST_CHARGE,BossAttacks.Attack.GROUND_SLAM,BossAttacks.Attack.DARK_FROST_BEAM,BossAttacks.Attack.GROUND_SLAM,BossAttacks.Attack.ICE_SPIKES}:new BossAttacks.Attack[]{BossAttacks.Attack.BLACK_FROST_STORM,BossAttacks.Attack.GROUND_SLAM,BossAttacks.Attack.BEAST_CHARGE,BossAttacks.Attack.DARK_FROST_BEAM,BossAttacks.Attack.GROUND_SLAM,BossAttacks.Attack.ICE_SPIKES};
            attacks.begin(pattern[choice++%pattern.length],this,santa,solids);cooldown=phase==0?(pattern[choice%pattern.length]==BossAttacks.Attack.GROUND_SLAM?70:100):phase==1?80:65;
        }else if(distanceTo(santa.getX()+16,santa.getY()+21)>110){
            double ox=getX(),oy=getY();moveToward(santa.getX()+16,santa.getY()+21,solids);
            if(ox!=getX()||oy!=getY()){int old=walkTicks/5%16;walkTicks=(walkTicks+1)%80;int frame=walkTicks/5;if(frame!=old&&(frame==0||frame==8)){footstepSequence++;stepEffect=12;}}
        }
    }
    public boolean moveAttack(double dx,double dy,Iterable<? extends Rectangle2D> solids){return moveSwept(dx,dy,solids);}
    /** Encounter-owned summoning is admitted only between complete attacks. */
    public boolean canSummon(){return !isDefeated()&&entrance==0&&transition==0&&attacks.getAttack()==BossAttacks.Attack.NONE;}
    public void prepareSummon(){if(canSummon())cooldown=Math.max(cooldown,150);}
    @Override public boolean tryAttackSanta(Santa santa){return false;}
    public String getAnimationName(){if(isDefeated())return "defeat";if(entrance>0)return "entrance";if(transition>0)return "phase_transition";if(getHitFlashFrames()>0)return "hurt";
        return switch(attacks.getAttack()){case BEAST_CHARGE->"beast_charge";case GROUND_SLAM->"ground_slam";case ICE_SPIKES,DARK_FROST_BEAM,BLACK_FROST_STORM->"cast";default->getVisualState()==VisualState.MOVING?"heavy_walk":"idle";};}
    public int getAnimationFrame(){if(attacks.getAttack()==BossAttacks.Attack.NONE&&entrance>0)return Math.min(3,(60-entrance)/15);if(attacks.getAttack()==BossAttacks.Attack.NONE&&transition>0)return Math.min(3,(72-transition)/18);if(attacks.getAttack()==BossAttacks.Attack.BEAST_CHARGE)return Math.min(15,attacks.getTicks()/5);if(attacks.getAttack()!=BossAttacks.Attack.NONE)return attacks.getWarningFrames()>0?Math.min(1,attacks.getTicks()/25):attacks.isRecovering()?3:2;return getAnimationName().equals("heavy_walk")?walkTicks/5:ticks/8%4;}
    @Override public void drawAttackWarning(Graphics2D g){attacks.draw(g);if(stepEffect>0)BossArtwork.effect(g,"titan_footstep",12-stepEffect,getX()+48,getY()+96,150,70,0);}
    @Override public void draw(Graphics2D g){BossArtwork.draw(g,"titan",getAnimationName(),getAnimationFrame(),getX()+48,getY()+96);
        if(!isDefeated()&&phase>0)BossArtwork.effect(g,"titan_blast",9+ticks/8%3,getX()+48,getY()-20,phase==1?23:31,phase==1?23:31,0);
        if(transition>0)BossArtwork.effect(g,"titan_storm",ticks/4,getX()+48,getY()+5,150,150,0);}
    public void drawDefeat(Graphics2D g,int frame){BossArtwork.drawDefeat(g,false,frame,getX()+48,getY()+96,false);BossArtwork.effect(g,"titan_defeat",Math.min(11,frame/8),getX()+48,getY()+60,230,180,0);}
}
