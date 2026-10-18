import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import game.*;

/** Authenticate exact UI-only revisions; prove unchanged gameplay, text, camera, artwork and audio. */
public class UiCleanupPreservationTest extends V2TestSupport {
 static String before(String p)throws Exception{return new String(UiCleanupSupport.before(p),StandardCharsets.UTF_8).replace("\r\n","\n");}
 static String now(String p)throws Exception{return Files.readString(Path.of(p)).replace("\r\n","\n");}
 static String segment(String s,String a,String b){return s.substring(s.indexOf(a),s.indexOf(b,s.indexOf(a)));}
 static List<String> strings(String s){return Pattern.compile("\"(?:[^\"\\\\]|\\\\.)*\"").matcher(s).results().map(MatchResult::group).toList();}
 public static void main(String[]a)throws Exception{
  var allowed=UiCleanupSupport.properties("AUTHORIZED");var production=Set.of("src/game/GamePanel.java","src/game/StoryOverlay.java","src/game/EndingCutscene.java");
  var m=Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("build/final-story/final-inventory.json")));int total=0;
  while(m.find()){
   String p=m.group(1),hash=m.group(2);byte[] bytes=Files.readAllBytes(Path.of(p));check(ForestPreservationSupport.hash(bytes).equals(allowed.getProperty(p,hash)),"Original or exact approved UI-only revision "+p);
   if(allowed.containsKey(p)){if(p.startsWith("src/"))check(production.contains(p),"Only three presentation owners");check(!p.startsWith("assets/")&&!p.startsWith(".vscode/")&&!p.endsWith(".ps1"),"No art/audio/config/script changes");}total++;
  }
  check(total==1894,"Complete latest final-story inventory protected");
  String p="src/game/GamePanel.java",s=now(p);
  s=s.replace("activityDialogueSite=recipient;activityFeedback(false);","activityDialogueSite=recipient;activityFeedback();")
   .replace("case \"village-outskirts\",\"forest-road\",\"forest-clearing\",\"town-side-road\",\"forest-camp\",\"forest-ambush\",\"forest-exit\" -> { /* Completion remains authoritative; redundant road toast omitted. */ }","case \"village-outskirts\",\"forest-road\",\"forest-clearing\",\"town-side-road\",\"forest-camp\",\"forest-ambush\",\"forest-exit\" -> notifyPlayer(\"The road is quiet again.\");")
   .replace("if(pickup!=null&&villageActivities.chapter.collect(pickup.id())){activityFeedback(false);return true;}","if(pickup!=null&&villageActivities.chapter.collect(pickup.id())){activityFeedback();return true;}")
   .replace("            gameplayAudio.play(GIFT_PICKUP);\n            return true;","            gameplayAudio.play(GIFT_PICKUP);\n            notifyPlayer(\"Toby's Gift Found! Return the Gift to Toby.\");\n            return true;");
  String start="    private void activityFeedback()",end="    /** Compare both existing NPC owners";
  s=s.replace(segment(s,start,end),segment(before(p),start,end));
  check(s.equals(before(p)),"Entire GamePanel exactly preserved except four event-scoped visual side effects; all transactions/audio/camera/render ordering/input exact");
  check(now(p).contains("activityWindow.feedback(message);if(showBanner){notifyPlayer(message);activityNotice=true;}"),"Feedback consumed normally; general notification owner remains intact");
  p="src/game/EndingCutscene.java";check(now(p).equals(before(p).replace("int top=(height-120)/2;","int top=height-120-30;")),"Entire ending/celebration/credits owner exact except Christmas Saved Y");
  p="src/game/StoryOverlay.java";start="    public void draw(Graphics2D";end="    private static List<String> wrap(";
  check(now(p).replace(segment(now(p),start,end),"<panel layout>").equals(before(p).replace(segment(before(p),start,end),"<panel layout>")),"All boss intro pages/state/camera/animation/audio timing methods exact");
  String currentDraw=segment(now(p),start,"    /** Read-only geometry;");String oldDraw=segment(before(p),start,end);
  check(strings(currentDraw).equals(strings(oldDraw)),"All illustrated frame keys, titles, subtitles, body controls and fonts unchanged");
  for(String path:List.of("src/game/WorkshopInteractions.java","src/game/VillageActivities.java","src/game/FinalChapter.java","src/ui/DialogueManager.java","src/game/EndingCredits.java","src/game/JourneyHomeVisuals.java","src/game/BossFallPresentation.java","src/ui/HUD.java","src/ui/AbilityHUD.java","src/game/Camera.java","src/util/ImageAssets.java","src/game/AdventureAudio.java","src/audio/GameplayAudio.java","src/audio/AudioCatalog.java","src/boss/FrostFang.java","src/boss/DarkTitan.java","src/game/EncounterManager.java","src/game/WorldMap.java","src/quest/StoryQuestManager.java"))check(Arrays.equals(Files.readAllBytes(Path.of(path)),UiCleanupSupport.before(path)),"Sensitive story/credits/gameplay/art/audio/PNG/HUD owner byte-identical "+path);
  check(BossFallPresentation.FANG_DURATION==240&&BossFallPresentation.TITAN_DURATION==300&&SpiritTreeTransformation.DURATION==180&&EndingCutscene.SLEIGH_DURATION==360,"All requested cinematic durations unchanged");
  System.out.println("UiCleanupPreservationTest PASS "+checks+" exact scope, full inventory, narrative/audio and cinematic preservation assertions");
 }
}
