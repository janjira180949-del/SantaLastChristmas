import game.*;
import entity.Santa;
import ui.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.Arrays;
import javax.imageio.ImageIO;

/** Deterministic animation timing, real cooldown presentation, shelter rules and captures. */
public class UIPolishTest extends V2TestSupport {
    static final Path CAPTURES=Path.of("build/v2.1/ui");
    static BufferedImage abilityImage(AbilityHUD hud,Santa santa,AbilitySystem abilities) {
        BufferedImage image=new BufferedImage(800,600,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();hud.draw(g,800,600,santa,abilities);g.dispose();return image;
    }
    static void save(BufferedImage image,String name)throws Exception { ImageIO.write(image,"png",CAPTURES.resolve(name+".png").toFile()); }
    public static void main(String[] args)throws Exception {
        Files.createDirectories(CAPTURES);AreaTitleOverlay title=new AreaTitleOverlay();
        check(!title.isActive()&&title.getOpacity()==0,"Area intro initially inactive");
        for(Area.Type area:Area.Type.values()) {
            title.show(area);check(title.isActive()&&title.getOpacity()==0,"Entrance begins transparent");
            for(int i=0;i<15;i++)title.update();check(title.getOpacity()==.5f,"Half fade-in at 15 frames");
            for(int i=15;i<30;i++)title.update();check(title.getOpacity()==1,"Fade-in complete at half second");
            check(!title.getTitle().isEmpty()&&!title.getSubtitle().isEmpty(),"Every area has title and subtitle");
            BufferedImage image=new BufferedImage(800,220,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();title.draw(g,800);g.dispose();save(image,"title-"+area);
            for(int i=30;i<135;i++)title.update();check(title.getOpacity()==1,"Hold through 2.25 seconds");
            for(int i=135;i<165;i++)title.update();check(Math.abs(title.getOpacity()-1/3f)<.001,"Fade-out during last 45 frames");
            for(int i=165;i<180;i++)title.update();check(!title.isActive()&&title.getOpacity()==0,"Gone at three seconds");
            image=new BufferedImage(800,220,BufferedImage.TYPE_INT_ARGB);g=image.createGraphics();title.draw(g,800);g.dispose();
            check(Arrays.stream(image.getRGB(0,0,800,220,null,0,800)).allMatch(v->v==0),"Expired title draws no pixels");
        }
        Santa santa=new Santa(100,100);AbilitySystem abilities=new AbilitySystem();AbilityHUD hud=new AbilityHUD();
        var slots=hud.getSlots(santa,abilities);check(slots.stream().allMatch(s->s.ready()&&s.status().equals("READY")&&s.progress()==1),"All three abilities ready initially");
        check(slots.get(0).input().equals("LMB")&&slots.get(1).input().equals("SPACE")&&slots.get(2).input().equals("1"),"Input hints match actual bindings");
        BufferedImage ready=abilityImage(hud,santa,abilities);save(ready,"abilities-ready");
        check(santa.startAttack()&&santa.startDash(false,false,false,true)&&abilities.use(1,santa,java.util.List.of(),java.util.List.of()),"Real combat activates existing cooldowns");
        slots=hud.getSlots(santa,abilities);
        check(slots.stream().limit(3).noneMatch(AbilityHUD.Slot::ready)&&slots.stream().limit(3).allMatch(s->s.progress()==0),"Cooldown overlay begins at zero progress");
        check(slots.get(0).status().equals("0.4s")&&slots.get(1).status().equals("1.5s")&&slots.get(2).status().equals("1.5s"),"Remaining cooldown readable in seconds");
        BufferedImage cooling=abilityImage(hud,santa,abilities);save(cooling,"abilities-cooldown");
        check(!Arrays.equals(ready.getRGB(0,0,800,600,null,0,800),cooling.getRGB(0,0,800,600,null,0,800)),"Unavailable slots visibly differ");
        for(int i=0;i<45;i++){santa.update(false,false,false,false,java.util.List.of());abilities.update();}
        slots=hud.getSlots(santa,abilities);check(slots.get(0).ready()&&slots.get(1).progress()==.5&&slots.get(2).status().equals("0.8s"),"Slots follow independent cooldown clocks");
        for(int i=45;i<300;i++){santa.update(false,false,false,false,java.util.List.of());abilities.update();}
        check(hud.getSlots(santa,abilities).stream().allMatch(AbilityHUD.Slot::ready),"Ready returns at actual cooldown expiry");
        for(int[] size:new int[][]{{800,600},{1270,681},{1270,720}})for(int i=0;i<5;i++) {
            Rectangle r=hud.bounds(size[0],size[1],i);
            check(new Rectangle(0,0,size[0],size[1]).contains(r)&&r.x>=306,"Compact slots stay visible and clear of quest panel");
            if(i>0)check(!r.intersects(hud.bounds(size[0],size[1],i-1)),"Consistent spacing prevents overlap");
        }
        santa=new Santa(100,100);HealingPoint shelter=new HealingPoint(Area.Type.FOREST,116,121);
        check(!shelter.use(santa,false)&&!shelter.isUsed(),"Full HP does not consume shelter");
        check(shelter.getInteractionText(santa,false).equals("[E] Rest (+40 HP) - once per adventure"),"Nearby rest prompt");
        santa.takeDamage(20);check(!shelter.use(santa,true)&&!shelter.isUsed(),"Combat blocks rest");
        check(shelter.getInteractionText(santa,true).isEmpty(),"Combat restriction is silent");
        BufferedImage image=new BufferedImage(320,240,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();shelter.draw(g,santa,false);g.dispose();save(image,"shelter-ready");
        check(shelter.use(santa,false)&&santa.getHealth()==100,"Healing clamps to max HP");
        check(!shelter.use(santa,false)&&shelter.getInteractionText(santa,false).equals("Shelter Already Used"),"Shelter remains spent");
        image=new BufferedImage(320,240,BufferedImage.TYPE_INT_ARGB);g=image.createGraphics();shelter.draw(g,santa,false);g.dispose();save(image,"shelter-used");
        santa.setPosition(500,500);check(shelter.getInteractionText(santa,false).isEmpty(),"No distant shelter label clutter");
        HealingPoint fresh=new HealingPoint(Area.Type.FOREST,516,521);santa.takeDamage(100);
        check(!fresh.use(santa,false)&&!fresh.isUsed(),"Defeated Santa cannot rest");
        GamePanel p=playing();AreaTitleOverlay live=(AreaTitleOverlay)field(p,"areaTitle");
        check(live.isActive(),"Village title begins after opening story");
        int startX=santa(p).getX();p.keyPressed(new java.awt.event.KeyEvent(p,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_D,'d'));tick(p,30);p.keyReleased(new java.awt.event.KeyEvent(p,KeyEvent.KEY_RELEASED,0,0,KeyEvent.VK_D,'d'));
        check(santa(p).getX()>startX&&live.getOpacity()==1,"Area title never blocks movement");save(render(p),"village-title-gameplay");
        tick(p,180);check(!live.isActive(),"Large title does not remain in HUD");save(render(p),"village-normal-gameplay");
        forestFixture(p);check(live.isActive()&&live.getTitle().equals("Frozen Forest"),"Transition restarts intro for destination");
        @SuppressWarnings("unchecked") var points=(java.util.List<HealingPoint>)field(p,"healingPoints");
        var marker=points.get(0).getPosition();at(santa(p),new WorldMap.Point(marker.x()+40,marker.y()));tick(p,30);save(render(p),"forest-shelter-gameplay");
        System.out.println("PASS: "+checks+" UI/animation/healing assertions; captures at "+CAPTURES);
    }
}
