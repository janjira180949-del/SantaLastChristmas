import game.*;import quest.*;import entity.*;import util.*;import java.awt.*;import java.awt.image.*;import java.io.*;import javax.imageio.*;import java.util.Arrays;
public class EnvironmentPolishTest {
 static int checks;static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static BufferedImage render(WorldMap m,boolean original,int x,int y){var im=new BufferedImage(800,600,2);var g=im.createGraphics();g.translate(-x,-y);if(original)m.drawOriginal(g,false);else m.draw(g,false);g.dispose();return im;}
 static void save(BufferedImage im,String n)throws Exception{ImageIO.write(im,"png",new File("build/environment-polish/"+n+".png"));}
 static boolean equal(BufferedImage a,BufferedImage b){return Arrays.equals(a.getRGB(0,0,a.getWidth(),a.getHeight(),null,0,a.getWidth()),b.getRGB(0,0,b.getWidth(),b.getHeight(),null,0,b.getWidth()));}
 static void pickupCapture(StoryQuestTarget t,String n)throws Exception{var m=WorldMap.forArea(Area.Type.FOREST);var p=m.point("items/Toby Gift");var im=render(m,false,p.x()-400,p.y()-300);var g=im.createGraphics();g.translate(-p.x()+400,-p.y()+300);t.drawCollectedPatch(g);t.drawPickup(g);g.dispose();save(im,n);}
 public static void main(String[]a)throws Exception{
 new File("build/environment-polish").mkdirs();var panel=new GamePanel();var story=new StoryQuestManager(()->0,()->3);story.synchronizeDelivery();var santa=new Santa(0,0);var targets=story.getTargets();
 for(int i=0;i<1;i++){check(story.startAfterDialogue(panel.getChildren().get(i)),"direct quest start");story.recordVillageCleared();var t=targets.get(i);var b=t.getBounds();santa.setPosition((int)b.getCenterX()-16,(int)b.getCenterY()-21);
 check(story.completeTarget(t,santa),"logical collect");var v=t.getPickupVisual();check(v!=null&&v.age()==0&&v.isActive()&&v.alpha()==1&&v.rise()==0,"exactly one start at original position");check(story.isGiftFound(),"progress independent of animation");check(!story.completeTarget(t,santa)&&!t.collect()&&v==t.getPickupVisual(),"duplicate rejected, same visual");check(!story.isLostGiftComplete(),"quest waits for Toby while pickup animates");
 if(i==0)pickupCapture(t,"01-pickup-start");double rise=0;float alpha=1;
 for(int f=1;f<=33;f++){t.updateVisual();check(v.rise()>=rise&&v.rise()-rise<1,"gradual rise");check(v.alpha()<=alpha&&alpha-v.alpha()<.08,"smooth monotonic alpha");rise=v.rise();alpha=v.alpha();if(i==0&&f==12)pickupCapture(t,"02-pickup-rise");if(i==0&&f==26)pickupCapture(t,"03-pickup-fade");}
 check(!v.isActive()&&v.alpha()==0,"fully gone at 33 ticks");
 var empty=new BufferedImage(160,160,2);var eg=empty.createGraphics();v.draw(eg);eg.dispose();check(equal(empty,new BufferedImage(160,160,2)),"terminated effect renders no pixels");t.updateVisual();check(v.age()==33,"terminates");}
 for(var type:new Area.Type[]{Area.Type.FOREST,Area.Type.VILLAGE,Area.Type.CHRISTMAS_TOWN}){var m=WorldMap.forArea(type);var solids=m.solids().stream().map(Object::toString).toList();var pts=java.util.Map.copyOf(m.points());var before=render(m,true,0,0);var after=render(m,false,0,0);check(equal(after,render(m,false,0,0)),"repeat render");check(solids.equals(m.solids().stream().map(Object::toString).toList())&&pts.equals(m.points()),"bounds solids transitions unchanged");check(equal(before,render(m,true,0,0)),"original ending artwork preserved");}
 var m=WorldMap.forArea(Area.Type.VILLAGE);save(render(m,true,1500,450),"04-road-before-or-reference");save(render(m,false,800,200),"05-road-polished-straight");save(render(m,false,1500,450),"06-road-polished-curve");save(render(m,false,900,700),"07-road-polished-intersection");var im=render(m,false,1500,450);var g=im.createGraphics();g.translate(-1500,-450);santa.setPosition(2010,650);santa.draw(g);g.dispose();save(im,"08-road-polished-gameplay");
 var src=ImageAssets.load("assets/world/village_clean.png");check(equal(PackedSnowPath.polish(src),PackedSnowPath.polish(src)),"independent deterministic generation");var full=VillageEnvironmentPolishTest.material(Area.Type.VILLAGE,ApprovedSnowGround.polish(src,PackedSnowPath.polish(src),true),PackedSnowPath.createOverlay(src));check(equal(render(m,false,1500,450),full.getSubimage(1500,450,800,600)),"camera world anchoring");
 check(!equal(render(m,true,1500,450),render(m,false,1500,450)),"curve treated");check(!equal(render(m,true,900,700),render(m,false,900,700)),"intersection treated");
 System.out.println("PASS: "+checks+" assertions; retained story/ground rendering, deterministic maps/camera; eight captures");
 }
}



