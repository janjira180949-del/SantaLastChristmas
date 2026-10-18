package quest;

import entity.Santa;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/** A one-time Forest interaction target for the current story quest. */
public final class StoryQuestTarget {

    // The clean-map item artwork extends above its authored ground point.
    private static final double INTERACTION_RANGE = 58.0;

    private final StoryQuest.Id questId;
    private final String label;
    private final double x;
    private final double y;
    private final Color color;
    private boolean collected;
    private BufferedImage collectedPatch;
    private util.PickupVisual pickup;
    private BufferedImage snapshot;

    public StoryQuestTarget(StoryQuest.Id questId, String label,
            double x, double y, Color color) {
        if (questId == null || label == null || label.isBlank() || color == null) {
            throw new IllegalArgumentException("Story target is incomplete");
        }
        this.questId = questId;
        this.label = label;
        this.x = x;
        this.y = y;
        this.color = color;
    }

    public StoryQuest.Id getQuestId() {
        return questId;
    }

    public boolean isCollected() {
        return collected;
    }

    public Rectangle2D getBounds() {
        return new Rectangle2D.Double(x - 12, y - 14, 24, 28);
    }

    public boolean canInteract(Santa santa) {
        if (santa == null || collected) {
            return false;
        }
        double santaCenterX = santa.getX() + 16.0;
        double santaCenterY = santa.getY() + 21.0;
        return Math.hypot(santaCenterX - x, santaCenterY - y) <= INTERACTION_RANGE;
    }

    public boolean collect() {
        if (collected) {
            return false;
        }
        prepareVisual();
        pickup = new util.PickupVisual(snapshot, x-48, y-88);
        collected = true;
        return true;
    }

    /** Cached separately so hiding an inactive baked page cannot start a pickup effect. */
    private void prepareVisual() {
        if (collectedPatch != null) return;
        BufferedImage forest = game.FrozenForestVisuals.sourceForPresentation();
        snapshot = new BufferedImage(96, 128, BufferedImage.TYPE_INT_ARGB);
        collectedPatch = new BufferedImage(96, 128 + repairTopExtension(), BufferedImage.TYPE_INT_ARGB);
        for (int py=0; py<128; py++) for (int px=0; px<96; px++) {
            if (forest == null) continue; // Unavailable artwork keeps collection/timeline playable without decoding here.
            int wx=(int)x-48+px, wy=(int)y-88+py;
            int rgb=forest.getRGB(wx,wy);
            if (!isGround(rgb)) snapshot.setRGB(px,py,rgb);
        }
        // All three authored targets sit wholly inside the path. Restore their terrain
        // window from the exact world-space material, including light sprite pixels
        // that the snapshot's color classifier cannot distinguish from snow.
        for (int py=0; py<collectedPatch.getHeight(); py++) for (int px=0; px<96; px++) {
            collectedPatch.setRGB(px,py,game.RoadSnowTexture.pixel((int)x-48+px,(int)y-88-repairTopExtension()+py));
        }
    }

    private int repairTopExtension() { return 0; }

    public void drawInactivePatch(Graphics2D graphics) {
        prepareVisual();
        graphics.drawImage(collectedPatch,(int)x-48,(int)y-88-repairTopExtension(),null);
    }

    public util.PickupVisual getPickupVisual() { return pickup; }
    public void updateVisual() { if (pickup != null) pickup.update(); }
    public void drawPickup(Graphics2D graphics) { if (pickup != null) pickup.draw(graphics); }

    public void draw(Graphics2D graphics, boolean nearby) {
        if (collected) {
            return;
        }
        int objectX = (int) x;
        int objectY = (int) y;
        graphics.setColor(new Color(17, 22, 30, 100));
        graphics.fill(new Ellipse2D.Double(objectX - 18, objectY + 14, 38, 10));
        graphics.setColor(color);
        graphics.fillRoundRect(objectX - 11, objectY - 9, 22, 20, 5, 5);
        graphics.setColor(new Color(255, 231, 158));
        graphics.fillRect(objectX - 2, objectY - 8, 4, 18);
        graphics.fillRect(objectX - 10, objectY - 1, 20, 4);

        drawMarker(graphics, nearby);
    }




    private static boolean isGround(int rgb) {
        int r=(rgb>>>16)&255, g=(rgb>>>8)&255, b=rgb&255;
        return (Math.abs(r-g)<22 && Math.abs(g-b)<22 && r>180)
            || rgb==0xff81919f || rgb==0xff6e8f9c || rgb==0xff244151;
    }

    /** Restores terrain under the entire baked item, including its formerly uncovered top. */
    public void drawCollectedPatch(Graphics2D graphics) {
        if (collected && collectedPatch!=null)
            graphics.drawImage(collectedPatch,(int)x-48,(int)y-88-repairTopExtension(),null);
    }

    /** Clean map artwork already contains the item; draw only its interaction label. */
    public void drawMarker(Graphics2D graphics, boolean nearby) {
        if (collected) return;
        int objectX = (int) x;
        int objectY = (int) y;
        graphics.setColor(new Color(35, 41, 48));
        graphics.drawString(label, objectX - 34, objectY - 19);
        if (nearby) {
            graphics.setColor(new Color(255, 225, 145));
            graphics.drawOval(objectX - 18, objectY - 19, 36, 38);
            graphics.drawString("E", objectX - 3, objectY + 31);
        }
    }
}


