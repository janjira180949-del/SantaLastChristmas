package entity;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Locale;
import util.ImageAssets;

/** Three cached stationary poses per child, separate from interaction geometry. */
final class ChildSprites {
    private static final double SCALE = 56.0 / 448.0;
    private final BufferedImage[][] frames = new BufferedImage[Child.Kind.values().length][3];
    private final int[][] feet = new int[Child.Kind.values().length][3];
    private final BufferedImage[][] rasters=new BufferedImage[Child.Kind.values().length][3];

    ChildSprites() {
        for (Child.Kind kind : Child.Kind.values()) {
            String character = kind.name().toLowerCase(Locale.ROOT);
            for (Child.VisualState state : Child.VisualState.values()) {
                String pose = state.name().toLowerCase(Locale.ROOT);
                BufferedImage image = ImageAssets.load("assets/characters/children/" + character
                        + "/" + pose + "/" + character + "_" + pose + "_down_01.png");
                frames[kind.ordinal()][state.ordinal()] = image;
                feet[kind.ordinal()][state.ordinal()] = visibleBottom(image);
                rasters[kind.ordinal()][state.ordinal()]=util.PixelArt.scale(image,56,56);
            }
        }
    }

    private static int visibleBottom(BufferedImage image) {
        if (image == null) return 448;
        for (int y = image.getHeight() - 1; y >= 0; y--) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) >= 16) return y + 1;
            }
        }
        return image.getHeight();
    }

    boolean draw(Graphics2D graphics, Child.Kind kind, Child.VisualState state,
            double anchorX, double anchorY) {
        BufferedImage image = rasters[kind.ordinal()][state.ordinal()];
        if (image == null) return false;
        Graphics2D visual = (Graphics2D) graphics.create();
        visual.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        visual.drawImage(image,(int)Math.round(anchorX-28),(int)Math.round(anchorY-feet[kind.ordinal()][state.ordinal()]*SCALE),null);
        visual.dispose();
        return true;
    }
}
