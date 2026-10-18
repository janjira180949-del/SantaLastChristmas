import game.*;import quest.*;import util.ImageAssets;import java.awt.image.*;import java.nio.file.*;import java.security.*;import java.util.*;
public class QuestTerrainRepairTest {
 static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 public static void main(String[]args)throws Exception{
  
  var forest=ImageAssets.load("assets/world/forest_clean.png");var mask=PackedSnowPath.createOverlay(forest);int pixels=0;
  for(var target:new StoryQuestManager().getTargets()){
   var bounds=target.getBounds();int height=128;int ox=(int)bounds.getCenterX()-48,oy=(int)bounds.getCenterY()-88-(height-128);var im=new BufferedImage(96,height,2);var g=im.createGraphics();g.translate(-ox,-oy);target.drawInactivePatch(g);g.dispose();check(target.getPickupVisual()==null&&!target.isCollected(),"inactive repair doesn't collect/start animation");
   boolean exact=true,originalGround=true,roadSame=true;for(int y=0;y<height;y++)for(int x=0;x<96;x++){
    int expected=RoadSnowTexture.pixel(ox+x,oy+y);if(im.getRGB(x,y)!=expected)exact=false;
    int authored=forest.getRGB(ox+x,oy+y);if(authored==0xff6e8f9c||authored==0xff244151)originalGround=false;
    if((mask.getRGB(ox+x,oy+y)>>>24)!=0&&im.getRGB(x,y)!=expected)roadSame=false;pixels++;
   }
   check(originalGround,"authored repair window is entirely path/item, not forest ground");check(exact,"every repair pixel matches world road material");check(roadSame,"existing path pixels preserved");
   check(target.collect()&&!target.collect(),"single collection");check(target.getPickupVisual().age()==0&&target.getPickupVisual().isActive(),"pickup timeline starts normally");for(int tick=0;tick<33;tick++)target.updateVisual();check(!target.getPickupVisual().isActive(),"pickup terminates normally");
   var collected=new BufferedImage(96,height,2);g=collected.createGraphics();g.translate(-ox,-oy);target.drawCollectedPatch(g);g.dispose();check(Arrays.equals(im.getRGB(0,0,96,height,null,0,96),collected.getRGB(0,0,96,height,null,0,96)),"inactive and collected repair same cached terrain");
  }
  System.out.println("PASS: "+checks+" terrain-repair checks; "+pixels+" pixels around Toby match exact world material; retained rendering verified");
 }
}


