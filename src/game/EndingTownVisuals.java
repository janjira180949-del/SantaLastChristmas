package game;
import entity.Child;
import entity.Santa;
import entity.Elias;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.List;
import ui.Dialogue;
import util.ImageAssets;
import util.LightBloom;
/** Cached terrain illumination reused by transformation and persistent restored Town. */
public final class EndingTownVisuals {
    private BufferedImage groundLight,restoredGroundLight;
    private int lightX,lightY;
    private static final double[][] LIGHTS={{912,800},{1680,800},{912,1488},{1680,1488},
        {1960,1250},{430,1135},{920,240},{1715,195},{2025,310},{380,400}};
    public void prepare() {
        if(groundLight!=null)return;
        BufferedImage source=ImageAssets.load("assets/world/christmas_town_corrupted_clean.png");
        if(source==null)throw new IllegalStateException("Missing existing Christmas Town artwork");
        WorldMap.Point root=WorldMap.forArea(Area.Type.CHRISTMAS_TOWN).point("landmarks/Corrupted Town Center");
        lightX=root.x()-480;lightY=root.y()+68-240;
        groundLight=new BufferedImage(960,480,BufferedImage.TYPE_INT_ARGB);
        restoredGroundLight=new BufferedImage(960,480,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<480;y++)for(int x=0;x<960;x++) {
            int sx=x+lightX,sy=y+lightY;
            if(sx<0||sy<0||sx>=source.getWidth()||sy>=source.getHeight())continue;
            int rgb=source.getRGB(sx,sy);
            // The map is baked artwork: illuminate verified terrain pixels, never recolor buildings.
            boolean clearedTree=sx>=1112&&sx<1480&&sy>=784&&sy<1120;
            if(!terrain(rgb)&&!clearedTree)continue;
            double distance=Math.hypot((x-480)/480.0,(y-240)/240.0);
            double falloff=Math.max(0,1-distance);falloff=falloff*falloff*(3-2*falloff);
            int light=((int)(falloff*170)<<24)|0xffdfaa;
            if(terrain(rgb))groundLight.setRGB(x,y,light);
            restoredGroundLight.setRGB(x,y,light);
        }
    }
    public int illuminationPixel(int x,int y){prepare();return groundLight.getRGB(x,y);}
    /** Shared world-space terrain pass for the short transformation and persistent Town. */
    public void drawGroundIllumination(Graphics2D original,WorldMap.Point tree,double progress) {
        drawGroundIllumination(original,tree,progress,false);
    }
    public int illuminationPixel(int x,int y,boolean restored){prepare();return (restored?restoredGroundLight:groundLight).getRGB(x,y);}
    public void drawGroundIllumination(Graphics2D original,WorldMap.Point tree,double progress,boolean restored) {
        prepare();BufferedImage illumination=restored?restoredGroundLight:groundLight;double level=smooth(Math.max(0,Math.min(1,progress)));
        if(level<=0)return;
        Graphics2D g=(Graphics2D)original.create();
        double spread=.28+.72*level;
        Shape clip=g.getClip();
        if(level>=.999) {
            g.setComposite(AlphaComposite.SrcOver.derive(.83f));g.drawImage(illumination,lightX,lightY,null);
        } else for(int ring=2;ring>=0;ring--) {
            double radius=480*spread+ring*12;
            g.setClip(clip);g.clip(new Ellipse2D.Double(tree.x()-radius,tree.y()+68-radius*.5,radius*2,radius));
            g.setComposite(AlphaComposite.SrcOver.derive((float)(level*(ring==0?.78:.08))));
            g.drawImage(illumination,lightX,lightY,null);
        }
        g.dispose();
        // Lamp reflections are also ground illumination, not foreground objects.
        g=(Graphics2D)original.create();
        for(int i=0;i<LIGHTS.length;i++) {
            double strength=level;
            glow(g,LIGHTS[i][0],LIGHTS[i][1]+50,85,strength*.22,LightBloom.GOLD);
        }
        g.dispose();
    }
    private static boolean terrain(int rgb) {
        return rgb==0xff3f4d5e||rgb==0xff7d8ea4||rgb==0xff244151||rgb==0xff81919f||rgb==0xffd0d9d9||rgb==0xfff6efd9
            ||rgb==0xff7e8e9d||rgb==0xffa9b2b8||rgb==0xffeaeee3;
    }
    public void drawActors(Graphics2D g,EndingCutscene cut,Santa santa,List<Child> children,Elias elias) {
        Dialogue dialogue=cut.getCinematicDialogue();String speaker=dialogue==null?"":dialogue.getCurrentLine().getSpeakerName();
        Point2D.Double a=cut.actorAnchor(0);santa.drawCinematic(g,a.x,a.y,cut.santaDirection());
        a=cut.actorAnchor(1);children.get(0).drawCinematic(g,a.x,a.y,speaker.equals("Toby"));
        a=cut.actorAnchor(2);elias.drawCinematic(g,a.x,a.y);
    }
    private static double smooth(double p){return p*p*(3-2*p);}
    private static void glow(Graphics2D g,double x,double y,double radius,double strength,Color color){LightBloom.draw(g,x,y,radius,strength,color);}
}
