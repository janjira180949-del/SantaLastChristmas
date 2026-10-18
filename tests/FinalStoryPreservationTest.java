import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import game.*;

/** Full original inventory plus token-level proof that narrative edits preserve gameplay ownership. */
public class FinalStoryPreservationTest extends V2TestSupport {
 static String prior(String p)throws Exception{return new String(FinalStorySupport.before(p),StandardCharsets.UTF_8).replace("\r\n","\n");}
 static String current(String p)throws Exception{return new String(UiCleanupSupport.previous(p,Files.readAllBytes(Path.of(p))),StandardCharsets.UTF_8).replace("\r\n","\n");}
 static String literals(String s){return s.replaceAll("\"(?:[^\"\\\\]|\\\\.)*\"","\"<text>\"").replaceAll("\\s+","");}
 static String part(String s,String a,String b){return s.substring(s.indexOf(a),s.indexOf(b,s.indexOf(a)));}
 public static void main(String[]args)throws Exception{
  var allowed=FinalStorySupport.properties("AUTHORIZED");
  var production=Set.of("src/game/GamePanel.java","src/game/WorkshopInteractions.java","src/game/VillageActivities.java","src/ui/DialogueManager.java","src/game/EndingCutscene.java","src/game/EndingCredits.java");
  var matcher=Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("build/post-phase4/final-inventory.json")));int count=0,changed=0;
  while(matcher.find()){
   String p=matcher.group(1),hash=matcher.group(2);byte[] bytes=UiCleanupSupport.previous(p,Files.readAllBytes(Path.of(p)));
   check(ForestPreservationSupport.hash(bytes).equals(allowed.getProperty(p,hash)),"Original or exact authorized narrative/test owner: "+p);
   if(!ForestPreservationSupport.hash(bytes).equals(hash)){changed++;if(p.startsWith("src/"))check(production.contains(p),"Only six narrative owners");check(!p.startsWith("assets/")&&!p.startsWith(".vscode/")&&!p.endsWith(".ps1"),"No asset, audio, launch or script change");}
   count++;
  }
  check(count==1886,"Complete latest successful baseline inventory");
  String p="src/game/GamePanel.java",s=current(p).replace("site.lines(villageActivities.chapter.homeReturned())","site.lines()");
  check(literals(s).equals(literals(prior(p))),"Entire GamePanel exact except approved words and homeReturned text selection; input, audio, world, collision, camera unchanged");
  p="src/game/VillageActivities.java";s=current(p).replace("if(recipient==Site.BEN)","if(deliveries()==3)");
  check(literals(s).equals(literals(prior(p))),"Whole delivery/restoration model unchanged except words and attaching original closing pages to Ben");
  String benStart="            case BEN->{",benEnd="            default->throw new IllegalArgumentException(\"Not a recipient\");";
  check(part(current(p),benStart,benEnd).equals(part(prior(p),benStart,benEnd)),"Every original Ben conversation byte retained");
  check(part(current(p),"lines.add(santaLine(\"I came to deliver presents", "        return List.copyOf(lines);").equals(part(prior(p),"lines.add(santaLine(\"I came to deliver presents", "        return List.copyOf(lines);")),"Both Ben closing/reflection pages exact");
  p="src/game/WorkshopInteractions.java";s=current(p);String a="        /** Narrative selection only;",b="        private static DialogueLine letter";s=s.replace(part(s,a,b),"");
  check(literals(s).equals(literals(prior(p))),"Whole optional interaction owner unchanged except words and selection overload");
  p="src/ui/DialogueManager.java";a="    public boolean startFor(Elias elias,Santa santa,boolean questAccepted,boolean bossDefeated,boolean roadCleared)";b="    private DialogueLine line(";
  s=current(p).replace(part(current(p),a,b),"<Elias text selection>");String old=prior(p).replace(part(prior(p),a,b),"<Elias text selection>");
  check(literals(s).equals(literals(old)),"All gift/clue/reward/dialogue-state logic exact outside Elias text selection");
  String tutorial="Two infected guard the eastern gate. Click toward them to attack. Space dashes past a warning. Press 1 to ring your Bell.";check(current(p).contains(tutorial)&&prior(p).contains(tutorial),"Original combat tutorial exact");
  check(part(current(p),"        activeDialogue=new Dialogue(lines);eliasDialogueActive=true;",b).equals(part(prior(p),"        activeDialogue=new Dialogue(lines);eliasDialogueActive=true;",b)),"Elias existing dialogue commit/flags exact");
  p="src/game/EndingCutscene.java";a="    public void drawScreen(";b="    private static void centered(";
  check(current(p).replace(part(current(p),a,b),"<screen text>").equals(prior(p).replace(part(prior(p),a,b),"<screen text>")),"All cinematic state/update/animation/camera/celebration dialogue exact; screen text only");
  for(String line:new String[]{"HOME FOR CHRISTMAS","Christmas was never truly lost.","Our neighbors have come out of their homes. The Spirit Tree is shining again. Thank you, Santa. We will keep its light together.","Then Christmas has a home here. Keep a place for one another, even on the coldest nights."})check(current(p).contains(line)&&prior(p).contains(line),"Approved preserved ending/celebration text exact");
  check(BossFallPresentation.FANG_DURATION==240&&BossFallPresentation.TITAN_DURATION==300&&SpiritTreeTransformation.DURATION==180&&EndingCutscene.SLEIGH_DURATION==360&&EndingCutscene.duration(EndingCutscene.State.CREDITS)==2400,"All five cinematic durations exact");
  check(EndingCredits.ENTRIES.size()==33&&EndingCredits.MENU_ENTRIES.size()==7,"Full readable credits and compact menu");
  String[] roles={"DEVELOPED BY","COURSE","PROGRAMMING","GAME DESIGN","STORY & NARRATIVE DESIGN","ART & VISUAL ASSETS","MUSIC & SOUND"};
  String[] values={"JANJIRA WANGKAOOM","Object Oriented Programming","JANJIRA WANGKAOOM\nOpenAI Codex","JANJIRA WANGKAOOM","JANJIRA WANGKAOOM","ChatGPT by OpenAI"};
  for(int i=0;i<roles.length;i++){check(EndingCredits.ENTRIES.get(i).role().equals(roles[i]),"Approved credits role/order "+i);if(i<values.length)check(EndingCredits.ENTRIES.get(i).credit().equals(values[i]),"Approved credits value "+i);}
  var im=new java.awt.image.BufferedImage(800,600,2);var g=im.createGraphics();
  for(var e:EndingCredits.ENTRIES){g.setFont(new java.awt.Font("Serif",0,16));check(g.getFontMetrics().stringWidth(e.role())<760,"Credits heading fits800: "+e.role());g.setFont(new java.awt.Font("Serif",0,20));for(String line:e.credit().split("\n"))check(g.getFontMetrics().stringWidth(line)<760,"Credits body fits800: "+line);}g.dispose();
  check(EndingCredits.ENTRIES.get(31).credit().equals("ChatGPT\nOpenAI Codex\nFor assisting with game development,\ncreative design, and\ntechnical implementation."),"Exact Special Thanks");
  check(EndingCredits.ENTRIES.get(32).credit().equals("Every gift carries a little hope.\nEvery act of kindness keeps Christmas alive.\n\nMay your days be filled with warmth,\njoy, and a little Christmas magic.\n\nMERRY CHRISTMAS!"),"Exact final Christmas greeting");
  System.out.println("FinalStoryPreservationTest PASS "+checks+" full inventory, original-text, credits and cinematic timing assertions; "+changed+" existing owners authorized");
 }
}
