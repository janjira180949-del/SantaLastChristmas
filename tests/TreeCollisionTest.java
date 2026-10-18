import game.*;import java.awt.*;import java.awt.event.*;import java.awt.image.BufferedImage;import java.awt.geom.Rectangle2D;
/** Real movement, tight ground collision, actual GamePanel occlusion and restoration interaction. */
public class TreeCollisionTest extends JourneyHomeTest {
 static final Rectangle BASE=new Rectangle(1264,856,64,24);
 static void move(GamePanel p,int key,int frames)throws Exception{p.keyPressed(new KeyEvent(p,KeyEvent.KEY_PRESSED,0,0,key,KeyEvent.CHAR_UNDEFINED));tick(p,frames);p.keyReleased(new KeyEvent(p,KeyEvent.KEY_RELEASED,0,0,key,KeyEvent.CHAR_UNDEFINED));}
 static void clear(GamePanel p,String label)throws Exception{check(!util.Collision.intersectsAny(santa(p).getBounds(),WorldMap.forArea(Area.Type.VILLAGE).solids()),label);}
 static void approaches(GamePanel p)throws Exception{
  santa(p).setPosition(1280,960);move(p,KeyEvent.VK_W,80);check(santa(p).getY()==888,"Below: head stops exactly at grounded base, no oversized wall");clear(p,"Below actor clear");
  santa(p).setPosition(1280,720);move(p,KeyEvent.VK_S,80);check(santa(p).getY()==814,"Above: feet stop at base rear");clear(p,"Above actor clear");
  santa(p).setPosition(1168,842);move(p,KeyEvent.VK_D,80);check(santa(p).getX()==1232,"Left: stops at actual64-pixel base");clear(p,"Left actor clear");
  santa(p).setPosition(1392,842);move(p,KeyEvent.VK_A,80);check(santa(p).getX()==1328,"Right: stops at exact base edge");clear(p,"Right actor clear");
  santa(p).setPosition(1232,888);for(int key:new int[]{KeyEvent.VK_D,KeyEvent.VK_W,KeyEvent.VK_A,KeyEvent.VK_S}){move(p,key,24);clear(p,"Full narrow orbit around base remains open");}
  check(santa(p).getX()==1232&&santa(p).getY()==888,"Complete96-pixel orbit in both axes, unchanged4px/update");
  santa(p).setPosition(1184,760);move(p,KeyEvent.VK_D,48);check(santa(p).getX()==1376,"Canopy is passable; full224x276 art is never a collider");clear(p,"Canopy corridor clear");
  move(p,KeyEvent.VK_A,48);check(santa(p).getX()==1184,"Market corridor traversable in reverse");
 }
 static BufferedImage tree(FinalChapter chapter)throws Exception{VillageRestorationVisuals.prepareAsync().get();var f=VillageRestorationVisuals.class.getDeclaredField("ART");f.setAccessible(true);var future=(java.util.concurrent.CompletableFuture<?>)f.get(null);Object art=future.get();var m=art.getClass().getDeclaredMethod("trees");m.setAccessible(true);return ((BufferedImage[])m.invoke(art))[chapter.restored(FinalChapter.Restoration.TREE)?1:0];}
 static void depth(GamePanel p)throws Exception{
  p.setSize(2560,1920);var sprite=tree(chapter(p));santa(p).setPosition(1280,1160);var background=render(p);
  santa(p).setPosition(1280,760);var rear=render(p);int same=0,opaque=0;
  for(int y=750;y<810;y++)for(int x=1270;x<1330;x++)if((sprite.getRGB(x-1184,y-604)>>>24)>250){opaque++;int a=rear.getRGB(x,y),b=background.getRGB(x,y),difference=0;for(int shift:new int[]{0,8,16})difference=Math.max(difference,Math.abs((a>>shift&255)-(b>>shift&255)));if(difference<=257-(sprite.getRGB(x-1184,y-604)>>>24))same++;}
  check(opaque>1000&&same==opaque,"Actual GamePanel canopy occludes rear actor within the approved near-opaque artwork alpha");
  santa(p).setPosition(1350,842);clear(p,"Front side approach is collision-clear");var front=render(p);int visible=0;
  for(int y=830;y<880;y++)for(int x=1345;x<1395;x++)if((sprite.getRGB(x-1184,y-604)>>>24)>250&&front.getRGB(x,y)!=background.getRGB(x,y))visible++;
  check(visible>40,"Actor in front of tree ground anchor renders over overlapping lower branch pixels");
 }
 public static void main(String[] args)throws Exception{
  GamePanel p=playing();try{
   var village=WorldMap.forArea(Area.Type.VILLAGE);check(VillageRestorationVisuals.TREE_ANCHOR.equals(new WorldMap.Point(1296,880))&&VillageRestorationVisuals.TREE_BOUNDS.equals(new Rectangle(1184,604,224,276)),"Exact approved tree art and world anchor");
   check(village.solids().stream().filter(BASE::equals).count()==1&&village.solids().size()==286,"Exactly one compact64x24 ground collider added");
   approaches(p);depth(p);
   for(var site:java.util.List.of(VillageActivities.Site.MARTHA,VillageActivities.Site.OLIVER)){near(p,site);tap(p,KeyEvent.VK_E);check(((ui.DialogueManager)field(p,"dialogueManager")).isActive(),"Market neighbor remains reachable "+site);closeDialogue(p);}
  }finally{p.stopGame();}
  p=postStory();try{collectMaterials(p);travelGate(p,Area.Type.VILLAGE);near(p,VillageActivities.Site.TREE);tap(p,KeyEvent.VK_E);check(chapter(p).restored(FinalChapter.Restoration.TREE)&&chapter(p).count(FinalChapter.Material.ORNAMENT)==3,"Actual E restoration retains all three ornaments and authoritative quest rules");approaches(p);depth(p);}finally{p.stopGame();}
  var town=WorldMap.forArea(Area.Type.CHRISTMAS_TOWN);Rectangle2D trunk=new Rectangle2D.Double(1184,896,192,192);check(town.solids().stream().anyMatch(trunk::equals),"Large Town Spirit Tree already has its authored trunk collider; no Town change");
  System.out.println("TreeCollisionTest PASS "+checks+" assertions");
 }
}
