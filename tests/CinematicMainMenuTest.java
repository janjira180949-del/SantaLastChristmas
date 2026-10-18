import game.*;
import ui.CinematicMenu;
import ui.MenuBackdrop;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/** Real menu input/actions, source preservation, responsive captures and bounded animation. */
public final class CinematicMainMenuTest extends V2TestSupport {
    private static final Path OUTPUT=Path.of("build/main-menu/captures");
    private static final StringBuilder timings=new StringBuilder("[\n");
    private static CinematicMenu menu(GamePanel p)throws Exception {return (CinematicMenu)field(p,"mainMenu");}
    private static void move(GamePanel p,Rectangle b) {
        MouseEvent e=new MouseEvent(p,MouseEvent.MOUSE_MOVED,0,0,(int)b.getCenterX(),(int)b.getCenterY(),0,false);
        for(var listener:p.getMouseMotionListeners())listener.mouseMoved(e);
    }
    private static void press(GamePanel p,Rectangle b) {
        for(var listener:p.getMouseListeners())listener.mousePressed(new MouseEvent(p,MouseEvent.MOUSE_PRESSED,0,0,
            (int)b.getCenterX(),(int)b.getCenterY(),1,false,MouseEvent.BUTTON1));
    }
    private static void release(GamePanel p,Rectangle b) {
        for(var listener:p.getMouseListeners())listener.mouseReleased(new MouseEvent(p,MouseEvent.MOUSE_RELEASED,0,0,
            (int)b.getCenterX(),(int)b.getCenterY(),1,false,MouseEvent.BUTTON1));
    }
    private static void click(GamePanel p,Rectangle b)throws Exception {move(p,b);press(p,b);release(p,b);tick(p,6);}
    private static void capture(GamePanel p,String name)throws Exception {
        ImageIO.write(render(p),"png",OUTPUT.resolve(p.getWidth()+"x"+p.getHeight()+"-"+name+".png").toFile());
    }
    private static boolean same(BufferedImage a,BufferedImage b) {
        return java.util.Arrays.equals(a.getRGB(0,0,a.getWidth(),a.getHeight(),null,0,a.getWidth()),
            b.getRGB(0,0,b.getWidth(),b.getHeight(),null,0,b.getWidth()));
    }
    private static void preserve()throws Exception {
        String baseline=Files.readString(Path.of("docs/MAIN_MENU_BASELINE.json"));
        var matcher=Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(baseline);
        int javaCount=0,pngCount=0;
        while(matcher.find()) {
            String path=matcher.group(1);
            if(path.equals("src/game/GamePanel.java")||path.equals("src/ui/MenuBackdrop.java")
                ||path.equals("src/game/EndingCutscene.java")||path.equals("src/game/EndingTownVisuals.java"))continue;
            String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(AudioPhase2PresentationSupport.originalBytes(path,Files.readAllBytes(Path.of(path)))));
            check(hash.equals(matcher.group(2)),"Unrelated gameplay/source/artwork preserved: "+path);
            if(path.endsWith(".java"))javaCount++;if(path.endsWith(".png"))pngCount++;
        }
        check(javaCount==54&&pngCount==110,"All 54 unrelated production sources and 110 supplied/runtime PNGs unchanged");
    }
    private static void keyboard()throws Exception {
        GamePanel p=new GamePanel();p.setSize(800,600);CinematicMenu m=menu(p);
        tap(p,KeyEvent.VK_UP);check(m.getSelected()==4,"Up wraps from START to EXIT");
        tap(p,KeyEvent.VK_DOWN);check(m.getSelected()==0,"Down wraps from EXIT to START");
        tap(p,KeyEvent.VK_DOWN);tap(p,KeyEvent.VK_ENTER);
        check(p.getState()==GamePanel.State.MAIN_MENU&&m.getPressTicks()>0,"Keyboard confirmation presents brief press animation");
        tick(p,5);check(m.getPage()==CinematicMenu.Page.STORY,"Keyboard confirms STORY");
        tap(p,KeyEvent.VK_ESCAPE);check(m.getPage()==CinematicMenu.Page.MAIN,"Escape closes informational page");
        tap(p,KeyEvent.VK_DOWN);tap(p,KeyEvent.VK_ENTER);tick(p,5);
        check(m.getPage()==CinematicMenu.Page.SETTINGS,"Keyboard confirms SETTINGS");
        tap(p,KeyEvent.VK_RIGHT);check(m.isReducedMotion(),"Real presentation setting toggles with Right");
        tap(p,KeyEvent.VK_LEFT);check(!m.isReducedMotion(),"Real presentation setting toggles with Left");
        tap(p,KeyEvent.VK_ENTER);tick(p,5);check(m.isReducedMotion(),"Enter toggles selected setting");
        for(int row=0;row<5;row++)tap(p,KeyEvent.VK_DOWN);
        tap(p,KeyEvent.VK_ENTER);tick(p,5);check(m.getPage()==CinematicMenu.Page.MAIN,"Keyboard BACK works after all five settings rows");
        tap(p,KeyEvent.VK_DOWN);tap(p,KeyEvent.VK_ENTER);tick(p,5);check(m.getPage()==CinematicMenu.Page.CREDITS,"Keyboard confirms CREDITS");
        tap(p,KeyEvent.VK_ENTER);tick(p,5);check(m.getPage()==CinematicMenu.Page.MAIN,"Enter closes credits");
        m.resetNavigation();
        p.keyPressed(new KeyEvent(p,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_ENTER,'\n'));
        tick(p,1);p.keyPressed(new KeyEvent(p,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_ENTER,'\n'));
        tick(p,5);check(p.getState()==GamePanel.State.PLAYING,"ENTER START launches unchanged adventure");
        check(((StoryOverlay)field(p,"storyOverlay")).isActive(),"Existing opening remains after menu START");
        p.keyReleased(new KeyEvent(p,KeyEvent.KEY_RELEASED,0,0,KeyEvent.VK_ENTER,'\n'));
        check(!story(p).isStoryStarted()&&enemies(p).isEmpty(),"Menu never advances quests or encounters");
    }
    private static void backdrop()throws Exception {
        MenuBackdrop b=new MenuBackdrop();BufferedImage a=new BufferedImage(800,600,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=a.createGraphics();b.draw(g,800,600,0,false);g.dispose();Object cache=field(b,"image");
        BufferedImage still=new BufferedImage(800,600,BufferedImage.TYPE_INT_RGB);g=still.createGraphics();b.draw(g,800,600,300,false);g.dispose();
        check(same(a,still)&&field(b,"image")==cache,"Reduced ambient motion is static and reuses scaled background");
        BufferedImage animated=new BufferedImage(800,600,BufferedImage.TYPE_INT_RGB);g=animated.createGraphics();b.draw(g,800,600,300,true);g.dispose();
        check(!same(a,animated)&&field(b,"image")==cache,"Snow/pan/light animate without recreating background cache");
        var snow=MenuBackdrop.class.getDeclaredField("SNOW_COUNT");snow.setAccessible(true);
        check(snow.getInt(null)==96,"Snow has fixed 96-flake budget");
    }
    private static void benchmark(GamePanel p)throws Exception {
        BufferedImage target=new BufferedImage(p.getWidth(),p.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=target.createGraphics();
        for(int i=0;i<20;i++)p.paint(g);long[] times=new long[60];
        for(int i=0;i<times.length;i++){tick(p,1);long start=System.nanoTime();p.paint(g);times[i]=System.nanoTime()-start;}
        g.dispose();java.util.Arrays.sort(times);
        if(timings.length()>2)timings.append(",\n");
        timings.append(String.format(java.util.Locale.ROOT,"{\"width\":%d,\"height\":%d,\"median_ms\":%.3f,\"p95_ms\":%.3f}",
            p.getWidth(),p.getHeight(),times[30]/1e6,times[57]/1e6));
    }
    private static void viewport(int w,int h)throws Exception {
        GamePanel p=new GamePanel();p.setSize(w,h);CinematicMenu m=menu(p);tick(p,30);capture(p,"01-main");
        Rectangle art=MenuBackdrop.artBounds(w,h);
        check(Math.abs(art.width/(double)art.height-1.5)<.004,"Background uniform aspect ratio: "+w+"x"+h);
        check(art.x>=0&&art.y>=0&&art.x+art.width<=w&&art.y+art.height<=h,"No artwork stretched beyond viewport");
        for(int i=0;i<5;i++) {
            Rectangle b=CinematicMenu.button(w,h,i);
            check(b.x>w/2&&new Rectangle(0,0,w,h).contains(b),"Button entirely in responsive right column: "+i);
            if(i>0)check(!b.intersects(CinematicMenu.button(w,h,i-1)),"No overlap with adjacent button: "+i);
            move(p,b);float before=m.getHighlight(i);tick(p,1);
            check(m.getHovered()==i&&m.getSelected()==i,"Mouse hover selects every row: "+i);
            if(i>0)check(m.getHighlight(i)>before&&m.getHighlight(i)<1,"Hover glow interpolates smoothly: "+i);
        }
        move(p,CinematicMenu.button(w,h,2));tick(p,24);capture(p,"02-settings-hover");
        press(p,CinematicMenu.button(w,h,2));capture(p,"03-pressed");release(p,CinematicMenu.button(w,h,2));
        tick(p,1);check(m.getPage()==CinematicMenu.Page.MAIN&&m.getPressTicks()==5,"Action waits for six-frame feedback");
        tick(p,5);capture(p,"04-settings-transition");tick(p,20);capture(p,"05-settings");
        check(m.getPage()==CinematicMenu.Page.SETTINGS,"Mouse SETTINGS button works");
        check(m.backButton(w,h).intersects(m.motionButton(w,h))==false,"Settings BACK and toggle do not overlap");
        click(p,m.motionButton(w,h));check(m.isReducedMotion(),"Mouse changes actual reduced motion setting");
        tick(p,30);BufferedImage frozen=render(p);tick(p,60);check(same(frozen,render(p)),"Reduced ambient motion stays fixed");
        capture(p,"06-settings-reduced");click(p,m.backButton(w,h));tick(p,20);
        click(p,CinematicMenu.button(w,h,1));tick(p,20);check(m.getPage()==CinematicMenu.Page.STORY,"Mouse STORY button works");capture(p,"07-story");
        check(!story(p).isStoryStarted()&&!story(p).isGiftFound(),"Reading STORY cannot modify quest progression");
        click(p,m.backButton(w,h));tick(p,20);click(p,CinematicMenu.button(w,h,3));tick(p,20);
        check(m.getPage()==CinematicMenu.Page.CREDITS&&EndingCredits.MENU_ENTRIES.size()==7&&EndingCredits.ENTRIES.size()==33,"CREDITS retains seven compact rows and all approved ending credits");capture(p,"08-credits");
        click(p,m.backButton(w,h));tick(p,20);click(p,CinematicMenu.button(w,h,2));tick(p,20);
        click(p,m.motionButton(w,h));click(p,m.backButton(w,h));tick(p,20);benchmark(p);
        Rectangle start=CinematicMenu.button(w,h,0),storyButton=CinematicMenu.button(w,h,1);
        press(p,start);release(p,storyButton);tick(p,10);check(p.getState()==GamePanel.State.MAIN_MENU&&m.getPage()==CinematicMenu.Page.MAIN,"Dragging release outside cancels click");
        click(p,start);check(p.getState()==GamePanel.State.PLAYING,"Mouse START launches gameplay at "+w+"x"+h);
        capture(p,"09-game-start");
        GamePanel exit=new GamePanel();exit.setSize(w,h);CinematicMenu exitMenu=menu(exit);
        move(exit,CinematicMenu.button(w,h,4));tick(exit,20);capture(exit,"10-exit-selected");
        exit.startGame();check((Boolean)field(exit,"running"),"Exit fixture starts an actual running loop");
        click(exit,CinematicMenu.button(w,h,4));check(exitMenu.pollAction()==CinematicMenu.Action.NONE,"EXIT action consumed exactly once by GamePanel");
        check(!(Boolean)field(exit,"running"),"EXIT stops game loop");
        ((Thread)field(exit,"gameThread")).join(2000);check(!((Thread)field(exit,"gameThread")).isAlive(),"EXIT terminates loop thread");
        CinematicMenu isolated=new CinematicMenu();isolated.mousePressed((int)CinematicMenu.button(w,h,4).getCenterX(),(int)CinematicMenu.button(w,h,4).getCenterY(),w,h);
        isolated.mouseReleased((int)CinematicMenu.button(w,h,4).getCenterX(),(int)CinematicMenu.button(w,h,4).getCenterY(),w,h);
        for(int i=0;i<6;i++)isolated.update();check(isolated.pollAction()==CinematicMenu.Action.EXIT,"EXIT produces real close request");
        check(isolated.pollAction()==CinematicMenu.Action.NONE,"Close request cannot repeat");
    }
    public static void main(String[] args)throws Exception {
        Files.createDirectories(OUTPUT);preserve();keyboard();backdrop();
        for(int[] size:new int[][]{{800,600},{1270,681},{1270,720}})viewport(size[0],size[1]);
        Files.writeString(Path.of("build/main-menu/render-performance.json"),timings.append("\n]\n").toString());
        System.out.println("PASS: "+checks+" cinematic menu assertions; 30 native captures; menu render timings recorded");
    }
}
