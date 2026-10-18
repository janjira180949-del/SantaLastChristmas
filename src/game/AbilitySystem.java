package game;

import entity.Enemy;
import entity.Santa;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/** Christmas Bell: one bounded pulse with collision-aware knockback. */
public final class AbilitySystem {
    public static final int BELL_COOLDOWN = 90;
    public static final int BELL_DAMAGE = 75;
    public AbilitySystem() { SkillArtwork.prepare(); }
    private final SantaSkills skills = new SantaSkills();
    public SantaSkills skills() { return skills; }
    public void clearEffects() { effectFrames=0;skills.clearEffects(); }
    private int bellCooldown, effectFrames;
    private double effectX, effectY;
    public int getBellCooldownFrames() { return bellCooldown; }
    public void reset() { bellCooldown = effectFrames = 0; skills.reset(); }
    public void update() {
        if (bellCooldown > 0) bellCooldown--;
        if (effectFrames > 0) effectFrames--;
    }
    public boolean use(int slot, Santa santa, List<Enemy> enemies) {
        return use(slot, santa, enemies, List.of());
    }
    public boolean use(int slot, Santa santa, List<Enemy> enemies,
            Iterable<? extends Rectangle2D> solids) {
        if (slot != 1 || bellCooldown > 0 || santa.isDefeated()) return false;
        effectX=santa.getX()+16; effectY=santa.getY()+21;
        bellCooldown=BELL_COOLDOWN; effectFrames=24;skills.audioEvent("bell.gather");
        for (Enemy enemy : enemies) {
            if (!enemy.isDefeated() && Math.hypot(enemy.getX()+enemy.getSize()/2.0-effectX,
                    enemy.getY()+enemy.getSize()/2.0-effectY) <= 125) {
                if(enemy.takeDamage(BELL_DAMAGE)){skills.audioEvent("bell.hit");skills.impact(enemy.getX()+enemy.getSize()/2.0,enemy.getY()+enemy.getSize()/2.0);}
                enemy.knockBack(effectX, effectY, enemy instanceof boss.ChristmasEater || enemy instanceof boss.BossCombatant ? 16 : 55, solids);
            }
        }
        return true;
    }
    public void draw(Graphics2D g) {
        skills.draw(g);
        if (effectFrames <= 0) return;
        SkillArtwork.bell(g,effectX,effectY,effectFrames);
    }
}
