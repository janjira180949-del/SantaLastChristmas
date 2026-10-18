package game;

import entity.Santa;
import java.awt.*;
import util.LightBloom;

/** Bounded, skippable visual handover. Delivery flags belong exclusively to VillageActivities. */
public final class GiftDeliveryPresentation {
    public static final int DURATION=132;
    private static final Font REACTION_FONT=new Font("Dialog",Font.BOLD,11);
    private VillageActivities.Site recipient;
    private int frame;
    private double fromX,fromY;
    public void start(Santa santa,VillageActivities.Site site){recipient=site;frame=0;fromX=santa.getX()+16;fromY=santa.getY()+15;}
    public boolean isActive(){return recipient!=null;}
    public VillageActivities.Site recipient(){return recipient;}
    public int frame(){return frame;}
    public boolean update(boolean skip){if(!isActive())return false;frame=skip?DURATION:frame+1;return frame>=DURATION;}
    public void clear(){recipient=null;frame=0;}
    public Santa.Direction facing(){double dx=recipient.x-fromX,dy=recipient.y-fromY;return Math.abs(dx)>Math.abs(dy)?dx<0?Santa.Direction.LEFT:Santa.Direction.RIGHT:dy<0?Santa.Direction.UP:Santa.Direction.DOWN;}
    public int reactionOffset(){return recipient==VillageActivities.Site.BEN&&frame>75?(int)(-5*Math.abs(Math.sin((frame-75)/9.0))):0;}
    public void draw(Graphics2D original){
        if(!isActive())return;
        Graphics2D g=(Graphics2D)original.create();
        double t=Math.min(1,Math.max(0,(frame-18)/66.0)),ease=t*t*(3-2*t);
        double x=fromX+(recipient.x-fromX)*ease,y=fromY+(recipient.y-8-fromY)*ease-Math.sin(Math.PI*t)*13;
        if(frame<100){var image=VillageActivityVisuals.presentImage(recipient.ordinal()-3);if(image!=null)g.drawImage(image,(int)x-13,(int)y-16,26,26,null);else ui.VillageActivityUI.present(g,recipient.ordinal()-3,(int)x-13,(int)y-16,26);}
        if(frame>=72){
            double fade=Math.max(0,1-(frame-72)/60.0);LightBloom.draw(g,recipient.x,recipient.y,48,.12*fade,LightBloom.GOLD);
            g.setColor(new Color(255,220,132,(int)(180*fade)));
            for(int i=0;i<10;i++){double angle=i*Math.PI/5,reach=8+(frame-72)*.35;int sx=recipient.x+(int)(Math.cos(angle)*reach),sy=recipient.y-8+(int)(Math.sin(angle)*reach*.65);g.fillOval(sx,sy,3,3);}
        }
        if(frame>84){String reaction=switch(recipient){case IRIS->"For me?";case CLARA->"Thank you...";default->"Wow!";};g.setFont(REACTION_FONT);int w=g.getFontMetrics().stringWidth(reaction);g.setColor(new Color(12,22,35,210));g.fillRoundRect(recipient.x-w/2-5,recipient.y-70,w+10,20,7,7);g.setColor(new Color(255,224,158));g.drawString(reaction,recipient.x-w/2,recipient.y-56);}
        g.dispose();
    }
}
