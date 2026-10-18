package game;

import entity.Enemy;
import entity.Santa;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Update-driven triple stars and gifts. No spell owns quest progress or basic attack identity. */
public final class SantaSkills {
    public static final int COOLDOWN=180, STAR_COOLDOWN=60, STAR_DAMAGE=18, BOMB_DAMAGE=110;
    public static final double STAR_SPEED=12, STAR_RANGE=480, THROW_RANGE=280, BOMB_RADIUS=130;
    public static final int THROW_FRAMES=20, FUSE_FRAMES=30, MAX_STARS=192;
    private int bombCooldown,starCooldown;
    private java.util.function.Consumer<String> audio=id->{};
    private java.util.function.BiConsumer<String,String> ownedAudio=(id,owner)->{};
    private java.util.function.Consumer<String> stopAudio=owner->{};
    public void bindAudio(java.util.function.Consumer<String> events,java.util.function.BiConsumer<String,String> owned,java.util.function.Consumer<String> stop){audio=events;ownedAudio=owned;stopAudio=stop;}
    public void audioEvent(String id){audio.accept(id);}
    private final List<Star> stars=new ArrayList<>(MAX_STARS);
    private final List<Star> pool=new ArrayList<>(MAX_STARS);
    private final List<Bomb> bombs=new ArrayList<>(2);
    private final List<Burst> bursts=new ArrayList<>(24);
    private List<Enemy> knownEnemies=List.of();
    private double castX,castY,castAngle;
    private int castFrames,castSlot;
    public record StarView(double x,double y,double dx,double dy,double distance) {}
    public record BombView(double x,double y,int age) {}
    private static final class Star {double x,y,dx,dy,distance;int delay,age,color;Enemy target;}
    private static final class Bomb {double sx,sy,x,y;int age;Bomb(double sx,double sy,double x,double y){this.sx=sx;this.sy=sy;this.x=x;this.y=y;}}
    private static final class Burst {double x,y;int age;boolean bomb;Burst(double x,double y,boolean bomb){this.x=x;this.y=y;this.bomb=bomb;}}
    public SantaSkills(){for(int i=0;i<MAX_STARS;i++)pool.add(new Star());}
    public int starCooldown(){return starCooldown;}
    public int bombCooldown(){return bombCooldown;}
    public List<StarView> stars(){return stars.stream().map(s->new StarView(s.x,s.y,s.dx,s.dy,s.distance)).toList();}
    public List<BombView> bombs(){return bombs.stream().map(b->new BombView(b.x,b.y,b.age)).toList();}
    public int burstCount(){return bursts.size();}
    private void recycle(int index){Star star=stars.remove(index);star.target=null;pool.add(star);}
    public void clearEffects(){stopAudio.accept("bomb-fuse");while(!stars.isEmpty())recycle(stars.size()-1);bombs.clear();bursts.clear();castFrames=0;knownEnemies=List.of();}
    public void reset(){clearEffects();bombCooldown=starCooldown=0;}
    private static boolean blocked(double x,double y,Iterable<? extends Rectangle2D> solids){
        for(var r:solids)if(r.intersects(x-4,y-4,8,8))return true;return false;
    }
    private static boolean clearLine(double x,double y,double tx,double ty,Iterable<? extends Rectangle2D> solids){
        for(var r:solids)if(r.intersectsLine(x,y,tx,ty))return false;return true;
    }
    private Enemy acquire(double x,double y,double dx,double dy,Iterable<? extends Rectangle2D> solids){
        Enemy best=null;double score=Double.MAX_VALUE;
        for(var e:knownEnemies)if(!e.isDefeated()){
            double vx=e.getX()+e.getSize()/2.0-x,vy=e.getY()+e.getSize()/2.0-y,d=Math.hypot(vx,vy);
            if(d<1||d>STAR_RANGE||Math.abs(vx)>360||Math.abs(vy)>240)continue;
            double forward=(vx*dx+vy*dy)/d;
            if(forward<.92||!clearLine(x,y,x+vx,y+vy,solids))continue;
            double value=d+600*(1-forward);
            if(value<score){score=value;best=e;}
        }return best;
    }
    public boolean cast(int slot,Santa santa,double tx,double ty,Iterable<? extends Rectangle2D> solids){
        if(santa.isDefeated()||(slot!=2&&slot!=3)||(slot==2&&starCooldown>0)||(slot==3&&bombCooldown>0))return false;
        double sx=santa.getX()+16,sy=santa.getY()+21,dx=tx-sx,dy=ty-sy,d=Math.hypot(dx,dy);
        if(d<1){dx=0;dy=1;d=1;}dx/=d;dy/=d;
        castX=sx;castY=sy;castAngle=Math.atan2(dy,dx);castFrames=12;castSlot=slot;
        if(slot==2){
            // One accepted volley starts one active-gameplay cooldown; delayed releases do not restart it.
            starCooldown=STAR_COOLDOWN;audio.accept("star.cast");audio.accept("star.release.v01");
            for(int i=0;i<3;i++){
                if(pool.isEmpty())recycle(0);
                Star star=pool.remove(pool.size()-1);double angle=castAngle+(i==0?0:i==1?-.075:.075);
                star.x=sx;star.y=sy;star.dx=Math.cos(angle);star.dy=Math.sin(angle);
                star.distance=0;star.delay=i*4;star.age=0;star.color=i;star.target=acquire(sx,sy,star.dx,star.dy,solids);
                stars.add(star);
            }
        }else{
            double distance=Math.min(d,THROW_RANGE),x=sx,y=sy;
            for(double t=1;t<=distance;t++){double nx=sx+dx*t,ny=sy+dy*t;if(blocked(nx,ny,solids))break;x=nx;y=ny;}
            audio.accept("bomb.throw");bombCooldown=COOLDOWN;bombs.add(new Bomb(sx,sy,x,y));
        }return true;
    }
    private void burst(double x,double y,boolean bomb){if(bursts.size()==24)bursts.remove(0);bursts.add(new Burst(x,y,bomb));}
    public void impact(double x,double y){burst(x,y,false);}
    public void update(List<Enemy> enemies,Iterable<? extends Rectangle2D> solids){
        knownEnemies=enemies;if(starCooldown>0)starCooldown--;if(bombCooldown>0)bombCooldown--;if(castFrames>0)castFrames--;
        bursts.removeIf(b->++b.age>=24);
        for(int i=stars.size()-1;i>=0;i--){Star s=stars.get(i);
            if(s.delay>0){s.delay--;if(s.delay==0)audio.accept(s.color==1?"star.release.v02":"star.release.v03");continue;}s.age++;
            if(s.target==null&&s.age==1)s.target=acquire(s.x,s.y,s.dx,s.dy,solids);
            if(s.target!=null){
                if(s.target.isDefeated()||!enemies.contains(s.target)||!clearLine(s.x,s.y,s.target.getX()+s.target.getSize()/2.0,s.target.getY()+s.target.getSize()/2.0,solids))s.target=null;
                else{
                    double desired=Math.atan2(s.target.getY()+s.target.getSize()/2.0-s.y,s.target.getX()+s.target.getSize()/2.0-s.x);
                    double heading=Math.atan2(s.dy,s.dx),delta=Math.atan2(Math.sin(desired-heading),Math.cos(desired-heading));
                    heading+=Math.max(-.025,Math.min(.025,delta));s.dx=Math.cos(heading);s.dy=Math.sin(heading);
                }
            }
            boolean gone=false;
            for(int step=0;step<(int)STAR_SPEED&&!gone;step++){
                s.x+=s.dx;s.y+=s.dy;s.distance++;
                boolean wall=blocked(s.x,s.y,solids);if(wall||s.distance>=STAR_RANGE){if(wall)audio.accept("star.wall_hit");gone=true;burst(s.x,s.y,false);break;}
                for(var e:enemies)if(!e.isDefeated()&&e.getBounds().intersects(s.x-4,s.y-4,8,8)){
                    if(e.takeDamage(STAR_DAMAGE)){audio.accept("star.enemy_hit");burst(s.x,s.y,false);}gone=true;break;
                }
            }if(gone)recycle(i);
        }
        for(int i=bombs.size()-1;i>=0;i--){var b=bombs.get(i);int nextAge=++b.age;
            if(nextAge==THROW_FRAMES){audio.accept("bomb.land");ownedAudio.accept("bomb.fuse","bomb-fuse");}
            if(nextAge==THROW_FRAMES+FUSE_FRAMES){stopAudio.accept("bomb-fuse");audio.accept("bomb.explosion");
            for(var e:enemies)if(!e.isDefeated()){
                double ex=e.getX()+e.getSize()/2.0,ey=e.getY()+e.getSize()/2.0;
                if(Math.hypot(ex-b.x,ey-b.y)<=BOMB_RADIUS&&clearLine(b.x,b.y,ex,ey,solids)&&e.takeDamage(BOMB_DAMAGE)){
                    audio.accept("bomb.confirmed_hit");e.applyGiftSlow(60);burst(ex,ey,false);
                }
            }burst(b.x,b.y,true);bombs.remove(i);
        }}
    }
    public void draw(Graphics2D original){
        Graphics2D g=(Graphics2D)original.create();
        try{
            if(castFrames>0)SkillArtwork.cast(g,castX,castY,castAngle,12-castFrames,castSlot);
            for(var s:stars)if(s.delay==0&&s.age>0){
                for(int i=4;i>0;i--)g.drawImage(SkillArtwork.trail(i-1),(int)(s.x-s.dx*i*7)-3,(int)(s.y-s.dy*i*7)-3,null);
                SkillArtwork.drawStar(g,s.x,s.y,s.age,s.color);
            }
            for(var b:bombs){double t=Math.min(1,b.age/(double)THROW_FRAMES),x=b.sx+(b.x-b.sx)*t,y=b.sy+(b.y-b.sy)*t;
                SkillArtwork.landing(g,b.sx,b.sy,b.x,b.y,BOMB_RADIUS,b.age);
                SkillArtwork.drawGift(g,x,y-Math.sin(t*Math.PI)*38,b.age);
            }
            for(var b:bursts)SkillArtwork.burst(g,b.x,b.y,b.age,b.bomb);
        }finally{g.dispose();}
    }
}
