package ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import util.ImageAssets;
import util.LightBloom;

/** Supplied cinematic artwork, cached at uniform scale; effects belong only to the menu. */
public final class MenuBackdrop {
    public static final int ART_WIDTH=1536, ART_HEIGHT=1024;
    private static final BufferedImage SCENE=prepareScene();
    private static final int SNOW_COUNT=96;
    private static final Color SNOW_BRIGHT=new Color(236,245,255,160),SNOW_FAINT=new Color(208,231,250,85);
    private BufferedImage image;
    private int viewportWidth,viewportHeight;
    private Rectangle bounds;
    private static BufferedImage prepareScene() {
        BufferedImage source=ImageAssets.load("assets/menu/main_menu_reference.png");
        if(source==null)throw new IllegalStateException("Missing supplied main menu reference");
        BufferedImage scene=new BufferedImage(ART_WIDTH,ART_HEIGHT,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=scene.createGraphics();
        try {
            g.drawImage(source,0,0,null);
            // Remove baked controls only in this render cache; the source PNG is unchanged.
            for(int inset=0;inset<24;inset++) {
                g.setComposite(AlphaComposite.SrcOver.derive(inset==23?1f:.12f));
                g.setColor(new Color(6,15,29));
                g.fillRoundRect(982+inset,386+inset,510-inset*2,514-inset*2,40,40);
            }
        } finally {g.dispose();}
        return scene;
    }
    public static Rectangle artBounds(int width,int height) {
        double scale=Math.min(width/(double)ART_WIDTH,height/(double)ART_HEIGHT);
        int w=(int)Math.round(ART_WIDTH*scale),h=(int)Math.round(ART_HEIGHT*scale);
        return new Rectangle((width-w)/2,(height-h)/2,w,h);
    }
    public void draw(Graphics2D original,int width,int height) {draw(original,width,height,0,false);}
    public void draw(Graphics2D original,int width,int height,int ticks,boolean motion) {
        if(width<=0||height<=0)return;
        if(image==null||viewportWidth!=width||viewportHeight!=height) {
            viewportWidth=width;viewportHeight=height;bounds=artBounds(width,height);
            image=new BufferedImage(bounds.width,bounds.height,BufferedImage.TYPE_INT_RGB);
            Graphics2D cache=image.createGraphics();
            try {
                cache.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                cache.drawImage(SCENE,0,0,bounds.width,bounds.height,null);
            } finally {cache.dispose();}
        }
        Graphics2D g=(Graphics2D)original.create();
        try {
            g.setColor(new Color(3,10,22));g.fillRect(0,0,width,height);
            g.clipRect(bounds.x,bounds.y,bounds.width,bounds.height);
            int drift=motion?(int)Math.round(Math.sin(ticks*.004)*1.5):0;
            g.drawImage(image,bounds.x+drift,bounds.y,null);
            double s=bounds.width/(double)ART_WIDTH,phase=motion?ticks*.027:0;
            double flicker=.09+.035*Math.sin(phase)+.015*Math.sin(phase*2.7);
            LightBloom.draw(g,bounds.x+drift+190*s,bounds.y+380*s,95*s,flicker,LightBloom.GOLD);
            LightBloom.draw(g,bounds.x+drift+259*s,bounds.y+728*s,76*s,flicker*.8,LightBloom.GOLD);
            LightBloom.draw(g,bounds.x+drift+733*s,bounds.y+201*s,110*s,.1+.035*Math.sin(phase*.5),LightBloom.CYAN);
            if(motion)for(int i=0;i<SNOW_COUNT;i++) {
                double x=(i*317.0+Math.sin(ticks*.009+i)*13+ticks*(.045+i%3*.018))%ART_WIDTH;
                double y=(i*193.0+ticks*(.45+i%5*.12))%ART_HEIGHT;
                int size=i%7==0?3:2;g.setColor(i%3==0?SNOW_BRIGHT:SNOW_FAINT);
                g.fillRect(bounds.x+(int)(x*s),bounds.y+(int)(y*s),Math.max(1,(int)(size*s)),Math.max(1,(int)(size*s)));
            }
        } finally {g.dispose();}
    }
}
