import entity.*;
import game.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.lang.reflect.*;
import java.util.List;
import javax.imageio.ImageIO;
import java.io.File;

/** Santa animation integration checks, using the existing input and game update path. */
public class SantaArtworkTest {
    static void check(boolean b, String message) { if (!b) throw new AssertionError(message); }
    static Object field(Object object, String name) throws Exception {
        Field f=object.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(object);
    }
    static void tick(Santa s) { s.update(false,false,false,false,List.of()); }
    static void key(GamePanel p, int code, boolean down) {
        KeyEvent e=new KeyEvent(p,down?KeyEvent.KEY_PRESSED:KeyEvent.KEY_RELEASED,0,0,code,KeyEvent.CHAR_UNDEFINED);
        if(down) p.keyPressed(e); else p.keyReleased(e);
    }
    public static void main(String[] args) throws Exception {
        Santa s=new Santa(100,100);
        check(s.getAnimationState()==Santa.AnimationState.IDLE && s.getVisualDirection()==Santa.Direction.DOWN,"Default idle down");
        check(s.getBounds().equals(new Rectangle2D.Double(100,92,32,50)),"Collision unchanged");
        Object sprites=field(s,"SPRITES");
        BufferedImage[][][] frames=(BufferedImage[][][])field(sprites,"frames");
        for(int state=0;state<3;state++) for(int dir=0;dir<4;dir++) {
            check(frames[state][dir].length==state+1,"Frame count");
            for(BufferedImage im:frames[state][dir]) check(im!=null && im.getWidth()==384 && im.getHeight()==384,"All 24 assets decoded");
        }
        GamePanel panel=V2TestSupport.playing(); panel.setSize(800,600);
        Santa player=(Santa)field(panel,"santa");
        Method update=GamePanel.class.getDeclaredMethod("update"); update.setAccessible(true);
        int[] keys={KeyEvent.VK_W,KeyEvent.VK_UP,KeyEvent.VK_S,KeyEvent.VK_DOWN,KeyEvent.VK_A,KeyEvent.VK_LEFT,KeyEvent.VK_D,KeyEvent.VK_RIGHT};
        Santa.Direction[] directions={Santa.Direction.UP,Santa.Direction.UP,Santa.Direction.DOWN,Santa.Direction.DOWN,Santa.Direction.LEFT,Santa.Direction.LEFT,Santa.Direction.RIGHT,Santa.Direction.RIGHT};
        for(int i=0;i<keys.length;i++) {
            player.setPosition(1296,720);
            key(panel,keys[i],true); update.invoke(panel);
            check(player.getVisualDirection()==directions[i] && player.getAnimationState()==Santa.AnimationState.WALK && player.getAnimationFrame()==0,"Input and first walk frame");
            for(int j=0;j<8;j++) update.invoke(panel);
            check(player.getAnimationFrame()==1,"Second walk frame");
            for(int j=0;j<8;j++) update.invoke(panel);
            check(player.getAnimationFrame()==0,"Walk loops");
            key(panel,keys[i],false); update.invoke(panel);
            check(player.getAnimationState()==Santa.AnimationState.IDLE && player.getVisualDirection()==directions[i],"Idle keeps direction");
        }
        GamePanel sustainedPanel=V2TestSupport.playing();
        sustainedPanel.setSize(10000,10000);
        Santa sustained=(Santa)field(sustainedPanel,"santa");
        sustained.setPosition(5000,5000);
        Method sustainedUpdate=GamePanel.class.getDeclaredMethod("update");
        sustainedUpdate.setAccessible(true);
        boolean[] seenFrames=new boolean[2];
        for(int d=0;d<keys.length;d++) {
            seenFrames[0]=false;
            seenFrames[1]=false;
            key(sustainedPanel,keys[d],true);
            for(int t=0;t<180;t++) {
                sustainedUpdate.invoke(sustainedPanel);
                check(sustained.getAnimationState()==Santa.AnimationState.WALK
                    && sustained.getVisualDirection()==directions[d],
                    "Three-second hold keeps the requested walk state/direction");
                seenFrames[sustained.getAnimationFrame()]=true;
            }
            check(seenFrames[0] && seenFrames[1],
                "Both walk frames continue looping during a three-second hold");
            key(sustainedPanel,keys[d],false);
            sustainedUpdate.invoke(sustainedPanel);
            check(sustained.getAnimationState()==Santa.AnimationState.IDLE
                && sustained.getVisualDirection()==directions[d],
                "Stopping returns to idle without losing facing");
        }
        sustained.setPosition(5000,5000);
        key(sustainedPanel,KeyEvent.VK_W,true);
        for(int t=0;t<9;t++) sustainedUpdate.invoke(sustainedPanel);
        check(sustained.getAnimationFrame()==1,"Walk phase reaches second frame before turn");
        int[] turnKeys={KeyEvent.VK_D,KeyEvent.VK_S,KeyEvent.VK_A};
        Santa.Direction[] turnDirections={
            Santa.Direction.RIGHT,Santa.Direction.DOWN,Santa.Direction.LEFT
        };
        int previousKey=KeyEvent.VK_W;
        for(int turn=0;turn<turnKeys.length;turn++) {
            key(sustainedPanel,previousKey,false);
            key(sustainedPanel,turnKeys[turn],true);
            sustainedUpdate.invoke(sustainedPanel);
            check(sustained.getVisualDirection()==turnDirections[turn]
                && sustained.getAnimationFrame()==1,
                "W/D/S/A direction changes retain the active walk phase");
            previousKey=turnKeys[turn];
        }
        key(sustainedPanel,previousKey,false);
        sustainedUpdate.invoke(sustainedPanel);
        check(sustained.getAnimationState()==Santa.AnimationState.IDLE,
            "Direction sequence returns to idle when released");
        sustained.setPosition(5000,5000);
        key(sustainedPanel,KeyEvent.VK_D,true);
        sustainedUpdate.invoke(sustainedPanel);
        MouseEvent movingClick=new MouseEvent(sustainedPanel,MouseEvent.MOUSE_PRESSED,
            0,0,sustained.getX()+116,sustained.getY()+21,1,false,
            MouseEvent.BUTTON1);
        for(MouseListener listener:sustainedPanel.getMouseListeners()) {
            listener.mousePressed(movingClick);
        }
        sustainedUpdate.invoke(sustainedPanel);
        for(int t=0;t<9;t++) {
            sustainedUpdate.invoke(sustainedPanel);
            if(t<8) check(sustained.getAnimationState()==Santa.AnimationState.ATTACK,
                "Mouse attack overrides held D movement");
        }
        check(!sustained.isAttacking()
            && sustained.getAnimationState()==Santa.AnimationState.WALK,
            "Actual mouse attack returns to held movement");
        int resumedFrame=sustained.getAnimationFrame();
        boolean resumedLoop=false;
        for(int t=0;t<16;t++) {
            sustainedUpdate.invoke(sustainedPanel);
            if(sustained.getAnimationFrame()!=resumedFrame) resumedLoop=true;
        }
        check(resumedLoop,"Actual GamePanel walk loop resumes after mouse attack");
        Santa speedCheck=new Santa(100,100);
        speedCheck.update(false,false,false,true,List.of());
        check(speedCheck.getX()==104,"Horizontal gameplay movement remains four pixels per update");
        s.update(false,false,false,true,List.of(new Rectangle2D.Double(132,92,20,50)));
        check(s.getX()==100 && s.getAnimationState()==Santa.AnimationState.IDLE,"Blocked movement stays still");
        int[][] vectors={{0,-100},{0,100},{-100,0},{100,0}};
        Santa.Direction[] cardinal={Santa.Direction.UP,Santa.Direction.DOWN,Santa.Direction.LEFT,Santa.Direction.RIGHT};
        for(int d=0;d<4;d++) {
            player.setPosition(1296,720);
            for(int j=0;j<24;j++) update.invoke(panel);
            MouseEvent click=new MouseEvent(panel,MouseEvent.MOUSE_PRESSED,0,0,1312+vectors[d][0]-((Camera)field(panel,"camera")).getX(),741+vectors[d][1]-((Camera)field(panel,"camera")).getY(),1,false,MouseEvent.BUTTON1);
            for(MouseListener listener:panel.getMouseListeners()) listener.mousePressed(click);
            update.invoke(panel);
            check(player.getAttackDamage()==30 && player.getAttackCooldownFrames()==23,"Combat constants unchanged");
            for(int t=0;t<9;t++) {
                check(player.getAnimationState()==Santa.AnimationState.ATTACK && player.getAnimationFrame()==t/3 && player.getVisualDirection()==cardinal[d],"Attack sequence and direction");
                update.invoke(panel);
            }
            check(player.getAnimationState()==Santa.AnimationState.IDLE,"Attack exits");
        }
        s.setAttackDirection(1,0); s.startAttack();
        check(s.getAttackBounds().getBounds2D().equals(new Rectangle2D.Double(116,106,54,30)),"Attack geometry unchanged");
        for(int i=0;i<9;i++) s.update(false,false,true,false,List.of());
        check(s.getAnimationState()==Santa.AnimationState.WALK && s.getX()==64,"Attack priority releases to moving");
        if(args.length>0) {
            BufferedImage sheet=new BufferedImage(700,460,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=sheet.createGraphics(); g.setColor(new Color(37,91,67)); g.fillRect(0,0,700,460);
            Method draw=sprites.getClass().getDeclaredMethod("draw",Graphics2D.class,Santa.AnimationState.class,Santa.Direction.class,int.class,double.class,double.class); draw.setAccessible(true);
            for(int d=0;d<4;d++) { int col=0;
                for(Santa.AnimationState state:Santa.AnimationState.values()) for(int f=0;f<=state.ordinal();f++) {
                    double ax=60+col*112,ay=80+d*110;
                    g.setColor(Color.GRAY); g.drawLine((int)ax-35,(int)ay,(int)ax+35,(int)ay);
                    draw.invoke(sprites,g,state,cardinal[d],f,ax,ay);
                    g.setColor(Color.WHITE); g.drawString(cardinal[d]+" "+state+" "+(f+1),(int)ax-46,(int)ay+20); col++;
                }
            }
            g.dispose(); ImageIO.write(sheet,"png",new File(args[0]+"/santa-animation-review.png"));
        }
        System.out.println("PASS: 24 assets, WASD/arrows, idle facing, walk timing, mouse attack sequence, movement, collision and combat geometry");
    }
}
