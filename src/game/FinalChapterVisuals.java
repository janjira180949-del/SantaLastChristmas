package game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import ui.UITheme;
import util.LightBloom;

/** Small shared chapter sprites; all decoding/cropping is outside paint and update. */
public final class FinalChapterVisuals {
    private record Art(BufferedImage[] materials) { }
    private static final CompletableFuture<Art> ART=CompletableFuture.supplyAsync(FinalChapterVisuals::load);
    private static final Font SMALL=new Font("Dialog",Font.PLAIN,11),TITLE=new Font("Dialog",Font.BOLD,12);
    private static final BufferedImage[] FALLBACK=icons();
    private FinalChapterVisuals(){ }
    public static CompletableFuture<?> prepareAsync(){return ART;}
    static Art ready(CompletableFuture<Art> future){try{return future.getNow(null);}catch(java.util.concurrent.CompletionException|java.util.concurrent.CancellationException error){return null;}}
    private static Art load(){
        try{
            BufferedImage atlas=javax.imageio.ImageIO.read(new File("assets/world/final_chapter/materials_atlas.png"));
            if(atlas==null||atlas.getWidth()!=2172||atlas.getHeight()!=724)return null;
            BufferedImage[] material=new BufferedImage[3];
            for(int i=0;i<3;i++)material[i]=trim(atlas.getSubimage(i*724,0,724,724),56,62);
            return new Art(material);
        }catch(IOException|RuntimeException error){return null;}
    }
    private static BufferedImage trim(BufferedImage source,int width,int height){
        int left=source.getWidth(),top=source.getHeight(),right=-1,bottom=-1;
        for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++)if((source.getRGB(x,y)>>>24)>24){left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);}
        if(right<left)throw new IllegalArgumentException("Empty chapter artwork");
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
        double scale=Math.min(width/(double)(right-left+1),height/(double)(bottom-top+1));int w=(int)Math.round((right-left+1)*scale),h=(int)Math.round((bottom-top+1)*scale);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(source,(width-w)/2,height-h,(width+w)/2,height,left,top,right+1,bottom+1,null);g.dispose();return image;
    }
    private static BufferedImage[] icons(){
        BufferedImage[] images=new BufferedImage[3];
        for(int i=0;i<3;i++){BufferedImage image=new BufferedImage(56,62,2);Graphics2D g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(17,29,45,95));g.fillOval(3,45,50,13);
            if(i==0){g.setPaint(new GradientPaint(12,8,new Color(182,238,255),39,52,new Color(41,117,198)));g.fillPolygon(new int[]{28,42,38,14,9},new int[]{3,28,51,56,31},5);g.setColor(new Color(223,250,255));g.drawLine(28,3,22,52);g.drawLine(10,31,42,28);}
            else if(i==1){for(int n=0;n<3;n++){int y=18+n*11;g.setColor(new Color(78+n*9,47,31));g.fillRoundRect(8,y,42,14,8,8);g.setColor(new Color(156,107,63));g.drawOval(7,y,12,13);g.setColor(new Color(206,216,222));g.drawLine(20,y,48,y);}g.setColor(new Color(175,133,80));g.drawLine(29,14,29,53);}
            else{g.setPaint(new GradientPaint(11,19,new Color(203,78,54),43,48,new Color(106,30,31)));g.fillOval(9,17,38,38);g.setColor(new Color(238,194,97));g.drawOval(22,2,11,11);g.fillRect(21,12,14,8);g.drawLine(28,26,28,46);g.drawLine(18,36,38,36);g.drawLine(21,29,35,43);g.drawLine(21,43,35,29);}
            g.dispose();images[i]=image;
        }return images;
    }
    public static BufferedImage materialImage(FinalChapter.Material kind){Art art=ready(ART);return art==null?FALLBACK[kind.ordinal()]:art.materials[kind.ordinal()];}
    public static void drawMaterials(Graphics2D g,FinalChapter chapter,Camera camera,int width,int height){
        if(!chapter.isUnlocked())return;
        for(var pickup:FinalChapter.PICKUPS)if(!chapter.isCollected(pickup.id())){
            var p=pickup.point();if(p.x()+40<camera.getX()||p.y()+40<camera.getY()||p.x()-40>camera.getX()+width||p.y()-70>camera.getY()+height)continue;
            if(pickup.material()==FinalChapter.Material.SPIRIT_FRAGMENT)LightBloom.draw(g,p.x(),p.y()-15,42,.13,LightBloom.CYAN);
            g.drawImage(materialImage(pickup.material()),p.x()-28,p.y()-46,null);
        }
    }
    public static void drawHouse(Graphics2D g,FinalChapter chapter){VillageRestorationVisuals.drawHouse(g,chapter);}
    public static void drawLights(Graphics2D g,FinalChapter chapter){VillageRestorationVisuals.drawLights(g,chapter,0);}
    public static Rectangle trackerBounds(int width,int height){return new Rectangle(Math.max(8,width-246),126,234,154);}
    public static void drawTracker(Graphics2D g,FinalChapter chapter,int width,int height){
        ui.HUD.drawRestorationTracker(g,chapter,width,height);
    }
    public static void drawPrompt(Graphics2D g,String text,entity.Santa santa,Camera camera,int width,int height){
        g.setFont(TITLE);int w=Math.min(width-24,g.getFontMetrics().stringWidth(text)+28);
        int x=Math.max(12,Math.min(width-w-12,santa.getX()+16-camera.getX()-w/2));int y=Math.max(118,Math.min(height-225,(int)santa.getBounds().getMaxY()-camera.getY()+12));
        if(x+w>width-246&&y<292)y=292;
        UITheme.panel(g,x,y,w,32);g.setColor(UITheme.IVORY);g.drawString(text,x+14,y+21);
    }
}
