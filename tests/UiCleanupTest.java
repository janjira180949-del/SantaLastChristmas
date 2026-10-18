import game.*;
import entity.*;
import ui.*;
import audio.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/** Event-scoped banner removal plus real E/audio/quest progression and stable cinematic geometry. */
public class UiCleanupTest extends JourneyHomeTest {
 static void clearNotice(GamePanel p)throws Exception {var f=GamePanel.class.getDeclaredField("areaMessage");f.setAccessible(true);f.set(p,null);f=GamePanel.class.getDeclaredField("areaMessageFrames");f.setAccessible(true);f.setInt(p,0);}
 static long count(List<String> events,String id){return events.stream().filter(id::equals).count();}
 static void notices()throws Exception {
  var p=playing();try {
   List<String> events=new ArrayList<>();((AudioManager)field(p,"audio")).setEventObserver(events::add);
   int n=0;for(var site:List.of(VillageActivities.Site.IRIS,VillageActivities.Site.CLARA,VillageActivities.Site.BEN)){
    clearNotice(p);santa(p).setPosition(site.x-16,site.y+4);tap(p,KeyEvent.VK_E);tick(p,GiftDeliveryPresentation.DURATION);
    check(activities(p).deliveries()==++n&&story(p).getDeliveryQuest().getProgress()==n,"Real gift transaction and tracker progress "+site);
    check(field(p,"areaMessage")==null,"No delivery banner "+site);
    check(count(events,"delivery.success")==n,"Exactly one original delivery SFX "+site);
    var d=((DialogueManager)field(p,"dialogueManager")).getActiveDialogue();check(d.getLineCount()==(site==VillageActivities.Site.IRIS?5:site==VillageActivities.Site.CLARA?6:9),"Approved dialogue pages "+site);closeDialogue(p);
   }
   check(story(p).isDeliveryComplete(),"All three still complete delivery quest");QuestFlowTest.nearToby(p);tap(p,KeyEvent.VK_E);closeDialogue(p);
   at(santa(p),encounters(p).get("village").getCenter());tick(p,1);clearWave(p);check(story(p).isVillageCleared()&&((String)field(p,"areaMessage")).startsWith("Gate cleared."),"Important gate notification preserved");
   travelGate(p,Area.Type.FOREST);clearNotice(p);at(santa(p),WorldMap.forArea(Area.Type.FOREST).point("items/Toby Gift"));tap(p,KeyEvent.VK_E);
   check(story(p).isGiftFound()&&!story(p).isLostGiftComplete()&&field(p,"areaMessage")==null,"Lost gift collected without redundant banner");check(count(events,"pickup.lost_gift")==1,"Original lost gift SFX preserved");
   check(story(p).getHudPresentation().objective().contains("Return the gift to Toby"),"Tracker supplies return guidance");returnGift(p);check(chapter(p).isUnlocked()&&story(p).isLostGiftComplete(),"Return dialogue and material unlock preserved");
   int collected=0;for(var pickup:FinalChapter.PICKUPS){clearNotice(p);at(santa(p),pickup.point());int before=chapter(p).count(pickup.material());tap(p,KeyEvent.VK_E);collected++;
    check(chapter(p).isCollected(pickup.id())&&chapter(p).count(pickup.material())==before+1,"Exact material transaction "+pickup.id());check(field(p,"areaMessage")==null,"No material collection banner, including milestones "+pickup.id());check(chapter(p).nearby(santa(p))==null,"Collected object removed "+pickup.id());}
   check(collected==9&&count(events,"pickup.spirit")==3&&count(events,"pickup.wood")==3&&count(events,"pickup.ornament")==3,"All nine original material pickup sounds preserved");
   restoreVillage(p);check(chapter(p).isComplete()&&field(p,"areaMessage")!=null,"Important restoration notices and four restorations preserved");
   travelGate(p,Area.Type.FOREST);clearNotice(p);var road=encounters(p).get("forest-road");at(santa(p),road.getCenter());tick(p,1);clearWave(p);
   check(road.getState()==Encounter.State.COMPLETE&&encounters(p).getActive()==null,"Road genuinely completes through combat");check(field(p,"areaMessage")==null,"No road-clear banner");
  }finally{p.stopGame();}
 }
 static void panels()throws Exception {
  String[][] pages={{"The final crystal shatters.\nA terrifying roar echoes through the forest.\nFrost Fang awakens, blocking the road\nto Christmas Town!","Beware its deadly claws and icy breath!\nDodge its marked attacks,\nthen strike when it leaves itself open."},{"Once the protector of the Spirit Tree,\nDark Titan now stands consumed\nby Black Frost.\nThe fallen guardian blocks\nSanta's final path.","To save the guardian, Santa must break\nBlack Frost's hold.\nDodge its devastating attacks,\nthen strike when it leaves itself open."}};
  for(int[] size:new int[][]{{800,600},{1270,720}})for(int b=0;b<2;b++){
   int w=size[0],h=size[1];var overlay=new StoryOverlay();overlay.startBossAt(b==0?"FROST FANG":"DARK TITAN",boss.FrostFang.ARENA,pages[b]);
   var im=new BufferedImage(w,h,2);var g=im.createGraphics();Rectangle first=overlay.panelBounds(g,w,h);
   check(new Rectangle(0,0,w,h).contains(first)&&first.getMaxY()==h-30&&first.getCenterX()==w/2.0,"Full stable bottom-centered parchment "+b+" "+w);
   check(first.height<=220&&first.y>=h/2,"Compact panel leaves upper boss focus clear");
   for(int page=0;page<2;page++){
    check(overlay.panelBounds(g,w,h).equals(first),"Both pages retain identical bounds");g.setFont(new Font("Dialog",0,14));var lines=ChristmasUI.wrap(g.getFontMetrics(),pages[b][page],first.width-92);
    check(77+(lines.size()-1)*18+5<first.height-40,"Body clears controls and decorative button");for(String line:lines)check(g.getFontMetrics().stringWidth(line)<=first.width-92,"Body fits horizontal bounds");
    int frames=overlay.getFrames();overlay.draw(g,w,h);check(overlay.getFrames()==frames,"Paint does not advance cinematic timing");overlay.advance();
   }check(!overlay.isActive(),"Exactly two pages and original continue semantics");g.dispose();
  }
  check(BossFallPresentation.FANG_DURATION==240&&BossFallPresentation.TITAN_DURATION==300&&SpiritTreeTransformation.DURATION==180&&EndingCutscene.SLEIGH_DURATION==360,"All requested cinematic clocks exact");
  var p=endingFixture();try{tick(p,180);check(ending(p).getState()==EndingCutscene.State.CELEBRATION,"Exact restoration still enters original celebration");for(int[] size:new int[][]{{800,600},{1270,720}}){p.setSize(size[0],size[1]);call(p,"updateCamera");render(p);check(!p.isGameplayHudVisible(),"Existing cinema routing hides skill/quest HUD");var r=new Rectangle((size[0]-Math.min(620,size[0]-40))/2,size[1]-150,Math.min(620,size[0]-40),120);check(r.getMaxY()==size[1]-30&&r.y>size[1]/2,"Saved parchment bottom center");}completeCelebration(p);check(chapter(p).celebrated(),"Original gratitude/celebration sequence finishes");}finally{p.stopGame();}
 }
 public static void main(String[]a)throws Exception {notices();panels();System.out.println("UiCleanupTest PASS "+checks+" event, audio, quest, cinematic layout and timing assertions");}
}
