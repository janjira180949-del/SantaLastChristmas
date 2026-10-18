package game;

import entity.Santa;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import ui.DialogueLine;
import ui.Portrait;
import ui.UITheme;
import util.LightBloom;

/** Optional authored descriptions, using the existing dialogue/input/area lifecycle. */
public final class WorkshopInteractions {
    public enum Site {
        LETTER_DESK("Letter Desk", "The Last Letter", List.of(
            letter("Dear Santa,\nChristmas is almost here!\nBut something strange is happening\nin our village."),
            letter("The Christmas lights are fading.\nIris, Clara, and Ben\nare still waiting for you."),
            letter("Could you bring their presents\nand visit us soon?\nWe could really use\na little Christmas cheer."),
            santa("Three presents, three old friends.\nSounds easy enough...\nWhat could possibly go wrong?"))),
        SPIRIT_LANTERN("Spirit Lantern", "The Fading Light", List.of(
            santa("The Spirit Lantern...\nIs its light getting weaker?"),
            santa("That's strange.\nIt always shines brightest\nat Christmas."),
            santa("First, those three presents.\nThen I'll find out\nwhat's going on."))),
        JOURNEY_MAP("Journey Map", "The Journey Ahead", List.of(
            new DialogueLine("THE JOURNEY AHEAD", Portrait.Kind.SANTA,
                "THE LAST VILLAGE\n\u2193\nFROZEN FOREST\n\u2193\nCHRISTMAS TOWN"),
            santa("The Last Village comes first.\nIris, Clara, and Ben are waiting\nfor their Christmas presents."),
            santa("The Frozen Forest...\nand Christmas Town?\nI hope this trip doesn't get\nmore complicated than planned."))),
        GIFT_STORAGE("Gift Storage", "Undelivered Presents", List.of(
            santa("So many presents..."),
            santa("These three are ready\nfor The Last Village."),
            santa("Christmas should reach\nevery home.")));

        private final String landmark, title, prompt;
        private final List<DialogueLine> lines;
        Site(String landmark, String title, List<DialogueLine> lines) {
            this.landmark=landmark;this.title=title;this.lines=lines;
            this.prompt="[E] "+landmark+" - "+title;
        }
        public String landmark() {return landmark;}
        public String title() {return title;}
        public List<DialogueLine> lines() {return lines;}
        /** Narrative selection only; the existing chapter owns homecoming progress. */
        public List<DialogueLine> lines(boolean homeReturned) {
            if(!homeReturned)return lines;
            return switch(this){
                case SPIRIT_LANTERN -> List.of(santa("The Spirit Lantern...\nIt's shining brighter than ever!"),santa("So that's the secret.\nChristmas Spirit grows\nwhen we care for each other."),santa("Good thing everyone helped!\nMy back was starting to hurt!"));
                case JOURNEY_MAP -> List.of(lines.get(0),santa("Three presents, a frozen forest,\nand two giant monsters...\nWhat a Christmas!"),santa("Next year, I'm sticking\nto delivering presents.\nNo more saving the world!"));
                default -> lines;
            };
        }
        private static DialogueLine letter(String text) {
            return new DialogueLine("THE LAST LETTER", Portrait.Kind.SANTA, Portrait.Expression.CONCERNED, text);
        }
        private static DialogueLine santa(String text) {
            return new DialogueLine("Santa", Portrait.Kind.SANTA, Portrait.Expression.CONCERNED, text);
        }
    }
    private static final WorldMap MAP=WorldMap.forArea(Area.Type.SANTA_BASE);
    private static final List<Rectangle2D> SOLIDS=MAP.solids();
    private static final Site[] SITES=Site.values();
    private static final Map<Site,WorldMap.Point> APPROACHES=approaches();
    private static final Font PROMPT_FONT=new Font("Dialog",Font.BOLD,12);
    public static final int INTERACTION_RADIUS=80;
    private int lanternResponseStart=-1;
    private boolean departureShown;

