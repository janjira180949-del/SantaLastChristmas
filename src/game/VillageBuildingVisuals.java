package game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;
import util.PixelArt;

/** Source-derived local compositions. Prepared by the existing Village worker, never during paint. */
public final class VillageBuildingVisuals {
    public record Building(String id,String asset,int x,int y,int width,int height,FinalChapter.Restoration warmth) {
        public Rectangle bounds(){return new Rectangle(x,y,width,height);}
    }
    public static final List<Building> BUILDINGS=List.of(
        new Building("west-house","house",373,206,310,290,FinalChapter.Restoration.STREETLIGHTS),
        new Building("last-village-library","library",1867,58,330,310,FinalChapter.Restoration.STREETLIGHTS),
        new Building("ben-route-house","house",2176,1120,300,280,FinalChapter.Restoration.CAMPFIRE),
        new Building("south-house","house",314,1176,300,280,FinalChapter.Restoration.HOUSE),
        new Building("clara-house","house",969,172,270,260,FinalChapter.Restoration.TREE));
    public static final Rectangle RELOCATED_HOUSE_SOURCE=new Rectangle(1850,920,300,280);
    public static Rectangle sourceBounds(Building b){return b.id.equals("ben-route-house")?new Rectangle(RELOCATED_HOUSE_SOURCE):b.bounds();}
    public static final Rectangle HELP_BOUNDS=new Rectangle(1428,650,178,190);
    private record Entry(Building building,BufferedImage mask) { }
    static final class Layout {
        final List<Entry> entries;
        Layout(List<Entry> entries){this.entries=entries;}
        boolean removesPixel(int x,int y){
            for(var entry:entries){var b=entry.building;var source=sourceBounds(b);int px=x-source.x,py=y-source.y;
                if(px>=0&&py>=0&&px<b.width&&py<b.height&&(entry.mask.getRGB(px,py)>>>24)>0)return true;
            }return false;
        }
    }
    private final BufferedImage[][] buildings;
    private final BufferedImage[] help;
    private final BufferedImage helpFoundation;
    private final List<Building> identities;
    private VillageBuildingVisuals(Layout layout,BufferedImage[][] buildings,BufferedImage[] help,BufferedImage helpFoundation){this.identities=layout.entries.stream().map(Entry::building).toList();this.buildings=buildings;this.help=help;this.helpFoundation=helpFoundation;}
    static Layout loadLayout(){
        var entries=new java.util.ArrayList<Entry>();
        for(var building:BUILDINGS)try{
            BufferedImage source=ImageIO.read(new File("SantaLastChristmas_MapPackage/assets/environment/buildings/"+building.asset+".png"));
            if(source!=null)entries.add(new Entry(building,PixelArt.scale(source,building.width,building.height)));
        }catch(java.io.IOException|RuntimeException missing){/* Keep original terrain if its source mask is unavailable. */}
        return new Layout(List.copyOf(entries));
    }
    static VillageBuildingVisuals prepare(Layout layout,BufferedImage original,BufferedImage[] marketStates,BufferedImage foundation){
        var states=new BufferedImage[layout.entries.size()][3];
        for(int i=0;i<states.length;i++){
            var e=layout.entries.get(i);var b=e.building;var origin=sourceBounds(b);var warm=new BufferedImage(b.width,b.height,BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<b.height;y++)for(int x=0;x<b.width;x++)if((e.mask.getRGB(x,y)>>>24)>0)warm.setRGB(x,y,original.getRGB(origin.x+x,origin.y+y)|0xff000000);
            states[i][2]=warm;states[i][0]=affected(warm,0,i);states[i][1]=affected(warm,1,i);
        }
        var help=new BufferedImage[3];for(int i=0;i<3;i++)help[i]=helpStall(i,marketStates[i]);
        var underlay=new BufferedImage(HELP_BOUNDS.width,HELP_BOUNDS.height,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<underlay.getHeight();y++)for(int x=0;x<underlay.getWidth();x++){
            boolean occupied=false;for(var image:help)if((image.getRGB(x,y)>>>24)>0){occupied=true;break;}
            if(occupied)underlay.setRGB(x,y,foundation.getRGB(HELP_BOUNDS.x+x,HELP_BOUNDS.y+y)|0xff000000);
        }
        return new VillageBuildingVisuals(layout,states,help,underlay);
    }
    private static boolean warmPixel(int r,int g,int b){return r>135&&g>65&&g<225&&b<135&&r>g*1.12;}
    private static BufferedImage affected(BufferedImage source,int state,int identity){
        int w=source.getWidth(),h=source.getHeight();var image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int pixel=source.getRGB(x,y);if((pixel>>>24)==0)continue;
            int r=(pixel>>>16)&255,g=(pixel>>>8)&255,b=pixel&255;
            boolean emissive=warmPixel(r,g,b);double mix=state==1?(emissive?.55:.27):0;
            int cr=emissive?27+r/12:(int)(r*(.80+identity*.008));
            int cg=emissive?34+g/10:(int)(g*.88),cb=emissive?47+b/5:Math.min(255,(int)(b*.99)+5);
            int rr=(int)(cr*(1-mix)+r*mix),gg=(int)(cg*(1-mix)+g*mix),bb=(int)(cb*(1-mix)+b*mix);
            image.setRGB(x,y,0xff000000|(rr<<16)|(gg<<8)|bb);
        }
        var graphics=image.createGraphics();graphics.setComposite(AlphaComposite.SrcAtop);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setStroke(new BasicStroke(1.5f));graphics.setColor(new Color(42,32,66,state==0?125:55));
        // Different shallow frost fractures, clipped to each existing silhouette.
        for(int n=0;n<5+identity;n++){
            int x=12+(n*53+identity*31)%(w-24),y=h-12-(n*29)%(h/3);
            graphics.drawLine(x,y,x-7,y-18);graphics.drawLine(x-7,y-18,x+3,y-38);
            graphics.drawLine(x-7,y-18,x-20,y-25);
        }
        graphics.setColor(new Color(131,175,205,state==0?100:42));
        for(int n=0;n<9;n++){int x=18+(n*41+identity*19)%(w-36),y=h/2+(n*23)%(h/2-12);graphics.fillRect(x,y,2,3);}
        graphics.dispose();return image;
    }
    private static BufferedImage helpStall(int state,BufferedImage structure){
        var image=new BufferedImage(HELP_BOUNDS.width,HELP_BOUNDS.height,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        if(structure!=null){
            // Reuse the established illustrated timber, torn canvas/snow and garland details.
            // Only the canopy cloth changes color; its original grain and snow remain.
            var reused=PixelArt.scale(structure,178,170);
            for(int y=0;y<82;y++)for(int x=0;x<178;x++){
                int p=reused.getRGB(x,y);if((p>>>24)==0)continue;int r=(p>>>16)&255,green=(p>>>8)&255,b=p&255;
                boolean red=r>green*1.32&&r>b*1.25,cream=r>95&&green>80&&b<green*.86;
                if(red||cream){int value=Math.min(235,(int)((r+green+b)/3.0*(red?1.35:.80)));int rr=(int)(value*.50),gg=(int)(value*.62),bb=(int)(value*.70);reused.setRGB(x,y,(p&0xff000000)|(rr<<16)|(gg<<8)|bb);}
            }
            g.setColor(new Color(17,26,40,85));g.fillOval(2,163,172,27);g.drawImage(reused,0,0,null);
            g.setColor(new Color(52,38,31));g.fillRoundRect(27,131,124,23,5,5);g.setColor(new Color(166,137,85));g.drawRoundRect(27,131,124,23,5,5);
            g.setFont(new Font("Dialog",Font.BOLD,12));g.setColor(state==0?new Color(179,186,180):new Color(249,224,156));g.drawString("VILLAGE HELP",43,147);
            if(state==0){g.setColor(new Color(57,49,81,110));g.setStroke(new BasicStroke(1));g.drawLine(158,163,149,144);g.drawLine(149,144,153,136);}
            g.dispose();return image;
        }
        BufferedImage timber=util.ImageAssets.load("assets/world/village_props/bench.png");
        g.setColor(new Color(17,26,40,85));g.fillOval(2,108,172,27);
        if(timber!=null)g.setPaint(new TexturePaint(timber.getSubimage(40,40,40,18),new Rectangle(0,0,80,36)));else g.setColor(new Color(76,53,48));
        g.fillRect(17,31,7,91);g.fillRect(150,31,7,91);g.fillRoundRect(14,92,145,33,3,3);
        g.setColor(new Color(66,44,40));for(int x=23;x<155;x+=17)g.drawLine(x,98,x,121);
        g.setColor(new Color(168,132,85));g.fillRoundRect(10,88,155,6,3,3);
        // Quiet single-piece canvas replaces the striped canopy, with worn seams and a snow rim.
        Polygon canopy=new Polygon(new int[]{8,25,151,168,166,10},new int[]{54,21,21,54,62,62},6);
        g.setPaint(new GradientPaint(20,20,new Color(state==0?63:84,71,76),148,62,new Color(43,47,58)));g.fill(canopy);
        g.setColor(new Color(130,146,159,70));for(int x=40;x<155;x+=28)g.drawLine(x,25,x-8,57);
        g.setColor(new Color(203,222,231));g.setStroke(new BasicStroke(5,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.drawLine(25,22,151,22);g.drawLine(9,54,25,24);
        g.setColor(new Color(42,33,32));g.fillRoundRect(36,65,104,20,4,4);g.setColor(new Color(159,132,85));g.setStroke(new BasicStroke(1));g.drawRoundRect(36,65,104,20,4,4);
        g.setFont(new Font("Dialog",Font.BOLD,11));g.setColor(state==0?new Color(169,176,172):new Color(241,211,143));g.drawString("VILLAGE HELP",47,79);
        // Reused material grain, wrapped supplies, notes and a brass lantern; no trading UI.
        for(int i=0;i<3;i++){g.setColor(new Color(84+i*9,66+i*4,52));g.fillRoundRect(32+i*29,80-i%2*5,23,13,3,3);g.setColor(new Color(180,150,102));g.drawLine(43+i*29,80-i%2*5,43+i*29,93-i%2*5);}
        g.setColor(new Color(191,192,161));g.fillPolygon(new int[]{124,142,143,126},new int[]{82,80,88,89},4);
        g.setColor(new Color(162,125,66));g.fillRoundRect(15,64,11,18,3,3);g.setColor(state==0?new Color(54,76,99):new Color(246,183,74));g.fillRect(18,68,5,10);
        if(state>0){g.setColor(new Color(29,64,44));g.setStroke(new BasicStroke(5));g.drawArc(23,38,133,25,182,176);g.setColor(new Color(193,63,48));g.fillOval(81,56,12,8);}
        if(state==2){g.setColor(new Color(255,209,101));for(int i=0;i<8;i++)g.fillOval(29+i*17,54+(i%3),3,3);}
        if(state==0){g.setColor(new Color(68,56,92,115));g.setStroke(new BasicStroke(1));g.drawLine(148,123,139,106);g.drawLine(139,106,144,99);}
        g.dispose();return image;
    }
    public static int stage(Building building,FinalChapter chapter){return chapter!=null&&chapter.isComplete()?2:chapter!=null&&chapter.restored(building.warmth)?1:0;}
    public static int helpStage(FinalChapter chapter){return chapter!=null&&chapter.isComplete()?2:chapter!=null&&chapter.restored(FinalChapter.Restoration.STREETLIGHTS)?1:0;}
    public static double emission(double x,double y,FinalChapter chapter){
        if(chapter==null)return 1;for(var building:BUILDINGS)if(x>=building.x&&x<building.x+building.width&&y>=building.y&&y<building.y+building.height)return switch(stage(building,chapter)){case 0->0;case 1->.45;default->1;};return 1;
    }
    public void draw(Graphics2D g,FinalChapter chapter){
        for(int i=0;i<buildings.length;i++){var building=identities.get(i);g.drawImage(buildings[i][stage(building,chapter)],building.x,building.y,null);}
        g.drawImage(help[helpStage(chapter)],HELP_BOUNDS.x,HELP_BOUNDS.y,null);
    }
    /** Default cold art is already baked. Only changed, visible sites require local blits. */
    public void drawChanges(Graphics2D g,FinalChapter chapter){
        for(int i=0;i<buildings.length;i++){var b=identities.get(i);int state=stage(b,chapter);if(state>0&&g.hitClip(b.x,b.y,b.width,b.height))g.drawImage(buildings[i][state],b.x,b.y,null);}
        int state=helpStage(chapter);
        if(state>0&&g.hitClip(HELP_BOUNDS.x,HELP_BOUNDS.y,HELP_BOUNDS.width,HELP_BOUNDS.height)){
            g.drawImage(helpFoundation,HELP_BOUNDS.x,HELP_BOUNDS.y,null);
            g.drawImage(help[state],HELP_BOUNDS.x,HELP_BOUNDS.y,null);
        }
    }
    public BufferedImage helpFoundation(){return helpFoundation;}
    /** Prepared sprite access for focused pixel/cache tests; production never mutates these images. */
    public BufferedImage image(String id,int stage){for(int i=0;i<identities.size();i++)if(identities.get(i).id.equals(id))return buildings[i][stage];return null;}
    public BufferedImage helpImage(int state){return help[state];}
}
