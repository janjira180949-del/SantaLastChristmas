package ui;

import entity.Child;
import entity.Santa;
import game.GamePanel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import quest.StoryQuestManager;
import quest.StoryQuestTarget;

/** State-flow and actual font/raster bounds regression for the Quest HUD. */
public class QuestHudTest {
    static int capture;
    static GamePanel gamePanel;
    static final java.util.Set<String> overlapSizes=new java.util.LinkedHashSet<>();
    static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
    static void verify(StoryQuestManager story,StoryQuestManager.HudState state,String title) throws Exception {
        var view=story.getHudPresentation();
        check(view.state()==state,"Expected "+state+", got "+view);
        check(view.title().equals(title),"Title: "+view);
        BufferedImage text=new BufferedImage(800,600,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=text.createGraphics();
        String[] copy={view.header(),view.title(),view.objective()};
        int[] sizes={HUD.HEADER_FONT_SIZE,HUD.TITLE_FONT_SIZE,HUD.OBJECTIVE_FONT_SIZE},baselines={HUD.HEADER_BASELINE,HUD.TITLE_BASELINE,HUD.OBJECTIVE_BASELINE};
        for(int i=0;i<3;i++) {
            g.setFont(new Font("Dialog",i<2?Font.BOLD:Font.PLAIN,sizes[i]));
            var lines=HUD.wrapQuestText(g.getFontMetrics(),copy[i]);
            check(lines.size()<=(i==0?1:i==1?2:3),"Too many lines: "+copy[i]+lines);
            for(int j=0;j<lines.size();j++) {
                check(g.getFontMetrics().stringWidth(lines.get(j))<=HUD.SAFE_WIDTH,"Width overflow");
                check(baselines[i]+j*HUD.LINE_HEIGHT+g.getFontMetrics().getDescent()<=HUD.SAFE_HEIGHT,"Height overflow");
            }
        }
        g.dispose();
        BufferedImage blank=new BufferedImage(800,600,BufferedImage.TYPE_INT_ARGB);
        g=blank.createGraphics();
        new HUD().drawQuest(g,600,new StoryQuestManager.HudPresentation(state,"","",""));
        g.dispose();
        g=text.createGraphics();new HUD().drawQuest(g,600,view);g.dispose();
        boolean green=false;
        Rectangle safe=new Rectangle(HUD.QUEST_X+HUD.SAFE_X,HUD.questY(600)+HUD.SAFE_Y,
                HUD.SAFE_WIDTH,HUD.SAFE_HEIGHT);
        for(int y=0;y<600;y++) for(int x=0;x<800;x++) {
            if(text.getRGB(x,y)!=blank.getRGB(x,y)) {
                check(safe.contains(x,y),"Text raster outside safe area at "+x+","+y);
                if(text.getRGB(x,y)==HUD.COMPLETE_TEXT.getRGB()) green=true;
            }
        }
        check(green==(state==StoryQuestManager.HudState.COMPLETE),"Green is exclusive to completion");
        for(int[] size:sizesToTest) {
            BufferedImage image=new BufferedImage(size[0],size[1],BufferedImage.TYPE_INT_RGB);
            g=image.createGraphics();
            gamePanel.setSize(size[0],size[1]);
            gamePanel.paint(g);
            g.dispose();
            check(HUD.questY(size[1])>=HUD.HP_Y+HUD.HP_HEIGHT+16,"HP overlap");
            check(HUD.questY(size[1])+HUD.QUEST_HEIGHT==size[1]-14,"Bottom anchor");
            check(HUD.QUEST_X==14 && HUD.QUEST_X+HUD.QUEST_WIDTH<size[0],"Left anchor / visibility");
            verifyAbilitySeparation(size[0],size[1],view);
            ImageIO.write(image,"png",new File("build/quest-hud/"+capture+"-"+size[0]+"x"+size[1]+".png"));
        }
        capture++;
    }
    static void verifyAbilitySeparation(int width,int height,StoryQuestManager.HudPresentation view) throws Exception {
        BufferedImage quest=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=quest.createGraphics();new HUD().drawQuest(g,height,view);g.dispose();
        BufferedImage abilities=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        g=abilities.createGraphics();
        new AbilityHUD().draw(g,width,height,new Santa(0,0),new game.AbilitySystem());
        g.dispose();
        for(int y=height-120;y<height;y++) for(int x=0;x<width;x++) {
            if((quest.getRGB(x,y)>>>24)>0 && (abilities.getRGB(x,y)>>>24)>0)
                overlapSizes.add(width+"x"+height);
        }
    }

    static int[][] sizesToTest;
    public static void main(String[] args)throws Exception {
        sizesToTest=new int[][]{{800,600},{1270,681}};
        new File("build/quest-hud").mkdirs();gamePanel=new GamePanel();gamePanel.beginAdventure();
        game.FinalChapter chapter=new game.FinalChapter();StoryQuestManager story=new StoryQuestManager(chapter::completed,()->3);
        var child=gamePanel.getChildren().get(0);
        verify(story,StoryQuestManager.HudState.ACTIVE,"A Christmas Delivery");story.synchronizeDelivery();
        verify(story,StoryQuestManager.HudState.NEXT_OBJECTIVE,"Lost Gift");
        story.startAfterDialogue(child);verify(story,StoryQuestManager.HudState.ACTIVE,"Lost Gift");
        story.recordVillageCleared();verify(story,StoryQuestManager.HudState.ACTIVE,"Lost Gift");
        var target=story.getTargets().get(0);var b=target.getBounds();Santa santa=new Santa((int)b.getCenterX()-16,(int)b.getCenterY()-21);
        story.completeTarget(target,santa);verify(story,StoryQuestManager.HudState.ACTIVE,"Lost Gift");
        santa.setPosition(child.getX()-1,child.getY());
        check(story.returnGiftToToby(child,santa),"Return completes Lost Gift");verify(story,StoryQuestManager.HudState.ACTIVE,"Restore the Village");
        chapter.unlock();for(var pickup:game.FinalChapter.PICKUPS)chapter.collect(pickup.id());for(var r:game.FinalChapter.Restoration.values())chapter.restore(r);
        check(story.synchronizeRestoration(),"Authoritative chapter progress completes restoration");verify(story,StoryQuestManager.HudState.ACTIVE,"Talk to Toby");
        check(story.acceptBlackFrostClue(child,santa),"Clue handoff activates investigation");verify(story,StoryQuestManager.HudState.ACTIVE,"Investigate the Black Frost");
        for(int i=1;i<=3;i++){story.recordCrystalDestroyed("crystal-"+i);verify(story,StoryQuestManager.HudState.ACTIVE,i==3?"Frost Fang":"Investigate the Black Frost");}
        story.recordFrostFangDefeated();verify(story,StoryQuestManager.HudState.NEXT_OBJECTIVE,"Save Christmas");
        story.acceptEliasQuest();verify(story,StoryQuestManager.HudState.ACTIVE,"Save Christmas");
        story.reachSpiritTree();verify(story,StoryQuestManager.HudState.ACTIVE,"Save Christmas");
        story.recordBossDefeated();verify(story,StoryQuestManager.HudState.ACTIVE,"Save Christmas");
        story.triggerEnding();verify(story,StoryQuestManager.HudState.COMPLETE,"Save Christmas");
        check(overlapSizes.isEmpty(),"Abilities overlap quest: "+overlapSizes);
        System.out.println("PASS: wrapped objective raster bounds and ability separation across three quests and two viewport sizes");
    }
}
