import boss.*;
import entity.*;
import game.*;
import ui.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Measured visible size, anchored animation, exact combat clocks and narrative preservation. */
public class Phase101BossPolishTest extends V2TestSupport {
 static Rectangle alpha(BufferedImage im){int l=im.getWidth(),t=im.getHeight(),r=-1,b=-1;for(int y=0;y<im.getHeight();y++)for(int x=0;x<im.getWidth();x++)if((im.getRGB(x,y)>>>24)>=16){l=Math.min(l,x);t=Math.min(t,y);r=Math.max(r,x);b=Math.max(b,y);}return new Rectangle(l,t,r-l+1,b-t+1);}
 static String method(String source,String start,String end){return source.substring(source.indexOf(start),source.indexOf(end,source.indexOf(start))).replace("\r\n","\n");}
 public static void main(String[] args)throws Exception{
  BossArtwork.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS);
  var santa=ImageIO.read(Path.of("assets/characters/santa/idle/santa_idle_down_01.png").toFile());
  int sh=alpha(util.PixelArt.scale(santa,64,64)).height;check(sh==55,"Measured actual Santa idle silhouette, not canvas height");
  int min=999,max=0;var names=java.util.List.of("idle","walk","run","claw_swipe","ice_blast","frost_wave","frost_dash","hurt","entrance","defeat");
  for(String name:names){var unique=new HashSet<String>();
   var sheet=ImageIO.read(Path.of("assets/bosses/frost_fang/"+name+"_sheet.png").toFile());
   check(sheet.getWidth()==1024&&sheet.getHeight()==512,"Eight frame,4x2 sheet "+name);
   for(int i=0;i<8;i++){
    Path file=Path.of(String.format("assets/bosses/frost_fang/%s/frame_%02d.png",name,i+1));var source=ImageIO.read(file.toFile());unique.add(ForestPreservationSupport.hash(Files.readAllBytes(file)));
    var frame=BossArtwork.frame("fang",name,i);check(frame.getWidth()==269&&frame.getHeight()==269,"Uniform cached canvas "+name+i);
    Rectangle a=alpha(source),b=alpha(frame);double scale=269.0/256;
    check(Math.abs(b.width-a.width*scale)<=2&&Math.abs(b.height-a.height*scale)<=2,"Uniform aspect-preserving scaling "+name+i);
    check(a.x>0&&a.y>0&&a.x+a.width<256&&a.y+a.height<=240,"No clipping; pinned ground envelope "+name+i);
    if(name.equals("idle")){double ratio=b.height/(double)sh;check(ratio>=2.5&&ratio<=3,"Actual visible Fang/Santa ratio "+ratio);min=Math.min(min,b.height);max=Math.max(max,b.height);}
    var target=new BufferedImage(600,600,BufferedImage.TYPE_INT_ARGB);var g=target.createGraphics();BossArtwork.drawFang(g,name,i,300,420,false);g.dispose();var left=alpha(target);
    check(left.y+left.height<=421,"Ground anchor never drops below feet "+name+i);
    if(name.equals("idle")||name.equals("walk")||name.equals("run"))check(Math.abs(left.y+left.height-420)<=1,"Planted foot contact stable "+name+i);
    target=new BufferedImage(600,600,BufferedImage.TYPE_INT_ARGB);g=target.createGraphics();BossArtwork.drawFang(g,name,i,300,420,true);g.dispose();var right=alpha(target);
    check(left.y==right.y&&left.height==right.height&&left.width==right.width,"Facing preserves vertical anchor and dimensions "+name+i);
   }
   check(unique.size()>=6,"Distinct update-driven frames "+name);
   try(var input=ImageIO.createImageInputStream(Path.of("assets/bosses/frost_fang/"+name+"_preview.gif").toFile())){var r=ImageIO.getImageReaders(input).next();r.setInput(input);check(r.getNumImages(true)==8,"Eight-frame preview "+name);r.dispose();}
  }
  var fang=new FrostFang(200,200);check(fang.getSize()==76&&fang.getBounds().getWidth()==76&&fang.getMaximumHealth()==900,"Visual enlargement preserves76 hitbox/900HP");
  int[] warnings={36,45,60,54,50,60,45,60,72},active={12,48,36,20,40,90,25,45,120},recovery={36,48,48,48,48,48,35,48,48};
  for(int i=0;i<9;i++){
   var kind=BossAttacks.Attack.values()[i+1];Enemy actor=i<4?new FrostFang(200,200):new DarkTitan(200,200);var boss=(BossCombatant)actor;var player=new Santa(2000,2000);
   boss.attacks().begin(kind,actor,player,java.util.List.of());check(boss.attacks().getWarningFrames()==warnings[i]&&boss.attacks().getTotalFrames()==warnings[i]+active[i]+recovery[i],"Exact original timing "+kind);
   for(int t=0;t<warnings[i]-1;t++)boss.attacks().update(actor,player,java.util.List.of());check(!boss.attacks().isExecuting()&&player.getHealth()==100,"Safe anticipation "+kind);
   boss.attacks().update(actor,player,java.util.List.of());check(boss.attacks().isExecuting(),"Execution starts on same update "+kind);
   for(int t=0;t<active[i];t++)boss.attacks().update(actor,player,java.util.List.of());check(boss.attacks().isRecovering(),"Exact recovery start "+kind);
   for(int t=0;t<recovery[i];t++)boss.attacks().update(actor,player,java.util.List.of());check(boss.attacks().getAttack()==BossAttacks.Attack.NONE,"Exact attack lifetime "+kind);
  }
  String attacks=Files.readString(Path.of("src/boss/BossAttacks.java")),old=new String(Phase10PreservationSupport.archive("src/boss/BossAttacks.java","PHASE10_1"),java.nio.charset.StandardCharsets.UTF_8);
  check(method(attacks,"    public void begin(","    private void effect(").equals(method(old,"    public void begin(","    /** Texture-based")),"All attack targeting,collision,damage and update code stays byte-exact");
  String current=Files.readString(Path.of("src/boss/FrostFang.java"));old=new String(Phase10PreservationSupport.archive("src/boss/FrostFang.java","PHASE10_1"),java.nio.charset.StandardCharsets.UTF_8);
  check(method(current,"    @Override public boolean takeDamage", "    public String getAnimationName").equals(method(old,"    @Override public boolean takeDamage", "    public String getAnimationName")),"Fang damage/phase/AI/movement remains exact");
  var dialogue=new DialogueManager();var elias=new Elias(0,0);var player=new Santa(0,0);check(dialogue.startFor(elias,player,false,false),"Actual Elias dialogue still opens");dialogue.advance();check(dialogue.getActiveDialogue().getCurrentLine().getText().equals("The Dark Titan?\nPlease tell me that's just\na dramatic nickname."),"Town dialogue names its actual guardian");
  EndingCutscene ending=new EndingCutscene();var dialogues=(Dialogue[])field(ending,"lines");check(dialogues.length==7&&dialogues[2].getCurrentLine().getSpeakerName().equals("Dark Titan")&&dialogues[2].getCurrentLine().getText().equals("I can finally put it down."),"Final farewell retains structure and meaning, corrected speaker");
  var overlay=new StoryOverlay();overlay.startBoss("The guardian awaits.");check(field(overlay,"bossTitle").equals("DARK TITAN"),"Town legacy introduction has contextual final-boss name");
  for(int width:new int[]{800,1270}){var bounds=HUD.bossBounds(width);check(!bounds.intersects(new Rectangle(0,0,250,110))&&!bounds.intersects(new Rectangle(width-250,0,250,110))&&bounds.y>=35&&bounds.y+bounds.height<=110,"Boss card clears HP,Spirit,area name and enlarged attack silhouettes "+width);}
  for(Path path:Files.walk(Path.of("src")).filter(p->p.toString().endsWith(".java")).toList())if(!path.endsWith("FrostFang.java"))check(!Files.readString(path).toLowerCase(Locale.ROOT).contains("christmas eater"),"No obsolete visible name "+path);
  check(Files.exists(Path.of("src/boss/ChristmasEater.java")),"Historical class retained");
  var baseline=java.util.regex.Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/PHASE10_1_BASELINE.json")));
  var approved=Phase101PreservationSupport.allowed();var changed=new HashSet<String>();
  while(baseline.find()){
   String path=baseline.group(1),hash=ForestPreservationSupport.hash(Phase102BEPreservationSupport.previous(path,Files.readAllBytes(Path.of(path))));
   if(!hash.equals(baseline.group(2)))changed.add(path);
   check(hash.equals(baseline.group(2))||hash.equals(approved.getProperty(path)),"Complete exact Phase10 baseline guard "+path);
  }
  for(String path:approved.stringPropertyNames()){
   check(Files.exists(Path.of(path))&&ForestPreservationSupport.hash(Phase102BEPreservationSupport.previous(path,Files.readAllBytes(Path.of(path)))).equals(approved.getProperty(path)),"Authenticated revision or addition "+path);
  }
  System.out.println("PASS: "+checks+" polish assertions; Santa="+sh+"px, Fang idle="+min+".."+max+"px, ratio="+min/(double)sh+".."+max/(double)sh);
 }
}
