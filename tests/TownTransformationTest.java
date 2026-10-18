import game.*;
import entity.Elias;
import spirit.SpiritTree;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.security.MessageDigest;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
/** New PNG alpha/anchors, all untouched owners/assets, complete presentation and persistence. */
public class TownTransformationTest extends V2TestSupport {
    static final Path OUTPUT=Path.of("build/town-redesign/captures");
    static void preserve()throws Exception {
        var matcher=Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/TOWN_REDESIGN_BASELINE.json")));
        int sources=0,pngs=0;
        while(matcher.find()) {
            String path=matcher.group(1);if(Set.of("src/game/GamePanel.java","src/ui/CinematicMenu.java","src/game/EndingCutscene.java","src/game/EndingTownVisuals.java").contains(path))continue;
            String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(AudioPhase2PresentationSupport.originalBytes(path,Files.readAllBytes(Path.of(path)))));
            check(hash.equals(matcher.group(2)),"Existing unrelated source/asset retained: "+path);
            if(path.endsWith(".java"))sources++;if(path.endsWith(".png"))pngs++;
        }
        check(sources==55&&pngs==110,"55 unrelated production sources and 110 prior PNGs unchanged; authorized menu audio controls excluded");
    }
    static void artwork()throws Exception {
        BufferedImage art=ImageIO.read(Path.of("assets/phase10_3/tree/frame_03.png").toFile());check(art.getColorModel().hasAlpha(),"New tree is RGBA");
        int clear=0,opaque=0,partial=0;for(int c:art.getRGB(0,0,art.getWidth(),art.getHeight(),null,0,art.getWidth())){int a=c>>>24;if(a==0)clear++;else if(a>=240)opaque++;else partial++;}
        check(clear>art.getWidth()*art.getHeight()/5&&opaque>10000&&partial>50,"Real transparency, solid tree detail and soft transparent bloom");
        for(int[] corner:new int[][]{{0,0},{art.getWidth()-1,0},{0,art.getHeight()-1},{art.getWidth()-1,art.getHeight()-1}})
            check((art.getRGB(corner[0],corner[1])>>>24)<=4,"No opaque rectangular tree background");
        ChristmasTownVisuals town=new ChristmasTownVisuals();town.prepare();Object cache=field(town,"tree");
        BufferedImage body=(BufferedImage)cache;check(body.getWidth()==384&&Math.abs(body.getWidth()/(double)body.getHeight()-art.getWidth()/(double)art.getHeight())<.01,"Tree cache preserves aspect ratio and sharp sampling");
        BufferedImage image=new BufferedImage(2560,1920,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();town.drawTree(g,true);g.dispose();
        WorldMap.Point root=WorldMap.forArea(Area.Type.CHRISTMAS_TOWN).point("landmarks/Corrupted Town Center");
        int maxY=0;for(int y=0;y<image.getHeight();y++)for(int x=root.x()-192;x<root.x()+192;x++)if((image.getRGB(x,y)>>>24)>20)maxY=Math.max(maxY,y);
        check(maxY>=root.y()+60&&maxY<root.y()+65,"Restored roots align with existing baked tree feet");
        g=image.createGraphics();town.drawTree(g,false);g.dispose();check(field(town,"tree")==cache,"Tree is cached rather than prepared per frame");
    }
    static void persist()throws Exception {
        GamePanel p=endingFixture();SpiritTreeTransformation t=(SpiritTreeTransformation)field(p,"treeTransformation");
        Elias elias=(Elias)field(p,"elias");int ex=elias.getX(),ey=elias.getY();
        var toby=p.getChildren().get(0);WorldMap.Point village=WorldMap.forArea(Area.Type.VILLAGE).point("npc/Toby");
        check(toby.getX()==village.x()-15&&toby.getY()==village.y()-21,"Restoration never relocates Toby from Village");
        tick(p,180);check(t.isComplete()&&((EndingCutscene)field(p,"endingCutscene")).isCelebration(),"Exact 180-update restoration hands off to celebration");JourneyHomeTest.completeCelebration(p);check(p.getState()==GamePanel.State.PLAYING,"Celebration returns control");
        int spirit=((SpiritTree)field(p,"spiritTree")).getSpirit();tap(p,KeyEvent.VK_E);tap(p,KeyEvent.VK_ESCAPE);
        check(p.getState()==GamePanel.State.PLAYING&&((SpiritTree)field(p,"spiritTree")).getSpirit()==spirit,"Outcome replay cannot award Spirit");
        santa(p).takeDamage(100);tick(p,1);check(p.getState()==GamePanel.State.GAME_OVER,"Retry fixture uses actual defeat");
        tap(p,KeyEvent.VK_R);check(p.getState()==GamePanel.State.PLAYING&&t.isRestored(),"Restored tree survives Retry");
        travelGate(p,Area.Type.FOREST);travelGate(p,Area.Type.CHRISTMAS_TOWN);check(t.isRestored(),"Tree persists through genuine area transitions");
        check(elias.getX()==ex&&elias.getY()==ey&&toby.getX()==village.x()-15&&toby.getY()==village.y()-21,"NPC positions preserved after epilogue/travel/Retry");
        for(int[] size:new int[][]{{800,600},{1270,681},{1270,720}}) {
            p.setSize(size[0],size[1]);at(santa(p),new WorldMap.Point(1296,1120));call(p,"updateCamera");
            for(int i=0;i<180;i++)((ui.AreaTitleOverlay)field(p,"areaTitle")).update();
            ImageIO.write(render(p),"png",OUTPUT.resolve(size[0]+"x"+size[1]+"-persistent-town.png").toFile());
            Camera camera=(Camera)field(p,"camera");ChristmasTownVisuals town=(ChristmasTownVisuals)field(p,"townVisuals");BufferedImage body=(BufferedImage)field(town,"tree");
            check(1008+64-body.getHeight()-camera.getY()>=0,"Restored crown remains framed at "+size[0]+"x"+size[1]);
        }
    }
    public static void main(String[] args)throws Exception {Files.createDirectories(OUTPUT);preserve();artwork();persist();System.out.println("PASS: "+checks+" Town artwork/persistence/preservation assertions");}
}
