import game.*;import entity.*;import audio.*;import java.awt.*;import java.awt.event.*;import java.nio.file.*;import java.util.*;
/** Actual Town input, single presentation ownership and authoritative departure edges. */
public class TownFinalFixTest extends JourneyHomeTest {
 static boolean same(java.awt.image.BufferedImage a,java.awt.image.BufferedImage b){return Arrays.equals(a.getRGB(0,0,a.getWidth(),a.getHeight(),null,0,a.getWidth()),b.getRGB(0,0,b.getWidth(),b.getHeight(),null,0,b.getWidth()));}
 static boolean keeperAt(java.awt.image.BufferedImage scene,Elias actor,int feetX,int feetY,Elias.Direction facing){
  var stamp=new java.awt.image.BufferedImage(64,64,java.awt.image.BufferedImage.TYPE_INT_ARGB);var g=stamp.createGraphics();actor.drawCinematic(g,32,60,facing);g.dispose();int pixels=0,matched=0;
  for(int y=0;y<64;y++)for(int x=0;x<64;x++){int pixel=stamp.getRGB(x,y);if((pixel>>>24)<250)continue;int sx=feetX-32+x,sy=feetY-60+y;if(sx<0||sy<0||sx>=scene.getWidth()||sy>=scene.getHeight())continue;pixels++;int actual=scene.getRGB(sx,sy),distance=0;for(int shift:new int[]{0,8,16})distance+=Math.abs((pixel>>shift&255)-(actual>>shift&255));if(distance<=16)matched++;}
  return pixels>300&&matched>pixels*.8;
 }
 static void press(GamePanel p,int key,int frames)throws Exception{p.keyPressed(new KeyEvent(p,KeyEvent.KEY_PRESSED,0,0,key,KeyEvent.CHAR_UNDEFINED));tick(p,frames);p.keyReleased(new KeyEvent(p,KeyEvent.KEY_RELEASED,0,0,key,KeyEvent.CHAR_UNDEFINED));}
 static void elias()throws Exception{
  GamePanel p=playing();try{townFixture(p);var e=(Elias)field(p,"elias");var map=WorldMap.forArea(Area.Type.CHRISTMAS_TOWN);var point=map.point("npc/Elias");
   check(point.equals(new WorldMap.Point(1776,1392))&&e.getX()==1760&&e.getY()==1368,"One authoritative deep Town marker and actor anchor");
   check(!util.Collision.intersectsAny(e.getBounds(),map.solids()),"Full keeper footprint remains clear");
   check(!BossArenaVisuals.arenaBounds().contains(point.x(),point.y())&&Math.hypot(point.x()-1296,point.y()-1008)>500,"Keeper outside arena and away from Spirit Tree");
   check(Math.hypot(point.x()-1296,point.y()-80)>1300,"Keeper materially farther from northern entrance");
   // The existing horizontal paved market route is open in both directions, using real held input.
   santa(p).setPosition(1590,1368);int x=santa(p).getX();press(p,KeyEvent.VK_D,32);check(santa(p).getX()==x+128,"Actual movement along market road reaches keeper");
   check(!santa(p).getBounds().intersects(e.getBounds()),"Walked approach does not overlap keeper");
   check(e.canInteract(santa(p)),"Unchanged58-pixel interaction range at walked approach");
   check(((QuestGuidance.Objective)call(p,"currentObjective")).target().equals(point),"Guidance follows authoritative marker");
   tap(p,KeyEvent.VK_E);check(((ui.DialogueManager)field(p,"dialogueManager")).isActive()&&!story(p).isBossQuestAccepted(),"E opens original dialogue before accepting quest");closeDialogue(p);
   check(story(p).isBossQuestAccepted(),"Original final-boss prerequisite commits at dialogue close");
   x=santa(p).getX();press(p,KeyEvent.VK_A,32);check(santa(p).getX()==x-128,"Same paved route traverses back");
   p.beginAdventure();e=(Elias)field(p,"elias");check(e.getX()==1760&&e.getY()==1368,"Fresh Start retains authored keeper placement");
  }finally{p.stopGame();}
 }
 static void speech()throws Exception{
  for(int t=0;t<=120;t++)check(TownCelebration.speechOpacity(t)==1&&TownCelebration.speechText(t).equals("A warm home again!"),"Full120-update readable period "+t);
  float last=1;for(int t=121;t<144;t++){float now=TownCelebration.speechOpacity(t);check(now>0&&now<last,"Smooth monotonic fade "+t);last=now;}
  for(int t=144;t<204;t++)check(TownCelebration.speechOpacity(t)==0,"Natural silent gap "+t);
  for(int i=0;i<16;i++)check(!TownCelebration.speechText(i*204).equals(TownCelebration.speechText((i+1)*204)),"Adjacent messages never repeat identical text");
  GamePanel p=endingFixture();try{tick(p,180);check(ending(p).isCelebration(),"Tree still takes180 active updates");tick(p,160);
   check(TownCelebration.names().equals(java.util.List.of("Martha","Oliver","Noah","Iris","Clara","Ben"))&&p.getChildren().size()==1&&p.getChildren().get(0).getName().equals("Toby"),"Seven existing Village residents plus cinematic Elias");
   check(JourneyHomeVisuals.ELIAS_FEET.equals(new WorldMap.Point(1240,1220)),"Single keeper is separated from Ben without moving any Village resident");
   p.setSize(2560,1920);call(p,"updateCamera");var actor=(Elias)field(p,"elias");var full=render(p);
   check(keeperAt(full,actor,JourneyHomeVisuals.ELIAS_FEET.x(),JourneyHomeVisuals.ELIAS_FEET.y(),Elias.Direction.RIGHT),"Actual full-world renderer contains approved cinematic keeper exactly at gathered anchor");
   check(!keeperAt(full,actor,actor.getX()+16,actor.getY()+48,Elias.Direction.DOWN)&&!keeperAt(full,actor,1376,443,Elias.Direction.DOWN),"No second permanent keeper at current or retired exploration position during celebration");
   int clock=ending(p).getVisualFrames();tick(p,1);check(ending(p).getVisualFrames()==clock+1,"Ambient clock advances on updates only");render(p);render(p);check(ending(p).getVisualFrames()==clock+1,"Paint cannot advance speech");
   completeCelebration(p);clock=ending(p).getVisualFrames();tick(p,1);check(ending(p).getVisualFrames()==clock+1,"Clock continues without restart after gratitude");
   check(keeperAt(render(p),actor,actor.getX()+16,actor.getY()+48,Elias.Direction.DOWN),"Ordinary Town restores approved permanent Elias sprite at deep location");
   tap(p,KeyEvent.VK_ESCAPE);clock=ending(p).getVisualFrames();tick(p,120);check(ending(p).getVisualFrames()==clock,"Pause freezes ambient lifetime");tap(p,KeyEvent.VK_ESCAPE);
   p.beginAdventure();check(ending(p).getVisualFrames()==0&&!chapter(p).celebrated(),"Fresh Start resets ambient sequence and roster state");
  }finally{p.stopGame();}
 }
 static void departureAudio()throws Exception{
  for(boolean skip:new boolean[]{false,true}){
   GamePanel p=endingFixture();try{tick(p,180);completeCelebration(p);var audio=(AudioManager)field(p,"audio");var log=new ArrayList<String>();audio.setEventObserver(log::add);
    at(santa(p),JourneyHomeVisuals.SLEIGH_APPROACH);tap(p,KeyEvent.VK_E);check(window(p).mode()==ui.VillageActivityUI.Mode.SLEIGH,"Existing sleigh approach opens confirmation");modalClick(p,180,427);
    check(ending(p).getState()==EndingCutscene.State.SLEIGH_DEPARTURE&&ending(p).getFrames()==0,"Original departure starts at0");
    check(Collections.frequency(log,"sleigh.board")==1&&!log.contains("sleigh.laugh"),"One gentle boarding cue at0");
    check(audio.getRequestedMusic()==AudioCatalog.Music.SLEIGH,"Existing ending soundtrack continues");
    tick(p,EndingCutscene.SLEIGH_BELLS_START-1);check(!log.contains("sleigh.bells.move"),"No bells before preparation");tick(p,1);check(Collections.frequency(log,"sleigh.bells.move")==1,"Preparation bells follow authoritative visual stage");
    tick(p,EndingCutscene.SLEIGH_TAKEOFF-EndingCutscene.SLEIGH_BELLS_START-1);check(!log.contains("sleigh.laugh"),"No premature laughter");tick(p,1);check(Collections.frequency(log,"sleigh.laugh")==1,"One laugh at actual takeoff");
    if(skip){ending(p).dismissPresentation();tick(p,1);check(!ending(p).hasStarted(),"Presentation cancellation returns control");}
    else{tick(p,EndingCutscene.SLEIGH_DURATION-EndingCutscene.SLEIGH_TAKEOFF-1);check(!chapter(p).homeReturned(),"No premature home transition359");tick(p,1);check(chapter(p).homeReturned()&&areas(p).getCurrentArea().getType()==Area.Type.SANTA_BASE,"Authoritative360-update return home");}
    check(Collections.frequency(log,"sleigh.board")==1&&Collections.frequency(log,"sleigh.bells.move")== (skip?2:3)&&Collections.frequency(log,"sleigh.laugh")==1,"Finite bell extensions; exactly one laugh and no per-frame repetition");
    int n=log.size();tick(p,20);check(log.subList(n,log.size()).stream().noneMatch(id->id.startsWith("sleigh.")),"No stale departure events after exit");
    p.beginAdventure();check(!chapter(p).homeReturned()&&!ending(p).hasStarted(),"Fresh Start clears ending lifecycle");
   }finally{p.stopGame();}
  }
  for(String id:java.util.List.of("sleigh.board","sleigh.bells.move","sleigh.laugh")){
   var cue=AudioCatalog.cue(id);check(cue!=null&&!cue.gameplay()&&!cue.loop(),"Owned finite cinematic cue "+id);byte[] pcm=AudioResources.decode(cue.path());check(pcm.length>44100/4&&pcm.length%4==0,"Actual Java Sound PCM decode "+id);
  }
  check(AudioManager.CACHE_LIMIT==128L*1024*1024,"Mixer budget unchanged");
 }
 static void ownedMixer()throws Exception{
  var sink=new Phase106CBossAudioTest.StepSink();byte[] signal=new byte[44100*4*3];for(int i=0;i<signal.length;i+=4){signal[i]=(byte)232;signal[i+1]=3;signal[i+2]=(byte)232;signal[i+3]=3;}
  var manager=new AudioManager(()->sink,path->signal,null);var events=new ArrayList<String>();manager.setEventObserver(events::add);
  try{
   manager.setMasterVolume(100);manager.setMusicVolume(100);manager.setSfxVolume(100);manager.playMusic(AudioCatalog.Music.SLEIGH);manager.start();Phase106AudioTest.await(()->sink.blocks.get()==1&&manager.getDecodedCount()>=77,"New finite SFX prepared through existing loader");sink.steps(101);
   var owner=new AdventureAudio(manager);var village=new VillageActivities();var story=new quest.StoryQuestManager();var ending=new EndingCutscene();var tree=new SpiritTreeTransformation();var santa=new Santa(2000,1500);var ui=new ui.VillageActivityUI();
   manager.beginUpdate();owner.sync(GamePanel.State.PLAYING,Area.Type.CHRISTMAS_TOWN,santa,story,village,ending,tree,null,false,false,ui,"",java.util.List.of(),null);
   ending.startSleighDeparture();manager.beginUpdate();owner.sync(GamePanel.State.ENDING,Area.Type.CHRISTMAS_TOWN,santa,story,village,ending,tree,null,false,false,ui,"",java.util.List.of(),null);sink.step();
   check(events.contains("sleigh.board")&&manager.getActiveVoices()>0,"Boarding requests audible nonzero PCM through shared mixer");
   for(int t=1;t<=EndingCutscene.SLEIGH_TAKEOFF;t++){ending.update(new spirit.SpiritTree());manager.beginUpdate();owner.sync(GamePanel.State.ENDING,Area.Type.CHRISTMAS_TOWN,santa,story,village,ending,tree,null,false,false,ui,"",java.util.List.of(),null);}
   sink.step();check(events.contains("sleigh.bells.move")&&events.contains("sleigh.laugh")&&manager.getActiveVoices()>=3&&manager.getActiveVoices()<=8,"Movement bells and one laugh overlap within existing eight voices");
   manager.setSfxVolume(0);sink.step();check(sink.sample>0,"SFX silence preserves ending music");manager.setMuted(true);sink.step();check(sink.sample==0,"Master mute includes bells and laughter");manager.setMuted(false);manager.setSfxVolume(100);
   ending.dismissPresentation();manager.beginUpdate();owner.sync(GamePanel.State.PLAYING,Area.Type.SANTA_BASE,santa,story,village,ending,tree,null,false,false,ui,"",java.util.List.of(),null);sink.step();check(manager.getActiveVoices()==0,"Departure exit/home cancels active and pending cinematic voices");
   owner.reset();sink.step();check(manager.getActiveVoices()==0&&manager.getPeakRetainedBytes()<=AudioManager.CACHE_LIMIT,"Fresh Start cancellation and128MiB budget retained");
  }finally{manager.close();}
 }
 public static void main(String[] args)throws Exception{elias();speech();departureAudio();ownedMixer();System.out.println("TownFinalFixTest PASS "+checks+" assertions");}
}
