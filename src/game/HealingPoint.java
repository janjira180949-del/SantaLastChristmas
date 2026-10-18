package game;

import entity.Santa;
import java.awt.*;

/** Fixed, single-use rest point; cannot heal during an encounter. */
public final class HealingPoint {
    private final Area.Type area;
    private final WorldMap.Point position;
    private boolean used;
    private boolean townRest, existingBench;
    private static final java.util.concurrent.CompletableFuture<java.awt.image.BufferedImage> REST_BENCH=
        TownChapterArt.prepareAsync().thenApply(ignored->TownChapterArt.fit(TownChapterArt.prop(5),72,32));
    /** Authoritative Town-only placements, chosen against the real roads, props and solids. */
    public static java.util.List<HealingPoint> townRestPoints() {
        var result=new java.util.ArrayList<HealingPoint>();
        for(int[] p:new int[][]{{1168,304},{688,656},{784,1376},{1872,1512},{1808,960}}) {
            var rest=new HealingPoint(Area.Type.CHRISTMAS_TOWN,p[0],p[1]);rest.townRest=true;
            rest.existingBench=p[0]==1872;result.add(rest);
        }
        return java.util.List.copyOf(result);
    }
    private int lightFrame;
    public HealingPoint(Area.Type area,int x,int y) { this.area=area;position=new WorldMap.Point(x,y); }
    public Area.Type getArea() { return area; }
    public boolean isUsed() { return used; }
    public WorldMap.Point getPosition() { return position; }
    public void update() { lightFrame=(lightFrame+1)%120; }
    public boolean isNearby(Santa santa) {
        return santa!=null&&Math.hypot(santa.getX()+16-position.x(),santa.getY()+21-position.y())<=58;
    }
    public String getInteractionText(Santa santa,boolean combatActive) {
        if(!isNearby(santa))return "";
        if(combatActive)return "";
        if(used)return "Shelter Already Used";
        return area==Area.Type.FOREST?"[E] Rest (+40 HP) - once per adventure":"[E] Rest (+40 HP)";
    }
    public boolean use(Santa santa,boolean combatActive) {
        if(used || combatActive || santa==null || santa.isDefeated() || !isNearby(santa))return false;
        if(!santa.heal(40))return false;
        used=true;return true;
    }
    /** Forest presentation is separate; the Town drawing path remains byte-for-byte below. */
    public void drawGround(Graphics2D graphics) {
        Graphics2D g=(Graphics2D)graphics.create();
        try {if(!ForestLandmarkArt.drawShrine(g,this,lightFrame))draw(graphics,null,false);}
        finally {g.dispose();}
    }
    public void drawForeground(Graphics2D graphics,Santa santa,boolean combatActive) {
        Graphics2D g=(Graphics2D)graphics.create();
        try {ForestLandmarkArt.shrinePrompt(g,this,santa,combatActive);}finally {g.dispose();}
    }
    public void draw(Graphics2D graphics) { draw(graphics,null,false); }
    public void draw(Graphics2D graphics,Santa santa,boolean combatActive) {
        if(area==Area.Type.FOREST&&ForestLandmarkArt.shrine(used)!=null){drawGround(graphics);drawForeground(graphics,santa,combatActive);return;}
        if(townRest){drawTownRest(graphics,santa,combatActive);return;}
        Graphics2D g=(Graphics2D)graphics.create();
        try {
            int x=position.x(),y=position.y();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            float pulse=(float)(.88+.12*Math.sin(lightFrame*Math.PI/60));
            g.setPaint(new RadialGradientPaint(x,y-12,48,new float[]{0,1},new Color[]{
                new Color(255,178,65,used?22:(int)(90*pulse)),new Color(255,178,65,0)}));
            g.fillOval(x-48,y-60,96,96);
            g.setColor(new Color(245,183,77,used?18:45));g.fillOval(x-32,y-5,64,20);
            g.setColor(new Color(181,144,89,90));g.drawOval(x-32,y-5,64,20);
            // Small snow-capped lantern shrine; no collision or map changes.
            g.setColor(new Color(47,31,34));g.fillRoundRect(x-20,y+5,40,7,4,4);
            g.setColor(new Color(99,63,44));g.fillRect(x-3,y-17,6,24);
            g.setColor(new Color(12,23,35));g.fillRoundRect(x-13,y-42,26,30,5,5);
            g.setPaint(new GradientPaint(x,y-40,used?new Color(71,76,79):new Color(255,223,135),x,y-15,used?new Color(54,61,69):new Color(205,115,40)));
            g.fillRoundRect(x-9,y-38,18,21,3,3);
            g.setColor(new Color(154,121,73));g.setStroke(new BasicStroke(2));
            g.drawRoundRect(x-13,y-42,26,30,5,5);g.drawLine(x-13,y-14,x+13,y-14);
            g.setColor(new Color(37,49,64));g.fillPolygon(new int[]{x-19,x,x+19},new int[]{y-41,y-53,y-41},3);
            g.setColor(new Color(216,225,231));g.drawLine(x-17,y-43,x,y-53);g.drawLine(x,y-53,x+17,y-43);
            g.setColor(used?new Color(89,111,100):new Color(113,226,158));
            g.fillRoundRect(x-2,y-33,4,13,2,2);g.fillRoundRect(x-7,y-29,14,4,2,2);
            g.setColor(new Color(33,90,63));g.fillOval(x-18,y-13,10,6);g.fillOval(x+8,y-13,10,6);
            g.setColor(new Color(158,50,52));g.fillOval(x-3,y-14,6,6);
            String label=getInteractionText(santa,combatActive);
            if(!label.isEmpty()) {
                g.setFont(new Font("Dialog",Font.BOLD,12));int width=g.getFontMetrics().stringWidth(label);
                g.setColor(new Color(8,16,29,235));g.fillRoundRect(x-width/2-10,y-85,width+20,25,9,9);
                g.setColor(used?new Color(179,187,196):new Color(253,226,171));
                g.drawString(label,x-width/2,y-68);
            }
        } finally { g.dispose(); }
    }
    private void drawTownRest(Graphics2D original,Santa santa,boolean combatActive) {
        Graphics2D g=(Graphics2D)original.create();
        try {
            int x=position.x(),y=position.y();
            java.awt.image.BufferedImage bench=REST_BENCH.getNow(null);
            if(!existingBench&&bench!=null)g.drawImage(bench,x-36,y-58,null);
            // A quiet lantern and holly sprig, with no floating medical symbol or new solid.
            g.setColor(new Color(46,35,36));g.fillRect(x-49,y-45,10,17);g.fillRect(x-51,y-29,14,3);
            g.setColor(new Color(173,131,76));g.drawRect(x-49,y-45,10,16);
            g.setColor(used?new Color(92,103,114):new Color(255,216,137));g.fillRect(x-46,y-42,4,10);
            g.setColor(new Color(224,235,238));g.fillRect(x-51,y-48,14,3);
            g.setColor(new Color(33,81,56));g.fillRect(x-40,y-30,8,4);g.setColor(new Color(176,53,57));g.fillRect(x-38,y-31,3,3);
            String label=getInteractionText(santa,combatActive);
            if(!label.isEmpty()){
                g.setFont(new Font("Dialog",Font.BOLD,12));int width=g.getFontMetrics().stringWidth(label);
                g.setColor(new Color(8,16,29,235));g.fillRoundRect(x-width/2-10,y-93,width+20,25,9,9);
                g.setColor(used?new Color(179,187,196):new Color(253,226,171));g.drawString(label,x-width/2,y-76);
            }
        } finally {g.dispose();}
    }

}
