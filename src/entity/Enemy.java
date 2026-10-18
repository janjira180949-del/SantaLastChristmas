package entity;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import util.Collision;

/** Shared state and collision-aware movement for enemies. */
public abstract class Enemy {

    public enum VisualState { IDLE, MOVING, ATTACK }
    private boolean movedThisFrame;
    private double lastMoveX;
    private double lastMoveY = 1.0;
    private int movementAnimationTicks;

    /** Observes existing movement and attack cooldown; never changes combat timing. */
    public final VisualState getVisualState() {
        if (isWarning() || attackCooldownFrames > Math.max(0, getAttackCooldown() - 12)) {
            return VisualState.ATTACK;
        }
        return movedThisFrame ? VisualState.MOVING : VisualState.IDLE;
    }

    /** Visual-only health bar anchor; placeholder enemies retain their old position. */
    public double getHealthBarY() { return getY() - 7; }

    private double x;
    private double y;
    private final int maximumHealth;
    private int health;
    private final double speed;
    private final int size;
    private int attackCooldownFrames;
    private int hitFlashFrames;
    private int giftSlowFrames;
    /** Presentation-independent, finite Gift Bomb slow; every boss is explicitly excluded. */
    public final void applyGiftSlow(int frames) {
        if(!(this instanceof boss.ChristmasEater)&&!(this instanceof boss.BossCombatant)&&!isDefeated())
            giftSlowFrames=Math.max(giftSlowFrames,Math.max(0,Math.min(90,frames)));
    }
    public final int getGiftSlowFrames(){return giftSlowFrames;}
    private int warningFrames, hitStunFrames;
    private boolean attackReady;
    // Presentation sequence only; never participates in combat calculations.
    private long attackEventSequence;
    public final long getAttackEventSequence() { return attackEventSequence; }
    protected final void notifyAttackExecuted() { attackEventSequence++; }
    public boolean isWarning() { return warningFrames > 0; }
    protected final void cancelMeleeWarning() {
        warningFrames = 0; attackReady = false;
        attackCooldownFrames = Math.max(attackCooldownFrames, getAttackCooldown());
    }
    protected int getWarningDuration() { return 30; }
    protected double getDetectionRange() { return 640; }
    public int getAttackCooldownFrames() { return attackCooldownFrames; }

    public final void knockBack(double fromX, double fromY, double distance,
            Iterable<? extends Rectangle2D> solids) {
        double dx=x+size/2.0-fromX, dy=y+size/2.0-fromY, length=Math.hypot(dx,dy);
        if (length == 0) { dx=1; length=1; }
        int steps=(int)Math.ceil(distance);
        for(int i=0;i<steps;i++) {
            double sx=dx/length, sy=dy/length;
            if(!Collision.intersectsAny(getBoundsAt(x+sx,y),solids)) x+=sx;
            if(!Collision.intersectsAny(getBoundsAt(x,y+sy),solids)) y+=sy;
        }
    }
    public void drawAttackWarning(Graphics2D g) {
        if (!isWarning()) return;
        double radius=getAttackRange();
        g.setColor(new java.awt.Color(245, 75, 85, 65));
        g.fill(new java.awt.geom.Ellipse2D.Double(x+size/2.0-radius,y+size/2.0-radius,radius*2,radius*2));
        g.setColor(new java.awt.Color(255, 155, 120));
        g.drawString("!", (int)x+size/2, (int)y-14);
    }

    protected Enemy(double x, double y, int health, double speed, int size) {
        if (health <= 0 || speed <= 0 || size <= 0) {
            throw new IllegalArgumentException("Enemy stats must be positive");
        }
        this.x = x;
        this.y = y;
        this.maximumHealth = health;
        this.health = health;
        this.speed = speed;
        this.size = size;
    }

    /** Moves this enemy toward Santa, avoiding solid map rectangles. */
    public void update(Santa santa, Iterable<? extends Rectangle2D> solidAreas) {
        movedThisFrame = false;
        if (isDefeated() || santa.isDefeated() || isWarning() || attackReady || hitStunFrames > 0) return;
        double targetX = santa.getX() + 16.0;
        double targetY = santa.getY() + 21.0;
        if (distanceTo(targetX, targetY) <= getDetectionRange() && distanceTo(targetX, targetY) > getAttackRange()) {
            moveToward(targetX, targetY, solidAreas);
        }
    }

    /** Advances shared combat timers once per game frame. */
    public final void advanceCombatTimers() {
        if(giftSlowFrames>0)giftSlowFrames--;
        movedThisFrame = false;
        if (hitStunFrames > 0) hitStunFrames--;
        if (warningFrames > 0 && --warningFrames == 0) attackReady = true;
        if (hitFlashFrames > 0) {
            hitFlashFrames--;
        }
        if (attackCooldownFrames > 0) {
            attackCooldownFrames--;
        }
    }

