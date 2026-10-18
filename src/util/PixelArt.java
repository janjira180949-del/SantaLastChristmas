package util;

import java.awt.*;
import java.awt.image.BufferedImage;

/** One-time raster preparation. Sprites are never blurred or resampled per frame. */
public final class PixelArt {
    private PixelArt() { }
    public static BufferedImage scale(BufferedImage source,int width,int height) {
        if(source==null)return null;
        BufferedImage result=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(source,0,0,width,height,null);g.dispose();return result;
    }
    public static BufferedImage silhouette(BufferedImage source,Color color) {
        if(source==null)return null;
        BufferedImage result=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=result.createGraphics();g.drawImage(source,0,0,null);
        g.setComposite(AlphaComposite.SrcIn);g.setColor(color);g.fillRect(0,0,result.getWidth(),result.getHeight());g.dispose();return result;
    }
}
