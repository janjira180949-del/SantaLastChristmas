package game;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import util.ImageAssets;

/** Map-package world geometry. Coordinates in this class are world pixels. */
public final class WorldMap {
    public record Point(int x, int y) { }
    private static final Map<Area.Type, WorldMap> MAPS = WorldMapData.load();
    private final String id;
    private final int width, height;
    private final String imagePath, restoredPath;
    private final Map<String, Point> points;
    private final List<Rectangle2D> solids = new ArrayList<>();
    private final List<Rectangle2D> combatRegions = new ArrayList<>();
    private BufferedImage image, restored, polished;

    WorldMap(String id, int width, int height, String imagePath, String restoredPath,
            Map<String, Point> points, int[][] blocks, int[][] regions) {
        this.id=id; this.width=width; this.height=height;
        this.imagePath=imagePath; this.restoredPath=restoredPath; this.points=points;
        for (int[] b:blocks) solids.add(rectangle(b));
        for (int[] r:regions) combatRegions.add(rectangle(r));
        // The market evergreen's canopy is visual; only its compact grounded base is solid.
        if(id.equals("village")) {
            // The relocated east house owns a compact foundation, not the open road/roof above it.
            Rectangle2D previousHouse=new Rectangle2D.Double(1824,992,384,256);
            int houseIndex=solids.indexOf(previousHouse);
            if(houseIndex>=0)solids.set(houseIndex,new Rectangle2D.Double(2200,1304,256,96));
            Point tree=points.get("landmarks/Christmas Tree");
            if(tree!=null)solids.add(new Rectangle2D.Double(tree.x()-32,tree.y()-24,64,24));
        }
        // Reference maps have a one-tile border, opened only at authored exits.
        for(int x=0;x<width;x+=32) { border(x,0); border(x,height-32); }
        for(int y=32;y<height-32;y+=32) { border(0,y); border(width-32,y); }
        // Hard outer limits keep the full actor inside the artwork, including gate corridors.
        solids.add(new Rectangle2D.Double(-128,-128,width+256,128));
        solids.add(new Rectangle2D.Double(-128,height,width+256,128));
        solids.add(new Rectangle2D.Double(-128,0,128,height));
        solids.add(new Rectangle2D.Double(width,0,128,height));
    }
    private static Rectangle2D rectangle(int[] r) {
        return new Rectangle2D.Double(r[0]*32,r[1]*32,(r[2]-r[0])*32,(r[3]-r[1])*32);
    }
    private void border(int x,int y) {
        for(var entry:points.entrySet()) if(entry.getKey().startsWith("exit/")) {
            Point p=entry.getValue();
            if(Math.abs(x+16-p.x())<=64 && Math.abs(y+16-p.y())<=64) return;
        }
        solids.add(new Rectangle2D.Double(x,y,32,32));
    }
    public static WorldMap forArea(Area.Type type) { return MAPS.get(type); }
    public String id() { return id; }
    public int width() { return width; }
    public int height() { return height; }
    public Point point(String name) {
        Point result=points.get(name);
        if(result==null) throw new IllegalArgumentException(id+": missing marker "+name);
        return result;
    }
    public Map<String,Point> points() { return points; }
    public List<Rectangle2D> solids() { return List.copyOf(solids); }
    public List<Rectangle2D> combatRegions() { return List.copyOf(combatRegions); }
    public synchronized void draw(Graphics2D graphics, boolean isRestored) {
        if(id.equals("village")){VillageRestorationVisuals.drawTerrain(graphics);return;}
        if(id.equals("santa_base")){SantaBaseVisuals.drawCachedTerrain(graphics);return;}
        if(image==null) image=load(imagePath);
        BufferedImage selected=image;
        if(isRestored && restoredPath!=null) {
            if(restored==null) restored=load(restoredPath);
            selected=restored;
        }
        if (!isRestored) {
            if (polished==null) {
                if(id.equals("village"))polished=VillageEnvironmentLayers.build(image);
                else {
                    var mask=PackedSnowPath.createOverlay(image);
                    polished=RoadSnowTexture.apply(ApprovedSnowGround.polish(image,PackedSnowPath.polish(image),true),mask);
                }
            }
            selected=polished;
        }
        graphics.drawImage(selected,0,0,null);
    }
    /** Cinematic artwork stays exactly as authored, including the completed ending. */
    public synchronized void drawOriginal(Graphics2D graphics, boolean isRestored) {
        if(id.equals("santa_base")){SantaBaseVisuals.drawCachedTerrain(graphics);return;}
        if(image==null) image=load(imagePath);
        if(isRestored && restoredPath!=null && restored==null) restored=load(restoredPath);
        graphics.drawImage(isRestored && restored!=null ? restored : image,0,0,null);
    }
    private BufferedImage load(String path) {
        BufferedImage result=ImageAssets.load(path);
        if(result==null || result.getWidth()!=width || result.getHeight()!=height)
            throw new IllegalStateException("Missing or incorrectly sized world map: "+path);
        return result;
    }
    /** Nearest deterministic free footprint, used by authored encounter spawns. */
    public Point safePosition(int x, int y, int actorWidth, int actorHeight) {
        for(int radius=0;radius<=320;radius+=16) {
            for(int dy=-radius;dy<=radius;dy+=16) for(int dx=-radius;dx<=radius;dx+=16) {
                if(Math.max(Math.abs(dx),Math.abs(dy))!=radius)continue;
                int px=x+dx,py=y+dy;
                if(px>=32 && py>=32 && px+actorWidth<width-32 && py+actorHeight<height-32
                        && !util.Collision.intersectsAny(new Rectangle2D.Double(px,py,actorWidth,actorHeight),solids))
                    return new Point(px,py);
            }
        }
        throw new IllegalStateException("No safe encounter position in "+id);
    }
    /** Deterministic placement within an authored combat region, respecting actor footprint. */
    public Point enemyPosition(int regionIndex, int slot, int size) {
        Rectangle2D r=combatRegions.get(regionIndex % combatRegions.size());
        int columns=(int)r.getWidth()/32, rows=(int)r.getHeight()/32;
        int start=Math.floorMod(slot*7+columns*rows/2,columns*rows);
        for(int i=0;i<columns*rows;i++) {
            int cell=(start+i)%(columns*rows);
            int x=(int)r.getX()+(cell%columns)*32+16-size/2;
            int y=(int)r.getY()+(cell/columns)*32+16-size/2;
            if(!util.Collision.intersectsAny(new Rectangle2D.Double(x,y,size,size),solids))
                return new Point(x,y);
        }
        throw new IllegalStateException("No safe enemy spawn in "+id);
    }
}



