package ui;

import java.awt.*;

/** Small menu/death/ending screens, shared keyboard and mouse button geometry. */
public final class GameScreens {
    private GameScreens() { }
    public static Rectangle button(int width,int height,int row) {
        return new Rectangle(width/2-150,height/2+40+row*56,300,42);
    }
    public static void draw(Graphics2D g,int width,int height,String title,String subtitle,String... labels) {
        if(title.equals("A MOMENT OF QUIET")){drawPause(g,width,height,new Point(-1,-1));return;}
        g.setColor(new Color(5,12,25,178));g.fillRect(0,0,width,height);
        UITheme.letterbox(g,width,height,18);
        UITheme.panel(g,width/2-335,height/2-143,670,labels.length*56+210);
        g.setColor(UITheme.IVORY);g.setFont(new Font("Serif",Font.BOLD,Math.min(34,width/24)));center(g,title,width/2,height/2-90);
        g.setColor(UITheme.GOLD);g.drawLine(width/2-115,height/2-72,width/2+115,height/2-72);
        g.setColor(new Color(230,235,240));g.setFont(new Font("Dialog",Font.PLAIN,15));center(g,subtitle,width/2,height/2-45);
        for(int i=0;i<labels.length;i++) {
            Rectangle b=button(width,height,i);UITheme.panel(g,b.x,b.y,b.width,b.height);
            g.setColor(UITheme.IVORY);
            g.setFont(new Font("Dialog",Font.BOLD,16));center(g,labels[i],width/2,b.y+27);
        }
        g.setColor(new Color(200,215,230));g.setFont(new Font("Dialog",Font.PLAIN,12));
        center(g,"WASD / Arrows: move   |   Click: attack   |   Space: dash   |   1: Bell   |   E: interact   |   Esc: pause",width/2,height-35);
    }
    public static void drawPause(Graphics2D original,int width,int height,Point mouse){
        Graphics2D g=(Graphics2D)original.create();g.setColor(new Color(5,12,25,155));g.fillRect(0,0,width,height);
        var panel=new Rectangle((width-Math.min(560,width-40))/2,height/2-140,Math.min(560,width-40),280);
        ChristmasUI.frame(g,"parchment",panel);g.setColor(ChristmasUI.RED);g.setFont(new Font("Serif",Font.BOLD,27));
        center(g,"A MOMENT OF QUIET",width/2,height/2-75);g.setColor(ChristmasUI.INK);g.setFont(new Font("Serif",Font.ITALIC,17));
        center(g,"Rest here. The last light will wait.",width/2,height/2-30);
        var b=button(width,height,0);ChristmasUI.button(g,"RESUME [ENTER / ESC]",b,"red",b.contains(mouse),true);g.dispose();
    }
    private static void center(Graphics2D g,String text,int x,int y) { g.drawString(text,x-g.getFontMetrics().stringWidth(text)/2,y); }
}
