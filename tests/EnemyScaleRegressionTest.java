import entity.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import javax.imageio.ImageIO;

/** Direct, deterministic renders; no navigation or game loop. */
public class EnemyScaleRegressionTest {
    static int checks;
    static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    static Rectangle bounds(BufferedImage im) {
        int x0=im.getWidth(), y0=im.getHeight(), x1=-1, y1=-1;
        for(int y=0;y<im.getHeight();y++) for(int x=0;x<im.getWidth();x++)
            if ((im.getRGB(x,y)>>>24)>0) {
                x0=Math.min(x0,x); y0=Math.min(y0,y);
                x1=Math.max(x1,x); y1=Math.max(y1,y);
            }
        return new Rectangle(x0,y0,x1-x0+1,y1-y0+1);
    }
    static BufferedImage render(Enemy e, int cameraX, int cameraY) {
        BufferedImage im=new BufferedImage(160,140,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=im.createGraphics();
        // Place the enemy at the same review anchor after scrolling the world.
        g.translate(80-e.getX()+cameraX, 80-e.getY()+cameraY);
        g.translate(-cameraX,-cameraY);
        g.setColor(Color.MAGENTA);
        g.setComposite(AlphaComposite.SrcOver);
        g.setClip(new Rectangle2D.Double(e.getX()-80,e.getY()-80,160,140));
        AffineTransform transform=g.getTransform(); Shape clip=g.getClip();
        RenderingHints hints=g.getRenderingHints(); Composite composite=g.getComposite();
        e.draw(g);
        check(transform.equals(g.getTransform()),"Transform leaked");
        check(clip.getBounds2D().equals(g.getClip().getBounds2D()),"Clip leaked");
        check(hints.equals(g.getRenderingHints()) && composite.equals(g.getComposite())
                && Color.MAGENTA.equals(g.getColor()),"Graphics state leaked");
        g.dispose(); return im;
    }
    static void sameSize(Rectangle expected, BufferedImage actual, String label) {
        Rectangle b=bounds(actual);
        check(Math.abs(expected.width-b.width)<=2 && Math.abs(expected.height-b.height)<=2,
            label+": expected "+expected+", actual "+b);
        check(Math.abs(expected.getCenterX()-b.getCenterX())<=1.5
            && Math.abs(expected.getMaxY()-b.getMaxY())<=2,label+": anchor changed");
    }
    static void set(Enemy e,String name,Object value)throws Exception {
        Field f=Enemy.class.getDeclaredField(name); f.setAccessible(true); f.set(e,value);
    }
    public static void main(String[] args)throws Exception {
        Enemy[] enemies={new Walker(300,300),new Runner(300,300)};
        String[] labels={"FAR","MEDIUM","CLOSE","ATTACK RANGE"};
        int[] distances={500,150,55,20};
        BufferedImage review=new BufferedImage(640,600,BufferedImage.TYPE_INT_RGB);
        Graphics2D rg=review.createGraphics(); rg.setColor(new Color(26,32,42));rg.fillRect(0,0,640,600);
        for(int type=0;type<enemies.length;type++) {
            Enemy e=enemies[type]; Rectangle reference=bounds(render(e,0,0));
            // Preserve the original normal idle size, including its authored padding.
            String name=e.getClass().getSimpleName().toLowerCase();
            BufferedImage idle=ImageIO.read(new File("assets/characters/enemies/"+name+"/idle/"+name+"_idle_down_01.png"));
            Rectangle raw=bounds(idle); int renderSize=type==1?60:type==3?62:64;
            check(Math.abs(reference.width-raw.width*renderSize/448.0)<=2
                && Math.abs(reference.height-raw.height*renderSize/448.0)<=2,"Normal idle size changed: "+name);
            for(int i=0;i<distances.length;i++) {
                Santa santa=new Santa((int)(e.getX()+e.getSize()/2.0-16),
                    (int)(e.getY()+e.getSize()/2.0+distances[i]-21));
                e.advanceCombatTimers(); e.update(santa,List.of());
                if(i==3) {
                    if (true) {
                        check(e.getVisualState()==Enemy.VisualState.IDLE,"Stop at attack range");
                        sameSize(reference,render(e,0,0),name+" close idle before attack");
                    }
                    e.tryAttackSanta(santa); check(e.getVisualState()==Enemy.VisualState.ATTACK,"Attack transition");
                }
                else check(e.getVisualState()==Enemy.VisualState.MOVING,"Chase/movement transition");
                BufferedImage frame=render(e,0,0); sameSize(reference,frame,name+" "+labels[i]);
                BufferedImage camera=render(e,157,233);
                check(java.util.Arrays.equals(frame.getRGB(0,0,160,140,null,0,160),
                    camera.getRGB(0,0,160,140,null,0,160)),"Camera changed render");
                rg.drawImage(frame,i*160,type*150,null);rg.setColor(Color.WHITE);
                rg.drawString(name+" / "+labels[i],i*160+6,type*150+18);
            }
            // Every authored movement frame/direction through the actual selection path.
            set(e,"attackCooldownFrames",0);set(e,"warningFrames",0); set(e,"movedThisFrame",true);
            double[][] directions={{0,1},{0,-1},{-1,0},{1,0}};
            for(double[] d:directions) for(int frame=0;frame<4;frame++) {
                set(e,"lastMoveX",d[0]);set(e,"lastMoveY",d[1]);set(e,"movementAnimationTicks",frame*8);
                sameSize(reference,render(e,0,0),name+" "+e.getMovementDirection()+" frame "+frame);
            }
            e.takeDamage(1);sameSize(reference,render(e,0,0),name+" damaged");
            e.advanceCombatTimers();check(e.getVisualState()==Enemy.VisualState.IDLE,"Return to idle");
            sameSize(reference,render(e,0,0),name+" idle again");
            check(e.getBounds().getWidth()==e.getSize() && e.getBounds().getHeight()==e.getSize(),
                "Collision dimensions changed");
        }
        rg.dispose(); new File("build/enemy-scale").mkdirs();
        ImageIO.write(review,"png",new File("build/enemy-scale/enemy-scale-review.png"));
        System.out.println("PASS: "+checks+" checks; both retained enemy types, distances, transitions, 64 movement frames, camera and Graphics2D state");
    }
}
