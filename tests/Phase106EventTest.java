import audio.*;
import boss.*;
import entity.*;
import game.*;
import java.util.*;

/** Dedicated boss warnings/execution/recovery are observed from actual attack clocks. */
public class Phase106EventTest extends V2TestSupport {
 public static void main(String[] args)throws Exception{
  var events=new ArrayList<String>();var stopped=new ArrayList<String>();
  var observer=new GameplayAudio(e->{});observer.bindEvents(events::add,(id,owner)->events.add(id),stopped::add);
  Santa santa=new Santa(500,400);santa.toggleDeveloperTestMode();
  String[] ids={"fang.claw","fang.ice_blast","fang.frost_wave","fang.frost_dash","titan.slam","titan.spikes","titan.charge","titan.beam","titan.storm"};
  int[] warnings={36,45,60,54,50,60,45,60,72};
  var attacks=BossAttacks.Attack.values();
  for(int i=1;i<attacks.length;i++){
   Enemy enemy=i<=4?new FrostFang(200,200):new DarkTitan(200,200);var boss=(BossCombatant)enemy;var a=boss.attacks();events.clear();stopped.clear();
   observer.captureCombat(santa,List.of(enemy));a.begin(attacks[i],enemy,santa,List.of());observer.resolveCombat();
   String id=ids[i-1];check(events.contains(id+".warning"),"Warning on actual begin "+id);
   int expected=warnings[i-1],executeAt=-1,recoveryAt=-1;
   for(int tick=1;tick<a.getTotalFrames();tick++){
    observer.captureCombat(santa,List.of(enemy));a.update(enemy,santa,List.of());observer.resolveCombat();
    if(executeAt<0&&events.contains(id+".execute"))executeAt=tick;
    if(recoveryAt<0&&a.isRecovering())recoveryAt=tick;
   }
   check(executeAt==expected,"Execution audio uses exact warning clock "+id);
   check(recoveryAt>executeAt,"Real recovery preserved "+id);
   check(events.stream().filter(e->e.equals(id+".warning")).count()==1&&events.stream().filter(e->e.equals(id+".execute")).count()==1,"No repeated warning/execution each update "+id);
   for(String event:events)check(AudioCatalog.cue(event)!=null,"Every observed event has valid runtime file "+event);
   if(i==7)check(events.contains("titan.charge.impact"),"Charge impact at actual launch end");
   if(i==8)check(events.contains("titan.beam.sustain")&&events.contains("titan.beam.end")&&stopped.contains("boss-attack"),"Beam owner stops on recovery");
   if(i==9)check(events.contains("titan.storm.sustain")&&events.contains("titan.storm.end")&&stopped.contains("boss-attack"),"Storm owner stops on recovery");
  }
  var p=playing();var manager=(AudioManager)field(p,"audio");events.clear();manager.setEventObserver(events::add);
  try{
   for(int i=0;i<3;i++){tick(p,1);manager.playEffect(AudioCatalog.Effect.DASH);}
   check(events.stream().filter(e->e.startsWith("santa.dash.v")).distinct().count()==3,"Accepted actions rotate all three real dash takes");
   forestFixture(p);tick(p,1);
   FinalChapterTest.collectMaterials(p);
   check(events.contains("pickup.spirit")&&events.contains("pickup.wood")&&events.contains("pickup.ornament"),"Actual nine-material inputs use all three kind cues");
   check(events.contains("materials.category_ready")&&events.contains("materials.all_ready"),"Actual collection milestone edges");
  }finally{p.stopGame();}
  System.out.println("Phase106EventTest PASS "+checks+" assertions");
 }
}
