import game.*;import boss.*;import java.awt.*;import java.awt.event.*;import java.awt.geom.Rectangle2D;import java.util.*;
/** Real movement/E input and state edges; no quest, timing, or boss-AI replacements. */
public class Phase1WorldTest extends JourneyHomeTest {
 static final Rectangle TREE=new Rectangle(1184,896,192,192);
 @SuppressWarnings("unchecked") static java.util.List<HealingPoint> heals(GamePanel p)throws Exception{return (java.util.List<HealingPoint>)field(p,"healingPoints");}
 static void move(GamePanel p,int key,int n)throws Exception{TreeCollisionTest.move(p,key,n);}
 static void rest()throws Exception{
  var p=playing();try{townFixture(p);var map=WorldMap.forArea(Area.Type.CHRISTMAS_TOWN);var points=heals(p).stream().filter(h->h.getArea()==Area.Type.CHRISTMAS_TOWN).toList();
   check(points.size()==5&&heals(p).size()==7,"Exactly five Town rests plus the unchanged two Forest shrines");
   check(points.stream().map(HealingPoint::getPosition).distinct().count()==5&&points.stream().noneMatch(h->h.getPosition().equals(new WorldMap.Point(1360,880))),"Old Tree-adjacent rest retired, no duplicate placements");
   for(var h:points){var pos=h.getPosition();check(Math.hypot(pos.x()-1296,pos.y()-1008)>420,"Rest outside active ritual arena");
    for(var other:points)if(other!=h)check(Math.hypot(pos.x()-other.getPosition().x(),pos.y()-other.getPosition().y())>260,"Distributed rests, no clusters");
    check(Math.hypot(pos.x()-1776,pos.y()-1416)>116,"No Elias conversation-range conflict");
    for(int[] d:new int[][]{{-1,0},{1,0},{0,-1},{0,1}}){santa(p).setPosition(pos.x()-16+d[0]*64,pos.y()-21+d[1]*64);int key=d[0]<0?KeyEvent.VK_D:d[0]>0?KeyEvent.VK_A:d[1]<0?KeyEvent.VK_S:KeyEvent.VK_W;move(p,key,12);check(h.isNearby(santa(p))&&!util.Collision.intersectsAny(santa(p).getBounds(),map.solids()),"Actual normal movement reaches rest from four directions "+pos);}
    at(santa(p),pos);check(!h.use(santa(p),false)&&!h.isUsed(),"Full HP does not consume rest");santa(p).takeDamage(60);check(!h.use(santa(p),true)&&!h.isUsed(),"Combat restriction unchanged");tap(p,KeyEvent.VK_E);check(h.isUsed()&&santa(p).getHealth()==80,"Actual E grants exactly40 HP once "+pos);tap(p,KeyEvent.VK_E);check(santa(p).getHealth()==80,"No repeated healing");santa(p).heal(100);tick(p,30);
   }
   call(p,"retryEncounter");check(points.stream().allMatch(HealingPoint::isUsed),"Spent rests survive retry");travelGate(p,Area.Type.FOREST);travelGate(p,Area.Type.CHRISTMAS_TOWN);check(points.stream().allMatch(HealingPoint::isUsed),"Spent rests survive travel");
   p.beginAdventure();check(heals(p).size()==7&&heals(p).stream().noneMatch(HealingPoint::isUsed),"Fresh Start reconstructs unused finite points");
  }finally{p.stopGame();}
 }
 @SuppressWarnings("unchecked") static java.util.List<Rectangle2D> movement(GamePanel p)throws Exception{var m=GamePanel.class.getDeclaredMethod("playerMovementSolids",java.util.List.class);m.setAccessible(true);return (java.util.List<Rectangle2D>)m.invoke(p,WorldMap.forArea(areas(p).getCurrentArea().getType()).solids());}
 static void tree(boolean skip)throws Exception{
  var p=Phase102FCinematicTest.entrance(false,800,600);try{
   var map=WorldMap.forArea(Area.Type.CHRISTMAS_TOWN);check(map.solids().contains(TREE)&&movement(p).contains(TREE),"Tree solid before live fight, including approved entrance");closeOverlay(p);check(!movement(p).contains(TREE)&&movement(p).size()==map.solids().size()-1,"Only exact Tree collider removed from player navigation");check(map.solids().contains(TREE),"Boss/skills/LOS retain authoritative world solids");
   santa(p).toggleDeveloperTestMode();
   for(int[] a:new int[][]{{1280,840,KeyEvent.VK_S,80},{1280,1140,KeyEvent.VK_W,80},{1100,980,KeyEvent.VK_D,80},{1420,980,KeyEvent.VK_A,80}}){santa(p).setPosition(a[0],a[1]);move(p,a[2],a[3]);check(a[2]==KeyEvent.VK_S?santa(p).getY()>1088:a[2]==KeyEvent.VK_W?santa(p).getY()<846:a[2]==KeyEvent.VK_D?santa(p).getX()>1376:santa(p).getX()<1152,"Actual full-footprint movement crosses hidden Tree in four directions");}
   santa(p).setPosition(1100,840);p.keyPressed(new KeyEvent(p,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_D,'d'));p.keyPressed(new KeyEvent(p,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_S,'s'));tick(p,40);p.keyReleased(new KeyEvent(p,KeyEvent.KEY_RELEASED,0,0,KeyEvent.VK_D,'d'));p.keyReleased(new KeyEvent(p,KeyEvent.KEY_RELEASED,0,0,KeyEvent.VK_S,'s'));check(santa(p).getBounds().intersects(TREE),"Diagonal crossing also uses the existing sliding/movement engine");
   var titan=(DarkTitan)field(p,"activeBoss");santa(p).setPosition(1260,990);check(santa(p).getBounds().intersects(TREE),"Player may occupy hidden base during battle");
   tap(p,KeyEvent.VK_ESCAPE);int x=santa(p).getX(),y=santa(p).getY();tick(p,20);check(santa(p).getX()==x&&santa(p).getY()==y,"Pause does not relocate player");tap(p,KeyEvent.VK_ESCAPE);
   titan.takeDamage(titan.getMaximumHealth());tick(p,1);var fall=(BossFallPresentation)field(p,"bossFall");check(fall.isActive()&&!santa(p).getBounds().intersects(TREE)&&!util.Collision.intersectsAny(santa(p).getBounds(),map.solids()),"Reappearing Tree clears Santa to valid nearest free footprint");check(movement(p).contains(TREE)&&santa(p).getX()==1260&&santa(p).getY()==1096,"Nearest clear base edge used; no unnecessary diagonal displacement");
   if(skip)tap(p,KeyEvent.VK_E);else tick(p,BossFallPresentation.TITAN_DURATION);check(story(p).isBossDefeated()&&!((SpiritTreeTransformation)field(p,"treeTransformation")).isActive(),"Normal/skip victory never auto-restores Tree");
   at(santa(p),(WorldMap.Point)call(p,"spiritTreeInteractionPoint"));tap(p,KeyEvent.VK_E);var transform=(SpiritTreeTransformation)field(p,"treeTransformation");check(transform.isActive()&&transform.getFrames()==0,"Tree approachable via existing E");tick(p,179);check(transform.isActive()&&!transform.isComplete(),"No early restoration");tick(p,1);check(transform.isComplete()&&JourneyHomeTest.ending(p).isCelebration(),"Exactly180 updates retain original story");check(movement(p).contains(TREE),"Restored visible Tree remains solid");
  }finally{p.stopGame();}
 }
 static void resetTree()throws Exception{
  var p=Phase102FCinematicTest.entrance(false,1270,720);try{closeOverlay(p);check(!movement(p).contains(TREE),"Live arena open");call(p,"retryEncounter");check(!movement(p).contains(TREE),"Retried live Titan still opens only player Tree base");check(areas(p).tryTravel(Area.Type.FOREST),"Existing area manager supports permitted scene departure");call(p,"loadCurrentAreaContent");check(areas(p).tryTravel(Area.Type.CHRISTMAS_TOWN),"Reconstruct Town after abandonment");call(p,"loadCurrentAreaContent");check(movement(p).contains(TREE),"Abandoned hidden state does not survive scene reconstruction");p.beginAdventure();check(areas(p).getCurrentArea().getType()==Area.Type.SANTA_BASE&&!story(p).isBossDefeated(),"Fresh Start clears boss state");}finally{p.stopGame();}
 }
 static void gate()throws Exception{var p=SantaBaseNavigationTest.opening();try{collectOpeningPresents(p);santa(p).setPosition(800,1008);move(p,KeyEvent.VK_S,80);check(areas(p).getCurrentArea().getType()==Area.Type.VILLAGE,"Actual walking through decorated Base gate retains transition");}finally{p.stopGame();}}
 public static void main(String[]args)throws Exception{gate();rest();tree(false);tree(true);resetTree();System.out.println("Phase1WorldTest PASS "+checks+" assertions");}
}
