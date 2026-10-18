import game.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;

/** Offscreen, fixed-camera measurements; also compiles against the immutable prior classes. */
public class VillageStoryPerformance extends V2TestSupport {
    public static void main(String[] args)throws Exception {
        long started=System.nanoTime();var p=playing();double fixture=(System.nanoTime()-started)/1e6;
        p.setSize(1270,720);santa(p).setPosition(1296,1160);call(p,"updateCamera");
        var message=GamePanel.class.getDeclaredField("areaMessage");message.setAccessible(true);message.set(p,null);
        if(args.length>1&&args[1].equals("restored")){var a=(VillageActivities)field(p,"villageActivities");a.chapter.unlock();for(var pickup:FinalChapter.PICKUPS)a.chapter.collect(pickup.id());for(var r:FinalChapter.Restoration.values())a.chapter.restore(r);}
        var image=new BufferedImage(1270,720,BufferedImage.TYPE_INT_ARGB);
        for(int i=0;i<40;i++){var g=image.createGraphics();p.paint(g);g.dispose();}
        double[] samples=new double[240];
        for(int i=0;i<samples.length;i++){var g=image.createGraphics();long t=System.nanoTime();p.paint(g);samples[i]=(System.nanoTime()-t)/1e6;g.dispose();}
        Arrays.sort(samples);String result=String.format(Locale.ROOT,"{\"fixture_ms\": %.3f, \"median_ms\": %.3f, \"p95_ms\": %.3f, \"paints\": 240, \"warmups\": 40, \"viewport\": [1270,720], \"actor\": [1296,1160], \"world_frame\": 2}%n",fixture,samples[120],samples[228]);
        Files.writeString(Path.of(args[0]),result);System.out.print(result);p.stopGame();
    }
}
