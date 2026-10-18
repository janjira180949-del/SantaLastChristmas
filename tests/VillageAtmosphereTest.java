import game.*;import entity.Santa;import java.awt.*;import java.awt.image.*;import java.nio.file.*;import java.security.*;import java.util.*;import javax.imageio.ImageIO;
public class VillageAtmosphereTest {
 static int checks;static GamePanel lastPanel;static Path dir=Path.of("build/village-dark-atmosphere");
 static void check(boolean ok,String m){checks++;if(!ok)throw new AssertionError(m);}
 static BufferedImage scene(int level,int area,int x,int y)throws Exception{
  var panel=V2TestSupport.playing();panel.setSize(1280,800);var f=GamePanel.class.getDeclaredField("villageAtmosphereBrightness");f.setAccessible(true);f.setInt(panel,level);
  f=GamePanel.class.getDeclaredField("areaManager");f.setAccessible(true);var manager=(AreaManager)f.get(panel);f=AreaManager.class.getDeclaredField("currentAreaIndex");f.setAccessible(true);f.setInt(manager,area);
  var load=GamePanel.class.getDeclaredMethod("loadCurrentAreaContent");load.setAccessible(true);load.invoke(panel);f=GamePanel.class.getDeclaredField("santa");f.setAccessible(true);((Santa)f.get(panel)).setPosition(x,y);
  var im=new BufferedImage(1280,800,2);var g=im.createGraphics();panel.paint(g);g.dispose();lastPanel=panel;return im;
 }
 static boolean equal(BufferedImage a,BufferedImage b){return Arrays.equals(a.getRGB(0,0,a.getWidth(),a.getHeight(),null,0,a.getWidth()),b.getRGB(0,0,b.getWidth(),b.getHeight(),null,0,b.getWidth()));}
 // Layered alpha may round to 255 while retaining one RGB level of the background.
 static int channelDistance(int a,int b){int max=0;for(int shift:new int[]{0,8,16})max=Math.max(max,Math.abs(((a>>>shift)&255)-((b>>>shift)&255)));return max;}
 static void save(BufferedImage im,String name)throws Exception{ImageIO.write(im,"png",dir.resolve(name+".png").toFile());}
 static BufferedImage lighting(int cameraX,int cameraY){var im=new BufferedImage(700,500,2);var g=im.createGraphics();g.translate(-cameraX,-cameraY);VillageAtmosphere.draw(g,cameraX,cameraY,700,500,70);g.dispose();return im;}
 public static void main(String[]args)throws Exception{
  Files.createDirectories(dir);var day=scene(100,0,1296,1160);var a=scene(70,0,1296,1160);var b=scene(60,0,1296,1160);
  check(equal(a,scene(70,0,1296,1160)),"deterministic gameplay");check(!equal(a,b),"options differ");check(equal(scene(70,2,1296,1136),scene(100,2,1296,1136)),"Town unaffected");check(equal(scene(70,1,1200,1200),scene(100,1,1200,1200)),"Forest unaffected");
  check(VillageEnvironment.props().size()==30,"all props retained");check(VillageAtmosphere.lampPositions().size()==10,"three authored, three prop and four square lamps");
  scene(70,0,1296,1160);var hudImage=new BufferedImage(1280,800,2);var hg=hudImage.createGraphics();Object[] fields=new Object[7];String[] names={"hud","santa","spiritTree","abilitySystem","areaManager","christmasEater","storyQuestManager"};for(int i=0;i<names.length;i++){var f=GamePanel.class.getDeclaredField(names[i]);f.setAccessible(true);fields[i]=f.get(lastPanel);}var unlocked=GamePanel.class.getDeclaredMethod("isCurrentMainExitUnlocked");unlocked.setAccessible(true);for(var method:fields[0].getClass().getMethods())if(method.getName().equals("draw")&&method.getParameterCount()==11)method.invoke(fields[0],hg,1280,800,fields[1],fields[2],fields[3],fields[4],fields[5],null,fields[6],unlocked.invoke(lastPanel));hg.dispose();int uiPixels=0;for(int y=0;y<800;y++)for(int x=0;x<1280;x++)if((hudImage.getRGB(x,y)>>>24)==255 && hudImage.getRGB(x,y)==day.getRGB(x,y)){uiPixels++;check(channelDistance(day.getRGB(x,y),a.getRGB(x,y))<=1&&channelDistance(day.getRGB(x,y),b.getRGB(x,y))<=1,"opaque HUD unchanged at "+x+","+y+" day="+Integer.toHexString(day.getRGB(x,y))+" dusk="+Integer.toHexString(a.getRGB(x,y))+" HUD="+Integer.toHexString(hudImage.getRGB(x,y)));}check(uiPixels>1000,"complete opaque HUD covered");
  var l=lighting(700,650);var r=lighting(800,650);check(equal(l.getSubimage(100,0,600,500),r.getSubimage(0,0,600,500)),"lamp and tint world anchored");
  
  save(day,"01-current-day");save(a,"02-dark-70-percent");save(b,"03-dark-60-percent");var comp=new BufferedImage(3840,832,2);var g=comp.createGraphics();g.setColor(new Color(20,30,40));g.fillRect(0,0,3840,832);BufferedImage[]ims={day,a,b};String[]labels={"CURRENT","70%","60%"};for(int i=0;i<3;i++){g.drawImage(ims[i],i*1280,32,null);g.setColor(Color.WHITE);g.drawString(labels[i],i*1280+20,22);}g.dispose();save(comp,"04-comparison");save(scene(70,0,912,944),"05-lamps-70");save(scene(60,0,912,944),"06-lamps-60");save(scene(70,0,528,736),"07-toby-70");save(scene(60,0,528,736),"08-toby-60");
  System.out.println("Opaque HUD pixels checked: "+uiPixels);System.out.println("PASS: "+checks+" atmosphere checks; eight gameplay review captures");
 }
}


