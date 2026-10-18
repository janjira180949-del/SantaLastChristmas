package game;

import entity.Child;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;
import java.awt.*;
import java.util.List;

/** Existing Village identities, presented locally without moving their authoritative actors. */
public final class TownCelebration {
    private static final VillageActivities.Site[] ROSTER={VillageActivities.Site.MARTHA,VillageActivities.Site.OLIVER,
        VillageActivities.Site.NOAH,VillageActivities.Site.IRIS,VillageActivities.Site.CLARA,VillageActivities.Site.BEN};
    private static final WorldMap.Point[] FEET={new WorldMap.Point(1088,1124),new WorldMap.Point(1128,1190),
        new WorldMap.Point(1450,1124),new WorldMap.Point(1530,1164),new WorldMap.Point(1032,1180),new WorldMap.Point(1440,1200)};
    private static final CompletableFuture<BufferedImage[][]> ART=CompletableFuture.supplyAsync(TownCelebration::prepare);
    public static CompletableFuture<?> prepareAsync(){return ART;}
    private static BufferedImage[][] prepare(){
        var atlas=util.ImageAssets.load("assets/characters/village/residents_atlas.png");var poses=new BufferedImage[6][19];
        if(atlas==null)return poses;
        for(int i=0;i<6;i++){
            int l=362,t=374,r=0,b=0;
            for(int y=0;y<374;y++)for(int x=0;x<362;x++)if((atlas.getRGB(i*362+x,y+350)>>>24)>24){l=Math.min(l,x);t=Math.min(t,y);r=Math.max(r,x);b=Math.max(b,y);}
            var src=atlas.getSubimage(i*362+l,t+350,r-l+1,b-t+1);
            var clean=util.ImageAssets.load("assets/phase10_5_1/residents/"+ROSTER[i].name().toLowerCase(java.util.Locale.ROOT)+".png");
            if(clean!=null&&clean.getWidth()==src.getWidth()&&clean.getHeight()==src.getHeight())src=clean;
            double scale=Math.min(48.0/src.getWidth(),60.0/src.getHeight());
            var base=new BufferedImage(64,70,BufferedImage.TYPE_INT_ARGB);var g=base.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            int w=(int)Math.round(src.getWidth()*scale),h=(int)Math.round(src.getHeight()*scale);g.drawImage(src,(64-w)/2,64-h,w,h,null);g.dispose();poses[i][0]=base;
            for(int pose=1;pose<19;pose++){
                boolean clap=pose>9;double progress=(clap?pose-10:pose-1)/8.0,angle=(clap?.20:.48)*progress;
                var im=new BufferedImage(64,70,BufferedImage.TYPE_INT_ARGB);g=im.createGraphics();g.drawImage(base,0,0,null);
                g.setComposite(AlphaComposite.Clear);g.fillRect(8,30,14,23);if(clap)g.fillRect(42,30,14,23);g.setComposite(AlphaComposite.SrcOver);
                g.rotate(angle,22,30);g.drawImage(base.getSubimage(8,30,14,23),8,30,null);g.dispose();
                if(clap){g=im.createGraphics();g.rotate(-angle,42,30);g.drawImage(base.getSubimage(42,30,14,23),42,30,null);g.dispose();}poses[i][pose]=im;
            }
        }
        return poses;
    }
    private TownCelebration(){}
    public static List<String> names(){return java.util.Arrays.stream(ROSTER).map(s->s.label).toList();}
    public static List<WorldMap.Point> feet(){return List.of(FEET);}
    public static final int SPEECH_HOLD=120,SPEECH_FADE=24,SPEECH_GAP=60,SPEECH_SLOT=204;
    private static final int[] SPEAKERS={0,1,3,2,0,4,3,5};
    public static int speechSpeaker(int frames){return SPEAKERS[Math.floorMod(Math.floorDiv(frames,SPEECH_SLOT),SPEAKERS.length)];}
    public static float speechOpacity(int frames){int age=Math.floorMod(frames,SPEECH_SLOT);return age<=SPEECH_HOLD?1:age<SPEECH_HOLD+SPEECH_FADE?1-(age-SPEECH_HOLD)/(float)SPEECH_FADE:0;}
    public static String speechText(int frames){int i=speechSpeaker(frames);return i==0?"A warm home again!":i==3?"Thank you, Santa!":"Christmas is home.";}
    public static void draw(Graphics2D original,int frames,List<Child> children){draw(original,frames,children,true);}
    public static void draw(Graphics2D original,int frames,List<Child> children,boolean ambientSpeech){
        WorldMap map=WorldMap.forArea(Area.Type.CHRISTMAS_TOWN);
        for(int i=0;i<ROSTER.length;i++){
            double t=Math.max(0,Math.min(1,(frames-i*15)/110.0));t=t*t*(3-2*t);
            var end=FEET[i];var free=map.safePosition(end.x()-24,end.y()-60,48,60);
            int x=free.x()+24+(int)(45*(1-t)),y=free.y()+60;
            Graphics2D g=(Graphics2D)original.create();var site=ROSTER[i];
            BufferedImage[][] poses=null;try{poses=ART.getNow(null);}catch(java.util.concurrent.CompletionException error){/* no invented fallback character */}
            int phase=Math.floorMod(frames+i*17,150);
            int gesture=phase<36?1+(int)Math.round(8*Math.sin(Math.PI*phase/36)):phase<58?10+(int)Math.round(8*Math.sin(Math.PI*(phase-36)/22)):0;
            if(poses!=null&&poses[i][gesture]!=null){
                var im=poses[i][gesture];g.setColor(new Color(8,19,30,95));g.fillOval(x-20,y-7,40,9);
                if(x<1296)g.drawImage(im,x+32,y-64,-64,70,null);else g.drawImage(im,x-32,y-64,null);
                g.setFont(new Font("Dialog",Font.BOLD,12));g.setColor(new Color(255,216,128));g.drawString(site.label,x-g.getFontMetrics().stringWidth(site.label)/2,y-70);
            }g.dispose();
            if(ambientSpeech&&i==speechSpeaker(frames)&&speechOpacity(frames)>0){
                g=(Graphics2D)original.create();g.setComposite(AlphaComposite.SrcOver.derive(speechOpacity(frames)));g.setFont(new Font("Dialog",Font.BOLD,11));
                String text=speechText(frames);
                int w=g.getFontMetrics().stringWidth(text);g.setColor(new Color(8,20,32,210));g.fillRoundRect(x-w/2-5,y-112,w+10,19,6,6);
                g.setColor(new Color(255,223,155));g.drawString(text,x-w/2,y-98);g.dispose();
            }
        }
        for(int i=0;i<children.size();i++){
            int[][] childPlaces={{1168,1144},{1220,1160},{1360,1168}};
            var p=map.safePosition(childPlaces[i][0],childPlaces[i][1],30,42);
            children.get(i).drawCinematic(original,p.x()+15,p.y()+42,false);
        }
    }
}
