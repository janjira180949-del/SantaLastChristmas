import game.*;
import entity.Santa;
import quest.StoryQuest;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.util.*;

/** Production start, full actor navigation, collision, one-way gate, retry and unchanged quests. */
public class SantaBaseNavigationTest extends V2TestSupport {
    static GamePanel opening()throws Exception {
        GamePanel p=new GamePanel();p.setSize(1270,720);tap(p,KeyEvent.VK_ENTER);tick(p,6);tap(p,KeyEvent.VK_E);return p;
    }
    public static void main(String[]args)throws Exception {
        GamePanel p=opening();WorldMap map=WorldMap.forArea(Area.Type.SANTA_BASE);
        check(areas(p).getAreas().size()==4&&areas(p).getCurrentArea().getType()==Area.Type.SANTA_BASE,"Real Main Menu Start enters registered Base");
        check(map.width()==1600&&map.height()==1280&&map.id().equals("santa_base"),"Permanent authored workshop dimensions");
        check(santa(p).getX()==800&&santa(p).getY()==731&&!util.Collision.intersectsAny(santa(p).getBounds(),map.solids()),"Exact central spawn is safe");
        check(areas(p).getNextArea().getType()==Area.Type.VILLAGE&&!areas(p).isNextAreaAvailable(),"Opening departure now requires actual presents");
        check(enemies(p).isEmpty()&&encounters(p).getEncounters().stream().noneMatch(e->e.getArea()==Area.Type.SANTA_BASE),"No enemy population/balance added to Base");
        var before=story(p);check(!before.isStoryStarted()&&!before.isGiftFound()&&!before.isVillageCleared(),"Base exploration does not advance adventure");
        check(before.getQuests().get(0).getState()==StoryQuest.State.LOCKED&&before.getQuests().get(1).getState()==StoryQuest.State.LOCKED,"Lost Gift locked behind appended delivery quest");
        check(!areas(p).tryTravel(Area.Type.FOREST)&&!areas(p).tryTravel(Area.Type.CHRISTMAS_TOWN),"No unintended destinations from Base");
        for(String marker:List.of("Christmas Tree","Spirit Lantern","Gift Storage","Workshop Machinery","Letter Desk","Journey Map","Exterior Entrance"))check(map.points().containsKey("landmarks/"+marker),"Physical landmark registered: "+marker);
        routes(map);
        // Existing movement actually stops at the west wall and machinery, including dash.
        Santa wall=new Santa(226,600);for(int i=0;i<30;i++)wall.update(false,false,true,false,map.solids());
        check(wall.getX()>=224&&!util.Collision.intersectsAny(wall.getBounds(),map.solids()),"Workshop wall blocks actual movement");
        Santa machine=new Santa(360,469);for(int i=0;i<30;i++)machine.update(true,false,false,false,map.solids());
        check(machine.getY()-8>=448,"Machinery collision follows visible footprint");
        machine.startDash(true,false,false,false);for(int i=0;i<10;i++)machine.update(false,false,false,false,map.solids());
        check(machine.getY()-8>=448,"Dash cannot tunnel into machine");
        santa(p).takeDamage(100);tick(p,1);check(p.getState()==GamePanel.State.GAME_OVER,"Base death uses existing game-over flow");
        tap(p,KeyEvent.VK_R);check(p.getState()==GamePanel.State.PLAYING&&santa(p).getHealth()==100,"Existing R control restores Base health");
        check(areas(p).getCurrentArea().getType()==Area.Type.SANTA_BASE&&santa(p).getX()==800&&santa(p).getY()==731,"Retry retains Base checkpoint");
        check(map.point("exit/to_village").equals(new WorldMap.Point(816,1136)),"Visible departure trigger is inside the southern exterior");
        collectOpeningPresents(p);santa(p).setPosition(800,731);
        p.keyPressed(new java.awt.event.KeyEvent(p,java.awt.event.KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_S,'s'));
        int steps=0;
        while(areas(p).getCurrentArea().getType()==Area.Type.SANTA_BASE&&steps++<300) {
            tick(p,1);if(areas(p).getCurrentArea().getType()==Area.Type.SANTA_BASE) {
                Camera camera=(Camera)field(p,"camera");
                check(santa(p).getBounds().getMaxY()-camera.getY()<p.getHeight()-118,"Santa remains above ability HUD throughout real departure walk");
                check(!util.Collision.intersectsAny(santa(p).getBounds(),map.solids()),"Real walking path stays collision-safe");
            }
        }
        p.keyReleased(new java.awt.event.KeyEvent(p,java.awt.event.KeyEvent.KEY_RELEASED,0,0,KeyEvent.VK_S,'s'));
        check(steps<300&&areas(p).getCurrentArea().getType()==Area.Type.VILLAGE,"Existing movement reaches and crosses permanent departure gate");
        tick(p,1);check(areas(p).getCurrentArea().getType()==Area.Type.VILLAGE,"Departure cannot bounce back");
        check(santa(p).getX()==576&&santa(p).getY()==699&&story(p)==before,"One-way departure preserves original safe arrival and same quest instance");
        check(areas(p).getCurrentExitZones().stream().noneMatch(g->g.getDestination()==Area.Type.SANTA_BASE)&&!areas(p).isConnectionAvailable(Area.Type.SANTA_BASE),"Authorized one-way opening removes Village return gate");
        tick(p,120);check(!story(p).isStoryStarted()&&!story(p).isVillageCleared(),"Arrival does not accept quests or duplicate rewards");
        completeOpeningDeliveries(p);
        at(santa(p),WorldMap.forArea(Area.Type.VILLAGE).point("npc/Toby"));tap(p,KeyEvent.VK_E);closeDialogue(p);
        check(story(p).isStoryStarted()&&story(p).getActiveQuest().getId()==StoryQuest.Id.LOST_GIFT,"Original Toby conversation still starts original quest");
        p.beginAdventure();check(areas(p).getCurrentArea().getType()==Area.Type.SANTA_BASE&&!story(p).isStoryStarted(),"A fresh Start always resets to Base");p.stopGame();
        System.out.println("PASS: "+checks+" Base registration/start/navigation/collision/gate/retry assertions");
    }
    private static void routes(WorldMap map) {
        int cols=100,rows=80;boolean[] free=new boolean[cols*rows],seen=new boolean[cols*rows];var solids=map.solids();
        for(int y=0;y<rows;y++)for(int x=0;x<cols;x++)free[y*cols+x]=!util.Collision.intersectsAny(new Rectangle2D.Double(x*16,y*16-8,32,50),solids);
        int start=46*cols+50;check(free[start],"Spawn's nearest flood-fill cell is free");ArrayDeque<Integer> q=new ArrayDeque<>();q.add(start);seen[start]=true;
        while(!q.isEmpty()) {
            int i=q.remove(),x=i%cols,y=i/cols;
            for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){int nx=x+d[0],ny=y+d[1];if(nx<0||nx>=cols||ny<0||ny>=rows)continue;int next=ny*cols+nx;
                if(!free[next]||seen[next])continue;boolean safe=true;
                for(int step=1;step<=16;step++)if(util.Collision.intersectsAny(new Rectangle2D.Double(x*16+d[0]*step,y*16+d[1]*step-8,32,50),solids)){safe=false;break;}
                if(safe){seen[next]=true;q.add(next);}
            }
        }
        for(int[] target:new int[][]{{800,512},{544,320},{1008,352},{1008,448},{512,752},{1008,784},{800,880},{800,1008},{800,1104},{800,1136}}) {
            int index=target[1]/16*cols+target[0]/16;check(seen[index],"Full actor reaches workshop/exterior approach "+Arrays.toString(target));
        }
    }
}
