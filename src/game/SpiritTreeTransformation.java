package game;

import java.awt.*;
import spirit.SpiritTree;
import util.LightBloom;

/** Presentation clock only. Exactly three seconds at the unchanged 60 Hz update rate. */
public final class SpiritTreeTransformation {
    public static final int DURATION=180;
    private int frames;
    private boolean started;
    public boolean start() {if(started)return false;started=true;frames=0;return true;}
    public boolean isActive() {return started&&frames<DURATION;}
    public boolean isRestored() {return started&&frames>=120;}
    public boolean isComplete() {return started&&frames==DURATION;}
    public int getFrames() {return frames;}
    /** Returns true exactly once; reward is capped by the existing SpiritTree implementation. */
    public boolean update(SpiritTree tree) {
        if(!isActive())return false;
        if(++frames==DURATION){tree.addSpirit(tree.getMaximumSpirit()-tree.getSpirit());return true;}
        return false;
    }
    public double lightProgress() {return started?Math.max(0,Math.min(1,(frames-30)/60.0)):0;}
    public float flashOpacity() {
        if(!started||frames<90||frames>=135)return 0;
        if(frames<120)return (float)Math.min(1,(frames-90)/7.0);
        return (135-frames)/15f;
    }
    public void drawMagic(Graphics2D original,WorldMap.Point tree) {
        double p=lightProgress();if(p<=0)return;
        Graphics2D g=(Graphics2D)original.create();
        double strength=isComplete()?.24:frames<120?.3+.65*p:.55-.3*(frames-120)/60.0;
        LightBloom.draw(g,tree.x(),tree.y()-56,75+300*p,strength,LightBloom.GOLD);
        LightBloom.draw(g,tree.x(),tree.y()+54,48+160*p,strength*.5,LightBloom.IVORY);
        if(frames<120) {
            g.setColor(new Color(255,222,149,(int)(95*p)));
            g.setStroke(new BasicStroke(2));
            double radius=18+390*p;
            g.draw(new java.awt.geom.Ellipse2D.Double(tree.x()-radius,tree.y()+58-radius*.5,2*radius,radius));
        }
        g.dispose();
    }
    /** Screen pass before any dialogue or HUD. The peak is genuinely opaque white. */
    public void drawFlash(Graphics2D original,int width,int height) {
        float a=flashOpacity();if(a<=0)return;
        Graphics2D g=(Graphics2D)original.create();g.setComposite(AlphaComposite.SrcOver.derive(a));
        g.setColor(Color.WHITE);g.fillRect(0,0,width,height);g.dispose();
    }
}
