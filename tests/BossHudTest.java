package ui;

import boss.DarkTitan;
import boss.FrostFang;
import entity.Enemy;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;

/** Focused presentation checks; historical source/pixel expectations are untouched. */
public final class BossHudTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static double value(HUD.BossHud hud,String name)throws Exception{
        Field field=HUD.BossHud.class.getDeclaredField(name);field.setAccessible(true);return ((Number)field.get(hud)).doubleValue();
    }
    private static BufferedImage render(HUD.BossHud hud,int width,int height){
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();hud.draw(g,HUD.bossBounds(width));g.dispose();return image;
    }
    public static void main(String[] args)throws Exception {
        boss.BossArtwork.prepareAsync().get(30,java.util.concurrent.TimeUnit.SECONDS);
        for(int width:new int[]{800,1270})for(boolean titan:new boolean[]{false,true}){
            int height=width==800?600:720;
            Enemy actor=titan?new DarkTitan(500,500):new FrostFang(500,500);
            int maximum=actor.getMaximumHealth();var hud=new HUD.BossHud();
            hud.update(actor,0);check(value(hud,"health")==maximum,"Authoritative HP sampled on entrance");
            check(value(hud,"opacity")==0,"Entrance begins hidden");
            hud.update(actor,.05);check(value(hud,"opacity")>0&&value(hud,"opacity")<1,"Gradual entrance");
            for(int i=0;i<10;i++)hud.update(actor,.05);
            check(value(hud,"opacity")==1,"Entrance completes");
            Rectangle bounds=HUD.bossBounds(width);
            check(!bounds.intersects(new Rectangle(0,0,259,113)),"Clears Santa HP");
            check(!bounds.intersects(new Rectangle(width-249,0,249,113)),"Clears Spirit");
            check(bounds.y>=35&&bounds.y+bounds.height<=110,"Clears area title and boss silhouette");
            var image=render(hud,width,height);int visible=0;
            for(int y=0;y<height;y++)for(int x=0;x<width;x++)if((image.getRGB(x,y)>>>24)>0){
                visible++;check(bounds.contains(x,y),"No clipped/overflowing HUD pixels");
            }
            check(visible>5000,"Dedicated detailed frame rendered");
            double opacity=value(hud,"opacity");for(int i=0;i<20;i++)render(hud,width,height);
            check(value(hud,"opacity")==opacity,"Pure drawing does not advance model");
            actor.takeDamage(100);hud.update(actor,0);
            check(value(hud,"health")==maximum-100&&actor.getHealth()==maximum-100,"HP accurate immediately, not interpolated");
            check(value(hud,"trail")==maximum&&value(hud,"hit")>0,"Trailing damage and hit flash");
            for(int i=0;i<30;i++)hud.update(actor,.05);
            check(value(hud,"trail")<maximum&&value(hud,"trail")>=actor.getHealth(),"Trail converges without undershooting");
            double trail=value(hud,"trail");hud.update(actor,0);
            check(value(hud,"trail")==trail,"Zero elapsed time freezes presentation");
            actor.takeDamage(maximum/2);hud.update(actor,0);
            check(value(hud,"phase")==1&&value(hud,"phaseFlash")>0,"Phase two highlight from boss API");
            actor.takeDamage(maximum/4);hud.update(actor,0);
            check(value(hud,"phase")==2,"Phase three from boss API");
            check(actor.getMaximumHealth()==maximum&&actor.getSize()==(titan?96:76),"No health/hitbox changes");
            actor.takeDamage(maximum);hud.update(actor,.05);
            check(value(hud,"health")==0&&value(hud,"opacity")>0&&value(hud,"opacity")<1,"Defeat immediately zeroes HP and starts exit");
            for(int i=0;i<8;i++)hud.update(null,.05);
            check(value(hud,"opacity")==0,"Exit completes");
            check((render(hud,width,height).getRGB(bounds.x+80,bounds.y+35)>>>24)==0,"No stale boss after exit");
            Enemy replacement=titan?new FrostFang(0,0):new DarkTitan(0,0);
            hud.update(replacement,.05);check(value(hud,"health")==replacement.getMaximumHealth(),"New boss resets trail/HP");
            hud.update(null,.05);check(value(hud,"opacity")==0,"Abandoned live boss removed immediately");
        }
        var fang=new HUD.BossHud();var titan=new HUD.BossHud();
        for(int i=0;i<8;i++){fang.update(newActor(false),.05);titan.update(newActor(true),.05);}
        // Use a stable actor identity for the two design comparison renders.
        Enemy f=new FrostFang(0,0),t=new DarkTitan(0,0);
        for(int i=0;i<8;i++){fang.update(f,.05);titan.update(t,.05);}
        var a=render(fang,800,600);var b=render(titan,800,600);int differences=0;
        Rectangle box=HUD.bossBounds(800);
        for(int y=box.y;y<box.y+box.height;y++)for(int x=box.x;x<box.x+box.width;x++)if(a.getRGB(x,y)!=b.getRGB(x,y))differences++;
        check(differences>3000,"Distinct frame geometry/portrait, not a simple recolor");
        HUD host=new HUD();var canvas=new BufferedImage(800,600,BufferedImage.TYPE_INT_ARGB);
        var graphics=canvas.createGraphics();var santa=new entity.Santa(0,0);
        var spirit=new spirit.SpiritTree();var abilities=new game.AbilitySystem();
        var story=new quest.StoryQuestManager();var areas=new game.AreaManager(story);
        host.draw(graphics,800,600,santa,spirit,abilities,areas,f,null,story,false,null,null);
        check(graphics.getFont().equals(new java.awt.Font("Dialog",java.awt.Font.PLAIN,11)),"Preserves outgoing font for existing screen labels");
        check(graphics.getColor().equals(UITheme.GOLD),"Preserves outgoing color");
        Field paintTime=HUD.class.getDeclaredField("bossHudPaintTime");paintTime.setAccessible(true);
        paintTime.setLong(host,System.nanoTime()-50_000_000L);
        host.draw(graphics,800,600,santa,spirit,abilities,areas,f,null,story,false,null,null);
        Field model=HUD.class.getDeclaredField("bossHud");model.setAccessible(true);
        check(value((HUD.BossHud)model.get(host),"opacity")>0,"Host draws entrance");
        f.takeDamage(f.getMaximumHealth());paintTime.setLong(host,System.nanoTime()-2_000_000_000L);
        host.draw(graphics,800,600,santa,spirit,abilities,areas,null,null,story,false,null,null);
        check(value((HUD.BossHud)model.get(host),"opacity")==0,"Hidden defeat cutscene cannot resurrect stale boss card");
        graphics.dispose();
        System.out.println("PASS: "+checks+" boss HUD assertions");
    }
    private static Enemy newActor(boolean titan){return titan?new DarkTitan(0,0):new FrostFang(0,0);}
}
