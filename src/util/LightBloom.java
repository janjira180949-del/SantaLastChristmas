package util;

import java.awt.*;
import java.awt.image.BufferedImage;

/** Cached soft light textures; smooth light is independent of sharp sprite sampling. */
public final class LightBloom {
    public static final Color GOLD=new Color(255,211,143), CYAN=new Color(157,223,239), IVORY=new Color(255,240,202), RED=new Color(211,90,99);
    private static final Color[] COLORS={GOLD,CYAN,IVORY,RED};
    private static final BufferedImage[] LIGHTS=new BufferedImage[COLORS.length];
    private static final AlphaComposite[] ALPHAS=new AlphaComposite[256];
    static {
        for(int i=0;i<ALPHAS.length;i++)ALPHAS[i]=AlphaComposite.SrcOver.derive(i/255f);
        for(int i=0;i<LIGHTS.length;i++) {
            BufferedImage image=new BufferedImage(192,192,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=image.createGraphics();Color c=COLORS[i];
            g.setPaint(new RadialGradientPaint(96,96,96,new float[]{0,.28f,.65f,1},new Color[]{
                new Color(c.getRed(),c.getGreen(),c.getBlue(),210),new Color(c.getRed(),c.getGreen(),c.getBlue(),130),
                new Color(c.getRed(),c.getGreen(),c.getBlue(),35),new Color(c.getRed(),c.getGreen(),c.getBlue(),0)}));
            g.fillRect(0,0,192,192);g.dispose();LIGHTS[i]=image;
        }
    }
    private LightBloom() { }
    public static void draw(Graphics2D g,double x,double y,double radius,double strength,Color color) {
        if(radius<=0||strength<=0)return;
        int palette=color.getBlue()>color.getRed()+15?1:color.getRed()>color.getGreen()*1.5?3:color.getBlue()>185?2:0;
        Composite previous=g.getComposite();g.setComposite(ALPHAS[(int)(Math.max(0,Math.min(1,strength))*255)]);
        int r=(int)Math.ceil(radius);g.drawImage(LIGHTS[palette],(int)Math.round(x)-r,(int)Math.round(y)-r,2*r,2*r,null);
        g.setComposite(previous);
    }
}
