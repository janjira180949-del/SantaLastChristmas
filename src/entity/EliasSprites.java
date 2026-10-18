package entity;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import util.ImageAssets;

/** Four supplied static poses, normalized once without changing NPC geometry. */
final class EliasSprites {
    static final int CANVAS=64, FEET_X=32, FEET_Y=60, VISIBLE_HEIGHT=52;
    private final Map<Elias.Direction,BufferedImage> frames=new EnumMap<>(Elias.Direction.class);

    EliasSprites() {
        for(Elias.Direction direction:Elias.Direction.values()) {
            String name=direction.name().toLowerCase(Locale.ROOT);
            BufferedImage source=ImageAssets.load("assets/characters/elias/idle/elias_idle_"+name+"_01.png");
            if(source==null)throw new IllegalStateException("Missing Elias pose: "+name);
            Rectangle body=visibleBounds(source);
            if(body.isEmpty())throw new IllegalStateException("Empty Elias pose: "+name);
            int height=VISIBLE_HEIGHT,width=Math.max(1,(int)Math.round(body.width*height/(double)body.height));
            if(width>CANVAS-4)throw new IllegalStateException("Elias pose proportions do not fit native canvas");
            BufferedImage frame=new BufferedImage(CANVAS,CANVAS,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=frame.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(source,FEET_X-width/2,FEET_Y-height,FEET_X-width/2+width,FEET_Y,
                    body.x,body.y,body.x+body.width,body.y+body.height,null);
            } finally {g.dispose();}
            frames.put(direction,frame);
        }
    }
    private static Rectangle visibleBounds(BufferedImage image) {
        int left=image.getWidth(),top=image.getHeight(),right=-1,bottom=-1;
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if((image.getRGB(x,y)>>>24)>=16) {
            left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);
        }
        return right<left?new Rectangle():new Rectangle(left,top,right-left+1,bottom-top+1);
    }
    void draw(Graphics2D original,Elias.Direction direction,double feetX,double feetY) {
        Graphics2D g=(Graphics2D)original.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(frames.get(direction),(int)Math.round(feetX)-FEET_X,(int)Math.round(feetY)-FEET_Y,null);
        } finally {g.dispose();}
    }
}
