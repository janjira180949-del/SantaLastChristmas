import game.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.KeyEvent;
import java.nio.file.*;
import java.util.*;
import java.security.MessageDigest;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/** Current environment, exact gameplay/source preservation, viewport captures and render samples. */
public class TownFinalEnvironmentTest extends V2TestSupport {
    private static final Path OUTPUT=Path.of("build/town-final/after");
    private static final StringBuilder TIMINGS=new StringBuilder("[\n");
    private static String text(Path file)throws Exception{return Files.readString(file).replace("\r\n","\n");}
    private static void preserve()throws Exception {
        var matcher=Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(text(Path.of("docs/TOWN_FINAL_BASELINE.json")));
        int sources=0,pngs=0;
        while(matcher.find()) {
            String path=matcher.group(1);if(Set.of("src/game/GamePanel.java","src/ui/CinematicMenu.java","src/game/ChristmasTownVisuals.java","src/game/EndingTownVisuals.java").contains(path))continue;
            check(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(AudioPhase2PresentationSupport.originalBytes(path,Files.readAllBytes(Path.of(path))))).equals(matcher.group(2)),"Unrelated owner/artwork unchanged: "+path);
            if(path.endsWith(".java"))sources++;else pngs++;
        }
        check(sources==57&&pngs==111,"57 prior owners unchanged; CinematicMenu has authorized audio controls; all 111 prior PNGs unchanged");
        String current=AudioPresentationSupport.withoutAudio(new String(OverhaulPreservationSupport.previous("src/game/GamePanel.java",Files.readAllBytes(Path.of("src/game/GamePanel.java"))),java.nio.charset.StandardCharsets.UTF_8).replace("\r\n","\n"));
        // Only the four authorized Town rendering substitutions may differ from the entire source snapshot.
        current=current.replace("if(!town||!treeTransformation.isRestored())worldAtmosphere.drawDepth", "worldAtmosphere.drawDepth")
            .replace("if(!town)worldAtmosphere.drawLights", "worldAtmosphere.drawLights")
            .replace("if(town)townVisuals.drawSnow(graphics,treeTransformation.isRestored(),worldAtmosphere.getTicks(),camera,getWidth(),getHeight());\n        else worldAtmosphere.drawSnow", "worldAtmosphere.drawSnow")
            .replace("townVisuals.drawSnow(world,treeTransformation.isRestored(),worldAtmosphere.getTicks()+treeTransformation.getFrames(),camera,getWidth(),getHeight());", "worldAtmosphere.drawSnow(world,Area.Type.CHRISTMAS_TOWN,currentMap(),camera,getWidth(),getHeight());");
        check(current.equals(text(Path.of("docs/TOWN_FINAL_RENDER_BASELINE.txt"))),"Entire GamePanel gameplay/menu/camera/ending/input logic unchanged; only four Town render substitutions");
        String lighting=text(Path.of("src/game/EndingTownVisuals.java"))
            .replace("for(int ring=2;ring>=0;ring--)","for(int ring=7;ring>=0;ring--)")
            .replace("480*spread+ring*12","480*spread+ring*5")
            .replace("ring==0?.78:.08","ring==0?.78:.035");
        check(lighting.equals(text(Path.of("docs/TOWN_FINAL_LIGHT_BASELINE.txt"))),"Lighting differs only in feather pass count/spacing/alpha; fixed world mask and actors unchanged");
    }
    private static int warm(BufferedImage image) {
        int count=0;for(int c:image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth())) {
            int r=(c>>>16)&255,g=(c>>>8)&255,b=c&255;if(r>165&&g>95&&r>b*1.4)count++;
        }return count;
    }
    private static void assetsAndPlacement()throws Exception {
        for(String path:new String[]{TownEnvironmentLayers.PLAZA,TownEnvironmentLayers.BANNER}) {
            BufferedImage image=ImageIO.read(Path.of(path).toFile());int clear=0;
            for(int c:image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth()))if(c>>>24==0)clear++;
            check(image.getColorModel().hasAlpha()&&clear>image.getWidth()*image.getHeight()/4,"Real transparent environment layer: "+path);
            check((image.getRGB(0,0)>>>24)==0&&(image.getRGB(image.getWidth()-1,image.getHeight()-1)>>>24)==0,"Transparent image corners: "+path);
        }
        ChristmasTownVisuals visual=new ChristmasTownVisuals();visual.prepare();
        BufferedImage cold=(BufferedImage)field(visual,"cold"),restored=(BufferedImage)field(visual,"restored");
        check(cold.getWidth()==2560&&cold.getHeight()==1920&&restored.getWidth()==2560&&restored.getHeight()==1920,"Actual map dimensions preserved");
        check(warm(restored)>warm(cold)&&warm(cold)>1000,"New facades are warmly lit before and after restoration");
        TownEnvironmentLayers environment=(TownEnvironmentLayers)field(visual,"environment");
        WorldMap map=WorldMap.forArea(Area.Type.CHRISTMAS_TOWN);
        for(var detail:environment.details()) {
            if(!detail.mountedOnExistingLamp()&&!detail.name().startsWith("festival-prop"))check(map.solids().stream().anyMatch(s->s.contains(detail.bounds())),"Facade detail fits an existing blocked building footprint: "+detail.name());
            check(detail.name().startsWith("festival-prop")||map.combatRegions().stream().noneMatch(a->a.intersects(detail.bounds())),"Vertical/raised decoration does not cover authored encounter regions: "+detail.name());
            check(!detail.bounds().intersects(1296-100,0,200,500),"Forest gate/spawn/Elias approach remains clear");
        }
        Object cache=field(visual,"cold");BufferedImage image=new BufferedImage(800,600,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
        g.translate(-896,-760);var transform=g.getTransform();var composite=g.getComposite();g.setClip(896,760,800,600);var clip=g.getClip().getBounds();
        visual.drawTerrain(g,true);visual.drawGroundLights(g,true,90);visual.drawForeground(g,true,90);
        check(g.getTransform().equals(transform)&&g.getComposite().equals(composite)&&g.getClip().getBounds().equals(clip),"Environment/light passes preserve caller graphics state");g.dispose();
        check(field(visual,"cold")==cache,"Repeated render reuses static composite");
    }
    private static void capture(GamePanel panel,String name)throws Exception {
        ImageIO.write(render(panel),"png",OUTPUT.resolve(panel.getWidth()+"x"+panel.getHeight()+"-"+name+".png").toFile());
    }
    private static void benchmark(GamePanel panel,String name)throws Exception {
        BufferedImage image=new BufferedImage(panel.getWidth(),panel.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
        for(int i=0;i<20;i++)panel.paint(g);long[] samples=new long[60];
        for(int i=0;i<samples.length;i++){long t=System.nanoTime();panel.paint(g);samples[i]=System.nanoTime()-t;}
        g.dispose();Arrays.sort(samples);if(TIMINGS.length()>2)TIMINGS.append(",\n");
        TIMINGS.append(String.format(Locale.ROOT,"{\"scene\":\"%s\",\"width\":%d,\"height\":%d,\"median_ms\":%.3f,\"p95_ms\":%.3f}",name,panel.getWidth(),panel.getHeight(),samples[30]/1e6,samples[57]/1e6));
    }
    private static void scenes()throws Exception {
        for(int[] size:new int[][]{{800,600},{1270,681},{1270,720}}) {
            GamePanel panel=endingFixture();panel.setSize(size[0],size[1]);
            var transformation=(SpiritTreeTransformation)field(panel,"treeTransformation");
            capture(panel,"plaza-abandoned");benchmark(panel,"abandoned-plaza");
            tick(panel,75);capture(panel,"gold-expansion");benchmark(panel,"gold-expansion");
            tick(panel,30);capture(panel,"white-flash");tick(panel,30);capture(panel,"tree-reveal");
            tick(panel,45);check(transformation.isComplete()&&((EndingCutscene)field(panel,"endingCutscene")).isCelebration(),"Exact 180-update restoration hands off to celebration");JourneyHomeTest.completeCelebration(panel);check(panel.getState()==GamePanel.State.PLAYING,"Celebration returns control");
            tick(panel,241);capture(panel,"plaza-restored");benchmark(panel,"restored-plaza");
            tap(panel,KeyEvent.VK_E);tap(panel,KeyEvent.VK_E);capture(panel,"ending-dialogue");tap(panel,KeyEvent.VK_ESCAPE);
            WorldMap map=WorldMap.forArea(Area.Type.CHRISTMAS_TOWN);var position=map.safePosition(1750,1260,32,50);
            santa(panel).setPosition(position.x(),position.y());call(panel,"updateCamera");capture(panel,"restored-facades");benchmark(panel,"restored-facades");
            entity.Elias keeper=(entity.Elias)field(panel,"elias");at(santa(panel),new WorldMap.Point(keeper.getX()+16,keeper.getY()+21));tap(panel,KeyEvent.VK_E);
            check(((ui.DialogueManager)field(panel,"dialogueManager")).isActive(),"Elias still interacts after restoration");capture(panel,"elias-restored");closeDialogue(panel);
            FinalChapterTest.finishChapter(panel);tap(panel,KeyEvent.VK_E);tap(panel,KeyEvent.VK_E);tap(panel,KeyEvent.VK_E);check(((EndingCutscene)field(panel,"endingCutscene")).getState()==EndingCutscene.State.CREDITS,"Existing credits remain accessible");tick(panel,650);capture(panel,"credits");
            GamePanel cold=playing();townFixture(cold);cold.setSize(size[0],size[1]);santa(cold).setPosition(position.x(),position.y());
            tick(cold,241);call(cold,"updateCamera");capture(cold,"abandoned-facades");
            var toby=panel.getChildren().get(0);check(toby.getX()==1281&&toby.getY()==987,"Toby's authorized Village plaza position");
        }
        Files.writeString(Path.of("build/town-final/render-performance.json"),TIMINGS.append("\n]\n").toString());
    }
    public static void main(String[] args)throws Exception {
        Files.createDirectories(OUTPUT);preserve();assetsAndPlacement();scenes();
        System.out.println("PASS: "+checks+" environment/preservation/viewport assertions; 30 frames; reused-buffer render timings recorded");
    }
}
