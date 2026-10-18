package game;

import java.awt.Point;

/** Pixel-aligned follow camera; gameplay positions never change when it scrolls. */
public final class Camera {
    private volatile int x, y;
    public void follow(double centerX,double centerY,int viewportWidth,int viewportHeight,
            int worldWidth,int worldHeight) {
        x=(int)Math.max(0,Math.min(Math.max(0,worldWidth-viewportWidth),Math.floor(centerX-viewportWidth/2.0)));
        y=(int)Math.max(0,Math.min(Math.max(0,worldHeight-viewportHeight),Math.floor(centerY-viewportHeight/2.0)));
    }
    public int getX() { return x; }
    public int getY() { return y; }
    public Point screenToWorld(Point screen) { return new Point(screen.x+x,screen.y+y); }
}
