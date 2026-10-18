package entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Set;
import util.Collision;

public class Santa {

    private static final int WIDTH = 32;
    private static final int HEIGHT = 42;

    public enum AnimationState { IDLE, WALK, ATTACK }
    public enum Direction { UP, DOWN, LEFT, RIGHT }

    private static final int WALK_FRAME_TICKS = 8;
    private static final SantaSprites SPRITES = new SantaSprites();
    private Direction facing = Direction.DOWN;
    private boolean moving;
    private int walkTicks;
    private int visualTicks;

    private int x;
    private int y;
    private final int speed;
    private double attackDirectionX;
    private double attackDirectionY = 1.0;
    private int health = 100;
    private volatile boolean developerTestMode;
    private int attackFramesRemaining;
    private int attackSequence;
    public int getAttackSequence() { return attackSequence; }
    private int attackCooldownFrames;
    private int damageCooldownFrames;
    public static final int DASH_COOLDOWN = 90;
    public static final int DASH_DURATION = 10;
    private int dashFrames, dashCooldown;
    private double dashX, dashY;
    private double movementRemainderX, movementRemainderY;

    public boolean startDash(boolean up, boolean down, boolean left, boolean right) {
        if (isDefeated() || dashCooldown > 0 || dashFrames > 0) return false;
        double dx = (right ? 1 : 0) - (left ? 1 : 0);
        double dy = (down ? 1 : 0) - (up ? 1 : 0);
        if (dx == 0 && dy == 0) {
            dx = facing == Direction.RIGHT ? 1 : facing == Direction.LEFT ? -1 : 0;
            dy = facing == Direction.DOWN ? 1 : facing == Direction.UP ? -1 : 0;
        }
        double length = Math.hypot(dx, dy);
        dashX = dx / length; dashY = dy / length;
        dashFrames = DASH_DURATION; dashCooldown = DASH_COOLDOWN;
        return true;
    }
    public boolean isDashing() { return dashFrames > 0; }
    public int getDashCooldownFrames() { return dashCooldown; }
    public void restoreForRetry(int x, int y) {
        setPosition(x, y); health = getMaximumHealth();
        attackFramesRemaining = attackCooldownFrames = damageCooldownFrames = 0;
        dashFrames = dashCooldown = 0;
        movementRemainderX = movementRemainderY = 0;
        stopForCutscene();
    }
    private void moveSafely(int dx, int dy, Iterable<? extends Rectangle2D> solids) {
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        for (int i = 1; i <= steps; i++) {
            int sx = (int)Math.round(dx * i / (double)steps) - (int)Math.round(dx * (i-1) / (double)steps);
            int sy = (int)Math.round(dy * i / (double)steps) - (int)Math.round(dy * (i-1) / (double)steps);
            if (!Collision.intersectsAny(getCollisionBoundsAt(x+sx,y), solids)) x += sx;
            if (!Collision.intersectsAny(getCollisionBoundsAt(x,y+sy), solids)) y += sy;
        }
    }
    private final Set<Enemy> hitEnemiesThisAttack = new HashSet<>();

    public Santa(int x, int y) {
        this.x = x;
        this.y = y;
        this.speed = 4;
    }

    public void update(boolean up, boolean down, boolean left, boolean right,
            Iterable<? extends Rectangle2D> solidAreas) {
        visualTicks++;
        if (isDefeated()) {
            attackFramesRemaining = 0;
            return;
        }

        if (attackCooldownFrames > 0) {
            attackCooldownFrames--;
        }
        if (attackFramesRemaining > 0) {
            attackFramesRemaining--;
            if (attackFramesRemaining == 0) {
                hitEnemiesThisAttack.clear();
            }
        }
        if (damageCooldownFrames > 0) {
            damageCooldownFrames--;
        }

        if (dashCooldown > 0) dashCooldown--;
        double dx = (right ? 1 : 0) - (left ? 1 : 0);
        double dy = (down ? 1 : 0) - (up ? 1 : 0);
        if (dx != 0 || dy != 0) {
            facing = directionFromVector(dx, dy);
            double length = Math.hypot(dx, dy);
            dx = dx / length * speed; dy = dy / length * speed;
        }
        if (dashFrames > 0) { dx = dashX * 14; dy = dashY * 14; dashFrames--; }
        movementRemainderX += dx; movementRemainderY += dy;
        int horizontalMovement = (int)Math.round(movementRemainderX);
        int verticalMovement = (int)Math.round(movementRemainderY);
        movementRemainderX -= horizontalMovement; movementRemainderY -= verticalMovement;
        int previousX = x, previousY = y;
        moveSafely(horizontalMovement, verticalMovement, solidAreas);
        boolean moved = x != previousX || y != previousY;
        if (moved) {
            walkTicks = moving
                    ? (walkTicks + 1) % (WALK_FRAME_TICKS * 2) : 0;
        } else {
            walkTicks = 0;
        }
        moving = moved;
    }

