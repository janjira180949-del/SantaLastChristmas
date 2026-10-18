package game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;
import util.LightBloom;

/** Bounded Town gathering and homecoming overlays; preparation never runs in paint/update. */
public final class JourneyHomeVisuals {
    public static final WorldMap.Point SLEIGH_ORIGIN=new WorldMap.Point(1168,1360);
    public static final WorldMap.Point SLEIGH_APPROACH=new WorldMap.Point(SLEIGH_ORIGIN.x()+128,SLEIGH_ORIGIN.y()+166);
    public static final WorldMap.Point HOME_ARRIVAL=new WorldMap.Point(800,731);
    public static final java.util.List<WorldMap.Point> CITIZEN_FEET=java.util.List.of(
        new WorldMap.Point(1120,1184),new WorldMap.Point(1264,1216),new WorldMap.Point(1424,1200));
    public static final WorldMap.Point ELIAS_FEET=new WorldMap.Point(1240,1220);
    private record Art(BufferedImage sleigh,BufferedImage[] citizens) { }
    private static final CompletableFuture<Art> ART=CompletableFuture.supplyAsync(JourneyHomeVisuals::load);
    private static final BufferedImage LETTERS=letters();
    private static final BufferedImage FALLBACK_SLEIGH=fallbackSleigh();
    private JourneyHomeVisuals(){ }
    public static CompletableFuture<?> prepareAsync(){return ART;}
    static Art ready(CompletableFuture<Art> future){try{return future.getNow(null);}catch(java.util.concurrent.CompletionException|java.util.concurrent.CancellationException failure){return null;}}
    private static Art load(){
        try{
            var sleigh=javax.imageio.ImageIO.read(new java.io.File("assets/world/journey_home/sleigh.png"));
            var atlas=javax.imageio.ImageIO.read(new java.io.File("assets/world/journey_home/town_citizens.png"));
            if(sleigh==null||atlas==null||atlas.getWidth()!=1536||atlas.getHeight()!=1024)return null;
            BufferedImage[] citizens=new BufferedImage[3];int[] cuts={0,584,1072,1536};
            for(int i=0;i<3;i++)citizens[i]=trim(atlas.getSubimage(cuts[i],0,cuts[i+1]-cuts[i],1024),48,60);
            return new Art(trim(sleigh,300,144),citizens);
        }catch(java.io.IOException|RuntimeException failure){return null;}
    }
    private static BufferedImage trim(BufferedImage source,int width,int height){
        int l=source.getWidth(),r=-1,t=source.getHeight(),b=-1;
        for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++)if((source.getRGB(x,y)>>>24)>40){l=Math.min(l,x);r=Math.max(r,x);t=Math.min(t,y);b=Math.max(b,y);}
        if(r<l)throw new IllegalArgumentException("Empty homecoming art");
        var result=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);var g=result.createGraphics();
        double scale=Math.min(width/(double)(r-l+1),height/(double)(b-t+1));int w=(int)Math.round((r-l+1)*scale),h=(int)Math.round((b-t+1)*scale);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source,(width-w)/2,height-h,(width+w)/2,height,l,t,r+1,b+1,null);g.dispose();return result;
    }
    /** Parked-only ground contact, in source-canvas pixels: body and two standing deer.
     * Open harness gaps and transparent canopy remain walkable; cinematic boarding uses no solids.
     */
    public static java.util.List<java.awt.geom.Rectangle2D> parkedFootprints(){
        int x=SLEIGH_ORIGIN.x(),y=SLEIGH_ORIGIN.y();
        return java.util.List.of(new java.awt.geom.Rectangle2D.Double(x+14,y+102,144,32),
            new java.awt.geom.Rectangle2D.Double(x+202,y+112,48,24),
            new java.awt.geom.Rectangle2D.Double(x+262,y+112,36,24));
    }
    public static boolean parkedInFrontOf(entity.Santa santa){return santa.getBounds().getMaxY()<SLEIGH_ORIGIN.y()+136;}
    /** A waist-high parked body hides boots, not Santa's entire silhouette behind the seat.
     * Only the overlapping foreground pixels are replayed; the source image is unchanged.
     */
    public static void drawParkedForeground(Graphics2D original,entity.Santa santa){
        if(!parkedInFrontOf(santa))return;
        Graphics2D g=(Graphics2D)original.create();
        try{g.clipRect(santa.getX(),santa.getY()+18,32,24);g.drawImage(sleighImage(),SLEIGH_ORIGIN.x(),SLEIGH_ORIGIN.y(),null);}
        finally{g.dispose();}
    }

    public static BufferedImage sleighImage(){Art art=ready(ART);return art==null?FALLBACK_SLEIGH:art.sleigh;}
    public static BufferedImage citizenImage(int index){Art art=ready(ART);return art==null?null:art.citizens[index];}
    public static WorldMap.Point citizenFeet(int index,int frames){
        var end=CITIZEN_FEET.get(index);double t=Math.max(0,Math.min(1,(frames-index*25)/140.0));t=t*t*(3-2*t);
        return new WorldMap.Point(end.x()+(int)Math.round(160*(1-t)),end.y()+(int)Math.round(70*(1-t)));
    }
    public static void drawCitizens(Graphics2D g,int frames){
        for(int i=0;i<3;i++){
            var p=citizenFeet(i,frames);g.setColor(new Color(8,17,26,90));g.fillOval(p.x()-20,p.y()-7,40,10);
            var image=citizenImage(i);
            if(image!=null)g.drawImage(image,p.x()-24,p.y()-60,null);
            else {g.setColor(new Color(65+i*35,92,112-i*20));g.fillRoundRect(p.x()-14,p.y()-43,28,35,9,9);g.setColor(new Color(229,187,147));g.fillOval(p.x()-11,p.y()-60,22,22);g.setColor(new Color(225,208,158));g.fillRect(p.x()-14,p.y()-37,28,5);}
        }
    }
    public static void drawSleigh(Graphics2D g,entity.Santa santa,int departureFrame){
        double p=departureFrame<0?0:EndingCutscene.sleighFlight(departureFrame);
        int x=SLEIGH_ORIGIN.x()+(int)(p*350),y=SLEIGH_ORIGIN.y()-(int)(p*100+p*p*p*120);
        Graphics2D departure=(Graphics2D)g.create();
        double fade=departureFrame<0?0:Math.max(0,Math.min(1,(departureFrame-(EndingCutscene.SLEIGH_FLIGHT_END-30))/30.0));fade=fade*fade*(3-2*fade);
        departure.setComposite(AlphaComposite.SrcOver.derive((float)(1-fade)));g=departure;
        LightBloom.draw(g,x+55,y+110,85,.18,LightBloom.GOLD);
        g.drawImage(sleighImage(),x,y,null);
        if(departureFrame>=0){
            // Santa boards the empty seat; clip his lower body behind its upholstered rim.
            double board=Math.max(0,Math.min(1,departureFrame/(double)EndingCutscene.SLEIGH_BOARD_END));board=board*board*(3-2*board);
            int seatX=(int)Math.round(santa.getX()+16+(x+72-santa.getX()-16)*board);
            int seatY=(int)Math.round(santa.getY()+42+(y+106-santa.getY()-42)*board);
            Graphics2D seated=(Graphics2D)g.create();if(board>.85)seated.clipRect(x+28,y-25,100,111);
            santa.drawCinematic(seated,seatX,seatY,entity.Santa.Direction.RIGHT);seated.dispose();
        }
        departure.dispose();
    }
    public static void drawLetterDesk(Graphics2D g,FinalChapter progress){
        if(!progress.homeReturned())return;g.drawImage(LETTERS,390,687,null);
        LightBloom.draw(g,424,706,72,.16,LightBloom.GOLD);
    }
    private static BufferedImage letters(){
        var image=new BufferedImage(68,43,2);var g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        for(int i=0;i<5;i++){Graphics2D paper=(Graphics2D)g.create();paper.rotate((i-2)*.11,34,23);int x=4+i*7,y=5+(i%2)*10;
            paper.setColor(new Color(35,22,16,110));paper.fillRect(x+2,y+2,29,21);paper.setColor(new Color(245-i*5,227-i*5,182-i*4));paper.fillRect(x,y,29,21);
            paper.setColor(new Color(174,133,86));paper.drawRect(x,y,29,21);paper.drawLine(x,y,x+14,y+12);paper.drawLine(x+29,y,x+14,y+12);
            paper.setColor(new Color(154,49,40));paper.fillOval(x+12,y+10,5,5);paper.dispose();}
        g.dispose();return image;
    }
    private static BufferedImage fallbackSleigh(){
        var image=new BufferedImage(300,144,2);var g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(91,30,32));g.fillRoundRect(5,53,123,63,21,21);g.setColor(new Color(221,180,91));g.setStroke(new BasicStroke(4));g.drawArc(0,85,143,48,180,170);g.drawLine(100,64,220,79);
        for(int i=0;i<2;i++){int x=166+i*70;g.setColor(new Color(107,74,46));g.fillOval(x,76,54,28);g.fillOval(x+36,52,24,29);g.fillRect(x+3,98,6,28);g.fillRect(x+36,98,6,28);g.setStroke(new BasicStroke(2));g.drawLine(x+42,56,x+36,28);g.drawLine(x+37,39,x+27,33);g.drawLine(x+48,54,x+57,23);g.drawLine(x+55,32,x+66,28);}
        g.setColor(new Color(254,213,112));g.fillOval(6,67,13,19);g.dispose();return image;
    }
}
