import game.*;
import java.nio.file.*;
import java.util.*;
import java.awt.image.BufferedImage;

/** Write only this revision's renderer evidence; historical baselines are immutable. */
public class JourneyHomeReview extends JourneyHomeTest {
    public static void main(String[] args)throws Exception{
        SantaBaseVisuals.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS);
        var p=SantaBaseNavigationTest.opening();p.setSize(1600,1280);
        Files.writeString(Path.of("docs/JOURNEY_HOME_RENDER.properties"),"# Actual Phase 8 opening renderer; localized prior-frame guard remains active\nbase-overview="+ForestPreservationTest.pixelHash(render(p))+"\n");p.stopGame();
        p=endingFixture();tick(p,180);tick(p,100);p.setSize(1270,720);
        BufferedImage target=new BufferedImage(1270,720,BufferedImage.TYPE_INT_RGB);var g=target.createGraphics();
        for(int i=0;i<40;i++)p.paint(g);long[] times=new long[240];for(int i=0;i<times.length;i++){long start=System.nanoTime();p.paint(g);times[i]=System.nanoTime()-start;}g.dispose();Arrays.sort(times);p.stopGame();
        Files.createDirectories(Path.of("build/journey-home"));Files.writeString(Path.of("build/journey-home/render-performance.json"),String.format(Locale.ROOT,"{\"scene\":\"actual Town celebration gathering\",\"viewport\":[1270,720],\"warmups\":40,\"samples\":240,\"median_ms\":%.3f,\"p95_ms\":%.3f,\"native_fps_measured\":false}\n",times[120]/1e6,times[228]/1e6));
        System.out.println("Actual Phase 8 render hash and offscreen timing recorded; no older baseline updated.");
    }
}
