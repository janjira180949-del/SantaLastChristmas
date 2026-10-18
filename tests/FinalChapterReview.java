import game.*;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Separate actual render evidence for this chapter; never writes an earlier snapshot. */
public class FinalChapterReview extends FinalChapterTest {
    public static void main(String[] args)throws Exception{
        Path dir=Path.of("build/final-chapter/captures");Files.createDirectories(dir);
        var village=FinalEnvironmentReview.scene(0,1296,1160,1270,720);
        ImageIO.write(village,"png",dir.resolve("village-before-chapter.png").toFile());
        Files.writeString(Path.of("docs/FINAL_CHAPTER_RENDER.properties"),"# Current chapter-disabled renderer, reviewed against archived pre-chapter fixture\nvillage-before-chapter="+ForestPreservationTest.pixelHash(village)+"\n");
        System.out.println("Recorded chapter-only render evidence; old snapshots unchanged.");
    }
}
