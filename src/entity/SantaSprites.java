package entity;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Locale;
import util.ImageAssets;

/** Cached gameplay artwork and visual-only anchors; never defines collision geometry. */
final class SantaSprites {
    private static final double SCALE = 64.0 / 384.0;
    private final BufferedImage[][][] frames = new BufferedImage[3][4][];
    private final int[][][] feet = new int[3][4][];
    private final BufferedImage[][][] rasters=new BufferedImage[3][4][], flashes=new BufferedImage[3][4][];

    SantaSprites() {
        String[] folders = {"idle", "walk", "attack"};
        for (Santa.AnimationState state : Santa.AnimationState.values()) {
            int s = state.ordinal();
            int count = s + 1;
            for (Santa.Direction direction : Santa.Direction.values()) {
                int d = direction.ordinal();
                frames[s][d] = new BufferedImage[count];
                feet[s][d] = new int[count];
                rasters[s][d]=new BufferedImage[count];flashes[s][d]=new BufferedImage[count];
                for (int f = 0; f < count; f++) {
                    String path = String.format(Locale.ROOT,
                            "assets/characters/santa/%s/santa_%s_%s_%02d.png",
                            folders[s], folders[s], direction.name().toLowerCase(Locale.ROOT), f + 1);
                    BufferedImage image = ImageAssets.load(path);
                    frames[s][d][f] = image;
                    feet[s][d][f] = visibleBottom(image);
                    rasters[s][d][f]=util.PixelArt.scale(image,64,64);
                    flashes[s][d][f]=util.PixelArt.silhouette(rasters[s][d][f],util.LightBloom.IVORY);
                }
            }
        }
    }

    // Ignore near-transparent edge noise. Keep a fixed horizontal canvas center:
    // centering each frame's silhouette would shift Santa when his weapon extends.
    private static int visibleBottom(BufferedImage image) {
        if (image == null) return 384;
        for (int y = image.getHeight() - 1; y >= 0; y--) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) >= 16) return y + 1;
            }
        }
        return image.getHeight();
    }

    boolean draw(Graphics2D graphics, Santa.AnimationState state, Santa.Direction direction,
            int frame, double anchorX, double anchorY) {
        BufferedImage image = rasters[state.ordinal()][direction.ordinal()][frame];
        if (image == null) return false;
        double bottom = feet[state.ordinal()][direction.ordinal()][frame];
        Graphics2D visual = (Graphics2D) graphics.create();
        visual.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        visual.drawImage(image,(int)Math.round(anchorX-32),(int)Math.round(anchorY-bottom*SCALE),null);
        visual.dispose();
        return true;
    }
    void drawFlash(Graphics2D g,Santa.AnimationState state,Santa.Direction direction,int frame,double x,double y) {
        BufferedImage image=flashes[state.ordinal()][direction.ordinal()][frame];
        if(image!=null)g.drawImage(image,(int)Math.round(x-32),(int)Math.round(y-feet[state.ordinal()][direction.ordinal()][frame]*SCALE),null);
    }
}
