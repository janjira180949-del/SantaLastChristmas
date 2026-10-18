import game.*;
import entity.*;
import ui.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.nio.file.*;
import java.util.List;
import javax.imageio.ImageIO;

/** Approved exact words exercised through the existing input/dialogue/story lifecycle. */
public class FinalStoryTest extends JourneyHomeTest {
 static List<DialogueLine> approved(String id){return switch(id){
 case "6" -> List.of(new DialogueLine("THE LAST LETTER",Portrait.Kind.SANTA,"Dear Santa,\nChristmas is almost here!\nBut something strange is happening\nin our village."),new DialogueLine("THE LAST LETTER",Portrait.Kind.SANTA,"The Christmas lights are fading.\nIris, Clara, and Ben\nare still waiting for you."),new DialogueLine("THE LAST LETTER",Portrait.Kind.SANTA,"Could you bring their presents\nand visit us soon?\nWe could really use\na little Christmas cheer."),new DialogueLine("Santa",Portrait.Kind.SANTA,"Three presents, three old friends.\nSounds easy enough...\nWhat could possibly go wrong?"));
 case "7" -> List.of(new DialogueLine("Santa",Portrait.Kind.SANTA,"The Spirit Lantern...\nIs its light getting weaker?"),new DialogueLine("Santa",Portrait.Kind.SANTA,"That's strange.\nIt always shines brightest\nat Christmas."),new DialogueLine("Santa",Portrait.Kind.SANTA,"First, those three presents.\nThen I'll find out\nwhat's going on."));
 case "8" -> List.of(new DialogueLine("THE JOURNEY AHEAD",Portrait.Kind.SANTA,"THE LAST VILLAGE\n\u2193\nFROZEN FOREST\n\u2193\nCHRISTMAS TOWN"),new DialogueLine("Santa",Portrait.Kind.SANTA,"The Last Village comes first.\nIris, Clara, and Ben are waiting\nfor their Christmas presents."),new DialogueLine("Santa",Portrait.Kind.SANTA,"The Frozen Forest...\nand Christmas Town?\nI hope this trip doesn't get\nmore complicated than planned."));
 case "11" -> List.of(new DialogueLine("Santa",Portrait.Kind.SANTA,"Merry Christmas, Iris!\nThis one's for you!"),new DialogueLine("Iris",Portrait.Kind.IRIS,"Santa! You really came!\nThank you so much!"),new DialogueLine("Santa",Portrait.Kind.SANTA,"Of course! But...\nwhere did all the Christmas lights go?"),new DialogueLine("Iris",Portrait.Kind.IRIS,"They went out a few nights ago.\nNo one knows why."),new DialogueLine("Santa",Portrait.Kind.SANTA,"Then I'll find out!\nChristmas isn't over yet!"));
 case "12" -> List.of(new DialogueLine("Santa",Portrait.Kind.SANTA,"Merry Christmas, Clara!\nI saved this present just for you!"),new DialogueLine("Clara",Portrait.Kind.CLARA,"Santa! You remembered!\nI was starting to lose hope."),new DialogueLine("Santa",Portrait.Kind.SANTA,"Of course I did!\nBut... what happened to the village?"),new DialogueLine("Clara",Portrait.Kind.CLARA,"A strange dark frost appeared.\nIt damaged our homes and trees."),new DialogueLine("Santa",Portrait.Kind.SANTA,"Dark frost?\nThat's definitely not on my Christmas list!"),new DialogueLine("Clara",Portrait.Kind.CLARA,"And it's still spreading...\nPlease be careful, Santa."));
 case "14" -> List.of(new DialogueLine("Toby",Portrait.Kind.TOY,"Santa... I lost the Christmas gift\nmy mother gave me."),new DialogueLine("Toby",Portrait.Kind.TOY,"It's the last thing I have\nto remember her by.\nI lost it on the northern Forest road.\nCould you please help me?"),new DialogueLine("Santa",Portrait.Kind.SANTA,"Of course, Toby.\nI promise I'll bring it back.\nSome gifts are simply too precious to lose."),new DialogueLine("Toby",Portrait.Kind.TOY,"Two infected guard the eastern gate. Click toward them to attack. Space dashes past a warning. Press 1 to ring your Bell."));
 case "15" -> List.of(new DialogueLine("Santa",Portrait.Kind.SANTA,"Here's your mother's gift, Toby.\nI promised I'd bring it back!"),new DialogueLine("Toby",Portrait.Kind.TOY,"Thank you, Santa!\nI thought I'd lost it forever.\nIt means so much to me."),new DialogueLine("Toby",Portrait.Kind.TOY,"But the village is still suffering.\nOur lights, houses, and Christmas tree\nare damaged. Even the campfire is out."),new DialogueLine("Toby",Portrait.Kind.TOY,"We need Spirit Fragments, Ancient Wood,\nand Christmas Ornaments from the forest.\nMartha knows how to use them."),new DialogueLine("Santa",Portrait.Kind.SANTA,"Then we'd better get to work!\nWe can't leave Christmas in the cold!"));
 case "16" -> List.of(new DialogueLine("Toby",Portrait.Kind.TOY,"The lights are back, Santa!\nEveryone is smiling again.\nThank you for saving our village!"),new DialogueLine("Santa",Portrait.Kind.SANTA,"Finally! Now that's more like Christmas!\nBut... the strange frost is still here."),new DialogueLine("Toby",Portrait.Kind.TOY,"I saw strange crystals deep in the forest.\nThe frost seemed to spread from them."),new DialogueLine("Santa",Portrait.Kind.SANTA,"Mysterious crystals in a frozen forest?\nThis Christmas just keeps getting weirder!\nCould they be connected to the Spirit Tree?"),new DialogueLine("Toby",Portrait.Kind.TOY,"There are three crystals.\nThey're blocking the road to Christmas Town.\nAnd something is guarding them."),new DialogueLine("Santa",Portrait.Kind.SANTA,"Then I'll destroy those crystals\nand find out what's behind all this.\nDon't worry, Toby. I'll bring Christmas back."));
 case "18" -> List.of(new DialogueLine("Elias",Portrait.Kind.ELIAS,"Santa... you made it.\nThe Spirit Tree is still alive,\nbut its guardian is losing himself."),new DialogueLine("Santa",Portrait.Kind.SANTA,"The Dark Titan?\nPlease tell me that's just\na dramatic nickname."),new DialogueLine("Elias",Portrait.Kind.ELIAS,"The Spirit Tree created him to carry\nour sorrow and fear. He was never evil.\nBut the burden became too much."),new DialogueLine("Elias",Portrait.Kind.ELIAS,"Black Frost took control of him.\nFree him, and the Spirit Tree\nmay shine again."),new DialogueLine("Santa",Portrait.Kind.SANTA,"Then I'll free him.\nNo one should have to carry\nall that sorrow alone."));
 case "24" -> List.of(new DialogueLine("Santa",Portrait.Kind.SANTA,"The Spirit Lantern...\nIt's shining brighter than ever!"),new DialogueLine("Santa",Portrait.Kind.SANTA,"So that's the secret.\nChristmas Spirit grows\nwhen we care for each other."),new DialogueLine("Santa",Portrait.Kind.SANTA,"Good thing everyone helped!\nMy back was starting to hurt!"));
 case "25" -> List.of(new DialogueLine("THE JOURNEY AHEAD",Portrait.Kind.SANTA,"THE LAST VILLAGE\n\u2193\nFROZEN FOREST\n\u2193\nCHRISTMAS TOWN"),new DialogueLine("Santa",Portrait.Kind.SANTA,"Three presents, a frozen forest,\nand two giant monsters...\nWhat a Christmas!"),new DialogueLine("Santa",Portrait.Kind.SANTA,"Next year, I'm sticking\nto delivering presents.\nNo more saving the world!"));
 default -> throw new IllegalArgumentException(id);};}
 static void shot(GamePanel p,String name)throws Exception {
  String dir=System.getProperty("finalStory.captures");if(dir==null)return;
  Path root=Path.of(dir);Files.createDirectories(root);
  int w=p.getWidth(),h=p.getHeight();
  for(int[] size:new int[][]{{800,600},{1270,720}}){p.setSize(size[0],size[1]);call(p,"updateCamera");ImageIO.write(render(p),"png",root.resolve(name+"-"+size[0]+"x"+size[1]+".png").toFile());}
  p.setSize(w,h);call(p,"updateCamera");
 }
 static void text(Dialogue actual,List<DialogueLine> expected,String label)throws Exception {
  check(actual.getLineCount()==expected.size(),label+" exact page count");
  @SuppressWarnings("unchecked") var lines=(List<DialogueLine>)field(actual,"lines");
  for(int i=0;i<lines.size();i++){
   check(lines.get(i).getText().equals(expected.get(i).getText()),label+" exact wording "+(i+1));
   check(lines.get(i).getSpeakerName().equals(expected.get(i).getSpeakerName()),label+" exact speaker "+(i+1));
   // Existing wrap/font/frame must leave a full line above the footer at both viewports.
   for(int width:new int[]{800,1270}){
    var im=new java.awt.image.BufferedImage(width,720,2);var g=im.createGraphics();g.setFont(new Font("Dialog",Font.PLAIN,16));
    var wrapped=ChristmasUI.wrap(g.getFontMetrics(),lines.get(i).getText(),Math.min(760,width-36)-172);
    int height=Math.max(184,115+(wrapped.size()-1)*23);
    check(75+(wrapped.size()-1)*23<height-35,label+" footer clearance "+width+" page "+i);
    for(String line:wrapped)check(g.getFontMetrics().stringWidth(line)<=Math.min(760,width-36)-172,label+" horizontal wrapping");g.dispose();
   }
  }
 }
 static void pages(GamePanel p,List<DialogueLine> expected,String label)throws Exception {
  var dm=(DialogueManager)field(p,"dialogueManager");check(dm.isActive(),label+" opens through real input");text(dm.getActiveDialogue(),expected,label);
  for(int i=0;i<expected.size();i++){shot(p,label+"-page-"+(i+1));tap(p,KeyEvent.VK_E);}
  check(!dm.isActive(),label+" closes after exact pages");
 }
 static void overlay(GamePanel p,String label,String... expected)throws Exception {
  var overlay=(StoryOverlay)field(p,"storyOverlay");check(overlay.isActive(),label+" active");
  check(field(overlay,"pages").equals(List.of(expected)),label+" exact authored narration");
  for(int i=0;i<expected.length;i++){shot(p,label+"-page-"+(i+1));tap(p,KeyEvent.VK_E);}
  check(!overlay.isActive(),label+" original skip controls");
 }
 public static void main(String[] args)throws Exception {
  boss.BossArtwork.prepareAsync().get();GamePanel p=new GamePanel();p.setSize(1270,720);tap(p,KeyEvent.VK_ENTER);tick(p,6);
  overlay(p,"opening","Christmas Eve has arrived!\n\nSanta has three presents to deliver\nin The Last Village.\n\nJust another peaceful Christmas...\nor so he thinks.");
  for(var site:new WorkshopInteractions.Site[]{WorkshopInteractions.Site.LETTER_DESK,WorkshopInteractions.Site.SPIRIT_LANTERN,WorkshopInteractions.Site.JOURNEY_MAP}){
   SantaBaseStoryInteractionTest.approach(p,site);tap(p,KeyEvent.VK_E);pages(p,approved(site==WorkshopInteractions.Site.LETTER_DESK?"6":site==WorkshopInteractions.Site.SPIRIT_LANTERN?"7":"8"),"base-"+site);
  }
  check(!story(p).isStoryStarted(),"Optional reading never advances main quest");collectOpeningPresents(p);check(!((DialogueManager)field(p,"dialogueManager")).isActive(),"Storage adds no dialogue");travelGate(p,Area.Type.VILLAGE);
  check(field(p,"areaMessage").equals("The letter was right...\nThe Christmas lights are fading.\nI should check on everyone."),"Brief Village arrival exact");shot(p,"village-arrival");
  for(var site:new VillageActivities.Site[]{VillageActivities.Site.IRIS,VillageActivities.Site.CLARA,VillageActivities.Site.BEN}){
   santa(p).setPosition(site.x-16,site.y+4);tap(p,KeyEvent.VK_E);tick(p,GiftDeliveryPresentation.DURATION);
   if(site==VillageActivities.Site.BEN){var dm=(DialogueManager)field(p,"dialogueManager");check(dm.getActiveDialogue().getLineCount()==9,"Ben retains all nine original pages");shot(p,"ben-original");closeDialogue(p);}else pages(p,approved(site==VillageActivities.Site.IRIS?"11":"12"),"delivery-"+site);
  }
  QuestFlowTest.nearToby(p);tap(p,KeyEvent.VK_E);pages(p,approved("14"),"toby-lost-gift");check(story(p).isStoryStarted(),"Lost Gift starts only on close");
  at(santa(p),encounters(p).get("village").getCenter());tick(p,1);clearWave(p);travelGate(p,Area.Type.FOREST);
  at(santa(p),WorldMap.forArea(Area.Type.FOREST).point("items/Toby Gift"));tap(p,KeyEvent.VK_E);travelGate(p,Area.Type.VILLAGE);QuestFlowTest.nearToby(p);tap(p,KeyEvent.VK_E);pages(p,approved("15"),"toby-gift-return");check(story(p).isLostGiftComplete(),"Original gift return commits");
  collectMaterials(p);restoreVillage(p);QuestFlowTest.nearToby(p);tap(p,KeyEvent.VK_E);pages(p,approved("16"),"toby-restored-village");check(story(p).isCrystalQuestActive(),"Investigation still commits only after dialogue");
  travelGate(p,Area.Type.FOREST);for(int i=1;i<=3;i++)story(p).recordCrystalDestroyed("crystal-"+i);
  at(santa(p),boss.FrostFang.ARENA);tick(p,61);
  overlay(p,"fang-intro","The final crystal shatters.\nA terrifying roar echoes through the forest.\nFrost Fang awakens, blocking the road\nto Christmas Town!","Beware its deadly claws and icy breath!\nDodge its marked attacks,\nthen strike when it leaves itself open.");clearWave(p);check(story(p).isFrostFangDefeated()&&!story(p).isBossDefeated(),"Fang still opens Town only");
  travelGate(p,Area.Type.CHRISTMAS_TOWN);var elias=(Elias)field(p,"elias");at(santa(p),new WorldMap.Point(elias.getX()+16,elias.getY()+24));tap(p,KeyEvent.VK_E);
  var initial=new java.util.ArrayList<>(approved("18"));initial.add(new DialogueLine("Elias",Portrait.Kind.ELIAS,"The road ahead is still dangerous.\nOnce it's clear, reach the Spirit Tree\nand free its guardian."));pages(p,initial,"elias-road-danger");
  at(santa(p),encounters(p).get("town").getCenter());tick(p,1);clearWave(p);
  // Exercise the alternate first-conversation selection against the same authoritative cleared encounter.
  at(santa(p),new WorldMap.Point(elias.getX()+16,elias.getY()+24));var dm=(DialogueManager)field(p,"dialogueManager");check(dm.startFor(elias,santa(p),false,false,encounters(p).isComplete("town")),"Cleared-road first-conversation branch");
  var cleared=new java.util.ArrayList<>(approved("18"));cleared.add(new DialogueLine("Elias",Portrait.Kind.ELIAS,"The infected on the road are gone.\nReach the Spirit Tree\nand free its guardian."));pages(p,cleared,"elias-road-clear");
  at(santa(p),(WorldMap.Point)call(p,"spiritTreeInteractionPoint"));tick(p,61);
  overlay(p,"titan-intro","Once the protector of the Spirit Tree,\nDark Titan now stands consumed\nby Black Frost.\nThe fallen guardian blocks\nSanta's final path.","To save the guardian, Santa must break\nBlack Frost's hold.\nDodge its devastating attacks,\nthen strike when it leaves itself open.");clearWave(p);
  at(santa(p),(WorldMap.Point)call(p,"spiritTreeInteractionPoint"));tap(p,KeyEvent.VK_E);check(((SpiritTreeTransformation)field(p,"treeTransformation")).isActive(),"Tree still requires actual E");tick(p,179);check(!((SpiritTreeTransformation)field(p,"treeTransformation")).isComplete(),"Restoration not early");tick(p,1);shot(p,"christmas-saved");completeCelebration(p);tick(p,EndingCutscene.CAMERA_RETURN_DURATION);
  at(santa(p),JourneyHomeVisuals.SLEIGH_APPROACH);tap(p,KeyEvent.VK_E);modalClick(p,180,427);shot(p,"return-home-line-1");tick(p,95);shot(p,"return-home-line-2");check(ending(p).getFrames()<EndingCutscene.SLEIGH_MOVE,"Both captions fit unchanged boarding");tick(p,EndingCutscene.SLEIGH_DURATION-ending(p).getFrames());check(chapter(p).homeReturned(),"Original360-update homecoming");
  for(var site:new WorkshopInteractions.Site[]{WorkshopInteractions.Site.SPIRIT_LANTERN,WorkshopInteractions.Site.JOURNEY_MAP}){SantaBaseStoryInteractionTest.approach(p,site);tap(p,KeyEvent.VK_E);pages(p,approved(site==WorkshopInteractions.Site.SPIRIT_LANTERN?"24":"25"),"postgame-"+site);}
  openLetters(p);shot(p,"letters-original");finishHomecoming(p);tick(p,60);shot(p,"ending-original-title");tap(p,KeyEvent.VK_E);shot(p,"ending-original-message");tap(p,KeyEvent.VK_E);
  check(ending(p).getState()==EndingCutscene.State.CREDITS,"Original ending leads to credits");
  for(int t:new int[]{150,480,800,1200,1600,2000,2200}){tick(p,t-ending(p).getFrames());shot(p,"credits-"+t);}
  tick(p,2400-ending(p).getFrames());check(ending(p).isComplete(),"Credits still finish at2400active updates");
  p.beginAdventure();closeOverlay(p);check(!chapter(p).homeReturned(),"Fresh Start clears authoritative homecoming");SantaBaseStoryInteractionTest.approach(p,WorkshopInteractions.Site.SPIRIT_LANTERN);tap(p,KeyEvent.VK_E);pages(p,approved("7"),"fresh-start-lantern");p.stopGame();
  System.out.println("FinalStoryTest PASS "+checks+" exact dialogue, layout, conditional state and full narrative route assertions");
 }
}
