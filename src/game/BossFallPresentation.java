package game;

import boss.ChristmasEater;
import java.awt.*;
import java.awt.image.BufferedImage;
import ui.UITheme;

/** Once-only, skippable fall before the original player-triggered restoration clock. */
public final class BossFallPresentation {
    public static final int FANG_DURATION=240, TITAN_DURATION=300;
    /** Maximum fall duration, retained for callers that wait for either boss. */
    public static final int DURATION=TITAN_DURATION;
    public int getDuration(){return forest?FANG_DURATION:TITAN_DURATION;}
    public int getFadeStart(){return forest?200:225;}
    public int getResolveFrame(){return getImpactFrame()+48;}
    /** Source-pose clock: anticipation holds, fast collapse, then a grounded settle. */
    public int getPoseTick(){
        int[] times=forest?FANG_POSE_TIMES:TITAN_POSE_TIMES;
        int[] poses=forest?FANG_POSE_CLOCK:TITAN_POSE_CLOCK;
        for(int i=1;i<times.length;i++)if(frames<times[i]){
            double t=(frames-times[i-1])/(double)(times[i]-times[i-1]);
            if(i==(forest?3:4))t=t*t; // Accelerate the fall, not every source pose.
            return (int)Math.floor(mix(poses[i-1],poses[i],t));
        }return poses[poses.length-1];
    }
    private static final int[] FANG_POSE_TIMES={0,18,56,104,128,145,165,184,200};
    private static final int[] FANG_POSE_CLOCK={0,11,21,29,40,51,62,73,87};
    private static final int[] TITAN_POSE_TIMES={0,34,94,100,150,174,180,186,225};
    private static final int[] TITAN_POSE_CLOCK={0,8,25,32,57,74,75,82,99};
    private boolean started;
    private int frames;
    private double x,y;
    private WorldMap.Point tree;
    private BufferedImage guardian;
    private entity.Enemy fallen;
    private boolean forest,cameraBound,facingRight;
    private double originX,originY;
    public boolean start(entity.Enemy boss,WorldMap.Point tree){
        if(!(boss instanceof boss.BossCombatant))return boss instanceof ChristmasEater legacy&&start(legacy,tree);
        if(started||boss==null)return false;
        started=true;this.tree=tree;fallen=boss;forest=boss instanceof boss.FrostFang;
        x=boss.getX()+boss.getSize()/2.0;y=boss.getY()+boss.getSize();facingRight=boss instanceof boss.FrostFang fang&&fang.isFacingRight();frames=0;return true;
    }
    public boolean start(ChristmasEater boss,WorldMap.Point tree) {
        if(started||boss==null)return false;
        started=true;this.tree=tree;x=boss.getX()+38;y=boss.getY()+76;frames=0;
        guardian=new BufferedImage(236,256,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=guardian.createGraphics();
        String phase=boss.getPhase()==ChristmasEater.Phase.PHASE_1?"1":"2";
        BufferedImage art=util.ImageAssets.load("assets/characters/boss/christmas_eater/phase"+phase+"/christmas_eater_p"+phase+"_idle_01.png");
        if(art!=null)g.drawImage(util.PixelArt.scale(art,144,144),46,48,null);
        else {g.translate(118-x,176-y);boss.draw(g);}
        g.dispose();return true;
    }
    public boolean isActive(){return started&&frames<getDuration();}
    public int getFrames(){return frames;}
    /** Existing grounded pose, after its seven-update dissolve; never a gameplay timer. */
    public int getImpactFrame(){return forest?104:150;}
    public void update(){if(isActive()&&++frames==getDuration()){guardian=null;fallen=null;}}
    public void finish(){if(started){frames=getDuration();guardian=null;fallen=null;}}
    public void frameCamera(Camera camera,int width,int height,WorldMap map) {
        double p=frames/(double)getDuration();p=p*p*(3-2*p);
        camera.follow(x+(tree.x()-x)*p,y+(tree.y()+40-y)*p,width,height,map.width(),map.height());
    }
    /** Start in the existing framing, reveal the release, then ease back before control resumes. */
    public void frameCamera(Camera camera,int width,int height,WorldMap map,entity.Santa santa){
        if(!cameraBound){originX=camera.getX()+width/2.0;originY=camera.getY()+height/2.0;cameraBound=true;}
        double focusX=x,focusY=y-(forest?25:55);
        double attentionX=forest?focusX:tree.x(),attentionY=forest?focusY:tree.y()+16;
        double cx,cy,t;
        int reveal=forest?72:90,attentionEnd=forest?132:180;
        if(frames<reveal){t=ease(frames/(double)reveal);cx=mix(originX,focusX,t);cy=mix(originY,focusY,t);}
        else if(frames<attentionEnd){t=ease((frames-reveal)/(double)(attentionEnd-reveal));cx=mix(focusX,attentionX,t);cy=mix(focusY,attentionY,t);}
        else{t=ease((frames-attentionEnd)/(double)(getDuration()-attentionEnd));cx=mix(attentionX,santa.getX()+16,t);
            double returnY=santa.getY()+21;
            if(!forest&&fallen instanceof boss.DarkTitan)returnY-=TownChapterArt.treeCameraLift(santa.getX()+16,returnY,tree);
            cy=mix(attentionY,returnY,t);}
        camera.follow(cx,cy,width,height,map.width(),map.height());
    }
    private static double ease(double t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    private static double mix(double a,double b,double t){return a+(b-a)*t;}
    public void drawWorld(Graphics2D original) {
        if(!isActive())return;
        double p=frames/(double)getDuration();
        Graphics2D g=(Graphics2D)original.create();
        try {
            if(fallen instanceof boss.BossCombatant){
                // Cached artwork follows the staged fall clock; no boss AI or animation update runs here.
                g.setComposite(AlphaComposite.SrcOver.derive((float)(1-p)));
                boss.BossArtwork.effect(g,"wave",frames/8,x,y-8,forest?210:270,90,0);
                if(!forest){
                    // Approved crimson core texture; contained instability, then energy collapse.
                    double collapse=1-ease((frames-getImpactFrame())/90.0);
                    float energy=(float)(collapse*(.45+.18*Math.sin(frames*.8)));
                    g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0,energy)));
                    boss.BossArtwork.effect(g,"titan_blast",8+frames/10%4,x,y-116,30,30,0);
                }
                g.setComposite(AlphaComposite.SrcOver.derive((float)(1-ease((frames-getFadeStart())/(double)(getDuration()-getFadeStart())))));
                boolean right=facingRight;
                Graphics2D body=(Graphics2D)g.create();
                if(frames<getImpactFrame()){
                    double stagger=ease(frames/(forest?56.0:94.0))*(1-ease((frames-getImpactFrame()+12)/12.0));
                    body.rotate((forest?-.035:.018)*Math.sin(frames/(forest?13.0:19.0))*stagger,x,y);
                }
                boss.BossArtwork.drawDefeat(body,forest,getPoseTick(),x,y,right);body.dispose();
                int impactAge=frames-getImpactFrame();
                if(impactAge>=0&&impactAge<48)boss.BossArtwork.effect(g,forest?"defeat":"titan_defeat",Math.min(11,impactAge/4),x,y-18,forest?180:230,forest?100:150,0);
                return;
            }
            util.LightBloom.draw(g,x,y-35,160,.24*(1-p),util.LightBloom.CYAN);
            g.setColor(new Color(145,192,219,(int)(80*(1-p))));
            for(int i=0;i<16;i++){double a=i*Math.PI/8,r=55+p*220;g.drawLine((int)(x+Math.cos(a)*r),(int)(y+Math.sin(a)*r*.5),(int)(x+Math.cos(a)*(r+30)),(int)(y+Math.sin(a)*(r+30)*.5));}
            if(guardian!=null) {
                g.setComposite(AlphaComposite.SrcOver.derive((float)(1-p*p)));
                g.translate(x,y);g.rotate(-p*.16);g.scale(1,1-p*.36);g.drawImage(guardian,-118,-176,null);
            }
        } finally {g.dispose();}
    }
    public void drawScreen(Graphics2D original,int width,int height) {
        if(!isActive())return;
        Graphics2D g=(Graphics2D)original.create();
        try {
            UITheme.letterbox(g,width,height,18);
            int w=Math.min(660,width-60),left=(width-w)/2,top=height-140-30;
            ui.ChristmasUI.frame(g,"parchment",new java.awt.Rectangle(left,top,w,140));g.setColor(ui.ChristmasUI.RED);g.setFont(new Font("Serif",Font.PLAIN,22));
            g.drawString(forest?"Frost Fang falls. The road opens.":"The guardian is free.",left+44,top+49);
            g.setFont(new Font("Dialog",Font.PLAIN,13));g.setColor(ui.ChristmasUI.INK);
            g.drawString(forest?"Christmas Town is beyond the eastern Forest gate.":"Black Frost weakens. The Spirit Tree can shine again.",left+44,top+76);
            g.setColor(ui.ChristmasUI.INK);g.drawString(forest?"[E / Esc] Continue   Follow the road to Christmas Town.":"[E / Esc] Continue   Then approach the Spirit Tree and press E.",left+44,top+103);
        } finally {g.dispose();}
    }
}
