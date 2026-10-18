package boss;

import java.awt.geom.Rectangle2D;

/** Presentation and swept movement shared by the two independent encounter actors. */
public interface BossCombatant {
    String getBossName();
    int getPhaseIndex();
    BossAttacks attacks();
    default String getCombatStatus(){return getTransitionFrames()>0?"Phase shifting - prepare":attacks().status();}
    String getAnimationName();
    int getAnimationFrame();
    boolean moveAttack(double dx,double dy,Iterable<? extends Rectangle2D> solids);
    default long getFootstepSequence(){return 0;}
    default int getTransitionFrames(){return 0;}
}
