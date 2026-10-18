import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/** Narrows authorized edits before all original source/art/story guards continue. */
public class PostPhase4PreservationTest extends V2TestSupport {
 static String before(String p)throws Exception{return new String(PostPhase4Support.before(p),StandardCharsets.UTF_8).replace("\r\n","\n");}
 static String now(String p)throws Exception{return new String(FinalStorySupport.previous(p,Files.readAllBytes(Path.of(p))),StandardCharsets.UTF_8).replace("\r\n","\n");}
 public static void main(String[]args)throws Exception{
  var allowed=PostPhase4Support.properties("AUTHORIZED");var production=Set.of("src/audio/AudioCatalog.java","src/audio/AudioManager.java","src/audio/GameplayAudio.java","src/game/BossFallPresentation.java","src/game/JourneyHomeVisuals.java","src/game/GamePanel.java");
  var matcher=Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/POST_PHASE4_BASELINE.json")));
  int sources=0;while(matcher.find()){
   String p=matcher.group(1);if(!p.startsWith("src/")&&!p.startsWith(".vscode/")&&!p.endsWith(".ps1")&&!p.equals("srcs.txt")&&!p.startsWith("assets/audio/incoming/"))continue;
   byte[] bytes=FinalStorySupport.previous(p,Files.readAllBytes(Path.of(p)));check(ForestPreservationSupport.hash(bytes).equals(allowed.getProperty(p,matcher.group(2))),"Original or exact reviewed source "+p);
   check(ForestPreservationSupport.hash(PostPhase4Support.previous(p,bytes)).equals(matcher.group(2)),"Recoverable prior source "+p);if(p.startsWith("src/")){if(p.endsWith(".java"))sources++;if(allowed.containsKey(p))check(production.contains(p),"Six narrow production owners only");}
  }
  check(sources==102,"Production manifest unchanged");
  check(PostPhase4Support.assets().size()==15,"Seven originals plus seven PCM and manifest authenticated");
  String p="src/game/BossFallPresentation.java";check(now(p).equals(before(p).replace("top=(height-140)/2","top=height-140-30")),"Fall text/art/controls/camera/pose/timing exact; only vertical placement");
  p="src/game/JourneyHomeVisuals.java";String s=now(p),start="    /** Parked-only ground contact,",end="    public static BufferedImage sleighImage()";s=s.substring(0,s.indexOf(start))+s.substring(s.indexOf(end));check(s.equals(before(p)),"Whole sleigh renderer/art/animation/trajectory unchanged; parked helpers only");
  p="src/game/GamePanel.java";s=now(p).replace("        if(town&&villageActivities.chapter.celebrated())JourneyHomeVisuals.drawSleigh(graphics,santa,-1);\n","")
   .replace("JourneyHomeVisuals.drawParkedForeground(graphics,santa);","JourneyHomeVisuals.drawSleigh(graphics,santa,-1);")
   .replace("        if(areaManager.getCurrentArea().getType()==Area.Type.CHRISTMAS_TOWN\n                &&villageActivities.chapter.celebrated()&&!endingCutscene.hasStarted()){\n            solids=new java.util.ArrayList<>(solids);solids.addAll(JourneyHomeVisuals.parkedFootprints());\n        }\n","");
  check(s.equals(before(p)),"Entire GamePanel exact except parked-only footprints and local actor/sleigh depth");
  p="src/audio/AudioCatalog.java";s=now(p);start="        // User-selected local recordings:";end="        Map<String,List<Cue>> variants";s=s.substring(0,s.indexOf(start))+s.substring(s.indexOf(end));check(s.equals(before(p)),"Every other catalog/music/ambience/gain/priority unchanged");
  p="src/audio/AudioManager.java";s=now(p).replace("// Only the selected sword uses its corrected route gain/admission; other effect policies stay exact.\n        ","")
   .replace("(effect==Effect.SANTA_ATTACK?cue.gain():effect.gain())*gain,effect==Effect.SANTA_ATTACK?cue.priority():effect.priority()","effect.gain()*gain,effect.priority()");check(s.equals(before(p)),"Whole mixer/lifecycle/voice/cache/settings exact except sword route gain/admission");
  check(game.BossFallPresentation.FANG_DURATION==240&&game.BossFallPresentation.TITAN_DURATION==300&&game.SpiritTreeTransformation.DURATION==180&&game.EndingCutscene.SLEIGH_DURATION==360,"All cinematic clocks unchanged");
  System.out.println("PostPhase4PreservationTest PASS "+checks+" strict scope/source assertions");
 }
}
