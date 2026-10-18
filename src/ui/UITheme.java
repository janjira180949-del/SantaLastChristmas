package ui;

import java.awt.*;

/** Shared navy/ivory/gold language with quiet pixel-stepped corners. */
public final class UITheme {
    public static final Color NAVY=new Color(8,17,32),IVORY=new Color(246,236,212),GOLD=new Color(217,183,125),MUTED=new Color(169,187,202);
    private UITheme() { }
    public static void panel(Graphics2D g,int x,int y,int w,int h) {
        g.setColor(new Color(8,17,32,238));g.fillRect(x+4,y,w-8,h);g.fillRect(x,y+4,w,h-8);
        g.setColor(new Color(98,91,78));g.drawLine(x+6,y,x+w-7,y);g.drawLine(x+6,y+h-1,x+w-7,y+h-1);g.drawLine(x,y+6,x,y+h-7);g.drawLine(x+w-1,y+6,x+w-1,y+h-7);
        g.setColor(GOLD);g.drawLine(x+8,y+3,x+32,y+3);g.drawLine(x+w-33,y+3,x+w-9,y+3);
    }
    public static void centered(Graphics2D g,String text,int x,int y) {g.drawString(text,x-g.getFontMetrics().stringWidth(text)/2,y);}
    public static void letterbox(Graphics2D g,int w,int h,int size) {g.setColor(new Color(3,8,17,225));g.fillRect(0,0,w,size);g.fillRect(0,h-size,w,size);}
}
