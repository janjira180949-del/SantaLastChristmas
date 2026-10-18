package entity;

import java.awt.Color;
import java.awt.Graphics2D;

/** A slow, sturdier enemy. */
public class Walker extends Enemy {

    private static final EnemySprites SPRITES = new EnemySprites("walker", "walk", 64);

    private static final int HEALTH = 100;
    private static final double SPEED = 1.0;
    private static final int SIZE = 30;

    public Walker(double x, double y) {
        super(x, y, HEALTH, SPEED, SIZE);
    }

    @Override
    public double getHealthBarY() {
        return SPRITES.top(this) - 7;
    }

    @Override
    public void draw(Graphics2D graphics) {
        if (SPRITES.draw(graphics, this)) return;
        int x = (int) getX();
        int y = (int) getY();

        graphics.setColor(new Color(81, 139, 67));
        graphics.fillOval(x + 3, y, 24, 23);
        graphics.fillRoundRect(x, y + 12, SIZE, 18, 7, 7);
        graphics.setColor(Color.WHITE);
        graphics.fillOval(x + 8, y + 8, 5, 5);
        graphics.fillOval(x + 18, y + 8, 5, 5);
        graphics.setColor(Color.BLACK);
        graphics.fillOval(x + 10, y + 9, 2, 3);
        graphics.fillOval(x + 20, y + 9, 2, 3);
    }
}