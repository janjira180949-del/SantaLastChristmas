import game.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import ui.*;
/** Pixel coverage of the current short transformation, including fixed world-space masks. */
public class EndingLightingV22Test extends V2TestSupport {
    static final Path OUTPUT=Path.of("build/town-redesign/captures");
    static BufferedImage ground(EndingTownVisuals visual,double level,WorldMap.Point tree,int cx,int cy) {
        BufferedImage image=new BufferedImage(800,600,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
        g.translate(-cx,-cy);visual.drawGroundIllumination(g,tree,level);g.dispose();return image;
    }
    static int count(BufferedImage image) {int count=0;for(int pixel:image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth()))if((pixel>>>24)>0)count++;return count;}
    public static void main(String[] args)throws Exception {
        Files.createDirectories(OUTPUT);EndingTownVisuals visual=new EndingTownVisuals();
        WorldMap.Point tree=WorldMap.forArea(Area.Type.CHRISTMAS_TOWN).point("landmarks/Corrupted Town Center");
        int early=count(ground(visual,.2,tree,tree.x()-400,tree.y()-250));
        check(count(ground(visual,.9,tree,tree.x()-400,tree.y()-250))>early,"Snow illumination expands gradually");
        for(int x=0;x<960;x++)check((visual.illuminationPixel(x,0)>>>24)==0&&(visual.illuminationPixel(x,479)>>>24)==0,"Transparent horizontal texture edges");
        for(int y=0;y<480;y++)check((visual.illuminationPixel(0,y)>>>24)==0&&(visual.illuminationPixel(959,y)>>>24)==0,"Transparent vertical texture edges");
        BufferedImage a=ground(visual,.9,tree,tree.x()-400,tree.y()-250),b=ground(visual,.9,tree,tree.x()-437,tree.y()-269);
        for(int y=0;y<581;y++)for(int x=0;x<763;x++)if(a.getRGB(x,y)!=b.getRGB(x+37,y+19))throw new AssertionError("Ground light lost its world anchor");
        check(true,"443,303 camera-overlap pixels align exactly");
        int cleared=0;for(int y=0;y<480;y++)for(int x=0;x<960;x++)if((visual.illuminationPixel(x,y)>>>24)==0&&(visual.illuminationPixel(x,y,true)>>>24)>0)cleared++;
        check(cleared>2000,"Restored terrain illumination removes the old baked tree silhouette");
        BufferedImage screen=new BufferedImage(800,600,BufferedImage.TYPE_INT_ARGB);Graphics2D g=screen.createGraphics();
        g.setColor(new Color(234,208,151));g.fillRect(0,0,800,600);int before=screen.getRGB(400,250);
        Dialogue line=new Dialogue(java.util.List.of(new DialogueLine("Santa",Portrait.Kind.SANTA,Portrait.Expression.NEUTRAL,"For tonight.")));
        new DialogueBox().drawCinematic(g,line,800,600);g.dispose();
        check(screen.getRGB(400,250)==before,"Cinematic dialogue cannot dim tree light outside its panel");
        g=screen.createGraphics();var transform=g.getTransform();var composite=g.getComposite();g.setClip(40,40,700,500);var clip=g.getClip().getBounds();
        ChristmasTownVisuals town=new ChristmasTownVisuals();SpiritTreeTransformation transformation=new SpiritTreeTransformation();transformation.start();
        spirit.SpiritTree spirit=new spirit.SpiritTree();for(int i=0;i<150;i++)transformation.update(spirit);
        visual.drawGroundIllumination(g,tree,.9);town.drawTree(g,true);transformation.drawMagic(g,tree);transformation.drawFlash(g,800,600);
        check(g.getTransform().equals(transform)&&g.getComposite().equals(composite)&&g.getClip().getBounds().equals(clip),"Lighting preserves caller transform/composite/clip");g.dispose();
        for(int[] size:new int[][]{{800,600},{1270,681},{1270,720}}) {
            GamePanel p=endingFixture();p.setSize(size[0],size[1]);transformation=(SpiritTreeTransformation)field(p,"treeTransformation");
            for(int frame:new int[]{0,29,45,60,89,105,120,135,150,179,180}) {
                tick(p,frame-transformation.getFrames());BufferedImage image=render(p);
                ImageIO.write(image,"png",OUTPUT.resolve(size[0]+"x"+size[1]+"-restoration-"+frame+".png").toFile());
                check(java.util.Arrays.equals(image.getRGB(0,0,size[0],size[1],null,0,size[0]),render(p).getRGB(0,0,size[0],size[1],null,0,size[0])),"Render cannot advance transformation");
                if(frame==105)check(image.getRGB(0,0)!=Color.WHITE.getRGB()&&p.isGameplayHudVisible(),"No obsolete fullscreen flash; actual-world HUD remains visible");
                if(frame==180) {
                    check(((spirit.SpiritTree)field(p,"spiritTree")).getSpirit()==100&&((EndingCutscene)field(p,"endingCutscene")).isCelebration(),"Exact reward and celebration handoff at frame 180");
                    Camera camera=(Camera)field(p,"camera");int tx=tree.x()-camera.getX(),ty=tree.y()+60-camera.getY(),gold=0;
                    for(int y=ty-15;y<ty+20;y++)for(int x=tx-55;x<tx+55;x++) {
                        int rgb=image.getRGB(x,y),r=(rgb>>>16)&255,green=(rgb>>>8)&255,blue=rgb&255;
                        if(r>140&&green>115&&r>blue+20)gold++;
                    }
                    check(gold>100,"Ground illumination survives terrain/sprite/HUD composition");
                }
            }
        }
        System.out.println("PASS: "+checks+" lighting assertions; 33 native transformation frames at "+OUTPUT);
    }
}
