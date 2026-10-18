package boss;

import entity.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import util.Collision;

/** Fixed-size, update-owned telegraph / execution / recovery. No quest or terrain ownership. */
public final class BossAttacks {
    public enum Attack { NONE,CLAW,ICE_BLAST,FROST_WAVE,FROST_DASH,GROUND_SLAM,ICE_SPIKES,BEAST_CHARGE,DARK_FROST_BEAM,BLACK_FROST_STORM }
    private Attack attack=Attack.NONE;
    private int ticks,warning,active,recovery;
    private double x,y,dx,dy,px,py,beamLength;
    private boolean hit,projectile;
    private final double[] sitesX=new double[8],sitesY=new double[8];
    private final boolean[] siteHit=new boolean[8];
    private int sites;
    public Attack getAttack(){return attack;}
    public int getTicks(){return ticks;}
    public int getExecutionAge(){return ticks-warning;}
    public boolean isProjectileActive(){return projectile;}
    /** Read-only successful-contact signal for presentation observers. */
    public boolean hasHit(){return hit;}
    public int getWarningFrames(){return Math.max(0,warning-ticks);}
    public boolean isExecuting(){return ticks>=warning&&ticks<warning+active;}
    public boolean isRecovering(){return ticks>=warning+active&&attack!=Attack.NONE;}
    public double getTargetX(){return x;}public double getTargetY(){return y;}
    public double getProjectileX(){return px;}public double getProjectileY(){return py;}
    public double getDirectionX(){return dx;}
    public int getTotalFrames(){return warning+active+recovery;}
    /** Presentation only: advance preparation, strike and recovery without changing their clocks. */
    public int getFangAnimationFrame(){
        if(ticks<warning)return Math.min(1,ticks*2/Math.max(1,warning));
        if(ticks<warning+active)return 2+Math.min(3,(ticks-warning)*4/Math.max(1,active));
        return 6+Math.min(1,(ticks-warning-active)*2/Math.max(1,recovery));
    }
    public void clear(){attack=Attack.NONE;sites=0;projectile=false;hit=false;ticks=0;}
    public void begin(Attack next,Enemy boss,Santa santa,Iterable<? extends Rectangle2D> solids){
        clear();attack=next;x=boss.getX()+boss.getSize()/2.0;y=next==Attack.DARK_FROST_BEAM?boss.getY()+4:next==Attack.GROUND_SLAM?boss.getY()+boss.getSize():boss.getY()+boss.getSize()/2.0;
        double vx=santa.getX()+16-x,vy=santa.getY()+21-y,d=Math.hypot(vx,vy);dx=d<1?0:vx/d;dy=d<1?1:vy/d;
        warning=switch(next){case CLAW->36;case ICE_BLAST->45;case FROST_WAVE->60;case FROST_DASH->54;case GROUND_SLAM->50;case ICE_SPIKES->60;case BEAST_CHARGE->45;case DARK_FROST_BEAM->60;case BLACK_FROST_STORM->72;default->0;};
        active=switch(next){case CLAW->12;case ICE_BLAST->48;case FROST_WAVE->36;case FROST_DASH->20;case GROUND_SLAM->40;case ICE_SPIKES->90;case BEAST_CHARGE->25;case DARK_FROST_BEAM->45;case BLACK_FROST_STORM->120;default->0;};
        recovery=next==Attack.BEAST_CHARGE?35:next==Attack.CLAW?36:48;
        px=next==Attack.ICE_BLAST?x+dx*30:x;py=next==Attack.ICE_BLAST?y-14+dy*8:y;
        beamLength=0;for(int i=0;i<380;i++){if(Collision.intersectsAny(new Rectangle2D.Double(x+dx*i-3,y+dy*i-3,6,6),solids))break;beamLength=i;}
        if(next==Attack.ICE_SPIKES||next==Attack.BLACK_FROST_STORM){
            int count=next==Attack.ICE_SPIKES?3:8;
            for(int i=0;i<count;i++){
                double sx=next==Attack.ICE_SPIKES?santa.getX()+16+(i==0?0:i==1?108:-108):x+Math.cos(i*Math.PI/4)*180;
                double sy=next==Attack.ICE_SPIKES?santa.getY()+21+(i==0?0:i==1?-56:56):y+Math.sin(i*Math.PI/4)*180;
                if(!Collision.intersectsAny(new Rectangle2D.Double(sx-32,sy-32,64,64),solids)){sitesX[sites]=sx;sitesY[sites]=sy;siteHit[sites++]=false;}
            }
        }
    }
    private static boolean line(double x,double y,double tx,double ty,Iterable<? extends Rectangle2D> solids){
        var segment=new java.awt.geom.Line2D.Double(x,y,tx,ty);for(var solid:solids)if(solid.intersectsLine(segment))return false;return true;
    }
    public boolean update(Enemy boss,Santa santa,Iterable<? extends Rectangle2D> solids){
        if(attack==Attack.NONE)return false;
        if(boss.isDefeated()||santa.isDefeated()){clear();return false;}
        ticks++;
        if(ticks<warning)return false;
        int t=ticks-warning;double sx=santa.getX()+16,sy=santa.getY()+21;
        if(t<active){
            switch(attack){
                case CLAW->{double vx=sx-x,vy=sy-y,d=Math.hypot(vx,vy);if(!hit&&t==0&&d<102&&(d<1||(vx*dx+vy*dy)/d>.5)&&line(x,y,sx,sy,solids))hit=santa.takeDamage(14);}
                case ICE_BLAST,FROST_WAVE->{
                    if(t==0)projectile=true;int speed=attack==Attack.ICE_BLAST?8:7;
                    double radius=attack==Attack.ICE_BLAST?10:32;
                    if(projectile)for(int i=0;i<speed;i++){
                        if(Collision.intersectsAny(new Rectangle2D.Double(px+dx-radius,py+dy-radius,radius*2,radius*2),solids)){projectile=false;break;}
                        px+=dx;py+=dy;
                        if(!hit&&Math.hypot(sx-px,sy-py)<radius+16){hit=santa.takeDamage(attack==Attack.ICE_BLAST?16:18);projectile=false;break;}
                    }
                }
                case FROST_DASH,BEAST_CHARGE->{
                    if(t<(attack==Attack.BEAST_CHARGE?15:20))((BossCombatant)boss).moveAttack(dx*(attack==Attack.BEAST_CHARGE?12:10),dy*(attack==Attack.BEAST_CHARGE?12:10),solids);
                    px=boss.getX()+boss.getSize()/2.0;py=boss.getY()+boss.getSize()/2.0;
                    double range=attack==Attack.BEAST_CHARGE?(t>=15?110:68):54;
                    if(!hit&&Math.hypot(sx-px,sy-py)<range&&line(px,py,sx,sy,solids))hit=santa.takeDamage(attack==Attack.BEAST_CHARGE?24:20);
                }
                case GROUND_SLAM->{double radius=18+t*6,d=Math.hypot(sx-x,sy-y);if(!hit&&Math.abs(d-radius)<22&&line(x,y,sx,sy,solids))hit=santa.takeDamage(20);}
                case ICE_SPIKES,BLACK_FROST_STORM->{for(int i=0;i<sites;i++)if(!siteHit[i]&&Math.hypot(sx-sitesX[i],sy-sitesY[i])<38&&line(sitesX[i],sitesY[i],sx,sy,solids))siteHit[i]=santa.takeDamage(attack==Attack.ICE_SPIKES?18:16);}
                case DARK_FROST_BEAM->{double vx=sx-x,vy=sy-y,along=vx*dx+vy*dy,side=Math.abs(vx*dy-vy*dx),front=Math.min(beamLength,(t+1)*64);if(!hit&&along>=0&&along<=front&&side<24&&line(x,y,sx,sy,solids))hit=santa.takeDamage(22);}
                default->{}
            }
        }
        if(ticks>=warning+active+recovery){clear();return false;}
        return t==0;
    }
    private void effect(Graphics2D g,String name,int frame,double x,double y,double w,double h,double angle){
        BossArtwork.effect(g,(attack.ordinal()>=Attack.GROUND_SLAM.ordinal()?"titan_":"")+name,frame,x,y,w,h,angle);
    }
    /** Texture-based dotted / crystalline warnings, not debug collision shapes. */
    private void outline(Graphics2D g,double cx,double cy,double radius){
        int count=radius>100?64:24;
        for(int i=0;i<count;i++){double a=i*Math.PI*2/count;effect(g,"warning",ticks/5,cx+Math.cos(a)*radius,cy+Math.sin(a)*radius,12,12,a);}
    }
    public void draw(Graphics2D g){
        if(attack==Attack.NONE)return;int t=ticks-warning,frame=Math.max(0,t)/3;
        if(attack==Attack.DARK_FROST_BEAM){game.Phase3Vfx.beam(g,x,y,dx,dy,beamLength,ticks,warning,active);if(t>=0)return;}
        if(t<0){
            if(attack==Attack.ICE_SPIKES||attack==Attack.BLACK_FROST_STORM)for(int i=0;i<sites;i++)outline(g,sitesX[i],sitesY[i],38);
            else if(attack==Attack.GROUND_SLAM)outline(g,x,y,274);
            else if(attack==Attack.CLAW)for(int i=-5;i<=5;i++){double a=Math.atan2(dy,dx)+i*.209;effect(g,"warning",ticks/5,x+Math.cos(a)*102,y+Math.sin(a)*102,14,14,a);}
            else for(int i=0;i<9;i++){double distance=i*(attack==Attack.DARK_FROST_BEAM?beamLength/9:30);effect(g,"warning",ticks/5,x+dx*distance,y+dy*distance,14,14,Math.atan2(dy,dx));}
        }else if(t<active){
            switch(attack){
                case CLAW->effect(g,"claw",frame,x+dx*48,y+dy*48,120,120,Math.atan2(dy,dx));
                case ICE_BLAST,FROST_WAVE->{if(projectile)effect(g,attack==Attack.ICE_BLAST?"blast":"wave",9+frame%3,px,py,attack==Attack.ICE_BLAST?50:108,attack==Attack.ICE_BLAST?64:144,Math.atan2(dy,dx));else effect(g,"hurt",frame,px,py,60,60,0);}
                case FROST_DASH,BEAST_CHARGE->{for(int i=0;i<5;i++)effect(g,"dash",frame+i,px-dx*i*22,py-dy*i*22,46,34,Math.atan2(dy,dx));if(attack==Attack.BEAST_CHARGE&&t>=15)effect(g,"slam",frame,px,py,220,160,0);}
                case GROUND_SLAM->{double r=18+t*6;effect(g,"slam",Math.min(11,frame),x,y,160,112,0);for(int i=0;i<28;i++){double a=i*Math.PI/14;effect(g,"blast",9+frame%3,x+Math.cos(a)*r,y+Math.sin(a)*r,26,34,a);}}
                case ICE_SPIKES,BLACK_FROST_STORM->{for(int i=0;i<sites;i++)effect(g,attack==Attack.ICE_SPIKES?"spikes":"storm",(frame+i)%12,sitesX[i],sitesY[i]-12,82,100,i*.3);}
                case DARK_FROST_BEAM->{double front=Math.min(beamLength,(t+1)*64);for(int i=12;i<front;i+=18)effect(g,"beam",9+frame%3,x+dx*i,y+dy*i,64,60,Math.atan2(dy,dx)-Math.PI/2);}
                default->{}
            }
        }else if(t<active+Math.min(12,recovery)){
            // A short, non-damaging residue distinguishes recovery from active danger.
            int fade=Math.min(11,8+(t-active)/3);
            if(attack==Attack.ICE_SPIKES||attack==Attack.BLACK_FROST_STORM)
                for(int i=0;i<sites;i++)effect(g,"hurt",fade,sitesX[i],sitesY[i],40,32,0);
            else effect(g,"hurt",fade,px,py,48,36,0);
        }
    }
    public String status(){return attack==Attack.NONE?"":getWarningFrames()>0?switch(attack){case CLAW->"Claws ready - step aside";case ICE_BLAST->"Ice blast - move sideways";case FROST_WAVE->"Frost wave approaching";case FROST_DASH,BEAST_CHARGE->"Charge locked - SPACE to dodge";case GROUND_SLAM->"Ground slam - dash out";case ICE_SPIKES->"Ice spikes marked - move";case DARK_FROST_BEAM->"Core charging - leave the line";case BLACK_FROST_STORM->"Storm marked - find a clear gap";default->"";}:isRecovering()?"Recovery - strike now":attack.name().replace('_',' ');}
}
