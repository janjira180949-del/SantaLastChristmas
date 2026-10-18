import game.Game;
import game.GamePanel;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Launches the actual Game entry point and checks the running menu/loop. */
public class LaunchSmokeTest extends V2TestSupport {
    public static void main(String[] args)throws Exception {
        verifyMenuExit();
        Game.main(new String[0]);
        JFrame[] window={null};GamePanel[] panel={null};
        for(int attempt=0;attempt<100&&window[0]==null;attempt++) {
            SwingUtilities.invokeAndWait(()-> {
                for(Frame frame:Frame.getFrames())if(frame instanceof JFrame f&&f.getTitle().equals("Santa: Last Christmas")&&f.isShowing()) {
                    window[0]=f;panel[0]=(GamePanel)f.getContentPane().getComponent(0);
                }
            });Thread.sleep(50);
        }
        try {
            check(window[0]!=null&&window[0].isDisplayable(),"Actual JFrame opens");
            check(panel[0].getState()==GamePanel.State.MAIN_MENU,"Launch opens main menu");
            SwingUtilities.invokeAndWait(()->{window[0].toFront();panel[0].requestFocusInWindow();});
            for(int i=0;i<100&&!panel[0].isFocusOwner();i++)Thread.sleep(25);
            check(panel[0].isFocusOwner(),"Native keyboard fixture waits for panel focus");
            SwingUtilities.invokeAndWait(()-> {
                // A real OS hover may select another menu item when the window appears.
                // Select START through the existing mouse listener before testing Enter.
                var start=ui.CinematicMenu.button(panel[0].getWidth(),panel[0].getHeight(),0);
                for(var listener:panel[0].getMouseMotionListeners())listener.mouseMoved(new java.awt.event.MouseEvent(panel[0],java.awt.event.MouseEvent.MOUSE_MOVED,0,0,(int)start.getCenterX(),(int)start.getCenterY(),0,false));
                panel[0].keyPressed(new KeyEvent(panel[0],KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_ENTER,'\n'));
                panel[0].keyReleased(new KeyEvent(panel[0],KeyEvent.KEY_RELEASED,0,0,KeyEvent.VK_ENTER,'\n'));
            });
            for(int i=0;i<100&&panel[0].getState()==GamePanel.State.MAIN_MENU;i++)Thread.sleep(25);
            check(panel[0].getState()==GamePanel.State.PLAYING,"Running game loop handles Start");
            check(areas(panel[0]).getCurrentArea().getType()==game.Area.Type.SANTA_BASE,"Native production Start opens Santa's Base");
            check(((Thread)field(panel[0],"gameThread")).isAlive(),"Existing 60Hz loop runs");
            System.out.println("PASS: real Game launch, menu Start, running loop and native menu Exit window closure");
        } finally {
            if(panel[0]!=null)panel[0].stopGame();
            if(window[0]!=null)SwingUtilities.invokeAndWait(()->window[0].dispose());
        }
    }
    private static void verifyMenuExit()throws Exception {
        JFrame[] frame={null};GamePanel[] p={null};java.util.concurrent.atomic.AtomicInteger closes=new java.util.concurrent.atomic.AtomicInteger();
        SwingUtilities.invokeAndWait(()-> {
            frame[0]=new JFrame("Menu exit smoke");frame[0].setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            p[0]=new GamePanel();frame[0].add(p[0]);frame[0].setSize(800,600);frame[0].setVisible(true);
            frame[0].addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosing(java.awt.event.WindowEvent event) {closes.incrementAndGet();}
            });p[0].startGame();
        });
        try {
            SwingUtilities.invokeAndWait(()-> {
                var button=ui.CinematicMenu.button(p[0].getWidth(),p[0].getHeight(),4);
                for(var listener:p[0].getMouseListeners()) {
                    listener.mousePressed(new java.awt.event.MouseEvent(p[0],java.awt.event.MouseEvent.MOUSE_PRESSED,0,0,
                        (int)button.getCenterX(),(int)button.getCenterY(),1,false,java.awt.event.MouseEvent.BUTTON1));
                    listener.mouseReleased(new java.awt.event.MouseEvent(p[0],java.awt.event.MouseEvent.MOUSE_RELEASED,0,0,
                        (int)button.getCenterX(),(int)button.getCenterY(),1,false,java.awt.event.MouseEvent.BUTTON1));
                }
            });
            for(int i=0;i<100&&frame[0].isDisplayable();i++)Thread.sleep(25);
            check(!frame[0].isDisplayable()&&closes.get()==1,"Actual EXIT dispatches exactly one native window-close event");
            check(!(Boolean)field(p[0],"running"),"Window EXIT stops actual loop");
        } finally {p[0].stopGame();SwingUtilities.invokeAndWait(()->frame[0].dispose());}
    }
}
