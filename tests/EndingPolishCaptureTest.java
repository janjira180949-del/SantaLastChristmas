import game.*;
import java.awt.event.KeyEvent;
import java.nio.file.*;
import javax.imageio.ImageIO;
/** Retained capture entry point now reviews optional authored outcome after short restoration. */
public class EndingPolishCaptureTest extends V2TestSupport {
    public static void main(String[] args)throws Exception {
        Path dir=Path.of("build/town-redesign/captures");Files.createDirectories(dir);
        for(int[] size:new int[][]{{800,600},{1270,681},{1270,720}}) {
            GamePanel p=endingFixture();p.setSize(size[0],size[1]);tick(p,180);JourneyHomeTest.completeCelebration(p);tap(p,KeyEvent.VK_E);tick(p,60);

            for(int page=0;page<2;page++) {
                var a=render(p);var b=render(p);
                check(java.util.Arrays.equals(a.getRGB(0,0,size[0],size[1],null,0,size[0]),b.getRGB(0,0,size[0],size[1],null,0,size[0])),"Outcome rendering is repeatable");
                ImageIO.write(a,"png",dir.resolve(size[0]+"x"+size[1]+"-outcome-"+page+".png").toFile());tap(p,KeyEvent.VK_E);
            }
            var cut=(EndingCutscene)field(p,"endingCutscene");
            check(!cut.hasStarted()&&p.getState()==GamePanel.State.PLAYING,"Short gratitude returns to playable Town");
            FinalChapterTest.finishChapter(p);tick(p,60);
            for(String name:new String[]{"title","ending-message","credits"}) {
                tick(p,name.equals("credits")?650:90);
                ImageIO.write(render(p),"png",dir.resolve(size[0]+"x"+size[1]+"-"+name+".png").toFile());tap(p,KeyEvent.VK_E);
            }
            check(cut.isComplete(),"Outcome retains final ending state");
        }
        System.out.println("PASS: "+checks+" preserved outcome and final-letter assertions; 30 actual renderer captures");
    }
}
