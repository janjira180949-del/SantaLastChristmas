package entity;

import game.VillageActivities.Site;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Stationary villagers with cached native Java2D winter clothing and matching portraits. */
public final class VillageResident {
    private static final Map<Site,BufferedImage> SPRITES=new EnumMap<>(Site.class),PORTRAITS=new EnumMap<>(Site.class);
    private static final Color[] COATS={new Color(90,139,129),new Color(80,115,73),new Color(143,102,63),new Color(122,99,139),new Color(151,88,91),new Color(70,111,152)};
    private static final Font NAME=new Font("Dialog",Font.BOLD,12);
    private record Art(Map<Site,BufferedImage> sprites,Map<Site,BufferedImage> portraits) { }
    private static final CompletableFuture<Art> ART=CompletableFuture.supplyAsync(VillageResident::loadArt);
    public static CompletableFuture<?> prepareAsync(){return ART;}
    private static Art ready(CompletableFuture<Art> future){
        try{return future.getNow(null);}catch(java.util.concurrent.CompletionException|java.util.concurrent.CancellationException error){return null;}
    }
    private static Art loadArt(){
        try {
            BufferedImage atlas=javax.imageio.ImageIO.read(new java.io.File("assets/characters/village/residents_atlas.png"));
            if(atlas==null||atlas.getWidth()!=2172||atlas.getHeight()!=724)return null;
            Map<Site,BufferedImage> sprites=new EnumMap<>(Site.class),portraits=new EnumMap<>(Site.class);
            for(Site site:Site.values())if(site.resident()){
                int column=site.ordinal()*362;
                portraits.put(site,trim(atlas,column,0,362,350));
                BufferedImage original=trim(atlas,column,350,362,374);
                BufferedImage clean=util.ImageAssets.load("assets/phase10_5_1/residents/"+site.name().toLowerCase(java.util.Locale.ROOT)+".png");
                sprites.put(site,clean!=null&&clean.getWidth()==original.getWidth()&&clean.getHeight()==original.getHeight()?clean:original);
            }
            return new Art(Map.copyOf(sprites),Map.copyOf(portraits));
        }catch(java.io.IOException|RuntimeException error){return null;}
    }
    private static BufferedImage trim(BufferedImage image,int x,int y,int w,int h){
        int left=w,top=h,right=-1,bottom=-1;
        for(int row=0;row<h;row++)for(int col=0;col<w;col++)if((image.getRGB(x+col,y+row)>>>24)>24){
            left=Math.min(left,col);right=Math.max(right,col);top=Math.min(top,row);bottom=Math.max(bottom,row);
        }
        if(right<left)throw new IllegalArgumentException("Empty villager cutout");
        return image.getSubimage(x+left,y+top,right-left+1,bottom-top+1);
    }
    static {
        for(Site site:Site.values())if(site.resident()){
            BufferedImage image=new BufferedImage(64,80,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int i=site.ordinal();Color coat=COATS[i];
            g.setColor(new Color(13,23,34,110));g.fillOval(12,65,40,10);
            g.setColor(new Color(39,29,29));g.fillRoundRect(19,58,10,12,4,4);g.fillRoundRect(35,58,10,12,4,4);
            g.setColor(coat.darker());g.fillRoundRect(13,34,38,28,9,9);
            g.setColor(coat);g.fillRoundRect(19,30,26,33,7,7);
            g.setColor(new Color(255,231,190,28));g.fillRoundRect(20,33,5,25,3,3);
            g.setColor(new Color(17,29,35,65));g.drawLine(43,38,43,59);g.drawLine(17,43,17,58);
            g.setColor(new Color(236,216,168,65));for(int stitch=40;stitch<60;stitch+=4){g.fillRect(22,stitch,2,1);g.fillRect(39,stitch,2,1);}
            g.setColor(new Color(215,195,153));g.fillRect(20,59,24,4);g.fillRect(29,32,5,28);
            g.setColor(new Color(40,31,28));g.fillOval(19,7,26,30);
            g.setColor(new Color(218,166,122));g.fillOval(23,13,19,23);
            g.setColor(new Color(168,116,85));g.fillRect(36,22,4,6);
            g.setColor(new Color(31,28,28));g.fillRect(27,21,3,3);g.fillRect(35,21,3,3);g.drawLine(29,30,35,30);
            g.setColor(new Color(215,202,171));g.fillArc(17,7,31,36,25,135);g.fillRect(18,14,6,26);
            g.setColor(coat.darker());g.fillArc(18,2,30,23,0,180);g.fillRect(18,11,30,5);
            g.setColor(new Color(222,183,111));g.fillRect(19,35,28,5);g.fillRect(38,37,6,15);
            g.setColor(new Color(174,134,81));for(int stripe=20;stripe<47;stripe+=4)g.drawLine(stripe,35,stripe,39);
            g.setColor(new Color(224,213,190));g.drawLine(19,14,45,14);g.fillOval(16,13,7,7);
            if(site==Site.NOAH){g.setColor(new Color(172,150,111));g.drawOval(25,12,9,6);g.drawOval(36,12,9,6);g.setColor(new Color(91,73,58));g.fillArc(23,27,19,13,180,180);}
            if(site==Site.OLIVER){g.setColor(new Color(159,50,44));g.fillRect(17,45,15,13);g.setColor(new Color(239,197,109));g.fillRect(23,44,3,15);g.fillRect(16,49,17,3);}
            if(site==Site.BEN){g.setColor(new Color(178,117,64));g.fillRect(35,43,13,15);g.setColor(new Color(238,220,177));g.fillRect(37,45,9,2);}
            g.dispose();SPRITES.put(site,image);
            BufferedImage portrait=new BufferedImage(96,96,BufferedImage.TYPE_INT_ARGB);g=portrait.createGraphics();
            g.setColor(new Color(15,27,43));g.fillRoundRect(0,0,96,96,12,12);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(image,-24,-3,120,177,null);g.dispose();PORTRAITS.put(site,portrait);
        }
    }
    private VillageResident(){ }
    public static BufferedImage portrait(String name){try{Art art=ready(ART);return (art==null?PORTRAITS:art.portraits).get(Site.valueOf(name));}catch(IllegalArgumentException error){return null;}}
    public static Rectangle bounds(Site site){return new Rectangle(site.x-32,site.y-55,64,84);}
    public static void draw(Graphics2D g,Site site,boolean delivered){draw(g,site,delivered,0);}
    public static void draw(Graphics2D original,Site site,boolean delivered,int reaction){
        Graphics2D g=(Graphics2D)original.create();g.translate(0,reaction);
        Art art=ready(ART);
        if(art==null)g.drawImage(SPRITES.get(site),site.x-24,site.y-32,48,60,null);
        else {
            BufferedImage image=art.sprites.get(site);double scale=Math.min(48.0/image.getWidth(),60.0/image.getHeight());
            int w=(int)Math.round(image.getWidth()*scale),h=(int)Math.round(image.getHeight()*scale);
            g.setColor(new Color(8,19,30,95));g.fillOval(site.x-20,site.y+19,40,9);
            g.drawImage(image,site.x-w/2,site.y+26-h,w,h,null);
        }
        g.setFont(NAME);g.setColor(delivered?new Color(255,216,128):new Color(230,217,185));
        g.drawString(site.label,site.x-g.getFontMetrics().stringWidth(site.label)/2,site.y-40);
        if(delivered){g.setColor(new Color(244,207,111));g.fillOval(site.x+22,site.y-35,5,5);}
        g.dispose();
    }
}
