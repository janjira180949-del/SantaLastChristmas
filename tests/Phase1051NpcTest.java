import game.*;
import entity.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Alpha-only extraction, preserved body samples/scale, actual renderer and immutable gameplay guards. */
public class Phase1051NpcTest extends V2TestSupport {
 static BufferedImage image(Path p)throws Exception{return ImageIO.read(p.toFile());}
 static void inventory()throws Exception{
  var allowed=Phase1051PreservationSupport.allowed();var changed=new HashSet<String>();
  var entries=java.util.regex.Pattern.compile("\"([^\"]+)\":\\s*\"([a-f0-9]{64})\"").matcher(Files.readString(Path.of("docs/PHASE10_5_1_BASELINE.json")));
  int toby=0;
  while(entries.find()){
   String p=entries.group(1),prior=entries.group(2);byte[] bytes=Phase106PreservationSupport.previous(p,Files.readAllBytes(Path.of(p)));String actual=ForestPreservationSupport.hash(bytes);
   check(actual.equals(allowed.getProperty(p,prior)),"Protected original or exact NPC-only revision "+p);
   check(ForestPreservationSupport.hash(Phase1051PreservationSupport.previous(p,bytes)).equals(prior),"Authenticated task-start bytes retained "+p);
   if(!actual.equals(prior))changed.add(p);
   if(p.startsWith("assets/characters/children/toy/")){toby++;check(actual.equals(prior),"Toby asset byte-identical "+p);}
  }
  check(changed.equals(allowed.stringPropertyNames())&&changed.size()==6,"Only two presentation owners and four historical guard migrations changed");
  check(toby==7,"All three Toby poses and four portraits protected");
  for(String p:Phase1051PreservationSupport.assets().stringPropertyNames())check(ForestPreservationSupport.hash(Files.readAllBytes(Path.of(p))).equals(Phase1051PreservationSupport.assets().getProperty(p)),"Exact derived NPC asset "+p);
 }
 static void art()throws Exception{
  var atlas=image(Path.of("assets/characters/village/residents_atlas.png"));
  for(var site:VillageActivities.Site.values())if(site.resident()){
   var crop=atlas.getSubimage(site.ordinal()*362,350,362,374);int l=362,t=374,r=-1,b=-1;
   for(int y=0;y<374;y++)for(int x=0;x<362;x++)if((crop.getRGB(x,y)>>>24)>24){l=Math.min(l,x);t=Math.min(t,y);r=Math.max(r,x);b=Math.max(b,y);}
   var clean=image(Path.of("assets/phase10_5_1/residents/"+site.name().toLowerCase(Locale.ROOT)+".png"));
   check(clean.getWidth()==r-l+1&&clean.getHeight()==b-t+1&&clean.getColorModel().hasAlpha(),"Original exact RGBA canvas "+site);
   int removed=0,retained=0;for(int y=0;y<clean.getHeight();y++)for(int x=0;x<clean.getWidth();x++){
    int old=crop.getRGB(x+l,y+t),now=clean.getRGB(x,y);
    check(now==old||now==0,"Only unwanted alpha cleared; no recoloring or reconstruction "+site);
    if((now>>>24)>24)retained++;
    if((old>>>24)>24&&(now>>>24)==0)removed++;
    if(y<20)check((now>>>24)==0,"Portrait-row fragment removed "+site);
    if(y>=30&&(old>>>24)>100)check(now==old,"All opaque body/detail/outline pixels preserved "+site);
   }
   check(retained>40000&&removed>2000,"Substantial body retained and confirmed portrait fragment removed "+site);
   for(int y=clean.getHeight()-8;y<clean.getHeight();y++)for(int x=0;x<clean.getWidth();x++)if((crop.getRGB(x+l,y+t)>>>24)>24)check(clean.getRGB(x,y)==crop.getRGB(x+l,y+t),"Feet source samples/ground anchor preserved "+site);
  }
  TownCelebration.prepareAsync().get();var f=TownCelebration.class.getDeclaredField("ART");f.setAccessible(true);
  var poses=(BufferedImage[][])((java.util.concurrent.CompletableFuture<?>)f.get(null)).get();
  for(int i=0;i<6;i++)for(int j=0;j<19;j++){
   var before=image(Path.of("build/phase10-5-1/before/poses/"+i+"-"+j+".png"));var now=poses[i][j];
   check(now.getWidth()==before.getWidth()&&now.getHeight()==before.getHeight(),"Celebration pose canvas preserved");
   int bodyTop=70;for(int y=0;y<70;y++)for(int x=0;x<64;x++)if((now.getRGB(x,y)>>>24)>24)bodyTop=Math.min(bodyTop,y);
   int changes=0;for(int y=0;y<70;y++)for(int x=0;x<64;x++){
    int old=before.getRGB(x,y),n=now.getRGB(x,y);if(n!=old){changes++;check((n>>>24)==0&&(y<bodyTop||(old>>>24)<=24),"Only detached strip above body or low-alpha noise removed; unchanged gesture/body: "+i+"/"+j+" @"+x+","+y+" old alpha "+(old>>>24));}
   }check(changes>0,"Cleanup reused in every celebration pose "+i+"/"+j);
  }
 }
 static void renders()throws Exception{
  Path before=Path.of("build/phase10-5-1/before"),after=Path.of("build/phase10-5-1/after");
  for(int area=0;area<4;area++){
   var old=image(before.resolve("neutral-"+area+".png"));var now=ForestLandmarkPreservationTest.raw(area);
   check(Phase105PreservationSupport.equal(old,Phase1051PreservationSupport.frame(area,now)),"All world pixels outside exact NPC cleanup preserved "+area);
  }
  int changedPairs=0,unchangedToby=0;
  for(Path p:Files.list(before).filter(p->p.toString().endsWith(".png")).toList()){
   var a=image(p);var b=image(after.resolve(p.getFileName()));boolean equal=Phase105PreservationSupport.equal(a,b);
   if(p.getFileName().toString().startsWith("Toby")){
    var camera=new Camera();camera.follow(1381,1008,a.getWidth(),a.getHeight(),2560,1920);
    for(int y=952;y<1034;y++)for(int x=1256;x<1326;x++)check(a.getRGB(x-camera.getX(),y-camera.getY())==b.getRGB(x-camera.getX(),y-camera.getY()),"Matched Toby body/name/anchor pixels unchanged "+p);
    unchangedToby++;
   }
   else if(!p.getFileName().toString().startsWith("neutral")){check(!equal,"Actual rendered edge correction visible "+p);changedPairs++;}
  }
  check(unchangedToby==12&&changedPairs==76,"Both viewport sizes, six residents/light-dark and 26 celebration animation frames reviewed");
  var p=playing();check(p.getChildren().get(0).getName().equals("Toby")&&p.getChildren().get(0).getX()==1281&&p.getChildren().get(0).getY()==987,"Toby identity and plaza actor unchanged");
  completeOpeningDeliveries(p);check(story(p).getStage()==quest.StoryQuestManager.Stage.MEET_TOBY,"Unchanged real delivery dialogues and quest progression through Iris, Clara and Ben");p.stopGame();
 }
 public static void main(String[] args)throws Exception{inventory();art();renders();System.out.println("PASS: "+checks+" NPC alpha, body, anchor, gesture, actual renderer and protected gameplay assertions");}
}
