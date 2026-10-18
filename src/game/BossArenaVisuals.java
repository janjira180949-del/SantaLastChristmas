package game;

import boss.ChristmasEater;
import boss.BossArtwork;
import java.awt.*;
import java.awt.image.BufferedImage;

/** Bounded presentation below actors; never adds arena collision. */
public final class BossArenaVisuals {
    private BossArenaVisuals() { }
    private static final java.util.Map<Object,ArenaState> SCENES=new java.util.WeakHashMap<>();
    private static BufferedImage ritual,crystal;
    private static final class ArenaState {
        entity.Enemy lastBoss;int lastTick=-1,phase,phaseTick,defeatTick=-1;
    }
    public static Rectangle arenaBounds(){return new Rectangle(976,808,640,400);}
    /** Called by the existing skill preparation at session construction, never by paint. */
    public static synchronized void prepare(){
        if(ritual!=null)return;
        ritual=new BufferedImage(640,400,BufferedImage.TYPE_INT_ARGB);Graphics2D g=ritual.createGraphics();
        for(int i=0;i<48;i++){
            double a=i*Math.PI/24,b=a+.08;int x=320+(int)(Math.cos(a)*216),y=200+(int)(Math.sin(a)*118);
            int nx=320+(int)(Math.cos(b)*216),ny=200+(int)(Math.sin(b)*118);
            g.setColor(new Color(11,16,36,180));g.setStroke(new BasicStroke(5));g.drawLine(x,y,nx,ny);
            g.setColor(i%4==0?new Color(139,118,210,180):new Color(78,165,192,155));g.setStroke(new BasicStroke(1));g.drawLine(x,y,nx,ny);
            if(i%4==0){g.drawLine(x-3,y-5,x+3,y+5);g.drawLine(x-4,y+3,x+4,y-3);}
        }
        for(int i=0;i<12;i++){
            double a=i*Math.PI/6;int x=320+(int)(Math.cos(a)*195),y=200+(int)(Math.sin(a)*105);
            int mx=320+(int)(Math.cos(a+.1)*120),my=200+(int)(Math.sin(a+.1)*62);
            g.setColor(new Color(15,24,45,170));g.setStroke(new BasicStroke(4));g.drawLine(320,200,mx,my);g.drawLine(mx,my,x,y);
            g.setColor(new Color(99,202,225,135));g.setStroke(new BasicStroke(1));g.drawLine(320,200,mx,my);g.drawLine(mx,my,x,y);
            g.setColor(new Color(149,128,220,145));g.drawLine(mx,my,mx+12,my-9);
        }
        for(int i=0;i<6;i++){
            double a=i*Math.PI/3,b=a+Math.PI*2/3;
            g.setColor(new Color(73,176,210,110));g.drawLine(320+(int)(Math.cos(a)*76),200+(int)(Math.sin(a)*42),320+(int)(Math.cos(b)*76),200+(int)(Math.sin(b)*42));
        }
        g.dispose();
        BufferedImage atlas=util.ImageAssets.load("assets/world/forest_landmarks/crystals.png");
        if(atlas!=null){
            BufferedImage cell=atlas.getSubimage(atlas.getWidth()*2/3,0,atlas.getWidth()/3,atlas.getHeight());
            int left=cell.getWidth(),top=cell.getHeight(),right=0,bottom=0;
            for(int y=0;y<cell.getHeight();y++)for(int x=0;x<cell.getWidth();x++)if((cell.getRGB(x,y)>>>24)>24){left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x);bottom=Math.max(bottom,y);}
            if(right>left&&bottom>top){crystal=new BufferedImage(86,130,BufferedImage.TYPE_INT_ARGB);g=crystal.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                double scale=Math.min(82.0/(right-left+1),126.0/(bottom-top+1));int w=(int)((right-left+1)*scale),h=(int)((bottom-top+1)*scale);
                g.drawImage(cell,(86-w)/2,128-h,(86+w)/2,128,left,top,right+1,bottom+1,null);g.dispose();
                for(int y=0;y<crystal.getHeight();y++)for(int x=0;x<crystal.getWidth();x++){
                    int c=crystal.getRGB(x,y),red=(c>>>16)&255,green=(c>>>8)&255,blue=c&255;
                    if(blue>red&&green>red)crystal.setRGB(x,y,(c&0xff000000)|(blue<<16)|((green/2)<<8)|Math.min(255,red+55));
                }
            }
        }
    }
    /** Only the one pre-existing arena call gains context; all effects remain beneath actors. */
    public static void draw(Graphics2D original,entity.Enemy boss,int ticks,boolean town,boolean restored,Object session){
        ArenaState scene=SCENES.computeIfAbsent(session,key->new ArenaState());
        if(!town){scene.lastBoss=null;scene.defeatTick=-1;scene.lastTick=-1;draw(original,boss,ticks);return;}
        if(ticks<scene.lastTick){scene.lastBoss=null;scene.defeatTick=-1;scene.phase=0;}
        scene.lastTick=ticks;
        if(boss instanceof boss.DarkTitan titan){
            if(scene.lastBoss!=boss){scene.lastBoss=boss;scene.defeatTick=-1;scene.phase=titan.getPhaseIndex();scene.phaseTick=ticks;}
            if(scene.phase!=titan.getPhaseIndex()){scene.phase=titan.getPhaseIndex();scene.phaseTick=ticks;}
        }
        if(scene.lastBoss!=null&&scene.lastBoss.isDefeated()&&scene.defeatTick<0)scene.defeatTick=ticks;
        if(ritual!=null){
            Graphics2D g=(Graphics2D)original.create();Rectangle r=arenaBounds();g.clip(r);
            try{
                double fade=restored?0:scene.defeatTick<0?1:Math.max(0,1-(ticks-scene.defeatTick)/(double)BossFallPresentation.TITAN_DURATION);
                boolean fighting=boss instanceof boss.DarkTitan&&!boss.isDefeated();
                float alpha=(float)(fade*(fighting?.66:.40));
                g.setComposite(AlphaComposite.SrcOver.derive(alpha));g.drawImage(ritual,r.x,r.y,null);
                if(crystal!=null){
                    g.setComposite(AlphaComposite.SrcOver.derive((float)(fade*(fighting?.90:.78))));
                    for(int i=0;i<4;i++){int x=i%2==0?r.x+14:r.x+r.width-100,y=i<2?r.y+25:r.y+225;g.drawImage(crystal,x,y,null);}
                }
                double pulse=.72+.18*Math.sin(ticks/55.0);
                g.setComposite(AlphaComposite.SrcOver.derive((float)(alpha*pulse)));
                g.setColor(new Color(237,103,164));
                for(int i=0;i<12;i++){
                    double a=i*Math.PI/6+ticks/1700.0;int x=1296+(int)(Math.cos(a)*180),y=1008+(int)(Math.sin(a)*92)-((ticks+i*17)%70)/5;
                    g.fillPolygon(new int[]{x,x+3,x,x-2},new int[]{y-4,y,y+4,y},4);
                }
                if(fighting&&ticks-scene.phaseTick<45){g.setComposite(AlphaComposite.SrcOver.derive((float)(fade*.10*(1-(ticks-scene.phaseTick)/45.0))));g.drawImage(ritual,r.x,r.y,null);}
                // Two small, pixel-stepped mist ribbons: no full arena fog or filled bloom.
                g.setColor(new Color(141,195,213,40));
                for(int i=0;i<2;i++){int x=r.x+120+(ticks/5+i*150)%230,y=r.y+110+i*154;for(int j=0;j<6;j++)g.fillRect(x+j*9,y+(j%2)*2,12,2);}
            }finally{g.dispose();
                for(int y=0;y<crystal.getHeight();y++)for(int x=0;x<crystal.getWidth();x++){
                    int c=crystal.getRGB(x,y),red=(c>>>16)&255,green=(c>>>8)&255,blue=c&255;
                    if(blue>red&&green>red)crystal.setRGB(x,y,(c&0xff000000)|(blue<<16)|((green/2)<<8)|Math.min(255,red+55));
                }
            }
        }
        draw(original,boss,ticks);
    }
    public static void draw(Graphics2D g,entity.Enemy boss,int ticks){
        if(!(boss instanceof boss.BossCombatant)||boss.isDefeated())return;
        if(boss instanceof boss.FrostFang)BossArtwork.drawFangContact(g,boss.getX()+38,boss.getY()+76);
        // Contact artwork is local and does not replace or recolour arena terrain.
        BossArtwork.effect(g,"footstep",11,boss.getX()+boss.getSize()/2.0,boss.getY()+boss.getSize(),boss instanceof boss.DarkTitan?180:220,48,0);
    }
    public static void draw(Graphics2D original,ChristmasEater boss,int ticks) {
        if(boss==null||boss.isDefeated())return;
        Graphics2D g=(Graphics2D)original.create();
        try {
            int phase=boss.getPhase().ordinal(),x=1296,y=1296;
            g.setColor(new Color(16,12,37,15+phase*12));g.fillOval(x-360,y-230,720,460);
            g.setStroke(new BasicStroke(phase==2?3:2));
            g.setColor(new Color(113+phase*20,107,171,30+phase*16));
            for(int i=0;i<16;i++) {
                double a=i*Math.PI/8,r=150+(i%3)*42;
                int px=x+(int)(Math.cos(a)*r),py=y+(int)(Math.sin(a)*r*.7);
                g.drawLine(px,py,px+(int)(Math.cos(a+.5)*55),py+(int)(Math.sin(a+.5)*55));
                g.drawLine(px,py,px+(int)(Math.cos(a-.7)*35),py+(int)(Math.sin(a-.7)*35));
            }
            if(phase==2)util.LightBloom.draw(g,x,y,280,.05+.015*Math.sin(ticks/35.0),util.LightBloom.RED);
        } finally {g.dispose();}
    }
}
