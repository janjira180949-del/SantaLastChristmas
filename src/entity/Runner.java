package entity;

import java.awt.Color;
import java.awt.Graphics2D;

/** A smaller, faster, less durable enemy. */
public class Runner extends Enemy {

    private static final EnemySprites SPRITES = new EnemySprites("runner", "run", 60);

    private static final int HEALTH = 70;
    private static final double SPEED = 2.4;
    private static final int SIZE = 24;

    public Runner(double x, double y) {
        super(x, y, HEALTH, SPEED, SIZE);
    }

    @Override protected int getWarningDuration() { return 22; }
    @Override protected int getAttackCooldown() { return 48; }

    @Override
    public double getHealthBarY() {
        return SPRITES.top(this) - 7;
    }

    @Override
    public void draw(Graphics2D graphics) {
        if (SPRITES.draw(graphics, this)) return;
        int x = (int) getX();
        int y = (int) getY();

        graphics.setColor(new Color(203, 111, 47));
        graphics.fillOval(x + 3, y, 18, 17);
        graphics.fillRoundRect(x, y + 9, SIZE, 15, 6, 6);
        graphics.setColor(new Color(255, 235, 204));
        graphics.fillOval(x + 6, y + 5, 4, 4);
        graphics.fillOval(x + 14, y + 5, 4, 4);
        graphics.setColor(new Color(128, 38, 36));
        graphics.drawLine(x + 7, y + 16, x + 17, y + 16);
    }
}