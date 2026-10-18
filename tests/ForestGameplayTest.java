import game.*;import entity.*;import java.awt.event.*;import java.nio.file.*;import java.util.*;import javax.imageio.ImageIO;
public class ForestGameplayTest extends V2TestSupport {
 static QuestGuidance.Objective objective(GamePanel p)throws Exception{return (QuestGuidance.Objective)call(p,"currentObjective");}
 static void capture(GamePanel p,String name)throws Exception{for(int[] size:new int[][]{{800,600},{1270,720}}){p.setSize(size[0],size[1]);call(p,"updateCamera");var image=render(p);ImageIO.write(image,"png",Path.of("build/phase9-3-2b/captures/"+name+"-"+size[0]+"x"+size[1]+".png").toFile());var bounds=ui.HUD.questBounds(objective(p),((VillageActivities)field(p,"villageActivities")).chapter,size[0],size[1]);check(bounds.y>=126&&bounds.getMaxY()<=size[1],"Quest panel fits viewport");}}
 public static void main(String[] args)throws Exception{
  Files.createDirectories(Path.of("build/phase9-3-2b/captures"));
  var chapter=new FinalChapter();chapter.unlock();var s=new Santa(416,1115);var first=chapter.guidedPickup(s);check(first.id()==3,"Nearest initial material");
  s.setPosition(2800,1595);check(chapter.guidedPickup(s).id()==3,"Movement alone cannot switch selected material");
  chapter.collect(3);check(chapter.guidedPickup(s).id()==8,"Collected target revalidated");
  for(int id:new int[]{4,5})chapter.collect(id);check(chapter.pollFeedback().contains("Repair the House"),"Wood milestone names repair");
  for(var pickup:FinalChapter.PICKUPS)chapter.collect(pickup.id());check(chapter.pollFeedback().contains("western Village gate"),"Nine materials return feedback");
  check(!chapter.collect(8)&&chapter.pollFeedback()==null,"No duplicate milestone");
  for(String id:new String[]{"crystal-1","crystal-2","crystal-3"}){
   var manager=new EncounterManager();var e=manager.get(id);var actors=new ArrayList<Enemy>();at(s,e.getCenter());e.start(s,actors);
   check(e.getWarningFrames()==60&&actors.isEmpty(),"Initial guardians warned "+id);
   for(int i=0;i<59;i++)e.advance(s,actors);check(actors.isEmpty(),"No early spawn");e.advance(s,actors);check(!actors.isEmpty(),"Original actors arrive");
   check(e.retry(s,actors)&&e.getWarningFrames()==60&&actors.isEmpty(),"Retry restores warning");e.abandon(actors);check(e.getState()==Encounter.State.READY&&e.getWarningFrames()==0,"Departure clears clock");
  }
  var p=playing();forestFixture(p);var a=(VillageActivities)field(p,"villageActivities");at(santa(p),new WorldMap.Point(880,1136));tick(p,245);
  capture(p,"materials");
  var rows=ui.HUD.class.getDeclaredMethod("questRows",QuestGuidance.Objective.class,boolean.class,FinalChapter.class);rows.setAccessible(true);
  String text=rows.invoke(null,objective(p),true,a.chapter).toString();check(text.contains("Spirit Fragments: 0/3")&&text.contains("Ancient Wood: 0/3")&&text.contains("Christmas Ornaments: 0/3")&&text.contains("0/4"),"Compact independent material/restoration counts");
  int sequence=santa(p).getAttackSequence();var box=ui.HUD.questBounds(objective(p),a.chapter,p.getWidth(),p.getHeight());QuestFlowPresentationTest.mouse(p,box.x+20,box.y+box.height-12);check(sequence==santa(p).getAttackSequence(),"Material row click cannot attack");
  a.chapter.toggleTracker();capture(p,"materials-expanded");a.chapter.toggleTracker();
  var selected=objective(p).target();travelGate(p,Area.Type.VILLAGE);travelGate(p,Area.Type.FOREST);check(objective(p).target().equals(selected)&&a.chapter.trackerCollapsed(),"Guidance and collapse preference survive travel");
  at(santa(p),new WorldMap.Point(880,1136));var before=objective(p).target();santa(p).setPosition(2800,1595);check(objective(p).target().equals(before),"Live guidance stable");
  at(santa(p),new WorldMap.Point(880,1136));var e=encounters(p).get("forest-road");e.start(santa(p),enemies(p));var active=EncounterManager.class.getDeclaredField("active");active.setAccessible(true);active.set(encounters(p),e);
  tap(p,KeyEvent.VK_E);check(((String)field(p,"areaMessage")).equals("Clear the nearby encounter before collecting."),"Actual E material combat feedback");check(!a.chapter.isCollected(0),"Combat E cannot collect");encounters(p).leaveArea(enemies(p));tap(p,KeyEvent.VK_E);check(a.chapter.count(FinalChapter.Material.SPIRIT_FRAGMENT)==1,"Explicit later E updates count");
  text=rows.invoke(null,objective(p),true,a.chapter).toString();check(text.contains("Spirit Fragments: 1/3"),"Live HUD count updates");capture(p,"materials-collected");
  for(int id:new int[]{1,2})a.chapter.collect(id);a.chapter.restore(FinalChapter.Restoration.STREETLIGHTS);
  text=rows.invoke(null,objective(p),true,a.chapter).toString();check(!text.contains("Spirit Fragments:")&&text.contains("1/4"),"Repaired category hidden, restoration progress retained");
  for(var pickup:FinalChapter.PICKUPS)a.chapter.collect(pickup.id());for(var task:FinalChapter.Restoration.values())a.chapter.restore(task);story(p).synchronizeRestoration();
  text=rows.invoke(null,objective(p),true,a.chapter).toString();check(!text.contains("Ancient Wood:")&&!text.contains("Christmas Ornaments:"),"Material counts hidden after restoration");
  p.beginAdventure();check(((VillageActivities)field(p,"villageActivities")).chapter.count(FinalChapter.Material.SPIRIT_FRAGMENT)==0&&((VillageActivities)field(p,"villageActivities")).chapter.trackerCollapsed(),"Fresh Start resets counts and presentation preference");
  p.stopGame();p=playing();investigationFixture(p);e=encounters(p).get("crystal-2");at(santa(p),e.getCenter());tick(p,1);capture(p,"guardian-warning");
  check(objective(p).action().contains("approaching"),"Initial warning is reported");
  int warning=e.getWarningFrames();tap(p,KeyEvent.VK_ESCAPE);tick(p,90);check(e.getWarningFrames()==warning,"Pause freezes guardian clock");tap(p,KeyEvent.VK_ESCAPE);tick(p,e.getWarningFrames());capture(p,"guardian-active");
  santa(p).setPosition(2600,1690);check(objective(p).target().equals(e.getCenter()),"Active crystal outranks closer dormant crystal");
  for(var enemy:e.getMembers())enemy.takeDamage(enemy.getMaximumHealth());e.advance(santa(p),enemies(p));check(objective(p).action().equals("Recovery between waves"),"Recovery feedback");e.advance(santa(p),enemies(p));
  for(int i=0;i<59;i++)e.advance(santa(p),enemies(p));check(objective(p).action().equals("Reinforcements approaching"),"Reinforcement feedback");
  int guard=400;while(e.getState()!=Encounter.State.COMPLETE&&guard-->0){for(var enemy:e.getMembers())enemy.takeDamage(enemy.getMaximumHealth());e.advance(santa(p),enemies(p));encounters(p).finishActive();}
  check(guard>0,"All original waves finish");encounters(p).finishActive();
  @SuppressWarnings("unchecked") var cores=(List<BlackFrostCrystal>)field(p,"crystals");for(var core:cores)core.update(santa(p));
  check(objective(p).target().equals(e.getCenter())&&objective(p).action().equals("Core exposed \u2014 attack with LMB!"),"Exposed core outranks dormant crystal");
  at(santa(p),new WorldMap.Point(2032,1095));capture(p,"core-exposed");at(santa(p),new WorldMap.Point(1664,1315));tick(p,1);capture(p,"shrine");p.stopGame();
  var shrine=new HealingPoint(Area.Type.FOREST,116,121);s=new Santa(100,100);check(shrine.getInteractionText(s,false).contains("once per adventure"),"Single-use rule readable");check(!shrine.use(s,false),"Full HP retains use");s.takeDamage(50);check(shrine.use(s,false)&&s.getHealth()==90&&!shrine.use(s,false),"Original 40 HP single use");check(new HealingPoint(Area.Type.CHRISTMAS_TOWN,116,121).getInteractionText(s,false).equals("[E] Rest (+40 HP)"),"Town feedback unchanged");
  System.out.println("PASS: "+checks+" Forest guidance/counts/warning/input/healing assertions");
 }
}