    private static Map<Site,WorldMap.Point> approaches() {
        Map<Site,WorldMap.Point> result=new EnumMap<>(Site.class);
        for(Site site:SITES) {
            WorldMap.Point marker=MAP.point("landmarks/"+site.landmark);
            Rectangle2D footprint=SOLIDS.stream().filter(s->s.contains(marker.x(),marker.y()))
                .findFirst().orElseThrow(()->new IllegalStateException("Missing workshop footprint: "+site));
            // The readable front face is just beyond the actual authored furniture solid.
            WorldMap.Point front=null;
            for(int x:new int[]{marker.x(),(int)footprint.getMinX()+16,(int)footprint.getMaxX()-16}) {
                int y=(int)footprint.getMaxY()+4;
                if(!util.Collision.intersectsAny(new Rectangle2D.Double(x-16,y-4,32,50),SOLIDS)) {
                    front=new WorldMap.Point(x,y);break;
                }
            }
            if(front==null)throw new IllegalStateException("No walkable workshop front: "+site);
            result.put(site,front);
        }
        return Map.copyOf(result);
    }
    public static WorldMap.Point approach(Site site) {return APPROACHES.get(site);}
    public static Site nearby(Santa santa) {
        if(santa==null||santa.isDefeated()||util.Collision.intersectsAny(santa.getBounds(),SOLIDS))return null;
        double x=santa.getX()+16,y=santa.getY()+21,best=INTERACTION_RADIUS*INTERACTION_RADIUS;
        Site nearest=null;
        for(Site site:SITES) {
            WorldMap.Point point=approach(site);
            double distance=(x-point.x())*(x-point.x())+(y-point.y())*(y-point.y());
            if(distance>best)continue;
            boolean blocked=false;
            for(Rectangle2D solid:SOLIDS)if(solid.intersectsLine(x,y,point.x(),point.y())){blocked=true;break;}
            if(!blocked){best=distance;nearest=site;}
        }
        return nearest;
    }
    public static String prompt(Site site) {return site.prompt;}
    public static Rectangle promptBounds(Graphics2D g,Site site,Camera camera,int width,int height) {
        g.setFont(PROMPT_FONT);
        int w=g.getFontMetrics().stringWidth(prompt(site))+28;
        WorldMap.Point point=approach(site);
        int x=Math.max(12,Math.min(width-w-12,point.x()-camera.getX()-w/2));
        int y=Math.max(114,Math.min(Math.max(114,height-232),point.y()-camera.getY()-75));
        return new Rectangle(x,y,w,32);
    }
    public static void drawPrompt(Graphics2D g,Site site,Camera camera,int width,int height) {
        if(site==null)return;
        Rectangle box=promptBounds(g,site,camera,width,height);
        UITheme.panel(g,box.x,box.y,box.width,box.height);
        g.setFont(PROMPT_FONT);g.setColor(UITheme.IVORY);
        g.drawString(prompt(site),box.x+14,box.y+21);
    }
    public void respond(Site site,int ticks) {if(site==Site.SPIRIT_LANTERN)lanternResponseStart=ticks;}
    /** Expire on the existing update clock, including its inspected 360000-tick wrap. */
    public void updateResponse(int ticks) {
        if(lanternResponseStart>=0&&Math.floorMod(ticks-lanternResponseStart,360000)>=90)lanternResponseStart=-1;
    }
    public void drawResponse(Graphics2D g,int ticks) {
        int elapsed=Math.floorMod(ticks-lanternResponseStart,360000);
        if(lanternResponseStart<0||elapsed>=90)return;
        WorldMap.Point lantern=MAP.point("landmarks/Spirit Lantern");
        LightBloom.draw(g,lantern.x(),lantern.y(),74,.09*Math.sin(Math.PI*elapsed/90.0),LightBloom.GOLD);
    }
    public boolean shouldShowDeparture(Santa santa) {
        if(departureShown)return false;
        WorldMap.Point gate=MAP.point("exit/to_village");
        if(Math.hypot(santa.getX()+16-gate.x(),santa.getY()+21-gate.y())>180)return false;
        departureShown=true;return true;
    }
    public void reset() {lanternResponseStart=-1;departureShown=false;}
}
