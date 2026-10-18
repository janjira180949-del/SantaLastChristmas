package entity;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.IdentityHashMap;
import java.util.Map;
import util.ImageAssets;

/** Cached enemy artwork. Movement uses the authored four-direction four-frame sets. */
final class EnemySprites {
    private final BufferedImage idle;
    private final BufferedImage attack;
    private final BufferedImage[][] movement = new BufferedImage[4][4];
    private final double scale;
    private final Map<BufferedImage, Rectangle> visibleBounds = new IdentityHashMap<>();
    private final Rectangle normalBounds;
    private final Map<BufferedImage,BufferedImage> rasters=new IdentityHashMap<>(),flashes=new IdentityHashMap<>();
    private final String character;
    private final String movementName;

    EnemySprites(String character, String movementName, int renderSize) {
        this.character = character;
        this.movementName = movementName;
        scale = renderSize / 448.0;
        idle = ImageAssets.load(path("idle", character + "_idle_down_01.png"));
        normalBounds = cacheBounds(idle);
        attack = (character.equals("walker") || character.equals("runner")) ? ImageAssets.load(path("attack", character + "_attack_down_01.png")) : null;
        cacheBounds(attack);
        String[] dirs = {"down", "up", "left", "right"};
        for (int d=0; d<dirs.length; d++) for (int f=0; f<4; f++)
            movement[d][f] = ImageAssets.load(path(movementName,
                character + "_" + movementName + "_" + dirs[d] + "_0" + (f+1) + ".png"));
        for (BufferedImage[] direction : movement)
            for (BufferedImage frame : direction) cacheBounds(frame);
        for(BufferedImage image:visibleBounds.keySet()) {
            if(image==null||normalBounds==null)continue;
            Rectangle bounds=visibleBounds.get(image);if(bounds==null)continue;
            BufferedImage nativeFrame=new BufferedImage(renderSize,renderSize,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=nativeFrame.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.scale(scale,scale);
            if(image!=idle) {g.translate(normalBounds.x,normalBounds.y);g.scale(normalBounds.width/(double)bounds.width,normalBounds.height/(double)bounds.height);g.translate(-bounds.x,-bounds.y);}
            g.drawImage(image,0,0,null);g.dispose();rasters.put(image,nativeFrame);flashes.put(image,util.PixelArt.silhouette(nativeFrame,util.LightBloom.IVORY));
        }
    }

    /** Measure once: equal canvas sizes do not imply equal character sizes. */
    private Rectangle cacheBounds(BufferedImage image) {
        if (image == null) return null;
        int left = image.getWidth(), top = image.getHeight(), right = -1, bottom = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    left = Math.min(left, x); top = Math.min(top, y);
                    right = Math.max(right, x); bottom = Math.max(bottom, y);
                }
            }
        }
        Rectangle bounds = right < left ? null
                : new Rectangle(left, top, right - left + 1, bottom - top + 1);
        visibleBounds.put(image, bounds);
        return bounds;
    }

    private String path(String folder, String file) {
        return "assets/characters/enemies/" + character + "/" + folder + "/" + file;
    }

    double top(Enemy enemy) { return enemy.getY() - 34; }

    boolean draw(Graphics2D graphics, Enemy enemy) {
        BufferedImage image;
        if (enemy.getVisualState() == Enemy.VisualState.MOVING) {
            int d = switch(enemy.getMovementDirection()) { case "up" -> 1; case "left" -> 2; case "right" -> 3; default -> 0; };
            image = movement[d][Math.min(3, enemy.getMovementAnimationFrame())];
        } else if (enemy.getVisualState() == Enemy.VisualState.ATTACK && attack != null) image = attack;
        else image = idle;
        if (image == null) return false;
        Rectangle bounds = visibleBounds.get(image);
        if (normalBounds == null || bounds == null) return false;
        // Keep the old idle canvas placement and scale as the canonical normal size.
        double drawW=idle.getWidth()*scale, drawH=idle.getHeight()*scale;
        Graphics2D g=(Graphics2D)graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            int x=(int)Math.round(enemy.getX()+enemy.getSize()/2.0-drawW/2.0),y=(int)Math.round(enemy.getY()+enemy.getSize()-drawH*.78);
            g.drawImage(rasters.get(image),x,y,null);
            if(enemy.getHitFlashFrames()>0) {g.setComposite(java.awt.AlphaComposite.SrcOver.derive(enemy.getHitFlashFrames()/12f));g.drawImage(flashes.get(image),x,y,null);}
        } finally {
            g.dispose();
        }
        return true;
    }
}
