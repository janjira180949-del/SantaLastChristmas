package game;

import java.awt.*;
import java.awt.image.BufferedImage;

/** Small cached pixel artwork, authored at final scale. No decoding/filtering during paint. */
public final class SkillArtwork {
    private static final Color GOLD=new Color(255,199,66),IVORY=new Color(255,250,200),ORANGE=new Color(207,104,28);
    private static final BufferedImage STAR=makeStar(28),GIFT=makeGift(),SPARK=makeStar(8),WHITE=makeStar(6);
    private static final BufferedImage[] BELL=new BufferedImage[24],BURST=new BufferedImage[24],BOMB=new BufferedImage[24];
    private static final BufferedImage[][] STARS=new BufferedImage[3][16];
    private static final BufferedImage[] GIFTS=new BufferedImage[16],CAST=new BufferedImage[12];
    private static final BufferedImage[] TRAIL=new BufferedImage[4];
    static {
        for(int i=0;i<24;i++){BELL[i]=wave(i);BURST[i]=makeBurst(i,false);BOMB[i]=makeBurst(i,true);}
        for(int i=0;i<16;i++){
            GIFTS[i]=rotation(GIFT,i*Math.PI/8,36,0);
            for(int n=0;n<3;n++)STARS[n][i]=rotation(STAR,i*Math.PI/8,40,n);
        }
        for(int i=0;i<12;i++)CAST[i]=castFrame(i);
        for(int i=0;i<4;i++){TRAIL[i]=image(8,8);var g=TRAIL[i].createGraphics();g.setComposite(AlphaComposite.SrcOver.derive((4-i)/5f));g.drawImage(i%2==0?SPARK:WHITE,0,0,null);g.dispose();}
        BossArenaVisuals.prepare();
    }
    private SkillArtwork(){}
    public static void prepare() { /* Class initialization prepares every reusable frame. */ }
    public static BufferedImage star(){return STAR;}
    public static BufferedImage gift(){return GIFT;}
    public static BufferedImage spark(int i){return i==0?SPARK:WHITE;}
    public static BufferedImage trail(int i){return TRAIL[i];}
    private static BufferedImage image(int w,int h){return new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);}
    private static BufferedImage makeStar(int size){var im=image(size,size);var g=im.createGraphics();int c=size/2;int[] x=new int[10],y=new int[10];
        for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5,r=i%2==0?c-1:c*.43;x[i]=c+(int)(Math.cos(a)*r);y[i]=c+(int)(Math.sin(a)*r);}
        g.setColor(ORANGE);g.fillPolygon(x,y,10);g.setColor(GOLD);for(int i=0;i<10;i++){x[i]=c+(int)((x[i]-c)*.8);y[i]=c+(int)((y[i]-c)*.8);}g.fillPolygon(x,y,10);
        g.setColor(IVORY);g.fillPolygon(new int[]{c,c+2,c,c-3},new int[]{2,c,c+5,c},4);g.fillRect(c-2,c-2,4,4);g.dispose();return im;
    }
    private static BufferedImage makeGift(){var im=image(26,26);var g=im.createGraphics();g.setColor(new Color(71,30,26));g.fillRect(3,7,21,17);
        g.setColor(new Color(186,48,53));g.fillRect(4,9,19,13);g.setColor(new Color(32,117,72));g.fillPolygon(new int[]{3,18,23,8},new int[]{7,4,8,11},4);
        g.setColor(GOLD);g.fillRect(11,9,4,14);g.fillPolygon(new int[]{10,14,19,15},new int[]{6,5,9,10},4);g.fillRect(3,8,21,3);
        g.setColor(IVORY);g.drawLine(5,12,5,18);g.fillRect(12,9,1,12);g.setColor(ORANGE);g.drawRect(6,1,6,4);g.drawRect(13,1,6,4);g.dispose();return im;}
    private static BufferedImage wave(int age){var im=image(270,270);var g=im.createGraphics();g.setComposite(AlphaComposite.SrcOver.derive(Math.min(1,(24-age)/9f)));double r=28+Math.min(15,age)*(97.0/15);
        for(int i=0;i<80;i++){double a=i*Math.PI*2/80;int x=135+(int)(Math.cos(a)*r),y=135+(int)(Math.sin(a)*r);
            g.setColor(new Color(255,211,107,Math.max(0,190-age*6)));g.fillRect(x-2,y-2,4,3);
            if(i%3!=0){g.setColor(new Color(236,153,49,Math.max(0,150-age*5)));g.fillRect(135+(int)(Math.cos(a)*(r-8))-1,135+(int)(Math.sin(a)*(r-8))-1,3,2);}
            if(i%5==0){g.setColor(IVORY);g.fillRect(x,y-1,2,2);}
            if(i%8==0)g.drawImage(i%16==0?SPARK:WHITE,x-4,y-4,null);}
        // Warm beveled facets and a second broken ring retain pixel edges without a filled halo.
        for(int i=0;i<24;i++){
            double a=i*Math.PI/12;int x=135+(int)(Math.cos(a)*(r-4)),y=135+(int)(Math.sin(a)*(r-4));
            g.setColor(new Color(227,137,42,Math.max(0,180-age*5)));g.fillPolygon(new int[]{x-4,x,x+4,x},new int[]{y,y-3,y,y+3},4);
            g.setColor(new Color(255,240,162,Math.max(0,220-age*7)));g.fillRect(x-1,y-2,2,3);
            if(i%3==0&&age<18)g.drawImage(SPARK,x-4,y-4,null);
            int ix=135+(int)(Math.cos(a)*(r-17)),iy=135+(int)(Math.sin(a)*(r-17));g.setColor(new Color(255,215,100,Math.max(0,140-age*5)));g.fillRect(ix-1,iy-1,3,2);
        }
        if(age<7){g.setColor(IVORY);g.fillArc(125,124,20,18,0,180);g.fillRect(123,132,24,5);g.setColor(GOLD);g.fillRect(133,137,4,3);g.drawRect(132,119,6,4);}
        g.dispose();return im;}
    private static BufferedImage makeBurst(int age,boolean bomb){int size=bomb?304:90;var im=image(size,size);var g=im.createGraphics();int c=size/2;g.setComposite(AlphaComposite.SrcOver.derive((24-age)/24f));
        int n=bomb?32:12;for(int i=0;i<n;i++){double a=i*Math.PI*2/n+.17,r=bomb?Math.min(130,12+age*7):5+age*1.4;int x=c+(int)(Math.cos(a)*r),y=c+(int)(Math.sin(a)*r);
            g.setColor(i%3==0&&bomb?new Color(177,45,55,255-age*10):i%3==1&&bomb?new Color(53,147,88,255-age*10):new Color(255,213,93,255-age*10));g.fillRect(x,y,3,3);
            if(i%4==0&&age<18)g.drawImage(bomb&&i%8==0?STAR:SPARK,x-(bomb&&i%8==0?14:4),y-(bomb&&i%8==0?14:4),null);}
        if(bomb){
            double radius=Math.min(130,18+age*9);
            for(int i=0;i<96;i++){double a=i*Math.PI/48;int x=c+(int)(Math.cos(a)*radius),y=c+(int)(Math.sin(a)*radius);
                g.setColor(i%3==0?new Color(131,232,252):GOLD);g.fillRect(x-1,y-1,3,3);
                if(i%12==0){g.setColor(IVORY);g.drawLine(x-5,y,x+5,y);g.drawLine(x,y-5,x,y+5);g.drawLine(x-3,y-3,x+3,y+3);}
            }
            for(int i=0;i<16;i++){
                double a=i*Math.PI/8+.12,r=Math.min(116,14+age*6);int x=c+(int)(Math.cos(a)*r),y=c+(int)(Math.sin(a)*r);
                g.setColor(i%3==0?new Color(230,68,81):i%3==1?new Color(94,216,143):new Color(161,237,255));
                g.fillPolygon(new int[]{x-4,x,x+4,x},new int[]{y-2,y-5,y+2,y+5},4);
                g.setColor(IVORY);g.fillRect(x,y-2,2,2);
            }
        }
        if(age<6){g.setColor(IVORY);g.fillRect(c-3,c-10,6,20);g.fillRect(c-10,c-3,20,6);}g.dispose();return im;}
    public static void bell(Graphics2D g,double x,double y,int remaining){g.drawImage(BELL[Math.min(23,24-remaining)],(int)x-135,(int)y-135,null);}
    public static void burst(Graphics2D g,double x,double y,int age,boolean bomb){var im=(bomb?BOMB:BURST)[age];g.drawImage(im,(int)x-im.getWidth()/2,(int)y-im.getHeight()/2,null);}
    public static void warning(Graphics2D g,double x,double y,double radius,int age){g.setColor(age%12<6?GOLD:new Color(190,67,63));for(int i=0;i<48;i++){double a=i*Math.PI/24;g.fillRect((int)(x+Math.cos(a)*radius)-1,(int)(y+Math.sin(a)*radius)-1,3,3);}}
    private static BufferedImage rotation(BufferedImage source,double angle,int size,int tint){
        var im=image(size,size);var g=im.createGraphics();
        g.translate(size/2.0,size/2.0);g.rotate(angle);
        g.drawImage(source,-source.getWidth()/2,-source.getHeight()/2,null);
        if(source==STAR){g.setColor(tint==0?new Color(142,234,255):tint==1?IVORY:new Color(252,173,85));g.fillRect(-2,-13,3,4);g.fillRect(8,3,3,3);g.setColor(IVORY);g.fillRect(-1,-1,3,3);}
        g.dispose();return im;
    }
    private static BufferedImage castFrame(int age){
        var im=image(80,80);var g=im.createGraphics();g.setComposite(AlphaComposite.SrcOver.derive((12-age)/12f));
        for(int i=0;i<8;i++){double a=i*Math.PI/4,r=12+age;int x=40+(int)(Math.cos(a)*r),y=40+(int)(Math.sin(a)*r);g.drawImage(i%2==0?SPARK:WHITE,x-4,y-4,null);}
        g.setColor(new Color(142,234,255));g.fillRect(38,22,3,5);g.fillRect(20,38,5,3);g.dispose();return im;
    }
    public static void drawStar(Graphics2D g,double x,double y,int age,int color){g.drawImage(STARS[color%3][age%16],(int)x-20,(int)y-20,null);}
    public static void drawGift(Graphics2D g,double x,double y,int age){g.drawImage(GIFTS[(age/2)%16],(int)x-18,(int)y-18,null);}
    public static void cast(Graphics2D original,double x,double y,double angle,int age,int slot){
        Graphics2D g=(Graphics2D)original.create();g.translate(x,y);g.rotate(angle);
        g.drawImage(CAST[Math.min(11,age)],-40,-40,null);
        g.drawImage(slot==3?GIFT:STARS[0][0],10,-13,null);g.dispose();
    }
    public static void landing(Graphics2D g,double sx,double sy,double x,double y,double radius,int age){
        warning(g,x,y,radius,age);
        if(age<20){g.setColor(new Color(255,222,143,150));for(int i=1;i<9;i++){double t=i/9.0;g.fillRect((int)(sx+(x-sx)*t),(int)(sy+(y-sy)*t-Math.sin(t*Math.PI)*38),2,2);}}
        g.setColor(IVORY);for(int i=0;i<4;i++){double a=i*Math.PI/2;g.drawImage(SPARK,(int)(x+Math.cos(a)*radius)-4,(int)(y+Math.sin(a)*radius)-4,null);}
    }
}
