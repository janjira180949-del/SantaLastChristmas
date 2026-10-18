import entity.Elias;
import game.GamePanel;
import game.Area;
import ui.Portrait;
import ui.DialogueManager;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/** Exact source-pixel extraction, static pose rendering and actual Town interaction. */
public final class EliasArtworkTest extends V2TestSupport {
    private static final Path CAPTURES=Path.of("build/elias/captures");
    private static final int[][] CROPS={{334,180,132,250},{481,180,125,250},
        {620,180,116,250},{755,180,118,250},{899,64,322,468}};
    public static void main(String[] args)throws Exception {
        Files.createDirectories(CAPTURES);
        verifyPreservation();verifyExports();verifyRendering();verifyTown();
        System.out.println("PASS: "+checks+" Elias extraction/rendering/interaction assertions; 17 native captures");
    }
    private static void verifyPreservation()throws Exception {
        String baseline=Files.readString(Path.of("docs/ELIAS_INTEGRATION_BASELINE.json"));
        var entries=Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(baseline);
        int javaCount=0,assetCount=0;
        while(entries.find()) {
            String path=entries.group(1);
            // Subsequent authorized menu overhaul changes these two presentation owners.
            if(path.equals("src/entity/Elias.java")||path.equals("src/ui/Portrait.java")
                ||path.equals("src/game/GamePanel.java")||path.equals("src/ui/MenuBackdrop.java")
                ||path.equals("src/game/EndingCutscene.java")||path.equals("src/game/EndingTownVisuals.java"))continue;
            byte[] bytes=AudioPhase2PresentationSupport.originalBytes(path,Files.readAllBytes(Path.of(path)));
            check(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).equals(entries.group(2)),
                "Existing unrelated source/artwork unchanged: "+path);
            if(path.endsWith(".java"))javaCount++;
            if(path.endsWith(".png"))assetCount++;
        }
        check(javaCount==51&&assetCount==104,"51 unrelated production sources and 104 original PNGs preserved");
    }
    private static void verifyExports()throws Exception {
        BufferedImage source=ImageIO.read(Path.of("reference/elias/elias_reference_sheet.png").toFile());
        String[] names={"down","up","left","right","portrait"};
        for(int i=0;i<names.length;i++) {
            String file=i==4?"portraits/elias_portrait_neutral.png":"idle/elias_idle_"+names[i]+"_01.png";
            BufferedImage exported=ImageIO.read(Path.of("assets/characters/elias/"+file).toFile());
            int[] c=CROPS[i];int retained=0,transparent=0;boolean exact=true,binary=true;
            check(exported!=null&&exported.getColorModel().hasAlpha(),"PNG decodes with alpha: "+file);
            check(exported.getWidth()==c[2]&&exported.getHeight()==c[3],"Unscaled original crop: "+file);
            for(int y=0;y<c[3];y++)for(int x=0;x<c[2];x++) {
                int rgba=exported.getRGB(x,y),alpha=rgba>>>24;
                binary&=alpha==0||alpha==255;
                if(alpha==0)transparent++;
                else {retained++;exact&=rgba==source.getRGB(c[0]+(i==2?c[2]-1-x:x),c[1]+y);}
            }
            check(exact&&binary,"Every retained RGB pixel is original sheet artwork: "+file);
            check(retained>c[2]*c[3]/4&&transparent>c[2]*c[3]/10,"Meaningful character and transparent exterior: "+file);
            check((exported.getRGB(0,0)>>>24)==0&&(exported.getRGB(c[2]-1,0)>>>24)==0,"No sheet rectangle at upper corners: "+file);
            if(i<4) {
                boolean border=true;
                for(int x=0;x<c[2];x++)border&=(exported.getRGB(x,0)>>>24)==0&&(exported.getRGB(x,c[3]-1)>>>24)==0;
                for(int y=0;y<c[3];y++)border&=(exported.getRGB(0,y)>>>24)==0&&(exported.getRGB(c[2]-1,y)>>>24)==0;
                check(border,"No labels, sheet borders or cast shadow at sprite edges: "+file);
            }
        }
    }
    private static BufferedImage pose(Elias elias,Elias.Direction direction,int cameraX,int cameraY) {
        BufferedImage image=new BufferedImage(160,160,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();
        try {
            g.translate(-cameraX,-cameraY);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            var transform=g.getTransform();var interpolation=g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            elias.drawCinematic(g,80,100,direction);
            check(g.getTransform().equals(transform)&&g.getRenderingHint(RenderingHints.KEY_INTERPOLATION)==interpolation,
                "Rendering leaves caller camera and interpolation unchanged");
        } finally {g.dispose();}
        return image;
    }
    private static Rectangle alphaBounds(BufferedImage image) {
        int l=image.getWidth(),t=image.getHeight(),r=-1,b=-1;
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if((image.getRGB(x,y)>>>24)>0) {
            l=Math.min(l,x);r=Math.max(r,x);t=Math.min(t,y);b=Math.max(b,y);
        }
        return new Rectangle(l,t,r-l+1,b-t+1);
    }
    private static boolean same(BufferedImage a,BufferedImage b) {
        return java.util.Arrays.equals(a.getRGB(0,0,a.getWidth(),a.getHeight(),null,0,a.getWidth()),
            b.getRGB(0,0,b.getWidth(),b.getHeight(),null,0,b.getWidth()));
    }
    @SuppressWarnings("unchecked")
    private static void verifyRendering()throws Exception {
        Elias elias=new Elias(100,100);GamePanel p=playing();var santa=santa(p);
        check(elias.getBounds().equals(new java.awt.geom.Rectangle2D.Double(100,100,32,48)),"Original collision rectangle retained");
        Object sprites=field(elias,"SPRITES");Map<Elias.Direction,BufferedImage> frames=(Map<Elias.Direction,BufferedImage>)field(sprites,"frames");
        check(frames.size()==4,"Exactly four cached static directional poses");
        BufferedImage contact=new BufferedImage(640,160,BufferedImage.TYPE_INT_ARGB);Graphics2D cg=contact.createGraphics();
        int i=0;
        for(Elias.Direction direction:Elias.Direction.values()) {
            BufferedImage frame=frames.get(direction);check(frame.getWidth()==64&&frame.getHeight()==64,"Consistent native canvas: "+direction);
            BufferedImage still=pose(elias,direction,0,0);Rectangle bounds=alphaBounds(still);
            check(bounds.y>=48&&bounds.y<=49&&bounds.y+bounds.height>=99&&bounds.y+bounds.height<=100,"Body rests at original feet anchor: "+direction);
            for(int ticks=0;ticks<240;ticks++)elias.updateVisual();
            check(same(still,pose(elias,direction,0,0)),"Static supplied pose has no invented animation: "+direction);
            BufferedImage camera=pose(elias,direction,17,23);boolean aligned=true;
            for(int y=23;y<160;y++)for(int x=17;x<160;x++)aligned&=still.getRGB(x,y)==camera.getRGB(x-17,y-23);
            check(aligned,"World feet remain aligned through camera translation: "+direction);
            cg.drawImage(still,i++*160,0,null);
            int dx=direction==Elias.Direction.LEFT?-40:direction==Elias.Direction.RIGHT?40:0;
            int dy=direction==Elias.Direction.UP?-40:direction==Elias.Direction.DOWN?40:0;
            santa.setPosition(100+dx,103+dy);
            check(elias.canInteract(santa)&&elias.getFacingDirection(santa)==direction,"Face adjacent Santa correctly: "+direction);
        }
        cg.dispose();ImageIO.write(contact,"png",CAPTURES.resolve("static-poses-transparent.png").toFile());
        santa.setPosition(159,103);check(!elias.canInteract(santa)&&elias.getFacingDirection(santa)==Elias.Direction.DOWN,"Original 58 pixel proximity retained");
        check(elias.getX()==100&&elias.getY()==100,"Rendering never moves NPC gameplay coordinates");
        var imageField=Portrait.class.getDeclaredField("IMAGES");imageField.setAccessible(true);
        Map<Portrait.Kind,Map<Portrait.Expression,BufferedImage>> portraits=
            (Map<Portrait.Kind,Map<Portrait.Expression,BufferedImage>>)imageField.get(null);
        Map<Portrait.Expression,BufferedImage> eliasPortraits=portraits.get(Portrait.Kind.ELIAS);
        check(eliasPortraits.size()==1&&same(eliasPortraits.get(Portrait.Expression.NEUTRAL),
            ImageIO.read(Path.of("assets/characters/elias/portraits/elias_portrait_neutral.png").toFile())),
            "Portrait loader caches actual supplied artwork, without fabricated expressions");
        BufferedImage neutral=portrait(Portrait.Expression.NEUTRAL);
        for(Portrait.Expression expression:Portrait.Expression.values())
            check(same(neutral,portrait(expression)),"Existing expression uses supplied neutral portrait: "+expression);
        ImageIO.write(neutral,"png",CAPTURES.resolve("dialogue-portrait.png").toFile());
    }
    private static BufferedImage portrait(Portrait.Expression expression) {
        BufferedImage image=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
        new Portrait(Portrait.Kind.ELIAS,expression).draw(g,8,8,112);g.dispose();return image;
    }
    private static void verifyTown()throws Exception {
        GamePanel p=playing();townFixture(p);Elias elias=(Elias)field(p,"elias");
        check(areas(p).getCurrentArea().getType()==Area.Type.CHRISTMAS_TOWN,"Elias appears in existing Christmas Town");
        for(int[] size:new int[][]{{800,600},{1270,681},{1270,720}}) {
            p.setSize(size[0],size[1]);
            for(Elias.Direction direction:Elias.Direction.values()) {
                int dx=direction==Elias.Direction.LEFT?-40:direction==Elias.Direction.RIGHT?40:0;
                int dy=direction==Elias.Direction.UP?-40:direction==Elias.Direction.DOWN?40:0;
                santa(p).setPosition(elias.getX()+dx,elias.getY()+3+dy);call(p,"updateCamera");
                ImageIO.write(render(p),"png",CAPTURES.resolve(size[0]+"x"+size[1]+"-town-"+direction.name().toLowerCase()+".png").toFile());
            }
            santa(p).setPosition(elias.getX()+40,elias.getY()+3);tap(p,KeyEvent.VK_E);
            DialogueManager d=(DialogueManager)field(p,"dialogueManager");
            check(d.isActive()&&d.isEliasDialogueActive(),"Original E prompt starts Elias dialogue");
            check(d.getActiveDialogue().getCurrentLine().getPortraitKind()==Portrait.Kind.ELIAS,"Dialogue selects new Elias portrait");
            ImageIO.write(render(p),"png",CAPTURES.resolve(size[0]+"x"+size[1]+"-town-dialogue.png").toFile());
            closeDialogue(p);check(story(p).isBossQuestAccepted(),"Existing quest accepts only after dialogue closes");
        }
        check(story(p).isLostGiftComplete()&&story(p).getCrystalsDestroyed()==3,"Gift and three-crystal prerequisites remain complete");
        check(!story(p).isTreeReached()&&!story(p).isBossDefeated(),"Talking never skips the final encounter or boss");
    }
}
