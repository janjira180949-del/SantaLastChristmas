package boss;

import entity.Enemy;
import entity.Santa;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import util.ImageAssets;

/** Three authored phases: directional sweeps, temporary frost and a final pulse with safe lanes. */
public class ChristmasEater extends Enemy {
    public enum Phase { PHASE_1, PHASE_2, PHASE_3 }
    private static final int MAX_HEALTH = 900;
    private static final int SIZE = 76;
    private static final BufferedImage[] PHASE_ART = {
        ImageAssets.load("assets/characters/boss/christmas_eater/phase1/christmas_eater_p1_idle_01.png"),
        ImageAssets.load("assets/characters/boss/christmas_eater/phase2/christmas_eater_p2_idle_01.png")};
    private Phase phase = Phase.PHASE_1;
    private int waveCooldown = 180, waveWarning, recovery, waveEffect;
    private double waveX, waveY;
    private int waveRadius;
    private int visualTicks,phaseTransitionFrames;
    private static final BufferedImage[] SHARP_ART={util.PixelArt.scale(PHASE_ART[0],144,144),util.PixelArt.scale(PHASE_ART[1],144,144)};
    private static final BufferedImage[] FLASH_ART={util.PixelArt.silhouette(SHARP_ART[0],util.LightBloom.IVORY),util.PixelArt.silhouette(SHARP_ART[1],util.LightBloom.IVORY)};
    public int getPhaseTransitionFrames() {return phaseTransitionFrames;}
    public ChristmasEater(double x,double y) { super(x,y,MAX_HEALTH,1.15,SIZE); }
    private Phase wavePhase=Phase.PHASE_1;
    private int heavyWarning,heavyCooldown,heavyEffect,hazardCooldown=90,hazardWarning,hazardLife;
    private double heavyX,heavyY,heavyDx,heavyDy;
    private final double[] hazardX=new double[3],hazardY=new double[3];
    private final boolean[] hazardHit=new boolean[3];
    private int hazards;
    public int getHeavyWarningFrames(){return heavyWarning;}
    public int getHazardCount(){return hazards;}
    public int getHazardWarningFrames(){return hazardWarning;}
    public int getHazardLifeFrames(){return hazardLife;}
    public double getHazardX(int index){return hazardX[index];}
    public double getHazardY(int index){return hazardY[index];}
    public double getPulseX(){return waveX;}
    public double getPulseY(){return waveY;}
    public boolean isPulseDangerous(double x,double y) {
        double dx=x-waveX,dy=y-waveY;
        return Math.hypot(dx,dy)<=waveRadius&&(wavePhase!=Phase.PHASE_3||(Math.abs(dx)>54&&Math.abs(dy)>54));
    }
    @Override public boolean takeDamage(int damage) {
        Phase previous=phase;boolean hit=super.takeDamage(damage);
        if(isDefeated()){clearHazards();heavyWarning=waveWarning=waveEffect=heavyEffect=recovery=0;return hit;}
        if(getHealth()<=getMaximumHealth()/4)phase=Phase.PHASE_3;
        else if(getHealth()<=getMaximumHealth()/2)phase=Phase.PHASE_2;
        if(previous!=phase) {
            phaseTransitionFrames=36;recovery=45;waveWarning=heavyWarning=waveEffect=0;
            waveCooldown=150;hazardCooldown=0;clearHazards();
        }
        return hit;
    }
    public Phase getPhase(){return phase;}
    public int getWaveWarningFrames(){return waveWarning;}
    public int getRecoveryFrames(){return recovery;}
    @Override protected double movementMultiplier(){return phase==Phase.PHASE_1?1:phase==Phase.PHASE_2?1.5:1.85;}
    @Override protected double getDetectionRange(){return 1200;}
    private void clearHazards(){hazards=hazardWarning=hazardLife=0;java.util.Arrays.fill(hazardHit,false);}
    private void updateHazards(Santa santa) {
        if(hazards==0)return;
        if(hazardWarning>0){if(--hazardWarning==0)hazardLife=90;return;}
        if(hazardLife>0) {
            for(int i=0;i<hazards;i++)if(!hazardHit[i]&&Math.hypot(santa.getX()+16-hazardX[i],santa.getY()+21-hazardY[i])<44)
                hazardHit[i]=santa.takeDamage(18);
            if(--hazardLife==0)clearHazards();
        }
    }
    private void beginHazards(Santa santa,Iterable<? extends Rectangle2D> solids) {
        clearHazards();double x=santa.getX()+16,y=santa.getY()+21;
        int[][] offsets={{0,0},{104,-58},{-104,58}};
        for(int[] offset:offsets) {
            double px=x+offset[0],py=y+offset[1];
            if(util.Collision.intersectsAny(new Rectangle2D.Double(px-44,py-44,88,88),solids))continue;
            hazardX[hazards]=px;hazardY[hazards++]=py;
        }
        hazardWarning=hazards==0?0:60;hazardCooldown=300;
    }
    @Override public void update(Santa santa,Iterable<? extends Rectangle2D> solids) {
        visualTicks++;if(phaseTransitionFrames>0)phaseTransitionFrames--;
        if(isDefeated()||santa.isDefeated())return;
        if(waveEffect>0)waveEffect--;if(heavyEffect>0)heavyEffect--;
        if(heavyCooldown>0)heavyCooldown--;if(hazardCooldown>0)hazardCooldown--;
        updateHazards(santa);
        if(recovery>0){recovery--;return;}
        if(waveWarning>0) {
            if(--waveWarning==0) {
                notifyAttackExecuted();
                if(isPulseDangerous(santa.getX()+16,santa.getY()+21))santa.takeDamage(wavePhase==Phase.PHASE_1?16:wavePhase==Phase.PHASE_2?22:24);
                waveEffect=18;recovery=wavePhase==Phase.PHASE_3?100:wavePhase==Phase.PHASE_2?65:90;
                waveCooldown=phase==Phase.PHASE_3?165:phase==Phase.PHASE_2?180:240;
            }
            return;
        }
        if(heavyWarning>0) {
            waveCooldown--;
            if(--heavyWarning==0) {
                notifyAttackExecuted();double dx=santa.getX()+16-heavyX,dy=santa.getY()+21-heavyY,d=Math.hypot(dx,dy);
                double range=phase==Phase.PHASE_1?125:150;
                if(d<=range&&(d<1||(dx*heavyDx+dy*heavyDy)/d>=.766))santa.takeDamage(phase==Phase.PHASE_1?14:20);
                heavyEffect=12;heavyCooldown=120;recovery=phase==Phase.PHASE_1?42:35;
            }
            return;
        }
        if(--waveCooldown<=0) {
            cancelMeleeWarning();clearHazards();
            waveX=getX()+SIZE/2.0;waveY=getY()+SIZE/2.0;wavePhase=phase;
            waveRadius=phase==Phase.PHASE_3?280:phase==Phase.PHASE_2?210:150;
            waveWarning=phase==Phase.PHASE_3?72:phase==Phase.PHASE_2?60:66;return;
        }
        if(phase!=Phase.PHASE_1&&hazards==0&&hazardCooldown==0&&waveCooldown>80)beginHazards(santa,solids);
        double dx=santa.getX()+16-getX()-38,dy=santa.getY()+21-getY()-38,d=Math.hypot(dx,dy);
        if(phase==Phase.PHASE_2&&d>125&&d<getDetectionRange()&&(visualTicks+60)%180<45&&getHitFlashFrames()<=2) {
            int side=(visualTicks/180)%2==0?1:-1;
            moveToward(santa.getX()+16-dy/d*80*side,santa.getY()+21+dx/d*80*side,solids);
        } else super.update(santa,solids);
    }
    @Override public boolean tryAttackSanta(Santa santa) {
        if(isDefeated()||santa.isDefeated()||waveWarning>0||recovery>0||heavyWarning>0||heavyCooldown>0||waveCooldown<60)return false;
        double dx=santa.getX()+16-getX()-38,dy=santa.getY()+21-getY()-38,d=Math.hypot(dx,dy);
        if(d<=110) {
            heavyX=getX()+38;heavyY=getY()+38;heavyDx=d==0?0:dx/d;heavyDy=d==0?1:dy/d;
            heavyWarning=phase==Phase.PHASE_1?48:phase==Phase.PHASE_2?38:32;
        }
        return false; // Execution is owned by update, once after its locked telegraph.
    }
    @Override public void drawAttackWarning(Graphics2D original) {
        Graphics2D g=(Graphics2D)original.create();
        try {
            g.setFont(new java.awt.Font("Dialog",java.awt.Font.BOLD,12));g.setStroke(new java.awt.BasicStroke(2));
            for(int i=0;i<hazards;i++) {
                g.setColor(new Color(125,69,149,hazardWarning>0?48:115));g.fillOval((int)hazardX[i]-44,(int)hazardY[i]-44,88,88);
                g.setColor(hazardWarning>0?new Color(255,176,121):new Color(191,130,228));g.drawOval((int)hazardX[i]-44,(int)hazardY[i]-44,88,88);
                if(hazardWarning>0)g.drawString("FROST",(int)hazardX[i]-22,(int)hazardY[i]-50);
                else for(int j=0;j<6;j++){double a=j*Math.PI/3;g.drawLine((int)hazardX[i],(int)hazardY[i],(int)(hazardX[i]+Math.cos(a)*35),(int)(hazardY[i]+Math.sin(a)*35));}
            }
            if(heavyWarning>0||heavyEffect>0) {
                int radius=phase==Phase.PHASE_1?125:150;
                var sector=new java.awt.geom.Arc2D.Double(heavyX-radius,heavyY-radius,radius*2,radius*2,
                    -Math.toDegrees(Math.atan2(heavyDy,heavyDx))-40,80,java.awt.geom.Arc2D.PIE);
                g.setColor(new Color(239,88,85,heavyWarning>0?60:125));g.fill(sector);g.setColor(new Color(255,188,132));g.draw(sector);
                g.drawString("HEAVY SWEEP - STEP ASIDE",(int)heavyX-95,(int)heavyY-radius-12);
            }
            if(waveWarning>0||waveEffect>0) {
                java.awt.geom.Area danger=new java.awt.geom.Area(new java.awt.geom.Ellipse2D.Double(waveX-waveRadius,waveY-waveRadius,waveRadius*2,waveRadius*2));
                if(wavePhase==Phase.PHASE_3) {
                    java.awt.Shape horizontal=new Rectangle2D.Double(waveX-waveRadius,waveY-54,waveRadius*2,108);
                    java.awt.Shape vertical=new Rectangle2D.Double(waveX-54,waveY-waveRadius,108,waveRadius*2);
                    danger.subtract(new java.awt.geom.Area(horizontal));danger.subtract(new java.awt.geom.Area(vertical));
                    g.setColor(new Color(93,229,191,65));g.fill(horizontal);g.fill(vertical);
                    g.setColor(new Color(165,255,225));g.draw(horizontal);g.draw(vertical);
                }
                g.setColor(new Color(157,72,143,waveWarning>0?60:135));g.fill(danger);g.setColor(new Color(255,173,144));g.draw(danger);
                int total=wavePhase==Phase.PHASE_3?72:wavePhase==Phase.PHASE_2?60:66;
                double progress=waveWarning>0?1-waveWarning/(double)total:1;int inner=(int)(waveRadius*progress);
                g.drawOval((int)waveX-inner,(int)waveY-inner,inner*2,inner*2);
                g.drawString(wavePhase==Phase.PHASE_3?"BLACK FROST PULSE - GREEN LANES ARE SAFE":"BLACK FROST - DASH OUT",wavePhase==Phase.PHASE_3?(int)waveX-waveRadius+16:(int)waveX-150,wavePhase==Phase.PHASE_3?(int)waveY-65:(int)waveY-waveRadius-12);
            } else if(recovery>0){g.setColor(new Color(255,229,164));g.drawString("RECOVERY - STRIKE NOW",(int)getX()-38,(int)getY()-72);}
        } finally {g.dispose();}
    }
    @Override
    public void draw(Graphics2D graphics) {
        int x = (int) getX();
        BufferedImage art = SHARP_ART[Math.min(1,phase.ordinal())];
        if (art != null) {
            Graphics2D g=(Graphics2D)graphics.create();
            g.setColor(new Color(5,14,29,100));g.fillOval(x-11,(int)getY()+61,98,24);
            util.LightBloom.draw(g,x+38,getY()+35,95,.12+.02*Math.sin(visualTicks/45.0),phase==Phase.PHASE_1?util.LightBloom.CYAN:util.LightBloom.RED);
            int breathe=(visualTicks/55)%2;g.drawImage(art,x-34,(int)getY()-52-breathe,null);
            if(getHitFlashFrames()>0) {g.setComposite(java.awt.AlphaComposite.SrcOver.derive(getHitFlashFrames()/12f));g.drawImage(FLASH_ART[Math.min(1,phase.ordinal())],x-34,(int)getY()-52-breathe,null);g.setComposite(java.awt.AlphaComposite.SrcOver);}
            if(phaseTransitionFrames>0) {
                util.LightBloom.draw(g,x+38,getY()+35,125,phaseTransitionFrames/80.0,util.LightBloom.RED);
                for(int i=0;i<12;i++){double a=i*Math.PI/6,r=50+(36-phaseTransitionFrames)*2;g.setColor(new Color(238,168,147,phaseTransitionFrames*5));g.fillRect(x+38+(int)(Math.cos(a)*r),(int)getY()+35+(int)(Math.sin(a)*r),2,3);}
            }
            g.dispose();
            return;
        }
        int y = (int) getY();
        Color body = switch (phase) {
            case PHASE_1 -> new Color(79, 43, 92);
            case PHASE_2 -> new Color(139, 39, 57);
            case PHASE_3 -> new Color(158, 45, 103);
        };

        graphics.setColor(body);
        graphics.fillOval(x + 8, y + 4, SIZE - 16, SIZE - 8);
        graphics.fillRoundRect(x, y + 30, SIZE, SIZE - 30, 22, 22);
        graphics.setColor(new Color(211, 203, 189));
        graphics.fillPolygon(new int[] {x + 8, x - 3, x + 17},
                new int[] {y + 20, y - 6, y + 13}, 3);
        graphics.fillPolygon(new int[] {x + SIZE - 8, x + SIZE + 3, x + SIZE - 17},
                new int[] {y + 20, y - 6, y + 13}, 3);
        graphics.setColor(new Color(255, 87, 65));
        graphics.fillOval(x + 23, y + 25, 9, 8);
        graphics.fillOval(x + SIZE - 32, y + 25, 9, 8);
        graphics.setColor(Color.BLACK);
        graphics.fillOval(x + 28, y + 47, 20, 13);
        graphics.setColor(new Color(238, 224, 195));
        graphics.drawLine(x + 31, y + 50, x + 31, y + 58);
        graphics.drawLine(x + 45, y + 50, x + 45, y + 58);

        if (phase == Phase.PHASE_2) {
            graphics.setColor(new Color(255, 92, 93));
            graphics.drawOval(x - 5, y - 7, SIZE + 10, SIZE + 12);
        }
    }
}
