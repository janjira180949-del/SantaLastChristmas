package game;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.Random;
import util.ImageAssets;
import util.LightBloom;

/** Authored workshop materials and silent machinery; composed once off the game thread. */
final class SantaBaseEnvironmentLayers {
    static final int WIDTH=1600, HEIGHT=1280;
    private final Random random=new Random(0x4c415354574f524bL);
    private final BufferedImage present=asset("decorations/present"), pine=asset("nature/pine"),
        rock=asset("nature/rock"), bench=asset("decorations/bench");
    private final BufferedImage[] illustrations=illustrations();
    private final BufferedImage[] materials=materials();
    private static BufferedImage[] materials() {
        BufferedImage atlas=ImageAssets.load("assets/world/santa_base/workshop_materials.png");
        BufferedImage[] tiles=new BufferedImage[4];
        if(atlas==null||atlas.getWidth()!=1254||atlas.getHeight()!=1254)return tiles;
        for(int i=0;i<4;i++) {
            BufferedImage cell=atlas.getSubimage(i%2*627,i/2*627,627,627);
            if(i==3){tiles[i]=cell;continue;}
            BufferedImage joined=new BufferedImage(1254,1254,BufferedImage.TYPE_INT_RGB);Graphics2D g=joined.createGraphics();
            try {
                g.drawImage(cell,0,0,null);g.drawImage(cell,1254,0,-627,627,null);
                g.drawImage(cell,0,1254,627,-627,null);g.drawImage(cell,1254,1254,-627,-627,null);
            } finally {g.dispose();}
            tiles[i]=joined;
        }
        return tiles;
    }
    private boolean material(Graphics2D g,int index,int x,int y,int width,int height,int size) {
        if(materials[index]==null)return false;
        g.setPaint(new TexturePaint(materials[index],new Rectangle(0,0,size,size)));g.fillRect(x,y,width,height);return true;
    }
    private static BufferedImage[] illustrations() {
        BufferedImage atlas=ImageAssets.load("assets/world/santa_base/workshop_props.png");
        BufferedImage[] parts=new BufferedImage[6];
        if(atlas==null||atlas.getWidth()!=1536||atlas.getHeight()!=1024)return parts;
        for(int i=0;i<6;i++) {
            int left=i%3*512,top=i/3*512,minX=512,minY=512,maxX=-1,maxY=-1;
            for(int y=0;y<512;y++)for(int x=0;x<512;x++)if((atlas.getRGB(left+x,top+y)>>>24)>32){minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);}
            if(maxX>=minX)parts[i]=atlas.getSubimage(left+minX,top+minY,maxX-minX+1,maxY-minY+1);
        }
        return parts;
    }
    private boolean illustrated(Graphics2D g,int part,int x,int y,int width,int height) {
        if(illustrations[part]==null)return false;
        shadow(g,x-6,y+height-36,width+12,49,.25f);
        g.drawImage(illustrations[part],x,y,width,height,null);return true;
    }
    private static BufferedImage asset(String name) {
        return ImageAssets.load("SantaLastChristmas_MapPackage/assets/environment/"+name+".png");
    }
    static BufferedImage build() {return new SantaBaseEnvironmentLayers().compose();}
    private BufferedImage compose() {
        BufferedImage image=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            snow(g); floor(g); walls(g);
            carpet(g,700,665,208,214); carpet(g,728,502,144,105);
            if(!illustrated(g,1,1056,112,288,208))shelf(g,1056,192,288,128);
            shelf(g,256,176,256,64);
            if(!illustrated(g,0,256,212,256,236))machine(g,256,256,256,192,false);
            if(!illustrated(g,0,1056,340,224,204))machine(g,1056,416,224,128,true);
            if(!illustrated(g,3,288,638,224,162))letterDesk(g);
            if(!illustrated(g,4,1056,670,224,162))journeyMap(g);
            if(!illustrated(g,2,674,160,284,320))christmasTree(g);
            if(!illustrated(g,5,936,284,80,164))spiritLantern(g,976,432);
            plaque(g,"GIFT STORAGE",1200,334);plaque(g,"LETTERS",400,817);plaque(g,"THE JOURNEY",1168,851);plaque(g,"SPIRIT LANTERN",976,471);
            details(g);
            exterior(g);
            for(int[] p:new int[][]{{240,560},{1357,576},{608,210},{992,210},{544,846},{1024,846}}) lantern(g,p[0],p[1]);
            plaque(g,"THE LAST WORKSHOP",800,137);
            return image;
        } finally {g.dispose();}
    }
    private void snow(Graphics2D g) {
        BufferedImage snow=ImageAssets.load("assets/world/ground/snow_base.png");
        g.setColor(new Color(77,108,150));g.fillRect(0,0,WIDTH,HEIGHT);
        if(snow!=null){g.setPaint(new TexturePaint(snow,new Rectangle(0,0,320,320)));g.fillRect(0,0,WIDTH,HEIGHT);}
        material(g,2,0,0,WIDTH,HEIGHT,768);
        g.setColor(new Color(20,44,82,100));g.fillRect(0,0,WIDTH,HEIGHT);
        for(int i=0;i<2200;i++) {
            int x=random.nextInt(WIDTH),y=random.nextInt(HEIGHT);
            g.setColor(new Color(186,211,232,random.nextInt(22)+8));g.fillOval(x,y,2+random.nextInt(6),1+random.nextInt(3));
        }
        shadow(g,130,100,1340,960,.30f);
    }
    private void floor(Graphics2D g) {
        g.setColor(new Color(38,27,28));g.fillRect(160,96,1280,896);
        for(int x=224;x<1376;x+=32)for(int y=160;y<928;y+=128) {
            int offset=(x/32%2)*64, top=Math.max(160,y-offset), bottom=Math.min(928,y+128-offset);
            wood(g,x,top,31,bottom-top,random.nextInt(12)-6);
            g.setColor(new Color(20,16,20,140));g.fillRect(x+2,top+4,2,2);g.fillRect(x+25,bottom-5,2,2);
        }
        material(g,0,224,160,1152,768,768);
        // Broad wear follows the original travel axis instead of a repeated tile pattern.
        g.setPaint(new RadialGradientPaint(810,690,480,new float[]{0,.7f,1},new Color[]{new Color(204,153,101,25),new Color(165,112,71,10),new Color(80,48,29,0)}));
        g.fillRect(224,160,1152,768);
        for(int i=0;i<650;i++) {
            int x=232+random.nextInt(1136),y=168+random.nextInt(752);
            g.setColor(new Color(205,161,110,random.nextInt(13)+4));g.drawLine(x,y,x+random.nextInt(8)+2,y+random.nextInt(3));
        }
        wood(g,704,928,192,64,0);
        for(int i=0;i<75;i++){int x=710+random.nextInt(180),y=930+random.nextInt(56);g.setColor(new Color(195,215,227,random.nextInt(70)+12));g.fillOval(x,y,5,2);}
    }
    private void wood(Graphics2D g,int x,int y,int w,int h,int tone) {
        g.setPaint(new GradientPaint(x,y,new Color(102+tone,66+tone/2,44+tone/2),x+w,y+h,new Color(65+tone,43+tone/2,35+tone/2)));
        g.fillRect(x,y,w,h);g.setColor(new Color(211,158,101,70));g.drawLine(x,y,x+w-1,y);g.drawLine(x,y,x,y+h-1);
        for(int i=0;i<Math.max(4,w*h/140);i++) {
            int px=x+random.nextInt(Math.max(1,w)),py=y+random.nextInt(Math.max(1,h));
            g.setColor(i%3==0?new Color(28,18,17,50):new Color(207,157,102,26));
            g.drawLine(px,py,px,Math.min(y+h-1,py+random.nextInt(24)+5));
        }
        if(w>15&&h>18&&random.nextInt(5)==0){g.setColor(new Color(36,24,22,90));g.drawOval(x+w/2-3,y+h/3,6,12);}
    }
    private void walls(Graphics2D g) {
        int[][] walls={{160,96,1280,64},{160,160,64,768},{1376,160,64,768},{160,928,544,64},{896,928,544,64}};
        for(int[] w:walls) {
            g.setColor(new Color(32,27,35));g.fillRect(w[0]-4,w[1]-4,w[2]+8,w[3]+12);
            for(int y=w[1];y<w[1]+w[3];y+=16)for(int x=w[0];x<w[0]+w[2];x+=32) {
                int tone=random.nextInt(18);g.setColor(new Color(70+tone,65+tone,67+tone));g.fillRoundRect(x+1,y+1,30,14,5,5);
                g.setColor(new Color(124,117,112,100));g.drawLine(x+4,y+2,x+25,y+2);
            }
            material(g,1,w[0],w[1],w[2],w[3],384);
            if(w[2]>w[3]){wood(g,w[0],w[1]+5,w[2],28,-13);garland(g,w[0]+14,w[1]+27,w[2]-28);}
            else {wood(g,w[0]+18,w[1],29,w[3],-10);}
        }
        for(int x=176;x<1440;x+=48){g.setColor(new Color(171,196,220,200));g.fillRoundRect(x,94,54,10+random.nextInt(12),8,8);}
        for(int y=176;y<900;y+=192){window(g,177,y,35,90);window(g,1388,y,35,90);}
        for(int x:new int[]{320,1152})window(g,x,108,100,40);
        // Door jambs visibly frame the only opening, with no solid across the threshold.
        wood(g,684,916,20,78,-8);wood(g,896,916,20,78,-8);
        garland(g,694,930,204);
        g.setColor(new Color(45,28,26));g.setStroke(new BasicStroke(16));
        g.draw(new CubicCurve2D.Double(700,945,724,890,876,890,900,945));
        g.setColor(new Color(149,103,58));g.setStroke(new BasicStroke(2));
        g.draw(new CubicCurve2D.Double(700,941,724,886,876,886,900,941));g.setStroke(new BasicStroke(1));
        garland(g,730,910,140);LightBloom.draw(g,816,970,130,.20,LightBloom.GOLD);
    }
    private void window(Graphics2D g,int x,int y,int w,int h) {
        g.setColor(new Color(28,23,30));g.fillRoundRect(x-4,y-4,w+8,h+8,8,8);
        g.setPaint(new GradientPaint(x,y,new Color(255,194,110),x+w,y+h,new Color(112,68,49)));g.fillRect(x,y,w,h);
        g.setColor(new Color(68,41,27));g.fillRect(x+w/2-2,y,4,h);g.fillRect(x,y+h/2-2,w,4);
        g.setColor(new Color(255,237,192,65));g.drawLine(x+3,y+3,x+w/2-4,y+3);g.drawLine(x+3,y+3,x+3,y+h/2-5);
        g.setColor(new Color(33,25,29,130));g.drawRect(x-1,y-1,w+2,h+2);
        LightBloom.draw(g,x+w/2.,y+h/2.,80,.19,LightBloom.GOLD);
    }
    private void garland(Graphics2D g,int x,int y,int w) {
        for(int i=0;i<w;i+=6) {
            int dy=(int)(7*Math.sin(i*.065));g.setColor(new Color(26,57+random.nextInt(18),40));g.fillOval(x+i,y+dy,12,8);
            if(i%30==0){g.setColor(new Color(156,41,46));g.fillOval(x+i+3,y+dy+1,5,5);}
            if(i%48==0){g.setColor(new Color(255,204,116));g.fillOval(x+i,y+dy,3,3);LightBloom.draw(g,x+i,y+dy,12,.13,LightBloom.GOLD);}
        }
    }
    private void carpet(Graphics2D g,int x,int y,int w,int h) {
        shadow(g,x-5,y+5,w+10,h,.13f);g.setColor(new Color(100,35,42));g.fillRect(x,y,w,h);
        g.setColor(new Color(140,102,58));g.setStroke(new BasicStroke(3));g.drawRect(x+7,y+7,w-14,h-14);
        g.setColor(new Color(23,64,52));g.fillRect(x+16,y+16,w-32,h-32);
        g.setColor(new Color(107,56,49));g.fillRect(x+24,y+24,w-48,h-48);
        for(int i=0;i<300;i++){g.setColor(new Color(230,173,113,random.nextInt(20)+5));g.fillRect(x+random.nextInt(w),y+random.nextInt(h),2,1);}
        star(g,x+w/2,y+h/2,17,7,new Color(195,142,79));
        for(int py=y+28;py<y+h-20;py+=24)for(int px:new int[]{x+11,x+w-12}){g.setColor(new Color(220,177,104));g.fillOval(px-2,py-2,4,4);}
        g.setStroke(new BasicStroke(1));
        if(materials[3]!=null)g.drawImage(materials[3],x,y,w,h,null);
    }
    private void shelf(Graphics2D g,int x,int y,int w,int h) {
        shadow(g,x-9,y+10,w+18,h,.30f);wood(g,x,y,w,h,-12);
        for(int row=0;row<2;row++) {
            int sy=y+row*h/2;g.setColor(new Color(27,22,25));g.fillRect(x+10,sy+5,w-20,h/2-14);
            for(int px=x+12;px<x+w-28;px+=38) {
                if(present!=null)g.drawImage(present,px,sy+8,32,35,null);
                else gift(g,px,sy+15,28,24);
                if((px/38)%3==0){g.setColor(new Color(191,159,109));g.fillOval(px+8,sy+22,17,16);g.setColor(new Color(74,45,35));g.drawLine(px+16,sy+20,px+16,sy+30);}
            }
            wood(g,x,sy+h/2-8,w,8,7);
        }
        wood(g,x,y,9,h,1);wood(g,x+w-9,y,9,h,1);garland(g,x+8,y+4,w-16);
    }
    private void table(Graphics2D g,int x,int y,int w,int h) {
        shadow(g,x-10,y+12,w+20,h+9,.27f);g.setColor(new Color(27,20,22));g.fillRoundRect(x-3,y-3,w+6,h+12,8,8);
        for(int px=x;px<x+w;px+=32)wood(g,px,y,Math.min(31,x+w-px),h,2);
        wood(g,x,y+h-9,w,9,-8);g.setColor(new Color(180,121,65));g.drawRect(x+4,y+4,w-8,h-12);
        for(int px:new int[]{x+3,x+w-10}){g.setColor(new Color(38,25,24));g.fillRect(px,y+h,7,8);}
    }
    private void machine(Graphics2D g,int x,int y,int w,int h,boolean broken) {
        table(g,x,y,w,h);g.setColor(new Color(39,41,47));g.fillRoundRect(x+24,y+28,w-48,h-54,9,9);
        g.setColor(new Color(93,98,101));g.drawRoundRect(x+26,y+30,w-52,h-58,9,9);
        gear(g,x+67,y+70,38);gear(g,x+130,y+76,26);gear(g,x+w-42,y+42,18);
        g.setColor(new Color(121,79,47));g.fillRect(x+16,y+h-47,w-32,23);
        for(int px=x+20;px<x+w-26;px+=16){g.setColor(new Color(54,43,36));g.fillRect(px,y+h-42,11,12);}
        g.setColor(new Color(168,115,67));g.setStroke(new BasicStroke(4));
        g.drawLine(x+66,y+65,x+66,y+14);g.drawLine(x+66,y+14,x+146,y+14);g.drawLine(x+146,y+14,x+146,y+64);
        g.setStroke(new BasicStroke(1));g.setColor(new Color(188,134,69));g.fillOval(x+140,y+52,14,14);
        for(int i=0;i<5;i++){int px=x+26+i*39;g.setColor(new Color(170,146,103));g.fillRect(px,y+h-22,12,6);g.setColor(new Color(85,45,33));g.fillOval(px,y+h-17,5,5);}
        if(broken){g.setColor(new Color(15,19,26));g.setStroke(new BasicStroke(3));g.drawLine(x+129,y+52,x+140,y+72);g.drawLine(x+140,y+72,x+125,y+87);g.setStroke(new BasicStroke(1));}
        else {g.setColor(new Color(161,136,88));g.drawLine(x+170,y+32,x+190,y+50);g.fillRect(x+185,y+48,9,4);}
    }
    private void gear(Graphics2D g,int x,int y,int r) {
        g.setColor(new Color(24,23,27));g.fillOval(x-r-2,y-r+3,2*r+4,2*r+4);
        for(int i=0;i<12;i++){double a=i*Math.PI/6;Graphics2D tooth=(Graphics2D)g.create();tooth.translate(x,y);tooth.rotate(a);tooth.setColor(new Color(141,103,60));tooth.fillRect(r-7,-5,13,10);tooth.dispose();}
        g.setColor(new Color(128,94,56));g.fillOval(x-r,y-r,2*r,2*r);g.setColor(new Color(217,160,85));g.drawOval(x-r+2,y-r+2,2*r-4,2*r-4);
        g.setColor(new Color(43,38,34));g.fillOval(x-r+8,y-r+8,2*r-16,2*r-16);
        g.setColor(new Color(159,121,75));g.setStroke(new BasicStroke(4));for(int i=0;i<6;i++){double a=i*Math.PI/3;g.drawLine(x,y,(int)(x+Math.cos(a)*(r-9)),(int)(y+Math.sin(a)*(r-9)));}g.setStroke(new BasicStroke(1));g.fillOval(x-7,y-7,14,14);g.setColor(new Color(44,38,31));g.fillOval(x-3,y-3,6,6);
    }
    private void letterDesk(Graphics2D g) {
        table(g,288,672,224,128);
        for(int i=0;i<4;i++){g.setColor(new Color(128-i*9,43+i*6,43));g.fillRect(304+i*3,686+i*5,38,26);g.setColor(new Color(209,174,112));g.drawRect(308+i*3,689+i*5,30,19);}
        paper(g,357,687,52,62,0);paper(g,404,698,44,51,1);paper(g,384,756,37,24,2);
        g.setColor(new Color(15,25,35));g.fillOval(454,738,13,13);g.setColor(new Color(197,185,161));g.drawLine(461,741,472,715);
        lantern(g,474,701);wood(g,365,804,61,26,-4);g.setColor(new Color(90,39,39));g.fillRect(372,810,46,17);
        plaque(g,"LETTERS",400,666);
    }
    private void journeyMap(Graphics2D g) {
        table(g,1056,704,224,128);g.setColor(new Color(197,163,109));g.fillRoundRect(1072,718,159,94,6,6);
        g.setColor(new Color(126,149,136,180));g.fillPolygon(new int[]{1088,1110,1128,1135,1164,1183,1207,1187,1170,1136,1107},new int[]{733,728,741,761,748,753,770,794,788,801,778},11);
        g.setColor(new Color(104,70,49));g.setStroke(new BasicStroke(2));g.draw(new CubicCurve2D.Double(1095,750,1130,798,1160,728,1203,779));g.setStroke(new BasicStroke(1));
        for(int[] p:new int[][]{{1095,750},{1148,771},{1203,779}}){g.setColor(new Color(136,44,39));g.fillOval(p[0]-3,p[1]-3,6,6);g.drawLine(p[0],p[1],p[0],p[1]-9);}
        g.setColor(new Color(180,144,90,90));for(int i=0;i<180;i++)g.fillRect(1077+random.nextInt(148),721+random.nextInt(85),2,1);
        star(g,1188,738,13,4,new Color(74,67,53));g.setColor(new Color(232,208,161));g.fillOval(1185,735,6,6);
        lantern(g,1250,743);paper(g,1230,779,31,29,0);plaque(g,"THE JOURNEY",1168,698);
    }
    private void paper(Graphics2D g,int x,int y,int w,int h,int kind) {
        g.setColor(new Color(37,25,24,100));g.fillRect(x+2,y+3,w,h);g.setColor(new Color(215,191,142));g.fillRect(x,y,w,h);
        g.setColor(new Color(113,93,66,140));for(int py=y+7;py<y+h-5;py+=6)g.drawLine(x+5,py,x+w-6-random.nextInt(9),py);
        if(kind==2){g.setColor(new Color(126,41,37));g.fillOval(x+w/2-4,y+h/2-4,8,8);}
    }
    private void christmasTree(Graphics2D g) {
        shadow(g,675,358,282,151,.35f);g.setColor(new Color(61,36,26));g.fillRect(795,386,30,84);
        for(int layer=0;layer<8;layer++) {
            int cy=218+layer*30, radius=29+layer*12;
            g.setColor(new Color(13,33,29));g.fillOval(816-radius,cy-8,2*radius,57);
            for(int j=0;j<75;j++) {
                double a=random.nextDouble()*Math.PI*2;double distance=Math.sqrt(random.nextDouble());
                int x=(int)(816+Math.cos(a)*radius*distance),y=(int)(cy+Math.sin(a)*24*distance);
                int tone=random.nextInt(32);g.setColor(new Color(23+tone/3,53+tone,42+tone/2));
                g.setStroke(new BasicStroke(1));g.drawLine(x,y,x+random.nextInt(13)-6,y+random.nextInt(8)+2);
            }
            g.setColor(new Color(204,156,81,110));g.drawArc(816-radius+7,cy-15,2*radius-14,35,190,155);
            for(int j=0;j<layer+3;j++) {
                double a=(j+1)*Math.PI/(layer+4);int x=(int)(816+Math.cos(a)*(radius-8)),y=(int)(cy+Math.sin(a)*17);
                Color ornament=j%3==0?new Color(180,46,41):j%3==1?new Color(219,168,66):new Color(177,191,160);
                g.setColor(new Color(9,22,23,130));g.fillOval(x-5,y-3,11,12);g.setColor(ornament);g.fillOval(x-4,y-4,8,9);g.setColor(new Color(255,234,174,150));g.fillOval(x-2,y-3,2,3);
            }
            for(int j=0;j<layer+4;j++){int x=816-radius+9+j*(radius*2-18)/(layer+3),y=cy+17+(int)(5*Math.sin(j));LightBloom.draw(g,x,y,17,.25,LightBloom.GOLD);g.setColor(new Color(255,222,138));g.fillOval(x-2,y-2,4,4);}
        }
        LightBloom.draw(g,816,203,48,.28,LightBloom.GOLD);star(g,816,201,22,9,new Color(255,209,107));star(g,816,201,13,5,new Color(255,241,194));
        for(int[] p:new int[][]{{719,429},{748,448},{837,448},{875,425}}){if(present!=null)g.drawImage(present,p[0],p[1],47,44,null);else gift(g,p[0],p[1],40,35);}
    }
    private void gift(Graphics2D g,int x,int y,int w,int h){g.setColor(new Color(119,37,38));g.fillRect(x,y,w,h);g.setColor(new Color(211,165,85));g.fillRect(x+w/2-2,y,4,h);g.fillRect(x,y+h/2-2,w,4);}
    private void spiritLantern(Graphics2D g,int x,int y) {
        shadow(g,x-35,y-12,70,38,.25f);g.setColor(new Color(56,43,36));g.fillOval(x-25,y-12,50,22);
        g.setColor(new Color(177,124,60));g.fillRect(x-7,y-63,14,55);g.fillOval(x-20,y-20,40,13);
        g.setPaint(new GradientPaint(x-16,y-84,new Color(255,222,153),x+20,y-43,new Color(195,126,53)));g.fillRoundRect(x-17,y-86,34,43,11,11);
        g.setColor(new Color(90,61,36));g.setStroke(new BasicStroke(3));g.drawRoundRect(x-17,y-86,34,43,11,11);g.drawLine(x,y-84,x,y-45);g.drawLine(x-15,y-66,x+15,y-66);g.drawArc(x-9,y-106,18,21,0,180);g.setStroke(new BasicStroke(1));
        star(g,x,y-63,9,4,new Color(255,235,176));plaque(g,"SPIRIT LANTERN",x,y+23);
    }
    private void lantern(Graphics2D g,int x,int y) {
        g.setColor(new Color(38,29,28));g.fillOval(x-10,y-6,20,9);g.fillRect(x-3,y-34,6,30);
        g.setColor(new Color(170,120,59));g.fillRoundRect(x-8,y-55,16,23,4,4);g.setColor(new Color(255,212,129));g.fillRect(x-5,y-52,10,17);g.setColor(new Color(65,44,32));g.drawLine(x,y-54,x,y-34);g.fillRect(x-9,y-57,18,3);g.fillRect(x-9,y-34,18,3);
        LightBloom.draw(g,x,y-45,55,.23,LightBloom.GOLD);
    }
    private void exterior(Graphics2D g) {
        Path2D path=new Path2D.Double();path.moveTo(711,991);path.curveTo(720,1070,680,1130,674,1280);path.lineTo(930,1280);path.curveTo(920,1130,892,1060,891,991);path.closePath();
        g.setPaint(new GradientPaint(800,990,new Color(200,216,229,120),800,1280,new Color(187,208,226,90)));g.fill(path);
        Shape clip=g.getClip();g.clip(path);
        for(int i=0;i<950;i++){int x=674+random.nextInt(256),y=991+random.nextInt(289);g.setColor(new Color(228,236,240,random.nextInt(24)+7));g.fillRect(x,y,1+random.nextInt(3),1);}
        g.setClip(clip);
        for(int y=1000;y<1250;y+=27){g.setColor(new Color(65,89,123,140));g.fillOval(785,y,6,10);g.fillOval(806,y+13,6,10);}
        for(int[] p:new int[][]{{40,30},{116,15},{1460,20},{1490,480},{24,540},{80,920},{190,1010},{1240,995},{1430,980}}) {
            shadow(g,p[0]-10,p[1]+142,150,36,.19f);if(pine!=null)g.drawImage(pine,p[0],p[1],150,190,null);
        }
        for(int[] p:new int[][]{{220,1140},{1290,1140},{100,775},{1490,780}})if(rock!=null)g.drawImage(rock,p[0],p[1],85,58,null);
        if(bench!=null){g.drawImage(bench,330,1020,145,95,null);g.drawImage(bench,1130,1020,145,95,null);}
        for(int x:new int[]{512,1010}) {
            wood(g,x,1140,140,12,-9);wood(g,x,1163,140,9,-9);
            for(int px=x;px<x+150;px+=35){wood(g,px,1120,10,74,-8);g.setColor(new Color(214,226,234));g.fillOval(px-3,1115,16,9);}
            garland(g,x,1140,140);
        }
        for(int[] p:new int[][]{{690,1050},{920,1050},{690,1176},{920,1176}})lantern(g,p[0],p[1]);
        departureGate(g);
    }
    /** Static local architecture only: the original road and transition remain open. */
    private void departureGate(Graphics2D original) {
        Graphics2D g=(Graphics2D)original.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
            shadow(g,678,1133,272,25,.22f);
            for(int x:new int[]{690,918}) {
                wood(g,x,1033,24,113,-10);
                g.setColor(new Color(47,32,29));g.fillRect(x-6,1132,36,14);
                g.setColor(new Color(190,145,77));g.drawRect(x+3,1040,17,87);
                g.setColor(new Color(235,238,226));g.fillRect(x-5,1028,34,7);g.fillRect(x-1,1024,26,5);
                g.setColor(new Color(155,42,47));g.fillRect(x+7,1062,10,32);
                g.setColor(new Color(231,181,87));g.fillRect(x+6,1070,12,3);
                g.setColor(new Color(184,45,48));g.fillPolygon(new int[]{x+12,x-1,x-1},new int[]{1065,1057,1072},3);g.fillPolygon(new int[]{x+12,x+26,x+26},new int[]{1065,1057,1072},3);
                lantern(g,x+12,1122);
            }
            // Stepped timber arch, layered gold moulding and a thick irregular snow cap.
            int[] ax={688,712,746,782,816,850,886,920,944};
            int[] ay={1046,1020,1004,994,991,994,1004,1020,1046};
            g.setStroke(new BasicStroke(25,BasicStroke.CAP_SQUARE,BasicStroke.JOIN_MITER));g.setColor(new Color(46,29,28));g.drawPolyline(ax,ay,9);
            g.setStroke(new BasicStroke(18));g.setColor(new Color(105,65,37));g.drawPolyline(ax,ay,9);
            g.setStroke(new BasicStroke(2));g.setColor(new Color(206,157,84));g.drawPolyline(ax,ay,9);
            for(int i=0;i<8;i++){
                g.setColor(new Color(182,203,215));g.fillRect(ax[i]-6,ay[i]-15,ax[i+1]-ax[i]+10,6);
                g.setColor(new Color(235,240,233));g.fillRect(ax[i]-4,ay[i]-18,ax[i+1]-ax[i]+7,5);
            }
            for(int i=0;i<=28;i++){
                double u=i/28.0;int x=700+i*8,y=1042-(int)(42*Math.sin(u*Math.PI));
                g.setColor(new Color(19,52,38));g.fillRect(x-8,y-3,17,9);
                g.setColor(new Color(40,83,49));g.fillRect(x-4,y-6,10,5);
                g.setColor(new Color(63,111,62));g.fillRect(x-3,y-4,5,2);
                if(i%3==0){g.setColor(new Color(193,49,49));g.fillRect(x,y+1,4,4);g.setColor(new Color(255,216,138));g.fillRect(x+1,y+1,1,1);}
            }
            // Wide readable sign hangs above the approach, never across Santa's walking feet.
            g.setStroke(new BasicStroke(2));g.setColor(new Color(210,161,79));g.drawLine(746,1002,746,1010);g.drawLine(886,1002,886,1010);
            g.setColor(new Color(39,25,27));g.fillRect(725,1007,182,30);
            g.setColor(new Color(129,78,43));g.fillRect(728,1010,176,24);
            g.setColor(new Color(224,176,90));g.drawRect(730,1012,172,20);
            g.setColor(new Color(70,39,31));g.fillRect(734,1015,164,14);
            g.setFont(new Font("Dialog",Font.BOLD,12));g.setColor(new Color(255,231,166));String text="THE LAST VILLAGE";g.drawString(text,816-g.getFontMetrics().stringWidth(text)/2,1027);
            star(g,816,990,11,5,new Color(231,180,82));star(g,816,990,5,2,new Color(255,237,178));
        } finally {g.dispose();}
    }

    private void details(Graphics2D g) {
        // Dust, forgotten ribbon, scattered wooden offcuts and unfinished toys, all non-colliding.
        for(int[] p:new int[][]{{526,373},{539,432},{1030,484},{1303,549},{541,708},{1276,843}}) {
            wood(g,p[0],p[1],18,7,4);g.setColor(new Color(166,49,43));g.drawArc(p[0]+11,p[1]-4,24,14,20,220);
            g.setColor(new Color(193,164,103));g.fillOval(p[0]+24,p[1]+8,7,7);
        }
        // Quiet directional warmth is baked into terrain; actors and HUD are drawn afterward.
        Shape prior=g.getClip();g.clipRect(224,160,1152,768);
        g.setPaint(new RadialGradientPaint(815,505,735,new float[]{0,.45f,1},new Color[]{new Color(9,7,15,0),new Color(10,8,19,20),new Color(8,10,22,132)}));g.fillRect(224,160,1152,768);g.setClip(prior);
    }
    private void plaque(Graphics2D g,String text,int x,int y) {
        g.setFont(new Font("Dialog",Font.BOLD,10));int w=g.getFontMetrics().stringWidth(text)+18;
        g.setColor(new Color(28,22,25,210));g.fillRoundRect(x-w/2,y-11,w,17,5,5);g.setColor(new Color(166,123,72));g.drawRoundRect(x-w/2,y-11,w,17,5,5);g.setColor(new Color(232,198,141));g.drawString(text,x-g.getFontMetrics().stringWidth(text)/2,y+1);
    }
    private static void star(Graphics2D g,int x,int y,int outer,int inner,Color color) {
        Path2D p=new Path2D.Double();for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5,r=i%2==0?outer:inner;double px=x+Math.cos(a)*r,py=y+Math.sin(a)*r;if(i==0)p.moveTo(px,py);else p.lineTo(px,py);}p.closePath();g.setColor(color);g.fill(p);
    }
    private static void shadow(Graphics2D g,int x,int y,int w,int h,float strength) {
        Composite prior=g.getComposite();g.setComposite(AlphaComposite.SrcOver.derive(strength));
        g.setPaint(new RadialGradientPaint(x+w/2f,y+h/2f,Math.max(w,h)/2f,new float[]{0,.65f,1},new Color[]{new Color(5,12,22,200),new Color(5,12,22,70),new Color(5,12,22,0)}));
        g.fillOval(x,y,w,h);g.setComposite(prior);
    }
}
