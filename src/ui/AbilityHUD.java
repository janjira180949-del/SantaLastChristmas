package ui;

import entity.Santa;
import game.AbilitySystem;
import java.awt.*;
import java.util.List;
import java.util.Locale;

/** Presentation only: reads the actual combat cooldowns without changing them. */
public final class AbilityHUD {
    public record Slot(String label,String input,int cooldown,int total,Color accent) {
        public boolean ready() { return cooldown==0; }
        public String status() { return ready()?"READY":String.format(Locale.ROOT,"%.1fs",cooldown/60.0); }
        public double progress() { return Math.max(0,Math.min(1,1-cooldown/(double)total)); }
    }
    private static final int SIZE=86,HEIGHT=104,GAP=6;
    private static final Color ATTACK=new Color(247,180,119),DASH=new Color(133,207,244),BELL=new Color(249,210,112);
    private static final GradientPaint FACE=new GradientPaint(0,0,new Color(21,34,55,245),0,HEIGHT,new Color(6,13,27,245));
    public List<Slot> getSlots(Santa santa,AbilitySystem abilities) {
        return List.of(new Slot("Attack","LMB",santa.getAttackCooldownFrames(),23,ATTACK),
            new Slot("Dash","SPACE",santa.getDashCooldownFrames(),Santa.DASH_COOLDOWN,DASH),
            new Slot("Christmas Bell","1",abilities.getBellCooldownFrames(),AbilitySystem.BELL_COOLDOWN,BELL),
            new Slot("Magic Star","2",abilities.skills().starCooldown(),game.SantaSkills.STAR_COOLDOWN,BELL),
            new Slot("Gift Bomb","3",abilities.skills().bombCooldown(),game.SantaSkills.COOLDOWN,new Color(133,219,156)));
    }
    public Rectangle bounds(int width,int height,int index) {
        return new Rectangle(Math.max(HUD.QUEST_X+HUD.QUEST_WIDTH+12,width/2-(5*SIZE+4*GAP)/2)+index*(SIZE+GAP),height-HEIGHT-14,SIZE,HEIGHT);
    }
    public static Rectangle stripBounds(int width,int height){
        int start=Math.max(HUD.QUEST_X+HUD.QUEST_WIDTH+12,width/2-(5*SIZE+4*GAP)/2);
        return new Rectangle(start,height-HEIGHT-14,5*SIZE+4*GAP,HEIGHT);
    }
    public static boolean contains(int width,int height,int x,int y) {
        int start=Math.max(HUD.QUEST_X+HUD.QUEST_WIDTH+12,width/2-(5*SIZE+4*GAP)/2);
        return x>=start&&x<start+5*SIZE+4*GAP&&y>=height-HEIGHT-14&&y<height-14;
    }
    public void draw(Graphics2D graphics,int width,int height,Santa santa,AbilitySystem abilities) {
        List<Slot> slots=getSlots(santa,abilities);
        for(int i=0;i<slots.size();i++)drawSlot(graphics,bounds(width,height,i),slots.get(i),i);
    }
    private void drawSlot(Graphics2D graphics,Rectangle r,Slot slot,int index) {
        Graphics2D g=(Graphics2D)graphics.create();
        try {
            ChristmasUI.frame(g,"tracker",r);
            g.setFont(new Font("Dialog",Font.BOLD,10));g.setColor(new Color(255,236,188));
            ChristmasUI.center(g,slot.input(),r.x+r.width/2,r.y+17);
            g.drawImage(ChristmasUI.icon(index),r.x+(r.width-48)/2,r.y+22,null);
            if(!slot.ready()) {
                g.setColor(new Color(4,10,22,155));g.fillRect(r.x+20,r.y+24,46,44);
                g.setColor(Color.WHITE);g.setFont(new Font("Dialog",Font.BOLD,12));ChristmasUI.center(g,slot.status(),r.x+r.width/2,r.y+49);
            }
            g.setFont(new Font("Dialog",Font.BOLD,index>=2?9:11));g.setColor(UITheme.IVORY);
            ChristmasUI.center(g,slot.label(),r.x+r.width/2,r.y+78);
            g.setFont(new Font("Dialog",Font.PLAIN,9));g.setColor(slot.ready()?slot.accent():UITheme.MUTED);
            ChristmasUI.center(g,slot.ready()?"READY":"RECOVERING",r.x+r.width/2,r.y+90);
            g.setColor(new Color(30,35,44));g.fillRect(r.x+14,r.y+95,r.width-28,3);
            g.setColor(slot.accent());g.fillRect(r.x+14,r.y+95,(int)((r.width-28)*slot.progress()),3);
        } finally { g.dispose(); }
    }
}
