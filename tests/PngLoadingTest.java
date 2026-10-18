import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import javax.imageio.spi.*;
import javax.imageio.stream.*;
import util.ImageAssets;

/** Regression for disk-cache exhaustion, concurrent decoding and critical initialization. */
public class PngLoadingTest extends V2TestSupport {
    private static final String[] PATHS={
        "assets/world/santa_base/workshop_props.png",
        "assets/world/santa_base/workshop_materials.png",
        "assets/characters/elias/idle/elias_idle_right_01.png",
        "assets/world/town_environment/spirit_plaza.png",
        "assets/world/town_environment/festival_banner.png"};
    public static void main(String[] args)throws Exception {
        boolean cache=ImageIO.getUseCache();
        var registry=IIORegistry.getDefaultInstance();
        // A deterministic failing stream factory models unavailable temporary storage.
        var reject=new ImageInputStreamSpi("test","1",java.io.InputStream.class) {
            @Override public String getDescription(Locale locale){return "Reject implicit disk-backed stream creation";}
            @Override public ImageInputStream createInputStreamInstance(Object input,boolean useCache,java.io.File dir)throws java.io.IOException {
                throw new java.io.IOException("Simulated unavailable temporary storage");
            }
        };
        var providers=new ArrayList<ImageInputStreamSpi>();
        registry.getServiceProviders(ImageInputStreamSpi.class,true).forEachRemaining(providers::add);
        registry.registerServiceProvider(reject);
        for(var provider:providers)registry.setOrdering(ImageInputStreamSpi.class,reject,provider);
        ImageIO.setUseCache(true);
        try {
            var pool=Executors.newFixedThreadPool(4);
            try {
                var jobs=new ArrayList<Callable<Boolean>>();
                for(String path:PATHS)for(int repeat=0;repeat<8;repeat++)jobs.add(()-> {
                    BufferedImage expected;
                    var stream=new FileImageInputStream(Path.of(path).toFile());
                    expected=ImageIO.read(stream); // ImageIO owns/closes a recognized stream.
                    var actual=ImageAssets.load(path);
                    return actual!=null&&expected.getWidth()==actual.getWidth()&&expected.getHeight()==actual.getHeight()
                        &&expected.getColorModel().hasAlpha()==actual.getColorModel().hasAlpha()
                        &&Arrays.equals(expected.getRGB(0,0,expected.getWidth(),expected.getHeight(),null,0,expected.getWidth()),
                            actual.getRGB(0,0,actual.getWidth(),actual.getHeight(),null,0,actual.getWidth()));
                });
                for(var result:pool.invokeAll(jobs))check(result.get(),"Concurrent critical asset decode matches all independent pixels");
            } finally {pool.shutdownNow();}
            var elias=new entity.Elias(100,100);
            check(elias.getBounds().getWidth()==32,"Critical Elias static initialization succeeds");
            var canvas=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB);var g=canvas.createGraphics();
            try {elias.drawCinematic(g,64,100,entity.Elias.Direction.RIGHT);}finally {g.dispose();}
            check(Arrays.stream(canvas.getRGB(0,0,128,128,null,0,128)).anyMatch(p->(p>>>24)!=0),"Actual right-facing Elias renders");
            check(ImageIO.getUseCache(),"Loader leaves global cache configuration unchanged");
        } finally {registry.deregisterServiceProvider(reject);ImageIO.setUseCache(cache);}
        System.out.println("PASS: "+checks+" memory-backed/concurrent PNG loading and Elias assertions");
    }
}
