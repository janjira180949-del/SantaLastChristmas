package entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

/** Stationary Town Keeper; uses the same 58-pixel E proximity as the children. */
public final class Elias {
    public enum Direction { DOWN, UP, LEFT, RIGHT }
    private static final EliasSprites SPRITES=new EliasSprites();
    private final int x, y;
    /** Compatibility with the existing visual clock: supplied poses are static. */
    public void updateVisual() { }

    public Elias(int x, int y) { this.x = x; this.y = y; }
    public int getX() { return x; }
    public int getY() { return y; }
    public Rectangle2D getBounds() { return new Rectangle2D.Double(x, y, 32, 48); }
    public boolean canInteract(Santa santa) {
        return !santa.isDefeated() && Math.hypot(santa.getX() + 16.0 - (x + 16),
                santa.getY() + 21.0 - (y + 24)) <= 58;
    }
    public void drawCinematic(Graphics2D graphics,double feetX,double feetY) {
        drawCinematic(graphics,feetX,feetY,Direction.DOWN);
    }
    public void drawCinematic(Graphics2D graphics,double feetX,double feetY,Direction direction) {
        SPRITES.draw(graphics,direction,feetX,feetY);
    }
    /** Face a nearby speaker using static art; never mutate position or collision. */
    public Direction getFacingDirection(Santa santa) {
        if(!canInteract(santa))return Direction.DOWN;
        double dx=santa.getX()+16.0-(x+16),dy=santa.getY()+21.0-(y+24);
        if(Math.abs(dx)>Math.abs(dy))return dx<0?Direction.LEFT:Direction.RIGHT;
        return dy<0?Direction.UP:Direction.DOWN;
    }
    public void draw(Graphics2D g, Santa santa) {
        g.setColor(new Color(20,25,32,75));g.fillOval(x-2,y+43,36,9);
        SPRITES.draw(g,getFacingDirection(santa),x+16,y+48);
        g.setColor(new Color(255, 225, 145));
        g.drawString("Elias - Town Keeper", x - 40, y - 14);
        if (canInteract(santa)) g.drawString("[E] Talk to Elias", x - 29, y - 32);
    }
}
