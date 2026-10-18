import game.*;
import entity.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Prepared landmark art plus real healing/attack state; no new gameplay fixtures or rewards. */
public class ForestLandmarkVisualTest extends V2TestSupport {
    static BufferedImage core(BlackFrostCrystal c) {
        var image=new BufferedImage(400,260,2);var g=image.createGraphics();
        g.translate(200-c.getPosition().x(),160-c.getPosition().y());c.drawGround(g);g.dispose();return image;
    }
    static BufferedImage shrine(HealingPoint p) {
        var image=new BufferedImage(300,240,2);var g=image.createGraphics();
        g.translate(150-p.getPosition().x(),160-p.getPosition().y());p.draw(g);g.dispose();return image;
    }
    static int visible(BufferedImage image,int top,int bottom) {
        int count=0;for(int y=top;y<bottom;y++)for(int x=0;x<image.getWidth();x++)if((image.getRGB(x,y)>>>24)>0)count++;return count;
    }
    static String pixels(BufferedImage image)throws Exception{return ForestPreservationTest.pixelHash(image);}
    static void transparency(BufferedImage image,String name) {
        check(image!=null,name+" loaded");check(visible(image,0,image.getHeight())>120,name+" has detailed artwork");
        for(int x=0;x<image.getWidth();x++){check(image.getRGB(x,0)>>>24==0,name+" clear upper edge");check(image.getRGB(x,image.getHeight()-1)>>>24==0,name+" clear lower edge");}
        for(int y=0;y<image.getHeight();y++){check(image.getRGB(0,y)>>>24==0,name+" clear left edge");check(image.getRGB(image.getWidth()-1,y)>>>24==0,name+" clear right edge");}
    }
    static void missing()throws Exception {
        var prepare=ForestLandmarkArt.class.getDeclaredMethod("prepareNow");prepare.setAccessible(true);prepare.invoke(null);
        check(ForestLandmarkArt.core("crystal-1",false)==null&&ForestLandmarkArt.shrine(false)==null&&ForestLandmarkArt.tent(0)==null,"Missing optional files return no replacement sprites");
        var p=new HealingPoint(Area.Type.FOREST,720,1168);check(visible(shrine(p),0,240)>100,"Missing shrine uses cached native safety artwork");
        var e=new EncounterManager().get("crystal-1");check(visible(core(new BlackFrostCrystal(e)),0,260)>100,"Missing core remains visible and attackable");
        System.out.println("PASS: missing-file safety presentation");
    }
    public static void main(String[] args)throws Exception {
        if(args.length>0){missing();return;}
        FrozenForestVisuals.prepareAsync().get(30,TimeUnit.SECONDS);
        check(ForestLandmarkArt.isPrepared(),"Landmarks prepare in the existing Forest worker");
        var identities=new HashSet<String>();
        for(int i=0;i<3;i++){
            String id="crystal-"+(i+1);var image=ForestLandmarkArt.core(id,false);transparency(image,id);
            check(identities.add(pixels(image)),"Three objective silhouettes differ");
            check(!pixels(image).equals(pixels(ForestLandmarkArt.core(id,true))),"Exposed energy has a distinct warm cue");
        }
        for(int i=0;i<2;i++){var image=ForestLandmarkArt.tent(i);transparency(image,"tent "+i);check(image.getWidth()==150&&image.getHeight()==(i==0?124:65),"Original tent envelope retained");}
        transparency(ForestLandmarkArt.shrine(false),"Christmas shrine");
        check(!pixels(ForestLandmarkArt.shrine(false)).equals(pixels(ForestLandmarkArt.shrine(true))),"Spent shrine has dim artwork");
        check(ForestLandmarkArt.lamp()!=null,"Original lamp source reused");
        double cost=ForestLandmarkArt.preparationMillis();var same=ForestLandmarkArt.core("crystal-1",false);
        var prepare=ForestLandmarkArt.class.getDeclaredMethod("prepareNow");prepare.setAccessible(true);prepare.invoke(null);
        check(same==ForestLandmarkArt.core("crystal-1",false)&&cost==ForestLandmarkArt.preparationMillis(),"Repeated preparation reuses immutable art");

        GamePanel panel=playing();investigationFixture(panel);
        @SuppressWarnings("unchecked") var crystals=(java.util.List<BlackFrostCrystal>)field(panel,"crystals");
        int beforeSpirit=((spirit.SpiritTree)field(panel,"spiritTree")).getSpirit();
        for(var c:crystals){
            var pos=c.getPosition();check(c.getBounds().equals(new java.awt.geom.Rectangle2D.Double(pos.x()-18,pos.y()-30,36,60)),"Exact original attack bounds "+c.getId());
            check(!util.Collision.intersectsAny(new Rectangle(pos.x()-48,pos.y()-75,96,125),WorldMap.forArea(Area.Type.FOREST).solids()),"Crystal combat approach stays clear");
            at(santa(panel),pos);tick(panel,1);check(c.getState()==BlackFrostCrystal.State.GUARDED,"Original defenders protect the core");
            clearWave(panel);check(c.getState()==BlackFrostCrystal.State.VULNERABLE,"Every original wave exposes core");
            check(!c.tryBreak(santa(panel)),"Final killing swing cannot also break exposed core");
            var live=core(c);at(santa(panel),new WorldMap.Point(pos.x(),pos.y()+48));attack(panel,pos.x(),pos.y());tick(panel,1);
            check(c.getState()==BlackFrostCrystal.State.BROKEN,"A new real basic attack breaks core");
            check(!c.tryBreak(santa(panel))&&!story(panel).recordCrystalDestroyed(c.getId()),"Duplicate attack/reward suppressed");
            check(!pixels(live).equals(pixels(core(c))),"Broken art differs immediately");
            tick(panel,36);var dead=core(c);check(visible(dead,0,166)==0,"No intact upper crystal or energy remains after the existing burst");
            check(visible(dead,166,260)>0,"Spent base is retained as a dim scar");
            String settled=pixels(dead);tick(panel,70);check(settled.equals(pixels(core(c))),"Destroyed body stays inactive without pulsing energy");
        }
        check(((spirit.SpiritTree)field(panel,"spiritTree")).getSpirit()==beforeSpirit+45,"Exactly the original three +15 crystal rewards");
        panel.stopGame();

        for(var pos:java.util.List.of(new WorldMap.Point(720,1168),new WorldMap.Point(1664,1280))){
            var point=new HealingPoint(Area.Type.FOREST,pos.x(),pos.y());var santa=new Santa(0,0);
            at(santa,new WorldMap.Point(pos.x()+58,pos.y()));check(point.isNearby(santa),"Inclusive original 58-pixel healing range");
            at(santa,new WorldMap.Point(pos.x()+59,pos.y()));check(!point.isNearby(santa)&&!point.use(santa,false),"Artwork does not enlarge range");
            at(santa,pos);check(!point.use(santa,false)&&!point.isUsed(),"Full HP does not consume shrine");
            santa.takeDamage(50);check(!point.use(santa,true)&&!point.isUsed(),"Combat cannot consume shrine");
            String ready=pixels(shrine(point));check(point.use(santa,false)&&santa.getHealth()==90&&point.isUsed(),"Both unchanged points heal exactly 40 HP once");
            check(!point.use(santa,false)&&santa.getHealth()==90,"Repeated E cannot heal again");
            check(!ready.equals(pixels(shrine(point))),"Successful use visibly dims shrine");
            String spent=pixels(shrine(point));for(int t=0;t<137;t++)point.update();check(spent.equals(pixels(shrine(point))),"Used shrine has no active pulse or particles");
            var clamp=new HealingPoint(Area.Type.FOREST,pos.x(),pos.y());var other=new Santa(0,0);at(other,pos);other.takeDamage(10);
            check(clamp.use(other,false)&&other.getHealth()==100,"Healing clamps at unchanged maximum");
            var defeated=new Santa(0,0);at(defeated,pos);defeated.takeDamage(100);check(!new HealingPoint(Area.Type.FOREST,pos.x(),pos.y()).use(defeated,false),"Defeated Santa cannot use shrine");
        }
        var town=new HealingPoint(Area.Type.CHRISTMAS_TOWN,1360,880);
        check(pixels(shrine(town)).equals(pixels(Phase931PreservationSupport.fixture("town-healing"))),"Town healing artwork remains pixel-identical");
        var pending=ForestLandmarkArt.class.getDeclaredField("prepared");pending.setAccessible(true);Object art=pending.get(null);
        try{pending.set(null,null);long start=System.nanoTime();check(visible(shrine(new HealingPoint(Area.Type.FOREST,720,1168)),0,240)>0,"Pending shrine returns native fallback");check((System.nanoTime()-start)<1_000_000_000L,"Pending art does not wait on paint");}finally{pending.set(null,art);}
        Path empty=Files.createTempDirectory("santa-forest-missing-");
        String executable=Path.of(System.getProperty("java.home"),"bin","java.exe").toString();
        String classpath=Path.of(ForestLandmarkVisualTest.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        var process=new ProcessBuilder(executable,"-Djava.awt.headless=true","-cp",classpath,"ForestLandmarkVisualTest","missing").directory(empty.toFile()).redirectErrorStream(true).start();
        check(process.waitFor(20,TimeUnit.SECONDS),"Missing-art child exits promptly");String output=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        check(process.exitValue()==0,"Real missing-file fallback process: "+output);Files.delete(empty);
        check(FrozenForestVisuals.buildCount()==1,"All landmark states share one Forest floor");
        System.out.println("PASS: "+checks+" landmark artwork, crystal combat, healing and fallback assertions");
    }
}
