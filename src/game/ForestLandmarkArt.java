package game;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import util.ImageAssets;

/** Small immutable presentation assets, prepared inside the existing Forest worker. */
public final class ForestLandmarkArt {
    private record Art(BufferedImage[] tents,BufferedImage[][] cores,BufferedImage[] energy,
            BufferedImage[] rubble,BufferedImage[] ground,BufferedImage[] scars,
            BufferedImage shrine,BufferedImage spent,BufferedImage lamp,BufferedImage[] fragments) { }
    private static volatile Art prepared;
    private static volatile double preparationMs;
    private static final int[][] CORE_SIZE={{96,136},{112,132},{118,156}};
    private static final AlphaComposite[] ALPHAS=new AlphaComposite[256];
    private static final Color CYAN=new Color(166,227,241,145),MINT=new Color(157,232,197,120);
    private static final Font LABEL=new Font("Dialog",Font.BOLD,12);
    private static final BufferedImage SHADOW=soft(112,38,new Color(7,17,30,120));
    private static final BufferedImage MINT_LIGHT=soft(72,54,new Color(129,224,177,55));
    private static final BufferedImage FROST_MIST=soft(88,18,new Color(116,187,219,26));
    private static final BufferedImage RUNE=rune();
    static {for(int i=0;i<256;i++)ALPHAS[i]=AlphaComposite.SrcOver.derive(i/255f);}
    private ForestLandmarkArt(){ }
    public static boolean isPrepared(){return prepared!=null;}
    public static double preparationMillis(){return preparationMs;}
    /** Never called by painting: the one existing background preparation job owns decoding. */
    static synchronized void prepareNow(){
        if(prepared!=null)return;
        long start=System.nanoTime();
        var tentAtlas=ImageAssets.load("assets/world/forest_landmarks/tents.png");
        var coreAtlas=ImageAssets.load("assets/world/forest_landmarks/crystals.png");
        var shrineSource=ImageAssets.load("assets/world/forest_landmarks/healing_shrine.png");
        var lampSource=ImageAssets.load("assets/world/village_props/warm_lamp.png");
        BufferedImage[] tents=new BufferedImage[2],energy=new BufferedImage[3],rubble=new BufferedImage[3],ground=new BufferedImage[3],scars=new BufferedImage[3];
        BufferedImage[][] cores=new BufferedImage[3][2];BufferedImage[] fragments=new BufferedImage[3];
        for(int i=0;i<2;i++)tents[i]=cell(tentAtlas,i,2,150,i==0?124:65);
        for(int i=0;i<3;i++){
            cores[i][0]=cell(coreAtlas,i,3,CORE_SIZE[i][0],CORE_SIZE[i][1]);
            cores[i][1]=variant(cores[i][0],true,false);energy[i]=energy(cores[i][0]);
            rubble[i]=rubble(cores[i][0]);ground[i]=fractures(i);scars[i]=variant(ground[i],false,true);
            if(cores[i][0]!=null){var source=cores[i][0];fragments[i]=fit(source.getSubimage(source.getWidth()/3,source.getHeight()/4,source.getWidth()/3,source.getHeight()/3),14,22);}
        }
        var shrine=fit(shrineSource,72,96);var lamp=woodenLamp(lampSource);
        prepared=new Art(tents,cores,energy,rubble,ground,scars,shrine,variant(shrine,false,true),lamp,fragments);
        preparationMs=(System.nanoTime()-start)/1e6;
    }
    private static BufferedImage cell(BufferedImage atlas,int index,int columns,int width,int height){
        if(atlas==null||atlas.getWidth()<columns*16)return null;
        int left=atlas.getWidth()*index/columns,right=atlas.getWidth()*(index+1)/columns;
        return fit(atlas.getSubimage(left,0,right-left,atlas.getHeight()),width,height);
    }
    private static BufferedImage woodenLamp(BufferedImage source){
        if(source==null)return null;
        var image=new BufferedImage(30,62,2);var g=image.createGraphics();
        g.setColor(new Color(60,42,38));g.fillRoundRect(12,27,7,32,3,3);
        g.setColor(new Color(113,82,61));g.drawLine(13,30,13,57);g.drawLine(17,35,17,55);
        g.setColor(new Color(35,33,37));g.fillOval(8,55,16,5);
        g.setColor(new Color(139,169,201));g.fillOval(6,57,19,4);
        var head=fit(source.getSubimage(0,0,source.getWidth(),Math.min(84,source.getHeight())),28,36);
        g.drawImage(head,1,0,null);g.dispose();return image;
    }
    private static BufferedImage fit(BufferedImage source,int width,int height){
        if(source==null)return null;
        int left=source.getWidth(),top=source.getHeight(),right=-1,bottom=-1;
        for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++)if((source.getRGB(x,y)>>>24)>24){left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);}
        if(right<left)return null;
        var image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        double scale=Math.min((width-4.0)/(right-left+1),(height-4.0)/(bottom-top+1));
        int w=(int)Math.round((right-left+1)*scale),h=(int)Math.round((bottom-top+1)*scale);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(source,(width-w)/2,height-2-h,(width+w)/2,height-2,left,top,right+1,bottom+1,null);g.dispose();
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){
            int c=image.getRGB(x,y),a=c>>>24,r=(c>>>16)&255,green=(c>>>8)&255,b=c&255;
            image.setRGB(x,y,(a<<24)|((int)(r*.72)<<16)|((int)(green*.80)<<8)|(int)(b*.95));
        }return image;
    }
    private static boolean luminous(int r,int g,int b){return g>r+35&&b>r+45&&g>100;}
    private static BufferedImage variant(BufferedImage source,boolean exposed,boolean spent){
        if(source==null)return null;
        var image=new BufferedImage(source.getWidth(),source.getHeight(),2);
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
            int c=source.getRGB(x,y),a=c>>>24,r=(c>>>16)&255,g=(c>>>8)&255,b=c&255;
            if(exposed&&luminous(r,g,b)){r=Math.min(225,g+50);g=Math.min(250,b+45);b=255;}
            if(spent){int grey=(r+g+b)/3;r=(int)(grey*.60);g=(int)(grey*.70);b=(int)(grey*.83);a=(int)(a*.82);}
            image.setRGB(x,y,(a<<24)|(r<<16)|(g<<8)|b);
        }return image;
    }
    private static BufferedImage energy(BufferedImage source){
        if(source==null)return null;var image=new BufferedImage(source.getWidth(),source.getHeight(),2);
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
            int c=source.getRGB(x,y),r=(c>>>16)&255,g=(c>>>8)&255,b=c&255;
            if(luminous(r,g,b))image.setRGB(x,y,((c>>>24)<<24)|0x99def2);
        }return image;
    }
    private static BufferedImage rubble(BufferedImage source){
        if(source==null)return null;var image=new BufferedImage(source.getWidth(),24,2);var g=image.createGraphics();
        g.drawImage(source,0,0,source.getWidth(),24,0,source.getHeight()-24,source.getWidth(),source.getHeight(),null);g.dispose();return variant(image,false,true);
    }
    private static BufferedImage soft(int width,int height,Color color){
        var image=new BufferedImage(width,height,2);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){
            double dx=(x-width/2.0)/(width/2.0),dy=(y-height/2.0)/(height/2.0),edge=Math.max(0,1-dx*dx-dy*dy);
            int a=(int)(color.getAlpha()*edge*edge);image.setRGB(x,y,(a<<24)|(color.getRGB()&0xffffff));
        }return image;
    }
    private static BufferedImage fractures(int kind){
        var image=soft(280,174,new Color(20,21,43,48));var g=image.createGraphics();
        // Every vein starts at the embedded foot; only short secondary branches diverge.
        for(int i=0;i<7+kind;i++){
            double angle=i*Math.PI*2/(7+kind)+kind*.37;int reach=58+((i*31+kind*17)%57);
            int x=140+(int)(Math.cos(angle)*reach),y=87+(int)(Math.sin(angle)*reach*.52);
            var vein=new Path2D.Double();vein.moveTo(140,87);vein.lineTo(140+(x-140)*.35+((i%2)*7-3),87+(y-87)*.45);vein.lineTo(140+(x-140)*.70-((i%3)*4),87+(y-87)*.78);vein.lineTo(x,y);
            g.setStroke(new BasicStroke(3,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(new Color(15,27,45,160));g.draw(vein);
            g.setStroke(new BasicStroke(1));g.setColor(new Color(106,155,188,105));g.draw(vein);
            g.setColor(new Color(163,194,217,90));g.drawLine(x,y,x+(int)(Math.sin(angle)*8),y-5);
        }g.dispose();return image;
    }
    public static BufferedImage tent(int index){var art=prepared;return art==null?null:art.tents[index];}
    public static BufferedImage lamp(){var art=prepared;return art==null?null:art.lamp;}
    public static BufferedImage core(String id,boolean exposed){var art=prepared;return art==null?null:art.cores[index(id)][exposed?1:0];}
    public static BufferedImage shrine(boolean used){var art=prepared;return art==null?null:used?art.spent:art.shrine;}
    private static int index(String id){return id.endsWith("2")?1:id.endsWith("3")?2:0;}
    public static int labelY(String id,int y){return y+30-CORE_SIZE[index(id)][1]-14;}
    static void contact(Graphics2D g,int x,int y,int width){g.drawImage(SHADOW,x-width/2,y-11,width,25,null);}
    public static void drawFractures(Graphics2D g,BlackFrostCrystal crystal){
        var art=prepared;if(art==null)return;var p=crystal.getPosition();int i=index(crystal.getId());
        g.drawImage(crystal.getState()==BlackFrostCrystal.State.BROKEN?art.scars[i]:art.ground[i],p.x()-140,p.y()-67,null);
        if(crystal.getState()!=BlackFrostCrystal.State.BROKEN){g.setComposite(ALPHAS[(int)(90+20*Math.sin(crystal.getVisualTicks()/70.0))]);g.drawImage(RUNE,p.x()-100,p.y()-42,null);g.setComposite(AlphaComposite.SrcOver);}
    }
    public static boolean drawCore(Graphics2D g,BlackFrostCrystal crystal,int ticks,int effectFrames){
        var art=prepared;int i=index(crystal.getId());if(art==null||art.cores[i][0]==null)return false;
        var p=crystal.getPosition();int x=p.x(),y=p.y();
        if(crystal.getState()==BlackFrostCrystal.State.BROKEN){
            if(art.rubble[i]!=null)g.drawImage(art.rubble[i],x-art.rubble[i].getWidth()/2,y+6,null);
            if(effectFrames>0){
                g.setComposite(ALPHAS[Math.min(255,effectFrames*7)]);
                for(int n=0;n<8;n++){double a=n*Math.PI/4,r=8+(36-effectFrames)*2;int px=x+(int)(Math.cos(a)*r),py=y+7+(int)(Math.sin(a)*r*.65)-(int)(Math.sin((36-effectFrames)*Math.PI/36)*22);
                    if(art.fragments[i]!=null)g.drawImage(art.fragments[i],px-7,py-11,null);}
                g.setComposite(AlphaComposite.SrcOver);g.setColor(new Color(139,238,254,Math.min(180,effectFrames*5)));g.fillRect(x-2,y-18,4,effectFrames);
                g.drawImage(FROST_MIST,x-44,y+9,null);
            }
            return true;
        }
        contact(g,x,y+27,CORE_SIZE[i][0]+12);
        boolean exposed=crystal.getState()==BlackFrostCrystal.State.VULNERABLE;var sprite=art.cores[i][exposed?1:0];
        int shake=crystal.getImpactFrames()>0?(crystal.getImpactFrames()%3-1)*2:0;
        g.drawImage(sprite,x-sprite.getWidth()/2+shake,y+30-sprite.getHeight(),null);
        if(art.energy[i]!=null){double power=crystal.getImpactFrames()>0?.85:exposed?.35+.18*Math.sin(ticks/12.0):.14+.08*Math.sin(ticks/80.0+i);
            g.setComposite(ALPHAS[(int)(255*power)]);g.drawImage(art.energy[i],x-sprite.getWidth()/2+shake,y+30-sprite.getHeight(),null);g.setComposite(AlphaComposite.SrcOver);}
        g.drawImage(FROST_MIST,x-44+(int)(Math.sin(ticks/95.0)*9),y+18,null);
        if(art.fragments[i]!=null)for(int n=0;n<3;n++){int px=x+(n-1)*35,py=y-35-n*12+(int)(Math.sin(ticks/60.0+n)*5);g.setComposite(ALPHAS[exposed?150:100]);g.drawImage(art.fragments[i],px-7,py-11,null);}g.setComposite(AlphaComposite.SrcOver);
        g.setColor(exposed?new Color(248,219,165,115):CYAN);
        for(int n=0;n<4;n++){double t=(ticks/180.0+n*.25)%1;g.fillRect(x+(int)(Math.sin(n*2.1)*18),y+8-(int)(t*45),1,2);}
        return true;
    }
    private static BufferedImage rune(){
        var image=new BufferedImage(200,124,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        for(int i=0;i<24;i++){double a=i*Math.PI/12,b=a+.12;int x=100+(int)(Math.cos(a)*78),y=62+(int)(Math.sin(a)*42);
            g.setColor(new Color(115,204,230,105));g.drawLine(x,y,100+(int)(Math.cos(b)*78),62+(int)(Math.sin(b)*42));
            if(i%4==0){g.drawLine(x,y-4,x,y+4);g.setColor(new Color(153,115,214,100));g.drawLine(x-3,y,x+3,y);}
        }g.dispose();return image;
    }
    public static boolean drawShrine(Graphics2D g,HealingPoint point,int ticks){
        var sprite=shrine(point.isUsed());if(sprite==null)return false;var p=point.getPosition();
        contact(g,p.x(),p.y()+9,72);g.drawImage(sprite,p.x()-36,p.y()-84,null);
        if(!point.isUsed()){
            g.setComposite(ALPHAS[(int)(255*(.70+.10*Math.sin(ticks*Math.PI/60)))]);g.drawImage(MINT_LIGHT,p.x()-36,p.y()-62,null);g.setComposite(AlphaComposite.SrcOver);
            g.setColor(MINT);for(int n=0;n<4;n++){double t=(ticks/120.0+n*.25)%1;g.fillRect(p.x()+(int)(Math.sin(n*2.4)*17),p.y()-20-(int)(t*47),1,2);}
        }return true;
    }
    public static void shrinePrompt(Graphics2D g,HealingPoint point,entity.Santa santa,boolean combat){
        String text=point.getInteractionText(santa,combat);if(text.isEmpty())return;var p=point.getPosition();
        g.setFont(LABEL);int w=g.getFontMetrics().stringWidth(text);
        g.setColor(new Color(8,16,29,235));g.fillRoundRect(p.x()-w/2-10,p.y()-119,w+20,24,9,9);
        g.setColor(point.isUsed()?new Color(179,187,196):new Color(216,241,222));g.drawString(text,p.x()-w/2,p.y()-102);
    }
}
