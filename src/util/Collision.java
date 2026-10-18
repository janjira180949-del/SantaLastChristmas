package util;

import java.awt.geom.Rectangle2D;

/** Reusable geometry checks for solid game objects. */
public final class Collision {

    private Collision() {
    }

    /**
     * Checks whether a rectangular actor bounds intersects any solid area.
     *
     * @param bounds the actor bounds to test
     * @param solidAreas the obstacles and boundaries in the world
     * @return true if the bounds overlap at least one solid area
     */
    public static boolean intersectsAny(Rectangle2D bounds,
            Iterable<? extends Rectangle2D> solidAreas) {
        for (Rectangle2D solidArea : solidAreas) {
            if (bounds.intersects(solidArea)) {
                return true;
            }
        }
        return false;
    }
}