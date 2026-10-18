import game.*;
import entity.*;
import java.awt.event.KeyEvent;
import java.util.List;

public class DeveloperTestModeTest extends V2TestSupport {
    public static void main(String[] args)throws Exception {
        GamePanel p=playing();Santa s=santa(p);
        check(!s.isDeveloperTestMode(),"Developer mode defaults off");
        tap(p,KeyEvent.VK_F1);check(s.isDeveloperTestMode(),"F1 toggles mode");
        check(!s.takeDamage(100)&&s.getHealth()==100,"Developer invulnerability");
        var press=new java.awt.event.KeyEvent(p,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_F1,KeyEvent.CHAR_UNDEFINED);
        p.keyPressed(press);tick(p,1);for(int i=0;i<50;i++){p.keyPressed(press);tick(p,1);}
        check(!s.isDeveloperTestMode(),"Held F1 toggles only once");p.keyReleased(press);
        s.takeDamage(8);check(s.getHealth()==92,"Disabling restores normal damage");
        int x=s.getX();s.update(false,false,false,true,List.of());check(s.getX()==x+4,"Movement retained");
        render(p);System.out.println("PASS: "+checks+" developer-mode assertions");
    }
}
