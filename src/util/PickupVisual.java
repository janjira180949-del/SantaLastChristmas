package util;
import java.awt.*;
import java.awt.image.BufferedImage;
/** Small deterministic 33-tick (0.55 second at 60 Hz) pickup, independent of quest state. */
public final class PickupVisual {
    public static final int DURATION=33;
    private final BufferedImage sprite;
    private final double x,y;
    private int age;
    public PickupVisual(BufferedImage sprite,double x,double y) {this.sprite=sprite;this.x=x;this.y=y;}
    public void update(){if(age<DURATION)age++;}
    public boolean isActive(){return age<DURATION;}
    public int age(){return age;}
    public double rise(){double t=(double)age/DURATION;return 13*(1-Math.pow(1-t,2));}
    public float alpha(){double t=(double)age/DURATION;double f=Math.max(0,(t-.35)/.65);return (float)(1-f*f*(3-2*f));}
    public void draw(Graphics2D graphics){
        if(!isActive())return;
        Graphics2D g=(Graphics2D)graphics.create();
        double t=(double)age/DURATION, cx=x+sprite.getWidth()/2.0,cy=y+sprite.getHeight()/3.0-rise();
        float glow=(float)(Math.sin(Math.PI*t)*.2);
        g.setComposite(AlphaComposite.SrcOver.derive(glow));g.setColor(new Color(255,218,135));
        g.fillOval((int)cx-20,(int)cy-17,40,34);
        g.setComposite(AlphaComposite.SrcOver.derive(alpha()));
        g.translate(x,y-rise());double scale=1-.06*t;g.translate(sprite.getWidth()*(1-scale)/2,sprite.getHeight()*(1-scale)/2);g.scale(scale,scale);
        g.drawImage(sprite,0,0,null);g.dispose();
        if(t>.35){g=(Graphics2D)graphics.create();g.setComposite(AlphaComposite.SrcOver.derive((float)(Math.sin(Math.PI*(t-.35)/.65)*.75)));
            g.setColor(new Color(255,232,164));for(int i=0;i<7;i++){double a=i*2.399;double d=8+9*(t-.35);int px=(int)(cx+Math.cos(a)*d),py=(int)(cy+Math.sin(a)*d-5*t);g.fillRect(px,py,i%3==0?3:2,2);}g.dispose();}
    }
}