    private Rectangle2D getCollisionBoundsAt(int candidateX, int candidateY) {
        // Include the hat and body in Santa's physical footprint.
        return new Rectangle2D.Double(candidateX, candidateY - 8, WIDTH, HEIGHT + 8);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    /** Repositions Santa at an area entrance. */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Rectangle2D getBounds() {
        return getCollisionBoundsAt(x, y);
    }

    /** Starts an attack without stopping movement. Repeated key-down is ignored by GamePanel. */
    public boolean startAttack() {
        if (attackCooldownFrames > 0 || isDefeated()) {
            return false;
        }
        facing = directionFromVector(attackDirectionX, attackDirectionY);
        attackSequence++;
        attackFramesRemaining = 9;
        attackCooldownFrames = 23;
        hitEnemiesThisAttack.clear();
        return true;
    }

    public boolean isAttacking() {
        return attackFramesRemaining > 0;
    }

    public int getAttackCooldownFrames() {
        return attackCooldownFrames;
    }

    public boolean hasHitDuringCurrentAttack(Enemy enemy) {
        return hitEnemiesThisAttack.contains(enemy);
    }

    public void markHitDuringCurrentAttack(Enemy enemy) {
        hitEnemiesThisAttack.add(enemy);
    }

    public int getAttackDamage() {
        return 30;
    }

    public int getHealth() {
        return health;
    }

    public int getMaximumHealth() {
        return 100;
    }

    /** Restores HP without reviving a fallen Santa or exceeding maximum health. */
    public boolean heal(int amount) {
        if (amount <= 0 || health <= 0 || health >= getMaximumHealth()) return false;
        health = (int) Math.min(getMaximumHealth(), (long) health + amount);
        return true;
    }

    public boolean takeDamage(int damage) {
        if (isDashing() || developerTestMode || damage <= 0 || health <= 0 || damageCooldownFrames > 0) {
            return false;
        }
        health = Math.max(0, health - damage);
        damageCooldownFrames = 22;
        return true;
    }

    public boolean isDeveloperTestMode() {
        return developerTestMode;
    }

    /** Does not heal or change stats; disabling restores the existing HP state. */
    public void toggleDeveloperTestMode() {
        developerTestMode = !developerTestMode;
    }

    public boolean isDefeated() {
        return !developerTestMode && health <= 0;
    }

    /** Sets a normalized 2D attack direction, ignoring a zero-length vector. */
    public void setAttackDirection(double dx, double dy) {
        double length = Math.hypot(dx, dy);
        if (length > 0.0) {
            attackDirectionX = dx / length;
            attackDirectionY = dy / length;
        }
    }

    public double getAttackDirectionX() {
        return attackDirectionX;
    }

    public double getAttackDirectionY() {
        return attackDirectionY;
    }

    /** Returns a short, rotated hitbox in front of Santa, aligned to the cursor. */
    public Shape getAttackBounds() {
        double centerX = x + WIDTH / 2.0;
        double centerY = y + HEIGHT / 2.0;
        double reach = 54.0;
        double thickness = 30.0;
        double hitboxCenterX = centerX + attackDirectionX * reach / 2.0;
        double hitboxCenterY = centerY + attackDirectionY * reach / 2.0;
        Rectangle2D localHitbox = new Rectangle2D.Double(
                hitboxCenterX - reach / 2.0,
                hitboxCenterY - thickness / 2.0,
                reach, thickness);
        double angle = Math.atan2(attackDirectionY, attackDirectionX);
        AffineTransform rotation = AffineTransform.getRotateInstance(
                angle, hitboxCenterX, hitboxCenterY);
        return rotation.createTransformedShape(localHitbox);
    }

    /** Clear transient animation/combat state without changing position or normal controls. */
    public void stopForCutscene() {
        dashFrames = 0;
        moving = false;
        walkTicks = 0;
        attackFramesRemaining = 0;
        hitEnemiesThisAttack.clear();
    }

    /** Render-only pose: no changes to facing, position, controls, stats, or collision. */
    public void drawCinematic(Graphics2D graphics, double feetX, double feetY, Direction direction) {
        if (!SPRITES.draw(graphics, AnimationState.IDLE, direction, 0, feetX, feetY)) {
            Graphics2D g=(Graphics2D)graphics.create();
            g.translate(feetX-(x+16),feetY-(y+42)); drawFallback(g); g.dispose();
        }
    }
    public void drawCinematicWalk(Graphics2D g,double feetX,double feetY,Direction direction,int frame) {
        SPRITES.draw(g,AnimationState.WALK,direction,Math.floorMod(frame,2),feetX,feetY);
    }

    public void draw(Graphics2D graphics) {
        if (isDefeated()) {
            return;
        }

        Graphics2D g=(Graphics2D)graphics.create();
        try {
            g.setColor(new Color(5,13,26,85));g.fillOval(x-2,y+36,36,9);
            if(dashFrames>0)for(int i=3;i>=1;i--) {
                g.setComposite(java.awt.AlphaComposite.SrcOver.derive(.24f/i));
                SPRITES.draw(g,getAnimationState(),getVisualDirection(),getAnimationFrame(),x+16-dashX*i*13,y+42-dashY*i*13);
            }
            g.setComposite(java.awt.AlphaComposite.SrcOver);
            int breathe=getAnimationState()==AnimationState.IDLE?(visualTicks/50)%2:0;
            if(!SPRITES.draw(g,getAnimationState(),getVisualDirection(),getAnimationFrame(),x+16,y+42-breathe))drawFallback(g);
            if(damageCooldownFrames>10) {
                g.setComposite(java.awt.AlphaComposite.SrcOver.derive((damageCooldownFrames-10)/18f));
                SPRITES.drawFlash(g,getAnimationState(),getVisualDirection(),getAnimationFrame(),x+16,y+42-breathe);
                g.setComposite(java.awt.AlphaComposite.SrcOver);
            }
            if(attackFramesRemaining>0) {
                g.translate(x+16,y+21);g.rotate(Math.atan2(attackDirectionY,attackDirectionX));
                double p=(9-attackFramesRemaining)/9.0;
                g.setStroke(new java.awt.BasicStroke(4,java.awt.BasicStroke.CAP_SQUARE,java.awt.BasicStroke.JOIN_MITER));
                g.setColor(new Color(226,173,106,(int)((1-p)*165)));g.drawArc(4,-24,48,48,-65+(int)(p*35),125);
                g.setStroke(new java.awt.BasicStroke(2));g.setColor(new Color(255,239,195,(int)((1-p)*230)));g.drawArc(8,-21,42,42,-55+(int)(p*35),100);
            }
        } finally {g.dispose();}
    }

    public AnimationState getAnimationState() {
        return isAttacking() ? AnimationState.ATTACK
                : moving ? AnimationState.WALK : AnimationState.IDLE;
    }

    public Direction getVisualDirection() {
        return isAttacking() ? directionFromVector(attackDirectionX, attackDirectionY) : facing;
    }

    /** Zero-based frame index, advanced by game updates, never by rendering. */
    public int getAnimationFrame() {
        if (isAttacking()) {
            return Math.min(2, (9 - attackFramesRemaining) / 3);
        }
        return moving ? walkTicks / WALK_FRAME_TICKS : 0;
    }

    private static Direction directionFromVector(double dx, double dy) {
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx < 0 ? Direction.LEFT : Direction.RIGHT;
        }
        return dy < 0 ? Direction.UP : Direction.DOWN;
    }

    /** Original placeholder remains usable if the selected artwork is unavailable. */
    private void drawFallback(Graphics2D graphics) {
        // Body and coat
        graphics.setColor(Color.RED);
        graphics.fillRoundRect(x, y + 14, WIDTH, HEIGHT - 14, 8, 8);

        // Face
        graphics.setColor(new Color(255, 211, 169));
        graphics.fillOval(x + 7, y + 5, 18, 18);

        // Hat
        graphics.setColor(Color.RED);
        int[] hatX = {x + 4, x + 16, x + 28};
        int[] hatY = {y + 10, y - 4, y + 10};
        graphics.fillPolygon(hatX, hatY, 3);
        graphics.setColor(Color.WHITE);
        graphics.fillOval(x + 12, y - 8, 8, 8);
        graphics.fillRoundRect(x + 3, y + 7, 26, 6, 3, 3);

        // Beard and belt
        graphics.fillOval(x + 7, y + 15, 18, 12);
        graphics.setColor(Color.BLACK);
        graphics.fillRect(x, y + 29, WIDTH, 4);
        graphics.setColor(Color.YELLOW);
        graphics.fillRect(x + 13, y + 29, 6, 4);

    }
}