    /** Attempts one close-range attack; cooldown prevents per-frame damage. */
    public boolean tryAttackSanta(Santa santa) {
        if (isDefeated() || santa.isDefeated() || hitStunFrames > 0 || attackCooldownFrames > 0) return false;
        double distance=distanceTo(santa.getX()+16, santa.getY()+21);
        if (attackReady) {
            attackReady=false; attackCooldownFrames=getAttackCooldown();
            notifyAttackExecuted();
            return distance<=getAttackRange() && santa.takeDamage(getAttackDamage());
        }
        if (!isWarning() && distance<=getAttackRange()) warningFrames=getWarningDuration();
        return false;
    }

    protected double getAttackRange() {
        return 43.0;
    }

    protected int getAttackDamage() {
        return 8;
    }

    protected int getAttackCooldown() {
        return 55;
    }

    public boolean takeDamage(int damage) {
        if (damage <= 0 || health <= 0) {
            return false;
        }
        health = Math.max(0, health - damage);
        hitFlashFrames = 8;
        hitStunFrames = 6;
        warningFrames = 0; attackReady = false;
        attackCooldownFrames = Math.max(attackCooldownFrames, 18);
        return true;
    }

    public boolean isDefeated() {
        return health <= 0;
    }

    public int getHitFlashFrames() {
        return hitFlashFrames;
    }

    public void drawHitFeedback(Graphics2D graphics) {
        if (hitFlashFrames > 0 && health > 0) {
            Graphics2D g=(Graphics2D)graphics.create();
            g.setColor(new java.awt.Color(255,228,183,hitFlashFrames*23));
            int cx=(int)x+size/2,cy=(int)y+size/2;
            for(int i=0;i<4;i++){double a=i*Math.PI/2+.4;int r=16+(8-hitFlashFrames)*2;g.drawLine(cx+(int)(Math.cos(a)*r),cy+(int)(Math.sin(a)*r),cx+(int)(Math.cos(a)*(r+4)),cy+(int)(Math.sin(a)*(r+4)));}
            g.dispose();
        }
    }

    /** Moves toward a world position while respecting the existing map solids. */
    protected final void moveToward(double targetX, double targetY,
            Iterable<? extends Rectangle2D> solidAreas) {
        double dx = targetX - (x + size / 2.0);
        double dy = targetY - (y + size / 2.0);
        double distance = Math.hypot(dx, dy);
        if (distance == 0) {
            return;
        }

        double movementDistance = Math.min(speed * movementMultiplier() * (giftSlowFrames>0?.65:1), distance);
        double movementX = dx / distance * movementDistance;
        double movementY = dy / distance * movementDistance;

        double previousX = x;
        double previousY = y;
        // Check each axis separately, allowing movement to slide along walls.
        if (!Collision.intersectsAny(getBoundsAt(x + movementX, y), solidAreas)) {
            x += movementX;
        }
        if (!Collision.intersectsAny(getBoundsAt(x, y + movementY), solidAreas)) {
            y += movementY;
        }
        boolean moved = x != previousX || y != previousY;
        movedThisFrame |= moved;
        if (moved) {
            lastMoveX = x - previousX;
            lastMoveY = y - previousY;
            movementAnimationTicks = (movementAnimationTicks + 1) % 32;
        } else if (!movedThisFrame) {
            movementAnimationTicks = 0;
        }
    }

    protected double movementMultiplier() { return 1.0; }

    /** Swept boss lunges reuse the authoritative footprint, with at most one pixel per probe. */
    protected final boolean moveSwept(double dx,double dy,Iterable<? extends Rectangle2D> solids) {
        int steps=Math.max(1,(int)Math.ceil(Math.max(Math.abs(dx),Math.abs(dy))));
        double sx=dx/steps,sy=dy/steps;boolean moved=false;
        for(int i=0;i<steps;i++) {
            if(Collision.intersectsAny(getBoundsAt(x+sx,y+sy),solids))break;
            x+=sx;y+=sy;moved=true;
        }
        if(moved){movedThisFrame=true;lastMoveX=dx;lastMoveY=dy;movementAnimationTicks=(movementAnimationTicks+1)%32;}
        return moved;
    }

    protected final double distanceTo(double targetX, double targetY) {
        return Math.hypot(targetX - (x + size / 2.0),
                targetY - (y + size / 2.0));
    }


    public final String getMovementDirection() {
        if (Math.abs(lastMoveX) > Math.abs(lastMoveY)) return lastMoveX < 0 ? "left" : "right";
        return lastMoveY < 0 ? "up" : "down";
    }

    public final int getMovementAnimationFrame() { return movementAnimationTicks / 8; }

    public abstract void draw(Graphics2D graphics);

    public final double getX() {
        return x;
    }

    public final double getY() {
        return y;
    }

    public final int getHealth() {
        return health;
    }

    public final int getMaximumHealth() {
        return maximumHealth;
    }

    public final double getSpeed() {
        return speed;
    }

    public final int getSize() {
        return size;
    }

    public final Rectangle2D getBounds() {
        return getBoundsAt(x, y);
    }

    private Rectangle2D getBoundsAt(double candidateX, double candidateY) {
        return new Rectangle2D.Double(candidateX, candidateY, size, size);
    }
}
