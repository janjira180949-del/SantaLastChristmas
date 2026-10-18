import game.*;
import java.awt.image.BufferedImage;

/** Child process with no assets: actual missing-file preparation must remain safe. */
public class JourneyHomeFallbackProbe {
    public static void main(String[] args)throws Exception{
        if(JourneyHomeVisuals.prepareAsync().get(15,java.util.concurrent.TimeUnit.SECONDS)!=null)throw new AssertionError("Unexpected art in empty cwd");
        var image=new BufferedImage(2560,1920,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        var p=new FinalChapter();p.unlock();for(var item:FinalChapter.PICKUPS)p.collect(item.id());for(var r:FinalChapter.Restoration.values())p.restore(r);
        p.beginCelebration();p.completeCelebration();p.returnHome();
        JourneyHomeVisuals.drawCitizens(g,240);JourneyHomeVisuals.drawSleigh(g,new entity.Santa(0,0),45);JourneyHomeVisuals.drawLetterDesk(g,p);g.dispose();
        if(JourneyHomeVisuals.sleighImage()==null||image.getRGB(424,706)==0)throw new AssertionError("Missing-art safety rendering failed");
        System.out.println("PASS: missing homecoming artwork uses bounded native safety art without blocking");
    }
}
