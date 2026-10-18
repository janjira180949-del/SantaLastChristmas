import game.*;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Record actual accepted renderer evidence, separately from assertions and old baselines. */
public class VillageIntegrationReview extends V2TestSupport {
    public static void main(String[]args)throws Exception{
        Path dir=Path.of("build/village-integration/captures");Files.createDirectories(dir);
        var village=FinalEnvironmentReview.scene(0,1296,1160,1270,720);
        ImageIO.write(village,"png",dir.resolve("10-village-localized-review.png").toFile());
        GamePanel p=SantaBaseNavigationTest.opening();p.setSize(1600,1280);var base=render(p);
        ImageIO.write(base,"png",dir.resolve("11-base-overview.png").toFile());p.stopGame();
        Files.writeString(Path.of("docs/VILLAGE_INTEGRATION_RENDER.properties"),"# Actual renderer hashes for authorized localized changes only\nvillage-after="+ForestPreservationTest.pixelHash(village)+"\nbase-overview="+ForestPreservationTest.pixelHash(base)+"\n");
        System.out.println("Recorded actual Base/Village frames without changing prior render snapshots.");
    }
}
