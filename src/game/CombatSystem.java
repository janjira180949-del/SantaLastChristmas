package game;

import entity.Enemy;
import entity.Santa;
import java.util.Iterator;
import java.util.List;

/** Coordinates attacks between Santa and the polymorphic enemy collection. */
public final class CombatSystem {

    private CombatSystem() {
    }

    public static void update(Santa santa, List<Enemy> enemies) {
        update(santa, enemies, null);
    }

    /** Audio-only contact observer; the original combat transaction remains authoritative. */
    public static void update(Santa santa, List<Enemy> enemies, java.util.function.Consumer<Enemy> contact) {
        if (santa.isAttacking()) {
            for (Enemy enemy : enemies) {
                if (!enemy.isDefeated()
                        && !santa.hasHitDuringCurrentAttack(enemy)
                        && santa.getAttackBounds().intersects(enemy.getBounds())) {
                    santa.markHitDuringCurrentAttack(enemy);
                    boolean damaged = enemy.takeDamage(santa.getAttackDamage());
                    if (damaged && contact != null) contact.accept(enemy);
                }
            }
        }

        for (Enemy enemy : enemies) {
            enemy.tryAttackSanta(santa);
        }

        Iterator<Enemy> iterator = enemies.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().isDefeated()) {
                iterator.remove();
            }
        }
    }
}