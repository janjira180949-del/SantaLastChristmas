package game;

import entity.Enemy;
import java.awt.*;
import java.util.List;

/** A bounded observer of damage. Never applies damage, stun, collision or hit-stop. */
public final class CombatVisuals {
    private final Enemy[] observed=new Enemy[16];
    private final int[] health=new int[16];
    private int observedCount,cursor;
    private final double[] x=new double[48],y=new double[48],vx=new double[48],vy=new double[48];
    private final int[] life=new int[48];
    public void reset() {java.util.Arrays.fill(life,0);observedCount=0;}
    public int activeParticles() {int count=0;for(int frames:life)if(frames>0)count++;return count;}
    public void beginFrame(List<Enemy> enemies) {
        for(int i=0;i<life.length;i++)if(life[i]>0){life[i]--;x[i]+=vx[i];y[i]+=vy[i];vy[i]+=.025;}
        observedCount=Math.min(observed.length,enemies.size());
        for(int i=0;i<observedCount;i++){observed[i]=enemies.get(i);health[i]=observed[i].getHealth();}
    }
    public void resolve() {
        for(int i=0;i<observedCount;i++) {
            Enemy enemy=observed[i];if(enemy.getHealth()>=health[i])continue;
            int count=enemy.isDefeated()?7:4;
            for(int n=0;n<count;n++) {
                int p=cursor++%life.length;double a=n*Math.PI*2/count+.35;
                x[p]=enemy.getX()+enemy.getSize()/2.0;y[p]=enemy.getY()+enemy.getSize()/2.0;
                vx[p]=Math.cos(a)*(enemy.isDefeated()?1.8:1.2);vy[p]=Math.sin(a)*1.2-.5;life[p]=enemy.isDefeated()?24:14;
            }
        }
        java.util.Arrays.fill(observed,0,observedCount,null);observedCount=0;
    }
    public void draw(Graphics2D graphics) {
        Graphics2D g=(Graphics2D)graphics.create();
        try {for(int i=0;i<life.length;i++)if(life[i]>0){g.setColor(new Color(255,224,169,Math.min(220,life[i]*12)));g.fillRect((int)x[i],(int)y[i],i%3==0?2:1,2);}}
        finally {g.dispose();}
    }
}
