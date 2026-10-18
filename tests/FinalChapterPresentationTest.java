import game.*;
import java.util.concurrent.*;
/** Phase 8 preserves material rendering/cache checks while replacing the post-climax capture route. */
public class FinalChapterPresentationTest extends FinalChapterTest {
    public static void main(String[]args)throws Exception{
        JourneyHomePresentationTest.main(args);
        var ready=java.util.Arrays.stream(FinalChapterVisuals.class.getDeclaredMethods()).filter(m->m.getName().equals("ready")).findFirst().orElseThrow();ready.setAccessible(true);
        CompletableFuture<Object> pending=new CompletableFuture<>(),failed=new CompletableFuture<>(),canceled=new CompletableFuture<>();failed.completeExceptionally(new java.io.IOException("fixture"));canceled.cancel(false);
        for(var future:java.util.List.of(pending,failed,canceled)){long start=System.nanoTime();check(ready.invoke(null,future)==null&&System.nanoTime()-start<500_000_000L,"Pending/failed/canceled original chapter cache read stays non-blocking");}
        check(FinalChapterVisuals.materialImage(FinalChapter.Material.ORNAMENT)==FinalChapterVisuals.materialImage(FinalChapter.Material.ORNAMENT),"Original material artwork remains shared");
        System.out.println("PASS: "+checks+" retained chapter cache and redesigned actual-render/input assertions");
    }
}
