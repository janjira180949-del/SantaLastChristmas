import game.*;import ui.*;import java.awt.event.*;import java.nio.file.*;import javax.imageio.*;
/** Actual GamePanel review fixtures; deterministic placements, never concept-art composites. */
public class VillageStoryReview extends V2TestSupport {
 static final Path DIR=Path.of(System.getProperty("santa.review.output","build/phase9-2/captures"));
 static void shot(GamePanel p,String name)throws Exception{call(p,"updateCamera");ImageIO.write(render(p),"png",DIR.resolve(name+".png").toFile());}
 static void views(GamePanel p,String name)throws Exception{for(int[] size:new int[][]{{800,600},{1024,768},{1270,720},{1600,900}}){p.setSize(size[0],size[1]);shot(p,name+"-"+size[0]+"x"+size[1]);}p.setSize(1270,720);}
 static void clean(GamePanel p)throws Exception{var f=GamePanel.class.getDeclaredField("areaMessage");f.setAccessible(true);f.set(p,null);var title=(AreaTitleOverlay)field(p,"areaTitle");for(int i=0;i<200;i++)title.update();}
 static void pose(GamePanel p,int x,int y)throws Exception{santa(p).setPosition(x-16,y+4);call(p,"updateCamera");}
 public static void main(String[] args)throws Exception{
  Files.createDirectories(DIR);var p=playing();views(p,"01-village-arrival");clean(p);pose(p,1296,1120);views(p,"02-village-before");
  for(var r:java.util.List.of(VillageActivities.Site.IRIS,VillageActivities.Site.CLARA,VillageActivities.Site.BEN)){
   pose(p,r.x,r.y+28);views(p,"03-"+r.name().toLowerCase()+"-prompt");tap(p,KeyEvent.VK_E);tick(p,92);views(p,"04-"+r.name().toLowerCase()+"-handover");tick(p,GiftDeliveryPresentation.DURATION);views(p,"05-"+r.name().toLowerCase()+"-dialogue");closeDialogue(p);
  }
  views(p,"06-after-deliveries-find-toby");p.stopGame();
  var base=new GamePanel();base.setSize(1270,720);base.beginAdventure();closeOverlay(base);SantaBaseStoryInteractionTest.approach(base,WorkshopInteractions.Site.GIFT_STORAGE);views(base,"07-storage-before");tap(base,KeyEvent.VK_E);views(base,"08-storage-collected-no-modal");base.stopGame();
  p=playing();forestFixture(p);FinalChapterTest.collectMaterials(p);travelGate(p,Area.Type.VILLAGE);
  clean(p);pose(p,1078,870);views(p,"09-market-damaged");
  for(var r:FinalChapter.Restoration.values()){
   clean(p);pose(p,r==FinalChapter.Restoration.TREE?VillageRestorationVisuals.TREE_ANCHOR.x()-120:r.approach.x(),r==FinalChapter.Restoration.TREE?VillageRestorationVisuals.TREE_ANCHOR.y()-96:r.approach.y());views(p,"10-"+r.name().toLowerCase()+"-before");pose(p,r.approach.x(),r.approach.y());tap(p,KeyEvent.VK_E);shot(p,"11-"+r.name().toLowerCase()+"-immediate-feedback");clean(p);pose(p,r==FinalChapter.Restoration.TREE?VillageRestorationVisuals.TREE_ANCHOR.x()-120:r.approach.x(),r==FinalChapter.Restoration.TREE?VillageRestorationVisuals.TREE_ANCHOR.y()-96:r.approach.y());views(p,"11-"+r.name().toLowerCase()+"-after");
   if(r==FinalChapter.Restoration.STREETLIGHTS){pose(p,1078,870);clean(p);views(p,"12-market-lights-restored");}
   if(r==FinalChapter.Restoration.CAMPFIRE){tick(p,9);shot(p,"13-fire-next-frame");}
  }
  pose(p,1078,870);clean(p);views(p,"14-market-festive");p.setSize(2560,1920);pose(p,1296,1120);shot(p,"15-village-restored-overview");p.stopGame();
  for(int[] size:new int[][]{{800,600},{1024,768},{1270,720},{1600,900}}){
   p=playing();p.setSize(size[0],size[1]);pose(p,592,720);shot(p,"16-delivery-tracker-collapsed-"+size[0]);
   var a=(VillageActivities)field(p,"villageActivities");a.chapter.toggleTracker();shot(p,"17-delivery-tracker-expanded-"+size[0]);
   var r=VillageActivities.Site.CLARA;pose(p,r.x,r.y);tap(p,KeyEvent.VK_E);tick(p,65);shot(p,"18-handover-"+size[0]);tick(p,GiftDeliveryPresentation.DURATION);shot(p,"19-clara-dialogue-"+size[0]);closeDialogue(p);
   completeOpeningDeliveries(p);shot(p,"20-find-toby-"+size[0]);forestFixture(p);FinalChapterTest.collectMaterials(p);FinalChapterTest.restoreVillage(p);shot(p,"21-restoration-done-"+size[0]);p.stopGame();
  }
  System.out.println("PASS: actual GamePanel review captures complete");
 }
}
