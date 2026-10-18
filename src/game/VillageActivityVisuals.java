package game;

import entity.Santa;
import game.VillageActivities.Site;
import java.awt.*;
import java.awt.image.BufferedImage;
import ui.UITheme;
import util.LightBloom;
import java.util.concurrent.CompletableFuture;

/** Additive, bounded world props and progress presentation; original terrain stays cached. */
public final class VillageActivityVisuals {
    private static final BufferedImage[] FROZEN={lantern(false,0),lantern(false,1),lantern(false,2)},WARM={lantern(true,0),lantern(true,1),lantern(true,2)};
    private static final BufferedImage COLD_FIRE=fire(-1),GATE=gate();
    private static final BufferedImage[] FLAMES={fire(0),fire(1),fire(2),fire(3)};
    private static final Font SMALL=new Font("Dialog",Font.PLAIN,12),TITLE=new Font("Dialog",Font.BOLD,13);
    private record Props(BufferedImage[] frozen,BufferedImage[] warm,BufferedImage off,BufferedImage[] on,BufferedImage gate,BufferedImage[] presents){ }
    private static final CompletableFuture<Props> ART=CompletableFuture.supplyAsync(VillageActivityVisuals::loadArt);
    public static CompletableFuture<?> prepareAsync(){return ART;}
    private static Props ready(CompletableFuture<Props> future){
        try{return future.getNow(null);}catch(java.util.concurrent.CompletionException|java.util.concurrent.CancellationException error){return null;}
    }
    private static Props loadArt(){
        try{
            BufferedImage atlas=javax.imageio.ImageIO.read(new java.io.File("assets/characters/village/activities_props_atlas.png"));
            if(atlas==null||atlas.getWidth()!=1254||atlas.getHeight()!=1254)return null;
            BufferedImage[] cold=new BufferedImage[3],warm=new BufferedImage[3];
            for(int i=0;i<3;i++){cold[i]=cut(atlas,i,0,78,135);warm[i]=cut(atlas,i,1,78,135);}
            BufferedImage source=javax.imageio.ImageIO.read(new java.io.File("assets/world/village_props/present.png"));BufferedImage[] gifts=new BufferedImage[3];
            if(source!=null)for(int i=0;i<3;i++){
                gifts[i]=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);
                for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++){
                    int pixel=source.getRGB(x,y);float[] hsb=Color.RGBtoHSB((pixel>>>16)&255,(pixel>>>8)&255,pixel&255,null);
                    if(i>0&&hsb[1]>.35f&&(hsb[0]<.085f||hsb[0]>.96f))pixel=(pixel&0xff000000)|(Color.HSBtoRGB(i==1?.36f:.61f,hsb[1],hsb[2])&0xffffff);
                    gifts[i].setRGB(x,y,pixel);
                }
            }
            return new Props(cold,warm,cut(atlas,0,2,110,100),animateFire(cut(atlas,1,2,110,100)),cut(atlas,2,2,176,156),gifts);
        }catch(java.io.IOException|RuntimeException error){return null;}
    }
    private static BufferedImage cut(BufferedImage atlas,int col,int row,int width,int height){
        int cell=418,offset=row==0?0:row==1?448:898,rows=row==0?448:row==1?450:356,left=cell,top=rows,right=-1,bottom=-1;
        for(int y=0;y<rows;y++)for(int x=0;x<cell;x++)if((atlas.getRGB(col*cell+x,offset+y)>>>24)>24){left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);}
        if(right<left)throw new IllegalArgumentException("Empty prop");
        var part=atlas.getSubimage(col*cell+left,offset+top,right-left+1,bottom-top+1);
        double scale=Math.min(width/(double)part.getWidth(),height/(double)part.getHeight());int w=(int)Math.round(part.getWidth()*scale),h=(int)Math.round(part.getHeight()*scale);
        BufferedImage result=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);Graphics2D g=result.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(part,(width-w)/2,height-h,w,h,null);g.dispose();return result;
    }
    private static BufferedImage[] animateFire(BufferedImage source){
        var result=new BufferedImage[4];
        for(int i=0;i<4;i++){var im=new BufferedImage(110,100,BufferedImage.TYPE_INT_ARGB);var g=im.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source,0,57,110,100,0,57,110,100,null);
            int sway=i%2==0?-1:1,height=57-(i%3)*3;g.drawImage(source,sway,57-height,110+sway,57,0,0,110,57,null);g.dispose();result[i]=im;
        }return result;
    }
    private VillageActivityVisuals(){ }
    private static BufferedImage lantern(boolean lit,int kind){
        BufferedImage image=new BufferedImage(52,90,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
        g.setColor(new Color(12,24,38,150));g.fillOval(7,72,40,12);
        g.setColor(new Color(68,56,49));g.fillRect(23,44,6,31);g.fillRoundRect(15,70,22,8,5,5);
        g.setColor(new Color(168,141,96));g.drawRect(14,16,25,29);g.fillPolygon(new int[]{10,26,42},new int[]{17,3,17},3);
        g.setColor(lit?new Color(245,184,79):new Color(60,94,139));g.fillRect(17,19,19,23);
        g.setColor(lit?new Color(255,231,151):new Color(132,181,214));g.fillRect(22,21,8,17);
        g.setColor(new Color(55,46,41));g.drawLine(26,18,26,44);g.drawLine(14,31,39,31);
        if(!lit){g.setColor(new Color(111,143,190));g.drawLine(13,25,37,41);g.drawLine(17,45,42,20);}
        g.setColor(lit?new Color(255,213,115):new Color(156,203,235));
        if(kind==0){g.drawOval(18,21,16,16);for(int tooth=0;tooth<8;tooth++){double a=tooth*Math.PI/4;g.drawLine(26+(int)(Math.cos(a)*7),29+(int)(Math.sin(a)*7),26+(int)(Math.cos(a)*11),29+(int)(Math.sin(a)*11));}}
        if(kind==1){g.drawArc(8,8,37,49,30,290);g.fillPolygon(new int[]{26,29,36,30,32,26,20,22,16,23},new int[]{22,27,27,31,37,34,37,31,27,27},10);}
        if(kind==2&&!lit){for(int x=7;x<44;x+=12)g.fillPolygon(new int[]{x,x+5,x+10},new int[]{52,30,52},3);}
        g.setColor(new Color(222,231,241));g.fillRoundRect(10,12,31,5,4,4);g.drawLine(18,70,35,70);
        g.dispose();return image;
    }
    private static BufferedImage fire(int frame){
        BufferedImage image=new BufferedImage(110,100,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(20,27,36,120));g.fillOval(7,54,96,37);g.setColor(new Color(44,42,44));g.fillOval(18,52,74,28);
        for(int i=0;i<9;i++){double a=i*Math.PI*2/9;int x=48+(int)(Math.cos(a)*42),y=64+(int)(Math.sin(a)*18);g.setColor(new Color(72+i%3*9,85+i%3*9,99+i%3*9));g.fillOval(x,y,17,13);g.setColor(new Color(208,220,233));g.fillArc(x,y,17,9,0,180);}
        g.setStroke(new BasicStroke(13,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(new Color(59,37,30));g.drawLine(32,76,76,57);g.drawLine(32,57,77,76);g.setStroke(new BasicStroke(2));g.setColor(new Color(108,74,44));g.drawLine(32,73,76,56);g.drawLine(32,54,76,74);
        if(frame>=0){int sway=frame%2*4;g.setPaint(new GradientPaint(50,15,new Color(255,197,85),55,76,new Color(191,58,29)));g.fillPolygon(new int[]{27,38,42,54+sway,66,79,81,69,40},new int[]{70,49,59,18,50,37,66,79,80},9);g.setColor(new Color(255,232,137));g.fillPolygon(new int[]{43,53-sway,62,70,64,49},new int[]{76,45,66,58,79,80},6);}
        else{g.setColor(new Color(150,167,190,125));g.fillOval(40,70,29,5);}
        g.dispose();return image;
    }
    private static BufferedImage gate(){
        BufferedImage image=new BufferedImage(176,156,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
        g.setColor(new Color(18,29,46,90));g.fillOval(3,135,170,18);
        g.setStroke(new BasicStroke(16));g.setColor(new Color(83,57,42));g.drawArc(19,5,139,122,0,180);g.drawLine(19,65,19,139);g.drawLine(157,65,157,139);
        g.setStroke(new BasicStroke(2));g.setColor(new Color(173,122,67));g.drawArc(19,5,139,122,0,180);g.drawLine(17,65,17,139);g.drawLine(155,65,155,139);
        g.setStroke(new BasicStroke(9));g.setColor(new Color(207,221,236));g.drawArc(19,1,139,122,15,150);
        g.setStroke(new BasicStroke(4));g.setColor(new Color(47,83,58));g.drawArc(19,19,139,115,8,165);
        for(int x=36;x<150;x+=22){g.setColor(new Color(176,60,47));g.fillOval(x,23+(int)(Math.abs(x-88)*.18),7,7);}
        g.setColor(new Color(74,45,35));g.fillRoundRect(58,50,60,24,5,5);g.setFont(TITLE);g.setColor(new Color(245,206,137));g.drawString("BASE",70,67);
        g.setColor(new Color(255,182,69));g.fillRoundRect(5,73,23,30,4,4);g.fillRoundRect(147,73,23,30,4,4);g.setColor(new Color(71,47,35));g.drawRect(5,73,23,30);g.drawRect(147,73,23,30);
        g.dispose();return image;
    }
    public static void drawProps(Graphics2D g,VillageActivities activities,int ticks){
        Props art=ready(ART);
        Site[] sites={Site.STREETLIGHTS,Site.LANTERN_EAST,Site.LANTERN_SOUTH};
        for(int i=0;i<3;i++){
            Site site=sites[i];

            g.drawImage(art==null?(activities.lantern(i)?WARM[i]:FROZEN[i]):(activities.lantern(i)?art.warm[i]:art.frozen[i]),site.x-39,site.y-126,78,135,null);
        }
        Site campfire=Site.CAMPFIRE;g.drawImage(art==null?(activities.campfire()?FLAMES[Math.floorMod(ticks/15,4)]:COLD_FIRE):(activities.campfire()?art.on[Math.floorMod(ticks/9,4)]:art.off),campfire.x-55,campfire.y-70,null);
        // The obsolete Village return arch is no longer an active destination.
    }
    public static void drawLights(Graphics2D g,VillageActivities activities){
        drawLights(g,activities,0);
    }
    public static void drawLights(Graphics2D g,VillageActivities activities,int ticks){
        // VillageAtmosphere owns the three lamp halos; avoid a second stacked glow.
        if(activities.campfire()){
            double flicker=.94+.06*Math.sin(ticks/7.0);
            LightBloom.draw(g,Site.CAMPFIRE.x,Site.CAMPFIRE.y-8,135,.28*flicker,LightBloom.GOLD);
            for(int i=0;i<6;i++){double age=Math.floorMod(ticks+i*17,90)/90.0;g.setColor(new Color(255,189,89,(int)(130*(1-age))));g.fillOval(Site.CAMPFIRE.x-16+i*6+(int)(Math.sin(age*5+i)*4),Site.CAMPFIRE.y-22-(int)(age*48),2,2);}
        }
        
    }
    public static Rectangle trackerBounds(int width,int height){return new Rectangle(Math.max(8,width-246),Math.min(126,Math.max(8,height-270)),234,100);}
    public static void drawTracker(Graphics2D g,VillageActivities a,int width,int height){
        // Small viewports keep existing HP/Spirit/quest/ability HUD readable; progress remains in activity panels.
        if(width<640||height<480)return;
        Rectangle r=trackerBounds(width,height);UITheme.panel(g,r.x,r.y,r.width,r.height);
        g.setFont(TITLE);g.setColor(UITheme.GOLD);g.drawString("A Christmas Delivery",r.x+12,r.y+20);
        g.setFont(SMALL);g.setColor(UITheme.IVORY);
        g.drawString("Presents at Base: "+(a.presentsCollected()?"collected":"waiting"),r.x+12,r.y+42);
        g.drawString("Deliver presents     "+a.deliveries()+" / 3",r.x+12,r.y+62);
        g.drawString("Village restoration follows the story",r.x+12,r.y+82);
    }
    public static void drawPrompt(Graphics2D g,Site site,Santa santa,Camera camera,int width,int height,VillageActivities activities){
        if(site==null)return;
        String text="[E] "+(site.resident()?(activities.canDeliver(site)?"Give Present to ":"Talk to ")+site.label:activities.chapter.restored(site.restoration())?"Restoration complete":site.label);
        g.setFont(SMALL);int w=Math.min(width-24,g.getFontMetrics().stringWidth(text)+28);
        int x=Math.max(12,Math.min(width-w-12,santa.getX()+16-camera.getX()-w/2));
        int y=Math.max(118,Math.min(height-225,(int)santa.getBounds().getMaxY()-camera.getY()+12));
        if(width>=900&&x+w>width-250&&y<230)x=Math.max(12,width-250-w);
        UITheme.panel(g,x,y,w,32);g.setColor(UITheme.IVORY);g.drawString(text,x+14,y+21);
    }
    public static void drawStorage(Graphics2D g,VillageActivities activities){
        var front=WorkshopInteractions.approach(WorkshopInteractions.Site.GIFT_STORAGE);
        if(!activities.presentsCollected())for(int i=0;i<3;i++)ui.VillageActivityUI.present(g,i,front.x()-50+i*32,front.y()-24,25);
        else{g.setColor(new Color(242,216,155));g.setFont(SMALL);g.drawString("Presents taken",front.x()-43,front.y()-15);}
    }
    public static BufferedImage presentImage(int index){Props art=ready(ART);return art==null||index<0||index>2?null:art.presents[index];}
    public static BufferedImage lanternImage(int index,boolean lit){Props art=ready(ART);return art==null?(lit?WARM[index]:FROZEN[index]):(lit?art.warm[index]:art.frozen[index]);}
    public static int notificationY(int width,int preferred){return width<1100?292:Math.max(118,preferred);}
    public static void drawStoragePrompt(Graphics2D g,WorkshopInteractions.Site site,Camera camera,int width,int height,VillageActivities activities){
        Rectangle box=WorkshopInteractions.promptBounds(g,site,camera,width,height);UITheme.panel(g,box.x,box.y,box.width,box.height);g.setFont(TITLE);g.setColor(UITheme.GOLD);g.drawString(activities.presentsCollected()?"[E] Gift Storage - Presents collected":"[E] Collect Presents",box.x+14,box.y+21);
    }
    public static void drawTravelPrompt(Graphics2D g,AreaManager areas,Santa santa,Camera camera,int width,int height){
        if(areas.getCurrentArea().getType()!=Area.Type.SANTA_BASE)return;
        String marker="exit/to_village";
        var point=WorldMap.forArea(areas.getCurrentArea().getType()).point(marker);
        if(Math.hypot(santa.getX()+16-point.x(),santa.getY()+21-point.y())>150)return;
        String text=areas.isNextAreaAvailable()?"Walk through - The Last Village":areas.hasOpeningPresents()?"Home for Christmas - Read the Letters":"Don't forget the presents.";
        g.setFont(TITLE);int w=g.getFontMetrics().stringWidth(text)+28,x=Math.max(12,Math.min(width-w-12,point.x()-camera.getX()-w/2)),y=Math.max(118,Math.min(height-232,point.y()-camera.getY()-96));
        UITheme.panel(g,x,y,w,32);g.setColor(UITheme.GOLD);g.drawString(text,x+14,y+21);
    }
}
