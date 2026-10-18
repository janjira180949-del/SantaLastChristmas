import game.*;
import java.awt.image.BufferedImage;

/** Runs in an empty directory to exercise actual failed asset decoding and native safety art. */
public class FinalChapterFallbackProbe {
    public static void main(String[]args)throws Exception{
        FinalChapterVisuals.prepareAsync().get(20,java.util.concurrent.TimeUnit.SECONDS);
        FinalChapter chapter=new FinalChapter();chapter.unlock();
        for(var pickup:FinalChapter.PICKUPS){
            var image=FinalChapterVisuals.materialImage(pickup.material());
            if(image==null||image.getWidth()!=56||image.getHeight()!=62)throw new AssertionError("Fallback material");
            chapter.collect(pickup.id());
        }
        for(var objective:FinalChapter.Restoration.values())chapter.restore(objective);
        BufferedImage frame=new BufferedImage(800,600,2);var g=frame.createGraphics();
        FinalChapterVisuals.drawHouse(g,chapter);FinalChapterVisuals.drawLights(g,chapter);FinalChapterVisuals.drawTracker(g,chapter,800,600);g.dispose();
        System.out.println("PASS: missing chapter assets safely use bounded native presentation");
    }
}
