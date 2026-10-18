package game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

/** A simple black transition zone between authored maps. */
public final class ExitZone {
    private final Rectangle2D bounds;
    private final Area.Type destination;
    private final String destinationName;

    public ExitZone(double x,double y,double width,double height,Area.Type destination,String destinationName){
        if(width<=0||height<=0||destination==null||destinationName==null||destinationName.isBlank())
            throw new IllegalArgumentException("Invalid exit zone");
        bounds=new Rectangle2D.Double(x,y,width,height);
        this.destination=destination; this.destinationName=destinationName;
    }
    public Area.Type getDestination(){return destination;}
    public String getDestinationName(){return destinationName;}
    public boolean intersects(Rectangle2D actorBounds){return bounds.intersects(actorBounds);}
    public Rectangle2D getBounds(){return (Rectangle2D)bounds.clone();}

    public void draw(Graphics2D graphics,boolean unlocked){draw(graphics,unlocked,Integer.MAX_VALUE,Integer.MAX_VALUE);}
    public void draw(Graphics2D graphics,boolean unlocked,int worldWidth,int worldHeight){
        int x=(int)bounds.getX(),y=(int)bounds.getY(),w=(int)bounds.getWidth(),h=(int)bounds.getHeight();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(x,y,w,h);
        if(!unlocked&&destination==Area.Type.CHRISTMAS_TOWN) {
            Graphics2D frost=(Graphics2D)graphics.create();
            frost.clip(bounds);
            frost.setColor(new Color(24,39,67,235));frost.fillRect(x,y,w,h);
            frost.setColor(new Color(119,150,204,175));
            for(int sy=y-8;sy<y+h+12;sy+=16) {
                frost.drawLine(x,sy,x+w,sy+16);frost.drawLine(x+w,sy,x,sy+16);
            }
            frost.dispose();
        }
        String label=(unlocked?"OPEN: ":"LOCKED: ")+destinationName;
        graphics.setFont(new java.awt.Font("Dialog",java.awt.Font.BOLD,12));
        int textWidth=graphics.getFontMetrics().stringWidth(label);
        int textX=Math.max(40,Math.min(worldWidth-textWidth-40,x+w/2-textWidth/2));
        int textY=y<64?y+h+45:y>worldHeight-64?y-25:y+h/2-65;
        graphics.setColor(new Color(8,14,24,220));graphics.fillRoundRect(textX-6,textY-16,textWidth+12,22,6,6);
        graphics.setColor(unlocked?new Color(170,240,205):new Color(245,155,145));graphics.drawString(label,textX,textY);
    }
    public void drawEntrance(Graphics2D graphics,boolean initialArea){
        int x=(int)bounds.getX(),y=(int)bounds.getY(),w=(int)bounds.getWidth(),h=(int)bounds.getHeight();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(x,y,w,h);
    }
}
