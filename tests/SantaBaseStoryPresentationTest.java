import game.*;
import ui.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

/** Real UI captures, small viewports, bounded response and pause/departure lifecycle. */
public class SantaBaseStoryPresentationTest extends V2TestSupport {
    static final Path OUT=Path.of("build/santa-base-story/captures");
    static void save(GamePanel p,String name)throws Exception {ImageIO.write(render(p),"png",OUT.resolve(name+".png").toFile());}
    public static void main(String[]args)throws Exception {
        Files.createDirectories(OUT);SantaBaseVisuals.prepareAsync().get(30,TimeUnit.SECONDS);
        GamePanel p=new GamePanel();p.setSize(1270,720);tap(p,KeyEvent.VK_ENTER);tick(p,35);save(p,"00-workshop-introduction");
        check(((StoryOverlay)field(p,"storyOverlay")).isActive()&&call(p,"nearbyWorkshopSite")==null,"Single existing opening hides interaction prompts");
        tap(p,KeyEvent.VK_E);check(!((StoryOverlay)field(p,"storyOverlay")).isActive(),"One E skips the only opening card");
        var sites=WorkshopInteractions.Site.values();
        String[] names={"letter","lantern","journey-map","gift-storage"};
        for(int i=0;i<sites.length;i++) {
            if(sites[i]==WorkshopInteractions.Site.GIFT_STORAGE)continue; // Dedicated collection captures cover the revised storage UI.
            SantaBaseStoryInteractionTest.approach(p,sites[i]);save(p,(i*2+1)+"-"+names[i]+"-prompt");
            tap(p,KeyEvent.VK_E);tick(p,15);save(p,(i*2+2)+"-"+names[i]+"-dialogue");
            check(!p.isGameplayHudVisible()&&call(p,"nearbyWorkshopSite")==null,"Dialogue hides HUD and proximity prompt");
            closeDialogue(p);
        }
        // Readable small-screen letter and five-line route, using the same DialogueBox.
        for(int[] size:new int[][]{{640,480},{800,600},{1270,720},{1600,1000}}) {
            p.setSize(size[0],size[1]);
            for(var site:sites) {
                if(site==WorkshopInteractions.Site.GIFT_STORAGE)continue;
                SantaBaseStoryInteractionTest.approach(p,site);render(p);
                var im=new BufferedImage(size[0],size[1],2);var g=im.createGraphics();var camera=(Camera)field(p,"camera");
                var bounds=WorkshopInteractions.promptBounds(g,site,camera,size[0],size[1]);g.dispose();
                check(bounds.x>=0&&bounds.y>=114&&bounds.getMaxX()<=size[0]&&bounds.getMaxY()<=size[1]-200,"Prompt stays inside viewport and above HUD: "+site);
                check(!bounds.intersects(new Rectangle2D.Double(0,size[1]-180,280,180)),"Prompt does not overlap quest HUD");
                tap(p,KeyEvent.VK_E);var dialogue=SantaBaseStoryInteractionTest.dialogues(p).getActiveDialogue();render(p);
                DialogueBox box=(DialogueBox)field(p,"dialogueBox");
                var lines=(java.util.List<?>)field(box,"cachedText");int height=(int)field(box,"lastBoxHeight");
                check(lines.size()>=1&&75+(lines.size()-1)*23<height-35,"Wrapped text clears dialogue footer at every viewport");
                check(height<size[1]-40,"Dialogue stays vertically on screen");
                if(size[0]==640&&(site==WorkshopInteractions.Site.LETTER_DESK||site==WorkshopInteractions.Site.JOURNEY_MAP))save(p,"small-"+site.name().toLowerCase());
                if(site==WorkshopInteractions.Site.JOURNEY_MAP)check(lines.size()==5,"Journey overview keeps explicit three destinations and two arrows");
                closeDialogue(p);
            }
        }
        p.setSize(1270,720);SantaBaseStoryInteractionTest.approach(p,WorkshopInteractions.Site.LETTER_DESK);
        tap(p,KeyEvent.VK_ESCAPE);check(p.getState()==GamePanel.State.PAUSED&&call(p,"nearbyWorkshopSite")==null,"Pause hides prompts");
        tap(p,KeyEvent.VK_E);check(!SantaBaseStoryInteractionTest.dialogues(p).isActive(),"Paused E cannot start narrative");
        tap(p,KeyEvent.VK_ENTER);check(p.getState()==GamePanel.State.PLAYING&&call(p,"nearbyWorkshopSite")==WorkshopInteractions.Site.LETTER_DESK,"Resume restores nearby prompt");
        var workshop=(WorkshopInteractions)field(p,"workshopInteractions");
        var before=new BufferedImage(1600,1280,2);var glow=before.createGraphics();workshop.respond(WorkshopInteractions.Site.SPIRIT_LANTERN,100);workshop.drawResponse(glow,145);glow.dispose();
        check(before.getRGB(976,432)!=0,"Lantern response draws a subtle warm pulse");
        var expired=new BufferedImage(1600,1280,2);glow=expired.createGraphics();workshop.drawResponse(glow,190);glow.dispose();
        check(expired.getRGB(976,432)==0,"Lantern response expires without timers or emissions");
        workshop.respond(WorkshopInteractions.Site.SPIRIT_LANTERN,100);workshop.reset();glow=expired.createGraphics();workshop.drawResponse(glow,145);glow.dispose();check(expired.getRGB(976,432)==0,"Reset clears interrupted response");
        workshop.respond(WorkshopInteractions.Site.SPIRIT_LANTERN,359990);workshop.updateResponse(35);
        var wrapped=new BufferedImage(1600,1280,2);glow=wrapped.createGraphics();workshop.drawResponse(glow,35);glow.dispose();
        check(wrapped.getRGB(976,432)!=0,"One response continues smoothly across actual world-clock wrap");
        workshop.updateResponse(80);wrapped=new BufferedImage(1600,1280,2);glow=wrapped.createGraphics();workshop.drawResponse(glow,35);glow.dispose();
        check(wrapped.getRGB(976,432)==0,"Expired response cannot restart on a later clock cycle");
        workshop.respond(WorkshopInteractions.Site.SPIRIT_LANTERN,100);workshop.updateResponse(190);
        glow=wrapped.createGraphics();workshop.drawResponse(glow,145);glow.dispose();check(wrapped.getRGB(976,432)==0,"Expiry clears state even when no paint occurred");
        santa(p).setPosition(800,980);tick(p,1);save(p,"09-departure-moment");
        check(areas(p).getCurrentArea().getType()==Area.Type.SANTA_BASE&&!SantaBaseStoryInteractionTest.dialogues(p).isActive()&&p.isGameplayHudVisible(),"Departure line is nonblocking");
        check(((String)field(p,"areaMessage")).equals("Don't forget the presents. Gift Storage is in the north-east workshop."),"Brief departure message appears");
        int frames=(int)field(p,"areaMessageFrames");tick(p,5);check((int)field(p,"areaMessageFrames")==frames-5,"Departure line does not restart every frame");
        travelGate(p,Area.Type.VILLAGE);check(((String)field(p,"areaMessage")).equals("The letter was right...\nThe Christmas lights are fading.\nI should check on everyone.")&&call(p,"nearbyWorkshopSite")==null,"Departure clears Base message/prompts on Village arrival");
        p.beginAdventure();closeOverlay(p);santa(p).setPosition(800,980);tick(p,1);check(field(p,"areaMessage")!=null,"Fresh adventure can show departure line again");p.stopGame();
        System.out.println("PASS: "+checks+" workshop UI/layout/effect/pause/departure assertions; actual renderer captures saved");
    }
}
